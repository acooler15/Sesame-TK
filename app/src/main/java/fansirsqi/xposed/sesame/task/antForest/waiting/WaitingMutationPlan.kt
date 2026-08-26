package fansirsqi.xposed.sesame.task.antForest.waiting

/**
 * 锁内状态变更的结果计划：将锁外副作用（持久化请求、worker 信号）与锁内状态变更解耦。
 * 锁内只返回 plan，锁外执行 plan 中的副作用。
 */
sealed interface MutationPlan {
    /** 无变更（重复任务、终态无效等） */
    data object None : MutationPlan

    /** 被拒绝（过期 session、过期 producer 等） */
    data class Rejected(val result: SubmitResult) : MutationPlan

    /** 变更成功：携带持久化快照和变化的好友 ID */
    data class Mutated(
        val persistSnapshot: PersistSnapshot?,
        val changedUserId: String?,
        val result: SubmitResult,   // 用于日志/指标，submitInSession 的 when(plan) 分支不消费此字段
    ) : MutationPlan

    companion object {
        fun none(): MutationPlan = None
        fun rejected(result: SubmitResult): MutationPlan = Rejected(result)
    }
}
