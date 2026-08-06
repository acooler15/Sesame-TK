package fansirsqi.xposed.sesame.task.antForest

import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.util.ResChecker
import fansirsqi.xposed.sesame.util.TimeUtil
import org.json.JSONArray
import org.json.JSONObject

/**
 * @author Byseven
 * @date 2025/3/7
 * @apiNote
 */
object Healthcare {

    @JvmField
    val TAG: String = Healthcare::class.java.simpleName

    @JvmStatic
    fun queryForestEnergy(scene: String) {
        try {
            var jo = JSONObject(AntForestRpcCall.queryForestEnergy(scene))
            if (!ResChecker.checkRes(TAG, jo)) {
                return
            }
            jo = jo.getJSONObject("data").getJSONObject("response")
            var ja = jo.getJSONArray("energyGeneratedList")
            if (ja.length() > 0) {
                harvestForestEnergy(scene, ja)
            }
            val remainBubble = jo.optInt("remainBubble")
            for (i in 0 until remainBubble) {
                ja = produceForestEnergy(scene)
                if (ja.length() == 0 || !harvestForestEnergy(scene, ja)) {
                    return
                }
                TimeUtil.sleepCompat(1000)
            }
        } catch (th: Throwable) {
            Log.record(TAG, "queryForestEnergy err:")
            Log.printStackTrace(TAG, th)
        }
    }

    private fun produceForestEnergy(scene: String): JSONArray {
        var energyGeneratedList = JSONArray()
        try {
            var jo = JSONObject(AntForestRpcCall.produceForestEnergy(scene))
            if (ResChecker.checkRes(TAG, jo)) {
                jo = jo.getJSONObject("data").getJSONObject("response")
                energyGeneratedList = jo.getJSONArray("energyGeneratedList")
                if (energyGeneratedList.length() > 0) {
                    val title = if (scene == "FEEDS") "绿色医疗" else "电子小票"
                    val cumulativeEnergy = jo.getInt("cumulativeEnergy")
                    Log.forest("医疗健康🚑完成[" + title + "]#产生[" + cumulativeEnergy + "g能量]")
                }
            }
        } catch (th: Throwable) {
            Log.record(TAG, "produceForestEnergy err:")
            Log.printStackTrace(TAG, th)
        }
        return energyGeneratedList
    }

    private fun harvestForestEnergy(scene: String, bubbles: JSONArray): Boolean {
        try {
            var jo = JSONObject(AntForestRpcCall.harvestForestEnergy(scene, bubbles))
            if (!ResChecker.checkRes(TAG, jo)) {
                return false
            }
            jo = jo.getJSONObject("data").getJSONObject("response")
            val collectedEnergy = jo.getInt("collectedEnergy")
            if (collectedEnergy > 0) {
                val title = if (scene == "FEEDS") "绿色医疗" else "电子小票"
                Log.forest("医疗健康🚑收取[" + title + "]#获得[" + collectedEnergy + "g能量]")
                return true
            }
        } catch (th: Throwable) {
            Log.record(TAG, "harvestForestEnergy err:")
            Log.printStackTrace(TAG, th)
        }
        return false
    }
}
