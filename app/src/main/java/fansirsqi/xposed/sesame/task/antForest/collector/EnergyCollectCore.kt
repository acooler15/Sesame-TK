package fansirsqi.xposed.sesame.task.antForest.collector

import fansirsqi.xposed.sesame.core.reflect.ReflectUtil
import fansirsqi.xposed.sesame.data.RuntimeInfo
import fansirsqi.xposed.sesame.entity.CollectEnergyEntity
import fansirsqi.xposed.sesame.hook.RequestManager.requestString
import fansirsqi.xposed.sesame.hook.Toast
import fansirsqi.xposed.sesame.hook.ApplicationHook
import fansirsqi.xposed.sesame.core.notify.Notify.updateLastExecText
import fansirsqi.xposed.sesame.core.notify.Notify.updateStatusText
import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.core.threads.GlobalThreadPools
import fansirsqi.xposed.sesame.core.util.Average
import fansirsqi.xposed.sesame.core.util.RandomUtil
import fansirsqi.xposed.sesame.core.util.ResChecker
import fansirsqi.xposed.sesame.core.util.TimeUtil
import fansirsqi.xposed.sesame.task.antForest.AntForest
import fansirsqi.xposed.sesame.task.antForest.AntForestRpcCall
import fansirsqi.xposed.sesame.task.antForest.ForestStatistics
import fansirsqi.xposed.sesame.task.antForest.ForestUtil
import fansirsqi.xposed.sesame.task.antForest.ForestUtil.hasBombCard
import fansirsqi.xposed.sesame.task.antForest.ForestUtil.hasShield
import fansirsqi.xposed.sesame.task.antForest.waiting.BubbleOutcome
import fansirsqi.xposed.sesame.task.antForest.waiting.RpcFailureKind
import fansirsqi.xposed.sesame.task.antForest.waiting.RpcResult
import fansirsqi.xposed.sesame.task.antForest.waiting.WaitingBatchResult
import fansirsqi.xposed.sesame.ui.ObjReference
import fansirsqi.xposed.sesame.util.maps.UserMap
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.util.Random
import kotlin.math.min

/**
 * 能量收取核心引擎
 *
 * 单一职责：承载各收取路径共享的基础能力——查询主页后收取（[collectEnergy]）、
 * 批量/逐一收取（[collectVivaEnergy]）、单个 RPC 收取（[collectEnergy] Entity 版本），
 * 以及收取间隔/重试/双击等共享状态。
 */
internal class EnergyCollectCore(
    private val task: AntForest,
    private val cache: RoundCache,
    private val extractor: EnergyBubbleExtractor,
) {
    internal var tryCountInt: Int? = null
    internal var retryIntervalInt: Int? = null
    internal var collectIntervalEntity: fansirsqi.xposed.sesame.hook.rpc.intervallimit.IntervalLimit? = null
    internal var doubleCollectIntervalEntity: fansirsqi.xposed.sesame.hook.rpc.intervallimit.IntervalLimit? = null

    internal var jsonCollectMap: MutableSet<String?> = HashSet()

    internal val delayTimeMath = Average(5)

    private val collectEnergyLockLimit = ObjReference(0L)

    private val emojiList: ArrayList<String> = ArrayList(
        listOf(
            "🍅", "🍓", "🥓", "🍂", "🍚", "🌰", "🟢", "🌴",
            "🥗", "🧀", "🥩", "🍍", "🌶️", "🍲", "🍆", "🥕",
            "✨", "🍑", "🍘", "🍀", "🥞", "🍈", "🥝", "🧅",
            "🌵", "🌾", "🥜", "🍇", "🌭", "🥑", "🥐", "🥖",
            "🍊", "🌽", "🍉", "🍖", "🍄", "🥚", "🥙", "🥦",
            "🍌", "🍱", "🍏", "🍎", "🌲", "🌿", "🍁", "🍒",
            "🥔", "🌯", "🌱", "🍐", "🍞", "🍳", "🍙", "🍋",
            "🍗", "🌮", "🍃", "🥘", "🥒", "🧄", "🍠", "🥥"
        )
    )
    private val random = Random()

    /**
     * 收取用户能量
     *
     * @param userId      用户ID
     * @param userHomeObj 用户主页的JSON对象，包含用户的蚂蚁森林信息
     * @return 更新后的用户主页JSON对象，如果发生异常返回null
     */
    fun collectEnergy(
        userId: String?,
        userHomeObj: JSONObject?,
        fromTag: String?
    ): JSONObject? {
        try {
            if (userHomeObj == null) {
                return null
            }
            // 1. 检查接口返回是否成功
            if (!ResChecker.checkRes(AntForest.TAG + "载入用户主页失败:", userHomeObj)) {
                Log.record(AntForest.TAG, "载入失败: " + userHomeObj.optString("resultDesc", "未知错误"))
                return userHomeObj
            }
            val serverTime = userHomeObj.optLong("now", System.currentTimeMillis())
            val isSelf = userId == UserMap.currentUid

            // 2. 自己的能量不受缓存限制，好友的能量检查缓存避免重复处理
            if (!isSelf && cache.containsProcessed(userId)) {
                return userHomeObj
            }

            // 标记用户为已处理（无论是否成功收取能量）
            cache.addProcessed(userId)
            val userName = cache.getAndCacheUserName(userId, userHomeObj, fromTag)

            // 3. 判断是否允许收取能量 (开关关闭 或 在黑名单中)
            if (!task.collectEnergy!!.value || jsonCollectMap.contains(userId)) {
                Log.record(AntForest.TAG, "[$userName] 不允许收取能量，跳过")
                return userHomeObj
            }

            // 4. 获取所有可收集的能量球 (extractBubbleInfo 内部已包含"收自己阈值"的逻辑)
            val availableBubbles: MutableList<Long> = ArrayList()
            extractor.extractBubbleInfo(userHomeObj, serverTime, availableBubbles, userId)

            if (availableBubbles.isEmpty()) {
                // 记录空森林的时间戳，避免本轮重复检查
                cache.markEmpty(userId)
                return userHomeObj
            }

            // 5. 检查是否有能量罩或炸弹卡保护
            var hasProtection = false
            if (!isSelf) {
                // 检查保护罩
                if (hasShield(userHomeObj, serverTime)) {
                    hasProtection = true
                    Log.record(AntForest.TAG, "[$userName]被能量罩❤️保护着哟，跳过收取")
                }

                // 检查炸弹卡 及 阈值判断逻辑
                if (!hasProtection && hasBombCard(userHomeObj, serverTime)) {
                    var bypassBomb = false
                    val bombLimit = task.collectBombEnergyLimit?.value ?: 0

                    // 如果设定了阈值(>0)，检查是否有大额能量球值得冒险
                    if (bombLimit > 0) {
                        val bubbles = userHomeObj.optJSONArray("bubbles")
                        if (bubbles != null) {
                            for (i in 0 until bubbles.length()) {
                                val bubble = bubbles.getJSONObject(i)
                                // 获取能量值 (fullEnergy通常是当前可收取的能量)
                                val energy = bubble.optInt("fullEnergy", 0)
                                if (energy >= bombLimit) {
                                    bypassBomb = true
                                    Log.record(AntForest.TAG, "[$userName] 发现大能量球($energy g) >= 炸弹阈值($bombLimit g)，无视炸弹卡强行收取！💥")
                                    break
                                }
                            }
                        }
                    }

                    if (!bypassBomb) {
                        hasProtection = true
                        Log.record(AntForest.TAG, "[$userName]开着炸弹卡💣，跳过收取")
                    }
                }
            }

            // 6. 只有没有保护(或无视保护)时才收集当前可用能量
            if (!hasProtection) {
                collectVivaEnergy(userId, userHomeObj, availableBubbles, fromTag)
            }

            return userHomeObj
        } catch (e: org.json.JSONException) {
            Log.printStackTrace(AntForest.TAG, "collectUserEnergy JSON解析错误", e)
            return null
        } catch (e: NullPointerException) {
            Log.printStackTrace(AntForest.TAG, "collectUserEnergy 空指针异常", e)
            return null
        } catch (t: Throwable) {
            Log.printStackTrace(AntForest.TAG, "collectUserEnergy 出现异常", t)
            return null
        }
    }

    /**
     * 批量或逐一收取能量
     *
     * @param userId      用户ID
     * @param userHomeObj 用户主页的JSON对象
     * @param bubbleIds   能量球ID列表
     * @param fromTag     收取来源标识
     * @param skipPropCheck 是否跳过道具检查（用于蹲点收取快速通道）
     */
    fun collectVivaEnergy(
        userId: String?,
        userHomeObj: JSONObject?,
        bubbleIds: MutableList<Long>,
        fromTag: String?,
        skipPropCheck: Boolean = false
    ) {
        val bizType = "GREEN"
        if (bubbleIds.isEmpty()) return
        val isBatchCollect = task.batchRobEnergy!!.value
        if (isBatchCollect) {
            var i = 0
            while (i < bubbleIds.size) {
                val subList: MutableList<Long> =
                    bubbleIds.subList(i, min(i + MAX_BATCH_SIZE, bubbleIds.size))
                collectEnergy(
                    CollectEnergyEntity(
                        userId,
                        userHomeObj,
                        AntForestRpcCall.batchEnergyRpcEntity(bizType, userId, subList),
                        fromTag,
                        skipPropCheck  // 传递快速通道标记
                    )
                )
                i += MAX_BATCH_SIZE
            }
        } else {
            for (id in bubbleIds) {
                collectEnergy(
                    CollectEnergyEntity(
                        userId,
                        userHomeObj,
                        AntForestRpcCall.energyRpcEntity(bizType, userId, id),
                        fromTag,
                        skipPropCheck  // 传递快速通道标记
                    )
                )
            }
        }
    }

    /**
     * 蹲点专用类型化收取（V2 §3.3.4）：一次请求、每球结果。
     *
     * 与普通 [collectVivaEnergy] 的区别：
     * - suspend 实现，不调用 Runnable 包装、不使用 runBlocking；
     * - 从本次 RPC 响应直接累计能量，不读取全局统计差值（避免并发普通收取造成误判）；
     * - 双击结果继续合并到相同 bubbleId；
     * - 分片请求部分成功时保留已成功结果；
     * - [CancellationException] 直接向上传播。
     */
    suspend fun collectVivaEnergyForWaiting(
        userId: String,
        expectedBubbleIds: Set<Long>,
    ): WaitingBatchResult {
        if (expectedBubbleIds.isEmpty()) {
            return WaitingBatchResult.Completed(emptyMap(), 0)
        }
        val outcomes = LinkedHashMap<Long, BubbleOutcome>()
        var exactCollectedEnergy = 0
        var doubleCollect = mutableListOf<Long>()

        // 分片请求：每片 ≤ MAX_BATCH_SIZE；双击球与下一片一起重试
        val batches = expectedBubbleIds.toList().chunked(MAX_BATCH_SIZE)
        for (batch in batches) {
            val toCollect = (batch + doubleCollect).distinct()
            doubleCollect = mutableListOf()
            if (toCollect.isEmpty()) continue

            when (val r = AntForestRpcCall.collectEnergyResult(userId, toCollect)) {
                is RpcResult.Ok -> {
                    val parsed = parseCollectResponse(userId, r.value, toCollect.toSet())
                    parsed.outcomes.forEach { (id, outcome) ->
                        outcomes[id] = outcome
                        if (outcome is BubbleOutcome.Collected) exactCollectedEnergy += outcome.energy
                    }
                    doubleCollect.addAll(parsed.robAgain)
                }
                is RpcResult.Failed -> {
                    toCollect.forEach { id -> outcomes[id] = BubbleOutcome.Failed(id, r) }
                }
            }
        }

        // 双击收尾：对仍可再偷的球再收一次，能量合并到相同 bubbleId
        if (doubleCollect.isNotEmpty()) {
            when (val r = AntForestRpcCall.collectEnergyResult(userId, doubleCollect)) {
                is RpcResult.Ok -> {
                    val parsed = parseCollectResponse(userId, r.value, doubleCollect.toSet())
                    parsed.outcomes.forEach { (id, outcome) ->
                        val existing = outcomes[id]
                        if (existing is BubbleOutcome.Collected && outcome is BubbleOutcome.Collected) {
                            outcomes[id] = existing.copy(energy = existing.energy + outcome.energy)
                            exactCollectedEnergy += outcome.energy
                        } else {
                            outcomes[id] = outcome
                            if (outcome is BubbleOutcome.Collected) exactCollectedEnergy += outcome.energy
                        }
                    }
                }
                is RpcResult.Failed -> {
                    doubleCollect.forEach { id ->
                        // 双击失败不覆盖已有成功结果，仅当该球尚无结果时标记失败
                        if (!outcomes.containsKey(id)) outcomes[id] = BubbleOutcome.Failed(id, r)
                    }
                }
            }
        }

        // 覆盖所有 expected：分片失败等场景下未产生结果的球标记为 Gone
        expectedBubbleIds.forEach { id ->
            if (!outcomes.containsKey(id)) outcomes[id] = BubbleOutcome.Gone(id)
        }

        // 展示统计：原子累加，不反向读取全局统计差值
        if (exactCollectedEnergy > 0) {
            ForestStatistics.addToTotalCollected(exactCollectedEnergy)
        }
        return WaitingBatchResult.Completed(outcomes, exactCollectedEnergy)
    }

    /**
     * 解析一次 collectEnergy 响应的每球结果。
     * @return 每球 outcome 与需要双击再收的球（canBeRobbedAgain=true）
     */
    private fun parseCollectResponse(
        userId: String,
        response: JSONObject,
        requested: Set<Long>,
    ): CollectParseResult {
        val resultCode = response.optString("resultCode")
        if (!"SUCCESS".equals(resultCode, ignoreCase = true)) {
            val failure = when {
                resultCode == "PARAM_ILLEGAL2" ->
                    RpcResult.Failed(RpcFailureKind.ALREADY_COLLECTED, code = resultCode)
                ForestUtil.checkAndRecordFrequencyError(userId, response) ->
                    RpcResult.Failed(RpcFailureKind.FREQUENCY, code = resultCode)
                else ->
                    RpcResult.Failed(RpcFailureKind.SERVER_REJECTED, code = resultCode)
            }
            return CollectParseResult(requested.associateWith { BubbleOutcome.Failed(it, failure) }, emptyList())
        }

        val outcomes = LinkedHashMap<Long, BubbleOutcome>()
        val robAgain = mutableListOf<Long>()
        val seen = HashSet<Long>()
        val bubbles = response.optJSONArray("bubbles")
        if (bubbles != null) {
            for (i in 0 until bubbles.length()) {
                val b = bubbles.getJSONObject(i)
                val id = b.optLong("id", 0L)
                if (id == 0L) continue
                seen.add(id)
                val collectedEnergy = b.optInt("collectedEnergy", 0)
                when {
                    collectedEnergy > 0 -> outcomes[id] = BubbleOutcome.Collected(id, collectedEnergy)
                    b.optBoolean("canBeRobbedAgain", false) -> robAgain.add(id)
                    else -> outcomes[id] = BubbleOutcome.AlreadyCollected(id)
                }
            }
        }
        // 未出现在响应中的 requested 球 → Gone
        requested.filter { it !in seen }.forEach { outcomes[it] = BubbleOutcome.Gone(it) }
        return CollectParseResult(outcomes, robAgain)
    }

    /** 一次 collectEnergy 响应的解析结果。 */
    private data class CollectParseResult(
        val outcomes: Map<Long, BubbleOutcome>,
        val robAgain: List<Long>,
    )

    /**
     * 单个/批量 RPC 收取能量
     */
    fun collectEnergy(collectEnergyEntity: CollectEnergyEntity) {
        if (AntForest.errorWait) {
            Log.record(AntForest.TAG, "异常⌛等待中...不收取能量")
            return
        }
        val runnable = Runnable {
            try {
                val userId = collectEnergyEntity.userId
                // 从 CollectEnergyEntity 中读取是否跳过道具检查的标记
                val skipPropCheck = collectEnergyEntity.skipPropCheck ?: false
                runBlocking { task.itemManager.usePropBeforeCollectEnergy(userId, skipPropCheck) }
                val rpcEntity = collectEnergyEntity.rpcEntity!!
                val needDouble = collectEnergyEntity.needDouble
                val needRetry = collectEnergyEntity.needRetry
                val tryCount = collectEnergyEntity.addTryCount()
                var collected = 0
                val startTime: Long

                synchronized(collectEnergyLockLimit) {
                    val sleep: Long
                    if (needDouble) {
                        collectEnergyEntity.unsetNeedDouble()
                        val interval = doubleCollectIntervalEntity!!.interval
                        sleep =
                            (interval ?: 1000) - System.currentTimeMillis() + collectEnergyLockLimit.get()!!
                    } else if (needRetry) {
                        collectEnergyEntity.unsetNeedRetry()
                        sleep =
                            retryIntervalInt!! - System.currentTimeMillis() + collectEnergyLockLimit.get()!!
                    } else {
                        val interval = collectIntervalEntity!!.interval
                        sleep =
                            (interval ?: 1000) - System.currentTimeMillis() + collectEnergyLockLimit.get()!!
                    }
                    if (sleep > 0) {
                        GlobalThreadPools.sleepCompat(sleep)
                    }
                    startTime = System.currentTimeMillis()
                    collectEnergyLockLimit.setForce(startTime)
                }

                runBlocking {
                    requestString(rpcEntity, 0, 0)
                }
                val spendTime = System.currentTimeMillis() - startTime
                if (task.balanceNetworkDelay!!.value) {
                    delayTimeMath.nextInteger((spendTime / 3).toInt())
                }

                if (rpcEntity.hasError) {
                    val errorCode = ReflectUtil.callMethod(
                        rpcEntity.responseObject,
                        "getString",
                        "error"
                    ) as String?
                    if ("1004" == errorCode) {
                        if (ApplicationHook.config.waitWhenException.value > 0) {
                            val waitTime =
                                System.currentTimeMillis() + ApplicationHook.config.waitWhenException.value
                            RuntimeInfo.getInstance()
                                .put(RuntimeInfo.RuntimeInfoKey.ForestPauseTime, waitTime)
                            updateStatusText("异常")
                            Log.record(AntForest.TAG, "触发异常,等待至" + TimeUtil.getCommonDate(waitTime))
                            AntForest.errorWait = true
                            return@Runnable
                        }
                        GlobalThreadPools.sleepCompat((600 + RandomUtil.delay()).toLong())
                    }
                    if (tryCount < tryCountInt!!) {
                        collectEnergyEntity.setNeedRetry()
                        collectEnergy(collectEnergyEntity)
                    }
                    return@Runnable
                }

                val responseString: String = rpcEntity.responseString ?: ""
                val jo = JSONObject(responseString)
                val resultCode = jo.getString("resultCode")
                if (!"SUCCESS".equals(resultCode, ignoreCase = true)) {
                    if ("PARAM_ILLEGAL2" == resultCode) {
                        Log.record(AntForest.TAG, "[" + cache.getAndCacheUserName(userId) + "]" + "能量已被收取,取消重试 错误:" + jo.getString("resultDesc"))
                        return@Runnable
                    }

                    // 检测并记录"手速太快"错误
                    if (ForestUtil.checkAndRecordFrequencyError(userId, jo)) {
                        return@Runnable
                    }

                    Log.record(AntForest.TAG, "[" + cache.getAndCacheUserName(userId) + "]" + jo.optString("resultDesc", ""))
                    if (tryCount < tryCountInt!!) {
                        collectEnergyEntity.setNeedRetry()
                        collectEnergy(collectEnergyEntity)
                    }
                    return@Runnable
                }

                // --- 收能量逻辑保持原样 ---
                val jaBubbles = jo.getJSONArray("bubbles")
                val jaBubbleLength = jaBubbles.length()
                if (jaBubbleLength > 1) {
                    val newBubbleIdList: MutableList<Long?> = ArrayList()
                    for (i in 0..<jaBubbleLength) {
                        val bubble = jaBubbles.getJSONObject(i)
                        if (bubble.getBoolean("canBeRobbedAgain")) {
                            newBubbleIdList.add(bubble.getLong("id"))
                        }
                        collected += bubble.getInt("collectedEnergy")
                    }
                    if (collected > 0) {
                        val randomIndex = random.nextInt(emojiList.size)
                        val randomEmoji = emojiList[randomIndex]
                        val collectType = when (collectEnergyEntity.fromTag) {
                            "takeLook" -> "找能量一键收取️"
                            "蹲点收取" -> "蹲点一键收取️"
                            else -> "一键收取️"
                        }
                        val str =
                            collectType + randomEmoji + collected + "g[" + cache.getAndCacheUserName(
                                userId
                            ) + "]#"
                        ForestStatistics.addToTotalCollected(collected)
                        if (needDouble) {
                            Log.forest(str + "耗时[" + spendTime + "]ms[双击]")
                            Toast.show("$str[双击]")
                        } else {
                            Log.forest(str + "耗时[" + spendTime + "]ms")
                            Toast.show(str)
                        }
                    }
                    if (!newBubbleIdList.isEmpty()) {
                        collectEnergyEntity.rpcEntity = AntForestRpcCall.batchEnergyRpcEntity(
                            "",
                            userId,
                            newBubbleIdList
                        )
                        collectEnergyEntity.setNeedDouble()
                        collectEnergyEntity.resetTryCount()
                        collectEnergy(collectEnergyEntity)
                    }
                } else if (jaBubbleLength == 1) {
                    val bubble = jaBubbles.getJSONObject(0)
                    collected += bubble.getInt("collectedEnergy")
                    if (collected > 0) {
                        val randomIndex = random.nextInt(emojiList.size)
                        val randomEmoji = emojiList[randomIndex]
                        val collectType = when (collectEnergyEntity.fromTag) {
                            "takeLook" -> "找能量收取"
                            "蹲点收取" -> "蹲点收取"
                            else -> "普通收取"
                        }
                        val str =
                            collectType + randomEmoji + collected + "g[" + cache.getAndCacheUserName(
                                userId
                            ) + "]"
                        ForestStatistics.addToTotalCollected(collected)
                        if (needDouble) {
                            Log.forest(str + "耗时[" + spendTime + "]ms[双击]")
                            Toast.show("$str[双击]")
                        } else {
                            Log.forest(str + "耗时[" + spendTime + "]ms")
                            Toast.show(str)
                        }
                    }
                    if (bubble.getBoolean("canBeRobbedAgain")) {
                        collectEnergyEntity.setNeedDouble()
                        collectEnergyEntity.resetTryCount()
                        collectEnergy(collectEnergyEntity)
                        return@Runnable
                    }

                    val userHome = collectEnergyEntity.userHome
                    if (userHome != null) {
                        val bizNo = userHome.optString("bizNo")
                        if (bizNo.isNotEmpty()) {
                            val returnCount = getReturnCount(collected)
                            if (returnCount > 0) {
                                // ✅ 调用 returnFriendWater 增加通知好友开关
                                val notify = task.notifyFriend!!.value // 从配置获取
                                runBlocking { task.returnFriendWater(userId, bizNo, 1, returnCount, notify) }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.printStackTrace(AntForest.TAG, "collectEnergy err", e)
            } finally {
                val strTotalCollected =
                    "本次总 收:" + ForestStatistics.totalCollected + "g 帮:" + ForestStatistics.TOTAL_HELP_COLLECTED + "g 浇:" + ForestStatistics.TOTAL_WATERED + "g"
                updateLastExecText(strTotalCollected)
                task.notifyMain()
            }
        }
        task.taskCount.incrementAndGet()
        runnable.run()
    }

    private fun getReturnCount(collected: Int): Int {
        var returnCount = 0
        if (task.returnWater33!!.value in 1..collected) {
            returnCount = 33
        } else if (task.returnWater18!!.value in 1..collected) {
            returnCount = 18
        } else if (task.returnWater10!!.value in 1..collected) {
            returnCount = 10
        }
        return returnCount
    }

    private companion object {
        const val MAX_BATCH_SIZE = 6
    }
}
