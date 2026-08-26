package fansirsqi.xposed.sesame.task.antForest

import android.annotation.SuppressLint
import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.task.antForest.waiting.BubbleOutcome
import fansirsqi.xposed.sesame.task.antForest.waiting.WaitingBatchResult
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

/**
 * 用户能量收取模式数据类
 * 用于分析用户的能量收取习惯，但不影响蹲点时机
 */
data class UserEnergyPattern(
    val userId: String,
    val collectSuccessRate: Double = 0.8, // 收取成功率
    val avgResponseTime: Long = 1000L,    // 平均响应时间
    val lastCollectTime: Long = 0L,       // 上次收取时间
    val isActiveUser: Boolean = true      // 是否活跃用户
)

/**
 * 用户能量模式管理器
 * 单一职责：管理用户的能量收取模式和统计数据
 */
object UserEnergyPatternManager {
    private const val TAG = "UserEnergyPatternManager"

    // === 内部调优常量（就近收敛到本类，见优化方案 3.6） ===
    private const val ALPHA = 0.1                        // EMA 平滑系数（成功率）
    private const val AVG_RESPONSE_OLD_WEIGHT = 0.8      // 平均响应时间旧值权重
    private const val AVG_RESPONSE_NEW_WEIGHT = 0.2      // 平均响应时间新值权重
    private const val ACTIVE_WINDOW_MS = 24 * 60 * 60 * 1000L  // 24h 内有活动视为活跃
    private const val CLEANUP_AGE_MS = 30L * 24 * 60 * 60 * 1000 // 30 天无活动清理
    private const val LOG_CHANGE_THRESHOLD = 0.05        // 成功率显著变化阈值（仅显著变化时输出日志，降噪）

    // 用户模式存储
    private val userPatterns = ConcurrentHashMap<String, UserEnergyPattern>()

    /**
     * 获取用户模式
     */
    fun getUserPattern(userId: String): UserEnergyPattern {
        return userPatterns[userId] ?: UserEnergyPattern(userId)
    }

    /**
     * 更新用户模式（基于批量收取结果，V2 §3.3.5）
     * 每个好友 RPC 更新一次，不按 taskId 重复加权。
     */
    @SuppressLint("DefaultLocale")
    fun updateUserPattern(userId: String, result: WaitingBatchResult, responseTime: Long) {
        val currentPattern = getUserPattern(userId)
        val currentTime = System.currentTimeMillis()

        // 使用指数移动平均更新成功率
        val isSuccess = when (result) {
            is WaitingBatchResult.Completed -> result.outcomes.values.any { it is BubbleOutcome.Collected }
            is WaitingBatchResult.RequestFailed -> false
        }
        val newSuccessRate = if (isSuccess) {
            currentPattern.collectSuccessRate * (1 - ALPHA) + ALPHA
        } else {
            currentPattern.collectSuccessRate * (1 - ALPHA)
        }

        // 更新平均响应时间
        val newAvgResponseTime = if (responseTime > 0) {
            (currentPattern.avgResponseTime * AVG_RESPONSE_OLD_WEIGHT + responseTime * AVG_RESPONSE_NEW_WEIGHT).toLong()
        } else {
            currentPattern.avgResponseTime
        }

        // 判断用户活跃度（24小时内有活动）
        val timeSinceLastCollect = currentTime - currentPattern.lastCollectTime
        val isActive = timeSinceLastCollect < ACTIVE_WINDOW_MS

        val updatedPattern = currentPattern.copy(
            collectSuccessRate = newSuccessRate,
            avgResponseTime = newAvgResponseTime,
            lastCollectTime = if (isSuccess) currentTime else currentPattern.lastCollectTime,
            isActiveUser = isActive
        )

        userPatterns[userId] = updatedPattern

        // 日志降噪（见优化方案 5）：每次蹲点都输出一行太吵，
        // 仅在成功率显著变化（|Δ| > LOG_CHANGE_THRESHOLD）时 Log.record，否则降级为 Log.debug
        val message = "更新用户[$userId]模式：成功率[${String.format("%.2f", newSuccessRate)}] 响应时间[${newAvgResponseTime}ms] 活跃[$isActive]"
        if (abs(newSuccessRate - currentPattern.collectSuccessRate) > LOG_CHANGE_THRESHOLD) {
            Log.record(TAG, message)
        } else {
            Log.debug(TAG, message)
        }
    }

    /**
     * 清理过期的用户模式数据
     */
    fun cleanupExpiredPatterns() {
        val currentTime = System.currentTimeMillis()

        val expiredUsers = userPatterns.filter { (_, pattern) ->
            currentTime - pattern.lastCollectTime > CLEANUP_AGE_MS
        }.keys

        expiredUsers.forEach { userId ->
            userPatterns.remove(userId)
        }

        if (expiredUsers.isNotEmpty()) {
            Log.record(TAG, "清理过期用户模式数据：${expiredUsers.size}个用户")
        }
    }
}