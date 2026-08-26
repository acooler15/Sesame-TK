package fansirsqi.xposed.sesame.task.antForest.waiting

import fansirsqi.xposed.sesame.core.log.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 蹲点运行指标（V2 §3.5.2，内部观测，不暴露为用户配置）。
 *
 * 只记录计数类事件（持久化请求/写入/跳过、被代次屏障拒绝的旧操作、类型化 RPC 结果分布、
 * 调度延迟、关闭耗时）；活跃任务/好友 worker 等快照由调用方在输出摘要时传入。
 * UID 不允许记录完整值，日志只输出计数与摘要。
 */
object WaitingMetrics {
    /** 被代次/会话屏障拒绝的旧操作次数 */
    val staleActionsRejected = AtomicLong(0)

    /** 请求保存次数 / 实际写入次数 / 因旧写入戳被跳过的次数 */
    val persistenceRequested = AtomicLong(0)
    val persistenceWritten = AtomicLong(0)
    val persistenceStaleSkipped = AtomicLong(0)

    /** 类型化 RPC 成功次数与失败分布 */
    val rpcOk = AtomicLong(0)
    private val rpcResultByKind = ConcurrentHashMap<RpcFailureKind, AtomicLong>()

    /** 实际 RPC 开始时间与目标时间之差（最近一次，毫秒） */
    val lastScheduleLatenessMs = AtomicLong(0)

    /** 账户关闭及最终 flush 耗时（最近一次，毫秒） */
    val accountCloseMs = AtomicLong(0)

    fun recordRpcResult(kind: RpcFailureKind?) {
        if (kind == null) {
            rpcOk.incrementAndGet()
        } else {
            rpcResultByKind.computeIfAbsent(kind) { AtomicLong(0) }.incrementAndGet()
        }
    }

    private fun rpcResultSummary(): String {
        val failures = rpcResultByKind.entries
            .sortedByDescending { it.value.get() }
            .joinToString(", ") { "${it.key}=${it.value.get()}" }
        return "成功=${rpcOk.get()}" + if (failures.isEmpty()) "" else " 失败[$failures]"
    }

    /**
     * 输出当前指标摘要（账户切换/服务销毁等关键节点调用）。
     * @param activeTasks 当前任务数
     * @param friendWorkers 好友 worker 数
     * @param retryPendingTasks 处于退避状态的任务数
     * @param restoreJobs 恢复 Job 数（必须为 0/1）
     */
    fun logSummary(
        activeTasks: Long,
        friendWorkers: Long,
        retryPendingTasks: Long,
        restoreJobs: Long,
    ) {
        Log.record(
            WaitingLogger.TAG,
            "蹲点指标: 活跃任务=$activeTasks 好友worker=$friendWorkers 退避任务=$retryPendingTasks " +
                "恢复Job=$restoreJobs 拒绝旧操作=${staleActionsRejected.get()} " +
                "持久化(请求/写入/跳过)=${persistenceRequested.get()}/${persistenceWritten.get()}/${persistenceStaleSkipped.get()} " +
                "RPC结果[${rpcResultSummary()}] 最近调度延迟=${lastScheduleLatenessMs.get()}ms " +
                "关闭耗时=${accountCloseMs.get()}ms"
        )
    }

    /** 清空计数（进程级单例，一般无需调用；保留供测试复位）。 */
    fun reset() {
        staleActionsRejected.set(0)
        persistenceRequested.set(0)
        persistenceWritten.set(0)
        persistenceStaleSkipped.set(0)
        rpcOk.set(0)
        rpcResultByKind.clear()
        lastScheduleLatenessMs.set(0)
        accountCloseMs.set(0)
    }
}
