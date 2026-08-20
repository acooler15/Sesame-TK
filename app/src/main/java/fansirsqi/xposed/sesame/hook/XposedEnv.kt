package fansirsqi.xposed.sesame.hook

import android.content.pm.ApplicationInfo


/**
 * 模块运行环境（由双入口在启动时注入）。
 *
 * 框架信息字段（apiLevel / frameworkName / frameworkVersion / frameworkVersionCode）
 * 由 82 入口（LegacyEntry）与 102 入口（modern.HookEntry）在启动时各自写入，
 * 供 ModuleStatus 等框架识别逻辑使用，避免业务代码直接探测框架类。
 */
object XposedEnv {
    lateinit var classLoader: ClassLoader
    lateinit var appInfo: ApplicationInfo
    lateinit var packageName: String
    lateinit var processName: String

    // 框架信息（双入口注入）
    var apiLevel: Int = 0
    var frameworkName: String = ""
    var frameworkVersion: String = ""
    var frameworkVersionCode: Long = 0
}
