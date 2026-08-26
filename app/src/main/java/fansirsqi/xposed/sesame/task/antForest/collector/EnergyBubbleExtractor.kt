package fansirsqi.xposed.sesame.task.antForest.collector

import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.core.util.TimeUtil
import fansirsqi.xposed.sesame.task.antForest.AntForest
import fansirsqi.xposed.sesame.task.antForest.ForestUtil
import fansirsqi.xposed.sesame.task.antForest.waiting.WaitingTaskDraft
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * 能量球提取器
 *
 * 单一职责：从用户主页 JSON 中提取可收取/待成熟的能量球（[extractBubbleInfo]），
 * 以及"收自己能量球"的阈值判断（[shouldCollectSelfBubble]）。
 *
 * V2 §3.3.3：新增纯解析 [parseBubbles]（无副作用），
 * 是否登记/提交/收取由调用方决定：
 * - 普通遍历路径（[extractBubbleInfo]）：收取 available、将 waiting 转成 [WaitingTaskDraft] 登记蹲点；
 * - 蹲点路径：只处理本次主页中的 available，不递归提交新任务；
 * - 恢复/验证路径：只读状态，不产生提交副作用。
 */
internal class EnergyBubbleExtractor(
    private val task: AntForest,
    private val cache: RoundCache,
) {

    /**
     * 收取状态的枚举类型
     */
    enum class CollectStatus {
        AVAILABLE, WAITING, INSUFFICIENT, ROBBED
    }

    /**
     * 单个能量球解析结果（纯数据，不含任何副作用）。
     */
    data class BubbleInfo(
        val bubbleId: Long,
        val energy: Int,
        val status: CollectStatus,
        val produceTime: Long,
        val canBeRobbedAgain: Boolean,
    )

    /**
     * 一次主页解析的快照：按状态分类，调用方自行决定后续动作。
     * INSUFFICIENT 与 ROBBED 均归入 [robbed]（本链路均不处理）。
     */
    data class BubbleSnapshot(
        val available: List<BubbleInfo>,
        val waiting: List<BubbleInfo>,
        val robbed: List<BubbleInfo>,
    )

    /**
     * 纯解析能量球状态（V2 §3.3.3）。
     * 不读取用户名缓存、不做阈值/保护判断、不提交蹲点任务。
     *
     * @param userHomeObj 用户主页的JSON对象
     * @param serverTime  服务器时间（用于判断等待球的成熟时间是否有效）
     */
    @Throws(JSONException::class)
    fun parseBubbles(userHomeObj: JSONObject, serverTime: Long): BubbleSnapshot {
        // 兼容组队模式：团队主页的能量球挂在 mainMember 下
        val jaBubbles = if (userHomeObj.optString("nextAction", "") == "Team") {
            userHomeObj.optJSONObject("teamHomeResult")
                ?.optJSONObject("mainMember")
                ?.optJSONArray("bubbles")
        } else {
            userHomeObj.optJSONArray("bubbles")
        } ?: JSONArray()

        val available = ArrayList<BubbleInfo>()
        val waiting = ArrayList<BubbleInfo>()
        val robbed = ArrayList<BubbleInfo>()

        for (i in 0 until jaBubbles.length()) {
            val bubble = jaBubbles.getJSONObject(i)
            val bubbleId = bubble.getLong("id")
            val statusStr = bubble.getString("collectStatus")
            val status = CollectStatus.valueOf(statusStr)
            val bubbleCount = bubble.getInt("fullEnergy")
            val produceTime = bubble.optLong("produceTime", 0L)
            val canBeRobbedAgain = bubble.optBoolean("canBeRobbedAgain", false)

            val info = BubbleInfo(bubbleId, bubbleCount, status, produceTime, canBeRobbedAgain)
            when (status) {
                CollectStatus.AVAILABLE -> available.add(info)
                CollectStatus.WAITING -> waiting.add(info)
                CollectStatus.INSUFFICIENT, CollectStatus.ROBBED -> robbed.add(info)
            }
        }
        return BubbleSnapshot(available, waiting, robbed)
    }

    /**
     * 提取能量球状态
     * 在 AVAILABLE 和 WAITING 分支增加了阈值判断（收自己能量球方式/阈值配置）
     *
     * @param userHomeObj      用户主页的JSON对象
     * @param serverTime       服务器时间
     * @param availableBubbles 可收集的能量球ID列表
     * @param userId           用户ID
     * @throws JSONException JSON解析异常
     */
    @Throws(JSONException::class)
    fun extractBubbleInfo(
        userHomeObj: JSONObject,
        serverTime: Long,
        availableBubbles: MutableList<Long>,
        userId: String?
    ) {
        val snapshot = parseBubbles(userHomeObj, serverTime)
        if (snapshot.available.isEmpty() && snapshot.waiting.isEmpty()) return

        // 2. 获取用户名（用于日志）
        val userName = cache.getAndCacheUserName(userId, userHomeObj, null)
        var waitingBubblesCount = 0

        // 3. 保护罩/炸弹卡日志记录（仅针对好友，仅做显示，实际拦截在collectEnergy）
        val isSelf = task.selfId == userId
        var protectionLog = ""
        if (!isSelf) {
            val shieldEndTime = ForestUtil.getShieldEndTime(userHomeObj)
            val bombEndTime = ForestUtil.getBombCardEndTime(userHomeObj)
            val hasShield = shieldEndTime > serverTime
            val hasBomb = bombEndTime > serverTime
            if (hasShield || hasBomb) {
                if (hasShield) {
                    val remainingTime = task.formatTimeDifference(shieldEndTime - serverTime)
                    protectionLog += " 保护罩剩余: $remainingTime. "
                }
                if (hasBomb) {
                    val remainingTime = task.formatTimeDifference(bombEndTime - serverTime)
                    protectionLog += " 炸弹卡剩余: $remainingTime."
                }
            }
        }

        // 4a. AVAILABLE：统一调用阈值判断后加入可收取列表
        snapshot.available.forEach { bubble ->
            if (isSelf) {
                // 获取是否还能被偷取的标记 (保底状态下该值为 false)
                if (shouldCollectSelfBubble(bubble.energy, bubble.canBeRobbedAgain)) {
                    availableBubbles.add(bubble.bubbleId)
                }
            } else {
                // 好友的能量直接添加，不进行阈值判断
                availableBubbles.add(bubble.bubbleId)
            }
        }

        // 4b. WAITING：阈值 + 保护覆盖判断通过后登记蹲点任务
        snapshot.waiting.forEach { bubble ->
            if (bubble.energy <= 0) {
                Log.record(AntForest.TAG, "跳过数量为[${bubble.bubbleId}]的等待能量球的蹲点任务")
                return@forEach
            }

            // 蹲点任务也必须严格遵循收自己能量的阈值配置
            if (isSelf) {
                // 对于等待中的球，我们暂时假设它是可被偷的(canBeRobbedAgain=true)以进行严格检查
                // 逻辑：如果只收>20g，现在有个5g的在等待，应该跳过，不加入蹲点队列
                if (!shouldCollectSelfBubble(bubble.energy, bubble.canBeRobbedAgain)) {
                    return@forEach
                }
            }

            // 等待成熟的能量球，添加到蹲点队列
            if (bubble.produceTime > 0 && bubble.produceTime > serverTime) {
                // 检查保护罩时间（仅好友）：如果保护罩覆盖整个成熟期，跳过蹲点
                // 自己的账号：无论是否有保护罩都要添加蹲点（到时间后直接收取）
                if (!isSelf && task.shieldManager.shouldSkipWaitingTaskDueToProtection(userHomeObj, bubble.produceTime, serverTime)) {
                    val shieldEndTime = ForestUtil.getShieldEndTime(userHomeObj)
                    val bombEndTime = ForestUtil.getBombCardEndTime(userHomeObj)
                    val protectionEndTime = maxOf(shieldEndTime, bombEndTime)
                    val remainingHours = (protectionEndTime - serverTime) / (1000 * 60 * 60)
                    Log.record(
                        AntForest.TAG,
                        "⏭️ 跳过好友蹲点[$userName]球[${bubble.bubbleId}]：保护罩覆盖整个成熟期(保护还剩${remainingHours}h，能量${TimeUtil.getCommonDate(bubble.produceTime)}成熟)"
                    )
                    return@forEach
                }

                waitingBubblesCount++
                // 添加蹲点任务（保护时间在下方提交前已由 shouldSkipWaitingTaskDueToProtection 判断，
                // 主号保护时间传 0，登记时由管理器按 produceTime 到点收取）
                task.submitWaitingDraft(
                    WaitingTaskDraft(
                        userId = userId ?: "",
                        userName = userName ?: "未知用户",
                        bubbleId = bubble.bubbleId,
                        produceTime = bubble.produceTime,
                        fromTag = "蹲点收取"
                    )
                )
                Log.record(
                    AntForest.TAG,
                    "添加蹲点: [$userName] 能量球[${bubble.bubbleId}] 将在[${TimeUtil.getCommonDate(bubble.produceTime)}]成熟$protectionLog"
                )
            }
        }

        // 5. 打印调试信息
        // 只有当有可收取的球，或者有等待的球时才打印，避免刷屏
        if (availableBubbles.isNotEmpty() || waitingBubblesCount > 0) {
            Log.record(AntForest.TAG, "[$userName] 可收集能量球: ${availableBubbles.size}个")
            if (waitingBubblesCount > 0) {
                Log.record(AntForest.TAG, "[$userName] 等待成熟能量球: ${waitingBubblesCount}个")
            }
        }
    }

    /**
     * 统一判断是否满足收自己能量的阈值条件
     * @param bubbleCount 能量球数值
     * @param canBeRobbedAgain 是否可被再次偷取（保底状态为false）
     */
    fun shouldCollectSelfBubble(bubbleCount: Int, canBeRobbedAgain: Boolean): Boolean {
        val type = task.collectSelfEnergyType?.value ?: AntForest.CollectSelfType.ALL
        val threshold = task.collectSelfEnergyThreshold?.value ?: 0

        return when (type) {
            AntForest.CollectSelfType.OVER_THRESHOLD -> {
                // 模式：大于阈值才收
                // 逻辑：只有当 [小于阈值] 且 [还能被偷] 时才跳过 (不收)
                // 如果已经到底了(!canBeRobbedAgain)，即使小于阈值也应该收回来，防止浪费
                if (bubbleCount < threshold && canBeRobbedAgain) {
                    false
                } else {
                    // 满足阈值 OR 触发保底收取 (能量很少了，朋友偷不走，必须自己收，不然就浪费了)
                    if (bubbleCount < threshold && !canBeRobbedAgain) {
                        Log.record(AntForest.TAG, "触发保底收取：能量[$bubbleCount g] < 阈值[$threshold g]，但已无法被偷，强制收取")
                    }
                    true
                }
            }
            AntForest.CollectSelfType.BELOW_THRESHOLD -> {
                // 模式：小于阈值才收
                bubbleCount < threshold
            }
            // CollectSelfType.ALL -> 默认 true
            else -> true
        }
    }
}
