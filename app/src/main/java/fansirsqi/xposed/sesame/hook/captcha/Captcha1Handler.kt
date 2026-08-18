package fansirsqi.xposed.sesame.hook.captcha

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.hook.view.PageMonitor
import fansirsqi.xposed.sesame.hook.view.PageMonitor.ActivityHandleResult
import fansirsqi.xposed.sesame.hook.view.ViewImage
import fansirsqi.xposed.sesame.hook.view.ViewHierarchyAnalyzer
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * 滑动坐标四元组（屏幕坐标系），封装滑动起点和终点。
 */
private data class SlideCoordinates(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float
)

/**
 * 验证码处理器 - 旧版 XPath（XRiverActivity 原生 View 滑块验证码）。
 *
 * 流程：解锁 -> XPath 定位"向右滑动验证"文本锚点 -> 定位滑块 View ->
 * 计算滑动坐标 -> 派发滑动 -> 文案消失判定。
 */
class Captcha1Handler : BaseCaptchaHandler(), PageMonitor.ActivityFocusHandler {

    companion object {
        /** 找到文本锚点后等待视图布局稳定的时间（ms） */
        private const val VIEW_SETTLE_DELAY_MS = 200L

        /** 滑动终点距屏幕右边缘的最小余量（px） */
        private const val END_X_SCREEN_MARGIN = 50f

        /** 最小滑动距离相对滑块宽度的倍数：保证滑块能到达轨道末端 */
        private const val MIN_SLIDE_DISTANCE_FACTOR = 1.5f

        /** 旧版验证码提示文案 XPath（CaptchaDialog 原生 View 树） */
        const val OLD_SLIDE_VERIFY_TEXT_XPATH = "//TextView[contains(@text,'向右滑动验证')]"

        /** 新版文案 XPath（历史互查遗留：新版文案在 WebView 虚拟树，Dialog 原生树实际查不到） */
        private const val NEW_SLIDE_VERIFY_TEXT_XPATH = "//View[contains(@text,'请拖动滑块完成拼图')]"
    }

    /** 旧版验证码提示文案是否在屏（CaptchaDialog 原生 View 树 XPath 探测） */
    override suspend fun isAnchorVisible(): Boolean {
        return PageMonitor.tryGetTopView(OLD_SLIDE_VERIFY_TEXT_XPATH) != null
    }

    override suspend fun handleActivity(activity: Activity, root: ViewImage): ActivityHandleResult {
        return runWithProcessingWindow(activity) {
            var processingWindowAcquired = false
            try {
                // ① 率先触发内置解锁（屏幕不亮时页面不会渲染，后续 View 查找白费）
                if (!performUnlockOrFail()) {
                    return@runWithProcessingWindow ActivityHandleResult.FAILED_RETRYABLE
                }

                // ② XPath 定位文本锚点，确认为旧版滑块验证码页
                val searchStartTime = System.currentTimeMillis()
                val slideTextInDialog = PageMonitor.tryGetTopView(OLD_SLIDE_VERIFY_TEXT_XPATH) ?: run {
                    Log.record(
                        TAG,
                        "未找到旧版滑动验证文本，搜索耗时: ${System.currentTimeMillis() - searchStartTime}ms"
                    )
                    logPrecheckSkip("旧版文本锚点缺失", listOf("旧版文本锚点未找到"), emptyList())
                    return@runWithProcessingWindow ActivityHandleResult.SKIP_NON_RETRYABLE
                }
                Log.record(
                    TAG,
                    "发现旧版滑动验证文本: ${slideTextInDialog.getText()}, 搜索耗时: ${System.currentTimeMillis() - searchStartTime}ms"
                )

                // ③ 获取处理窗口互斥锁
                if (!acquireProcessingWindow("旧版验证码处理窗口被占用")) {
                    return@runWithProcessingWindow ActivityHandleResult.FAILED_RETRYABLE
                }
                processingWindowAcquired = true
                logAcceptedAfterSkip("旧版文本锚点命中")

                delay(VIEW_SETTLE_DELAY_MS)

                // ④ 定位滑块 View
                val findViewStartTime = System.currentTimeMillis()
                val sliderView = ViewHierarchyAnalyzer.findActualSliderView(slideTextInDialog) ?: run {
                    Log.record(
                        TAG,
                        "无法找到滑块视图，查找耗时: ${System.currentTimeMillis() - findViewStartTime}ms"
                    )
                    logRetryableFailure("旧版滑块视图缺失")
                    return@runWithProcessingWindow ActivityHandleResult.FAILED_RETRYABLE
                }
                Log.record(TAG, "滑块视图查找耗时: ${System.currentTimeMillis() - findViewStartTime}ms")

                // ⑤ 计算滑动坐标
                val coordStartTime = System.currentTimeMillis()
                val coordinates = calculateSlideCoordinates(activity, sliderView) ?: run {
                    Log.record(
                        TAG,
                        "坐标计算失败，计算耗时: ${System.currentTimeMillis() - coordStartTime}ms"
                    )
                    logRetryableFailure("旧版坐标计算失败")
                    return@runWithProcessingWindow ActivityHandleResult.FAILED_RETRYABLE
                }
                Log.record(TAG, "坐标计算耗时: ${System.currentTimeMillis() - coordStartTime}ms")

                // ⑥ 派发滑动并判定
                if (executeSlideAndVerify(sliderView, coordinates)) {
                    ActivityHandleResult.HANDLED
                } else {
                    logRetryableFailure("旧版滑动执行失败")
                    ActivityHandleResult.FAILED_RETRYABLE
                }
            } finally {
                if (processingWindowAcquired) {
                    releaseProcessingWindow()
                }
            }
        }
    }

    /**
     * 根据滑动轨道容器与滑块 View 的屏幕位置计算滑动坐标（盲猜逻辑，无缺口识别）。
     *
     * 规则：起点取滑块中心稍偏右（模拟人手按住），终点取轨道右端留边距，
     * 并保证不超出屏幕右缘、滑动距离不小于滑块宽度的 1.5 倍；坐标附加随机抖动。
     *
     * @return 计算失败（滑块无父容器等）返回 null
     */
    private fun calculateSlideCoordinates(activity: Activity, sliderView: View): SlideCoordinates? {
        // 滑动轨道容器（滑块的父容器）
        val slideContainer = sliderView.parent as? ViewGroup ?: return null

        val screen = activity.resources.displayMetrics
        val screenWidth = screen.widthPixels
        val screenHeight = screen.heightPixels

        val containerPos = IntArray(2)
        slideContainer.getLocationOnScreen(containerPos)
        val containerWidth = slideContainer.width
        val containerHeight = slideContainer.height

        val sliderPos = IntArray(2)
        sliderView.getLocationOnScreen(sliderPos)
        val sliderWidth = sliderView.width
        val sliderHeight = sliderView.height

        // 起点：滑块中心稍偏右 + 随机抖动，模拟人手按住滑块
        val startX = sliderPos[0] + sliderWidth / 2f + SLIDE_START_OFFSET + Random.nextInt(-3, 4)
        val startY = sliderPos[1] + sliderHeight / 2f + Random.nextInt(-2, 3)

        // 终点：轨道右端留边距；不超出屏幕右缘，且滑动距离不小于滑块宽度的 1.5 倍
        val containerRightEdge = containerPos[0] + containerWidth
        val maxEndX = screenWidth - END_X_SCREEN_MARGIN
        val minSlideDistance = sliderWidth * MIN_SLIDE_DISTANCE_FACTOR

        var endX = containerRightEdge - SLIDE_END_MARGIN + Random.nextInt(-5, 6).toFloat()
        if (endX > maxEndX) {
            endX = maxEndX
            Log.record(TAG, "调整滑动终点以适配屏幕边界")
        }
        if (endX - startX < minSlideDistance) {
            endX = startX + minSlideDistance + Random.nextInt(-3, 4)
            Log.record(TAG, "调整滑动距离至最小要求: ${minSlideDistance}px")
        }
        val endY = startY // 保持水平滑动

        Log.record(TAG, "屏幕信息: 尺寸=${screenWidth}x$screenHeight")
        Log.record(TAG, "滑动区域信息: 容器位置=[${containerPos[0]},${containerPos[1]}], 尺寸=${containerWidth}x$containerHeight")
        Log.record(TAG, "滑块信息: 位置=[${sliderPos[0]},${sliderPos[1]}], 尺寸=${sliderWidth}x${sliderHeight}")
        Log.record(TAG, "计算结果: 起点=[$startX,$startY], 终点=[$endX,$endY], 滑动距离=${endX - startX}px")

        return SlideCoordinates(startX, startY, endX, endY)
    }

    /**
     * 派发滑动并判定结果：滑动后检查验证码提示文案是否消失。
     */
    private suspend fun executeSlideAndVerify(sliderView: View, coordinates: SlideCoordinates): Boolean {
        val duration = Random.nextLong(SLIDE_DURATION_MIN, SLIDE_DURATION_MAX + 1)
        Log.record(
            TAG,
            "执行滑动: (${coordinates.startX}, ${coordinates.startY}) -> (${coordinates.endX}, ${coordinates.endY}), 时长: $duration"
        )

        if (!dispatchSwipeWithFallback(
                view = sliderView,
                startX = coordinates.startX,
                startY = coordinates.startY,
                endX = coordinates.endX,
                endY = coordinates.endY,
                duration = duration
            )
        ) {
            return false
        }

        delay(POST_SLIDE_CHECK_DELAY_MS)
        val checkStartTime = System.currentTimeMillis()
        val solved = isCaptchaTextGone()
        Log.record(TAG, "[滑动判定] 检查耗时=${System.currentTimeMillis() - checkStartTime}ms, 路径=xpath-text, 结果=$solved")
        return solved
    }

    /**
     * 验证码提示文案是否已消失（新旧两版互查，任一仍在即视为未通过）。
     */
    private fun isCaptchaTextGone(): Boolean {
        val oldText = PageMonitor.tryGetTopView(OLD_SLIDE_VERIFY_TEXT_XPATH)
        val newText = PageMonitor.tryGetTopView(NEW_SLIDE_VERIFY_TEXT_XPATH)

        return if (oldText == null && newText == null) {
            Log.record(TAG, "验证码文本已消失，滑动成功。")
            true
        } else {
            Log.record(TAG, "验证码文本仍然存在，滑动可能失败。")
            false
        }
    }
}
