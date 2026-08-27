package fansirsqi.xposed.sesame.service.unlock

import android.content.Context
import android.view.accessibility.AccessibilityNodeInfo
import fansirsqi.xposed.sesame.core.app.DeviceStateChecker
import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.hook.ApplicationHook
import fansirsqi.xposed.sesame.model.UnlockType
import fansirsqi.xposed.sesame.service.ShellManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/** 解锁结果。reason 成功时为 OK/SKIP_ALREADY_UNLOCKED，失败时为原因码 */
data class UnlockResult(val success: Boolean, val reason: String)

/** 锁屏类型（两级检测结果） */
enum class LockType { NONE, PIN, PASSWORD, PATTERN, UNKNOWN }

object UnlockManager {
    private const val TAG = "UnlockManager"

    // 原因码常量集中定义（宿主日志使用，均为无敏感信息的枚举字符串）
    const val FAIL_NO_ACTIVE_USER = "no_active_user"
    const val FAIL_ACCESSIBILITY = "accessibility_disabled"
    const val FAIL_NO_SHELL = "no_privilege_shell"
    const val FAIL_WAKE_TIMEOUT = "wake_timeout"
    const val FAIL_PATTERN = "pattern_not_supported"
    const val FAIL_TYPE_DETECT = "type_detect_failed"
    const val FAIL_AFTER_RETRIES = "unlock_failed_after_retries"

    private const val WAKE_POLL_TIMEOUT_MS = 3000L
    private const val BOUNCER_DISMISS_POLL_MS = 2000L
    private const val TYPE_DETECT_TIMEOUT_MS = 2000L

    /**
     * 模块进程内被 CommandService 调用。context 传 CommandService（用 applicationContext）。
     * 决策链（H4）：无障碍未启用 → FAIL(accessibility_disabled)；Root 优先、Shizuku 次之。
     */
    suspend fun requestUnlock(context: Context, shellManager: ShellManager): UnlockResult {
        Log.record(TAG, ">>> 内置解锁开始（$TAG）")

        // —— 快路径：已亮屏已解锁
        if (DeviceStateChecker.isUnlockedAndAwake(context)) {
            Log.record(TAG, "已亮屏且已解锁，跳过（SKIP_ALREADY_UNLOCKED）")
            return UnlockResult(true, "SKIP_ALREADY_UNLOCKED")
        }
        Log.record(TAG, "设备状态: screenOn=${DeviceStateChecker.isScreenOn(context)}, locked=${DeviceStateChecker.isLocked(context)}, secure=${DeviceStateChecker.isSecure(context)}")

        // —— [0] 决策链（H4）
        if (UnlockAccessibilityService.instance == null) {
            Log.record(TAG, "决策链[0]：无障碍服务实例为空 → FAIL(accessibility_disabled)，不执行任何 shell")
            return UnlockResult(false, FAIL_ACCESSIBILITY)
        }
        Log.record(TAG, "决策链[0]：无障碍服务可用 ✓")
        val probe = shellManager.exec("echo ok") // 触发 selectExecutor：Root 优先，Shizuku 次之
        val hasShell = !(probe.exitCode == -1 && probe.stderr.contains("No valid"))
        Log.record(TAG, "决策链[0]：shell 探测 exitCode=${probe.exitCode}, selected=${shellManager.selectedType}, hasShell=$hasShell")
        if (!hasShell) {
            Log.record(TAG, "决策链[0]：Root/Shizuku 均不可用 → FAIL(no_privilege_shell)")
            return UnlockResult(false, FAIL_NO_SHELL)
        }

        val timeoutSec = ApplicationHook.config.unlockTimeoutSeconds.value
        val retryCount = ApplicationHook.config.unlockRetryCount.value
        Log.record(TAG, "解锁配置: retryCount=$retryCount, timeout=${timeoutSec}s")

        repeat(retryCount) { round ->
            Log.record(TAG, "--- 第 ${round + 1}/$retryCount 轮 ---")
            try {
                // [1] 亮屏：KEYCODE_WAKEUP，轮询验证 isScreenOn
                shellManager.exec("input keyevent 224")
                Log.record(TAG, "[1] 已发送 input keyevent 224(WAKEUP)，等待屏幕点亮...")
                if (!pollCondition(WAKE_POLL_TIMEOUT_MS, 500) { DeviceStateChecker.isScreenOn(context) }) {
                    Log.record(TAG, "[1] 屏幕未点亮（超时 ${WAKE_POLL_TIMEOUT_MS}ms）→ FAIL(wake_timeout)")
                    return UnlockResult(false, FAIL_WAKE_TIMEOUT)
                }
                Log.record(TAG, "[1] 屏幕已点亮 ✓")

                // [2] 保持常亮（H3：finally 恢复）
                shellManager.exec("svc power stayon true")
                Log.record(TAG, "[2] 已执行 svc power stayon true")
                try {
                    // [3] 唤出密码页
                    val dismissed = dismissKeyguardOrSwipe(context, shellManager)
                    Log.record(TAG, if (dismissed) "[3] keyguard 已消失（bouncer 关闭或无安全锁）" else "[3] keyguard 仍锁定（已上滑兜底，等待 bouncer）")

                    // [3.5] 锁屏类型判定
                    val type = detectLockType(context)
                    Log.record(TAG, "[3.5] 锁屏类型判定结果: $type")

                    // [4] 输入密码
                    var inputOk = false
                    when (type) {
                        LockType.NONE -> {
                            Log.record(TAG, "[4] 无安全锁，dismiss 后即已解锁，跳过输密码")
                            inputOk = true
                        }
                        LockType.PIN -> {
                            Log.record(TAG, "[4] 按 PIN 解锁")
                            inputOk = inputPin(context, shellManager, ApplicationHook.config.unlockCredential.value)
                            Log.record(TAG, "[4] PIN 输入结果: $inputOk")
                        }
                        LockType.PASSWORD -> {
                            Log.record(TAG, "[4] 按混合密码解锁")
                            inputOk = inputPassword(context, shellManager, ApplicationHook.config.unlockCredential.value)
                            Log.record(TAG, "[4] 密码输入结果: $inputOk")
                        }
                        LockType.PATTERN -> {
                            Log.record(TAG, "[4] 检测到图案锁 → FAIL(pattern_not_supported)（H6 不盲试，建议改用 PIN）")
                            return UnlockResult(false, FAIL_PATTERN)
                        }
                        LockType.UNKNOWN -> {
                            Log.record(TAG, "[4] 锁屏类型无法判定 → FAIL(type_detect_failed)（可手动设置 unlockType 覆盖）")
                            return UnlockResult(false, FAIL_TYPE_DETECT)
                        }
                    }

                    // [5] 轮询验证
                    if (inputOk && pollCondition(timeoutSec * 1000L, 500) { DeviceStateChecker.isUnlockedAndAwake(context) }) {
                        Log.record(TAG, "[5] 解锁成功（OK）")
                        return UnlockResult(true, "OK")
                    }
                    Log.record(TAG, "[5] 本轮验证超时（${timeoutSec}s）仍未解锁")
                } finally {
                    runCatching { shellManager.exec("svc power stayon false") } // H3：必须恢复常亮
                    Log.record(TAG, "[2] 已恢复 svc power stayon false")
                }

                // 重试前回锁屏 + 轮间隔 1s
                if (round < retryCount - 1) {
                    shellManager.exec("input keyevent 223") // KEYCODE_SLEEP
                    Log.record(TAG, "已发送 input keyevent 223(SLEEP) 回锁屏，1s 后重试")
                    delay(1000)
                }
            } catch (e: Exception) {
                // H2：异常信息不拼入密码内容
                Log.printStackTrace(TAG, "第 ${round + 1} 轮执行异常", e)
            }
        }
        Log.record(TAG, "重试 ${retryCount} 轮仍未解锁 → FAIL(unlock_failed_after_retries)")
        return UnlockResult(false, FAIL_AFTER_RETRIES)
    }

    /**
     * [3] wm dismiss-keyguard；2s 后仍 locked 则上滑兜底（无障碍手势优先，input swipe 备用，起点位置轮换）。
     * @return true = keyguard 已解除（或无安全锁），false = 仍锁定
     */
    private suspend fun dismissKeyguardOrSwipe(context: Context, shellManager: ShellManager): Boolean {
        Log.record(TAG, "[3] 执行 wm dismiss-keyguard")
        shellManager.exec("wm dismiss-keyguard")
        if (!pollCondition(BOUNCER_DISMISS_POLL_MS, 500) { !DeviceStateChecker.isLocked(context) }) {
            Log.record(TAG, "[3] dismiss-keyguard 后 2s 仍锁定（可能为安全锁或 ROM 不支持），执行上滑兜底")
            swipeUp(context, shellManager)
        }
        return !DeviceStateChecker.isLocked(context)
    }

    /** 上滑：位置轮换 0.96h - 0.15h*i，时长 115~300ms */
    private suspend fun swipeUp(context: Context, shellManager: ShellManager, attempt: Int = 0) {
        Log.record(TAG, "[3] 上滑兜底 attempt=$attempt（无障碍手势优先）")
        val ok = UnlockAccessibilityService.dispatchSwipeUp(attempt)
        if (!ok) {
            Log.record(TAG, "[3] 无障碍手势不可用，改用 input swipe")
            val metrics = context.resources.displayMetrics
            val y1 = metrics.heightPixels * (0.96f - 0.15f * attempt)
            val y2 = metrics.heightPixels * 0.3f
            val x = metrics.widthPixels / 2f
            shellManager.exec("input swipe ${x.toInt()} ${y1.toInt()} ${x.toInt()} ${y2.toInt()} 200")
        } else {
            Log.record(TAG, "[3] 无障碍上滑手势已分发")
        }
    }

    /** 锁屏类型两级检测：系统 API → 无障碍节点树 → 手动配置回退 */
    private suspend fun detectLockType(context: Context): LockType {
        // 第一级：系统 API（可靠）
        if (!DeviceStateChecker.isSecure(context)) {
            Log.record(TAG, "[3.5] isSecure=false → LockType.NONE（无密码/滑动锁）")
            return LockType.NONE // 无密码/滑动锁
        }
        Log.record(TAG, "[3.5] isSecure=true，进入第二级节点树检测")

        // 第二级：无障碍节点树（需在 [3] 唤出 bouncer 后调用）
        val manual = ApplicationHook.config.unlockType.value // UnlockType：0=自动 1=PIN 2=PASSWORD
        val detected = withTimeoutOrNull(TYPE_DETECT_TIMEOUT_MS) {
            val svc = UnlockAccessibilityService.instance ?: run {
                Log.record(TAG, "[3.5] 无障碍实例为空（节点树检测跳过）")
                return@withTimeoutOrNull null
            }
            val root = svc.rootInActiveWindow ?: run {
                Log.record(TAG, "[3.5] rootInActiveWindow 为空（当前无活动窗口）")
                return@withTimeoutOrNull null
            }
            when {
                hasQwertyKeys(root) -> {
                    Log.record(TAG, "[3.5] 节点树命中字母/空格/回车键 → PASSWORD")
                    LockType.PASSWORD
                }
                hasDigitKeys(root) -> {
                    Log.record(TAG, "[3.5] 节点树命中数字键 → PIN")
                    LockType.PIN
                }
                else -> {
                    Log.record(TAG, "[3.5] 节点树未见字母/数字键 → PATTERN（九宫格）")
                    LockType.PATTERN
                }
            }
        }
        // 检测超时/服务不可用 → 回退手动配置；手动=自动仍未知 → FAIL
        if (detected != null) return detected
        Log.record(TAG, "[3.5] 节点检测未得到结果（超时或窗口缺失），回退手动配置 unlockType=$manual")
        return when (manual) {
            UnlockType.PIN -> LockType.PIN
            UnlockType.PASSWORD -> LockType.PASSWORD
            else -> LockType.UNKNOWN // → FAIL(type_detect_failed)
        }
    }

    private fun hasDigitKeys(root: AccessibilityNodeInfo): Boolean {
        for (d in '0'..'9') {
            if (root.findAccessibilityNodeInfosByText(d.toString()).any { it.isClickable }) return true
        }
        return false
    }

    private fun hasQwertyKeys(root: AccessibilityNodeInfo): Boolean {
        for (t in listOf("q", "a", "空格", "回车")) {
            if (root.findAccessibilityNodeInfosByText(t).any { it.isClickable }) return true
        }
        return false
    }

    /** PIN 三层输入策略：①节点点击 → ②keyevent 逐位 → ③3×4 网格 tap 兜底 */
    private suspend fun inputPin(context: Context, shellManager: ShellManager, digits: String?): Boolean {
        val pin = digits ?: ""
        if (pin.isEmpty()) {
            Log.record(TAG, "[4][PIN] unlockCredential 为空，视为无输入（直接验证）")
            return unlockedSoon(context)
        }
        // ① 无障碍节点点击（主通道）
        Log.record(TAG, "[4][PIN] 第①层：无障碍节点逐位点击（${pin.length} 位）")
        if (UnlockAccessibilityService.inputPinByNodes(pin) && unlockedSoon(context)) {
            Log.record(TAG, "[4][PIN] 第①层成功")
            return true
        }
        Log.record(TAG, "[4][PIN] 第①层失败，进入第②层 keyevent")
        // ② keyevent（KEYCODE_0=7 … KEYCODE_9=16）
        pin.forEachIndexed { i, ch ->
            shellManager.exec("input keyevent ${ch.digitToInt() + 7}")
            Log.record(TAG, "[4][PIN] 第②层已输入第 ${i + 1} 位")
            delay((150L..300L).random())
        }
        if (unlockedSoon(context)) {
            Log.record(TAG, "[4][PIN] 第②层成功")
            return true
        }
        Log.record(TAG, "[4][PIN] 第②层失败，进入第③层网格 tap")
        // ③ 网格 tap（O=解锁轮次无关，attempt 传 0 即可）
        val m = context.resources.displayMetrics
        pin.forEachIndexed { i, ch ->
            val d = ch.digitToInt()
            val row = if (d == 0) 3 else (d - 1) / 3
            val col = if (d == 0) 1 else (d - 1) % 3
            val x = (m.widthPixels * (col + 0.5f) / 3f).toInt()
            val y = (m.heightPixels * (0.44f + row * 0.115f)).toInt()
            shellManager.exec("input tap $x $y")
            Log.record(TAG, "[4][PIN] 第③层已输入第 ${i + 1} 位")
            delay((150L..300L).random())
        }
        val ok = unlockedSoon(context)
        Log.record(TAG, "[4][PIN] 第③层结果: $ok")
        return ok
    }

    /** 混合密码：无障碍 focus + ACTION_SET_TEXT → 失败回退 → shell input text + keyevent 66 */
    private suspend fun inputPassword(context: Context, shellManager: ShellManager, pwd: String?): Boolean {
        val password = pwd ?: ""
        if (password.isEmpty()) {
            Log.record(TAG, "[4][PWD] unlockCredential 为空，视为无输入（直接验证）")
            return unlockedSoon(context)
        }
        // 无障碍通道
        Log.record(TAG, "[4][PWD] 无障碍通道：focus + ACTION_SET_TEXT")
        if (UnlockAccessibilityService.inputPasswordByText(password) && unlockedSoon(context)) {
            Log.record(TAG, "[4][PWD] 无障碍通道成功")
            return true
        }
        Log.record(TAG, "[4][PWD] 无障碍通道失败，回退 shell input text")
        // shell 通道（H2：日志不输出密码内容）
        shellManager.exec("input text ${shellEscape(password)}")
        shellManager.exec("input keyevent 66") // KEYCODE_ENTER
        Log.record(TAG, "[4][PWD] 密码已通过 shell 输入完成（input text ***，日志脱敏）")
        val ok = unlockedSoon(context)
        Log.record(TAG, "[4][PWD] shell 通道结果: $ok")
        return ok
    }

    /** shell 单参数转义：双引号包裹 + 转义 `\ " $ ` 与换行 */
    private fun shellEscape(s: String): String =
        "\"" + s
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("$", "\\$")
            .replace("`", "\\`")
            .replace("\r", "")
            .replace("\n", " ") + "\""

    /** 输完后轮询 keyguard 解锁（500ms 间隔，至 unlockTimeoutSeconds） */
    private suspend fun unlockedSoon(context: Context): Boolean {
        val timeoutMs = ApplicationHook.config.unlockTimeoutSeconds.value * 1000L
        val ok = pollCondition(timeoutMs, 500) { !DeviceStateChecker.isLocked(context) }
        Log.record(TAG, "输密码后轮询（${timeoutMs}ms）结果: $ok")
        return ok
    }

    /** 通用轮询 */
    private suspend fun pollCondition(timeoutMs: Long, intervalMs: Long = 500, cond: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (cond()) return true
            delay(intervalMs)
        }
        return cond()
    }
}
