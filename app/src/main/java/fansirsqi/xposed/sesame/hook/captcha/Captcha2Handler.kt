package fansirsqi.xposed.sesame.hook.captcha

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import fansirsqi.xposed.sesame.core.app.CommandUtil
import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.hook.ApplicationHook
import fansirsqi.xposed.sesame.hook.view.PageMonitor
import fansirsqi.xposed.sesame.hook.view.PageMonitor.ActivityHandleResult
import fansirsqi.xposed.sesame.hook.view.ViewImage
import fansirsqi.xposed.sesame.hook.view.SliderTFLite
import fansirsqi.xposed.sesame.hook.view.WebViewAccessibilityFinder
import kotlinx.coroutines.delay
import kotlin.random.Random

/** 滑动执行计划（DecorView 局部坐标系） */
private data class SlidePlan(
    val view: View,
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val cropTop: Int,
    val cropBottom: Int
)

/** 滑动准备阶段结果：Ready=携带可执行计划；Terminate=提前结束（原因已记录日志） */
private sealed interface SlidePreparation {
    data class Ready(val plan: SlidePlan) : SlidePreparation
    data class Terminate(val result: ActivityHandleResult) : SlidePreparation
}

/**
 * 验证码处理器 - 新版图像识别（XRiverTransActivity$Main / NebulaTransActivity$Main 的 WebView 滑块验证码）。
 *
 * 主流程（阶段函数按调用顺序）：
 * ① [awaitRpcConfirmation] RPC 信号闸门 -> ② 解锁 -> ③ 等待渲染 ->
 * ④ [probeAnchorText] 无障碍文本锚点探测 -> ⑤ [evaluateLightweightPreCheck] 截图轻量前置检测 ->
 * ⑦ [prepareSlidePlan] 模型识别 + 深度前置 + 构建滑动计划 -> ⑧ [executeSlideOnView] 派发滑动 + 文案消失轮询复核（必要时校正滑动）
 */
class Captcha2Handler : BaseCaptchaHandler(), PageMonitor.ActivityFocusHandler {

    companion object {
        /** 模型识别置信度门槛 */
        private const val MODEL_CONFIDENCE_THRESHOLD = 0.55f

        /** 校正滑动时长范围（ms）：小幅修正用短时长 */
        private const val CORRECTIVE_SLIDE_DURATION_MIN = 420L
        private const val CORRECTIVE_SLIDE_DURATION_MAX = 650L

        /** 校正滑动距离允许范围（px）：过小说明已对齐，过大说明识别异常 */
        private const val CORRECTION_MIN_DISTANCE = 4f
        private const val CORRECTION_MAX_DISTANCE = 36f

        /** 校正后等待文案消失的时长（ms） */
        private const val CORRECTION_SETTLE_DELAY_MS = 700L

        /** RPC 信号等待超时：H5 发起验证码 RPC 通常滞后 onResume 0.2~1s，8s 冗余覆盖慢网络 */
        private const val RPC_SIGNAL_WAIT_TIMEOUT_MS = 8_000L

        /** 页面渲染稳定等待（ms）：onResume 后 H5 需要时间完成验证码绘制 */
        private const val RENDER_SETTLE_DELAY_MS = 1_200L

        /** 新版验证码提示文案（无障碍探测关键词，文案位于 WebView H5 虚拟树中） */
        const val NEW_CAPTION_ANCHOR_TEXT = "请拖动滑块完成拼图"

        /** 文案消失判定总超时：实测验证通过后页面约 1.5s 关闭，3.5s 覆盖慢设备 */
        private const val CAPTION_GONE_TIMEOUT_MS = 3_500L

        /** 文案轮询间隔（ms） */
        private const val CAPTION_POLL_INTERVAL_MS = 500L

        /** "消失"二次确认间隔（ms）：防刷新瞬间虚拟树重建造成的虚灭 */
        private const val CAPTION_GONE_CONFIRM_MS = 300L

        // ---- 轻量前置检测：手柄位置/形状判定阈值 ----

        /** 手柄中心横坐标占比上限（滑块初始必在轨道左侧） */
        private const val HANDLE_X_RATIO_MAX = 0.42f

        /** 手柄中心纵坐标占比合法区间（验证码位于屏幕中下部） */
        private const val HANDLE_Y_RATIO_MIN = 0.55f
        private const val HANDLE_Y_RATIO_MAX = 0.95f

        // ---- 识别失败刷新流程阈值 ----

        /** gap 识别失败时最大刷新次数（每次点击刷新按钮换一张新图重试） */
        private const val MAX_REFRESH_ATTEMPTS = 2

        /** 点击刷新按钮后等待新图加载完成的时长（ms） */
        private const val REFRESH_SETTLE_DELAY_MS = 1_800L

        /** 刷新按钮点击所需最小置信度 */
        private const val REFRESH_CONFIDENCE_MIN = 0.4f

        /** 无文本锚点时手柄最小像素数（有锚点时放宽，靠锚点兜底） */
        private const val HANDLE_MIN_PIXELS_NO_ANCHOR = 1400

        /** 手柄包围盒边长合法区间（px） */
        private const val HANDLE_SIZE_MIN = 68
        private const val HANDLE_SIZE_MAX = 190

        // ---- 手柄像素扫描：搜索窗口与有效性阈值 ----

        /** 扫描区底部预留边距（px）：避开底部导航区 */
        private const val HANDLE_SCAN_BOTTOM_MARGIN = 140

        /** 扫描区右边界：屏宽的 45/100（滑块初始在左侧） */
        private const val HANDLE_SCAN_X_LIMIT = 45

        /** 手柄蓝像素数下限 */
        private const val HANDLE_MIN_PIXELS = 1200

        /** 手柄包围盒边长合法区间（px） */
        private const val HANDLE_BOUNDS_MIN = 60
        private const val HANDLE_BOUNDS_MAX = 220

        /** 从下往上扫描时行间空白容差（px）：防手柄内部小空洞导致提前分段 */
        private const val HANDLE_GAP_TOLERANCE = 5

        /** 手柄宽高比合法区间 */
        private const val HANDLE_ASPECT_RATIO_MIN = 0.75f
        private const val HANDLE_ASPECT_RATIO_MAX = 1.35f

        // ---- 拼图卡片边界反推（训练集 484/484 样本恒定几何，卡片内部坐标系） ----
        // 训练图为 puzzleContainer 区域截图（288×270），模型推理输入应与该分布一致：
        // 旧逻辑"以手柄为中心横带"带入标题/装饰/轨道等训练外元素，是 gap/refresh 识别率低的主因。

        /** 手柄宽 / 卡片宽 = 41.92 / 288.2 */
        private const val HANDLE_WIDTH_TO_CARD_WIDTH = 0.1455f

        /** 卡片宽 / 卡片高 = 288.2 / 269.5 */
        private const val CARD_WIDTH_TO_HEIGHT = 1.0694f

        /** 手柄 top 距卡片顶部 / 卡片高 = 204.57 / 269.5 */
        private const val HANDLE_TOP_TO_CARD_HEIGHT = 0.7595f

        /** 反推卡片宽合法区间（相对屏宽）：验证码卡片几乎满宽，显著偏离视为布局变体 */
        private const val CARD_WIDTH_RATIO_MIN = 0.7f
        private const val CARD_WIDTH_RATIO_MAX = 1.05f
    }

    /** 新版验证码提示文案是否在屏（WebView H5 虚拟树无障碍探测，单轮快速探测不重试） */
    override suspend fun isAnchorVisible(): Boolean {
        return WebViewAccessibilityFinder.collectWindowRoots()
            .any { WebViewAccessibilityFinder.findText(it, NEW_CAPTION_ANCHOR_TEXT, maxAttempts = 1) != null }
    }

    override suspend fun handleActivity(activity: Activity, root: ViewImage): ActivityHandleResult {
        return runWithProcessingWindow(activity) {
            var processingWindowAcquired = false
            try {
                // ① RPC 信号闸门：等待 H5 发起验证码 RPC 确认是验证码页，超时直接跳过
                val rpcConfirmed = awaitRpcConfirmation()
                    ?: return@runWithProcessingWindow ActivityHandleResult.SKIP_NON_RETRYABLE

                // ② 率先触发内置解锁（屏幕不亮时页面不会渲染，后续等待白费）
                if (!performUnlockOrFail()) {
                    return@runWithProcessingWindow ActivityHandleResult.FAILED_RETRYABLE
                }

                // ③ 等待 H5 渲染稳定后截图
                delay(RENDER_SETTLE_DELAY_MS)
                val decorView = activity.window.decorView

                // ④ 无障碍文本锚点探测
                val anchorText = probeAnchorText(activity)
                val hasStrongAnchor = !anchorText.isNullOrBlank()
                if (hasStrongAnchor) {
                    Log.record(TAG, "[前置命中] 文本锚点=$anchorText")
                } else {
                    Log.record(TAG, "[前置提示] 文本锚点缺失，回退视觉前置检测")
                }

                // ⑤ 截图轻量前置检测 + 决策闸门
                val lightweight = evaluateLightweightPreCheck(decorView, anchorText)
                decideLightweightGate(lightweight, rpcConfirmed, hasStrongAnchor)?.let {
                    return@runWithProcessingWindow it
                }

                // ⑥ 获取处理窗口互斥锁
                if (!acquireProcessingWindow("新版验证码处理窗口被占用")) {
                    return@runWithProcessingWindow ActivityHandleResult.FAILED_RETRYABLE
                }
                processingWindowAcquired = true
                logAcceptedAfterSkip(
                    if (hasStrongAnchor) "文本锚点命中且前置通过" else "文本锚点缺失但视觉前置通过"
                )

                // ⑦⑧ 模型识别 + 深度前置 + 滑动执行与复核
                when (val preparation = prepareSlidePlan(activity, lightweight, anchorText, rpcConfirmed, hasStrongAnchor)) {
                    is SlidePreparation.Ready ->
                        if (executeSlideOnView(preparation.plan)) {
                            ActivityHandleResult.HANDLED
                        } else {
                            logRetryableFailure("滑动执行失败")
                            ActivityHandleResult.FAILED_RETRYABLE
                        }
                    is SlidePreparation.Terminate -> preparation.result
                }
            } finally {
                if (processingWindowAcquired) {
                    releaseProcessingWindow()
                }
            }
        }
    }

    /**
     * 阶段①：RPC 信号闸门（事件驱动）。
     *
     * handler 触发常早于 H5 发起验证码 RPC（onResume 即执行），等待信号到达再继续可消除读取竞态。
     * newRpc=false 时 RPC hook 未安装、信号不会到来，退回一次性读取走旧回退逻辑。
     *
     * @return true=RPC 信号确认是验证码页；false=未确认（弱信号，继续走视觉检测）；null=超时判定非验证码页
     */
    private suspend fun awaitRpcConfirmation(): Boolean? {
        if (!ApplicationHook.config.newRpc.value) {
            return CaptchaRpcSignal.isVerifyRpcRecent()
        }
        val hit = CaptchaRpcSignal.awaitHit(RPC_SIGNAL_WAIT_TIMEOUT_MS)
        if (hit) {
            Log.record(TAG, "[RPC 信号] 命中验证码加载 RPC (${CaptchaRpcSignal.hitAgeMs()}ms 前)，确认验证码页")
            return true
        }
        Log.record(TAG, "[RPC 信号] ${RPC_SIGNAL_WAIT_TIMEOUT_MS}ms 内无验证码 RPC，判定非验证码页，跳过处理")
        return null
    }

    /**
     * 阶段④：无障碍探测验证码提示文案。
     *
     * 提示文案位于 WebView H5 虚拟树中，原生 XPath（仅遍历原生 View 树）永远不可见。
     * 主通道：进程内 provider id 扫描（已验证可下钻 MYWebView 虚拟树）；
     * 备通道：借道模块无障碍服务（对 MYWebView 文本不命中，留作其它容器兜底）。
     *
     * @return 命中的文案；未命中为 null
     */
    private suspend fun probeAnchorText(activity: Activity): String? {
        val wvMatch = WebViewAccessibilityFinder.collectWindowRoots()
            .mapNotNull { WebViewAccessibilityFinder.findText(it, NEW_CAPTION_ANCHOR_TEXT) }
            .firstOrNull()
        if (wvMatch != null) {
            Log.record(TAG, "[无障碍探测] 进程内命中: text='${wvMatch.text}' bounds=${wvMatch.bounds} <<< 命中")
            return wvMatch.text
        }
        Log.record(TAG, "[无障碍探测] 进程内未命中，服务通道兜底")

        val context = PageMonitor.getContext() ?: return null
        return try {
            val serviceResult = CommandUtil.findNodeByText(context, activity.packageName, NEW_CAPTION_ANCHOR_TEXT)
            if (serviceResult.found && serviceResult.rect != null) {
                Log.record(TAG, "[无障碍探测] 服务通道命中: text='${serviceResult.text}' bounds=${serviceResult.rect} <<< 命中")
                serviceResult.text
            } else {
                Log.record(TAG, "[无障碍探测] 服务通道未命中: reason=${serviceResult.reason}")
                null
            }
        } catch (e: Throwable) {
            Log.record(TAG, "[无障碍探测] 服务通道异常: ${e.message}")
            null
        }
    }

    /**
     * 阶段⑤决策：轻量前置检测结果闸门。
     *
     * - 通过且无强信号：记录"前置放行"后继续；
     * - 未通过但有强信号（RPC/文本锚点）：信号覆盖视觉检测，继续处理；
     * - 未通过且无强信号：判定非验证码页，不可重试跳过。
     *
     * @return null=继续处理；非 null=直接作为最终处理结果
     */
    private fun decideLightweightGate(
        lightweight: CaptchaPreCheckResult,
        rpcConfirmed: Boolean,
        hasStrongAnchor: Boolean
    ): ActivityHandleResult? {
        if (lightweight.passed) {
            if (!rpcConfirmed && !hasStrongAnchor) {
                Log.record(
                    TAG,
                    "[前置放行] 原因=文本锚点缺失但视觉前置通过; 通过项=${lightweight.passReasons.joinToString(", ")}"
                )
            }
            return null
        }
        if (rpcConfirmed || hasStrongAnchor) {
            Log.record(
                TAG,
                "[信号覆盖] 视觉检测未通过但${if (rpcConfirmed) "RPC" else "文本锚点"}确认是验证码页，继续处理: fail=${lightweight.failReasons.joinToString("; ")}"
            )
            return null
        }
        logPrecheckSkip("文本锚点缺失且前置检测未通过", lightweight.failReasons, lightweight.passReasons)
        return ActivityHandleResult.SKIP_NON_RETRYABLE
    }

    /**
     * 阶段⑤：截图级轻量前置检测。
     *
     * DecorView 全屏截图 -> 扫描蓝色滑块手柄 -> 按手柄位置/形状判定是否为验证码页，
     * 并产出后续模型识别所需的裁剪图（验证码所在横带区域）。
     * 有文本锚点时手柄像素数/尺寸判定放宽（锚点已提供强确认）。
     */
    private fun evaluateLightweightPreCheck(decorView: View, anchorText: String?): CaptchaPreCheckResult {
        val passReasons = mutableListOf<String>()
        val failReasons = mutableListOf<String>()
        passReasons += formatAnchorReason(anchorText)

        val fullBitmap = getBitmapFromView(decorView) ?: run {
            failReasons += "DecorView截图失败"
            return CaptchaPreCheckResult(passed = false, passReasons = passReasons, failReasons = failReasons, sliderHandle = null)
        }
        saveDebugBitmap(fullBitmap, "full_decorview")

        // 粗扫：手柄大概率在屏高 40% 以下
        val initialScanTop = fullBitmap.height * 40 / 100
        val sliderHandle = detectSliderHandle(fullBitmap, initialScanTop, null)
        if (sliderHandle != null) {
            passReasons += "blueHandle=(${sliderHandle.left},${sliderHandle.top},${sliderHandle.right},${sliderHandle.bottom})"
        } else {
            failReasons += "蓝色手柄缺失"
        }

        val (cropTop, cropBottom) = buildCaptchaCropBounds(fullBitmap, sliderHandle)
        val croppedBitmap = Bitmap.createBitmap(fullBitmap, 0, cropTop, fullBitmap.width, cropBottom - cropTop)
        Log.record(TAG, "[前置检测] 裁剪区域: top=$cropTop, bottom=$cropBottom, size=${croppedBitmap.width}x${croppedBitmap.height}")
        saveDebugBitmap(croppedBitmap, "cropped_captcha_area")

        sliderHandle?.let { handle ->
            // 手柄中心横坐标：滑块初始必在轨道左侧
            val handleXRatio = handle.centerX / fullBitmap.width.toFloat()
            if (handleXRatio <= HANDLE_X_RATIO_MAX) {
                passReasons += "handleXRatio=${"%.2f".format(handleXRatio)}"
            } else {
                failReasons += "handleXRatio=${"%.2f".format(handleXRatio)}>$HANDLE_X_RATIO_MAX"
            }

            // 手柄中心纵坐标：验证码位于屏幕中下部
            val handleYRatio = handle.centerY / fullBitmap.height.toFloat()
            if (handleYRatio in HANDLE_Y_RATIO_MIN..HANDLE_Y_RATIO_MAX) {
                passReasons += "handleYRatio=${"%.2f".format(handleYRatio)}"
            } else {
                failReasons += "handleYRatio=${"%.2f".format(handleYRatio)} 越界"
            }

            if (anchorText.isNullOrBlank()) {
                // 无文本锚点时需手柄自身特征充分自证
                if (handle.pixelCount >= HANDLE_MIN_PIXELS_NO_ANCHOR) {
                    passReasons += "handlePixels=${handle.pixelCount}"
                } else {
                    failReasons += "handlePixels=${handle.pixelCount}<$HANDLE_MIN_PIXELS_NO_ANCHOR"
                }
                val handleWidth = handle.right - handle.left
                val handleHeight = handle.bottom - handle.top
                if (handleWidth in HANDLE_SIZE_MIN..HANDLE_SIZE_MAX && handleHeight in HANDLE_SIZE_MIN..HANDLE_SIZE_MAX) {
                    passReasons += "handleSize=${handleWidth}x$handleHeight"
                } else {
                    failReasons += "handleSize=${handleWidth}x$handleHeight 越界"
                }
            }
        }

        return CaptchaPreCheckResult(
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

    /**
     * 阶段⑦：模型识别 + 深度前置检测 + 构建滑动计划。
     *
     * 坐标系约定：模型识别结果在裁剪图坐标系，Y 轴补偿 [CaptchaPreCheckResult.cropTop]
     * 后即为 DecorView 局部坐标；实际按压点取像素扫描的滑块手柄中心，
     * 水平位移取模型识别的滑块->缺口距离。
     *
     * 缺口（gap）识别失败时，若模型识别到刷新按钮，则点击刷新换图并重新
     * 截图+识别重试（上限 [MAX_REFRESH_ATTEMPTS] 次）。
     */
    private suspend fun prepareSlidePlan(
        activity: Activity,
        initialLightweight: CaptchaPreCheckResult,
        anchorText: String?,
        rpcConfirmed: Boolean,
        hasStrongAnchor: Boolean
    ): SlidePreparation {
        var lightweight = initialLightweight

        for (attempt in 0..MAX_REFRESH_ATTEMPTS) {
            // 轻量检测产物完备性校验（信号覆盖路径下产物可能缺失）
            lightweight.fullBitmap ?: return terminateRetryable("轻量前置检测未产出全屏截图", "轻量前置产出缺失: fullBitmap")
            val croppedBitmap = lightweight.croppedBitmap
                ?: return terminateRetryable("轻量前置检测未产出裁剪截图", "轻量前置产出缺失: croppedBitmap")
            val detectedHandle = lightweight.sliderHandle
                ?: return terminateRetryable("轻量前置检测未命中滑块手柄", "轻量前置产出缺失: sliderHandle")
            val cropTop = lightweight.cropTop
            val cropBottom = lightweight.cropBottom

            // 模型识别滑块/缺口（裁剪图坐标系）
            Log.record(TAG, "[模型识别] callerThread=${Thread.currentThread().name}, isMain=${isMainThread()}")
            val recognition = SliderTFLite.identifyShared(activity.applicationContext, croppedBitmap)
                ?: return terminateRetryable("裁剪区域模型识别失败，推理无结果", "模型识别无结果")

            // gap 缺失：尝试点击刷新按钮换图重试
            if (!recognition.hasGap) {
                Log.record(
                    TAG,
                    "[刷新流程] 未识别到缺口，尝试刷新 (第 ${attempt + 1} 次) candidateCount=${recognition.candidateCount}"
                )
                if (attempt >= MAX_REFRESH_ATTEMPTS) {
                    return terminateRetryable("刷新${MAX_REFRESH_ATTEMPTS}次后仍未识别到缺口", "刷新后仍无gap")
                }
                val refreshed = refreshAndReEvaluate(activity, recognition, cropTop, anchorText)
                    ?: return terminateRetryable("无法点击刷新按钮重试", "刷新按钮缺失或点击失败")
                lightweight = refreshed
                continue
            }

            // block 缺失：用像素扫描的蓝色手柄中心代偿滑块参考坐标
            // （x 用手柄中心、y 用缺口中心，滑块块与缺口同水平线；避免 sliderX==targetX 导致距离为 0）
            val effectiveRecognition = compensateSliderReference(recognition, detectedHandle.centerX)

            val distance = effectiveRecognition.targetX - effectiveRecognition.sliderX
            Log.record(
                TAG,
                "裁剪识别成功: 裁剪内坐标 滑块=(${effectiveRecognition.sliderX.toInt()},${effectiveRecognition.sliderY.toInt()}) 目标=(${effectiveRecognition.targetX.toInt()},${effectiveRecognition.targetY.toInt()})"
            )
            Log.record(
                TAG,
                "DecorView坐标: 滑块=(${effectiveRecognition.sliderX.toInt()},${(effectiveRecognition.sliderY + cropTop).toInt()}), 目标=(${effectiveRecognition.targetX.toInt()},${(effectiveRecognition.targetY + cropTop).toInt()}), 置信度=${effectiveRecognition.confidence}"
            )

            // 深度前置检测（识别结果级）
            val preCheck = evaluateModelPreCheck(croppedBitmap, effectiveRecognition, anchorText, detectedHandle)
            if (!preCheck.passed) {
                if (rpcConfirmed || hasStrongAnchor) {
                    // 强信号已确认是验证码页，模型检测失败应刷新换图重试
                    Log.record(
                        TAG,
                        "[刷新流程] 模型检测未通过，尝试刷新 (第 ${attempt + 1} 次): ${preCheck.failReasons.joinToString("; ")}"
                    )
                    if (attempt >= MAX_REFRESH_ATTEMPTS) {
                        return terminateRetryable(
                            "刷新${MAX_REFRESH_ATTEMPTS}次后模型检测仍未通过",
                            if (rpcConfirmed) "RPC信号确认但模型前置检测持续失败" else "文本锚点确认但模型前置检测持续失败"
                        )
                    }
                    val refreshed = refreshAndReEvaluate(activity, recognition, cropTop, anchorText)
                        ?: return terminateRetryable("无法点击刷新按钮重试", "刷新按钮缺失或点击失败")
                    lightweight = refreshed
                    continue
                }
                logPrecheckSkip("文本锚点缺失且前置检测未通过", preCheck.failReasons, preCheck.passReasons)
                return SlidePreparation.Terminate(ActivityHandleResult.SKIP_NON_RETRYABLE)
            }
            Log.record(
                TAG,
                "[前置检测通过] 原因=${if (rpcConfirmed) "RPC信号确认" else if (hasStrongAnchor) "文本锚点确认" else "文本锚点缺失但视觉前置通过"}; 判定为滑块验证码页: ${preCheck.passReasons.joinToString(", ")}"
            )
            Log.record(
                TAG,
                "滑动参数: 模型滑块=(${effectiveRecognition.sliderX.toInt()},${(effectiveRecognition.sliderY + cropTop).toInt()}), 模型目标=(${effectiveRecognition.targetX.toInt()},${(effectiveRecognition.targetY + cropTop).toInt()}), 距离=${distance.toInt()}px"
            )

            val handle = preCheck.sliderHandle ?: detectedHandle
            val plan = SlidePlan(
                view = activity.window.decorView,
                startX = handle.centerX,
                startY = handle.centerY,
                endX = handle.centerX + distance,
                endY = handle.centerY,
                cropTop = cropTop,
                cropBottom = cropBottom
            )
            Log.record(
                TAG,
                "命中滑块手柄: bounds=(${handle.left},${handle.top},${handle.right},${handle.bottom}), center=(${handle.centerX.toInt()},${handle.centerY.toInt()}), pixels=${handle.pixelCount}"
            )
            Log.record(
                TAG,
                "实际滑动参数: 起点=(${plan.startX.toInt()},${plan.startY.toInt()}), 终点=(${plan.endX.toInt()},${plan.endY.toInt()}), 距离=${distance.toInt()}px"
            )
            return SlidePreparation.Ready(plan)
        }
        return terminateRetryable("滑动计划构建失败", "prepareSlidePlan 未产出计划")
    }

    /**
     * 阶段⑦刷新：点击验证码刷新按钮换一张新图。
     *
     * 刷新按钮中心位于模型识别返回的裁剪图坐标系，需换算到 DecorView 局部坐标
     * （Y 加 cropTop、X 加 cropLeft=0）后派发点击手势。
     *
     * @return true=已成功点击刷新；false=无刷新框或点击失败（无可重试路径）
     */
    private suspend fun attemptRefreshForRetry(
        activity: Activity,
        recognition: SliderTFLite.SlideRecognitionResult,
        cropTop: Int
    ): Boolean {
        val refreshX = recognition.refreshX ?: run {
            Log.record(TAG, "[刷新流程] 模型未识别到刷新按钮，无法刷新重试")
            return false
        }
        val refreshY = recognition.refreshY ?: return false
        val refreshScore = recognition.refreshScore ?: 0f
        if (refreshScore < REFRESH_CONFIDENCE_MIN) {
            Log.record(TAG, "[刷新流程] 刷新按钮置信度过低 $refreshScore < $REFRESH_CONFIDENCE_MIN，放弃刷新")
            return false
        }

        // 模型坐标在裁剪图（DecorView 局部）坐标系：X 不变，Y 补偿 cropTop
        val decorView = activity.window.decorView
        val tapX = refreshX
        val tapY = refreshY + cropTop
        Log.record(
            TAG,
            "[刷新流程] 点击刷新按钮: 裁剪坐标=(${refreshX.toInt()},${refreshY.toInt()}) -> DecorView局部=(${tapX.toInt()},${tapY.toInt()}), conf=${refreshScore}"
        )

        // DecorView 局部坐标 -> 屏幕坐标
        val viewLocation = IntArray(2)
        decorView.getLocationOnScreen(viewLocation)
        return dispatchTapWithFallback(decorView, tapX + viewLocation[0], tapY + viewLocation[1])
    }

    /**
     * 刷新换图并重新执行轻量前置检测。
     *
     * 点击验证码刷新按钮后等待新图加载，重新截图并产出新的轻量前置检测结果，
     * 供下一次循环迭代重新走模型识别。
     *
     * @return 新图的轻量前置检测结果；点击刷新失败时为 null（无可重试路径）
     */
    private suspend fun refreshAndReEvaluate(
        activity: Activity,
        recognition: SliderTFLite.SlideRecognitionResult,
        cropTop: Int,
        anchorText: String?
    ): CaptchaPreCheckResult? {
        if (!attemptRefreshForRetry(activity, recognition, cropTop)) {
            return null
        }
        // 等待新图加载完成后重新截图 + 轻量前置检测
        delay(REFRESH_SETTLE_DELAY_MS)
        return evaluateLightweightPreCheck(activity.window.decorView, anchorText)
    }

    /**
     * 阶段⑦深度检测：基于模型识别结果的验证码合法性判定。
     *
     * 校验候选框数量、置信度、滑块->缺口距离/垂直偏差、滑块与缺口的横向占比，
     * 过滤误识别（如把页面装饰元素认成滑块）。
     */
    private fun evaluateModelPreCheck(
        croppedBitmap: Bitmap,
        recognition: SliderTFLite.SlideRecognitionResult,
        anchorText: String?,
        sliderHandle: SliderHandleDetection
    ): CaptchaPreCheckResult {
        val passReasons = mutableListOf<String>()
        val failReasons = mutableListOf<String>()

        passReasons += formatAnchorReason(anchorText)
        passReasons += "blueHandle=(${sliderHandle.left},${sliderHandle.top},${sliderHandle.right},${sliderHandle.bottom})"

        // 滑块+缺口候选框至少 2 个
        if (recognition.candidateCount >= 2) {
            passReasons += "candidateCount=${recognition.candidateCount}"
        } else {
            failReasons += "candidateCount=${recognition.candidateCount}<2"
        }

        if (recognition.confidence >= MODEL_CONFIDENCE_THRESHOLD) {
            passReasons += "confidence=${recognition.confidence}"
        } else {
            failReasons += "confidence=${recognition.confidence}<$MODEL_CONFIDENCE_THRESHOLD"
        }

        // 滑块->缺口水平距离：过短无意义，过长超出轨道
        val distance = recognition.targetX - recognition.sliderX
        val minDistance = maxOf(croppedBitmap.width * 0.05f, 40f)
        val maxDistance = croppedBitmap.width * 0.82f
        if (distance in minDistance..maxDistance) {
            passReasons += "distance=${distance.toInt()}"
        } else {
            failReasons += "distance=${distance.toInt()} 不在范围 ${minDistance.toInt()}..${maxDistance.toInt()}"
        }

        // 滑块与缺口的垂直偏差：滑道水平，偏差过大说明误识别
        val verticalDelta = kotlin.math.abs(recognition.targetY - recognition.sliderY)
        val maxVerticalDelta = maxOf(croppedBitmap.height * 0.12f, 72f)
        if (verticalDelta <= maxVerticalDelta) {
            passReasons += "verticalDelta=${verticalDelta.toInt()}"
        } else {
            failReasons += "verticalDelta=${verticalDelta.toInt()}>${maxVerticalDelta.toInt()}"
        }

        // 滑块横向占比：初始滑块必在左侧
        val sliderRatio = recognition.sliderX / croppedBitmap.width.toFloat()
        if (sliderRatio <= 0.35f) {
            passReasons += "sliderRatio=${"%.2f".format(sliderRatio)}"
        } else {
            failReasons += "sliderRatio=${"%.2f".format(sliderRatio)}>0.35"
        }

        // 缺口横向占比：缺口应在滑道中后段
        val targetRatio = recognition.targetX / croppedBitmap.width.toFloat()
        if (targetRatio in 0.20f..0.95f) {
            passReasons += "targetRatio=${"%.2f".format(targetRatio)}"
        } else {
            failReasons += "targetRatio=${"%.2f".format(targetRatio)} 越界"
        }

        return CaptchaPreCheckResult(
            passed = failReasons.isEmpty(),
            passReasons = passReasons,
            failReasons = failReasons,
            sliderHandle = sliderHandle
        )
    }

    /**
     * 阶段⑧：按计划派发滑动并复核结果。
     *
     * 滑动后轮询无障碍探测提示文案：验证通过则 H5 关闭页面、文案随之消失。
     * 文案未消失时尝试校正滑动。
     */
    private suspend fun executeSlideOnView(plan: SlidePlan): Boolean {
        val view = plan.view
        // DecorView 局部坐标 -> 屏幕坐标
        val viewLocation = IntArray(2)
        view.getLocationOnScreen(viewLocation)
        val screenStartX = plan.startX + viewLocation[0]
        val screenStartY = plan.startY + viewLocation[1]
        val screenEndX = plan.endX + viewLocation[0]
        val screenEndY = plan.endY + viewLocation[1]

        val duration = Random.nextLong(SLIDE_DURATION_MIN, SLIDE_DURATION_MAX + 1)
        Log.record(
            TAG,
            "执行滑动(全屏模式): 局部(${plan.startX.toInt()},${plan.startY.toInt()})->(${plan.endX.toInt()},${plan.endY.toInt()}), 屏幕(${screenStartX.toInt()},${screenStartY.toInt()})->(${screenEndX.toInt()},${screenEndY.toInt()}), 时长: ${duration}ms"
        )

        if (!dispatchSwipeWithFallback(view, screenStartX, screenStartY, screenEndX, screenEndY, duration)) {
            return false
        }

        val checkStartTime = System.currentTimeMillis()
        var verified = awaitCaptchaPageClosed()
        if (!verified) {
            Log.record(TAG, "[滑动复核] 文案未消失，尝试校正滑动...")
            verified = attemptCorrectiveSwipeIfNeeded(view, plan.cropTop, plan.cropBottom)
        }
        Log.record(TAG, "[滑动判定] 路径=caption-poll, 检查耗时=${System.currentTimeMillis() - checkStartTime}ms, 最终结果=$verified")
        return verified
    }

    /**
     * 滑动后等待验证码页关闭：轮询无障碍探测提示文案。
     *
     * 验证通过后 H5 会关闭/跳转，提示文案随之从 WebView 虚拟树消失（或整棵 WebView 销毁），
     * 这是比图像残留更直接的成功信号。"消失"需连续两次探测确认，防刷新瞬间虚拟树重建造成的虚灭。
     *
     * @return true=文案已消失（验证通过）；false=超时文案仍在（未通过，交由上层校正/重试）
     */
    private suspend fun awaitCaptchaPageClosed(): Boolean {
        delay(POST_SLIDE_CHECK_DELAY_MS)
        val deadline = System.currentTimeMillis() + CAPTION_GONE_TIMEOUT_MS
        while (true) {
            if (!isAnchorVisible()) {
                delay(CAPTION_GONE_CONFIRM_MS)
                if (!isAnchorVisible()) {
                    Log.record(TAG, "[滑动复核] 提示文案已消失，验证码页已关闭，判定通过")
                    return true
                }
            }
            if (System.currentTimeMillis() >= deadline) {
                Log.record(TAG, "[滑动复核] ${CAPTION_GONE_TIMEOUT_MS}ms 内提示文案仍在，判定未通过")
                return false
            }
            delay(CAPTION_POLL_INTERVAL_MS)
        }
    }

    /**
     * 校正滑动：首滑未通过时重新截图+识别，按残余偏差小幅修正。
     *
     * @return true=校正后文案消失（验证通过）
     */
    private suspend fun attemptCorrectiveSwipeIfNeeded(view: View, cropTop: Int, cropBottom: Int): Boolean {
        val probeFullBitmap = getBitmapFromView(view) ?: return false
        saveDebugBitmap(probeFullBitmap, "correction_probe_full_decorview")

        // 按首滑时的裁剪带重新裁剪，保证识别坐标系一致
        val safeCropTop = cropTop.coerceIn(0, (probeFullBitmap.height - 1).coerceAtLeast(0))
        val safeCropBottom = cropBottom.coerceIn(safeCropTop + 1, probeFullBitmap.height)
        val probeCroppedBitmap = Bitmap.createBitmap(
            probeFullBitmap, 0, safeCropTop, probeFullBitmap.width, safeCropBottom - safeCropTop
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

        // 校正路径下缺口缺失则目标不可靠，放弃校正（保留原滑动位置等待页面自行判定）
        if (!probeRecognition.hasGap) {
            Log.record(TAG, "校正探测未识别到缺口(gap)，放弃校正滑动")
            return false
        }

        val sliderHandle = detectSliderHandle(probeFullBitmap, cropTop, probeRecognition) ?: run {
            Log.record(TAG, "校正探测未定位到滑块手柄")
            return false
        }

        // 校正距离过小说明已对齐、过大说明识别异常，均放弃校正
        val effectiveProbe = compensateSliderReference(probeRecognition, sliderHandle.centerX)
        val correctionDistance = estimateCorrectionDistance(effectiveProbe)
        if (kotlin.math.abs(correctionDistance) !in CORRECTION_MIN_DISTANCE..CORRECTION_MAX_DISTANCE) {
            Log.record(TAG, "校正距离超出允许范围: $correctionDistance")
            return false
        }

        val endX = sliderHandle.centerX + correctionDistance
        val endY = sliderHandle.centerY
        val duration = Random.nextLong(CORRECTIVE_SLIDE_DURATION_MIN, CORRECTIVE_SLIDE_DURATION_MAX)
        Log.record(
            TAG,
            "[校正滑动] handle=(${sliderHandle.centerX.toInt()},${sliderHandle.centerY.toInt()}), correctionDistance=${correctionDistance.toInt()}"
        )
        dispatchSwipeWithFallback(view, sliderHandle.centerX, sliderHandle.centerY, endX, endY, duration)

        delay(CORRECTION_SETTLE_DELAY_MS)
        return awaitCaptchaPageClosed()
    }

    /**
     * 计算模型识别用裁剪带（拼图卡片整体）的上下边界。
     *
     * 主路径（有手柄）：按训练集恒定几何（484/484 样本一致）从手柄包围盒反推拼图卡片边界——
     * 卡宽 = 手柄宽 / [HANDLE_WIDTH_TO_CARD_WIDTH]，卡片 top = 手柄 top - 0.7595×卡高。
     * 裁出完整卡片使推理输入与训练图（puzzleContainer 截图，288×270）分布一致，
     * 卡片内的 gap/block/refresh（含刷新按钮）均完整落入裁剪带。
     * 反推不合理（卡宽显著偏离屏宽，布局变体）时回退旧逻辑。
     * 无手柄：回退到屏高 28%~88% 的固定带。
     */
    private fun buildCaptchaCropBounds(fullBitmap: Bitmap, sliderHandle: SliderHandleDetection?): Pair<Int, Int> {
        val fallbackTop = (fullBitmap.height * 28 / 100).coerceIn(0, fullBitmap.height - 1)
        val fallbackBottom = (fullBitmap.height * 88 / 100).coerceIn(fallbackTop + 1, fullBitmap.height)
        if (sliderHandle == null) {
            return fallbackTop to fallbackBottom
        }

        val handleWidth = (sliderHandle.right - sliderHandle.left).toFloat()
        if (handleWidth > 0f) {
            val cardWidth = handleWidth / HANDLE_WIDTH_TO_CARD_WIDTH
            val cardHeight = cardWidth / CARD_WIDTH_TO_HEIGHT
            val cardTop = sliderHandle.top - cardHeight * HANDLE_TOP_TO_CARD_HEIGHT
            // 合理性校验：卡片几乎满屏宽；反推明显偏离（布局变体/手柄误检）时回退旧逻辑
            if (cardWidth in fullBitmap.width * CARD_WIDTH_RATIO_MIN..fullBitmap.width * CARD_WIDTH_RATIO_MAX && cardTop >= 0f) {
                val cropTop = cardTop.toInt().coerceIn(0, fullBitmap.height - 1)
                val cropBottom = (cardTop + cardHeight).toInt().coerceIn(cropTop + 1, fullBitmap.height)
                Log.record(
                    TAG,
                    "[卡片反推] handleW=$handleWidth -> card=${cardWidth.toInt()}x${cardHeight.toInt()}, cropTop=$cropTop, cropBottom=$cropBottom"
                )
                return cropTop to cropBottom
            }
            Log.record(
                TAG,
                "[卡片反推] 结果不合理: cardWidth=$cardWidth, screenW=${fullBitmap.width}, cardTop=$cardTop -> 回退旧裁剪逻辑"
            )
        }

        // 旧逻辑回退：手柄上方 42% 屏高覆盖缺口区，下方 8% 屏高余量
        val cropTop = maxOf(0, sliderHandle.top - fullBitmap.height * 42 / 100)
        val cropBottom = minOf(fullBitmap.height, sliderHandle.bottom + fullBitmap.height * 8 / 100)
        return if (cropBottom - cropTop >= fullBitmap.height * 22 / 100) {
            cropTop to cropBottom
        } else {
            fallbackTop to fallbackBottom
        }
    }

    /**
     * block 缺失时用像素扫描的蓝色手柄中心代偿滑块参考坐标。
     *
     * 模型未识别到滑块块（hasBlock=false）时 sliderX/Y 无效。代偿规则：
     * - x 用蓝色手柄中心（滑块与手柄初始都在最左，x 对齐）；
     * - y 用缺口中心（滑块块与缺口在同一水平线上）。
     * 这样 distance=targetX-sliderX 不再为 0，能通过后续距离前置检测。
     */
    private fun compensateSliderReference(
        recognition: SliderTFLite.SlideRecognitionResult,
        handleCenterX: Float
    ): SliderTFLite.SlideRecognitionResult {
        if (recognition.hasBlock) return recognition
        Log.record(TAG, "[代偿] 未识别到滑块块(block)，用蓝色手柄中心 x 代偿: handleX=${handleCenterX.toInt()}")
        return recognition.copy(
            sliderX = handleCenterX,
            sliderY = recognition.targetY
        )
    }

    /**
     * 估算校正滑动距离：目标与当前滑块位置的残余偏差，外加 8px 过冲补偿。
     * 偏差 <1px 时给固定 12px 轻推（视觉上已对齐但判定未过的情况）。
     */
    private fun estimateCorrectionDistance(recognition: SliderTFLite.SlideRecognitionResult): Float {
        val rawDistance = recognition.targetX - recognition.sliderX
        return when {
            kotlin.math.abs(rawDistance) < 1f -> 12f
            rawDistance > 0f -> rawDistance + 8f
            else -> rawDistance - 8f
        }
    }

    /**
     * 在全屏截图的限定窗口内扫描"手柄蓝"像素，返回手柄包围盒。
     *
     * @param recognitionResult 已有模型识别结果时，用于收窄扫描窗口（滑块下方区域）；
     *                          null 时从屏高 55% 起扫（无识别先验的粗扫）
     */
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
        val searchBottom = (fullBitmap.height - HANDLE_SCAN_BOTTOM_MARGIN)
            .coerceAtLeast(searchTop + 1)
            .coerceAtMost(fullBitmap.height)
        val searchRight = (fullBitmap.width * HANDLE_SCAN_X_LIMIT / 100).coerceAtMost(fullBitmap.width)

        // 行投影：每行蓝色像素数
        val rowCount = searchBottom - searchTop
        val rowBlue = IntArray(rowCount)
        for (i in 0 until rowCount) {
            val yRow = searchTop + i
            var c = 0
            var xScan = 0
            while (xScan < searchRight) {
                if (isLikelySliderHandleBlue(fullBitmap.getPixel(xScan, yRow))) c++
                xScan++
            }
            rowBlue[i] = c
        }

        // 从下往上找第一段连续非零行（带 gap 容差，防手柄内部小空洞）
        // 先验：手柄位于搜索窗口下方，噪点(UI装饰/标题)位于上方，从下往上首段即手柄
        var segBottomIdx = -1
        var segTopIdx = -1
        var gap = 0
        var idx = rowCount - 1
        while (idx >= 0) {
            if (rowBlue[idx] > 0) {
                if (segBottomIdx == -1) segBottomIdx = idx
                segTopIdx = idx
                gap = 0
            } else if (segBottomIdx != -1) {
                gap++
                if (gap > HANDLE_GAP_TOLERANCE) break
            }
            idx--
        }
        if (segBottomIdx == -1) {
            Log.record(
                TAG,
                "滑块手柄检测失败: searchRegion=(0,$searchTop,$searchRight,$searchBottom), pixelCount=0 (无蓝色像素)"
            )
            return null
        }
        val segTop = searchTop + segTopIdx
        val segBottom = searchTop + segBottomIdx

        // 仅在 [segTop, segBottom] 段内算 bbox，避免上方噪点合并进包围盒
        var minX = Int.MAX_VALUE
        var minY = Int.MAX_VALUE
        var maxX = -1
        var maxY = -1
        var pixelCount = 0
        var y = segTop
        while (y <= segBottom) {
            var x = 0
            while (x < searchRight) {
                if (isLikelySliderHandleBlue(fullBitmap.getPixel(x, y))) {
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

        if (pixelCount < HANDLE_MIN_PIXELS || maxX <= minX || maxY <= minY) {
            Log.record(
                TAG,
                "滑块手柄检测失败: segment=[$segTop,$segBottom], searchRegion=(0,$searchTop,$searchRight,$searchBottom), pixelCount=$pixelCount"
            )
            return null
        }

        val width = maxX - minX
        val height = maxY - minY
        if (width !in HANDLE_BOUNDS_MIN..HANDLE_BOUNDS_MAX || height !in HANDLE_BOUNDS_MIN..HANDLE_BOUNDS_MAX) {
            Log.record(TAG, "滑块手柄检测失败: boundsSize=${width}x$height, pixelCount=$pixelCount, segment=[$segTop,$segBottom]")
            return null
        }

        val aspectRatio = width.toFloat() / height.toFloat()
        if (aspectRatio !in HANDLE_ASPECT_RATIO_MIN..HANDLE_ASPECT_RATIO_MAX) {
            Log.record(TAG, "滑块手柄检测失败: aspectRatio=$aspectRatio, boundsSize=${width}x$height, pixelCount=$pixelCount, segment=[$segTop,$segBottom]")
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

    /** "手柄蓝"像素判定：高蓝、中绿、低红，且蓝色显著高于红/绿（支付宝滑块手柄配色特征） */
    private fun isLikelySliderHandleBlue(pixel: Int): Boolean {
        val red = android.graphics.Color.red(pixel)
        val green = android.graphics.Color.green(pixel)
        val blue = android.graphics.Color.blue(pixel)
        return blue >= 170 && green >= 80 && red <= 120 && blue - red >= 70 && blue - green >= 25
    }

    /** 获取 View 的截屏 Bitmap（view.draw(canvas) 方式） */
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

    /** 记录可重试失败并以 FAILED_RETRYABLE 终止滑动准备（先输出明细日志） */
    private fun terminateRetryable(detailLog: String, retryReason: String): SlidePreparation {
        Log.record(TAG, detailLog)
        logRetryableFailure(retryReason)
        return SlidePreparation.Terminate(ActivityHandleResult.FAILED_RETRYABLE)
    }

    private fun formatAnchorReason(anchorText: String?): String {
        return if (anchorText.isNullOrBlank()) "文本锚点缺失" else "文本锚点=$anchorText"
    }
}
