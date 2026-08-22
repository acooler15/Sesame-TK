package fansirsqi.xposed.sesame.task.antStall

import fansirsqi.xposed.sesame.core.util.StringUtil
import fansirsqi.xposed.sesame.hook.RequestManager
import fansirsqi.xposed.sesame.hook.rpc.RpcRequestData

/**
 * @file ReadingDadaRpcCall.kt
 * @brief 阅读答题相关RPC调用
 * @author
 * @since 2023/08/22
 */
object ReadingDadaRpcCall {
    private const val VERSION = "1"

    /**
     * @brief 提交答案
     * @param activityId 活动ID
     * @param outBizId 外部业务ID
     * @param questionId 问题ID
     * @param answer 答案
     * @return 返回结果字符串
     */
    suspend fun submitAnswer(
        activityId: String,
        outBizId: String,
        questionId: String,
        answer: String
    ): String {
        return RequestManager.requestString(
            "com.alipay.reading.game.dada.openDailyAnswer.submitAnswer",
            RpcRequestData.array {
                put("activityId", activityId)
                put("answer", answer)
                put("dadaVersion", "1.3.0")
                if (!StringUtil.isEmpty(outBizId)) {
                    put("outBizId", outBizId)
                }
                put("questionId", questionId)
                put("version", VERSION)
            }
        )
    }

    /**
     * @brief 获取问题
     * @param activityId 活动ID
     * @return 返回结果字符串
     */
    suspend fun getQuestion(activityId: String): String {
        return RequestManager.requestString(
            "com.alipay.reading.game.dada.openDailyAnswer.getQuestion",
            RpcRequestData.array {
                put("activityId", activityId)
                put("dadaVersion", "1.3.0")
                put("version", VERSION)
            }
        )
    }
}
