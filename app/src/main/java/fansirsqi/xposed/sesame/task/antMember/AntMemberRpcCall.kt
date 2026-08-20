package fansirsqi.xposed.sesame.task.antMember

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.UUID
import fansirsqi.xposed.sesame.hook.RequestManager
import fansirsqi.xposed.sesame.core.util.RandomUtil
import fansirsqi.xposed.sesame.core.util.TimeUtil

object AntMemberRpcCall {
    private fun getUniqueId(): String {
        return System.currentTimeMillis().toString() + RandomUtil.nextLong()
    }

    /* ant member point */
    @JvmStatic
    suspend fun queryPointCert(page: Int, pageSize: Int): String {
        val args1 = "[{\"page\":$page,\"pageSize\":$pageSize}]"
        return RequestManager.requestString("alipay.antmember.biz.rpc.member.h5.queryPointCert", args1)
    }

    @JvmStatic
    suspend fun receivePointByUser(certId: String?): String {
        val args1 = "[{\"certId\":$certId}]"
        return RequestManager.requestString("alipay.antmember.biz.rpc.member.h5.receivePointByUser", args1)
    }

    @JvmStatic
    suspend fun queryMemberSigninCalendar(): String {
        return RequestManager.requestString(
            "com.alipay.amic.biz.rpc.signin.h5.queryMemberSigninCalendar",
            "[{\"autoSignIn\":true,\"invitorUserId\":\"\",\"sceneCode\":\"QUERY\"}]"
        )
    }

    /* 商家开门打卡任务 */
    @JvmStatic
    suspend fun signIn(activityNo: String?): String {
        return RequestManager.requestString(
            "alipay.merchant.kmdk.signIn",
            "[{\"activityNo\":\"$activityNo\"}]"
        )
    }

    @JvmStatic
    suspend fun signUp(activityNo: String?): String {
        return RequestManager.requestString(
            "alipay.merchant.kmdk.signUp",
            "[{\"activityNo\":\"$activityNo\"}]"
        )
    }

    /* 商家服务 */
    @JvmStatic
    suspend fun transcodeCheck(): String {
        return RequestManager.requestString(
            "alipay.mrchservbase.mrchbusiness.sign.transcode.check",
            "[{}]"
        )
    }

    @JvmStatic
    suspend fun merchantSign(): String {
        return RequestManager.requestString(
            "alipay.mrchservbase.mrchpoint.sqyj.homepage.signin.v1",
            "[{}]"
        )
    }

    @JvmStatic
    suspend fun taskListQuery(): String {
        return RequestManager.requestString(
            "alipay.mrchservbase.task.more.query",
            "[{\"paramMap\":{\"platform\":\"Android\"},\"taskItemCode\":\"\"}]"
        )
    }

    @JvmStatic
    suspend fun queryActivity(): String {
        return RequestManager.requestString(
            "alipay.merchant.kmdk.query.activity",
            "[{\"scene\":\"activityCenter\"}]"
        )
    }

    /* 商家服务任务 */
    @JvmStatic
    suspend fun taskFinish(bizId: String?): String {
        return RequestManager.requestString(
            "com.alipay.adtask.biz.mobilegw.service.task.finish",
            "[{\"bizId\":\"$bizId\"}]"
        )
    }

    @JvmStatic
    suspend fun taskReceive(taskCode: String?): String {
        return RequestManager.requestString(
            "alipay.mrchservbase.sqyj.task.receive",
            "[{\"compId\":\"ZTS_TASK_RECEIVE\",\"extInfo\":{\"taskCode\":\"$taskCode\"}}]"
        )
    }

    @JvmStatic
    suspend fun actioncode(actionCode: String?): String {
        return RequestManager.requestString(
            "alipay.mrchservbase.task.query.by.actioncode",
            "[{\"actionCode\":\"$actionCode\"}]"
        )
    }

    @JvmStatic
    suspend fun produce(actionCode: String?): String {
        return RequestManager.requestString(
            "alipay.mrchservbase.biz.task.action.produce",
            "[{\"actionCode\":\"$actionCode\"}]"
        )
    }

    @JvmStatic
    suspend fun ballReceive(ballIds: String?): String {
        return RequestManager.requestString(
            "alipay.mrchservbase.mrchpoint.ball.receive",
            "[{\"ballIds\":[\"$ballIds\"],\"channel\":\"MRCH_SELF\",\"outBizNo\":\"${getUniqueId()}\"}]"
        )
    }

    @JvmStatic
    suspend fun executeTask(bizParam: String?, bizSubType: String?, bizType: String?, taskConfigId: Long?): String {
        return RequestManager.requestString(
            "alipay.antmember.biz.rpc.membertask.h5.executeTask",
            "[{\"bizOutNo\":\"${TimeUtil.getFormatDate().replace("-", "")}\",\"bizParam\":\"$bizParam\",\"bizSubType\":\"$bizSubType\",\"bizType\":\"$bizType\",\"sourcePassMap\":{\"innerSource\":\"\",\"source\":\"myTab\",\"unid\":\"\"},\"syncProcess\":true,\"taskConfigId\":\"$taskConfigId\"}]"
        )
    }

    @JvmStatic
    suspend fun queryAllStatusTaskList(): String {
        return RequestManager.requestString(
            "alipay.antmember.biz.rpc.membertask.h5.queryAllStatusTaskList",
            "[{\"sourceBusiness\":\"signInAd\",\"sourcePassMap\":{\"innerSource\":\"\",\"source\":\"myTab\",\"unid\":\"\"}}]"
        )
    }

    /**
     * 游戏中心签到查询
     * 对应: com.alipay.gamecenteruprod.biz.rpc.v3.querySignInBall
     */
    @JvmStatic
    suspend fun querySignInBall(): String {
        return RequestManager.requestString(
            "com.alipay.gamecenteruprod.biz.rpc.v3.querySignInBall",
            "[{\"source\":\"ch_alipaysearch__chsub_normal\"}]"
        )
    }

    /**
     * 游戏中心签到
     * 对应: com.alipay.gamecenteruprod.biz.rpc.continueSignIn
     */
    @JvmStatic
    suspend fun continueSignIn(): String {
        return RequestManager.requestString(
            "com.alipay.gamecenteruprod.biz.rpc.continueSignIn",
            "[{\"sceneId\":\"GAME_CENTER\",\"signType\":\"NORMAL_SIGN\",\"source\":\"ch_alipaysearch__chsub_normal\"}]"
        )
    }

    /**
     * 游戏中心任务列表
     * 对应: com.alipay.gamecenteruprod.biz.rpc.v4.queryTaskList
     */
    @JvmStatic
    suspend fun queryGameCenterTaskList(): String {
        return RequestManager.requestString(
            "com.alipay.gamecenteruprod.biz.rpc.v4.queryTaskList",
            "[{\"source\":\"ch_alipaysearch__chsub_normal\"}]"
        )
    }

    /**
     * 游戏中心查询待领取乐豆列表
     * 对应: com.alipay.gamecenteruprod.biz.rpc.v3.queryPointBallList
     */
    @JvmStatic
    suspend fun queryPointBallList(): String {
        return RequestManager.requestString(
            "com.alipay.gamecenteruprod.biz.rpc.v3.queryPointBallList",
            "[{\"source\":\"ch_alipaysearch__chsub_normal\"}]"
        )
    }

    /**
     * 游戏中心全部领取
     * 对应: com.alipay.gamecenteruprod.biz.rpc.v3.batchReceivePointBall
     */
    @JvmStatic
    suspend fun batchReceivePointBall(): String {
        return RequestManager.requestString(
            "com.alipay.gamecenteruprod.biz.rpc.v3.batchReceivePointBall",
            "[{}]"
        )
    }

    /**
     * 游戏中心普通平台任务完成（如貔貅任务）
     * 对应: com.alipay.gamecenteruprod.biz.rpc.v3.doTaskSend
     */
    @JvmStatic
    suspend fun doTaskSend(taskId: String?): String {
        return RequestManager.requestString(
            "com.alipay.gamecenteruprod.biz.rpc.v3.doTaskSend",
            "[{\"taskId\":\"$taskId\"}]"
        )
    }

    /**
     * 游戏中心签到类平台任务完成（needSignUp = true）
     * 对应: com.alipay.gamecenteruprod.biz.rpc.v3.doTaskSignup
     */
    @JvmStatic
    suspend fun doTaskSignup(taskId: String?): String {
        return RequestManager.requestString(
            "com.alipay.gamecenteruprod.biz.rpc.v3.doTaskSignup",
            "[{\"source\":\"ch_alipaysearch__chsub_normal\",\"taskId\":\"$taskId\"}]"
        )
    }

    /**
     * 芝麻信用首页
     */
    @JvmStatic
    suspend fun queryHome(): String {
        return RequestManager.requestString(
            "com.antgroup.zmxy.zmcustprod.biz.rpc.home.api.HomeV8RpcManager.queryHome",
            "[{\"invokeSource\":\"zmHome\",\"miniZmGrayInside\":\"\",\"version\":\"week\"}]"
        )
    }

    /**
     * 芝麻信用首页 - 服务卡片（含芝麻粒签到卡片）
     * 对应: com.antgroup.zmxy.zmcustprod.biz.rpc.home.api.HomeV8RpcManager.queryServiceCard
     */
    @JvmStatic
    suspend fun queryServiceCard(): String {
        return RequestManager.requestString(
            "com.antgroup.zmxy.zmcustprod.biz.rpc.home.api.HomeV8RpcManager.queryServiceCard",
            "[{}]"
        )
    }

    /**
     * 芝麻签到 - 通用完成接口（芝麻粒/炼金等）
     * 对应: com.antgroup.zmxy.zmmemberop.biz.rpc.pointtask.CheckInTaskRpcManager.completeTask
     * @param checkInDate yyyyMMdd
     * @param sceneCode   "zml" 对应芝麻粒福利签到, "alchemy" 对应芝麻炼金签到
     */
    @JvmStatic
    suspend fun zmCheckInCompleteTask(checkInDate: String?, sceneCode: String?): String {
        return RequestManager.requestString(
            "com.antgroup.zmxy.zmmemberop.biz.rpc.pointtask.CheckInTaskRpcManager.completeTask",
            "[{\"checkInDate\":\"$checkInDate\",\"sceneCode\":\"$sceneCode\"}]"
        )
    }

    /**
     * 获取芝麻信用任务列表
     */
    @JvmStatic
    suspend fun queryAvailableSesameTask(): String {
        return RequestManager.requestString(
            "com.antgroup.zmxy.zmmemberop.biz.rpc.creditaccumulate.CreditAccumulateStrategyRpcManager.queryListV3",
            "[{}]"
        )
    }

    /**
     * 芝麻信用领取任务
     */
    @JvmStatic
    suspend fun joinSesameTask(taskTemplateId: String?): String {
        return RequestManager.requestString(
            "com.antgroup.zmxy.zmmemberop.biz.rpc.promise.PromiseRpcManager.joinActivity",
            "[{\"chInfo\":\"seasameList\",\"joinFromOuter\":false,\"templateId\":\"$taskTemplateId\"}]"
        )
    }

    /**
     * 芝麻信用获取任务回调
     */
    @JvmStatic
    suspend fun feedBackSesameTask(taskTemplateId: String?): String {
        return RequestManager.requestString(
            "com.antgroup.zmxy.zmmemberop.biz.rpc.creditaccumulate.CreditAccumulateStrategyRpcManager.taskFeedback",
            "[{\"actionType\":\"TO_COMPLETE\",\"templateId\":\"$taskTemplateId\"}]",
            "zmmemberop", "taskFeedback", "CreditAccumulateStrategyRpcManager"
        )
    }

    /**
     * 芝麻信用完成任务
     */
    @JvmStatic
    suspend fun finishSesameTask(recordId: String?): String {
        return RequestManager.requestString(
            "com.antgroup.zmxy.zmmemberop.biz.rpc.promise.PromiseRpcManager.pushActivity",
            "[{\"recordId\":\"$recordId\"}]"
        )
    }

    /**
     * 查询可收取的芝麻粒
     */
    @JvmStatic
    suspend fun queryCreditFeedback(): String {
        return RequestManager.requestString(
            "com.antgroup.zmxy.zmcustprod.biz.rpc.home.creditaccumulate.api.CreditAccumulateRpcManager.queryCreditFeedback",
            "[{\"queryPotential\":false,\"size\":20,\"status\":\"UNCLAIMED\"}]"
        )
    }

    /**
     * 一键收取芝麻粒
     */
    @JvmStatic
    suspend fun collectAllCreditFeedback(): String {
        return RequestManager.requestString(
            "com.antgroup.zmxy.zmcustprod.biz.rpc.home.creditaccumulate.api.CreditAccumulateRpcManager.collectCreditFeedback",
            "[{\"collectAll\":true,\"status\":\"UNCLAIMED\"}]"
        )
    }

    /**
     * 收取芝麻粒
     *
     * @param creditFeedbackId creditFeedbackId
     */
    @JvmStatic
    suspend fun collectCreditFeedback(creditFeedbackId: String?): String {
        return RequestManager.requestString(
            "com.antgroup.zmxy.zmcustprod.biz.rpc.home.creditaccumulate.api.CreditAccumulateRpcManager.collectCreditFeedback",
            "[{\"collectAll\":false,\"creditFeedbackId\":\"$creditFeedbackId\",\"status\":\"UNCLAIMED\"}]"
        )
    }

    /**
     * 获取所有可领取的保障金
     */
    @JvmStatic
    suspend fun queryAvailableCollectInsuredGold(): String {
        return RequestManager.requestString(
            "com.alipay.insgiftbff.insgiftMain.queryMultiSceneWaitToGainList",
            "[{\"entrance\":\"wealth_entry\",\"eventToWaitParamDTO\":{\"giftProdCode\":\"GIFT_UNIVERSAL_COVERAGE\",\"rightNoList\":[\"UNIVERSAL_ACCIDENT\",\"UNIVERSAL_HOSPITAL\",\"UNIVERSAL_OUTPATIENT\",\"UNIVERSAL_SERIOUSNESS\",\"UNIVERSAL_WEALTH\",\"UNIVERSAL_TRANS\",\"UNIVERSAL_FRAUD_LIABILITY\"]},\"helpChildParamDTO\":{\"giftProdCode\":\"GIFT_HEALTH_GOLD_CHILD\",\"rightNoList\":[\"UNIVERSAL_ACCIDENT\",\"UNIVERSAL_HOSPITAL\",\"UNIVERSAL_OUTPATIENT\",\"UNIVERSAL_SERIOUSNESS\",\"UNIVERSAL_WEALTH\",\"UNIVERSAL_TRANS\",\"UNIVERSAL_FRAUD_LIABILITY\"]},\"priorityChannelParamDTO\":{\"giftProdCode\":\"GIFT_UNIVERSAL_COVERAGE\",\"rightNoList\":[\"UNIVERSAL_ACCIDENT\",\"UNIVERSAL_HOSPITAL\",\"UNIVERSAL_OUTPATIENT\",\"UNIVERSAL_SERIOUSNESS\",\"UNIVERSAL_WEALTH\",\"UNIVERSAL_TRANS\",\"UNIVERSAL_FRAUD_LIABILITY\"]},\"signInParamDTO\":{\"giftProdCode\":\"GIFT_UNIVERSAL_COVERAGE\",\"rightNoList\":[\"UNIVERSAL_ACCIDENT\",\"UNIVERSAL_HOSPITAL\",\"UNIVERSAL_OUTPATIENT\",\"UNIVERSAL_SERIOUSNESS\",\"UNIVERSAL_WEALTH\",\"UNIVERSAL_TRANS\",\"UNIVERSAL_FRAUD_LIABILITY\"]}}]",
            "insgiftbff", "queryMultiSceneWaitToGainList", "insgiftMain"
        )
    }

    /**
     * 领取保障金
     */
    @JvmStatic
    suspend fun collectInsuredGold(goldBallObj: JSONObject): String {
        return RequestManager.requestString(
            "com.alipay.insgiftbff.insgiftMain.gainMyAndFamilySumInsured",
            goldBallObj.toString(), "insgiftbff", "gainMyAndFamilySumInsured", "insgiftMain"
        )
    }

    // 安心豆
    @JvmStatic
    suspend fun querySignInProcess(appletId: String?, scene: String?): String {
        return RequestManager.requestString(
            "com.alipay.insmarketingbff.bean.querySignInProcess",
            "[{\"appletId\":\"$appletId\",\"scene\":\"$scene\"}]"
        )
    }

    @JvmStatic
    suspend fun signInTrigger(appletId: String?, scene: String?): String {
        return RequestManager.requestString(
            "com.alipay.insmarketingbff.bean.signInTrigger",
            "[{\"appletId\":\"$appletId\",\"scene\":\"$scene\"}]"
        )
    }

    @JvmStatic
    suspend fun beanExchangeDetail(itemId: String?): String {
        return RequestManager.requestString(
            "com.alipay.insmarketingbff.onestop.planTrigger",
            "[{\"extParams\":{\"itemId\":\"$itemId\"},\"planCode\":\"bluebean_onestop\",\"planOperateCode\":\"exchangeDetail\"}]"
        )
    }

    @JvmStatic
    suspend fun beanExchange(itemId: String?, pointAmount: Int): String {
        return RequestManager.requestString(
            "com.alipay.insmarketingbff.onestop.planTrigger",
            "[{\"extParams\":{\"itemId\":\"$itemId\",\"pointAmount\":\"$pointAmount\"},\"planCode\":\"bluebean_onestop\",\"planOperateCode\":\"exchange\"}]"
        )
    }

    @JvmStatic
    suspend fun queryUserAccountInfo(pointProdCode: String?): String {
        return RequestManager.requestString(
            "com.alipay.insmarketingbff.point.queryUserAccountInfo",
            "[{\"channel\":\"HiChat\",\"pointProdCode\":\"$pointProdCode\",\"pointUnitType\":\"COUNT\"}]"
        )
    }

    /**
     * 查询会员信息
     */
    @JvmStatic
    suspend fun queryMemberInfo(): String {
        val data = "[{\"needExpirePoint\":true,\"needGrade\":true,\"needPoint\":true,\"queryScene\":\"POINT_EXCHANGE_SCENE\",\"source\":\"POINT_EXCHANGE_SCENE\",\"sourcePassMap\":{\"innerSource\":\"\",\"source\":\"\",\"unid\":\"\"}}]"
        return RequestManager.requestString("com.alipay.alipaymember.biz.rpc.member.h5.queryMemberInfo", data)
    }

    /**
     * 查询0元兑公益道具列表
     *
     * @param userId       userId
     * @param pointBalance 当前可用会员积分
     */
    @JvmStatic
    suspend fun queryShandieEntityList(userId: String?, pointBalance: String?): String {
        val uniqueId = "${System.currentTimeMillis()}$userId" + "94000SR202501061144200394000SR2025010611458003"
        val data = "[{\"blackIds\":[],\"deliveryIdList\":[\"94000SR2025010611442003\",\"94000SR2025010611458003\"],\"filterCityCode\":false,\"filterPointNoEnough\":false,\"filterStockNoEnough\":false,\"pageNum\":1,\"pageSize\":18,\"point\":$pointBalance,\"previewCopyDbId\":\"\",\"queryType\":\"DELIVERY_ID_LIST\",\"source\":\"member_day\",\"sourcePassMap\":{\"innerSource\":\"\",\"source\":\"0yuandui\",\"unid\":\"\"},\"topIds\":[],\"uniqueId\":\"$uniqueId\"}]"
        return RequestManager.requestString("com.alipay.alipaymember.biz.rpc.config.h5.queryShandieEntityList", data)
    }

    @JvmStatic
    suspend fun queryDeliveryZoneDetail(deliveryIdList: List<String>, pageNum: Int, pageSize: Int): String {
        // 1. 处理 uniqueId 的拼接逻辑
        // 固定前缀：17665547901390and99999999INTELLIGENT_SORT92524974
        val idsJoined = deliveryIdList.joinToString(",")
        val uniqueId = "17665547901390and99999999INTELLIGENT_SORT92524974" + idsJoined

        // 2. 将 deliveryIdList 转换为 JSON 数组字符串格式
        // 这里为了简单直观使用 String.format，如果项目中有 Fastjson/Gson 建议使用序列化
        val deliveryIdListJson = StringBuilder("[")
        for (i in deliveryIdList.indices) {
            deliveryIdListJson.append("\"").append(deliveryIdList[i]).append("\"")
            if (i < deliveryIdList.size - 1) {
                deliveryIdListJson.append(",")
            }
        }
        deliveryIdListJson.append("]")

        // 3. 构造完整的请求 Data 字符串
        val data = "[{" +
                "\"deliveryIdList\":$deliveryIdListJson," +
                "\"lowerPoint\":0," +
                "\"pageNum\":$pageNum," +
                "\"pageSize\":$pageSize," +
                "\"queryNoReserve\":true," +
                "\"resourceCardChannel\":\"ZERO_EXCHANGE_CHANNEL\"," +
                "\"sourcePassMap\":{\"innerSource\":\"\",\"source\":\"\",\"unid\":\"\"}," +
                "\"startPageFirstQuery\":false," +
                "\"topIdList\":[\"202412231259661040\"]," +
                "\"uniqueId\":\"$uniqueId\"," +
                "\"upperPoint\":99999999," +
                "\"withPointRange\":false" +
                "}]"
        return RequestManager.requestString("com.alipay.alipaymember.biz.rpc.config.h5.queryDeliveryZoneDetail", data)
    }

    /*
    public static String exchangeBenefit(String benefitId, String userId) {
        // 1. 生成请求ID（前缀+当前毫秒时间戳）
        String requestId = "requestId" + System.currentTimeMillis();
        // 2. 生成唯一unid（UUID随机生成，也可使用时间戳，此处UUID更规范）
        String unid = UUID.randomUUID().toString();
        // 3. 拼接requestSourceInfo（用户ID+|0+当前毫秒时间戳）
        String requestSourceInfo = String.format("SID:%s%s|0", userId, System.currentTimeMillis());
        // 4. 构建符合新结构的请求体（移除废弃字段，新增sceneId等必填字段）
        String data = String.format("[{\"benefitId\":\"%s\",\"exchangeType\":\"POINT_PAY\",\"requestId\":\"%s\",\"requestSourceInfo\":\"%s\",\"sceneId\":\"1209\",\"sourcePassMap\":{\"bid\":\"\",\"feedsIndex\":\"0\",\"innerSource\":\"a169.b52659\",\"isCpc\":\"\",\"source\":\"\",\"unid\":\"%s\",\"uniqueId\":\"%s%s\"}]",
                benefitId,
                requestId,
                requestSourceInfo,
                unid,
                userId,
                System.currentTimeMillis());
        // 5. 发起接口请求并返回结果
        return RequestManager.requestString("com.alipay.alipaymember.biz.rpc.exchange.h5.exchangeBenefit", data);
    }*/
    @JvmStatic
    suspend fun exchangeBenefit(benefitId: String?, itemId: String?, userId: String?): String {
        val now = System.currentTimeMillis()

        // 1. 生成请求ID
        val requestId = "requestId$now"

        // 2. 生成唯一unid (UUID)
        val unid = UUID.randomUUID().toString()

        // 3. 生成 uniqueId (通常是 userId + 时间戳，或者直接是 userId)
        // 根据你提供的 JSON，这里似乎直接是 userId 拼接了一个标记或时间戳
        val uniqueId = userId + now

        // 4. 拼接 requestSourceInfo
        val requestSourceInfo = String.format("SID:%s|0", uniqueId)

        // 5. 构建符合最新结构的 JSON 数据
        // 注意：增加了 itemId, cityCode, miniAppId 等字段
        val data = String.format(
            "[" +
                    "{" +
                    "\"benefitId\":\"%s\"," +
                    "\"cityCode\":\"\"," +
                    "\"exchangeType\":\"POINT_PAY\"," +
                    "\"itemId\":\"%s\"," +
                    "\"miniAppId\":\"\"," +
                    "\"orderSource\":\"\"," +
                    "\"requestId\":\"%s\"," +
                    "\"requestSourceInfo\":\"%s\"," +
                    "\"sourcePassMap\":{" +
                    "\"alipayClientVersion\":\"10.7.80.8000\"," +
                    "\"bid\":\"\"," +
                    "\"feedsIndex\":\"0\"," +
                    "\"innerSource\":\"a159.b52659\"," +
                    "\"isCpc\":\"\"," +
                    "\"mobileOsType\":\"Android\"," +
                    "\"source\":\"\"," +
                    "\"unid\":\"%s\"," +
                    "\"uniqueId\":\"%s\"" +
                    "}," +
                    "\"userOutAccount\":\"\"" +
                    "}" +
                    "]",
            benefitId,
            itemId,
            requestId,
            requestSourceInfo,
            unid,
            uniqueId
        )

        // 6. 发起接口请求
        return RequestManager.requestString("com.alipay.alipaymember.biz.rpc.exchange.h5.exchangeBenefit", data)
    }

    /**
     * 查询芝麻粒兑换商品列表
     * 对应接口: com.antgroup.zmxy.zmmemberop.biz.rpc.award.AwardRpcManager.queryListV2
     *
     * @param page     页码
     * @param pageSize 每页数量
     */
    @JvmStatic
    suspend fun queryExchangeList(page: Int, pageSize: Int): String {
        // 参数构造参考抓包: [{"currentPage":1,"formDelivery":"false","pageSize":20,"privilegeSource":"","privilegeTab":"","tabList":[]}]
        val args = "[{\"currentPage\":$page,\"formDelivery\":\"false\",\"pageSize\":$pageSize,\"privilegeSource\":\"\",\"privilegeTab\":\"\",\"tabList\":[]}]"
        return RequestManager.requestString(
            "com.antgroup.zmxy.zmmemberop.biz.rpc.award.AwardRpcManager.queryListV2",
            args
        )
    }

    /**
     * 执行芝麻粒兑换
     * 对应接口: com.antgroup.zmxy.zmmemberop.biz.rpc.award.AwardRpcManager.obtainAward
     *
     * @param templateId 商品ID (awardTemplateId)
     */
    @JvmStatic
    suspend fun obtainAward(templateId: String?): String {
        // 参数构造参考抓包: [{"awardTemplateId":"245213012"}]
        val args = "[{\"awardTemplateId\":\"$templateId\"}]"
        return RequestManager.requestString(
            "com.antgroup.zmxy.zmmemberop.biz.rpc.award.AwardRpcManager.obtainAward",
            args
        )
    }

    // ================= 年度回顾（任务中心） =================
    const val ANNUAL_REVIEW_OPERATION_IDENTIFY: String =
        "independent_component_program2025111803036407"
    const val ANNUAL_REVIEW_COMPONENT_PREFIX: String =
        "independent_component_task_reward_v2_02888775"
    const val ANNUAL_REVIEW_QUERY_COMPONENT: String =
        ANNUAL_REVIEW_COMPONENT_PREFIX + "_independent_component_task_reward_query"
    const val ANNUAL_REVIEW_APPLY_COMPONENT: String =
        ANNUAL_REVIEW_COMPONENT_PREFIX + "_independent_component_task_reward_apply"
    const val ANNUAL_REVIEW_PROCESS_COMPONENT: String =
        ANNUAL_REVIEW_COMPONENT_PREFIX + "_independent_component_task_reward_process"
    const val ANNUAL_REVIEW_Get: String =
        ANNUAL_REVIEW_COMPONENT_PREFIX + "_independent_component_task_reward_process"
    const val ANNUAL_REVIEW_GET_REWARD_COMPONENT: String =
        ANNUAL_REVIEW_COMPONENT_PREFIX + "_independent_component_task_reward_get_reward"

    @Throws(JSONException::class)
    private fun buildAnnualReviewBasePayload(): JSONObject {
        val root = JSONObject()
        root.put("channel", "share")
        root.put("cityCode", "110000")
        root.put("operationParamIdentify", ANNUAL_REVIEW_OPERATION_IDENTIFY)
        // 默认 source 为查询组件，具体请求中可覆盖
        root.put("source", ANNUAL_REVIEW_QUERY_COMPONENT)
        return root
    }

    /**
     * 年度回顾 - 查询任务列表
     *
     * 对应文档示例：components 中携带
     *   independent_component_task_reward_v2_02888775_independent_component_task_reward_query
     */
    @JvmStatic
    suspend fun annualReviewQueryTasks(): String? {
        try {
            val body = buildAnnualReviewBasePayload()
            val components = JSONObject()
            components.put(ANNUAL_REVIEW_QUERY_COMPONENT, JSONObject())
            body.put("components", components)
            body.put("source", ANNUAL_REVIEW_QUERY_COMPONENT)
            return RequestManager.requestString(
                "alipay.imasp.program.programInvoke",
                JSONArray().put(body).toString()
            )
        } catch (e: Throwable) {
            return null
        }
    }

    /**
     * 年度回顾 - 领取单个任务（apply）
     *
     * 请求示例参见文档：components 中携带
     *   independent_component_task_reward_v2_02888775_independent_component_task_reward_apply
     */
    @JvmStatic
    suspend fun annualReviewApplyTask(code: String?): String? {
        try {
            val body = buildAnnualReviewBasePayload()
            val compBody = JSONObject()
            compBody.put("code", code)
            compBody.put("consultAfterLuckDraw", "false")
            compBody.put("skipLuckDrawConsult", "true")

            val components = JSONObject()
            components.put(ANNUAL_REVIEW_APPLY_COMPONENT, compBody)

            body.put("components", components)
            body.put("source", ANNUAL_REVIEW_APPLY_COMPONENT)
            return RequestManager.requestString(
                "alipay.imasp.program.programInvoke",
                JSONArray().put(body).toString()
            )
        } catch (e: Throwable) {
            return null
        }
    }

    /**
     * 年度回顾 - 提交任务完成（process）
     *
     * 请求示例参见文档：components 中携带
     *   independent_component_task_reward_v2_02888775_independent_component_task_reward_process
     */
    @JvmStatic
    suspend fun annualReviewProcessTask(code: String?, recordNo: String?): String? {
        try {
            val body = buildAnnualReviewBasePayload()
            val compBody = JSONObject()
            compBody.put("code", code)
            compBody.put("recordNo", recordNo)

            val components = JSONObject()
            components.put(ANNUAL_REVIEW_PROCESS_COMPONENT, compBody)

            body.put("components", components)
            body.put("source", ANNUAL_REVIEW_PROCESS_COMPONENT)
            return RequestManager.requestString(
                "alipay.imasp.program.programInvoke",
                JSONArray().put(body).toString()
            )
        } catch (e: Throwable) {
            return null
        }
    }

    /**
     * 年度回顾 - 领取奖励（get_reward）
     *
     * 在任务完成后，根据 code + recordNo 领取成长值奖励。
     */
    @JvmStatic
    suspend fun annualReviewGetReward(code: String?, recordNo: String?): String? {
        try {
            val body = buildAnnualReviewBasePayload()
            val compBody = JSONObject()
            compBody.put("code", code)
            compBody.put("consultAfterLuckDraw", "false")
            compBody.put("recordNo", recordNo)
            compBody.put("skipLuckDrawConsult", "true")

            val components = JSONObject()
            components.put(ANNUAL_REVIEW_GET_REWARD_COMPONENT, compBody)

            body.put("components", components)
            body.put("source", ANNUAL_REVIEW_GET_REWARD_COMPONENT)
            return RequestManager.requestString(
                "alipay.imasp.program.programInvoke",
                JSONArray().put(body).toString()
            )
        } catch (e: Throwable) {
            return null
        }
    }

    // ================= 芝麻树 =================
    private const val ZHIMATREE_PLAY_INFO = "SwbtxJSo8OOUrymAU%2FHnY2jyFRc%2BkCJ3"
    private const val ZHIMATREE_REFER = "https://render.alipay.com/p/yuyan/180020010001269849/zmTree.html?caprMode=sync&chInfo=chInfo=ch_zmzltf__chsub_xinyongsyyingxiaowei"

    /**
     * 查询芝麻树首页
     */
    @JvmStatic
    suspend fun zhimaTreeHomePage(): String? {
        try {
            val args = JSONObject()
            args.put("operation", "ZHIMA_TREE_HOME_PAGE")
            args.put("playInfo", ZHIMATREE_PLAY_INFO)
            args.put("refer", ZHIMATREE_REFER)
            args.put("extInfo", JSONObject())
            return RequestManager.requestString(
                "alipay.promoprod.play.trigger",
                JSONArray().put(args).toString()
            )
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * 净化芝麻树 (消耗净化值)
     */
    @JvmStatic
    suspend fun zhimaTreeCleanAndPush(treeCode: String?): String? {
        try {
            val args = JSONObject()
            args.put("operation", "ZHIMA_TREE_CLEAN_AND_PUSH")
            args.put("playInfo", ZHIMATREE_PLAY_INFO)
            args.put("refer", ZHIMATREE_REFER)

            val extInfo = JSONObject()
            extInfo.put("clickNum", "1")
            extInfo.put("treeCode", treeCode)
            args.put("extInfo", extInfo)
            return RequestManager.requestString(
                "alipay.promoprod.play.trigger",
                JSONArray().put(args).toString()
            )
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * 查询做任务赚净化值列表
     */
    @JvmStatic
    suspend fun queryRentGreenTaskList(): String? {
        try {
            val args = JSONObject()
            args.put("operation", "RENT_GREEN_TASK_LIST_QUERY")
            args.put("playInfo", ZHIMATREE_PLAY_INFO)
            args.put("refer", ZHIMATREE_REFER)

            val extInfo = JSONObject()
            extInfo.put("chInfo", "ch_share__chsub_ALPContact")
            extInfo.put("batchId", "")
            args.put("extInfo", extInfo)
            return RequestManager.requestString(
                "alipay.promoprod.play.trigger",
                JSONArray().put(args).toString()
            )
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * 完成/领取净化值任务
     * @param stageCode "send" 表示去完成/开始, "receive" 表示领取奖励
     */
    @JvmStatic
    suspend fun rentGreenTaskFinish(taskId: String?, stageCode: String?): String? {
        try {
            val args = JSONObject()
            args.put("operation", "RENT_GREEN_TASK_FINISH")
            args.put("playInfo", ZHIMATREE_PLAY_INFO)
            args.put("refer", ZHIMATREE_REFER)

            val extInfo = JSONObject()
            extInfo.put("chInfo", "ch_share__chsub_ALPContact")
            extInfo.put("taskId", taskId)
            extInfo.put("stageCode", stageCode)
            args.put("extInfo", extInfo)
            return RequestManager.requestString(
                "alipay.promoprod.play.trigger",
                JSONArray().put(args).toString()
            )
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * [新] 福利中心首页
     */
    @JvmStatic
    suspend fun queryWelfareHome(): String? {
        try {
            val args = JSONObject()
            args.put("isResume", true)
            // 接口: com.alipay.finaggexpbff.needle.welfareCenter.index
            return RequestManager.requestString(
                "com.alipay.finaggexpbff.needle.welfareCenter.index",
                JSONArray().put(args).toString()
            )
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * [新] 任务查询推送
     */
    @JvmStatic
    suspend fun taskQueryPush(taskId: String?): String? {
        try {
            val args = JSONObject()
            args.put("mode", 1) // 固定参数
            args.put("taskId", taskId)

            // 接口: com.alipay.wealthgoldtwa.needle.taskQueryPush
            return RequestManager.requestString(
                "com.alipay.wealthgoldtwa.needle.taskQueryPush",
                JSONArray().put(args).toString()
            )
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * 签到 / 领取奖励
     * @param type "SIGN"
     */
    @JvmStatic
    suspend fun welfareCenterTrigger(type: String?): String? {
        try {
            val args = JSONObject()
            args.put("type", type)
            return RequestManager.requestString(
                "com.alipay.finaggexpbff.needle.welfareCenter.trigger",
                JSONArray().put(args).toString()
            )
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * 任务触发/报名
     */
    @JvmStatic
    suspend fun goldBillTaskTrigger(taskId: String?): String? {
        try {
            val args = JSONObject()
            args.put("taskId", taskId)
            return RequestManager.requestString(
                "com.alipay.wealthgoldtwa.goldbill.v4.task.trigger",
                JSONArray().put(args).toString()
            )
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * [新增] 查询黄金票提取页信息
     * 用于获取最新的可用数量、基金ID (productId) 和 赠送份数 (bonusAmount)
     */
    @JvmStatic
    suspend fun queryConsumeHome(): String? {
        try {
            val args = JSONObject()
            args.put("tabBubbleDeliverParam", JSONObject())
            args.put("tabTypeDeliverParam", JSONObject())
            // 接口: com.alipay.wealthgoldtwa.needle.consume.query
            return RequestManager.requestString(
                "com.alipay.wealthgoldtwa.needle.consume.query",
                JSONArray().put(args).toString()
            )
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * [新增] 提交提取黄金
     * @param amount 提取数量 (如 100, 200, 2900)
     * @param productId 基金ID
     * @param bonusAmount 额外赠送数量
     */
    @JvmStatic
    suspend fun submitConsume(amount: Int, productId: String?, bonusAmount: Int): String? {
        try {
            val args = JSONObject()
            args.put("exchangeAmount", amount)
            // 计算金额：100份 = 0.10元。公式：份数 / 1000.0
            args.put("exchangeMoney", String.format("%.2f", amount / 1000.0))
            args.put("prizeType", "GOLD") // 固定为黄金
            args.put("productId", productId)
            args.put("bonusAmount", bonusAmount)
            // 接口: com.alipay.wealthgoldtwa.needle.consume.submit
            return RequestManager.requestString(
                "com.alipay.wealthgoldtwa.needle.consume.submit",
                JSONArray().put(args).toString()
            )
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * @brief 查询当月是否有可领取的贴纸
     * @param year 年份
     * @param month 月份
     */
    @JvmStatic
    suspend fun queryStickerCanReceive(year: String?, month: String?): String {
        val data = "[{" +
                "\"isFirstShow\":\"false\"," +
                "\"month\":\"$month\"," +
                "\"scene\":\"\"," +
                "\"year\":\"$year\"" +
                "}]"
        return RequestManager.requestString("alipay.memberasset.sticker.queryStickerCanReceive", data)
    }

    /**
     * 领取指定的贴纸
     * @param stickerIds 贴纸ID集合
     */
    @JvmStatic
    suspend fun receiveSticker(year: String?, month: String?, stickerIds: List<String>?): String? {
        if (stickerIds.isNullOrEmpty()) return null

        // 构建 stickerIds 的 JSON 数组字符串
        val sb = StringBuilder("[")
        for (i in stickerIds.indices) {
            sb.append("\"").append(stickerIds[i]).append("\"")
            if (i < stickerIds.size - 1) sb.append(",")
        }
        sb.append("]")

        val data = "[{\"month\":\"$month\",\"stickerIds\":$sb,\"year\":\"$year\"}]"
        return RequestManager.requestString("alipay.memberasset.sticker.receiveSticker", data)
    }

    //芝麻信誉 部分
    object Zmxy {
        private var Version = "2025-10-22"
        //芝麻粒炼金

        /**
         * 信誉获取任务列表（成长任务）
         * <p>
         * 对应抓包：
         * <pre>
         *   Method: com.antgroup.zmxy.zmcustprod.biz.rpc.growthbehavior.apiGrowthBehaviorRpcManager.queryToDoList
         *   requestData: [{"guideBehaviorId":"yuebao_7d","invokeVersion":"1.0.2025.10.27","switchNewPage":true}]
         * </pre>
         * 说明：
         * <ul>
         *   <li>__apiCallStartTime / __apiNativeCallId 属于容器层元数据，不需要拼在 requestData 里</li>
         *   <li>guideBehaviorId 用来指定「引导任务」的入口，通常传 yuebao_7d 即可拉全量列表</li>
         *   <li>invokeVersion 建议保持和抓包一致，方便服务器做灰度控制</li>
         * </ul>
         *
         * @param guideBehaviorId 抓包中的 guideBehaviorId，例如 "yuebao_7d"
         * @param invokeVersion   抓包中的 invokeVersion，例如 "1.0.2025.10.27"
         */
        @JvmStatic
        suspend fun queryGrowthGuideToDoList(guideBehaviorId: String?, invokeVersion: String?): String {
            var guideBehaviorId = guideBehaviorId
            var invokeVersion = invokeVersion
            if (guideBehaviorId.isNullOrEmpty()) {
                guideBehaviorId = "yuebao_7d"
            }
            if (invokeVersion.isNullOrEmpty()) {
                // 默认使用抓包中观察到的版本号，避免服务端按版本做限流/灰度
                invokeVersion = "1.0.2025.10.27"
            }
            val data = "[{" +
                    "\"guideBehaviorId\":\"$guideBehaviorId\"," +
                    "\"invokeVersion\":\"$invokeVersion\"," +
                    "\"switchNewPage\":true" +
                    "}]"
            return RequestManager.requestString(
                "com.antgroup.zmxy.zmcustprod.biz.rpc.growthbehavior.apiGrowthBehaviorRpcManager.queryToDoList",
                data
            )
        }

        /**
         * 信誉任务「领取任务 / 触发接收」接口。
         * <p>
         * 对应抓包：
         * <pre>
         *   Method: com.antgroup.zmxy.zmcustprod.biz.rpc.growthbehavior.apiGrowthBehaviorRpcManager.openBehaviorCollect
         *   requestData: [{"behaviorId":"babanongchang_7d"}]
         * </pre>
         * behaviorId 直接来自 queryToDoList 返回的 toDoList[i].behaviorId。
         */
        @JvmStatic
        suspend fun openBehaviorCollect(behaviorId: String?): String {
            val data = "[{\"behaviorId\":\"$behaviorId\"}]"
            return RequestManager.requestString(
                "com.antgroup.zmxy.zmcustprod.biz.rpc.growthbehavior.apiGrowthBehaviorRpcManager.openBehaviorCollect",
                data
            )
        }

        /**
         * 查询每日答题题目（每日问答）。
         *
         * 对应抓包：
         *   Method: com.antgroup.zmxy.zmcustprod.biz.rpc.growthtask.api.GrowthTaskRpcManager.queryDailyQuiz
         *   requestData: [{"behaviorId":"meiriwenda"}]
         *
         * @param behaviorId 行为 ID（例如 "meiriwenda"）
         */
        @JvmStatic
        suspend fun queryDailyQuiz(behaviorId: String?): String {
            val data = "[{\"behaviorId\":\"$behaviorId\"}]"
            return RequestManager.requestString(
                "com.antgroup.zmxy.zmcustprod.biz.rpc.growthtask.api.GrowthTaskRpcManager.queryDailyQuiz",
                data
            )
        }

        /**
         * 提交每日答题结果。
         *
         * 对应抓包：
         *   Method: com.antgroup.zmxy.zmcustprod.biz.rpc.growthtask.api.GrowthTaskRpcManager.pushDailyTask
         *   requestData: [{
         *       "behaviorId":"meiriwenda",
         *       "bizDate":1764564388751,
         *       "extInfo":{
         *           "answerId":"20250925_3_0",
         *           "answerStatus":"RIGHT",
         *           "questionId":"20250925_3"
         *       }
         *   }]
         *
         * @param behaviorId    行为 ID（meiriwenda）
         * @param bizDate       业务时间戳（直接使用 queryDailyQuiz 返回的 data.bizDate）
         * @param answerId      选中的答案 ID（data.questionVo.rightAnswer.answerId）
         * @param questionId    题目 ID（data.questionVo.questionId）
         * @param answerStatus  答案状态：RIGHT / WRONG
         */
        @JvmStatic
        suspend fun pushDailyTask(
            behaviorId: String?, bizDate: Long,
            answerId: String?, questionId: String?,
            answerStatus: String?
        ): String {
            var answerStatus = answerStatus
            if (answerStatus.isNullOrEmpty()) {
                answerStatus = "RIGHT"
            }
            val sb = StringBuilder()
            sb.append("[{\"behaviorId\":\"")
                .append(behaviorId)
                .append("\",\"bizDate\":")
                .append(bizDate)
                .append(",\"extInfo\":{")
                .append("\"answerId\":\"").append(answerId).append("\",")
                .append("\"answerStatus\":\"").append(answerStatus).append("\",")
                .append("\"questionId\":\"").append(questionId).append("\"")
                .append("}}]")
            val data = sb.toString()
            return RequestManager.requestString(
                "com.antgroup.zmxy.zmcustprod.biz.rpc.growthtask.api.GrowthTaskRpcManager.pushDailyTask",
                data
            )
        }

        /**
         * 提交信用知识视频答题（shipingwenda）
         *
         * 对应抓包：
         *   Method: com.antgroup.zmxy.zmcustprod.biz.rpc.growthtask.api.GrowthTaskRpcManager.pushDailyTask
         *
         * requestData:
         * [{
         *     "behaviorId": "shipingwenda",
         *     "bizDate": 1765254295706,
         *     "extInfo": {
         *         "answerId": "A",
         *         "answerStatus": "RIGHT",
         *         "questionId": "question3"
         *     }
         * }]
         *
         * @param bizDate      业务时间戳
         * @param answerId     选中的答案 ID
         * @param questionId   题目 ID
         * @param answerStatus RIGHT / WRONG，默认 RIGHT
         */
        @JvmStatic
        suspend fun pushVideoQuizTask(
            bizDate: Long,
            answerId: String?,
            questionId: String?,
            answerStatus: String?
        ): String {
            var answerStatus = answerStatus
            if (answerStatus.isNullOrEmpty()) {
                answerStatus = "RIGHT"
            }

            val sb = StringBuilder()
            sb.append("[{")
                .append("\"behaviorId\":\"shipingwenda\",")
                .append("\"bizDate\":").append(bizDate).append(',')
                .append("\"extInfo\":{")
                .append("\"answerId\":\"").append(answerId).append("\",")
                .append("\"answerStatus\":\"").append(answerStatus).append("\",")
                .append("\"questionId\":\"").append(questionId).append("\"")
                .append("}")
                .append("}]")

            val data = sb.toString()
            return RequestManager.requestString(
                "com.antgroup.zmxy.zmcustprod.biz.rpc.growthtask.api.GrowthTaskRpcManager.pushDailyTask",
                data
            )
        }

        /**
         * 查询芝麻分进度
         * 接口: com.antgroup.zmxy.zmcustprod.biz.rpc.home.api.HomeV8RpcManager.queryScoreProgress
         */
        @JvmStatic
        suspend fun queryScoreProgress(): String? {
            try {
                val args = JSONObject()
                args.put("needTotalProcess", "TRUE")
                args.put("queryGuideInfo", true)
                args.put("switchNewPage", true)
                return RequestManager.requestString(
                    "com.antgroup.zmxy.zmcustprod.biz.rpc.home.api.HomeV8RpcManager.queryScoreProgress",
                    JSONArray().put(args).toString()
                )
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }

        /**
         * 领取进度球
         * 接口: com.antgroup.zmxy.zmcustprod.biz.rpc.growthbehavior.api.GrowthBehaviorRpcManager.collectProgressBall
         */
        @JvmStatic
        suspend fun collectProgressBall(ballIdList: JSONArray?): String? {
            try {
                val args = JSONObject()
                args.put("ballIdList", ballIdList) // 直接用 JSONArray
                return RequestManager.requestString(
                    "com.antgroup.zmxy.zmcustprod.biz.rpc.growthbehavior.apiGrowthBehaviorRpcManager.collectProgressBall",
                    JSONArray().put(args).toString()
                )
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }

        object Alchemy {
            /**
             * 芝麻炼金/积分首页
             */
            @JvmStatic
            suspend fun alchemyQueryHome(): String {
                return RequestManager.requestString(
                    "com.antgroup.zmxy.zmmemberop.biz.rpc.AlchemyRpcManager.queryHome",
                    "[{}]"
                )
            }

            /**
             * [日志对应] 芝麻炼金-执行炼金
             * Method: com.antgroup.zmxy.zmmemberop.biz.rpc.AlchemyRpcManager.alchemy
             * Params: [null]
             */
            @JvmStatic
            suspend fun alchemyExecute(): String {
                // 日志中 requestData 为 [null]
                return RequestManager.requestString("com.antgroup.zmxy.zmmemberop.biz.rpc.AlchemyRpcManager.alchemy", "[{}]")
            }

            /**
             * [日志对应] 芝麻炼 /金-签到列表查询
             *
             *
             *      * @param checkInDate yyyyMMdd
             *      * @param sceneCode   "zml" 对应芝麻粒福利签到, "alchemy" 对应芝麻炼金签到
             *      *
             * Method: com.antgroup.zmxy.zmmemberop.biz.rpc.pointtask.CheckInTaskRpcManager.queryTaskLists
             */
            @JvmStatic
            suspend fun alchemyQueryCheckIn(scenecode: String?): String {
                return RequestManager.requestString(
                    "com.antgroup.zmxy.zmmemberop.biz.rpc.pointtask.CheckInTaskRpcManager.queryTaskLists",
                    "[{\"sceneCode\":\"$scenecode\",\"version\":\"$Version\"}]"
                )
            }

            /**
             * [日志对应] 芝麻炼金-时段奖励查询 (午饭/晚饭)
             * Method: com.antgroup.zmxy.zmmemberop.biz.rpc.pointtask.TimeLimitedTaskRpcManager.queryTask
             */
            @JvmStatic
            suspend fun alchemyQueryTimeLimitedTask(): String {
                return RequestManager.requestString(
                    "com.antgroup.zmxy.zmmemberop.biz.rpc.pointtask.TimeLimitedTaskRpcManager.queryTask",
                    "[{}]"
                )
            }

            /**
             * [日志对应] 芝麻炼金-完成时段任务 (午饭/晚饭)
             * Method: com.antgroup.zmxy.zmmemberop.biz.rpc.pointtask.TimeLimitedTaskRpcManager.completeTask
             *
             * 请求示例:
             * {
             *     "templateId": "wujianli"
             * }
             *
             * 返回示例:
             * {
             *     "ariverRpcTraceId": "client`aBYSOR/y0xEDACWu2y9mPoqMPhTTaIz_5694806",
             *     "data": {
             *         "degrade": false,
             *         "toast": "领取成功,得10芝麻粒",
             *         "zmlNum": 10
             *     },
             *     "resultCode": "SUCCESS",
             *     "resultView": "成功",
             *     "success": true,
             *     "traceId": "21d0e34417646521077286391ee43a"
             * }
             */
            @JvmStatic
            suspend fun alchemyCompleteTimeLimitedTask(templateId: String?): String {
                val body = "[{\n" +
                        "    \"templateId\": \"$templateId\"\n" +
                        "}]"
                return RequestManager.requestString(
                    "com.antgroup.zmxy.zmmemberop.biz.rpc.pointtask.TimeLimitedTaskRpcManager.completeTask",
                    body
                )
            }

            /**
             * [日志对应] 芝麻炼金-任务列表 V3 (参数精确匹配日志)
             * Method: com.antgroup.zmxy.zmmemberop.biz.rpc.creditaccumulate.CreditAccumulateStrategyRpcManager.queryListV3
             */
            @JvmStatic
            suspend fun alchemyQueryListV3(): String {
                return RequestManager.requestString(
                    "com.antgroup.zmxy.zmmemberop.biz.rpc.creditaccumulate.CreditAccumulateStrategyRpcManager.queryListV3",
                    "[{\"chInfo\":\"\",\"deliverStatus\":\"\",\"deliveryTemplateId\":\"\",\"searchSubscribeTask\":true,\"version\":\"alchemy\"}]"
                )
            }

            /**
             * [日志对应] 芝麻炼金 - 领取奖励
             *
             * Method: com.antgroup.zmxy.zmmemberop.biz.rpc.AlchemyRpcManager.claimAward
             */
            @JvmStatic
            suspend fun claimAward(): String {
                return RequestManager.requestString(
                    "com.antgroup.zmxy.zmmemberop.biz.rpc.AlchemyRpcManager.claimAward",
                    "[{}]"
                )
            }
        }
    }
}
