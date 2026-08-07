package fansirsqi.xposed.sesame.task.antFarm

import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.core.util.ResChecker
import fansirsqi.xposed.sesame.data.Status
import fansirsqi.xposed.sesame.util.maps.UserMap
import org.json.JSONObject

internal class FarmDonateManager(private val farm: AntFarm) {

    internal fun harvestProduce(farmId: String?) {
        try {
            val s = AntFarmRpcCall.harvestProduce(farmId)
            val jo = JSONObject(s)
            val memo = jo.getString("memo")
            if (ResChecker.checkRes(AntFarm.TAG, jo)) {
                val harvest = jo.getDouble("harvestBenevolenceScore")
                farm.harvestBenevolenceScore = jo.getDouble("finalBenevolenceScore")
                Log.farm("收取鸡蛋🥚[" + harvest + "颗]#剩余" + farm.harvestBenevolenceScore + "颗")
            } else {
                Log.record(memo)
                Log.record(s)
            }
        } catch (t: Throwable) {
            Log.printStackTrace(AntFarm.TAG, "harvestProduce err:",t)
        }
    }

    /* 捐赠爱心鸡蛋 */
    internal fun handleDonation(donationType: Int) {
        try {
            val s = AntFarmRpcCall.listActivityInfo()
            var jo = JSONObject(s)
            val memo = jo.getString("memo")
            if (ResChecker.checkRes(AntFarm.TAG, jo)) {
                val jaActivityInfos = jo.getJSONArray("activityInfos")
                var activityId: String? = null
                var activityName: String?
                var isDonation = false
                for (i in 0..<jaActivityInfos.length()) {
                    jo = jaActivityInfos.getJSONObject(i)
                    if (jo.get("donationTotal") != jo.get("donationLimit")) {
                        activityId = jo.getString("activityId")
                        activityName = jo.optString("projectName", activityId)
                        if (performDonation(activityId, activityName)) {
                            isDonation = true
                            if (donationType == AntFarm.DonationCount.ONE) {
                                break
                            }
                        }
                    }
                }
                if (isDonation) {
                    val userId = UserMap.currentUid
                    Status.donationEgg(userId)
                }
                if (activityId == null) {
                    Log.record(AntFarm.TAG, "今日已无可捐赠的活动")
                }
            } else {
                Log.record(memo)
                Log.record(s)
            }
        } catch (t: Throwable) {
            Log.printStackTrace(AntFarm.TAG, "donation err:",t)
        }
    }

    private fun performDonation(activityId: String?, activityName: String?): Boolean {
        try {
            val s = AntFarmRpcCall.donation(activityId, 1)
            val donationResponse = JSONObject(s)
            val memo = donationResponse.getString("memo")
            if (ResChecker.checkRes(AntFarm.TAG, donationResponse)) {
                val donationDetails = donationResponse.getJSONObject("donation")
                farm.harvestBenevolenceScore = donationDetails.getDouble("harvestBenevolenceScore")
                Log.farm("捐赠活动❤️[" + activityName + "]#累计捐赠" + donationDetails.getInt("donationTimesStat") + "次")
                return true
            } else {
                Log.record(memo)
                Log.record(s)
            }
        } catch (t: Throwable) {
            Log.printStackTrace(t)
        }
        return false
    }
}
