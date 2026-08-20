package fansirsqi.xposed.sesame.hook.view

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeProvider
import android.webkit.WebView
import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.core.reflect.ReflectUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * 进程内无障碍节点探测工具。
 *
 * 不依赖无障碍服务权限：模块已由 Xposed 注入进宿主进程，WebView 的虚拟无障碍子树
 * （内部 HTML/DOM 元素，如 puzzle_slot、slide_root_view、提示文案）由 WebView 的
 * [android.view.accessibility.AccessibilityNodeProvider] 提供，可通过
 * findAccessibilityNodeInfosByText 直接查询并拿到 boundsInScreen。
 *
 * 注意：不能从 DecorView 节点用 getChild 向下遍历——进程内直接创建的 AccessibilityNodeInfo
 * 无 accessibility connection（未 sealed），getChild 会抛 "not sealed instance"，
 * 必须走 provider 查询接口。
 *
 * 前提：设备上启用了任一无障碍服务时，WebView(Chromium) 才会构建虚拟无障碍树。
 */
object WebViewAccessibilityFinder {

    private const val TAG = "WebViewAccessibilityFinder"

    // 原生 View 树 → 定位 WebView 的最大深度
    private const val MAX_VIEW_DEPTH = 40

    // provider 探测轮数与轮间等待（无障碍树异步构建，H5 节点填充可能滞后于页面渲染）
    private const val PROBE_MAX_ATTEMPTS = 3
    private const val PROBE_RETRY_DELAY_MS = 700L

    // 虚拟 id 扫描上限（Chromium/U4 系 WebView 的虚拟节点 id 通常为小整数）
    private const val MAX_VIRTUAL_ID_SCAN = 500

    data class TextMatch(val text: String, val bounds: Rect)

    /**
     * 枚举宿主当前所有窗口的根 View（通过 WindowManagerGlobal.mViews 反射）。
     * 验证码可能位于独立 Dialog 窗口，不一定在 Activity 主窗口下，因此必须扫全量窗口。
     */
    fun collectWindowRoots(): List<View> {
        val roots = mutableListOf<View>()
        try {
            val wmgClass = Class.forName("android.view.WindowManagerGlobal")
            val instance = ReflectUtil.callStaticMethod(wmgClass, "getInstance")
            val mViews = instance?.let { ReflectUtil.getObjectField(it, "mViews") } as? List<*>
            when (mViews) {
                null -> Log.record(TAG, "枚举窗口失败: WindowManagerGlobal.mViews 取不到（instance=${instance != null}）")
                else -> {
                    mViews.filterIsInstance<View>().forEach { roots.add(it) }
                }
            }
        } catch (e: Throwable) {
            Log.record(TAG, "枚举窗口异常: ${e.message}")
        }
        return roots
    }

    /**
     * 在指定窗口根下定位 WebView，并通过其 AccessibilityNodeProvider 按文本匹配虚拟节点。
     *
     * provider 操作需在主线程执行，自动切换主线程；含 WebView 的窗口最多探测
     * [maxAttempts] 轮（无障碍树异步构建，首轮可能未填充 H5 节点），轮间等待
     * [PROBE_RETRY_DELAY_MS]。滑动复核等时延敏感场景传 1（单轮不等待）。
     *
     * @return 命中的文本与屏幕 bounds；未命中返回 null。
     */
    suspend fun findText(root: View, keyword: String, maxAttempts: Int = PROBE_MAX_ATTEMPTS): TextMatch? {
        return withContext(Dispatchers.Main) {
            doFindTextViaProvider(root, keyword, maxAttempts)
        }
    }

    private suspend fun doFindTextViaProvider(root: View, keyword: String, maxAttempts: Int): TextMatch? {
        val webViews = mutableListOf<View>()
        collectWebViewViews(root, 0, webViews)
        if (webViews.isEmpty()) return null

        // provider 每轮重新获取：MYWebView 在 H5 内容渲染后才创建 provider，首轮可能全为 null（时序不稳定）
        for (attempt in 1..maxAttempts) {
            val providers = mutableListOf<Pair<View, AccessibilityNodeProvider>>()
            for (wv in webViews) {
                val p = try {
                    wv.getAccessibilityNodeProvider()
                } catch (_: Throwable) {
                    null
                }
                if (p != null) {
                    providers.add(wv to p)
                }
            }
            if (providers.isEmpty()) {
                if (attempt < maxAttempts) {
                    Log.record(TAG, "provider探测: 第${attempt}轮 ${webViews.size} 个 WebView provider 均为 null（内容未渲染），${PROBE_RETRY_DELAY_MS}ms后重试")
                    delay(PROBE_RETRY_DELAY_MS)
                    continue
                }
                Log.record(TAG, "provider探测: ${maxAttempts}轮 provider 均为 null，放弃该窗口")
                return null
            }
            for ((wv, provider) in providers) {
                // ① 框架文本搜索
                val nodes = try {
                    // virtualViewId=-1 表示宿主 View 根
                    provider.findAccessibilityNodeInfosByText(keyword, -1)
                } catch (e: Throwable) {
                    Log.record(TAG, "provider探测: findAccessibilityNodeInfosByText 异常: ${e.message}")
                    null
                }
                if (!nodes.isNullOrEmpty()) {
                    for (node in nodes) {
                        try {
                            val text = node.text?.toString()
                            val bounds = Rect()
                            node.getBoundsInScreen(bounds)
                            if (!text.isNullOrEmpty()) {
                                Log.record(TAG, "provider探测命中: text='${text.take(40)}' bounds=$bounds <<< 命中")
                                return TextMatch(text, bounds)
                            }
                        } catch (e: Throwable) {
                            Log.record(TAG, "provider探测: 节点属性读取异常: ${e.message}")
                        }
                    }
                }
                // ② 文本搜索未命中 → 虚拟 id 扫描兜底（createAccessibilityNodeInfo 不需要 sealed 节点）
                scanVirtualIds(wv, provider, keyword, attempt)?.let { return it }
            }
            if (attempt < maxAttempts) {
                Log.record(TAG, "provider探测: 第${attempt}轮未命中，${PROBE_RETRY_DELAY_MS}ms后重试")
                delay(PROBE_RETRY_DELAY_MS)
            }
        }
        return null
    }

    /**
     * 虚拟 id 扫描：对 id 0..[MAX_VIRTUAL_ID_SCAN-1] 逐个 createAccessibilityNodeInfo，
     * 直接读取 text/contentDescription 匹配 keyword。
     *
     * 背景：wrapper provider 的 findAccessibilityNodeInfosByText 可能未委托实现（返回空），
     * 且 getChild 受 not sealed 限制无法遍历；createAccessibilityNodeInfo(id) 是唯一可用的
     * 深度探测接口（外部 uiautomator 抓取证实 H5 文案节点确实存在于虚拟树中）。
     *
     * @return 命中时返回文本与 bounds；未命中返回 null。
     */
    private fun scanVirtualIds(wv: View, provider: AccessibilityNodeProvider, keyword: String, attempt: Int): TextMatch? {
        for (id in 0 until MAX_VIRTUAL_ID_SCAN) {
            val node = try {
                provider.createAccessibilityNodeInfo(id)
            } catch (_: Throwable) {
                null
            } ?: continue
            val text = node.text?.toString()?.takeIf { it.isNotBlank() }
            val cd = node.contentDescription?.toString()?.takeIf { it.isNotBlank() }
            if ((text != null && text.contains(keyword)) || (cd != null && cd.contains(keyword))) {
                val bounds = Rect()
                node.getBoundsInScreen(bounds)
                Log.record(TAG, "[id扫描] 命中: ${wv.javaClass.simpleName} id=$id bounds=$bounds <<< 命中（第${attempt}轮）")
                return TextMatch(text ?: cd!!, bounds)
            }
        }
        return null
    }

    // 支付宝自研 WebView（MYWebView，UC/U4 内核）相关类名前缀。
    // 实测验证码窗口（XRiverTransActivity）中的 WebView 均不继承 android.webkit.WebView：
    // com.alipay.mywebview.sdk_shell.MYWebView$WebViewEx / sdk.internal.WebViewInternalForM /
    // sdk.embedview.EmbedViewContainer / mywebview_obfuscated.wc
    private val MY_WEBVIEW_PREFIXES = arrayOf(
        "com.alipay.mywebview.",
        "mywebview_obfuscated."
    )

    private fun isWebViewLike(view: View): Boolean {
        if (view is WebView) return true
        val name = view.javaClass.name
        if (name == "android.webkit.WebView") return true
        return MY_WEBVIEW_PREFIXES.any { name.startsWith(it) }
    }

    /** 递归原生 View 树收集 WebView（框架 WebView + 支付宝 MYWebView） */
    private fun collectWebViewViews(view: View, depth: Int, out: MutableList<View>) {
        if (depth > MAX_VIEW_DEPTH) return
        if (isWebViewLike(view)) {
            out.add(view)
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                collectWebViewViews(view.getChildAt(i), depth + 1, out)
            }
        }
    }
}