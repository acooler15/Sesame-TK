package fansirsqi.xposed.sesame.task.antForest.waiting

import fansirsqi.xposed.sesame.task.antForest.EnergyWaitingManager.WaitingTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 能量收取回调接口（蹲点专用，批量、请求级，V2 §3.3.5）
 */
fun interface EnergyCollectCallback {
    /**
     * 收取指定好友本次请求中已成熟的所有球（一次 RPC 组，按 bubbleId 返回每球结果）
     * @param request 批量收取请求（token 归属 + 期望收取的球及其版本戳）
     * @return 批量收取结果（每球 outcome；请求级失败为 RequestFailed）
     */
    suspend fun collectUserEnergyForWaiting(request: WaitingCollectRequest): WaitingBatchResult
}

/**
 * 蹲点等待错误类型
 * 统一错误分类，替代 message.contains(...) 字符串匹配
 */
enum class WaitingError {
    NETWORK,         // 网络错误（可重试）
    FREQUENCY,       // 频繁请求（可重试）
    SHIELD_OR_BOMB,  // 有保护罩/炸弹卡（不可重试）
    NO_BUBBLE,       // 能量球不存在（不可重试）
    EMPTY_COLLECT,   // 提取到球但收取 0 能量（可重试）
    QUERY_FAILED,    // 无法查询（不可重试）
    UNKNOWN;
}

/**
 * 重试决策结果
 */
sealed class RetryDecision {
    /** 放弃重试（调用方应移除任务） */
    data object GiveUp : RetryDecision()

    /** 延迟后重试 */
    data class Retry(val delayMillis: Long, val nextRetryTask: WaitingTask) : RetryDecision()
}

/**
 * 统一重试策略（替代 SmartRetryStrategy 双方法）
 * 单一决策入口：根据错误类型与重试次数决定重试或放弃
 * 通过构造注入 [clock] / [jitter]，替代硬编码的 System.currentTimeMillis() / Random.nextLong，便于虚拟时间测试。
 */
class WaitingRetryPolicy(
    private val clock: WaitingClock,
    private val jitter: WaitingJitter,
) {
    private companion object {
        const val RETRY_MIN_TIME_TO_TARGET_MS = 10_000L  // 剩余 <10s 不重试（仅异常路径传入）
        const val RETRY_DELAY_NETWORK_MS = 5_000L       // 网络错误 5s
        const val RETRY_DELAY_FREQUENCY_MS = 10_000L   // 频繁错误 10s
        const val RETRY_DELAY_DEFAULT_MS = 5_000L      // 默认 5s
        const val RETRY_JITTER_MS = 2_000L             // 随机抖动 ±2s
    }

    /**
     * 决策是否重试
     * @param task 当前蹲点任务
     * @param error 错误类型
     * @param timeToTarget 距目标时间的剩余毫秒（仅协程异常路径传入，用于提前失败时放弃；业务失败路径传 null）
     * @return [RetryDecision.GiveUp] 放弃（调用方移除任务）；[RetryDecision.Retry] 延迟后重试
     */
    fun decide(
        task: WaitingTask,
        error: WaitingError,
        timeToTarget: Long? = null
    ): RetryDecision {
        if (task.retryCount >= task.maxRetries) return RetryDecision.GiveUp
        if (timeToTarget != null && timeToTarget < RETRY_MIN_TIME_TO_TARGET_MS) return RetryDecision.GiveUp

        val baseDelay = when (error) {
            // 不可重试类型：保护/无球/查询失败 → 放弃
            WaitingError.SHIELD_OR_BOMB,
            WaitingError.NO_BUBBLE,
            WaitingError.QUERY_FAILED -> return RetryDecision.GiveUp

            WaitingError.NETWORK -> RETRY_DELAY_NETWORK_MS
            WaitingError.FREQUENCY -> RETRY_DELAY_FREQUENCY_MS
            WaitingError.EMPTY_COLLECT,
            WaitingError.UNKNOWN -> RETRY_DELAY_DEFAULT_MS
        }

        val jitterVal = jitter.nextLong(-RETRY_JITTER_MS, RETRY_JITTER_MS + 1) // [-JITTER, JITTER]
        val retryAt = clock.nowMillis() + baseDelay + jitterVal
        return RetryDecision.Retry(
            delayMillis = baseDelay + jitterVal,
            nextRetryTask = task.withRetry().copy(retryNotBefore = retryAt),
        )
    }
}

/**
 * 重试调度器：统一的延迟执行入口（合并业务失败重试与异常重试两条路径）
 */
/**
 * 持久化去抖器
 *
 * 合并短时间内的多次触发为最后一次延迟执行，降低持久化 IO 频率。
 * 批量添加/移除场景下（如主任务遍历好友产生多个 WAITING 球），
 * 连续 N 次变更最终只落盘 1 次。
 */
internal class PersistenceDebouncer(
    private val scope: CoroutineScope,
    private val delayMillis: Long,
    private val action: suspend () -> Unit,
) {
    private var job: Job? = null

    /**
     * 触发一次保存（重置计时，取消上一次待执行的保存）
     */
    fun trigger() {
        synchronized(this) {
            job?.cancel()
            job = scope.launch {
                delay(delayMillis)
                action()
            }
        }
    }

    /**
     * 取消待执行的保存（账户切换等需要立即切换落盘目标的场景）
     */
    fun cancel() {
        synchronized(this) {
            job?.cancel()
            job = null
        }
    }
}
