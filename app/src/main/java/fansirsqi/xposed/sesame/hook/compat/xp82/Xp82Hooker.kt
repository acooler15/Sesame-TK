package fansirsqi.xposed.sesame.hook.compat.xp82

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import fansirsqi.xposed.sesame.hook.compat.HookCallback
import fansirsqi.xposed.sesame.hook.compat.HookParam
import fansirsqi.xposed.sesame.hook.compat.Hooker
import fansirsqi.xposed.sesame.hook.compat.UnhookHandle
import java.lang.reflect.Member
import java.lang.reflect.Method

/**
 * Xposed API 82 后端（de.robv 允许区①）：直接把 [HookCallback] 适配为 [XC_MethodHook]，
 * 依赖 82 框架原生的 before/after / returnEarly / throwable 语义，无需自行映射。
 */
class Xp82Hooker : Hooker {

    override val apiLevel: Int = 82
    override val frameworkName: String = "Xposed"
    override val frameworkVersion: String = XposedBridge.XPOSED_BRIDGE_VERSION.toString()
    override val frameworkVersionCode: Long = XposedBridge.getXposedVersion().toLong()

    override fun hookMethod(member: Member, callback: HookCallback, priority: Int): UnhookHandle =
        HandleAdapter(XposedBridge.hookMethod(member, XcAdapter(callback, priority)))

    override fun hookAllConstructors(clazz: Class<*>, callback: HookCallback, priority: Int): List<UnhookHandle> =
        XposedBridge.hookAllConstructors(clazz, XcAdapter(callback, priority)).map { HandleAdapter(it) }

    override fun invokeOriginalMethod(method: Method, thisObject: Any?, args: Array<Any?>): Any? =
        XposedBridge.invokeOriginalMethod(method, thisObject, args)

    override val isDeoptimizationSupported: Boolean = true

    override fun deoptimize(member: Member): Boolean = runCatching {
        deoptimizeMethod!!.invoke(null, member)
    }.isSuccess

    override fun log(msg: String) = XposedBridge.log(msg)

    override fun log(t: Throwable) = XposedBridge.log(t)

    /** [HookCallback] → [XC_MethodHook] 适配器（priority 直接透传，保持 82 原生排序语义） */
    private class XcAdapter(
        private val callback: HookCallback,
        priority: Int,
    ) : XC_MethodHook(priority) {
        override fun beforeHookedMethod(param: MethodHookParam) {
            callback.before(ParamAdapter(param))
        }

        override fun afterHookedMethod(param: MethodHookParam) {
            callback.after(ParamAdapter(param))
        }
    }

    /** [XC_MethodHook.MethodHookParam] → [HookParam]（result/throwable 直通映射） */
    private class ParamAdapter(private val param: XC_MethodHook.MethodHookParam) : HookParam {
        override val member: Member get() = param.method
        override val thisObject: Any? get() = param.thisObject
        override val args: Array<Any?> get() = param.args
        override var result: Any?
            get() = param.result
            set(value) {
                param.result = value
            }
        override var throwable: Throwable?
            get() = param.throwable
            set(value) {
                param.throwable = value
            }
    }

    private class HandleAdapter(private val handle: XC_MethodHook.Unhook) : UnhookHandle {
        override val member: Member get() = handle.hookedMethod
        override fun unhook() = handle.unhook()
    }

    companion object {
        /**
         * 82 框架的反优化方法（LSPosed 提供私有静态方法；原版 Xposed 无此方法时反射失败自然降级）。
         */
        private val deoptimizeMethod: Method? = runCatching {
            XposedBridge::class.java.getDeclaredMethod("deoptimizeMethod", Member::class.java)
                .apply { isAccessible = true }
        }.getOrNull()
    }
}
