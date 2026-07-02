package fansirsqi.xposed.sesame.hook.simple

import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import android.view.View
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * 模拟系统级 MotionEvent 的工具类.
 * 用于执行如滑动等自动化操作.
 */
object MotionEventSimulator {

    private const val TAG = "MotionEventSimulator"

    private data class LocalPoint(val x: Float, val y: Float)

    /**
     * 异步模拟一个从起点到终点的滑动操作.
     *
     * @param view 要在其上执行滑动操作的视图 (通常是滑块本身).
     * @param startX 滑动的屏幕绝对 X 坐标起点.
     * @param startY 滑动的屏幕绝对 Y 坐标起点.
     * @param endX 滑动的屏幕绝对 X 坐标终点.
     * @param endY 滑动的屏幕绝对 Y 坐标终点.
     * @param duration 滑动动画的总时长 (毫秒).
     * @return true 表示触摸事件已成功派发，false 表示 View 不可交互无法派发.
     */
    suspend fun simulateSwipe(
        view: View,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        duration: Long = 800L
    ): Boolean {
        if (!view.isShown || !view.isEnabled) {
            Log.w(TAG, "simulateSwipe 跳过: view.isShown=${view.isShown}, view.isEnabled=${view.isEnabled}，屏幕可能处于锁屏/息屏状态")
            return false
        }

        val localStart = toLocalPoint(view, startX, startY)
        val localEnd = toLocalPoint(view, endX, endY)

        Log.d(
            TAG,
            "simulateSwipe screen=($startX,$startY)->($endX,$endY), local=(${localStart.x},${localStart.y})->(${localEnd.x},${localEnd.y}), duration=$duration"
        )

        val downTime = SystemClock.uptimeMillis()
        try {
            // 1. ACTION_DOWN
            dispatchTouchEvent(view, MotionEvent.ACTION_DOWN, localStart.x, localStart.y, downTime, downTime)
            delay(Random.nextLong(80, 140)) // 模拟按压瞬间的反应时间

            // 2. 生成拟人化轨迹点
            val distance = localEnd.x - localStart.x
            val tracks = generateHumanLikeTracks(distance)
            val stepDelay = if (tracks.isEmpty()) 12L else (duration / tracks.size).coerceIn(8L, 28L)

            var lastEventTime = downTime
            tracks.forEach { (relX, relY) ->
                val currentX = localStart.x + relX
                // 在 Y 轴加入随机微抖动
                val currentY = localStart.y + relY + Random.nextInt(-2, 3)

                lastEventTime += stepDelay + Random.nextLong(0, 8)
                dispatchTouchEvent(view, MotionEvent.ACTION_MOVE, currentX, currentY, downTime, lastEventTime)
                delay(stepDelay)
            }

            // 3. ACTION_UP (在最终目标点抬起)
            val finalUpTime = lastEventTime + Random.nextLong(60, 120)
            delay(Random.nextLong(50, 90))
            dispatchTouchEvent(view, MotionEvent.ACTION_UP, localEnd.x, localEnd.y, downTime, finalUpTime)
        } catch (e: Throwable) {
            Log.e(TAG, "滑动异常", e)
            return false
        }
        return true
    }

    /**
     * 生成非线性的拟人轨迹
     * 逻辑：加速 -> 快速 -> 减速 -> 超过目标(Overshoot) -> 拨回(Back)
     */
    private fun generateHumanLikeTracks(totalDistance: Float): List<Pair<Float, Float>> {
        val tracks = mutableListOf<Pair<Float, Float>>()
        var currentX = 0f
        var v = 0f         // 初速度
        val a_accel = 1.5f // 加速度系数
        val a_decel = -2.0f // 减速阶段系数
        val mid = totalDistance * 0.7f // 前70%进行冲刺
        
        // 模拟滑动过程
        while (currentX < totalDistance) {
            val a = if (currentX < mid) a_accel else a_decel
            val t = 0.5f // 时间步长
            val move = v * t + 0.5f * a * t * t
            v += a * t
            if (v < 0.5f && currentX >= mid) v = 0.5f // 保持最起码的移动
            
            currentX += move
            tracks.add(Pair(currentX, 0f))
            
            if (currentX > totalDistance) break
        }

        // 模拟拟人化特征：回摆 (Overshoot & Back)
        // 手指经常会滑过一点点然后再拨回来
        val overshoot = Random.nextFloat() * 5f + 2f // 划过 2~7 像素
        tracks.add(Pair(totalDistance + overshoot, 0f))
        tracks.add(Pair(totalDistance + (overshoot / 2), 0f))
        tracks.add(Pair(totalDistance, 0f))

        return tracks
    }

    /**
     * 辅助函数，用于创建和派发 MotionEvent.
     */
    private fun dispatchTouchEvent(
        view: View,
        action: Int,
        x: Float,
        y: Float,
        downTime: Long,
        eventTime: Long
    ) {
        val properties = arrayOf(MotionEvent.PointerProperties().apply {
            id = 0
            toolType = MotionEvent.TOOL_TYPE_FINGER
        })
        val cords = arrayOf(MotionEvent.PointerCoords().apply {
            this.x = x
            this.y = y
            pressure = 1f
            size = 1f
        })
        val event = MotionEvent.obtain(downTime, eventTime, action, 1, properties, cords, 0, 0, 1f, 1f, 0, 0, 0, 0)
        view.dispatchTouchEvent(event)
        event.recycle()
    }

    private fun toLocalPoint(view: View, screenX: Float, screenY: Float): LocalPoint {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        return LocalPoint(
            x = screenX - location[0],
            y = screenY - location[1]
        )
    }
}
