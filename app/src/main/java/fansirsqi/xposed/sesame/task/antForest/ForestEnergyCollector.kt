package fansirsqi.xposed.sesame.task.antForest

import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.core.threads.GlobalThreadPools
import fansirsqi.xposed.sesame.core.util.Average
import fansirsqi.xposed.sesame.entity.CollectEnergyEntity
import fansirsqi.xposed.sesame.hook.ApplicationHook
import fansirsqi.xposed.sesame.hook.rpc.intervallimit.IntervalLimit
import fansirsqi.xposed.sesame.task.TaskCommon
import fansirsqi.xposed.sesame.task.antForest.collector.EnergyBubbleExtractor
import fansirsqi.xposed.sesame.task.antForest.collector.EnergyCollectCore
import fansirsqi.xposed.sesame.task.antForest.collector.FriendEnergyCollector
import fansirsqi.xposed.sesame.task.antForest.collector.PKEnergyCollector
import fansirsqi.xposed.sesame.task.antForest.collector.RoundCache
import fansirsqi.xposed.sesame.task.antForest.collector.SelfEnergyCollector
import fansirsqi.xposed.sesame.task.antForest.collector.WaitingEnergyCollector
import fansirsqi.xposed.sesame.task.antForest.collector.WateringBubbleCollector
import fansirsqi.xposed.sesame.task.antForest.waiting.WaitingBatchResult
import fansirsqi.xposed.sesame.task.antForest.waiting.WaitingCollectRequest
import fansirsqi.xposed.sesame.util.maps.UserMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 蚂蚁森林能量收取器（门面）
 *
 * 门面（Facade）：组合 collector 子包各职责单一的组件，向 AntForest 暴露统一的收取 API。
 * 职责拆分见 collector 子包：
 * - [RoundCache]：用户名/已处理/空森林/跳过 缓存
 * - [EnergyBubbleExtractor]：能量球提取 + 收自己阈值判断
 * - [EnergyCollectCore]：收取核心引擎（collectEnergy / collectVivaEnergy / RPC 收取）
 * - [WateringBubbleCollector]：浇水/复活/回赠金球收取
 * - [WaitingEnergyCollector]：蹲点收取（避免二次收取）
 * - [SelfEnergyCollector]：立即收取自己能量
 * - [FriendEnergyCollector]：排行榜好友 + 找能量 + 批量好友处理
 * - [PKEnergyCollector]：PK 好友收取
 */
internal class ForestEnergyCollector(private val task: AntForest) {
    private val TAG = AntForest.TAG

    private val isEnergyLoopRunning = AtomicBoolean(false)

    // === 组件装配 ===
    private val cache = RoundCache()
    private val extractor = EnergyBubbleExtractor(task, cache)
    private val core = EnergyCollectCore(task, cache, extractor)
    private val wateringCollector = WateringBubbleCollector(task)
    private val waitingCollector = WaitingEnergyCollector(task, core, extractor, cache)
    private val selfCollector = SelfEnergyCollector(task, core, extractor)
    private val friendCollector = FriendEnergyCollector(task, core, cache)
    private val pkCollector = PKEnergyCollector(task, friendCollector)

    // === 共享状态字段转发（由 EnergyCollectCore 持有，boot() 配置） ===
    internal var tryCountInt: Int?
        get() = core.tryCountInt
        set(value) {
            core.tryCountInt = value
        }

    internal var retryIntervalInt: Int?
        get() = core.retryIntervalInt
        set(value) {
            core.retryIntervalInt = value
        }

    internal var collectIntervalEntity: IntervalLimit?
        get() = core.collectIntervalEntity
        set(value) {
            core.collectIntervalEntity = value
        }

    internal var doubleCollectIntervalEntity: IntervalLimit?
        get() = core.doubleCollectIntervalEntity
        set(value) {
            core.doubleCollectIntervalEntity = value
        }

    internal var jsonCollectMap: MutableSet<String?>
        get() = core.jsonCollectMap
        set(value) {
            core.jsonCollectMap = value
        }

    internal val delayTimeMath: Average
        get() = core.delayTimeMath

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
                            core.collectEnergy(UserMap.currentUid, selfHomeObj, "self")
                            Log.record(TAG, "✅ 收取自己的能量完成")
                        } else {
                            Log.error(TAG, "❌ 获取自己主页信息失败，跳过收取自己的能量")
                        }
                        // 只收能量时间段，启用循环查找能量功能
                        Log.record(TAG, "👥 开始执行查找能量...")
                        try {
                            friendCollector.quickcollectEnergyByTakeLook() // 查找能量（协程）
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
     */
    internal suspend fun collectWateringBubbles(wateringBubbles: JSONArray) {
        wateringCollector.collectWateringBubbles(wateringBubbles)
    }

    /**
     * 收取用户能量（经能量球提取与保护检查）
     */
    internal fun collectEnergy(
        userId: String?,
        userHomeObj: JSONObject?,
        fromTag: String?
    ): JSONObject? {
        return core.collectEnergy(userId, userHomeObj, fromTag)
    }

    /**
     * 批量或逐一收取能量
     */
    internal fun collectVivaEnergy(
        userId: String?,
        userHomeObj: JSONObject?,
        bubbleIds: MutableList<Long>,
        fromTag: String?,
        skipPropCheck: Boolean = false
    ) {
        core.collectVivaEnergy(userId, userHomeObj, bubbleIds, fromTag, skipPropCheck)
    }

    /**
     * 收取PK好友能量
     */
    internal suspend fun collectPKEnergyCoroutine() {
        pkCollector.collectPKEnergyCoroutine()
    }

    /**
     * 使用找能量功能收取好友能量
     */
    internal suspend fun collectEnergyByTakeLook() {
        friendCollector.collectEnergyByTakeLook()
    }

    /**
     * 协程版本：收取好友能量
     */
    internal suspend fun collectFriendEnergyCoroutine() {
        friendCollector.collectFriendEnergyCoroutine()
    }

    /**
     * 单个/批量 RPC 收取能量
     */
    internal fun collectEnergy(collectEnergyEntity: CollectEnergyEntity) {
        core.collectEnergy(collectEnergyEntity)
    }

    /**
     * 立即收取自己能量（专用方法）
     */
    internal suspend fun collectSelfEnergyImmediately(tag: String = "立即收取") {
        selfCollector.collectSelfEnergyImmediately(tag)
    }

    /**
     * 为蹲点管理器提供批量、请求级的能量收取（V2 §3.3.5）
     */
    internal suspend fun collectUserEnergyForWaiting(request: WaitingCollectRequest): WaitingBatchResult {
        return waitingCollector.collectUserEnergyForWaiting(request)
    }

    /**
     * 清空本轮全部缓存（每轮任务结束时调用）
     */
    internal fun clearRoundCaches() {
        cache.clearAll()
    }
}
