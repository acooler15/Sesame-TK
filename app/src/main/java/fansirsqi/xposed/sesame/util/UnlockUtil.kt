package fansirsqi.xposed.sesame.util

import android.app.KeyguardManager
import android.content.Context
import android.os.PowerManager
import fansirsqi.xposed.sesame.model.BaseModel
import kotlinx.coroutines.delay

/**
 * 外部解锁工具。
 * 通过 Shell 命令触发屏幕解锁（也可通过 am start 启动外部应用如 AutoJS）。
 * 命令由用户配置，空字符串表示关闭。
 */
object UnlockUtil {

    private const val TAG = "UnlockUtil"

    /**
     * 检查设备是否需要解锁：
     * - 屏幕已熄灭 或 锁屏界面正在展示 → 需要解锁
     * - 屏幕已亮且已解锁 → 不需要
     */
    private fun needsUnlock(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager ?: return true
        val screenOn = pm.isInteractive
        val locked = km.isKeyguardLocked
        Log.record(TAG, "[屏幕状态] screenOn=$screenOn, keyguardLocked=$locked")
        return !screenOn || locked
    }

    /**
     * 执行用户配置的解锁 Shell 命令，并等待足够时间让外部脚本完成解锁。
     *
     * @return true 表示命令已发送且等待完成，false 表示未配置、无需解锁或发送失败
     */
    suspend fun triggerUnlock(context: Context): Boolean {
        val command = BaseModel.unlockShellCommand.value
        if (command.isNullOrBlank()) {
            return false
        }

        // 只有屏幕熄灭或锁屏时才需要触发解锁
        if (!needsUnlock(context)) {
            Log.record(TAG, "[外部解锁] 屏幕已亮且已解锁，跳过")
            return false
        }

        return try {
            Log.record(TAG, "[外部解锁] 执行: $command")
            val result = CommandUtil.executeCommand(context, command)
            val ok = result != null
            if (ok) {
                Log.record(TAG, "[外部解锁] 返回: ${result.trim()}")
            } else {
                Log.record(TAG, "[外部解锁] 返回 null（服务未就绪或执行失败）")
                return false
            }

            // 等待外部脚本完成解锁操作
            val waitSec = BaseModel.unlockWaitSeconds.value.toLong()
            if (waitSec > 0) {
                Log.record(TAG, "[外部解锁] 等待 ${waitSec}s 让外部脚本完成解锁...")
                delay(waitSec * 1000L)
            }
            true
        } catch (e: Exception) {
            Log.record(TAG, "[外部解锁] 异常: ${e.message}")
            false
        }
    }
}
