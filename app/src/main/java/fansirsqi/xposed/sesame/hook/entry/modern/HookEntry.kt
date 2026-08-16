package fansirsqi.xposed.sesame.hook.entry.modern

import fansirsqi.xposed.sesame.data.General
import fansirsqi.xposed.sesame.hook.ApplicationHook
import fansirsqi.xposed.sesame.hook.XposedEnv
import fansirsqi.xposed.sesame.hook.compat.Hooker
import fansirsqi.xposed.sesame.hook.compat.lsp102.Lsp102Hooker
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface

/**
 * libxposed API 102 现代入口：框架以无参构造实例化本类，经 attachFramework 注入环境，
 * 由 META-INF/xposed/java_init.list 指向本类。
 *
 * 职责：
 * 1. onModuleLoaded：注入 102 后端（[Hooker.install] 传入 [Lsp102Hooker]），写入框架信息到 [XposedEnv]；
 * 2. onPackageLoaded：hook 注册留在本回调（须抢在 Application 创建前 hook Application.attach）；
 * 3. 热重载回调保留实现（module.prop 未开启 autoHotReload，暂不生效，留待后续迭代）。
 */
class HookEntry : XposedModule() {

    private val tag = "LsposedEntry"
    private var processName = "unknown"

    override fun onModuleLoaded(param: XposedModuleInterface.ModuleLoadedParam) {
        processName = param.processName
        // 注入 102 后端（双入口互斥，仅允许一次注入）
        if (Hooker.getOrNull() == null) {
            Hooker.install(Lsp102Hooker(this))
        }
        val hooker = Hooker.get()

        XposedEnv.apiLevel = hooker.apiLevel
        XposedEnv.frameworkName = hooker.frameworkName
        XposedEnv.frameworkVersion = hooker.frameworkVersion
        XposedEnv.frameworkVersionCode = hooker.frameworkVersionCode

        hooker.log("$tag: Initialized for process $processName")
    }

    override fun onPackageLoaded(param: XposedModuleInterface.PackageLoadedParam) {
        try {
            if (General.PACKAGE_NAME != param.packageName) return

            XposedEnv.classLoader = param.defaultClassLoader
            XposedEnv.appInfo = param.applicationInfo
            XposedEnv.packageName = param.packageName
            XposedEnv.processName = processName

            ApplicationHook().loadPackage(
                param.defaultClassLoader,
                param.packageName,
                param.applicationInfo.sourceDir
            )
            Hooker.getOrNull()?.log("$tag: Hooking ${param.packageName} in process $processName")
        } catch (t: Throwable) {
            Hooker.getOrNull()?.log("$tag: Hook failed - ${t.message}")
            Hooker.getOrNull()?.log(t)
        }
    }

    override fun onHotReloading(param: XposedModuleInterface.HotReloadingParam): Boolean {
        Hooker.getOrNull()?.log("$tag: Hot reloading...")
        // 暂不支持热重载（DexKit/TFLite native 库须自行退役旧状态，风险后置）
        return false
    }

    override fun onHotReloaded(param: XposedModuleInterface.HotReloadedParam) {
        Hooker.getOrNull()?.log("$tag: Hot reloaded")
    }
}
