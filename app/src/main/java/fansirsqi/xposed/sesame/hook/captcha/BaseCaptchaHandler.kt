package fansirsqi.xposed.sesame.hook.captcha

import android.app.Activity
import android.graphics.Bitmap
import android.os.Looper
import android.view.View
import android.view.WindowManager
import fansirsqi.xposed.sesame.core.app.CommandUtil
import fansirsqi.xposed.sesame.core.app.DeviceStateChecker
import fansirsqi.xposed.sesame.core.app.UnlockUtil
import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.hook.view.MotionEventSimulator
import fansirsqi.xposed.sesame.hook.view.PageMonitor
import fansirsqi.xposed.sesame.hook.view.PageMonitor.ActivityHandleResult
import fansirsqi.xposed.sesame.hook.view.ViewImage
import fansirsqi.xposed.sesame.hook.view.SystemInputSwiper
import kotlinx.coroutines.sync.Mutex
import java.io.File
import java.io.FileOutputStream

/**
 * 验证码处理程序的基类，提供处理滑动验证码的公共骨架与通用工具。
 *
 * 具体的验证码处理流程由子类各自实现（职责内聚，演化隔离）：
 * - [Captcha1Handler]：旧版 XPath 路径（XRiverActivity 原生 View 滑块验证码）
 * - [Captcha2Handler]：新版图像识别路径（XRiverTransActivity$Main / NebulaTransActivity$Main 的 WebView 滑块验证码）
 */
abstract class BaseCaptchaHandler {

    companion object {
        protected const val TAG = "CaptchaHandler"

        // 滑动参数配置（子类共享）
        protected const val SLIDE_START_OFFSET = 25
        protected const val SLIDE_END_MARGIN = 20
        protected const val SLIDE_DURATION_MIN = 900L
        protected const val SLIDE_DURATION_MAX = 1400L

        /** 滑动后等待页面响应再开始判定的时间（ms） */
        protected const val POST_SLIDE_CHECK_DELAY_MS = 1200L

        protected val captchaProcessingMutex = Mutex()

        /** 调试截图目录名（内/外部缓存下） */
        private const val DEBUG_DIR_NAME = "captcha_debug"

        /** 调试截图保留上限（张）：超出后删除最旧文件，防止截图无限堆积 */
        private const val DEBUG_KEEP_COUNT = 60
    }

    /**
     * 验证码处理入口。由子类各自实现完整流程：
     * [Captcha1Handler] 走旧版 XPath，[Captcha2Handler] 走新版图像识别。
     */
    abstract suspend fun handleActivity(activity: Activity, root: ViewImage): ActivityHandleResult

    /**
     * 本版验证码提示文案是否在屏（锚点探测契约）。
     *
     * 供 [CaptchaAnchorWatcher] 在闸门暂停期间轮询：文案出现->消失即判定验证码页关闭。
     * 子类按各自验证码形态实现探测通道（原生 View 树 / WebView 虚拟树）。
     */
    abstract suspend fun isAnchorVisible(): Boolean

    /**
     * 公共骨架：记录触发日志，保证结束时恢复 KEEP_SCREEN_ON，并兜底捕获异常。
     * 子类在其中执行各自的完整流程；处理窗口互斥锁由子类自行获取/释放。
     */
    protected suspend fun runWithProcessingWindow(
        activity: Activity,
        flow: suspend () -> ActivityHandleResult
    ): ActivityHandleResult {
        // 防止处理过程中息屏
        val originalFlags = applyKeepScreenOn(activity)
        return try {
            Log.record(
                TAG,
                "[触发命中] Activity 命中验证码处理器: ${activity.javaClass.name}, thread=${Thread.currentThread().name}, isMain=${isMainThread()}"
            )
            val startTime = System.currentTimeMillis()
            val result = flow()
            Log.record(TAG, "验证码处理完成，耗时: ${System.currentTimeMillis() - startTime}ms, 结果: $result")
            result
        } catch (e: Exception) {
            Log.error(TAG, "处理验证码页面时发生异常: ${e.stackTraceToString()}")
            ActivityHandleResult.FAILED_RETRYABLE
        } finally {
            restoreKeepScreenOn(activity, originalFlags)
        }
    }

    /**
     * 处理窗口互斥。获取成功返回 true；已被占用则记录可重试失败并返回 false。
     * @param busyReason 处理窗口被占用时记录的失败原因码
     */
    protected suspend fun acquireProcessingWindow(busyReason: String = "验证码处理窗口被占用"): Boolean {
        if (!captchaProcessingMutex.tryLock()) {
            logRetryableFailure(busyReason)
            return false
        }
        return true
    }

    protected fun releaseProcessingWindow() {
        captchaProcessingMutex.unlock()
    }

    /**
     * 派发滑动手势（屏幕坐标系）：优先 view 本地 dispatch（MotionEvent 注入），
     * 失败（view.isShown=false 等）退 shell input 兜底。
     *
     * @return true=手势已派发；false=两条路径均失败
     */
    protected suspend fun dispatchSwipeWithFallback(
        view: View,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        duration: Long
    ): Boolean {
        val startTime = System.currentTimeMillis()
        if (MotionEventSimulator.simulateSwipe(view, startX, startY, endX, endY, duration)) {
            Log.record(TAG, "[滑动路径] path=view-dispatch, 耗时=${System.currentTimeMillis() - startTime}ms")
            return true
        }
        Log.record(TAG, "[滑动路径] view-dispatch 失败(view.isShown=false)，尝试 shell input 兜底")
        if (!SystemInputSwiper.swipe(startX, startY, endX, endY, duration)) {
            Log.record(TAG, "[滑动路径] shell input 兜底也失败")
            return false
        }
        Log.record(TAG, "[滑动路径] path=shell-input, 耗时=${System.currentTimeMillis() - startTime}ms")
        return true
    }

    /**
     * 设置 KEEP_SCREEN_ON，返回原始 Window Flag 以便恢复。
     */
    protected fun applyKeepScreenOn(activity: Activity): Int {
        val originalFlags = activity.window?.attributes?.flags ?: 0
        activity.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        return originalFlags
    }

    /**
     * 恢复原始 KEEP_SCREEN_ON 状态。
     */
    protected fun restoreKeepScreenOn(activity: Activity, originalFlags: Int) {
        try {
            if ((originalFlags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) == 0) {
                activity.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        } catch (_: Exception) {}
    }

    /**
     * 率先触发内置解锁（屏幕不亮时页面不会渲染，后续 delay 白等）。
     * 需要解锁但解锁失败（功能关闭/已解锁均不误杀）→ 返回 false（可重试失败）。
     */
    protected suspend fun performUnlockOrFail(): Boolean {
        val context = PageMonitor.getContext() ?: return true
        CommandUtil.connect(context)
        Log.record(TAG, "已发起 CommandService 预连接")
        if (!UnlockUtil.triggerUnlock(context) && !DeviceStateChecker.isUnlockedAndAwake(context)) {
            logRetryableFailure("解锁失败") // 详细原因已在 UnlockUtil 日志中
            return false
        }
        return true
    }

    protected fun logPrecheckSkip(skipReason: String, failReasons: List<String>, passReasons: List<String>) {
        Log.record(
            TAG,
            "[前置跳过·不可重试] 原因=$skipReason; 未通过=${failReasons.joinToString("; ")}; 通过项=${passReasons.joinToString(", ")}"
        )
        Log.record(TAG, "[跳过后释放处理窗口] 原因=$skipReason")
    }

    protected fun logAcceptedAfterSkip(anchorReason: String) {
        Log.record(TAG, "[确认真实验证码] 原因=$anchorReason")
    }

    protected fun logRetryableFailure(reason: String) {
        Log.record(TAG, "[处理失败·可重试] 原因=$reason")
    }

    /**
     * 保存调试截图到内/外部缓存目录（captcha_debug），并清理超过上限的旧图。
     * 外部缓存一份便于直接拉取排查。
     */
    protected fun saveDebugBitmap(bitmap: Bitmap, fileName: String) {
        try {
            val context = PageMonitor.getContext() ?: return
            val fileSuffixName = "${System.currentTimeMillis()}_$fileName.jpg"

            // 内部缓存：/data/data/<host>/cache/captcha_debug/
            val internalDir = File(context.cacheDir, DEBUG_DIR_NAME).also { it.mkdirs() }
            writeJpeg(File(internalDir, fileSuffixName), bitmap)
            Log.record("CaptchaDebug", "调试图片已导出: ${internalDir.absolutePath}/$fileSuffixName")
            trimDebugDir(internalDir)

            // 外部缓存：无需 root 即可通过文件管理器拉取
            context.externalCacheDir?.let { externalCacheDir ->
                val externalDir = File(externalCacheDir, DEBUG_DIR_NAME).also { it.mkdirs() }
                writeJpeg(File(externalDir, fileSuffixName), bitmap)
                Log.record("CaptchaDebug", "调试图片已同步到外部缓存: ${externalDir.absolutePath}/$fileSuffixName")
                trimDebugDir(externalDir)
            }
        } catch (e: Exception) {
            Log.error("CaptchaDebug", "导出调试图片失败: ${e.message}")
        }
    }

    /** 以 JPEG 质量 90 写出位图文件 */
    private fun writeJpeg(file: File, bitmap: Bitmap) {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
    }

    /**
     * 清理调试截图目录：仅保留最近 [DEBUG_KEEP_COUNT] 张，删除最旧的，防止无限堆积。
     */
    private fun trimDebugDir(dir: File) {
        try {
            val files = dir.listFiles { f -> f.isFile && f.name.endsWith(".jpg") } ?: return
            if (files.size <= DEBUG_KEEP_COUNT) return
            files.sortedBy { it.lastModified() }
                .take(files.size - DEBUG_KEEP_COUNT)
                .forEach { it.delete() }
            Log.record("CaptchaDebug", "调试目录已清理，保留最近 $DEBUG_KEEP_COUNT 张")
        } catch (e: Exception) {
            Log.error("CaptchaDebug", "清理调试图片失败: ${e.message}")
        }
    }

    protected fun isMainThread(): Boolean {
        return Looper.myLooper() == Looper.getMainLooper()
    }
}
