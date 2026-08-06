package fansirsqi.xposed.sesame.task.antForest

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import fansirsqi.xposed.sesame.entity.AlipayVersion
import fansirsqi.xposed.sesame.entity.RpcEntity
import fansirsqi.xposed.sesame.hook.ApplicationHook
import fansirsqi.xposed.sesame.hook.RequestManager
import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.util.RandomUtil
import java.util.UUID

/** 森林 RPC 调用类 */
object AntForestRpcCall {
    private var VERSION = "20250813"

    @JvmStatic
    fun init() {
        val alipayVersion: AlipayVersion = ApplicationHook.alipayVersion
        Log.record("AntForestRpcCall", "当前目标应用版本: $alipayVersion")
        try {
            when (alipayVersion.versionString) {
                "10.7.30.8000" ->
                    VERSION = "20250813" // 2025年版本
                "10.5.88.8000" ->
                    VERSION = "20240403" // 2024年版本
                "10.3.96.8100" ->
                    VERSION = "20230501" // 2023年版本
                else -> VERSION = "20250813"
            }
            Log.record("AntForestRpcCall", "使用API版本: $VERSION")
        } catch (e: Exception) {
            Log.error("AntForestRpcCall", "版本初始化异常，使用默认版本: $VERSION")
            Log.printStackTrace(e)
        }
    }

    @JvmStatic
    fun queryFriendsEnergyRanking(): String {
        try {
            val arg = JSONObject()
            arg.put("source", "chInfo_ch_appcenter__chsub_9patch")
            arg.put("periodType", "total")
            arg.put("rankType", "energyRank")
            arg.put("version", VERSION)
            val param = "[" + arg + "]"
            val correlationLocal = JSONObject()
            correlationLocal.put("pathList", JSONArray().put("friendRanking").put("myself").put("totalDatas"))
            val relationLocal = "[" + correlationLocal + "]"
            return RequestManager.requestString("alipay.antmember.forest.h5.queryEnergyRanking", param, relationLocal)
        } catch (e: Exception) {
            return ""
        }
    }

    @JvmStatic
    fun queryTopEnergyChallengeRanking(): String {
        try {
            val arg = JSONObject()
            arg.put("source", "chInfo_ch_appcenter__chsub_9patch")
            val param = "[" + arg + "]"
            return RequestManager.requestString("alipay.antforest.forest.h5.queryTopEnergyChallengeRanking", param)
        } catch (e: Exception) {
            Log.printStackTrace(e)
            return ""
        }
    }

    /** 批量获取好友能量信息（标准版） */
    @JvmStatic
    fun fillUserRobFlag(userIdList: JSONArray?): String {
        try {
            val arg = JSONObject()
            arg.put("source", "chInfo_ch_appcenter__chsub_9patch")
            arg.put("userIdList", userIdList)
            val param = "[" + arg + "]"
            val joRelationLocal = JSONObject()
            joRelationLocal.put("pathList", JSONArray().put("friendRanking"))
            val relationLocal = "[" + joRelationLocal + "]"
            return RequestManager.requestString("alipay.antforest.forest.h5.fillUserRobFlag", param, relationLocal)
        } catch (e: Exception) {
            return ""
        }
    }

    /** 批量获取好友能量信息（增强版 - PK排行榜专用） */
    @JvmStatic
    fun fillUserRobFlag(userIdList: JSONArray?, needFillUserInfo: Boolean): String {
        try {
            val arg = JSONObject()
            arg.put("source", "chInfo_ch_appcenter__chsub_9patch")
            arg.put("userIdList", userIdList)
            arg.put("needFillUserInfo", needFillUserInfo)
            val param = "[" + arg + "]"
            return RequestManager.requestString("alipay.antforest.forest.h5.fillUserRobFlag", param)
        } catch (e: Exception) {
            return ""
        }
    }

    @JvmStatic
    @Throws(JSONException::class)
    fun queryHomePage(): String {
        val requestObject = JSONObject()
            .put("activityParam", JSONObject())
            .put("configVersionMap", JSONObject().put("wateringBubbleConfig", "0"))
            .put("skipWhackMole", false)
            .put("source", "chInfo_ch_appcenter__chsub_9patch")
            .put("version", VERSION)
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.queryHomePage",
            JSONArray().put(requestObject).toString(),
            3,
            1000
        )
    }

    @JvmStatic
    fun queryFriendHomePage(userId: String?, fromAct: String?): String {
        try {
            var fromAct = fromAct
            if (fromAct == null) {
                fromAct = "TAKE_LOOK_FRIEND"
            }
            val arg = JSONObject()
            val arg1 = JSONObject()
            arg1.put("wateringBubbleConfig", "0")
            arg.put("canRobFlags", "T,F,F,F,F")
            arg.put("configVersionMap", arg1)
            arg.put("source", "chInfo_ch_appcenter__chsub_9patch")
            arg.put("userId", userId)
            arg.put("fromAct", fromAct)
            arg.put("version", VERSION)
            val param = "[" + arg + "]"
            return RequestManager.requestString("alipay.antforest.forest.h5.queryFriendHomePage", param, 3, 1000)
        } catch (e: Exception) {
            Log.printStackTrace(e)
            return ""
        }
    }

    /** 找能量方法 - 查找可收取能量的好友（带跳过用户列表） */
    @JvmStatic
    fun takeLook(skipUsers: JSONObject?): String {
        try {
            val requestData = JSONObject()
            requestData.put("contactsStatus", "N")
            requestData.put("exposedUserId", "")
            requestData.put("skipUsers", skipUsers)
            requestData.put("source", "chInfo_ch_appcenter__chsub_9patch")
            requestData.put("takeLookEnd", false)
            requestData.put("takeLookStart", true)
            requestData.put("version", VERSION)
            return RequestManager.requestString(
                "alipay.antforest.forest.h5.takeLook",
                "[" + requestData + "]"
            )
        } catch (e: JSONException) {
            Log.printStackTrace("AntForestRpcCall", "takeLook构建请求参数失败", e)
            return ""
        }
    }

    @JvmStatic
    fun energyRpcEntity(bizType: String?, userId: String?, bubbleId: Long): RpcEntity? {
        try {
            val args = JSONObject()
            val bubbleIds = JSONArray()
            bubbleIds.put(bubbleId)
            args.put("bizType", bizType)
            args.put("bubbleIds", bubbleIds)
            args.put("source", "chInfo_ch_appcenter__chsub_9patch")
            args.put("userId", userId)
            args.put("version", VERSION)
            val param = "[" + args + "]"
            return RpcEntity("alipay.antmember.forest.h5.collectEnergy", param, null)
        } catch (e: Exception) {
            Log.printStackTrace(e)
            return null
        }
    }

    @JvmStatic
    fun collectEnergy(bizType: String?, userId: String?, bubbleId: Long?): String {
        val r = energyRpcEntity(bizType, userId, bubbleId!!)
        return r?.let { RequestManager.requestString(it) } ?: ""
    }

    @JvmStatic
    @Throws(JSONException::class)
    fun batchEnergyRpcEntity(bizType: String?, userId: String?, bubbleIds: List<Long?>?): RpcEntity {
        val arg = JSONObject()
        arg.put("bizType", bizType)
        arg.put("bubbleIds", JSONArray(bubbleIds))
        arg.put("fromAct", "BATCH_ROB_ENERGY")
        arg.put("source", "chInfo_ch_appcenter__chsub_9patch")
        arg.put("userId", userId)
        arg.put("version", VERSION)
        val param = "[" + arg + "]"
        return RpcEntity("alipay.antmember.forest.h5.collectEnergy", param)
    }

    /** 收取复活能量 */
    @JvmStatic
    fun collectRebornEnergy(): String {
        try {
            val arg = JSONObject()
            arg.put("source", "chInfo_ch_appcenter__chsub_9patch")
            val param = "[" + arg + "]"
            return RequestManager.requestString("alipay.antforest.forest.h5.collectRebornEnergy", param)
        } catch (e: Exception) {
            Log.printStackTrace(e)
            return ""
        }
    }

    @JvmStatic
    fun transferEnergy(targetUser: String?, bizNo: String, energyId: Int, notifyFriend: Boolean): String {
        try {
            val arg = JSONObject()
            arg.put("bizNo", bizNo + UUID.randomUUID().toString())
            arg.put("energyId", energyId)
            arg.put("extInfo", JSONObject().put("sendChat", if (notifyFriend) "Y" else "N"))
            arg.put("from", "friendIndex")
            arg.put("source", "chInfo_ch_appcenter__chsub_9patch")
            arg.put("targetUser", targetUser)
            arg.put("transferType", "WATERING")
            arg.put("version", VERSION)
            val param = "[" + arg + "]"
            return RequestManager.requestString("alipay.antmember.forest.h5.transferEnergy", param)
        } catch (e: Exception) {
            Log.printStackTrace(e)
            return ""
        }
    }

    @JvmStatic
    fun queryEnergyRainHome(): String {
        return RequestManager.requestString("alipay.antforest.forest.h5.queryEnergyRainHome", "[{\"source\":\"senlinguangchuangrukou\",\"version\":\"" + VERSION + "\"}]")
    }

    @JvmStatic
    fun queryEnergyRainCanGrantList(): String {
        return RequestManager.requestString("alipay.antforest.forest.h5.queryEnergyRainCanGrantList", "[{}]")
    }

    @JvmStatic
    fun grantEnergyRainChance(targetUserId: String?): String {
        return RequestManager.requestString("alipay.antforest.forest.h5.grantEnergyRainChance", "[{\"targetUserId\":" + targetUserId + "}]")
    }

    @JvmStatic
    fun startEnergyRain(): String {
        return RequestManager.requestString("alipay.antforest.forest.h5.startEnergyRain", "[{\"version\":\"" + VERSION + "\"}]")
    }

    @JvmStatic
    fun energyRainSettlement(saveEnergy: Int, token: String?): String {
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.energyRainSettlement",
            "[{\"activityPropNums\":0,\"saveEnergy\":" + saveEnergy + ",\"token\":\"" + token + "\",\"version\":\"" + VERSION + "\"}]"
        )
    }

    /** 查询能量雨/游戏结束列表奖励 */
    @JvmStatic
    fun queryEnergyRainEndGameList(): String {
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.queryEnergyRainEndGameList",
            "[ {} ]"
        )
    }

    @JvmStatic
    @Throws(JSONException::class)
    fun queryTaskList(): String {
        val jo = JSONObject()
        jo.put("extend", JSONObject())
        jo.put("fromAct", "home_task_list")
        jo.put("source", "chInfo_ch_appcenter__chsub_9patch")
        jo.put("version", VERSION)
        return RequestManager.requestString("alipay.antforest.forest.h5.queryTaskList", JSONArray().put(jo).toString())
    }

    /*青春特权道具任务状态查询🔍*/
    @JvmStatic
    @Throws(JSONException::class)
    fun queryTaskListV2(firstTaskType: String): String {
        val jo = JSONObject()
        val extend = JSONObject()
        extend.put("firstTaskType", firstTaskType) // DNHZ_SL_college,DXS_BHZ，DXS_JSQ
        jo.put("extend", extend)
        jo.put("fromAct", "home_task_list")
        if (firstTaskType == "DNHZ_SL_college") {
            jo.put("source", firstTaskType)
        }
        if (firstTaskType == "DXS_BHZ" || firstTaskType == "DXS_JSQ") {
            jo.put("source", "202212TJBRW")
        }
        jo.put("version", VERSION)
        return RequestManager.requestString("alipay.antforest.forest.h5.queryTaskList", JSONArray().put(jo).toString())
    }

    @JvmStatic
    @Throws(JSONException::class)
    fun receiveTaskAward(sceneCode: String?, taskType: String?): String {
        val jo = JSONObject()
        jo.put("ignoreLimit", false)
        jo.put("requestType", "H5")
        jo.put("sceneCode", sceneCode)
        jo.put("source", "ANTFOREST")
        jo.put("taskType", taskType)
        return RequestManager.requestString("com.alipay.antiep.receiveTaskAward", JSONArray().put(jo).toString())
    }

    /** 领取青春特权道具 */
    @JvmStatic
    @Throws(JSONException::class)
    fun receiveTaskAwardV2(taskType: String?): String {
        val jo = JSONObject()
        jo.put("ignoreLimit", false)
        jo.put("requestType", "H5")
        jo.put("sceneCode", "ANTFOREST_VITALITY_TASK")
        jo.put("source", "ANTFOREST")
        jo.put("taskType", taskType) // DAXUESHENG_SJK,NENGLIANGZHAO_20230807,JIASUQI_20230808
        return RequestManager.requestString("com.alipay.antiep.receiveTaskAward", JSONArray().put(jo).toString())
    }

    @JvmStatic
    @Throws(JSONException::class)
    fun finishTask(sceneCode: String?, taskType: String): String {
        val outBizNo = taskType + "_" + RandomUtil.nextDouble()
        val jo = JSONObject()
        jo.put("outBizNo", outBizNo)
        jo.put("requestType", "H5")
        jo.put("sceneCode", sceneCode)
        jo.put("source", "ANTFOREST")
        jo.put("taskType", taskType)
        val args = "[" + jo + "]"
        return RequestManager.requestString("com.alipay.antiep.finishTask", args)
    }

    @JvmStatic
    @Throws(JSONException::class)
    fun antiepSign(entityId: String?, userId: String?, sceneCode: String?): String {
        val jo = JSONObject()
        jo.put("entityId", entityId)
        jo.put("requestType", "rpc")
        jo.put("sceneCode", sceneCode)
        jo.put("source", "ANTFOREST")
        jo.put("userId", userId)
        val args = "[" + jo + "]"
        return RequestManager.requestString("com.alipay.antiep.sign", args)
    }

    @JvmStatic
    @Throws(JSONException::class)
    fun antiepSign(userId: String?, sceneCode: String?): String {
        val jo = JSONObject()
        jo.put("requestType", "rpc")
        jo.put("sceneCode", sceneCode)
        jo.put("source", "ANTFOREST")
        jo.put("userId", userId)
        val args = "[" + jo + "]"
        return RequestManager.requestString("com.alipay.antiep.sign", args)
    }

    @JvmStatic
    @Throws(JSONException::class)
    fun queryCommonSign(bizType: String?): String {
        val jo = JSONObject()
        jo.put("bizType", bizType)
        jo.put("source", "chInfo_ch_appcenter__chsub_9patch")
        jo.put("withEntity", true)
        return RequestManager.requestString("alipay.antforest.forest.h5.queryCommonSign", JSONArray().put(jo).toString())
    }

    /** 查询背包道具列表 */
    @JvmStatic
    @Throws(JSONException::class)
    fun queryPropList(onlyGive: Boolean): String {
        val jo = JSONObject()
        jo.put("onlyGive", if (onlyGive) "Y" else "")
        jo.put("source", "chInfo_ch_appcenter__chsub_9patch")
        jo.put("version", VERSION)
        return RequestManager.requestString("alipay.antforest.forest.h5.queryPropList", JSONArray().put(jo).toString())
    }

    @JvmStatic
    @Throws(JSONException::class)
    fun queryAnimalPropList(): String {
        val jo = JSONObject()
        jo.put("source", "chInfo_ch_appcenter__chsub_9patch")
        return RequestManager.requestString("alipay.antforest.forest.h5.queryAnimalPropList", JSONArray().put(jo).toString())
    }

    /** 创建使用道具的请求数据 */
    @Throws(JSONException::class)
    private fun createConsumePropRequestData(propGroup: String?, propId: String?, propType: String?, secondConfirm: Boolean?): JSONObject {
        val jo = JSONObject()
        if (!propGroup.isNullOrEmpty()) {
            jo.put("propGroup", propGroup)
        }
        jo.put("propId", propId)
        jo.put("propType", propType)
        jo.put("sToken", System.currentTimeMillis().toString() + "_" + RandomUtil.getRandomString(8))
        if (secondConfirm != null) {
            jo.put("secondConfirm", secondConfirm)
        }
        jo.put("source", "chInfo_ch_appcenter__chsub_9patch")
        jo.put("timezoneId", "Asia/Shanghai")
        jo.put("version", VERSION)
        return jo
    }

    /** 调用蚂蚁森林 RPC 使用道具 (可续写/二次确认) */
    @JvmStatic
    @Throws(JSONException::class)
    fun consumeProp(propGroup: String?, propId: String?, propType: String?, secondConfirm: Boolean): String {
        val requestData = createConsumePropRequestData(propGroup, propId, propType, secondConfirm)
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.consumeProp",
            "[" + requestData + "]"
        )
    }

    /** 调用蚂蚁森林 RPC 使用道具 (不可续写/直接使用) */
    @JvmStatic
    @Throws(JSONException::class)
    fun consumeProp2(propGroup: String?, propId: String?, propType: String?): String {
        val requestData = createConsumePropRequestData(propGroup, propId, propType, null)
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.consumeProp",
            "[" + requestData + "]"
        )
    }

    @JvmStatic
    @Throws(JSONException::class)
    fun giveProp(giveConfigId: String?, propId: String?, targetUserId: String?): String {
        val jo = JSONObject()
        jo.put("giveConfigId", giveConfigId)
        jo.put("propId", propId)
        jo.put("source", "self_corner")
        jo.put("targetUserId", targetUserId)
        return RequestManager.requestString("alipay.antforest.forest.h5.giveProp", JSONArray().put(jo).toString())
    }

    @JvmStatic
    @Throws(JSONException::class)
    fun collectProp(giveConfigId: String?, giveId: String?): String {
        val jo = JSONObject()
        jo.put("giveConfigId", giveConfigId)
        jo.put("giveId", giveId)
        jo.put("source", "chInfo_ch_appcenter__chsub_9patch")
        return RequestManager.requestString("alipay.antforest.forest.h5.collectProp", JSONArray().put(jo).toString())
    }

    /** 收取能量炸弹卡 */
    @JvmStatic
    @Throws(JSONException::class)
    fun collectBombCardEnergy(propId: String?): String {
        val jo = JSONObject()
        jo.put("propId", propId)
        jo.put("source", "chInfo_ch_appcenter__chsub_9patch")
        return RequestManager.requestString("alipay.antforest.forest.h5.collectBombCardEnergy", JSONArray().put(jo).toString())
    }

    @JvmStatic
    fun itemList(labelType: String?): String {
        return RequestManager.requestString(
            "com.alipay.antiep.itemList",
            "[{\"extendInfo\":\"{}\",\"labelType\":\""
                + labelType
                + "\",\"pageSize\":20,\"requestType\":\"rpc\",\"sceneCode\":\"ANTFOREST_VITALITY\",\"source\":\"afEntry\",\"startIndex\":0}]"
        )
    }

    @JvmStatic
    fun itemDetail(spuId: String?): String {
        return RequestManager.requestString(
            "com.alipay.antiep.itemDetail",
            "[{\"requestType\":\"rpc\",\"sceneCode\":\"ANTFOREST_VITALITY\",\"source\":\"afEntry\",\"spuId\":\"" + spuId + "\"}]"
        )
    }

    @JvmStatic
    @Throws(JSONException::class)
    fun exchangeBenefit(spuId: String?, skuId: String?): String {
        val jo = JSONObject()
        jo.put("sceneCode", "ANTFOREST_VITALITY")
        jo.put("requestId", System.currentTimeMillis().toString() + "_" + RandomUtil.getRandomInt(17))
        jo.put("spuId", spuId)
        jo.put("skuId", skuId)
        jo.put("source", "GOOD_DETAIL")
        return RequestManager.requestString("com.alipay.antcommonweal.exchange.h5.exchangeBenefit", JSONArray().put(jo).toString())
    }

    /** 巡护保护地 */
    @JvmStatic
    @Throws(JSONException::class)
    fun queryUserPatrol(): String {
        val jo = JSONObject()
        jo.put("source", "ant_forest")
        jo.put("timezoneId", "Asia/Shanghai")
        return RequestManager.requestString("alipay.antforest.forest.h5.queryUserPatrol", JSONArray().put(jo).toString())
    }

    @JvmStatic
    @Throws(JSONException::class)
    fun queryMyPatrolRecord(): String {
        val jo = JSONObject()
        jo.put("source", "ant_forest")
        jo.put("timezoneId", "Asia/Shanghai")
        return RequestManager.requestString("alipay.antforest.forest.h5.queryMyPatrolRecord", JSONArray().put(jo).toString())
    }

    @JvmStatic
    @Throws(JSONException::class)
    fun switchUserPatrol(targetPatrolId: String?): String {
        val jo = JSONObject()
        jo.put("source", "ant_forest")
        jo.put("targetPatrolId", targetPatrolId)
        jo.put("timezoneId", "Asia/Shanghai")
        return RequestManager.requestString("alipay.antforest.forest.h5.switchUserPatrol", JSONArray().put(jo).toString())
    }

    @JvmStatic
    fun patrolGo(nodeIndex: Int, patrolId: Int): String {
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.patrolGo", "[{\"nodeIndex\":" + nodeIndex + ",\"patrolId\":" + patrolId + ",\"source\":\"ant_forest\"," +
                "\"timezoneId\":\"Asia/Shanghai\"}]"
        )
    }

    @JvmStatic
    fun patrolKeepGoing(nodeIndex: Int, patrolId: Int, eventType: String): String {
        val args = when (eventType) {
            "video" ->
                "[{\"nodeIndex\":" + nodeIndex + ",\"patrolId\":" + patrolId + ",\"reactParam\":{\"viewed\":\"Y\"},\"source\":\"ant_forest\"," +
                    "\"timezoneId\":\"Asia/Shanghai\"}]"
            "chase" ->
                "[{\"nodeIndex\":" + nodeIndex + ",\"patrolId\":" + patrolId + ",\"reactParam\":{\"sendChat\":\"Y\"},\"source\":\"ant_forest\"," +
                    "\"timezoneId\":\"Asia/Shanghai\"}]"
            "quiz" ->
                "[{\"nodeIndex\":" + nodeIndex + ",\"patrolId\":" + patrolId + ",\"reactParam\":{\"answer\":\"correct\"},\"source\":\"ant_forest\"," +
                    "\"timezoneId\":\"Asia/Shanghai\"}]"
            else ->
                "[{\"nodeIndex\":" + nodeIndex + ",\"patrolId\":" + patrolId + ",\"reactParam\":{},\"source\":\"ant_forest\"," +
                    "\"timezoneId\":\"Asia/Shanghai\"}]"
        }
        return RequestManager.requestString("alipay.antforest.forest.h5.patrolKeepGoing", args)
    }

    @JvmStatic
    fun exchangePatrolChance(costStep: Int): String {
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.exchangePatrolChance", "[{\"costStep\":" + costStep + ",\"source\":\"ant_forest\"," +
                "\"timezoneId\":\"Asia/Shanghai\"}]"
        )
    }

    @JvmStatic
    fun queryAnimalAndPiece(animalId: Int): String {
        val args: String
        if (animalId != 0) {
            args = "[{\"animalId\":" + animalId + ",\"source\":\"ant_forest\",\"timezoneId\":\"Asia/Shanghai\"}]"
        } else {
            args = "[{\"source\":\"ant_forest\",\"timezoneId\":\"Asia/Shanghai\",\"withDetail\":\"N\",\"withGift\":true}]"
        }
        return RequestManager.requestString("alipay.antforest.forest.h5.queryAnimalAndPiece", args)
    }

    @JvmStatic
    fun combineAnimalPiece(animalId: Int, piecePropIds: String?): String {
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.combineAnimalPiece",
            "[{\"animalId\":" + animalId + ",\"piecePropIds\":" + piecePropIds + ",\"timezoneId\":\"Asia/Shanghai\",\"source\":\"ant_forest\"}]"
        )
    }

    @JvmStatic
    fun AnimalConsumeProp(propGroup: String?, propId: String?, propType: String?): String {
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.consumeProp",
            "[{\"propGroup\":\"" + propGroup + "\",\"propId\":\"" + propId + "\",\"propType\":\"" + propType + "\",\"source\":\"ant_forest\"," +
                "\"timezoneId\":\"Asia/Shanghai\"}]"
        )
    }

    @JvmStatic
    fun collectAnimalRobEnergy(propId: String?, propType: String?, shortDay: String?): String {
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.collectAnimalRobEnergy",
            "[{\"propId\":\"" + propId + "\",\"propType\":\"" + propType + "\",\"shortDay\":\"" + shortDay + "\",\"source" +
                "\":\"chInfo_ch_appcenter__chsub_9patch\",\"version\":\"" + VERSION + "\"}]"
        )
    }

    /** 复活能量 */
    @JvmStatic
    fun protectBubble(targetUserId: String?): String {
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.protectBubble",
            "[{\"source\":\"ANT_FOREST_H5\",\"targetUserId\":\"" + targetUserId + "\",\"version\":\"" + VERSION + "\"}]"
        )
    }

    /** 森林礼盒 */
    @JvmStatic
    fun collectFriendGiftBox(targetId: String?, targetUserId: String?): String {
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.collectFriendGiftBox",
            "[{\"source\":\"chInfo_ch_appcenter__chsub_9patch\",\"targetId\":\"" + targetId + "\",\"targetUserId\":\"" + targetUserId + "\"}]"
        )
    }

    /** 6秒拼手速 打地鼠 */
    @JvmStatic
    @Throws(JSONException::class)
    fun startWhackMole(): String {
        val param = JSONObject()
        param.put("source", "senlinguangchangdadishu")
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.startWhackMole",
            "[" + param + "]"
        )
    }

    /** 6秒拼手速 兼容模式打地鼠 */
    @JvmStatic
    fun oldstartWhackMole(source: String?): String {
        return RequestManager.requestString("alipay.antforest.forest.h5.startWhackMole", "[{\"source\":\"" + source + "\"}]")
    }

    /** 打单个地鼠 道具 */
    @JvmStatic
    @Throws(JSONException::class)
    fun whackMole(moleId: Long, token: String?): String {
        val param = JSONObject()
        param.put("moleId", moleId)
        param.put("source", "senlinguangchangdadishu")
        param.put("token", token)
        param.put("version", VERSION)

        return RequestManager.requestString(
            "alipay.antforest.forest.h5.whackMole",
            "[" + param + "]"
        )
    }

    /**
     * 兼容模式打单个地鼠
     */
    @JvmStatic
    fun oldwhackMole(moleId: Long, token: String?, source: String?): String {
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.whackMole",
            "[{\"moleId\":" + moleId + ",\"source\":\"" + source + "\",\"token\":\"" + token + "\",\"version\":\"" + VERSION + "\"}]"
        )
    }

    @JvmStatic
    @Throws(JSONException::class)
    fun settlementWhackMole(token: String?): String {
        // moleIdList 改为 1 ,20（包含 1-20）
        val moleIdList: List<Int> = (1..15).toList()
        val param = JSONObject()
        param.put("moleIdList", JSONArray(moleIdList))
        param.put("settlementScene", "NORMAL")
        param.put("source", "senlinguangchangdadishu")
        param.put("token", token)
        param.put("version", VERSION)
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.settlementWhackMole",
            "[" + param + "]"
        )
    }

    //兼容模式结算
    @JvmStatic
    fun oldsettlementWhackMole(token: String?, moleIdList: List<String?>?, source: String?): String {
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.settlementWhackMole",
            "[{\"moleIdList\":["
                + moleIdList!!.joinToString(",")
                + "],\"settlementScene\":\"NORMAL\",\"source\":\"" + source + "\",\"token\":\""
                + token
                + "\",\"version\":\""
                + VERSION
                + "\"}]"
        )
    }

    /** 森林集市 */
    @JvmStatic
    fun consultForSendEnergyByAction(sourceType: String?): String {
        return RequestManager.requestString("alipay.bizfmcg.greenlife.consultForSendEnergyByAction", "[{\"sourceType\":\"" + sourceType + "\"}]")
    }

    /** 森林集市 */
    @JvmStatic
    fun sendEnergyByAction(sourceType: String?): String {
        return RequestManager.requestString(
            "alipay.bizfmcg.greenlife.sendEnergyByAction",
            "[{\"actionType\":\"GOODS_BROWSE\",\"requestId\":\"" + RandomUtil.getRandomString(8) + "\",\"sourceType\":\"" + sourceType + "\"}]"
        )
    }

    /** 翻倍额外能量收取 */
    @JvmStatic
    fun collectRobExpandEnergy(propId: String?, propType: String?): String {
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.collectRobExpandEnergy",
            "[{\"propId\":\"" + propId + "\",\"propType\":\"" + propType + "\",\"source\":\"chInfo_ch_appcenter__chsub_9patch\"}]"
        )
    }

    @JvmStatic
    @Throws(JSONException::class)
    fun studentQqueryCheckInModel(): String {
        val jo = JSONObject()
        jo.put("chInfo", "ch_appcollect__chsub_my-recentlyUsed")
        jo.put("skipTaskModule", false)
        return RequestManager.requestString("alipay.membertangram.biz.rpc.student.queryCheckInModel", JSONArray().put(jo).toString())
    }

    /*青春特权领红包*/
    @JvmStatic
    @Throws(JSONException::class)
    fun studentCheckin(): String {
        val jo = JSONObject()
        jo.put("source", "chInfo_ch_appcenter__chsub_9patch")
        return RequestManager.requestString("alipay.membertangram.biz.rpc.student.checkIn", JSONArray().put(jo).toString())
    }

    /** 查询绿色行动 */
    @JvmStatic
    fun ecolifeQueryHomePage(): String {
        return RequestManager.requestString(
            "alipay.ecolife.rpc.h5.queryHomePage",
            "[{\"channel\":\"ALIPAY\",\"source\":\"search_brandbox\"}]"
        )
    }

    /** 开通绿色行动 */
    @JvmStatic
    fun ecolifeOpenEcolife(): String {
        return RequestManager.requestString(
            "alipay.ecolife.rpc.h5.openEcolife",
            "[{\"channel\":\"ALIPAY\",\"source\":\"renwuGD\"}]"
        )
    }

    /** 执行任务 */
    @JvmStatic
    fun ecolifeTick(actionId: String?, dayPoint: String?, source: String?): String {
        val args1 = "[{\"actionId\":\"" + actionId + "\",\"channel\":\"ALIPAY\",\"dayPoint\":\"" +
            dayPoint + "\",\"generateEnergy\":false,\"source\":\"" + source + "\"}]"
        return RequestManager.requestString("alipay.ecolife.rpc.h5.tick", args1)
    }

    /** 查询任务信息 */
    @JvmStatic
    fun ecolifeQueryDish(source: String?, dayPoint: String?): String {
        return RequestManager.requestString(
            "alipay.ecolife.rpc.h5.queryDish",
            "[{\"channel\":\"ALIPAY\",\"dayPoint\":\"" + dayPoint +
                "\",\"source\":\"" + source + "\"}]"
        )
    }

    /** 上传照片 */
    @JvmStatic
    fun ecolifeUploadDishImage(
        operateType: String?, imageId: String?,
        conf1: Double, conf2: Double, conf3: Double, dayPoint: String?
    ): String {
        return RequestManager.requestString(
            "alipay.ecolife.rpc.h5.uploadDishImage",
            "[{\"channel\":\"ALIPAY\",\"dayPoint\":\"" + dayPoint +
                "\",\"source\":\"photo-comparison\",\"uploadParamMap\":{\"AIResult\":[{\"conf\":" + conf1 + ",\"kvPair\":false," +
                "\"label\":\"other\",\"pos\":[1.0002995,0.22104378,0.0011976048,0.77727276],\"value\":\"\"}," +
                "{\"conf\":" + conf2 + ",\"kvPair\":false,\"label\":\"guangpan\",\"pos\":[1.0002995,0.22104378,0.0011976048,0.77727276]," +
                "\"value\":\"\"},{\"conf\":" + conf3 + ",\"kvPair\":false,\"label\":\"feiguangpan\"," +
                "\"pos\":[1.0002995,0.22104378,0.0011976048,0.77727276],\"value\":\"\"}],\"existAIResult\":true,\"imageId\":\"" +
                imageId + "\",\"imageUrl\":\"https://mdn.alipayobjects.com/afts/img/" + imageId +
                "/original?bz=APM_20000067\",\"operateType\":\"" + operateType + "\"}}]"
        )
    }

    // 查询森林能量
    @JvmStatic
    fun queryForestEnergy(scene: String?): String {
        val args = "[{\"activityCode\":\"query_forest_energy\",\"activityId\":\"2024052300762675\",\"body\":{\"scene\":\"" + scene + "\"},\"version\":\"2" +
            ".0\"}]"
        return RequestManager.requestString("alipay.iblib.channel.data", args)
    }

    // 生成森林能量
    @JvmStatic
    fun produceForestEnergy(scene: String?): String {
        val uniqueId = System.currentTimeMillis()
        val args = "[{\"activityCode\":\"produce_forest_energy\",\"activityId\":\"2024052300762674\",\"body\":{\"scene\":\"" + scene + "\",\"uniqueId" +
            "\":\"" + uniqueId + "\"},\"version\":\"2.0\"}]"
        return RequestManager.requestString("alipay.iblib.channel.data", args)
    }

    // 领取森林能量
    @JvmStatic
    fun harvestForestEnergy(scene: String?, bubbles: JSONArray?): String {
        val args = "[{\"activityCode\":\"harvest_forest_energy\",\"activityId\":\"2024052300762676\",\"body\":{\"bubbles\":" + bubbles + ",\"scene\":\"" + scene + "\"},\"version\":\"2.0\"}]"
        return RequestManager.requestString("alipay.iblib.channel.data", args)
    }

    // ==================== 森林抽抽乐相关方法（最终修复版） ====================

    /** 森林抽抽乐-活动列表（最终修复版） 根据抓包日志，正确的参数结构应该是直接传递参数，不需要requestData包装 */
    @JvmStatic
    @Throws(JSONException::class)
    fun enterDrawActivityopengreen(activityId: String?, sceneCode: String?, source: String?): String {
        // 根据抓包日志，正确的参数结构是直接传递，不需要requestData包装
        val requestData = JSONObject()
        if (!activityId.isNullOrEmpty()) {
            requestData.put("activityId", activityId)
        } else {
            requestData.put("activityId", "")
        }
        requestData.put("requestType", "RPC")
        requestData.put("sceneCode", sceneCode) // 必须传递 sceneCode
        requestData.put("source", source) // 必须传递 source

        val args = "[" + requestData + "]"
        Log.record("AntForestRpcCall", "enterDrawActivityopengreen - 活动: $activityId, 场景: $sceneCode, source: $source")
        return RequestManager.requestString("com.alipay.antiepdrawprod.enterDrawActivityopengreen", args)
    }

    /** 森林抽抽乐-请求任务列表（最终修复版） */
    @JvmStatic
    @Throws(JSONException::class)
    fun listTaskopengreen(sceneCode: String?, source: String?): String {
        // 根据抓包日志，正确的参数结构是直接传递，不需要requestData包装
        val requestData = JSONObject()
        requestData.put("requestType", "RPC")
        requestData.put("sceneCode", sceneCode) // 必须传递 sceneCode
        requestData.put("source", source) // 必须传递 source

        val args = "[" + requestData + "]"
        Log.record("AntForestRpcCall", "listTaskopengreen - 场景: $sceneCode, source: $source")
        return RequestManager.requestString("com.alipay.antieptask.listTaskopengreen", args)
    }

    /** 森林抽抽乐-抽奖（最终修复版） */
    @JvmStatic
    @Throws(JSONException::class)
    fun drawopengreen(activityId: String?, sceneCode: String?, source: String?, userId: String?): String {
        // 根据抓包日志，正确的参数结构是直接传递，不需要requestData包装
        val requestData = JSONObject()
        requestData.put("activityId", activityId)
        requestData.put("requestType", "RPC")
        requestData.put("sceneCode", sceneCode) // 必须传递 sceneCode
        requestData.put("source", source) // 必须传递 source
        requestData.put("userId", userId)

        val args = "[" + requestData + "]"
        Log.record("AntForestRpcCall", "drawopengreen - 活动: $activityId, 场景: $sceneCode, source: $source")
        return RequestManager.requestString("com.alipay.antiepdrawprod.drawopengreen", args)
    }

    /** 森林抽抽乐-签到领取次数（最终修复版） */
    @JvmStatic
    @Throws(JSONException::class)
    fun receiveTaskAwardopengreen(source: String?, sceneCode: String?, taskType: String?): String {
        // 根据抓包日志，正确的参数结构是直接传递，不需要requestData包装
        val requestData = JSONObject()
        requestData.put("ignoreLimit", true)
        requestData.put("requestType", "RPC")
        requestData.put("sceneCode", sceneCode)
        requestData.put("source", source) // 必须传递 source
        requestData.put("taskType", taskType)

        val args = "[" + requestData + "]"
        Log.record("AntForestRpcCall", "receiveTaskAwardopengreen - 任务: $taskType, source: $source")
        return RequestManager.requestString("com.alipay.antieptask.receiveTaskAwardopengreen", args)
    }

    /** 森林抽抽乐-任务-活力值兑换抽奖次数（最终修复版） */
    @JvmStatic
    @Throws(JSONException::class)
    fun exchangeTimesFromTaskopengreen(activityId: String?, sceneCode: String?, source: String?, taskSceneCode: String?, taskType: String?): String {
        // 根据抓包日志，正确的参数结构是直接传递，不需要requestData包装
        val requestData = JSONObject()
        requestData.put("activityId", activityId)
        requestData.put("requestType", "RPC")
        requestData.put("sceneCode", sceneCode)
        requestData.put("source", source) // 必须传递 source
        requestData.put("taskSceneCode", taskSceneCode)
        requestData.put("taskType", taskType)

        val args = "[" + requestData + "]"
        Log.record("AntForestRpcCall", "exchangeTimesFromTaskopengreen - 活动: $activityId, 任务: $taskType, source: $source")
        return RequestManager.requestString("com.alipay.antiepdrawprod.exchangeTimesFromTaskopengreen", args)
    }

    /** 森林抽抽乐-任务-广告（支持普通版和活动版） */
    @JvmStatic
    @Throws(JSONException::class)
    fun finishTask4Chouchoule(taskType: String, sceneCode: String?): String {
        val params = JSONObject()
        params.put("outBizNo", taskType + RandomUtil.getRandomTag())
        params.put("requestType", "RPC")
        params.put("sceneCode", sceneCode)

        // 根据任务类型设置不同的source
        if (taskType.contains("XLIGHT")) {
            params.put("source", "ADBASICLIB")
        } else if (taskType.startsWith("FOREST_ACTIVITY_DRAW")) {
            params.put("source", "task_entry") // 活动版任务使用task_entry
        } else {
            params.put("source", "task_entry") // 默认使用task_entry
        }

        params.put("taskType", taskType)
        val args = "[" + params + "]"
        Log.record("AntForestRpcCall", "finishTask4Chouchoule - 任务: $taskType")
        return RequestManager.requestString("com.alipay.antiep.finishTask", args)
    }

    /** 完成森林抽抽乐任务（支持普通版和活动版） */
    @JvmStatic
    @Throws(JSONException::class)
    fun finishTaskopengreen(taskType: String, sceneCode: String?): String {
        val params = JSONObject()
        params.put("outBizNo", taskType + RandomUtil.getRandomTag())
        params.put("requestType", "RPC")
        params.put("sceneCode", sceneCode)

        // 统一使用 task_entry，因为从日志看两种任务都使用这个source
        params.put("source", "task_entry")

        params.put("taskType", taskType)
        val args = "[" + params + "]"
        Log.record("AntForestRpcCall", "finishTaskopengreen - 任务: $taskType")
        return RequestManager.requestString("com.alipay.antieptask.finishTaskopengreen", args)
    }

    /** 根据道具类型获取道具组 */
    @JvmStatic
    fun getPropGroup(propType: String): String {
        if (propType.contains("SHIELD")) {
            return "shield"
        } else if (propType.contains("DOUBLE_CLICK")) {
            return "doubleClick"
        } else if (propType.contains("STEALTH")) {
            return "stealthCard"
        } else if (propType.contains("BOMB_CARD") || propType.contains("NO_EXPIRE")) {
            return "energyBombCard"
        } else if (propType.contains("ROB_EXPAND")) {
            return "robExpandCard"
        } else if (propType.contains("BUBBLE_BOOST")) {
            return "boost"
        }
        return "" // 默认返回空字符串
    }

    /** 查询游戏列表 */
    @JvmStatic
    fun queryGameList(): String {
        return RequestManager.requestString(
            "com.alipay.charitygamecenter.queryGameList",
            "[{" +
                "  \"bizType\": \"ANTFOREST\"," +
                "  \"commonDegradeFilterRequest\": {" +
                "    \"deviceLevel\": \"high\"," +
                "    \"platform\": \"Android\"," +
                "    \"unityDeviceLevel\": \"high\"" +
                "  }," +
                "  \"requestType\": \"RPC\"," +
                "  \"sceneCode\": \"ANTFOREST\"," +
                "  \"source\": \"chInfo_ch_appcenter__chsub_9patch\"," +
                "  \"version\": \"" + VERSION + "\"" +
                "}]"
        )
    }

    /**
     * 领取游戏中心奖励 (批量开宝箱)
     * @param batchDrawCount 批量领取的次数 (例如 1 或 10)
     */
    @JvmStatic
    fun drawGameCenterAward(batchDrawCount: Int): String {
        return RequestManager.requestString(
            "com.alipay.charitygamecenter.drawGameCenterAward",
            "[{" +
                "  \"batchDrawCount\": " + batchDrawCount + "," +
                "  \"bizType\": \"ANTFOREST\"," +
                "  \"requestType\": \"RPC\"," +
                "  \"sceneCode\": \"ANTFOREST\"," +
                "  \"source\": \"leyuan\"," +
                "  \"version\": \"" + VERSION + "\"" +
                "}]"
        )
    }

    /** 初始化/上报游戏任务 */
    @JvmStatic
    fun initTask(taskType: String?): String {
        // 生成类似 GAME_DONE_SLJYD_1769062463227_569cf36c 的 outBizNo
        val timestamp = System.currentTimeMillis().toString()
        val randomSuffix = UUID.randomUUID().toString().substring(0, 8)
        val outBizNo = taskType + "_" + timestamp + "_" + randomSuffix

        return RequestManager.requestString(
            "com.alipay.antiep.initTask",
            "[{" +
                "  \"outBizNo\": \"" + outBizNo + "\"," +
                "  \"requestType\": \"H5\"," +
                "  \"sceneCode\": \"ANTFOREST_ENERGY_RAIN_TASK\"," +
                "  \"source\": \"ANTFOREST\"," +
                "  \"taskType\": \"" + taskType + "\"" +
                "}]"
        )
    }

    /** 查询森林乐园限定活动 */
    @JvmStatic
    fun queryOptionalPlay(): String {
        val args1 = "[{\"bizType\":\"ANTFOREST\",\"commonDegradeFilterRequest\":{\"appMode\":\"normal\",\"deviceLevel\":\"high\",\"platform\":\"Android\",\"unityDeviceLevel\":\"high\"},\"playTypeList\":[\"TASK_TRIGGER\",\"TOP_UP_COUPON\"],\"recentAppRecordList\":[],\"requestType\":\"RPC\",\"sceneCode\":\"ANTFOREST_COMMON\",\"source\":\"chInfo_ch_appcenter__chsub_9patch\",\"version\":\"" + VERSION + "\"}]"
        return RequestManager.requestString("com.alipay.charitygamecenter.queryOptionalPlay", args1)
    }

    /** 查询 1V1 能量挑战入口信息 */
    @JvmStatic
    fun queryEnergyPvpInfo(): String {
        try {
            val extInfo = JSONObject().put("checkReward", true)
            val arg = JSONObject()
                .put("extInfo", extInfo.toString())
                .put("queryBizType", "energyPvpInfo")
                .put("source", "chInfo_ch_appcenter__chsub_9patch")
            return RequestManager.requestString(
                "alipay.antforest.forest.h5.queryMiscInfo",
                JSONArray().put(arg).toString()
            )
        } catch (e: JSONException) {
            Log.printStackTrace("AntForestRpcCall", "构造 1V1 入口请求失败", e)
            return ""
        }
    }

    /** 查询 1V1 当前和上一场记录 */
    @JvmStatic
    fun queryPvpHomeInfo(): String {
        try {
            val arg = JSONObject()
                .put("queryWaitToReceive", true)
                .put("source", "chInfo_ch_appcenter__chsub_9patch")
            return RequestManager.requestString(
                "alipay.antforest.forest.h5.queryPvpHomeInfo",
                JSONArray().put(arg).toString()
            )
        } catch (e: JSONException) {
            Log.printStackTrace("AntForestRpcCall", "构造 1V1 主页请求失败", e)
            return ""
        }
    }

    /** 领取已结算的 1V1 奖励 */
    @JvmStatic
    fun receivePvpRewards(): String {
        try {
            val arg = JSONObject()
                .put("source", "chInfo_ch_appcenter__chsub_9patch")
            return RequestManager.requestString(
                "alipay.antforest.forest.h5.receivePvpRewards",
                JSONArray().put(arg).toString()
            )
        } catch (e: JSONException) {
            Log.printStackTrace("AntForestRpcCall", "构造 1V1 领奖请求失败", e)
            return ""
        }
    }

    /** 查询 1V1 历史记录，用于领奖后复查 */
    @JvmStatic
    fun queryPvpBattleRecords(pageSize: Int): String {
        try {
            val arg = JSONObject()
                .put("pageSize", Math.max(1, pageSize))
                .put("source", "chInfo_ch_appcenter__chsub_9patch")
            return RequestManager.requestString(
                "alipay.antforest.forest.h5.queryPvpBattleRecords",
                JSONArray().put(arg).toString()
            )
        } catch (e: JSONException) {
            Log.printStackTrace("AntForestRpcCall", "构造 1V1 记录请求失败", e)
            return ""
        }
    }
}
