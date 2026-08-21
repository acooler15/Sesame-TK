package fansirsqi.xposed.sesame.hook.view

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import android.view.View
import fansirsqi.xposed.sesame.hook.compat.HookCallback
import fansirsqi.xposed.sesame.hook.compat.HookParam
import fansirsqi.xposed.sesame.hook.compat.Hooker
import fansirsqi.xposed.sesame.hook.captcha.RpcPauseGate
import fansirsqi.xposed.sesame.core.log.Log as SesameLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

/**
 * 页面监视器：监控顶层 Activity 恢复与 Dialog 创建事件，
 * 并将页面分发给已注册的 [ActivityFocusHandler]（验证码处理器）处理。
 */
@SuppressLint("StaticFieldLeak")
object PageMonitor {

    private const val TAG = "PageMonitor"

    /** 验证码对话框类名（可弹在任意宿主 Activity 之上，路由时优先走 Dialog 处理器） */
    private const val CAPTCHA_DIALOG_CLASS = "com.alipay.rdssecuritysdk.v3.captcha.view.CaptchaDialog"

    private var mContextRef: WeakReference<Context>? = null
    private var mClassLoader: ClassLoader? = null
    private var topActivity: Activity? = null

    private val activityFocusHandlerMap = ConcurrentHashMap<String, ActivityFocusHandler>()

    /**
     * Dialog 兜底处理器：验证码类 Dialog（CaptchaDialog）弹出时优先路由到这里，
     * 不依赖顶层 Activity 类名（它可能弹在任意宿主 Activity 之上）。
     */
    private var dialogFocusHandler: ActivityFocusHandler? = null

    /** 处理器触发延迟（毫秒），等待页面布局稳定 */
    private const val TRIGGER_DELAY_MS = 100L

    private var hasPendingActivityTask = false

    private val dialogs = ArrayList<WeakReference<android.app.Dialog>>()
    private var windowMonitorEnabled = false

    enum class ActivityHandleResult {
        HANDLED,
        SKIP_NON_RETRYABLE,
        FAILED_RETRYABLE
    }

    interface ActivityFocusHandler {
        suspend fun handleActivity(activity: Activity, root: ViewImage): ActivityHandleResult
    }

    init {
        enablePageMonitor()
    }

    fun getContext(): Context? = mContextRef?.get()

    private fun getClassLoader(): ClassLoader? = mClassLoader

    /**
     * 参数类型解析：把 hookDialogConstructor 的松散参数（Class / 类名字符串）解析为具体类型。
     * 兼容基本类型名（如 "boolean"）与含 $ 的内联类名（如 "X$Y"）。
     */
    private fun resolveParamClass(param: Any, classLoader: ClassLoader?): Class<*> = when (param) {
        is Class<*> -> param
        is String -> when (param) {
            "boolean" -> java.lang.Boolean.TYPE
            "byte" -> java.lang.Byte.TYPE
            "char" -> java.lang.Character.TYPE
            "short" -> java.lang.Short.TYPE
            "int" -> java.lang.Integer.TYPE
            "long" -> java.lang.Long.TYPE
            "float" -> java.lang.Float.TYPE
            "double" -> java.lang.Double.TYPE
            "void" -> java.lang.Void.TYPE
            else -> {
                if (param.contains("$")) {
                    val parts = param.split("$")
                    val outerClass = Class.forName(parts[0], false, classLoader)
                    outerClass.declaredClasses.find { it.simpleName == parts[1] }
                        ?: throw ClassNotFoundException("Inner class ${parts[1]} not found in ${parts[0]}")
                } else {
                    Class.forName(param, false, classLoader)
                }
            }
        }
        else -> param.javaClass
    }

    fun addHandler(activityClassName: String, handler: ActivityFocusHandler) {
        activityFocusHandlerMap[activityClassName] = handler
    }

    /** 注册 Dialog 兜底处理器（验证码 Dialog 弹出时优先路由，与顶层 Activity 类名无关） */
    fun addDialogHandler(handler: ActivityFocusHandler) {
        dialogFocusHandler = handler
    }

    fun enableWindowMonitoring(classLoader: ClassLoader? = null) {
        if (classLoader != null) {
            mClassLoader = classLoader
        }
        Log.i(
            TAG,
            "启用窗口监控被调用，窗口监控已启用: $windowMonitorEnabled, 类加载器: ${mClassLoader?.javaClass?.name}"
        )
        SesameLog.record(TAG, "启用窗口监控被调用，窗口监控已启用: $windowMonitorEnabled")
        if (!windowMonitorEnabled) {
            enableWindowMonitor()
            windowMonitorEnabled = true
            SesameLog.record(TAG, "窗口监控初始化完成")
        }
    }

    /**
     * 尝试在对话框中查找视图
     */
    @SuppressLint("UseKtx")
    fun tryGetTopView(xpath: String): ViewImage? {
        Log.d(TAG, "tryGetTopView 搜索 xpath: $xpath, 对话框数量: ${dialogs.size}")
        dialogs.removeIf { it.get() == null }
        for (dialogWeakReference in dialogs) {
            val dialog = dialogWeakReference.get() ?: continue
            if (!dialog.isShowing) {
                continue
            }
            val decorView = dialog.window?.decorView ?: continue
            Log.d(TAG, "  - 对话框: ${dialog.javaClass.name}, 正在显示: ${dialog.isShowing}")
            debugPrintAllTextViews(decorView, 0)
            val viewImage = ViewImage(decorView)
            val results = XpathParser.evaluate(viewImage, xpath)
            if (results.isNotEmpty()) {
                return results[0]
            }
        }
        return null
    }

    /**
     * 打印所有 TextView 的文本内容（用于调试）
     */
    private fun debugPrintAllTextViews(view: View, depth: Int) {
        val indent = "  ".repeat(depth)
        if (view is android.widget.TextView) {
            val text = view.text?.toString() ?: ""
            val contentDesc = view.contentDescription?.toString() ?: ""
            if (text.isNotEmpty() || contentDesc.isNotEmpty()) {
                Log.d(
                    TAG,
                    "${indent}文本视图[${view.javaClass.simpleName}] 文本='$text' 内容描述='$contentDesc'"
                )
            }
        }
        if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                debugPrintAllTextViews(view.getChildAt(i), depth + 1)
            }
        }
    }

    /**
     * 启用 Activity 监控
     */
    private fun enablePageMonitor() {
        try {
            Hooker.get().hookMethod(
                Application::class.java.getDeclaredMethod("dispatchActivityResumed", Activity::class.java).apply { isAccessible = true },
                object : HookCallback {
                    override fun before(p: HookParam) {
                        topActivity = p.args[0] as Activity
                        SesameLog.record(TAG, "Activity resumed: ${topActivity?.javaClass?.name}")
                        if (mContextRef?.get() == null) {
                            mContextRef = WeakReference(topActivity?.applicationContext)
                        }
                        mClassLoader = topActivity?.classLoader
                        triggerActivity()
                    }
                }
            )
        } catch (e: Throwable) {
            Log.e(TAG, "挂钩 Activity->dispatchActivityResumed 错误: ", e)
        }
    }

    /**
     * 如果对话框不存在则添加到监控列表
     */
    /**
     * 如果对话框不存在则添加到监控列表
     * @return true=本次新增（已触发处理）；false=已存在（未触发）
     */
    private fun addDialogIfNotExists(dialog: android.app.Dialog, source: String): Boolean {
        if (!dialogs.any { it.get() === dialog }) {
            dialogs.add(WeakReference(dialog))
            Log.d(TAG, "对话框已从 $source 添加，总数: ${dialogs.size}")
            triggerDialogProcessing(dialog)
            return true
        }
        Log.d(TAG, "对话框从 $source 已存在于列表中")
        return false
    }

    /**
     * 挂钩对话框构造函数
     */
    private fun hookDialogConstructor(vararg parameterTypes: Any) {
        val parameterTypesString = parameterTypes.joinToString(",") {
            if (it is Class<*>) it.simpleName else it.toString()
        }
        try {
            val paramClasses = parameterTypes.map { resolveParamClass(it, getClassLoader()) }.toTypedArray()
            val constructor = android.app.Dialog::class.java.getDeclaredConstructor(*paramClasses).apply { isAccessible = true }
            Hooker.get().hookMethod(
                constructor,
                object : HookCallback {
                    override fun after(p: HookParam) {
                        val dialog = p.thisObject as android.app.Dialog
                        addDialogIfNotExists(dialog, "构造函数($parameterTypesString)")
                    }
                }
            )
           // Log.i(TAG, "挂钩对话框构造函数($parameterTypesString) 成功")
        } catch (e: Throwable) {
            Log.e(TAG, "挂钩对话框构造函数($parameterTypesString) 错误: ", e)
        }
    }

    /**
     * 启用对话框监控
     */
    private fun enableWindowMonitor() {
        Log.i(TAG, "启用窗口监控被调用，类加载器: ${mClassLoader?.javaClass?.name}")
        hookDialogConstructor("android.content.Context")
        hookDialogConstructor("android.content.Context", Int::class.java)
        hookDialogConstructor(
            "android.content.Context",
            "boolean",
            "android.content.DialogInterface.OnCancelListener"
        )

        try {
            val captchaDialogClass = Class.forName(
                CAPTCHA_DIALOG_CLASS,
                false,
                getClassLoader()
            )
            Hooker.get().hookMethod(
                captchaDialogClass.getDeclaredMethod("show").apply { isAccessible = true },
                object : HookCallback {
                    override fun after(p: HookParam) {
                        val dialog = p.thisObject as android.app.Dialog
                        val newlyAdded = addDialogIfNotExists(dialog, "CaptchaDialog.show()")
                        // 构造函数钩子可能已提前触发过一次处理（彼时 Dialog 尚未 show，
                        // 锚点查不到会被 SKIP_NON_RETRYABLE 终止），show() 时视图已挂载，
                        // 无论是否去重命中都强制再路由一次；与构造期任务并发由
                        // hasPendingActivityTask / 处理窗口互斥锁兜底。
                        if (!newlyAdded) {
                            triggerDialogProcessing(dialog)
                        }
                    }
                }
            )
        } catch (e: Throwable) {
            Log.e(TAG, "挂钩 CaptchaDialog.show() 错误: ", e)
        }
    }

    /**
     * 触发 Activity 处理
     */
    private fun triggerActivity() {
        triggerPendingActivityHandler("Activity 已恢复")
    }

    /**
     * 触发 Dialog 处理：验证码 Dialog（CaptchaDialog）优先路由 Dialog 兜底处理器，
     * 其余 Dialog 维持原有按顶层 Activity 类名路由。
     */
    private fun triggerDialogProcessing(dialog: android.app.Dialog) {
        val preferDialogHandler = dialog.javaClass.name == CAPTCHA_DIALOG_CLASS
        triggerPendingActivityHandler("Dialog 已创建", preferDialogHandler)
    }

    /**
     * 触发待处理的 Activity 处理器
     *
     * @param preferDialogHandler true=优先使用 Dialog 兜底处理器（验证码 Dialog 弹在任意宿主
     * Activity 上都能被处理，且覆盖底下 H5 页面自己的处理器——此时可见的验证码是 Dialog 里的）
     */
    private fun triggerPendingActivityHandler(source: String, preferDialogHandler: Boolean = false) {
        val activity = topActivity ?: run {
            Log.i(TAG, "无法从 $source 触发处理器，未找到顶层 Activity")
            SesameLog.record(TAG, "无法从 $source 触发处理器，未找到顶层 Activity")
            return
        }
        val handler = if (preferDialogHandler) {
            dialogFocusHandler ?: activityFocusHandlerMap[activity.javaClass.name]
        } else {
            activityFocusHandlerMap[activity.javaClass.name]
        }
        if (handler == null) {
            Log.d(TAG, "未找到 ${activity.javaClass.name} 的处理器，来源: $source")
            SesameLog.record(TAG, "未找到 ${activity.javaClass.name} 的处理器，来源: $source")
            return
        }
        if (hasPendingActivityTask) {
            Log.d(TAG, "跳过从 $source 触发，已有待处理任务")
            SesameLog.record(TAG, "跳过从 $source 触发，已有待处理任务")
            return
        }
        hasPendingActivityTask = true
        Log.i(TAG, "从 $source 触发 ${activity.javaClass.name} 的处理器，延迟: ${TRIGGER_DELAY_MS}ms")
        SesameLog.record(TAG, "从 $source 触发 ${activity.javaClass.name} 的处理器，延迟: ${TRIGGER_DELAY_MS}ms")
        triggerActivityActive(activity, handler, 0)
    }

    /**
     * 延迟触发 Activity 处理
     */
    private fun triggerActivityActive(
        activity: Activity,
        activityFocusHandler: ActivityFocusHandler,
        triggerCount: Int
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            delay(TRIGGER_DELAY_MS)
            try {
                hasPendingActivityTask = false
                val startTime = System.currentTimeMillis()
                Log.d(TAG, "开始执行验证码处理器，第${triggerCount + 1}次尝试")
                SesameLog.record(TAG, "开始执行验证码处理器，第${triggerCount + 1}次尝试")
                val result = activityFocusHandler.handleActivity(activity, ViewImage(activity.window.decorView))
                val endTime = System.currentTimeMillis()
                Log.d(TAG, "验证码处理器执行完成，耗时: ${endTime - startTime}ms, 结果: $result")
                SesameLog.record(TAG, "验证码处理器执行完成，耗时: ${endTime - startTime}ms, 结果: $result")
                when (result) {
                    ActivityHandleResult.HANDLED -> {
                        // 验证码处理成功，放行暂停闸门，恢复后续 RPC 请求
                        RpcPauseGate.onCaptchaHandled("handler 处理成功(${activity.javaClass.simpleName})")
                        return@launch
                    }

                    ActivityHandleResult.SKIP_NON_RETRYABLE -> {
                        SesameLog.record(TAG, "precheck-skip-non-retryable: ${activity.javaClass.name}, stop retry loop")
                        // 判定非验证码页（闸门误激活），放行避免 RPC 死等超时
                        RpcPauseGate.onCaptchaHandled("handler 判定非验证码页(${activity.javaClass.simpleName})")
                        return@launch
                    }

                    ActivityHandleResult.FAILED_RETRYABLE -> {
                        SesameLog.record(TAG, "[处理失败·可重试] ${activity.javaClass.name}，重试次数=${triggerCount + 1}")
                    }
                }
            } catch (throwable: Throwable) {
                Log.e(TAG, "处理 Activity 出错: ${activity.javaClass.name}", throwable)
                SesameLog.record(TAG, "处理 Activity 出错: ${activity.javaClass.name}: ${throwable.message}")
            }
            
            // 限制重试次数并增加重试间隔
            if (triggerCount >= 3) {  // 从10次减少到3次
                Log.w(TAG, "Activity 事件触发失败次数过多(${triggerCount + 1}次)，停止重试")
                SesameLog.record(TAG, "Activity 事件触发失败次数过多(${triggerCount + 1}次)，停止重试")
                return@launch
            }
            
            // 递增重试延迟：100ms -> 200ms -> 300ms
            val retryDelay = (triggerCount + 1) * 100L
            Log.d(TAG, "第${triggerCount + 1}次处理失败，${retryDelay}ms后重试")
            SesameLog.record(TAG, "第${triggerCount + 1}次处理失败，${retryDelay}ms后重试")
            delay(retryDelay)
            triggerActivityActive(activity, activityFocusHandler, triggerCount + 1)
        }
    }
}
