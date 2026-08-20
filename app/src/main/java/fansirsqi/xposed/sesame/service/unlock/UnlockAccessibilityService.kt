package fansirsqi.xposed.sesame.service.unlock

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import fansirsqi.xposed.sesame.core.log.Log
import kotlinx.coroutines.delay

/**
 * 无障碍服务，为内置解锁提供手势与节点操作能力。
 * 仅在解锁期间被 UnlockManager 主动调用，不监听事件流。
 */
class UnlockAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        instance = this
        Log.record(TAG, "无障碍服务已连接")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        Log.record(TAG, "无障碍服务已断开")
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 不监听事件流，仅在解锁流程中按需查询节点树
    }

    override fun onInterrupt() {}

    companion object {
        private const val TAG = "UnlockAccessibilityService"

        /**
         * 模块进程判断"无障碍可用"的唯一运行时依据。
         * 仅由系统回调维护；服务被杀时置 null，解锁请求直接 FAIL(accessibility_disabled)。
         */
        @Volatile
        var instance: UnlockAccessibilityService? = null
            private set

        /**
         * 上滑手势（attempt 用于起点轮换）。返回 false 表示服务不可用或分发失败。
         */
        suspend fun dispatchSwipeUp(attempt: Int = 0): Boolean {
            val svc = instance ?: return false
            val metrics = svc.resources.displayMetrics
            val w = metrics.widthPixels.toFloat()
            val h = metrics.heightPixels.toFloat()
            val path = Path().apply {
                moveTo(w * 0.5f, h * (0.96f - 0.15f * attempt))
                lineTo(w * 0.5f, h * 0.3f)
            }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 200L))
                .build()
            return svc.dispatchGesture(gesture, null, null)
        }

        /**
         * PIN 节点点击（第①层主通道）。逐位查找数字键节点并 CLICK，位间隔 150~300ms 随机。
         * H2：只记录位数，不记录密码内容。
         */
        suspend fun inputPinByNodes(digits: String): Boolean {
            val svc = instance ?: return false
            val root = svc.rootInActiveWindow ?: return false // SystemUI 锁屏窗口
            for ((i, ch) in digits.withIndex()) {
                val node = root.findAccessibilityNodeInfosByText(ch.toString())
                    .firstOrNull { it.isClickable }
                    ?: root.findAccessibilityNodeInfosByText("数字$ch").firstOrNull()
                if (node?.performAction(AccessibilityNodeInfo.ACTION_CLICK) != true) return false
                Log.record(TAG, "PIN 已输入第 ${i + 1} 位") // H2：只记位数不记内容
                delay((150L..300L).random())
            }
            return true
        }

        /**
         * 混合密码：focus + ACTION_SET_TEXT。
         */
        suspend fun inputPasswordByText(pwd: String): Boolean {
            val svc = instance ?: return false
            val root = svc.rootInActiveWindow ?: return false
            val editable = root.findAccessibilityNodeInfosByText("")
                .firstOrNull { it.isEditable }
                ?: return false
            if (!editable.performAction(AccessibilityNodeInfo.ACTION_FOCUS)) return false
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, pwd)
            }
            return editable.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        }

        /** 跨应用文本检索结果：found=true 时 text/rect 有效；reason 携带失败原因供诊断 */
        data class PkgTextResult(
            val found: Boolean,
            val text: String? = null,
            val rect: Rect? = null,
            val reason: String = ""
        )

        /**
         * 在指定应用的全部窗口内按文本检索节点（sealed 官方通道，可下钻 WebView H5 虚拟子树）。
         * 供 CommandService 响应支付宝进程的 findNodeByText 请求（验证码文案定位）。
         *
         * reason 枚举：service_off（无障碍服务未连接）/ no_window:pkg（窗口列表无该应用窗口）/
         * not_found:roots=N,hits=M（检索执行但未命中，缺 flags 或页面未渲染）
         */
        fun findTextInPackage(pkg: String, keyword: String): PkgTextResult {
            val svc = instance ?: return PkgTextResult(false, reason = "service_off")
            // 收集目标包名的窗口根节点（getWindows 含 Dialog 等非主窗口）
            val roots = mutableListOf<AccessibilityNodeInfo>()
            try {
                for (w in svc.windows) {
                    val root = w.root ?: continue
                    if (root.packageName?.toString() == pkg) roots.add(root)
                }
            } catch (e: Throwable) {
                Log.record(TAG, "findTextInPackage 枚举窗口异常: ${e.message}")
            }
            if (roots.isEmpty()) {
                try {
                    svc.rootInActiveWindow?.let {
                        if (it.packageName?.toString() == pkg) roots.add(it)
                    }
                } catch (_: Throwable) {
                }
            }
            if (roots.isEmpty()) return PkgTextResult(false, reason = "no_window:$pkg")
            var hits = 0
            for (root in roots) {
                val nodes = try {
                    root.findAccessibilityNodeInfosByText(keyword)
                } catch (e: Throwable) {
                    Log.record(TAG, "findTextInPackage 检索异常: ${e.message}")
                    continue
                }
                for (n in nodes) {
                    hits++
                    val text = n.text?.toString()
                    if (!text.isNullOrBlank() && text.contains(keyword)) {
                        val rect = Rect()
                        n.getBoundsInScreen(rect)
                        return PkgTextResult(true, text, rect, "hit")
                    }
                }
            }
            return PkgTextResult(false, reason = "not_found:roots=${roots.size},hits=$hits")
        }
    }
}
