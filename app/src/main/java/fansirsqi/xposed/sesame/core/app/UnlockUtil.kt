package fansirsqi.xposed.sesame.core.app

import fansirsqi.xposed.sesame.hook.captcha.BaseCaptchaHandler
import android.content.Context
import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.hook.ApplicationHook

/**
 * 内置解锁触发工具（宿主侧）。
 * 宿主进程只做两件事：检测是否需要解锁 + 通过 AIDL 发起一次无参 requestUnlock；
 * 实际编排（亮屏/唤出密码页/输入密码）全部在模块 App 进程执行，密码不出宿主。
 */
object UnlockUtil {

    private const val TAG = "UnlockUtil"

    /**
     * 触发内置解锁。返回 true=已解锁/解锁成功；false=功能关闭/无需解锁/解锁失败（原因码已记录日志）。
     * BaseCaptchaHandler 调用点签名不变。
     */
    suspend fun triggerUnlock(context: Context): Boolean {
        // 功能开关（宿主进程读宿主侧 config 实例）
        if (!ApplicationHook.config.enableBuiltinUnlock.value) return false
        // 已亮屏已解锁 → 无需解锁
        if (DeviceStateChecker.isUnlockedAndAwake(context)) return false
        // 发起 AIDL 触发（模块进程编排，密码不出宿主）
        val result = CommandUtil.requestUnlock(context)
        if (result == null) {
            Log.record(TAG, "解锁请求通信失败（CommandService 不可达）")
            return false
        }
        val (ok, reason) = result
        Log.record(TAG, if (ok) "解锁成功: $reason" else "解锁失败: $reason") // reason 为原因码，无敏感信息
        return ok
    }
}
