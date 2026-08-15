package fansirsqi.xposed.sesame.task.antOrchard

import fansirsqi.xposed.sesame.hook.RequestManager

object AntOrchardRpcCall {
    private const val VERSION = "20251209.01"

    suspend fun orchardIndex(): String {
        return RequestManager.requestString("com.alipay.antfarm.orchardIndex",
            "[{\"inHomepage\":\"true\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ORCHARD\",\"source\":\"ch_appcenter__chsub_9patch\",\"version\":\""
                    + VERSION + "\"}]");
    }

    /**
     * 获取额外信息（包含每日肥料、施肥礼盒）
     * @param from 来源：entry(首页), water(施肥后)
     */
    suspend fun extraInfoGet(from: String = "entry"): String {
        return RequestManager.requestString(
            "com.alipay.antorchard.extraInfoGet",
            "[{\"from\":\"$from\",\"requestType\":\"NORMAL\",\"sceneCode\":\"FUGUO\",\"source\":\"ch_appcenter__chsub_9patch\",\"version\":\"$VERSION\"}]"
        )
    }

    suspend fun extraInfoSet(): String {
        return RequestManager.requestString(
            "com.alipay.antorchard.extraInfoSet",
            "[{\"bizCode\":\"fertilizerPacket\",\"bizParam\":{\"action\":\"queryCollectFertilizerPacket\"},\"requestType\":\"NORMAL\",\"sceneCode\":\"ORCHARD\",\"source\":\"ch_appcenter__chsub_9patch\",\"version\":\"$VERSION\"}]"
        )
    }

    // 修改：增加 LIMITED_TIME_CHALLENGE 和 LOTTERY_PLUS 类型
    suspend fun querySubplotsActivity(treeLevel: String): String {
        return RequestManager.requestString(
            "com.alipay.antorchard.querySubplotsActivity",
            "[{\"activityType\":[\"WISH\",\"BATTLE\",\"HELP_FARMER\",\"DEFOLIATION\",\"CAMP_TAKEOVER\",\"LIMITED_TIME_CHALLENGE\",\"LOTTERY_PLUS\"],\"inHomepage\":false,\"requestType\":\"NORMAL\",\"sceneCode\":\"ORCHARD\",\"source\":\"ch_appcenter__chsub_9patch\",\"treeLevel\":\"$treeLevel\",\"version\":\"$VERSION\"}]"
        )
    }

    suspend fun triggerSubplotsActivity(activityId: String, activityType: String, optionKey: String): String {
        return RequestManager.requestString(
            "com.alipay.antorchard.triggerSubplotsActivity",
            "[{\"activityId\":\"$activityId\",\"activityType\":\"$activityType\",\"optionKey\":\"$optionKey\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ORCHARD\",\"source\":\"ch_appcenter__chsub_9patch\",\"version\":\"$VERSION\"}]"
        )
    }

    suspend fun receiveOrchardRights(activityId: String, activityType: String): String {
        return RequestManager.requestString(
            "com.alipay.antorchard.receiveOrchardRights",
            "[{\"activityId\":\"$activityId\",\"activityType\":\"$activityType\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ORCHARD\",\"source\":\"ch_appcenter__chsub_9patch\",\"version\":\"$VERSION\"}]"
        )
    }

    /* 七日礼包 */
    suspend fun drawLottery(): String {
        return RequestManager.requestString(
            "com.alipay.antorchard.drawLottery",
            "[{\"lotteryScene\":\"receiveLotteryPlus\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ORCHARD\",\"source\":\"ch_appcenter__chsub_9patch\",\"version\":\"$VERSION\"}]"
        )
    }

    /**
     * 切换种植场景
     * @param plantScene main(果树) 或 yeb(摇钱树)
     */
    suspend fun switchPlantScene(plantScene: String): String {
        return RequestManager.requestString(
            "com.alipay.antorchard.switchPlantScene",
            "[{\"plantScene\":\"$plantScene\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ORCHARD\",\"source\":\"ch_appcenter__chsub_9patch\",\"version\":\"$VERSION\"}]"
        )
    }

    /**
     * 施肥
     * @param wua 用户标识
     * @param source 来源标识，可自定义
     * @param useBatchSpread 一键5次
     * @param plantScene 场景：main 或 yeb
     */
    suspend fun orchardSpreadManure(wua: String, source: String, useBatchSpread: Boolean = false, plantScene: String = "main"): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.orchardSpreadManure",
            "[{\"plantScene\":\"$plantScene\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ORCHARD\",\"source\":\"$source\",\"useBatchSpread\":$useBatchSpread,\"version\":\"$VERSION\",\"wua\":\"$wua\"}]"
        )
    }

    suspend fun receiveTaskAward(sceneCode: String, taskType: String): String {
        return RequestManager.requestString(
            "com.alipay.antiep.receiveTaskAward",
            "[{\"ignoreLimit\":true,\"requestType\":\"NORMAL\",\"sceneCode\":\"$sceneCode\",\"source\":\"ch_alipaysearch__chsub_normal\",\"taskType\":\"$taskType\",\"version\":\"$VERSION\"}]"
        )
    }

    suspend fun orchardListTask(): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.orchardListTask",
            "[{\"plantHiddenMMC\":\"false\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ORCHARD\",\"source\":\"zhifujianglizhitiao1000\",\"version\":\"$VERSION\"}]"
        )
    }

    suspend fun orchardSign(): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.orchardSign",
            "[{\"requestType\":\"NORMAL\",\"sceneCode\":\"ORCHARD\",\"signScene\":\"ANTFARM_ORCHARD_SIGN_V2\",\"source\":\"ch_appcenter__chsub_9patch\",\"version\":\"$VERSION\"}]"
        )
    }

    suspend fun finishTask(userId: String, sceneCode: String, taskType: String): String {
        return RequestManager.requestString(
            "com.alipay.antiep.finishTask",
            "[{\"outBizNo\":\"${userId}${System.currentTimeMillis()}\",\"requestType\":\"NORMAL\",\"sceneCode\":\"$sceneCode\",\"source\":\"ch_appcenter__chsub_9patch\",\"taskType\":\"$taskType\",\"userId\":\"$userId\",\"version\":\"$VERSION\"}]"
        )
    }

    suspend fun triggerTbTask(taskId: String, taskPlantType: String): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.triggerTbTask",
            "[{\"requestType\":\"NORMAL\",\"sceneCode\":\"ORCHARD\",\"source\":\"ch_appcenter__chsub_9patch\",\"taskId\":\"$taskId\",\"taskPlantType\":\"$taskPlantType\",\"version\":\"$VERSION\"}]"
        )
    }

    //砸蛋
    suspend fun smashedGoldenEgg(count: Int): String {
        val jsonArgs = """
        [
            {
                "batchSmashCount": $count,
                "requestType": "NORMAL",
                "sceneCode": "ORCHARD",
                "source": "ch_appcenter__chsub_9patch",
                "version": "$VERSION"
            }
        ]
    """.trimIndent()

        return RequestManager.requestString(
            "com.alipay.antorchard.smashedGoldenEgg",
            jsonArgs
        )
    }

    /**
     * 收取果园回访奖励
     * @param diversionSource 引流来源（如：widget、tmall）
     * @param source 具体来源（如：widget_shoufei、upgrade_tmall_exchange_task）
     * @return 请求结果字符串
     */
    suspend fun receiveOrchardVisitAward(
        diversionSource: String,
        source: String
    ): String {
        val requestParams = """
        [{"diversionSource":"$diversionSource",
          "requestType":"NORMAL",
          "sceneCode":"ORCHARD",
          "source":"$source",
          "version":"$VERSION"}]
    """.trimIndent()

        return RequestManager.requestString(
            "com.alipay.antorchard.receiveOrchardVisitAward",
            requestParams
        )
    }

    suspend fun orchardSyncIndex(Wua: String): String {
        val jsonArgs = """
         [{
             "requestType": "NORMAL",
             "sceneCode": "ORCHARD",
             "source": "ch_appcenter__chsub_9patch",
             "syncIndexTypes": "LIMITED_TIME_CHALLENGE",
             "useWua": true,
             "version": "$VERSION",
             "wua": "$Wua"
         }]
    """.trimIndent()

        return RequestManager.requestString(
            "com.alipay.antorchard.orchardSyncIndex",
            jsonArgs
        )
    }

    suspend fun noticeGame(appId: String): String {
        val jsonArgs = """
          [{
             "appId": "2021004165643274",
             "requestType": "NORMAL",
             "sceneCode": "ORCHARD",
             "source": "ch_appcenter__chsub_9patch",
             "version": "$VERSION"
         }]
    """.trimIndent()

        return RequestManager.requestString(
            "com.alipay.antorchard.noticeGame",
            jsonArgs
        )
    }

    suspend fun achieveBeShareP2P(shareId: String): String {
        return RequestManager.requestString(
            "com.alipay.antiep.achieveBeShareP2P",
            "[{\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM_ORCHARD_SHARE_P2P\",\"shareId\":\"$shareId\",\"source\":\"share\",\"version\":\"$VERSION\"}]"
        )
    }

    /* 摇钱树收余额奖励 */
    suspend fun moneyTreeTrigger(): String {
        return RequestManager.requestString(
            "com.alipay.yebbffweb.needle.yebHome.moneyTree.trigger",
            "[{\"sceneType\":\"default\",\"type\":\"trigger\"}]"
        )
    }
}