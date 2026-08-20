package fansirsqi.xposed.sesame.task.antForest

import android.annotation.SuppressLint
import fansirsqi.xposed.sesame.core.reflect.ReflectUtil
import fansirsqi.xposed.sesame.data.RuntimeInfo
import fansirsqi.xposed.sesame.data.Status
import fansirsqi.xposed.sesame.entity.CollectEnergyEntity
import fansirsqi.xposed.sesame.hook.RequestManager.requestString
import fansirsqi.xposed.sesame.hook.Toast
import fansirsqi.xposed.sesame.hook.rpc.intervallimit.FixedOrRangeIntervalLimit
import fansirsqi.xposed.sesame.hook.rpc.intervallimit.IntervalLimit
import fansirsqi.xposed.sesame.task.antForest.ForestUtil.hasBombCard
import fansirsqi.xposed.sesame.task.antForest.ForestUtil.hasShield
import fansirsqi.xposed.sesame.hook.ApplicationHook
import fansirsqi.xposed.sesame.core.notify.Notify.updateLastExecText
import fansirsqi.xposed.sesame.core.notify.Notify.updateStatusText
import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.core.threads.GlobalThreadPools
import fansirsqi.xposed.sesame.core.util.Average
import fansirsqi.xposed.sesame.core.util.RandomUtil
import fansirsqi.xposed.sesame.core.util.ResChecker
import fansirsqi.xposed.sesame.core.util.TimeCounter
import fansirsqi.xposed.sesame.core.util.TimeUtil
import fansirsqi.xposed.sesame.task.TaskCommon
import fansirsqi.xposed.sesame.ui.ObjReference
import fansirsqi.xposed.sesame.util.maps.UserMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Random
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

internal class ForestEnergyCollector(private val task: AntForest) {
    private val TAG = AntForest.TAG

    private val isEnergyLoopRunning = AtomicBoolean(false)
    internal val delayTimeMath = Average(5)
    private val collectEnergyLockLimit = ObjReference(0L)

    // 并发控制信号量，限制同时处理的好友数量，避免过多并发导致性能问题
    // 设置为60
    private val concurrencyLimiter = Semaphore(60)

    internal var tryCountInt: Int? = null
    internal var retryIntervalInt: Int? = null
    internal var collectIntervalEntity: IntervalLimit? = null
    internal var doubleCollectIntervalEntity: IntervalLimit? = null

    internal var jsonCollectMap: MutableSet<String?> = HashSet()

    var emojiList: ArrayList<String> = ArrayList(
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
     * 用户名缓存：userId -> userName 的映射
     */
    private val userNameCache: ConcurrentHashMap<String, String> = ConcurrentHashMap()

    /**
     * 已处理用户缓存：记录本轮已处理过的用户ID，避免重复处理
     */
    private val processedUsersCache: ConcurrentHashMap.KeySetView<String, Boolean> = ConcurrentHashMap.newKeySet()

    /**
     * 空森林缓存，用于记录在本轮任务中已经确认没有能量的好友。
     * 在每轮蚂蚁森林任务开始时清空（见run方法finally块）。
     * “一轮任务”通常指由"执行间隔"触发的一次完整的好友遍历。
     */
    private val emptyForestCache: ConcurrentHashMap<String, Long> = ConcurrentHashMap<String, Long>()

    /**
     * 跳过用户缓存，用于记录有保护罩或其他需要跳过的用户
     * Key: 用户ID，Value: 跳过原因（如"baohuzhao"表示有保护罩）
     */
    private val skipUsersCache: ConcurrentHashMap<String, String> = ConcurrentHashMap<String, String>()

    /**
     * 只收能量时间的循环任务（协程版本）
     */
    internal fun startEnergyCollectionLoop() {
        if (!isEnergyLoopRunning.compareAndSet(false, true)) {
            Log.record(TAG, "只收能量循环任务已在运行中，跳过重复启动。")
            return
        }
        try {
            val energyTimeStr = ApplicationHook.config.energyTime.value.toString()
            Log.record(TAG, "⏸ 当前为只收能量时间【$energyTimeStr】，开始循环收取自己、好友和PK好友的能量")
            runBlocking {
                try {
                    while (true) {
                        // 每次循环更新状态
                        TaskCommon.update()
                        // 如果不在能量时间段，退出循环
                        val now = Calendar.getInstance()
                        val hour = now.get(Calendar.HOUR_OF_DAY)
                        val minute = now.get(Calendar.MINUTE)
                        if (!(TaskCommon.IS_ENERGY_TIME || hour == 7 && minute < 30)) {
                            Log.record(TAG, "当前不在只收能量时间段，退出循环")
                            break
                        }
                        // 收取自己能量（协程中执行）
                        Log.record(TAG, "🌳 开始收取自己的能量...")
                        val selfHomeObj = task.querySelfHome()
                        if (selfHomeObj != null) {
                            collectEnergy(UserMap.currentUid, selfHomeObj, "self")
                            Log.record(TAG, "✅ 收取自己的能量完成")
                        } else {
                            Log.error(TAG, "❌ 获取自己主页信息失败，跳过收取自己的能量")
                        }
                        // 只收能量时间段，启用循环查找能量功能
                        Log.record(TAG, "👥 开始执行查找能量...")
                        try {
                            quickcollectEnergyByTakeLook() // 查找能量（协程）
                        } catch (e: CancellationException) {
                            Log.record(TAG, "查找能量被取消，退出循环")
                            break
                        }
                        // 循环间隔（使用协程延迟）
                        val sleepMillis = task.cycleinterval!!.value.toLong()
                        Log.record(TAG, "✨ 只收能量时间一轮完成，等待 $sleepMillis 毫秒后开始下一轮")
                        GlobalThreadPools.sleepCompat(sleepMillis)
                    }
                } catch (e: CancellationException) {
                    Log.record(TAG, "只收能量循环被取消")
                }
            }
        } finally {
            Log.record(TAG, "🏁 只收能量时间循环结束")
            isEnergyLoopRunning.set(false)
        }
    }

    /**
     * 收取回赠能量，好友浇水金秋，好友复活能量
     *
     * @param wateringBubbles 包含不同类型金球的对象数组
     */
    internal suspend fun collectWateringBubbles(wateringBubbles: JSONArray) {
        for (i in 0..<wateringBubbles.length()) {
            try {
                val wateringBubble = wateringBubbles.getJSONObject(i)
                when (val bizType = wateringBubble.getString("bizType")) {
                    "jiaoshui" -> collectWater(wateringBubble)
                    "fuhuo" -> collectRebornEnergy()
                    "baohuhuizeng" -> collectReturnEnergy(wateringBubble)
                    else -> {
                        Log.record(TAG, "未知bizType: $bizType")
                        continue
                    }
                }
                GlobalThreadPools.sleepCompat(500L)
            } catch (e: JSONException) {
                Log.record(TAG, "浇水金球JSON解析错误: " + e.message)
            } catch (e: RuntimeException) {
                Log.record(TAG, "浇水金球处理异常: " + e.message)
            }
        }
    }

    private suspend fun collectWater(wateringBubble: JSONObject) {
        try {
            val id = wateringBubble.getLong("id")
            val response = AntForestRpcCall.collectEnergy("jiaoshui", task.selfId, id)
            processCollectResult(response, "收取金球🍯浇水")
        } catch (e: JSONException) {
            Log.record(TAG, "收取浇水JSON解析错误: " + e.message)
        }
    }

    private suspend fun collectRebornEnergy() {
        try {
            val response = AntForestRpcCall.collectRebornEnergy()
            processCollectResult(response, "收取金球🍯复活")
        } catch (e: RuntimeException) {
            Log.record(TAG, "收取金球运行时异常: " + e.message)
        }
    }

    private suspend fun collectReturnEnergy(wateringBubble: JSONObject) {
        try {
            val friendId = wateringBubble.getString("userId")
            val id = wateringBubble.getLong("id")
            val response = AntForestRpcCall.collectEnergy("baohuhuizeng", task.selfId, id)
            processCollectResult(
                response,
                "收取金球🍯[" + UserMap.getMaskName(friendId) + "]复活回赠"
            )
        } catch (e: JSONException) {
            Log.record(TAG, "收取金球回赠JSON解析错误: " + e.message)
        }
    }

    /**
     * 处理金球-浇水、收取结果
     *
     * @param response       收取结果
     * @param successMessage 成功提示信息
     */
    private fun processCollectResult(response: String, successMessage: String?) {
        try {
            val joEnergy = JSONObject(response)
            if (ResChecker.checkRes(TAG + "收集能量失败:", joEnergy)) {
                val bubbles = joEnergy.getJSONArray("bubbles")
                if (bubbles.length() > 0) {
                    val collected = bubbles.getJSONObject(0).getInt("collectedEnergy")
                    if (collected > 0) {
                        val msg = successMessage + "[" + collected + "g]"
                        Log.forest(msg)
                        Toast.show(msg)
                    } else {
                        Log.record(successMessage + "失败")
                    }
                } else {
                    Log.record(successMessage + "失败: 未找到金球信息")
                }
            } else {
                Log.record(successMessage + "失败:" + joEnergy.getString("resultDesc"))
                Log.record(response)
            }
        } catch (e: JSONException) {
            Log.record(TAG, "JSON解析错误: " + e.message)
        } catch (e: Exception) {
            Log.record(TAG, "处理收能量结果错误: " + e.message)
        }
    }

    /**
     * 收取用户能量
     *
     * @param userId      用户ID
     * @param userHomeObj 用户主页的JSON对象，包含用户的蚂蚁森林信息
     * @return 更新后的用户主页JSON对象，如果发生异常返回null
     */
    internal fun collectEnergy(
        userId: String?,
        userHomeObj: JSONObject?,
        fromTag: String?
    ): JSONObject? {
        try {
            if (userHomeObj == null) {
                return null
            }
            // 1. 检查接口返回是否成功
            if (!ResChecker.checkRes(TAG + "载入用户主页失败:", userHomeObj)) {
                Log.record(TAG, "载入失败: " + userHomeObj.optString("resultDesc", "未知错误"))
                return userHomeObj
            }
            val serverTime = userHomeObj.optLong("now", System.currentTimeMillis())
            val isSelf = userId == UserMap.currentUid

            // 2. 自己的能量不受缓存限制，好友的能量检查缓存避免重复处理
            if (!isSelf && !userId.isNullOrEmpty() && processedUsersCache.contains(userId)) {
                return userHomeObj
            }

            // 标记用户为已处理（无论是否成功收取能量）
            if (!isSelf && !userId.isNullOrEmpty()) {
                processedUsersCache.add(userId)
            }
            val userName = getAndCacheUserName(userId, userHomeObj, fromTag)

            // 3. 判断是否允许收取能量 (开关关闭 或 在黑名单中)
            if (!task.collectEnergy!!.value || jsonCollectMap.contains(userId)) {
                Log.record(TAG, "[$userName] 不允许收取能量，跳过")
                return userHomeObj
            }

            // 4. 获取所有可收集的能量球 (extractBubbleInfo 内部已包含"收自己阈值"的逻辑)
            val availableBubbles: MutableList<Long> = ArrayList()
            extractBubbleInfo(userHomeObj, serverTime, availableBubbles, userId)

            if (availableBubbles.isEmpty()) {
                // 记录空森林的时间戳，避免本轮重复检查
                if (!userId.isNullOrEmpty()) {
                    emptyForestCache[userId] = System.currentTimeMillis()
                }
                return userHomeObj
            }

            // 5. 检查是否有能量罩或炸弹卡保护
            var hasProtection = false
            if (!isSelf) {
                // 检查保护罩
                if (hasShield(userHomeObj, serverTime)) {
                    hasProtection = true
                    Log.record(TAG, "[$userName]被能量罩❤️保护着哟，跳过收取")
                }

                // 🆕【核心修改】检查炸弹卡 及 阈值判断逻辑
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
                                    Log.record(TAG, "[$userName] 发现大能量球($energy g) >= 炸弹阈值($bombLimit g)，无视炸弹卡强行收取！💥")
                                    break
                                }
                            }
                        }
                    }

                    if (!bypassBomb) {
                        hasProtection = true
                        Log.record(TAG, "[$userName]开着炸弹卡💣，跳过收取")
                    }
                }
            }

            // 6. 只有没有保护(或无视保护)时才收集当前可用能量
            if (!hasProtection) {
                collectVivaEnergy(userId, userHomeObj, availableBubbles, fromTag)
            }

            return userHomeObj
        } catch (e: JSONException) {
            Log.printStackTrace(TAG, "collectUserEnergy JSON解析错误", e)
            return null
        } catch (e: NullPointerException) {
            Log.printStackTrace(TAG, "collectUserEnergy 空指针异常", e)
            return null
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "collectUserEnergy 出现异常", t)
            return null
        }
    }

    /**
     * {{ 新增辅助方法：统一判断是否满足收自己能量的阈值条件 }}
     * @param bubbleCount 能量球数值
     * @param canBeRobbedAgain 是否可被再次偷取（保底状态为false）
     */
    private fun shouldCollectSelfBubble(bubbleCount: Int, canBeRobbedAgain: Boolean): Boolean {
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
                        Log.record(TAG, "触发保底收取：能量[$bubbleCount g] < 阈值[$threshold g]，但已无法被偷，强制收取")
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

    /**
     * 提取能量球状态
     * {{ 修改了该方法，在 AVAILABLE 和 WAITING 分支增加了阈值判断 }}
     *
     * @param userHomeObj      用户主页的JSON对象
     * @param serverTime       服务器时间
     * @param availableBubbles 可收集的能量球ID列表
     * @param userId           用户ID
     * @throws JSONException JSON解析异常
     */
    @Throws(JSONException::class)
    private fun extractBubbleInfo(
        userHomeObj: JSONObject,
        serverTime: Long,
        availableBubbles: MutableList<Long>,
        userId: String?
    ) {
        // 1. 获取能量球数组（兼容组队模式）
        val jaBubbles = if (task.isTeam(userHomeObj)) {
            userHomeObj.optJSONObject("teamHomeResult")
                ?.optJSONObject("mainMember")
                ?.optJSONArray("bubbles")
        } else {
            userHomeObj.optJSONArray("bubbles")
        } ?: JSONArray()

        if (jaBubbles.length() == 0) return

        // 2. 获取用户名（用于日志）
        val userName = getAndCacheUserName(userId, userHomeObj, null)
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

        // 4. 遍历能量球
        for (i in 0..<jaBubbles.length()) {
            val bubble = jaBubbles.getJSONObject(i)
            val bubbleId = bubble.getLong("id")
            val statusStr = bubble.getString("collectStatus")
            val status = CollectStatus.valueOf(statusStr)
            val bubbleCount = bubble.getInt("fullEnergy")

            when (status) {
                CollectStatus.AVAILABLE -> {
                    // 🆕【修改点1】：可收取状态，统一调用阈值判断
                    if (isSelf) {
                        // 获取是否还能被偷取的标记 (保底状态下该值为 false)
                        val canBeRobbedAgain = bubble.optBoolean("canBeRobbedAgain", false)

                        if (shouldCollectSelfBubble(bubbleCount, canBeRobbedAgain)) {
                            availableBubbles.add(bubbleId)
                        }
                    } else {
                        // 好友的能量直接添加，不进行阈值判断
                        availableBubbles.add(bubbleId)
                    }
                }

                CollectStatus.WAITING -> {
                    if (bubbleCount <= 0) {
                        Log.record(TAG, "跳过数量为[$bubbleId]的等待能量球的蹲点任务")
                        continue
                    }

                    // 🆕【修改点2】：蹲点任务也必须严格遵循收自己能量的阈值配置
                    if (isSelf) {
                        // 对于等待中的球，我们暂时假设它是可被偷的(canBeRobbedAgain=true)以进行严格检查
                        // 逻辑：如果只收>20g，现在有个5g的在等待，应该跳过，不加入蹲点队列
                        // 如果有明确的canBeRobbedAgain字段则使用，否则默认为true
                        val canBeRobbed = bubble.optBoolean("canBeRobbedAgain", true)
                        if (!shouldCollectSelfBubble(bubbleCount, canBeRobbed)) {
                            // 可选：Log.record(TAG, "跳过等待能量[$bubbleCount g] (不满足阈值配置)")
                            continue
                        }
                    }

                    // 等待成熟的能量球，添加到蹲点队列
                    val produceTime = bubble.optLong("produceTime", 0L)
                    if (produceTime > 0 && produceTime > serverTime) {
                        // 检查保护罩时间（仅好友）：如果保护罩覆盖整个成熟期，跳过蹲点
                        // 自己的账号：无论是否有保护罩都要添加蹲点（到时间后直接收取）
                        if (!isSelf && task.shieldManager.shouldSkipWaitingTaskDueToProtection(userHomeObj, produceTime, serverTime)) {
                            val shieldEndTime = ForestUtil.getShieldEndTime(userHomeObj)
                            val bombEndTime = ForestUtil.getBombCardEndTime(userHomeObj)
                            val protectionEndTime = maxOf(shieldEndTime, bombEndTime)
                            val remainingHours = (protectionEndTime - serverTime) / (1000 * 60 * 60)
                            Log.record(
                                TAG,
                                "⏭️ 跳过好友蹲点[$userName]球[$bubbleId]：保护罩覆盖整个成熟期(保护还剩${remainingHours}h，能量${TimeUtil.getCommonDate(produceTime)}成熟)"
                            )
                            continue
                        }

                        waitingBubblesCount++
                        // 添加蹲点任务
                        EnergyWaitingManager.addWaitingTask(
                            userId = userId ?: "",
                            userName = userName ?: "未知用户",
                            bubbleId = bubbleId,
                            produceTime = produceTime,
                            fromTag = "蹲点收取"
                        )
                        Log.record(
                            TAG,
                            "添加蹲点: [$userName] 能量球[$bubbleId] 将在[${TimeUtil.getCommonDate(produceTime)}]成熟$protectionLog"
                        )
                    }
                }

                else -> {
                    // 其他状态（INSUFFICIENT, ROBBED等）跳过
                    continue
                }
            }
        }

        // 5. 打印调试信息
        // 只有当有可收取的球，或者有等待的球时才打印，避免刷屏
        if (availableBubbles.isNotEmpty() || waitingBubblesCount > 0) {
            Log.record(TAG, "[$userName] 可收集能量球: ${availableBubbles.size}个")
            if (waitingBubblesCount > 0) {
                Log.record(TAG, "[$userName] 等待成熟能量球: ${waitingBubblesCount}个")
            }
        }
    }

    /**
     * 批量或逐一收取能量
     *
     * @param userId      用户ID
     * @param userHomeObj 用户主页的JSON对象
     * @param bubbleIds   能量球ID列表
     * @param fromTag     收取来源标识
     */
    @Throws(JSONException::class)
    /**
     * 收取活力能量
     * @param userId 用户ID
     * @param userHomeObj 用户主页对象
     * @param bubbleIds 能量球ID列表
     * @param fromTag 来源标识
     * @param skipPropCheck 是否跳过道具检查（用于蹲点收取快速通道）
     */
    internal fun collectVivaEnergy(
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
                        skipPropCheck  // 🚀 传递快速通道标记
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
                        skipPropCheck  // 🚀 传递快速通道标记
                    )
                )
            }
        }
    }

    /**
     * 函数式接口，用于提供RPC调用
     */
    private fun interface RpcSupplier<T> {
        @Throws(Exception::class)
        suspend fun get(): T?
    }

    /**
     * 函数式接口，用于对JSON对象进行断言
     */
    private fun interface JsonPredicate<T> {
        @Throws(Exception::class)
        fun test(t: T?): Boolean
    }

    /**
     * 协程版本的排行榜收取方法
     */

    private suspend fun collectRankingsCoroutine(
        rankingName: String?,
        rpcCall: RpcSupplier<String?>,
        jsonArrayKey: String?,
        flag: String,
        preCondition: JsonPredicate<JSONObject?>?
    ) = withContext(Dispatchers.Default) {
        try {
            Log.record(TAG, "开始处理$rankingName...")
            val tc = TimeCounter(TAG)
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
                        TAG,
                        "collectRankings $rankingName, response: $response",
                        e
                    )
                }
                if (i < 2) {
                    Log.record(TAG, "获取" + rankingName + "失败，" + (5 * (i + 1)) + "秒后重试")
                    GlobalThreadPools.sleepCompat(5000L * (i + 1))
                }
            }

            if (rankingObject == null) {
                Log.error(TAG, "获取" + rankingName + "失败")
                return@withContext
            }
            if (!ResChecker.checkRes(TAG + "获取" + rankingName + "失败:", rankingObject)) {
                Log.error(
                    TAG,
                    "获取" + rankingName + "失败: " + rankingObject.optString("resultDesc")
                )
                return@withContext
            }
            val totalDatas = rankingObject.optJSONArray(jsonArrayKey)
            if (totalDatas == null) {
                Log.record(TAG, rankingName + "数据为空，跳过处理。")
                return@withContext
            }
            Log.record(
                TAG,
                "成功获取" + rankingName + "数据，共发现" + totalDatas.length() + "位好友。"
            )
            tc.countDebug("获取$rankingName")
            if (preCondition != null && !preCondition.test(rankingObject)) {
                return@withContext
            }
            // 处理前20个  超过会报错
            Log.record(TAG, "开始处理" + rankingName + "前20位好友...")
            val friendRanking = rankingObject.optJSONArray("friendRanking")
            if (friendRanking != null) {
                processFriendsEnergyCoroutine(friendRanking, flag, "${rankingName}前20位")
            }
            tc.countDebug("处理" + rankingName + "靠前的好友")
            // 分批并行处理后续的（协程版本）
            if (totalDatas.length() <= 20) {
                Log.record(TAG, rankingName + "没有更多的好友需要处理，跳过")
                return@withContext
            }

            // 处理所有好友（无限制模式）
            val remainingToProcess = totalDatas.length() - 20

            if (remainingToProcess <= 0) {
                Log.record(TAG, rankingName + "已处理前20位好友，跳过后续处理")
                return@withContext
            }

            val idList: MutableList<String?> = ArrayList()
            val batchSize = 20
            val batches = (remainingToProcess + batchSize - 1) / batchSize
            Log.record(
                TAG,
                "🌟 处理所有好友：" + rankingName + "共${totalDatas.length()}位好友，需处理后续${remainingToProcess}位，共${batches}批"
            )

            // 串行处理批次，避免总并发数过高
            var batchCount = 0

            for (pos in 20..<totalDatas.length()) {
                // 检查协程是否被取消
                if (!isActive) {
                    Log.record(TAG, "协程被取消，停止处理${rankingName}批次")
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
                    Log.record(TAG, "[批次$currentBatchNum/$batches] 开始处理...")
                    try {
                        processFriendsEnergyCoroutine(batch, flag, "批次$currentBatchNum")
                        Log.record(TAG, "[批次$currentBatchNum/$batches] 处理完成")
                    } catch (e: CancellationException) {
                        Log.record(TAG, "[批次$currentBatchNum/$batches] 被取消")
                        throw e
                    }

                    idList.clear()
                }
            }

            // 处理剩余的用户
            if (idList.isNotEmpty()) {
                // 检查协程是否被取消
                if (!isActive) {
                    Log.record(TAG, "协程被取消，跳过${rankingName}剩余用户处理")
                    return@withContext
                }

                val currentBatchNum = ++batchCount
                Log.record(TAG, "[批次$currentBatchNum/$batches] 开始处理...")
                try {
                    processFriendsEnergyCoroutine(idList, flag, "批次$currentBatchNum")
                    Log.record(TAG, "[批次$currentBatchNum/$batches] 处理完成")
                } catch (e: CancellationException) {
                    Log.record(TAG, "[批次$currentBatchNum/$batches] 被取消")
                    throw e
                }
            }
            tc.countDebug("分批处理" + rankingName + "其他好友")
            Log.record(TAG, "收取" + rankingName + "能量完成！")
        } catch (e: CancellationException) {
            // 协程被取消是正常行为，不记录错误日志
            Log.record(TAG, "处理" + rankingName + "时协程被取消")
            throw e // 重新抛出，让协程系统处理
        } catch (e: Exception) {
            Log.printStackTrace(TAG, "collectRankings 异常", e)
        }
    }

    /**
     * 协程版本：收取PK好友能量
     */
    internal suspend fun collectPKEnergyCoroutine() {
        collectRankingsCoroutine(
            "PK排行榜",
            { AntForestRpcCall.queryTopEnergyChallengeRanking() },
            "totalData",
            "pk",
            JsonPredicate { pkObject: JSONObject? ->
                if (pkObject!!.getString("rankMemberStatus") != "JOIN") {
                    Log.record(TAG, "未加入PK排行榜,跳过,尝试关闭")
                    task.pkEnergy!!.value = false
                    return@JsonPredicate false
                }
                true
            }
        )
    }

    /**
     * 使用找能量功能收取好友能量（协程版本）
     * 这是一个更高效的收取方式，可以直接找到有能量的好友
     */
    /**
     * 使用找能量功能收取好友能量（协程版本 - 修正版）
     * 逻辑：服务器自动轮询，返回空 friendId 代表无更多目标
     */
    internal suspend fun collectEnergyByTakeLook() {
        // 1. 冷却检查
        val currentTime = System.currentTimeMillis()
        if (currentTime < nextTakeLookTime) {
            val remaining = (nextTakeLookTime - currentTime) / 1000
            Log.record(TAG, "找能量冷却中，等待 ${remaining / 60}分${remaining % 60}秒")
            return
        }

        val tc = TimeCounter(TAG)
        var foundCount = 0
        val maxAttempts = 10
        var consecutiveEmpty = 0
        var shouldCooldown = false

        // 本地去重集合：防止单次运行中服务器重复返回同一个有保护罩的人
        val visitedInSession = mutableSetOf<String>()
        // 空参数对象，仅为了满足接口签名（如果接口允许传null这里可以改为null）
        val emptyParam = JSONObject()

        Log.record(TAG, "开始找能量 (服务器自动轮询)")

        try {
            loop@ for (attempt in 1..maxAttempts) {
                // A. 调用接口
                val takeLookResult = try {
                    // 传空参，由服务器自动分配
                    val resStr = AntForestRpcCall.takeLook(emptyParam)
                    JSONObject(resStr)
                } catch (e: Exception) {
                    Log.printStackTrace(TAG, "找能量接口异常", e)
                    shouldCooldown = true
                    break@loop
                }

                // B. 检查接口返回是否成功
                if (!ResChecker.checkRes("$TAG 接口业务失败:", takeLookResult)) {
                    break@loop
                }

                // C. 核心判断：获取 friendId
                val friendId = takeLookResult.optString("friendId")

                // 如果 friendId 为空，说明服务器那边已经没有可以收取的对象了
                if (friendId.isNullOrBlank()) {
                    consecutiveEmpty++
                    Log.record(TAG, "第$attempt 次未发现有能量的好友")

                    // 连续2次没有返回ID，说明真的没了，直接结束
                    if (consecutiveEmpty >= 2) {
                        Log.record(TAG, "系统无可偷取目标，结束")
                        break@loop
                    }
                    // 缓冲一下重试
                    GlobalThreadPools.sleepCompat(500L)
                    continue@loop
                }

                // D. 排除自己
                if (friendId == task.selfId) {
                    Log.record(TAG, "发现自己，跳过")
                    consecutiveEmpty++ // 某种意义上也是无效结果
                    continue@loop
                }

                // E. 本地重复检查 (防止死循环刷同一个有盾的人)
                if (visitedInSession.contains(friendId)) {
                    Log.record(TAG, "本次已检查过用户($friendId)，跳过")
                    consecutiveEmpty++
                    if (consecutiveEmpty >= 3) break@loop // 如果一直重复返回已访问的人，也没必要继续了
                    continue@loop
                }

                // 标记已访问
                visitedInSession.add(friendId)

                // F. 检查全局黑名单 (如之前炸弹被记录的人)
                if (skipUsersCache.containsKey(friendId)) {
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
                    Log.record(TAG, "发现[$friendName]有$type，跳过")
                    // 记录到全局缓存，防止下次运行再次浪费时间查询
                    addToSkipUsers(friendId)
                    // 注意：这里不需要传给服务器 skipUsers，因为我们单纯不收，服务器下次轮询可能还会给，但被上面的 visitedInSession 拦截
                } else {
                    // I. 收取能量
                    collectEnergy(friendId, friendHomeObj, "takeLook")
                    foundCount++
                    consecutiveEmpty = 0 // 重置空计数

                    // 收取成功后，稍微等待，模拟人为操作并给服务器状态同步时间
                    GlobalThreadPools.sleepCompat(1200L)
                }
            }
        } catch (e: Exception) {
            Log.printStackTrace(TAG, "找能量流程异常", e)
        } finally {
            // 逻辑结束后的状态处理
            if (shouldCooldown) {
                nextTakeLookTime = System.currentTimeMillis() + TAKE_LOOK_COOLDOWN_MS
            } else {
                // 正常结束，下次可立即执行（或者根据需求设置一个小间隔）
                nextTakeLookTime = 0
            }
            val msg = "找能量结束，本次收取: $foundCount 个"
            Log.record(TAG, msg)
            tc.countDebug(msg)
        }
    }

    /**
     * 7点-7点30分快速收取能量，跳过道具判断
     */
    private suspend fun quickcollectEnergyByTakeLook() {
        // 1. 冷却检查
        val currentTime = System.currentTimeMillis()
        if (currentTime < nextTakeLookTime) {
            val remaining = (nextTakeLookTime - currentTime) / 1000
            Log.record(TAG, "找能量冷却中，等待 ${remaining / 60}分${remaining % 60}秒")
            return
        }

        val tc = TimeCounter(TAG)
        var foundCount = 0
        val maxAttempts = 10
        var consecutiveEmpty = 0
        var shouldCooldown = false

        // 本地去重集合：只防止单次运行中死循环刷同一个人，不跨运行记忆
        val visitedInSession = mutableSetOf<String>()
        val emptyParam = JSONObject()

        Log.record(TAG, "开始找能量 (无视黑名单与道具)")

        try {
            loop@ for (attempt in 1..maxAttempts) {
                // A. 调用接口
                val takeLookResult = try {
                    val resStr = AntForestRpcCall.takeLook(emptyParam)
                    JSONObject(resStr)
                } catch (e: Exception) {
                    Log.printStackTrace(TAG, "找能量接口异常", e)
                    shouldCooldown = true
                    break@loop
                }

                // B. 检查接口返回是否成功
                if (!ResChecker.checkRes("$TAG 接口业务失败:", takeLookResult)) {
                    break@loop
                }

                // C. 获取 friendId
                val friendId = takeLookResult.optString("friendId")

                // 如果 friendId 为空，说明服务器无目标推荐
                if (friendId.isNullOrBlank()) {
                    consecutiveEmpty++
                    Log.record(TAG, "第$attempt 次未发现有能量的好友")

                    if (consecutiveEmpty >= 2) {
                        Log.record(TAG, "系统无可偷取目标，结束")
                        break@loop
                    }
                    GlobalThreadPools.sleepCompat(500L)
                    continue@loop
                }

                // D. 排除自己
                if (friendId == task.selfId) {
                    Log.record(TAG, "发现自己，跳过")
                    consecutiveEmpty++
                    continue@loop
                }

                // E. 本地会话去重 (防止服务器一直返回同一个ID造成本次死循环)
                if (visitedInSession.contains(friendId)) {
                    Log.record(TAG, "本次已检查过用户($friendId)，跳过")
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
                collectEnergy(friendId, friendHomeObj, "takeLook")

                foundCount++
                consecutiveEmpty = 0 // 重置空计数

                // 模拟操作延迟
                GlobalThreadPools.sleepCompat(500L)
            }
        } catch (e: Exception) {
            Log.printStackTrace(TAG, "找能量流程异常", e)
        } finally {
            if (shouldCooldown) {
                nextTakeLookTime = System.currentTimeMillis() + TAKE_LOOK_COOLDOWN_MS
            } else {
                nextTakeLookTime = 0
            }
            val msg = "找能量结束，本次尝试收取: $foundCount 个"
            Log.record(TAG, msg)
            tc.countDebug(msg)
        }
    }

    /**
     * 将用户添加到跳过列表（内存缓存）
     *
     * @param userId 用户ID
     */
    private fun addToSkipUsers(userId: String?) {
        try {
            if (!userId.isNullOrEmpty()) {
                skipUsersCache[userId] = "baohuzhao"
            }
        } catch (e: Exception) {
            Log.printStackTrace(TAG, "添加跳过用户失败", e)
        }
    }

    /**
     * 协程版本：收取好友能量
     */
    internal suspend fun collectFriendEnergyCoroutine() {
        collectRankingsCoroutine(
            "好友排行榜",
            { AntForestRpcCall.queryFriendsEnergyRanking() },
            "totalDatas",
            "普通好友",
            null
        )
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
                    Log.error(TAG, "不支持的好友数据源类型: ${friendSource.javaClass.simpleName}")
                    return@withContext
                }
            }

            if (friendList == null) {
                Log.record(TAG, "${sourceName}数据为空，跳过处理")
                return@withContext
            }

            if (friendList.length() == 0) {
                Log.record(TAG, "${sourceName}列表为空，跳过处理")
                return@withContext
            }

            // 先收集并显示所有好友名单
            val friendNames = mutableListOf<String>()
            for (i in 0..<friendList.length()) {
                val friendObj = friendList.getJSONObject(i)
                val userId = friendObj.optString("userId", "")
                val displayName = friendObj.optString("displayName", UserMap.getMaskName(userId))
                friendNames.add(displayName)
            }

            Log.record(TAG, "📋 开始处理${friendList.length()}个${sourceName}（并发数:60）")
            Log.record(TAG, "👥 ${friendNames.joinToString(" | ")}")
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
                        Log.printStackTrace(TAG, "处理好友异常", e)
                    } finally {
                        concurrencyLimiter.release()
                    }
                }
                friendJobs.add(job)
            }

            // 等待所有好友处理完成
            friendJobs.awaitAll()
            val elapsed = System.currentTimeMillis() - startTime
            Log.record(TAG, "✅ ${sourceName}处理完成，耗时${elapsed}ms，平均${elapsed / friendList.length()}ms/人")

        } catch (e: CancellationException) {
            // 协程被取消是正常行为，不记录错误日志
            Log.record(TAG, "处理${sourceName}时协程被取消")
            throw e // 重新抛出，让协程系统处理
        } catch (e: JSONException) {
            Log.printStackTrace(TAG, "解析${sourceName}数据失败", e)
        } catch (e: Exception) {
            Log.printStackTrace(TAG, "处理${sourceName}出错", e)
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
        var userName = obj.optString("displayName", UserMap.getMaskName(userId))
        if (emptyForestCache.containsKey(userId)) { //本轮已知为空的树林
            return
        }

        val isPk = "pk" == flag
        if (isPk) {
            userName = "PK榜好友|$userName"
        }
        //  Log.record(TAG, "  processEnergy 开始处理用户: [" + userName + "], 类型: " + (isPk ? "PK" : "普通"));
        if (isPk) {
            val needCollectEnergy = task.collectEnergy!!.value && task.pkEnergy!!.value
            if (!needCollectEnergy) {
                Log.record(TAG, "    PK好友: [$userName$userId], 不满足收取条件，跳过")
                return
            }
            Log.record(TAG, "  正在查询PK好友 [$userName$userId] 的主页...")
            collectEnergy(userId, task.queryFriendHome(userId, "PKContest"), "pk")
        } else { // 普通好友
            val needCollectEnergy =
                task.collectEnergy!!.value && !jsonCollectMap.contains(userId)
            val needHelpProtect = task.helpFriendCollectType!!.value != AntForest.HelpFriendCollectType.NONE && obj.optBoolean("canProtectBubble") && Status.canProtectBubbleToday(task.selfId)
            val needCollectGiftBox = task.collectGiftBox!!.value && obj.optBoolean("canCollectGiftBox")
            if (!needCollectEnergy && !needHelpProtect && !needCollectGiftBox) {
                //   Log.record(TAG, "    普通好友: [$userName$userId], 所有条件不满足，跳过")
                return
            }
            var userHomeObj: JSONObject? = null
            // 只要开启了收能量，就进去看看，以便添加蹲点
            if (needCollectEnergy) {
                // 即使排行榜信息显示没有可收能量，也进去检查，以便添加蹲点任务
                Log.record(TAG, "  正在查询好友 [$userName$userId] 的主页...")
                userHomeObj = collectEnergy(userId, task.queryFriendHome(userId, null), "friend")
            }
            if (needHelpProtect) {
                val isProtected = task.shieldManager.isIsProtected(userId)
                /** lzw add end */
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

    /** lzw add end */
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
                            if (!ResChecker.checkRes(TAG + "领取好友礼盒失败:", giftBoxResult)) {
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

    internal fun collectEnergy(collectEnergyEntity: CollectEnergyEntity) {
        if (AntForest.errorWait) {
            Log.record(TAG, "异常⌛等待中...不收取能量")
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
                            Log.record(TAG, "触发异常,等待至" + TimeUtil.getCommonDate(waitTime))
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
                        Log.record(TAG, "[" + getAndCacheUserName(userId) + "]" + "能量已被收取,取消重试 错误:" + jo.getString("resultDesc"))
                        return@Runnable
                    }

                    // 检测并记录"手速太快"错误
                    if (ForestUtil.checkAndRecordFrequencyError(userId, jo)) {
                        return@Runnable
                    }

                    Log.record(TAG, "[" + getAndCacheUserName(userId) + "]" + jo.optString("resultDesc", ""))
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
                            collectType + randomEmoji + collected + "g[" + getAndCacheUserName(
                                userId
                            ) + "]#"
                        ForestStatistics.totalCollected += collected
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
                            collectType + randomEmoji + collected + "g[" + getAndCacheUserName(
                                userId
                            ) + "]"
                        ForestStatistics.totalCollected += collected
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
                Log.printStackTrace(TAG, "collectEnergy err", e)
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

    /**
     * 收取状态的枚举类型
     */
    enum class CollectStatus {
        AVAILABLE, WAITING, INSUFFICIENT, ROBBED
    }

    /**
     * 立即收取自己能量（专用方法）
     */
    internal suspend fun collectSelfEnergyImmediately(tag: String = "立即收取") {
        try {
            // querySelfHome 内部会处理 updateSelfHomePage 逻辑，确保道具倒计时等状态同步
            val selfHomeObj = task.querySelfHome()
            if (selfHomeObj != null) {
                Log.record(TAG, "🎯 $tag：开始收取自己能量...")
                val availableBubbles: MutableList<Long> = ArrayList()
                val serverTime = selfHomeObj.optLong("now", System.currentTimeMillis())

                // ✅ 核心逻辑点：
                // 调用 extractBubbleInfo，该方法内部调用了 shouldCollectSelfBubble(bubbleCount, canBeRobbedAgain)
                // 从而严格执行了【收自己单个能量球方式】和【阈值】的判断逻辑。
                // 只有符合条件的 bubbleId 才会加入 availableBubbles
                extractBubbleInfo(selfHomeObj, serverTime, availableBubbles, UserMap.currentUid)

                if (availableBubbles.isNotEmpty()) {
                    Log.record(TAG, "🎯 $tag：找到${availableBubbles.size}个符合阈值条件的可收能量球")
                    // 即使 batchRobEnergy 为 true，collectVivaEnergy 也是对传入的 list 进行操作
                    // 因此【一键收取】、【找能量】、【普通收取】都复用了这个逻辑，保证了统一性
                    collectVivaEnergy(UserMap.currentUid, selfHomeObj, availableBubbles, "加速卡$tag", skipPropCheck = true)
                } else {
                    Log.record(TAG, "🎯 $tag：未找到满足条件的能量球 (可能是被阈值过滤或无能量)")
                }
            }
        } catch (e: Exception) {
            Log.printStackTrace(TAG, "collectSelfEnergyImmediately err", e)
        }
    }

    /**
     * 统一获取和缓存用户名的方法
     * @param userId 用户ID
     * @param userHomeObj 用户主页对象（可选）
     * @param fromTag 来源标记（可选）
     * @return 用户名
     */
    private fun getAndCacheUserName(userId: String?, userHomeObj: JSONObject?, fromTag: String?): String? {
        // 输入验证：userId为空时直接返回
        if (userId.isNullOrEmpty()) {
            return null
        }

        // 1. 尝试从缓存获取
        val cachedUserName = userNameCache.get(userId)
        if (!cachedUserName.isNullOrEmpty() && cachedUserName != userId) {
            // 如果缓存的不是userId本身，且不为空，则返回缓存值
            return cachedUserName
        }

        // 2. 根据上下文解析用户名
        var userName = resolveUserNameFromContext(userId, userHomeObj, fromTag)

        // 3. Fallback处理：如果解析失败，使用userId作为显示名
        if (userName.isNullOrEmpty()) {
            userName = userId
        }

        // 4. 存入缓存（只缓存有效的用户名）
        if (userName.isNotEmpty()) {
            userNameCache[userId] = userName
        }

        return userName
    }

    /**
     * 统一获取用户名的简化方法（无上下文）
     */
    private fun getAndCacheUserName(userId: String?): String? {
        return getAndCacheUserName(userId, null, null)
    }

    /**
     * 从上下文中解析用户名
     */
    private fun resolveUserNameFromContext(
        userId: String?,
        userHomeObj: JSONObject?,
        fromTag: String?
    ): String? {
        var userName: String? = null

        if ("pk" == fromTag && userHomeObj != null) {
            val userEnergy = userHomeObj.optJSONObject("userEnergy")
            if (userEnergy != null) {
                userName = "PK榜好友|" + userEnergy.optString("displayName")
            }
        } else {
            userName = UserMap.getMaskName(userId)
            if ((userName == null || userName == userId) && userHomeObj != null) {
                val userEnergy = userHomeObj.optJSONObject("userEnergy")
                if (userEnergy != null) {
                    val displayName = userEnergy.optString("displayName")
                    if (!displayName.isEmpty()) {
                        userName = displayName
                    }
                }
            }
        }
        return userName
    }

    /**
     * 专门用于蹲点的能量收取方法
     */
    @SuppressLint("SimpleDateFormat")
    private fun collectEnergyForWaiting(
        userId: String,
        userHomeObj: JSONObject,
        fromTag: String?,
        userName: String?
    ): CollectResult {
        try {
            Log.record(TAG, "蹲点收取开始：用户[${userName}] userId[${userId}] fromTag[${fromTag}]")
            // 获取服务器时间
            val serverTime = userHomeObj.optLong("now", System.currentTimeMillis())
            // 判断是否是自己的账号
            val isSelf = userId == UserMap.currentUid

            // 先检查保护罩和炸弹（仅对好友检查）
            val shieldEndTime = ForestUtil.getShieldEndTime(userHomeObj)
            val bombEndTime = ForestUtil.getBombCardEndTime(userHomeObj)
            val hasShield = shieldEndTime > serverTime
            val hasBomb = bombEndTime > serverTime
            val hasProtection = hasShield || hasBomb

            Log.record(TAG, "蹲点收取保护检查详情：")
            Log.record(TAG, "  是否是主号: $isSelf")
            Log.record(
                TAG, "  服务器时间: $serverTime (${
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(
                        Date(serverTime)
                    )
                })"
            )
            Log.record(
                TAG, "  保护罩结束时间: $shieldEndTime (${
                    if (shieldEndTime > 0) SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(
                        Date(shieldEndTime)
                    ) else "无保护罩"
                })"
            )
            Log.record(
                TAG, "  炸弹卡结束时间: $bombEndTime (${
                    if (bombEndTime > 0) SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(
                        Date(bombEndTime)
                    ) else "无炸弹卡"
                })"
            )
            Log.record(TAG, "  是否有保护罩: $hasShield")
            Log.record(TAG, "  是否有炸弹卡: $hasBomb")
            Log.record(TAG, "  总体保护状态: $hasProtection")

            // 只对好友账号进行保护检查，主号无视保护罩
            if (!isSelf && hasProtection) {
                // 调用原有的日志输出方法
                task.shieldManager.checkUserShieldAndBomb(userHomeObj, userName, userId, serverTime)
                return CollectResult(
                    success = false,
                    userName = userName,
                    message = "有保护，已跳过",
                    hasShield = hasShield,
                    hasBomb = hasBomb
                )
            }

            // 主号的保护罩不影响收取自己的能量
            if (isSelf && hasProtection) {
                Log.record(TAG, "  ⭐ 主号有保护罩，但可以收取自己的能量")
            }

            // 先查询用户能量状态
            val queryResult = collectEnergy(userId, userHomeObj, fromTag) ?: return CollectResult(
                success = false,
                userName = userName,
                message = "无法查询用户能量信息"
            )

            // 提取可收取的能量球ID
            val availableBubbles: MutableList<Long> = ArrayList()
            val queryServerTime = queryResult.optLong("now", System.currentTimeMillis())
            extractBubbleInfo(queryResult, queryServerTime, availableBubbles, userId)

            if (availableBubbles.isEmpty()) {
                return CollectResult(
                    success = false,
                    userName = userName,
                    message = "用户无可收取的能量球"
                )
            }

            Log.record(TAG, "蹲点收取找到${availableBubbles.size}个可收取能量球: $availableBubbles")

            // 记录收取前的总能量
            val beforeTotal = ForestStatistics.totalCollected

            // 🚀 启用快速收取通道：跳过道具检查，加速蹲点收取
            collectVivaEnergy(userId, queryResult, availableBubbles, fromTag, skipPropCheck = true)

            // 计算收取的能量数量
            val collectedEnergy = ForestStatistics.totalCollected - beforeTotal

            return if (collectedEnergy > 0) {
                CollectResult(
                    success = true,
                    userName = userName,
                    energyCount = collectedEnergy,
                    totalCollected = ForestStatistics.totalCollected,
                    message = "收取成功，共收取${availableBubbles.size}个能量球，${collectedEnergy}g能量"
                )
            } else {
                CollectResult(
                    success = false,
                    userName = userName,
                    message = "未收取到任何能量"
                )
            }
        } catch (e: Exception) {
            Log.printStackTrace(TAG, "collectEnergyForWaiting err", e)
            return CollectResult(
                success = false,
                userName = userName,
                message = "收取异常：${e.message}"
            )
        }
    }

    /**
     * 为蹲点管理器提供能量收取功能（增强版）
     */
    internal suspend fun collectUserEnergyForWaiting(waitingTask: EnergyWaitingManager.WaitingTask): CollectResult {
        return try {
            withContext(Dispatchers.Default) {
                // 查询好友主页
                val friendHomeObj = task.queryFriendHome(waitingTask.userId, waitingTask.fromTag)
                if (friendHomeObj != null) {
                    // 获取真实用户名
                    val realUserName = getAndCacheUserName(waitingTask.userId, friendHomeObj, waitingTask.fromTag)
                    val isSelf = waitingTask.userId == UserMap.currentUid
                    Log.record(TAG, "蹲点收取：用户[${realUserName}] userId=${waitingTask.userId} currentUid=${UserMap.currentUid} isSelf=${isSelf}")
                    // 直接执行能量收取，让原有的collectEnergy方法处理保护罩和炸弹检查
                    val result = collectEnergyForWaiting(waitingTask.userId, friendHomeObj, waitingTask.fromTag, realUserName)
                    result.copy(userName = realUserName)
                } else {
                    CollectResult(
                        success = false,
                        userName = waitingTask.userName,
                        message = "无法获取用户主页信息"
                    )
                }
            }
        } catch (e: CancellationException) {
            // 协程取消是正常现象，不记录为错误
            Log.record(TAG, "collectUserEnergyForWaiting 协程被取消")
            throw e  // 必须重新抛出以保证取消机制正常工作
        } catch (e: Exception) {
            Log.printStackTrace(TAG, "collectUserEnergyForWaiting err", e)
            CollectResult(
                success = false,
                userName = waitingTask.userName,
                message = "异常：${e.message}"
            )
        }
    }

    internal fun clearRoundCaches() {
        userNameCache.clear()
        processedUsersCache.clear()
        // 清空本轮的空森林缓存，以便下一轮（如下次"执行间隔"到达）重新检查所有好友
        emptyForestCache.clear()
        // 清空跳过用户缓存，下一轮重新检测保护罩状态
        skipUsersCache.clear()
    }

    companion object {
        private const val MAX_BATCH_SIZE = 6

        // 找能量功能的冷却时间（毫秒），15分钟
        private const val TAKE_LOOK_COOLDOWN_MS = 15 * 60 * 1000L

        /**
         * 下次可以执行找能量的时间戳
         * 使用 @Volatile 确保多线程环境下的可见性
         */
        @Volatile
        private var nextTakeLookTime: Long = 0
    }
}
