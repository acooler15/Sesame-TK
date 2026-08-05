package fansirsqi.xposed.sesame.hook.rpc.bridge

import fansirsqi.xposed.sesame.data.General
import fansirsqi.xposed.sesame.data.RuntimeInfo
import fansirsqi.xposed.sesame.entity.RpcEntity
import fansirsqi.xposed.sesame.hook.ApplicationHook
import fansirsqi.xposed.sesame.hook.rpc.intervallimit.RpcIntervalLimit
import fansirsqi.xposed.sesame.model.BaseModel
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.Notify
import fansirsqi.xposed.sesame.util.StringUtil
import fansirsqi.xposed.sesame.util.TimeUtil
import org.json.JSONException
import org.json.JSONObject
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.Objects

class OldRpcBridge : RpcBridge {
    private var loader: ClassLoader? = null
    private var h5PageClazz: Class<*>? = null
    private var rpcCallMethod: Method? = null
    private var getResponseMethod: Method? = null
    private var curH5PageImpl: Any? = null

    override fun getVersion(): RpcVersion {
        return RpcVersion.NEW // 返回 RPC 的版本
    }

    /**
     * 加载 RPC 所需的类和方法。
     */
    @Throws(Exception::class)
    override fun load() {
        loader = ApplicationHook.classLoader
        try {
            h5PageClazz = loader!!.loadClass(General.H5PAGE_NAME)
            Log.record(TAG, "RPC 类加载成功")
            loadRpcMethods() // 加载 RPC 方法
        } catch (e: ClassNotFoundException) {
            Log.record(TAG, "加载 RPC 类时出错：")
            Log.printStackTrace(TAG, e)
            throw RuntimeException(e)
        } catch (t: Throwable) {
            Log.record(TAG, "加载 RPC 类时发生意外错误：")
            Log.printStackTrace(TAG, t)
            throw t
        }
    }

    /**
     * 使用反射加载 RPC 方法。
     */
    private fun loadRpcMethods() {
        if (rpcCallMethod == null) {
            try {
                val rpcUtilClass = loader!!.loadClass("com.alipay.mobile.nebulaappproxy.api.rpc.H5RpcUtil")
                val responseClass = loader!!.loadClass("com.alipay.mobile.nebulaappproxy.api.rpc.H5Response")
                rpcCallMethod = rpcUtilClass.getMethod("rpcCall", String::class.java, String::class.java, String::class.java,
                        Boolean::class.javaPrimitiveType, loader!!.loadClass(General.JSON_OBJECT_NAME), String::class.java,
                        Boolean::class.javaPrimitiveType, h5PageClazz, Int::class.javaPrimitiveType, String::class.java, Boolean::class.javaPrimitiveType, Int::class.javaPrimitiveType, String::class.java)
                getResponseMethod = responseClass.getMethod("getResponse")
                Log.record(TAG, "RPC 调用方法加载成功")
            } catch (e: Exception) {
                Log.record(TAG, "加载 RPC 调用方法时出错：")
                Log.printStackTrace(TAG, e)
            }
        }
    }

    override fun unload() {
        getResponseMethod = null // 清空响应方法
        rpcCallMethod = null // 清空调用方法
        h5PageClazz = null // 清空 H5 页面类
        loader = null // 清空类加载器
    }

    /**
     * 向 RPC 实体请求字符串响应。
     *
     * @param rpcEntity     要发送的 RPC 实体。
     * @param tryCount      重试次数。
     * @param retryInterval  重试间隔。
     * @return 响应字符串，如果失败则返回 null。
     */
    override fun requestString(rpcEntity: RpcEntity, tryCount: Int, retryInterval: Int): String? {
        val responseEntity = requestObject(rpcEntity, tryCount, retryInterval)
        return responseEntity?.responseString // 返回响应字符串或 null
    }

    override fun requestObject(rpcEntity: RpcEntity, tryCount: Int, retryInterval: Int): RpcEntity? {
        if (ApplicationHook.offline) {
            return null // 如果离线，直接返回 null
        }
        val id = rpcEntity.hashCode() // 获取请求 ID
        val requestMethod = rpcEntity.requestMethod // 获取请求方法
        val args = rpcEntity.requestData // 获取请求参数
        for (count in 0 until tryCount) {
            try {
                RpcIntervalLimit.enterIntervalLimit(Objects.requireNonNull(requestMethod) as String) // 进入 RPC 调用间隔限制
                val method = requestMethod!! // 非空请求方法
                val response = invokeRpcCall(method, args) // 调用 RPC 方法
                return processResponse(rpcEntity, response, id, method, args, retryInterval) // 处理响应
            } catch (t: Throwable) {
                handleError(rpcEntity, t, requestMethod, id, args) // 处理错误
            }
        }
        return null // 所有尝试失败后返回 null
    }

    /**
     * 使用反射调用 RPC 方法。
     *
     * @param method 请求的方法名。
     * @param args   请求的参数。
     * @return 响应对象。
     * @throws Throwable 如果调用过程中出现错误。
     */
    @Throws(Throwable::class)
    private fun invokeRpcCall(method: String, args: String?): Any? {
        val localRpcCallMethod = rpcCallMethod!!
        return if (localRpcCallMethod.parameterTypes.size == 12) {
            localRpcCallMethod.invoke(null, method, args, "", true, null, null, false, curH5PageImpl, 0, "", false, -1)
        } else {
            localRpcCallMethod.invoke(null, method, args, "", true, null, null, false, curH5PageImpl, 0, "", false, -1, "")
        }
    }

    /**
     * 处理 RPC 响应。
     *
     * @param rpcEntity   要更新的 RPC 实体。
     * @param response    响应对象。
     * @param id          唯一请求 ID。
     * @param method      请求的方法名。
     * @param args        请求的参数。
     * @param retryInterval 重试间隔。
     * @return 更新后的 RPC 实体。
     * @throws Throwable 如果处理过程中出现错误。
     */
    @Throws(Throwable::class)
    private fun processResponse(rpcEntity: RpcEntity, response: Any?, id: Int, method: String, args: String?, retryInterval: Int): RpcEntity? {
        val resultStr = getResponseMethod!!.invoke(response) as String // 获取响应字符串
        val resultObject = JSONObject(resultStr)
        rpcEntity.setResponseObject(resultObject, resultStr) // 设置响应对象
        // 检查响应中的 "memo" 字段是否包含 "系统繁忙"
        if (resultObject.optString("memo", "").contains("系统繁忙")) {
            ApplicationHook.setOffline(true) // 设置为离线状态
            Notify.updateStatusText("系统繁忙，可能需要滑动验证")
            Log.record(TAG, "系统繁忙，可能需要滑动验证")
            return null // 返回 null
        }
        if (!resultObject.optBoolean("success")) {
            rpcEntity.setError() // 设置为错误状态
            Log.error(TAG, "旧 RPC 响应 | id: " + id + " | method: " + method + " args: " + args + " | data: " + rpcEntity.responseString)
        }
        return rpcEntity // 返回更新后的 RPC 实体
    }

    /**
     * 处理 RPC 请求过程中发生的错误。
     *
     * @param rpcEntity 要更新的 RPC 实体。
     * @param t        发生的异常。
     * @param method   请求的方法名。
     * @param id       唯一请求 ID。
     * @param args     请求的参数。
     */
    private fun handleError(rpcEntity: RpcEntity, t: Throwable, method: String?, id: Int, args: String?) {
        rpcEntity.setError() // 设置为错误状态
        Log.error(TAG, "旧 RPC 请求 | id: " + id + " | method: " + method + " err:")
        Log.printStackTrace(t) // 打印堆栈跟踪
        if (t is InvocationTargetException) {
            handleInvocationException(rpcEntity, t, method) // 处理调用异常
        }
    }

    /**
     * 处理调用过程中的特定异常。
     *
     * @param rpcEntity 要更新的 RPC 实体。
     * @param e        发生的 InvocationTargetException。
     * @param method   请求的方法名。
     */
    private fun handleInvocationException(rpcEntity: RpcEntity, e: InvocationTargetException, method: String?) {
        val cause = e.cause
        if (cause != null) {
            val msg = cause.message
            if (!StringUtil.isEmpty(msg)) {
                handleErrorMessage(rpcEntity, msg!!, method) // 处理错误消息
            }
        }
    }

    /**
     * 处理特定的错误消息，并根据内容执行相应的操作。
     *
     * @param rpcEntity 要更新的 RPC 实体。
     * @param msg      错误消息。
     * @param method   请求的方法名。
     */
    private fun handleErrorMessage(rpcEntity: RpcEntity, msg: String, method: String?) {
        if (msg.contains("登录超时")) {
            handleLoginTimeout() // 处理登录超时
        } else if (msg.contains("[1004]") && "alipay.antmember.forest.h5.collectEnergy" == method) {
            handleEnergyCollectException() // 处理能量收集异常
        } else if (msg.contains("MMTPException")) {
            handleException(rpcEntity) // 处理 MMTP 异常
        }
    }

    /**
     * 处理登录超时的情况。
     */
    private fun handleLoginTimeout() {
        if (!ApplicationHook.offline) {
            ApplicationHook.setOffline(true)
            Notify.updateStatusText("登录超时")
            if (BaseModel.timeoutRestart.value) {
                Log.record(TAG, "尝试重新登录")
                ApplicationHook.reLoginByBroadcast()
            }
        }
    }

    /**
     * 处理能量收集异常的情况。
     */
    private fun handleEnergyCollectException() {
        if (BaseModel.waitWhenException.value > 0) {
            val waitTime = System.currentTimeMillis() + BaseModel.waitWhenException.value
            RuntimeInfo.getInstance().put(RuntimeInfo.RuntimeInfoKey.ForestPauseTime, waitTime)
            Notify.updateStatusText("异常")
            Log.record(TAG, "触发异常, 等待至" + TimeUtil.getCommonDate(waitTime))
        }
    }

    /**
     * 处理 MTP 异常的情况。
     *
     * @param rpcEntity 要更新的 RPC 实体。
     */
    private fun handleException(rpcEntity: RpcEntity) {
        try {
            val jsonString: String
            val jo = JSONObject()
            jo.put("resultCode", "FAIL")
            jo.put("memo", "MMTPException")
            jo.put("resultDesc", "MMTPException")
            jsonString = jo.toString()
            rpcEntity.setResponseObject(JSONObject(jsonString), jsonString) // 设置 MMTP 异常响应
        } catch (e: JSONException) {
            Log.printStackTrace(e) // 打印异常信息
        }
    }

    companion object {
        private val TAG = OldRpcBridge::class.java.simpleName
    }
}
