package fansirsqi.xposed.sesame.task.reserve

import fansirsqi.xposed.sesame.hook.RequestManager

object ReserveRpcCall {
    private const val VERSION = "20230501"
    private const val VERSION2 = "20230522"

    @JvmStatic
    suspend fun queryTreeItemsForExchange(): String {
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.queryTreeItemsForExchange",
            "[{\"cityCode\":\"370100\",\"itemTypes\":\"\",\"source\":\"chInfo_ch_appcenter__chsub_9patch\",\"version\":\"$VERSION2\"}]"
        )
    }

    @JvmStatic
    suspend fun queryTreeForExchange(projectId: String?): String {
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.queryTreeForExchange",
            "[{\"projectId\":\"$projectId\",\"version\":\"$VERSION\",\"source\":\"chInfo_ch_appcenter__chsub_9patch\"}]"
        )
    }

    @JvmStatic
    suspend fun exchangeTree(projectId: String): String {
        val projectId_num = projectId.toInt()
        return RequestManager.requestString(
            "alipay.antmember.forest.h5.exchangeTree",
            "[{\"projectId\":$projectId_num,\"sToken\":\"${System.currentTimeMillis()}\",\"version\":\"$VERSION\",\"source\":\"chInfo_ch_appcenter__chsub_9patch\"}]"
        )
    }

    /* 查询地图树苗 */
    @JvmStatic
    suspend fun queryAreaTrees(): String {
        return RequestManager.requestString("alipay.antmember.forest.h5.queryAreaTrees", "[{}]")
    }

    @JvmStatic
    suspend fun queryTreeItemsForExchange(applyActions: String?, itemTypes: String?): String {
        val args = "[{\"applyActions\":\"$applyActions\",\"itemTypes\":\"$itemTypes\"}]"
        return RequestManager.requestString("alipay.antforest.forest.h5.queryTreeItemsForExchange", args)
    }
}
