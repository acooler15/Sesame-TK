package fansirsqi.xposed.sesame.task.antForest.waiting

import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.task.antForest.EnergyWaitingManager.WaitingTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

/**
 * 蹲点协程调度器（V2 单好友调度者 + 绝对时间等待）
 *
 * 核心语义：
 * - 每个 [userId] 只有一个长期 FriendWorker（经 [WaitingJobRegistry] 登记，幂等启动），同一好友的所有球由它统一调度；
 * - 不再按"成熟时间差 ≤ BATCH_WINDOW_MS"合并组、不再取组内最晚时间（避免主动延迟较早球）；
 * - 所有等待基于绝对目标时间，每次唤醒后重新计算 `targetAt - now`（Doze 晚醒/验证 RPC 耗时自动扣除）；
 * - 重试表示为任务的 `retryNotBefore/retryCount`，由 FriendWorker 统一等待，不创建独立 Retry Job；
 * - 到点后由执行器捕获该好友已熟任务快照，一次性收取（请求级、每球结果）。
 *
 * V3：等待改经注入的 [WaitingDelayController]（可信号打断、可虚拟时间测试），
 * 时钟统一走 [WaitingClock]，不再直接 System.currentTimeMillis() / withTimeoutOrNull。
 */
internal class WaitingScheduler(
    private val scope: CoroutineScope,
    private val jobs: WaitingJobRegistry,
    private val repository: WaitingTaskRepository,
    private val timingCalculator: WaitingTimingCalculator,
    private val retryPolicy: WaitingRetryPolicy,
    private val clock: WaitingClock,
    private val delayController: WaitingDelayController,
    @Suppress("unused") private val rpcGateway: WaitingRpcGateway,
    /** 到点执行回调：由 session 装配为 WaitingExecutor::execute(userId)，内部捕获该好友已熟任务请求 */
    private val execution: suspend (String) -> Unit,
) {

    // 好友任务变化信号：新任务/更新/移除后发送，唤醒 worker 重新选目标。
    // channel 清理由 worker loop 退出的 finally 块处理（jobs.launchUnique 幂等，无需 friendWorkers map）。
    private val userSignals = ConcurrentHashMap<String, Channel<Unit>>()

    /** 待调度任务：目标时间已包含 retryNotBefore 门禁。 */
    data class ScheduledTask(
        val task: WaitingTask,
        val targetAt: Long,
    )

    /** 启动/唤醒该好友的蹲点调度（幂等：已存在 worker 则只发信号）。 */
    fun start(task: WaitingTask) {
        ensureUserWorker(task.userId)
    }

    /** 好友任务集合发生变化时发送 conflated 信号（锁外调用）。 */
    fun signalUserChanged(userId: String) {
        userSignals[userId]?.trySend(Unit)
    }

    /**
     * 统一登记好友 worker（V2 §3.2.2 对齐命名）：同一 userId 只启动一个长期 worker。
     * 竞态说明：jobs.launchUnique 用 computeIfAbsent 原子操作，重复调用只唤醒不创建第二个 worker。
     */
    fun ensureUserWorker(userId: String) {
        val signal = userSignals.computeIfAbsent(userId) { Channel(Channel.CONFLATED) }
        jobs.launchUnique(JobKey.FriendWorker(userId)) {
            try {
                friendWorkerLoop(userId, signal)
            } finally {
                // worker 退出（channel 关闭或 rootJob 取消）时清理 signal channel
                userSignals.remove(userId)?.close()
            }
        }
        signal.trySend(Unit)   // 幂等：已存在则只唤醒，不创建第二个 worker
    }

    /**
     * 好友 worker 主循环：始终以该好友最早待执行任务为目标；
     * 空队列时等待信号（不退出），避免"新任务已入库但旧 worker 未完成"导致无人调度。
     */
    private suspend fun friendWorkerLoop(userId: String, signal: Channel<Unit>) {
        while (currentCoroutineContext().isActive) {
            val scheduled = repository.tasksOf(userId)
                .map { task ->
                    ScheduledTask(
                        task = task,
                        targetAt = max(timingCalculator.calculatePreciseCollectTime(task), task.retryNotBefore),
                    )
                }
                .minByOrNull { it.targetAt }

            if (scheduled == null) {
                // 空队列竞态窗口：等待信号而非退出
                if (signal.receiveCatching().getOrNull() == null) {
                    return // channel 关闭（session 关闭）→ worker 退出
                }
                continue
            }

            if (!awaitTargetOrSignal(scheduled, signal)) {
                continue // 被信号打断 → 重新选目标
            }

            // 到点：执行器捕获该好友已熟任务请求并一次性收取（请求级、每球结果）
            WaitingMetrics.lastScheduleLatenessMs.set(max(0L, clock.nowMillis() - scheduled.targetAt))
            execution(userId)
        }
    }

    /**
     * 绝对时间等待：每次唤醒都重新计算 `targetAt - now`。
     * 经 [WaitingDelayController.delayOrSignal] 一次性等待剩余时长：
     * 收到信号返回 false（需重新选目标）；等待完整超时返回 true（已到点，进入执行）。
     * @return true 表示已到点；false 表示收到任务变化信号或任务被移除，需重新选目标。
     */
    private suspend fun awaitTargetOrSignal(scheduled: ScheduledTask, signal: Channel<Unit>): Boolean {
        val task = scheduled.task
        val targetAt = scheduled.targetAt
        while (currentCoroutineContext().isActive) {
            if (!repository.contains(task.taskId)) return false

            val remaining = targetAt - clock.nowMillis()
            if (remaining <= 0L) return true

            val signaled = delayController.delayOrSignal(remaining, signal)
            if (signaled) return false
            return true   // 等待完整超时 → 已到点
        }
        return false
    }

    /**
     * 统一重试入口（业务失败与协程异常共用）。
     * 决策后把带 retryNotBefore 的任务写回仓库并唤醒好友 worker；不再创建独立延迟 Job。
     */
    fun scheduleRetry(task: WaitingTask, errorType: WaitingError, timeToTarget: Long? = null): Boolean {
        val decision = retryPolicy.decide(task, errorType, timeToTarget)
        return when (decision) {
            is RetryDecision.GiveUp -> {
                Log.record(WaitingLogger.TAG, "  → 不满足重试条件，移除任务")
                repository.removeAndPersist(task.taskId)
                signalUserChanged(task.userId)
                false
            }
            is RetryDecision.Retry -> {
                val retryTask = decision.nextRetryTask
                Log.record(
                    WaitingLogger.TAG,
                    "  → ${decision.delayMillis / 1000}秒后重试(${retryTask.retryCount}/${task.maxRetries})"
                )
                // ConcurrentHashMap 原子 put；含 retryNotBefore 的状态由 policy 写入
                if (repository.contains(retryTask.taskId)) {
                    repository.put(retryTask.taskId, retryTask)
                }
                signalUserChanged(task.userId)
                start(retryTask)
                true
            }
        }
    }

    /** 当前好友 worker 数（指标用，V2 §3.5.2；统计 jobs 中活跃的 FriendWorker）。 */
    fun friendWorkerCount(): Int = jobs.countActiveByType { it is JobKey.FriendWorker }
}
