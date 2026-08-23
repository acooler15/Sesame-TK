package fansirsqi.xposed.sesame.task.antFarm

import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.core.util.ResChecker
import fansirsqi.xposed.sesame.data.Status
import fansirsqi.xposed.sesame.util.maps.IdMapManager
import fansirsqi.xposed.sesame.util.maps.ParadiseCoinBenefitIdMap
import fansirsqi.xposed.sesame.util.maps.UserMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

internal class FarmShopManager(private val farm: AntFarm) {

    internal suspend fun paradiseCoinExchangeBenefit() {
        try {
            val jo = JSONObject(AntFarmRpcCall.getMallHome())

            if (!ResChecker.checkRes(AntFarm.TAG, jo)) {
                Log.error(AntFarm.TAG, "小鸡乐园币💸[未获取到可兑换权益]")
                return
            }
            val mallItemSimpleList = jo.getJSONArray("mallItemSimpleList")
            for (i in 0..<mallItemSimpleList.length()) {
                val mallItemInfo = mallItemSimpleList.getJSONObject(i)
                val oderInfo: String?
                val spuName = mallItemInfo.getString("spuName")
                val minPrice = mallItemInfo.getInt("minPrice")
                val controlTag = mallItemInfo.getString("controlTag")
                val spuId = mallItemInfo.getString("spuId")
                oderInfo = spuName + "\n价格" + minPrice + "乐园币\n" + controlTag
                IdMapManager.getInstance(ParadiseCoinBenefitIdMap::class.java)
                    .add(spuId, oderInfo)
                val itemStatusList = mallItemInfo.getJSONArray("itemStatusList")
                if (!Status.canParadiseCoinExchangeBenefitToday(spuId) || !farm.paradiseCoinExchangeBenefitList!!.value
                        .contains(spuId) || isExchange(itemStatusList, spuId, spuName)
                ) {
                    continue
                }
                var exchangedCount = 0
                while (exchangeBenefit(spuId)) {
                    exchangedCount += 1
                    Log.farm("乐园币兑换💸#花费[" + minPrice + "乐园币]" + "#第" + exchangedCount + "次兑换" + "[" + spuName + "]")
                    delay(3000)
                }
            }
            IdMapManager.getInstance(ParadiseCoinBenefitIdMap::class.java)
                .save()
        } catch (e: CancellationException) {
            // 协程取消异常必须重新抛出，不能吞掉
             Log.record(AntFarm.TAG, "paradiseCoinExchangeBenefit 协程被取消")
            throw e
        } catch (t: Throwable) {
            Log.printStackTrace(AntFarm.TAG, "paradiseCoinExchangeBenefit err:",t)
        }
    }

    private suspend fun exchangeBenefit(spuId: String?): Boolean {
        try {
            val jo = JSONObject(AntFarmRpcCall.getMallItemDetail(spuId))
            if (!ResChecker.checkRes(AntFarm.TAG, jo)) {
                return false
            }
            val mallItemDetail = jo.getJSONObject("mallItemDetail")
            val mallSubItemDetailList = mallItemDetail.getJSONArray("mallSubItemDetailList")
            for (i in 0..<mallSubItemDetailList.length()) {
                val mallSubItemDetail = mallSubItemDetailList.getJSONObject(i)
                val skuId = mallSubItemDetail.getString("skuId")
                val skuName = mallSubItemDetail.getString("skuName")
                val itemStatusList = mallSubItemDetail.getJSONArray("itemStatusList")

                if (isExchange(itemStatusList, spuId, skuName)) {
                    return false
                }

                if (exchangeBenefit(spuId, skuId)) {
                    return true
                }
            }
        } catch (t: Throwable) {
            Log.printStackTrace(AntFarm.TAG, "exchangeBenefit err:",t)
        }
        return false
    }

    private suspend fun exchangeBenefit(spuId: String?, skuId: String?): Boolean {
        try {
            val jo = JSONObject(AntFarmRpcCall.buyMallItem(spuId, skuId))
            return ResChecker.checkRes(AntFarm.TAG, jo)
        } catch (t: Throwable) {
            Log.printStackTrace(AntFarm.TAG, "exchangeBenefit err:",t)
        }
        return false
    }

    private fun isExchange(itemStatusList: JSONArray, spuId: String?, spuName: String?): Boolean {
        try {
            for (j in 0..<itemStatusList.length()) {
                val itemStatus = itemStatusList.getString(j)
                if (AntFarm.PropStatus.REACH_LIMIT.name == itemStatus
                    || AntFarm.PropStatus.REACH_USER_HOLD_LIMIT.name == itemStatus
                    || AntFarm.PropStatus.NO_ENOUGH_POINT.name == itemStatus
                ) {
                    Log.record(
                        AntFarm.TAG,
                        "乐园兑换💸[$spuName]停止:" + AntFarm.PropStatus.valueOf(itemStatus)
                            .nickName()
                    )
                    if (AntFarm.PropStatus.REACH_LIMIT.name == itemStatus) {
                        Status.setFlagToday("farm::paradiseCoinExchangeLimit::$spuId")
                    }
                    return true
                }
            }
        } catch (t: Throwable) {
            Log.printStackTrace(AntFarm.TAG, "isItemExchange err:",t)
        }
        return false
    }
}
