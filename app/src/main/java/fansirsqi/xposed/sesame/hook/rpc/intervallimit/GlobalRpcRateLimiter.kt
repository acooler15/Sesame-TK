package fansirsqi.xposed.sesame.hook.rpc.intervallimit

import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.hook.captcha.RpcPauseGate
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * 全局限流器，替代 [RpcIntervalLimit] 的串行化职责。
 *
 * 限流语义：
 * - 全局 `Semaphore` 限制同时在途的 RPC 请求数（默认 [maxConcurrency]，可配置）。允许小幅并发但不批量，满足"不批量请求"的风控约束。
 * - per-method `Mutex` + `delay` 保证同一 method 两次发送之间的最小间隔，不持有到 RPC 完成，不影响不同 method 的并发。
 * - 两层独立：并发数限制是全局的（跨所有 method），per-method 间隔是局部的（仅约束同一 method），二者取并集。
 */
object GlobalRpcRateLimiter {
    private const val TAG = "GlobalRpcRateLimiter"

    // 全局并发信号量：限制同时在途的 RPC 请求数
    // 默认 2，可配置。允许小幅并发但不批量，平衡风控与吞吐。
    @Volatile
    var maxConcurrency: Int = 2

    private val concurrencySemaphore = Semaphore(maxConcurrency)

    // per-method 互斥锁：保证同一 method 的请求之间保持间隔
    private val methodMutexMap = ConcurrentHashMap<String, Mutex>()

    // per-method 间隔配置（保留现有业务注册能力）
    private val methodIntervalMap = ConcurrentHashMap<String, IntervalLimit>()

    /**
     * 获取发送许可（挂起，直到满足验证码暂停闸门 + 并发数 + per-method 间隔要求）。
     *
     * 流程：
     * 1. 检查验证码暂停闸门：若处于验证码处理暂停状态，挂起等待放行
     *    （放行返回 true；超时被取消则返回 false，调用方应丢弃本次请求）
     * 2. 若该 method 有间隔配置，获取 method 互斥锁并等待间隔（保证同一 method 两次发送间隔）
     * 3. 获取全局并发许可（限制同时在途请求数）
     *
     * 调用方在 acquire 返回非空后执行 RPC，执行完毕调用 Permit.close()（推荐 use {} 模式）。
     *
     * @return 非空表示可发送；null 表示请求被暂停闸门取消（验证码超时），应丢弃本次请求。
     */
    suspend fun acquire(method: String?): Permit? {
        // 0. 验证码暂停闸门：验证码处理期间暂停后续 RPC 请求
        if (!RpcPauseGate.awaitSendable()) {
            Log.record(TAG, "请求被验证码暂停闸门取消，丢弃: $method")
            return null
        }

        // 1. per-method 间隔控制（发送间隔，不持有到 RPC 完成）
        val methodInterval = method?.let { methodIntervalMap[it] }
        if (methodInterval != null) {
            val methodMutex = methodMutexMap.computeIfAbsent(method) { Mutex() }
            methodMutex.withLock {
                val now = System.currentTimeMillis()
                val interval = methodInterval.interval ?: 0
                val wait = interval.toLong() - (now - methodInterval.time)
                if (wait > 0) {
                    Log.debug(TAG, "method[$method] 等待 ${wait}ms 后发送")
                    delay(wait)
                }
                methodInterval.time = System.currentTimeMillis()
            }
            // method Mutex 在此释放，RPC 执行期间不持有
        }

        // 2. 获取全局并发许可
        concurrencySemaphore.acquire()
        Log.debug(TAG, "获取并发许可，开始发送: $method")

        return Permit {
            concurrencySemaphore.release()
            Log.debug(TAG, "释放并发许可，完成: $method")
        }
    }

    // per-method 间隔注册（保留兼容）
    fun addIntervalLimit(method: String, interval: Int) {
        methodIntervalMap[method] = DefaultIntervalLimit(interval)
    }

    fun addIntervalLimit(method: String, intervalLimit: IntervalLimit) {
        methodIntervalMap[method] = intervalLimit
    }

    fun updateIntervalLimit(method: String, interval: Int) {
        methodIntervalMap[method] = DefaultIntervalLimit(interval)
    }

    fun clearIntervalLimit() {
        methodIntervalMap.clear()
        methodMutexMap.clear()
    }

    /**
     * 许可证，AutoCloseable，用 use {} 确保释放并发许可
     */
    class Permit(private val onRelease: () -> Unit) : AutoCloseable {
        override fun close() = onRelease()
    }
}