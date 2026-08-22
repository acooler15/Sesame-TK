package fansirsqi.xposed.sesame.task.reserve

import fansirsqi.xposed.sesame.hook.RequestManager
import fansirsqi.xposed.sesame.hook.rpc.RpcRequestData

object ReserveRpcCall {
    private const val VERSION = "20230501"
    private const val VERSION2 = "20230522"

    @JvmStatic
    suspend fun queryTreeItemsForExchange(): String {
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.queryTreeItemsForExchange",
            RpcRequestData.array {
                put("cityCode", "370100")
                put("itemTypes", "")
                put("source", "chInfo_ch_appcenter__chsub_9patch")
                put("version", VERSION2)
            }
        )
    }

    @JvmStatic
    suspend fun queryTreeForExchange(projectId: String?): String {
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.queryTreeForExchange",
            RpcRequestData.array {
                // 原为 "$projectId" 字符串插值，null 时产出字面 "null"，用 ?: "null" 保持等价
                put("projectId", projectId ?: "null")
                put("version", VERSION)
                put("source", "chInfo_ch_appcenter__chsub_9patch")
            }
        )
    }

    @JvmStatic
    suspend fun exchangeTree(projectId: String): String {
        return RequestManager.requestString(
            "alipay.antmember.forest.h5.exchangeTree",
            RpcRequestData.array {
                put("projectId", projectId.toInt())
                put("sToken", System.currentTimeMillis().toString())
                put("version", VERSION)
                put("source", "chInfo_ch_appcenter__chsub_9patch")
            }
        )
    }

    /* 查询地图树苗 */
    @JvmStatic
    suspend fun queryAreaTrees(): String {
        return RequestManager.requestString("alipay.antmember.forest.h5.queryAreaTrees", RpcRequestData.array { })
    }

    @JvmStatic
    suspend fun queryTreeItemsForExchange(applyActions: String?, itemTypes: String?): String {
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.queryTreeItemsForExchange",
            RpcRequestData.array {
                // 原为 "$applyActions"/"$itemTypes" 字符串插值，null 时产出字面 "null"，用 ?: "null" 保持等价
                put("applyActions", applyActions ?: "null")
                put("itemTypes", itemTypes ?: "null")
            }
        )
    }
}
