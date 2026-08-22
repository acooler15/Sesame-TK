package fansirsqi.xposed.sesame.task.antDodo

import org.json.JSONObject
import fansirsqi.xposed.sesame.hook.RequestManager
import fansirsqi.xposed.sesame.core.util.RandomUtil
import fansirsqi.xposed.sesame.hook.rpc.RpcRequestData

object AntDodoRpcCall {
    private const val VERSION = "20241203"

    /* 神奇物种 */
    @JvmStatic
    suspend fun queryAnimalStatus(): String {
        return RequestManager.requestString(
            "alipay.antdodo.rpc.h5.queryAnimalStatus",
            RpcRequestData.array {
                put("source", "chInfo_ch_appcenter__chsub_9patch")
            }
        )
    }

    @JvmStatic
    suspend fun homePage(): String {
        return RequestManager.requestString(
            "alipay.antdodo.rpc.h5.homePage",
            RpcRequestData.array { }
        )
    }

    @JvmStatic
    suspend fun collect(): String {
        return RequestManager.requestString(
            "alipay.antdodo.rpc.h5.collect",
            RpcRequestData.array { }
        )
    }

    @JvmStatic
    suspend fun taskList(): String {
        return RequestManager.requestString(
            "alipay.antdodo.rpc.h5.taskList",
            RpcRequestData.array {
                put("version", VERSION)
            }
        )
    }

    @JvmStatic
    suspend fun finishTask(sceneCode: String?, taskType: String?): String {
        val uniqueId = getUniqueId()
        return RequestManager.requestString(
            "com.alipay.antiep.finishTask",
            RpcRequestData.array {
                put("outBizNo", uniqueId)
                put("requestType", "rpc")
                put("sceneCode", sceneCode)
                put("source", "af-biodiversity")
                put("taskType", taskType)
                put("uniqueId", uniqueId)
            }
        )
    }

    private fun getUniqueId(): String {
        return System.currentTimeMillis().toString() + RandomUtil.nextLong()
    }

    @JvmStatic
    suspend fun receiveTaskAward(sceneCode: String?, taskType: String?): String {
        return RequestManager.requestString(
            "com.alipay.antiep.receiveTaskAward",
            RpcRequestData.array {
                put("ignoreLimit", 0)
                put("requestType", "rpc")
                put("sceneCode", sceneCode)
                put("source", "af-biodiversity")
                put("taskType", taskType)
            }
        )
    }

    @JvmStatic
    suspend fun propList(): String {
        return RequestManager.requestString(
            "alipay.antdodo.rpc.h5.propList",
            RpcRequestData.array { }
        )
    }

    //使用道具
    @JvmStatic
    suspend fun consumeProp(propId: String?, propType: String?, animalId: String?): String {
        return RequestManager.requestString(
            "alipay.antdodo.rpc.h5.consumeProp",
            RpcRequestData.array {
                // 如果 animalId 不为空，则构建 extendInfo 字段
                if (!animalId.isNullOrEmpty()) {
                    put("extendInfo", JSONObject().apply { put("animalId", animalId) })
                }
                put("propId", propId)
                put("propType", propType)
            }
        )
    }

    /**
     * 专门用于：抽好友卡道具 的消耗请求
     * 参数格式：[{"propId":"...","propType":"..."}]
     */
    @JvmStatic
    suspend fun consumePropForFriend(propId: String?, propType: String?): String {
        return RequestManager.requestString(
            "alipay.antdodo.rpc.h5.consumeProp",
            RpcRequestData.array {
                put("propId", propId)
                put("propType", propType)
            }
        )
    }

    //查询图鉴详情
    @JvmStatic
    suspend fun queryBookInfo(bookId: String?): String {
        return RequestManager.requestString(
            "alipay.antdodo.rpc.h5.queryBookInfo",
            RpcRequestData.array {
                put("bookId", bookId)
            }
        )
    }

    // 送卡片给好友
    @JvmStatic
    suspend fun social(targetAnimalId: String?, targetUserId: String?): String {
        return RequestManager.requestString(
            "alipay.antdodo.rpc.h5.social",
            RpcRequestData.array {
                put("actionCode", "GIFT_TO_FRIEND")
                put("source", "GIFT_TO_FRIEND_FROM_CC")
                put("targetAnimalId", targetAnimalId)
                put("targetUserId", targetUserId)
                put("triggerTime", System.currentTimeMillis().toString())
            }
        )
    }

    @JvmStatic
    suspend fun queryFriend(): String {
        return RequestManager.requestString(
            "alipay.antdodo.rpc.h5.queryFriend",
            RpcRequestData.array {
                put("sceneCode", "EXCHANGE")
            }
        )
    }

    @JvmStatic
    suspend fun collecttarget(targetUserId: String?): String {
        return RequestManager.requestString(
            "alipay.antdodo.rpc.h5.collect",
            RpcRequestData.array {
                // targetUserId 原为无引号插值（数字形态，调用方传 alipay userId 数字字符串），toBigDecimalOrNull 保持数字类型（兼容整数与小数）
                put("targetUserId", targetUserId?.toBigDecimalOrNull() ?: JSONObject.NULL)
            }
        )
    }

    @JvmStatic
    suspend fun queryBookList(pageSize: Int, pageStart: String?): String {
        try {
            return RequestManager.requestString(
                "alipay.antdodo.rpc.h5.queryBookList",
                RpcRequestData.array {
                    put("pageSize", pageSize)
                    put("v2", "true")

                    // 仅在 pageStart 不为空时才添加该字段
                    if (!pageStart.isNullOrEmpty()) {
                        put("pageStart", pageStart)
                    }
                }
            )
        } catch (e: Exception) {
            return ""
        }
    }

    @JvmStatic
    suspend fun generateBookMedal(bookId: String?): String {
        return RequestManager.requestString(
            "alipay.antdodo.rpc.h5.generateBookMedal",
            RpcRequestData.array {
                put("bookId", bookId)
            }
        )
    }
}
