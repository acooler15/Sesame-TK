package fansirsqi.xposed.sesame.hook.simple

import android.content.Context
import android.graphics.*
import android.os.Looper
import fansirsqi.xposed.sesame.ml.Slider
import fansirsqi.xposed.sesame.util.GlobalThreadPools
import fansirsqi.xposed.sesame.util.Log
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.tensorflow.lite.DataType
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.support.model.Model
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import kotlin.math.*

class SliderTFLite(val context: Context) {

    companion object {
        private const val TAG = "SliderTFLite"
        private const val CONF_THRESHOLD = 0.5f
        private const val IOU_THRESHOLD = 0.8f
        private const val Y_IOU_THRESHOLD = 0.85f
        private const val INPUT_SIZE = 640
        private const val MASK_NUM = 32
        private const val NUM_ANCHORS = 8400
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
            conf: Float = CONF_THRESHOLD,
            iou: Float = IOU_THRESHOLD
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
                    detector.identifySlideRecognition(bitmap, conf, iou)
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
            // 配置 GPU 或 CPU 线程
            val optionsBuilder = Model.Options.Builder()
            //if (CompatibilityList().isDelegateSupportedOnThisDevice) {
            //    Log.record(TAG, "GPU支持加速,启用GPU加速")
            //    optionsBuilder.setDevice(Model.Device.GPU)
            //} else {
            //    optionsBuilder.setNumThreads(4)
            //}

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
        val classId: Int,
        val maskCoeffs: FloatArray,
        var mask: Bitmap? = null
    )

    /**
     * 滑块识别结果：包含滑块坐标和目标缺口坐标
     */
    data class SlideRecognitionResult(
        val sliderX: Float,     // 滑块中心X
        val sliderY: Float,     // 滑块中心Y
        val targetX: Float,     // 目标缺口中心X
        val targetY: Float,     // 目标缺口中心Y
        val confidence: Float,  // 置信度
        val candidateCount: Int // 模型候选框数量
    )

    fun identifyOffset(
        bitmap: Bitmap,
        conf: Float = CONF_THRESHOLD,
        iou: Float = IOU_THRESHOLD
    ): Pair<Int, Float> {
        val result = identifySlideRecognition(bitmap, conf, iou)
        return if (result != null) {
            Pair(result.targetX.toInt(), result.confidence)
        } else {
            Pair(0, 0f)
        }
    }

    /**
     * 识别滑块验证码的滑块和目标缺口位置。
     * 用于全屏截图模式，返回滑块和缺口的屏幕坐标。
     */
    fun identifySlideRecognition(
        bitmap: Bitmap,
        conf: Float = CONF_THRESHOLD,
        iou: Float = IOU_THRESHOLD
    ): SlideRecognitionResult? {
        val results = predict(bitmap, conf, iou)

        Log.record(TAG, "识别候选框数量: ${results.size}")
        results.forEachIndexed { index, result ->
            Log.record(
                TAG,
                "候选[$index] box=(${result.x1.toInt()},${result.y1.toInt()},${result.x2.toInt()},${result.y2.toInt()}) score=${result.score}"
            )
        }

        if (results.isEmpty()) return null

        var targetBox: DetectionResult?
        var sliderBox: DetectionResult? = null

        if (results.size == 1) {
            // 只检测到一个框，可能是滑块也可能是目标，返回其位置让调用方判断
            val box = results[0]
            Log.record(TAG, "仅检测到1个框: (${box.x1.toInt()},${box.y1.toInt()}) score=${box.score}")
            return SlideRecognitionResult(
                sliderX = (box.x1 + box.x2) / 2f,
                sliderY = (box.y1 + box.y2) / 2f,
                targetX = (box.x1 + box.x2) / 2f,
                targetY = (box.y1 + box.y2) / 2f,
                confidence = box.score,
                candidateCount = 1
            )
        } else {
            // 1. 找到最左边的作为滑块
            val sliderIndex = results.indices.minByOrNull { results[it].x1 } ?: 0
            val slider = results[sliderIndex]
            sliderBox = slider
            Log.record(TAG, "判定滑块框: index=$sliderIndex center=(${(slider.x1+slider.x2)/2f},${(slider.y1+slider.y2)/2f}) score=${slider.score}")

            val candidates = results.filterIndexed { index, _ -> index != sliderIndex }

            if (candidates.isEmpty()) {
                targetBox = slider
            } else {
                // 2. 筛选 Y 轴 IoU
                val yFiltered = candidates.filter {
                    yIou(slider, it) > Y_IOU_THRESHOLD
                }

                val finalCandidates = if (yFiltered.isEmpty()) candidates else yFiltered

                if (finalCandidates.size == 1) {
                    targetBox = finalCandidates[0]
                } else {
                    // 3. 形状匹配
                    var maxIou = -1f
                    var bestCandidate = finalCandidates[0]

                    val sliderMask = slider.mask ?: generateMask(slider)

                    for (candidate in finalCandidates) {
                        val candidateMask = candidate.mask ?: generateMask(candidate)
                        val shapeIou = calculateShapeIou(sliderMask, candidateMask)
                        if (shapeIou > maxIou) {
                            maxIou = shapeIou
                            bestCandidate = candidate
                        }
                    }
                    targetBox = bestCandidate
                }
            }
        }

        if (targetBox != null && sliderBox != null) {
            val sliderCenterX = (sliderBox.x1 + sliderBox.x2) / 2f
            val sliderCenterY = (sliderBox.y1 + sliderBox.y2) / 2f
            val targetCenterX = (targetBox.x1 + targetBox.x2) / 2f
            val targetCenterY = (targetBox.y1 + targetBox.y2) / 2f
            Log.record(TAG, "滑块中心: (${sliderCenterX.toInt()},${sliderCenterY.toInt()}), 目标中心: (${targetCenterX.toInt()},${targetCenterY.toInt()}), 距离: ${(targetCenterX-sliderCenterX).toInt()}")
            return SlideRecognitionResult(
                sliderX = sliderCenterX,
                sliderY = sliderCenterY,
                targetX = targetCenterX,
                targetY = targetCenterY,
                confidence = targetBox.score,
                candidateCount = results.size
            )
        }

        return null
    }

    private fun predict(
        img: Bitmap,
        confThreshold: Float,
        iouThreshold: Float
    ): List<DetectionResult> {
        val model = sliderModel ?: return emptyList()

        // 1. 预处理 Letterbox
        val (inputBitmap, ratio, padding) = letterbox(img)

        // 2. 准备输入 TensorBuffer
        // 创建固定大小的 TensorBuffer [1, 640, 640, 3]
        val inputFeature0 = TensorBuffer.createFixedSize(
            intArrayOf(1, INPUT_SIZE, INPUT_SIZE, 3),
            DataType.FLOAT32
        )

        // 将 Bitmap 数据加载到 TensorBuffer
        loadBitmapToTensorBuffer(inputBitmap, inputFeature0)

        // 3. 执行推理
        val outputs = model.process(inputFeature0)

        // 4. 获取扁平化的输出数组
        // Output 0: [1, 37, 8400] -> 扁平化数组
        val predsFlat = outputs.outputFeature0AsTensorBuffer.floatArray
        // Output 1: [1, 160, 160, 32] -> 扁平化数组
        val protosFlat = outputs.outputFeature1AsTensorBuffer.floatArray

        return postprocess(
            predsFlat,
            protosFlat,
            img.width,
            img.height,
            ratio,
            padding,
            confThreshold,
            iouThreshold
        )
    }

    private fun loadBitmapToTensorBuffer(bitmap: Bitmap, tensorBuffer: TensorBuffer) {
        val floatBuffer = tensorBuffer.buffer.order(ByteOrder.nativeOrder())
        // 确保 buffer 在写入前处于正确位置
        floatBuffer.rewind()

        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        // 归一化并写入：(pixel / 255.0f)
        for (pixel in pixels) {
            floatBuffer.putFloat(((pixel shr 16) and 0xFF) / 255.0f) // R
            floatBuffer.putFloat(((pixel shr 8) and 0xFF) / 255.0f)  // G
            floatBuffer.putFloat((pixel and 0xFF) / 255.0f)         // B
        }
    }

    private fun postprocess(
        preds: FloatArray,  // Flat: [37 * 8400]
        protos: FloatArray, // Flat: [160 * 160 * 32]
        orgW: Int, orgH: Int,
        ratio: Float, padding: Pair<Int, Int>,
        confThreshold: Float, iouThreshold: Float
    ): List<DetectionResult> {
        val proposals = ArrayList<DetectionResult>()

        // 数据布局说明：
        // preds 原始形状 [1, 37, 8400] (Batch, Channels, Anchors)
        // 在扁平数组中，索引计算为: channel * NUM_ANCHORS + anchor_index
        // Channel 0: x, 1: y, 2: w, 3: h, 4: score, 5..36: mask coeffs

        for (i in 0 until NUM_ANCHORS) {
            val scoreIdx = 4 * NUM_ANCHORS + i
            val score = preds[scoreIdx]

            if (score > confThreshold) {
                // 读取坐标
                val cx = preds[0 * NUM_ANCHORS + i]
                val cy = preds[1 * NUM_ANCHORS + i]
                val w = preds[2 * NUM_ANCHORS + i]
                val h = preds[3 * NUM_ANCHORS + i]

                val x1 = cx - w / 2
                val y1 = cy - h / 2
                val x2 = cx + w / 2
                val y2 = cy + h / 2

                // 提取 Mask 系数 (32个)
                val maskCoeffs = FloatArray(MASK_NUM)
                for (j in 0 until MASK_NUM) {
                    val coeffIdx = (5 + j) * NUM_ANCHORS + i
                    maskCoeffs[j] = preds[coeffIdx]
                }

                proposals.add(DetectionResult(x1, y1, x2, y2, score, 0, maskCoeffs))
            }
        }

        // NMS
        val nmsResults = nms(proposals, iouThreshold)

        val finalResults = ArrayList<DetectionResult>()

        for (res in nmsResults) {
            // 还原坐标到原图
            val rX1 = ((res.x1 - padding.first) / ratio).coerceIn(0f, orgW.toFloat())
            val rY1 = ((res.y1 - padding.second) / ratio).coerceIn(0f, orgH.toFloat())
            val rX2 = ((res.x2 - padding.first) / ratio).coerceIn(0f, orgW.toFloat())
            val rY2 = ((res.y2 - padding.second) / ratio).coerceIn(0f, orgH.toFloat())

            // 生成 Mask
            // 注意：process_mask 使用的是 letterbox 后的坐标
            // protos 已经是扁平的，直接传入
            val mask = processMask(
                res.maskCoeffs,
                protos,
                res.x1, res.y1, res.x2, res.y2,
                160, 160,
                INPUT_SIZE, INPUT_SIZE
            )

            val croppedMask = cropAndScaleMask(
                mask, 160, 160,
                res.x1, res.y1, res.x2, res.y2,
                ratio, padding, orgW, orgH, rX1, rY1, rX2 - rX1, rY2 - rY1
            )

            finalResults.add(
                DetectionResult(
                    rX1, rY1, rX2, rY2,
                    res.score, res.classId,
                    res.maskCoeffs, croppedMask
                )
            )
        }

        return finalResults
    }

    /**
     * 矩阵乘法 + Sigmoid 生成原始 Mask
     * 优化：直接使用扁平数组，移除多余对象创建
     */
    private fun processMask(
        coeffs: FloatArray, // [32]
        protos: FloatArray, // [160 * 160 * 32]
        boxX1: Float, boxY1: Float, boxX2: Float, boxY2: Float,
        protoH: Int, protoW: Int,
        imgH: Int, imgW: Int
    ): BooleanArray {
        val mask = BooleanArray(protoH * protoW)

        val sx = protoW.toFloat() / imgW
        val sy = protoH.toFloat() / imgH

        val bx1 = (boxX1 * sx).toInt().coerceIn(0, protoW)
        val by1 = (boxY1 * sy).toInt().coerceIn(0, protoH)
        val bx2 = (boxX2 * sx).toInt().coerceIn(0, protoW)
        val by2 = (boxY2 * sy).toInt().coerceIn(0, protoH)

        // Proto 内存布局是 [H, W, C] (160, 160, 32)
        // 扁平索引 = (y * W + x) * C + k

        for (y in by1 until by2) {
            val yOffset = y * protoW
            for (x in bx1 until bx2) {
                var sum = 0f
                val baseIdx = (yOffset + x) * MASK_NUM

                for (k in 0 until MASK_NUM) {
                    sum += coeffs[k] * protos[baseIdx + k]
                }

                val prob = 1.0f / (1.0f + exp(-sum))
                if (prob > 0.5f) {
                    mask[yOffset + x] = true
                }
            }
        }
        return mask
    }

    // --- 辅助函数保持逻辑不变，仅作签名适配 ---

    private fun cropAndScaleMask(
        rawMask: BooleanArray, rawW: Int, rawH: Int,
        lbX1: Float, lbY1: Float, lbX2: Float, lbY2: Float,
        ratio: Float, padding: Pair<Int, Int>,
        orgW: Int, orgH: Int,
        finalX: Float, finalY: Float, finalW: Float, finalH: Float
    ): Bitmap {
        val w = max(1, finalW.toInt())
        val h = max(1, finalH.toInt())
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ALPHA_8)

        for (y in 0 until h) {
            for (x in 0 until w) {
                val ox = finalX + x
                val oy = finalY + y
                val lx = ox * ratio + padding.first
                val ly = oy * ratio + padding.second
                val mx = (lx * (rawW.toFloat() / INPUT_SIZE)).toInt()
                val my = (ly * (rawH.toFloat() / INPUT_SIZE)).toInt()

                if (mx in 0 until rawW && my in 0 until rawH) {
                    if (rawMask[my * rawW + mx]) {
                        bmp.setPixel(x, y, Color.WHITE)
                    }
                }
            }
        }
        return bmp
    }

    private fun generateMask(result: DetectionResult): Bitmap? = result.mask

    private fun yIou(box1: DetectionResult, box2: DetectionResult): Float {
        val start = max(box1.y1, box2.y1)
        val end = min(box1.y2, box2.y2)
        val intersection = max(0f, end - start)
        val len1 = box1.y2 - box1.y1
        val len2 = box2.y2 - box2.y1
        val union = len1 + len2 - intersection
        return if (union != 0f) intersection / union else 0f
    }

    private fun calculateShapeIou(mask1: Bitmap?, mask2: Bitmap?): Float {
        if (mask1 == null || mask2 == null) return 0f
        val compareSize = 100
        val sMask1 = Bitmap.createScaledBitmap(mask1, compareSize, compareSize, true)
        val sMask2 = Bitmap.createScaledBitmap(mask2, compareSize, compareSize, true)
        var intersection = 0
        var union = 0
        for (y in 0 until compareSize) {
            for (x in 0 until compareSize) {
                val p1 = sMask1.getPixel(x, y) != 0
                val p2 = sMask2.getPixel(x, y) != 0
                if (p1 && p2) intersection++
                if (p1 || p2) union++
            }
        }
        return if (union > 0) intersection.toFloat() / union else 0f
    }

    private fun nms(boxes: List<DetectionResult>, threshold: Float): List<DetectionResult> {
        val sorted = boxes.sortedByDescending { it.score }.toMutableList()
        val selected = ArrayList<DetectionResult>()
        while (sorted.isNotEmpty()) {
            val first = sorted[0]
            selected.add(first)
            sorted.removeAt(0)
            val iterator = sorted.iterator()
            while (iterator.hasNext()) {
                val next = iterator.next()
                if (iou(first, next) >= threshold) {
                    iterator.remove()
                }
            }
        }
        return selected
    }

    private fun iou(a: DetectionResult, b: DetectionResult): Float {
        val areaA = (a.x2 - a.x1) * (a.y2 - a.y1)
        val areaB = (b.x2 - b.x1) * (b.y2 - b.y1)
        val left = max(a.x1, b.x1)
        val right = min(a.x2, b.x2)
        val top = max(a.y1, b.y1)
        val bottom = min(a.y2, b.y2)
        val w = max(0f, right - left)
        val h = max(0f, bottom - top)
        val inter = w * h
        return inter / (areaA + areaB - inter)
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
