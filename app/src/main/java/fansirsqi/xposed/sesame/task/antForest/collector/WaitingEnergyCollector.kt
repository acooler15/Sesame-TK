package fansirsqi.xposed.sesame.task.antForest.collector

import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.core.util.ResChecker
import fansirsqi.xposed.sesame.task.antForest.AntForest
import fansirsqi.xposed.sesame.task.antForest.AntForestRpcCall
import fansirsqi.xposed.sesame.task.antForest.ForestUtil
import fansirsqi.xposed.sesame.task.antForest.waiting.BubbleOutcome
import fansirsqi.xposed.sesame.task.antForest.waiting.RpcFailureKind
import fansirsqi.xposed.sesame.task.antForest.waiting.RpcResult
import fansirsqi.xposed.sesame.task.antForest.waiting.WaitingBatchResult
import fansirsqi.xposed.sesame.task.antForest.waiting.WaitingCollectRequest
import kotlinx.coroutines.CancellationException
import org.json.JSONObject

/**
 * 蹲点收取器（V2 §3.3.4/§3.3.5）
 *
 * 单一职责：为蹲点管理器提供批量、请求级的能量收取（collectUserEnergyForWaiting），
 * 只处理本次主页中 expected 的球，不递归提交新任务、不读取全局统计差值。
 *
 * 与 collectEnergy 不同：不调用 collectEnergy，避免其内部 collectVivaEnergy 造成二次收取
 * （collectEnergy 已收一次后再收第二次，空收导致误判失败 + 无意义重试）、避免向
 * processedUsersCache 写入（否则蹲点收完后本轮排行榜遍历会跳过该好友的其他可收球）。
 */
internal class WaitingEnergyCollector(
    private val task: AntForest,
    private val core: EnergyCollectCore,
    private val extractor: EnergyBubbleExtractor,
    private val cache: RoundCache,
) {

    /**
     * 批量、请求级蹲点收取。
     * @param request 期望收取的球及其版本戳
     * @return 每球结果；主页查询失败返回请求级 [WaitingBatchResult.RequestFailed]
     */
    suspend fun collectUserEnergyForWaiting(request: WaitingCollectRequest): WaitingBatchResult {
        return try {
            when (val home = AntForestRpcCall.queryFriendHomePageResult(request.userId, "蹲点收取")) {
                is RpcResult.Ok -> collectFromHome(request, home.value)
                is RpcResult.Failed -> WaitingBatchResult.RequestFailed(home)
            }
        } catch (e: CancellationException) {
            // 协程取消是正常现象，必须继续抛出以保证取消机制正常工作
            throw e
        } catch (e: Exception) {
            WaitingBatchResult.RequestFailed(RpcResult.Failed(RpcFailureKind.UNKNOWN, message = e.message))
        }
    }

    /**
     * 基于好友主页执行蹲点收取：逐球决定 outcome，一次收取已成熟且未被保护覆盖的球。
     */
    private suspend fun collectFromHome(
        request: WaitingCollectRequest,
        friendHomeObj: JSONObject,
    ): WaitingBatchResult {
        // 1. 接口返回校验（避免用无效响应继续收取）
        if (!ResChecker.checkRes(AntForest.TAG + "载入用户主页失败:", friendHomeObj)) {
            return WaitingBatchResult.RequestFailed(
                RpcResult.Failed(
                    RpcFailureKind.SERVER_REJECTED,
                    message = friendHomeObj.optString("resultDesc", "未知错误")
                )
            )
        }
        val serverTime = friendHomeObj.optLong("now", System.currentTimeMillis())
        val realUserName = cache.getAndCacheUserName(request.userId, friendHomeObj, "蹲点收取")
        val isSelf = request.userId == request.token.ownerUid
        Log.record(
            AntForest.TAG,
            "蹲点收取：用户[$realUserName] userId=${request.userId} isSelf=${isSelf} 期望${request.expected.size}个球"
        )

        // 2. 收能量开关 + 黑名单检查（与 collectEnergy 保持一致）
        if (!task.collectEnergy!!.value || core.jsonCollectMap.contains(request.userId)) {
            Log.record(AntForest.TAG, "[$realUserName] 不允许收取能量，跳过蹲点")
            val failure = RpcResult.Failed(RpcFailureKind.SERVER_REJECTED, message = "不允许收取能量")
            return WaitingBatchResult.Completed(
                request.expected.keys.associateWith { BubbleOutcome.Failed(it, failure) },
                0
            )
        }

        // 3. 保护检查（仅好友）：SKIP_IF_PROTECTION_COVERS_MATURITY（V2 §4）
        val shieldEndTime = if (!isSelf) ForestUtil.getShieldEndTime(friendHomeObj) else 0L
        val bombEndTime = if (!isSelf) ForestUtil.getBombCardEndTime(friendHomeObj) else 0L
        val protectionEndTime = maxOf(shieldEndTime, bombEndTime)

        // 4. 解析主页球（纯解析，蹲点路径不递归提交新任务）
        val snapshot = extractor.parseBubbles(friendHomeObj, serverTime)
        val availableIds = snapshot.available.mapTo(HashSet()) { it.bubbleId }
        val waitingById = snapshot.waiting.associateBy { it.bubbleId }

        // 5. 逐球决定 outcome：保护覆盖 → Protected；成熟可收 → 加入收取；仍等待 → StillWaiting；否则 → Gone
        val outcomes = LinkedHashMap<Long, BubbleOutcome>()
        val toCollect = mutableListOf<Long>()
        request.expected.forEach { (bubbleId, stamp) ->
            val covered = !isSelf && protectionEndTime > stamp.produceTime
            when {
                covered -> outcomes[bubbleId] = BubbleOutcome.Protected(bubbleId, protectionEndTime)
                bubbleId in availableIds -> toCollect.add(bubbleId)
                bubbleId in waitingById -> {
                    val w = waitingById.getValue(bubbleId)
                    outcomes[bubbleId] = BubbleOutcome.StillWaiting(bubbleId, w.produceTime)
                }
                else -> outcomes[bubbleId] = BubbleOutcome.Gone(bubbleId)
            }
        }

        // 6. 收取本次主页中已成熟且未被保护覆盖的球（类型化逐球结果）
        if (toCollect.isNotEmpty()) {
            when (val batch = core.collectVivaEnergyForWaiting(request.userId, toCollect.toSet())) {
                is WaitingBatchResult.Completed -> {
                    batch.outcomes.forEach { (id, outcome) -> outcomes[id] = outcome }
                }
                is WaitingBatchResult.RequestFailed -> {
                    toCollect.forEach { id -> outcomes[id] = BubbleOutcome.Failed(id, batch.failure) }
                }
            }
        }

        // 7. 汇总请求级结果（Completed.outcomes 覆盖所有 expected）
        val exactCollectedEnergy = outcomes.values.filterIsInstance<BubbleOutcome.Collected>().sumOf { it.energy }
        return WaitingBatchResult.Completed(outcomes, exactCollectedEnergy)
    }
}
