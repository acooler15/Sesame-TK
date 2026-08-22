package fansirsqi.xposed.sesame.task.antFarm

import fansirsqi.xposed.sesame.hook.RequestManager
import fansirsqi.xposed.sesame.hook.rpc.RpcRequestData
import org.json.JSONObject

/**
 * @author Constanline
 * @since 2023/08/04
 */
object DadaDailyRpcCall {
    @JvmStatic
    suspend fun home(activityId: String?): String {
        return RequestManager.requestString(
            "com.alipay.reading.game.dadaDaily.home",
            RpcRequestData.array {
                // activityId 原为无引号插值（数字形态，调用方恒传数字字符串如 "100"），toBigDecimalOrNull 保持数字类型（兼容整数与小数）
                put("activityId", activityId?.toBigDecimalOrNull() ?: JSONObject.NULL)
                put("dadaVersion", "1.3.0")
                put("version", "1")
            }
        )
    }

    @JvmStatic
    suspend fun submit(activityId: String?, answer: String?, questionId: Long?): String {
        return RequestManager.requestString(
            "com.alipay.reading.game.dadaDaily.submit",
            RpcRequestData.array {
                // activityId 原为无引号插值（数字形态，调用方恒传数字字符串如 "100"），toBigDecimalOrNull 保持数字类型（兼容整数与小数）
                put("activityId", activityId?.toBigDecimalOrNull() ?: JSONObject.NULL)
                // answer 原为 "null" 字符串插值副作用，保持 JSON null 语义（put 传 null 会删键）
                put("answer", answer ?: JSONObject.NULL)
                put("dadaVersion", "1.3.0")
                put("questionId", questionId ?: JSONObject.NULL)
                put("version", "1")
            }
        )
    }
}
