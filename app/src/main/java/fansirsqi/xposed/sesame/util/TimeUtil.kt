package fansirsqi.xposed.sesame.util
import fansirsqi.xposed.sesame.core.log.Log

import android.annotation.SuppressLint
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 时间工具类。 提供了一系列方法来处理时间相关的操作，包括时间范围检查、时间比较、日期格式化等。
 */
object TimeUtil {
    @JvmStatic
    fun checkNowInTimeRange(timeRange: String): Boolean =
        checkInTimeRange(System.currentTimeMillis(), timeRange)

    @JvmStatic
    fun checkInTimeRange(timeMillis: Long?, timeRangeList: List<String>): Boolean {
        for (timeRange in timeRangeList) {
            if (checkInTimeRange(timeMillis, timeRange)) {
                return true
            }
        }
        return false
    }

    @JvmStatic
    fun checkInTimeRange(timeMillis: Long?, timeRange: String): Boolean {
        try {
            val timeRangeArray = timeRange.split("-")
            if (timeRangeArray.size == 2) {
                val min = timeRangeArray[0]
                val max = timeRangeArray[1]
                return isAfterOrCompareTimeStr(timeMillis, min) && isBeforeOrCompareTimeStr(timeMillis, max)
            }
        } catch (e: Exception) {
            Log.printStackTrace(e)
        }
        return false
    }

    @JvmStatic
    fun isNowBeforeTimeStr(beforeTimeStr: String): Boolean =
        isBeforeTimeStr(System.currentTimeMillis(), beforeTimeStr)

    @JvmStatic
    fun isNowAfterTimeStr(afterTimeStr: String): Boolean =
        isAfterTimeStr(System.currentTimeMillis(), afterTimeStr)

    @JvmStatic
    fun isNowBeforeOrCompareTimeStr(beforeTimeStr: String): Boolean =
        isBeforeOrCompareTimeStr(System.currentTimeMillis(), beforeTimeStr)

    @JvmStatic
    fun isNowAfterOrCompareTimeStr(afterTimeStr: String): Boolean =
        isAfterOrCompareTimeStr(System.currentTimeMillis(), afterTimeStr)

    @JvmStatic
    fun isBeforeTimeStr(timeMillis: Long?, beforeTimeStr: String): Boolean {
        val compared = isCompareTimeStr(timeMillis, beforeTimeStr)
        if (compared != null) {
            return compared < 0
        }
        return false
    }

    @JvmStatic
    fun isAfterTimeStr(timeMillis: Long?, afterTimeStr: String): Boolean {
        val compared = isCompareTimeStr(timeMillis, afterTimeStr)
        if (compared != null) {
            return compared > 0
        }
        return false
    }

    @JvmStatic
    fun isBeforeOrCompareTimeStr(timeMillis: Long?, beforeTimeStr: String): Boolean {
        val compared = isCompareTimeStr(timeMillis, beforeTimeStr)
        if (compared != null) {
            return compared <= 0
        }
        return false
    }

    @JvmStatic
    fun isAfterOrCompareTimeStr(timeMillis: Long?, afterTimeStr: String): Boolean {
        val compared = isCompareTimeStr(timeMillis, afterTimeStr)
        if (compared != null) {
            return compared >= 0
        }
        return false
    }

    @JvmStatic
    fun isCompareTimeStr(timeMillis: Long?, compareTimeStr: String): Int? {
        try {
            val timeCalendar = Calendar.getInstance()
            timeCalendar.timeInMillis = timeMillis!!
            val compareCalendar = getTodayCalendarByTimeStr(compareTimeStr)
            if (compareCalendar != null) {
                return timeCalendar.compareTo(compareCalendar)
            }
        } catch (e: Exception) {
            Log.printStackTrace(e)
        }
        return null
    }

    @JvmStatic
    fun getTodayCalendarByTimeStr(timeStr: String): Calendar? =
        getCalendarByTimeStr(null as Long?, timeStr)

    @JvmStatic
    fun getCalendarByTimeStr(timeMillis: Long?, timeStr: String): Calendar? {
        try {
            val timeCalendar = getCalendarByTimeMillis(timeMillis)
            return getCalendarByTimeStr(timeCalendar, timeStr)
        } catch (e: Exception) {
            Log.printStackTrace(e)
        }
        return null
    }

    @JvmStatic
    fun getCalendarByTimeStr(timeCalendar: Calendar, timeStr: String): Calendar? {
        try {
            when (timeStr.length) {
                6 -> {
                    timeCalendar.set(Calendar.SECOND, timeStr.substring(4).toInt())
                    timeCalendar.set(Calendar.MINUTE, timeStr.substring(2, 4).toInt())
                    timeCalendar.set(Calendar.HOUR_OF_DAY, timeStr.substring(0, 2).toInt())
                }

                4 -> {
                    timeCalendar.set(Calendar.SECOND, 0)
                    timeCalendar.set(Calendar.MINUTE, timeStr.substring(2, 4).toInt())
                    timeCalendar.set(Calendar.HOUR_OF_DAY, timeStr.substring(0, 2).toInt())
                }

                2 -> {
                    timeCalendar.set(Calendar.SECOND, 0)
                    timeCalendar.set(Calendar.MINUTE, 0)
                    timeCalendar.set(Calendar.HOUR_OF_DAY, timeStr.substring(0, 2).toInt())
                }

                else -> return null
            }
            timeCalendar.set(Calendar.MILLISECOND, 0)
            return timeCalendar
        } catch (e: Exception) {
            Log.printStackTrace(e)
        }
        return null
    }

    @JvmStatic
    fun getCalendarByTimeMillis(timeMillis: Long?): Calendar {
        val timeCalendar = Calendar.getInstance()
        if (timeMillis != null) {
            timeCalendar.timeInMillis = timeMillis
        }
        return timeCalendar
    }

    /**
     * 获取当前时间的字符串表示
     *
     * @param ts 时间戳
     * @return "下午8:00:00"（东八区）或 "8:00:00 PM"（英语环境）
     */
    @JvmStatic
    fun getTimeStr(ts: Long): String = DateFormat.getTimeInstance().format(Date(ts))

    /**
     * 获取当前时间的字符串表示
     *
     * @return "下午8:00:00"（东八区）或 "8:00:00 PM"（英语环境）
     */
    @JvmStatic
    fun getTimeStr(): String = getTimeStr(System.currentTimeMillis())

    /**
     * 获取当前日期的字符串表示
     *
     * @return 格式：yyyy年*M月*d日
     */
    @JvmStatic
    fun getDateStr(): String = getDateStr(0)

    /**
     * 获取日期的字符串表示
     *
     * @param plusDay 日期偏移量
     * @return 格式：yyyy年*M月*d日
     */
    @JvmStatic
    fun getDateStr(plusDay: Int): String {
        val c = Calendar.getInstance()
        if (plusDay != 0) {
            c.add(Calendar.DATE, plusDay)
        }
        return DateFormat.getDateInstance().format(c.time)
    }

    /**
     * 默认获取今天
     *
     * @return yyyy-MM-dd
     */
    @JvmStatic
    fun getDateStr2(): String = getDateStr2(0)

    /**
     * 默认获取今天
     *
     * @param plusDay 日期偏移量
     * @return yyyy-MM-dd
     */
    @JvmStatic
    fun getDateStr2(plusDay: Int): String {
        val c = Calendar.getInstance()
        if (plusDay != 0) {
            c.add(Calendar.DATE, plusDay)
        }
        val date = c.time

        // 使用固定格式 yyyy-MM-dd
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(date)
    }

    @JvmStatic
    fun getToday(): Calendar {
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c
    }

    @JvmStatic
    fun getNow(): Calendar = Calendar.getInstance()

    /**
     * 协程兼容的延迟方法
     */
    @JvmStatic
    fun sleepCompat(millis: Long) {
        CoroutineUtils.sleepCompat(millis)
    }

    /**
     * 获取指定时间的周数
     *
     * @param dateTime 时间
     * @return 当前年的第几周
     */
    @JvmStatic
    fun getWeekNumber(dateTime: Date): Int {
        val calendar = Calendar.getInstance()
        calendar.time = dateTime
        // 设置周的第一天为周一
        calendar.firstDayOfWeek = Calendar.MONDAY
        return calendar.get(Calendar.WEEK_OF_YEAR)
    }

    /**
     * 比较第一个日历的天数小于第二个日历的天数
     *
     * @param firstCalendar  第一个日历
     * @param secondCalendar 第二个日历
     * @return Boolean 如果小于，则为true，否则为false
     */
    @JvmStatic
    fun isLessThanSecondOfDays(firstCalendar: Calendar, secondCalendar: Calendar): Boolean =
        (firstCalendar.get(Calendar.YEAR) < secondCalendar.get(Calendar.YEAR))
                || (firstCalendar.get(Calendar.YEAR) == secondCalendar.get(Calendar.YEAR)
                && firstCalendar.get(Calendar.DAY_OF_YEAR) < secondCalendar.get(Calendar.DAY_OF_YEAR))

    /**
     * 比较第一个时间戳的天数是否小于第二个时间戳的天数
     *
     * @param firstTimestamp  第一个时间戳
     * @param secondTimestamp 第二个时间戳
     * @return Boolean 如果小于，则为true，否则为false
     */
    @JvmStatic
    fun isLessThanSecondOfDays(firstTimestamp: Long?, secondTimestamp: Long?): Boolean {
        val firstCalendar = getCalendarByTimeMillis(firstTimestamp)
        val secondCalendar = getCalendarByTimeMillis(secondTimestamp)
        return isLessThanSecondOfDays(firstCalendar, secondCalendar)
    }

    /**
     * 通过时间戳比较传入的时间戳的天数是否小于当前时间戳的天数
     *
     * @param timestamp 时间戳
     * @return Boolean 如果小于当前时间戳所计算的天数，则为true，否则为false
     */
    @JvmStatic
    fun isLessThanNowOfDays(timestamp: Long?): Boolean =
        isLessThanSecondOfDays(getCalendarByTimeMillis(timestamp), getNow())

    /**
     * 判断两个日历对象是否为同一天
     *
     * @param firstCalendar  第一个日历对象
     * @param secondCalendar 第二个日历对象
     * @return 两个日历对象是否为同一天
     */
    @JvmStatic
    fun isSameDay(firstCalendar: Calendar, secondCalendar: Calendar): Boolean =
        firstCalendar.get(Calendar.YEAR) == secondCalendar.get(Calendar.YEAR)
                && firstCalendar.get(Calendar.DAY_OF_YEAR) == secondCalendar.get(Calendar.DAY_OF_YEAR)

    /**
     * 判断两个时间戳是否为同一天
     *
     * @param firstTimestamp  第一个时间戳
     * @param secondTimestamp 第二个时间戳
     * @return 两个时间戳是否为同一天
     */
    @JvmStatic
    fun isSameDay(firstTimestamp: Long?, secondTimestamp: Long?): Boolean {
        val firstCalendar = getCalendarByTimeMillis(firstTimestamp)
        val secondCalendar = getCalendarByTimeMillis(secondTimestamp)
        return isSameDay(firstCalendar, secondCalendar)
    }

    /**
     * 判断日历对象是否为今天
     *
     * @param calendar 日历对象
     * @return 日历对象是否为今天
     */
    @JvmStatic
    fun isToday(calendar: Calendar): Boolean = isSameDay(getToday(), calendar)

    /**
     * 判断时间戳是否为今天
     *
     * @param timestamp 时间戳
     * @return 时间戳是否为今天
     */
    @JvmStatic
    fun isToday(timestamp: Long?): Boolean = isToday(getCalendarByTimeMillis(timestamp))

    @SuppressLint("SimpleDateFormat")
    @JvmStatic
    fun getCommonDateFormat(): DateFormat = SimpleDateFormat("dd日HH:mm:ss")

    @SuppressLint("SimpleDateFormat")
    @JvmStatic
    fun getCommonDate(timestamp: Long?): String = getCommonDateFormat().format(timestamp!!)

    @JvmField
    val DATE_TIME_FORMAT_THREAD_LOCAL: ThreadLocal<SimpleDateFormat> = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat =
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    }

    @JvmField
    val OTHER_DATE_TIME_FORMAT_THREAD_LOCAL: ThreadLocal<SimpleDateFormat> = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat =
            SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.getDefault())
    }

    @JvmStatic
    fun timeToStamp(timers: String): Long {
        var d = Date()
        val timeStemp: Long
        try {
            var simpleDateFormat = OTHER_DATE_TIME_FORMAT_THREAD_LOCAL.get()
            if (simpleDateFormat == null) {
                simpleDateFormat = SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.getDefault())
            }
            val newD = simpleDateFormat.parse(timers)
            if (newD != null) {
                d = newD
            }
        } catch (_: ParseException) {
        }
        timeStemp = d.time
        return timeStemp
    }

    /**
     * 获取格式化的日期 时间字符串yyyy-MM-dd HH:mm:ss
     *
     */
    @JvmStatic
    fun getFormatDateTime(): String {
        var simpleDateFormat = DATE_TIME_FORMAT_THREAD_LOCAL.get()
        if (simpleDateFormat == null) {
            simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH-mm-ss", Locale.getDefault())
        }
        return simpleDateFormat.format(Date())
    }

    /**
     * 获取格式化的日期符串yyyy-MM-dd
     *
     */
    @JvmStatic
    fun getFormatDate(): String = getFormatDateTime().split(" ")[0]

    /**
     * 获取格式化的时间字符串HH:mm:ss
     *
     */
    @JvmStatic
    fun getFormatTime(): String = getFormatDateTime().split(" ")[1]

    /**
     * 根据传入的格式化字符串获取格式化后的时间字符串
     *
     * @param offset 日期偏移量
     * @param format 格式化字符串
     * @return 格式化后的时间字符串
     */
    @SuppressLint("SimpleDateFormat")
    @JvmStatic
    fun getFormatTime(offset: Int, format: String): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, offset)
        val sdf = SimpleDateFormat(format)
        return sdf.format(calendar.time)
    }

    /**
     * 将毫秒数格式化为 时:分:秒:毫秒
     * @param durationMillis 毫秒数
     * @return 格式化后的字符串
     */
    @JvmStatic
    fun formatDuration(durationMillis: Long): String {
        val millis = durationMillis % 1000
        val second = (durationMillis / 1000) % 60
        val minute = (durationMillis / (1000 * 60)) % 60
        val hour = (durationMillis / (1000 * 60 * 60))

        return String.format(Locale.getDefault(), "%02d:%02d:%02d:%03d", hour, minute, second, millis)
    }

    /**
     * 获取无分隔符的日期字符串
     *
     * @return yyyyMMdd
     */
    @JvmStatic
    fun getDateStrNoSplite(): String = getDateStrNoSplite(0)

    /**
     * 获取无分隔符的日期字符串
     *
     * @param plusDay 日期偏移量
     * @return yyyyMMdd
     */
    @JvmStatic
    fun getDateStrNoSplite(plusDay: Int): String {
        val c = Calendar.getInstance()
        if (plusDay != 0) {
            c.add(Calendar.DATE, plusDay)
        }
        val date = c.time
        // 使用 yyyyMMdd 格式，匹配好家无忧卡签到记录中的 date 格式
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        return sdf.format(date)
    }
}
