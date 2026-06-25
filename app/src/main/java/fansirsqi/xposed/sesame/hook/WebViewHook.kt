package fansirsqi.xposed.sesame.hook

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.Log.record
import java.util.concurrent.ConcurrentHashMap

/**
 * WebView Hook - 拦截 XRiver/WebView 场景中的网络请求 URL 和页面 HTML 内容
 *
 * 基于 XRiverTransActivity 的布局层级分析：
 *   FrameLayout → ... → h5_trans_web_content → h5_trans_progress_rl →
 *     com.alipay.mywebview.sdk.WebView (支付宝封装, FrameLayout子类)
 *       └── android.webkit.WebView (系统WebView, 内嵌子View)
 *
 * Hook 策略 (双层级 loadUrl + 动态子类 Hook):
 *   1. Hook 两个层级的 loadUrl(String) / loadUrl(String, Map) → 捕获 URL
 *   2. 在 loadUrl 的 afterHookedMethod 中，通过 getWebViewClient() 获取实例并动态 hook
 *   3. 基类 WebViewClient.onPageFinished 作为 fallback
 *   4. 原因: setWebViewClient 在 Activity 初始化时调用，早于 Xposed hook 安装时机
 */
object WebViewHook {

    private const val TAG = "WebViewHook"

    /** 支付宝封装 WebView 类名 */
    private const val ALIPAY_WEBVIEW = "com.alipay.mywebview.sdk.WebView"

    /** 系统 WebView 类名 */
    private const val SYSTEM_WEBVIEW = "android.webkit.WebView"

    private var isInitialized = false

    /** 已 Hook 的 WebViewClient 子类集合，避免重复 Hook */
    private val hookedClientClasses = ConcurrentHashMap.newKeySet<Class<*>>()

    /**
     * 安装 WebView Hook
     */
    fun installHook(classLoader: ClassLoader) {
        if (isInitialized) {
            return
        }

        try {
            record(TAG, "开始安装WebView Hook")

            // --- URL 捕获 (双层级) ---
            // 支付宝封装层: com.alipay.mywebview.sdk.WebView
            hookLoadUrl(ALIPAY_WEBVIEW, classLoader, "[AlipayWV]")
            hookLoadUrlWithHeaders(ALIPAY_WEBVIEW, classLoader, "[AlipayWV]")

            // 系统层: android.webkit.WebView (兜底 + 内部调用)
            hookLoadUrl(SYSTEM_WEBVIEW, classLoader, "[SysWV]")
            hookLoadUrlWithHeaders(SYSTEM_WEBVIEW, classLoader, "[SysWV]")

            // --- 基类 onPageFinished fallback ---
            // 兜底: 如果子类没有 override onPageFinished，基类 hook 仍会触发
            hookBaseOnPageFinished()

            // --- 已加载页面尝试提取 HTML (兜底) ---
            hookEvaluateJavascript(classLoader)

            isInitialized = true
            record(TAG, "WebView Hook安装完成")
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "WebView Hook安装失败", t)
        }
    }

    // ============================================================
    // 1. Hook loadUrl(String) — 记录 URL 并在回调中动态 hook WebViewClient
    // ============================================================
    private fun hookLoadUrl(className: String, classLoader: ClassLoader, label: String) {
        try {
            XposedHelpers.findAndHookMethod(
                className,
                classLoader,
                "loadUrl",
                String::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            val url = param.args[0] as? String ?: return
                            if (url.startsWith("javascript:")) return
                            record(TAG, "$label loadUrl: $url")
                        } catch (t: Throwable) {
                            Log.printStackTrace(TAG, "$label loadUrl 回调异常", t)
                        }
                    }

                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            val url = param.args[0] as? String ?: return
                            if (url.startsWith("javascript:")) return
                            // 从 WebView 实例获取已设置的 WebViewClient 并动态 hook
                            tryHookWebViewClientFromWebView(param.thisObject, label)
                        } catch (t: Throwable) {
                            Log.printStackTrace(TAG, "$label loadUrl afterHook 异常", t)
                        }
                    }
                }
            )
            record(TAG, "$label loadUrl(String) Hook 安装成功")
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "$label loadUrl(String) Hook 失败", t)
        }
    }

    // ============================================================
    // 2. Hook loadUrl(String, Map) — 同上
    // ============================================================
    private fun hookLoadUrlWithHeaders(className: String, classLoader: ClassLoader, label: String) {
        try {
            XposedHelpers.findAndHookMethod(
                className,
                classLoader,
                "loadUrl",
                String::class.java,
                Map::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            val url = param.args[0] as? String ?: return
                            if (url.startsWith("javascript:")) return
                            val headers = param.args[1] as? Map<*, *>
                            val headerStr = headers?.entries?.joinToString(", ") { "${it.key}=${it.value}" } ?: "null"
                            record(TAG, "$label loadUrl(Header): $url | headers={$headerStr}")
                        } catch (t: Throwable) {
                            Log.printStackTrace(TAG, "$label loadUrl(Header) 回调异常", t)
                        }
                    }

                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            val url = param.args[0] as? String ?: return
                            if (url.startsWith("javascript:")) return
                            tryHookWebViewClientFromWebView(param.thisObject, label)
                        } catch (t: Throwable) {
                            Log.printStackTrace(TAG, "$label loadUrl(Header) afterHook 异常", t)
                        }
                    }
                }
            )
            record(TAG, "$label loadUrl(String,Map) Hook 安装成功")
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "$label loadUrl(String,Map) Hook 失败", t)
        }
    }

    /**
     * 从 WebView 实例中获取已设置的 WebViewClient，并动态 hook 其 onPageFinished。
     *
     * 与 hookSetWebViewClient 方案的区别：
     * setWebViewClient 在 Activity 初始化时就被调用，早于 Xposed hook 安装时机，
     * 导致 afterHookedMethod 永远不触发。
     * 而 loadUrl 一定在 WebViewClient 设置之后才调用，此时通过 getWebViewClient()
     * 能可靠地拿到实际子类实例。
     */
    private fun tryHookWebViewClientFromWebView(webViewObj: Any, label: String) {
        try {
            val wvClient = XposedHelpers.callMethod(webViewObj, "getWebViewClient") ?: return
            val clientClass = wvClient.javaClass
            if (clientClass.name == "android.webkit.WebViewClient") {
                // 直接用的基类，不需要动态 hook（前面已 hook 基类）
                return
            }
            record(TAG, "$label 从 WebView 获取 WebViewClient: ${clientClass.name}")
            hookOnPageFinishedForClass(clientClass)
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "$label tryHookWebViewClientFromWebView 异常", t)
        }
    }

    // ============================================================
    // 基类 WebViewClient.onPageFinished fallback
    // ============================================================
    /**
     * Hook 基类 WebViewClient.onPageFinished 作为兜底。
     * 当子类没有 override onPageFinished 时（直接使用默认实现），此 hook 会触发。
     * 如果子类 override 了，则通过 loadUrl 回调中的 tryHookWebViewClientFromWebView 动态 hook 子类。
     */
    private fun hookBaseOnPageFinished() {
        if (!hookedClientClasses.add(android.webkit.WebViewClient::class.java)) {
            return
        }
        try {
            XposedHelpers.findAndHookMethod(
                android.webkit.WebViewClient::class.java,
                "onPageFinished",
                android.webkit.WebView::class.java,
                String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        onPageFinishedCallback(param)
                    }
                }
            )
            record(TAG, "基类 WebViewClient.onPageFinished Hook 安装成功")
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "基类 WebViewClient.onPageFinished Hook 失败", t)
        }
    }

    /**
     * 沿继承链向上查找声明了 onPageFinished 的类，并 hook 之。
     *
     * 关键: 不限定 onPageFinished 的参数类型，因为支付宝自定义 WebViewClient
     * (如 com.alipay.mywebview.sdk.WebViewClient) 的参数类型是自定义 WebView，
     * 而非 android.webkit.WebView，用 getDeclaredMethod 精确匹配会漏掉。
     */
    private fun hookOnPageFinishedForClass(clientClass: Class<*>) {
        record(TAG, "hookOnPageFinishedForClass 入口: ${clientClass.name}")

        var cls: Class<*>? = clientClass
        while (cls != null && cls != Any::class.java) {
            if (!hookedClientClasses.add(cls)) {
                record(TAG, "onPageFinished 已 hook: ${cls.name}，跳过")
                return
            }
            // 按名称查找该类自身声明的 onPageFinished (不限参数类型和数量)
            val methods = cls.declaredMethods.filter { it.name == "onPageFinished" }
            if (methods.isNotEmpty()) {
                for (m in methods) {
                    try {
                        XposedBridge.hookMethod(m, object : XC_MethodHook() {
                            override fun afterHookedMethod(param: MethodHookParam) {
                                onPageFinishedCallback(param)
                            }
                        })
                    } catch (t: Throwable) {
                        Log.printStackTrace(TAG, "hookOnPageFinishedForClass 单个方法 Hook 失败: ${cls.name}#${m.name}", t)
                    }
                }
                record(TAG, "onPageFinished 动态 Hook 成功: ${cls.name} (${methods.size} 个方法)")
                return
            }
            record(TAG, "  ${cls.name} 未声明 onPageFinished，向上查找父类")
            cls = cls.superclass
        }
        record(TAG, "继承链中未找到声明的 onPageFinished (已到 Object)")
    }

    /**
     * onPageFinished 回调处理：提取 HTML。
     *
     * 参数可能为 android.webkit.WebView 或 com.alipay.mywebview.sdk.WebView，
     * 不能强制转换，通过反射调用 evaluateJavascript。
     */
    private fun onPageFinishedCallback(param: XC_MethodHook.MethodHookParam) {
        try {
            val webViewObj = param.args[0] ?: return
            val url = param.args[1] as? String ?: return
            record(TAG, "onPageFinished: $url")
            extractHtml(webViewObj, url)
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "onPageFinished 回调异常", t)
        }
    }

    /**
     * 通过反射调用任意 WebView 对象的 evaluateJavascript 提取 HTML。
     *
     * 兼容 android.webkit.WebView 和支付宝自定义 WebView（如
     * com.alipay.mywebview.sdk_shell.MYWebView$WebViewEx），后者的
     * evaluateJavascript 参数类型可能是自定义回调接口而非
     * android.webkit.ValueCallback，用 findMethodBestMatch 会
     * 因参数类型不匹配而 NoSuchMethodError。
     *
     * 方案: 按名称找到 evaluateJavascript，用 java.lang.reflect.Proxy
     * 动态生成回调代理，兼容任意回调接口。
     */
    private fun extractHtml(webViewObj: Any, url: String) {
        try {
            val method = findEvaluateJavascript(webViewObj.javaClass) ?: run {
                record(TAG, "HTML提取: evaluateJavascript 方法未找到 (${webViewObj.javaClass.name})")
                return
            }
            // 动态创建回调代理: 兼容 android.webkit.ValueCallback 及任意自定义回调接口
            val callbackType = method.parameterTypes[1]
            val proxy = java.lang.reflect.Proxy.newProxyInstance(
                callbackType.classLoader,
                arrayOf(callbackType)
            ) { _, _, args ->
                try {
                    val rawHtml = args.firstOrNull() as? String
                    if (rawHtml == null || rawHtml == "null" || rawHtml.length < 2) {
                        record(TAG, "HTML提取: evaluateJavascript 返回空 (rawHtml=$rawHtml)")
                    } else {
                        val html = unescapeJsonString(rawHtml)
                        if (html.isBlank()) {
                            record(TAG, "HTML提取: 反转义后为空")
                        } else {
                            record(TAG, "HTML[$url] (${html.length} chars):\n$html")
                        }
                    }
                } catch (t: Throwable) {
                    Log.printStackTrace(TAG, "HTML提取回调异常", t)
                }
                null
            }
            method.invoke(webViewObj,
                "(function(){return document.documentElement.outerHTML;})()",
                proxy)
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "extractHtml 调用 evaluateJavascript 失败", t)
        }
    }

    /**
     * 按名称查找 evaluateJavascript 方法 (不限参数类型)，沿继承链向上查找。
     */
    private fun findEvaluateJavascript(cls: Class<*>?): java.lang.reflect.Method? {
        var c: Class<*>? = cls ?: return null
        while (c != null && c != Any::class.java) {
            c.declaredMethods.firstOrNull {
                it.name == "evaluateJavascript" && it.parameterTypes.size == 2
            }?.let { return it }
            c = c.superclass
        }
        return null
    }

    // ============================================================
    // Hook evaluateJavascript → 捕获 JS 执行内容 (兜底)
    // ============================================================
    private fun hookEvaluateJavascript(classLoader: ClassLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                SYSTEM_WEBVIEW,
                classLoader,
                "evaluateJavascript",
                String::class.java,
                "android.webkit.ValueCallback",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            val script = param.args[0] as? String ?: return
                            record(TAG, "[SysWV] evaluateJavascript: $script")
                        } catch (t: Throwable) {
                            // ignore
                        }
                    }
                }
            )
            record(TAG, "[SysWV] evaluateJavascript Hook 安装成功")
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "[SysWV] evaluateJavascript Hook 失败", t)
        }
    }

    // ============================================================
    // 辅助方法
    // ============================================================

    /**
     * 对 evaluateJavascript 返回的 JSON 字符串进行解码。
     *
     * evaluateJavascript 回调返回的是标准 JSON 字符串，例如:
     *   "\"<html><head>...<\\/head>...\""
     * 即外层是一个被 JSON 编码的字符串值。
     *
     * 使用 org.json.JSONTokener 解析，能正确处理所有 JSON 转义序列
     * (\\\", \\\\, \\n, \\t, \\r, \\uXXXX 等)，完全不会误伤 HTML 中
     * JS 代码的正则表达式里的 \\n \\\\ 等字面字符。
     */
    private fun unescapeJsonString(raw: String): String {
        return try {
            val value = org.json.JSONTokener(raw).nextValue()
            value as? String ?: raw
        } catch (e: Exception) {
            // JSON 解析失败，退回简单去引号处理
            Log.printStackTrace(TAG, "unescapeJsonString JSON解析失败, raw=$raw", e)
            raw
        }
    }
}
