package fansirsqi.xposed.sesame.hook

import android.content.Intent
import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.core.log.Log.error
import fansirsqi.xposed.sesame.core.log.Log.record
import fansirsqi.xposed.sesame.core.util.TimeUtil
import fansirsqi.xposed.sesame.data.Config
import fansirsqi.xposed.sesame.data.General
import fansirsqi.xposed.sesame.data.Status.Companion.save
import fansirsqi.xposed.sesame.hook.ApplicationHook.Companion.appContext
import fansirsqi.xposed.sesame.hook.ApplicationHook.Companion.classLoader
import fansirsqi.xposed.sesame.hook.keepalive.SmartSchedulerManager.initialize
import fansirsqi.xposed.sesame.hook.keepalive.SmartSchedulerManager.schedule
import fansirsqi.xposed.sesame.task.MainTask
import fansirsqi.xposed.sesame.task.ModelTask.Companion.stopAllTask
import fansirsqi.xposed.sesame.task.TaskRunnerAdapter
import fansirsqi.xposed.sesame.util.maps.UserMap.currentUid
import java.util.Calendar
import kotlin.concurrent.Volatile

object TaskScheduler {
    private val TAG = ApplicationHook.TAG

    // 任务锁
    private val taskLock = Any()

    @Volatile
    private var isTaskRunning = false

    private class TaskLock : AutoCloseable {
        private val acquired: Boolean

        init {
            synchronized(taskLock) {
                if (isTaskRunning) {
                    acquired = false
                    throw IllegalStateException("任务已在运行中")
                }
                isTaskRunning = true
                acquired = true
            }
        }

        override fun close() {
            if (acquired) {
                synchronized(taskLock) {
                    isTaskRunning = false
                }
            }
        }
    }

    var mainTask: MainTask? = null

    @Volatile
    var lastExecTime: Long = 0

    @Volatile
    var nextExecutionTime: Long = 0
    private const val MAX_INACTIVE_TIME: Long = 3600000 // 1小时

    @Volatile
    var dayCalendar: Calendar?

    init {
        dayCalendar = Calendar.getInstance()
        resetToMidnight(dayCalendar!!)
    }

    fun runMainTaskLogic() {
        try {
            TaskLock().use { _ ->
                if (!ApplicationHook.init || !Config.isLoaded()) return
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastExecTime < 2000) {
                    record(TAG, "⚠️ 间隔过短，跳过")
                    schedule(ApplicationHook.config.checkInterval.value.toLong(), "间隔重试") {
                        execHandler()
                    }
                    return
                }

                val currentUid = currentUid
                val targetUid = HookUtil.getUserId(classLoader!!)
                if (targetUid == null || targetUid != currentUid) {
                    reOpenApp()
                    return
                }

                lastExecTime = currentTime
                TaskRunnerAdapter().run()
            }
        } catch (e: IllegalStateException) {
            record(TAG, "⚠️ " + e.message)
        } catch (e: Exception) {
            Log.printStackTrace(TAG, e)
        }
    }

    // --- 辅助方法 ---
    fun ensureScheduler() {
        if (appContext != null) {
            initialize(appContext!!)
        }
    }

    fun scheduleNextExecutionInternal(lastTime: Long) {
        try {
            checkInactiveTime()
            val checkInterval = ApplicationHook.config.checkInterval.value
            val execAtTimeList = ApplicationHook.config.execAtTimeList.value
            if (execAtTimeList != null && execAtTimeList.contains("-1")) {
                record(TAG, "定时执行未开启")
                return
            }
            var delayMillis = checkInterval.toLong()
            var targetTime: Long = 0
            if (execAtTimeList != null) {
                val lastCal = TimeUtil.getCalendarByTimeMillis(lastTime)
                val nextCal = TimeUtil.getCalendarByTimeMillis(lastTime + checkInterval)
                for (timeStr in execAtTimeList) {
                    val execCal = TimeUtil.getTodayCalendarByTimeStr(timeStr)
                    if (execCal != null && lastCal < execCal && nextCal > execCal) {
                        record(TAG, "设置定时执行:$timeStr")
                        targetTime = execCal.getTimeInMillis()
                        delayMillis = targetTime - lastTime
                        break
                    }
                }
            }
            nextExecutionTime = if (targetTime > 0) targetTime else (lastTime + delayMillis)
            ensureScheduler()
            schedule(delayMillis, "轮询任务") {
                execHandler()
            }
        } catch (e: Exception) {
            Log.printStackTrace(TAG, "scheduleNextExecution failed", e)
        }
    }

    fun execHandler() {
        if (mainTask != null) mainTask!!.startTask(false)
    }

    fun stopHandler() {
        if (mainTask != null) mainTask!!.stopTask()
        stopAllTask()
    }

    // --- 杂项方法 ---
    private fun checkInactiveTime() {
        if (lastExecTime == 0L) return
        val inactiveTime: Long = System.currentTimeMillis() - lastExecTime
        if (inactiveTime > MAX_INACTIVE_TIME) {
            record(TAG, "⚠️ 检测到长时间未执行(" + inactiveTime / 60000 + "m)，重新登录")
            reOpenApp()
        }
    }

    fun updateDay() {
        val now = Calendar.getInstance()
        if (dayCalendar == null || dayCalendar!!.get(Calendar.DAY_OF_MONTH) != now.get(Calendar.DAY_OF_MONTH)) {
            dayCalendar = now.clone() as Calendar
            resetToMidnight(dayCalendar!!)
            record(TAG, "日期更新")
            setWakenAtTimeAlarm()
        }
        try {
            save(now)
        } catch (_: Exception) {
        }
    }

    private fun resetToMidnight(calendar: Calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
    }

    fun reOpenApp() {
        ensureScheduler()
        schedule(20000L, "重新登录") {
            try {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setClassName(General.PACKAGE_NAME, General.CURRENT_USING_ACTIVITY)
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ApplicationHook.offline = true
                if (appContext != null) appContext!!.startActivity(intent)
            } catch (e: Exception) {
                error(TAG, "重启Activity失败: " + e.message)
            }
        }
    }

    // --- 定时唤醒 ---
    fun setWakenAtTimeAlarm() {
        if (appContext == null) return
        ensureScheduler()

        val wakenAtTimeList = ApplicationHook.config.wakenAtTimeList.value
        if (wakenAtTimeList != null && wakenAtTimeList.contains("-1")) return

        // 1. 每日0点
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        resetToMidnight(calendar)
        val delayToMidnight = calendar.getTimeInMillis() - System.currentTimeMillis()

        if (delayToMidnight > 0) {
            schedule(delayToMidnight, "每日0点任务") {
                record(TAG, "⏰ 0点任务触发")
                updateDay()
                execHandler()
                setWakenAtTimeAlarm() // 递归设置明天
            }
        }

        // 2. 自定义时间
        if (wakenAtTimeList != null) {
            val now = Calendar.getInstance()
            for (timeStr in wakenAtTimeList) {
                try {
                    val target = TimeUtil.getTodayCalendarByTimeStr(timeStr)
                    if (target != null && target > now) {
                        val delay = target.getTimeInMillis() - System.currentTimeMillis()
                        schedule(delay, "自定义: $timeStr") {
                            record(TAG, "⏰ 自定义触发: $timeStr")
                            execHandler()
                        }
                    }
                } catch (_: Exception) { /* ignore */
                }
            }
        }
    }
}
