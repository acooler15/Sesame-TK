package fansirsqi.xposed.sesame.task.antForest.collector

import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.core.threads.GlobalThreadPools
import fansirsqi.xposed.sesame.core.util.ResChecker
import fansirsqi.xposed.sesame.hook.Toast
import fansirsqi.xposed.sesame.task.antForest.AntForest
import fansirsqi.xposed.sesame.task.antForest.AntForestRpcCall
import fansirsqi.xposed.sesame.util.maps.UserMap
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * 金球收取器（浇水回赠/复活/回赠能量）
 *
 * 单一职责：处理浇水金球、好友复活能量、好友复活回赠能量的收取。
 */
internal class WateringBubbleCollector(
    private val task: AntForest,
) {

    /**
     * 收取回赠能量，好友浇水金秋，好友复活能量
     *
     * @param wateringBubbles 包含不同类型金球的对象数组
     */
    suspend fun collectWateringBubbles(wateringBubbles: JSONArray) {
        for (i in 0..<wateringBubbles.length()) {
            try {
                val wateringBubble = wateringBubbles.getJSONObject(i)
                when (val bizType = wateringBubble.getString("bizType")) {
                    "jiaoshui" -> collectWater(wateringBubble)
                    "fuhuo" -> collectRebornEnergy()
                    "baohuhuizeng" -> collectReturnEnergy(wateringBubble)
                    else -> {
                        Log.record(AntForest.TAG, "未知bizType: $bizType")
                        continue
                    }
                }
                GlobalThreadPools.sleepCompat(500L)
            } catch (e: JSONException) {
                Log.record(AntForest.TAG, "浇水金球JSON解析错误: " + e.message)
            } catch (e: RuntimeException) {
                Log.record(AntForest.TAG, "浇水金球处理异常: " + e.message)
            }
        }
    }

    private suspend fun collectWater(wateringBubble: JSONObject) {
        try {
            val id = wateringBubble.getLong("id")
            val response = AntForestRpcCall.collectEnergy("jiaoshui", task.selfId, id)
            processCollectResult(response, "收取金球🍯浇水")
        } catch (e: JSONException) {
            Log.record(AntForest.TAG, "收取浇水JSON解析错误: " + e.message)
        }
    }

    private suspend fun collectRebornEnergy() {
        try {
            val response = AntForestRpcCall.collectRebornEnergy()
            processCollectResult(response, "收取金球🍯复活")
        } catch (e: RuntimeException) {
            Log.record(AntForest.TAG, "收取金球运行时异常: " + e.message)
        }
    }

    private suspend fun collectReturnEnergy(wateringBubble: JSONObject) {
        try {
            val friendId = wateringBubble.getString("userId")
            val id = wateringBubble.getLong("id")
            val response = AntForestRpcCall.collectEnergy("baohuhuizeng", task.selfId, id)
            processCollectResult(
                response,
                "收取金球🍯[" + UserMap.getMaskName(friendId) + "]复活回赠"
            )
        } catch (e: JSONException) {
            Log.record(AntForest.TAG, "收取金球回赠JSON解析错误: " + e.message)
        }
    }

    /**
     * 处理金球-浇水、收取结果
     *
     * @param response       收取结果
     * @param successMessage 成功提示信息
     */
    private fun processCollectResult(response: String, successMessage: String?) {
        try {
            val joEnergy = JSONObject(response)
            if (ResChecker.checkRes(AntForest.TAG + "收集能量失败:", joEnergy)) {
                val bubbles = joEnergy.getJSONArray("bubbles")
                if (bubbles.length() > 0) {
                    val collected = bubbles.getJSONObject(0).getInt("collectedEnergy")
                    if (collected > 0) {
                        val msg = successMessage + "[" + collected + "g]"
                        Log.forest(msg)
                        Toast.show(msg)
                    } else {
                        Log.record(successMessage + "失败")
                    }
                } else {
                    Log.record(successMessage + "失败: 未找到金球信息")
                }
            } else {
                Log.record(successMessage + "失败:" + joEnergy.getString("resultDesc"))
                Log.record(response)
            }
        } catch (e: JSONException) {
            Log.record(AntForest.TAG, "JSON解析错误: " + e.message)
        } catch (e: Exception) {
            Log.record(AntForest.TAG, "处理收能量结果错误: " + e.message)
        }
    }
}
