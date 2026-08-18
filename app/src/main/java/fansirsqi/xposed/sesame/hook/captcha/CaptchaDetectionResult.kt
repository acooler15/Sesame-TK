package fansirsqi.xposed.sesame.hook.captcha

import android.graphics.Bitmap

/**
 * 滑块手柄像素检测结果（截图扫描出的蓝色手柄包围盒）。
 *
 * @param centerXY 手柄中心坐标（所在截图的局部坐标系）
 * @param left/top/right/bottom 手柄包围盒边界
 * @param pixelCount 命中"手柄蓝"特征的像素总数，用于形状有效性判定
 */
data class SliderHandleDetection(
    val centerX: Float,
    val centerY: Float,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val pixelCount: Int
)

/**
 * 验证码前置检测结果（轻量检测与模型深检共用）。
 *
 * - 轻量检测（截图级）：填充全部字段，携带截图产物供后续模型识别复用；
 * - 模型深检（识别级）：仅填充判定与手柄字段，位图字段为 null。
 *
 * @param passed 是否通过（failReasons 为空即通过）
 * @param passReasons/failReasons 通过/未通过项明细，用于日志输出
 * @param sliderHandle 像素扫描定位到的滑块手柄，未命中为 null
 */
data class CaptchaPreCheckResult(
    val passed: Boolean,
    val passReasons: List<String>,
    val failReasons: List<String>,
    val sliderHandle: SliderHandleDetection?,
    val fullBitmap: Bitmap? = null,
    val croppedBitmap: Bitmap? = null,
    val cropTop: Int = 0,
    val cropBottom: Int = 0
)
