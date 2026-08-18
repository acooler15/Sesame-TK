package fansirsqi.xposed.sesame.core.app

import android.app.KeyguardManager
import android.content.Context
import android.os.PowerManager

/**
 * 屏幕状态检测。宿主进程与模块进程均可使用（传入各自 context）。
 * 服务获取失败时的默认值与旧 UnlockUtil.needsUnlock() 语义一致（息屏/锁屏保守判定）。
 */
object DeviceStateChecker {
    fun isScreenOn(context: Context): Boolean =
        (context.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isInteractive ?: true

    fun isLocked(context: Context): Boolean =
        (context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager)?.isKeyguardLocked ?: true

    fun isSecure(context: Context): Boolean =
        (context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager)?.isKeyguardSecure ?: false

    fun isUnlockedAndAwake(context: Context): Boolean = isScreenOn(context) && !isLocked(context)
}
