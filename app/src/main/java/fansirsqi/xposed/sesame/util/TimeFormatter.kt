package fansirsqi.xposed.sesame.util

import kotlin.math.abs

/**
 * 时间格式化工具类
 * 提供各种时间差和剩余时间的人性化格式化功能
 */
object TimeFormatter {
    /**
     * 时间常量
     */
    const val ONE_SECOND_MS: Long = 1000L
    const val ONE_MINUTE_MS: Long = 60 * ONE_SECOND_MS
    const val ONE_HOUR_MS: Long = 60 * ONE_MINUTE_MS
    const val ONE_DAY_MS: Long = 24 * ONE_HOUR_MS

    /**
     * 格式化时间差为人性化的字符串
     * @param milliseconds 时差毫秒
     * @param showSign 是否显示正负号
     * @return 格式化后的时间字符串
     */
    @JvmStatic
    fun formatTimeDifference(milliseconds: Long, showSign: Boolean): String {
        val absMillis = abs(milliseconds)
        val sign = if (showSign && milliseconds >= 0) "+" else if (showSign) "-" else ""

        return when {
            absMillis < ONE_MINUTE_MS -> sign + (absMillis / ONE_SECOND_MS) + "秒"
            absMillis < ONE_HOUR_MS -> sign + (absMillis / ONE_MINUTE_MS) + "分钟"
            absMillis < ONE_DAY_MS -> sign + (absMillis / ONE_HOUR_MS) + "小时"
            else -> sign + (absMillis / ONE_DAY_MS) + "天"
        }
    }

    /**
     * 格式化时间差为人性化的字符串（带正负号）
     */
    @JvmStatic
    fun formatTimeDifference(milliseconds: Long): String = formatTimeDifference(milliseconds, true)

    /**
     * 格式化剩余时间（不带正负号）
     */
    @JvmStatic
    fun formatRemainingTime(milliseconds: Long): String = formatDetailedRemainingTime(abs(milliseconds))

    /**
     * 格式化详细的剩余时间（显示天、小时、分钟）
     * @param milliseconds 毫秒数
     * @return 详细的时间字符串，如 "1天2小时3分钟"
     */
    @JvmStatic
    fun formatDetailedRemainingTime(milliseconds: Long): String {
        if (milliseconds < ONE_MINUTE_MS) {
            return (milliseconds / ONE_SECOND_MS).toString() + "秒"
        } else if (milliseconds < ONE_HOUR_MS) {
            val minutes = milliseconds / ONE_MINUTE_MS
            val seconds = (milliseconds % ONE_MINUTE_MS) / ONE_SECOND_MS
            return if (seconds > 0) "${minutes}分钟${seconds}秒" else "${minutes}分钟"
        } else if (milliseconds < ONE_DAY_MS) {
            val hours = milliseconds / ONE_HOUR_MS
            val minutes = (milliseconds % ONE_HOUR_MS) / ONE_MINUTE_MS
            return if (minutes > 0) "${hours}小时${minutes}分钟" else "${hours}小时"
        } else {
            val days = milliseconds / ONE_DAY_MS
            val hours = (milliseconds % ONE_DAY_MS) / ONE_HOUR_MS
            val minutes = ((milliseconds % ONE_DAY_MS) % ONE_HOUR_MS) / ONE_MINUTE_MS

            val result = StringBuilder().append(days).append("天")
            if (hours > 0) {
                result.append(hours).append("小时")
            }
            if (minutes > 0) {
                result.append(minutes).append("分钟")
            }
            return result.toString()
        }
    }
}
