package fansirsqi.xposed.sesame.util
import fansirsqi.xposed.sesame.core.log.Log

import java.time.Duration
import java.time.Instant
import java.util.function.Consumer

class TimeCounter(private val name: String) {

    private val start: Instant = Instant.now()
    private var lastCheckpoint: Instant = start
    private var stopped = false
    private var unexceptCnt = 0
    private val resultMsg = StringBuilder()
    private var _logger: Consumer<String>? = null

    // 类似 C++ 析构的手动调用逻辑
    fun close() {
        if (stopped) {
            return
        }
        if (unexceptCnt > 0) {
            stop()
        }
    }

    fun stop() {
        val end = Instant.now()
        val durationMs = Duration.between(start, end).toMillis()
        Log.record(name, String.format("========================\n%s 耗时: %d ms (%s)",
                name, durationMs, resultMsg))
        stopped = true
    }

    fun countDebug(msg: String) {
        val now = Instant.now()
        val durationMs = Duration.between(lastCheckpoint, now).toMillis()
        Log.record(name, String.format("========================\n%s 耗时: %d ms", msg, durationMs))
        lastCheckpoint = now
    }

    fun count(msg: String) {
        val now = Instant.now()
        val durationMs = Duration.between(lastCheckpoint, now).toMillis()
        resultMsg.append(msg).append(":").append(durationMs).append(" ms, ")
        lastCheckpoint = now
    }

    fun countUnexcept(msg: String, exceptMs: Long) {
        val now = Instant.now()
        val durationMs = Duration.between(lastCheckpoint, now).toMillis()
        if (durationMs > exceptMs) {
            resultMsg.append(msg).append(":").append(durationMs)
                     .append(" ms(except:").append(exceptMs).append("ms), ")
            unexceptCnt++
        }
        lastCheckpoint = now
    }
}
