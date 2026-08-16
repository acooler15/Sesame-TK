package fansirsqi.xposed.sesame.hook.compat

import java.lang.reflect.Member
import java.lang.reflect.Method

/**
 * Hook 调用参数抽象（Xposed 82 / libxposed 102 语义交集）。
 *
 * 语义约定：
 * - [result]：before 阶段设值 = 短路原方法（不执行原方法）；after 阶段设值 = 篡改返回值；
 * - [throwable]：after 阶段可读/清除（置 null 吞掉原方法异常）；设值 = 替换异常；
 * - [args]：before 阶段可改参（82 后端直接改 MethodHookParam.args，102 后端改快照数组）。
 */
interface HookParam {
    /** 被 hook 的方法/构造器 */
    val member: Member
    /** 调用对象（静态方法为 null） */
    val thisObject: Any?
    /** 参数数组（before 可改） */
    val args: Array<Any?>
    /** 返回值：before 设值=短路；after 设值=篡改 */
    var result: Any?
    /** 异常：after 可读/清除/替换 */
    var throwable: Throwable?
}

/** 业务侧回调：before/after 双阶段（缺省空实现） */
interface HookCallback {
    fun before(p: HookParam) {}
    fun after(p: HookParam) {}
}

/** hook 句柄（取消注册） */
interface UnhookHandle {
    val member: Member
    fun unhook()
}

/**
 * 项目自有 hook 抽象层：封装框架相关能力（hook 注册 / 原方法调用 / 反优化 / 框架日志 / 框架信息），
 * 与具体框架解耦。由入口在启动时 [install] 注入 82 或 102 后端，业务代码一律经 [get] 使用。
 *
 * 框架无关能力（找类/找方法/字符串反射）不进本层，直接用 JDK / [fansirsqi.xposed.sesame.core.reflect.ReflectUtil]。
 */
interface Hooker {
    // ---------- 框架信息 ----------
    val apiLevel: Int
    val frameworkName: String
    val frameworkVersion: String
    val frameworkVersionCode: Long

    // ---------- hook 注册（priority 越大越先执行） ----------
    fun hookMethod(member: Member, callback: HookCallback, priority: Int = PRIORITY_DEFAULT): UnhookHandle
    fun hookAllConstructors(clazz: Class<*>, callback: HookCallback, priority: Int = PRIORITY_DEFAULT): List<UnhookHandle>

    // ---------- 原方法调用 ----------
    fun invokeOriginalMethod(method: Method, thisObject: Any?, args: Array<Any?>): Any?

    // ---------- 反优化（可选能力） ----------
    val isDeoptimizationSupported: Boolean
    fun deoptimize(member: Member): Boolean

    // ---------- 框架日志域（勿改项目 Log.record——那是 slf4j/logback 文件日志） ----------
    fun log(msg: String)
    fun log(t: Throwable)

    companion object {
        const val PRIORITY_DEFAULT = 50
        const val PRIORITY_LOWEST = -10000
        const val PRIORITY_HIGHEST = 10000

        @Volatile
        private var impl: Hooker? = null

        /** 由入口注入后端，仅允许一次 */
        fun install(r: Hooker) {
            check(impl == null) { "Hooker 已初始化，禁止重复注入" }
            impl = r
        }

        /** 获取已注入的后端（未注入时抛错） */
        fun get(): Hooker = impl ?: error("Hooker 未初始化：须由入口在 handleLoadPackage/onModuleLoaded 注入")

        /** 获取已注入的后端（未注入时返回 null） */
        fun getOrNull(): Hooker? = impl
    }
}
