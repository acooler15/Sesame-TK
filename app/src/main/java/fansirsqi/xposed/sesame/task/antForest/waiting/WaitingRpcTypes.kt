package fansirsqi.xposed.sesame.task.antForest.waiting

/**
 * 类型化 RPC 结果（V2 §3.3.1）。
 * 传输结果与业务结果分层：先区分传输失败，再解析主页/收取业务结果。
 */
sealed interface RpcResult<out T> {
    data class Ok<T>(val value: T) : RpcResult<T>

    data class Failed(
        val kind: RpcFailureKind,
        val code: String? = null,
        val message: String? = null,
    ) : RpcResult<Nothing>
}

enum class RpcFailureKind {
    OFFLINE,
    NETWORK,
    BRIDGE_UNAVAILABLE,
    EMPTY_RESPONSE,
    FREQUENCY,
    ALREADY_COLLECTED,
    AUTH,
    SERVER_REJECTED,
    MALFORMED_RESPONSE,
    UNKNOWN;

    /** 是否为可重试的瞬时失败（网络/频率/未知），用于逐球结果映射。 */
    fun isRetryable(): Boolean = when (this) {
        NETWORK, FREQUENCY, UNKNOWN -> true
        OFFLINE, BRIDGE_UNAVAILABLE, EMPTY_RESPONSE,
        ALREADY_COLLECTED, AUTH, SERVER_REJECTED, MALFORMED_RESPONSE -> false
    }
}

/** 每球结果：蹲点路径按 bubbleId 独立处理，禁止把聚合结果复制给整组任务。 */
sealed interface BubbleOutcome {
    data class Collected(val bubbleId: Long, val energy: Int) : BubbleOutcome
    data class Protected(val bubbleId: Long, val protectionEndTime: Long) : BubbleOutcome
    data class Empty(val bubbleId: Long) : BubbleOutcome
    data class StillWaiting(val bubbleId: Long, val produceTime: Long) : BubbleOutcome
    data class AlreadyCollected(val bubbleId: Long) : BubbleOutcome
    data class Gone(val bubbleId: Long) : BubbleOutcome
    data class Failed(val bubbleId: Long, val failure: RpcResult.Failed) : BubbleOutcome
}

/** 一次蹲点收取请求的结果。 */
sealed interface WaitingBatchResult {
    data class Completed(
        val outcomes: Map<Long, BubbleOutcome>,
        val exactCollectedEnergy: Int,
    ) : WaitingBatchResult

    data class RequestFailed(val failure: RpcResult.Failed) : WaitingBatchResult
}

/** 任务版本戳：拒绝 RPC 期间已更新任务的过期结果；produceTime 用于到点保护覆盖判断。 */
data class TaskStamp(
    val taskId: String,
    val taskVersion: Long,
    val produceTime: Long = 0L,
)

/** 批量收取请求：token 归属 + 期望收取的球（bubbleId → 版本戳）。 */
data class WaitingCollectRequest(
    val token: AccountToken,
    val userId: String,
    val expected: Map<Long, TaskStamp>,
)
