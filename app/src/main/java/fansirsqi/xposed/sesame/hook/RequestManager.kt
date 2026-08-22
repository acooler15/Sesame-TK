package fansirsqi.xposed.sesame.hook

import android.Manifest
import androidx.annotation.RequiresPermission
import fansirsqi.xposed.sesame.entity.RpcEntity
import fansirsqi.xposed.sesame.hook.rpc.bridge.RpcBridge
import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.core.threads.CoroutineUtils
import fansirsqi.xposed.sesame.core.app.NetworkUtils
import fansirsqi.xposed.sesame.core.notify.Notify
import fansirsqi.xposed.sesame.core.util.TimeUtil
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicInteger

/**
 * RPC 请求管理器 (带熔断与兜底机制)
 */
object RequestManager {

    private const val TAG = "RequestManager"

    // 连续失败计数器
    private val errorCount = AtomicInteger(0)

    /**
     * 核心执行函数 (内联优化)
     * 流程：离线检查 -> 获取 Bridge -> 执行请求 -> 结果校验 -> 错误计数/重置
     */
    private suspend inline fun executeRpc(methodLog: String?, data: String?, block: suspend (RpcBridge) -> String?): String {
        // 1. 【前置检查】如果已经离线，直接中断并尝试恢复
        if (ApplicationHook.offline) {
            Log.record(TAG, "当前处于离线状态，拦截请求: $methodLog")
            handleOfflineRecovery()
            return ""
        }

        // 2. 获取 Bridge (包含网络检查)
        // 如果这里获取失败，也视为一次错误
        val bridge = getRpcBridge()
        if (bridge == null) {
            handleFailure("Network/Bridge Unavailable", data, "网络或Bridge不可用")
            return ""
        }

        // 3. 执行请求
        val result = try {
            block(bridge)
        } catch (e: Throwable) {
            Log.printStackTrace(TAG, "RPC 执行异常: $methodLog\n请求体: ${truncateLog(data)}", e)
            null // 异常视为 null，触发失败逻辑
        }

        // 4. 结果校验与状态维护
        if (result.isNullOrBlank()) {
            // 失败：增加计数，检查兜底
            handleFailure(methodLog ?: "Unknown", data, "返回数据为空")
            return ""
        } else {
            // 成功：重置计数器
            if (errorCount.get() > 0) {
                errorCount.set(0)
                Log.record(TAG, "RPC 恢复正常，错误计数重置")
            }
            return result
        }
    }

    /**
     * 处理失败逻辑：计数、报警、熔断
     */
    private fun handleFailure(method: String, data: String?, reason: String) {
        val currentCount = errorCount.incrementAndGet()
        // 从全局配置读取异常次数阈值
        val maxCount = ApplicationHook.config.setMaxErrorCount.value

        Log.error(TAG, "RPC 失败 ($currentCount/$maxCount) | Method: $method | Data: ${truncateLog(data)} | Reason: $reason")

        // 触发兜底阈值
        if (currentCount >= maxCount) {
            Log.record(TAG, "🔴 连续失败次数达到阈值，触发熔断兜底机制！")
            // 1. 设置离线状态，停止后续任务
            ApplicationHook.offline = true
            // 2. 发送通知 (根据用户配置)
            if (ApplicationHook.config.errNotify.value) {
                val msg = "${TimeUtil.getTimeStr()} | 网络异常次数超过阈值[$maxCount]"
                Notify.sendNewNotification(msg, "RPC 连续失败，脚本已暂停")
            }
            // 3. 立即尝试一次恢复
            handleOfflineRecovery()
        }
    }

    /**
     * 日志内容截断，防止超长请求体/响应体刷屏
     */
    private fun truncateLog(s: String?, max: Int = 500): String {
        if (s == null) return "null"
        return if (s.length > max) s.substring(0, max) + "...(len=${s.length}, truncated)" else s
    }

    /**
     * 处理离线恢复逻辑
     * 可以是发送广播、拉起 App 等
     */
    private fun handleOfflineRecovery() {
        // 防止短时间内频繁触发恢复逻辑 (可选)
        // 这里简单实现：尝试拉起支付宝或发送重登录广播

        Log.record(TAG, "正在尝试执行离线恢复策略...")
        // 策略 A: 重新拉起 App (推荐)
        ApplicationHook.reOpenApp()
        // 策略 B: 发送重登录广播 (如果宿主还能响应广播)
        // ApplicationHook.reLoginByBroadcast()
    }

    /**
     * 获取 RpcBridge 实例
     */
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    private suspend fun getRpcBridge(): RpcBridge? {
        if (!NetworkUtils.isNetworkAvailable()) {
            Log.record(TAG, "网络不可用，尝试等待 5秒...")
            delay(5000)  // 替代 sleepCompat(5000)
            if (!NetworkUtils.isNetworkAvailable()) {
                return null
            }
        }

        var bridge = ApplicationHook.rpcBridge
        if (bridge == null) {
            Log.record(TAG, "RpcBridge 未初始化，尝试等待 5秒...")
            delay(5000)  // 替代 sleepCompat(5000)
            bridge = ApplicationHook.rpcBridge
        }

        return bridge
    }

    // ================== 公开 API ==================

    @JvmStatic
    suspend fun requestString(rpcEntity: RpcEntity): String {
        return executeRpc(rpcEntity.methodName, rpcEntity.requestData) { bridge ->
            bridge.requestString(rpcEntity, 3, 1200)
        }
    }

    @JvmStatic
    suspend fun requestString(rpcEntity: RpcEntity, tryCount: Int, retryInterval: Int): String {
        return executeRpc(rpcEntity.methodName, rpcEntity.requestData) { bridge ->
            bridge.requestString(rpcEntity, tryCount, retryInterval)
        }
    }

    @JvmStatic
    suspend fun requestString(method: String?, data: String?): String {
        return executeRpc(method, data) { bridge ->
            bridge.requestString(method, data)
        }
    }

    @JvmStatic
    suspend fun requestString(method: String?, data: String?, relation: String?): String {
        return executeRpc(method, data) { bridge ->
            bridge.requestString(method, data, relation)
        }
    }

    @JvmStatic
    suspend fun requestString(
        method: String?,
        data: String?,
        appName: String?,
        methodName: String?,
        facadeName: String?
    ): String {
        return executeRpc(method, data) { bridge ->
            bridge.requestString(method, data, appName, methodName, facadeName)
        }
    }

    @JvmStatic
    suspend fun requestString(method: String?, data: String?, tryCount: Int, retryInterval: Int): String {
        return executeRpc(method, data) { bridge ->
            bridge.requestString(method, data, tryCount, retryInterval)
        }
    }

    @JvmStatic
    suspend fun requestString(
        method: String?,
        data: String?,
        relation: String?,
        tryCount: Int,
        retryInterval: Int
    ): String {
        return executeRpc(method, data) { bridge ->
            bridge.requestString(method, data, relation, tryCount, retryInterval)
        }
    }

    @JvmStatic
    suspend fun requestObject(rpcEntity: RpcEntity?, tryCount: Int, retryInterval: Int): RpcEntity? {
        if (rpcEntity == null) return null
        // requestObject 不涉及返回值判断，但同样需要离线检查
        if (ApplicationHook.offline) {
            handleOfflineRecovery()
            return null
        }

        val bridge = getRpcBridge()
        if (bridge == null) {
            handleFailure("requestObject", rpcEntity.requestData, "Bridge Unavailable")
            return null
        }

        return try {
            val result = bridge.requestObject(rpcEntity, tryCount, retryInterval)
            errorCount.set(0)
            result  // ← 修复：返回结果而非丢弃
        } catch (e: Throwable) {
            Log.printStackTrace(TAG, "requestObject 异常: ${rpcEntity.methodName}\n请求体: ${truncateLog(rpcEntity.requestData)}", e)
            handleFailure(rpcEntity.methodName ?: "Unknown", rpcEntity.requestData, "Exception")
            null
        }
    }

    // ================== 兼容层（Java/UI 调用方） ==================

    /**
     * 同步兼容入口，仅供无法改造为非协程的 Java/UI 调用方使用
     */
    @JvmStatic
    fun requestStringBlocking(rpcEntity: RpcEntity): String = CoroutineUtils.runBlockingSafe {
        requestString(rpcEntity)
    } ?: ""
}