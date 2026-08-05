package fansirsqi.xposed.sesame.task.antDodo

import org.json.JSONObject
import fansirsqi.xposed.sesame.hook.RequestManager
import fansirsqi.xposed.sesame.util.RandomUtil

object AntDodoRpcCall {
    private const val VERSION = "20241203"

    /* 神奇物种 */
    @JvmStatic
    fun queryAnimalStatus(): String {
        return RequestManager.requestString(
            "alipay.antdodo.rpc.h5.queryAnimalStatus",
            "[{\"source\":\"chInfo_ch_appcenter__chsub_9patch\"}]"
        )
    }

    @JvmStatic
    fun homePage(): String {
        return RequestManager.requestString(
            "alipay.antdodo.rpc.h5.homePage",
            "[{}]"
        )
    }

    @JvmStatic
    fun collect(): String {
        return RequestManager.requestString(
            "alipay.antdodo.rpc.h5.collect",
            "[{}]"
        )
    }

    @JvmStatic
    fun taskList(): String {
        return RequestManager.requestString(
            "alipay.antdodo.rpc.h5.taskList",
            "[{\"version\":\"$VERSION\"}]"
        )
    }

    @JvmStatic
    fun finishTask(sceneCode: String?, taskType: String?): String {
        val uniqueId = getUniqueId()
        return RequestManager.requestString(
            "com.alipay.antiep.finishTask",
            "[{\"outBizNo\":\"$uniqueId\",\"requestType\":\"rpc\",\"sceneCode\":\"$sceneCode\",\"source\":\"af-biodiversity\",\"taskType\":\"$taskType\",\"uniqueId\":\"$uniqueId\"}]"
        )
    }

    private fun getUniqueId(): String {
        return System.currentTimeMillis().toString() + RandomUtil.nextLong()
    }

    @JvmStatic
    fun receiveTaskAward(sceneCode: String?, taskType: String?): String {
        return RequestManager.requestString(
            "com.alipay.antiep.receiveTaskAward",
            "[{\"ignoreLimit\":0,\"requestType\":\"rpc\",\"sceneCode\":\"$sceneCode\",\"source\":\"af-biodiversity\",\"taskType\":\"$taskType\"}]"
        )
    }

    @JvmStatic
    fun propList(): String {
        return RequestManager.requestString(
            "alipay.antdodo.rpc.h5.propList",
            "[{}]"
        )
    }

    //使用道具
    @JvmStatic
    fun consumeProp(propId: String?, propType: String?, animalId: String?): String {
        // 基础参数
        val params = StringBuilder("[{")

        // 如果 animalId 不为空，则构建 extendInfo 字段
        if (!animalId.isNullOrEmpty()) {
            params.append("\"extendInfo\":{")
                .append("\"animalId\":\"").append(animalId).append("\"")
                .append("},")
        }

        // 拼接 propId 和 propType
        params.append("\"propId\":\"").append(propId).append("\",")
            .append("\"propType\":\"").append(propType).append("\"")
            .append("}]")
        return RequestManager.requestString("alipay.antdodo.rpc.h5.consumeProp", params.toString())
    }

    /**
     * 专门用于：抽好友卡道具 的消耗请求
     * 参数格式：[{"propId":"...","propType":"..."}]
     */
    @JvmStatic
    fun consumePropForFriend(propId: String?, propType: String?): String {
        // 构造不含 extendInfo 的参数
        val params = "[{" +
                "\"propId\":\"$propId\"," +
                "\"propType\":\"$propType\"" +
                "}]"
        return RequestManager.requestString("alipay.antdodo.rpc.h5.consumeProp", params)
    }

    //查询图鉴详情
    @JvmStatic
    fun queryBookInfo(bookId: String?): String {
        return RequestManager.requestString(
            "alipay.antdodo.rpc.h5.queryBookInfo",
            "[{\"bookId\":\"$bookId\"}]"
        )
    }

    // 送卡片给好友
    @JvmStatic
    fun social(targetAnimalId: String?, targetUserId: String?): String {
        return RequestManager.requestString(
            "alipay.antdodo.rpc.h5.social",
            "[{\"actionCode\":\"GIFT_TO_FRIEND\",\"source\":\"GIFT_TO_FRIEND_FROM_CC\",\"targetAnimalId\":\"$targetAnimalId\",\"targetUserId\":\"$targetUserId\",\"triggerTime\":\"${System.currentTimeMillis()}\"}]"
        )
    }

    @JvmStatic
    fun queryFriend(): String {
        return RequestManager.requestString(
            "alipay.antdodo.rpc.h5.queryFriend",
            "[{\"sceneCode\":\"EXCHANGE\"}]"
        )
    }

    @JvmStatic
    fun collecttarget(targetUserId: String?): String {
        return RequestManager.requestString(
            "alipay.antdodo.rpc.h5.collect",
            "[{\"targetUserId\":$targetUserId}]"
        )
    }

    @JvmStatic
    fun queryBookList(pageSize: Int, pageStart: String?): String {
        try {
            // 使用 JSONObject 构造可以避免手动拼接字符串导致的转义和逗号错误
            val params = JSONObject()
            params.put("pageSize", pageSize)
            params.put("v2", "true")

            // 仅在 pageStart 不为空时才添加该字段
            if (!pageStart.isNullOrEmpty()) {
                params.put("pageStart", pageStart)
            }

            return RequestManager.requestString("alipay.antdodo.rpc.h5.queryBookList", "[" + params.toString() + "]")
        } catch (e: Exception) {
            return ""
        }
    }

    @JvmStatic
    fun generateBookMedal(bookId: String?): String {
        val args = "[{\"bookId\":\"$bookId\"}]"
        return RequestManager.requestString("alipay.antdodo.rpc.h5.generateBookMedal", args)
    }
}
