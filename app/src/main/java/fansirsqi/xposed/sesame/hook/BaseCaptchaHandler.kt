package fansirsqi.xposed.sesame.hook

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.util.Base64
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import fansirsqi.xposed.sesame.hook.simple.MotionEventSimulator
import fansirsqi.xposed.sesame.hook.simple.SimplePageManager
import fansirsqi.xposed.sesame.hook.simple.SimplePageManager.ActivityHandleResult
import fansirsqi.xposed.sesame.hook.simple.SimpleViewImage
import fansirsqi.xposed.sesame.hook.simple.ViewHierarchyAnalyzer
import fansirsqi.xposed.sesame.hook.simple.SliderTFLite
import fansirsqi.xposed.sesame.hook.simple.SystemInputSwiper
import fansirsqi.xposed.sesame.util.CommandUtil
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.UnlockUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import java.io.ByteArrayOutputStream
import kotlin.random.Random

import fansirsqi.xposed.sesame.hook.VersionHook
import fansirsqi.xposed.sesame.entity.AlipayVersion

/*
 * 滑动坐标四元组，用于封装滑动起点和终点坐标。
 */
data class SlideCoordinates(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float
)

private data class CaptchaVisualSnapshot(
    val fullBitmap: Bitmap,
    val croppedBitmap: Bitmap,
    val recognitionResult: SliderTFLite.SlideRecognitionResult?
)

private data class SliderHandleDetection(
    val centerX: Float,
    val centerY: Float,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val pixelCount: Int
)

private data class CaptchaPreCheckResult(
    val passed: Boolean,
    val passReasons: List<String>,
    val failReasons: List<String>,
    val sliderHandle: SliderHandleDetection?
)

private data class LightweightCaptchaPreCheckResult(
    val passed: Boolean,
    val passReasons: List<String>,
    val failReasons: List<String>,
    val sliderHandle: SliderHandleDetection?,
    val fullBitmap: Bitmap?,
    val croppedBitmap: Bitmap?,
    val cropTop: Int,
    val cropBottom: Int
)

/**
 * 验证码处理程序的基类，提供处理滑动验证码的通用逻辑。
 */
abstract class BaseCaptchaHandler {

    companion object {
        private const val TAG = "CaptchaHandler"

        // 滑动参数配置
        private const val SLIDE_START_OFFSET = 25
        private const val SLIDE_END_MARGIN = 20
        private const val SLIDE_DURATION_MIN = 900L  // XRiver/WebView 场景改为更慢的系统级滑动
        private const val SLIDE_DURATION_MAX = 1400L
        private const val CORRECTIVE_SLIDE_DURATION_MIN = 420L
        private const val CORRECTIVE_SLIDE_DURATION_MAX = 650L
        private const val POST_SLIDE_CHECK_DELAY_MS = 1200L
        private const val NEW_CAPTCHA_CONFIDENCE_THRESHOLD = 0.55f

        // 旧版本 XPath
        private const val OLD_SLIDE_VERIFY_TEXT_XPATH = "//TextView[contains(@text,'向右滑动验证')]"
        
        // 新版本 XPath
        private const val NEW_SLIDE_VERIFY_TEXT_XPATH = "//View[contains(@text,'请拖动滑块完成拼图')]"
        
        private val captchaProcessingMutex = Mutex()
    }

    protected abstract fun getSlidePathKey(): String

    open suspend fun handleActivity(activity: Activity, root: SimpleViewImage): ActivityHandleResult {
        return try {
            // 立即记录开始处理时间
            val startTime = System.currentTimeMillis()
            Log.record(
                TAG,
                "[触发命中] Activity 命中验证码处理器: ${activity.javaClass.name}, thread=${Thread.currentThread().name}, isMain=${isMainThread()}"
            )

            // 版本判断逻辑
            val isNewVersion = if (VersionHook.hasVersion()) {
                val currentVersion = VersionHook.getCapturedVersion() ?: AlipayVersion("")
                val thresholdVersion = AlipayVersion("10.6.58.9999") 
                currentVersion.compareTo(thresholdVersion) > 0
            } else {
                false
            }

            val result = if (isNewVersion) {
                Log.record(TAG, "检测到新版本应用，使用图像识别模式处理验证码。")
                handleNewVersionCaptcha(activity)
            } else {
                Log.record(TAG, "检测到旧版本应用，使用传统模式处理验证码。")
                handleLegacySlideCaptcha(activity)
            }
            
            val endTime = System.currentTimeMillis()
            Log.record(TAG, "验证码处理完成，耗时: ${endTime - startTime}ms, 结果: $result")
            result
        } catch (e: Exception) {
            Log.error(TAG, "处理验证码页面时发生异常: ${e.stackTraceToString()}")
            ActivityHandleResult.FAILED_RETRYABLE
        }
    }

    private fun logPrecheckSkip(skipReason: String, failReasons: List<String>, passReasons: List<String>) {
        Log.record(
            TAG,
            "precheck-skip-non-retryable: reason=$skipReason; fail=${failReasons.joinToString("; ")}; pass=${passReasons.joinToString(", ")}"
        )
        Log.record(TAG, "processing-window-released-after-skip: reason=$skipReason")
    }

    private fun logAcceptedAfterSkip(anchorReason: String) {
        Log.record(TAG, "real-captcha-accepted-after-previous-skip: reason=$anchorReason")
    }

    private fun logRetryableFailure(reason: String) {
        Log.record(TAG, "captcha-processing-failed-retryable: reason=$reason")
    }

    @SuppressLint("SuspiciousIndentation")
    private suspend fun handleNewVersionCaptcha(activity: Activity): ActivityHandleResult {
        var processingWindowAcquired = false
        // 防止处理过程中息屏
        val originalFlags = activity.window?.attributes?.flags ?: 0
        try {
            activity.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            Log.record(
                TAG,
                "[新验证码] 开始处理, thread=${Thread.currentThread().name}, isMain=${isMainThread()}"
            )

            val textAnchor = SimplePageManager.tryGetTopView(NEW_SLIDE_VERIFY_TEXT_XPATH)
            val anchorText = textAnchor
                ?.getText()
                ?.take(24)
                ?.toString()
                ?.takeIf { it.isNotBlank() }
            val hasTextAnchor = !anchorText.isNullOrBlank()

            if (hasTextAnchor) {
                Log.record(TAG, "[前置命中] text-anchor=$anchorText")
            } else {
                Log.record(TAG, "[前置提示] text-anchor-missing, fallback=visual-precheck")
            }

            // 率先触发外部解锁（屏幕不亮时页面不会渲染，后续 delay 白等）
            val context = SimplePageManager.getContext()
            if (context != null) {
                CommandUtil.connect(context)
                Log.record(TAG, "已发起 CommandService 预连接")
                UnlockUtil.triggerUnlock(context)
            }

            // 延迟等待渲染，确保截图时验证码已显示
            delay(1200L)

            val decorView = activity.window.decorView

            val lightweightPreCheck = evaluateLightweightCaptchaPreCheck(
                decorView = decorView,
                anchorText = anchorText
            )
            if (!lightweightPreCheck.passed) {
                val skipReason = if (hasTextAnchor) {
                    "normal-page-skip"
                } else {
                    "text-anchor-missing and precheck-fail"
                }
                logPrecheckSkip(skipReason, lightweightPreCheck.failReasons, lightweightPreCheck.passReasons)
                return ActivityHandleResult.SKIP_NON_RETRYABLE
            }

            if (!hasTextAnchor) {
                Log.record(
                    TAG,
                    "[前置放行] reason=text-anchor-missing but visual-precheck-pass; pass=${lightweightPreCheck.passReasons.joinToString(", ")}"
                )
            }

            if (!captchaProcessingMutex.tryLock()) {
                logRetryableFailure("captcha-processing-window-busy")
                return ActivityHandleResult.FAILED_RETRYABLE
            }
            processingWindowAcquired = true

            logAcceptedAfterSkip(
                if (hasTextAnchor) {
                    "text-anchor-present and precheck-pass"
                } else {
                    "text-anchor-missing but visual-precheck-pass"
                }
            )

            val fullBitmap = lightweightPreCheck.fullBitmap ?: run {
                Log.record(TAG, "轻量前置检测未生成 fullBitmap")
                logRetryableFailure("lightweight-precheck-missing-fullBitmap")
                return ActivityHandleResult.FAILED_RETRYABLE
            }
            val croppedBitmap = lightweightPreCheck.croppedBitmap ?: run {
                Log.record(TAG, "轻量前置检测未生成 croppedBitmap")
                logRetryableFailure("lightweight-precheck-missing-croppedBitmap")
                return ActivityHandleResult.FAILED_RETRYABLE
            }
            val cropTop = lightweightPreCheck.cropTop
            val cropBottom = lightweightPreCheck.cropBottom
            val detectedHandle = lightweightPreCheck.sliderHandle ?: run {
                Log.record(TAG, "轻量前置检测未命中滑块手柄")
                logRetryableFailure("lightweight-precheck-missing-sliderHandle")
                return ActivityHandleResult.FAILED_RETRYABLE
            }

            Log.record(
                TAG,
                "[模型转后台] callerThread=${Thread.currentThread().name}, isMain=${isMainThread()}"
            )
            val recognitionResult = SliderTFLite.identifyShared(activity.applicationContext, croppedBitmap) ?: run {
                Log.record(TAG, "裁剪区域模型识别失败，未检测到滑块和缺口")
                logRetryableFailure("model-recognition-null")
                return ActivityHandleResult.FAILED_RETRYABLE
            }

            // 将裁剪区域坐标转换为 DecorView 局部坐标
            val sliderLocalX = recognitionResult.sliderX
            val sliderLocalY = recognitionResult.sliderY + cropTop
            val targetLocalX = recognitionResult.targetX
            val targetLocalY = recognitionResult.targetY + cropTop

            Log.record(TAG, "裁剪识别成功: 裁剪内坐标 滑块=(${recognitionResult.sliderX.toInt()},${recognitionResult.sliderY.toInt()}) 目标=(${recognitionResult.targetX.toInt()},${recognitionResult.targetY.toInt()})")
            Log.record(TAG, "DecorView坐标: 滑块=(${sliderLocalX.toInt()},${sliderLocalY.toInt()}), 目标=(${targetLocalX.toInt()},${targetLocalY.toInt()}), 置信度=${recognitionResult.confidence}")

            val distance = targetLocalX - sliderLocalX

            val preCheck = evaluateNewCaptchaPreCheck(
                croppedBitmap = croppedBitmap,
                recognitionResult = recognitionResult,
                anchorText = anchorText,
                sliderHandle = detectedHandle
            )
            if (!preCheck.passed) {
                val skipReason = if (hasTextAnchor) {
                    "normal-page-skip"
                } else {
                    "text-anchor-missing and precheck-fail"
                }
                logPrecheckSkip(skipReason, preCheck.failReasons, preCheck.passReasons)
                return ActivityHandleResult.SKIP_NON_RETRYABLE
            }

            Log.record(
                TAG,
                "[前置检测通过] reason=${if (hasTextAnchor) "captcha-precheck-pass" else "text-anchor-missing but visual-precheck-pass"}; 判定为滑块验证码页: ${preCheck.passReasons.joinToString(", ")}"
            )

            Log.record(TAG, "滑动参数: 起点=(${sliderLocalX.toInt()},${sliderLocalY.toInt()}), 终点=(${targetLocalX.toInt()},${targetLocalY.toInt()}), 距离=${distance.toInt()}px")

            // 对 DecorView 执行滑动（WebView 内容在 DecorView 坐标系下）
            val sliderHandle = preCheck.sliderHandle ?: detectedHandle
            val actualStartX = sliderHandle.centerX
            val actualStartY = sliderHandle.centerY
            val actualEndX = actualStartX + distance
            val actualEndY = actualStartY

            Log.record(
                TAG,
                "命中滑块手柄: bounds=(${sliderHandle.left},${sliderHandle.top},${sliderHandle.right},${sliderHandle.bottom}), center=(${sliderHandle.centerX.toInt()},${sliderHandle.centerY.toInt()}), pixels=${sliderHandle.pixelCount}"
            )

            Log.record(
                TAG,
                "实际滑动参数: 起点=(${actualStartX.toInt()},${actualStartY.toInt()}), 终点=(${actualEndX.toInt()},${actualEndY.toInt()}), 距离=${distance.toInt()}px"
            )

            val beforeSnapshot = CaptchaVisualSnapshot(
                fullBitmap = fullBitmap,
                croppedBitmap = croppedBitmap,
                recognitionResult = recognitionResult
            )
            return if (executeSlideOnView(
                view = decorView,
                localStartX = actualStartX,
                localStartY = actualStartY,
                localEndX = actualEndX,
                localEndY = actualEndY,
                beforeSnapshot = beforeSnapshot,
                cropTop = cropTop,
                cropBottom = cropBottom
            )) {
                ActivityHandleResult.HANDLED
            } else {
                logRetryableFailure("execute-slide-on-view-failed")
                ActivityHandleResult.FAILED_RETRYABLE
            }

        } catch (e: Exception) {
            Log.record(TAG, "新版验证码处理出错: ${e.stackTraceToString()}")
            logRetryableFailure("new-version-exception")
            return ActivityHandleResult.FAILED_RETRYABLE
        } finally {
            // 恢复原始 Window Flag
            try {
                if ((originalFlags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) == 0) {
                    activity.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            } catch (_: Exception) {}
            if (processingWindowAcquired) {
                captchaProcessingMutex.unlock()
            }
        }
    }

    private fun saveDebugBitmap(bitmap: Bitmap, fileName: String) {
        try {
            val context = SimplePageManager.getContext() ?: return
            // 保存到 /data/data/com.eg.android.AlipayGphone/cache/captcha_debug/
            val dir = java.io.File(context.cacheDir, "captcha_debug")
            if (!dir.exists()) dir.mkdirs()

            val file = java.io.File(dir, "${System.currentTimeMillis()}_$fileName.jpg")
            java.io.FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            Log.record("CaptchaDebug", "调试图片已导出: ${file.absolutePath}")

            context.externalCacheDir?.let { externalCacheDir ->
                val externalDir = java.io.File(externalCacheDir, "captcha_debug")
                if (!externalDir.exists()) externalDir.mkdirs()
                val externalFile = java.io.File(externalDir, file.name)
                java.io.FileOutputStream(externalFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                Log.record("CaptchaDebug", "调试图片已同步到外部缓存: ${externalFile.absolutePath}")
            }
        } catch (e: Exception) {
            Log.error("CaptchaDebug", "导出调试图片失败: ${e.message}")
        }
    }

    private fun findSliderByFeature(root: View): View? {
        val candidates = mutableListOf<View>()
        val rootWidth = root.rootView?.width ?: root.width
        val rootHeight = root.rootView?.height ?: root.height

        fun traverse(view: View) {
            if (view.visibility != View.VISIBLE) return

            // 获取绝对坐标和尺寸
            val location = IntArray(2)
            view.getLocationOnScreen(location)
            val w = view.width
            val h = view.height

            // 关键调试：如果你还是找不到，观察这里打印出的宽高
            // Log.record(TAG, "扫描 View: ${view.javaClass.simpleName} [$w x $h] at (${location[0]}, ${location[1]})")

            // 策略调整：
            // 1. 滑块通常是接近正方形的 (宽/高比在 0.8~1.2 之间)
            // 2. 滑块不会太小（大于50像素），也不会太大（小于屏幕宽度的1/3）
            // 3. 它通常在屏幕的下半部分
            val isSquare = w > 0 && h > 0 && kotlin.math.abs(w - h) <= (maxOf(w, h) * 0.35)
            val inReasonableSize = w in 60..220 && h in 60..220
            val inLeftArea = location[0] in 0..(rootWidth * 45 / 100)
            val inLowerArea = location[1] >= (rootHeight * 60 / 100)

            if (isSquare && inReasonableSize && inLeftArea && inLowerArea) {
                candidates.add(view)
            }

            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    traverse(view.getChildAt(i))
                }
            }
        }

        traverse(root)

        // 支付宝新版滑块的一个显著特征：它是横向滑动条里唯一的、可点击的小方块
        // 我们取符合条件的最后一个（通常层级最深，即最具体的那个 View）
        Log.record(TAG, "特征搜索候选数量: ${candidates.size}")
        candidates.forEachIndexed { index, candidate ->
            val loc = IntArray(2)
            candidate.getLocationOnScreen(loc)
            Log.record(TAG, "  候选[$index]: ${candidate.javaClass.simpleName} [${candidate.width}x${candidate.height}] at (${loc[0]}, ${loc[1]}) clickable=${candidate.isClickable}")
        }

        val result = candidates
            .sortedWith(
                compareByDescending<View> { it.isClickable }
                    .thenByDescending { it.width * it.height }
                    .thenBy { v ->
                        val loc = IntArray(2)
                        v.getLocationOnScreen(loc)
                        kotlin.math.abs(loc[0])
                    }
            )
            .firstOrNull()

        if (result != null) {
            Log.record(TAG, "特征搜索命中: ${result.javaClass.simpleName} [${result.width}x${result.height}]")
        }
        return result
    }

    private fun findSliderByBottomSliderArea(root: View): View? {
        val candidates = mutableListOf<View>()
        val rootWidth = root.rootView?.width ?: root.width
        val rootHeight = root.rootView?.height ?: root.height

        fun traverse(view: View) {
            if (view.visibility != View.VISIBLE) return

            val location = IntArray(2)
            view.getLocationOnScreen(location)
            val x = location[0]
            val y = location[1]
            val w = view.width
            val h = view.height

            val inBottomHalf = y > rootHeight * 2 / 3
            val inLeftHalf = x < rootWidth / 2
            val reasonableSize = w in 80..220 && h in 80..220
            val visibleEnough = view.alpha > 0.8f

            if (inBottomHalf && inLeftHalf && reasonableSize && visibleEnough) {
                candidates.add(view)
            }

            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    traverse(view.getChildAt(i))
                }
            }
        }

        traverse(root)

        Log.record(TAG, "底部滑条区域候选数量: ${candidates.size}")
        candidates.forEachIndexed { index, candidate ->
            val loc = IntArray(2)
            candidate.getLocationOnScreen(loc)
            Log.record(TAG, "  底部候选[$index]: ${candidate.javaClass.simpleName} [${candidate.width}x${candidate.height}] at (${loc[0]}, ${loc[1]}) clickable=${candidate.isClickable}")
        }

        val result = candidates.sortedWith(
            compareBy<View> { v ->
                val loc = IntArray(2)
                v.getLocationOnScreen(loc)
                loc[0]
            }.thenByDescending { it.width * it.height }
        ).firstOrNull()

        if (result != null) {
            Log.record(TAG, "底部滑条区域命中: ${result.javaClass.simpleName} [${result.width}x${result.height}]")
        }
        return result
    }

    private fun findSliderByTextAnchor(): View? {
        return try {
            val slideTextInDialog = SimplePageManager.tryGetTopView(NEW_SLIDE_VERIFY_TEXT_XPATH)
            if (slideTextInDialog == null) {
                Log.record(TAG, "未找到新版滑动验证文本锚点")
                return null
            }
            Log.record(TAG, "发现新版滑动验证文本: ${slideTextInDialog.getText()}")
            ViewHierarchyAnalyzer.findActualSliderView(slideTextInDialog)
        } catch (e: Throwable) {
            Log.record(TAG, "通过文本锚点定位滑块失败: ${e.message}")
            null
        }
    }

    private fun evaluateNewCaptchaPreCheck(
        croppedBitmap: Bitmap,
        recognitionResult: SliderTFLite.SlideRecognitionResult,
        anchorText: String?,
        sliderHandle: SliderHandleDetection
    ): CaptchaPreCheckResult {
        val passReasons = mutableListOf<String>()
        val failReasons = mutableListOf<String>()

        passReasons += formatAnchorReason(anchorText)
        passReasons += "blueHandle=(${sliderHandle.left},${sliderHandle.top},${sliderHandle.right},${sliderHandle.bottom})"

        if (recognitionResult.candidateCount >= 2) {
            passReasons += "candidateCount=${recognitionResult.candidateCount}"
        } else {
            failReasons += "candidateCount=${recognitionResult.candidateCount}<2"
        }

        if (recognitionResult.confidence >= NEW_CAPTCHA_CONFIDENCE_THRESHOLD) {
            passReasons += "confidence=${recognitionResult.confidence}"
        } else {
            failReasons += "confidence=${recognitionResult.confidence}<${NEW_CAPTCHA_CONFIDENCE_THRESHOLD}"
        }

        val distance = recognitionResult.targetX - recognitionResult.sliderX
        val minDistance = maxOf(croppedBitmap.width * 0.05f, 40f)
        val maxDistance = croppedBitmap.width * 0.82f
        if (distance in minDistance..maxDistance) {
            passReasons += "distance=${distance.toInt()}"
        } else {
            failReasons += "distance=${distance.toInt()} not in ${minDistance.toInt()}..${maxDistance.toInt()}"
        }

        val verticalDelta = kotlin.math.abs(recognitionResult.targetY - recognitionResult.sliderY)
        val maxVerticalDelta = maxOf(croppedBitmap.height * 0.12f, 72f)
        if (verticalDelta <= maxVerticalDelta) {
            passReasons += "verticalDelta=${verticalDelta.toInt()}"
        } else {
            failReasons += "verticalDelta=${verticalDelta.toInt()}>${maxVerticalDelta.toInt()}"
        }

        val sliderRatio = recognitionResult.sliderX / croppedBitmap.width.toFloat()
        if (sliderRatio <= 0.35f) {
            passReasons += "sliderRatio=${"%.2f".format(sliderRatio)}"
        } else {
            failReasons += "sliderRatio=${"%.2f".format(sliderRatio)}>0.35"
        }

        val targetRatio = recognitionResult.targetX / croppedBitmap.width.toFloat()
        if (targetRatio in 0.20f..0.95f) {
            passReasons += "targetRatio=${"%.2f".format(targetRatio)}"
        } else {
            failReasons += "targetRatio=${"%.2f".format(targetRatio)} out-of-range"
        }

        val passed = failReasons.isEmpty()
        return CaptchaPreCheckResult(
            passed = passed,
            passReasons = passReasons,
            failReasons = failReasons,
            sliderHandle = sliderHandle
        )
    }

    private fun evaluateLightweightCaptchaPreCheck(
        decorView: View,
        anchorText: String?
    ): LightweightCaptchaPreCheckResult {
        val passReasons = mutableListOf<String>()
        val failReasons = mutableListOf<String>()
        passReasons += formatAnchorReason(anchorText)

        val fullBitmap = getBitmapFromView(decorView)
        if (fullBitmap == null) {
            failReasons += "decor-screenshot-failed"
            return LightweightCaptchaPreCheckResult(
                passed = false,
                passReasons = passReasons,
                failReasons = failReasons,
                sliderHandle = null,
                fullBitmap = null,
                croppedBitmap = null,
                cropTop = 0,
                cropBottom = 0
            )
        }

        saveDebugBitmap(fullBitmap, "full_decorview")

        val initialCropTop = fullBitmap.height * 40 / 100
        val sliderHandle = detectSliderHandle(fullBitmap, initialCropTop, null)
        if (sliderHandle != null) {
            passReasons += "blueHandle=(${sliderHandle.left},${sliderHandle.top},${sliderHandle.right},${sliderHandle.bottom})"
        } else {
            failReasons += "blueHandle-missing"
        }

        val cropBounds = buildCaptchaCropBounds(fullBitmap, sliderHandle)
        val cropTop = cropBounds.first
        val cropBottom = cropBounds.second
        val croppedBitmap = Bitmap.createBitmap(
            fullBitmap,
            0,
            cropTop,
            fullBitmap.width,
            cropBottom - cropTop
        )
        Log.record(
            TAG,
            "[前置检测] 裁剪区域: top=$cropTop, bottom=$cropBottom, size=${croppedBitmap.width}x${croppedBitmap.height}"
        )
        saveDebugBitmap(croppedBitmap, "cropped_captcha_area")

        sliderHandle?.let {
            val handleWidth = it.right - it.left
            val handleHeight = it.bottom - it.top

            val handleXRatio = it.centerX / fullBitmap.width.toFloat()
            if (handleXRatio <= 0.42f) {
                passReasons += "handleXRatio=${"%.2f".format(handleXRatio)}"
            } else {
                failReasons += "handleXRatio=${"%.2f".format(handleXRatio)}>0.42"
            }

            val handleYRatio = it.centerY / fullBitmap.height.toFloat()
            if (handleYRatio in 0.55f..0.95f) {
                passReasons += "handleYRatio=${"%.2f".format(handleYRatio)}"
            } else {
                failReasons += "handleYRatio=${"%.2f".format(handleYRatio)} out-of-range"
            }

            if (anchorText.isNullOrBlank()) {
                if (it.pixelCount >= 1400) {
                    passReasons += "handlePixels=${it.pixelCount}"
                } else {
                    failReasons += "handlePixels=${it.pixelCount}<1400"
                }

                if (handleWidth in 68..190 && handleHeight in 68..190) {
                    passReasons += "handleSize=${handleWidth}x${handleHeight}"
                } else {
                    failReasons += "handleSize=${handleWidth}x${handleHeight} out-of-range"
                }
            }
        }

        return LightweightCaptchaPreCheckResult(
            passed = failReasons.isEmpty(),
            passReasons = passReasons,
            failReasons = failReasons,
                sliderHandle = sliderHandle,
                fullBitmap = fullBitmap,
                croppedBitmap = croppedBitmap,
                cropTop = cropTop,
                cropBottom = cropBottom
            )
    }

    /*
     * 在 sliderView 附近查找大的 ImageView (验证码背景)
     * 策略：向上找父容器，然后在父容器中找尺寸最大的 ImageView
     */
    private fun findCaptchaImageView(sliderView: View): ImageView? {
        val parent = sliderView.parent as? ViewGroup ?: return null
        // 简单策略：遍历父容器子View，找面积最大的 ImageView
        var maxArea = 0
        var targetImage: ImageView? = null
        
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            if (child is ImageView && child.visibility == View.VISIBLE) {
                val area = child.width * child.height
                // 通常验证码图片高度会比较大，且不是小图标
                if (area > maxArea && child.width > 200) { 
                    maxArea = area
                    targetImage = child
                }
            }
        }
        return targetImage
    }

    private suspend fun recognizeCaptchaGapNative(view: View): Pair<Int, Float>? {
        return try {
            // 1. 从传入的 View 中获取 Bitmap (如果是全屏则传入 DecorView)
            val bitmap = getBitmapFromView(view) ?: return null

            saveDebugBitmap(bitmap, "input_to_model")

            // 2. 直接调用 TFLite 识别全图中的缺口位置
            // 根据你的演示截图，模型在全图中也能返回坐标
            val recognitionResult = SliderTFLite.identifyShared(view.context.applicationContext, bitmap) ?: return null
            val x1 = recognitionResult.targetX.toInt()
            val conf = recognitionResult.confidence

            Log.record(TAG, "TFLite 识别成功: 目标X=$x1, 置信度=$conf")

            // 因为是 1:1 截图，bitmap 的坐标系与屏幕物理像素坐标系是一致的
            return Pair(x1, conf)
        } catch (e: Exception) {
            Log.record(TAG, "TFLite 识别过程异常: ${e.message}")
            null
        }
    }

    /**
     * 获取 View 的截屏 Bitmap，使用 view.draw(canvas) 方式。
     *
     * 前提条件：屏幕需处于亮屏且解锁状态（由外部编排层如 AutoJS 保证）。
     * 模块自身不处理屏幕唤醒/解锁，聚焦于验证码识别与滑动操作。
     */
    private fun getBitmapFromView(view: View): Bitmap? {
        if (view.width <= 0 || view.height <= 0) return null

        return try {
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            view.draw(canvas)
            Log.record(TAG, "view.draw 截屏成功: ${bitmap.width}x${bitmap.height}")
            bitmap
        } catch (e: Exception) {
            Log.record(TAG, "view.draw 截屏失败: ${e.message}")
            null
        }
    }

    private fun calculateDistance(gapXInImage: Int, imageRealWidth: Int, bgView: View, sliderView: View): Float {
        // 因为我们传入的是 View 的截图 (getBitmapFromView)，所以 gapXInImage 已经是屏幕坐标系下的像素值
        // imageRealWidth 传入的是 bgView.width
        // 所以 scale 应该是 1.0
        val scale = bgView.width.toFloat() / imageRealWidth.toFloat()

        // 这里的计算可以简化，直接认为 gapXInImage 就是目标位置
        val distance = gapXInImage.toFloat()

        Log.record(TAG, "计算距离: GapX=$gapXInImage, Distance=$distance")
        return distance
    }

    @SuppressLint("SuspiciousIndentation")
    private suspend fun handleLegacySlideCaptcha(activity: Activity): ActivityHandleResult {
        var processingWindowAcquired = false
        // 防止处理过程中息屏
        val originalFlags = activity.window?.attributes?.flags ?: 0
        try {
            activity.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            // 率先触发外部解锁（屏幕不亮时页面不会渲染，后续 View 查找白费）
            val context = SimplePageManager.getContext()
            if (context != null) {
                CommandUtil.connect(context)
                UnlockUtil.triggerUnlock(context)
            }

            val searchStartTime = System.currentTimeMillis()
            val slideTextInDialog = SimplePageManager.tryGetTopView(OLD_SLIDE_VERIFY_TEXT_XPATH) ?: run {
                Log.record(TAG, "未找到旧版滑动验证文本，搜索耗时: ${System.currentTimeMillis() - searchStartTime}ms")
                logPrecheckSkip("legacy-text-anchor-missing", listOf("legacy text anchor not found"), emptyList())
                return ActivityHandleResult.SKIP_NON_RETRYABLE
            }
            Log.record(TAG, "发现旧版滑动验证文本: ${slideTextInDialog.getText()}, 搜索耗时: ${System.currentTimeMillis() - searchStartTime}ms")

            if (!captchaProcessingMutex.tryLock()) {
                Log.record(TAG, "验证码正在处理中，跳过本次处理")
                logRetryableFailure("legacy-captcha-processing-window-busy")
                return ActivityHandleResult.FAILED_RETRYABLE
            }
            processingWindowAcquired = true
            logAcceptedAfterSkip("legacy-text-anchor-present")
            
            // 减少等待时间
            delay(200L) // 从500ms减少到200ms
            
            // 使用旧版盲猜逻辑
            val findViewStartTime = System.currentTimeMillis()
            val sliderView = ViewHierarchyAnalyzer.findActualSliderView(slideTextInDialog) ?: run {
                Log.record(TAG, "无法找到滑块视图，查找耗时: ${System.currentTimeMillis() - findViewStartTime}ms")
                logRetryableFailure("legacy-slider-view-missing")
                return ActivityHandleResult.FAILED_RETRYABLE
            }
            Log.record(TAG, "滑块视图查找耗时: ${System.currentTimeMillis() - findViewStartTime}ms")
            
            val coordStartTime = System.currentTimeMillis()
            val (startX, startY, endX, endY) = calculateLegacySlideCoordinates(activity, sliderView) ?: run {
                Log.record(TAG, "坐标计算失败，计算耗时: ${System.currentTimeMillis() - coordStartTime}ms")
                logRetryableFailure("legacy-coordinate-calc-failed")
                return ActivityHandleResult.FAILED_RETRYABLE
            }
            Log.record(TAG, "坐标计算耗时: ${System.currentTimeMillis() - coordStartTime}ms")
            
            return if (executeSlide(sliderView, startX, startY, endX, endY)) {
                ActivityHandleResult.HANDLED
            } else {
                logRetryableFailure("legacy-execute-slide-failed")
                ActivityHandleResult.FAILED_RETRYABLE
            }
        } catch (e: Exception) {
            Log.record(TAG, "旧版处理出错: ${e.stackTraceToString()}")
            logRetryableFailure("legacy-exception")
            return ActivityHandleResult.FAILED_RETRYABLE
        } finally {
            // 恢复原始 Window Flag
            try {
                if ((originalFlags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) == 0) {
                    activity.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            } catch (_: Exception) {}
            if (processingWindowAcquired) {
                captchaProcessingMutex.unlock()
            }
        }
    }

    /*
     * 计算滑动验证码的坐标参数。
     * 
     * @param activity 当前Activity，用于获取屏幕信息
     * @param sliderView 滑块视图
     * @return 包含(startX, startY, endX, endY)的四元组，如果计算失败返回null
     */
    private fun calculateLegacySlideCoordinates(activity: Activity, sliderView: android.view.View): SlideCoordinates? {
        // 获取滑动区域的整体容器（滑块的父容器）
        val slideContainer = sliderView.parent as? android.view.ViewGroup ?: run {
          //  Log.captcha(TAG, "未能找到滑块容器")
            return null
        }
        
        // 获取屏幕尺寸信息
        val displayMetrics = activity.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        
        // 计算滑动区域的边界
        val containerLocation = IntArray(2)
        slideContainer.getLocationOnScreen(containerLocation)
        val containerX = containerLocation[0]
        val containerY = containerLocation[1]
        val containerWidth = slideContainer.width
        val containerHeight = slideContainer.height

        // 计算滑块位置
        val sliderLocation = IntArray(2)
        sliderView.getLocationOnScreen(sliderLocation)
        val sliderX = sliderLocation[0]
        val sliderY = sliderLocation[1]
        val sliderWidth = sliderView.width
        val sliderHeight = sliderView.height

        // 计算滑动起点（滑块中心稍微偏右，模拟手指按住滑块）
        val startX = sliderX + sliderWidth / 2f + SLIDE_START_OFFSET.toFloat() + Random.nextInt(-3, 4) // 添加随机偏移
        val startY = sliderY + sliderHeight / 2f + Random.nextInt(-2, 3)

        // 计算滑动终点
        val containerRightEdge = containerX + containerWidth
        val maxEndX = screenWidth - 50f // 距离屏幕右边缘50像素
        
        // 计算理想的滑动终点（容器右端减去边距）
        var endX = containerRightEdge - SLIDE_END_MARGIN.toFloat() + Random.nextInt(-5, 6) // 添加随机偏移
        
        // 确保滑动终点不超过屏幕边界
        if (endX > maxEndX) {
            endX = maxEndX
            Log.record(TAG, "调整滑动终点以适配屏幕边界")
        }
        // 确保滑动距离足够（至少滑块宽度的1.5倍）
        val minSlideDistance = sliderWidth * 1.5f
        val actualSlideDistance = endX - startX
        if (actualSlideDistance < minSlideDistance) {
            endX = startX + minSlideDistance + Random.nextInt(-3, 4) // 添加随机偏移
            Log.record(TAG, "调整滑动距离至最小要求: ${minSlideDistance}px")
        }
        val endY = startY // 保持水平滑动
        // 输出详细的调试信息
        Log.record(TAG, "屏幕信息: 尺寸=${screenWidth}x$screenHeight")
        Log.record(TAG, "滑动区域信息: 容器位置=[$containerX,$containerY], 尺寸=${containerWidth}x$containerHeight")
        Log.record(TAG, "滑块信息: 位置=[$sliderX,$sliderY], 尺寸=${sliderWidth}x${sliderHeight}")
        Log.record(TAG, "计算结果: 起点=[$startX,$startY], 终点=[$endX,$endY], 滑动距离=${endX-startX}px")

        return SlideCoordinates(startX, startY, endX, endY)
    }
    
    // 用于新版逻辑：根据计算出的距离滑动
    private suspend fun performSlide(activity: Activity, sliderView: View, distance: Float): Boolean {
        val location = IntArray(2)
        sliderView.getLocationOnScreen(location)
        val startX = location[0] + sliderView.width / 2f
        val startY = location[1] + sliderView.height / 2f
        
        // 计算终点
        val endX = startX + distance
        val endY = startY + Random.nextInt(-2, 3) // 微小抖动

        return executeSlide(sliderView, startX, startY, endX, endY)
    }

    // 真正的滑动执行和结果检查（系统级输入优先，本地 dispatch 备选）
    private suspend fun executeSlide(sliderView: View, startX: Float, startY: Float, endX: Float, endY: Float): Boolean {
        val slideDuration = Random.nextLong(SLIDE_DURATION_MIN, SLIDE_DURATION_MAX + 1)
        
        Log.record(TAG, "执行滑动: ($startX, $startY) -> ($endX, $endY), 时长: $slideDuration")

        val localStartTime = System.currentTimeMillis()
        val dispatched = MotionEventSimulator.simulateSwipe(
            view = sliderView,
            startX = startX,
            startY = startY,
            endX = endX,
            endY = endY,
            duration = slideDuration
        )

        if (dispatched) {
            Log.record(TAG, "[滑动路径] path=view-dispatch, 耗时=${System.currentTimeMillis() - localStartTime}ms")
        } else {
            Log.record(TAG, "[滑动路径] view-dispatch 失败(view.isShown=false)，尝试 shell input 兜底")
            val shellOk = SystemInputSwiper.swipe(startX, startY, endX, endY, slideDuration)
            if (!shellOk) {
                Log.record(TAG, "[滑动路径] shell input 兜底也失败")
                return false
            }
            Log.record(TAG, "[滑动路径] path=shell-input, 耗时=${System.currentTimeMillis() - localStartTime}ms")
        }

        delay(POST_SLIDE_CHECK_DELAY_MS)
        val checkStartTime = System.currentTimeMillis()
        val result = checkCaptchaTextGoneLegacyOnly()
        Log.record(TAG, "[滑动判定] 检查耗时=${System.currentTimeMillis() - checkStartTime}ms, 路径=local-dispatch-only, 结果=$result")
        return result
    }

    /**
     * 在指定 View 上执行滑动（全屏截图模式）。
     * 模型返回的坐标是 View 局部坐标系，需要转换为屏幕坐标。
     */
    private suspend fun executeSlideOnView(
        view: View,
        localStartX: Float,
        localStartY: Float,
        localEndX: Float,
        localEndY: Float,
        beforeSnapshot: CaptchaVisualSnapshot,
        cropTop: Int,
        cropBottom: Int
    ): Boolean {
        // 将 View 局部坐标转换为屏幕坐标
        val viewLocation = IntArray(2)
        view.getLocationOnScreen(viewLocation)
        val screenStartX = localStartX + viewLocation[0]
        val screenStartY = localStartY + viewLocation[1]
        val screenEndX = localEndX + viewLocation[0]
        val screenEndY = localEndY + viewLocation[1]

        val slideDuration = Random.nextLong(SLIDE_DURATION_MIN, SLIDE_DURATION_MAX + 1)

        Log.record(TAG, "执行滑动(全屏模式): 局部(${localStartX.toInt()},${localStartY.toInt()})->(${localEndX.toInt()},${localEndY.toInt()}), 屏幕(${screenStartX.toInt()},${screenStartY.toInt()})->(${screenEndX.toInt()},${screenEndY.toInt()}), 时长: ${slideDuration}ms")

        val localStartTime = System.currentTimeMillis()
        val dispatched = MotionEventSimulator.simulateSwipe(
            view = view,
            startX = screenStartX,
            startY = screenStartY,
            endX = screenEndX,
            endY = screenEndY,
            duration = slideDuration
        )

        if (dispatched) {
            Log.record(TAG, "[滑动路径] path=view-dispatch, 耗时=${System.currentTimeMillis() - localStartTime}ms")
        } else {
            Log.record(TAG, "[滑动路径] view-dispatch 失败(view.isShown=false)，尝试 shell input 兜底")
            val shellOk = SystemInputSwiper.swipe(screenStartX, screenStartY, screenEndX, screenEndY, slideDuration)
            if (!shellOk) {
                Log.record(TAG, "[滑动路径] shell input 兜底也失败")
                return false
            }
            Log.record(TAG, "[滑动路径] path=shell-input, 耗时=${System.currentTimeMillis() - localStartTime}ms")
        }

        delay(POST_SLIDE_CHECK_DELAY_MS)
        val checkStartTime = System.currentTimeMillis()
        Log.record(TAG, "[截图复核] 滑动后开始截图验证...")
        var result = verifyCaptchaSolvedByScreenshotOnce(view, beforeSnapshot, cropTop, cropBottom)
        if (!result) {
            Log.record(TAG, "[截图复核] 首次验证失败，尝试校正滑动...")
            result = attemptCorrectiveSwipeIfNeeded(view, cropTop, cropBottom)
        }
        Log.record(TAG, "[滑动判定] 路径=local-dispatch-only, 检查耗时=${System.currentTimeMillis() - checkStartTime}ms, 最终结果=$result")
        return result
    }

    private fun checkCaptchaTextGoneLegacyOnly(): Boolean {
        // 检查旧版和新版文本是否都不存在了
        val oldText = SimplePageManager.tryGetTopView(OLD_SLIDE_VERIFY_TEXT_XPATH)
        val newText = SimplePageManager.tryGetTopView(NEW_SLIDE_VERIFY_TEXT_XPATH)
        
        return if (oldText == null && newText == null) {
            Log.record(TAG, "验证码文本已消失，滑动成功。")
            true
        } else {
            Log.record(TAG, "验证码文本仍然存在，滑动可能失败。")
            false
        }
    }

    private suspend fun verifyCaptchaSolvedByScreenshotOnce(
        view: View,
        beforeSnapshot: CaptchaVisualSnapshot,
        cropTop: Int,
        cropBottom: Int
    ): Boolean {
        val oldText = SimplePageManager.tryGetTopView(OLD_SLIDE_VERIFY_TEXT_XPATH)
        val newText = SimplePageManager.tryGetTopView(NEW_SLIDE_VERIFY_TEXT_XPATH)
        val textStillVisible = oldText != null || newText != null

        val afterFullBitmap = getBitmapFromView(view) ?: run {
            Log.record(TAG, "滑动后截图失败，按失败处理")
            return false
        }
        saveDebugBitmap(afterFullBitmap, "post_slide_full_decorview")

        val safeCropTop = cropTop.coerceIn(0, (afterFullBitmap.height - 1).coerceAtLeast(0))
        val safeCropBottom = cropBottom.coerceIn(safeCropTop + 1, afterFullBitmap.height)
        val afterCroppedBitmap = Bitmap.createBitmap(
            afterFullBitmap,
            0,
            safeCropTop,
            afterFullBitmap.width,
            safeCropBottom - safeCropTop
        )
        saveDebugBitmap(afterCroppedBitmap, "post_slide_cropped_captcha_area")

        val afterRecognition = try {
            SliderTFLite.identifyShared(view.context.applicationContext, afterCroppedBitmap)
        } catch (e: Exception) {
            Log.record(TAG, "滑动后二次识别异常: ${e.message}")
            null
        }

        val beforeRecognition = beforeSnapshot.recognitionResult
        val diffRatio = calculateBitmapDifferenceRatio(beforeSnapshot.croppedBitmap, afterCroppedBitmap)

        Log.record(
            TAG,
            "截图校验: textStillVisible=$textStillVisible, diffRatio=$diffRatio, beforeRecognition=${formatRecognition(beforeRecognition)}, afterRecognition=${formatRecognition(afterRecognition)}"
        )

        if (afterRecognition != null) {
            if (isSolvedResidualDetection(beforeRecognition, afterRecognition, afterCroppedBitmap)) {
                Log.record(TAG, "截图校验命中 residual-target-only，按验证通过处理")
            } else {
                Log.record(TAG, "截图校验失败：滑动后仍可识别到滑块/缺口，判定验证码未通过")
                return false
            }
        }

        if (diffRatio < 0.015f) {
            Log.record(TAG, "截图校验失败：滑动前后画面变化过小，疑似误报成功")
            return false
        }

        if (textStillVisible) {
            Log.record(TAG, "截图校验失败：验证码提示文本仍存在")
            return false
        }

        Log.record(TAG, "截图校验通过：滑动后未再识别到验证码，且页面状态已变化")
        return true
    }

    private suspend fun attemptCorrectiveSwipeIfNeeded(view: View, cropTop: Int, cropBottom: Int): Boolean {
        val probeFullBitmap = getBitmapFromView(view) ?: return false
        saveDebugBitmap(probeFullBitmap, "correction_probe_full_decorview")

        val safeCropTop = cropTop.coerceIn(0, (probeFullBitmap.height - 1).coerceAtLeast(0))
        val safeCropBottom = cropBottom.coerceIn(safeCropTop + 1, probeFullBitmap.height)
        val probeCroppedBitmap = Bitmap.createBitmap(
            probeFullBitmap,
            0,
            safeCropTop,
            probeFullBitmap.width,
            safeCropBottom - safeCropTop
        )
        saveDebugBitmap(probeCroppedBitmap, "correction_probe_cropped_captcha_area")

        val probeRecognition = try {
            SliderTFLite.identifyShared(view.context.applicationContext, probeCroppedBitmap)
        } catch (e: Exception) {
            Log.record(TAG, "校正探测识别异常: ${e.message}")
            null
        } ?: run {
            Log.record(TAG, "校正探测未识别到可继续修正的目标")
            return false
        }

        val sliderHandle = detectSliderHandle(probeFullBitmap, cropTop, probeRecognition) ?: run {
            Log.record(TAG, "校正探测未定位到滑块手柄")
            return false
        }

        val correctionDistance = estimateCorrectionDistance(probeRecognition)
        if (kotlin.math.abs(correctionDistance) !in 4f..36f) {
            Log.record(TAG, "校正距离超出允许范围: $correctionDistance")
            return false
        }

        val correctionEndX = sliderHandle.centerX + correctionDistance
        val correctionEndY = sliderHandle.centerY
        val correctionDuration = Random.nextLong(
            CORRECTIVE_SLIDE_DURATION_MIN,
            CORRECTIVE_SLIDE_DURATION_MAX
        )

        Log.record(
            TAG,
            "[校正滑动] handle=(${sliderHandle.centerX.toInt()},${sliderHandle.centerY.toInt()}), correctionDistance=${correctionDistance.toInt()}, 路径=local-dispatch-only"
        )

        val dispatched = MotionEventSimulator.simulateSwipe(
            view = view,
            startX = sliderHandle.centerX,
            startY = sliderHandle.centerY,
            endX = correctionEndX,
            endY = correctionEndY,
            duration = correctionDuration
        )
        if (dispatched) {
            Log.record(TAG, "[校正滑动] path=view-dispatch")
        } else {
            Log.record(TAG, "[校正滑动] view-dispatch 失败，尝试 shell input 兜底")
            SystemInputSwiper.swipe(sliderHandle.centerX, sliderHandle.centerY, correctionEndX, correctionEndY, correctionDuration)
            Log.record(TAG, "[校正滑动] path=shell-input")
        }

        delay(700L)
        return verifyCaptchaSolvedByScreenshotOnce(
            view = view,
            beforeSnapshot = CaptchaVisualSnapshot(
                fullBitmap = probeFullBitmap,
                croppedBitmap = probeCroppedBitmap,
                recognitionResult = probeRecognition
            ),
            cropTop = cropTop,
            cropBottom = cropBottom
        )
    }

    private fun buildCaptchaCropBounds(fullBitmap: Bitmap, sliderHandle: SliderHandleDetection?): Pair<Int, Int> {
        if (sliderHandle == null) {
            val fallbackTop = (fullBitmap.height * 28 / 100).coerceIn(0, fullBitmap.height - 1)
            val fallbackBottom = (fullBitmap.height * 88 / 100).coerceIn(fallbackTop + 1, fullBitmap.height)
            return fallbackTop to fallbackBottom
        }

        val cropTop = maxOf(0, sliderHandle.top - (fullBitmap.height * 42 / 100))
        val cropBottom = minOf(fullBitmap.height, sliderHandle.bottom + (fullBitmap.height * 8 / 100))
        return if (cropBottom - cropTop >= fullBitmap.height * 22 / 100) {
            cropTop to cropBottom
        } else {
            val fallbackTop = (fullBitmap.height * 28 / 100).coerceIn(0, fullBitmap.height - 1)
            val fallbackBottom = (fullBitmap.height * 88 / 100).coerceIn(fallbackTop + 1, fullBitmap.height)
            fallbackTop to fallbackBottom
        }
    }

    private fun isSolvedResidualDetection(
        beforeRecognition: SliderTFLite.SlideRecognitionResult?,
        afterRecognition: SliderTFLite.SlideRecognitionResult,
        croppedBitmap: Bitmap
    ): Boolean {
        val before = beforeRecognition ?: return false
        if (afterRecognition.candidateCount != 1) return false

        val targetDeltaX = kotlin.math.abs(afterRecognition.sliderX - before.targetX)
        val targetDeltaY = kotlin.math.abs(afterRecognition.sliderY - before.targetY)
        val maxDeltaX = maxOf(croppedBitmap.width * 0.12f, 90f)
        val maxDeltaY = maxOf(croppedBitmap.height * 0.10f, 80f)
        val collapsedBox = kotlin.math.abs(afterRecognition.targetX - afterRecognition.sliderX) <= 1f &&
            kotlin.math.abs(afterRecognition.targetY - afterRecognition.sliderY) <= 1f

        return collapsedBox && targetDeltaX <= maxDeltaX && targetDeltaY <= maxDeltaY
    }

    private fun estimateCorrectionDistance(recognitionResult: SliderTFLite.SlideRecognitionResult): Float {
        val rawDistance = recognitionResult.targetX - recognitionResult.sliderX
        return if (kotlin.math.abs(rawDistance) < 1f) {
            12f
        } else if (rawDistance > 0f) {
            rawDistance + 8f
        } else {
            rawDistance - 8f
        }
    }

    private fun detectSliderHandle(
        fullBitmap: Bitmap,
        cropTop: Int,
        recognitionResult: SliderTFLite.SlideRecognitionResult?
    ): SliderHandleDetection? {
        val searchTop = if (recognitionResult != null) {
            (cropTop + recognitionResult.sliderY + 120f).toInt().coerceIn(0, fullBitmap.height - 1)
        } else {
            maxOf(cropTop + 40, fullBitmap.height * 55 / 100).coerceIn(0, fullBitmap.height - 1)
        }
        val searchBottom = (fullBitmap.height - 140).coerceAtLeast(searchTop + 1).coerceAtMost(fullBitmap.height)
        val searchLeft = 0
        val searchRight = (fullBitmap.width * 45 / 100).coerceAtMost(fullBitmap.width)

        var minX = Int.MAX_VALUE
        var minY = Int.MAX_VALUE
        var maxX = -1
        var maxY = -1
        var pixelCount = 0

        var y = searchTop
        while (y < searchBottom) {
            var x = searchLeft
            while (x < searchRight) {
                val pixel = fullBitmap.getPixel(x, y)
                if (isLikelySliderHandleBlue(pixel)) {
                    if (x < minX) minX = x
                    if (y < minY) minY = y
                    if (x > maxX) maxX = x
                    if (y > maxY) maxY = y
                    pixelCount++
                }
                x += 1
            }
            y += 1
        }

        if (pixelCount < 1200 || maxX <= minX || maxY <= minY) {
            Log.record(
                TAG,
                "滑块手柄检测失败: searchRegion=($searchLeft,$searchTop,$searchRight,$searchBottom), pixelCount=$pixelCount"
            )
            return null
        }

        val width = maxX - minX
        val height = maxY - minY
        if (width !in 60..220 || height !in 60..220) {
            Log.record(
                TAG,
                "滑块手柄检测失败: boundsSize=${width}x$height, pixelCount=$pixelCount"
            )
            return null
        }

        val aspectRatio = width.toFloat() / height.toFloat()
        if (aspectRatio !in 0.75f..1.35f) {
            Log.record(
                TAG,
                "滑块手柄检测失败: aspectRatio=$aspectRatio, boundsSize=${width}x$height, pixelCount=$pixelCount"
            )
            return null
        }

        return SliderHandleDetection(
            centerX = (minX + maxX) / 2f,
            centerY = (minY + maxY) / 2f,
            left = minX,
            top = minY,
            right = maxX,
            bottom = maxY,
            pixelCount = pixelCount
        )
    }

    private fun isLikelySliderHandleBlue(pixel: Int): Boolean {
        val red = android.graphics.Color.red(pixel)
        val green = android.graphics.Color.green(pixel)
        val blue = android.graphics.Color.blue(pixel)
        return blue >= 170 && green >= 80 && red <= 120 && blue - red >= 70 && blue - green >= 25
    }

    private fun formatAnchorReason(anchorText: String?): String {
        return if (anchorText.isNullOrBlank()) {
            "text-anchor-missing"
        } else {
            "text-anchor=$anchorText"
        }
    }

    private fun formatRecognition(result: SliderTFLite.SlideRecognitionResult?): String {
        return if (result == null) {
            "none"
        } else {
            "slider=(${result.sliderX.toInt()},${result.sliderY.toInt()}), target=(${result.targetX.toInt()},${result.targetY.toInt()}), confidence=${result.confidence}, candidates=${result.candidateCount}"
        }
    }

    private fun isMainThread(): Boolean {
        return Looper.myLooper() == Looper.getMainLooper()
    }

    private fun calculateBitmapDifferenceRatio(before: Bitmap, after: Bitmap): Float {
        if (before.width != after.width || before.height != after.height) {
            return 1f
        }

        val sampleStepX = maxOf(1, before.width / 48)
        val sampleStepY = maxOf(1, before.height / 48)
        var totalDiff = 0L
        var sampleCount = 0

        var y = 0
        while (y < before.height) {
            var x = 0
            while (x < before.width) {
                val beforePixel = before.getPixel(x, y)
                val afterPixel = after.getPixel(x, y)
                totalDiff += kotlin.math.abs(android.graphics.Color.red(beforePixel) - android.graphics.Color.red(afterPixel))
                totalDiff += kotlin.math.abs(android.graphics.Color.green(beforePixel) - android.graphics.Color.green(afterPixel))
                totalDiff += kotlin.math.abs(android.graphics.Color.blue(beforePixel) - android.graphics.Color.blue(afterPixel))
                sampleCount++
                x += sampleStepX
            }
            y += sampleStepY
        }

        if (sampleCount == 0) return 0f
        return totalDiff.toFloat() / (sampleCount * 255f * 3f)
    }
}
