package fansirsqi.xposed.sesame.task.antSports

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import fansirsqi.xposed.sesame.hook.ApplicationHook
import fansirsqi.xposed.sesame.hook.RequestManager
import fansirsqi.xposed.sesame.hook.rpc.RpcRequestData

/**
 * @file AntSportsRpcCall.kt
 * @brief 支付宝蚂蚁运动（AntSports）RPC 接口调用集合
 * 
 * @details
 * 本模块提供了支付宝蚂蚁运动所有功能的 RPC 接口的 Kotlin 封装版本。
 * 包括：
 * - 运动任务查询与完成
 * - 能量球（金币）收集与捐赠
 * - 行走路线管理与进度查询
 * - 运动币兑换与礼品领取
 * - 健康岛（Neverland）任务系统
 * - 抢好友大战功能
 * 
 * @author [Original Java Author]
 * @since 2025.01.20
 * @version 1.0.0
 */
object AntSportsRpcCall {

    //━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 常量定义
    //━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /**
     * @brief 应用渠道信息 - 应用中心子频道9patch
     */
    private const val CH_INFO = "ch_appcenter__chsub_9patch"

    /**
     * @brief 时区信息 - 亚洲/上海
     */
    private const val TIME_ZONE = "Asia/Shanghai"

    /**
     * @brief 版本号 - 蚂蚁运动版本
     */
    private const val VERSION = "3.0.1.2"

    /**
     * @brief 支付宝应用版本 - 动态获取
     */
    private val ALIPAY_APP_VERSION =ApplicationHook.alipayVersion

    /**
     * @brief 城市代码 - 杭州
     */
    private const val CITY_CODE = "330100"

    /**
     * @brief 应用ID - 蚂蚁运动小程序ID
     */
    private const val APP_ID = "2021002116659397"

    /**
     * @brief 功能特性列表 - JSON 格式字符串
     * 
     * 包含运动各项功能的支持标识符，用于API请求。
     */
    private const val FEATURES = """["DAILY_STEPS_RANK_V2","STEP_BATTLE","CLUB_HOME_CARD","NEW_HOME_PAGE_STATIC","CLOUD_SDK_AUTH","STAY_ON_COMPLETE","EXTRA_TREASURE_BOX","NEW_HOME_PAGE_STATIC","SUPPORT_AI","SUPPORT_TAB3","SUPPORT_FLYRABBIT","SUPPORT_NEW_MATCH","EXTERNAL_ADVERTISEMENT_TASK","PROP","PROPV2","ASIAN_GAMES"]"""

    //━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 运动任务面板接口
    //━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /**
     * @brief 查询运动币任务面板
     * 
     * @details 获取首页运动任务列表，包括能量获取任务和活动任务。
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @remark 对应API：com.alipay.sportshealth.biz.rpc.SportsHealthCoinTaskRpc.queryCoinTaskPanel
     */
    suspend fun queryCoinTaskPanel(): String {
        return RequestManager.requestString(
            "com.alipay.sportshealth.biz.rpc.SportsHealthCoinTaskRpc.queryCoinTaskPanel",
            RpcRequestData.array {
                put("apiVersion", "energy")
                put("canAddHome", false)
                put("chInfo", "medical_health")
                put("clientAuthStatus", "not_support")
                put("clientOS", "android")
                put("features", JSONArray(FEATURES))
                put("topTaskId", "")
            }
        )
    }

    /**
     * @brief 运动任务签到
     * 
     * @param taskId 任务ID - 特定任务的唯一标识
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @remark 对应API：com.alipay.sportshealth.biz.rpc.SportsHealthCoinTaskRpc.signUpTask
     */
    suspend fun signUpTask(taskId: String): String {
        return RequestManager.requestString(
            "com.alipay.sportshealth.biz.rpc.SportsHealthCoinTaskRpc.signUpTask",
            RpcRequestData.array {
                put("apiVersion", "energy")
                put("chInfo", "medical_health")
                put("clientOS", "android")
                put("features", JSONArray(FEATURES))
                put("taskCenId", "")
                put("taskId", taskId)
            }
        )
    }

    /**
     * @brief 完成运动锻炼任务（旧版接口）
     * 
     * @param taskId 任务ID
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @deprecated 该接口为旧版，建议使用 #completeTask
     * @remark 对应API：com.alipay.sportshealth.biz.rpc.SportsHealthCoinTaskRpc.completeTask
     */
    suspend fun completeExerciseTasks(taskId: String): String {
        return RequestManager.requestString(
            "com.alipay.sportshealth.biz.rpc.SportsHealthCoinTaskRpc.completeTask",
            RpcRequestData.array {
                put("chInfo", "ch_appcenter__chsub_9patch")
                put("clientOS", "android")
                put("features", JSONArray(FEATURES))
                put("taskAction", "JUMP")
                put("taskId", taskId)
            }
        )
    }

    /**
     * @brief 完成运动任务（新版接口）
     * 
     * @param taskId 任务ID
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @remark 对应API：com.alipay.sportshealth.biz.rpc.SportsHealthCoinTaskRpc.completeTask
     */
    suspend fun completeTask(taskId: String): String {
        return RequestManager.requestString(
            "com.alipay.sportshealth.biz.rpc.SportsHealthCoinTaskRpc.completeTask",
            RpcRequestData.array {
                put("apiVersion", "energy")
                put("chInfo", "medical_health")
                put("clientOS", "android")
                put("features", JSONArray(FEATURES))
                put("taskAction", "JUMP")
                put("taskId", taskId)
            }
        )
    }

    /**
     * @brief 查询运动主页信息
     * 
     * @details 获取运动首页的数据，包括个人信息、排行榜等。
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @remark 对应API：com.alipay.sportshealth.biz.rpc.queryMainPage
     */
    suspend fun queryMainPage(): String {
        return RequestManager.requestString(
            "com.alipay.sportshealth.biz.rpc.queryMainPage",
            RpcRequestData.array {
                put("apiVersion", "energy")
                put("chInfo", "ch_shouquan_shouye")
                put("cityCode", CITY_CODE)
                put("clientOS", "android")
                put("features", JSONArray(FEATURES))
                put("timezone", TIME_ZONE)
            }
        )
    }

    /**
     * @brief 运动健康签到/查询接口
     * 
     * @param operatorType 操作类型
     *   - "signIn" - 执行签到
     *   - "query" - 查询签到状态
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @remark 对应API：com.alipay.sportshealth.biz.rpc.SportsHealthCoinTaskRpc.signInCoinTask
     */
    suspend fun signInCoinTask(operatorType: String): String {
        return RequestManager.requestString(
            "com.alipay.sportshealth.biz.rpc.SportsHealthCoinTaskRpc.signInCoinTask",
            RpcRequestData.array {
                put("apiVersion", "energy")
                put("chInfo", "medical_health")
                put("clientOS", "android")
                put("features", JSONArray(FEATURES))
                put("operatorType", operatorType)
            }
        )
    }

    //━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 能量球模块接口
    //━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /**
     * @brief 查询能量球泡泡模块
     * 
     * @details 获取首页可领取的能量球列表。
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @remark 对应API：com.alipay.sportshealth.biz.rpc.sportsHealthHomeRpc.queryCoinBubbleModule
     */
    suspend fun queryCoinBubbleModule(): String {
        return RequestManager.requestString(
            "com.alipay.sportshealth.biz.rpc.sportsHealthHomeRpc.queryCoinBubbleModule",
            RpcRequestData.array {
                put("bubbleId", "")
                put("canAddHome", false)
                put("chInfo", CH_INFO)
                put("clientAuthStatus", "not_support")
                put("clientOS", "android")
                put("distributionChannel", "")
                put(
                    "features",
                    JSONArray().apply {
                        put("DAILY_STEPS_RANK_V2")
                        put("STEP_BATTLE")
                        put("CLUB_HOME_CARD")
                        put("NEW_HOME_PAGE_STATIC")
                        put("CLOUD_SDK_AUTH")
                        put("STAY_ON_COMPLETE")
                        put("EXTRA_TREASURE_BOX")
                        put("NEW_HOME_PAGE_STATIC")
                        put("SUPPORT_AI")
                        put("SUPPORT_TAB3")
                        put("SUPPORT_FLYRABBIT")
                        put("PROP")
                        put("PROPV2")
                        put("ASIAN_GAMES")
                    }
                )
            }
        )
    }

    /**
     * @brief 领取能量球任务能量（无指定方式，默认不抢好友能量球）
     * 
     * @param medEnergyBallInfoRecordId 能量球记录ID
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @remark 对应API：com.alipay.neverland.biz.rpc.pickBubbleTaskEnergy
     */
    suspend fun pickBubbleTaskEnergy(medEnergyBallInfoRecordId: String): String {
        return pickBubbleTaskEnergy(medEnergyBallInfoRecordId, false)
    }

    /**
     * @brief 领取能量球任务能量（可指定是否抢好友能量球）
     * 
     * @param medEnergyBallInfoRecordId 能量球记录ID
     * @param pickAllEnergyBall 是否领取所有能量球（包括好友的）
     *   - true - 领取所有能量球，包括好友的
     *   - false - 仅领取自己的
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @remark 对应API：com.alipay.neverland.biz.rpc.pickBubbleTaskEnergy
     */
    suspend fun pickBubbleTaskEnergy(medEnergyBallInfoRecordId: String, pickAllEnergyBall: Boolean): String {
        return RequestManager.requestString(
            "com.alipay.neverland.biz.rpc.pickBubbleTaskEnergy",
            RpcRequestData.array {
                put("apiVersion", "energy")
                put("chInfo", "medical_health")
                put(
                    "medEnergyBallInfoRecordIds",
                    JSONArray().apply { put(medEnergyBallInfoRecordId) }
                )
                put("pickAllEnergyBall", pickAllEnergyBall)
                put("source", "SPORT")
            }
        )
    }

    /**
     * @brief 查询能量球模块 - 推荐列表
     * 
     * @details 获取首页能量球推荐列表，包含广告和活动任务。
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @remark 对应API：com.alipay.sportshealth.biz.rpc.sportsHealthHomeRpc.queryEnergyBubbleModule
     */
    suspend fun queryEnergyBubbleModule(): String {
        return RequestManager.requestString(
            "com.alipay.sportshealth.biz.rpc.sportsHealthHomeRpc.queryEnergyBubbleModule",
            RpcRequestData.array {
                put("apiVersion", "energy")
                put("bubbleId", "")
                put("canAddHome", false)
                put("chInfo", "ch_appid-20001003__chsub_pageid-com.alipay.android.phone.businesscommon.globalsearch.ui.MainSearchActivity")
                put("clientAuthStatus", "not_support")
                put("clientOS", "android")
                put("distributionChannel", "")
                put("features", JSONArray(FEATURES))
                put("outBizNo", "")
            }
        )
    }

    /**
     * @brief 拾取能量球（无参数版本）
     * 
     * @details 领取所有待领取的能量球，传递空数组。
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @remark 对应API：com.alipay.neverland.biz.rpc.pickBubbleTaskEnergy
     */
    suspend fun pickBubbleTaskEnergy(): String {
        return RequestManager.requestString(
            "com.alipay.neverland.biz.rpc.pickBubbleTaskEnergy",
            RpcRequestData.array {
                put("apiVersion", "energy")
                put("chInfo", "ch_appid-20001003__chsub_pageid-com.alipay.android.phone.businesscommon.globalsearch.ui.MainSearchActivity")
                put("clientOS", "android")
                put("features", JSONArray(FEATURES))
                put("medEnergyBallInfoRecordIds", JSONArray())
                put("pickAllEnergyBall", true)
                put("source", "SPORT")
            }
        )
    }

    /**
     * @brief 收集金币资产
     * 
     * @param assetId 资产ID - 金币资产唯一标识
     * @param coinAmount 金币数量 - 收集的金币数量
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @remark 对应API：com.alipay.sportshealth.biz.rpc.SportsHealthCoinCenterRpc.receiveCoinAsset
     */
    suspend fun receiveCoinAsset(assetId: String, coinAmount: Int): String {
        return RequestManager.requestString(
            "com.alipay.sportshealth.biz.rpc.SportsHealthCoinCenterRpc.receiveCoinAsset",
            RpcRequestData.array {
                put("assetId", assetId)
                put("chInfo", CH_INFO)
                put("clientOS", "android")
                put("coinAmount", coinAmount)
                put(
                    "features",
                    JSONArray().apply {
                        put("DAILY_STEPS_RANK_V2")
                        put("STEP_BATTLE")
                        put("CLUB_HOME_CARD")
                        put("NEW_HOME_PAGE_STATIC")
                        put("CLOUD_SDK_AUTH")
                        put("STAY_ON_COMPLETE")
                        put("EXTRA_TREASURE_BOX")
                        put("NEW_HOME_PAGE_STATIC")
                        put("SUPPORT_TAB3")
                        put("SUPPORT_FLYRABBIT")
                        put("PROP")
                        put("PROPV2")
                        put("ASIAN_GAMES")
                    }
                )
                put("tracertPos", "首页金币收集")
            }
        )
    }

    //━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 运动币兑换模块
    //━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /**
     * @brief 查询礼品详情
     * 
     * @param itemId 礼品ID - 要查询的礼品唯一标识
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @remark 对应API：com.alipay.sportshealth.biz.rpc.SportsHealthItemCenterRpc.queryItemDetail
     */
    suspend fun queryItemDetail(itemId: String): String {
        return RequestManager.requestString(
            "com.alipay.sportshealth.biz.rpc.SportsHealthItemCenterRpc.queryItemDetail",
            RpcRequestData.array {
                put("itemId", itemId)
            }
        )
    }

    /**
     * @brief 兑换礼品
     * 
     * @param itemId 礼品ID
     * @param coinAmount 消耗运动币数量
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @remark 对应API：com.alipay.sportshealth.biz.rpc.SportsHealthItemCenterRpc.exchangeItem
     */
    suspend fun exchangeItem(itemId: String, coinAmount: Int): String {
        return RequestManager.requestString(
            "com.alipay.sportshealth.biz.rpc.SportsHealthItemCenterRpc.exchangeItem",
            RpcRequestData.array {
                put("coinAmount", coinAmount)
                put("itemId", itemId)
            }
        )
    }

    /**
     * @brief 查询兑换记录详情
     * 
     * @param exchangeRecordId 兑换记录ID
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @remark 对应API：com.alipay.sportshealth.biz.rpc.SportsHealthItemCenterRpc.queryExchangeRecordPage
     */
    suspend fun queryExchangeRecordPage(exchangeRecordId: String): String {
        return RequestManager.requestString(
            "com.alipay.sportshealth.biz.rpc.SportsHealthItemCenterRpc.queryExchangeRecordPage",
            RpcRequestData.array {
                put("exchangeRecordId", exchangeRecordId)
            }
        )
    }

    //━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 旧版行走路线模块（已过时，保留兼容性）
    //━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /**
     * @brief 查询个人首页 - 旧版运动路线
     * 
     * @details 获取用户在旧版蚂蚁行走中的个人主页信息。
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @deprecated 该为旧版接口，新版应使用新API
     * @remark 对应API：alipay.antsports.walk.map.queryMyHomePage
     */
    suspend fun queryMyHomePage(): String {
        return RequestManager.requestString(
            "alipay.antsports.walk.map.queryMyHomePage",
            RpcRequestData.array {
                put("alipayAppVersion", ALIPAY_APP_VERSION)
                put("chInfo", CH_INFO)
                put("clientOS", "android")
                put(
                    "features",
                    JSONArray().apply {
                        put("DAILY_STEPS_RANK_V2")
                        put("STEP_BATTLE")
                        put("CLUB_HOME_CARD")
                        put("NEW_HOME_PAGE_STATIC")
                        put("CLOUD_SDK_AUTH")
                        put("STAY_ON_COMPLETE")
                        put("EXTRA_TREASURE_BOX")
                        put("NEW_HOME_PAGE_STATIC")
                        put("SUPPORT_TAB3")
                        put("SUPPORT_FLYRABBIT")
                        put("PROP")
                        put("PROPV2")
                        put("ASIAN_GAMES")
                    }
                )
                put("pathListUsePage", true)
                put("timeZone", TIME_ZONE)
            }
        )
    }

    /**
     * @brief 加入旧版运动路线
     * 
     * @param pathId 路线ID
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @deprecated 该为旧版接口
     * @remark 对应API：alipay.antsports.walk.map.join
     */
    suspend fun join(pathId: String): String {
        return RequestManager.requestString(
            "alipay.antsports.walk.map.join",
            RpcRequestData.array {
                put("chInfo", CH_INFO)
                put("clientOS", "android")
                put(
                    "features",
                    JSONArray().apply {
                        put("DAILY_STEPS_RANK_V2")
                        put("STEP_BATTLE")
                        put("CLUB_HOME_CARD")
                        put("NEW_HOME_PAGE_STATIC")
                        put("CLOUD_SDK_AUTH")
                        put("STAY_ON_COMPLETE")
                        put("EXTRA_TREASURE_BOX")
                        put("NEW_HOME_PAGE_STATIC")
                        put("SUPPORT_TAB3")
                        put("SUPPORT_FLYRABBIT")
                        put("PROP")
                        put("PROPV2")
                        put("ASIAN_GAMES")
                    }
                )
                put("pathId", pathId)
            }
        )
    }

    /**
     * @brief 首次开启并加入旧版运动路线
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @deprecated 该为旧版接口
     * @remark 对应API：alipay.antsports.walk.user.openAndJoinFirst
     */
    suspend fun openAndJoinFirst(): String {
        return RequestManager.requestString(
            "alipay.antsports.walk.user.openAndJoinFirst",
            RpcRequestData.array {
                put("chInfo", CH_INFO)
                put("clientOS", "android")
                put(
                    "features",
                    JSONArray().apply {
                        put("DAILY_STEPS_RANK_V2")
                        put("STEP_BATTLE")
                        put("CLUB_HOME_CARD")
                        put("NEW_HOME_PAGE_STATIC")
                        put("CLOUD_SDK_AUTH")
                        put("STAY_ON_COMPLETE")
                        put("EXTRA_TREASURE_BOX")
                        put("NEW_HOME_PAGE_STATIC")
                        put("SUPPORT_TAB3")
                        put("SUPPORT_FLYRABBIT")
                        put("PROP")
                        put("PROPV2")
                        put("ASIAN_GAMES")
                    }
                )
            }
        )
    }

    /**
     * @brief 行走旧版运动路线
     * 
     * @param day 日期字符串，格式如 "yyyy-MM-dd"
     * @param rankCacheKey 排行榜缓存键
     * @param stepCount 使用的步数
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @deprecated 该为旧版接口
     * @remark 对应API：alipay.antsports.walk.map.go
     */
    suspend fun go(day: String, rankCacheKey: String, stepCount: Int): String {
        return RequestManager.requestString(
            "alipay.antsports.walk.map.go",
            RpcRequestData.array {
                put("chInfo", CH_INFO)
                put("clientOS", "android")
                put("day", day)
                put(
                    "features",
                    JSONArray().apply {
                        put("DAILY_STEPS_RANK_V2")
                        put("STEP_BATTLE")
                        put("CLUB_HOME_CARD")
                        put("NEW_HOME_PAGE_STATIC")
                        put("CLOUD_SDK_AUTH")
                        put("STAY_ON_COMPLETE")
                        put("EXTRA_TREASURE_BOX")
                        put("NEW_HOME_PAGE_STATIC")
                        put("SUPPORT_TAB3")
                        put("SUPPORT_FLYRABBIT")
                        put("PROP")
                        put("PROPV2")
                        put("ASIAN_GAMES")
                    }
                )
                put("needAllBox", true)
                put("rankCacheKey", rankCacheKey)
                put("timeZone", TIME_ZONE)
                put("useStepCount", stepCount)
            }
        )
    }

    /**
     * @brief 开启旧版运动宝箱
     * 
     * @param boxNo 宝箱编号
     * @param userId 用户ID
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @deprecated 该为旧版接口
     * @remark 对应API：alipay.antsports.walk.treasureBox.openTreasureBox
     */
    suspend fun openTreasureBox(boxNo: String, userId: String): String {
        return RequestManager.requestString(
            "alipay.antsports.walk.treasureBox.openTreasureBox",
            RpcRequestData.array {
                put("boxNo", boxNo)
                put("chInfo", CH_INFO)
                put("clientOS", "android")
                put(
                    "features",
                    JSONArray().apply {
                        put("DAILY_STEPS_RANK_V2")
                        put("STEP_BATTLE")
                        put("CLUB_HOME_CARD")
                        put("NEW_HOME_PAGE_STATIC")
                        put("CLOUD_SDK_AUTH")
                        put("STAY_ON_COMPLETE")
                        put("EXTRA_TREASURE_BOX")
                        put("NEW_HOME_PAGE_STATIC")
                        put("SUPPORT_TAB3")
                        put("SUPPORT_FLYRABBIT")
                        put("PROP")
                        put("PROPV2")
                        put("ASIAN_GAMES")
                    }
                )
                put("userId", userId)
            }
        )
    }

    /**
     * @brief 查询路线基础列表
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @deprecated 该为旧版接口
     * @remark 对应API：alipay.antsports.walk.path.queryBaseList
     */
    suspend fun queryBaseList(): String {
        return RequestManager.requestString(
            "alipay.antsports.walk.path.queryBaseList",
            RpcRequestData.array {
                put("chInfo", CH_INFO)
                put("clientOS", "android")
                put(
                    "features",
                    JSONArray().apply {
                        put("DAILY_STEPS_RANK_V2")
                        put("STEP_BATTLE")
                        put("CLUB_HOME_CARD")
                        put("NEW_HOME_PAGE_STATIC")
                        put("CLOUD_SDK_AUTH")
                        put("STAY_ON_COMPLETE")
                        put("EXTRA_TREASURE_BOX")
                        put("NEW_HOME_PAGE_STATIC")
                        put("SUPPORT_TAB3")
                        put("SUPPORT_FLYRABBIT")
                        put("PROP")
                        put("PROPV2")
                        put("ASIAN_GAMES")
                    }
                )
            }
        )
    }

    /**
     * @brief 查询项目列表
     * 
     * @param index 页码索引，从0开始
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @deprecated 该为旧版接口
     * @remark 对应API：alipay.antsports.walk.charity.queryProjectList
     */
    suspend fun queryProjectList(index: Int): String {
        return RequestManager.requestString(
            "alipay.antsports.walk.charity.queryProjectList",
            RpcRequestData.array {
                put("chInfo", CH_INFO)
                put("clientOS", "android")
                put(
                    "features",
                    JSONArray().apply {
                        put("DAILY_STEPS_RANK_V2")
                        put("STEP_BATTLE")
                        put("CLUB_HOME_CARD")
                        put("NEW_HOME_PAGE_STATIC")
                        put("CLOUD_SDK_AUTH")
                        put("STAY_ON_COMPLETE")
                        put("EXTRA_TREASURE_BOX")
                        put("NEW_HOME_PAGE_STATIC")
                        put("SUPPORT_TAB3")
                        put("SUPPORT_FLYRABBIT")
                        put("PROP")
                        put("PROPV2")
                        put("ASIAN_GAMES")
                    }
                )
                put("index", index)
                put("projectListUseVertical", true)
            }
        )
    }

    /**
     * @brief 捐赠慈善能量
     * 
     * @param donateCharityCoin 捐赠慈善能量数量
     * @param projectId 项目ID
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @deprecated 该为旧版接口
     * @remark 对应API：alipay.antsports.walk.charity.donate
     */
    suspend fun donate(donateCharityCoin: Int, projectId: String): String {
        return RequestManager.requestString(
            "alipay.antsports.walk.charity.donate",
            RpcRequestData.array {
                put("chInfo", CH_INFO)
                put("clientOS", "android")
                put("donateCharityCoin", donateCharityCoin)
                put(
                    "features",
                    JSONArray().apply {
                        put("DAILY_STEPS_RANK_V2")
                        put("STEP_BATTLE")
                        put("CLUB_HOME_CARD")
                        put("NEW_HOME_PAGE_STATIC")
                        put("CLOUD_SDK_AUTH")
                        put("STAY_ON_COMPLETE")
                        put("EXTRA_TREASURE_BOX")
                        put("NEW_HOME_PAGE_STATIC")
                        put("SUPPORT_TAB3")
                        put("SUPPORT_FLYRABBIT")
                        put("PROP")
                        put("PROPV2")
                        put("ASIAN_GAMES")
                    }
                )
                put("projectId", projectId)
            }
        )
    }

    /**
     * @brief 查询行走步数
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @deprecated 该为旧版接口
     * @remark 对应API：alipay.antsports.walk.user.queryWalkStep
     */
    suspend fun queryWalkStep(): String {
        return RequestManager.requestString(
            "alipay.antsports.walk.user.queryWalkStep",
            RpcRequestData.array {
                put("chInfo", CH_INFO)
                put("clientOS", "android")
                put(
                    "features",
                    JSONArray().apply {
                        put("DAILY_STEPS_RANK_V2")
                        put("STEP_BATTLE")
                        put("CLUB_HOME_CARD")
                        put("NEW_HOME_PAGE_STATIC")
                        put("CLOUD_SDK_AUTH")
                        put("STAY_ON_COMPLETE")
                        put("EXTRA_TREASURE_BOX")
                        put("NEW_HOME_PAGE_STATIC")
                        put("SUPPORT_TAB3")
                        put("SUPPORT_FLYRABBIT")
                        put("PROP")
                        put("PROPV2")
                        put("ASIAN_GAMES")
                    }
                )
                put("timeZone", TIME_ZONE)
            }
        )
    }

    /**
     * @brief 行走捐赠签到信息
     * 
     * @param count 步数
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @deprecated 该为旧版接口
     * @remark 对应API：alipay.charity.mobile.donate.walk.walkDonateSignInfo
     */
    suspend fun walkDonateSignInfo(count: Int): String {
        return RequestManager.requestString(
            "alipay.charity.mobile.donate.walk.walkDonateSignInfo",
            RpcRequestData.array {
                put("needDonateAction", false)
                put("source", "walkDonateHome")
                put("steps", count)
                put("timezoneId", TIME_ZONE)
            }
        )
    }

    /**
     * @brief 行走捐赠首页
     * 
     * @param count 步数
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @deprecated 该为旧版接口
     * @remark 对应API：alipay.charity.mobile.donate.walk.home
     */
    suspend fun donateWalkHome(count: Int): String {
        return RequestManager.requestString(
            "alipay.charity.mobile.donate.walk.home",
            RpcRequestData.array {
                put("module", "3")
                put("steps", count)
                put("timezoneId", TIME_ZONE)
            }
        )
    }

    /**
     * @brief 兑换捐赠步数
     * 
     * @param actId 活动ID
     * @param count 步数
     * @param donateToken 捐赠令牌
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @deprecated 该为旧版接口
     * @remark 对应API：alipay.charity.mobile.donate.walk.exchange
     */
    suspend fun exchange(actId: String, count: Int, donateToken: String): String {
        return RequestManager.requestString(
            "alipay.charity.mobile.donate.walk.exchange",
            RpcRequestData.array {
                put("actId", actId)
                put("count", count)
                put("donateToken", donateToken)
                put("timezoneId", TIME_ZONE)
                put("ver", 0)
            }
        )
    }

    //━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 新版行走路线模块（推荐使用）
    //━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /**
     * @brief 查询用户信息 - 新版
     * 
     * @details 获取新版蚂蚁运动中用户的基本信息。
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @remark 对应API：com.alipay.sportsplay.biz.rpc.walk.queryUser
     */

    suspend fun queryUser(): String {
        return RequestManager.requestString(
            "com.alipay.sportsplay.biz.rpc.walk.queryUser",
            RpcRequestData.array {
                put("apiVersion", "energy")
                put("chInfo", "medical_health")
                put("clientOS", "android")
                put("features", JSONArray(FEATURES))
                put("mainPage", true)
                put("timeZone", TIME_ZONE)
            }
        )
    }

    /**
     * @brief 查询主题列表 - 新版
     * 
     * @details 获取可用的运动路线主题列表。
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @remark 对应API：com.alipay.sportsplay.biz.rpc.walk.theme.queryThemeList
     */
    suspend fun queryThemeList(): String {
        return RequestManager.requestString(
            "com.alipay.sportsplay.biz.rpc.walk.theme.queryThemeList",
            RpcRequestData.array {
                put("apiVersion", "energy")
                put("chInfo", "medical_health")
                put("clientOS", "android")
                put("features", JSONArray(FEATURES))
            }
        )
    }

    /**
     * @brief 查询世界地图 - 新版
     * 
     * @param themeId 主题ID
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @remark 对应API：com.alipay.sportsplay.biz.rpc.walk.queryWorldMap
     */
    suspend fun queryWorldMap(themeId: String): String {
        return RequestManager.requestString(
            "com.alipay.sportsplay.biz.rpc.walk.queryWorldMap",
            RpcRequestData.array {
                put("apiVersion", "energy")
                put("chInfo", "medical_health")
                put("clientOS", "android")
                put("features", JSONArray(FEATURES))
                put("themeId", themeId)
            }
        )
    }

    /**
     * @brief 查询城市路线 - 新版
     * 
     * @param cityId 城市ID
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @remark 对应API：com.alipay.sportsplay.biz.rpc.walk.queryCityPath
     */
    suspend fun queryCityPath(cityId: String): String {
        return RequestManager.requestString(
            "com.alipay.sportsplay.biz.rpc.walk.queryCityPath",
            RpcRequestData.array {
                put("apiVersion", "energy")
                put("chInfo", "ch_othertinyapp")
                put("cityId", cityId)
                put("clientOS", "android")
                put("features", JSONArray(FEATURES))
            }
        )
    }

    /**
     * @brief 查询路线详情 - 新版
     * 
     * @param date 日期字符串，格式如 "yyyy-MM-dd"
     * @param pathId 路线ID
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @remark 对应API：com.alipay.sportsplay.biz.rpc.walk.queryPath
     */
    suspend fun queryPath(date: String, pathId: String): String {
        return RequestManager.requestString(
            "com.alipay.sportsplay.biz.rpc.walk.queryPath",
            RpcRequestData.array {
                put("apiVersion", "energy")
                put("chInfo", "medical_health")
                put("clientOS", "android")
                put("date", date)
                put("enableNewVersion", true)
                put("features", JSONArray(FEATURES))
                put("pathId", pathId)
                put("timeZone", TIME_ZONE)
            }
        )
    }

    /**
     * @brief 加入运动路线 - 新版
     * 
     * @param pathId 路线ID
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @remark 对应API：com.alipay.sportsplay.biz.rpc.walk.joinPath
     */
    suspend fun joinPath(pathId: String): String {
        return RequestManager.requestString(
            "com.alipay.sportsplay.biz.rpc.walk.joinPath",
            RpcRequestData.array {
                put("apiVersion", "energy")
                put("chInfo", "ch_othertinyapp")
                put("clientOS", "android")
                put("features", JSONArray(FEATURES))
                put("pathId", pathId)
            }
        )
    }

    /**
     * @brief 行走运动路线 - 新版
     * 
     * @param date 日期字符串，格式如 "yyyy-MM-dd"
     * @param pathId 路线ID
     * @param useStepCount 使用的步数
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @remark 对应API：com.alipay.sportsplay.biz.rpc.walk.go
     */
    suspend fun walkGo(date: String, pathId: String, useStepCount: Int): String {
        return RequestManager.requestString(
            "com.alipay.sportsplay.biz.rpc.walk.go",
            RpcRequestData.array {
                put("apiVersion", "energy")
                put("chInfo", "ch_othertinyapp")
                put("clientOS", "android")
                put("date", date)
                put("features", JSONArray(FEATURES))
                put("pathId", pathId)
                put("source", "ch_othertinyapp")
                put("timeZone", TIME_ZONE)
                put("useStepCount", useStepCount)
            }
        )
    }

    /**
     * @brief 领取路线事件奖励（如宝箱）- 新版
     * 
     * @param eventBillNo 事件账单号（宝箱号）
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @remark 对应API：com.alipay.sportsplay.biz.rpc.walk.receiveEvent
     */
    suspend fun receiveEvent(eventBillNo: String): String {
        return RequestManager.requestString(
            "com.alipay.sportsplay.biz.rpc.walk.receiveEvent",
            RpcRequestData.array {
                put("eventBillNo", eventBillNo)
            }
        )
    }

    /**
     * @brief 查询路线奖励 - 新版
     * 
     * @param appId 应用ID
     * @param pathId 路线ID
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @remark 对应API：com.alipay.sportsplay.biz.rpc.walk.queryPathReward
     */
    suspend fun queryPathReward(appId: String, pathId: String): String {
        return RequestManager.requestString(
            "com.alipay.sportsplay.biz.rpc.walk.queryPathReward",
            RpcRequestData.array {
                put("appId", appId)
                put("pathId", pathId)
                put("source", "ch_appcenter__chsub_9patch")
            }
        )
    }

    /**
     * @brief 兑换成功回调 - 旧版（已过时）
     * 
     * @param exchangeId 兑换ID
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @deprecated 该接口已过时
     * @remark 对应API：alipay.charity.mobile.donate.exchange.success
     */
    suspend fun exchangeSuccess(exchangeId: String): String {
        return RequestManager.requestString(
            "alipay.charity.mobile.donate.exchange.success",
            RpcRequestData.array {
                put("exchangeId", exchangeId)
                put("timezone", "GMT+08:00")
                put("version", VERSION)
            }
        )
    }

    //━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 文体中心模块
    //━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /**
     * @brief 查询用户任务组
     * 
     * @param groupId 任务组ID
     *   - "SPORTS_DAILY_SIGN_GROUP" - 日常签到组
     *   - "SPORTS_DAILY_GROUP" - 日常任务组
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @remark 对应API：alipay.tiyubiz.sports.userTaskGroup.query
     */
    suspend fun userTaskGroupQuery(groupId: String): String {
        return RequestManager.requestString(
            "alipay.tiyubiz.sports.userTaskGroup.query",
            RpcRequestData.array {
                put("cityCode", CITY_CODE)
                put("groupId", groupId)
            }
        )
    }

    /**
     * @brief 完成用户任务
     * 
     * @param bizType 业务类型
     * @param taskId 任务ID
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @remark 对应API：alipay.tiyubiz.sports.userTask.complete
     */
    suspend fun userTaskComplete(bizType: String, taskId: String): String {
        return RequestManager.requestString(
            "alipay.tiyubiz.sports.userTask.complete",
            RpcRequestData.array {
                put("bizType", bizType)
                put("cityCode", CITY_CODE)
                put("completedTime", System.currentTimeMillis())
                put("taskId", taskId)
            }
        )
    }

    /**
     * @brief 领取用户任务权益
     * 
     * @param taskId 任务ID
     * @param userTaskId 用户任务ID
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @remark 对应API：alipay.tiyubiz.sports.userTaskRights.receive
     */
    suspend fun userTaskRightsReceive(taskId: String, userTaskId: String): String {
        return RequestManager.requestString(
            "alipay.tiyubiz.sports.userTaskRights.receive",
            RpcRequestData.array {
                put("taskId", taskId)
                put("userTaskId", userTaskId)
            }
        )
    }

    /**
     * @brief 查询账户信息
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @remark 对应API：alipay.tiyubiz.user.asset.query.account
     */
    suspend fun queryAccount(): String {
        return RequestManager.requestString(
            "alipay.tiyubiz.user.asset.query.account",
            RpcRequestData.array {
                put("accountType", "TIYU_SEED")
            }
        )
    }

    /**
     * @brief 查询赛轮列表
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @remark 对应API：alipay.tiyubiz.wenti.walk.queryRoundList
     */
    suspend fun queryRoundList(): String {
        return RequestManager.requestString(
            "alipay.tiyubiz.wenti.walk.queryRoundList",
            RpcRequestData.array { }
        )
    }

    /**
     * @brief 参与比赛
     * 
     * @param bettingPoints 下注积分
     * @param instanceId 实例ID
     * @param resultId 结果ID
     * @param roundId 轮次ID
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @remark 对应API：alipay.tiyubiz.wenti.walk.participate
     */
    suspend fun participate(bettingPoints: Int, instanceId: String, resultId: String, roundId: String): String {
        return RequestManager.requestString(
            "alipay.tiyubiz.wenti.walk.participate",
            RpcRequestData.array {
                put("bettingPoints", bettingPoints)
                put("guessInstanceId", instanceId)
                put("guessResultId", resultId)
                put("newParticipant", false)
                put("roundId", roundId)
                put("stepTimeZone", TIME_ZONE)
            }
        )
    }

    /**
     * @brief 查询路线功能特性
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @remark 对应API：alipay.tiyubiz.path.feature.query
     */
    suspend fun pathFeatureQuery(): String {
        return RequestManager.requestString(
            "alipay.tiyubiz.path.feature.query",
            RpcRequestData.array {
                put("appId", APP_ID)
                put("features", JSONArray().apply { put("USER_CURRENT_PATH_SIMPLE") })
                put("sceneCode", "wenti_shijiebei")
            }
        )
    }

    /**
     * @brief 加入路线地图
     * 
     * @param pathId 路线ID
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @remark 对应API：alipay.tiyubiz.path.map.join
     */
    suspend fun pathMapJoin(pathId: String): String {
        return RequestManager.requestString(
            "alipay.tiyubiz.path.map.join",
            RpcRequestData.array {
                put("appId", APP_ID)
                put("pathId", pathId)
            }
        )
    }

    /**
     * @brief 查询路线地图首页
     * 
     * @param pathId 路线ID
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @remark 对应API：alipay.tiyubiz.path.map.homepage
     */
    suspend fun pathMapHomepage(pathId: String): String {
        return RequestManager.requestString(
            "alipay.tiyubiz.path.map.homepage",
            RpcRequestData.array {
                put("appId", APP_ID)
                put("pathId", pathId)
            }
        )
    }

    /**
     * @brief 查询步数
     * 
     * @param countDate 统计日期
     * @param pathId 路线ID
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @remark 对应API：alipay.tiyubiz.path.map.step.query
     */
    suspend fun stepQuery(countDate: String, pathId: String): String {
        return RequestManager.requestString(
            "alipay.tiyubiz.path.map.step.query",
            RpcRequestData.array {
                put("appId", APP_ID)
                put("countDate", countDate)
                put("pathId", pathId)
                put("timeZone", TIME_ZONE)
            }
        )
    }

    /**
     * @brief 行走路线（文体中心版本）
     * 
     * @param countDate 统计日期
     * @param goStepCount 行走步数
     * @param pathId 路线ID
     * @param userPathRecordId 用户路线记录ID
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @remark 对应API：alipay.tiyubiz.path.map.go
     */
    suspend fun tiyubizGo(countDate: String, goStepCount: Int, pathId: String, userPathRecordId: String): String {
        return RequestManager.requestString(
            "alipay.tiyubiz.path.map.go",
            RpcRequestData.array {
                put("appId", APP_ID)
                put("countDate", countDate)
                put("goStepCount", goStepCount)
                put("pathId", pathId)
                put("timeZone", TIME_ZONE)
                put("userPathRecordId", userPathRecordId)
            }
        )
    }

    /**
     * @brief 领取路线奖励（文体中心版本）
     * 
     * @param pathId 路线ID
     * @param userPathRewardId 用户路线奖励ID
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @remark 对应API：alipay.tiyubiz.path.map.reward.receive
     */
    suspend fun rewardReceive(pathId: String, userPathRewardId: String): String {
        return RequestManager.requestString(
            "alipay.tiyubiz.path.map.reward.receive",
            RpcRequestData.array {
                put("appId", APP_ID)
                put("pathId", pathId)
                put("userPathRewardId", userPathRewardId)
            }
        )
    }

    //━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 抢好友大战模块
    //━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /**
     * @brief 查询抢好友俱乐部首页
     * 
     * @details 获取抢好友大战中的俱乐部首页数据。
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @remark 对应API：alipay.antsports.club.home.queryClubHome
     */
    suspend fun queryClubHome(): String {
        return RequestManager.requestString(
            "alipay.antsports.club.home.queryClubHome",
            RpcRequestData.array {
                put("apiVersion", "energy")
                put("chInfo", "healthstep")
                put("timeZone", TIME_ZONE)
            }
        )
    }

    /**
     * @brief 查询训练项目
     * 
     * @details 获取可用的训练项目列表。
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @remark 对应API：alipay.antsports.club.train.queryTrainItem
     */
    suspend fun queryTrainItem(): String {
        return RequestManager.requestString(
            "alipay.antsports.club.train.queryTrainItem",
            RpcRequestData.array {
                put("apiVersion", "energy")
                put("chInfo", "healthstep")
            }
        )
    }

    /**
     * @brief 训练好友
     * 
     * @param bizId 业务ID
     * @param itemType 训练项目类型（如 "skate"）
     * @param memberId 成员ID
     * @param originBossId 原老板ID
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @remark 对应API：alipay.antsports.club.train.trainMember
     */
    suspend fun trainMember(bizId: String, itemType: String, memberId: String, originBossId: String): String {
        return RequestManager.requestString(
            "alipay.antsports.club.train.trainMember",
            RpcRequestData.array {
                put("apiVersion", "energy")
                put("bizId", bizId)
                put("chInfo", "healthstep")
                put("itemType", itemType)
                put("memberId", memberId)
                put("originBossId", originBossId)
            }
        )
    }

    /**
     * @brief 查询成员价格排行
     * 
     * @param coinBalance 当前能量余额
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @remark 对应API：alipay.antsports.club.ranking.queryMemberPriceRanking
     */
    suspend fun queryMemberPriceRanking(coinBalance: Int): String {
        return RequestManager.requestString(
            "alipay.antsports.club.ranking.queryMemberPriceRanking",
            RpcRequestData.array {
                put("apiVersion", "energy")
                put("buyMember", true)
                put("chInfo", "healthstep")
                put("coinBalance", coinBalance)
            }
        )
    }

    /**
     * @brief 查询俱乐部成员详情
     * 
     * @param memberId 成员ID
     * @param originBossId 原老板ID
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @remark 对应API：alipay.antsports.club.trade.queryClubMember
     */
    suspend fun queryClubMember(memberId: String, originBossId: String): String {
        return RequestManager.requestString(
            "alipay.antsports.club.trade.queryClubMember",
            RpcRequestData.array {
                put("apiVersion", "energy")
                put("chInfo", "healthstep")
                put("memberId", memberId)
                put("originBossId", originBossId)
            }
        )
    }

    /**
     * @brief 抢购好友
     * 
     * @param currentBossId 当前老板ID
     * @param memberId 成员ID
     * @param originBossId 原老板ID
     * @param priceInfo 价格信息（JSON字符串）
     * @param roomId 房间ID
     * 
     * @return RPC调用结果的 JSON 字符串
     * 
     * @remark 对应API：alipay.antsports.club.trade.buyMember
     */
    suspend fun buyMember(currentBossId: String, memberId: String, originBossId: String, priceInfo: String, roomId: String): String {
        return RequestManager.requestString(
            "alipay.antsports.club.trade.buyMember",
            RpcRequestData.array {
                put("apiVersion", "energy")
                put("chInfo", "healthstep")
                put("currentBossId", currentBossId)
                put("memberId", memberId)
                put("originBossId", originBossId)
                put("priceInfo", JSONObject(priceInfo))
                put("roomId", roomId)
            }
        )
    }

    //━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 健康岛（Neverland）内部类
    //━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    /**
     * @brief 健康岛（Neverland）RPC 接口集合
     * 
     * @details 专门用于健康岛任务系统的 RPC 接口组合。
     * 包括签到、任务、泡泡、建造、走路等功能。
     */
    object NeverlandRpcCall {

        /**
         * @brief 查询签到状态
         * 
         * @param signType 签到类型（健康岛固定为 3）
         * @param source 来源标识（固定为 "jkdsportcard"）
         * 
         * @return RPC调用结果的 JSON 字符串
         * 
         * @remark 对应API：com.alipay.neverland.biz.rpc.querySign
         */
        suspend fun querySign(signType: Int, source: String): String {
            return RequestManager.requestString(
                "com.alipay.neverland.biz.rpc.querySign",
                RpcRequestData.array {
                    put("signType", signType)
                    put("source", source)
                }
            )
        }

        /**
         * @brief 执行签到
         * 
         * @param signType 签到类型（健康岛固定为 3）
         * @param source 来源标识（固定为 "jkdsportcard"）
         * 
         * @return RPC调用结果的 JSON 字符串
         * 
         * @remark 对应API：com.alipay.neverland.biz.rpc.takeSign
         */
        suspend fun takeSign(signType: Int, source: String): String {
            return RequestManager.requestString(
                "com.alipay.neverland.biz.rpc.takeSign",
                RpcRequestData.array {
                    put("signType", signType)
                    put("source", source)
                }
            )
        }

        /**
         * @brief 查询泡泡任务列表
         * 
         * @return RPC调用结果的 JSON 字符串
         * 
         * @remark 对应API：com.alipay.neverland.biz.rpc.queryBubbleTask
         */
        suspend fun queryBubbleTask(): String {
            return RequestManager.requestString(
                "com.alipay.neverland.biz.rpc.queryBubbleTask",
                RpcRequestData.array {
                    put("source", "jkdsportcard")
                    put("sportsAuthed", true)
                }
            )
        }

        /**
         * @brief 领取泡泡能量
         *
         * @param ids 泡泡记录ID列表
         *
         * @return RPC调用结果的 JSON 字符串
         *
         * @remark 对应API：com.alipay.neverland.biz.rpc.pickBubbleTaskEnergy
         */
        suspend fun pickBubbleTaskEnergy(ids: List<String>): String {
            return RequestManager.requestString(
                "com.alipay.neverland.biz.rpc.pickBubbleTaskEnergy",
                RpcRequestData.array {
                    put("medEnergyBallInfoRecordIds", JSONArray().apply {
                        ids.forEach { put(it) }
                    })
                    put("pickAllEnergyBall", true)
                    put("source", "jkdsportcard")
                }
            )
        }

        /**
         * @brief 查询任务中心
         * 
         * @details 获取健康岛任务大厅的任务列表。
         * 
         * @return RPC调用结果的 JSON 字符串
         * 
         * @remark 对应API：com.alipay.neverland.biz.rpc.queryTaskCenter
         */
        suspend fun queryTaskCenter(): String {
            return RequestManager.requestString(
                "com.alipay.neverland.biz.rpc.queryTaskCenter",
                RpcRequestData.array {
                    put("apDid", "6b30jO17Z6Wbr2ggRytFxB09hZdhixfSekjytgi9Ytc=")
                    put("cityCode", "")
                    put("deviceLevel", "high")
                    put("newGame", 0)
                    put("source", "jkdsportcard")
                }
            )
        }

        /**
         * @brief 查询指定类型的任务信息
         * 
         * @param source 来源标识（如 "health-island"）
         * @param type 任务类型（如 "LIGHT_FEEDS_TASK"）
         * 
         * @return RPC调用结果的 JSON 字符串
         * 
         * @remark 对应API：com.alipay.neverland.biz.rpc.queryTaskInfo
         */
        suspend fun queryTaskInfo(source: String, type: String): String {
            return RequestManager.requestString(
                "com.alipay.neverland.biz.rpc.queryTaskInfo",
                RpcRequestData.array {
                    put("source", source)
                    put("type", type)
                }
            )
        }

        /**
         * @brief 领取能量任务奖励
         *
         * @param encryptValue 任务加密值
         * @param energyNum 能量数量
         * @param type 任务类型
         * @param lightTaskId 轻任务ID（可选）
         *
         * @return RPC调用结果的 JSON 字符串
         *
         * @remark 对应API：com.alipay.neverland.biz.rpc.energyReceive
         */
        suspend fun energyReceive(encryptValue: String, energyNum: Int, type: String, lightTaskId: String?): String {
            return RequestManager.requestString(
                "com.alipay.neverland.biz.rpc.energyReceive",
                RpcRequestData.array {
                    put("encryptValue", encryptValue)
                    put("energyNum", energyNum)
                    put("source", "jkdsportcard")
                    put("type", type)
                    if (!lightTaskId.isNullOrEmpty()) {
                        put("lightTaskId", lightTaskId)
                    }
                }
            )
        }

        /**
         * @brief 提交任务
         * 
         * @param taskObj 任务对象（JSONObject）
         * 
         * @return RPC调用结果的 JSON 字符串
         * 
         * @remark 对应API：com.alipay.neverland.biz.rpc.taskSend
         */
        suspend fun taskSend(taskObj: JSONObject): String {
            return RequestManager.requestString(
                "com.alipay.neverland.biz.rpc.taskSend",
                RpcRequestData.arrayOf(taskObj)
            )
        }

        /**
         * @brief 领取任务
         * 
         * @param taskObj 任务对象（JSONObject）
         * 
         * @return RPC调用结果的 JSON 字符串
         * 
         * @remark 对应API：com.alipay.neverland.biz.rpc.taskReceive
         */
        suspend fun taskReceive(taskObj: JSONObject): String {
            return RequestManager.requestString(
                "com.alipay.neverland.biz.rpc.taskReceive",
                RpcRequestData.arrayOf(taskObj)
            )
        }

        /**
         * @brief 完成广告任务
         * 
         * @param bizId 业务ID
         * 
         * @return RPC调用结果的 JSON 字符串
         * 
         * @remark 对应API：com.alipay.adtask.biz.mobilegw.service.task.finish
         */
        suspend fun finish(bizId: String): String {
            return RequestManager.requestString(
                "com.alipay.adtask.biz.mobilegw.service.task.finish",
                RpcRequestData.array {
                    put("bizId", bizId)
                }
            )
        }

        /**
         * @brief 查询地图列表
         * 
         * @return RPC调用结果的 JSON 字符串
         * 
         * @remark 对应API：com.alipay.neverland.biz.rpc.queryMapList
         */
        suspend fun queryMapList(): String {
            return RequestManager.requestString(
                "com.alipay.neverland.biz.rpc.queryMapList",
                RpcRequestData.array {
                    put("source", "jkdsportcard")
                }
            )
        }

        /**
         * @brief 查询地图信息（旧版）
         * 
         * @param mapId 地图ID
         * @param branchId 分支ID
         * 
         * @return RPC调用结果的 JSON 字符串
         * 
         * @throws JSONException JSON解析异常
         * @remark 对应API：com.alipay.neverland.biz.rpc.queryMapInfo
         */
        @Throws(JSONException::class)
        suspend fun queryMapInfo(mapId: String, branchId: String): String {
            return RequestManager.requestString(
                "com.alipay.neverland.biz.rpc.queryMapInfo",
                RpcRequestData.array {
                    put("branchId", branchId)
                    put("drilling", false)
                    put("mapId", mapId)
                    put("source", "jkdsportcard")
                }
            )
        }

        /**
         * @brief 查询地图信息（新版）
         * 
         * @param mapId 地图ID
         * 
         * @return RPC调用结果的 JSON 字符串
         * 
         * @remark 对应API：com.alipay.neverland.biz.rpc.queryMapInfoNew
         */
        suspend fun queryMapInfoNew(mapId: String): String {
            return RequestManager.requestString(
                "com.alipay.neverland.biz.rpc.queryMapInfoNew",
                RpcRequestData.array {
                    put("mapId", mapId)
                    put("source", "jkdsportcard")
                }
            )
        }

        /**
         * @brief 查询基础信息
         * 
         * @return RPC调用结果的 JSON 字符串
         * 
         * @remark 对应API：com.alipay.neverland.biz.rpc.queryBaseinfo
         */
        suspend fun queryBaseinfo(): String {
            return RequestManager.requestString(
                "com.alipay.neverland.biz.rpc.queryBaseinfo",
                RpcRequestData.array {
                    put("source", "jkdsportcard")
                }
            )
        }

        /**
         * @brief 建造建筑
         * 
         * @param branchId 分支ID
         * @param mapId 地图ID
         * @param multiNum 建造倍数（1-10）
         * 
         * @return RPC调用结果的 JSON 字符串
         * 
         * @remark 对应API：com.alipay.neverland.biz.rpc.build
         */
        suspend fun build(branchId: String, mapId: String, multiNum: Int): String {
            return RequestManager.requestString(
                "com.alipay.neverland.biz.rpc.build",
                RpcRequestData.array {
                    put("branchId", branchId)
                    put("mapId", mapId)
                    put("multiNum", multiNum)
                    put("source", "jkdsportcard")
                }
            )
        }

        /**
         * @brief 查询地图详情
         * 
         * @param mapId 地图ID
         * 
         * @return RPC调用结果的 JSON 字符串
         * 
         * @remark 对应API：com.alipay.neverland.biz.rpc.queryMapDetail
         */
        suspend fun queryMapDetail(mapId: String): String {
            return RequestManager.requestString(
                "com.alipay.neverland.biz.rpc.queryMapDetail",
                RpcRequestData.array {
                    put("mapId", mapId)
                    put("source", "jkdsportcard")
                }
            )
        }

        /**
         * @brief 领取地图关卡奖励
         * 
         * @param branchId 分支ID
         * @param level 关卡等级
         * @param mapId 地图ID
         * 
         * @return RPC调用结果的 JSON 字符串
         * 
         * @remark 对应API：com.alipay.neverland.biz.rpc.mapStageReward
         */
        suspend fun mapStageReward(branchId: String, level: Int, mapId: String): String {
            return RequestManager.requestString(
                "com.alipay.neverland.biz.rpc.mapStageReward",
                RpcRequestData.array {
                    put("branchId", branchId)
                    put("level", level)
                    put("mapId", mapId)
                    put("source", "jkdsportcard")
                }
            )
        }

        /**
         * @brief 选择奖励
         * 
         * @param branchId 分支ID
         * @param mapId 地图ID
         * @param rewardId 奖励ID
         * 
         * @return RPC调用结果的 JSON 字符串
         * 
         * @remark 对应API：com.alipay.neverland.biz.rpc.mapChooseReward
         */
        suspend fun chooseReward(branchId: String, mapId: String, rewardId: String): String {
            return RequestManager.requestString(
                "com.alipay.neverland.biz.rpc.mapChooseReward",
                RpcRequestData.array {
                    put("branchId", branchId)
                    put("channel", "jkdsportcard")
                    put("mapId", mapId)
                    put("rewardId", rewardId)
                    put("source", "jkdsportcard")
                }
            )
        }

        /**
         * @brief 选择地图
         * 
         * @param branchId 分支ID
         * @param mapId 地图ID
         * 
         * @return RPC调用结果的 JSON 字符串
         * 
         * @remark 对应API：com.alipay.neverland.biz.rpc.mapChooseFree
         */
        suspend fun chooseMap(branchId: String, mapId: String): String {
            return RequestManager.requestString(
                "com.alipay.neverland.biz.rpc.mapChooseFree",
                RpcRequestData.array {
                    put("branchId", branchId)
                    put("mapId", mapId)
                    put("source", "jkdsportcard")
                }
            )
        }

        /**
         * @brief 行走地格
         * 
         * @param branchId 分支ID
         * @param mapId 地图ID
         * @param drilling 是否钻探（通常为 false）
         * 
         * @return RPC调用结果的 JSON 字符串
         * 
         * @remark 对应API：com.alipay.neverland.biz.rpc.walkGrid
         */
        suspend fun walkGrid(branchId: String, mapId: String, drilling: Boolean): String {
            return RequestManager.requestString(
                "com.alipay.neverland.biz.rpc.walkGrid",
                RpcRequestData.array {
                    put("branchId", branchId)
                    put("mapId", mapId)
                    put("drilling", drilling)
                    put("source", "jkdsportcard")
                }
            )
        }

        /**
         * @brief 查询用户能量
         * 
         * @return RPC调用结果的 JSON 字符串
         * 
         * @remark 对应API：com.alipay.neverland.biz.rpc.queryUserAccount
         */
        suspend fun queryUserEnergy(): String {
            return RequestManager.requestString(
                "com.alipay.neverland.biz.rpc.queryUserAccount",
                RpcRequestData.array {
                    put("source", "jkdsportcard")
                }
            )
        }
    }
}
