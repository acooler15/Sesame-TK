package fansirsqi.xposed.sesame.service

import android.content.pm.PackageManager
import android.util.Log
import rikka.shizuku.Shizuku

/**
 * Shizuku 就绪检测工具，统一检测逻辑，避免各处重复实现。
 */
object ShizukuChecker {

    private const val TAG = "ShizukuChecker"

    /**
     * Shizuku 是否已就绪（binder 存活且已授权）。
     */
    fun isReady(): Boolean {
        return try {
            val isBinderAlive = Shizuku.pingBinder()
            val hasPermission = if (isBinderAlive) {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            } else {
                false
            }
            Log.d(TAG, "isBinderAlive: $isBinderAlive, hasPermission: $hasPermission, PID: ${android.os.Process.myPid()}")
            isBinderAlive && hasPermission
        } catch (e: Exception) {
            Log.e(TAG, "isReady", e)
            false
        }
    }
}
