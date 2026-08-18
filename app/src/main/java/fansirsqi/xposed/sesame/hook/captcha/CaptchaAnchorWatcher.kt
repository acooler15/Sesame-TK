package fansirsqi.xposed.sesame.hook.captcha

import fansirsqi.xposed.sesame.core.log.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 验证码锚点文案监测器。
 *
 * RPC 暂停闸门（[RpcPauseGate]）激活期间，轮询探测验证码提示文案：
 * 文案先出现、后消失（连续确认）即判定验证码页已关闭，放行闸门。
 * 与"谁完成了滑动"完全解耦——handler 自动处理、用户手动滑动、其它方式均覆盖，
 * 解决用户手动滑块后闸门只能干等超时的问题。
 *
 * 探测契约 [BaseCaptchaHandler.isAnchorVisible] 由各 handler 按各自验证码形态实现
 * （职责内聚、演化隔离；处理器实例统一由 [CaptchaHook.captchaHandlers] 持有，
 * 新增 Captcha3Handler 仅需在 CaptchaHook 一处加入）：
 * - 旧版：Captcha1Handler（CaptchaDialog 原生 View 树 XPath）；
 * - 新版：Captcha2Handler（WebView H5 虚拟树无障碍探测）。
 *
 * 生命周期：
 * - 由 [CaptchaRpcSignal.onVerifyRpcHit] 启动（幂等，重复信号忽略）；
 * - 闸门已被其它路径放行/超时关闭（isPaused=false）时自行退出；
 * - 观察期（[APPEAR_WINDOW_MS]）内文案从未出现则放行（非标准流程，大概率已被处理）。
 */
object CaptchaAnchorWatcher {

    private const val TAG = "CaptchaAnchorWatcher"

    /** 轮询间隔（ms） */
    private const val POLL_INTERVAL_MS = 1_000L

    /** 观察期（ms）：启动后该窗口内文案从未出现则视为非标准流程，放行闸门 */
    private const val APPEAR_WINDOW_MS = 10_000L

    /** "消失"连续确认次数：防刷新瞬间虚拟树重建造成的虚灭 */
    private const val GONE_CONFIRM_COUNT = 2

    /** "消失"确认探测间隔（ms），与 Captcha2Handler 的二次确认节奏一致 */
    private const val GONE_CONFIRM_MS = 300L

    /** 监测协程（自包含作用域，生命周期与模块一致） */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var watchJob: Job? = null

    /** 启动监测（幂等：已在运行则忽略） */
    fun start() {
        if (watchJob?.isActive == true) return
        watchJob = scope.launch { watchLoop() }
    }

    private suspend fun watchLoop() {
        val startTime = System.currentTimeMillis()
        var seenOnce = false
        var goneStreak = 0
        Log.record(TAG, "[锚点监测] 启动，观察期=${APPEAR_WINDOW_MS}ms")

        while (true) {
            // 闸门已被其它路径放行/超时关闭，监测使命结束
            if (!RpcPauseGate.isPaused()) {
                Log.record(TAG, "[锚点监测] 闸门已放行，监测退出")
                return
            }

            val visible = try {
                anyAnchorVisible()
            } catch (e: Throwable) {
                Log.record(TAG, "[锚点监测] 探测异常: ${e.message}")
                false // 探测异常按不可见处理，由后续轮询自纠
            }

            if (visible) {
                if (!seenOnce) {
                    seenOnce = true
                    Log.record(TAG, "[锚点监测] 验证码文案已出现，进入消失判定")
                }
                goneStreak = 0
                delay(POLL_INTERVAL_MS)
                continue
            }

            if (seenOnce) {
                goneStreak++
                if (goneStreak >= GONE_CONFIRM_COUNT) {
                    Log.record(TAG, "[锚点监测] 验证码文案已消失（连续${GONE_CONFIRM_COUNT}次确认），验证码页已关闭，耗时=${System.currentTimeMillis() - startTime}ms")
                    RpcPauseGate.onCaptchaHandled("锚点文案消失，验证码页已关闭（含用户手动完成场景）")
                    return
                }
                delay(GONE_CONFIRM_MS)
                continue
            }

            // 文案尚未出现过
            if (System.currentTimeMillis() - startTime >= APPEAR_WINDOW_MS) {
                Log.record(TAG, "[锚点监测] 观察期(${APPEAR_WINDOW_MS}ms)内文案从未出现，判定非标准流程，放行闸门")
                RpcPauseGate.onCaptchaHandled("锚点文案观察期内未出现，判定非验证码流程")
                return
            }
            delay(POLL_INTERVAL_MS)
        }
    }

    /**
     * 任一版验证码提示文案是否在屏（探测契约 [BaseCaptchaHandler.isAnchorVisible] 多态分发，
     * 处理器实例取自 [CaptchaHook.captchaHandlers]，与 PageMonitor 注册共用同一批实例）。
     */
    private suspend fun anyAnchorVisible(): Boolean {
        return CaptchaHook.captchaHandlers.any { it.isAnchorVisible() }
    }
}
