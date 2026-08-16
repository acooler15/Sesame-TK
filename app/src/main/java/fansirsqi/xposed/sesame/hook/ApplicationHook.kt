package fansirsqi.xposed.sesame.hook

import android.annotation.SuppressLint
import android.app.Application
import android.app.Service
import android.content.Context
import android.content.pm.PackageInfo
import android.os.Handler
import android.os.Looper
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import fansirsqi.xposed.sesame.BuildConfig
import fansirsqi.xposed.sesame.SesameApplication
import fansirsqi.xposed.sesame.data.Config
import fansirsqi.xposed.sesame.data.General
import fansirsqi.xposed.sesame.data.Status
import fansirsqi.xposed.sesame.data.Status.Companion.load
import fansirsqi.xposed.sesame.entity.AlipayVersion
import fansirsqi.xposed.sesame.hook.Toast.show
import fansirsqi.xposed.sesame.hook.TokenHooker.start
import fansirsqi.xposed.sesame.hook.XposedEnv.processName
import fansirsqi.xposed.sesame.hook.internal.AlipayMiniMarkHelper
import fansirsqi.xposed.sesame.hook.internal.LocationHelper
import fansirsqi.xposed.sesame.hook.internal.AuthCodeHelper
import fansirsqi.xposed.sesame.hook.internal.SecurityBodyHelper
import fansirsqi.xposed.sesame.hook.keepalive.SmartSchedulerManager.cleanup
import fansirsqi.xposed.sesame.hook.rpc.bridge.NewRpcBridge
import fansirsqi.xposed.sesame.hook.rpc.bridge.OldRpcBridge
import fansirsqi.xposed.sesame.hook.rpc.bridge.RpcBridge
import fansirsqi.xposed.sesame.hook.rpc.intervallimit.GlobalRpcRateLimiter.clearIntervalLimit
import fansirsqi.xposed.sesame.hook.simple.SliderTFLite
import fansirsqi.xposed.sesame.hook.server.ModuleHttpServerManager.startIfNeeded
import fansirsqi.xposed.sesame.hook.simple.SimplePageManager.addHandler
import fansirsqi.xposed.sesame.hook.simple.SimplePageManager.enableWindowMonitoring
import fansirsqi.xposed.sesame.model.BaseModel.Companion.destroyData
import fansirsqi.xposed.sesame.model.Model
import fansirsqi.xposed.sesame.model.SesameConfig
import fansirsqi.xposed.sesame.task.MainTask.Companion.newInstance
import fansirsqi.xposed.sesame.task.ModelTask.Companion.stopAllTask
import fansirsqi.xposed.sesame.core.app.AssetUtil
import fansirsqi.xposed.sesame.core.app.AssetUtil.copyStorageSoFileToPrivateDir
import fansirsqi.xposed.sesame.core.app.AssetUtil.dexkitDestFile
import fansirsqi.xposed.sesame.core.app.AssetUtil.tfliteDestFile
import fansirsqi.xposed.sesame.core.app.AssetUtil.tfliteGpuDestFile
import fansirsqi.xposed.sesame.core.store.DataStore.init

import fansirsqi.xposed.sesame.core.app.Files
import fansirsqi.xposed.sesame.core.threads.GlobalThreadPools.shutdownAndRestart
import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.core.log.Log.printStackTrace
import fansirsqi.xposed.sesame.core.log.Log.record
import fansirsqi.xposed.sesame.core.app.ModuleStatus
import fansirsqi.xposed.sesame.core.notify.Notify
import fansirsqi.xposed.sesame.core.notify.Notify.stop
import fansirsqi.xposed.sesame.core.notify.Notify.updateStatusText
import fansirsqi.xposed.sesame.core.permission.PermissionUtil
import fansirsqi.xposed.sesame.core.permission.PermissionUtil.checkBatteryPermissions
import fansirsqi.xposed.sesame.core.app.StatusManager.updateStatus
import fansirsqi.xposed.sesame.util.maps.UserMap
import fansirsqi.xposed.sesame.util.maps.UserMap.currentUid
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import org.luckypray.dexkit.DexKitBridge
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Member
import java.lang.reflect.Method
import java.util.Calendar
import kotlin.concurrent.Volatile
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive

class ApplicationHook {
    var xposedInterface: XposedInterface? = null

    private object AlipayClasses {
        const val APPLICATION: String = "com.alipay.mobile.framework.AlipayApplication"
        const val SOCIAL_SDK: String = "com.alipay.mobile.personalbase.service.SocialSdkContactService"
        const val LAUNCHER_ACTIVITY: String = "com.alipay.mobile.quinox.LauncherActivity"
        const val SERVICE: String = "android.app.Service"
        const val LOADED_APK: String = "android.app.LoadedApk"
    }

    // --- 入口方法 ---
    fun loadPackage(lpparam: PackageLoadedParam) {
        if (General.PACKAGE_NAME != lpparam.packageName) return
        handleHookLogic(
            lpparam.classLoader,
            lpparam.packageName,
            lpparam.applicationInfo.sourceDir,
            lpparam
        )
    }

    fun loadPackageCompat(lpparam: LoadPackageParam) {
        if (General.PACKAGE_NAME != lpparam.packageName) return
        val apkPath: String = (if (lpparam.appInfo != null) lpparam.appInfo.sourceDir else null)!!
        handleHookLogic(lpparam.classLoader, lpparam.packageName, apkPath, lpparam)
    }

    @SuppressLint("PrivateApi")
    private fun handleHookLogic(loader: ClassLoader?, packageName: String, apkPath: String, rawParam: Any?) {
        classLoader = loader
        // 1. 初始化配置读取
        val prefs = XSharedPreferences(General.MODULE_PACKAGE_NAME, SesameApplication.PREFERENCES_KEY)
        prefs.makeWorldReadable()

        // 2. 进程检查
        resolveProcessName(rawParam)
        if (!shouldHookProcess()) return

        init(Files.CONFIG_DIR)
        if (isHooked) return
        isHooked = true

        // 3. 基础环境 Hook
        ModuleStatus.detectFramework(classLoader!!)
        updateStatus(ModuleStatus.detectFramework(classLoader!!), packageName)
        VersionHook.installHook(classLoader)
        initReflection(classLoader!!)

        // 4. 功能模块 Hook
        try {
            CaptchaHook.setupHook(classLoader!!)
        } catch (t: Throwable) {
            printStackTrace(TAG, "验证码Hook初始化失败", t)
        }

        // 5. WebView Hook
        if (config.webViewDebug.value) {
            try {
                WebViewHook.installHook(classLoader!!)
            } catch (t: Throwable) {
                printStackTrace(TAG, "WebView Hook初始化失败", t)
            }
        }

        // 6. 核心生命周期 Hook
        hookApplicationAttach(packageName)
        hookLauncherResume()
        hookServiceLifecycle(apkPath)

        HookUtil.hookOtherService(classLoader!!)
    }

    private fun resolveProcessName(rawParam: Any?) {
        if (rawParam is LoadPackageParam) {
            finalProcessName = rawParam.processName
        } else if (rawParam is PackageLoadedParam) {
            finalProcessName = processName
        }
    }

    private fun shouldHookProcess(): Boolean {
        val isMainProcess = General.PACKAGE_NAME == finalProcessName
        return isMainProcess
//            record(TAG, "跳过辅助进程: $finalProcessName")
    }

    private fun initReflection(loader: ClassLoader) {
        try {
            XposedHelpers.findClass(AlipayClasses.APPLICATION, loader)
            XposedHelpers.findClass(AlipayClasses.SOCIAL_SDK, loader)
        } catch (_: Throwable) {
            // ignore
        }

        try {
            @SuppressLint("PrivateApi") val loadedApkClass = loader.loadClass(AlipayClasses.LOADED_APK)
            deoptimizeClass(loadedApkClass)
        } catch (_: Throwable) {
            // ignore
        }
    }

    private fun hookApplicationAttach(packageName: String?) {
        try {
            XposedHelpers.findAndHookMethod(
                Application::class.java,
                "attach",
                Context::class.java,
                object : XC_MethodHook() {
                    @Throws(Throwable::class)
                    override fun afterHookedMethod(param: MethodHookParam) {
                        appContext = param.args[0] as Context?
                        mainHandler = Handler(Looper.getMainLooper())
                        Log.init(appContext!!)
                        TaskScheduler.ensureScheduler()

                        SecurityBodyHelper.init(classLoader!!)
                        AlipayMiniMarkHelper.init(classLoader!!)
                        LocationHelper.init(classLoader!!)
                        AuthCodeHelper.init(classLoader!!)
                        AuthCodeHelper.getAuthCode("2021005114632037" )

                        initVersionInfo(packageName)
                        loadLibs()

                        try {
                            HookUtil.hookAssetManagerForModel(classLoader!!)
                        } catch (t: Throwable) {
                            printStackTrace(TAG, "hookAssetManagerForModel 失败", t)
                        }
                        // 特殊版本处理
                        try {
                            if (VersionHook.hasVersion() && alipayVersion.compareTo(AlipayVersion("10.7.26.8100")) == 0) {
                                HookUtil.fuckAccounLimit(classLoader!!)
                            }
                        } catch (t: Throwable) {
                            printStackTrace(TAG, "fuckAccounLimit 失败", t)
                        }

                        try {
                            initSimplePageManager()
                        } catch (t: Throwable) {
                            printStackTrace(TAG, "initSimplePageManager 失败", t)
                        }

                        try {
                            SliderTFLite.preloadAsync(appContext!!)
                        } catch (t: Throwable) {
                            printStackTrace(TAG, "SliderTFLite 预加载失败", t)
                        }
                    }
                })
        } catch (e: Exception) {
            Log.printStackTrace(TAG, "Hook attach failed", e)
        }
    }

    private fun hookLauncherResume() {
        try {
            XposedHelpers.findAndHookMethod(
                AlipayClasses.LAUNCHER_ACTIVITY,
                classLoader,
                "onResume",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam?) {
                        val targetUid = HookUtil.getUserId(classLoader!!)
                        if (targetUid == null) {
                            show("用户未登录")
                            return
                        }
                        if (!init) {
                            if (initHandler()) init = true
                            return
                        }
                        val currentUid = currentUid
                        if (targetUid != currentUid) {
                            if (currentUid != null) {
                                initHandler()
                                TaskScheduler.lastExecTime = 0
                                show("用户已切换")
                                return
                            }
                            HookUtil.hookUser(classLoader!!)
                        }
                    }
                })
        } catch (t: Throwable) {
            printStackTrace(TAG, "Hook Launcher failed", t)
        }
    }

    private fun hookServiceLifecycle(apkPath: String) {
        try {
            XposedHelpers.findAndHookMethod(AlipayClasses.SERVICE, classLoader, "onCreate", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val appService = param.thisObject as Service
                    if (General.CURRENT_USING_SERVICE != appService.javaClass.getCanonicalName()) {
                        return
                    }

                    service = appService
                    appContext = appService.applicationContext
                    TaskScheduler.ensureScheduler()

                    DexKitBridge.create(apkPath).use { _ ->
                        record(TAG, "Hook DexKit successfully")
                    }
                    TaskScheduler.mainTask = newInstance("主任务") { TaskScheduler.runMainTaskLogic() }
                    TaskScheduler.dayCalendar = Calendar.getInstance()
                    if (initHandler()) {
                        init = true
                    }
                }
            })

            XposedHelpers.findAndHookMethod(AlipayClasses.SERVICE, classLoader, "onDestroy", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val s = param.thisObject as Service
                    if (General.CURRENT_USING_SERVICE == s.javaClass.getCanonicalName()) {
                        updateStatusText("目标应用前台服务被销毁")
                        destroyHandler()
                        BroadcastReceiverManager.restartByBroadcast()
                    }
                }
            })
        } catch (t: Throwable) {
            printStackTrace(TAG, "Hook Service failed", t)
        }
    }

    private fun initVersionInfo(packageName: String?) {
        if (VersionHook.hasVersion()) {
            alipayVersion = VersionHook.getCapturedVersion() ?: AlipayVersion("")
            record(TAG, "📦 目标应用版本(Hook): $alipayVersion")
        } else {
            try {
                val pInfo: PackageInfo = appContext!!.packageManager.getPackageInfo(packageName!!, 0)
                alipayVersion = AlipayVersion(pInfo.versionName.toString())
            } catch (_: Exception) {
                alipayVersion = AlipayVersion("")
            }
        }
    }

    private fun loadLibs() {
        loadNativeLibs(appContext!!, dexkitDestFile)
        loadNativeLibs(appContext!!, tfliteDestFile)
        loadNativeLibs(appContext!!, tfliteGpuDestFile)

        try {
            AssetUtil.copyStorageModelToPrivateDir(appContext!!, AssetUtil.sliderModelDestFile)
            if (AssetUtil.modelPrivateFile != null) {

                Log.record(TAG, "Model loaded: ${AssetUtil.modelPrivateFile?.absolutePath}")
            } else {
                Log.error(TAG, "Model load failed")
            }
        } catch (e: Exception) {
            Log.printStackTrace(TAG, "载入模型失败", e)
        }
    }

    // 滑块验证hook注册
    private fun initSimplePageManager() {
        record(TAG, "准备初始化 SimplePageManager，当前版本: $alipayVersion")
        if (shouldEnableSimplePageManager()) {
            record(TAG, "SimplePageManager 已启用，开始注册验证码页面处理器")
            enableWindowMonitoring(classLoader)
            addHandler("com.alipay.mobile.nebulax.xriver.activity.XRiverActivity", Captcha1Handler())
            addHandler("com.alipay.mobile.nebulax.xriver.activity.XRiverTransActivity\$Main", Captcha2Handler())
            addHandler("com.alipay.mobile.nebulax.integration.mpaas.activity.NebulaTransActivity\$Main", Captcha2Handler())
            record(TAG, "验证码页面处理器注册完成")
        } else {
            record(TAG, "SimplePageManager 未启用，跳过验证码页面处理器注册")
        }
    }

    @SuppressLint("UnsafeDynamicallyLoadedCode")
    private fun loadNativeLibs(context: Context, soFile: File) {
        try {
            val finalSoFile = copyStorageSoFileToPrivateDir(context, soFile)
            if (finalSoFile != null) {
                System.load(finalSoFile.absolutePath)
            } else {
                System.loadLibrary(soFile.getName().replace(".so", "").replace("lib", ""))
            }
        } catch (t: Throwable) {
            // 必须捕获 Throwable：System.load 抛 UnsatisfiedLinkError（Error），
            // 若逃逸会击穿 attach 回调导致 initSimplePageManager 等后续初始化全部跳过
            Log.printStackTrace(TAG, "载入so库失败: " + soFile.getName(), t)
        }
    }

    companion object {
        const val TAG: String = "ApplicationHook" // 简化TAG
        var finalProcessName: String? = ""

        /**
         * 全局配置对象，运行时统一读取配置（方案13重构）。
         */
        lateinit var config: SesameConfig
            private set

        /**
         * 全局应用协程作用域
         * 替代 GlobalScope，统一管理应用内长生命周期协程的生命周期。
         * 在 destroyHandler 中统一取消，切换账号时由 shutdownAndRestart 取消并重建。
         */
        @Volatile
        var applicationScope: CoroutineScope = CoroutineScope(
            SupervisorJob() + Dispatchers.Default + CoroutineName("Application")
        )

        var classLoader: ClassLoader? = null

        @Volatile
        var appContext: Context? = null

        var alipayVersion: AlipayVersion = AlipayVersion("")

        @Volatile
        var isHooked: Boolean = false
            private set

        /**
         * 检查目标应用版本是否需要启用SimplePageManager功能
         * @return true表示版本低于等于12.99.99.99999，需要启用；false表示不需要
         */
        fun shouldEnableSimplePageManager(): Boolean {
            if (!VersionHook.hasVersion() || alipayVersion.toString().isEmpty()) {
                record(TAG, "SimplePageManager 版本判断失败：未捕获到目标应用版本")
                return false
            }

            val maxSupported = AlipayVersion("12.99.99.99999")
            if (alipayVersion > maxSupported) {
                // 只有在不支持时才打印警告
                record(TAG, "目标应用版本 $alipayVersion 高于 $maxSupported，不支持自动过滑块验证")
                return false
            }

            record(TAG, "SimplePageManager 版本判断通过: $alipayVersion <= $maxSupported")
            return true
        }

        @Volatile
        internal var init = false

        @Volatile
        var offline: Boolean = false

        @Volatile
        private var batteryPermissionChecked = false

        @SuppressLint("StaticFieldLeak")
        var service: Service? = null

        var mainHandler: Handler? = null

        @Volatile
        var rpcBridge: RpcBridge? = null
        private val rpcBridgeLock = Any()

        // Deoptimize 方法缓存
        private val deoptimizeMethod: Method?

        init {
            config = SesameConfig()
            var m: Method? = null
            try {
                m = XposedBridge::class.java.getDeclaredMethod("deoptimizeMethod", Member::class.java)
            } catch (_: Throwable) {
            }
            deoptimizeMethod = m
        }

        // --- 委托方法（保持对外 API 兼容，实现见 TaskScheduler/BroadcastReceiverManager） ---
        var lastExecTime: Long
            get() = TaskScheduler.lastExecTime
            set(value) {
                TaskScheduler.lastExecTime = value
            }

        var nextExecutionTime: Long
            get() = TaskScheduler.nextExecutionTime
            set(value) {
                TaskScheduler.nextExecutionTime = value
            }

        fun updateDay() = TaskScheduler.updateDay()

        fun scheduleNextExecutionInternal(lastTime: Long) = TaskScheduler.scheduleNextExecutionInternal(lastTime)

        fun reOpenApp() = TaskScheduler.reOpenApp()

        fun execHandler() = TaskScheduler.execHandler()

        fun sendBroadcast(action: String?) = BroadcastReceiverManager.sendBroadcast(action)

        fun sendBroadcastShell(api: String?, message: String?) = BroadcastReceiverManager.sendBroadcastShell(api, message)

        fun reLoginByBroadcast() = BroadcastReceiverManager.reLoginByBroadcast()

        fun restartByBroadcast() = BroadcastReceiverManager.restartByBroadcast()

        fun registerBroadcastReceiver(context: Context) = BroadcastReceiverManager.registerBroadcastReceiver(context)

        fun unregisterBroadcastReceiver(context: Context?) = BroadcastReceiverManager.unregisterBroadcastReceiver(context)

        @Throws(InvocationTargetException::class, IllegalAccessException::class)
        fun deoptimizeClass(c: Class<*>) {
            if (deoptimizeMethod == null) return
            for (m in c.getDeclaredMethods()) {
                if (m.name == "makeApplicationInner") {
                    deoptimizeMethod.invoke(null, m)
                }
            }
        }

        // --- 初始化核心逻辑 ---
        @Synchronized
        internal fun initHandler(): Boolean {
            try {
                if (init) destroyHandler()

                // 账号切换/服务重建后 applicationScope 已被取消，重新初始化前需要重建为活跃作用域
                if (!applicationScope.isActive) {
                    applicationScope = CoroutineScope(
                        SupervisorJob() + Dispatchers.Default + CoroutineName("Application")
                    )
                }

                // 调试模式初始化
                if (BuildConfig.DEBUG) {
                    try {
                        startIfNeeded(8080, "ET3vB^#td87sQqKaY*eMUJXP", processName, General.PACKAGE_NAME)
                        BroadcastReceiverManager.registerBroadcastReceiver(appContext!!)
                    } catch (_: Throwable) { /* ignore */
                    }
                }

                TaskScheduler.ensureScheduler()
                Model.initAllModel()

                if (service == null) return false
                val userId = HookUtil.getUserId(classLoader!!)
                if (userId == null) {
                    show("用户未登录")
                    return false
                }

                HookUtil.hookUser(classLoader!!)
                record(TAG, "芝麻粒-TK 开始初始化...")

                Config.load(userId)
                if (!Config.isLoaded()) return false

                Notify.start(service!!)
                TaskScheduler.setWakenAtTimeAlarm()

                synchronized(rpcBridgeLock) {
                    rpcBridge = if (config.newRpc.value) NewRpcBridge() else OldRpcBridge()
                    rpcBridge!!.load()
                }

                if (config.newRpc.value && config.debugMode.value) {
                    HookUtil.hookRpcBridgeExtension(classLoader!!)
                    HookUtil.hookDefaultBridgeCallback(classLoader!!)
                }

                start(userId)
                checkBatteryPermission()

                Model.bootAllModel(classLoader)
                load(userId)
                TaskScheduler.updateDay()

                val successMsg = "Loaded SesameTk " + BuildConfig.VERSION_NAME + "✨"
                record(successMsg)
                show(successMsg)

                offline = false
                init = true
                TaskScheduler.execHandler()
                return true
            } catch (th: Throwable) {
                printStackTrace(TAG, "startHandler", th)
                return false
            }
        }

        private fun checkBatteryPermission() {
            if (!config.batteryPerm.value || batteryPermissionChecked) return

            val hasPermission = checkBatteryPermissions(appContext)
            batteryPermissionChecked = true
            if (!hasPermission) {
                record(TAG, "无后台运行权限，2秒后申请")
                mainHandler!!.postDelayed({
                    if (!PermissionUtil.checkOrRequestBatteryPermissions(appContext!!)) {
                        show("请授予目标应用始终在后台运行权限")
                    }
                }, 2000)
            }
        }

        @Synchronized
        fun destroyHandler() {
            try {
                // 先同步停止所有任务，确保在资源卸载前所有 taskScope 已取消，避免竞态
                stopAllTask()
                shutdownAndRestart()

                if (service != null) {
                    TaskScheduler.stopHandler()
                    destroyData()
                    Status.unload()
                    stop()
                    clearIntervalLimit()
                    Config.unload()
                    UserMap.unload()
                }

                cleanup()

                // 注销广播接收器
                BroadcastReceiverManager.unregisterBroadcastReceiver(appContext)

                synchronized(rpcBridgeLock) {
                    if (rpcBridge != null) {
                        rpcBridge!!.unload()
                        rpcBridge = null
                    }
                }

                // 最后统一取消 applicationScope
                applicationScope.cancel("Application destroyed")
            } catch (th: Throwable) {
                printStackTrace(TAG, "stopHandler err:", th)
            }
        }
    }
}
