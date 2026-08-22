package fansirsqi.xposed.sesame.task.antOrchard

import fansirsqi.xposed.sesame.hook.RequestManager
import fansirsqi.xposed.sesame.hook.rpc.RpcConst
import fansirsqi.xposed.sesame.hook.rpc.RpcRequestData
import fansirsqi.xposed.sesame.hook.rpc.RpcRequestData.putStandard
import org.json.JSONArray
import org.json.JSONObject

object AntOrchardRpcCall {
    private const val VERSION = "20251209.01"

    suspend fun orchardIndex(): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.orchardIndex",
            RpcRequestData.array {
                put("inHomepage", "true")
                putStandard(
                    RpcConst.Type.NORMAL,
                    "ORCHARD",
                    "ch_appcenter__chsub_9patch",
                    VERSION
                )
            }
        )
    }

    /**
     * 获取额外信息（包含每日肥料、施肥礼盒）
     * @param from 来源：entry(首页), water(施肥后)
     */
    suspend fun extraInfoGet(from: String = "entry"): String {
        return RequestManager.requestString(
            "com.alipay.antorchard.extraInfoGet",
            RpcRequestData.array {
                put("from", from)
                putStandard(
                    RpcConst.Type.NORMAL,
                    "FUGUO",
                    "ch_appcenter__chsub_9patch",
                    VERSION
                )
            }
        )
    }

    suspend fun extraInfoSet(): String {
        return RequestManager.requestString(
            "com.alipay.antorchard.extraInfoSet",
            RpcRequestData.array {
                put("bizCode", "fertilizerPacket")
                put(
                    "bizParam",
                    JSONObject().apply { put("action", "queryCollectFertilizerPacket") })
                putStandard(
                    RpcConst.Type.NORMAL,
                    "ORCHARD",
                    "ch_appcenter__chsub_9patch",
                    VERSION
                )
            }
        )
    }

    // 修改：增加 LIMITED_TIME_CHALLENGE 和 LOTTERY_PLUS 类型
    suspend fun querySubplotsActivity(treeLevel: String): String {
        return RequestManager.requestString(
            "com.alipay.antorchard.querySubplotsActivity",
            RpcRequestData.array {
                put(
                    "activityType",
                    JSONArray(
                        listOf(
                            "WISH",
                            "BATTLE",
                            "HELP_FARMER",
                            "DEFOLIATION",
                            "CAMP_TAKEOVER",
                            "LIMITED_TIME_CHALLENGE",
                            "LOTTERY_PLUS"
                        )
                    )
                )
                put("inHomepage", false)
                put("treeLevel", treeLevel)
                putStandard(
                    RpcConst.Type.NORMAL,
                    "ORCHARD",
                    "ch_appcenter__chsub_9patch",
                    VERSION
                )
            }
        )
    }

    suspend fun triggerSubplotsActivity(
        activityId: String,
        activityType: String,
        optionKey: String
    ): String {
        return RequestManager.requestString(
            "com.alipay.antorchard.triggerSubplotsActivity",
            RpcRequestData.array {
                put("activityId", activityId)
                put("activityType", activityType)
                put("optionKey", optionKey)
                putStandard(
                    RpcConst.Type.NORMAL,
                    "ORCHARD",
                    "ch_appcenter__chsub_9patch",
                    VERSION
                )
            }
        )
    }

    suspend fun receiveOrchardRights(activityId: String, activityType: String): String {
        return RequestManager.requestString(
            "com.alipay.antorchard.receiveOrchardRights",
            RpcRequestData.array {
                put("activityId", activityId)
                put("activityType", activityType)
                putStandard(
                    RpcConst.Type.NORMAL,
                    "ORCHARD",
                    "ch_appcenter__chsub_9patch",
                    VERSION
                )
            }
        )
    }

    /* 七日礼包 */
    suspend fun drawLottery(): String {
        return RequestManager.requestString(
            "com.alipay.antorchard.drawLottery",
            RpcRequestData.array {
                put("lotteryScene", "receiveLotteryPlus")
                putStandard(
                    RpcConst.Type.NORMAL,
                    "ORCHARD",
                    "ch_appcenter__chsub_9patch",
                    VERSION
                )
            }
        )
    }

    /**
     * 切换种植场景
     * @param plantScene main(果树) 或 yeb(摇钱树)
     */
    suspend fun switchPlantScene(plantScene: String): String {
        return RequestManager.requestString(
            "com.alipay.antorchard.switchPlantScene",
            RpcRequestData.array {
                put("plantScene", plantScene)
                putStandard(
                    RpcConst.Type.NORMAL,
                    "ORCHARD",
                    "ch_appcenter__chsub_9patch",
                    VERSION
                )
            }
        )
    }

    /**
     * 施肥
     * @param wua 用户标识
     * @param source 来源标识，可自定义
     * @param useBatchSpread 一键5次
     * @param plantScene 场景：main 或 yeb
     */
    suspend fun orchardSpreadManure(
        wua: String,
        source: String,
        useBatchSpread: Boolean = false,
        plantScene: String = "main"
    ): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.orchardSpreadManure",
            RpcRequestData.array {
                put("plantScene", plantScene)
                put("source", source)
                put("useBatchSpread", useBatchSpread)
                put("wua", wua)
                putStandard(RpcConst.Type.NORMAL, "ORCHARD", version = VERSION)
            }
        )
    }

    suspend fun receiveTaskAward(sceneCode: String, taskType: String): String {
        return RequestManager.requestString(
            "com.alipay.antiep.receiveTaskAward",
            RpcRequestData.array {
                put("ignoreLimit", true)
                put("sceneCode", sceneCode)
                put("taskType", taskType)
                putStandard(
                    RpcConst.Type.NORMAL,
                    source = "ch_alipaysearch__chsub_normal",
                    version = VERSION
                )
            }
        )
    }

    suspend fun orchardListTask(): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.orchardListTask",
            RpcRequestData.array {
                put("plantHiddenMMC", "false")
                putStandard(
                    RpcConst.Type.NORMAL,
                    "ORCHARD",
                    "zhifujianglizhitiao1000",
                    VERSION
                )
            }
        )
    }

    suspend fun orchardSign(): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.orchardSign",
            RpcRequestData.array {
                put("signScene", "ANTFARM_ORCHARD_SIGN_V2")
                putStandard(
                    RpcConst.Type.NORMAL,
                    "ORCHARD",
                    "ch_appcenter__chsub_9patch",
                    VERSION
                )
            }
        )
    }

    suspend fun finishTask(userId: String, sceneCode: String, taskType: String): String {
        return RequestManager.requestString(
            "com.alipay.antiep.finishTask",
            RpcRequestData.array {
                put("outBizNo", "$userId${System.currentTimeMillis()}")
                put("sceneCode", sceneCode)
                put("taskType", taskType)
                put("userId", userId)
                putStandard(
                    RpcConst.Type.NORMAL,
                    source = "ch_appcenter__chsub_9patch",
                    version = VERSION
                )
            }
        )
    }

    suspend fun triggerTbTask(taskId: String, taskPlantType: String): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.triggerTbTask",
            RpcRequestData.array {
                put("taskId", taskId)
                put("taskPlantType", taskPlantType)
                putStandard(
                    RpcConst.Type.NORMAL,
                    "ORCHARD",
                    "ch_appcenter__chsub_9patch",
                    VERSION
                )
            }
        )
    }

    //砸蛋
    suspend fun smashedGoldenEgg(count: Int): String {
        return RequestManager.requestString(
            "com.alipay.antorchard.smashedGoldenEgg",
            RpcRequestData.array {
                put("batchSmashCount", count)
                putStandard(
                    RpcConst.Type.NORMAL,
                    "ORCHARD",
                    "ch_appcenter__chsub_9patch",
                    VERSION
                )
            }
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
        return RequestManager.requestString(
            "com.alipay.antorchard.receiveOrchardVisitAward",
            RpcRequestData.array {
                put("diversionSource", diversionSource)
                put("source", source)
                putStandard(RpcConst.Type.NORMAL, "ORCHARD", version = VERSION)
            }
        )
    }

    suspend fun orchardSyncIndex(Wua: String): String {
        return RequestManager.requestString(
            "com.alipay.antorchard.orchardSyncIndex",
            RpcRequestData.array {
                put("syncIndexTypes", "LIMITED_TIME_CHALLENGE")
                put("useWua", true)
                put("wua", Wua)
                putStandard(
                    RpcConst.Type.NORMAL,
                    "ORCHARD",
                    "ch_appcenter__chsub_9patch",
                    VERSION
                )
            }
        )
    }

    suspend fun noticeGame(appId: String): String {
        return RequestManager.requestString(
            "com.alipay.antorchard.noticeGame",
            RpcRequestData.array {
                // 原请求体硬编码 appId（入参 appId 原代码即未使用），保持原样
                put("appId", "2021004165643274")
                putStandard(
                    RpcConst.Type.NORMAL,
                    "ORCHARD",
                    "ch_appcenter__chsub_9patch",
                    VERSION
                )
            }
        )
    }

    suspend fun achieveBeShareP2P(shareId: String): String {
        return RequestManager.requestString(
            "com.alipay.antiep.achieveBeShareP2P",
            RpcRequestData.array {
                put("shareId", shareId)
                putStandard(
                    RpcConst.Type.NORMAL,
                    "ANTFARM_ORCHARD_SHARE_P2P",
                    "share",
                    VERSION
                )
            }
        )
    }

    /* 摇钱树收余额奖励 */
    suspend fun moneyTreeTrigger(): String {
        return RequestManager.requestString(
            "com.alipay.yebbffweb.needle.yebHome.moneyTree.trigger",
            RpcRequestData.array {
                put("sceneType", "default")
                put("type", "trigger")
            }
        )
    }
}
