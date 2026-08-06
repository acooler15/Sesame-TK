package fansirsqi.xposed.sesame.task.antFarm

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.UUID
import fansirsqi.xposed.sesame.hook.RequestManager
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
    fun enterFarm(userId: String?, targetUserId: String?): String {
        val args = JSONObject()
        args.put("animalId", "")
        args.put("bizCode", "")
        args.put("gotoneScene", "")
        args.put("gotoneTemplateId", "")
        args.put("groupId", "")
        args.put("growthExtInfo", "")
        args.put("inviteUserId", "")
        args.put("masterFarmId", "")
        args.put("queryLastRecordNum", true)
        args.put("recall", false)
        args.put("requestType", "NORMAL")
        args.put("sceneCode", "ANTFARM")
        args.put("shareId", "")
        args.put("shareUniqueId", "${System.currentTimeMillis()}_$targetUserId")
        args.put("source", "ANTFOREST")
        args.put("starFarmId", "")
        args.put("subBizCode", "")
        args.put("touchRecordId", "")
        args.put("userId", userId)
        args.put("userToken", "")
        args.put("version", VERSION)
        val paras = "[$args]"
        return RequestManager.requestString("com.alipay.antfarm.enterFarm", paras)
    }

    // 一起拿小鸡饲料
    @JvmStatic
    fun letsGetChickenFeedTogether(): String {
        val args1 = "[{\"needHasInviteUserByCycle\":\"true\",\"requestType\":\"RPC\",\"sceneCode\":\"ANTFARM_P2P\",\"source\":\"ANTFARM\",\"startIndex\":0,\"version\":\"$VERSION\"}]"
        val args = "[{\"needHasInviteUserByCycle\":true,\"requestType\":\"RPC\",\"sceneCode\":\"ANTFARM_FAMILY_SHARE\",\"source\":\"ANTFARM\",\"startIndex\":0}]"
        return RequestManager.requestString("com.alipay.antiep.canInvitePersonListP2P", args1)
    }

    // 赠送饲料
    @JvmStatic
    fun giftOfFeed(bizTraceId: String?, userId: String?): String {
        val args1 = "[{\"beInvitedUserId\":\"$userId\",\"bizTraceId\":\"$bizTraceId\",\"requestType\":\"RPC\",\"sceneCode\":\"ANTFARM_P2P\",\"source\":\"ANTFARM\",\"version\":\"$VERSION\"}]"
        return RequestManager.requestString("com.alipay.antiep.inviteP2P", args1)
    }

    @JvmStatic
    @Throws(JSONException::class)
    fun syncAnimalStatus(farmId: String?, operTag: String?, operType: String?): String {
        val args = JSONObject()
        args.put("farmId", farmId)
        args.put("operTag", operTag)
        args.put("operType", operType)
        args.put("requestType", "NORMAL")
        args.put("sceneCode", "ANTFARM")
        args.put("source", "H5")
        args.put("version", VERSION)
        val params = "[$args]"
        return RequestManager.requestString("com.alipay.antfarm.syncAnimalStatus", params)
    }

    @JvmStatic
    fun sleep(): String {
        val args1 = "[{\"requestType\":\"RPC\",\"sceneCode\":\"ANTFARM\",\"source\":\"LOVECABIN\",\"version\":\"unknown\"}]"
        return RequestManager.requestString("com.alipay.antfarm.sleep", args1)
    }

    /**
     * 家庭睡觉
     *
     * @param groupId 家庭ID
     * @return 返回结果
     */
    @JvmStatic
    fun sleep(groupId: String?): String {
        val args1 = "[{\"groupId\":\"$groupId\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"spaceType\":\"ChickFamily\", \"version\":\"unknown\"}]"
        return RequestManager.requestString("com.alipay.antfarm.sleep", args1)
    }

    @JvmStatic
    fun wakeUp(): String {
        val args1 = "[{\"requestType\":\"RPC\",\"sceneCode\":\"ANTFARM\",\"source\":\"LOVECABIN\",\"version\":\"unknown\"}]"
        return RequestManager.requestString("com.alipay.antfarm.wakeUp", args1)
    }

    @JvmStatic
    fun rewardFriend(consistencyKey: String?, friendId: String?, productNum: String?, time: String?): String {
        val args1 = "[{\"canMock\":true,\"consistencyKey\":\"$consistencyKey\",\"friendId\":\"$friendId\",\"operType\":\"1\",\"productNum\":$productNum,\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"time\":$time,\"version\":\"$VERSION\"}]"
        return RequestManager.requestString("com.alipay.antfarm.rewardFriend", args1)
    }

    @JvmStatic
    fun recallAnimal(animalId: String?, currentFarmId: String?, masterFarmId: String?): String {
        val args1 = "[{\"animalId\":\"$animalId\",\"currentFarmId\":\"$currentFarmId\",\"masterFarmId\":\"$masterFarmId\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"version\":\"$VERSION\"}]"
        return RequestManager.requestString("com.alipay.antfarm.recallAnimal", args1)
    }

    @JvmStatic
    fun orchardRecallAnimal(animalId: String?, userId: String?): String {
        val args1 = "[{\"animalId\":\"$animalId\",\"orchardUserId\":\"$userId\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ORCHARD\",\"source\":\"zhuangyuan_zhaohuixiaoji\",\"version\":\"0.1.2403061630.6\"}]"
        return RequestManager.requestString("com.alipay.antorchard.recallAnimal", args1)
    }

    @JvmStatic
    fun sendBackAnimal(sendType: String?, animalId: String?, currentFarmId: String?, masterFarmId: String?): String {
        val args1 = "[{\"animalId\":\"$animalId\",\"currentFarmId\":\"$currentFarmId\",\"masterFarmId\":\"$masterFarmId\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"sendType\":\"$sendType\",\"source\":\"H5\",\"version\":\"$VERSION\"}]"
        return RequestManager.requestString("com.alipay.antfarm.sendBackAnimal", args1)
    }

    @JvmStatic
    fun harvestProduce(farmId: String?): String {
        val args1 = "[{\"canMock\":true,\"farmId\":\"$farmId\",\"giftType\":\"\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"version\":\"$VERSION\"}]"
        return RequestManager.requestString("com.alipay.antfarm.harvestProduce", args1)
    }

    @JvmStatic
    fun listActivityInfo(): String {
        val args1 = "[{\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"version\":\"$VERSION\"}]"
        return RequestManager.requestString("com.alipay.antfarm.listActivityInfo", args1)
    }

    @JvmStatic
    fun donation(activityId: String?, donationAmount: Int): String {
        val args1 = "[{\"activityId\":\"$activityId\",\"donationAmount\":$donationAmount,\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"version\":\"$VERSION\"}]"
        return RequestManager.requestString("com.alipay.antfarm.donation", args1)
    }

    @JvmStatic
    fun listFarmTask(): String {
        val args1 = "[{\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"version\":\"$VERSION\"}]"
        return RequestManager.requestString("com.alipay.antfarm.listFarmTask", args1)
    }

    @JvmStatic
    fun receiveFarmTaskAward(taskId: String?): String {
        val args1 = "[{\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"taskId\":\"$taskId\",\"version\":\"$VERSION\"}]"
        return RequestManager.requestString("com.alipay.antfarm.receiveFarmTaskAward", args1)
    }

    @JvmStatic
    fun listToolTaskDetails(): String {
        val args1 = "[{\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"version\":\"$VERSION\"}]"
        return RequestManager.requestString("com.alipay.antfarm.listToolTaskDetails", args1)
    }

    @JvmStatic
    fun receiveToolTaskReward(rewardType: String?, rewardCount: Int, taskType: String?): String {
        val args1 = "[{\"ignoreLimit\":false,\"requestType\":\"NORMAL\",\"rewardCount\":$rewardCount,\"rewardType\":\"$rewardType\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"taskType\":\"$taskType\",\"version\":\"$VERSION\"}]"
        return RequestManager.requestString("com.alipay.antfarm.receiveToolTaskReward", args1)
    }

    @JvmStatic
    @Throws(JSONException::class)
    fun feedAnimal(farmId: String?): String {
        val args = JSONObject()
        args.put("animalType", "CHICK")
        args.put("canMock", true)
        args.put("farmId", farmId)
        args.put("requestType", "NORMAL")
        args.put("sceneCode", "ANTFARM")
        args.put("source", "chInfo_ch_appcollect__chsub_my-recentlyUsed")
        args.put("version", VERSION)
        val params = "[$args]"
        return RequestManager.requestString("com.alipay.antfarm.feedAnimal", params)
    }

    @JvmStatic
    fun listFarmTool(): String {
        val args1 = "[{\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"version\":\"$VERSION\"}]"
        return RequestManager.requestString("com.alipay.antfarm.listFarmTool", args1)
    }

    @JvmStatic
    fun useFarmTool(targetFarmId: String?, toolId: String?, toolType: String?): String {
        val args1 = "[{\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"targetFarmId\":\"$targetFarmId\",\"toolId\":\"$toolId\",\"toolType\":\"$toolType\",\"version\":\"$VERSION\"}]"
        return RequestManager.requestString("com.alipay.antfarm.useFarmTool", args1)
    }

    @JvmStatic
    fun rankingList(pageStartSum: Int): String {
        val args1 = "[{\"pageSize\":20,\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"startNum\":$pageStartSum,\"version\":\"$VERSION\"}]"
        return RequestManager.requestString("com.alipay.antfarm.rankingList", args1)
    }

    @JvmStatic
    fun notifyFriend(animalId: String?, notifiedFarmId: String?): String {
        val args1 = "[{\"animalId\":\"$animalId\",\"animalType\":\"CHICK\",\"canBeGuest\":true,\"notifiedFarmId\":\"$notifiedFarmId\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"version\":\"$VERSION\"}]"
        return RequestManager.requestString("com.alipay.antfarm.notifyFriend", args1)
    }

    @JvmStatic
    @Throws(JSONException::class)
    fun feedFriendAnimal(friendFarmId: String?): String {
        val args = JSONObject()
        args.put("friendFarmId", friendFarmId)
        args.put("requestType", "NORMAL")
        args.put("sceneCode", "ANTFARM")
        args.put("source", "chInfo_ch_appcenter__chsub_9patch")
        args.put("version", VERSION)
        val params = "[$args]"
        return RequestManager.requestString("com.alipay.antfarm.feedFriendAnimal", params)
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
    fun collectManurePot(manurePotNO: String?): String {
        //        "isSkipTempLimit":true, 肥料满了也强行收取，解决 农场未开通 打扫鸡屎失败问题
        return RequestManager.requestString(
            "com.alipay.antfarm.collectManurePot",
            "[{\"isSkipTempLimit\":true,\"manurePotNOs\":\"$manurePotNO\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"version\":\"$VERSION\"}]"
        )
    }

    @JvmStatic
    fun sign(): String {
        return RequestManager.requestString("com.alipay.antfarm.sign", "[{\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"version\":\"$VERSION\"}]")
    }

    @JvmStatic
    fun initFarmGame(gameType: String?): String {
        if ("flyGame" == gameType) {
            return RequestManager.requestString(
                "com.alipay.antfarm.initFarmGame",
                "[{\"gameType\":\"flyGame\",\"requestType\":\"RPC\",\"sceneCode\":\"FLAYGAME\",\"source\":\"FARM_game_yundongfly\",\"toolTypes\":\"ACCELERATETOOL,SHARETOOL,NONE\",\"version\":\"\"}]"
            )
        } else if ("hitGame" == gameType) {
            return RequestManager.requestString(
                "com.alipay.antfarm.initFarmGame",
                "[{\"gameType\":\"hitGame\",\"requestType\":\"RPC\",\"sceneCode\":\"HITGAME\",\"source\":\"FARM_game_zouxiaoji\",\"toolTypes\":\"ACCELERATETOOL,SHARETOOL,NONE\",\"version\":\"\"}]"
            )
        }
        return RequestManager.requestString(
            "com.alipay.antfarm.initFarmGame",
            "[{\"gameType\":\"$gameType\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"toolTypes\":\"STEALTOOL,ACCELERATETOOL,SHARETOOL\"}]"
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
    fun recordFarmGame(gameType: String?): String {
        val uuid = getUuid()
        val md5String = getMD5(uuid)
        val score = RandomScore(gameType)
        if ("flyGame" == gameType) {
            val foodCount = score / 50
            return RequestManager.requestString(
                "com.alipay.antfarm.recordFarmGame",
                "[{\"foodCount\":$foodCount,\"gameType\":\"flyGame\",\"md5\":\"$md5String\",\"requestType\":\"RPC\",\"sceneCode\":\"FLAYGAME\",\"score\":$score,\"source\":\"ANTFARM\",\"toolTypes\":\"ACCELERATETOOL,SHARETOOL,NONE\",\"uuid\":\"$uuid\",\"version\":\"\"}]"
            )
        } else if ("hitGame" == gameType) {
            return RequestManager.requestString(
                "com.alipay.antfarm.recordFarmGame",
                "[{\"gameType\":\"hitGame\",\"md5\":\"$md5String\",\"requestType\":\"RPC\",\"sceneCode\":\"HITGAME\",\"score\":$score,\"source\":\"ANTFARM\",\"toolTypes\":\"ACCELERATETOOL,SHARETOOL,NONE\",\"uuid\":\"$uuid\",\"version\":\"\"}]"
            )
        }
        return RequestManager.requestString(
            "com.alipay.antfarm.recordFarmGame",
            "[{\"gameType\":\"$gameType\",\"md5\":\"$md5String\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"score\":$score,\"source\":\"H5\",\"toolTypes\":\"STEALTOOL,ACCELERATETOOL,SHARETOOL\",\"uuid\":\"$uuid\"}]"
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
    fun enterKitchen(userId: String?): String {
        val args = JSONObject()
        args.put("requestType", "RPC")
        args.put("sceneCode", "ANTFARM")
        args.put("source", "VILLA")
        args.put("userId", userId)
        args.put("version", "unknown")
        val params = "[$args]"
        return RequestManager.requestString("com.alipay.antfarm.enterKitchen", params)
    }

    @JvmStatic
    fun collectDailyFoodMaterial(dailyFoodMaterialAmount: Int): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.collectDailyFoodMaterial",
            "[{\"collectDailyFoodMaterialAmount\":$dailyFoodMaterialAmount,\"requestType\":\"RPC\",\"sceneCode\":\"ANTFARM\",\"source\":\"VILLA\",\"version\":\"unknown\"}]"
        )
    }

    @JvmStatic
    fun queryFoodMaterialPack(): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.queryFoodMaterialPack",
            "[{\"requestType\":\"RPC\",\"sceneCode\":\"ANTFARM\",\"source\":\"kitchen\",\"version\":\"unknown\"}]"
        )
    }

    @JvmStatic
    fun collectDailyLimitedFoodMaterial(dailyLimitedFoodMaterialAmount: Int): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.collectDailyLimitedFoodMaterial",
            "[{\"collectDailyLimitedFoodMaterialAmount\":$dailyLimitedFoodMaterialAmount,\"requestType\":\"RPC\",\"sceneCode\":\"ANTFARM\",\"source\":\"kitchen\",\"version\":\"unknown\"}]"
        )
    }

    @JvmStatic
    fun farmFoodMaterialCollect(): String {
        return RequestManager.requestString(
            "com.alipay.antorchard.farmFoodMaterialCollect",
            "[{\"collect\":true,\"requestType\":\"RPC\",\"sceneCode\":\"ORCHARD\",\"source\":\"VILLA\",\"version\":\"unknown\"}]"
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
    fun cook(userId: String?, source: String?): String {
        //[{"requestType":"RPC","sceneCode":"ANTFARM","source":"VILLA","userId":"2088522730162798","version":"unknown"}]
        val args = JSONObject()
        args.put("requestType", "RPC")
        args.put("sceneCode", "ANTFARM")
        args.put("source", source)
        args.put("userId", userId)
        args.put("version", "unknown")
        val params = "[$args]"
        return RequestManager.requestString("com.alipay.antfarm.cook", params)
    }

    @JvmStatic
    fun useFarmFood(cookbookId: String?, cuisineId: String?): String {
        //        return RequestManager.requestString("com.alipay.antfarm.useFarmFood",
        //                "[{\"cookbookId\":\"" + cookbookId + "\",\"cuisineId\":\"" + cuisineId
        //                        + "\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"chInfo_ch_appcenter__chsub_9patch\",\"useCuisine\":true,\"version\":\""
        //                        + VERSION + "\"}]");
        try {
            val args = JSONObject()
            args.put("cookbookId", cookbookId)
            args.put("cuisineId", cuisineId)
            args.put("requestType", "NORMAL")
            args.put("sceneCode", "ANTFARM")
            args.put("canMock", true)
            args.put("source", "chInfo_ch_appcenter__chsub_9patch")
            args.put("useCuisine", true)
            args.put("version", VERSION)
            val params = "[$args]"
            return RequestManager.requestString("com.alipay.antfarm.useFarmFood", params)
        } catch (e: JSONException) {
            return ""
        }
    }

    @JvmStatic
    fun collectKitchenGarbage(): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.collectKitchenGarbage",
            "[{\"requestType\":\"RPC\",\"sceneCode\":\"ANTFARM\",\"source\":\"VILLA\",\"version\":\"unknown\"}]"
        )
    }

    /* 日常任务 */
    @JvmStatic
    fun doFarmTask(bizKey: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.doFarmTask",
            "[{\"bizKey\":\"$bizKey\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"version\":\"$VERSION\"}]"
        )
    }

    @JvmStatic
    fun queryTabVideoUrl(): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.queryTabVideoUrl",
            "[{\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"version\":\"$VERSION\"}]"
        )
    }

    @JvmStatic
    fun videoDeliverModule(bizId: String?): String {
        return RequestManager.requestString(
            "alipay.content.reading.life.deliver.module",
            "[{\"bizId\":\"$bizId\",\"bizType\":\"CONTENT\",\"chInfo\":\"ch_antFarm\",\"refer\":\"antFarm\",\"timestamp\":\"${System.currentTimeMillis()}\"}]"
        )
    }

    @JvmStatic
    fun videoTrigger(bizId: String?): String {
        return RequestManager.requestString(
            "alipay.content.reading.life.prize.trigger",
            "[{\"bizId\":\"$bizId\",\"bizType\":\"CONTENT\",\"prizeFlowNum\":\"VIDEO_TASK\",\"prizeType\":\"farmFeed\"}]"
        )
    }

    /* 惊喜礼包 */
    @JvmStatic
    fun drawLotteryPlus(): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.drawLotteryPlus",
            "[{\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5 \",\"version\":\"\"}]"
        )
    }

    /* 小麦 */
    @JvmStatic
    fun acceptGift(): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.acceptGift",
            "[{\"ignoreLimit\":false,\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"version\":\"$VERSION\"}]"
        )
    }

    @JvmStatic
    fun visitFriend(friendFarmId: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.visitFriend",
            "[{\"friendFarmId\":\"$friendFarmId\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"version\":\"$VERSION\"}]"
        )
    }

    /**
     * 小鸡日志当月日期查询
     *
     * @return
     */
    @JvmStatic
    fun queryChickenDiaryList(): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.queryChickenDiaryList",
            "[{\"requestType\":\"NORMAL\",\"sceneCode\":\"DIARY\",\"source\":\"antfarm_icon\"}]"
        )
    }

    /**
     * 小鸡日志指定月份日期查询
     *
     * @param yearMonth 日期格式：yyyy-MM
     * @return
     */
    @JvmStatic
    fun queryChickenDiaryList(yearMonth: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.queryChickenDiaryList",
            "[{\"queryMonthStr\":\"$yearMonth\",\"requestType\":\"NORMAL\",\"sceneCode\":\"DIARY\",\"source\":\"antfarm_icon\"}]"
        )
    }

    @JvmStatic
    fun queryChickenDiary(queryDayStr: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.queryChickenDiary",
            "[{\"queryDayStr\":\"$queryDayStr\",\"requestType\":\"NORMAL\",\"sceneCode\":\"DIARY\",\"source\":\"antfarm_icon\"}]"
        )
    }

    @JvmStatic
    fun diaryTietie(diaryDate: String?, roleId: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.diaryTietie",
            "[{\"diaryDate\":\"$diaryDate\",\"requestType\":\"NORMAL\",\"roleId\":\"$roleId\",\"sceneCode\":\"DIARY\",\"source\":\"antfarm_icon\"}]"
        )
    }

    /**
     * 小鸡日记点赞
     *
     * @param DiaryId 日记id
     * @return
     */
    @JvmStatic
    fun collectChickenDiary(DiaryId: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.collectChickenDiary",
            "[{\"collectStatus\":true,\"diaryId\":\"$DiaryId\",\"requestType\":\"NORMAL\",\"sceneCode\":\"MOOD\",\"source\":\"H5\"}]"
        )
    }

    @JvmStatic
    fun visitAnimal(): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.visitAnimal",
            "[{\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"version\":\"$VERSION\"}]"
        )
    }

    @JvmStatic
    fun feedFriendAnimalVisit(friendFarmId: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.feedFriendAnimal",
            "[{\"friendFarmId\":\"$friendFarmId\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"visitChicken\",\"version\":\"$VERSION\"}]"
        )
    }

    @JvmStatic
    fun visitAnimalSendPrize(token: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.visitAnimalSendPrize",
            "[{\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"token\":\"$token\",\"version\":\"$VERSION\"}]"
        )
    }

    @JvmStatic
    fun hireAnimal(farmId: String?, animalId: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.hireAnimal",
            "[{\"friendFarmId\":\"$farmId\",\"hireActionType\":\"HIRE_IN_FRIEND_FARM\",\"hireAnimalId\":\"$animalId\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"sendCardChat\":false,\"source\":\"H5\",\"version\":\"$VERSION\"}]"
        )
    }

    /**
     * 雇佣NPC小鸡（修复版：支持传入source）
     * @param animalId 动物ID
     * @param source 请求来源，如 "zhimaxiaoji_lianjin"
     */
    @JvmStatic
    @Throws(JSONException::class)
    fun hireNpcAnimal(animalId: String?, source: String?): String {
        val args = JSONObject()
        args.put("hireActionType", "HIRE_IN_SELF_FARM")
        args.put("hireAnimalId", animalId)
        args.put("isNpcAnimal", true)
        args.put("requestType", "NORMAL")
        args.put("sceneCode", "ANTFARM")
        args.put("source", source)
        args.put("version", VERSION)
        return RequestManager.requestString("com.alipay.antfarm.hireAnimal", "[$args]")
    }

    /**
     * 遣返NPC小鸡（领取奖励）
     */
    @JvmStatic
    @Throws(JSONException::class)
    fun sendBackNpcAnimal(animalId: String?, currentFarmId: String?, masterFarmId: String?): String {
        val args = JSONObject()
        args.put("animalId", animalId)
        args.put("currentFarmId", currentFarmId)
        args.put("masterFarmId", masterFarmId)
        args.put("receiveNPCReward", true) // 关键参数：领取奖励
        args.put("requestType", "NORMAL")
        args.put("sceneCode", "ANTFARM")
        args.put("sendType", "NORMAL")
        args.put("source", "H5")
        args.put("version", VERSION)
        return RequestManager.requestString("com.alipay.antfarm.sendBackAnimal", "[$args]")
    }

    /**
     * 获取芝麻NPC任务列表  大表哥
     */
    @JvmStatic
    fun listZhimaNpcFarmTask(): String {
        val args = "[{\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"zhimaxiaoji_lianjin\",\"taskSceneCode\":\"ANTFARM_ZHIMA_NPC_TASK\",\"version\":\"$VERSION\"}]"
        return RequestManager.requestString("com.alipay.antfarm.listFarmTask", args)
    }

    /**
     * 领取芝麻NPC任务奖励
     */
    @JvmStatic
    fun receiveZhimaNpcFarmTaskAward(taskId: String?): String {
        val args = "[{\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"zhimaxiaoji_lianjin\",\"taskId\":\"$taskId\",\"taskSceneCode\":\"ANTFARM_ZHIMA_NPC_TASK\",\"version\":\"$VERSION\"}]"
        return RequestManager.requestString("com.alipay.antfarm.receiveFarmTaskAward", args)
    }

    @JvmStatic
    fun DrawPrize(): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.DrawPrize",
            "[{\"requestType\":\"RPC\",\"sceneCode\":\"ANTFARM\",\"source\":\"chouchoule\"}]"
        )
    }

    /**
     * 获取黄金小鸡任务列表
     * 对应日志中的 taskSceneCode: "ANTFARM_CAIFU_NPC_TASK"
     */
    @JvmStatic
    fun listGoldChickenFarmTask(): String {
        val args = "[{\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"taskSceneCode\":\"ANTFARM_CAIFU_NPC_TASK\",\"version\":\"$VERSION\"}]"
        return RequestManager.requestString("com.alipay.antfarm.listFarmTask", args)
    }

    /**
     * 领取黄金小鸡任务奖励 (新增)
     * taskSceneCode: ANTFARM_CAIFU_NPC_TASK
     */
    @JvmStatic
    fun receiveGoldChickenTaskAward(taskId: String?): String {
        val args = "[{\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"taskId\":\"$taskId\",\"taskSceneCode\":\"ANTFARM_CAIFU_NPC_TASK\",\"version\":\"$VERSION\"}]"
        return RequestManager.requestString("com.alipay.antfarm.receiveFarmTaskAward", args)
    }

    /**
     * 理财体验金：查询活动首页
     */
    @JvmStatic
    fun lctyj2025PromoIndex(): String {
        val args = "[{\"blockFeature\":{},\"isAdoutterPos\":\"N\",\"pageType\":\"normal\"}]"
        // 注意：headers 需要根据实际抓包补充，这里简化处理，通常 RPC 框架会自动处理 headers
        return RequestManager.requestString("com.alipay.fundscenebff.needle.lctyj2025.promoIndex", args)
    }

    /**
     * 理财体验金：开启体验计划
     */
    @JvmStatic
    fun lctyj2025OpenPlan(): String {
        // 构造模拟的请求数据，基于日志分析
        // 关键参数：conditionType, userPurchaseAmount, payChannelIndex
        val args = "[{" +
                "\"conditionType\":\"STABLE_AND_EQUITY_BUY_ONE_POSITIVE_PROFIT\"," +
                "\"context\":{" +
                "\"AuthenticationType\":\"PASSWORD\"," +
                "\"payChannelFullName\":\"余额宝\"," +
                "\"payChannelIndex\":\"[\\\"FUND_DC_MONEYFUND_DEFAULT_ALIPAY_NULL\\\"]\"," +
                "\"payChannelType\":\"MONEYFUND\"," +
                "\"userPurchaseAmount\":\"10\"" +
                "}," +
                "\"mismatch\":\"AGREE_MISMATCH\"," +
                "\"needCreatePackage\":true," +
                "\"packageType\":\"bigAmountCamp\"" +
                "}]"
        return RequestManager.requestString("com.alipay.fundscenebff.needle.lctyj2025.openPlan", args)
    }

    /**
     * 黄金票任务：模拟进入黄金攒存页面（仅查询，不涉及交易）
     */
    @JvmStatic
    fun queryGoldCollectionV2(): String {
        val args = "[{\"pageSize\":10,\"queryType\":\"CREATE_PLAN\",\"specifyGram\":\"2\",\"subQueryType\":\"MAKE_PLAN\"}]"
        return RequestManager.requestString("com.alipay.ficcbffweb.gold.collectionV2.query", args)
    }

    /**
     * 获取农场小鸡(肥料鸡)任务列表
     * source: feiliaoji_202507
     * taskSceneCode: ANTFARM_ORCHARD_NPC_TASK
     */
    @JvmStatic
    fun listFarmChickenFarmTask(): String {
        val args = "[{\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"feiliaoji_202507\",\"taskSceneCode\":\"ANTFARM_ORCHARD_NPC_TASK\",\"version\":\"$VERSION\"}]"
        return RequestManager.requestString("com.alipay.antfarm.listFarmTask", args)
    }

    /**
     * 领取农场小鸡任务奖励
     * awardType: NPC_ANIMAL_FOOD
     */
    @JvmStatic
    fun receiveFarmChickenTaskAward(taskId: String?): String {
        val args = "[{\"awardType\":\"NPC_ANIMAL_FOOD\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"feiliaoji_202507\",\"taskId\":\"$taskId\",\"taskSceneCode\":\"ANTFARM_ORCHARD_NPC_TASK\",\"version\":\"$VERSION\"}]"
        return RequestManager.requestString("com.alipay.antfarm.receiveFarmTaskAward", args)
    }

    /**
     * 领取蚂蚁庄园游戏中心奖励 (开宝箱)
     * @param drawTimes 开启次数
     */
    @JvmStatic
    fun drawGameCenterAward(drawTimes: Int): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.drawGameCenterAward",
            "[{\"drawTimes\": $drawTimes,\"requestType\": \"NORMAL\",\"sceneCode\": \"ANTFARM\",\"source\": \"H5\",\"version\": \"$VERSION\"}]"
        )
    }

    /**
     * 查询游戏列表 (如：蚂蚁农场、庄园等)
     * 对应 methodName: com.alipay.charitygamecenter.queryGameList
     */
    @JvmStatic
    fun queryGameList(): String {
        // 按照你提供的 JSON 结构进行字符串拼接
        // 注意：requestData 是一个数组，内部包含一个对象
        val data = "[{" +
                "\"bizType\":\"ANTFARM\"," +
                "\"commonDegradeFilterRequest\":{" +
                "\"deviceLevel\":\"high\"," +
                "\"platform\":\"Android\"," +
                "\"unityDeviceLevel\":\"high\"" +
                "}," +
                "\"recentAppRecordList\":[]," +
                "\"requestType\":\"NORMAL\"," +
                "\"sceneCode\":\"ANTFARM\"," +
                "\"source\":\"H5\"," +
                "\"version\":\"$VERSION\"" +
                "}]"
        return RequestManager.requestString("com.alipay.charitygamecenter.queryGameList", data)
    }

    // 小鸡换装
    @JvmStatic
    fun listOrnaments(): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.listOrnaments",
            "[{\"pageNo\":\"1\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"setsType\":\"ACHIEVEMENTSETS\",\"source\":\"H5\",\"subType\":\"sets\",\"type\":\"apparels\",\"version\":\"$VERSION\"}]"
        )
    }

    @JvmStatic
    fun saveOrnaments(animalId: String?, farmId: String?, ornaments: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.saveOrnaments",
            "[{\"animalId\":\"$animalId\",\"farmId\":\"$farmId\",\"ornaments\":\"$ornaments\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"version\":\"$VERSION\"}]"
        )
    }

    // 亲密家庭
    @JvmStatic
    fun enterFamily(): String {
        val args = "[{\"fromAnn\":false,\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"timeZoneId\":\"Asia/Shanghai\"}]"
        return RequestManager.requestString("com.alipay.antfarm.enterFamily", args)
    }

    /**
     * 家庭任务入口 - 查询当前是否还有「道早安」等家庭任务
     *
     * 对应看我.txt 中：com.alipay.antfarm.familyTaskTips
     *
     * @param animals enterFamily 接口返回的家庭 animals 数组（原样透传给 RPC）
     */
    @JvmStatic
    fun familyTaskTips(animals: JSONArray?): String {
        val args = "[{" +
                "\"animals\":$animals" +
                ",\"requestType\":\"NORMAL\"" +
                ",\"sceneCode\":\"ANTFARM\"" +
                ",\"source\":\"H5\"" +
                ",\"taskSceneCode\":\"ANTFARM_FAMILY_TASK\"" +
                ",\"timeZoneId\":\"Asia/Shanghai\"" +
                "}]"
        return RequestManager.requestString("com.alipay.antfarm.familyTaskTips", args)
    }

    @JvmStatic
    fun familyReceiveFarmTaskAward(taskId: String?): String {
        val args = "[{\"awardType\":\"FAMILY_INTIMACY\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"taskId\":\"$taskId\",\"taskSceneCode\":\"ANTFARM_FAMILY_TASK\"}]"
        return RequestManager.requestString("com.alipay.antfarm.receiveFarmTaskAward", args)
    }

    @JvmStatic
    fun familyAwardList(): String {
        val args = "[{\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\"}]"
        return RequestManager.requestString("com.alipay.antfarm.familyAwardList", args)
    }

    @JvmStatic
    fun receiveFamilyAward(rightId: String?): String {
        val args = "[{\"requestType\":\"NORMAL\",\"rightId\":\"$rightId\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\"}]"
        return RequestManager.requestString("com.alipay.antfarm.receiveFamilyAward", args)
    }

    @JvmStatic
    fun assignFamilyMember(assignAction: String?, beAssignUser: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.assignFamilyMember",
            "[{\"assignAction\":\"$assignAction\",\"beAssignUser\":\"$beAssignUser\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\"}]"
        )
    }

    @JvmStatic
    fun sendChat(chatCardType: String?, receiverUserId: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.sendChat",
            "[{\"chatCardType\":\"$chatCardType\",\"receiverUserId\":\"$receiverUserId\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\"}]"
        )
    }

    @JvmStatic
    fun deliverSubjectRecommend(friendUserIdList: JSONArray?): String {
        val args = "[{\"friendUserIds\":$friendUserIdList,\"requestType\":\"NORMAL\",\"sceneCode\":\"ChickFamily\",\"source\":\"H5\"}]"
        return RequestManager.requestString("com.alipay.antfarm.deliverSubjectRecommend", args)
    }

    @JvmStatic
    @Throws(JSONException::class)
    fun OpenAIPrivatePolicy(): String {
        val args = JSONObject()
        args.put("privatePolicyIdList", JSONArray().put("AI_CHICK_PRIVATE_POLICY"))
        args.put("requestType", "NORMAL")
        args.put("sceneCode", "ANTFARM")
        args.put("source", "H5")
        args.put("version", VERSION)
        val params = "[$args]"
        return RequestManager.requestString("com.alipay.antfarm.OpenPrivatePolicy", params)
    }

    @JvmStatic
    @Throws(JSONException::class)
    fun deliverContentExpand(
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
        val args = JSONObject()
        args.put("ariverRpcTraceId", ariverRpcTraceId)
        args.put("eventId", eventId)
        args.put("eventName", eventName)
        args.put("friendUserIds", friendUserIdList)
        args.put("memo", memo)
        args.put("requestType", "NORMAL")
        args.put("resultCode", resultCode)
        args.put("sceneCode", "ANTFARM")
        args.put("sceneId", sceneId)
        args.put("sceneName", sceneName)
        args.put("source", "H5")
        args.put("success", success)
        val params = "[$args]"
        return RequestManager.requestString("com.alipay.antfarm.DeliverContentExpand", params)
    }

    @JvmStatic
    @Throws(JSONException::class)
    fun QueryExpandContent(deliverId: String?): String {
        val args = JSONObject()
        args.put("requestType", "NORMAL")
        args.put("sceneCode", "ANTFARM")
        args.put("source", "H5")
        args.put("deliverId", deliverId)
        val params = "[$args]"
        return RequestManager.requestString("com.alipay.antfarm.QueryExpandContent", params)
    }

    @JvmStatic
    @Throws(JSONException::class)
    fun deliverMsgSend(groupId: String?, friendUserIds: JSONArray?, content: String?, deliverId: String?): String {
        val args = JSONObject()
        args.put("content", content)
        args.put("deliverId", deliverId)
        args.put("friendUserIds", friendUserIds)
        args.put("groupId", groupId)
        args.put("mode", "AI")
        args.put("requestType", "NORMAL")
        args.put("sceneCode", "ANTFARM")
        args.put("source", "H5")
        args.put("spaceType", "ChickFamily")
        val params = "[$args]"
        return RequestManager.requestString("com.alipay.antfarm.DeliverMsgSend", params)
    }

    @JvmStatic
    fun syncFamilyStatus(groupId: String?, operType: String?, syncUserIds: String?): String {
        val args = "[{\"groupId\":\"$groupId\",\"operType\":\"$operType\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"syncUserIds\":[\"$syncUserIds\"]}]"
        return RequestManager.requestString("com.alipay.antfarm.syncFamilyStatus", args)
    }

    @JvmStatic
    fun inviteFriendVisitFamily(receiverUserId: JSONArray?): String {
        val args = "[{\"bizType\":\"FAMILY_SHARE\",\"receiverUserId\":$receiverUserId,\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\"}]"
        return RequestManager.requestString("com.alipay.antfarm.inviteFriendVisitFamily", args)
    }

    @JvmStatic
    fun familyEatTogether(groupId: String?, friendUserIdList: JSONArray?, cuisines: JSONArray?): String {
        val args = "[{\"cuisines\":$cuisines,\"friendUserIds\":$friendUserIdList,\"groupId\":\"$groupId\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"spaceType\":\"ChickFamily\"}]"
        return RequestManager.requestString("com.alipay.antfarm.familyEatTogether", args)
    }

    @JvmStatic
    fun queryRecentFarmFood(queryNum: Int): String {
        val args = "[{\"queryNum\": $queryNum,\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\"}]"
        return RequestManager.requestString("com.alipay.antfarm.queryRecentFarmFood", args)
    }

    @JvmStatic
    fun feedFriendAnimal(friendFarmId: String?, groupId: String?): String {
        val args = "[{\"friendFarmId\": \"$friendFarmId\",\"groupId\": \"$groupId\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ChickFamily\",\"source\":\"H5\",\"spaceType\":\"ChickFamily\"}]"
        return RequestManager.requestString("com.alipay.antfarm.feedFriendAnimal", args)
    }

    @JvmStatic
    fun queryFamilyDrawActivity(): String {
        val args = "[{\"bizType\":\"ANTFARM_GAME_CENTER\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\"}]"
        return RequestManager.requestString("com.alipay.antfarm.queryFamilyDrawActivity", args)
    }

    @JvmStatic
    fun familyDraw(): String {
        val args = "[{\"bizType\":\"ANTFARM_GAME_CENTER\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\"}]"
        return RequestManager.requestString("com.alipay.antfarm.familyDraw", args)
    }

    @JvmStatic
    fun familyBatchInviteP2P(inviteP2PVOList: JSONArray?, sceneCode: String?): String {
        val args = "[{\"inviteP2PVOList\":$inviteP2PVOList,\"requestType\":\"RPC\",\"sceneCode\":\"$sceneCode\",\"source\":\"antfarm\"}]"
        return RequestManager.requestString("com.alipay.antiep.batchInviteP2P", args)
    }

    @JvmStatic
    fun familyDrawSignReceiveFarmTaskAward(taskId: String?): String {
        val args = "[{\"awardType\":\"FAMILY_DRAW_TIME\",\"bizType\":\"ANTFARM_GAME_CENTER\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"taskId\":\"$taskId\",\"taskSceneCode\":\"ANTFARM_FAMILY_DRAW_TASK\"}]"
        return RequestManager.requestString("com.alipay.antfarm.receiveFarmTaskAward", args)
    }

    /**
     * 扭蛋任务查询好友列表
     */
    @JvmStatic
    @Throws(JSONException::class)
    fun familyShareP2PPanelInfo(sceneCode: String?): String {
        val jo = JSONObject()
        jo.put("requestType", "RPC")
        jo.put("source", "antfarm")
        jo.put("sceneCode", sceneCode)
        return RequestManager.requestString("com.alipay.antiep.shareP2PPanelInfo", JSONArray().put(jo).toString())
    }

    /**
     * 扭蛋任务列表
     */
    @JvmStatic
    fun familyDrawListFarmTask(): String {
        val args = "[{\"bizType\":\"ANTFARM_GAME_CENTER\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM_FAMILY_DRAW_TASK\",\"signSceneCode\":\"\",\"source\":\"H5\",\"taskSceneCode\":\"ANTFARM_FAMILY_DRAW_TASK\"}]"
        return RequestManager.requestString("com.alipay.antfarm.listFarmTask", args)
    }

    @JvmStatic
    fun giftFamilyDrawFragment(giftUserId: String?, giftNum: Int): String {
        val args = "[{\"bizType\":\"ANTFARM_GAME_CENTER\",\"giftNum\":$giftNum,\"giftUserId\":\"$giftUserId\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\"}]"
        return RequestManager.requestString("com.alipay.antfarm.giftFamilyDrawFragment", args)
    }

    @JvmStatic
    fun getMallHome(): String {
        val data = "[{\"bizType\":\"ANTFARM_GAME_CENTER\",\"pageSize\":10,\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"startIndex\":0}]"
        return RequestManager.requestString("com.alipay.charitygamecenter.getMallHome", data)
    }

    @JvmStatic
    fun getMallItemDetail(spuId: String?): String {
        val data = "[{\"bizType\":\"ANTFARM_GAME_CENTER\",\"itemId\":\"$spuId\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\"}]"
        return RequestManager.requestString("com.alipay.charitygamecenter.getMallItemDetail", data)
    }

    @JvmStatic
    fun buyMallItem(spuId: String?, skuId: String?): String {
        val data = "[{\"bizType\":\"ANTFARM_GAME_CENTER\",\"ignoreHoldLimit\":false,\"itemId\":\"$spuId\",\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\",\"subItemId\":\"$skuId\"}]"
        return RequestManager.requestString("com.alipay.charitygamecenter.buyMallItem", data)
    }

    /**
     * 领取活动食物
     *
     * @param foodType
     * @param giftIndex
     * @return
     */
    @JvmStatic
    fun clickForGiftV2(foodType: String?, giftIndex: Int): String {
        val data = "[{\"foodType\":\"$foodType\",\"giftIndex\":$giftIndex,\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"ANTFOREST\",\"version\":\"$VERSION\"}]"
        return RequestManager.requestString("com.alipay.antfarm.clickForGiftV2", data)
    }

    /**
     * 查询抽抽乐活动信息
     *
     * @param userId 用户ID
     * @return 返回结果JSON字符串
     */
    @JvmStatic
    fun queryLoveCabin(userId: String?): String {
        val args1 = "[{\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"ENTERFARM\",\"userId\":\"$userId\",\"version\":\"$VERSION\"}]"
        return RequestManager.requestString("com.alipay.antfarm.queryLoveCabin", args1)
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
    fun chouchouleListFarmTask(drawType: String?): String {
        val taskSceneCode = if ("dailyDraw" == drawType) "ANTFARM_DAILY_DRAW_TASK" else "ANTFARM_IP_DRAW_TASK"
        val args = JSONObject()
        args.put("requestType", "NORMAL")
        args.put("sceneCode", "ANTFARM")
        args.put("signSceneCode", "")
        args.put("source", "H5")
        args.put("taskSceneCode", taskSceneCode)
        args.put("topTask", "")
        val params = "[$args]"
        return RequestManager.requestString("com.alipay.antfarm.listFarmTask", params)
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
    fun chouchouleDoFarmTask(drawType: String?, bizKey: String?): String {
        val taskSceneCode = if ("dailyDraw" == drawType) "ANTFARM_DAILY_DRAW_TASK" else "ANTFARM_IP_DRAW_TASK"
        val args = JSONObject()
        args.put("bizKey", bizKey)
        args.put("requestType", "RPC")
        args.put("sceneCode", "ANTFARM")
        args.put("source", "H5")
        args.put("taskSceneCode", taskSceneCode)
        val params = "[$args]"
        return RequestManager.requestString("com.alipay.antfarm.doFarmTask", params)
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
    fun chouchouleReceiveFarmTaskAward(drawType: String?, taskId: String?): String {
        val taskSceneCode = if ("dailyDraw" == drawType) "ANTFARM_DAILY_DRAW_TASK" else "ANTFARM_IP_DRAW_TASK"
        val awardType = if ("dailyDraw" == drawType) "DAILY_DRAW_TIMES" else "IP_DRAW_MACHINE_DRAW_TIMES"
        val args = JSONObject()
        args.put("awardType", awardType)
        args.put("requestType", "RPC")
        args.put("sceneCode", "ANTFARM")
        args.put("source", "antfarm_villa")
        args.put("taskId", taskId)
        args.put("taskSceneCode", taskSceneCode)
        val params = "[$args]"
        return RequestManager.requestString("com.alipay.antfarm.receiveFarmTaskAward", params)
    }

    /**
     * 查询抽抽乐活动详情（新版统一接口）
     *
     * @param scene 主场景 "dailyDrawMachine" 或 "ipDrawMachine"
     * @param otherScene 其他场景
     * @return 返回结果JSON字符串
     */
    @JvmStatic
    fun queryDrawMachineActivity_New(scene: String?, otherScene: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.queryDrawMachineActivity",
            "[{\"otherScenes\":[\"$otherScene\"],\"requestType\":\"RPC\",\"scene\":\"$scene\",\"sceneCode\":\"ANTFARM\",\"source\":\"antfarm_villa\"}]"
        )
    }

    /**
     * 执行抽奖（IP抽抽乐）
     *
     * @return 返回结果JSON字符串
     */
    @JvmStatic
    fun drawMachineIP(): String {
        return drawMachineIP(1)
    }

    /**
     * 执行抽奖（IP抽抽乐）- 支持连抽
     * @param batchDrawTimes 连抽次数
     * @return 返回结果JSON字符串
     */
    @JvmStatic
    fun drawMachineIP(batchDrawTimes: Int): String {
        val data = "[{" +
                "\"batchDrawTimes\":$batchDrawTimes," +
                "\"requestType\":\"RPC\"," +
                "\"scene\":\"ipDrawMachine\"," +
                "\"sceneCode\":\"ANTFARM\"," +
                "\"source\":\"antfarm_villa\"" +
                "}]"
        return RequestManager.requestString("com.alipay.antfarm.drawMachine", data)
    }

    /**
     * 执行抽奖（普通抽抽乐）
     * @param activityId 活动ID
     * @return 返回结果JSON字符串
     */
    @JvmStatic
    fun drawMachineDaily(activityId: String?): String {
        return drawMachineDaily(1)
    }

    @JvmStatic
    fun drawMachineDaily(batchDrawTimes: Int): String {
        // 构造请求数据，完全匹配日志中的字段
        val data = "[{" +
                "\"batchDrawTimes\":$batchDrawTimes," +
                "\"requestType\":\"RPC\"," +
                "\"scene\":\"dailyDrawMachine\"," +
                "\"sceneCode\":\"ANTFARM\"," +
                "\"source\":\"antfarm_villa\"" + //siliaorenwu  庄园首页抽一次抽抽乐获得饲料任务
                "}]"

        // 使用 RequestManager 发送请求
        return RequestManager.requestString("com.alipay.antfarm.drawMachine", data)
    }

    /**
     * 执行抽奖（指定活动ID）
     *
     * @param activityId 活动ID
     * @return 返回结果JSON字符串
     */
    @JvmStatic
    fun DrawPrize(activityId: String?): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.DrawPrize",
            "[{\"activityId\":\"$activityId\",\"requestType\":\"RPC\",\"sceneCode\":\"ANTFARM\",\"source\":\"icon\"}]"
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
    fun xlightPlugin(referToken: String?, spaceCode: String?): String {
        val positionRequest = JSONObject()
        val referInfo = JSONObject()
        referInfo.put("referToken", referToken)
        positionRequest.put("referInfo", referInfo)
        positionRequest.put("spaceCode", spaceCode)

        val sdkPageInfo = JSONObject()
        sdkPageInfo.put("adComponentType", "GUESS_PRICE")
        sdkPageInfo.put("adComponentVersion", "4.28.66")
        sdkPageInfo.put("networkType", "WIFI")
        sdkPageInfo.put("pageFrom", "ch_url-https://render.alipay.com/p/yuyan/180020380000000182/prizeMachine.html")
        sdkPageInfo.put("pageNo", 1)
        sdkPageInfo.put("pageUrl", "https://render.alipay.com/p/yuyan/180020010001256918/antfarm-landing.html?caprMode=sync")
        sdkPageInfo.put("session", "u_0c09f_b010f")
        sdkPageInfo.put("unionAppId", "2060090000304921")
        sdkPageInfo.put("xlightRuntimeSDKversion", "4.28.66")
        sdkPageInfo.put("xlightSDKType", "h5")
        sdkPageInfo.put("xlightSDKVersion", "4.28.66")

        val args = JSONObject()
        args.put("positionRequest", positionRequest)
        args.put("sdkPageInfo", sdkPageInfo)

        val params = "[$args]"
        return RequestManager.requestString("com.alipay.adexchange.ad.facade.xlightPlugin", params)
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
    fun finishAdTask(playBizId: String?, playEventInfo: JSONObject?, iepTaskType: String?, iepTaskSceneCode: String?): String {
        val extendInfo = JSONObject()
        extendInfo.put("iepTaskSceneCode", iepTaskSceneCode)
        extendInfo.put("iepTaskType", iepTaskType)
        extendInfo.put("playEndingStatus", "success")

        val args = JSONObject()
        args.put("extendInfo", extendInfo)
        args.put("playBizId", playBizId)
        args.put("playEventInfo", playEventInfo)
        args.put("source", "adx")

        val params = "[$args]"
        return RequestManager.requestString("com.alipay.adtask.biz.mobilegw.service.interaction.finish", params)
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
    fun finishTask(taskType: String?, sceneCode: String?, outBizNo: String?): String {
        val args = JSONObject()
        args.put("outBizNo", outBizNo)
        args.put("requestType", "RPC")
        args.put("sceneCode", sceneCode)
        args.put("source", "ADBASICLIB")
        args.put("taskType", taskType)

        val params = "[$args]"
        return RequestManager.requestString("com.alipay.antiep.finishTask", params)
    }

    /**
     * 查询家庭装修信息
     *
     * @return 返回结果JSON字符串
     */
    @JvmStatic
    fun queryFamilyDecoration(): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.queryFamilyDecoration",
            "[{\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM\",\"source\":\"H5\"}]"
        )
    }

    /**
     * 装修金商城 - 分页查询家具列表
     */
    @JvmStatic
    fun getFitmentItemList(activityId: String?, pageSize: Int, labelType: String?, startIndex: Int): String {
        try {
            val args = JSONObject()
            args.put("activityId", activityId)
            if (!labelType.isNullOrEmpty()) {
                args.put("labelType", labelType)
            }
            args.put("pageSize", pageSize)
            args.put("requestType", "NORMAL")
            args.put("sceneCode", "ANTFARM_FITMENT_MALL")
            args.put("source", "antfarm")
            args.put("startIndex", startIndex)
            return RequestManager.requestString("com.alipay.antiep.itemList", "[$args]")
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
    fun getItemDetail(spuId: String?): String {
        return RequestManager.requestString(
            "com.alipay.antiep.itemDetail",
            "[{\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM_FITMENT_MALL\",\"source\":\"antfarm\",\"spuId\":\"$spuId\"}]"
        )
    }

    /**
     * 兑换庄园家具
     */
    @JvmStatic
    fun exchangeBenefit(spuId: String?, skuId: String?, activityId: String?): String {
        val requestId = generateRequestId()
        try {
            val requestDataItem = JSONObject()
            val context = JSONObject()
            context.put("activityId", activityId)

            requestDataItem.put("context", context)
            requestDataItem.put("requestId", requestId)
            requestDataItem.put("requestType", "NORMAL")
            requestDataItem.put("sceneCode", "ANTFARM_FITMENT_MALL")
            requestDataItem.put("skuId", skuId)
            requestDataItem.put("source", "H5")
            requestDataItem.put("spuId", spuId)

            val requestData = JSONArray().put(requestDataItem)
            return RequestManager.requestString(
                "com.alipay.antcommonweal.exchange.h5.exchangeBenefit",
                requestData.toString()
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
    fun FlyGameListFarmTask(): String {
        val args = "[{" +
                "\"bizKey\":\"SHANGYEHUA_GAME_TIMES\"," +
                "\"gameType\":\"flyGame\"," +
                "\"requestType\":\"RPC\"," +
                "\"sceneCode\":\"FLAYGAME\"," +
                "\"signSceneCode\":\"\"," +
                "\"source\":\"ANTFARM\"," +
                "\"taskSceneCode\":\"ANTFARM_GAME_TIMES_TASK\"," +
                "\"version\":\"\"" +
                "}]"
        return RequestManager.requestString("com.alipay.antfarm.listFarmTask", args)
    }

    @JvmStatic
    fun HitGameListFarmTask(): String {
        val args = "[{" +
                "\"bizKey\":\"SHANGYEHUA_HIT_ANIMAL\"," +
                "\"gameType\":\"hitGame\"," +
                "\"requestType\":\"RPC\"," +
                "\"sceneCode\":\"HITGAME\"," +
                "\"signSceneCode\":\"\"," +
                "\"source\":\"ANTFARM\"," +
                "\"taskSceneCode\":\"ANTFARM_GAME_TIMES_TASK\"," +
                "\"version\":\"\"" +
                "}]"
        return RequestManager.requestString("com.alipay.antfarm.listFarmTask", args)
    }

    /**
     * 查询物品列表（ip抽抽乐）
     *
     * @param activityId 活动ID（如图片中的 ipDrawMachine_260112）
     * @param pageSize   每页数量 * @param startIndex 起始索引
     * @return 返回结果JSON字符串
     */
    @JvmStatic
    fun getItemList(activityId: String?, pageSize: Int, startIndex: Int): String {
        val data = "[{\"activityId\":\"$activityId\",\"pageSize\":$pageSize,\"requestType\":\"RPC\",\"sceneCode\":\"ANTFARM_IP_DRAW_MALL\",\"source\":\"antfarm.villa\",\"startIndex\":$startIndex}]"
        return RequestManager.requestString("com.alipay.antiep.itemList", data)
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
    fun exchangeBenefit(spuId: String?, skuId: String?, activityId: String?, sceneCode: String?, source: String?): String {
        val requestId = generateRequestId()
        try {
            val requestDataItem = JSONObject()
            val context = JSONObject()
            context.put("activityId", activityId)

            requestDataItem.put("context", context)
            requestDataItem.put("requestId", requestId)
            requestDataItem.put("requestType", "RPC")
            requestDataItem.put("sceneCode", sceneCode)
            requestDataItem.put("skuId", skuId)
            requestDataItem.put("source", source)
            requestDataItem.put("spuId", spuId)
            val requestData = JSONArray().put(requestDataItem)
            return RequestManager.requestString(
                "com.alipay.antcommonweal.exchange.h5.exchangeBenefit",
                requestData.toString()
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
    fun queryOptionalPlay(): String {
        val args1 = "[{\"bizType\":\"ANTFARM\",\"commonDegradeFilterRequest\":{\"appMode\":\"normal\",\"deviceLevel\":\"high\",\"initialized\":true,\"platform\":\"Android\",\"unityDeviceLevel\":\"high\"},\"playTypeList\":[\"TASK_TRIGGER\",\"TOP_UP_COUPON\"],\"recentAppRecordList\":[],\"requestType\":\"NORMAL\",\"sceneCode\":\"ANTFARM_COMMON\",\"source\":\"H5\",\"version\":\"$VERSION\"}]"
        return RequestManager.requestString("com.alipay.charitygamecenter.queryOptionalPlay", args1)
    }

    /**
     * 庄园限定活动领取
     * @param awardCountForReceive
     * @param sceneCode
     * @param taskType
     * @return
     */
    @JvmStatic
    fun receiveTaskAwardantfarm(awardCountForReceive: Int, sceneCode: String?, taskType: String?): String {
        val args1 = "[{\"awardCountForReceive\":$awardCountForReceive,\"ignoreLimit\":true,\"requestType\":\"RPC\",\"sceneCode\":\"$sceneCode\",\"source\":\"antfarm\",\"taskType\":\"$taskType\"}]"
        return RequestManager.requestString("com.alipay.antieptask.receiveTaskAwardantfarm", args1)
    }
}
