package fansirsqi.xposed.sesame.task.greenFinance

import fansirsqi.xposed.sesame.hook.RequestManager
import fansirsqi.xposed.sesame.hook.rpc.RpcRequestData
import fansirsqi.xposed.sesame.util.maps.UserMap
import org.json.JSONArray
import org.json.JSONObject

/**
 * 绿色经营Rpc请求类
 *
 * @author xiong
 */
object GreenFinanceRpcCall {
    /**
     * 查询任务
     *
     * @param appletId appletId
     * @return 结果
     */
    @JvmStatic
    suspend fun taskQuery(appletId: String?): String {
        return RequestManager.requestString(
            "com.alipay.loanpromoweb.promo.task.taskQuery",
            RpcRequestData.array {
                put("appletId", appletId)
                put("completedBottom", true)
            }
        )
    }

    /**
     * 触发任务
     *
     * @param appletId appletId
     * @param stageCode stageCode
     * @param taskCenId 任务ID
     * @return 结果
     */
    @JvmStatic
    suspend fun taskTrigger(appletId: String?, stageCode: String?, taskCenId: String?): String {
        return RequestManager.requestString(
            "com.alipay.loanpromoweb.promo.task.taskTrigger",
            RpcRequestData.array {
                put("appletId", appletId)
                put("stageCode", stageCode)
                put("taskCenId", taskCenId)
            }
        )
    }

    @JvmStatic
    suspend fun signInTrigger(sceneId: String?): String {
        return RequestManager.requestString(
            "com.alipay.loanpromoweb.promo.signin.trigger",
            RpcRequestData.array {
                put("extInfo", JSONObject())
                put("sceneId", sceneId)
            }
        )
    }

    /**
     * 绿色经营首页
     *
     * @return 结果
     */
    @JvmStatic
    suspend fun greenFinanceIndex(): String {
        return RequestManager.requestString(
            "com.alipay.mcaplatformunit.common.mobile.newservice.GreenFinancePageQueryService.indexV2",
            RpcRequestData.array {
                put("clientVersion", "VERSION2")
                put("custType", "MERCHANT")
            }
        )
    }

    /**
     * 批量收取
     *
     * @param bsnIds bsnIds
     * @return 结果
     */
    @JvmStatic
    suspend fun batchSelfCollect(bsnIds: JSONArray): String {
        return RequestManager.requestString(
            "com.alipay.mcaplatformunit.common.mobile.service.GreenFinancePointCollectService.batchSelfCollect",
            RpcRequestData.array {
                put("bsnIds", bsnIds)
                put("clientVersion", "VERSION2")
                put("custType", "MERCHANT")
                put("uid", UserMap.currentUid)
            }
        )
    }

    /**
     * 签到查询
     *
     * @param sceneId sceneId
     * @return 结果
     */
    @JvmStatic
    suspend fun signInQuery(sceneId: String?): String {
        return RequestManager.requestString(
            "com.alipay.loanpromoweb.promo.signin.query",
            RpcRequestData.array {
                put("cycleCount", 7)
                put("cycleType", "d")
                put("extInfo", JSONObject())
                put("needContinuous", 1)
                put("sceneId", sceneId)
            }
        )
    }

    /**
     * 查询打卡记录
     *
     * @param firstBehaviorType 打卡类型
     * @return 结果
     */
    @JvmStatic
    suspend fun queryUserTickItem(firstBehaviorType: String?): String {
        return RequestManager.requestString(
            "com.alipay.mcaplatformunit.common.mobile.newservice.GreenFinanceTickService.queryUserTickItem",
            RpcRequestData.array {
                put("custType", "MERCHANT")
                put("firstBehaviorType", firstBehaviorType)
                put("uid", UserMap.currentUid)
            }
        )
    }

    /**
     * 提交打卡
     *
     * @param firstBehaviorType 打卡类型
     * @param behaviorCode 记录编码
     * @return 结果
     */
    @JvmStatic
    suspend fun submitTick(firstBehaviorType: String?, behaviorCode: String?): String {
        return RequestManager.requestString(
            "com.alipay.mcaplatformunit.common.mobile.newservice.GreenFinanceTickService.submitTick",
            RpcRequestData.array {
                put("custType", "MERCHANT")
                put("firstBehaviorType", firstBehaviorType)
                put("uid", UserMap.currentUid)
                put("behaviorCode", behaviorCode)
            }
        )
    }

    /**
     * 查询要过期了的金币
     *
     * @param day 多少天后
     * @return 结果
     */
    @JvmStatic
    suspend fun queryExpireMcaPoint(day: Long): String {
        // {"ariverRpcTraceId":"client`ZWBWO+Zb5kQDAHgksDyLs/tHP11O+Xc_283027","result":{"expirePoint":{"amount":"6762.00","amountInt":"6762","cent":"676200"}},"resultView":"处理成功","success":true}
        // 十天后
        return RequestManager.requestString(
            "com.alipay.mcaplatformunit.common.mobile.newservice.GreenFinancePageQueryService.queryExpireMcaPoint",
            RpcRequestData.array {
                put("custType", "MERCHANT")
                put("profitType", "MYBK_LOAN_DISCOUNT")
                put("uid", UserMap.currentUid)
                put("expireDate", "${System.currentTimeMillis() + day * 24 * 60 * 60 * 1000}")
            }
        )
    }

    /**
     * 查询可捐助的项目、
     *
     * @return 结果
     */
    @JvmStatic
    suspend fun queryAllDonationProjectNew(): String {
        return RequestManager.requestString(
            "com.alipay.mcaplatformunit.common.mobile.newservice.GreenFinanceDonationService.queryAllDonationProjectNew",
            RpcRequestData.array {
                put("custType", "MERCHANT")
                put("subjectType", "ALL_DONATION")
                put("uid", UserMap.currentUid)
            }
        )
    }

    /**
     * 捐助
     *
     * @param projectId 项目id
     * @param amount 金额
     * @return 结果
     */
    @JvmStatic
    suspend fun donation(projectId: String?, amount: String?): String {
        // {"ariverRpcTraceId":"client`ZWBWO+Zb5kQDAHgksDyLs/tHP11fNHg_230398","result":{"amount":200,"bsnId":"202406231073250005003700277823650280","certificateId":"MBKO1043330320","custType":"MERCHANT","donateElectricityRatio":2,"donateTime":1719088281085,"gmtCreate":1652176865000,"gmtModify":32487667200000,"outBizNo":"1719088280762","projectId":"CLEAN_ENERGY_00001","projectName":"朝阳县光伏发电项目","showFlag":"Y","targetAmount":1162,"uid":"2088302146583284"},"resultView":"处理成功","success":true}
        return RequestManager.requestString(
            "com.alipay.mcaplatformunit.common.mobile.newservice.GreenFinanceDonationService.donation",
            RpcRequestData.array {
                put("custType", "MERCHANT")
                put("donationGold", amount)
                put("uid", UserMap.currentUid)
                put("outbizNo", "${System.currentTimeMillis()}")
                put("projectId", projectId)
            }
        )
    }

    /**
     * 查询评级任务列表
     *
     * @return 结果
     */
    @JvmStatic
    suspend fun consultProveTaskList(): String {
        return RequestManager.requestString(
            "com.alipay.mcaplatformunit.common.mobile.newservice.GreenFinanceProveTaskService.consultProveTaskList",
            RpcRequestData.array {
                put("custType", "MERCHANT")
                put("uid", UserMap.currentUid)
            }
        )
    }

    /**
     * 查询绿色特权奖品
     *
     * @param campId campId
     * @return 结果
     */
    @JvmStatic
    suspend fun queryPrizes(campId: String?): String {
        return RequestManager.requestString(
            "com.alipay.loanpromoweb.promo.camp.queryPrizes",
            RpcRequestData.array {
                put("campIds", JSONArray().put(campId))
            }
        )
    }

    /**
     * 绿色特权奖品领取
     *
     * @param campId campId
     * @return 结果
     */
    @JvmStatic
    suspend fun campTrigger(campId: String?): String {
        return RequestManager.requestString(
            "com.alipay.loanpromoweb.promo.camp.trigger",
            RpcRequestData.array {
                put("campId", campId)
            }
        )
    }

    /**
     * 绿色评级
     *
     * @param bizType 类型ECO_FRIENDLY_BAG_PROVE、classifyTrashCanProve
     * @param imageUrl 图片路径
     * @return 结果
     */
    @JvmStatic
    suspend fun proveTask(bizType: String?, imageUrl: String?): String {
        return RequestManager.requestString(
            "com.alipay.mcaplatformunit.common.mobile.newservice.GreenFinanceProveTaskService.proveTask",
            RpcRequestData.array {
                put("bizType", bizType)
                put("custType", "MERCHANT")
                put("imageUrl", imageUrl)
                put("uid", UserMap.currentUid)
            }
        )
    }

    /**
     * 绿色评级
     *
     * @param taskId 任务ID
     * @return 结果
     */
    @JvmStatic
    suspend fun queryProveTaskStatus(taskId: String?): String {
        return RequestManager.requestString(
            "com.alipay.mcaplatformunit.common.mobile.newservice.GreenFinanceProveTaskService.queryProveTaskStatus",
            RpcRequestData.array {
                put("taskId", taskId)
                put("custType", "MERCHANT")
                put("uid", UserMap.currentUid)
            }
        )
    }

    /**
     * 查询好友列表
     *
     * @return 结果
     */
    @JvmStatic
    suspend fun queryRankingList(startIndex: Int): String {
        return RequestManager.requestString(
            "com.alipay.mcaplatformunit.common.mobile.service.GreenFinanceUserInteractionQueryService.queryRankingList",
            RpcRequestData.array {
                put("clientVersion", "VERSION2")
                put("custType", "MERCHANT")
                put("includeMe", true)
                put("onlyRealFriend", true)
                put("pageLimit", 10)
                put("rankingScene", "FRIEND")
                put("rankingType", "OVERALL")
                put("startIndex", startIndex)
                put("uid", UserMap.currentUid)
            }
        )
    }

    /**
     * 查询一个可以收金币的好友
     *
     * @return 结果
     */
    @JvmStatic
    suspend fun queryGuestIndexPoints(guestId: String?): String {
        return RequestManager.requestString(
            "com.alipay.mcaplatformunit.common.mobile.service.GreenFinanceUserInteractionQueryService.queryGuestIndexPoints",
            RpcRequestData.array {
                put("clientVersion", "VERSION2")
                put("custType", "MERCHANT")
                put("guestCustType", "MERCHANT")
                put("guestUid", guestId)
                put("uid", UserMap.currentUid)
            }
        )
    }

    @JvmStatic
    suspend fun batchSteal(bsnIds: JSONArray, collectedUid: String?): String {
        return RequestManager.requestString(
            "com.alipay.mcaplatformunit.common.mobile.service.GreenFinancePointCollectService.batchSteal",
            RpcRequestData.array {
                put("bsnIds", bsnIds)
                put("clientVersion", "VERSION2")
                put("collectedCustType", "MERCHANT")
                put("collectedUid", collectedUid)
                put("custType", "MERCHANT")
                put("uid", UserMap.currentUid)
            }
        )
    }
}
