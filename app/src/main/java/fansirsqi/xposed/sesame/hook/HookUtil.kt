package fansirsqi.xposed.sesame.hook

import fansirsqi.xposed.sesame.hook.rpc.TokenHooker
import fansirsqi.xposed.sesame.hook.rpc.HookSender
import fansirsqi.xposed.sesame.data.General
import fansirsqi.xposed.sesame.entity.UserEntity
import fansirsqi.xposed.sesame.core.app.AssetUtil
import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.core.reflect.ReflectUtil
import fansirsqi.xposed.sesame.hook.compat.HookCallback
import fansirsqi.xposed.sesame.hook.compat.HookParam
import fansirsqi.xposed.sesame.hook.compat.Hooker
import fansirsqi.xposed.sesame.util.maps.UserMap
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap


object HookUtil {
    private const val TAG = "HookUtil"

    val rpcHookMap = ConcurrentHashMap<Any, Array<Any?>>()

    private var lastToastTime = 0L

    private var microContextCache: Any? = null

    /**
     * Hook RpcBridgeExtension.rpc 方法，记录请求信息
     *
     * debugMode/sendHookData 在回调内运行时读取，保证关闭开关后立即生效，
     * 不依赖 RESTART 广播触发的重新初始化（广播可能未送达导致配置陈旧）。
     */
    fun hookRpcBridgeExtension(classLoader: ClassLoader) {
        try {
            val className = "com.alibaba.ariver.commonability.network.rpc.RpcBridgeExtension"
            val jsonClassName = General.JSON_OBJECT_NAME // 替换为你项目中的实际 JSON 类名

            val jsonClass = Class.forName(jsonClassName, false, classLoader)
            val appClass = Class.forName("com.alibaba.ariver.app.api.App", false, classLoader)
            val pageClass = Class.forName("com.alibaba.ariver.app.api.Page", false, classLoader)
            val apiContextClass = Class.forName("com.alibaba.ariver.engine.api.bridge.model.ApiContext", false, classLoader)
            val bridgeCallbackClass = Class.forName("com.alibaba.ariver.engine.api.bridge.extension.BridgeCallback", false, classLoader)

            Hooker.get().hookMethod(
                ReflectUtil.findMethodExact(
                    Class.forName(className, false, classLoader),
                    "rpc",
                    String::class.java,
                    Boolean::class.javaPrimitiveType!!,
                    Boolean::class.javaPrimitiveType!!,
                    String::class.java,
                    jsonClass,
                    String::class.java,
                    jsonClass,
                    Boolean::class.javaPrimitiveType!!,
                    Boolean::class.javaPrimitiveType!!,
                    Int::class.javaPrimitiveType!!,
                    Boolean::class.javaPrimitiveType!!,
                    String::class.java,
                    appClass,
                    pageClass,
                    apiContextClass,
                    bridgeCallbackClass,
                ),
                object : HookCallback {
                    override fun before(p: HookParam) {
                        val args = p.args
                        if (args.size > 15) {// 参数校验
                            // 1. 获取方法名
                            val methodName = args[0] as? String ?: return
                            // 2. 获取参数 (这是一个反射得到的 com.alibaba.fastjson.JSONObject 对象)
                            val rawParams = args[4]

                            // 3. 这里的 rawParams 是阿里内部的 JSON 对象，不是 org.json.JSONObject
                            // 需要转换一下。最稳妥的方法是 toString() 然后再转 org.json.JSONObject
                            if (rawParams != null) {
                                val jsonString = rawParams.toString()
                                val jsonObject = JSONObject(jsonString)
                                // ✅✅✅ 关键：把拦截到的数据扔给 VIPHook 进行分发
                                TokenHooker.handleRpc(methodName, jsonObject)
                            }

                            val callback = args[15] ?: return
                            // 抓包开关关闭时不记录请求数据（响应回调也据此跳过）
                            if (ApplicationHook.config.debugMode.value) {
                                val recordArray = arrayOfNulls<Any>(4).apply {
                                    this[0] = System.currentTimeMillis()
                                    this[1] = args[0] ?: "null" // method name
                                    this[2] = args[4] ?: "null" // params
                                }
                                rpcHookMap[callback] = recordArray
                            }
                        }
                    }

                    override fun after(p: HookParam) {
                        val args = p.args
                        if (args.size > 15) {
                            val callback = args[15] ?: return
                            val recordArray = rpcHookMap.remove(callback)
                            recordArray?.let {
                                try {
                                    val time = it[0]
                                    val method = it.getOrNull(1)
                                    val params = it.getOrNull(2)
                                    val data = it.getOrNull(3)

                                    val dataIsNullValue: Boolean = data == null
                                    if (!dataIsNullValue) {

                                        val res = JSONObject().apply {
                                            put("TimeStamp", time)
                                            put("Method", method)
                                            put("Params", params)
                                            put("Data", data)
                                        }

                                        val prettyRecord = """
{
"TimeStamp": $time,
"Method": "$method",
"Params": $params,
"Data": $data
}
""".trimIndent()

                                        // 运行时读取开关，配置重载后立即按新值发送
                                        if (ApplicationHook.config.sendHookData.value) {
                                            HookSender.sendHookData(res, ApplicationHook.config.sendHookDataUrl.value)
                                        }
                                        Log.capture(prettyRecord)
                                    }
                                } catch (e: Exception) {
                                    Log.record(TAG, "JSON 构建失败: ${e.message}")
                                }
                            }
                        }
                    }
                })
            Log.record(TAG, "Hook RpcBridgeExtension#rpc 成功")
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "Hook RpcBridgeExtension#rpc 失败", t)
        }
    }

    fun hookOtherService(classLoader: ClassLoader) {
        try {
            //hook 服务不在后台
            val fgBgClass = Class.forName("com.alipay.mobile.common.fgbg.FgBgMonitorImpl", false, classLoader)
            val constFalse = object : HookCallback {
                override fun before(p: HookParam) {
                    p.result = false
                }
            }
            Hooker.get().hookMethod(ReflectUtil.findMethodExact(fgBgClass, "isInBackground"), constFalse)
            Hooker.get().hookMethod(
                ReflectUtil.findMethodExact(fgBgClass, "isInBackground", Boolean::class.javaPrimitiveType!!),
                constFalse
            )
            Hooker.get().hookMethod(ReflectUtil.findMethodExact(fgBgClass, "isInBackgroundV2"), constFalse)
            //hook 服务在前台
            Hooker.get().hookMethod(
                ReflectUtil.findMethodExact(
                    Class.forName("com.alipay.mobile.common.transport.utils.MiscUtils", false, classLoader),
                    "isAtFrontDesk",
                    classLoader.loadClass("android.content.Context"),
                ),
                object : HookCallback {
                    override fun before(p: HookParam) {
                        p.result = true
                    }
                }
            )
        } catch (e: Exception) {
            Log.printStackTrace(TAG, "hookOtherService 失败", e)
        }
    }

    /**
     * Hook DefaultBridgeCallback.sendJSONResponse 方法，记录响应内容
     */
    fun hookDefaultBridgeCallback(classLoader: ClassLoader) {
        try {
            val className = "com.alibaba.ariver.engine.common.bridge.internal.DefaultBridgeCallback"
            val jsonClassName = General.JSON_OBJECT_NAME
            val jsonClass = Class.forName(jsonClassName, false, classLoader)
            Hooker.get().hookMethod(
                ReflectUtil.findMethodExact(Class.forName(className, false, classLoader), "sendJSONResponse", jsonClass),
                object : HookCallback {
                    override fun before(p: HookParam) {
                        val callback = p.thisObject ?: return
                        val recordArray = rpcHookMap[callback]
                        if (recordArray != null && p.args.isNotEmpty()) {
                            recordArray[3] = p.args[0].toString()
                        }
                    }
                })
            Log.record(TAG, "Hook DefaultBridgeCallback#sendJSONResponse 成功")
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "Hook DefaultBridgeCallback#sendJSONResponse 失败", t)
        }
    }

    /**
     * 突破目标应用最大可登录账号数量限制
     * @param classLoader 类加载器
     */
    fun fuckAccounLimit(classLoader: ClassLoader) {
        Log.record(TAG, "Hook AccountManagerListAdapter#getCount")
        Hooker.get().hookMethod(
            ReflectUtil.findMethodExact(
                Class.forName("com.alipay.mobile.security.accountmanager.data.AccountManagerListAdapter", false, classLoader),
                "getCount",
            ),
            object : HookCallback {
                override fun after(p: HookParam) {
                    // 获取真实账号列表大小
                    try {
                        val list = ReflectUtil.getObjectField(p.thisObject!!, "queryAccountList") as? List<*>
                        if (list != null) {
                            p.result = list.size  // 设置返回值为真实数量
                            val now = System.currentTimeMillis()
                            if (now - lastToastTime > 1000 * 60) { // 每N秒最多显示一次
                                Toast.show("🎉 TK已尝试为您突破限制")
                                lastToastTime = now
                            }
                        }
                        return
//                        Log.runtime(TAG, "Hook AccountManagerListAdapter#getCount but return is null")
                    } catch (e: Throwable) {
                        // 错误日志处理（你可以替换为自己的日志方法）
                        e.printStackTrace()
                        Log.error(TAG, "Hook AccountManagerListAdapter#getCount failed: ${e.message}")
                    }
                }
            })
        Log.record(TAG, "Hook AccountManagerListAdapter#getCount END")
    }


    fun getMicroApplicationContext(classLoader: ClassLoader): Any? {
        if (microContextCache != null) return microContextCache
        return runCatching {
            val appClass = Class.forName(
                "com.alipay.mobile.framework.AlipayApplication", false, classLoader
            )
            val appInstance = ReflectUtil.callStaticMethod(appClass, "getInstance")
            ReflectUtil.callMethod(appInstance, "getMicroApplicationContext")
                .also { microContextCache = it }
        }.onFailure {
            Log.printStackTrace(TAG, it)
        }.getOrNull()
    }

    fun getServiceObject(classLoader: ClassLoader, serviceName: String): Any? = runCatching {
        val microContext = getMicroApplicationContext(classLoader)
        ReflectUtil.callMethod(microContext, "findServiceByInterface", serviceName)
    }.onFailure {
        Log.printStackTrace(TAG, it)
    }.getOrNull()

    fun getUserObject(classLoader: ClassLoader): Any? = runCatching {
        val serviceClassName = "com.alipay.mobile.personalbase.service.SocialSdkContactService"
        val serviceClass = Class.forName(serviceClassName, false, classLoader)
        val serviceObject = getServiceObject(classLoader, serviceClass.name)
        ReflectUtil.callMethod(serviceObject, "getMyAccountInfoModelByLocal")
    }.onFailure {
        Log.printStackTrace(TAG, it)
    }.getOrNull()

    fun getUserId(classLoader: ClassLoader): String? = runCatching {
        val userObject = getUserObject(classLoader)
        userObject?.let { ReflectUtil.getObjectField(it, "userId") as? String }
    }.onFailure {
        Log.printStackTrace(TAG, it)
    }.getOrNull()

    fun hookUser(classLoader: ClassLoader) {
        runCatching {
            UserMap.unload()
            val selfId = getUserId(classLoader)
            UserMap.setCurrentUserId(selfId) //有些地方要用到 要set一下
            val clsUserIndependentCache = classLoader.loadClass("com.alipay.mobile.socialcommonsdk.bizdata.UserIndependentCache")
            val clsAliAccountDaoOp = classLoader.loadClass("com.alipay.mobile.socialcommonsdk.bizdata.contact.data.AliAccountDaoOp")
            val aliAccountDaoOp = ReflectUtil.callStaticMethod(clsUserIndependentCache, "getCacheObj", clsAliAccountDaoOp)
            val allFriends = ReflectUtil.callMethod(aliAccountDaoOp, "getAllFriends") as? List<*> ?: emptyList<Any>()
            if (allFriends.isEmpty()) return
            val friendClass = allFriends.firstOrNull()?.javaClass ?: return
            val userIdField = ReflectUtil.findField(friendClass, "userId")
            val accountField = ReflectUtil.findField(friendClass, "account")
            val nameField = ReflectUtil.findField(friendClass, "name")
            val nickNameField = ReflectUtil.findField(friendClass, "nickName")
            val remarkNameField = ReflectUtil.findField(friendClass, "remarkName")
            val friendStatusField = ReflectUtil.findField(friendClass, "friendStatus")
            var selfEntity: UserEntity? = null
            allFriends.forEach { userObject ->
                runCatching {
                    val userId = userIdField.get(userObject) as? String
                    val account = accountField.get(userObject) as? String
                    val name = nameField.get(userObject) as? String
                    val nickName = nickNameField.get(userObject) as? String
                    val remarkName = remarkNameField.get(userObject) as? String
                    val friendStatus = friendStatusField.get(userObject) as? Int
                    val userEntity = UserEntity(userId, account, friendStatus, name, nickName, remarkName)
                    if (userId == selfId) selfEntity = userEntity
                    UserMap.add(userEntity)
                }.onFailure {
                    Log.record(TAG, "addUserObject err:")
                    Log.printStackTrace(it)
                }
            }

            UserMap.saveSelf(selfEntity)
            UserMap.save(selfId)
            Log.record(TAG, "userCache load scuess !")
        }.onFailure {
            Log.printStackTrace(TAG, "hookUser 失败", it)
        }
    }
    fun hookAssetManagerForModel(loader: ClassLoader) {
        try {
            val assetManagerClass = android.content.res.AssetManager::class.java

            fun tryRedirectModel(p: HookParam) {
                val fileName = p.args.getOrNull(0) as? String ?: return
                if (fileName != AssetUtil.SLIDER_MODEL) return

                val modelFile = AssetUtil.modelPrivateFile
                if (modelFile != null && modelFile.exists()) {
                    val pfd = android.os.ParcelFileDescriptor.open(
                        modelFile,
                        android.os.ParcelFileDescriptor.MODE_READ_ONLY
                    )
                    val afd = android.content.res.AssetFileDescriptor(pfd, 0, modelFile.length())
                    p.result = afd
                    Log.record(TAG, "成功拦截 Asset 加载：重定向 ${AssetUtil.SLIDER_MODEL} 到 ${modelFile.absolutePath}")
                } else {
                    Log.error(TAG, "拦截失败：私有模型文件不存在")
                }
            }

            Hooker.get().hookMethod(
                ReflectUtil.findMethodExact(assetManagerClass, "openFd", String::class.java),
                object : HookCallback {
                    override fun before(p: HookParam) {
                        tryRedirectModel(p)
                    }
                }
            )

            Hooker.get().hookMethod(
                ReflectUtil.findMethodExact(assetManagerClass, "openAssetFd", String::class.java),
                object : HookCallback {
                    override fun before(p: HookParam) {
                        tryRedirectModel(p)
                    }
                }
            )

            Log.record(TAG, "Hook AssetManager(openFd/openAssetFd) 成功")
        } catch (e: Exception) {
            Log.printStackTrace(TAG, "Hook AssetManager 失败", e)
        }
    }
}
