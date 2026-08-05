package fansirsqi.xposed.sesame.task.antFarm

import fansirsqi.xposed.sesame.hook.RequestManager

/**
 * @author Constanline
 * @since 2023/08/04
 */
object DadaDailyRpcCall {
    @JvmStatic
    fun home(activityId: String?): String {
        return RequestManager.requestString(
            "com.alipay.reading.game.dadaDaily.home",
            "[{\"activityId\":$activityId,\"dadaVersion\":\"1.3.0\",\"version\":1}]"
        )
    }

    @JvmStatic
    fun submit(activityId: String?, answer: String?, questionId: Long?): String {
        return RequestManager.requestString(
            "com.alipay.reading.game.dadaDaily.submit",
            "[{\"activityId\":$activityId,\"answer\":\"$answer\",\"dadaVersion\":\"1.3.0\",\"questionId\":$questionId,\"version\":1}]"
        )
    }
}
