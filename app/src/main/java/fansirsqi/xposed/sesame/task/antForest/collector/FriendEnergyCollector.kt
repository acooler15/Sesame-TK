package fansirsqi.xposed.sesame.task.antForest.collector

import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.core.threads.GlobalThreadPools
import fansirsqi.xposed.sesame.core.util.ResChecker
import fansirsqi.xposed.sesame.core.util.TimeCounter
import fansirsqi.xposed.sesame.data.Status
import fansirsqi.xposed.sesame.task.antForest.AntForest
import fansirsqi.xposed.sesame.task.antForest.AntForestRpcCall
import fansirsqi.xposed.sesame.task.antForest.ForestUtil
import fansirsqi.xposed.sesame.task.antForest.ForestUtil.hasBombCard
import fansirsqi.xposed.sesame.task.antForest.ForestUtil.hasShield
import fansirsqi.xposed.sesame.util.maps.UserMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * 函数式接口，用于提供RPC调用
 */
internal fun interface RpcSupplier<T> {
    @Throws(Exception::class)
    suspend fun get(): T?
}

/**
 * 函数式接口，用于对JSON对象进行断言
 */
internal fun interface JsonPredicate<T> {
    @Throws(Exception::class)
    fun test(t: T?): Boolean
}

/**
 * 好友能量收取器
 *
 * 单一职责：排行榜好友收取（collectFriendEnergyCoroutine / collectRankingsCoroutine）、
 * 找能量（collectEnergyByTakeLook / quickcollectEnergyByTakeLook）、
 * 批量好友处理（processFriendsEnergyCoroutine / processEnergyInternal）与礼物盒领取。
 */
internal class FriendEnergyCollector(
    private val task: AntForest,
    private val core: EnergyCollectCore,
    private val cache: RoundCache,
) {

    // 并发控制信号量，限制同时处理的好友数量，避免过多并发导致性能问题
    private val concurrencyLimiter = Semaphore(60)

    /**
     * 协程版本的排行榜收取方法
     */
    internal suspend fun collectRankingsCoroutine(
        rankingName: String?,
        rpcCall: RpcSupplier<String?>,
        jsonArrayKey: String?,
        flag: String,
        preCondition: JsonPredicate<JSONObject?>?
    ) = withContext(Dispatchers.Default) {
        try {
            Log.record(AntForest.TAG, "开始处理$rankingName...")
            val tc = TimeCounter(AntForest.TAG)
            var rankingObject: JSONObject? = null
            for (i in 0..2) {
                var response: String? = null
                try {
                    response = rpcCall.get()
                    if (response != null && !response.isEmpty()) {
                        rankingObject = JSONObject(response)
                        break
                    }
                } catch (e: Exception) {
                    Log.printStackTrace(
                        AntForest.TAG,
                        "collectRankings $rankingName, response: $response",
                        e
                    )
                }
                if (i < 2) {
                    Log.record(AntForest.TAG, "获取" + rankingName + "失败，" + (5 * (i + 1)) + "秒后重试")
                    GlobalThreadPools.sleepCompat(5000L * (i + 1))
                }
            }

            if (rankingObject == null) {
                Log.error(AntForest.TAG, "获取" + rankingName + "失败")
                return@withContext
            }
            if (!ResChecker.checkRes(AntForest.TAG + "获取" + rankingName + "失败:", rankingObject)) {
                Log.error(
                    AntForest.TAG,
                    "获取" + rankingName + "失败: " + rankingObject.optString("resultDesc")
                )
                return@withContext
            }
            val totalDatas = rankingObject.optJSONArray(jsonArrayKey)
            if (totalDatas == null) {
                Log.record(AntForest.TAG, rankingName + "数据为空，跳过处理。")
                return@withContext
            }
            Log.record(
                AntForest.TAG,
                "成功获取" + rankingName + "数据，共发现" + totalDatas.length() + "位好友。"
            )
            tc.countDebug("获取$rankingName")
            if (preCondition != null && !preCondition.test(rankingObject)) {
                return@withContext
            }
            // 处理前20个  超过会报错
            Log.record(AntForest.TAG, "开始处理" + rankingName + "前20位好友...")
            val friendRanking = rankingObject.optJSONArray("friendRanking")
            if (friendRanking != null) {
                processFriendsEnergyCoroutine(friendRanking, flag, "${rankingName}前20位")
            }
            tc.countDebug("处理" + rankingName + "靠前的好友")
            // 分批并行处理后续的（协程版本）
            if (totalDatas.length() <= 20) {
                Log.record(AntForest.TAG, rankingName + "没有更多的好友需要处理，跳过")
                return@withContext
            }

            // 处理所有好友（无限制模式）
            val remainingToProcess = totalDatas.length() - 20

            if (remainingToProcess <= 0) {
                Log.record(AntForest.TAG, rankingName + "已处理前20位好友，跳过后续处理")
                return@withContext
            }

            val idList: MutableList<String?> = ArrayList()
            val batchSize = 20
            val batches = (remainingToProcess + batchSize - 1) / batchSize
            Log.record(
                AntForest.TAG,
                "🌟 处理所有好友：" + rankingName + "共${totalDatas.length()}位好友，需处理后续${remainingToProcess}位，共${batches}批"
            )

            // 串行处理批次，避免总并发数过高
            var batchCount = 0

            for (pos in 20..<totalDatas.length()) {
                // 检查协程是否被取消
                if (!isActive) {
                    Log.record(AntForest.TAG, "协程被取消，停止处理${rankingName}批次")
                    return@withContext
                }

                val friend = totalDatas.getJSONObject(pos)
                val userId = friend.getString("userId")
                if (userId == task.selfId) continue
                idList.add(userId)

                if (idList.size == batchSize) {
                    val batch: MutableList<String?> = ArrayList(idList)
                    val currentBatchNum = ++batchCount

                    // 串行执行：等待当前批次完成再处理下一批次
                    Log.record(AntForest.TAG, "[批次$currentBatchNum/$batches] 开始处理...")
                    try {
                        processFriendsEnergyCoroutine(batch, flag, "批次$currentBatchNum")
                        Log.record(AntForest.TAG, "[批次$currentBatchNum/$batches] 处理完成")
                    } catch (e: CancellationException) {
                        Log.record(AntForest.TAG, "[批次$currentBatchNum/$batches] 被取消")
                        throw e
                    }

                    idList.clear()
                }
            }

            // 处理剩余的用户
            if (idList.isNotEmpty()) {
                // 检查协程是否被取消
                if (!isActive) {
                    Log.record(AntForest.TAG, "协程被取消，跳过${rankingName}剩余用户处理")
                    return@withContext
                }

                val currentBatchNum = ++batchCount
                Log.record(AntForest.TAG, "[批次$currentBatchNum/$batches] 开始处理...")
                try {
                    processFriendsEnergyCoroutine(idList, flag, "批次$currentBatchNum")
                    Log.record(AntForest.TAG, "[批次$currentBatchNum/$batches] 处理完成")
                } catch (e: CancellationException) {
                    Log.record(AntForest.TAG, "[批次$currentBatchNum/$batches] 被取消")
                    throw e
                }
            }
            tc.countDebug("分批处理" + rankingName + "其他好友")
            Log.record(AntForest.TAG, "收取" + rankingName + "能量完成！")
        } catch (e: CancellationException) {
            // 协程被取消是正常行为，不记录错误日志
            Log.record(AntForest.TAG, "处理" + rankingName + "时协程被取消")
            throw e // 重新抛出，让协程系统处理
        } catch (e: Exception) {
            Log.printStackTrace(AntForest.TAG, "collectRankings 异常", e)
        }
    }

    /**
     * 协程版本：收取好友能量
     */
    suspend fun collectFriendEnergyCoroutine() {
        collectRankingsCoroutine(
            "好友排行榜",
            { AntForestRpcCall.queryFriendsEnergyRanking() },
            "totalDatas",
            "普通好友",
            null
        )
    }

    /**
     * 使用找能量功能收取好友能量（协程版本 - 修正版）
     * 逻辑：服务器自动轮询，返回空 friendId 代表无更多目标
     */
    suspend fun collectEnergyByTakeLook() {
        // 1. 冷却检查
        val currentTime = System.currentTimeMillis()
        if (currentTime < nextTakeLookTime) {
            val remaining = (nextTakeLookTime - currentTime) / 1000
            Log.record(AntForest.TAG, "找能量冷却中，等待 ${remaining / 60}分${remaining % 60}秒")
            return
        }

        val tc = TimeCounter(AntForest.TAG)
        var foundCount = 0
        val maxAttempts = 10
        var consecutiveEmpty = 0
        var shouldCooldown = false

        // 本地去重集合：防止单次运行中服务器重复返回同一个有保护罩的人
        val visitedInSession = mutableSetOf<String>()
        // 空参数对象，仅为了满足接口签名
        val emptyParam = JSONObject()

        Log.record(AntForest.TAG, "开始找能量 (服务器自动轮询)")

        try {
            loop@ for (attempt in 1..maxAttempts) {
                // A. 调用接口
                val takeLookResult = try {
                    // 传空参，由服务器自动分配
                    val resStr = AntForestRpcCall.takeLook(emptyParam)
                    JSONObject(resStr)
                } catch (e: Exception) {
                    Log.printStackTrace(AntForest.TAG, "找能量接口异常", e)
                    shouldCooldown = true
                    break@loop
                }

                // B. 检查接口返回是否成功
                if (!ResChecker.checkRes("${AntForest.TAG} 接口业务失败:", takeLookResult)) {
                    break@loop
                }

                // C. 核心判断：获取 friendId
                val friendId = takeLookResult.optString("friendId")

                // 如果 friendId 为空，说明服务器那边已经没有可以收取的对象了
                if (friendId.isNullOrBlank()) {
                    consecutiveEmpty++
                    Log.record(AntForest.TAG, "第$attempt 次未发现有能量的好友")

                    // 连续2次没有返回ID，说明真的没了，直接结束
                    if (consecutiveEmpty >= 2) {
                        Log.record(AntForest.TAG, "系统无可偷取目标，结束")
                        break@loop
                    }
                    // 缓冲一下重试
                    GlobalThreadPools.sleepCompat(500L)
                    continue@loop
                }

                // D. 排除自己
                if (friendId == task.selfId) {
                    Log.record(AntForest.TAG, "发现自己，跳过")
                    consecutiveEmpty++ // 某种意义上也是无效结果
                    continue@loop
                }

                // E. 本地重复检查 (防止死循环刷同一个有盾的人)
                if (visitedInSession.contains(friendId)) {
                    Log.record(AntForest.TAG, "本次已检查过用户($friendId)，跳过")
                    consecutiveEmpty++
                    if (consecutiveEmpty >= 3) break@loop // 如果一直重复返回已访问的人，也没必要继续了
                    continue@loop
                }

                // 标记已访问
                visitedInSession.add(friendId)

                // F. 检查全局黑名单 (如之前炸弹被记录的人)
                if (cache.containsSkip(friendId)) {
                    continue@loop
                }
                // G. 查询主页详情
                val friendHomeObj = task.queryFriendHome(friendId, "TAKE_LOOK")
                if (friendHomeObj == null) {
                    continue@loop
                }

                // H. 检查保护罩/炸弹
                val now = System.currentTimeMillis()
                val hasShield = hasShield(friendHomeObj, now)
                val hasBomb = hasBombCard(friendHomeObj, now)

                if (hasShield || hasBomb) {
                    val friendName = UserMap.getMaskName(friendId) ?: "未知好友"
                    val type = if (hasShield) "保护罩" else "炸弹卡"
                    Log.record(AntForest.TAG, "发现[$friendName]有$type，跳过")
                    // 记录到全局缓存，防止下次运行再次浪费时间查询
                    cache.addSkip(friendId, "baohuzhao")
                    // 注意：这里不需要传给服务器 skipUsers，因为我们单纯不收，服务器下次轮询可能还会给，但被上面的 visitedInSession 拦截
                } else {
                    // I. 收取能量
                    core.collectEnergy(friendId, friendHomeObj, "takeLook")
                    foundCount++
                    consecutiveEmpty = 0 // 重置空计数

                    // 收取成功后，稍微等待，模拟人为操作并给服务器状态同步时间
                    GlobalThreadPools.sleepCompat(1200L)
                }
            }
        } catch (e: Exception) {
            Log.printStackTrace(AntForest.TAG, "找能量流程异常", e)
        } finally {
            // 逻辑结束后的状态处理
            if (shouldCooldown) {
                nextTakeLookTime = System.currentTimeMillis() + TAKE_LOOK_COOLDOWN_MS
            } else {
                // 正常结束，下次可立即执行（或者根据需求设置一个小间隔）
                nextTakeLookTime = 0
            }
            val msg = "找能量结束，本次收取: $foundCount 个"
            Log.record(AntForest.TAG, msg)
            tc.countDebug(msg)
        }
    }

    /**
     * 7点-7点30分快速收取能量，跳过道具判断
     */
    internal suspend fun quickcollectEnergyByTakeLook() {
        // 1. 冷却检查
        val currentTime = System.currentTimeMillis()
        if (currentTime < nextTakeLookTime) {
            val remaining = (nextTakeLookTime - currentTime) / 1000
            Log.record(AntForest.TAG, "找能量冷却中，等待 ${remaining / 60}分${remaining % 60}秒")
            return
        }

        val tc = TimeCounter(AntForest.TAG)
        var foundCount = 0
        val maxAttempts = 10
        var consecutiveEmpty = 0
        var shouldCooldown = false

        // 本地去重集合：只防止单次运行中死循环刷同一个人，不跨运行记忆
        val visitedInSession = mutableSetOf<String>()
        val emptyParam = JSONObject()

        Log.record(AntForest.TAG, "开始找能量 (无视黑名单与道具)")

        try {
            loop@ for (attempt in 1..maxAttempts) {
                // A. 调用接口
                val takeLookResult = try {
                    val resStr = AntForestRpcCall.takeLook(emptyParam)
                    JSONObject(resStr)
                } catch (e: Exception) {
                    Log.printStackTrace(AntForest.TAG, "找能量接口异常", e)
                    shouldCooldown = true
                    break@loop
                }

                // B. 检查接口返回是否成功
                if (!ResChecker.checkRes("${AntForest.TAG} 接口业务失败:", takeLookResult)) {
                    break@loop
                }

                // C. 获取 friendId
                val friendId = takeLookResult.optString("friendId")

                // 如果 friendId 为空，说明服务器无目标推荐
                if (friendId.isNullOrBlank()) {
                    consecutiveEmpty++
                    Log.record(AntForest.TAG, "第$attempt 次未发现有能量的好友")

                    if (consecutiveEmpty >= 2) {
                        Log.record(AntForest.TAG, "系统无可偷取目标，结束")
                        break@loop
                    }
                    GlobalThreadPools.sleepCompat(500L)
                    continue@loop
                }

                // D. 排除自己
                if (friendId == task.selfId) {
                    Log.record(AntForest.TAG, "发现自己，跳过")
                    consecutiveEmpty++
                    continue@loop
                }

                // E. 本地会话去重 (防止服务器一直返回同一个ID造成本次死循环)
                if (visitedInSession.contains(friendId)) {
                    Log.record(AntForest.TAG, "本次已检查过用户($friendId)，跳过")
                    consecutiveEmpty++
                    if (consecutiveEmpty >= 3) break@loop
                    continue@loop
                }

                // 标记已访问
                visitedInSession.add(friendId)

                // G. 查询主页详情 (获取能量球ID必须步骤)
                val friendHomeObj = task.queryFriendHome(friendId, "TAKE_LOOK")
                if (friendHomeObj == null) {
                    continue@loop
                }

                // I. 直接收取能量
                // 即使有保护罩（收0g）或炸弹（可能扣能量），也执行收取动作
                core.collectEnergy(friendId, friendHomeObj, "takeLook")

                foundCount++
                consecutiveEmpty = 0 // 重置空计数

                // 模拟操作延迟
                GlobalThreadPools.sleepCompat(500L)
            }
        } catch (e: Exception) {
            Log.printStackTrace(AntForest.TAG, "找能量流程异常", e)
        } finally {
            if (shouldCooldown) {
                nextTakeLookTime = System.currentTimeMillis() + TAKE_LOOK_COOLDOWN_MS
            } else {
                nextTakeLookTime = 0
            }
            val msg = "找能量结束，本次尝试收取: $foundCount 个"
            Log.record(AntForest.TAG, msg)
            tc.countDebug(msg)
        }
    }

    /**
     * 统一的协程批量好友处理方法
     *
     * @param friendSource 好友数据源，可以是：
     *   - JSONArray: 直接的好友列表
     *   - MutableList<String?>: 用户ID列表，需要通过API获取
     * @param flag 标记（空字符串=普通好友，"pk"=PK好友）
     * @param sourceName 数据源名称（用于日志）
     */
    private suspend fun processFriendsEnergyCoroutine(
        friendSource: Any,
        flag: String,
        sourceName: String = "好友"
    ) = withContext(Dispatchers.Default) {
        try {
            if (AntForest.errorWait) return@withContext

            val friendList: JSONArray? = when (friendSource) {
                is JSONArray -> {
                    // 直接的好友列表
                    friendSource
                }

                is MutableList<*> -> {
                    // 用户ID列表，需要通过API获取详细信息
                    @Suppress("UNCHECKED_CAST")
                    val userIds = friendSource as MutableList<String?>
                    val jsonStr = if (flag == "pk") {
                        AntForestRpcCall.fillUserRobFlag(JSONArray(userIds), true)
                    } else {
                        AntForestRpcCall.fillUserRobFlag(JSONArray(userIds))
                    }
                    val batchObj = JSONObject(jsonStr)
                    batchObj.optJSONArray("friendRanking")
                }

                else -> {
                    Log.error(AntForest.TAG, "不支持的好友数据源类型: ${friendSource.javaClass.simpleName}")
                    return@withContext
                }
            }

            if (friendList == null) {
                Log.record(AntForest.TAG, "${sourceName}数据为空，跳过处理")
                return@withContext
            }

            if (friendList.length() == 0) {
                Log.record(AntForest.TAG, "${sourceName}列表为空，跳过处理")
                return@withContext
            }

            // 先收集并显示所有好友名单
            val friendNames = mutableListOf<String>()
            for (i in 0..<friendList.length()) {
                val friendObj = friendList.getJSONObject(i)
                val userId = friendObj.optString("userId", "")
                val displayName = friendObj.optString("displayName", UserMap.getMaskName(userId) ?: "")
                friendNames.add(displayName)
            }

            Log.record(AntForest.TAG, "📋 开始处理${friendList.length()}个${sourceName}（并发数:60）")
            Log.record(AntForest.TAG, "👥 ${friendNames.joinToString(" | ")}")
            val startTime = System.currentTimeMillis()

            // 使用协程并发处理每个好友（带并发控制）
            val friendJobs = mutableListOf<Deferred<Unit>>()
            for (i in 0..<friendList.length()) {
                val friendObj = friendList.getJSONObject(i)
                val job = async {
                    concurrencyLimiter.acquire()
                    try {
                        // 直接调用内部方法，减少一层包装以提高性能
                        processEnergyInternal(friendObj, flag)
                    } catch (e: Exception) {
                        Log.printStackTrace(AntForest.TAG, "处理好友异常", e)
                    } finally {
                        concurrencyLimiter.release()
                    }
                }
                friendJobs.add(job)
            }

            // 等待所有好友处理完成
            friendJobs.awaitAll()
            val elapsed = System.currentTimeMillis() - startTime
            Log.record(AntForest.TAG, "✅ ${sourceName}处理完成，耗时${elapsed}ms，平均${elapsed / friendList.length()}ms/人")

        } catch (e: CancellationException) {
            // 协程被取消是正常行为，不记录错误日志
            Log.record(AntForest.TAG, "处理${sourceName}时协程被取消")
            throw e // 重新抛出，让协程系统处理
        } catch (e: JSONException) {
            Log.printStackTrace(AntForest.TAG, "解析${sourceName}数据失败", e)
        } catch (e: Exception) {
            Log.printStackTrace(AntForest.TAG, "处理${sourceName}出错", e)
        }
    }

    /**
     * 处理单个好友的核心逻辑（无锁）
     *
     * @param obj  好友/PK好友 的JSON对象
     * @param flag 标记是普通好友还是PK好友
     */
    @Throws(Exception::class)
    private suspend fun processEnergyInternal(obj: JSONObject, flag: String?) {
        if (AntForest.errorWait) return
        val userId = obj.getString("userId")
        if (userId == task.selfId) return  // 跳过自己
        // 检查是否在"手速太快"冷却期
        if (ForestUtil.isUserInFrequencyCooldown(userId)) {
            return  // 跳过处理
        }
        var userName = obj.optString("displayName", UserMap.getMaskName(userId) ?: "")
        if (cache.containsEmpty(userId)) { //本轮已知为空的树林
            return
        }

        val isPk = "pk" == flag
        if (isPk) {
            userName = "PK榜好友|$userName"
        }
        if (isPk) {
            val needCollectEnergy = task.collectEnergy!!.value && task.pkEnergy!!.value
            if (!needCollectEnergy) {
                Log.record(AntForest.TAG, "    PK好友: [$userName$userId], 不满足收取条件，跳过")
                return
            }
            Log.record(AntForest.TAG, "  正在查询PK好友 [$userName$userId] 的主页...")
            core.collectEnergy(userId, task.queryFriendHome(userId, "PKContest"), "pk")
        } else { // 普通好友
            val needCollectEnergy =
                task.collectEnergy!!.value && !core.jsonCollectMap.contains(userId)
            val needHelpProtect = task.helpFriendCollectType!!.value != AntForest.HelpFriendCollectType.NONE && obj.optBoolean("canProtectBubble") && Status.canProtectBubbleToday(task.selfId)
            val needCollectGiftBox = task.collectGiftBox!!.value && obj.optBoolean("canCollectGiftBox")
            if (!needCollectEnergy && !needHelpProtect && !needCollectGiftBox) {
                return
            }
            var userHomeObj: JSONObject? = null
            // 只要开启了收能量，就进去看看，以便添加蹲点
            if (needCollectEnergy) {
                // 即使排行榜信息显示没有可收能量，也进去检查，以便添加蹲点任务
                Log.record(AntForest.TAG, "  正在查询好友 [$userName$userId] 的主页...")
                userHomeObj = core.collectEnergy(userId, task.queryFriendHome(userId, null), "friend")
            }
            if (needHelpProtect) {
                val isProtected = task.shieldManager.isIsProtected(userId)
                if (isProtected) {
                    if (userHomeObj == null) {
                        userHomeObj = task.queryFriendHome(userId, null)
                    }
                    if (userHomeObj != null) {
                        task.shieldManager.protectFriendEnergy(userHomeObj)
                    }
                }
            }
            // 尝试领取礼物盒
            if (needCollectGiftBox) {
                if (userHomeObj == null) {
                    userHomeObj = task.queryFriendHome(userId, null)
                }
                if (userHomeObj != null) {
                    collectGiftBox(userHomeObj)
                }
            }
        }
    }

    /**
     * 协程版本：收取排名靠前好友能量
     */
    private suspend fun collectGiftBox(userHomeObj: JSONObject) {
        try {
            val giftBoxInfo = userHomeObj.optJSONObject("giftBoxInfo")
            val userEnergy = userHomeObj.optJSONObject("userEnergy")
            val userId =
                if (userEnergy == null) UserMap.currentUid else userEnergy.optString("userId")
            if (giftBoxInfo != null) {
                val giftBoxList = giftBoxInfo.optJSONArray("giftBoxList")
                if (giftBoxList != null && giftBoxList.length() > 0) {
                    for (ii in 0..<giftBoxList.length()) {
                        try {
                            val giftBox = giftBoxList.getJSONObject(ii)
                            val giftBoxId = giftBox.getString("giftBoxId")
                            val title = giftBox.getString("title")
                            val giftBoxResult =
                                JSONObject(AntForestRpcCall.collectFriendGiftBox(giftBoxId, userId))
                            if (!ResChecker.checkRes(AntForest.TAG + "领取好友礼盒失败:", giftBoxResult)) {
                                Log.record(giftBoxResult.getString("resultDesc"))
                                Log.record(giftBoxResult.toString())
                                continue
                            }
                            val energy = giftBoxResult.optInt("energy", 0)
                            Log.forest("礼盒能量🎁[" + UserMap.getMaskName(userId) + "-" + title + "]#" + energy + "g")
                        } catch (t: Throwable) {
                            Log.printStackTrace(t)
                            break
                        } finally {
                            GlobalThreadPools.sleepCompat(500L)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.printStackTrace(e)
        }
    }

    private companion object {
        // 找能量功能的冷却时间（毫秒），15分钟
        const val TAKE_LOOK_COOLDOWN_MS = 15 * 60 * 1000L

        /**
         * 下次可以执行找能量的时间戳
         * 使用 @Volatile 确保多线程环境下的可见性
         */
        @Volatile
        var nextTakeLookTime: Long = 0
    }
}
