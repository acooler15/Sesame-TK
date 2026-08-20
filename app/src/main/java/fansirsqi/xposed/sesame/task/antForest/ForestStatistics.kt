package fansirsqi.xposed.sesame.task.antForest

/**
 * 蚂蚁森林收取统计
 */
internal object ForestStatistics {
    var totalCollected = 0

    const val TOTAL_HELP_COLLECTED = 0
    const val TOTAL_WATERED = 0

    fun addToTotalCollected(energyCount: Int) {
        totalCollected += energyCount
    }
}
