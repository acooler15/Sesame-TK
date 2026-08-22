package fansirsqi.xposed.sesame.task.other.credit2101

import fansirsqi.xposed.sesame.hook.RequestManager
import fansirsqi.xposed.sesame.hook.rpc.RpcRequestData
import org.json.JSONObject

object Credit2101RpcCall {

    /** 查询账户资产：包含信用印记、碎片、体力、宝箱等 */
    suspend fun queryAccountAsset(): String {
        return RequestManager.requestString(
            "com.alipay.innovationprod.biz.rpc.queryAccountAsset",
            RpcRequestData.array { }
        )
    }

    /** 开宝箱（触发收益） */
    suspend fun triggerBenefit(): String {
        return RequestManager.requestString(
            "com.alipay.innovationprod.biz.rpc.triggerBenefit",
            RpcRequestData.array { }
        )
    }

    /** 查询签到数据 */
    suspend fun querySignInData(): String {
        return RequestManager.requestString(
            "com.alipay.innovationprod.biz.rpc.querySignInData",
            RpcRequestData.array { }
        )
    }

    /** 执行签到，day 为 totalLoginDays */
    suspend fun userSignIn(day: Int): String {
        return RequestManager.requestString(
            "com.alipay.innovationprod.biz.rpc.userSignIn",
            RpcRequestData.array {
                put("day", day)
            }
        )
    }

    /** 查询当前坐标附近事件 */
    suspend fun queryGridEvent(cityCode: String, latitude: Double, longitude: Double, guideState: Boolean = false): String {
        return RequestManager.requestString(
            "com.alipay.innovationprod.biz.rpc.queryGridEvent",
            RpcRequestData.array {
                put("extParams", JSONObject().apply {
                    put("cityCode", cityCode)
                    put("latitude", latitude.toString())
                    put("longitude", longitude.toString())
                })
                put("guideState", guideState)
            }
        )
    }

    /** 小游戏开始：MINI_GAME_ELIMINATE / MINI_GAME_COLLECTYJ 通用 */
    suspend fun eventGameStart(batchNo: String, eventId: String, miniGameStageId: String): String {
        return RequestManager.requestString(
            "com.alipay.innovationprod.biz.rpc.eventGameStart",
            RpcRequestData.array {
                put("batchNo", batchNo)
                put("eventId", eventId)
                put("miniGameStageId", miniGameStageId)
            }
        )
    }


    /**
     * 小游戏完成：收集 YJ 类型（带 collectedYJ 扩展参数）
     * @param batchNo 批次号（非空）
     * @param eventId 事件ID（非空）
     * @param miniGameStageId 小游戏关卡ID（非空）
     * @param collectedYJ 收集的印记数
     * @return 接口响应字符串
     */
    suspend fun eventGameCompleteCollectYj(
        batchNo: String,
        eventId: String,
        miniGameStageId: String,
        collectedYJ: Int // 明确要求传入 Int 类型
    ): String {
        return RequestManager.requestString(
            "com.alipay.innovationprod.biz.rpc.eventGameComplete",
            RpcRequestData.array {
                val extParams = JSONObject().apply {
                    put("collectedYJ", collectedYJ) // JSONObject 存入 Int 时不会带引号
                }
                put("batchNo", batchNo)
                put("eventId", eventId)
                put("extParams", extParams)
                put("miniGameStageId", miniGameStageId)
                put("passed", 1) // 保持为数字 1
            }
        )
    }

    /**
     * 小游戏完成（通用）
     *
     * @param extParams 奖励 / 扩展参数，完全由上层决定
     *                  例如：
     *                  {
     *                    "YJ_PRIZE": 118,
     *                    "killCount": 3,
     *                    "BX_PRIZE": 3
     *                  }
     */
    suspend fun eventGameComplete(
        batchNo: String,
        eventId: String,
        miniGameStageId: String,
        extParams: JSONObject?
    ): String {
        return RequestManager.requestString(
            "com.alipay.innovationprod.biz.rpc.eventGameComplete",
            RpcRequestData.array {
                put("batchNo", batchNo)
                put("eventId", eventId)
                put("miniGameStageId", miniGameStageId)
                put("passed", 1)
                if (extParams != null) {
                    put("extParams", extParams)
                } else {
                    put("extParams", JSONObject.NULL)
                }
            }
        )
    }

    /** 小游戏完成：普通消除类，不带 extParams */
    suspend fun eventGameCompleteSimple(
        batchNo: String,
        eventId: String,
        miniGameStageId: String
    ): String {
        return RequestManager.requestString(
            "com.alipay.innovationprod.biz.rpc.eventGameComplete",
            RpcRequestData.array {
                put("batchNo", batchNo)
                put("eventId", eventId)
                put("miniGameStageId", miniGameStageId)
                put("passed", 1)
            }
        )
    }

    /** 黄金印记事件领取 */
    suspend fun collectCredit(
        batchNo: String,
        eventId: String,
        cityCode: String,
        latitude: Double,
        longitude: Double
    ): String {
        return RequestManager.requestString(
            "com.alipay.innovationprod.biz.rpc.collectCredit",
            RpcRequestData.array {
                put("batchNo", batchNo)
                put("eventId", eventId)
                put("extParams", JSONObject().apply {
                    put("cityCode", cityCode)
                    put("latitude", latitude)
                    put("longitude", longitude)
                })
            }
        )
    }

    /** 查询黑色印记事件详情 */
    suspend fun queryBlackMarkEvent(eventId: String): String {
        return RequestManager.requestString(
            "com.alipay.innovationprod.biz.rpc.queryBlackMarkEvent",
            RpcRequestData.array {
                put("eventId", eventId)
            }
        )
    }

    /** 加入黑色印记事件（最低 10 点能量） */
    suspend fun joinBlackMarkEvent(creditEnergy: Int, eventId: String): String {
        return RequestManager.requestString(
            "com.alipay.innovationprod.biz.rpc.joinBlackMarkEvent",
            RpcRequestData.array {
                put("creditEnergy", creditEnergy)
                put("eventId", eventId)
            }
        )
    }

    /** 黑色印记事件注能 */
    suspend fun chargeBlackMarkEvent(creditEnergy: Int, eventId: String): String {
        return RequestManager.requestString(
            "com.alipay.innovationprod.biz.rpc.chargeBlackMarkEvent",
            RpcRequestData.array {
                put("creditEnergy", creditEnergy)
                put("eventId", eventId)
            }
        )
    }

    /** 探测事件（消耗探索次数） */
    suspend fun exploreGridEvent(cityCode: String, latitude: Double, longitude: Double): String {
        return RequestManager.requestString(
            "com.alipay.innovationprod.biz.rpc.exploreGridEvent",
            RpcRequestData.array {
                put("extParams", JSONObject().apply {
                    put("cityCode", cityCode)
                    put("latitude", latitude.toString())
                    put("longitude", longitude.toString())
                })
            }
        )
    }

    /** 查询每日任务列表 */
    suspend fun queryUserTask(): String {
        return RequestManager.requestString(
            "com.alipay.innovationprod.biz.rpc.queryUserTask",
            RpcRequestData.array { }
        )
    }

    /** 任务操作：例如 TASK_CLAIM */
    suspend fun operateTask(taskAction: String, taskConfigId: String): String {
        return RequestManager.requestString(
            "com.alipay.innovationprod.biz.rpc.operateTask",
            RpcRequestData.array {
                put("taskAction", taskAction)
                put("taskConfigId", taskConfigId)
            }
        )
    }

    /** 领取任务奖励 */
    suspend fun awardTask(taskConfigId: String): String {
        return RequestManager.requestString(
            "com.alipay.innovationprod.biz.rpc.awardTask",
            RpcRequestData.array {
                put("taskConfigId", taskConfigId)
            }
        )
    }

    /** 查询故事事件（时空之门） */
    suspend fun queryEventGate(
        batchNo: String,
        eventId: String,
        cityCode: String,
        latitude: Double,
        longitude: Double
    ): String {
        return RequestManager.requestString(
            "com.alipay.innovationprod.biz.rpc.queryEventGate",
            RpcRequestData.array {
                put("batchNo", batchNo)
                put("eventId", eventId)
                put("extParams", JSONObject().apply {
                    put("cityCode", cityCode)
                    put("latitude", latitude.toString())
                    put("longitude", longitude.toString())
                })
            }
        )
    }

    /** 完成故事事件（时空之门） */
    suspend fun completeEventGate(
        batchNo: String,
        eventId: String,
        cityCode: String,
        latitude: Double,
        longitude: Double,
        storyId: String
    ): String {
        return RequestManager.requestString(
            "com.alipay.innovationprod.biz.rpc.completeEventGate",
            RpcRequestData.array {
                put("batchNo", batchNo)
                put("eventId", eventId)
                put("extParams", JSONObject().apply {
                    put("cityCode", cityCode)
                    put("latitude", latitude.toString())
                    put("longitude", longitude.toString())
                    put("storyId", storyId)
                })
            }
        )
    }

    /**
     * 查询弹窗展示信息
     *
     * RPC: com.alipay.innovationprod.biz.rpc.queryPopupView
     *
     * 请求参数示例：
     * [
     *   {
     *     "popupId": "1"
     *   }
     * ]
     *
     * 响应示例 1：
     * {
     *   "ariverRpcTraceId": "client`aBYSOR/y0xEDACWu2y9mPoqMPiT3WMd_5849815",
     *   "degrade": false,
     *   "popupViewVO": {
     *     "resultMap": {
     *       "energyRecover": 78,
     *       "exploreRecover": 1
     *     },
     *     "showResult": true
     *   },
     *   "resultCode": "SUCCESS",
     *   "resultMsg": "成功",
     *   "success": true,
     *   "traceId": "0b407b1617657190168605938e22e7"
     * }
     *
     * 响应示例 2：
     * {
     *   "ariverRpcTraceId": "0b43b49517657210811446533ebbce",
     *   "degrade": false,
     *   "popupViewVO": {
     *     "resultMap": {
     *       "nextEnergyRecoverMinutes": 25,
     *       "nextExploreRecoverMinutes": 31
     *     },
     *     "showResult": true
     *   },
     *   "resultCode": "SUCCESS",
     *   "resultMsg": "成功",
     *   "success": true,
     *   "traceId": "0b43b49517657210811446533ebbce"
     * }
     *
     * @param popupId 弹窗 ID
     * @return RPC 返回的原始 JSON 字符串
     */
    suspend fun queryPopupView(popupId: String = "1"): String {
        return RequestManager.requestString(
            "com.alipay.innovationprod.biz.rpc.queryPopupView",
            RpcRequestData.array {
                put("popupId", popupId)
            }
        )
    }

    /**
     * 查询所有图鉴进度
     *
     * 示例响应：
     * {
     * "success": true,
     * "resultCode": "SUCCESS",
     * "charterProgress": [
     * { "chapter": "10008", "cardCount": 6, "obtainedCardCount": 6, "awardStatus": "CLAIMED" },
     * { "chapter": "10007", "cardCount": 6, "obtainedCardCount": 4, "awardStatus": "LOCKED" }
     * ]
     * }
     */
    suspend fun queryChapterProgress(): String {
        return RequestManager.requestString(
            "com.alipay.innovationprod.biz.rpc.queryChapterProgress",
            RpcRequestData.array { }
        )
    }

    /**
     * 执行图鉴动作（合成或领奖）
     *
     * @param action  动作类型：
     * "CHAPTER_COMPLETE" -> 合成图鉴
     * "CHAPTER_AWARD"    -> 领取图鉴奖励
     * @param chapter 图鉴章节ID (例如: "10005")
     *
     * 合成响应示例：{"success":true, "chapter":"10005", "awardStatus":"UNLOCKED"}
     * 领奖响应示例：{"success":true, "gainByCollectedAll":{"awardAmount":"1200","awardType":"YJ_PRIZE"}}
     */
    suspend fun completeChapterAction(action: String, chapter: String): String {
        return RequestManager.requestString(
            "com.alipay.innovationprod.biz.rpc.completeChapterAction",
            RpcRequestData.array {
                put("action", action)
                put("chapter", chapter)
            }
        )
    }

    /** 查询天赋状态 */
    suspend fun queryRelationTalent(): String {
        return RequestManager.requestString(
            "com.alipay.innovationprod.biz.rpc.queryRelationTalent",
            RpcRequestData.array { }
        )
    }

    /** 升级具体属性 */
    suspend fun upgradeTalentAttribute(
        attrType: String,
        treeType: String,
        targetLevel: Int
    ): String {
        return RequestManager.requestString(
            "com.alipay.innovationprod.biz.rpc.upgradeTalentAttribute",
            RpcRequestData.array {
                put("roleId", "")
                put("talentAttributeType", attrType)
                put("talentTreeType", treeType)
                put("targetAttributeLevel", targetLevel.toString()) // 严格对齐抓包：字符串类型
            }
        )
    }

    /** 查询修复列表 (黑色印记列表) */
    suspend fun queryGuardMarkList(): String {
        return RequestManager.requestString(
            "com.alipay.innovationprod.biz.rpc.queryGuardMarkList",
            RpcRequestData.array { }
        )
    }

    /** 领取修复列表奖励 */
    suspend fun claimGuardMarkAward(): String {
        return RequestManager.requestString(
            "com.alipay.innovationprod.biz.rpc.claimGuardMarkAward",
            RpcRequestData.array { }
        )
    }


}