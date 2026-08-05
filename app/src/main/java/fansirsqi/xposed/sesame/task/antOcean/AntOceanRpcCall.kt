package fansirsqi.xposed.sesame.task.antOcean

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import fansirsqi.xposed.sesame.hook.RequestManager
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.RandomUtil

/**
 * @author Constanline
 * @since 2023/08/01
 */
object AntOceanRpcCall {
    private const val VERSION = "20241203"

    private fun getUniqueId(): String {
        return System.currentTimeMillis().toString() + RandomUtil.nextLong()
    }

    @JvmStatic
    fun queryOceanStatus(): String {
        return RequestManager.requestString(
            "alipay.antocean.ocean.h5.queryOceanStatus",
            "[{\"source\":\"chInfo_ch_appcenter__chsub_9patch\"}]"
        )
    }

    @JvmStatic
    fun queryHomePage(): String {
        return RequestManager.requestString(
            "alipay.antocean.ocean.h5.queryHomePage",
            "[{\"source\":\"ANT_FOREST\",\"uniqueId\":\"${getUniqueId()}\",\"version\":\"$VERSION\"}]"
        )
    }

    @JvmStatic
    fun cleanOcean(userId: String?): String {
        return RequestManager.requestString(
            "alipay.antocean.ocean.h5.cleanOcean",
            "[{\"cleanedUserId\":\"$userId\",\"source\":\"ANT_FOREST\",\"uniqueId\":\"${getUniqueId()}\"}]"
        )
    }

    @JvmStatic
    fun ipOpenSurprise(): String {
        return RequestManager.requestString(
            "alipay.antocean.ocean.h5.ipOpenSurprise",
            "[{\"source\":\"chInfo_ch_appcenter__chsub_9patch\",\"uniqueId\":\"${getUniqueId()}\"}]"
        )
    }

    @JvmStatic
    fun collectReplicaAsset(): String {
        return RequestManager.requestString(
            "alipay.antocean.ocean.h5.collectReplicaAsset",
            "[{\"replicaCode\":\"avatar\",\"source\":\"senlinzuoshangjiao\",\"uniqueId\":\"${getUniqueId()}\",\"version\":\"$VERSION\"}]"
        )
    }

    @JvmStatic
    fun receiveTaskAward(sceneCode: String?, taskType: String?): String {
        return RequestManager.requestString(
            "com.alipay.antiep.receiveTaskAward",
            "[{\"ignoreLimit\":false,\"requestType\":\"RPC\",\"sceneCode\":\"$sceneCode\",\"source\":\"ANT_FOREST\",\"taskType\":\"$taskType\",\"uniqueId\":\"${getUniqueId()}\"}]"
        )
    }

    @JvmStatic
    fun finishTask(sceneCode: String?, taskType: String?): String {
        val outBizNo = taskType + "_" + RandomUtil.nextDouble()
        return RequestManager.requestString(
            "com.alipay.antiep.finishTask",
            "[{\"outBizNo\":\"$outBizNo\",\"requestType\":\"RPC\",\"sceneCode\":\"$sceneCode\",\"source\":\"ANTFOCEAN\",\"taskType\":\"$taskType\",\"uniqueId\":\"${getUniqueId()}\"}]"
        )
    }

    @JvmStatic
    fun unLockReplicaPhase(replicaCode: String?, replicaPhaseCode: String?): String {
        return RequestManager.requestString(
            "alipay.antocean.ocean.h5.unLockReplicaPhase",
            "[{\"replicaCode\":\"$replicaCode\",\"replicaPhaseCode\":\"$replicaPhaseCode\",\"source\":\"senlinzuoshangjiao\",\"uniqueId\":\"${getUniqueId()}\",\"version\":\"20220707\"}]"
        )
    }

    @JvmStatic
    fun queryReplicaHome(): String {
        return RequestManager.requestString(
            "alipay.antocean.ocean.h5.queryReplicaHome",
            "[{\"replicaCode\":\"avatar\",\"source\":\"senlinzuoshangjiao\",\"uniqueId\":\"${getUniqueId()}\"}]"
        )
    }

    @JvmStatic
    fun repairSeaArea(): String {
        return RequestManager.requestString(
            "alipay.antocean.ocean.h5.repairSeaArea",
            "[{\"source\":\"chInfo_ch_appcenter__chsub_9patch\",\"uniqueId\":\"${getUniqueId()}\"}]"
        )
    }

    @JvmStatic
    fun queryOceanPropList(): String {
        return RequestManager.requestString(
            "alipay.antocean.ocean.h5.queryOceanPropList",
            "[{\"propTypeList\":\"UNIVERSAL_PIECE\",\"skipPropId\":false,\"source\":\"chInfo_ch_appcenter__chsub_9patch\",\"uniqueId\":\"${getUniqueId()}\"}]"
        )
    }

    @JvmStatic
    fun querySeaAreaDetailList(): String {
        return RequestManager.requestString(
            "alipay.antocean.ocean.h5.querySeaAreaDetailList",
            "[{\"seaAreaCode\":\"\",\"source\":\"chInfo_ch_appcenter__chsub_9patch\",\"targetUserId\":\"\",\"uniqueId\":\"${getUniqueId()}\"}]"
        )
    }

    @JvmStatic
    fun queryOceanChapterList(): String {
        return RequestManager.requestString(
            "alipay.antocean.ocean.h5.queryOceanChapterList",
            "[{\"source\":\"chInfo_ch_url-https://2021003115672468.h5app.alipay.com/www/atlasOcean.html\",\"uniqueId\":\"${getUniqueId()}\"}]"
        )
    }

    @JvmStatic
    fun switchOceanChapter(chapterCode: String?): String {
        return RequestManager.requestString(
            "alipay.antocean.ocean.h5.switchOceanChapter",
            "[{\"chapterCode\":\"$chapterCode\",\"source\":\"chInfo_ch_url-https://2021003115672468.h5app.alipay.com/www/atlasOcean.html\",\"uniqueId\":\"${getUniqueId()}\"}]"
        )
    }

    @JvmStatic
    fun queryMiscInfo(): String {
        return RequestManager.requestString(
            "alipay.antocean.ocean.h5.queryMiscInfo",
            "[{\"queryBizTypes\":[\"HOME_TIPS_REFRESH\"],\"source\":\"chInfo_ch_appcenter__chsub_9patch\",\"uniqueId\":\"${getUniqueId()}\"}]"
        )
    }

    @JvmStatic
    fun combineFish(fishId: String?): String {
        return RequestManager.requestString(
            "alipay.antocean.ocean.h5.combineFish",
            "[{\"fishId\":\"$fishId\",\"source\":\"ANT_FOREST\",\"uniqueId\":\"${getUniqueId()}\"}]"
        )
    }

    @JvmStatic
    fun collectEnergy(bubbleId: String?, userId: String?): String {
        return RequestManager.requestString(
            "alipay.antmember.forest.h5.collectEnergy",
            "[{\"bubbleIds\":[$bubbleId],\"channel\":\"ocean\",\"source\":\"ANT_FOREST\",\"uniqueId\":\"${getUniqueId()}\",\"userId\":\"$userId\",\"version\":\"$VERSION\"}]"
        )
    }

    @JvmStatic
    fun cleanFriendOcean(userId: String?): String {
        return RequestManager.requestString(
            "alipay.antocean.ocean.h5.cleanFriendOcean",
            "[{\"cleanedUserId\":\"$userId\",\"source\":\"ANT_FOREST\",\"uniqueId\":\"${getUniqueId()}\"}]"
        )
    }

    @JvmStatic
    fun queryFriendPage(userId: String?): String {
        return RequestManager.requestString(
            "alipay.antocean.ocean.h5.queryFriendPage",
            "[{\"friendUserId\":\"$userId\",\"interactFlags\":\"T\",\"source\":\"ANT_FOREST\",\"uniqueId\":\"${getUniqueId()}\",\"version\":\"$VERSION\"}]"
        )
    }

    @JvmStatic
    fun queryUserRanking(): String {
        return RequestManager.requestString(
            "alipay.antocean.ocean.h5.queryUserRanking",
            "[{\"source\":\"ANT_FOREST\",\"uniqueId\":\"${getUniqueId()}\"}]"
        )
    }

    /* 保护海洋净滩行动 */
    @JvmStatic
    fun queryCultivationList(): String {
        return RequestManager.requestString(
            "alipay.antocean.ocean.h5.queryCultivationList",
            "[{\"source\":\"ANT_FOREST\",\"version\":\"20231031\"}]"
        )
    }

    @JvmStatic
    fun queryCultivationDetail(cultivationCode: String?, projectCode: String?): String {
        return RequestManager.requestString(
            "alipay.antocean.ocean.h5.queryCultivationDetail",
            "[{\"cultivationCode\":\"$cultivationCode\",\"projectCode\":\"$projectCode\",\"source\":\"ANT_FOREST\",\"uniqueId\":\"${getUniqueId()}\"}]"
        )
    }

    @JvmStatic
    fun oceanExchangeTree(cultivationCode: String?, projectCode: String?): String {
        return RequestManager.requestString(
            "alipay.antocean.ocean.h5.exchangeTree",
            "[{\"cultivationCode\":\"$cultivationCode\",\"projectCode\":\"$projectCode\",\"source\":\"ANT_FOREST\",\"uniqueId\":\"${getUniqueId()}\"}]"
        )
    }

    // 答题
    @JvmStatic
    fun getQuestion(): String {
        return RequestManager.requestString(
            "com.alipay.reading.game.dada.openDailyAnswer.getQuestion",
            "[{\"activityId\":\"363\",\"dadaVersion\":\"1.3.0\",\"version\":1}]"
        )
    }

    @JvmStatic
    fun record(): String {
        return RequestManager.requestString(
            "com.alipay.reading.game.dada.mdap.record",
            "[{\"behavior\":\"visit\",\"dadaVersion\":\"1.3.0\",\"version\":\"1\"}]"
        )
    }

    @JvmStatic
    fun submitAnswer(answer: String?, questionId: String?): String {
        return RequestManager.requestString(
            "com.alipay.reading.game.dada.openDailyAnswer.submitAnswer",
            "[{\"activityId\":\"363\",\"answer\":\"$answer\",\"dadaVersion\":\"1.3.0\",\"outBizId\":\"ANTOCEAN_DATI_PINTU_722_new\",\"questionId\":\"$questionId\",\"version\":\"1\"}]"
        )
    }

    // 潘多拉任务
    @JvmStatic
    fun PDLqueryReplicaHome(): String {
        return RequestManager.requestString(
            "alipay.antocean.ocean.h5.queryReplicaHome",
            "[{\"replicaCode\":\"avatar\",\"source\":\"seaAreaList\",\"uniqueId\":\"${getUniqueId()}\"}]"
        )
    }

    @JvmStatic
    fun queryTaskList(): String {
        return RequestManager.requestString(
            "alipay.antocean.ocean.h5.queryTaskList",
            "[{\"extend\":{},\"fromAct\":\"dynamic_task\",\"sceneCode\":\"ANTOCEAN_TASK\",\"source\":\"ANT_FOREST\",\"uniqueId\":\"${getUniqueId()}\",\"version\":\"$VERSION\"}]"
        )
    }

    @JvmStatic
    fun PDLqueryTaskList(): String {
        return RequestManager.requestString(
            "alipay.antocean.ocean.h5.queryTaskList",
            "[{\"fromAct\":\"dynamic_task\",\"sceneCode\":\"ANTOCEAN_AVATAR_TASK\",\"source\":\"seaAreaList\",\"uniqueId\":\"${getUniqueId()}\",\"version\":\"$VERSION\"}]"
        )
    }

    @JvmStatic
    fun PDLreceiveTaskAward(taskType: String?): String {
        return RequestManager.requestString(
            "com.alipay.antiep.receiveTaskAward",
            "[{\"ignoreLimit\":\"false\",\"requestType\":\"RPC\",\"sceneCode\":\"ANTOCEAN_AVATAR_TASK\",\"source\":\"ANTFOCEAN\",\"taskType\":\"$taskType\",\"uniqueId\":\"${getUniqueId()}\"}]"
        )
    }

    // 制作万能拼图
    @JvmStatic
    fun exchangePropList(): String {
        return RequestManager.requestString(
            "alipay.antocean.ocean.h5.queryOceanPropList",
            "[{\"skipPropId\":false,\"source\":\"ANT_FOREST\",\"uniqueId\":\"${getUniqueId()}\"}]"
        )
    }

    @JvmStatic
    fun exchangeProp(): String {
        val timestamp = System.currentTimeMillis()
        return RequestManager.requestString(
            "alipay.antocean.ocean.h5.exchangeProp",
            "[{\"bizNo\":\"$timestamp\",\"exchangeNum\":\"1\",\"propCode\":\"UNIVERSAL_PIECE\",\"propType\":\"UNIVERSAL_PIECE\",\"source\":\"ANT_FOREST\",\"uniqueId\":\"${getUniqueId()}\"}]"
        )
    }

    // 使用万能拼图
    @JvmStatic
    fun usePropByTypeList(): String {
        return RequestManager.requestString(
            "alipay.antocean.ocean.h5.queryOceanPropList",
            "[{\"propTypeList\":\"UNIVERSAL_PIECE\",\"skipPropId\":false,\"source\":\"chInfo_ch_appcenter__chsub_9patch\",\"uniqueId\":\"${getUniqueId()}\"}]"
        )
    }

    @JvmStatic
    fun queryFishList(pageNum: Int): String {
        return RequestManager.requestString(
            "alipay.antocean.ocean.h5.queryFishList",
            "[{\"combineStatus\":\"UNOBTAINED\",\"needSummary\":\"Y\",\"pageNum\":$pageNum,\"source\":\"chInfo_ch_appcenter__chsub_9patch\",\"targetUserId\":\"\",\"uniqueId\":\"${getUniqueId()}\"}]"
        )
    }

    @JvmStatic
    fun usePropByType(assets: Int, attachAssetsSet: Set<Int>?): String? {
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
                    "[{\"assetsDetails\":$jsonArray,\"propCode\":\"UNIVERSAL_PIECE\",\"propType\":\"UNIVERSAL_PIECE\",\"source\":\"chInfo_ch_appcenter__chsub_9patch\",\"uniqueId\":\"${getUniqueId()}\"}]"
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
    fun createSeaAreaExtraCollect(): String {
        try {
            val param = JSONObject()
            param.put("source", "chInfo_ch_appcenter__chsub_9patch")
            param.put("uniqueId", getUniqueId())

            val params = JSONArray()
            params.put(param)
            return RequestManager.requestString(
                "alipay.antocean.ocean.h5.createSeaAreaExtraCollect",
                params.toString()
            )
        } catch (e: Exception) {
            Log.printStackTrace("AntOceanRpcCall", e)
            return "{}"
        }
    }
}
