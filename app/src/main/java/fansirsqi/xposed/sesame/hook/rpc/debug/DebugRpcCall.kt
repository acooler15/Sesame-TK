package fansirsqi.xposed.sesame.hook.rpc.debug

import fansirsqi.xposed.sesame.hook.RequestManager

object DebugRpcCall {
    private const val version = "2.0"

    @JvmStatic
    suspend fun queryBaseinfo(): String {
        return RequestManager.requestString(
            "com.alipay.neverland.biz.rpc.queryBaseinfo",
            "[{\"branchId\":\"WUFU\",\"source\":\"fuqiTown\"}]"
        )
    }

    /** 行走格子 */
    @JvmStatic
    suspend fun walkGrid(): String {
        return RequestManager.requestString(
            "com.alipay.neverland.biz.rpc.walkGrid",
            "[{\"drilling\":false,\"mapId\":\"MF1\",\"source\":\"fuqiTown\"}]"
        )
    }

    /** 小游戏 */
    @JvmStatic
    suspend fun miniGameFinish(gameId: String?, gameKey: String?): String {
        return RequestManager.requestString(
            "com.alipay.neverland.biz.rpc.miniGameFinish",
            "[{\"gameId\":\"" + gameId + "\",\"gameKey\":\"" + gameKey +
                    "\",\"mapId\":\"MF1\",\"score\":490,\"source\":\"fuqiTown\"}]"
        )
    }

    @JvmStatic
    suspend fun taskFinish(bizId: String?): String {
        return RequestManager.requestString(
            "com.alipay.adtask.biz.mobilegw.service.task.finish",
            "[{\"bizId\":\"" + bizId + "\"}]"
        )
    }

    @JvmStatic
    suspend fun queryAdFinished(bizId: String?, scene: String?): String {
        return RequestManager.requestString(
            "com.alipay.neverland.biz.rpc.queryAdFinished",
            "[{\"adBizNo\":\"" + bizId + "\",\"scene\":\"" + scene +
                    "\",\"source\":\"fuqiTown\"}]"
        )
    }

    @JvmStatic
    suspend fun queryWufuTaskHall(): String {
        return RequestManager.requestString(
            "com.alipay.neverland.biz.rpc.queryWufuTaskHall",
            "[{\"source\":\"fuqiTown\"}]"
        )
    }

    @JvmStatic
    suspend fun fuQiTaskQuery(): String {
        return RequestManager.requestString(
            "com.alipay.wufudragonprod.biz.wufu2024.fuQiTown.fuQiTask.query",
            "[{}]"
        )
    }

    @JvmStatic
    suspend fun fuQiTaskTrigger(appletId: String?, stageCode: String?): String {
        return RequestManager.requestString(
            "com.alipay.wufudragonprod.biz.wufu2024.fuQiTown.fuQiTask.trigger",
            "[{\"appletId\":\"" + appletId + "\",\"stageCode\":\"" + stageCode + "\"}]"
        )
    }

    @JvmStatic
    suspend fun queryEnvironmentCertDetailList(alias: String?, pageNum: Int, targetUserID: String?): String {
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.queryEnvironmentCertDetailList",
            "[{\"alias\":\"" + alias + "\",\"certId\":\"\",\"pageNum\":" + pageNum +
                    ",\"shareId\":\"\",\"source\":\"chInfo_ch_appcenter__chsub_9patch\",\"targetUserID\":\"" +
                    targetUserID + "\",\"version\":\"20230701\"}]"
        )
    }

    @JvmStatic
    suspend fun sendTree(certificateId: String?, friendUserId: String?): String {
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.sendTree",
            "[{\"blessWords\":\"梭梭没有叶子，四季常青，从不掉发，祝你发量如梭。\",\"certificateId\":\"" + certificateId +
                    "\",\"friendUserId\":\"" + friendUserId +
                    "\",\"source\":\"chInfo_ch_appcenter__chsub_9patch\"}]"
        )
    }
}
