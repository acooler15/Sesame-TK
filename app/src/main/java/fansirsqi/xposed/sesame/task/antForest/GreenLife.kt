package fansirsqi.xposed.sesame.task.antForest

import fansirsqi.xposed.sesame.util.CoroutineUtils
import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.core.util.ResChecker
import org.json.JSONObject

object GreenLife {
    @JvmField
    val TAG: String = GreenLife::class.java.simpleName

    /** 森林集市 */
    @JvmStatic
    fun ForestMarket(sourceType: String) {
        try {
            var jo = JSONObject(AntForestRpcCall.consultForSendEnergyByAction(sourceType))
            if (ResChecker.checkRes(TAG, jo)) {
                var data = jo.getJSONObject("data")
                if (data.optBoolean("canSendEnergy", false)) {
                    CoroutineUtils.sleepCompat(1000)
                    jo = JSONObject(AntForestRpcCall.sendEnergyByAction(sourceType))
                    if (ResChecker.checkRes(TAG, jo)) {
                        data = jo.getJSONObject("data")
                        if (data.optBoolean("canSendEnergy", false)) {
                            val receivedEnergyAmount = data.getInt("receivedEnergyAmount")
                            Log.forest("集市逛街🛍[获得:能量" + receivedEnergyAmount + "g]")
                        }
                    }
                }
            } else {
                Log.record(TAG, jo.getJSONObject("data").getString("resultCode"))
                CoroutineUtils.sleepCompat(300)
            }
        } catch (t: Throwable) {
            Log.record(TAG, "sendEnergyByAction err:")
            Log.printStackTrace(TAG, t)
        }
    }
}
