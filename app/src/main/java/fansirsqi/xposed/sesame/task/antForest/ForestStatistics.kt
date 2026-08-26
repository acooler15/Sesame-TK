package fansirsqi.xposed.sesame.task.antForest

import java.util.concurrent.atomic.AtomicInteger

/**
 * 蚂蚁森林收取统计
 *
 * 统计累加统一使用原子 API（V2 §3.3.4）：并发普通收取与蹲点收取可能同时写入，
 * 普通 `Int +=` 存在丢更新风险，且蹲点结果不得反向读取全局统计差值判断成功。
 */
internal object ForestStatistics {
    private val totalCollectedAtomic = AtomicInteger(0)

    /** 本次会话累计收取能量（仅用于展示，不参与蹲点业务判断） */
    val totalCollected: Int
        get() = totalCollectedAtomic.get()

    const val TOTAL_HELP_COLLECTED = 0
    const val TOTAL_WATERED = 0

    /** 原子累加展示统计，避免并发丢更新。 */
    fun addToTotalCollected(energyCount: Int) {
        totalCollectedAtomic.addAndGet(energyCount)
    }
}
