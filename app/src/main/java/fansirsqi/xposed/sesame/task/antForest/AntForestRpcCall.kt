package fansirsqi.xposed.sesame.task.antForest

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import fansirsqi.xposed.sesame.entity.AlipayVersion
import fansirsqi.xposed.sesame.entity.RpcEntity
import fansirsqi.xposed.sesame.hook.ApplicationHook
import fansirsqi.xposed.sesame.hook.RequestManager
import fansirsqi.xposed.sesame.hook.rpc.RpcConst
import fansirsqi.xposed.sesame.hook.rpc.RpcRequestData
import fansirsqi.xposed.sesame.hook.rpc.RpcRequestData.putStandard
import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.core.util.RandomUtil
import fansirsqi.xposed.sesame.task.antForest.waiting.RpcFailureKind
import fansirsqi.xposed.sesame.task.antForest.waiting.RpcResult
import kotlinx.coroutines.CancellationException
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.error("AntForestRpcCall", "版本初始化异常，使用默认版本: $VERSION")
            Log.printStackTrace(e)
        }
    }

    @JvmStatic
    suspend fun queryFriendsEnergyRanking(): String {
        try {
            val correlationLocal = JSONObject()
            correlationLocal.put(
                "pathList",
                JSONArray().put("friendRanking").put("myself").put("totalDatas")
            )
            val relationLocal = "[" + correlationLocal + "]"
            return RequestManager.requestString(
                "alipay.antmember.forest.h5.queryEnergyRanking",
                RpcRequestData.array {
                    put("periodType", "total")
                    put("rankType", "energyRank")
                    putStandard(source = "chInfo_ch_appcenter__chsub_9patch", version = VERSION)
                },
                relationLocal
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return ""
        }
    }

    @JvmStatic
    suspend fun queryTopEnergyChallengeRanking(): String {
        try {
            return RequestManager.requestString(
                "alipay.antforest.forest.h5.queryTopEnergyChallengeRanking",
                RpcRequestData.array {
                    putStandard(source = "chInfo_ch_appcenter__chsub_9patch")
                }
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.printStackTrace(e)
            return ""
        }
    }

    /** 批量获取好友能量信息（标准版） */
    @JvmStatic
    suspend fun fillUserRobFlag(userIdList: JSONArray?): String {
        try {
            val joRelationLocal = JSONObject()
            joRelationLocal.put("pathList", JSONArray().put("friendRanking"))
            val relationLocal = "[" + joRelationLocal + "]"
            return RequestManager.requestString(
                "alipay.antforest.forest.h5.fillUserRobFlag",
                RpcRequestData.array {
                    put("userIdList", userIdList)
                    putStandard(source = "chInfo_ch_appcenter__chsub_9patch")
                },
                relationLocal
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return ""
        }
    }

    /** 批量获取好友能量信息（增强版 - PK排行榜专用） */
    @JvmStatic
    suspend fun fillUserRobFlag(userIdList: JSONArray?, needFillUserInfo: Boolean): String {
        try {
            return RequestManager.requestString(
                "alipay.antforest.forest.h5.fillUserRobFlag",
                RpcRequestData.array {
                    put("userIdList", userIdList)
                    put("needFillUserInfo", needFillUserInfo)
                    putStandard(source = "chInfo_ch_appcenter__chsub_9patch")
                }
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return ""
        }
    }

    @JvmStatic
    @Throws(JSONException::class)
    suspend fun queryHomePage(): String {
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.queryHomePage",
            RpcRequestData.array {
                put("activityParam", JSONObject())
                put("configVersionMap", JSONObject().put("wateringBubbleConfig", "0"))
                put("skipWhackMole", false)
                putStandard(source = "chInfo_ch_appcenter__chsub_9patch", version = VERSION)
            },
            3,
            1000
        )
    }

    @JvmStatic
    suspend fun queryFriendHomePage(userId: String?, fromAct: String?): String {
        try {
            var fromAct = fromAct
            if (fromAct == null) {
                fromAct = "TAKE_LOOK_FRIEND"
            }
            return RequestManager.requestString(
                "alipay.antforest.forest.h5.queryFriendHomePage",
                RpcRequestData.array {
                    put("canRobFlags", "T,F,F,F,F")
                    put("configVersionMap", JSONObject().put("wateringBubbleConfig", "0"))
                    put("userId", userId)
                    put("fromAct", fromAct)
                    putStandard(source = "chInfo_ch_appcenter__chsub_9patch", version = VERSION)
                },
                3,
                1000
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.printStackTrace(e)
            return ""
        }
    }

    /**
     * 蹲点专用：类型化查询好友主页（V2 §3.3.2）。
     * 单次尝试（tryCount=1），重试时机由蹲点自己的重试策略决定。
     */
    @JvmStatic
    suspend fun queryFriendHomePageResult(userId: String?, fromAct: String?): RpcResult<JSONObject> {
        var act = fromAct
        if (act == null) {
            act = "TAKE_LOOK_FRIEND"
        }
        val entity = RpcEntity(
            "alipay.antforest.forest.h5.queryFriendHomePage",
            RpcRequestData.array {
                put("canRobFlags", "T,F,F,F,F")
                put("configVersionMap", JSONObject().put("wateringBubbleConfig", "0"))
                put("userId", userId)
                put("fromAct", act)
                putStandard(source = "chInfo_ch_appcenter__chsub_9patch", version = VERSION)
            },
            null
        )
        return when (val r = RequestManager.requestStringResult(entity, 1, 0)) {
            is RpcResult.Ok -> try {
                RpcResult.Ok(JSONObject(r.value))
            } catch (e: JSONException) {
                RpcResult.Failed(RpcFailureKind.MALFORMED_RESPONSE, message = e.message)
            }
            is RpcResult.Failed -> r
        }
    }

    /** 蹲点专用：类型化收取能量（V2 §3.3.2）。单次尝试，重试时机由蹲点策略决定。 */
    @JvmStatic
    suspend fun collectEnergyResult(userId: String?, bubbleIds: List<Long>): RpcResult<JSONObject> {
        if (bubbleIds.isEmpty()) {
            return RpcResult.Failed(RpcFailureKind.EMPTY_RESPONSE, message = "empty bubble ids")
        }
        val entity = batchEnergyRpcEntity("GREEN", userId, bubbleIds)
        return when (val r = RequestManager.requestStringResult(entity, 1, 0)) {
            is RpcResult.Ok -> try {
                RpcResult.Ok(JSONObject(r.value))
            } catch (e: JSONException) {
                RpcResult.Failed(RpcFailureKind.MALFORMED_RESPONSE, message = e.message)
            }
            is RpcResult.Failed -> r
        }
    }

    /** 找能量方法 - 查找可收取能量的好友（带跳过用户列表） */
    @JvmStatic
    suspend fun takeLook(skipUsers: JSONObject?): String {
        try {
            return RequestManager.requestString(
                "alipay.antforest.forest.h5.takeLook",
                RpcRequestData.array {
                    put("contactsStatus", "N")
                    put("exposedUserId", "")
                    put("skipUsers", skipUsers)
                    put("takeLookEnd", false)
                    put("takeLookStart", true)
                    putStandard(source = "chInfo_ch_appcenter__chsub_9patch", version = VERSION)
                }
            )
        } catch (e: JSONException) {
            Log.printStackTrace("AntForestRpcCall", "takeLook构建请求参数失败", e)
            return ""
        }
    }

    @JvmStatic
    fun energyRpcEntity(bizType: String?, userId: String?, bubbleId: Long): RpcEntity? {
        try {
            return RpcEntity(
                "alipay.antmember.forest.h5.collectEnergy",
                RpcRequestData.array {
                    put("bizType", bizType)
                    put("bubbleIds", JSONArray().put(bubbleId))
                    put("userId", userId)
                    putStandard(source = "chInfo_ch_appcenter__chsub_9patch", version = VERSION)
                },
                null
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.printStackTrace(e)
            return null
        }
    }

    @JvmStatic
    suspend fun collectEnergy(bizType: String?, userId: String?, bubbleId: Long?): String {
        val r = energyRpcEntity(bizType, userId, bubbleId!!)
        return r?.let { RequestManager.requestString(it) } ?: ""
    }

    @JvmStatic
    @Throws(JSONException::class)
    fun batchEnergyRpcEntity(
        bizType: String?,
        userId: String?,
        bubbleIds: List<Long?>?
    ): RpcEntity {
        return RpcEntity(
            "alipay.antmember.forest.h5.collectEnergy",
            RpcRequestData.array {
                put("bizType", bizType)
                put("bubbleIds", JSONArray(bubbleIds))
                put("fromAct", "BATCH_ROB_ENERGY")
                put("userId", userId)
                putStandard(source = "chInfo_ch_appcenter__chsub_9patch", version = VERSION)
            }
        )
    }

    /** 收取复活能量 */
    @JvmStatic
    suspend fun collectRebornEnergy(): String {
        try {
            return RequestManager.requestString(
                "alipay.antforest.forest.h5.collectRebornEnergy",
                RpcRequestData.array {
                    putStandard(source = "chInfo_ch_appcenter__chsub_9patch")
                }
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.printStackTrace(e)
            return ""
        }
    }

    @JvmStatic
    suspend fun transferEnergy(
        targetUser: String?,
        bizNo: String,
        energyId: Int,
        notifyFriend: Boolean
    ): String {
        try {
            return RequestManager.requestString(
                "alipay.antmember.forest.h5.transferEnergy",
                RpcRequestData.array {
                    put("bizNo", bizNo + UUID.randomUUID().toString())
                    put("energyId", energyId)
                    put("extInfo", JSONObject().put("sendChat", if (notifyFriend) "Y" else "N"))
                    put("from", "friendIndex")
                    put("targetUser", targetUser)
                    put("transferType", "WATERING")
                    putStandard(source = "chInfo_ch_appcenter__chsub_9patch", version = VERSION)
                }
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.printStackTrace(e)
            return ""
        }
    }

    @JvmStatic
    suspend fun queryEnergyRainHome(): String {
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.queryEnergyRainHome",
            RpcRequestData.array {
                putStandard(source = "senlinguangchuangrukou", version = VERSION)
            }
        )
    }

    @JvmStatic
    suspend fun queryEnergyRainCanGrantList(): String {
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.queryEnergyRainCanGrantList",
            RpcRequestData.array {}
        )
    }

    @JvmStatic
    suspend fun grantEnergyRainChance(targetUserId: String?): String {
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.grantEnergyRainChance",
            RpcRequestData.array {
                // 原 targetUserId 为无引号插值（数字语义），用 BigDecimal 保持数字类型；null 时保持 "targetUserId":null
                put("targetUserId", targetUserId?.toBigDecimalOrNull() ?: JSONObject.NULL)
            }
        )
    }

    @JvmStatic
    suspend fun startEnergyRain(): String {
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.startEnergyRain",
            RpcRequestData.array {
                putStandard(version = VERSION)
            }
        )
    }

    @JvmStatic
    suspend fun energyRainSettlement(saveEnergy: Int, token: String?): String {
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.energyRainSettlement",
            RpcRequestData.array {
                put("activityPropNums", 0)
                put("saveEnergy", saveEnergy)
                put("token", token ?: "null")
                putStandard(version = VERSION)
            }
        )
    }

    /** 查询能量雨/游戏结束列表奖励 */
    @JvmStatic
    suspend fun queryEnergyRainEndGameList(): String {
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.queryEnergyRainEndGameList",
            RpcRequestData.array {}
        )
    }

    @JvmStatic
    @Throws(JSONException::class)
    suspend fun queryTaskList(): String {
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.queryTaskList",
            RpcRequestData.array {
                put("extend", JSONObject())
                put("fromAct", "home_task_list")
                putStandard(source = "chInfo_ch_appcenter__chsub_9patch", version = VERSION)
            }
        )
    }

    /*青春特权道具任务状态查询🔍*/
    @JvmStatic
    @Throws(JSONException::class)
    suspend fun queryTaskListV2(firstTaskType: String): String {
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.queryTaskList",
            RpcRequestData.array {
                put(
                    "extend",
                    JSONObject().put("firstTaskType", firstTaskType)
                ) // DNHZ_SL_college,DXS_BHZ，DXS_JSQ
                put("fromAct", "home_task_list")
                if (firstTaskType == "DNHZ_SL_college") {
                    put("source", firstTaskType)
                }
                if (firstTaskType == "DXS_BHZ" || firstTaskType == "DXS_JSQ") {
                    put("source", "202212TJBRW")
                }
                putStandard(version = VERSION)
            }
        )
    }

    @JvmStatic
    @Throws(JSONException::class)
    suspend fun receiveTaskAward(sceneCode: String?, taskType: String?): String {
        return RequestManager.requestString(
            "com.alipay.antiep.receiveTaskAward",
            RpcRequestData.array {
                put("ignoreLimit", false)
                put("taskType", taskType)
                putStandard(requestType = RpcConst.Type.H5, sceneCode = sceneCode, source = "ANTFOREST")
            }
        )
    }

    /** 领取青春特权道具 */
    @JvmStatic
    @Throws(JSONException::class)
    suspend fun receiveTaskAwardV2(taskType: String?): String {
        return RequestManager.requestString(
            "com.alipay.antiep.receiveTaskAward",
            RpcRequestData.array {
                put("ignoreLimit", false)
                put("taskType", taskType) // DAXUESHENG_SJK,NENGLIANGZHAO_20230807,JIASUQI_20230808
                putStandard(
                    requestType = RpcConst.Type.H5,
                    sceneCode = "ANTFOREST_VITALITY_TASK",
                    source = "ANTFOREST"
                )
            }
        )
    }

    @JvmStatic
    @Throws(JSONException::class)
    suspend fun finishTask(sceneCode: String?, taskType: String): String {
        val outBizNo = taskType + "_" + RandomUtil.nextDouble()
        return RequestManager.requestString(
            "com.alipay.antiep.finishTask",
            RpcRequestData.array {
                put("outBizNo", outBizNo)
                put("taskType", taskType)
                putStandard(requestType = RpcConst.Type.H5, sceneCode = sceneCode, source = "ANTFOREST")
            }
        )
    }

    @JvmStatic
    @Throws(JSONException::class)
    suspend fun antiepSign(entityId: String?, userId: String?, sceneCode: String?): String {
        return RequestManager.requestString(
            "com.alipay.antiep.sign",
            RpcRequestData.array {
                put("entityId", entityId)
                put("userId", userId)
                putStandard(requestType = RpcConst.Type.RPC_L, sceneCode = sceneCode, source = "ANTFOREST")
            }
        )
    }

    @JvmStatic
    @Throws(JSONException::class)
    suspend fun antiepSign(userId: String?, sceneCode: String?): String {
        return RequestManager.requestString(
            "com.alipay.antiep.sign",
            RpcRequestData.array {
                put("userId", userId)
                putStandard(requestType = RpcConst.Type.RPC_L, sceneCode = sceneCode, source = "ANTFOREST")
            }
        )
    }

    @JvmStatic
    @Throws(JSONException::class)
    suspend fun queryCommonSign(bizType: String?): String {
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.queryCommonSign",
            RpcRequestData.array {
                put("bizType", bizType)
                put("withEntity", true)
                putStandard(source = "chInfo_ch_appcenter__chsub_9patch")
            }
        )
    }

    /** 查询背包道具列表 */
    @JvmStatic
    @Throws(JSONException::class)
    suspend fun queryPropList(onlyGive: Boolean): String {
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.queryPropList",
            RpcRequestData.array {
                put("onlyGive", if (onlyGive) "Y" else "")
                putStandard(source = "chInfo_ch_appcenter__chsub_9patch", version = VERSION)
            }
        )
    }

    @JvmStatic
    @Throws(JSONException::class)
    suspend fun queryAnimalPropList(): String {
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.queryAnimalPropList",
            RpcRequestData.array {
                putStandard(source = "chInfo_ch_appcenter__chsub_9patch")
            }
        )
    }

    /** 创建使用道具的请求数据 */
    @Throws(JSONException::class)
    private fun createConsumePropRequestData(
        propGroup: String?,
        propId: String?,
        propType: String?,
        secondConfirm: Boolean?
    ): JSONObject {
        val jo = JSONObject()
        if (!propGroup.isNullOrEmpty()) {
            jo.put("propGroup", propGroup)
        }
        jo.put("propId", propId)
        jo.put("propType", propType)
        jo.put(
            "sToken",
            System.currentTimeMillis().toString() + "_" + RandomUtil.getRandomString(8)
        )
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
    suspend fun consumeProp(
        propGroup: String?,
        propId: String?,
        propType: String?,
        secondConfirm: Boolean
    ): String {
        val requestData = createConsumePropRequestData(propGroup, propId, propType, secondConfirm)
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.consumeProp",
            RpcRequestData.arrayOf(requestData)
        )
    }

    /** 调用蚂蚁森林 RPC 使用道具 (不可续写/直接使用) */
    @JvmStatic
    @Throws(JSONException::class)
    suspend fun consumeProp2(propGroup: String?, propId: String?, propType: String?): String {
        val requestData = createConsumePropRequestData(propGroup, propId, propType, null)
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.consumeProp",
            RpcRequestData.arrayOf(requestData)
        )
    }

    @JvmStatic
    @Throws(JSONException::class)
    suspend fun giveProp(giveConfigId: String?, propId: String?, targetUserId: String?): String {
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.giveProp",
            RpcRequestData.array {
                put("giveConfigId", giveConfigId)
                put("propId", propId)
                put("targetUserId", targetUserId)
                putStandard(source = "self_corner")
            }
        )
    }

    @JvmStatic
    @Throws(JSONException::class)
    suspend fun collectProp(giveConfigId: String?, giveId: String?): String {
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.collectProp",
            RpcRequestData.array {
                put("giveConfigId", giveConfigId)
                put("giveId", giveId)
                putStandard(source = "chInfo_ch_appcenter__chsub_9patch")
            }
        )
    }

    /** 收取能量炸弹卡 */
    @JvmStatic
    @Throws(JSONException::class)
    suspend fun collectBombCardEnergy(propId: String?): String {
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.collectBombCardEnergy",
            RpcRequestData.array {
                put("propId", propId)
                putStandard(source = "chInfo_ch_appcenter__chsub_9patch")
            }
        )
    }

    @JvmStatic
    suspend fun itemList(labelType: String?, startIndex: Int = 0): String {
        return RequestManager.requestString(
            "com.alipay.antiep.itemList",
            RpcRequestData.array {
                put("extendInfo", "{}")
                put("fromSpuId", "")
                put("labelType", labelType ?: "null")
                put("pageSize", 20)
                put("startIndex", startIndex)
                putStandard(
                    requestType = RpcConst.Type.RPC_L,
                    sceneCode = "ANTFOREST_VITALITY",
                    source = "afEntry"
                )
            }
        )
    }

    @JvmStatic
    suspend fun itemDetail(spuId: String?): String {
        return RequestManager.requestString(
            "com.alipay.antiep.itemDetail",
            RpcRequestData.array {
                put("spuId", spuId ?: "null")
                putStandard(
                    requestType = RpcConst.Type.RPC_L,
                    sceneCode = "ANTFOREST_VITALITY",
                    source = "afEntry"
                )
            }
        )
    }

    @JvmStatic
    @Throws(JSONException::class)
    suspend fun exchangeBenefit(spuId: String?, skuId: String?): String {
        return RequestManager.requestString(
            "com.alipay.antcommonweal.exchange.h5.exchangeBenefit",
            RpcRequestData.array {
                put(
                    "requestId",
                    System.currentTimeMillis().toString() + "_" + RandomUtil.getRandomInt(17)
                )
                put("spuId", spuId)
                put("skuId", skuId)
                putStandard(sceneCode = "ANTFOREST_VITALITY", source = "GOOD_DETAIL")
            }
        )
    }

    /** 秒杀兑换（专门用于秒杀场景的活力值兑换接口） */
    @JvmStatic
    @Throws(JSONException::class)
    suspend fun exchangeSkillBenefit(spuId: String?, skuId: String?): String {
        return RequestManager.requestString(
            "com.alipay.antcommonweal.exchange.h5.exchangeSkillBenefit",
            RpcRequestData.array {
                put(
                    "requestId",
                    System.currentTimeMillis().toString() + "_" + RandomUtil.getRandomInt(17)
                )
                put("spuId", spuId)
                put("skuId", skuId)
                putStandard(sceneCode = "ANTFOREST_VITALITY", source = "GOOD_DETAIL")
            }
        )
    }

    /** 查询活力值商店首页信息（含用户活力值余额 totalVitalityAmount） */
    @JvmStatic
    @Throws(JSONException::class)
    suspend fun queryVitalityStoreIndex(): String {
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.queryVitalityStoreIndex",
            RpcRequestData.array {
                put("source", "afEntry")
            }
        )
    }

    /** 查询活力值秒杀活动列表（含所有秒杀商品及 secKillStartTime/secKillEndTime） */
    @JvmStatic
    @Throws(JSONException::class)
    suspend fun secKillActivity(): String {
        return RequestManager.requestString(
            "com.alipay.antiep.seckill",
            RpcRequestData.array {
                put("secKillId", "ANTFOREST_VITALITY_MALL_SEC_KILL")
                putStandard(
                    requestType = RpcConst.Type.RPC,
                    sceneCode = "ANTFOREST_VITALITY",
                    source = "afEntry"
                )
            }
        )
    }

    /** 巡护保护地 */
    @JvmStatic
    @Throws(JSONException::class)
    suspend fun queryUserPatrol(): String {
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.queryUserPatrol",
            RpcRequestData.array {
                put("timezoneId", "Asia/Shanghai")
                putStandard(source = "ant_forest")
            }
        )
    }

    @JvmStatic
    @Throws(JSONException::class)
    suspend fun queryMyPatrolRecord(): String {
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.queryMyPatrolRecord",
            RpcRequestData.array {
                put("timezoneId", "Asia/Shanghai")
                putStandard(source = "ant_forest")
            }
        )
    }

    @JvmStatic
    @Throws(JSONException::class)
    suspend fun switchUserPatrol(targetPatrolId: String?): String {
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.switchUserPatrol",
            RpcRequestData.array {
                put("targetPatrolId", targetPatrolId)
                put("timezoneId", "Asia/Shanghai")
                putStandard(source = "ant_forest")
            }
        )
    }

    @JvmStatic
    suspend fun patrolGo(nodeIndex: Int, patrolId: Int): String {
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.patrolGo",
            RpcRequestData.array {
                put("nodeIndex", nodeIndex)
                put("patrolId", patrolId)
                put("timezoneId", "Asia/Shanghai")
                putStandard(source = "ant_forest")
            }
        )
    }

    @JvmStatic
    suspend fun patrolKeepGoing(nodeIndex: Int, patrolId: Int, eventType: String): String {
        val reactParam: JSONObject = when (eventType) {
            "video" ->
                JSONObject().put("viewed", "Y")

            "chase" ->
                JSONObject().put("sendChat", "Y")

            "quiz" ->
                JSONObject().put("answer", "correct")

            else ->
                JSONObject()
        }
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.patrolKeepGoing",
            RpcRequestData.array {
                put("nodeIndex", nodeIndex)
                put("patrolId", patrolId)
                put("reactParam", reactParam)
                put("timezoneId", "Asia/Shanghai")
                putStandard(source = "ant_forest")
            }
        )
    }

    @JvmStatic
    suspend fun exchangePatrolChance(costStep: Int): String {
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.exchangePatrolChance",
            RpcRequestData.array {
                put("costStep", costStep)
                put("timezoneId", "Asia/Shanghai")
                putStandard(source = "ant_forest")
            }
        )
    }

    @JvmStatic
    suspend fun queryAnimalAndPiece(animalId: Int): String {
        val args: String
        if (animalId != 0) {
            args = RpcRequestData.array {
                put("animalId", animalId)
                put("timezoneId", "Asia/Shanghai")
                putStandard(source = "ant_forest")
            }
        } else {
            args = RpcRequestData.array {
                put("withDetail", "N")
                put("withGift", true)
                put("timezoneId", "Asia/Shanghai")
                putStandard(source = "ant_forest")
            }
        }
        return RequestManager.requestString("alipay.antforest.forest.h5.queryAnimalAndPiece", args)
    }

    @JvmStatic
    suspend fun combineAnimalPiece(animalId: Int, piecePropIds: String?): String {
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.combineAnimalPiece",
            RpcRequestData.array {
                put("animalId", animalId)
                // 原 piecePropIds 无引号插值，实为 JSONArray.toString() 传入，按数组形态还原
                put("piecePropIds", piecePropIds?.let { JSONArray(it) } ?: JSONObject.NULL)
                put("timezoneId", "Asia/Shanghai")
                putStandard(source = "ant_forest")
            }
        )
    }

    @JvmStatic
    suspend fun AnimalConsumeProp(propGroup: String?, propId: String?, propType: String?): String {
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.consumeProp",
            RpcRequestData.array {
                put("propGroup", propGroup ?: "null")
                put("propId", propId ?: "null")
                put("propType", propType ?: "null")
                put("timezoneId", "Asia/Shanghai")
                putStandard(source = "ant_forest")
            }
        )
    }

    @JvmStatic
    suspend fun collectAnimalRobEnergy(
        propId: String?,
        propType: String?,
        shortDay: String?
    ): String {
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.collectAnimalRobEnergy",
            RpcRequestData.array {
                put("propId", propId ?: "null")
                put("propType", propType ?: "null")
                put("shortDay", shortDay ?: "null")
                putStandard(source = "chInfo_ch_appcenter__chsub_9patch", version = VERSION)
            }
        )
    }

    /** 复活能量 */
    @JvmStatic
    suspend fun protectBubble(targetUserId: String?): String {
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.protectBubble",
            RpcRequestData.array {
                put("targetUserId", targetUserId ?: "null")
                putStandard(source = "ANT_FOREST_H5", version = VERSION)
            }
        )
    }

    /** 森林礼盒 */
    @JvmStatic
    suspend fun collectFriendGiftBox(targetId: String?, targetUserId: String?): String {
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.collectFriendGiftBox",
            RpcRequestData.array {
                put("targetId", targetId ?: "null")
                put("targetUserId", targetUserId ?: "null")
                putStandard(source = "chInfo_ch_appcenter__chsub_9patch")
            }
        )
    }

    /** 6秒拼手速 打地鼠 */
    @JvmStatic
    @Throws(JSONException::class)
    suspend fun startWhackMole(): String {
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.startWhackMole",
            RpcRequestData.array {
                putStandard(source = "senlinguangchangdadishu")
            }
        )
    }

    /** 6秒拼手速 兼容模式打地鼠 */
    @JvmStatic
    suspend fun oldstartWhackMole(source: String?): String {
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.startWhackMole",
            RpcRequestData.array {
                putStandard(source = source ?: "null")
            }
        )
    }

    /** 打单个地鼠 道具 */
    @JvmStatic
    @Throws(JSONException::class)
    suspend fun whackMole(moleId: Long, token: String?): String {
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.whackMole",
            RpcRequestData.array {
                put("moleId", moleId)
                put("token", token)
                putStandard(source = "senlinguangchangdadishu", version = VERSION)
            }
        )
    }

    /**
     * 兼容模式打单个地鼠
     */
    @JvmStatic
    suspend fun oldwhackMole(moleId: Long, token: String?, source: String?): String {
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.whackMole",
            RpcRequestData.array {
                put("moleId", moleId)
                put("token", token ?: "null")
                putStandard(source = source ?: "null", version = VERSION)
            }
        )
    }

    @JvmStatic
    @Throws(JSONException::class)
    suspend fun settlementWhackMole(token: String?): String {
        // moleIdList 改为 1 ,20（包含 1-20）
        val moleIdList: List<Int> = (1..15).toList()
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.settlementWhackMole",
            RpcRequestData.array {
                put("moleIdList", JSONArray(moleIdList))
                put("settlementScene", "NORMAL")
                put("token", token)
                putStandard(source = "senlinguangchangdadishu", version = VERSION)
            }
        )
    }

    //兼容模式结算
    @JvmStatic
    suspend fun oldsettlementWhackMole(
        token: String?,
        moleIdList: List<String?>?,
        source: String?
    ): String {
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.settlementWhackMole",
            RpcRequestData.array {
                // 原 moleIdList 为无引号 joinToString 数字数组，转 Int 保持数字类型
                put("moleIdList", JSONArray(moleIdList!!.mapNotNull { it?.toIntOrNull() }))
                put("settlementScene", "NORMAL")
                put("token", token ?: "null")
                putStandard(source = source ?: "null", version = VERSION)
            }
        )
    }

    /** 森林集市 */
    @JvmStatic
    suspend fun consultForSendEnergyByAction(sourceType: String?): String {
        return RequestManager.requestString(
            "alipay.bizfmcg.greenlife.consultForSendEnergyByAction",
            RpcRequestData.array {
                put("sourceType", sourceType ?: "null")
            }
        )
    }

    /** 森林集市 */
    @JvmStatic
    suspend fun sendEnergyByAction(sourceType: String?): String {
        return RequestManager.requestString(
            "alipay.bizfmcg.greenlife.sendEnergyByAction",
            RpcRequestData.array {
                put("actionType", "GOODS_BROWSE")
                put("requestId", RandomUtil.getRandomString(8))
                put("sourceType", sourceType ?: "null")
            }
        )
    }

    /** 翻倍额外能量收取 */
    @JvmStatic
    suspend fun collectRobExpandEnergy(propId: String?, propType: String?): String {
        return RequestManager.requestString(
            "alipay.antforest.forest.h5.collectRobExpandEnergy",
            RpcRequestData.array {
                put("propId", propId ?: "null")
                put("propType", propType ?: "null")
                putStandard(source = "chInfo_ch_appcenter__chsub_9patch")
            }
        )
    }

    @JvmStatic
    @Throws(JSONException::class)
    suspend fun studentQqueryCheckInModel(): String {
        return RequestManager.requestString(
            "alipay.membertangram.biz.rpc.student.queryCheckInModel",
            RpcRequestData.array {
                put("chInfo", "ch_appcollect__chsub_my-recentlyUsed")
                put("skipTaskModule", false)
            }
        )
    }

    /*青春特权领红包*/
    @JvmStatic
    @Throws(JSONException::class)
    suspend fun studentCheckin(): String {
        return RequestManager.requestString(
            "alipay.membertangram.biz.rpc.student.checkIn",
            RpcRequestData.array {
                putStandard(source = "chInfo_ch_appcenter__chsub_9patch")
            }
        )
    }

    /** 查询绿色行动 */
    @JvmStatic
    suspend fun ecolifeQueryHomePage(): String {
        return RequestManager.requestString(
            "alipay.ecolife.rpc.h5.queryHomePage",
            RpcRequestData.array {
                put("channel", "ALIPAY")
                putStandard(source = "search_brandbox")
            }
        )
    }

    /** 开通绿色行动 */
    @JvmStatic
    suspend fun ecolifeOpenEcolife(): String {
        return RequestManager.requestString(
            "alipay.ecolife.rpc.h5.openEcolife",
            RpcRequestData.array {
                put("channel", "ALIPAY")
                putStandard(source = "renwuGD")
            }
        )
    }

    /** 执行任务 */
    @JvmStatic
    suspend fun ecolifeTick(actionId: String?, dayPoint: String?, source: String?): String {
        return RequestManager.requestString(
            "alipay.ecolife.rpc.h5.tick",
            RpcRequestData.array {
                put("actionId", actionId ?: "null")
                put("channel", "ALIPAY")
                put("dayPoint", dayPoint ?: "null")
                put("generateEnergy", false)
                putStandard(source = source ?: "null")
            }
        )
    }

    /** 查询任务信息 */
    @JvmStatic
    suspend fun ecolifeQueryDish(source: String?, dayPoint: String?): String {
        return RequestManager.requestString(
            "alipay.ecolife.rpc.h5.queryDish",
            RpcRequestData.array {
                put("channel", "ALIPAY")
                put("dayPoint", dayPoint ?: "null")
                putStandard(source = source ?: "null")
            }
        )
    }

    /** 上传照片 */
    @JvmStatic
    suspend fun ecolifeUploadDishImage(
        operateType: String?, imageId: String?,
        conf1: Double, conf2: Double, conf3: Double, dayPoint: String?
    ): String {
        return RequestManager.requestString(
            "alipay.ecolife.rpc.h5.uploadDishImage",
            RpcRequestData.array {
                put("channel", "ALIPAY")
                put("dayPoint", dayPoint ?: "null")
                putStandard(source = "photo-comparison")
                put(
                    "uploadParamMap",
                    JSONObject()
                        .put(
                            "AIResult",
                            JSONArray()
                                .put(
                                    JSONObject()
                                        .put("conf", conf1)
                                        .put("kvPair", false)
                                        .put("label", "other")
                                        .put(
                                            "pos",
                                            JSONArray(
                                                listOf(
                                                    1.0002995,
                                                    0.22104378,
                                                    0.0011976048,
                                                    0.77727276
                                                )
                                            )
                                        )
                                        .put("value", "")
                                )
                                .put(
                                    JSONObject()
                                        .put("conf", conf2)
                                        .put("kvPair", false)
                                        .put("label", "guangpan")
                                        .put(
                                            "pos",
                                            JSONArray(
                                                listOf(
                                                    1.0002995,
                                                    0.22104378,
                                                    0.0011976048,
                                                    0.77727276
                                                )
                                            )
                                        )
                                        .put("value", "")
                                )
                                .put(
                                    JSONObject()
                                        .put("conf", conf3)
                                        .put("kvPair", false)
                                        .put("label", "feiguangpan")
                                        .put(
                                            "pos",
                                            JSONArray(
                                                listOf(
                                                    1.0002995,
                                                    0.22104378,
                                                    0.0011976048,
                                                    0.77727276
                                                )
                                            )
                                        )
                                        .put("value", "")
                                )
                        )
                        .put("existAIResult", true)
                        .put("imageId", imageId ?: "null")
                        .put(
                            "imageUrl",
                            "https://mdn.alipayobjects.com/afts/img/" + (imageId
                                ?: "null") + "/original?bz=APM_20000067"
                        )
                        .put("operateType", operateType ?: "null")
                )
            }
        )
    }

    // 查询森林能量
    @JvmStatic
    suspend fun queryForestEnergy(scene: String?): String {
        return RequestManager.requestString(
            "alipay.iblib.channel.data",
            RpcRequestData.array {
                put("activityCode", "query_forest_energy")
                put("activityId", "2024052300762675")
                put("body", JSONObject().put("scene", scene ?: "null"))
                putStandard(version = "2.0")
            }
        )
    }

    // 生成森林能量
    @JvmStatic
    suspend fun produceForestEnergy(scene: String?): String {
        val uniqueId = System.currentTimeMillis()
        return RequestManager.requestString(
            "alipay.iblib.channel.data",
            RpcRequestData.array {
                put("activityCode", "produce_forest_energy")
                put("activityId", "2024052300762674")
                put(
                    "body",
                    JSONObject()
                        .put("scene", scene ?: "null")
                        .put("uniqueId", uniqueId.toString())
                )
                putStandard(version = "2.0")
            }
        )
    }

    // 领取森林能量
    @JvmStatic
    suspend fun harvestForestEnergy(scene: String?, bubbles: JSONArray?): String {
        return RequestManager.requestString(
            "alipay.iblib.channel.data",
            RpcRequestData.array {
                put("activityCode", "harvest_forest_energy")
                put("activityId", "2024052300762676")
                put(
                    "body",
                    JSONObject()
                        .put("bubbles", bubbles ?: JSONObject.NULL)
                        .put("scene", scene ?: "null")
                )
                putStandard(version = "2.0")
            }
        )
    }

    // ==================== 森林抽抽乐相关方法（最终修复版） ====================

    /** 森林抽抽乐-活动列表（最终修复版） 根据抓包日志，正确的参数结构应该是直接传递参数，不需要requestData包装 */
    @JvmStatic
    @Throws(JSONException::class)
    suspend fun enterDrawActivityopengreen(
        activityId: String?,
        sceneCode: String?,
        source: String?
    ): String {
        // 根据抓包日志，正确的参数结构是直接传递，不需要requestData包装
        val requestData = RpcRequestData.array {
            if (!activityId.isNullOrEmpty()) {
                put("activityId", activityId)
            } else {
                put("activityId", "")
            }
            putStandard(
                requestType = RpcConst.Type.RPC,
                sceneCode = sceneCode,
                source = source
            )
        }
        Log.record(
            "AntForestRpcCall",
            "enterDrawActivityopengreen - 活动: $activityId, 场景: $sceneCode, source: $source"
        )
        return RequestManager.requestString(
            "com.alipay.antiepdrawprod.enterDrawActivityopengreen",
            requestData
        )
    }

    /** 森林抽抽乐-请求任务列表（最终修复版） */
    @JvmStatic
    @Throws(JSONException::class)
    suspend fun listTaskopengreen(sceneCode: String?, source: String?): String {
        // 根据抓包日志，正确的参数结构是直接传递，不需要requestData包装
        val requestData = RpcRequestData.array {
            putStandard(
                requestType = RpcConst.Type.RPC,
                sceneCode = sceneCode,
                source = source
            )
        }
        Log.record("AntForestRpcCall", "listTaskopengreen - 场景: $sceneCode, source: $source")
        return RequestManager.requestString("com.alipay.antieptask.listTaskopengreen", requestData)
    }

    /** 森林抽抽乐-抽奖（最终修复版） */
    @JvmStatic
    @Throws(JSONException::class)
    suspend fun drawopengreen(
        activityId: String?,
        sceneCode: String?,
        source: String?,
        userId: String?
    ): String {
        // 根据抓包日志，正确的参数结构是直接传递，不需要requestData包装
        val requestData = RpcRequestData.array {
            put("activityId", activityId)
            put("userId", userId)
            putStandard(
                requestType = RpcConst.Type.RPC,
                sceneCode = sceneCode,
                source = source
            )
        }
        Log.record(
            "AntForestRpcCall",
            "drawopengreen - 活动: $activityId, 场景: $sceneCode, source: $source"
        )
        return RequestManager.requestString("com.alipay.antiepdrawprod.drawopengreen", requestData)
    }

    /** 森林抽抽乐-签到领取次数（最终修复版） */
    @JvmStatic
    @Throws(JSONException::class)
    suspend fun receiveTaskAwardopengreen(
        source: String?,
        sceneCode: String?,
        taskType: String?
    ): String {
        // 根据抓包日志，正确的参数结构是直接传递，不需要requestData包装
        val requestData = RpcRequestData.array {
            put("ignoreLimit", true)
            put("taskType", taskType)
            putStandard(
                requestType = RpcConst.Type.RPC,
                sceneCode = sceneCode,
                source = source
            )
        }
        Log.record(
            "AntForestRpcCall",
            "receiveTaskAwardopengreen - 任务: $taskType, source: $source"
        )
        return RequestManager.requestString(
            "com.alipay.antieptask.receiveTaskAwardopengreen",
            requestData
        )
    }

    /** 森林抽抽乐-任务-活力值兑换抽奖次数（最终修复版） */
    @JvmStatic
    @Throws(JSONException::class)
    suspend fun exchangeTimesFromTaskopengreen(
        activityId: String?,
        sceneCode: String?,
        source: String?,
        taskSceneCode: String?,
        taskType: String?
    ): String {
        // 根据抓包日志，正确的参数结构是直接传递，不需要requestData包装
        val requestData = RpcRequestData.array {
            put("activityId", activityId)
            put("taskSceneCode", taskSceneCode)
            put("taskType", taskType)
            putStandard(
                requestType = RpcConst.Type.RPC,
                sceneCode = sceneCode,
                source = source
            )
        }
        Log.record(
            "AntForestRpcCall",
            "exchangeTimesFromTaskopengreen - 活动: $activityId, 任务: $taskType, source: $source"
        )
        return RequestManager.requestString(
            "com.alipay.antiepdrawprod.exchangeTimesFromTaskopengreen",
            requestData
        )
    }

    /** 森林抽抽乐-任务-广告（支持普通版和活动版） */
    @JvmStatic
    @Throws(JSONException::class)
    suspend fun finishTask4Chouchoule(taskType: String, sceneCode: String?): String {
        val params = RpcRequestData.array {
            put("outBizNo", taskType + RandomUtil.getRandomTag())
            put("taskType", taskType)
            putStandard(requestType = RpcConst.Type.RPC, sceneCode = sceneCode)

            // 根据任务类型设置不同的source
            if (taskType.contains("XLIGHT")) {
                put("source", "ADBASICLIB")
            } else if (taskType.startsWith("FOREST_ACTIVITY_DRAW")) {
                put("source", "task_entry") // 活动版任务使用task_entry
            } else {
                put("source", "task_entry") // 默认使用task_entry
            }
        }
        Log.record("AntForestRpcCall", "finishTask4Chouchoule - 任务: $taskType")
        return RequestManager.requestString("com.alipay.antiep.finishTask", params)
    }

    /** 完成森林抽抽乐任务（支持普通版和活动版） */
    @JvmStatic
    @Throws(JSONException::class)
    suspend fun finishTaskopengreen(taskType: String, sceneCode: String?): String {
        val params = RpcRequestData.array {
            put("outBizNo", taskType + RandomUtil.getRandomTag())
            put("taskType", taskType)
            putStandard(
                requestType = RpcConst.Type.RPC,
                sceneCode = sceneCode,
                source = "task_entry"
            )
        }
        Log.record("AntForestRpcCall", "finishTaskopengreen - 任务: $taskType")
        return RequestManager.requestString("com.alipay.antieptask.finishTaskopengreen", params)
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
    suspend fun queryGameList(): String {
        return RequestManager.requestString(
            "com.alipay.charitygamecenter.queryGameList",
            RpcRequestData.array {
                put("bizType", "ANTFOREST")
                put(
                    "commonDegradeFilterRequest",
                    JSONObject()
                        .put("deviceLevel", "high")
                        .put("platform", "Android")
                        .put("unityDeviceLevel", "high")
                )
                putStandard(
                    requestType = RpcConst.Type.RPC,
                    sceneCode = "ANTFOREST",
                    source = "chInfo_ch_appcenter__chsub_9patch",
                    version = VERSION
                )
            }
        )
    }

    /**
     * 领取游戏中心奖励 (批量开宝箱)
     * @param batchDrawCount 批量领取的次数 (例如 1 或 10)
     */
    @JvmStatic
    suspend fun drawGameCenterAward(batchDrawCount: Int): String {
        return RequestManager.requestString(
            "com.alipay.charitygamecenter.drawGameCenterAward",
            RpcRequestData.array {
                put("batchDrawCount", batchDrawCount)
                put("bizType", "ANTFOREST")
                putStandard(
                    requestType = RpcConst.Type.RPC,
                    sceneCode = "ANTFOREST",
                    source = "leyuan",
                    version = VERSION
                )
            }
        )
    }

    /** 初始化/上报游戏任务 */
    @JvmStatic
    suspend fun initTask(taskType: String?): String {
        // 生成类似 GAME_DONE_SLJYD_1769062463227_569cf36c 的 outBizNo
        val timestamp = System.currentTimeMillis().toString()
        val randomSuffix = UUID.randomUUID().toString().substring(0, 8)
        val outBizNo = taskType + "_" + timestamp + "_" + randomSuffix

        return RequestManager.requestString(
            "com.alipay.antiep.initTask",
            RpcRequestData.array {
                put("outBizNo", outBizNo)
                put("taskType", taskType ?: "null")
                putStandard(
                    requestType = RpcConst.Type.H5,
                    sceneCode = "ANTFOREST_ENERGY_RAIN_TASK",
                    source = "ANTFOREST"
                )
            }
        )
    }

    /** 查询森林乐园限定活动 */
    @JvmStatic
    suspend fun queryOptionalPlay(): String {
        return RequestManager.requestString(
            "com.alipay.charitygamecenter.queryOptionalPlay",
            RpcRequestData.array {
                put("bizType", "ANTFOREST")
                put(
                    "commonDegradeFilterRequest",
                    JSONObject()
                        .put("appMode", "normal")
                        .put("deviceLevel", "high")
                        .put("platform", "Android")
                        .put("unityDeviceLevel", "high")
                )
                put("playTypeList", JSONArray(listOf("TASK_TRIGGER", "TOP_UP_COUPON")))
                put("recentAppRecordList", JSONArray())
                putStandard(
                    requestType = RpcConst.Type.RPC,
                    sceneCode = "ANTFOREST_COMMON",
                    source = "chInfo_ch_appcenter__chsub_9patch",
                    version = VERSION
                )
            }
        )
    }

    /** 查询 1V1 能量挑战入口信息 */
    @JvmStatic
    suspend fun queryEnergyPvpInfo(): String {
        try {
            return RequestManager.requestString(
                "alipay.antforest.forest.h5.queryMiscInfo",
                RpcRequestData.array {
                    put("extInfo", JSONObject().put("checkReward", true).toString())
                    put("queryBizType", "energyPvpInfo")
                    putStandard(source = "chInfo_ch_appcenter__chsub_9patch")
                }
            )
        } catch (e: JSONException) {
            Log.printStackTrace("AntForestRpcCall", "构造 1V1 入口请求失败", e)
            return ""
        }
    }

    /** 查询 1V1 当前和上一场记录 */
    @JvmStatic
    suspend fun queryPvpHomeInfo(): String {
        try {
            return RequestManager.requestString(
                "alipay.antforest.forest.h5.queryPvpHomeInfo",
                RpcRequestData.array {
                    put("queryWaitToReceive", true)
                    putStandard(source = "chInfo_ch_appcenter__chsub_9patch")
                }
            )
        } catch (e: JSONException) {
            Log.printStackTrace("AntForestRpcCall", "构造 1V1 主页请求失败", e)
            return ""
        }
    }

    /** 领取已结算的 1V1 奖励 */
    @JvmStatic
    suspend fun receivePvpRewards(): String {
        try {
            return RequestManager.requestString(
                "alipay.antforest.forest.h5.receivePvpRewards",
                RpcRequestData.array {
                    putStandard(source = "chInfo_ch_appcenter__chsub_9patch")
                }
            )
        } catch (e: JSONException) {
            Log.printStackTrace("AntForestRpcCall", "构造 1V1 领奖请求失败", e)
            return ""
        }
    }

    /** 查询 1V1 历史记录，用于领奖后复查 */
    @JvmStatic
    suspend fun queryPvpBattleRecords(pageSize: Int): String {
        try {
            return RequestManager.requestString(
                "alipay.antforest.forest.h5.queryPvpBattleRecords",
                RpcRequestData.array {
                    put("pageSize", Math.max(1, pageSize))
                    putStandard(source = "chInfo_ch_appcenter__chsub_9patch")
                }
            )
        } catch (e: JSONException) {
            Log.printStackTrace("AntForestRpcCall", "构造 1V1 记录请求失败", e)
            return ""
        }
    }
}
