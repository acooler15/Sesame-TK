package fansirsqi.xposed.sesame.task.antFishPond

import fansirsqi.xposed.sesame.hook.RequestManager
import fansirsqi.xposed.sesame.core.util.RandomUtil
import fansirsqi.xposed.sesame.hook.rpc.RpcRequestData
import org.json.JSONArray
import org.json.JSONObject

interface FishPondGateway {
    suspend fun fishpondIndex(): String
    suspend fun fishpondSyncIndex(syncTypes: List<String>): String
    suspend fun querySubplotsActivity(): String
    suspend fun triggerSubplotsActivity(activityType: String, actionType: String): String
    suspend fun listTask(): String
    suspend fun sign(signKey: String): String
    suspend fun fishpondExchangeReward(): String
    suspend fun finishTask(taskType: String, sceneCode: String): String
    suspend fun receiveTaskAward(taskType: String, sceneCode: String): String
    suspend fun fishpondAngle(riskToken: String): String
    suspend fun fishpondAngleRodPositioning(bizNo: String, areaType: String): String
}

object AntFishPondRpcCall {
    private const val VERSION = "20260211.01"
    private const val SOURCE = "farmpool"
    private const val SCENE_GAME_CENTER = "GameCenter"

    private fun JSONObject.baseArgs() {
        put("requestType", "NORMAL")
        put("sceneCode", SCENE_GAME_CENTER)
        put("source", SOURCE)
        put("version", VERSION)
    }

    private fun JSONObject.indexArgs() {
        baseArgs()
        put("appMode", "normal")
    }

    private suspend fun request(method: String, build: JSONObject.() -> Unit): String {
        return RequestManager.requestString(method, RpcRequestData.array(build))
    }

    suspend fun fishpondIndex(): String {
        return request("com.alipay.antfishpond.fishpondIndex") {
            indexArgs()
            put("darwinSceneList", JSONArray().put("taskFullAreaClick"))
        }
    }

    suspend fun fishpondSyncIndex(syncTypes: List<String>): String {
        return request("com.alipay.antfishpond.fishpondSyncIndex") {
            indexArgs()
            put("syncTypeList", JSONArray(syncTypes))
        }
    }

    suspend fun querySubplotsActivity(): String {
        return request("com.alipay.antfishpond.querySubplotsActivity") {
            indexArgs()
        }
    }

    suspend fun triggerSubplotsActivity(activityType: String, actionType: String): String {
        return request("com.alipay.antfishpond.triggerSubplotsActivity") {
            baseArgs()
            put("activityType", activityType)
            put("actionType", actionType)
        }
    }

    suspend fun listTask(): String {
        return request("com.alipay.antfishpond.listTask") {
            indexArgs()
        }
    }

    suspend fun sign(signKey: String): String {
        return request("com.alipay.antfishpond.sign") {
            baseArgs()
            put("signKey", signKey)
        }
    }

    suspend fun fishpondExchangeReward(): String {
        return request("com.alipay.antfishpond.fishpondExchangeReward") {
            baseArgs()
        }
    }

    suspend fun finishTask(taskType: String, sceneCode: String): String {
        return request("com.alipay.antiep.finishTask") {
            put(
                "outBizNo",
                "${taskType}_${System.currentTimeMillis()}_${RandomUtil.getRandomString(8)}"
            )
            put("requestType", "RPC")
            put("sceneCode", sceneCode)
            put("source", "ADBASICLIB")
            put("taskType", taskType)
        }
    }

    suspend fun receiveTaskAward(taskType: String, sceneCode: String): String {
        return request("com.alipay.antiep.receiveTaskAward") {
            baseArgs()
            put("ignoreLimit", false)
            put("sceneCode", sceneCode)
            put("taskType", taskType)
        }
    }

    suspend fun fishpondAngle(riskToken: String): String {
        return request("com.alipay.antfishpond.fishpondAngle") {
            baseArgs()
            put("bizNo", "")
            put("riskToken", riskToken)
        }
    }

    suspend fun fishpondAngleRodPositioning(bizNo: String, areaType: String): String {
        return request("com.alipay.antfishpond.fishpondAngleRodPositioning") {
            baseArgs()
            put("bizNo", bizNo)
            put("areaType", areaType)
        }
    }
}

class AntFishPondRpcGateway : FishPondGateway {
    override suspend fun fishpondIndex(): String = AntFishPondRpcCall.fishpondIndex()

    override suspend fun fishpondSyncIndex(syncTypes: List<String>): String =
        AntFishPondRpcCall.fishpondSyncIndex(syncTypes)

    override suspend fun querySubplotsActivity(): String =
        AntFishPondRpcCall.querySubplotsActivity()

    override suspend fun triggerSubplotsActivity(activityType: String, actionType: String): String =
        AntFishPondRpcCall.triggerSubplotsActivity(activityType, actionType)

    override suspend fun listTask(): String = AntFishPondRpcCall.listTask()

    override suspend fun sign(signKey: String): String = AntFishPondRpcCall.sign(signKey)

    override suspend fun fishpondExchangeReward(): String =
        AntFishPondRpcCall.fishpondExchangeReward()

    override suspend fun finishTask(taskType: String, sceneCode: String): String =
        AntFishPondRpcCall.finishTask(taskType, sceneCode)

    override suspend fun receiveTaskAward(taskType: String, sceneCode: String): String =
        AntFishPondRpcCall.receiveTaskAward(taskType, sceneCode)

    override suspend fun fishpondAngle(riskToken: String): String =
        AntFishPondRpcCall.fishpondAngle(riskToken)

    override suspend fun fishpondAngleRodPositioning(bizNo: String, areaType: String): String =
        AntFishPondRpcCall.fishpondAngleRodPositioning(bizNo, areaType)
}
