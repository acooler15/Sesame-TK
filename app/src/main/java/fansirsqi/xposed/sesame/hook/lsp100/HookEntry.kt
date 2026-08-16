package fansirsqi.xposed.sesame.hook.lsp100

import de.robv.android.xposed.XposedBridge
import fansirsqi.xposed.sesame.data.General
import fansirsqi.xposed.sesame.hook.ApplicationHook
import fansirsqi.xposed.sesame.hook.XposedEnv
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface

/**
 * libxposed API 101 入口：框架以无参构造实例化本类，通过 onModuleLoaded 回调注入环境。
 */
class HookEntry : XposedModule() {
    val tag = "LsposedEntry"
    private var processName = "unknown"
    var customHooker: ApplicationHook? = null

    override fun onModuleLoaded(param: XposedModuleInterface.ModuleLoadedParam) {
        processName = param.processName
        customHooker = ApplicationHook()
        // 模块自身即框架接口包装（attachFramework 后可直接使用），传递给逻辑核心
        customHooker?.xposedInterface = this
        XposedBridge.log("$tag: Initialized for process $processName")

        val baseFw = "$frameworkName $frameworkVersion $frameworkVersionCode target_model_process: ${moduleApplicationInfo.processName}"
        XposedBridge.log("LspEntry: Framework from base: $baseFw ")
    }

    /**
     * 当模块作用域内的应用进程启动时，框架会回调此方法。
     */
    override fun onPackageLoaded(param: XposedModuleInterface.PackageLoadedParam) {
        try {
            if (General.PACKAGE_NAME != param.packageName) return
            XposedEnv.classLoader = param.defaultClassLoader
            XposedEnv.appInfo = param.applicationInfo
            XposedEnv.packageName = param.packageName
            XposedEnv.processName = processName
            customHooker?.loadPackage(param)
            XposedBridge.log("$tag: Hooking ${param.packageName} in process $processName")
        } catch (e: Throwable) {
            XposedBridge.log("$tag: Hook failed - ${e.message}")
            XposedBridge.log(e)
        }
    }
}
