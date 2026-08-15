package fansirsqi.xposed.sesame.task.EcoProtection

import fansirsqi.xposed.sesame.hook.RequestManager.requestString

object EcoProtectionRpcCall {
    private const val VERSION = "20230522"
    suspend fun homePage(selectCityCode: String?): String {
        return requestString(
            "alipay.greenmatrix.rpc.h5.ancienttree.homePage",
            ("[{\"cityCode\":\"330100\",\"selectCityCode\":\"" + selectCityCode
                    + "\",\"source\":\"antforesthome\"}]")
        )
    }

    suspend fun queryTreeItemsForExchange(cityCode: String?): String {
        return requestString(
            "alipay.antforest.forest.h5.queryTreeItemsForExchange",
            ("[{\"cityCode\":\"" + cityCode
                    + "\",\"itemTypes\":\"\",\"source\":\"chInfo_ch_appcenter__chsub_9patch\",\"version\":\""
                    + VERSION + "\"}]")
        )
    }

    suspend fun districtDetail(districtCode: String?): String {
        return requestString(
            "alipay.greenmatrix.rpc.h5.ancienttree.districtDetail",
            "[{\"districtCode\":\"" + districtCode + "\",\"source\":\"antforesthome\"}]"
        )
    }

    suspend fun projectDetail(ancientTreeProjectId: String?, cityCode: String?): String {
        return requestString(
            "alipay.greenmatrix.rpc.h5.ancienttree.projectDetail",
            ("[{\"ancientTreeProjectId\":\"" + ancientTreeProjectId
                    + "\",\"channel\":\"ONLINE\",\"cityCode\":\"" + cityCode
                    + "\",\"source\":\"ancientreethome\"}]")
        )
    }

    suspend fun protect(activityId: String?, ancientTreeProjectId: String?, cityCode: String?): String {
        return requestString(
            "alipay.greenmatrix.rpc.h5.ancienttree.protect",
            ("[{\"ancientTreeActivityId\":\"" + activityId + "\",\"ancientTreeProjectId\":\""
                    + ancientTreeProjectId + "\",\"cityCode\":\"" + cityCode
                    + "\",\"source\":\"ancientreethome\"}]")
        )
    }
}