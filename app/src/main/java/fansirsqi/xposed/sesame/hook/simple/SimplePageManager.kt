package fansirsqi.xposed.sesame.hook.simple

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import fansirsqi.xposed.sesame.hook.simple.xpcompat.CompatHelpers
import fansirsqi.xposed.sesame.util.Log as SesameLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

/**
 * A simplified PageManager - only keeps Activity monitoring and Dialog tracking.
 */
@SuppressLint("StaticFieldLeak")
object SimplePageManager {

    private const val TAG = "SimplePageManager"

    private var mContextRef: WeakReference<Context>? = null
    private var mClassLoader: ClassLoader? = null
    private var topActivity: Activity? = null

    private val activityFocusHandlerMap = ConcurrentHashMap<String, ActivityFocusHandler>()

    val handler = Handler(Looper.getMainLooper())

    private var taskDuration = 100  // 减少延迟时间，提高响应速度
    private var hasPendingActivityTask = false
    private var disable = false

    private val dialogs = ArrayList<WeakReference<android.app.Dialog>>()
    private var windowMonitorEnabled = false

    enum class ActivityHandleResult {
        HANDLED,
        SKIP_NON_RETRYABLE,
        FAILED_RETRYABLE
    }

    interface ActivityFocusHandler {
        suspend fun handleActivity(activity: Activity, root: SimpleViewImage): ActivityHandleResult
    }

    init {
        enablePageMonitor()
    }

    fun getContext(): Context? = mContextRef?.get()

    fun getClassLoader(): ClassLoader? = mClassLoader

    fun getTopActivity(): Activity? = topActivity

    fun setTaskDuration(duration: Int) {
        taskDuration = duration
    }

    fun setDisable(disabled: Boolean) {
        disable = disabled
    }

    fun addHandler(activityClassName: String, handler: ActivityFocusHandler) {
        activityFocusHandlerMap[activityClassName] = handler
    }

    fun removeHandler(activityClassName: String) {
        activityFocusHandlerMap.remove(activityClassName)
    }

    fun getDialogs(): ArrayList<WeakReference<android.app.Dialog>> = dialogs

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
    fun tryGetTopView(xpath: String): SimpleViewImage? {
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
            val viewImage = SimpleViewImage(decorView)
            val results = SimpleXpathParser.evaluate(viewImage, xpath)
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
            CompatHelpers.findAndHookMethod(
                Application::class.java,
                "dispatchActivityResumed",
                Activity::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        topActivity = param.args[0] as Activity
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
    private fun addDialogIfNotExists(dialog: android.app.Dialog, source: String) {
        if (!dialogs.any { it.get() === dialog }) {
            dialogs.add(WeakReference(dialog))
            Log.d(TAG, "对话框已从 $source 添加，总数: ${dialogs.size}")
            triggerDialogProcessing()
        } else {
            Log.d(TAG, "对话框从 $source 已存在于列表中")
        }
    }

    /**
     * 挂钩对话框构造函数
     */
    private fun hookDialogConstructor(vararg parameterTypes: Any) {
        val parameterTypesString = parameterTypes.joinToString(",") {
            if (it is Class<*>) it.simpleName else it.toString()
        }
        try {
            CompatHelpers.findAndHookConstructor(
                "android.app.Dialog",
                getClassLoader(),
                *parameterTypes,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val dialog = param.thisObject as android.app.Dialog
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
            val captchaDialogClass = XposedHelpers.findClass(
                "com.alipay.rdssecuritysdk.v3.captcha.view.CaptchaDialog",
                getClassLoader()
            )
            CompatHelpers.findAndHookMethod(
                captchaDialogClass,
                "show",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val dialog = param.thisObject as android.app.Dialog
                        addDialogIfNotExists(dialog, "CaptchaDialog.show()")
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
     * 触发 Dialog 处理
     */
    private fun triggerDialogProcessing() {
        triggerPendingActivityHandler("Dialog 已创建")
    }

    /**
     * 触发待处理的 Activity 处理器
     */
    private fun triggerPendingActivityHandler(source: String) {
        val activity = topActivity ?: run {
            Log.i(TAG, "无法从 $source 触发处理器，未找到顶层 Activity")
            SesameLog.record(TAG, "无法从 $source 触发处理器，未找到顶层 Activity")
            return
        }
        val handler = activityFocusHandlerMap[activity.javaClass.name]
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
        Log.i(TAG, "从 $source 触发 ${activity.javaClass.name} 的处理器，延迟: ${taskDuration}ms")
        SesameLog.record(TAG, "从 $source 触发 ${activity.javaClass.name} 的处理器，延迟: ${taskDuration}ms")
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
        if (disable) {
            Log.i(TAG, "页面触发管理器已禁用")
            return
        }
        CoroutineScope(Dispatchers.Main).launch {
            delay(taskDuration.toLong())
            try {
                hasPendingActivityTask = false
                val startTime = System.currentTimeMillis()
                Log.d(TAG, "开始执行验证码处理器，第${triggerCount + 1}次尝试")
                SesameLog.record(TAG, "开始执行验证码处理器，第${triggerCount + 1}次尝试")
                val result = activityFocusHandler.handleActivity(activity, SimpleViewImage(activity.window.decorView))
                val endTime = System.currentTimeMillis()
                Log.d(TAG, "验证码处理器执行完成，耗时: ${endTime - startTime}ms, 结果: $result")
                SesameLog.record(TAG, "验证码处理器执行完成，耗时: ${endTime - startTime}ms, 结果: $result")
                when (result) {
                    ActivityHandleResult.HANDLED -> {
                        return@launch
                    }

                    ActivityHandleResult.SKIP_NON_RETRYABLE -> {
                        SesameLog.record(TAG, "precheck-skip-non-retryable: ${activity.javaClass.name}, stop retry loop")
                        return@launch
                    }

                    ActivityHandleResult.FAILED_RETRYABLE -> {
                        SesameLog.record(TAG, "captcha-processing-failed-retryable: ${activity.javaClass.name}, retryCount=${triggerCount + 1}")
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
