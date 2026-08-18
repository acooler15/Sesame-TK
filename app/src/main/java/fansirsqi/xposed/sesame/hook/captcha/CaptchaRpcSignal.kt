package fansirsqi.xposed.sesame.hook.captcha

import fansirsqi.xposed.sesame.hook.rpc.TokenHooker
import fansirsqi.xposed.sesame.core.log.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 验证码加载 RPC 信号。
 *
 * 支付宝加载 WebView 滑块验证码前会发起 RPC 请求 "alipay.security.antcaptcha.verify"
 * （仅 XRiverTransActivity$Main / NebulaTransActivity$Main 加载验证码时才会请求）。
 * 记录最近一次命中时间，供 Captcha2Handler 作为可靠的验证码出现信号，
 * 避免在非验证码页执行解锁与截图识别造成电量消耗。
 *
 * 除被动查询（isVerifyRpcRecent）外，提供事件驱动等待（awaitHit）：
 * handler 触发常早于 H5 发起该 RPC（onResume 后即执行），等待机制消除读取竞态。
 */
object CaptchaRpcSignal {

    private const val TAG = "CaptchaRpcSignal"

    /** 加载验证码的 RPC 方法名 */
    const val VERIFY_RPC_METHOD = "alipay.security.antcaptcha.verify"

    /** 信号判定时间窗口（毫秒）：命中后该窗口内视为验证码页 */
    private const val SIGNAL_WINDOW_MS = 15_000L

    @Volatile
    private var lastVerifyRpcAt = 0L

    /** 事件等待器：onVerifyRpcHit 完成并重建，保证每次等待都对应"新"命中 */
    private val signalLock = Any()

    @Volatile
    private var hitSignal = CompletableDeferred<Unit>()

    /** 记录验证码 RPC 命中（由 TokenHooker 回调触发） */
    fun onVerifyRpcHit() {
        lastVerifyRpcAt = System.currentTimeMillis()
        Log.record(TAG, "命中验证码加载 RPC: $VERIFY_RPC_METHOD")
        synchronized(signalLock) {
            hitSignal.complete(Unit)
            hitSignal = CompletableDeferred()
        }
        // 同步激活暂停闸门：验证码处理期间暂停后续 RPC 请求
        RpcPauseGate.activate()
        // 启动锚点监测：文案消失（验证码页关闭）即放行闸门，覆盖用户手动完成场景
        CaptchaAnchorWatcher.start()
    }

    /** 最近 [SIGNAL_WINDOW_MS] 内是否命中过验证码 RPC */
    fun isVerifyRpcRecent(): Boolean {
        return System.currentTimeMillis() - lastVerifyRpcAt <= SIGNAL_WINDOW_MS
    }

    /** 距离最近一次命中的毫秒数 */
    fun hitAgeMs(): Long {
        return System.currentTimeMillis() - lastVerifyRpcAt
    }

    /**
     * 事件驱动等待验证码 RPC 命中。
     *
     * 已有近期命中立即返回 true；否则挂起等待 [onVerifyRpcHit]（不占线程），
     * 超时未命中返回 false。用于 handler 入口消除"读取早于 RPC 发出"的竞态。
     */
    suspend fun awaitHit(timeoutMs: Long): Boolean {
        if (isVerifyRpcRecent()) return true
        val deferred = synchronized(signalLock) {
            if (isVerifyRpcRecent()) return true
            hitSignal
        }
        val hit = withTimeoutOrNull(timeoutMs) { deferred.await() } != null
        return hit && isVerifyRpcRecent()
    }
}
