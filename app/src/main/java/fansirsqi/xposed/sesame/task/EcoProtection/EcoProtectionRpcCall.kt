package fansirsqi.xposed.sesame.task.EcoProtection

import fansirsqi.xposed.sesame.hook.RequestManager.requestString
import fansirsqi.xposed.sesame.hook.rpc.RpcRequestData

object EcoProtectionRpcCall {
    private const val VERSION = "20230522"
    suspend fun homePage(selectCityCode: String?): String {
        return requestString(
            "alipay.greenmatrix.rpc.h5.ancienttree.homePage",
            RpcRequestData.array {
                put("cityCode", "330100")
                // 原为字符串拼接，null 时输出字符串 "null"（非 JSON null），保持等价
                put("selectCityCode", selectCityCode ?: "null")
                put("source", "antforesthome")
            }
        )
    }

    suspend fun queryTreeItemsForExchange(cityCode: String?): String {
        return requestString(
            "alipay.antforest.forest.h5.queryTreeItemsForExchange",
            RpcRequestData.array {
                put("cityCode", cityCode ?: "null")
                put("itemTypes", "")
                put("source", "chInfo_ch_appcenter__chsub_9patch")
                put("version", VERSION)
            }
        )
    }

    suspend fun districtDetail(districtCode: String?): String {
        return requestString(
            "alipay.greenmatrix.rpc.h5.ancienttree.districtDetail",
            RpcRequestData.array {
                put("districtCode", districtCode ?: "null")
                put("source", "antforesthome")
            }
        )
    }

    suspend fun projectDetail(ancientTreeProjectId: String?, cityCode: String?): String {
        return requestString(
            "alipay.greenmatrix.rpc.h5.ancienttree.projectDetail",
            RpcRequestData.array {
                put("ancientTreeProjectId", ancientTreeProjectId ?: "null")
                put("channel", "ONLINE")
                put("cityCode", cityCode ?: "null")
                put("source", "ancientreethome")
            }
        )
    }

    suspend fun protect(activityId: String?, ancientTreeProjectId: String?, cityCode: String?): String {
        return requestString(
            "alipay.greenmatrix.rpc.h5.ancienttree.protect",
            RpcRequestData.array {
                put("ancientTreeActivityId", activityId ?: "null")
                put("ancientTreeProjectId", ancientTreeProjectId ?: "null")
                put("cityCode", cityCode ?: "null")
                put("source", "ancientreethome")
            }
        )
    }
}
