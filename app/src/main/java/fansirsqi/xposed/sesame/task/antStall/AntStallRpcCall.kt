package fansirsqi.xposed.sesame.task.antStall

import fansirsqi.xposed.sesame.hook.RequestManager
import fansirsqi.xposed.sesame.hook.rpc.RpcRequestData
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * @class AntStallRpcCall
 * @brief 蚂蚁小铺 (Ant Stall) RPC 调用类
 * @details 处理蚂蚁小铺相关的网络请求，包括店铺管理、任务、好友互动等
 * @author
 * @since 2023/08/22
 */
object AntStallRpcCall {

    /** 接口版本号 */
    private const val VERSION = "0.1.2601161444.47"

    /**
     * @brief 获取个人主页数据
     * @return 响应字符串
     */
    suspend fun home(): String {
        return RequestManager.requestString(
            "com.alipay.antstall.self.home",
            RpcRequestData.array {
                put("arouseAppParams", JSONObject())
                put("source", "search")
                put("systemType", "android")
                put("version", VERSION)
            }
        )
    }

    /**
     * @brief 结算收益
     * @param assetId 资产ID
     * @param settleCoin 结算金币数量
     * @return 响应字符串
     */
    suspend fun settle(assetId: String, settleCoin: Int): String {
        return RequestManager.requestString(
            "com.alipay.antstall.self.settle",
            RpcRequestData.array {
                put("assetId", assetId)
                put("coinType", "MASTER")
                put("settleCoin", settleCoin)
                put("source", "search")
                put("systemType", "android")
                put("version", VERSION)
            }
        )
    }

    /**
     * @brief 获取商店列表
     * @return 响应字符串
     */
    suspend fun shopList(): String {
        return RequestManager.requestString(
            "com.alipay.antstall.shop.list",
            RpcRequestData.array {
                put("freeTop", false)
                put("source", "search")
                put("systemType", "android")
                put("version", VERSION)
            }
        )
    }

    /**
     * @brief 一键收摊前的预检查
     * @return 响应字符串
     */
    suspend fun preOneKeyClose(): String {
        return RequestManager.requestString(
            "com.alipay.antstall.user.shop.close.preOneKey",
            RpcRequestData.array {
                put("source", "search")
                put("systemType", "android")
                put("version", VERSION)
            }
        )
    }

    /**
     * @brief 一键收摊
     * @return 响应字符串
     */
    suspend fun oneKeyClose(): String {
        return RequestManager.requestString(
            "com.alipay.antstall.user.shop.oneKeyClose",
            RpcRequestData.array {
                put("source", "search")
                put("systemType", "android")
                put("version", VERSION)
            }
        )
    }

    /**
     * @brief 收摊前的预检查
     * @param shopId 商店ID
     * @param billNo 账单编号
     * @return 响应字符串
     */
    suspend fun preShopClose(shopId: String, billNo: String): String {
        return RequestManager.requestString(
            "com.alipay.antstall.user.shop.close.pre",
            RpcRequestData.array {
                put("billNo", billNo)
                put("shopId", shopId)
                put("source", "search")
                put("systemType", "android")
                put("version", VERSION)
            }
        )
    }

    /**
     * @brief 收摊
     * @param shopId 商店ID
     * @return 响应字符串
     */
    suspend fun shopClose(shopId: String): String {
        return RequestManager.requestString(
            "com.alipay.antstall.user.shop.close",
            RpcRequestData.array {
                put("shopId", shopId)
                put("source", "search")
                put("systemType", "android")
                put("version", VERSION)
            }
        )
    }

    /**
     * @brief 一键开店
     * @return 响应字符串
     */
    suspend fun oneKeyOpen(): String {
        return RequestManager.requestString(
            "com.alipay.antstall.user.shop.oneKeyOpen",
            RpcRequestData.array {
                put("source", "search")
                put("systemType", "android")
                put("version", VERSION)
            }
        )
    }

    /**
     * @brief 在好友位开店
     * @param friendSeatId 好友位置ID
     * @param friendUserId 好友用户ID
     * @param shopId 商店ID
     * @return 响应字符串
     */
    suspend fun shopOpen(friendSeatId: String, friendUserId: String, shopId: String): String {
        return RequestManager.requestString(
            "com.alipay.antstall.user.shop.open",
            RpcRequestData.array {
                put("friendSeatId", friendSeatId)
                put("friendUserId", friendUserId)
                put("shopId", shopId)
                put("source", "search")
                put("systemType", "android")
                put("version", VERSION)
            }
        )
    }

    /**
     * @brief 捐赠排名金币
     * @return 响应字符串
     */
    suspend fun rankCoinDonate(): String {
        return RequestManager.requestString(
            "com.alipay.antstall.rank.coin.donate",
            RpcRequestData.array {
                put("source", "ANTFARM")
                put("systemType", "android")
                put("version", VERSION)
            }
        )
    }

    /**
     * @brief 进入好友的小铺首页
     * @param userId 好友用户ID
     * @return 响应字符串
     */
    suspend fun friendHome(userId: String): String {
        return RequestManager.requestString(
            "com.alipay.antstall.friend.home",
            RpcRequestData.array {
                put("arouseAppParams", JSONObject())
                put("friendUserId", userId)
                put("source", "search")
                put("systemType", "android")
                put("version", VERSION)
            }
        )
    }

    /**
     * @brief 获取任务列表
     * @return 响应字符串
     */
    suspend fun taskList(): String {
        return RequestManager.requestString(
            "com.alipay.antstall.task.list",
            RpcRequestData.array {
                put("source", "search")
                put("systemType", "android")
                put("version", VERSION)
            }
        )
    }

    /**
     * @brief 今日签到
     * @return 响应字符串
     */
    suspend fun signToday(): String {
        return RequestManager.requestString(
            "com.alipay.antstall.sign.today",
            RpcRequestData.array {
                put("source", "search")
                put("systemType", "android")
                put("version", VERSION)
            }
        )
    }

    /**
     * @brief 完成通用任务
     * @param outBizNo 外部业务编号
     * @param taskType 任务类型
     * @return 响应字符串
     */
    suspend fun finishTask(outBizNo: String, taskType: String): String {
        return RequestManager.requestString(
            "com.alipay.antiep.finishTask",
            RpcRequestData.array {
                put("outBizNo", outBizNo)
                put("requestType", "RPC")
                put("sceneCode", "ANTSTALL_TASK")
                put("source", "AST")
                put("systemType", "android")
                put("taskType", taskType)
                put("version", VERSION)
            }
        )
    }

    /**
     * @brief 调用广告/插件接口
     * @return 响应字符串
     */
    suspend fun xlightPlugin(): String {
        return RequestManager.requestString(
            "com.alipay.adexchange.ad.facade.xlightPlugin",
            RpcRequestData.array {
                put(
                    "positionRequest", JSONObject().apply {
                        put(
                            "extMap", JSONObject().apply {
                                put("xlightPlayInstanceId", "300004")
                            }
                        )
                        put("referInfo", JSONObject())
                        put("spaceCode", "ANT_FARM_NEW_VILLAGE")
                    }
                )
                put(
                    "sdkPageInfo", JSONObject().apply {
                        put("adComponentType", "FEEDS")
                        put("adComponentVersion", "4.11.13")
                        put("enableFusion", true)
                        put("networkType", "WIFI")
                        put("pageFrom", "ch_url-https://68687809.h5app.alipay.com/www/game.html")
                        put("pageNo", 1)
                        put(
                            "pageUrl",
                            "https://render.alipay.com/p/yuyan/180020010001256918/multi-stage-task.html?caprMode=sync&spaceCodeFeeds=ANT_FARM_NEW_VILLAGE&usePlayLink=true&xlightPlayInstanceId=300004"
                        )
                        put("session", "u_54b721d9fffd6_1904b8eba8f")
                        put("unionAppId", "2060090000304921")
                        put("usePlayLink", "true")
                        put("xlightSDKType", "h5")
                        put("xlightSDKVersion", "4.11.13")
                    }
                )
            }
        )
    }

    /**
     * @brief 结束特定业务
     * @param playBizId 播放业务ID
     * @param jsonObject 事件信息
     * @return 响应字符串
     */
    suspend fun finish(playBizId: String, jsonObject: JSONObject): String {
        return RequestManager.requestString(
            "com.alipay.adtask.biz.mobilegw.service.interaction.finish",
            RpcRequestData.array {
                put(
                    "extendInfo", JSONObject().apply {
                        put("iepTaskSceneCode", "ANTSTALL_TASK")
                        put("iepTaskType", "ANTSTALL_XLIGHT_VARIABLE_AWARD")
                    }
                )
                put("playBizId", playBizId)
                put("playEventInfo", jsonObject)
                put("source", "adx")
            }
        )
    }

    /**
     * @brief 查询应用跳转 Schema
     * @param sceneCode 场景代码
     * @return 响应字符串
     */
    suspend fun queryCallAppSchema(sceneCode: String): String {
        return RequestManager.requestString(
            "alipay.antmember.callApp.queryCallAppSchema",
            RpcRequestData.array {
                put("sceneCode", sceneCode)
            }
        )
    }

    /**
     * @brief 领取任务奖励 (IEP 接口)
     * @param taskType 任务类型
     * @return 响应字符串
     */
    suspend fun receiveTaskAward(taskType: String): String {
        return RequestManager.requestString(
            "com.alipay.antiep.receiveTaskAward",
            RpcRequestData.array {
                put("ignoreLimit", true)
                put("requestType", "RPC")
                put("sceneCode", "ANTSTALL_TASK")
                put("source", "AST")
                put("systemType", "android")
                put("taskType", taskType)
                put("version", VERSION)
            }
        )
    }

    /**
     * @brief 完成小铺任务
     * @param taskType 任务类型
     * @return 响应字符串
     */
    suspend fun taskFinish(taskType: String): String {
        return RequestManager.requestString(
            "com.alipay.antstall.task.finish",
            RpcRequestData.array {
                put("source", "search")
                put("systemType", "android")
                put("taskType", taskType)
                put("version", VERSION)
            }
        )
    }

    /**
     * @brief 领取小铺任务奖励
     * @param amount 奖励数量
     * @param prizeId 奖品ID
     * @param taskType 任务类型
     * @return 响应字符串
     */
    suspend fun taskAward(amount: String, prizeId: String, taskType: String): String {
        return RequestManager.requestString(
            "com.alipay.antstall.task.award",
            RpcRequestData.array {
                // 原为无引号插值 $amount，数字字符串时产出 JSON 数字，toBigDecimalOrNull 保持数字类型（兼容整数与小数）
                put("amount", amount.toBigDecimalOrNull() ?: JSONObject.NULL)
                put("prizeId", prizeId)
                put("source", "search")
                put("systemType", "android")
                put("taskType", taskType)
                put("version", VERSION)
            }
        )
    }

    /**
     * @brief 获取任务权益
     * @return 响应字符串
     */
    suspend fun taskBenefit(): String {
        return RequestManager.requestString(
            "com.alipay.antstall.task.benefit",
            RpcRequestData.array {
                put("source", "search")
                put("systemType", "android")
                put("version", VERSION)
            }
        )
    }

    /**
     * @brief 收集肥料
     * @return 响应字符串
     */
    suspend fun collectManure(): String {
        return RequestManager.requestString(
            "com.alipay.antstall.manure.collectManure",
            RpcRequestData.array {
                put("source", "search")
                put("systemType", "android")
                put("version", VERSION)
            }
        )
    }

    /**
     * @brief 查询肥料信息
     * @return 响应字符串
     */
    suspend fun queryManureInfo(): String {
        return RequestManager.requestString(
            "com.alipay.antstall.manure.queryManureInfo",
            RpcRequestData.array {
                put("queryManureType", "ANTSTALL")
                put("source", "search")
                put("systemType", "android")
                put("version", VERSION)
            }
        )
    }

    /**
     * @brief 获取项目列表
     * @return 响应字符串
     */
    suspend fun projectList(): String {
        return RequestManager.requestString(
            "com.alipay.antstall.project.list",
            RpcRequestData.array {
                put("source", "search")
                put("systemType", "android")
                put("version", VERSION)
            }
        )
    }

    /**
     * @brief 获取项目详情
     * @param projectId 项目ID
     * @return 响应字符串
     */
    suspend fun projectDetail(projectId: String): String {
        return RequestManager.requestString(
            "com.alipay.antstall.project.detail",
            RpcRequestData.array {
                put("projectId", projectId)
                put("source", "search")
                put("systemType", "android")
                put("version", VERSION)
            }
        )
    }

    /**
     * @brief 捐赠项目
     * @param projectId 项目ID
     * @return 响应字符串
     */
    suspend fun projectDonate(projectId: String): String {
        return RequestManager.requestString(
            "com.alipay.antstall.project.donate",
            RpcRequestData.array {
                put("bizNo", UUID.randomUUID().toString())
                put("projectId", projectId)
                put("source", "search")
                put("systemType", "android")
                put("version", VERSION)
            }
        )
    }

    /**
     * @brief 获取路线图
     * @return 响应字符串
     */
    suspend fun roadmap(): String {
        return RequestManager.requestString(
            "com.alipay.antstall.village.roadmap",
            RpcRequestData.array {
                put("source", "search")
                put("systemType", "android")
                put("version", VERSION)
            }
        )
    }

    /**
     * @brief 进入下一个村庄
     * @return 响应字符串
     */
    suspend fun nextVillage(): String {
        return RequestManager.requestString(
            "com.alipay.antstall.user.ast.next.village",
            RpcRequestData.array {
                put("source", "search")
                put("systemType", "android")
                put("version", VERSION)
            }
        )
    }

    /**
     * @brief 注册排行榜邀请
     * @return 响应字符串
     */
    suspend fun rankInviteRegister(): String {
        return RequestManager.requestString(
            "com.alipay.antstall.rank.invite.register",
            RpcRequestData.array {
                put("source", "search")
                put("systemType", "android")
                put("version", VERSION)
            }
        )
    }

    /**
     * @brief 注册好友邀请
     * @param friendUserId 好友用户ID
     * @return 响应字符串
     */
    suspend fun friendInviteRegister(friendUserId: String): String {
        return RequestManager.requestString(
            "com.alipay.antstall.friend.invite.register",
            RpcRequestData.array {
                put("friendUserId", friendUserId)
                put("source", "search")
                put("systemType", "android")
                put("version", VERSION)
            }
        )
    }

    /**
     * @brief 分享助力 (P2P)
     * @return 响应字符串
     */
    suspend fun shareP2P(): String {
        return RequestManager.requestString(
            "com.alipay.antiep.shareP2P",
            RpcRequestData.array {
                put("requestType", "RPC")
                put("sceneCode", "ANTSTALL_P2P_SHARER")
                put("source", "ANTSTALL")
                put("systemType", "android")
                put("version", VERSION)
            }
        )
    }

    /**
     * @brief 领取被分享的助力奖励
     * @param shareId 分享ID
     * @return 响应字符串
     */
    suspend fun achieveBeShareP2P(shareId: String): String {
        return RequestManager.requestString(
            "com.alipay.antiep.achieveBeShareP2P",
            RpcRequestData.array {
                put("requestType", "RPC")
                put("sceneCode", "ANTSTALL_P2P_SHARER")
                put("shareId", shareId)
                put("source", "ANTSTALL")
                put("systemType", "android")
                put("version", VERSION)
            }
        )
    }

    /**
     * @brief 遣返好友店铺前的预检查
     * @param billNo 账单号
     * @param seatId 位置ID
     * @param shopId 商店ID
     * @param shopUserId 店主用户ID
     * @return 响应字符串
     */
    suspend fun shopSendBackPre(
        billNo: String,
        seatId: String,
        shopId: String,
        shopUserId: String
    ): String {
        return RequestManager.requestString(
            "com.alipay.antstall.friend.shop.sendback.pre",
            RpcRequestData.array {
                put("billNo", billNo)
                put("seatId", seatId)
                put("shopId", shopId)
                put("shopUserId", shopUserId)
                put("source", "search")
                put("systemType", "android")
                put("version", VERSION)
            }
        )
    }

    /**
     * @brief 遣返好友店铺
     * @param seatId 位置ID
     * @return 响应字符串
     */
    suspend fun shopSendBack(seatId: String): String {
        return RequestManager.requestString(
            "com.alipay.antstall.friend.shop.sendback",
            RpcRequestData.array {
                put("seatId", seatId)
                put("source", "search")
                put("systemType", "android")
                put("version", VERSION)
            }
        )
    }

    /**
     * @brief 打开排行榜邀请
     * @return 响应字符串
     */
    suspend fun rankInviteOpen(): String {
        return RequestManager.requestString(
            "com.alipay.antstall.rank.invite.open",
            RpcRequestData.array {
                put("source", "search")
                put("systemType", "android")
                put("version", VERSION)
            }
        )
    }

    /**
     * @brief 一键邀请好友开店
     * @param friendUserId 好友用户ID
     * @param mySeatId 我的位置ID
     * @return 响应字符串
     */
    suspend fun oneKeyInviteOpenShop(friendUserId: String, mySeatId: String): String {
        return RequestManager.requestString(
            "com.alipay.antstall.user.shop.oneKeyInviteOpenShop",
            RpcRequestData.array {
                put("friendUserId", friendUserId)
                put("mySeatId", mySeatId)
                put("source", "search")
                put("systemType", "android")
                put("version", VERSION)
            }
        )
    }

    /**
     * @brief 获取动态损失（如被贴罚单记录）
     * @return 响应字符串
     */
    suspend fun dynamicLoss(): String {
        return RequestManager.requestString(
            "com.alipay.antstall.dynamic.loss",
            RpcRequestData.array {
                put("source", "search")
                put("systemType", "android")
                put("version", VERSION)
            }
        )
    }

    /**
     * @brief 扔肥料（复仇等）
     * @param dynamicList 动态列表JSONArray
     * @return 响应字符串
     */
    suspend fun throwManure(dynamicList: JSONArray): String {
        return RequestManager.requestString(
            "com.alipay.antstall.manure.throwManure",
            RpcRequestData.array {
                put("dynamicList", dynamicList)
                put("sendMsg", false)
                put("source", "search")
                put("systemType", "android")
                put("version", VERSION)
            }
        )
    }

    /**
     * @brief 结算待收收益
     * @return 响应字符串
     */
    suspend fun settleReceivable(): String {
        return RequestManager.requestString(
            "com.alipay.antstall.self.settle.receivable",
            RpcRequestData.array {
                put("source", "search")
                put("systemType", "android")
                put("version", VERSION)
            }
        )
    }

    /**
     * @brief 查找下一个可以贴罚单的好友
     * @return 响应字符串
     */
    suspend fun nextTicketFriend(): String {
        return RequestManager.requestString(
            "com.alipay.antstall.friend.nextTicketFriend",
            RpcRequestData.array {
                put("source", "search")
                put("systemType", "android")
                put("version", VERSION)
            }
        )
    }

    /**
     * @brief 给好友贴罚单
     * @param billNo 账单编号
     * @param seatId 位置ID
     * @param shopId 商店ID
     * @param shopUserId 商店所属用户ID
     * @param seatUserId 位置所属用户ID
     * @return 响应字符串
     */
    suspend fun ticket(
        billNo: String,
        seatId: String,
        shopId: String,
        shopUserId: String,
        seatUserId: String
    ): String {
        return RequestManager.requestString(
            "com.alipay.antstall.friend.paste.ticket",
            RpcRequestData.array {
                put("billNo", billNo)
                put("seatId", seatId)
                put("shopId", shopId)
                put("shopUserId", shopUserId)
                put("seatUserId", seatUserId)
                put("source", "search")
                put("systemType", "android")
                put("version", VERSION)
            }
        )
    }
}
