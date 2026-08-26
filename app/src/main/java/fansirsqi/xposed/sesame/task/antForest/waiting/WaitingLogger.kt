package fansirsqi.xposed.sesame.task.antForest.waiting

import fansirsqi.xposed.sesame.task.antForest.EnergyWaitingManager.WaitingTask

/**
 * 蹲点日志工具
 * 单一职责：统一日志 TAG、时间格式化、蹲点日志前缀
 */
object WaitingLogger {
    /** 统一日志 TAG（与历史日志保持一致，避免日志分流） */
    const val TAG = "EnergyWaitingManager"

    /**
     * 蹲点任务日志前缀（如：蹲点[好友|xxx]）
     */
    fun WaitingTask.logTag(): String = "蹲点[${getUserTypeTag()}${userName}]"

    /**
     * 格式化时间为人性化的字符串
     * @param milliseconds 毫秒数
     * @return 格式化后的时间字符串
     */
    fun formatTime(milliseconds: Long): String {
        val hours = milliseconds / (1000 * 60 * 60)
        val minutes = (milliseconds % (1000 * 60 * 60)) / (1000 * 60)
        return when {
            hours > 0 -> "${hours}小时${minutes}分钟"
            minutes > 0 -> "${minutes}分钟"
            else -> "${milliseconds / 1000}秒"
        }
    }

    /**
     * 格式化剩余时间状态
     * @param currentTime 当前时间
     * @param targetTime 目标时间
     * @return 格式化后的状态字符串（如："剩余2分19秒" 或 "已成熟1分5秒"）
     */
    fun formatTimeStatus(currentTime: Long, targetTime: Long): String {
        val timeRemainMs = targetTime - currentTime
        val timeRemainSeconds = timeRemainMs / 1000
        val timeRemainMinutes = timeRemainSeconds / 60

        return if (timeRemainMs > 0) {
            if (timeRemainMinutes > 0) {
                "剩余${timeRemainMinutes}分${timeRemainSeconds % 60}秒"
            } else {
                "剩余${timeRemainSeconds}秒"
            }
        } else {
            val overTimeMinutes = (-timeRemainSeconds) / 60
            if (overTimeMinutes > 0) {
                "已成熟${overTimeMinutes}分${(-timeRemainSeconds) % 60}秒"
            } else {
                "已成熟${-timeRemainSeconds}秒"
            }
        }
    }
}
