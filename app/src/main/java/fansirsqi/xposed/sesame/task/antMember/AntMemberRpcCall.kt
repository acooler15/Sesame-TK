package fansirsqi.xposed.sesame.task.antMember

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.UUID
import fansirsqi.xposed.sesame.hook.RequestManager
import fansirsqi.xposed.sesame.hook.rpc.RpcRequestData
import fansirsqi.xposed.sesame.core.util.RandomUtil
import fansirsqi.xposed.sesame.core.util.TimeUtil

object AntMemberRpcCall {
    private fun getUniqueId(): String {
        return System.currentTimeMillis().toString() + RandomUtil.nextLong()
    }

    /* ant member point */
    @JvmStatic
    suspend fun queryPointCert(page: Int, pageSize: Int): String {
        return RequestManager.requestString(
            "alipay.antmember.biz.rpc.member.h5.queryPointCert",
            RpcRequestData.array {
                put("page", page)
                put("pageSize", pageSize)
            }
        )
    }

    @JvmStatic
    suspend fun receivePointByUser(certId: String?): String {
        return RequestManager.requestString(
            "alipay.antmember.biz.rpc.member.h5.receivePointByUser",
            RpcRequestData.array {
                put("certId", certId?.toBigDecimalOrNull() ?: JSONObject.NULL)
            }
        )
    }

    @JvmStatic
    suspend fun queryMemberSigninCalendar(): String {
        return RequestManager.requestString(
            "com.alipay.amic.biz.rpc.signin.h5.queryMemberSigninCalendar",
            RpcRequestData.array {
                put("autoSignIn", true)
                put("invitorUserId", "")
                put("sceneCode", "QUERY")
            }
        )
    }

    /* 商家开门打卡任务 */
    @JvmStatic
    suspend fun signIn(activityNo: String?): String {
        return RequestManager.requestString(
            "alipay.merchant.kmdk.signIn",
            RpcRequestData.array {
                put("activityNo", activityNo ?: "null")
            }
        )
    }

    @JvmStatic
    suspend fun signUp(activityNo: String?): String {
        return RequestManager.requestString(
            "alipay.merchant.kmdk.signUp",
            RpcRequestData.array {
                put("activityNo", activityNo ?: "null")
            }
        )
    }

    /* 商家服务 */
    @JvmStatic
    suspend fun transcodeCheck(): String {
        return RequestManager.requestString(
            "alipay.mrchservbase.mrchbusiness.sign.transcode.check",
            RpcRequestData.array { }
        )
    }

    @JvmStatic
    suspend fun merchantSign(): String {
        return RequestManager.requestString(
            "alipay.mrchservbase.mrchpoint.sqyj.homepage.signin.v1",
            RpcRequestData.array { }
        )
    }

    @JvmStatic
    suspend fun taskListQuery(): String {
        return RequestManager.requestString(
            "alipay.mrchservbase.task.more.query",
            RpcRequestData.array {
                put("paramMap", JSONObject().apply { put("platform", "Android") })
                put("taskItemCode", "")
            }
        )
    }

    @JvmStatic
    suspend fun queryActivity(): String {
        return RequestManager.requestString(
            "alipay.merchant.kmdk.query.activity",
            RpcRequestData.array {
                put("scene", "activityCenter")
            }
        )
    }

    /* 商家服务任务 */
    @JvmStatic
    suspend fun taskFinish(bizId: String?): String {
        return RequestManager.requestString(
            "com.alipay.adtask.biz.mobilegw.service.task.finish",
            RpcRequestData.array {
                put("bizId", bizId ?: "null")
            }
        )
    }

    @JvmStatic
    suspend fun taskReceive(taskCode: String?): String {
        return RequestManager.requestString(
            "alipay.mrchservbase.sqyj.task.receive",
            RpcRequestData.array {
                put("compId", "ZTS_TASK_RECEIVE")
                put("extInfo", JSONObject().apply { put("taskCode", taskCode ?: "null") })
            }
        )
    }

    @JvmStatic
    suspend fun actioncode(actionCode: String?): String {
        return RequestManager.requestString(
            "alipay.mrchservbase.task.query.by.actioncode",
            RpcRequestData.array {
                put("actionCode", actionCode ?: "null")
            }
        )
    }

    @JvmStatic
    suspend fun produce(actionCode: String?): String {
        return RequestManager.requestString(
            "alipay.mrchservbase.biz.task.action.produce",
            RpcRequestData.array {
                put("actionCode", actionCode ?: "null")
            }
        )
    }

    @JvmStatic
    suspend fun ballReceive(ballIds: String?): String {
        return RequestManager.requestString(
            "alipay.mrchservbase.mrchpoint.ball.receive",
            RpcRequestData.array {
                put("ballIds", JSONArray().put(ballIds ?: "null"))
                put("channel", "MRCH_SELF")
                put("outBizNo", getUniqueId())
            }
        )
    }

    @JvmStatic
    suspend fun executeTask(bizParam: String?, bizSubType: String?, bizType: String?, taskConfigId: Long?): String {
        return RequestManager.requestString(
            "alipay.antmember.biz.rpc.membertask.h5.executeTask",
            RpcRequestData.array {
                put("bizOutNo", TimeUtil.getFormatDate().replace("-", ""))
                put("bizParam", bizParam ?: "null")
                put("bizSubType", bizSubType ?: "null")
                put("bizType", bizType ?: "null")
                put("sourcePassMap", JSONObject().apply {
                    put("innerSource", "")
                    put("source", "myTab")
                    put("unid", "")
                })
                put("syncProcess", true)
                put("taskConfigId", taskConfigId?.toString() ?: "null")
            }
        )
    }

    @JvmStatic
    suspend fun queryAllStatusTaskList(): String {
        return RequestManager.requestString(
            "alipay.antmember.biz.rpc.membertask.h5.queryAllStatusTaskList",
            RpcRequestData.array {
                put("sourceBusiness", "signInAd")
                put("sourcePassMap", JSONObject().apply {
                    put("innerSource", "")
                    put("source", "myTab")
                    put("unid", "")
                })
            }
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
            RpcRequestData.array {
                put("source", "ch_alipaysearch__chsub_normal")
            }
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
            RpcRequestData.array {
                put("sceneId", "GAME_CENTER")
                put("signType", "NORMAL_SIGN")
                put("source", "ch_alipaysearch__chsub_normal")
            }
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
            RpcRequestData.array {
                put("source", "ch_alipaysearch__chsub_normal")
            }
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
            RpcRequestData.array {
                put("source", "ch_alipaysearch__chsub_normal")
            }
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
            RpcRequestData.array { }
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
            RpcRequestData.array {
                put("taskId", taskId ?: "null")
            }
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
            RpcRequestData.array {
                put("source", "ch_alipaysearch__chsub_normal")
                put("taskId", taskId ?: "null")
            }
        )
    }

    /**
     * 芝麻信用首页
     */
    @JvmStatic
    suspend fun queryHome(): String {
        return RequestManager.requestString(
            "com.antgroup.zmxy.zmcustprod.biz.rpc.home.api.HomeV8RpcManager.queryHome",
            RpcRequestData.array {
                put("invokeSource", "zmHome")
                put("miniZmGrayInside", "")
                put("version", "week")
            }
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
            RpcRequestData.array { }
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
            RpcRequestData.array {
                put("checkInDate", checkInDate ?: "null")
                put("sceneCode", sceneCode ?: "null")
            }
        )
    }

    /**
     * 获取芝麻信用任务列表
     */
    @JvmStatic
    suspend fun queryAvailableSesameTask(): String {
        return RequestManager.requestString(
            "com.antgroup.zmxy.zmmemberop.biz.rpc.creditaccumulate.CreditAccumulateStrategyRpcManager.queryListV3",
            RpcRequestData.array { }
        )
    }

    /**
     * 芝麻信用领取任务
     */
    @JvmStatic
    suspend fun joinSesameTask(taskTemplateId: String?): String {
        return RequestManager.requestString(
            "com.antgroup.zmxy.zmmemberop.biz.rpc.promise.PromiseRpcManager.joinActivity",
            RpcRequestData.array {
                put("chInfo", "seasameList")
                put("joinFromOuter", false)
                put("templateId", taskTemplateId ?: "null")
            }
        )
    }

    /**
     * 芝麻信用获取任务回调
     */
    @JvmStatic
    suspend fun feedBackSesameTask(taskTemplateId: String?): String {
        return RequestManager.requestString(
            "com.antgroup.zmxy.zmmemberop.biz.rpc.creditaccumulate.CreditAccumulateStrategyRpcManager.taskFeedback",
            RpcRequestData.array {
                put("actionType", "TO_COMPLETE")
                put("templateId", taskTemplateId ?: "null")
            },
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
            RpcRequestData.array {
                put("recordId", recordId ?: "null")
            }
        )
    }

    /**
     * 查询可收取的芝麻粒
     */
    @JvmStatic
    suspend fun queryCreditFeedback(): String {
        return RequestManager.requestString(
            "com.antgroup.zmxy.zmcustprod.biz.rpc.home.creditaccumulate.api.CreditAccumulateRpcManager.queryCreditFeedback",
            RpcRequestData.array {
                put("queryPotential", false)
                put("size", 20)
                put("status", "UNCLAIMED")
            }
        )
    }

    /**
     * 一键收取芝麻粒
     */
    @JvmStatic
    suspend fun collectAllCreditFeedback(): String {
        return RequestManager.requestString(
            "com.antgroup.zmxy.zmcustprod.biz.rpc.home.creditaccumulate.api.CreditAccumulateRpcManager.collectCreditFeedback",
            RpcRequestData.array {
                put("collectAll", true)
                put("status", "UNCLAIMED")
            }
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
            RpcRequestData.array {
                put("collectAll", false)
                put("creditFeedbackId", creditFeedbackId ?: "null")
                put("status", "UNCLAIMED")
            }
        )
    }

    /**
     * 获取所有可领取的保障金
     */
    @JvmStatic
    suspend fun queryAvailableCollectInsuredGold(): String {
        return RequestManager.requestString(
            "com.alipay.insgiftbff.insgiftMain.queryMultiSceneWaitToGainList",
            RpcRequestData.array {
                put("entrance", "wealth_entry")
                val rights = listOf("UNIVERSAL_ACCIDENT", "UNIVERSAL_HOSPITAL", "UNIVERSAL_OUTPATIENT", "UNIVERSAL_SERIOUSNESS", "UNIVERSAL_WEALTH", "UNIVERSAL_TRANS", "UNIVERSAL_FRAUD_LIABILITY")
                val dto: (String, List<String>) -> JSONObject = { giftProdCode, rightNoList ->
                    JSONObject().apply {
                        put("giftProdCode", giftProdCode)
                        put("rightNoList", JSONArray().apply { rightNoList.forEach { put(it) } })
                    }
                }
                put("eventToWaitParamDTO", dto("GIFT_UNIVERSAL_COVERAGE", rights))
                put("helpChildParamDTO", dto("GIFT_HEALTH_GOLD_CHILD", rights))
                put("priorityChannelParamDTO", dto("GIFT_UNIVERSAL_COVERAGE", rights))
                put("signInParamDTO", dto("GIFT_UNIVERSAL_COVERAGE", rights))
            },
            "insgiftbff", "queryMultiSceneWaitToGainList", "insgiftMain"
        )
    }

    /**
     * 领取保障金
     * 抓包确认 requestData 为数组形态，需用 JSONArray 包裹
     */
    @JvmStatic
    suspend fun collectInsuredGold(goldBallObj: JSONObject): String {
        return RequestManager.requestString(
            "com.alipay.insgiftbff.insgiftMain.gainMyAndFamilySumInsured",
            RpcRequestData.arrayOf(goldBallObj), "insgiftbff", "gainMyAndFamilySumInsured", "insgiftMain"
        )
    }

    // 安心豆
    @JvmStatic
    suspend fun querySignInProcess(appletId: String?, scene: String?): String {
        return RequestManager.requestString(
            "com.alipay.insmarketingbff.bean.querySignInProcess",
            RpcRequestData.array {
                put("appletId", appletId ?: "null")
                put("scene", scene ?: "null")
            }
        )
    }

    @JvmStatic
    suspend fun signInTrigger(appletId: String?, scene: String?): String {
        return RequestManager.requestString(
            "com.alipay.insmarketingbff.bean.signInTrigger",
            RpcRequestData.array {
                put("appletId", appletId ?: "null")
                put("scene", scene ?: "null")
            }
        )
    }

    @JvmStatic
    suspend fun beanExchangeDetail(itemId: String?): String {
        return RequestManager.requestString(
            "com.alipay.insmarketingbff.onestop.planTrigger",
            RpcRequestData.array {
                put("extParams", JSONObject().apply { put("itemId", itemId ?: "null") })
                put("planCode", "bluebean_onestop")
                put("planOperateCode", "exchangeDetail")
            }
        )
    }

    @JvmStatic
    suspend fun beanExchange(itemId: String?, pointAmount: Int): String {
        return RequestManager.requestString(
            "com.alipay.insmarketingbff.onestop.planTrigger",
            RpcRequestData.array {
                put("extParams", JSONObject().apply {
                    put("itemId", itemId ?: "null")
                    put("pointAmount", pointAmount.toString())
                })
                put("planCode", "bluebean_onestop")
                put("planOperateCode", "exchange")
            }
        )
    }

    @JvmStatic
    suspend fun queryUserAccountInfo(pointProdCode: String?): String {
        return RequestManager.requestString(
            "com.alipay.insmarketingbff.point.queryUserAccountInfo",
            RpcRequestData.array {
                put("channel", "HiChat")
                put("pointProdCode", pointProdCode ?: "null")
                put("pointUnitType", "COUNT")
            }
        )
    }

    /**
     * 查询会员信息
     */
    @JvmStatic
    suspend fun queryMemberInfo(): String {
        return RequestManager.requestString(
            "com.alipay.alipaymember.biz.rpc.member.h5.queryMemberInfo",
            RpcRequestData.array {
                put("needExpirePoint", true)
                put("needGrade", true)
                put("needPoint", true)
                put("queryScene", "POINT_EXCHANGE_SCENE")
                put("source", "POINT_EXCHANGE_SCENE")
                put("sourcePassMap", JSONObject().apply {
                    put("innerSource", "")
                    put("source", "")
                    put("unid", "")
                })
            }
        )
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
        return RequestManager.requestString(
            "com.alipay.alipaymember.biz.rpc.config.h5.queryShandieEntityList",
            RpcRequestData.array {
                put("blackIds", JSONArray())
                put("deliveryIdList", JSONArray().apply {
                    put("94000SR2025010611442003")
                    put("94000SR2025010611458003")
                })
                put("filterCityCode", false)
                put("filterPointNoEnough", false)
                put("filterStockNoEnough", false)
                put("pageNum", 1)
                put("pageSize", 18)
                put("point", pointBalance?.toBigDecimalOrNull() ?: JSONObject.NULL)
                put("previewCopyDbId", "")
                put("queryType", "DELIVERY_ID_LIST")
                put("source", "member_day")
                put("sourcePassMap", JSONObject().apply {
                    put("innerSource", "")
                    put("source", "0yuandui")
                    put("unid", "")
                })
                put("topIds", JSONArray())
                put("uniqueId", uniqueId)
            }
        )
    }

    @JvmStatic
    suspend fun queryDeliveryZoneDetail(deliveryIdList: List<String>, pageNum: Int, pageSize: Int): String {
        // 1. 处理 uniqueId 的拼接逻辑
        // 固定前缀：17665547901390and99999999INTELLIGENT_SORT92524974
        val uniqueId = "17665547901390and99999999INTELLIGENT_SORT92524974" + deliveryIdList.joinToString(",")

        // 2. 构造完整的请求 Data
        return RequestManager.requestString(
            "com.alipay.alipaymember.biz.rpc.config.h5.queryDeliveryZoneDetail",
            RpcRequestData.array {
                put("deliveryIdList", JSONArray().apply { deliveryIdList.forEach { put(it) } })
                put("lowerPoint", 0)
                put("pageNum", pageNum)
                put("pageSize", pageSize)
                put("queryNoReserve", true)
                put("resourceCardChannel", "ZERO_EXCHANGE_CHANNEL")
                put("sourcePassMap", JSONObject().apply {
                    put("innerSource", "")
                    put("source", "")
                    put("unid", "")
                })
                put("startPageFirstQuery", false)
                put("topIdList", JSONArray().put("202412231259661040"))
                put("uniqueId", uniqueId)
                put("upperPoint", 99999999)
                put("withPointRange", false)
            }
        )
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
        return RequestManager.requestString(
            "com.alipay.alipaymember.biz.rpc.exchange.h5.exchangeBenefit",
            RpcRequestData.array {
                put("benefitId", benefitId ?: "null")
                put("cityCode", "")
                put("exchangeType", "POINT_PAY")
                put("itemId", itemId ?: "null")
                put("miniAppId", "")
                put("orderSource", "")
                put("requestId", requestId)
                put("requestSourceInfo", requestSourceInfo)
                put("sourcePassMap", JSONObject().apply {
                    put("alipayClientVersion", "10.7.80.8000")
                    put("bid", "")
                    put("feedsIndex", "0")
                    put("innerSource", "a159.b52659")
                    put("isCpc", "")
                    put("mobileOsType", "Android")
                    put("source", "")
                    put("unid", unid)
                    put("uniqueId", uniqueId)
                })
                put("userOutAccount", "")
            }
        )
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
        return RequestManager.requestString(
            "com.antgroup.zmxy.zmmemberop.biz.rpc.award.AwardRpcManager.queryListV2",
            RpcRequestData.array {
                put("currentPage", page)
                put("formDelivery", "false")
                put("pageSize", pageSize)
                put("privilegeSource", "")
                put("privilegeTab", "")
                put("tabList", JSONArray())
            }
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
        return RequestManager.requestString(
            "com.antgroup.zmxy.zmmemberop.biz.rpc.award.AwardRpcManager.obtainAward",
            RpcRequestData.array {
                put("awardTemplateId", templateId ?: "null")
            }
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
                RpcRequestData.arrayOf(body)
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
                RpcRequestData.arrayOf(body)
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
                RpcRequestData.arrayOf(body)
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
                RpcRequestData.arrayOf(body)
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
            return RequestManager.requestString(
                "alipay.promoprod.play.trigger",
                RpcRequestData.array {
                    put("operation", "ZHIMA_TREE_HOME_PAGE")
                    put("playInfo", ZHIMATREE_PLAY_INFO)
                    put("refer", ZHIMATREE_REFER)
                    put("extInfo", JSONObject())
                }
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
            return RequestManager.requestString(
                "alipay.promoprod.play.trigger",
                RpcRequestData.array {
                    put("operation", "ZHIMA_TREE_CLEAN_AND_PUSH")
                    put("playInfo", ZHIMATREE_PLAY_INFO)
                    put("refer", ZHIMATREE_REFER)
                    put("extInfo", JSONObject().apply {
                        put("clickNum", "1")
                        put("treeCode", treeCode)
                    })
                }
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
            return RequestManager.requestString(
                "alipay.promoprod.play.trigger",
                RpcRequestData.array {
                    put("operation", "RENT_GREEN_TASK_LIST_QUERY")
                    put("playInfo", ZHIMATREE_PLAY_INFO)
                    put("refer", ZHIMATREE_REFER)
                    put("extInfo", JSONObject().apply {
                        put("chInfo", "ch_share__chsub_ALPContact")
                        put("batchId", "")
                    })
                }
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
            return RequestManager.requestString(
                "alipay.promoprod.play.trigger",
                RpcRequestData.array {
                    put("operation", "RENT_GREEN_TASK_FINISH")
                    put("playInfo", ZHIMATREE_PLAY_INFO)
                    put("refer", ZHIMATREE_REFER)
                    put("extInfo", JSONObject().apply {
                        put("chInfo", "ch_share__chsub_ALPContact")
                        put("taskId", taskId)
                        put("stageCode", stageCode)
                    })
                }
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
            return RequestManager.requestString(
                "com.alipay.finaggexpbff.needle.welfareCenter.index",
                RpcRequestData.array {
                    put("isResume", true)
                }
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
            return RequestManager.requestString(
                "com.alipay.wealthgoldtwa.needle.taskQueryPush",
                RpcRequestData.array {
                    put("mode", 1) // 固定参数
                    put("taskId", taskId)
                }
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
            return RequestManager.requestString(
                "com.alipay.finaggexpbff.needle.welfareCenter.trigger",
                RpcRequestData.array {
                    put("type", type)
                }
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
            return RequestManager.requestString(
                "com.alipay.wealthgoldtwa.goldbill.v4.task.trigger",
                RpcRequestData.array {
                    put("taskId", taskId)
                }
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
            return RequestManager.requestString(
                "com.alipay.wealthgoldtwa.needle.consume.query",
                RpcRequestData.array {
                    put("tabBubbleDeliverParam", JSONObject())
                    put("tabTypeDeliverParam", JSONObject())
                }
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
            return RequestManager.requestString(
                "com.alipay.wealthgoldtwa.needle.consume.submit",
                RpcRequestData.array {
                    put("exchangeAmount", amount)
                    // 计算金额：100份 = 0.10元。公式：份数 / 1000.0
                    put("exchangeMoney", String.format("%.2f", amount / 1000.0))
                    put("prizeType", "GOLD") // 固定为黄金
                    put("productId", productId)
                    put("bonusAmount", bonusAmount)
                }
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
        return RequestManager.requestString(
            "alipay.memberasset.sticker.queryStickerCanReceive",
            RpcRequestData.array {
                put("isFirstShow", "false")
                put("month", month ?: "null")
                put("scene", "")
                put("year", year ?: "null")
            }
        )
    }

    /**
     * 领取指定的贴纸
     * @param stickerIds 贴纸ID集合
     */
    @JvmStatic
    suspend fun receiveSticker(year: String?, month: String?, stickerIds: List<String>?): String? {
        if (stickerIds.isNullOrEmpty()) return null

        return RequestManager.requestString(
            "alipay.memberasset.sticker.receiveSticker",
            RpcRequestData.array {
                put("month", month ?: "null")
                put("stickerIds", JSONArray().apply { stickerIds.forEach { put(it) } })
                put("year", year ?: "null")
            }
        )
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
            return RequestManager.requestString(
                "com.antgroup.zmxy.zmcustprod.biz.rpc.growthbehavior.apiGrowthBehaviorRpcManager.queryToDoList",
                RpcRequestData.array {
                    put("guideBehaviorId", guideBehaviorId)
                    put("invokeVersion", invokeVersion)
                    put("switchNewPage", true)
                }
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
            return RequestManager.requestString(
                "com.antgroup.zmxy.zmcustprod.biz.rpc.growthbehavior.apiGrowthBehaviorRpcManager.openBehaviorCollect",
                RpcRequestData.array {
                    put("behaviorId", behaviorId ?: "null")
                }
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
            return RequestManager.requestString(
                "com.antgroup.zmxy.zmcustprod.biz.rpc.growthtask.api.GrowthTaskRpcManager.queryDailyQuiz",
                RpcRequestData.array {
                    put("behaviorId", behaviorId ?: "null")
                }
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
            return RequestManager.requestString(
                "com.antgroup.zmxy.zmcustprod.biz.rpc.growthtask.api.GrowthTaskRpcManager.pushDailyTask",
                RpcRequestData.array {
                    put("behaviorId", behaviorId ?: "null")
                    put("bizDate", bizDate)
                    put("extInfo", JSONObject().apply {
                        put("answerId", answerId ?: "null")
                        put("answerStatus", answerStatus)
                        put("questionId", questionId ?: "null")
                    })
                }
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

            return RequestManager.requestString(
                "com.antgroup.zmxy.zmcustprod.biz.rpc.growthtask.api.GrowthTaskRpcManager.pushDailyTask",
                RpcRequestData.array {
                    put("behaviorId", "shipingwenda")
                    put("bizDate", bizDate)
                    put("extInfo", JSONObject().apply {
                        put("answerId", answerId ?: "null")
                        put("answerStatus", answerStatus)
                        put("questionId", questionId ?: "null")
                    })
                }
            )
        }

        /**
         * 查询芝麻分进度
         * 接口: com.antgroup.zmxy.zmcustprod.biz.rpc.home.api.HomeV8RpcManager.queryScoreProgress
         */
        @JvmStatic
        suspend fun queryScoreProgress(): String? {
            try {
                return RequestManager.requestString(
                    "com.antgroup.zmxy.zmcustprod.biz.rpc.home.api.HomeV8RpcManager.queryScoreProgress",
                    RpcRequestData.array {
                        put("needTotalProcess", "TRUE")
                        put("queryGuideInfo", true)
                        put("switchNewPage", true)
                    }
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
                return RequestManager.requestString(
                    "com.antgroup.zmxy.zmcustprod.biz.rpc.growthbehavior.apiGrowthBehaviorRpcManager.collectProgressBall",
                    RpcRequestData.array {
                        put("ballIdList", ballIdList) // 直接用 JSONArray
                    }
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
                    RpcRequestData.array { }
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
                return RequestManager.requestString("com.antgroup.zmxy.zmmemberop.biz.rpc.AlchemyRpcManager.alchemy", RpcRequestData.array { })
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
                    RpcRequestData.array {
                        put("sceneCode", scenecode ?: "null")
                        put("version", Version)
                    }
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
                    RpcRequestData.array { }
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
                return RequestManager.requestString(
                    "com.antgroup.zmxy.zmmemberop.biz.rpc.pointtask.TimeLimitedTaskRpcManager.completeTask",
                    RpcRequestData.array {
                        put("templateId", templateId ?: "null")
                    }
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
                    RpcRequestData.array {
                        put("chInfo", "")
                        put("deliverStatus", "")
                        put("deliveryTemplateId", "")
                        put("searchSubscribeTask", true)
                        put("version", "alchemy")
                    }
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
                    RpcRequestData.array { }
                )
            }
        }
    }
}
