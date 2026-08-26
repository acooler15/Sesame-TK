package fansirsqi.xposed.sesame.task.antForest.waiting

import fansirsqi.xposed.sesame.task.antForest.AntForestRpcCall
import fansirsqi.xposed.sesame.task.antForest.EnergyWaitingManager.WaitingTask
import fansirsqi.xposed.sesame.task.antForest.ForestUtil
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.util.concurrent.ThreadLocalRandom

/**
 * 引擎组件包：由门面工厂方法创建，生产装配真实实现，测试注入 fake。
 * session 构造时一次性注入，session 生命周期内不变。
 */
internal data class WaitingEngineComponents(
    val clock: WaitingClock,
    val delayController: WaitingDelayController,
    val jitter: WaitingJitter,
    val rpcGateway: WaitingRpcGateway,
    val retryPolicy: WaitingRetryPolicy,
    val timingCalculator: WaitingTimingCalculator,
)

/**
 * 引擎组件工厂：生产装配真实实现（V3 §3.3.3）。
 * [createTestComponents] 供测试注入 fake clock/delay/jitter/rpcGateway。
 */
internal object WaitingEngineFactory {

    fun createRealComponents(): WaitingEngineComponents {
        val clock = WaitingClock { System.currentTimeMillis() }
        val jitter = WaitingJitter { from, until -> ThreadLocalRandom.current().nextLong(from, until) }
        return WaitingEngineComponents(
            clock = clock,
            delayController = ProductionDelayController(),
            jitter = jitter,
            rpcGateway = ProductionRpcGateway(),
            retryPolicy = WaitingRetryPolicy(clock, jitter),
            timingCalculator = WaitingTimingCalculator(),
        )
    }

    fun createTestComponents(
        clock: WaitingClock,
        delayController: WaitingDelayController,
        jitter: WaitingJitter,
        rpcGateway: WaitingRpcGateway,
    ): WaitingEngineComponents = WaitingEngineComponents(
        clock = clock,
        delayController = delayController,
        jitter = jitter,
        rpcGateway = rpcGateway,
        retryPolicy = WaitingRetryPolicy(clock, jitter),
        timingCalculator = WaitingTimingCalculator(),
    )
}

/**
 * 生产延迟控制器：
 * - [delayOrSignal]：`withTimeoutOrNull(delayMillis) { signal.receive() }`，收到信号返回 true，超时返回 false；
 * - [delayWithWakeLockOrSignal]：select 语义（信号优先），等待期间不额外持锁（当前调度路径不使用该变体）。
 */
internal class ProductionDelayController : WaitingDelayController {

    override suspend fun delayOrSignal(delayMillis: Long, signal: ReceiveChannel<Unit>): Boolean {
        if (delayMillis <= 0L) return signal.tryReceive().getOrNull() != null
        return withTimeoutOrNull(delayMillis) { signal.receive() } != null
    }

    override suspend fun delayWithWakeLockOrSignal(delayMillis: Long, signal: ReceiveChannel<Unit>): Boolean {
        // 与 delayOrSignal 同语义（信号优先）；当前调度路径不使用该变体，等待期间不额外持锁
        if (delayMillis <= 0L) return signal.tryReceive().getOrNull() != null
        return withTimeoutOrNull(delayMillis) { signal.receive() } != null
    }
}

/**
 * 生产 RPC 网关：委托 [AntForestRpcCall] 的类型化入口。
 * - [queryFriendHomePage]：委托 `queryFriendHomePageResult`（RpcResult<JSONObject>）；
 * - [validateProtection]：自己的账号直接 Valid；好友账号查询主页后判断保护罩是否覆盖成熟期。
 */
internal class ProductionRpcGateway : WaitingRpcGateway {

    override suspend fun queryFriendHomePage(userId: String, fromAct: String?): RpcResult<JSONObject> {
        return AntForestRpcCall.queryFriendHomePageResult(userId, fromAct)
    }

    override suspend fun validateProtection(task: WaitingTask): Validation {
        if (task.isSelf()) return Validation.Valid
        return when (val r = queryFriendHomePage(task.userId, task.fromTag)) {
            is RpcResult.Ok -> {
                if (ForestUtil.shouldSkipWaitingDueToProtection(r.value, task.produceTime)) {
                    Validation.TerminalInvalid("保护罩覆盖能量成熟期")
                } else {
                    Validation.Valid
                }
            }
            is RpcResult.Failed -> Validation.TransientFailure
        }
    }
}
