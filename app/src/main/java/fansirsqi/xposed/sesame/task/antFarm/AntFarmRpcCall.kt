package fansirsqi.xposed.sesame.task.antFarm

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.UUID
import fansirsqi.xposed.sesame.hook.RequestManager
import fansirsqi.xposed.sesame.hook.rpc.RpcRequestData
import fansirsqi.xposed.sesame.hook.rpc.RpcConst
import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.core.util.RandomUtil

object AntFarmRpcCall {
    private const val VERSION = "1.8.2302070202.46"

    /**
     * 进入农场
     *
     * @param userId       自己的用户id
     * @param targetUserId 所在农场的用户id
     * @return 返回结果
     * @throws JSONException 异常内容
     */
    @JvmStatic
    @Throws(JSONException::class)
    suspend fun enterFarm(userId: String?, targetUserId: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.enterFarm",
            RpcRequestData.array {
                put("animalId", "")
                put("bizCode", "")
                put("gotoneScene", "")
                put("gotoneTemplateId", "")
                put("groupId", "")
                put("growthExtInfo", "")
                put("inviteUserId", "")
                put("masterFarmId", "")
                put("queryLastRecordNum", true)
                put("recall", false)
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("shareId", "")
                put("shareUniqueId", "${System.currentTimeMillis()}_$targetUserId")
                put("source", "ANTFOREST")
                put("starFarmId", "")
                put("subBizCode", "")
                put("touchRecordId", "")
                put("userId", userId)
                put("userToken", "")
                put("version", VERSION)
            }
        )
    }

    // 一起拿小鸡饲料
    @JvmStatic
    suspend fun letsGetChickenFeedTogether(): String {
        return RequestManager.requestString(
            "com.alipay.antiep.canInvitePersonListP2P",
            RpcRequestData.array {
                // 注意：needHasInviteUserByCycle 原为字符串 "true"（抓包对齐），非布尔
                put("needHasInviteUserByCycle", "true")
                put("requestType", "RPC")
                put("sceneCode", "ANTFARM_P2P")
                put("source", "ANTFARM")
                put("startIndex", 0)
                put("version", VERSION)
            }
        )
    }

    // 赠送饲料
    @JvmStatic
    suspend fun giftOfFeed(bizTraceId: String?, userId: String?): String {
        return RequestManager.requestString(
            "com.alipay.antiep.inviteP2P",
            RpcRequestData.array {
                put("beInvitedUserId", userId ?: "null")
                put("bizTraceId", bizTraceId ?: "null")
                put("requestType", "RPC")
                put("sceneCode", "ANTFARM_P2P")
                put("source", "ANTFARM")
                put("version", VERSION)
            }
        )
    }

    @JvmStatic
    @Throws(JSONException::class)
    suspend fun syncAnimalStatus(farmId: String?, operTag: String?, operType: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.syncAnimalStatus",
            RpcRequestData.array {
                put("farmId", farmId ?: "null")
                put("operTag", operTag ?: "null")
                put("operType", operType ?: "null")
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "H5")
                put("version", VERSION)
            }
        )
    }

    @JvmStatic
    suspend fun sleep(): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.sleep",
            RpcRequestData.array {
                put("requestType", "RPC")
                put("sceneCode", "ANTFARM")
                put("source", "LOVECABIN")
                put("version", "unknown")
            }
        )
    }

    /**
     * 家庭睡觉
     *
     * @param groupId 家庭ID
     * @return 返回结果
     */
    @JvmStatic
    suspend fun sleep(groupId: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.sleep",
            RpcRequestData.array {
                put("groupId", groupId ?: "null")
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "H5")
                put("spaceType", "ChickFamily")
                put("version", "unknown")
            }
        )
    }

    @JvmStatic
    suspend fun wakeUp(): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.wakeUp",
            RpcRequestData.array {
                put("requestType", "RPC")
                put("sceneCode", "ANTFARM")
                put("source", "LOVECABIN")
                put("version", "unknown")
            }
        )
    }

    @JvmStatic
    suspend fun rewardFriend(consistencyKey: String?, friendId: String?, productNum: String?, time: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.rewardFriend",
            RpcRequestData.array {
                put("canMock", true)
                put("consistencyKey", consistencyKey ?: "null")
                put("friendId", friendId ?: "null")
                put("operType", "1")
                // productNum/time 原为无引号插值（JSON 数字形态），toBigDecimalOrNull 保持数字类型（兼容整数与小数）
                put("productNum", productNum?.toBigDecimalOrNull() ?: JSONObject.NULL)
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "H5")
                put("time", time?.toBigDecimalOrNull() ?: JSONObject.NULL)
                put("version", VERSION)
            }
        )
    }

    @JvmStatic
    suspend fun recallAnimal(animalId: String?, currentFarmId: String?, masterFarmId: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.recallAnimal",
            RpcRequestData.array {
                put("animalId", animalId ?: "null")
                put("currentFarmId", currentFarmId ?: "null")
                put("masterFarmId", masterFarmId ?: "null")
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "H5")
                put("version", VERSION)
            }
        )
    }

    @JvmStatic
    suspend fun orchardRecallAnimal(animalId: String?, userId: String?): String {
        return RequestManager.requestString(
            "com.alipay.antorchard.recallAnimal",
            RpcRequestData.array {
                put("animalId", animalId ?: "null")
                put("orchardUserId", userId ?: "null")
                put("requestType", "NORMAL")
                put("sceneCode", "ORCHARD")
                put("source", "zhuangyuan_zhaohuixiaoji")
                put("version", "0.1.2403061630.6")
            }
        )
    }

    @JvmStatic
    suspend fun sendBackAnimal(sendType: String?, animalId: String?, currentFarmId: String?, masterFarmId: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.sendBackAnimal",
            RpcRequestData.array {
                put("animalId", animalId ?: "null")
                put("currentFarmId", currentFarmId ?: "null")
                put("masterFarmId", masterFarmId ?: "null")
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("sendType", sendType ?: "null")
                put("source", "H5")
                put("version", VERSION)
            }
        )
    }

    @JvmStatic
    suspend fun harvestProduce(farmId: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.harvestProduce",
            RpcRequestData.array {
                put("canMock", true)
                put("farmId", farmId ?: "null")
                put("giftType", "")
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "H5")
                put("version", VERSION)
            }
        )
    }

    @JvmStatic
    suspend fun listActivityInfo(): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.listActivityInfo",
            RpcRequestData.array {
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "H5")
                put("version", VERSION)
            }
        )
    }

    @JvmStatic
    suspend fun donation(activityId: String?, donationAmount: Int): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.donation",
            RpcRequestData.array {
                put("activityId", activityId ?: "null")
                put("donationAmount", donationAmount)
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "H5")
                put("version", VERSION)
            }
        )
    }

    @JvmStatic
    suspend fun listFarmTask(): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.listFarmTask",
            RpcRequestData.array {
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "H5")
                put("version", VERSION)
            }
        )
    }

    @JvmStatic
    suspend fun receiveFarmTaskAward(taskId: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.receiveFarmTaskAward",
            RpcRequestData.array {
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "H5")
                put("taskId", taskId ?: "null")
                put("version", VERSION)
            }
        )
    }

    @JvmStatic
    suspend fun listToolTaskDetails(): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.listToolTaskDetails",
            RpcRequestData.array {
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "H5")
                put("version", VERSION)
            }
        )
    }

    @JvmStatic
    suspend fun receiveToolTaskReward(rewardType: String?, rewardCount: Int, taskType: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.receiveToolTaskReward",
            RpcRequestData.array {
                put("ignoreLimit", false)
                put("requestType", "NORMAL")
                put("rewardCount", rewardCount)
                put("rewardType", rewardType ?: "null")
                put("sceneCode", "ANTFARM")
                put("source", "H5")
                put("taskType", taskType ?: "null")
                put("version", VERSION)
            }
        )
    }

    @JvmStatic
    @Throws(JSONException::class)
    suspend fun feedAnimal(farmId: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.feedAnimal",
            RpcRequestData.array {
                put("animalType", "CHICK")
                put("canMock", true)
                put("farmId", farmId ?: "null")
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "chInfo_ch_appcollect__chsub_my-recentlyUsed")
                put("version", VERSION)
            }
        )
    }

    @JvmStatic
    suspend fun listFarmTool(): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.listFarmTool",
            RpcRequestData.array {
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "H5")
                put("version", VERSION)
            }
        )
    }

    @JvmStatic
    suspend fun useFarmTool(targetFarmId: String?, toolId: String?, toolType: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.useFarmTool",
            RpcRequestData.array {
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "H5")
                put("targetFarmId", targetFarmId ?: "null")
                put("toolId", toolId ?: "null")
                put("toolType", toolType ?: "null")
                put("version", VERSION)
            }
        )
    }

    @JvmStatic
    suspend fun rankingList(pageStartSum: Int): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.rankingList",
            RpcRequestData.array {
                put("pageSize", 20)
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "H5")
                put("startNum", pageStartSum)
                put("version", VERSION)
            }
        )
    }

    @JvmStatic
    suspend fun notifyFriend(animalId: String?, notifiedFarmId: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.notifyFriend",
            RpcRequestData.array {
                put("animalId", animalId ?: "null")
                put("animalType", "CHICK")
                put("canBeGuest", true)
                put("notifiedFarmId", notifiedFarmId ?: "null")
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "H5")
                put("version", VERSION)
            }
        )
    }

    @JvmStatic
    @Throws(JSONException::class)
    suspend fun feedFriendAnimal(friendFarmId: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.feedFriendAnimal",
            RpcRequestData.array {
                put("friendFarmId", friendFarmId ?: "null")
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "chInfo_ch_appcenter__chsub_9patch")
                put("version", VERSION)
            }
        )
    }

    @JvmStatic
    fun farmId2UserId(farmId: String?): String {
        val l = farmId!!.length / 2
        return farmId.substring(l)
    }

    /**
     * 收集肥料
     *
     * @param manurePotNO 肥料袋号
     * @return 返回结果
     */
    @JvmStatic
    suspend fun collectManurePot(manurePotNO: String?): String {
        //        "isSkipTempLimit":true, 肥料满了也强行收取，解决 农场未开通 打扫鸡屎失败问题
        return RequestManager.requestString(
            "com.alipay.antfarm.collectManurePot",
            RpcRequestData.array {
                put("isSkipTempLimit", true)
                put("manurePotNOs", manurePotNO ?: "null")
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "H5")
                put("version", VERSION)
            }
        )
    }

    @JvmStatic
    suspend fun sign(): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.sign",
            RpcRequestData.array {
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "H5")
                put("version", VERSION)
            }
        )
    }

    @JvmStatic
    suspend fun initFarmGame(gameType: String?): String {
        if ("flyGame" == gameType) {
            return RequestManager.requestString(
                "com.alipay.antfarm.initFarmGame",
                RpcRequestData.array {
                    put("gameType", "flyGame")
                    put("requestType", "RPC")
                    put("sceneCode", "FLAYGAME")
                    put("source", "FARM_game_yundongfly")
                    put("toolTypes", "ACCELERATETOOL,SHARETOOL,NONE")
                    put("version", "")
                }
            )
        } else if ("hitGame" == gameType) {
            return RequestManager.requestString(
                "com.alipay.antfarm.initFarmGame",
                RpcRequestData.array {
                    put("gameType", "hitGame")
                    put("requestType", "RPC")
                    put("sceneCode", "HITGAME")
                    put("source", "FARM_game_zouxiaoji")
                    put("toolTypes", "ACCELERATETOOL,SHARETOOL,NONE")
                    put("version", "")
                }
            )
        }
        return RequestManager.requestString(
            "com.alipay.antfarm.initFarmGame",
            RpcRequestData.array {
                put("gameType", gameType ?: "null")
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "H5")
                put("toolTypes", "STEALTOOL,ACCELERATETOOL,SHARETOOL")
            }
        )
    }

    @JvmStatic
    fun RandomScore(str: String?): Int {
        return if ("starGame" == str) {
            RandomUtil.nextInt(300, 400)
        } else if ("jumpGame" == str) {
            RandomUtil.nextInt(250, 270) * 10
        } else if ("flyGame" == str) {
            RandomUtil.nextInt(4000, 8000)
        } else if ("hitGame" == str) {
            RandomUtil.nextInt(80, 120)
        } else {
            210
        }
    }

    @JvmStatic
    suspend fun recordFarmGame(gameType: String?): String {
        val uuid = getUuid()
        val md5String = getMD5(uuid)
        val score = RandomScore(gameType)
        if ("flyGame" == gameType) {
            val foodCount = score / 50
            return RequestManager.requestString(
                "com.alipay.antfarm.recordFarmGame",
                RpcRequestData.array {
                    put("foodCount", foodCount)
                    put("gameType", "flyGame")
                    put("md5", md5String)
                    put("requestType", "RPC")
                    put("sceneCode", "FLAYGAME")
                    put("score", score)
                    put("source", "ANTFARM")
                    put("toolTypes", "ACCELERATETOOL,SHARETOOL,NONE")
                    put("uuid", uuid)
                    put("version", "")
                }
            )
        } else if ("hitGame" == gameType) {
            return RequestManager.requestString(
                "com.alipay.antfarm.recordFarmGame",
                RpcRequestData.array {
                    put("gameType", "hitGame")
                    put("md5", md5String)
                    put("requestType", "RPC")
                    put("sceneCode", "HITGAME")
                    put("score", score)
                    put("source", "ANTFARM")
                    put("toolTypes", "ACCELERATETOOL,SHARETOOL,NONE")
                    put("uuid", uuid)
                    put("version", "")
                }
            )
        }
        return RequestManager.requestString(
            "com.alipay.antfarm.recordFarmGame",
            RpcRequestData.array {
                put("gameType", gameType ?: "null")
                put("md5", md5String)
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("score", score)
                put("source", "H5")
                put("toolTypes", "STEALTOOL,ACCELERATETOOL,SHARETOOL")
                put("uuid", uuid)
            }
        )
    }

    private fun getUuid(): String {
        val sb = StringBuilder()
        for (str in UUID.randomUUID().toString().split("-")) {
            sb.append(str.substring(str.length / 2))
        }
        return sb.toString()
    }

    @JvmStatic
    fun getMD5(password: String): String {
        try {
            // 得到一个信息摘要器
            val digest = MessageDigest.getInstance("md5")
            val result = digest.digest(password.toByteArray())
            val buffer = StringBuilder()
            // 把没一个byte 做一个与运算 0xff;
            for (b in result) {
                // 与运算
                val number = b.toInt() and 0xff // 加盐
                val str = Integer.toHexString(number)
                if (str.length == 1) {
                    buffer.append("0")
                }
                buffer.append(str)
            }
            // 标准的md5加密后的结果
            return buffer.toString()
        } catch (e: NoSuchAlgorithmException) {
            Log.printStackTrace(e)
            return ""
        }
    }

    /**
     * 小鸡厨房 - 进厨房
     *
     * @param userId 用户id
     * @return 返回结果
     * @throws JSONException 异常
     */
    @JvmStatic
    @Throws(JSONException::class)
    suspend fun enterKitchen(userId: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.enterKitchen",
            RpcRequestData.array {
                put("requestType", "RPC")
                put("sceneCode", "ANTFARM")
                put("source", "VILLA")
                put("userId", userId ?: "null")
                put("version", "unknown")
            }
        )
    }

    @JvmStatic
    suspend fun collectDailyFoodMaterial(dailyFoodMaterialAmount: Int): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.collectDailyFoodMaterial",
            RpcRequestData.array {
                put("collectDailyFoodMaterialAmount", dailyFoodMaterialAmount)
                put("requestType", "RPC")
                put("sceneCode", "ANTFARM")
                put("source", "VILLA")
                put("version", "unknown")
            }
        )
    }

    @JvmStatic
    suspend fun queryFoodMaterialPack(): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.queryFoodMaterialPack",
            RpcRequestData.array {
                put("requestType", "RPC")
                put("sceneCode", "ANTFARM")
                put("source", "kitchen")
                put("version", "unknown")
            }
        )
    }

    @JvmStatic
    suspend fun collectDailyLimitedFoodMaterial(dailyLimitedFoodMaterialAmount: Int): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.collectDailyLimitedFoodMaterial",
            RpcRequestData.array {
                put("collectDailyLimitedFoodMaterialAmount", dailyLimitedFoodMaterialAmount)
                put("requestType", "RPC")
                put("sceneCode", "ANTFARM")
                put("source", "kitchen")
                put("version", "unknown")
            }
        )
    }

    @JvmStatic
    suspend fun farmFoodMaterialCollect(): String {
        return RequestManager.requestString(
            "com.alipay.antorchard.farmFoodMaterialCollect",
            RpcRequestData.array {
                put("collect", true)
                put("requestType", "RPC")
                put("sceneCode", "ORCHARD")
                put("source", "VILLA")
                put("version", "unknown")
            }
        )
    }

    /**
     * 小鸡厨房 - 做菜
     *
     * @param userId
     * @param source
     * @return
     * @throws JSONException
     */
    @JvmStatic
    @Throws(JSONException::class)
    suspend fun cook(userId: String?, source: String?): String {
        //[{"requestType":"RPC","sceneCode":"ANTFARM","source":"VILLA","userId":"2088522730162798","version":"unknown"}]
        return RequestManager.requestString(
            "com.alipay.antfarm.cook",
            RpcRequestData.array {
                put("requestType", "RPC")
                put("sceneCode", "ANTFARM")
                put("source", source ?: "null")
                put("userId", userId ?: "null")
                put("version", "unknown")
            }
        )
    }

    @JvmStatic
    suspend fun useFarmFood(cookbookId: String?, cuisineId: String?): String {
        try {
            return RequestManager.requestString(
                "com.alipay.antfarm.useFarmFood",
                RpcRequestData.array {
                    put("cookbookId", cookbookId ?: "null")
                    put("cuisineId", cuisineId ?: "null")
                    put("requestType", "NORMAL")
                    put("sceneCode", "ANTFARM")
                    put("canMock", true)
                    put("source", "chInfo_ch_appcenter__chsub_9patch")
                    put("useCuisine", true)
                    put("version", VERSION)
                }
            )
        } catch (e: JSONException) {
            return ""
        }
    }

    @JvmStatic
    suspend fun collectKitchenGarbage(): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.collectKitchenGarbage",
            RpcRequestData.array {
                put("requestType", "RPC")
                put("sceneCode", "ANTFARM")
                put("source", "VILLA")
                put("version", "unknown")
            }
        )
    }

    /* 日常任务 */
    @JvmStatic
    suspend fun doFarmTask(bizKey: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.doFarmTask",
            RpcRequestData.array {
                put("bizKey", bizKey ?: "null")
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "H5")
                put("version", VERSION)
            }
        )
    }

    @JvmStatic
    suspend fun queryTabVideoUrl(): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.queryTabVideoUrl",
            RpcRequestData.array {
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "H5")
                put("version", VERSION)
            }
        )
    }

    @JvmStatic
    suspend fun videoDeliverModule(bizId: String?): String {
        return RequestManager.requestString(
            "alipay.content.reading.life.deliver.module",
            RpcRequestData.array {
                put("bizId", bizId ?: "null")
                put("bizType", "CONTENT")
                put("chInfo", "ch_antFarm")
                put("refer", "antFarm")
                put("timestamp", "${System.currentTimeMillis()}")
            }
        )
    }

    @JvmStatic
    suspend fun videoTrigger(bizId: String?): String {
        return RequestManager.requestString(
            "alipay.content.reading.life.prize.trigger",
            RpcRequestData.array {
                put("bizId", bizId ?: "null")
                put("bizType", "CONTENT")
                put("prizeFlowNum", "VIDEO_TASK")
                put("prizeType", "farmFeed")
            }
        )
    }

    /* 惊喜礼包 */
    @JvmStatic
    suspend fun drawLotteryPlus(): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.drawLotteryPlus",
            RpcRequestData.array {
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                // 注意 source 原值带尾随空格 "H5 "（抓包对齐），保持原样
                put("source", "H5 ")
                put("version", "")
            }
        )
    }

    /* 小麦 */
    @JvmStatic
    suspend fun acceptGift(): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.acceptGift",
            RpcRequestData.array {
                put("ignoreLimit", false)
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "H5")
                put("version", VERSION)
            }
        )
    }

    @JvmStatic
    suspend fun visitFriend(friendFarmId: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.visitFriend",
            RpcRequestData.array {
                put("friendFarmId", friendFarmId ?: "null")
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "H5")
                put("version", VERSION)
            }
        )
    }

    /**
     * 小鸡日志当月日期查询
     *
     * @return
     */
    @JvmStatic
    suspend fun queryChickenDiaryList(): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.queryChickenDiaryList",
            RpcRequestData.array {
                put("requestType", "NORMAL")
                put("sceneCode", "DIARY")
                put("source", "antfarm_icon")
            }
        )
    }

    /**
     * 小鸡日志指定月份日期查询
     *
     * @param yearMonth 日期格式：yyyy-MM
     * @return
     */
    @JvmStatic
    suspend fun queryChickenDiaryList(yearMonth: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.queryChickenDiaryList",
            RpcRequestData.array {
                put("queryMonthStr", yearMonth ?: "null")
                put("requestType", "NORMAL")
                put("sceneCode", "DIARY")
                put("source", "antfarm_icon")
            }
        )
    }

    @JvmStatic
    suspend fun queryChickenDiary(queryDayStr: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.queryChickenDiary",
            RpcRequestData.array {
                put("queryDayStr", queryDayStr ?: "null")
                put("requestType", "NORMAL")
                put("sceneCode", "DIARY")
                put("source", "antfarm_icon")
            }
        )
    }

    @JvmStatic
    suspend fun diaryTietie(diaryDate: String?, roleId: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.diaryTietie",
            RpcRequestData.array {
                put("diaryDate", diaryDate ?: "null")
                put("requestType", "NORMAL")
                put("roleId", roleId ?: "null")
                put("sceneCode", "DIARY")
                put("source", "antfarm_icon")
            }
        )
    }

    /**
     * 小鸡日记点赞
     *
     * @param DiaryId 日记id
     * @return
     */
    @JvmStatic
    suspend fun collectChickenDiary(DiaryId: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.collectChickenDiary",
            RpcRequestData.array {
                put("collectStatus", true)
                put("diaryId", DiaryId ?: "null")
                put("requestType", "NORMAL")
                put("sceneCode", "MOOD")
                put("source", "H5")
            }
        )
    }

    @JvmStatic
    suspend fun visitAnimal(): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.visitAnimal",
            RpcRequestData.array {
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "H5")
                put("version", VERSION)
            }
        )
    }

    @JvmStatic
    suspend fun feedFriendAnimalVisit(friendFarmId: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.feedFriendAnimal",
            RpcRequestData.array {
                put("friendFarmId", friendFarmId ?: "null")
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "visitChicken")
                put("version", VERSION)
            }
        )
    }

    @JvmStatic
    suspend fun visitAnimalSendPrize(token: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.visitAnimalSendPrize",
            RpcRequestData.array {
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "H5")
                put("token", token ?: "null")
                put("version", VERSION)
            }
        )
    }

    @JvmStatic
    suspend fun hireAnimal(farmId: String?, animalId: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.hireAnimal",
            RpcRequestData.array {
                put("friendFarmId", farmId ?: "null")
                put("hireActionType", "HIRE_IN_FRIEND_FARM")
                put("hireAnimalId", animalId ?: "null")
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("sendCardChat", false)
                put("source", "H5")
                put("version", VERSION)
            }
        )
    }

    /**
     * 雇佣NPC小鸡（修复版：支持传入source）
     * @param animalId 动物ID
     * @param source 请求来源，如 "zhimaxiaoji_lianjin"
     */
    @JvmStatic
    @Throws(JSONException::class)
    suspend fun hireNpcAnimal(animalId: String?, source: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.hireAnimal",
            RpcRequestData.array {
                put("hireActionType", "HIRE_IN_SELF_FARM")
                put("hireAnimalId", animalId ?: "null")
                put("isNpcAnimal", true)
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", source ?: "null")
                put("version", VERSION)
            }
        )
    }

    /**
     * 遣返NPC小鸡（领取奖励）
     */
    @JvmStatic
    @Throws(JSONException::class)
    suspend fun sendBackNpcAnimal(animalId: String?, currentFarmId: String?, masterFarmId: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.sendBackAnimal",
            RpcRequestData.array {
                put("animalId", animalId ?: "null")
                put("currentFarmId", currentFarmId ?: "null")
                put("masterFarmId", masterFarmId ?: "null")
                put("receiveNPCReward", true) // 关键参数：领取奖励
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("sendType", "NORMAL")
                put("source", "H5")
                put("version", VERSION)
            }
        )
    }

    /**
     * 获取芝麻NPC任务列表  大表哥
     */
    @JvmStatic
    suspend fun listZhimaNpcFarmTask(): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.listFarmTask",
            RpcRequestData.array {
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "zhimaxiaoji_lianjin")
                put("taskSceneCode", "ANTFARM_ZHIMA_NPC_TASK")
                put("version", VERSION)
            }
        )
    }

    /**
     * 领取芝麻NPC任务奖励
     */
    @JvmStatic
    suspend fun receiveZhimaNpcFarmTaskAward(taskId: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.receiveFarmTaskAward",
            RpcRequestData.array {
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "zhimaxiaoji_lianjin")
                put("taskId", taskId ?: "null")
                put("taskSceneCode", "ANTFARM_ZHIMA_NPC_TASK")
                put("version", VERSION)
            }
        )
    }

    @JvmStatic
    suspend fun DrawPrize(): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.DrawPrize",
            RpcRequestData.array {
                put("requestType", "RPC")
                put("sceneCode", "ANTFARM")
                put("source", "chouchoule")
            }
        )
    }

    /**
     * 获取黄金小鸡任务列表
     * 对应日志中的 taskSceneCode: "ANTFARM_CAIFU_NPC_TASK"
     */
    @JvmStatic
    suspend fun listGoldChickenFarmTask(): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.listFarmTask",
            RpcRequestData.array {
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "H5")
                put("taskSceneCode", "ANTFARM_CAIFU_NPC_TASK")
                put("version", VERSION)
            }
        )
    }

    /**
     * 领取黄金小鸡任务奖励 (新增)
     * taskSceneCode: ANTFARM_CAIFU_NPC_TASK
     */
    @JvmStatic
    suspend fun receiveGoldChickenTaskAward(taskId: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.receiveFarmTaskAward",
            RpcRequestData.array {
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "H5")
                put("taskId", taskId ?: "null")
                put("taskSceneCode", "ANTFARM_CAIFU_NPC_TASK")
                put("version", VERSION)
            }
        )
    }

    /**
     * 理财体验金：查询活动首页
     */
    @JvmStatic
    suspend fun lctyj2025PromoIndex(): String {
        // 注意：headers 需要根据实际抓包补充，这里简化处理，通常 RPC 框架会自动处理 headers
        return RequestManager.requestString(
            "com.alipay.fundscenebff.needle.lctyj2025.promoIndex",
            RpcRequestData.array {
                put("blockFeature", JSONObject())
                put("isAdoutterPos", "N")
                put("pageType", "normal")
            }
        )
    }

    /**
     * 理财体验金：开启体验计划
     */
    @JvmStatic
    suspend fun lctyj2025OpenPlan(): String {
        // 构造模拟的请求数据，基于日志分析
        // 关键参数：conditionType, userPurchaseAmount, payChannelIndex
        return RequestManager.requestString(
            "com.alipay.fundscenebff.needle.lctyj2025.openPlan",
            RpcRequestData.array {
                put("conditionType", "STABLE_AND_EQUITY_BUY_ONE_POSITIVE_PROFIT")
                put(
                    "context",
                    JSONObject().apply {
                        put("AuthenticationType", "PASSWORD")
                        put("payChannelFullName", "余额宝")
                        put("payChannelIndex", "[\"FUND_DC_MONEYFUND_DEFAULT_ALIPAY_NULL\"]")
                        put("payChannelType", "MONEYFUND")
                        put("userPurchaseAmount", "10")
                    }
                )
                put("mismatch", "AGREE_MISMATCH")
                put("needCreatePackage", true)
                put("packageType", "bigAmountCamp")
            }
        )
    }

    /**
     * 黄金票任务：模拟进入黄金攒存页面（仅查询，不涉及交易）
     */
    @JvmStatic
    suspend fun queryGoldCollectionV2(): String {
        return RequestManager.requestString(
            "com.alipay.ficcbffweb.gold.collectionV2.query",
            RpcRequestData.array {
                put("pageSize", 10)
                put("queryType", "CREATE_PLAN")
                put("specifyGram", "2")
                put("subQueryType", "MAKE_PLAN")
            }
        )
    }

    /**
     * 获取农场小鸡(肥料鸡)任务列表
     * source: feiliaoji_202507
     * taskSceneCode: ANTFARM_ORCHARD_NPC_TASK
     */
    @JvmStatic
    suspend fun listFarmChickenFarmTask(): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.listFarmTask",
            RpcRequestData.array {
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "feiliaoji_202507")
                put("taskSceneCode", "ANTFARM_ORCHARD_NPC_TASK")
                put("version", VERSION)
            }
        )
    }

    /**
     * 领取农场小鸡任务奖励
     * awardType: NPC_ANIMAL_FOOD
     */
    @JvmStatic
    suspend fun receiveFarmChickenTaskAward(taskId: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.receiveFarmTaskAward",
            RpcRequestData.array {
                put("awardType", "NPC_ANIMAL_FOOD")
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "feiliaoji_202507")
                put("taskId", taskId ?: "null")
                put("taskSceneCode", "ANTFARM_ORCHARD_NPC_TASK")
                put("version", VERSION)
            }
        )
    }

    /**
     * 领取蚂蚁庄园游戏中心奖励 (开宝箱)
     * @param drawTimes 开启次数
     */
    @JvmStatic
    suspend fun drawGameCenterAward(drawTimes: Int): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.drawGameCenterAward",
            RpcRequestData.array {
                put("drawTimes", drawTimes)
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "H5")
                put("version", VERSION)
            }
        )
    }

    /**
     * 查询游戏列表 (如：蚂蚁农场、庄园等)
     * 对应 methodName: com.alipay.charitygamecenter.queryGameList
     */
    @JvmStatic
    suspend fun queryGameList(): String {
        return RequestManager.requestString(
            "com.alipay.charitygamecenter.queryGameList",
            RpcRequestData.array {
                put("bizType", "ANTFARM")
                put(
                    "commonDegradeFilterRequest",
                    JSONObject().apply {
                        put("deviceLevel", "high")
                        put("platform", "Android")
                        put("unityDeviceLevel", "high")
                    }
                )
                put("recentAppRecordList", JSONArray())
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "H5")
                put("version", VERSION)
            }
        )
    }

    // 小鸡换装
    @JvmStatic
    suspend fun listOrnaments(): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.listOrnaments",
            RpcRequestData.array {
                put("pageNo", "1")
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("setsType", "ACHIEVEMENTSETS")
                put("source", "H5")
                put("subType", "sets")
                put("type", "apparels")
                put("version", VERSION)
            }
        )
    }

    @JvmStatic
    suspend fun saveOrnaments(animalId: String?, farmId: String?, ornaments: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.saveOrnaments",
            RpcRequestData.array {
                put("animalId", animalId ?: "null")
                put("farmId", farmId ?: "null")
                put("ornaments", ornaments ?: "null")
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "H5")
                put("version", VERSION)
            }
        )
    }

    // 亲密家庭
    @JvmStatic
    suspend fun enterFamily(): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.enterFamily",
            RpcRequestData.array {
                put("fromAnn", false)
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "H5")
                put("timeZoneId", "Asia/Shanghai")
            }
        )
    }

    /**
     * 家庭任务入口 - 查询当前是否还有「道早安」等家庭任务
     *
     * 对应看我.txt 中：com.alipay.antfarm.familyTaskTips
     *
     * @param animals enterFamily 接口返回的家庭 animals 数组（原样透传给 RPC）
     */
    @JvmStatic
    suspend fun familyTaskTips(animals: JSONArray?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.familyTaskTips",
            RpcRequestData.array {
                // animals 原为无引号嵌入（JSONArray 文本），null 时原产出 JSON null
                put("animals", animals ?: JSONObject.NULL)
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "H5")
                put("taskSceneCode", "ANTFARM_FAMILY_TASK")
                put("timeZoneId", "Asia/Shanghai")
            }
        )
    }

    @JvmStatic
    suspend fun familyReceiveFarmTaskAward(taskId: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.receiveFarmTaskAward",
            RpcRequestData.array {
                put("awardType", "FAMILY_INTIMACY")
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "H5")
                put("taskId", taskId ?: "null")
                put("taskSceneCode", "ANTFARM_FAMILY_TASK")
            }
        )
    }

    @JvmStatic
    suspend fun familyAwardList(): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.familyAwardList",
            RpcRequestData.array {
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "H5")
            }
        )
    }

    @JvmStatic
    suspend fun receiveFamilyAward(rightId: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.receiveFamilyAward",
            RpcRequestData.array {
                put("requestType", "NORMAL")
                put("rightId", rightId ?: "null")
                put("sceneCode", "ANTFARM")
                put("source", "H5")
            }
        )
    }

    @JvmStatic
    suspend fun assignFamilyMember(assignAction: String?, beAssignUser: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.assignFamilyMember",
            RpcRequestData.array {
                put("assignAction", assignAction ?: "null")
                put("beAssignUser", beAssignUser ?: "null")
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "H5")
            }
        )
    }

    @JvmStatic
    suspend fun sendChat(chatCardType: String?, receiverUserId: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.sendChat",
            RpcRequestData.array {
                put("chatCardType", chatCardType ?: "null")
                put("receiverUserId", receiverUserId ?: "null")
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "H5")
            }
        )
    }

    @JvmStatic
    suspend fun deliverSubjectRecommend(friendUserIdList: JSONArray?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.deliverSubjectRecommend",
            RpcRequestData.array {
                // friendUserIds 原为无引号嵌入（JSONArray 文本），null 时原产出 JSON null
                put("friendUserIds", friendUserIdList ?: JSONObject.NULL)
                put("requestType", "NORMAL")
                put("sceneCode", "ChickFamily")
                put("source", "H5")
            }
        )
    }

    @JvmStatic
    @Throws(JSONException::class)
    suspend fun OpenAIPrivatePolicy(): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.OpenPrivatePolicy",
            RpcRequestData.array {
                put("privatePolicyIdList", JSONArray().put("AI_CHICK_PRIVATE_POLICY"))
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "H5")
                put("version", VERSION)
            }
        )
    }

    @JvmStatic
    @Throws(JSONException::class)
    suspend fun deliverContentExpand(
        ariverRpcTraceId: String?,
        eventId: String?,
        eventName: String?,
        memo: String?,
        resultCode: String?,
        sceneId: String?,
        sceneName: String?,
        success: Boolean,
        friendUserIdList: JSONArray?
    ): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.DeliverContentExpand",
            RpcRequestData.array {
                put("ariverRpcTraceId", ariverRpcTraceId ?: "null")
                put("eventId", eventId ?: "null")
                put("eventName", eventName ?: "null")
                put("friendUserIds", friendUserIdList)
                put("memo", memo ?: "null")
                put("requestType", "NORMAL")
                put("resultCode", resultCode ?: "null")
                put("sceneCode", "ANTFARM")
                put("sceneId", sceneId ?: "null")
                put("sceneName", sceneName ?: "null")
                put("source", "H5")
                put("success", success)
            }
        )
    }

    @JvmStatic
    @Throws(JSONException::class)
    suspend fun QueryExpandContent(deliverId: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.QueryExpandContent",
            RpcRequestData.array {
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "H5")
                put("deliverId", deliverId ?: "null")
            }
        )
    }

    @JvmStatic
    @Throws(JSONException::class)
    suspend fun deliverMsgSend(groupId: String?, friendUserIds: JSONArray?, content: String?, deliverId: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.DeliverMsgSend",
            RpcRequestData.array {
                put("content", content ?: "null")
                put("deliverId", deliverId ?: "null")
                put("friendUserIds", friendUserIds)
                put("groupId", groupId ?: "null")
                put("mode", "AI")
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "H5")
                put("spaceType", "ChickFamily")
            }
        )
    }

    @JvmStatic
    suspend fun syncFamilyStatus(groupId: String?, operType: String?, syncUserIds: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.syncFamilyStatus",
            RpcRequestData.array {
                put("groupId", groupId ?: "null")
                put("operType", operType ?: "null")
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "H5")
                // 原 syncUserIds 为单元素数组 ["$syncUserIds"]
                put("syncUserIds", JSONArray().put(syncUserIds ?: "null"))
            }
        )
    }

    @JvmStatic
    suspend fun inviteFriendVisitFamily(receiverUserId: JSONArray?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.inviteFriendVisitFamily",
            RpcRequestData.array {
                put("bizType", "FAMILY_SHARE")
                // receiverUserId 原为无引号嵌入（JSONArray 文本），null 时原产出 JSON null
                put("receiverUserId", receiverUserId ?: JSONObject.NULL)
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "H5")
            }
        )
    }

    @JvmStatic
    suspend fun familyEatTogether(groupId: String?, friendUserIdList: JSONArray?, cuisines: JSONArray?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.familyEatTogether",
            RpcRequestData.array {
                // cuisines/friendUserIds 原为无引号嵌入（JSONArray 文本），null 时原产出 JSON null
                put("cuisines", cuisines ?: JSONObject.NULL)
                put("friendUserIds", friendUserIdList ?: JSONObject.NULL)
                put("groupId", groupId ?: "null")
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "H5")
                put("spaceType", "ChickFamily")
            }
        )
    }

    @JvmStatic
    suspend fun queryRecentFarmFood(queryNum: Int): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.queryRecentFarmFood",
            RpcRequestData.array {
                put("queryNum", queryNum)
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "H5")
            }
        )
    }

    @JvmStatic
    suspend fun feedFriendAnimal(friendFarmId: String?, groupId: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.feedFriendAnimal",
            RpcRequestData.array {
                put("friendFarmId", friendFarmId ?: "null")
                put("groupId", groupId ?: "null")
                put("requestType", "NORMAL")
                put("sceneCode", "ChickFamily")
                put("source", "H5")
                put("spaceType", "ChickFamily")
            }
        )
    }

    @JvmStatic
    suspend fun queryFamilyDrawActivity(): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.queryFamilyDrawActivity",
            RpcRequestData.array {
                put("bizType", "ANTFARM_GAME_CENTER")
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "H5")
            }
        )
    }

    @JvmStatic
    suspend fun familyDraw(): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.familyDraw",
            RpcRequestData.array {
                put("bizType", "ANTFARM_GAME_CENTER")
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "H5")
            }
        )
    }

    @JvmStatic
    suspend fun familyBatchInviteP2P(inviteP2PVOList: JSONArray?, sceneCode: String?): String {
        return RequestManager.requestString(
            "com.alipay.antiep.batchInviteP2P",
            RpcRequestData.array {
                // inviteP2PVOList 原为无引号嵌入（JSONArray 文本），null 时原产出 JSON null
                put("inviteP2PVOList", inviteP2PVOList ?: JSONObject.NULL)
                put("requestType", "RPC")
                put("sceneCode", sceneCode ?: "null")
                put("source", "antfarm")
            }
        )
    }

    @JvmStatic
    suspend fun familyDrawSignReceiveFarmTaskAward(taskId: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.receiveFarmTaskAward",
            RpcRequestData.array {
                put("awardType", "FAMILY_DRAW_TIME")
                put("bizType", "ANTFARM_GAME_CENTER")
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "H5")
                put("taskId", taskId ?: "null")
                put("taskSceneCode", "ANTFARM_FAMILY_DRAW_TASK")
            }
        )
    }

    /**
     * 扭蛋任务查询好友列表
     */
    @JvmStatic
    @Throws(JSONException::class)
    suspend fun familyShareP2PPanelInfo(sceneCode: String?): String {
        return RequestManager.requestString(
            "com.alipay.antiep.shareP2PPanelInfo",
            RpcRequestData.array {
                put("requestType", "RPC")
                put("source", "antfarm")
                put("sceneCode", sceneCode ?: "null")
            }
        )
    }

    /**
     * 扭蛋任务列表
     */
    @JvmStatic
    suspend fun familyDrawListFarmTask(): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.listFarmTask",
            RpcRequestData.array {
                put("bizType", "ANTFARM_GAME_CENTER")
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM_FAMILY_DRAW_TASK")
                put("signSceneCode", "")
                put("source", "H5")
                put("taskSceneCode", "ANTFARM_FAMILY_DRAW_TASK")
            }
        )
    }

    @JvmStatic
    suspend fun giftFamilyDrawFragment(giftUserId: String?, giftNum: Int): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.giftFamilyDrawFragment",
            RpcRequestData.array {
                put("bizType", "ANTFARM_GAME_CENTER")
                put("giftNum", giftNum)
                put("giftUserId", giftUserId ?: "null")
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "H5")
            }
        )
    }

    @JvmStatic
    suspend fun getMallHome(): String {
        return RequestManager.requestString(
            "com.alipay.charitygamecenter.getMallHome",
            RpcRequestData.array {
                put("bizType", "ANTFARM_GAME_CENTER")
                put("pageSize", 10)
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "H5")
                put("startIndex", 0)
            }
        )
    }

    @JvmStatic
    suspend fun getMallItemDetail(spuId: String?): String {
        return RequestManager.requestString(
            "com.alipay.charitygamecenter.getMallItemDetail",
            RpcRequestData.array {
                put("bizType", "ANTFARM_GAME_CENTER")
                put("itemId", spuId ?: "null")
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "H5")
            }
        )
    }

    @JvmStatic
    suspend fun buyMallItem(spuId: String?, skuId: String?): String {
        return RequestManager.requestString(
            "com.alipay.charitygamecenter.buyMallItem",
            RpcRequestData.array {
                put("bizType", "ANTFARM_GAME_CENTER")
                put("ignoreHoldLimit", false)
                put("itemId", spuId ?: "null")
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "H5")
                put("subItemId", skuId ?: "null")
            }
        )
    }

    /**
     * 领取活动食物
     *
     * @param foodType
     * @param giftIndex
     * @return
     */
    @JvmStatic
    suspend fun clickForGiftV2(foodType: String?, giftIndex: Int): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.clickForGiftV2",
            RpcRequestData.array {
                put("foodType", foodType ?: "null")
                put("giftIndex", giftIndex)
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "ANTFOREST")
                put("version", VERSION)
            }
        )
    }

    /**
     * 查询抽抽乐活动信息
     *
     * @param userId 用户ID
     * @return 返回结果JSON字符串
     */
    @JvmStatic
    suspend fun queryLoveCabin(userId: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.queryLoveCabin",
            RpcRequestData.array {
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "ENTERFARM")
                put("userId", userId ?: "null")
                put("version", VERSION)
            }
        )
    }

    /**
     * 查询抽抽乐任务列表（新版统一接口）
     *
     * @param drawType 抽奖类型 "dailyDraw" 或 "ipDraw"
     * @return 返回结果JSON字符串
     * @throws JSONException JSON异常
     */
    @JvmStatic
    @Throws(JSONException::class)
    suspend fun chouchouleListFarmTask(drawType: String?): String {
        val taskSceneCode = if ("dailyDraw" == drawType) "ANTFARM_DAILY_DRAW_TASK" else "ANTFARM_IP_DRAW_TASK"
        return RequestManager.requestString(
            "com.alipay.antfarm.listFarmTask",
            RpcRequestData.array {
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("signSceneCode", "")
                put("source", "H5")
                put("taskSceneCode", taskSceneCode)
                put("topTask", "")
            }
        )
    }

    /**
     * 执行抽抽乐任务
     *
     * @param drawType 抽奖类型
     * @param bizKey 任务ID
     * @return 返回结果JSON字符串
     * @throws JSONException JSON异常
     */
    @JvmStatic
    @Throws(JSONException::class)
    suspend fun chouchouleDoFarmTask(drawType: String?, bizKey: String?): String {
        val taskSceneCode = if ("dailyDraw" == drawType) "ANTFARM_DAILY_DRAW_TASK" else "ANTFARM_IP_DRAW_TASK"
        return RequestManager.requestString(
            "com.alipay.antfarm.doFarmTask",
            RpcRequestData.array {
                put("bizKey", bizKey ?: "null")
                put("requestType", "RPC")
                put("sceneCode", "ANTFARM")
                put("source", "H5")
                put("taskSceneCode", taskSceneCode)
            }
        )
    }

    /**
     * 领取抽抽乐任务奖励
     *
     * @param drawType 抽奖类型
     * @param taskId 任务ID
     * @return 返回结果JSON字符串
     * @throws JSONException JSON异常
     */
    @JvmStatic
    @Throws(JSONException::class)
    suspend fun chouchouleReceiveFarmTaskAward(drawType: String?, taskId: String?): String {
        val taskSceneCode = if ("dailyDraw" == drawType) "ANTFARM_DAILY_DRAW_TASK" else "ANTFARM_IP_DRAW_TASK"
        val awardType = if ("dailyDraw" == drawType) "DAILY_DRAW_TIMES" else "IP_DRAW_MACHINE_DRAW_TIMES"
        return RequestManager.requestString(
            "com.alipay.antfarm.receiveFarmTaskAward",
            RpcRequestData.array {
                put("awardType", awardType)
                put("requestType", "RPC")
                put("sceneCode", "ANTFARM")
                put("source", "antfarm_villa")
                put("taskId", taskId ?: "null")
                put("taskSceneCode", taskSceneCode)
            }
        )
    }

    /**
     * 查询抽抽乐活动详情（新版统一接口）
     *
     * @param scene 主场景 "dailyDrawMachine" 或 "ipDrawMachine"
     * @param otherScene 其他场景
     * @return 返回结果JSON字符串
     */
    @JvmStatic
    suspend fun queryDrawMachineActivity_New(scene: String?, otherScene: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.queryDrawMachineActivity",
            RpcRequestData.array {
                // 原 otherScenes 为单元素数组 ["$otherScene"]
                put("otherScenes", JSONArray().put(otherScene ?: "null"))
                put("requestType", "RPC")
                put("scene", scene ?: "null")
                put("sceneCode", "ANTFARM")
                put("source", "antfarm_villa")
            }
        )
    }

    /**
     * 执行抽奖（IP抽抽乐）
     *
     * @return 返回结果JSON字符串
     */
    @JvmStatic
    suspend fun drawMachineIP(): String {
        return drawMachineIP(1)
    }

    /**
     * 执行抽奖（IP抽抽乐）- 支持连抽
     * @param batchDrawTimes 连抽次数
     * @return 返回结果JSON字符串
     */
    @JvmStatic
    suspend fun drawMachineIP(batchDrawTimes: Int): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.drawMachine",
            RpcRequestData.array {
                put("batchDrawTimes", batchDrawTimes)
                put("requestType", "RPC")
                put("scene", "ipDrawMachine")
                put("sceneCode", "ANTFARM")
                put("source", "antfarm_villa")
            }
        )
    }

    /**
     * 执行抽奖（普通抽抽乐）
     * @param activityId 活动ID
     * @return 返回结果JSON字符串
     */
    @JvmStatic
    suspend fun drawMachineDaily(activityId: String?): String {
        return drawMachineDaily(1)
    }

    @JvmStatic
    suspend fun drawMachineDaily(batchDrawTimes: Int): String {
        // 构造请求数据，完全匹配日志中的字段
        return RequestManager.requestString(
            "com.alipay.antfarm.drawMachine",
            RpcRequestData.array {
                put("batchDrawTimes", batchDrawTimes)
                put("requestType", "RPC")
                put("scene", "dailyDrawMachine")
                put("sceneCode", "ANTFARM")
                put("source", "antfarm_villa") //siliaorenwu  庄园首页抽一次抽抽乐获得饲料任务
            }
        )
    }

    /**
     * 执行抽奖（指定活动ID）
     *
     * @param activityId 活动ID
     * @return 返回结果JSON字符串
     */
    @JvmStatic
    suspend fun DrawPrize(activityId: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.DrawPrize",
            RpcRequestData.array {
                put("activityId", activityId ?: "null")
                put("requestType", "RPC")
                put("sceneCode", "ANTFARM")
                put("source", "icon")
            }
        )
    }

    /**
     * 广告插件接口 - 获取广告任务
     *
     * @param referToken 引用Token
     * @param spaceCode 广告位代码
     * @return 返回结果JSON字符串
     * @throws JSONException JSON异常
     */
    @JvmStatic
    @Throws(JSONException::class)
    suspend fun xlightPlugin(referToken: String?, spaceCode: String?): String {
        return RequestManager.requestString(
            "com.alipay.adexchange.ad.facade.xlightPlugin",
            RpcRequestData.array {
                put(
                    "positionRequest",
                    JSONObject().apply {
                        put(
                            "referInfo",
                            JSONObject().apply {
                                put("referToken", referToken ?: "null")
                            }
                        )
                        put("spaceCode", spaceCode ?: "null")
                    }
                )
                put(
                    "sdkPageInfo",
                    JSONObject().apply {
                        put("adComponentType", "GUESS_PRICE")
                        put("adComponentVersion", "4.28.66")
                        put("networkType", "WIFI")
                        put("pageFrom", "ch_url-https://render.alipay.com/p/yuyan/180020380000000182/prizeMachine.html")
                        put("pageNo", 1)
                        put("pageUrl", "https://render.alipay.com/p/yuyan/180020010001256918/antfarm-landing.html?caprMode=sync")
                        put("session", "u_0c09f_b010f")
                        put("unionAppId", "2060090000304921")
                        put("xlightRuntimeSDKversion", "4.28.66")
                        put("xlightSDKType", "h5")
                        put("xlightSDKVersion", "4.28.66")
                    }
                )
            }
        )
    }

    /**
     * 完成广告任务
     *
     * @param playBizId 播放业务ID
     * @param playEventInfo 播放事件信息
     * @param iepTaskType 任务类型
     * @param iepTaskSceneCode 任务场景代码
     * @return 返回结果JSON字符串
     * @throws JSONException JSON异常
     */
    @JvmStatic
    @Throws(JSONException::class)
    suspend fun finishAdTask(playBizId: String?, playEventInfo: JSONObject?, iepTaskType: String?, iepTaskSceneCode: String?): String {
        return RequestManager.requestString(
            "com.alipay.adtask.biz.mobilegw.service.interaction.finish",
            RpcRequestData.array {
                put(
                    "extendInfo",
                    JSONObject().apply {
                        put("iepTaskSceneCode", iepTaskSceneCode ?: "null")
                        put("iepTaskType", iepTaskType ?: "null")
                        put("playEndingStatus", "success")
                    }
                )
                put("playBizId", playBizId ?: "null")
                put("playEventInfo", playEventInfo)
                put("source", "adx")
            }
        )
    }

    /**
     * 完成普通任务（无广告）
     *
     * @param taskType 任务类型
     * @param sceneCode 场景代码
     * @param outBizNo 外部业务号
     * @return 返回结果JSON字符串
     * @throws JSONException JSON异常
     */
    @JvmStatic
    @Throws(JSONException::class)
    suspend fun finishTask(taskType: String?, sceneCode: String?, outBizNo: String?): String {
        return RequestManager.requestString(
            "com.alipay.antiep.finishTask",
            RpcRequestData.array {
                put("outBizNo", outBizNo ?: "null")
                put("requestType", "RPC")
                put("sceneCode", sceneCode ?: "null")
                put("source", "ADBASICLIB")
                put("taskType", taskType ?: "null")
            }
        )
    }

    /**
     * 查询家庭装修信息
     *
     * @return 返回结果JSON字符串
     */
    @JvmStatic
    suspend fun queryFamilyDecoration(): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.queryFamilyDecoration",
            RpcRequestData.array {
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM")
                put("source", "H5")
            }
        )
    }

    /**
     * 装修金商城 - 分页查询家具列表
     */
    @JvmStatic
    suspend fun getFitmentItemList(activityId: String?, pageSize: Int, labelType: String?, startIndex: Int): String {
        try {
            return RequestManager.requestString(
                "com.alipay.antiep.itemList",
                RpcRequestData.array {
                    put("activityId", activityId ?: "null")
                    if (!labelType.isNullOrEmpty()) {
                        put("labelType", labelType)
                    }
                    put("pageSize", pageSize)
                    put("requestType", "NORMAL")
                    put("sceneCode", "ANTFARM_FITMENT_MALL")
                    put("source", "antfarm")
                    put("startIndex", startIndex)
                }
            )
        } catch (e: Exception) {
            return ""
        }
    }

    /**
     * 查询道具详情
     *
     * @param spuId 标准产品单元ID
     * @return 返回结果JSON字符串
     */
    @JvmStatic
    suspend fun getItemDetail(spuId: String?): String {
        return RequestManager.requestString(
            "com.alipay.antiep.itemDetail",
            RpcRequestData.array {
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM_FITMENT_MALL")
                put("source", "antfarm")
                put("spuId", spuId ?: "null")
            }
        )
    }

    /**
     * 兑换庄园家具
     */
    @JvmStatic
    suspend fun exchangeBenefit(spuId: String?, skuId: String?, activityId: String?): String {
        val requestId = generateRequestId()
        try {
            return RequestManager.requestString(
                "com.alipay.antcommonweal.exchange.h5.exchangeBenefit",
                RpcRequestData.array {
                    put(
                        "context",
                        JSONObject().apply {
                            put("activityId", activityId ?: "null")
                        }
                    )
                    put("requestId", requestId)
                    put("requestType", "NORMAL")
                    put("sceneCode", "ANTFARM_FITMENT_MALL")
                    put("skuId", skuId ?: "null")
                    put("source", "H5")
                    put("spuId", spuId ?: "null")
                }
            )
        } catch (e: JSONException) {
            Log.printStackTrace("exchangeBenefit Error", e)
            return ""
        }
    }

    /**
     * 生成RequestId: 时间戳 + _ + 16位随机数
     */
    private fun generateRequestId(): String {
        val timestamp = System.currentTimeMillis()
        // 生成16位随机长整型数字（正数）
        val randomNum = ((Math.random() * 9 + 1) * Math.pow(10.0, 15.0)).toLong()
        return "${timestamp}_$randomNum"
    }

    @JvmStatic
    suspend fun FlyGameListFarmTask(): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.listFarmTask",
            RpcRequestData.array {
                put("bizKey", "SHANGYEHUA_GAME_TIMES")
                put("gameType", "flyGame")
                put("requestType", "RPC")
                put("sceneCode", "FLAYGAME")
                put("signSceneCode", "")
                put("source", "ANTFARM")
                put("taskSceneCode", "ANTFARM_GAME_TIMES_TASK")
                put("version", "")
            }
        )
    }

    @JvmStatic
    suspend fun HitGameListFarmTask(): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.listFarmTask",
            RpcRequestData.array {
                put("bizKey", "SHANGYEHUA_HIT_ANIMAL")
                put("gameType", "hitGame")
                put("requestType", "RPC")
                put("sceneCode", "HITGAME")
                put("signSceneCode", "")
                put("source", "ANTFARM")
                put("taskSceneCode", "ANTFARM_GAME_TIMES_TASK")
                put("version", "")
            }
        )
    }

    /**
     * 查询物品列表（ip抽抽乐）
     *
     * @param activityId 活动ID（如图片中的 ipDrawMachine_260112）
     * @param pageSize   每页数量 * @param startIndex 起始索引
     * @return 返回结果JSON字符串
     */
    @JvmStatic
    suspend fun getItemList(activityId: String?, pageSize: Int, startIndex: Int): String {
        return RequestManager.requestString(
            "com.alipay.antiep.itemList",
            RpcRequestData.array {
                put("activityId", activityId ?: "null")
                put("pageSize", pageSize)
                put("requestType", "RPC")
                put("sceneCode", "ANTFARM_IP_DRAW_MALL")
                put("source", "antfarm.villa")
                put("startIndex", startIndex)
            }
        )
    }

    /**
     * ip抽抽乐兑换装扮
     *
     * @param spuId      标准产品单元ID
     * @param skuId      库存保持单位ID
     * @param activityId 活动ID (例如: ipDrawMachine_260112)
     * @param sceneCode  场景代码 (例如: ANTFARM_IP_DRAW_MALL)
     * @param source     来源
     * @return 返回结果JSON字符串
     */
    @JvmStatic
    suspend fun exchangeBenefit(spuId: String?, skuId: String?, activityId: String?, sceneCode: String?, source: String?): String {
        val requestId = generateRequestId()
        try {
            return RequestManager.requestString(
                "com.alipay.antcommonweal.exchange.h5.exchangeBenefit",
                RpcRequestData.array {
                    put(
                        "context",
                        JSONObject().apply {
                            put("activityId", activityId ?: "null")
                        }
                    )
                    put("requestId", requestId)
                    put("requestType", "RPC")
                    put("sceneCode", sceneCode ?: "null")
                    put("skuId", skuId ?: "null")
                    put("source", source ?: "null")
                    put("spuId", spuId ?: "null")
                }
            )
        } catch (e: JSONException) {
            Log.printStackTrace("exchangeBenefit Error", e)
            return ""
        }
    }

    /**
     * 庄园限定活动查询
     * @return
     */
    @JvmStatic
    suspend fun queryOptionalPlay(): String {
        return RequestManager.requestString(
            "com.alipay.charitygamecenter.queryOptionalPlay",
            RpcRequestData.array {
                put("bizType", "ANTFARM")
                put(
                    "commonDegradeFilterRequest",
                    JSONObject().apply {
                        put("appMode", "normal")
                        put("deviceLevel", "high")
                        put("initialized", true)
                        put("platform", "Android")
                        put("unityDeviceLevel", "high")
                    }
                )
                put("playTypeList", JSONArray().put("TASK_TRIGGER").put("TOP_UP_COUPON"))
                put("recentAppRecordList", JSONArray())
                put("requestType", "NORMAL")
                put("sceneCode", "ANTFARM_COMMON")
                put("source", "H5")
                put("version", VERSION)
            }
        )
    }

    /**
     * 庄园限定活动领取
     * @param awardCountForReceive
     * @param sceneCode
     * @param taskType
     * @return
     */
    @JvmStatic
    suspend fun receiveTaskAwardantfarm(awardCountForReceive: Int, sceneCode: String?, taskType: String?): String {
        return RequestManager.requestString(
            "com.alipay.antieptask.receiveTaskAwardantfarm",
            RpcRequestData.array {
                put("awardCountForReceive", awardCountForReceive)
                put("ignoreLimit", true)
                put("requestType", "RPC")
                put("sceneCode", sceneCode ?: "null")
                put("source", "antfarm")
                put("taskType", taskType ?: "null")
            }
        )
    }
}
