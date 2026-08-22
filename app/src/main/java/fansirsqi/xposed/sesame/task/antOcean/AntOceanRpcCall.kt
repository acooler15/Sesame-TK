package fansirsqi.xposed.sesame.task.antOcean

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import fansirsqi.xposed.sesame.hook.RequestManager
import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.core.util.RandomUtil
import fansirsqi.xposed.sesame.hook.rpc.RpcRequestData

/**
 * @author Constanline
 * @since 2023/08/01
 */
object AntOceanRpcCall {
    private const val VERSION = "20241203"

    private fun getUniqueId(): String {
        return System.currentTimeMillis().toString() + RandomUtil.nextLong()
    }

    private fun getAIFishUniqueId(): String {
        return RandomUtil.getRandomString(16)
    }

    @JvmStatic
    suspend fun queryOceanStatus(): String {
        return RequestManager.requestString(
            "alipay.antocean.ocean.h5.queryOceanStatus",
            RpcRequestData.array {
                put("source", "chInfo_ch_appcenter__chsub_9patch")
            }
        )
    }

    @JvmStatic
    suspend fun queryHomePage(): String {
        return RequestManager.requestString(
            "alipay.antocean.ocean.h5.queryHomePage",
            RpcRequestData.array {
                put("source", "ANT_FOREST")
                put("uniqueId", getUniqueId())
                put("version", VERSION)
            }
        )
    }

    @JvmStatic
    suspend fun cleanOcean(userId: String?): String {
        return RequestManager.requestString(
            "alipay.antocean.ocean.h5.cleanOcean",
            RpcRequestData.array {
                put("cleanedUserId", userId ?: "null")
                put("source", "ANT_FOREST")
                put("uniqueId", getUniqueId())
            }
        )
    }

    @JvmStatic
    suspend fun ipOpenSurprise(): String {
        return RequestManager.requestString(
            "alipay.antocean.ocean.h5.ipOpenSurprise",
            RpcRequestData.array {
                put("source", "chInfo_ch_appcenter__chsub_9patch")
                put("uniqueId", getUniqueId())
            }
        )
    }

    @JvmStatic
    suspend fun collectReplicaAsset(): String {
        return RequestManager.requestString(
            "alipay.antocean.ocean.h5.collectReplicaAsset",
            RpcRequestData.array {
                put("replicaCode", "avatar")
                put("source", "senlinzuoshangjiao")
                put("uniqueId", getUniqueId())
                put("version", VERSION)
            }
        )
    }

    @JvmStatic
    suspend fun receiveTaskAward(sceneCode: String?, taskType: String?): String {
        return RequestManager.requestString(
            "com.alipay.antiep.receiveTaskAward",
            RpcRequestData.array {
                put("ignoreLimit", false)
                put("requestType", "RPC")
                put("sceneCode", sceneCode ?: "null")
                put("source", "ANT_FOREST")
                put("taskType", taskType ?: "null")
                put("uniqueId", getUniqueId())
            }
        )
    }

    @JvmStatic
    suspend fun finishTask(sceneCode: String?, taskType: String?): String {
        val outBizNo = taskType + "_" + RandomUtil.nextDouble()
        return RequestManager.requestString(
            "com.alipay.antiep.finishTask",
            RpcRequestData.array {
                put("outBizNo", outBizNo)
                put("requestType", "RPC")
                put("sceneCode", sceneCode ?: "null")
                put("source", "ANTFOCEAN")
                put("taskType", taskType ?: "null")
                put("uniqueId", getUniqueId())
            }
        )
    }

    @JvmStatic
    suspend fun unLockReplicaPhase(replicaCode: String?, replicaPhaseCode: String?): String {
        return RequestManager.requestString(
            "alipay.antocean.ocean.h5.unLockReplicaPhase",
            RpcRequestData.array {
                put("replicaCode", replicaCode ?: "null")
                put("replicaPhaseCode", replicaPhaseCode ?: "null")
                put("source", "senlinzuoshangjiao")
                put("uniqueId", getUniqueId())
                put("version", "20220707")
            }
        )
    }

    @JvmStatic
    suspend fun queryReplicaHome(): String {
        return RequestManager.requestString(
            "alipay.antocean.ocean.h5.queryReplicaHome",
            RpcRequestData.array {
                put("replicaCode", "avatar")
                put("source", "senlinzuoshangjiao")
                put("uniqueId", getUniqueId())
            }
        )
    }

    @JvmStatic
    suspend fun repairSeaArea(): String {
        return RequestManager.requestString(
            "alipay.antocean.ocean.h5.repairSeaArea",
            RpcRequestData.array {
                put("source", "chInfo_ch_appcenter__chsub_9patch")
                put("uniqueId", getUniqueId())
            }
        )
    }

    @JvmStatic
    suspend fun queryOceanPropList(): String {
        return RequestManager.requestString(
            "alipay.antocean.ocean.h5.queryOceanPropList",
            RpcRequestData.array {
                put("propTypeList", "UNIVERSAL_PIECE")
                put("skipPropId", false)
                put("source", "chInfo_ch_appcenter__chsub_9patch")
                put("uniqueId", getUniqueId())
            }
        )
    }

    @JvmStatic
    suspend fun querySeaAreaDetailList(): String {
        return RequestManager.requestString(
            "alipay.antocean.ocean.h5.querySeaAreaDetailList",
            RpcRequestData.array {
                put("seaAreaCode", "")
                put("source", "chInfo_ch_appcenter__chsub_9patch")
                put("targetUserId", "")
                put("uniqueId", getUniqueId())
            }
        )
    }

    @JvmStatic
    suspend fun queryOceanChapterList(): String {
        return RequestManager.requestString(
            "alipay.antocean.ocean.h5.queryOceanChapterList",
            RpcRequestData.array {
                put("source", "chInfo_ch_url-https://2021003115672468.h5app.alipay.com/www/atlasOcean.html")
                put("uniqueId", getUniqueId())
            }
        )
    }

    @JvmStatic
    suspend fun switchOceanChapter(chapterCode: String?): String {
        return RequestManager.requestString(
            "alipay.antocean.ocean.h5.switchOceanChapter",
            RpcRequestData.array {
                put("chapterCode", chapterCode ?: "null")
                put("source", "chInfo_ch_url-https://2021003115672468.h5app.alipay.com/www/atlasOcean.html")
                put("uniqueId", getUniqueId())
            }
        )
    }

    @JvmStatic
    suspend fun queryMiscInfo(): String {
        return RequestManager.requestString(
            "alipay.antocean.ocean.h5.queryMiscInfo",
            RpcRequestData.array {
                put("queryBizTypes", JSONArray().put("HOME_TIPS_REFRESH"))
                put("source", "chInfo_ch_appcenter__chsub_9patch")
                put("uniqueId", getUniqueId())
            }
        )
    }

    @JvmStatic
    suspend fun combineFish(fishId: String?): String {
        return RequestManager.requestString(
            "alipay.antocean.ocean.h5.combineFish",
            RpcRequestData.array {
                put("fishId", fishId ?: "null")
                put("source", "ANT_FOREST")
                put("uniqueId", getUniqueId())
            }
        )
    }

    @JvmStatic
    suspend fun collectEnergy(bubbleId: String?, userId: String?): String {
        return RequestManager.requestString(
            "alipay.antmember.forest.h5.collectEnergy",
            RpcRequestData.array {
                // bubbleId 原为无引号插值（数字形态），toBigDecimalOrNull 保持数字类型（兼容整数与小数）；null 时保持 null
                put("bubbleIds", JSONArray().put(bubbleId?.toBigDecimalOrNull() ?: JSONObject.NULL))
                put("channel", "ocean")
                put("source", "ANT_FOREST")
                put("uniqueId", getUniqueId())
                put("userId", userId ?: "null")
                put("version", VERSION)
            }
        )
    }

    @JvmStatic
    suspend fun cleanFriendOcean(userId: String?): String {
        return RequestManager.requestString(
            "alipay.antocean.ocean.h5.cleanFriendOcean",
            RpcRequestData.array {
                put("cleanedUserId", userId ?: "null")
                put("source", "ANT_FOREST")
                put("uniqueId", getUniqueId())
            }
        )
    }

    @JvmStatic
    suspend fun queryFriendPage(userId: String?): String {
        return RequestManager.requestString(
            "alipay.antocean.ocean.h5.queryFriendPage",
            RpcRequestData.array {
                put("friendUserId", userId ?: "null")
                put("interactFlags", "T")
                put("source", "ANT_FOREST")
                put("uniqueId", getUniqueId())
                put("version", VERSION)
            }
        )
    }

    @JvmStatic
    suspend fun queryUserRanking(): String {
        return RequestManager.requestString(
            "alipay.antocean.ocean.h5.queryUserRanking",
            RpcRequestData.array {
                put("source", "ANT_FOREST")
                put("uniqueId", getUniqueId())
            }
        )
    }

    /* 保护海洋净滩行动 */
    @JvmStatic
    suspend fun queryCultivationList(): String {
        return RequestManager.requestString(
            "alipay.antocean.ocean.h5.queryCultivationList",
            RpcRequestData.array {
                put("source", "ANT_FOREST")
                put("version", "20231031")
            }
        )
    }

    @JvmStatic
    suspend fun queryCultivationDetail(cultivationCode: String?, projectCode: String?): String {
        return RequestManager.requestString(
            "alipay.antocean.ocean.h5.queryCultivationDetail",
            RpcRequestData.array {
                put("cultivationCode", cultivationCode ?: "null")
                put("projectCode", projectCode ?: "null")
                put("source", "ANT_FOREST")
                put("uniqueId", getUniqueId())
            }
        )
    }

    @JvmStatic
    suspend fun oceanExchangeTree(cultivationCode: String?, projectCode: String?): String {
        return RequestManager.requestString(
            "alipay.antocean.ocean.h5.exchangeTree",
            RpcRequestData.array {
                put("cultivationCode", cultivationCode ?: "null")
                put("projectCode", projectCode ?: "null")
                put("source", "ANT_FOREST")
                put("uniqueId", getUniqueId())
            }
        )
    }

    // 答题
    @JvmStatic
    suspend fun getQuestion(): String {
        return RequestManager.requestString(
            "com.alipay.reading.game.dada.openDailyAnswer.getQuestion",
            RpcRequestData.array {
                put("activityId", "363")
                put("dadaVersion", "1.3.0")
                // 协议约束 version 必为字符串，原字面量数字 1 修正为 "1"
                put("version", "1")
            }
        )
    }

    @JvmStatic
    suspend fun record(): String {
        return RequestManager.requestString(
            "com.alipay.reading.game.dada.mdap.record",
            RpcRequestData.array {
                put("behavior", "visit")
                put("dadaVersion", "1.3.0")
                put("version", "1")
            }
        )
    }

    @JvmStatic
    suspend fun submitAnswer(answer: String?, questionId: String?): String {
        return RequestManager.requestString(
            "com.alipay.reading.game.dada.openDailyAnswer.submitAnswer",
            RpcRequestData.array {
                put("activityId", "363")
                put("answer", answer ?: "null")
                put("dadaVersion", "1.3.0")
                put("outBizId", "ANTOCEAN_DATI_PINTU_722_new")
                put("questionId", questionId ?: "null")
                put("version", "1")
            }
        )
    }

    // 潘多拉任务
    @JvmStatic
    suspend fun PDLqueryReplicaHome(): String {
        return RequestManager.requestString(
            "alipay.antocean.ocean.h5.queryReplicaHome",
            RpcRequestData.array {
                put("replicaCode", "avatar")
                put("source", "seaAreaList")
                put("uniqueId", getUniqueId())
            }
        )
    }

    @JvmStatic
    suspend fun queryTaskList(): String {
        return RequestManager.requestString(
            "alipay.antocean.ocean.h5.queryTaskList",
            RpcRequestData.array {
                put("extend", JSONObject())
                put("fromAct", "dynamic_task")
                put("sceneCode", "ANTOCEAN_TASK")
                put("source", "ANT_FOREST")
                put("uniqueId", getUniqueId())
                put("version", VERSION)
            }
        )
    }

    @JvmStatic
    suspend fun PDLqueryTaskList(): String {
        return RequestManager.requestString(
            "alipay.antocean.ocean.h5.queryTaskList",
            RpcRequestData.array {
                put("fromAct", "dynamic_task")
                put("sceneCode", "ANTOCEAN_AVATAR_TASK")
                put("source", "seaAreaList")
                put("uniqueId", getUniqueId())
                put("version", VERSION)
            }
        )
    }

    @JvmStatic
    suspend fun PDLreceiveTaskAward(taskType: String?): String {
        return RequestManager.requestString(
            "com.alipay.antiep.receiveTaskAward",
            RpcRequestData.array {
                put("ignoreLimit", "false")
                put("requestType", "RPC")
                put("sceneCode", "ANTOCEAN_AVATAR_TASK")
                put("source", "ANTFOCEAN")
                put("taskType", taskType ?: "null")
                put("uniqueId", getUniqueId())
            }
        )
    }

    // 制作万能拼图
    @JvmStatic
    suspend fun exchangePropList(): String {
        return RequestManager.requestString(
            "alipay.antocean.ocean.h5.queryOceanPropList",
            RpcRequestData.array {
                put("skipPropId", false)
                put("source", "ANT_FOREST")
                put("uniqueId", getUniqueId())
            }
        )
    }

    @JvmStatic
    suspend fun exchangeProp(): String {
        val timestamp = System.currentTimeMillis()
        return RequestManager.requestString(
            "alipay.antocean.ocean.h5.exchangeProp",
            RpcRequestData.array {
                put("bizNo", timestamp.toString())
                put("exchangeNum", "1")
                put("propCode", "UNIVERSAL_PIECE")
                put("propType", "UNIVERSAL_PIECE")
                put("source", "ANT_FOREST")
                put("uniqueId", getUniqueId())
            }
        )
    }

    // 使用万能拼图
    @JvmStatic
    suspend fun usePropByTypeList(): String {
        return RequestManager.requestString(
            "alipay.antocean.ocean.h5.queryOceanPropList",
            RpcRequestData.array {
                put("propTypeList", "UNIVERSAL_PIECE")
                put("skipPropId", false)
                put("source", "chInfo_ch_appcenter__chsub_9patch")
                put("uniqueId", getUniqueId())
            }
        )
    }

    @JvmStatic
    suspend fun queryFishList(pageNum: Int): String {
        return RequestManager.requestString(
            "alipay.antocean.ocean.h5.queryFishList",
            RpcRequestData.array {
                put("combineStatus", "UNOBTAINED")
                put("needSummary", "Y")
                put("pageNum", pageNum)
                put("source", "chInfo_ch_appcenter__chsub_9patch")
                put("targetUserId", "")
                put("uniqueId", getUniqueId())
            }
        )
    }

    @JvmStatic
    suspend fun usePropByType(assets: Int, attachAssetsSet: Set<Int>?): String? {
        try {
            if (!attachAssetsSet.isNullOrEmpty()) {
                val jsonArray = JSONArray()
                for (attachAssets in attachAssetsSet) {
                    val jsonObject = JSONObject()
                    jsonObject.put("assets", assets)
                    jsonObject.put("assetsNum", 1)
                    jsonObject.put("attachAssets", attachAssets)
                    jsonObject.put("propCode", "UNIVERSAL_PIECE")
                    jsonArray.put(jsonObject)
                }
                return RequestManager.requestString(
                    "alipay.antocean.ocean.h5.usePropByType",
                    RpcRequestData.array {
                        put("assetsDetails", jsonArray)
                        put("propCode", "UNIVERSAL_PIECE")
                        put("propType", "UNIVERSAL_PIECE")
                        put("source", "chInfo_ch_appcenter__chsub_9patch")
                        put("uniqueId", getUniqueId())
                    }
                )
            }
        } catch (e: JSONException) {
            Log.printStackTrace(e)
        }
        return null
    }

    /**
     * 限时挑战
     */
    @JvmStatic
    suspend fun createSeaAreaExtraCollect(): String {
        try {
            return RequestManager.requestString(
                "alipay.antocean.ocean.h5.createSeaAreaExtraCollect",
                RpcRequestData.array {
                    put("source", "chInfo_ch_appcenter__chsub_9patch")
                    put("uniqueId", getUniqueId())
                }
            )
        } catch (e: Exception) {
            Log.printStackTrace("AntOceanRpcCall", e)
            return "{}"
        }
    }

}
