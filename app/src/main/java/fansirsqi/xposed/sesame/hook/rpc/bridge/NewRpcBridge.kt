package fansirsqi.xposed.sesame.hook.rpc.bridge

import de.robv.android.xposed.XposedHelpers
import fansirsqi.xposed.sesame.data.General
import fansirsqi.xposed.sesame.entity.RpcEntity
import fansirsqi.xposed.sesame.hook.ApplicationHook
import fansirsqi.xposed.sesame.hook.Toast
import fansirsqi.xposed.sesame.hook.rpc.intervallimit.RpcIntervalLimit
import fansirsqi.xposed.sesame.model.BaseModel
import fansirsqi.xposed.sesame.core.threads.CoroutineUtils
import fansirsqi.xposed.sesame.core.threads.GlobalThreadPools
import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.util.Notify
import fansirsqi.xposed.sesame.core.util.RandomUtil
import fansirsqi.xposed.sesame.util.SwipeUtil
import fansirsqi.xposed.sesame.core.util.TimeUtil
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicInteger

/**
 * 新版rpc接口，支持最低目标应用版本v10.3.96.8100 记录rpc抓包，支持最低目标应用版本v10.3.96.8100
 */
class NewRpcBridge : RpcBridge {
    private var loader: ClassLoader? = null
    private var newRpcInstance: Any? = null
    private var parseObjectMethod: Method? = null
    private var bridgeCallbackClazzArray: Array<Class<*>>? = null
    private var newRpcCallMethod: Method? = null
    private val maxErrorCount = AtomicInteger(0)
    private val setMaxErrorCount = BaseModel.setMaxErrorCount.value

    private val errorMark = ArrayList(listOf(
            "1004", "1009", "2000", "46", "48"
    ))
    private val errorStringMark = ArrayList(listOf(
            "繁忙", "拒绝", "网络不可用", "重试"
    ))

    // 需要屏蔽错误日志的RPC方法列表
    private val silentErrorMethods = ArrayList(listOf(
            "com.alipay.adexchange.ad.facade.xlightPlugin",  //木兰集市 第一次
            "alipay.antforest.forest.h5.takeLook"  //找能量
    ))

    /**
     * 检查指定的RPC方法是否应该显示错误日志
     *
     * @param methodName RPC方法名称
     * @return 如果应该显示错误日志返回true，否则返回false
     */
    private fun shouldShowErrorLog(methodName: String?): Boolean {
        return methodName != null && !silentErrorMethods.contains(methodName)
    }

    /**
     * 记录RPC请求返回null的原因
     *
     * @param rpcEntity RPC请求实体
     * @param reason    返回null的原因
     * @param count     当前重试次数
     */
    private fun logNullResponse(rpcEntity: RpcEntity?, reason: String?, count: Int) {
        val methodName = if (rpcEntity != null) rpcEntity.requestMethod else "unknown"
        if (shouldShowErrorLog(methodName)) {
            Log.error(TAG, "RPC返回null | 方法: $methodName | 原因: $reason | 重试: $count")
        }
    }

    @Deprecated("rpcVersion 死字段已清理，版本信息暂无消费方")
    override fun getVersion(): RpcVersion {
        return RpcVersion.NEW
    }

    @Throws(Exception::class)
    override fun load() {
        loader = ApplicationHook.classLoader
        try {
            val service = XposedHelpers.callStaticMethod(XposedHelpers.findClass("com.alipay.mobile.nebulacore.Nebula", loader), "getService")
            val extensionManager = XposedHelpers.callMethod(service, "getExtensionManager")
            val getExtensionByName = extensionManager!!.javaClass.getDeclaredMethod("createExtensionInstance", Class::class.java)
            getExtensionByName.isAccessible = true
            newRpcInstance = getExtensionByName.invoke(null, loader!!.loadClass("com.alibaba.ariver.commonability.network.rpc.RpcBridgeExtension"))
            if (newRpcInstance == null) {
                val nodeExtensionMap = XposedHelpers.callMethod(extensionManager, "getNodeExtensionMap")
                if (nodeExtensionMap != null) {
                    @Suppress("UNCHECKED_CAST")
                    val map = nodeExtensionMap as Map<Any?, Map<String, Any?>>
                    for (entry in map.entries) {
                        val map1 = entry.value
                        for (entry1 in map1.entries) {
                            if ("com.alibaba.ariver.commonability.network.rpc.RpcBridgeExtension" == entry1.key) {
                                newRpcInstance = entry1.value
                                break
                            }
                        }
                    }
                }
                if (newRpcInstance == null) {
                    Log.record(TAG, "get newRpcInstance null")
                    throw RuntimeException("get newRpcInstance is null")
                }
            }
            parseObjectMethod = loader!!.loadClass("com.alibaba.fastjson.JSON").getMethod("parseObject", String::class.java)
            val bridgeCallbackClazz = loader!!.loadClass("com.alibaba.ariver.engine.api.bridge.extension.BridgeCallback")
            bridgeCallbackClazzArray = arrayOf(bridgeCallbackClazz)
            newRpcCallMethod = newRpcInstance!!.javaClass.getMethod("rpc"
                    , String::class.java
                    , Boolean::class.javaPrimitiveType
                    , Boolean::class.javaPrimitiveType
                    , String::class.java
                    , loader!!.loadClass(General.JSON_OBJECT_NAME)
                    , String::class.java
                    , loader!!.loadClass(General.JSON_OBJECT_NAME)
                    , Boolean::class.javaPrimitiveType
                    , Boolean::class.javaPrimitiveType
                    , Int::class.javaPrimitiveType
                    , Boolean::class.javaPrimitiveType
                    , String::class.java
                    , loader!!.loadClass("com.alibaba.ariver.app.api.App")
                    , loader!!.loadClass("com.alibaba.ariver.app.api.Page")
                    , loader!!.loadClass("com.alibaba.ariver.engine.api.bridge.model.ApiContext")
                    , bridgeCallbackClazz
            )
            Log.record(TAG, "get newRpcCallMethod successfully")
        } catch (e: Exception) {
            Log.record(TAG, "get newRpcCallMethod err:")
            throw e
        }
    }

    override fun unload() {
        newRpcCallMethod = null
        bridgeCallbackClazzArray = null
        parseObjectMethod = null
        newRpcInstance = null
        loader = null
    }

    /**
     * 发送RPC请求并获取响应字符串
     *
     * 该方法是requestObject的包装，将RPC响应对象转换为字符串返回：
     * 1. 调用requestObject执行实际的RPC请求
     * 2. 从返回的RPC实体中提取响应字符串
     *
     * @param rpcEntity RPC请求实体，包含请求方法、参数等信息
     * @param tryCount 最大尝试次数，设置为1表示只尝试一次不重试，设置为0表示不尝试，大于1表示有重试
     * @param retryInterval 重试间隔（毫秒），负值表示使用默认延迟，0表示立即重试
     * @return 响应字符串，如果请求失败则返回null
     */
    override fun requestString(rpcEntity: RpcEntity, tryCount: Int, retryInterval: Int): String? {
        val resRpcEntity = requestObject(rpcEntity, tryCount, retryInterval)
        if (resRpcEntity != null) {
            return resRpcEntity.responseString
        }
        return null
    }

    /**
     * 发送RPC请求并获取响应对象
     *
     * 该方法负责执行实际的RPC调用，支持重试机制和错误处理：
     * 1. 根据tryCount参数控制重试次数
     * 2. 根据retryInterval参数控制重试间隔
     *    - retryInterval < 0: 使用600ms+随机延迟
     *    - retryInterval = 0: 不等待立即重试
     *    - retryInterval > 0: 使用指定的毫秒数等待
     * 3. 检测网络错误并根据配置进入离线模式或尝试重新登录
     *
     * @param rpcEntity RPC请求实体，包含请求方法、参数等信息
     * @param tryCount 最大尝试次数，设置为1表示只尝试一次不重试，设置为0表示不尝试，大于1表示有重试
     * @param retryInterval 重试间隔（毫秒），负值表示使用默认延迟，0表示立即重试
     * @return 包含响应数据的RPC实体，如果请求失败则返回null
     */
    override fun requestObject(rpcEntity: RpcEntity, tryCount: Int, retryInterval: Int): RpcEntity? {
        // 方法开始时，将成员变量赋值给局部变量，以避免在方法执行期间因其他线程的unload()调用而导致成员变量变为null
        var localNewRpcCallMethod = newRpcCallMethod
        var localParseObjectMethod = parseObjectMethod
        var localNewRpcInstance = newRpcInstance
        var localLoader = loader
        var localBridgeCallbackClazzArray = bridgeCallbackClazzArray

        if (ApplicationHook.offline) {
            return null
        }

        // 如果RPC组件未准备好，尝试重新初始化一次
        if (localNewRpcCallMethod == null) {
             Log.record(TAG, "RPC方法为null，尝试重新初始化...")
            try {
                load()
                // 重新加载初始化后的变量
                localNewRpcCallMethod = newRpcCallMethod
                localParseObjectMethod = parseObjectMethod
                localNewRpcInstance = newRpcInstance
                localLoader = loader
                localBridgeCallbackClazzArray = bridgeCallbackClazzArray
                 Log.record(TAG, "RPC重新初始化成功")
            } catch (e: Exception) {
                Log.error(TAG, "RPC重新初始化失败:")
                Log.printStackTrace(e)
                logNullResponse(rpcEntity, "RPC组件初始化失败", 0)
                return null
            }
        }

        if (localNewRpcCallMethod == null || localParseObjectMethod == null
                || localNewRpcInstance == null || localLoader == null || localBridgeCallbackClazzArray == null) {
            logNullResponse(rpcEntity, "RPC组件不完整", 0)
            return null
        }
        try {
            var count = 0
            do {
                count++
                try {
                    RpcIntervalLimit.enterIntervalLimit(rpcEntity.requestMethod!!)
                    val finalLocalBridgeCallbackClazzArray = localBridgeCallbackClazzArray
                    localNewRpcCallMethod.invoke(
                            localNewRpcInstance, rpcEntity.requestMethod, false, false, "json", localParseObjectMethod.invoke(null,
                                    rpcEntity.rpcFullRequestData), "", null, true, false, 0, false, "", null, null, null, Proxy.newProxyInstance(localLoader,
                                    finalLocalBridgeCallbackClazzArray) { proxy, innerMethod, args ->
                                        if ("equals" == innerMethod.name) {
                                            return@newProxyInstance proxy === args!![0]
                                        }
                                        if ("hashCode" == innerMethod.name) {
                                            return@newProxyInstance System.identityHashCode(proxy)
                                        }
                                        if ("toString" == innerMethod.name) {
                                            return@newProxyInstance "Proxy for " + finalLocalBridgeCallbackClazzArray[0].name
                                        }
                                        if (args != null && args.size >= 1 && "sendJSONResponse" == innerMethod.name) {
                                            try {
                                                val obj = args[0]
                                                // 获取 JSON 字符串，失败时重试一次
                                                var jsonString: String? = null
                                                try {
                                                    jsonString = XposedHelpers.callMethod(obj, "toJSONString") as String?
                                                } catch (e: Exception) {
                                                    // 第一次失败，尝试重试
                                                    try {
                                                        GlobalThreadPools.sleepCompat(100L)
                                                        jsonString = XposedHelpers.callMethod(obj, "toJSONString") as String?
                                                    } catch (retryException: Exception) {
                                                        // 重试后仍失败，记录日志并标记错误，触发外层RPC重试
                                                        Log.record(TAG, "toJSONString 重试后仍然失败，将触发整个 RPC 请求重试: " + retryException.message)
                                                        rpcEntity.setResponseObject(obj, null)
                                                        rpcEntity.setError()
                                                        return@newProxyInstance null
                                                    }
                                                }

                                                rpcEntity.setResponseObject(obj, jsonString)
                                                if (!(XposedHelpers.callMethod(obj, "containsKey", "success") as Boolean)
                                                        && !(XposedHelpers.callMethod(obj, "containsKey", "isSuccess") as Boolean)) {
                                                    rpcEntity.setError()
                                                    if (shouldShowErrorLog(rpcEntity.requestMethod)) {
                                                        Log.error(TAG, "new rpc response1 | id: " + rpcEntity.hashCode() + " | method: " + rpcEntity.requestMethod + "\n " +
                                                                "args: " + rpcEntity.requestData + " |\n data: " + rpcEntity.responseString)
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                rpcEntity.setError()
                                                Log.printStackTrace(TAG,"new rpc response2 | id: " + rpcEntity.hashCode() + " | method: " + rpcEntity.requestMethod +
                                                        " err:",e)
                                            }
                                        }
                                        null
                                    }
                    )
                    if (!rpcEntity.hasResult) {
                        logNullResponse(rpcEntity, "无响应结果", count)
                        return null
                    }
                    if (!rpcEntity.hasError) {
                        return rpcEntity
                    }
                    try {
                        val errorCode = XposedHelpers.callMethod(rpcEntity.responseObject, "getString", "error") as String?
                        val errorMessage = XposedHelpers.callMethod(rpcEntity.responseObject, "getString", "errorMessage") as String?
                        val response = rpcEntity.responseString
                        val methodName = rpcEntity.requestMethod

                        // 检测安全验证错误，自动启动目标应用（带防抖和版本检查）

                        if (errorMessage != null && errorMessage.contains("为了保障您的操作安全，请进行验证后继续")) {
                            // 检查版本号，只有版本低于等于10.6.58.99999才自动启动目标应用
                            if (!ApplicationHook.shouldEnableSimplePageManager()) {
                              //  Log.record(TAG, "目标应用版本不支持自动启动目标应用进行滑块验证，跳过")
                                return null
                            }
                            var currentTime = System.currentTimeMillis()
                            var timeSinceLastStart = currentTime - lastAlipayStartTime
                            if (timeSinceLastStart < ALIPAY_START_DEBOUNCE_TIME) {
                                 Log.record(TAG, "距离上次启动目标应用仅 " + timeSinceLastStart + "ms，跳过本次启动")
                            } else {
                                synchronized(alipayStartLock) {
                                    // 双重检查，防止多线程竞争
                                    currentTime = System.currentTimeMillis()
                                    timeSinceLastStart = currentTime - lastAlipayStartTime
                                    if (timeSinceLastStart < ALIPAY_START_DEBOUNCE_TIME) {
                                         Log.record(TAG, "距离上次启动目标应用仅 " + timeSinceLastStart + "ms，跳过本次启动（双重检查）")
                                    } else {
                                        lastAlipayStartTime = currentTime
                                         Log.record(TAG, "检测到安全验证错误，自动启动目标应用进行滑块中...")
                                        Toast.show(
                                                "为了保障您的操作安全，请进行验证后继续,自动启动目标应用进行滑块中..."
                                        )
                                        // 使用增强的shell命令启动目标应用，
                                        SwipeUtil.startAlipay(ApplicationHook.appContext!!)
                                    }
                                }
                            }
                            return null
                        }

                        if ((errorCode != null && errorMark.contains(errorCode)) || (errorMessage != null && errorStringMark.contains(errorMessage))) {
                            val currentErrorCount = maxErrorCount.incrementAndGet()
                            if (!ApplicationHook.offline) {
                                if (currentErrorCount > setMaxErrorCount) {
                                    ApplicationHook.setOffline(true)
                                    Notify.updateStatusText("网络连接异常，已进入离线模式")
                                    if (BaseModel.errNotify.value) {
                                        Notify.sendNewNotification(TimeUtil.getTimeStr() + " | 网络异常次数超过阈值[" + setMaxErrorCount + "]", response)
                                    }
                                }
//                                if (BaseModel.errNotify.value) {
//                                    Notify.sendNewNotification(TimeUtil.getTimeStr() + " | 网络异常: " + methodName, response)
//                                }//做得多错的多，不做就不会错
                                if (BaseModel.timeoutRestart.value) {
                                    Log.record(TAG, "尝试重新登录")
                                    ApplicationHook.reLoginByBroadcast()
                                }
                            }
                            logNullResponse(rpcEntity, "网络错误: $errorCode/$errorMessage", count)
                            return null
                        }
                        return rpcEntity
                    } catch (e: Exception) {
                        Log.error(TAG, "new rpc response | id: " + rpcEntity.hashCode() + " | method: " + rpcEntity.requestMethod + " get err:")
                        Log.printStackTrace(e)
                    }
                    if (retryInterval < 0) {
                        CoroutineUtils.sleepCompat((600 + RandomUtil.delay()).toLong())
                    } else if (retryInterval > 0) {
                        CoroutineUtils.sleepCompat(retryInterval.toLong())
                    }
                } catch (t: Throwable) {
                    Log.error(TAG, "new rpc request | id: " + rpcEntity.hashCode() + " | method: " + rpcEntity.requestMethod + " err:")
                    Log.printStackTrace(t)
                    if (retryInterval < 0) {
                        CoroutineUtils.sleepCompat((600 + RandomUtil.delay()).toLong())
                    } else if (retryInterval > 0) {
                        CoroutineUtils.sleepCompat(retryInterval.toLong())
                    }
                }
            } while (count < tryCount)
            logNullResponse(rpcEntity, "重试次数耗尽", tryCount)
            return null
        } finally {
         //   Log.record(TAG, "New RPC\n方法: " + rpcEntity.requestMethod + "\n参数: " + rpcEntity.requestData + "\n数据: " + rpcEntity.responseString + "\n" + "\n" + "堆栈:" + Exception().stackTrace[1].toString())
         //   Log.printStack(TAG)

        }
    }

    companion object {
        private val TAG = NewRpcBridge::class.java.simpleName
        private const val ALIPAY_START_DEBOUNCE_TIME = 8000L // 目标应用启动防抖时间：8秒

        @Volatile
        private var lastAlipayStartTime = 0L // 上次启动目标应用的时间戳
        private val alipayStartLock = Any() // 目标应用启动锁
    }
}
