package fansirsqi.xposed.sesame.hook.compat.lsp102

import android.util.Log
import fansirsqi.xposed.sesame.hook.compat.HookCallback
import fansirsqi.xposed.sesame.hook.compat.HookParam
import fansirsqi.xposed.sesame.hook.compat.Hooker
import fansirsqi.xposed.sesame.hook.compat.UnhookHandle
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import java.lang.reflect.Executable
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Member
import java.lang.reflect.Method

/**
 * libxposed API 102 后端（libxposed 允许区）：把 OkHttp 风格拦截器链适配为 before/after 双阶段，
 * 照 QAuxiliary DispatchAgent 模式。与 82 语义逐处对齐：
 * - before 设 result = 短路（不调 proceed，且跳过 after——与 82 returnEarly 一致）；
 * - 否则 proceed，原方法结果/异常写入 param 供 after 读取/篡改/吞掉；
 * - 回调自身抛异常按 82 语义记录并继续（82 中 XposedBridge 捕获并仅打日志）。
 */
class Lsp102Hooker(private val self: XposedModule) : Hooker {

    override val apiLevel: Int get() = self.apiVersion
    override val frameworkName: String get() = self.frameworkName
    override val frameworkVersion: String get() = self.frameworkVersion
    override val frameworkVersionCode: Long get() = self.frameworkVersionCode

    override fun hookMethod(member: Member, callback: HookCallback, priority: Int): UnhookHandle {
        val executable = member as? Executable
            ?: throw IllegalArgumentException("仅支持方法/构造器: $member")
        return HandleAdapter(
            self.hook(executable)
                .setPriority(priority)
                // PASSTHROUGH：确保 proceed 异常经本层重抛后能传播给调用方（PROTECTIVE 会吞掉）
                .setExceptionMode(XposedInterface.ExceptionMode.PASSTHROUGH)
                .intercept { chain -> dispatch(callback, chain) }
        )
    }

    override fun hookAllConstructors(clazz: Class<*>, callback: HookCallback, priority: Int): List<UnhookHandle> =
        clazz.declaredConstructors.map { hookMethod(it, callback, priority) }

    override fun invokeOriginalMethod(method: Method, thisObject: Any?, args: Array<Any?>): Any? = try {
        self.getInvoker(method).setType(XposedInterface.Invoker.Type.ORIGIN).invoke(thisObject, *args)
    } catch (e: InvocationTargetException) {
        throw (e.cause ?: e)
    }

    override val isDeoptimizationSupported: Boolean = true

    override fun deoptimize(member: Member): Boolean = runCatching {
        self.deoptimize(member as Executable)
    }.getOrDefault(false)

    override fun log(msg: String) = self.log(Log.INFO, TAG, msg)

    override fun log(t: Throwable) = self.log(Log.ERROR, TAG, t.message ?: "error", t)

    /** 拦截器链 → before/after 双阶段适配（DispatchAgent）。 */
    private fun dispatch(callback: HookCallback, chain: XposedInterface.Chain): Any? {
        val param = ChainParamAdapter(chain)

        // before 阶段（回调异常记录并继续，与 82 一致）
        try {
            callback.before(param)
        } catch (t: Throwable) {
            self.log(Log.ERROR, TAG, "before 回调异常: ${chain.executable}", t)
            param.resetAfterBeforeException()
        }
        if (param.resultAssigned) {
            // 短路：不执行原方法，也不跑 after（与 82 returnEarly 语义一致）
            return param.result
        }

        // 执行原方法（异常写入 param.throwable 供 after 读取/清除/替换）
        try {
            param.result = chain.proceed()
        } catch (t: Throwable) {
            param.throwable = t
        }

        // after 阶段（回调异常记录并忽略本次修改，与 82 一致）
        try {
            callback.after(param)
        } catch (t: Throwable) {
            self.log(Log.ERROR, TAG, "after 回调异常: ${chain.executable}", t)
        }

        if (param.throwable != null) throw param.throwable!!
        return param.result
    }

    /** [XposedInterface.Chain] → [HookParam]（args 为快照数组，102 语义：改参须经 chain.proceed(新参)） */
    private class ChainParamAdapter(private val chain: XposedInterface.Chain) : HookParam {
        var resultAssigned: Boolean = false
            private set

        private var mResult: Any? = null
        private var mThrowable: Throwable? = null

        override val member: Member get() = chain.executable
        override val thisObject: Any? get() = chain.thisObject
        override val args: Array<Any?> get() = chain.args.toTypedArray()

        override var result: Any?
            get() = mResult
            set(value) {
                mResult = value
                resultAssigned = true
            }

        override var throwable: Throwable?
            get() = mThrowable
            set(value) {
                mThrowable = value
            }

        /** before 回调抛异常时重置，避免其已写入的 result 被误判为短路 */
        fun resetAfterBeforeException() {
            resultAssigned = false
            mResult = null
            mThrowable = null
        }
    }

    private class HandleAdapter(private val handle: XposedInterface.HookHandle) : UnhookHandle {
        override val member: Member get() = handle.executable
        override fun unhook() = handle.unhook()
    }

    companion object {
        private const val TAG = "Sesame-TK"
    }
}
