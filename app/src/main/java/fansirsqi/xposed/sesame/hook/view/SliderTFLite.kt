package fansirsqi.xposed.sesame.hook.view

import android.content.Context
import android.graphics.*
import android.os.Looper
import fansirsqi.xposed.sesame.ml.Slider
import fansirsqi.xposed.sesame.core.threads.GlobalThreadPools
import fansirsqi.xposed.sesame.core.log.Log
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.model.Model
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import kotlin.math.*

/**
 * 新版滑块验证码检测模型封装（YOLO26 detect，四类 gap/block/refresh/feedback）。
 *
 * 关键特性：
 * - 输入为 **NCHW** `[1, 3, 640, 640]`（LiteRT 新版导出布局）；
 * - 输出为单张 `[1, 300, 6]`（e2e 一对一头，每行 x1,y1,x2,y2,conf,classId），
 *   **无 NMS**（模型已内置端到端筛选）；坐标按 `(v - pad) / ratio` 还原到原图；
 * - 滑块/缺口/刷新按钮/反馈按钮由 classId 直接区分。
 */
class SliderTFLite(val context: Context) {

    companion object {
        private const val TAG = "SliderTFLite"
        private const val CONF_THRESHOLD = 0.5f
        private const val INPUT_SIZE = 640
        private const val INPUT_CHANNELS = 3
        private const val MAX_DET = 300
        private const val NUM_CLASSES = 4

        // 类别索引（与 config/slider.yaml 一致，勿改动顺序）
        const val CLASS_GAP = 0
        const val CLASS_BLOCK = 1
        const val CLASS_REFRESH = 2
        const val CLASS_FEEDBACK = 3

        private const val MODEL_IDLE_TIMEOUT_MS = 60 * 60 * 1000L

        private val sharedModelMutex = Mutex()

        @Volatile
        private var sharedModel: SliderTFLite? = null

        @Volatile
        private var lastUsedAt: Long = 0L

        @Volatile
        private var unloadTicket: Long = 0L

        fun preloadAsync(context: Context) {
            val appContext = context.applicationContext
            GlobalThreadPools.execute(
                CoroutineName("SliderTFLitePreload") + GlobalThreadPools.computeDispatcher
            ) {
                val startTime = System.currentTimeMillis()
                Log.record(
                    TAG,
                    "[预加载开始] thread=${Thread.currentThread().name}, isMain=${isMainThread()}"
                )
                try {
                    obtainSharedModel(appContext, "preload")
                    Log.record(
                        TAG,
                        "[预加载结束] success=true, cost=${System.currentTimeMillis() - startTime}ms"
                    )
                } catch (e: Exception) {
                    Log.record(
                        TAG,
                        "[预加载结束] success=false, cost=${System.currentTimeMillis() - startTime}ms, error=${e.message}"
                    )
                    Log.printStackTrace(TAG, "模型预加载失败", e)
                }
            }
        }

        suspend fun identifyShared(
            context: Context,
            bitmap: Bitmap,
            conf: Float = CONF_THRESHOLD
        ): SlideRecognitionResult? {
            val detector = obtainSharedModel(context.applicationContext, "inference")
            val callerThread = Thread.currentThread().name
            val callerIsMain = isMainThread()
            return withContext(GlobalThreadPools.computeDispatcher) {
                val startTime = System.currentTimeMillis()
                Log.record(
                    TAG,
                    "[模型推理开始] callerThread=$callerThread, callerIsMain=$callerIsMain, workerThread=${Thread.currentThread().name}, isMain=${isMainThread()}, size=${bitmap.width}x${bitmap.height}"
                )
                try {
                    detector.identifySlideRecognition(bitmap, conf)
                } finally {
                    touchSharedModelLocked()
                    Log.record(
                        TAG,
                        "[模型推理结束] cost=${System.currentTimeMillis() - startTime}ms, workerThread=${Thread.currentThread().name}, isMain=${isMainThread()}"
                    )
                }
            }
        }

        private suspend fun obtainSharedModel(context: Context, reason: String): SliderTFLite {
            return withContext(GlobalThreadPools.computeDispatcher) {
                sharedModelMutex.withLock {
                    sharedModel?.let { model ->
                        lastUsedAt = System.currentTimeMillis()
                        scheduleIdleReleaseLocked()
                        Log.record(
                            TAG,
                            "[复用全局模型实例] reason=$reason, thread=${Thread.currentThread().name}, isMain=${isMainThread()}"
                        )
                        return@withLock model
                    }

                    val initStartTime = System.currentTimeMillis()
                    Log.record(
                        TAG,
                        "[初始化开始] reason=$reason, thread=${Thread.currentThread().name}, isMain=${isMainThread()}"
                    )
                    val detector = SliderTFLite(context.applicationContext)
                    val initSuccess = detector.init()
                    if (!initSuccess) {
                        detector.close()
                        throw IllegalStateException("SliderTFLite init failed")
                    }
                    sharedModel = detector
                    lastUsedAt = System.currentTimeMillis()
                    scheduleIdleReleaseLocked()
                    Log.record(
                        TAG,
                        "[初始化结束] success=true, cost=${System.currentTimeMillis() - initStartTime}ms"
                    )
                    detector
                }
            }
        }

        private suspend fun touchSharedModelLocked() {
            sharedModelMutex.withLock {
                if (sharedModel != null) {
                    lastUsedAt = System.currentTimeMillis()
                    scheduleIdleReleaseLocked()
                }
            }
        }

        private fun scheduleIdleReleaseLocked() {
            val ticket = ++unloadTicket
            GlobalThreadPools.execute(
                CoroutineName("SliderTFLiteIdleRelease") + GlobalThreadPools.computeDispatcher
            ) {
                delay(MODEL_IDLE_TIMEOUT_MS)
                sharedModelMutex.withLock {
                    if (ticket != unloadTicket) {
                        return@withLock
                    }
                    val idleFor = System.currentTimeMillis() - lastUsedAt
                    if (sharedModel != null && idleFor >= MODEL_IDLE_TIMEOUT_MS) {
                        Log.record(
                            TAG,
                            "[模型空闲超时卸载] idleMs=$idleFor, thread=${Thread.currentThread().name}, isMain=${isMainThread()}"
                        )
                        sharedModel?.close()
                        sharedModel = null
                    }
                }
            }
        }

        private fun isMainThread(): Boolean {
            return Looper.getMainLooper().thread === Thread.currentThread()
        }
    }

    private var sliderModel: Slider? = null

    fun init(): Boolean {
        return initModel()
    }

    private fun initModel(): Boolean {
        try {
            val optionsBuilder = Model.Options.Builder()
            // 使用生成的 Slider 类实例化
            sliderModel = Slider.newInstance(context, optionsBuilder.build())
            Log.record(TAG, "模型初始化成功")
            return true
        } catch (e: IOException) {
            Log.record(TAG, "模型初始化失败: ${e.message}")
            Log.printStackTrace(TAG, "SliderTFLite 初始化异常", e)
            return false
        }
    }

    /**
     * 释放资源
     */
    fun close() {
        sliderModel?.close()
        sliderModel = null
        Log.record(TAG, "模型资源已释放")
    }

    data class DetectionResult(
        val x1: Float, val y1: Float, val x2: Float, val y2: Float,
        val score: Float,
        val classId: Int
    )

    /**
     * 滑块识别结果：包含滑块、缺口、刷新按钮的中心坐标（裁剪图坐标系）。
     * [hasGap] 为 false 表示未识别到缺口（识别失败），但可能仍携带 refresh 按钮
     * 坐标，供调用方点击刷新重试。
     */
    data class SlideRecognitionResult(
        val hasGap: Boolean,    // 是否识别到缺口
        val hasBlock: Boolean,  // 是否识别到滑块块（false 时 sliderX/Y 无效，调用方用手柄中心代偿）
        val sliderX: Float,     // 滑块块中心X（hasGap=false 或 hasBlock=false 时无效）
        val sliderY: Float,     // 滑块块中心Y
        val targetX: Float,     // 缺口中心X
        val targetY: Float,     // 缺口中心Y
        val confidence: Float,  // 置信度
        val candidateCount: Int,// 模型候选框数量
        val refreshX: Float?,   // 刷新按钮中心X（可为 null）
        val refreshY: Float?,   // 刷新按钮中心Y（可为 null）
        val refreshScore: Float?// 刷新按钮置信度（可为 null）
    )

    fun identifyOffset(
        bitmap: Bitmap,
        conf: Float = CONF_THRESHOLD
    ): Pair<Int, Float> {
        val result = identifySlideRecognition(bitmap, conf)
        return if (result != null) {
            Pair(result.targetX.toInt(), result.confidence)
        } else {
            Pair(0, 0f)
        }
    }

    /**
     * 识别滑块验证码的缺口、滑块块和刷新按钮。
     * 返回裁剪图坐标系（原图）坐标；缺口缺失时 [SlideRecognitionResult.hasGap]=false，
     * 调用方可据此触发刷新重试。
     */
    fun identifySlideRecognition(
        bitmap: Bitmap,
        conf: Float = CONF_THRESHOLD
    ): SlideRecognitionResult? {
        val results = predict(bitmap, conf)

        Log.record(TAG, "识别候选框数量: ${results.size}")
        results.forEachIndexed { index, result ->
            Log.record(
                TAG,
                "候选[$index] classId=${result.classId} box=(${result.x1.toInt()},${result.y1.toInt()},${result.x2.toInt()},${result.y2.toInt()}) score=${result.score}"
            )
        }

        if (results.isEmpty()) return null

        // 按类别取最高分框
        val gap = results.filter { it.classId == CLASS_GAP }.maxByOrNull { it.score }
        val block = results.filter { it.classId == CLASS_BLOCK }.maxByOrNull { it.score }
        val refresh = results.filter { it.classId == CLASS_REFRESH }.maxByOrNull { it.score }

        // 缺口缺失 → 仍返回结果（携带 refresh 框），调用方据此触发刷新流程
        if (gap == null) {
            Log.record(TAG, "未识别到缺口(gap)，模型候选=${results.size}，交由调用方刷新重试")
            return SlideRecognitionResult(
                hasGap = false,
                hasBlock = block != null,
                sliderX = 0f, sliderY = 0f,
                targetX = 0f, targetY = 0f,
                confidence = refresh?.score ?: 0f,
                candidateCount = results.size,
                refreshX = refresh?.let { (it.x1 + it.x2) / 2f },
                refreshY = refresh?.let { (it.y1 + it.y2) / 2f },
                refreshScore = refresh?.score
            )
        }

        // 滑块块缺失 → sliderX/Y 置无效值，由调用方用像素扫描的蓝色手柄中心代偿
        // （不能用缺口中心代偿，否则 sliderX==targetX、距离=0，前置检测必失败）
        val hasBlock = block != null
        val blockCenterX = if (block != null) (block.x1 + block.x2) / 2f else 0f
        val blockCenterY = if (block != null) (block.y1 + block.y2) / 2f else 0f
        if (!hasBlock) {
            Log.record(TAG, "未识别到滑块块(block)，slider 坐标置无效，交由调用方用手柄中心代偿")
        }

        val targetCenterX = (gap.x1 + gap.x2) / 2f
        val targetCenterY = (gap.y1 + gap.y2) / 2f
        val refreshCenterX = refresh?.let { (it.x1 + it.x2) / 2f }
        val refreshCenterY = refresh?.let { (it.y1 + it.y2) / 2f }

        Log.record(
            TAG,
            "缺口中心: (${targetCenterX.toInt()},${targetCenterY.toInt()}), 滑块块中心: ${if (hasBlock) "(${blockCenterX.toInt()},${blockCenterY.toInt()})" else "无"}, 刷新: ${if (refresh != null) "(${refreshCenterX!!.toInt()},${refreshCenterY!!.toInt()})" else "null"}, 距离: ${if (hasBlock) (targetCenterX - blockCenterX).toInt() else "N/A"}"
        )
        return SlideRecognitionResult(
            hasGap = true,
            hasBlock = hasBlock,
            sliderX = blockCenterX,
            sliderY = blockCenterY,
            targetX = targetCenterX,
            targetY = targetCenterY,
            confidence = gap.score,
            candidateCount = results.size,
            refreshX = refreshCenterX,
            refreshY = refreshCenterY,
            refreshScore = refresh?.score
        )
    }

    private fun predict(
        img: Bitmap,
        confThreshold: Float
    ): List<DetectionResult> {
        val model = sliderModel ?: return emptyList()

        // 1. 预处理 Letterbox
        val (inputBitmap, ratio, padding) = letterbox(img)

        // 2. 准备输入 TensorBuffer（NCHW [1, 3, 640, 640]，RGB 三通道在前）
        val inputFeature0 = TensorBuffer.createFixedSize(
            intArrayOf(1, INPUT_CHANNELS, INPUT_SIZE, INPUT_SIZE),
            DataType.FLOAT32
        )

        // 将 Bitmap 数据按 NCHW 布局加载
        loadBitmapToTensorBufferNchw(inputBitmap, inputFeature0)

        // 3. 执行推理
        val outputs = model.process(inputFeature0)

        // 4. 获取扁平化的输出数组（[1, 300, 6] = MAX_DET * 6）
        val predsFlat = outputs.outputFeature0AsTensorBuffer.floatArray

        return postprocess(
            predsFlat,
            img.width,
            img.height,
            ratio,
            padding,
            confThreshold
        )
    }

    /**
     * 以 NCHW 布局加载像素到 TensorBuffer。
     * 模型输入为 [1, 3, 640, 640]，即通道在前，需逐通道填充。
     */
    private fun loadBitmapToTensorBufferNchw(bitmap: Bitmap, tensorBuffer: TensorBuffer) {
        val floatBuffer = tensorBuffer.buffer.order(ByteOrder.nativeOrder())
        floatBuffer.rewind()

        val size = INPUT_SIZE * INPUT_SIZE
        val pixels = IntArray(size)
        bitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        val rBuffer = FloatArray(size)
        val gBuffer = FloatArray(size)
        val bBuffer = FloatArray(size)
        for (i in pixels.indices) {
            val p = pixels[i]
            rBuffer[i] = ((p shr 16) and 0xFF) / 255.0f
            gBuffer[i] = ((p shr 8) and 0xFF) / 255.0f
            bBuffer[i] = (p and 0xFF) / 255.0f
        }
        // NCHW: 先写所有 R 通道，再写 G，再写 B
        val floatView = floatBuffer.asFloatBuffer()
        floatView.put(rBuffer)
        floatView.put(gBuffer)
        floatView.put(bBuffer)
    }

    /**
     * 后处理：解析 [1, 300, 6] e2e 输出（无 NMS）。
     * 每行 [x1, y1, x2, y2, confidence, classId]，坐标为 letterbox 640 空间像素，
     * 按 `(v - pad) / ratio` 还原到原图。
     */
    private fun postprocess(
        preds: FloatArray,  // Flat: [MAX_DET * 6] = [300 * 6]
        orgW: Int, orgH: Int,
        ratio: Float, padding: Pair<Int, Int>,
        confThreshold: Float
    ): List<DetectionResult> {
        val finalResults = ArrayList<DetectionResult>()
        val numDet = preds.size / 6
        for (i in 0 until numDet) {
            val off = i * 6
            val x1 = preds[off + 0]
            val y1 = preds[off + 1]
            val x2 = preds[off + 2]
            val y2 = preds[off + 3]
            val score = preds[off + 4]
            val classId = preds[off + 5].toInt()

            if (score < confThreshold) continue
            if (classId < 0 || classId >= NUM_CLASSES) continue

            // 坐标还原：letterbox 像素 → 原图
            val rX1 = ((x1 - padding.first) / ratio).coerceIn(0f, orgW.toFloat())
            val rY1 = ((y1 - padding.second) / ratio).coerceIn(0f, orgH.toFloat())
            val rX2 = ((x2 - padding.first) / ratio).coerceIn(0f, orgW.toFloat())
            val rY2 = ((y2 - padding.second) / ratio).coerceIn(0f, orgH.toFloat())
            finalResults.add(
                DetectionResult(rX1, rY1, rX2, rY2, score, classId)
            )
        }

        return finalResults
    }

    private fun letterbox(img: Bitmap): Triple<Bitmap, Float, Pair<Int, Int>> {
        val w = img.width
        val h = img.height
        val newShape = INPUT_SIZE
        val r = min(newShape.toFloat() / w, newShape.toFloat() / h)
        val newUnpadW = (w * r).roundToInt()
        val newUnpadH = (h * r).roundToInt()
        val dw = (newShape - newUnpadW) / 2
        val dh = (newShape - newUnpadH) / 2
        val resized = Bitmap.createScaledBitmap(img, newUnpadW, newUnpadH, true)
        val result = Bitmap.createBitmap(newShape, newShape, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(Color.rgb(114, 114, 114))
        canvas.drawBitmap(resized, dw.toFloat(), dh.toFloat(), null)
        return Triple(result, r, Pair(dw, dh))
    }
}
