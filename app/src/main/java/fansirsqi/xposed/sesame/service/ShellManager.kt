package fansirsqi.xposed.sesame.service

import android.content.Context
import android.util.Log
import com.niki.cmd.Shell
import com.niki.cmd.ShizukuShell
import com.niki.cmd.model.bean.ShellResult
import fansirsqi.xposed.sesame.service.patch.SafeRootShell

class ShellManager(context: Context) {

    companion object {
        private const val TAG = "ShellManager"
    }

    var onStateChanged: ((ShellType) -> Unit)? = null

    // 只保留特权 Shell（Root 优先，Shizuku 次之）
    private val executors = listOf(
        SafeRootShell(),
        ShizukuShell(context)
    )

    // 使用 Volatile 确保多线程下的可见性
    @Volatile
    private var selectedShell: Shell? = null

    /**
     * 当前选中的 Shell 类型（未选中时为 NONE）。
     */
    val selectedType: ShellType
        get() = selectedShell?.toShellType() ?: ShellType.NONE

    /**
     * 将具体 Shell 实现显式映射为类型枚举。
     * 替代原先依赖 javaClass.simpleName 的脆弱反射取名。
     */
    private fun Shell.toShellType(): ShellType = when (this) {
        is SafeRootShell -> ShellType.ROOT
        is ShizukuShell -> ShellType.SHIZUKU
        else -> ShellType.NONE
    }

    private fun notifyChange() {
        val currentType = selectedType
        Log.d(TAG, "Shell状态变更 -> $currentType")
        onStateChanged?.invoke(currentType)
    }

    /**
     * 强制重置选择状态（例如 Shizuku 授权后）。
     */
    fun reset() {
        selectedShell = null
        Log.d(TAG, "ShellManager 已重置，下次执行将重新选择 Executor")
        notifyChange()
    }

    private suspend fun selectExecutor() {
        // 如果已经选中且可用，直接返回
        if (selectedShell != null && selectedShell!!.isAvailable()) return

        Log.d(TAG, "正在寻找可用的 Root 或 Shizuku Shell...")

        for (shell in executors) {
            try {
                // 针对 Shizuku 做特殊检查，防止未授权时报错或假死
                if (shell is ShizukuShell) {
                    if (!ShizukuChecker.isReady()) {
                        Log.d(TAG, "跳过 ShizukuShell: 未授权或服务未运行")
                        continue
                    }
                }

                if (shell.isAvailable()) {
                    selectedShell = shell
                    notifyChange()
                    Log.i(TAG, "✅ 成功选中 Shell: ${shell.toShellType()}")
                    return
                }
            } catch (e: Exception) {
                Log.w(TAG, "Shell ${shell.toShellType()} 检测失败: ${e.message}")
            }
        }
        // 如果都失败了，置空
        selectedShell = null
        notifyChange()
    }

    /**
     * 执行命令
     */
    suspend fun exec(command: String): ShellResult {
        selectExecutor()
        val shell = selectedShell ?: return ShellResult("", "No valid Root/Shizuku shell found.", -1)
        Log.d(TAG, "执行命令: $command (via $selectedType)")
        return shell.exec(command, 5_000L)
    }
}
