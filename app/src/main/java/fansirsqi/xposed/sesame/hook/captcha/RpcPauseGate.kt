package fansirsqi.xposed.sesame.hook.captcha

import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.hook.ApplicationHook
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * RPC 暂停闸门。
 *
 * 当 RPC 检测到验证码加载信号（[CaptchaRpcSignal] 命中 "alipay.security.antcaptcha.verify"）时，
 * 暂停本模块后续的 RPC 请求，避免在验证码未通过时继续发起请求、反复触发新的验证码。
 *
 * 语义：
 * - [activate]：命中验证码 RPC 时激活，暂停后续 RPC 请求。
 * - [onCaptchaHandled]：验证码处理成功（handler 返回 HANDLED）时调用，放行挂起的请求。
 * - 超时：超时时长复用任务默认超时配置（[ApplicationHook.config.taskDefaultTimeout]），
 *   超时后**取消**（丢弃）挂起中的请求并关闭闸门，而非放行——
 *   放行可能让这些请求重新触发验证码 RPC（超时取消语义）。
 */
object RpcPauseGate {

    private const val TAG = "RpcPauseGate"

    @Volatile
    private var paused = false

    // 等待放行的挂起请求集合（内部锁保护）
    private val waiters = linkedSetOf<CompletableDeferred<Boolean>>()
    private val lock = Any()

    // 闸门超时任务的作用域（自包含，生命周期与模块一致）
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** 当前是否处于暂停状态 */
    fun isPaused(): Boolean = synchronized(lock) { paused }

    /**
     * 激活暂停闸门：命中验证码加载 RPC 时调用。
     * 超时时长在激活时运行时读取，支持配置热更新；已在暂停中则忽略重复信号。
     */
    fun activate() {
        synchronized(lock) {
            if (paused) return
            paused = true
            waiters.clear()
        }
        val timeoutMs = ApplicationHook.config.taskDefaultTimeout.value.toLong()
        Log.record(TAG, "[暂停闸门] 激活，暂停后续 RPC 请求，超时=${timeoutMs}ms")
        scope.launch {
            delay(timeoutMs)
            synchronized(lock) {
                if (!paused) return@launch
                paused = false
                waiters.forEach { it.complete(false) }
                waiters.clear()
            }
            Log.record(TAG, "[暂停闸门] 超时(${timeoutMs}ms)，取消挂起中的 RPC 请求")
        }
    }

    /**
     * 放行挂起中的 RPC 请求。
     *
     * 调用场景（[reason] 用于日志区分放行来源）：
     * - 验证码处理成功（handler 返回 HANDLED）；
     * - 锚点文案消失，验证码页已关闭（[CaptchaAnchorWatcher]，覆盖用户手动完成场景）；
     * - 判定非验证码页（handler 返回 SKIP_NON_RETRYABLE，闸门误激活）。
     */
    fun onCaptchaHandled(reason: String) {
        synchronized(lock) {
            if (!paused) return
            paused = false
            waiters.forEach { it.complete(true) }
            waiters.clear()
        }
        Log.record(TAG, "[暂停闸门] 放行挂起的 RPC 请求: $reason")
    }

    /**
     * 等待闸门放行（挂起）。
     *
     * @return true=可继续发送；false=请求被闸门取消（验证码超时），调用方应丢弃本次请求。
     */
    suspend fun awaitSendable(): Boolean {
        synchronized(lock) {
            if (!paused) return true
        }
        val deferred = CompletableDeferred<Boolean>()
        synchronized(lock) {
            if (!paused) {
                // 竞态：加入等待前闸门已恢复，直接放行
                return true
            }
            waiters.add(deferred)
        }
        return try {
            deferred.await()
        } catch (e: CancellationException) {
            // 调用方协程被取消，视同取消本次请求
            false
        } finally {
            synchronized(lock) { waiters.remove(deferred) }
        }
    }
}
