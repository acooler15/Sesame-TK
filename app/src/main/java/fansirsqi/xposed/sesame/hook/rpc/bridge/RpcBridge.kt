package fansirsqi.xposed.sesame.hook.rpc.bridge

import fansirsqi.xposed.sesame.entity.RpcEntity

interface RpcBridge {
    @Throws(Exception::class)
    fun load()
    fun unload()

    // 核心：改为 suspend
    suspend fun requestString(rpcEntity: RpcEntity, tryCount: Int, retryInterval: Int): String?
    suspend fun requestObject(rpcEntity: RpcEntity, tryCount: Int, retryInterval: Int): RpcEntity?

    suspend fun requestString(rpcEntity: RpcEntity): String? {
        return requestString(rpcEntity, 3, -1)
    }

    /**
     * 发送RPC请求并获取响应字符串（使用默认重试参数）
     *
     * @param method RPC方法名
     * @param data 请求数据
     * @return 响应字符串，如果请求失败则返回null
     */
    suspend fun requestString(method: String?, data: String?): String? {
        return requestString(method, data, 3, 1500)
    }

    /**
     * 发送带关联数据的RPC请求并获取响应字符串（使用默认重试参数）
     *
     * @param method RPC方法名
     * @param data 请求数据
     * @param relation 关联数据
     * @return 响应字符串，如果请求失败则返回null
     */
    suspend fun requestString(method: String?, data: String?, relation: String?): String? {
        return requestString(method, data, relation, 3, 1500)
    }

    suspend fun requestString(method: String?, data: String?, appName: String?, methodName: String?, facadeName: String?): String? {
        return requestString(RpcEntity(method, data, appName, methodName, facadeName), 3, -1)
    }

    suspend fun requestString(method: String?, data: String?, tryCount: Int, retryInterval: Int): String? {
        return requestString(RpcEntity(method, data), tryCount, retryInterval)
    }

    suspend fun requestString(method: String?, data: String?, relation: String?, tryCount: Int, retryInterval: Int): String? {
        return requestString(RpcEntity(method, data, relation), tryCount, retryInterval)
    }

    suspend fun requestObject(method: String?, data: String?, relation: String?): RpcEntity? {
        return requestObject(method, data, relation, 3, -1)
    }

    suspend fun requestObject(method: String?, data: String?, tryCount: Int, retryInterval: Int): RpcEntity? {
        return requestObject(RpcEntity(method, data), tryCount, retryInterval)
    }

    suspend fun requestObject(method: String?, data: String?, relation: String?, tryCount: Int, retryInterval: Int): RpcEntity? {
        return requestObject(RpcEntity(method, data, relation), tryCount, retryInterval)
    }
}
