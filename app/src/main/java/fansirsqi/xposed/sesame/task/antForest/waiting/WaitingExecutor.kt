package fansirsqi.xposed.sesame.task.antForest.waiting

import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.task.antForest.EnergyWaitingManager.WaitingTask
import fansirsqi.xposed.sesame.task.antForest.UserEnergyPatternManager
import fansirsqi.xposed.sesame.task.antForest.waiting.WaitingLogger.logTag
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 蹲点执行器（V2 §3.3.5：请求级 + 每球结果）
 *
 * 单一职责：到点捕获精确请求快照并执行收取（execute），RPC 节流经 [RpcSlotReserver] 预约槽位，
 * 结果在锁内按 taskVersion 匹配逐球应用（applyBatchResultIfCurrent）。
 * 重试决策统一通过 [onScheduleRetry] 回调交给调度器处理，避免与调度逻辑耦合。
 *
 * V3：callback / clock / jitter / rpcGateway / slotReserver 全部构造注入（session 私有，切换时整体丢弃）。
 */
internal class WaitingExecutor(
    private val repository: WaitingTaskRepository,
    private val stateMutex: Mutex,
    private val callback: EnergyCollectCallback,
    private val clock: WaitingClock,
    @Suppress("unused") private val jitter: WaitingJitter,
    @Suppress("unused") private val rpcGateway: WaitingRpcGateway,
    private val slotReserver: RpcSlotReserver,
    /** 业务失败重试入口：由 session 装配为 WaitingScheduler::scheduleRetry */
    private val onScheduleRetry: (WaitingTask, WaitingError, Long?) -> Boolean = { _, _, _ -> false },
    /** 任务状态变化（如 StillWaiting 更新 produceTime）后唤醒好友 worker */
    private val onUserChanged: (String) -> Unit = {},
) {

    /**
     * 执行该好友的蹲点收取：锁内捕获精确请求快照，锁外执行收取 RPC，
     * 结果在锁内按 taskVersion 匹配逐球应用。
     */
    suspend fun execute(userId: String) {
        val request = stateMutex.withLock {
            repository.captureDueCollectRequest(userId, clock.nowMillis())
        }
        if (request.expected.isEmpty()) {
            Log.record(WaitingLogger.TAG, "好友[$userId]暂无到点蹲点任务，跳过执行")
            return
        }

        // 预约 RPC 槽位：短锁内推进 nextAllowedAt，锁外等待返回的时长（不持锁等待 delay）
        val primaryTaskId = request.expected.values.first().taskId
        val waitMillis = slotReserver.reserve()
        if (waitMillis > 0L) {
            Log.record(WaitingLogger.TAG, "🎲 RPC 节流：延迟${waitMillis / 1000}秒执行蹲点任务[$primaryTaskId]")
            delay(waitMillis)
        }

        // 执行收取（锁外 RPC）
        val startTime = clock.nowMillis()
        val result = collectFromWaiting(request)
        val executeTime = clock.nowMillis() - startTime

        // 结果应用（锁内，仅状态变更）
        stateMutex.withLock {
            applyBatchResultIfCurrent(request, result, executeTime)
        }
    }

    /**
     * 结果应用（V2 §3.3.5）：逐球状态转换，只有当前 taskVersion 与请求快照匹配时才允许修改。
     * 仅持锁调用，内部均为非阻塞操作（去抖保存 / 重试决策）。
     */
    private fun applyBatchResultIfCurrent(
        request: WaitingCollectRequest,
        result: WaitingBatchResult,
        executeTime: Long,
    ) {
        when (result) {
            is WaitingBatchResult.Completed -> {
                // 观测：每个好友 RPC 只更新一次用户模式，不按 taskId 重复加权
                UserEnergyPatternManager.updateUserPattern(request.userId, result, executeTime)

                var mutated = false
                result.outcomes.forEach { (bubbleId, outcome) ->
                    val stamp = request.expected[bubbleId] ?: return@forEach
                    val task = repository.get(stamp.taskId) ?: return@forEach
                    if (task.taskVersion != stamp.taskVersion) {
                        Log.record(WaitingLogger.TAG, "任务[${stamp.taskId}]已在 RPC 期间被更新，丢弃过期结果")
                        return@forEach
                    }
                    when (outcome) {
                        is BubbleOutcome.Collected -> {
                            Log.record(WaitingLogger.TAG, "✅ ${task.logTag()}球[${outcome.bubbleId}]收取成功${outcome.energy}g(耗时${executeTime}ms)")
                            repository.remove(stamp.taskId)
                            mutated = true
                        }
                        is BubbleOutcome.AlreadyCollected, is BubbleOutcome.Gone -> {
                            Log.record(WaitingLogger.TAG, "❌ ${task.logTag()}球[$bubbleId]已收取/不存在，移除任务")
                            repository.remove(stamp.taskId)
                            mutated = true
                        }
                        is BubbleOutcome.StillWaiting -> {
                            Log.record(WaitingLogger.TAG, "⏳ ${task.logTag()}球[${outcome.bubbleId}]仍在等待，更新成熟时间")
                            repository.put(
                                stamp.taskId,
                                task.copy(produceTime = outcome.produceTime, taskVersion = task.taskVersion + 1)
                            )
                            mutated = true
                        }
                        is BubbleOutcome.Empty -> {
                            Log.record(WaitingLogger.TAG, "⚠️ ${task.logTag()}球[${outcome.bubbleId}]为空，进入重试")
                            onScheduleRetry(task, WaitingError.EMPTY_COLLECT, null)
                        }
                        is BubbleOutcome.Failed -> {
                            Log.record(WaitingLogger.TAG, "❌ ${task.logTag()}球[${outcome.bubbleId}]收取失败：${outcome.failure.kind}")
                            onScheduleRetry(task, errorFromFailure(outcome.failure), null)
                        }
                        is BubbleOutcome.Protected -> {
                            if (outcome.protectionEndTime > task.produceTime) {
                                Log.record(WaitingLogger.TAG, "❌ ${task.logTag()}球[${outcome.bubbleId}]被保护覆盖，移除任务")
                                repository.remove(stamp.taskId)
                                mutated = true
                            }
                        }
                    }
                }
                if (mutated) {
                    repository.schedulePersistenceSave()
                    onUserChanged(request.userId)
                }
            }

            is WaitingBatchResult.RequestFailed -> {
                // 请求级失败：只更新本请求中版本仍匹配的任务
                request.expected.values.forEach { stamp ->
                    val task = repository.get(stamp.taskId) ?: return@forEach
                    if (task.taskVersion == stamp.taskVersion) {
                        onScheduleRetry(task, errorFromFailure(result.failure), null)
                    }
                }
            }
        }
    }

    /** 类型化失败 → 重试错误类型（网络/频率进入重试，其余交给重试策略的不可重试分支） */
    private fun errorFromFailure(failure: RpcResult.Failed): WaitingError = when (failure.kind) {
        RpcFailureKind.FREQUENCY -> WaitingError.FREQUENCY
        RpcFailureKind.OFFLINE, RpcFailureKind.NETWORK, RpcFailureKind.BRIDGE_UNAVAILABLE,
        RpcFailureKind.EMPTY_RESPONSE, RpcFailureKind.UNKNOWN -> WaitingError.NETWORK
        RpcFailureKind.ALREADY_COLLECTED -> WaitingError.NO_BUBBLE
        RpcFailureKind.AUTH, RpcFailureKind.SERVER_REJECTED, RpcFailureKind.MALFORMED_RESPONSE -> WaitingError.UNKNOWN
    }

    /**
     * 收取等待的能量（通过批量回调调用 AntForest）
     */
    private suspend fun collectFromWaiting(request: WaitingCollectRequest): WaitingBatchResult {
        return try {
            callback.collectUserEnergyForWaiting(request)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.printStackTrace(WaitingLogger.TAG, "收取能量失败", e)
            WaitingBatchResult.RequestFailed(
                RpcResult.Failed(RpcFailureKind.UNKNOWN, message = e.message)
            )
        }
    }
}

/**
 * RPC 预约槽位：替代旧 AtomicLong get→delay→set 防抖模式（V3 §3.3.2）。
 * 用 Mutex 保护短临界区，[reserve] 返回后即释放锁，不持有锁等待 delay，避免阻塞其他协程。
 *
 * 节流间隔保留旧值 500–1500ms（MIN_INTERVAL_MS / MAX_INTERVAL_MS），避免触发蚂蚁森林 FREQUENCY 频率限制。
 * 首次调用（nextAllowedAt == 0）：立即执行，返回 0，并预约下一次为 now + MIN_INTERVAL_MS；
 * 后续调用：返回需等待的毫秒数（0 表示立即执行），同时推进 nextAllowedAt 保证任意相邻两次执行至少间隔 MIN。
 */
internal class RpcSlotReserver(
    private val clock: WaitingClock,
    private val jitter: WaitingJitter,
) {
    private val mutex = Mutex()
    private var nextAllowedAt: Long = 0L   // 0L 表示首次调用，立即执行

    /**
     * 预约下一个可执行时间点，返回本次调用需要等待的毫秒数（0 表示立即执行）。
     * 关键：必须在返回 wait 的同时推进 nextAllowedAt，否则「本次延迟执行」之后的下一次调用
     * 会因 nextAllowedAt 已过期而立即执行，导致节流失效（相邻两次执行间隔为 0）。
     */
    suspend fun reserve(): Long {
        return mutex.withLock {
            val now = clock.nowMillis()
            if (nextAllowedAt == 0L) {
                // 首次立即执行：预约下一次为 now + MIN_INTERVAL，本次返回 0
                nextAllowedAt = now + MIN_INTERVAL_MS
                return@withLock 0L
            }
            val wait = (nextAllowedAt - now).coerceAtLeast(0L)
            // 本次执行发生在 now + wait；执行后预约下一次随机间隔，保证任意相邻两次执行至少间隔 MIN
            nextAllowedAt = (now + wait) + jitter.nextLong(MIN_INTERVAL_MS, MAX_INTERVAL_MS + 1)
            return@withLock wait
        }
    }

    private companion object {
        const val MIN_INTERVAL_MS = 500L   // 最小0.5秒（与旧值一致）
        const val MAX_INTERVAL_MS = 1500L  // 最大1.5秒（与旧值一致）
    }
}
