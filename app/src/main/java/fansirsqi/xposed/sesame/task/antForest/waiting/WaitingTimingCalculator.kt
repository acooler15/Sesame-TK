package fansirsqi.xposed.sesame.task.antForest.waiting

import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.task.antForest.EnergyWaitingManager.WaitingTask

/**
 * 蹲点时机计算器（纯决策，不负责等待）
 *
 * 单一职责：计算精确收取时间（calculatePreciseCollectTime）。
 * 所有等待统一由 [WaitingScheduler] 的 FriendWorker 按绝对目标时间执行；
 * 本类不再包含 delay 或第二套 TimingDecision。
 */
internal class WaitingTimingCalculator {

    /**
     * 精确时机计算 - 能量成熟或保护结束后立即收取。
     * 保护策略冻结为 SKIP_IF_PROTECTION_COVERS_MATURITY（见 V2 §4）：
     * 登记/验证路径负责跳过被保护覆盖的任务，这里只返回不早于成熟时间的目标。
     */
    fun calculatePreciseCollectTime(task: WaitingTask): Long {
        // 自己的账号：不考虑保护罩，直接在能量成熟时收取
        if (task.isSelf()) {
            return task.produceTime
        }

        // 好友账号：考虑保护罩（登记时已过滤覆盖场景，这里兜底取不早于成熟的时间）
        val protectionEndTime = task.getProtectionEndTime()
        return maxOf(task.produceTime, protectionEndTime)
    }
}
