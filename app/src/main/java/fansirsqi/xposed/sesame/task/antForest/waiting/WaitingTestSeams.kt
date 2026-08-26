package fansirsqi.xposed.sesame.task.antForest.waiting

import fansirsqi.xposed.sesame.task.antForest.EnergyWaitingManager.WaitingTask
import kotlinx.coroutines.channels.ReceiveChannel
import org.json.JSONObject

/** 生产代码可注入的确定性时钟端口。 */
fun interface WaitingClock {
    fun nowMillis(): Long
}

/**
 * 将等待行为从调度策略中隔离，便于虚拟时间测试。
 * 两个方法都支持信号打断：[signal] 收到信号返回 true（调用方需重新选目标），等待完整超时返回 false。
 */
interface WaitingDelayController {
    suspend fun delayOrSignal(delayMillis: Long, signal: ReceiveChannel<Unit>): Boolean
    suspend fun delayWithWakeLockOrSignal(delayMillis: Long, signal: ReceiveChannel<Unit>): Boolean
}

/**
 * 伪随机抖动源：返回 [from, until) 半开区间内的 Long，与 Kotlin `Random.nextLong(from, until)` 语义一致。
 * 调用方如需闭区间，传 `until = desiredMax + 1`（如 `nextLong(MIN, MAX + 1)`）。
 */
fun interface WaitingJitter {
    fun nextLong(from: Long, until: Long): Long
}

/**
 * 蹲点 RPC 网关：类型化查询/验证，隔离 RPC 实现细节，便于测试注入。
 */
interface WaitingRpcGateway {
    /**
     * 类型化查询好友主页。方法名与底层 [fansirsqi.xposed.sesame.task.antForest.AntForestRpcCall.queryFriendHomePageResult] 对齐。
     * @param userId 目标好友 UID（非 null；底层方法的可空参数由调用方保证）
     */
    suspend fun queryFriendHomePage(userId: String, fromAct: String?): RpcResult<JSONObject>

    /** 验证任务保护状态：保护罩覆盖成熟期返回 [Validation.TerminalInvalid]；RPC 失败返回 [Validation.TransientFailure]。 */
    suspend fun validateProtection(task: WaitingTask): Validation
}

sealed interface Validation {
    data object Valid : Validation
    data object TransientFailure : Validation
    data class TerminalInvalid(val reason: String) : Validation
    data object DeferredByBackoff : Validation
}
