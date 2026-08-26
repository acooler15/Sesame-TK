package fansirsqi.xposed.sesame.task.antForest.collector

import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.task.antForest.AntForest
import fansirsqi.xposed.sesame.task.antForest.AntForestRpcCall
import org.json.JSONObject

/**
 * PK 好友能量收取器
 *
 * 单一职责：收取 PK 排行榜好友能量（collectPKEnergyCoroutine）。
 * 复用 FriendEnergyCollector 的排行榜处理框架。
 */
internal class PKEnergyCollector(
    private val task: AntForest,
    private val friendCollector: FriendEnergyCollector,
) {

    /**
     * 协程版本：收取PK好友能量
     */
    suspend fun collectPKEnergyCoroutine() {
        friendCollector.collectRankingsCoroutine(
            "PK排行榜",
            { AntForestRpcCall.queryTopEnergyChallengeRanking() },
            "totalData",
            "pk",
            JsonPredicate { pkObject: JSONObject? ->
                if (pkObject!!.getString("rankMemberStatus") != "JOIN") {
                    Log.record(AntForest.TAG, "未加入PK排行榜,跳过,尝试关闭")
                    task.pkEnergy!!.value = false
                    return@JsonPredicate false
                }
                true
            }
        )
    }
}
