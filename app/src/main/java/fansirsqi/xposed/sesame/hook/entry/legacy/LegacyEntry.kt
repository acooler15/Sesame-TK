package fansirsqi.xposed.sesame.hook.entry.legacy

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage
import fansirsqi.xposed.sesame.data.General
import fansirsqi.xposed.sesame.hook.ApplicationHook
import fansirsqi.xposed.sesame.hook.XposedEnv
import fansirsqi.xposed.sesame.hook.compat.Hooker
import fansirsqi.xposed.sesame.hook.compat.xp82.Xp82Hooker

/**
 * legacy（Xposed API 82）入口：原版 Xposed / EdXposed / 旧版 LSPosed 经 assets/xposed_init 指向本类。
 *
 * 职责：
 * 1. 注入 82 后端（[Hooker.install][Hooker.install] 传入 [Xp82Hooker]）；
 * 2. 写入框架信息到 [XposedEnv]，供框架识别逻辑使用；
 * 3. 进入统一初始化链路 [ApplicationHook.loadPackage]（参数仅框架无关类型）。
 */
class LegacyEntry : IXposedHookLoadPackage {

    private val tag = "LegacyEntry"

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            // 只在目标应用执行
            if (lpparam.packageName != General.PACKAGE_NAME) {
                return
            }

            // 注入 82 后端（双入口互斥，仅允许一次注入）
            if (Hooker.getOrNull() == null) {
                Hooker.install(Xp82Hooker())
            }
            val hooker = Hooker.get()

            XposedEnv.classLoader = lpparam.classLoader
            XposedEnv.appInfo = lpparam.appInfo
            XposedEnv.packageName = lpparam.packageName
            XposedEnv.processName = lpparam.processName
            XposedEnv.apiLevel = hooker.apiLevel
            XposedEnv.frameworkName = hooker.frameworkName
            XposedEnv.frameworkVersion = hooker.frameworkVersion
            XposedEnv.frameworkVersionCode = hooker.frameworkVersionCode

            hooker.log("$tag: Hooking ${lpparam.packageName} in process ${lpparam.processName}")
            ApplicationHook().loadPackage(
                lpparam.classLoader,
                lpparam.packageName,
                lpparam.appInfo?.sourceDir ?: ""
            )
        } catch (t: Throwable) {
            Hooker.getOrNull()?.log("$tag: Hook failed - ${t.message}")
            Hooker.getOrNull()?.log(t)
        }
    }
}
