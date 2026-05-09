package fansirsqi.xposed.sesame.hook.simple

import fansirsqi.xposed.sesame.util.CommandUtil
import fansirsqi.xposed.sesame.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * 系统级滑动注入工具。
 * 优先通过 CommandService -> ShellManager -> Root/Shizuku 执行 `input swipe`。
 * 若 CommandService 不可用，直接尝试 Runtime.exec("su -c input swipe") 作为后备。
 */
object SystemInputSwiper {

    private const val TAG = "SystemInputSwiper"
    private const val SHELL_EXEC_TIMEOUT_MS = 10000L

    /**
     * 使用系统级 `input swipe` 命令执行滑动。
     * 策略：CommandService 优先 -> 直接 su 执行 -> 失败返回 false。
     *
     * @return true 表示 shell 命令执行成功，false 表示所有路径均失败
     */
    suspend fun swipe(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        durationMs: Long
    ): Boolean {
        val context = SimplePageManager.getContext()
        if (context == null) {
            Log.record(TAG, "[系统级输入] 无法获取 Context，跳过")
            return false
        }

        val x1 = startX.toInt()
        val y1 = startY.toInt()
        val x2 = endX.toInt()
        val y2 = endY.toInt()
        val cmd = "input swipe $x1 $y1 $x2 $y2 $durationMs"

        // 路径1: 通过 CommandService (AIDL -> ShellManager -> Root/Shizuku)
        // CommandUtil.executeCommand 内部会自动 ensureServiceBound
        Log.record(TAG, "[系统级输入] 尝试路径1: CommandService -> $cmd")
        val commandResult = tryCommandService(context, cmd)
        if (commandResult) {
            Log.record(TAG, "[系统级输入] 路径1(CommandService) 成功")
            return true
        }
        Log.record(TAG, "[系统级输入] 路径1(CommandService) 失败，尝试路径2")

        // 路径2: 直接 Runtime.exec("su -c input swipe")
        Log.record(TAG, "[系统级输入] 尝试路径2: 直接 su -> $cmd")
        val directResult = tryDirectSuExec(cmd)
        if (directResult) {
            Log.record(TAG, "[系统级输入] 路径2(直接su) 成功")
            return true
        }

        Log.record(TAG, "[系统级输入] 所有系统级输入路径均失败")
        return false
    }

    /**
     * 通过 CommandService (AIDL) 执行命令。
     * 不检查 serviceStatus，因为 executeCommand 内部会自动绑定服务。
     */
    private suspend fun tryCommandService(context: android.content.Context, cmd: String): Boolean {
        return try {
            val result = CommandUtil.executeCommand(context, cmd)
            if (result != null) {
                Log.record(TAG, "[系统级输入] CommandService 返回: '${result.trim()}'")
                true
            } else {
                Log.record(TAG, "[系统级输入] CommandService 返回 null（服务未绑定或命令失败）")
                false
            }
        } catch (e: Exception) {
            Log.record(TAG, "[系统级输入] CommandService 异常: ${e.message}")
            false
        }
    }

    /**
     * 直接通过 Runtime.exec 执行 su -c 命令（不依赖 CommandService）
     */
    private suspend fun tryDirectSuExec(cmd: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                withTimeout(SHELL_EXEC_TIMEOUT_MS) {
                    val process = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
                    val stdout = process.inputStream.bufferedReader().use { it.readText() }.trim()
                    val stderr = process.errorStream.bufferedReader().use { it.readText() }.trim()
                    val exitCode = process.waitFor()

                    Log.record(TAG, "[系统级输入] 直接su结果: exitCode=$exitCode, stdout='$stdout', stderr='$stderr'")
                    exitCode == 0
                }
            } catch (e: Exception) {
                Log.record(TAG, "[系统级输入] 直接su异常: ${e.message}")
                false
            }
        }
    }
}
