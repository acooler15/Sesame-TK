package fansirsqi.xposed.sesame.task.antForest

import fansirsqi.xposed.sesame.data.Status
import fansirsqi.xposed.sesame.entity.VitalityStore.ExchangeStatus
import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.core.util.ResChecker
import fansirsqi.xposed.sesame.core.util.TimeUtil
import fansirsqi.xposed.sesame.task.ModelTask
import fansirsqi.xposed.sesame.util.maps.IdMapManager
import fansirsqi.xposed.sesame.util.maps.UserMap
import fansirsqi.xposed.sesame.util.maps.VitalityRewardsMap
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject

/**
 * @author Byseven
 * @apiNote
 * @see 2025/1/20
 */
object Vitality {
    private val TAG: String = Vitality::class.java.simpleName
    private val skuInfo: MutableMap<String, JSONObject> = HashMap()

    @JvmStatic
    fun ItemListByType(labelType: String): JSONArray? {
        val itemInfoVOList: JSONArray = JSONArray()
        var hasMore: Boolean
        var startIndex = 0
        try {
            do {
                val jo = JSONObject(runBlocking { AntForestRpcCall.itemList(labelType, startIndex) })
                if (!ResChecker.checkRes(TAG + "查询森林活力值商品列表失败:", jo)) {
                    return null
                }
                val page = jo.optJSONArray("itemInfoVOList")
                if (page != null) {
                    for (i in 0 until page.length()) {
                        itemInfoVOList.put(page.optJSONObject(i))
                    }
                }
                hasMore = jo.optBoolean("hasMore")
                startIndex = jo.optInt("nextStartIndex", startIndex + (page?.length() ?: 0))
            } while (hasMore)
        } catch (th: Throwable) {
            Log.record(TAG, "ItemListByType err")
            Log.printStackTrace(TAG, th)
            return null
        }
        return itemInfoVOList
    }

    @JvmStatic
    fun ItemDetailBySpuId(spuId: String) {
        try {
            val jo = JSONObject(runBlocking { AntForestRpcCall.itemDetail(spuId) })
            if (ResChecker.checkRes(TAG + "查询森林活力值商品详情失败:", jo)) {
                val itemDetail = jo.getJSONObject("spuItemInfoVO")
                handleItemDetail(itemDetail)
            }
        } catch (th: Throwable) {
            Log.record(TAG, "ItemDetailBySpuId err")
            Log.printStackTrace(TAG, th)
        }
    }

    @JvmStatic
    fun initVitality(labelType: String) {
        try {
            val itemInfoVOList = ItemListByType(labelType)
            if (itemInfoVOList != null) {
                for (i in 0 until itemInfoVOList.length()) {
                    val itemInfoVO = itemInfoVOList.getJSONObject(i)
                    handleVitalityItem(itemInfoVO)
                }
            } else {
                Log.error(TAG, "活力兑换🍃初始化失败！")
            }
        } catch (th: Throwable) {
            Log.record(TAG, "initVitality err")
            Log.printStackTrace(TAG, th)
        }
    }

    private fun handleVitalityItem(vitalityItem: JSONObject) {
        try {
            //海洋随机拼图skuModelList节点下没有spuId
            val spuId = vitalityItem.optString("spuId")
            val skuModelList = vitalityItem.getJSONArray("skuModelList")
            for (i in 0 until skuModelList.length()) {
                val skuModel = skuModelList.getJSONObject(i)
                val skuId = skuModel.getString("skuId")
                var oderInfo: String
                val skuName = skuModel.getString("skuName")
                val price = skuModel.getJSONObject("price").getInt("amount")
                oderInfo = skuName + "\n价格" + price + "🍃活力值"
                if (skuName.contains("能量雨") || skuName.contains("敦煌") || skuName.contains("保护罩") || skuName.contains("海洋") || skuName.contains("物种") || skuName.contains("收能量") || skuName.contains("隐身")) {
                    oderInfo = skuName + "\n价格" + price + "🍃活力值" + "\n每日限时兑1个"
                } else if (skuName == "限时31天内使用31天长效双击卡") {
                    oderInfo = skuName + "\n价格" + price + "🍃活力值" + "\n每月限时兑1个，记得关，艹"
                }
                if (!skuModel.has("spuId")) {
                    skuModel.put("spuId", spuId)
                }
                skuInfo[skuId] = skuModel
                IdMapManager.getInstance(VitalityRewardsMap::class.java).add(skuId, oderInfo)
            }
            IdMapManager.getInstance(VitalityRewardsMap::class.java).save()
        } catch (th: Throwable) {
            Log.record(TAG, "handleVitalityItem err")
            Log.printStackTrace(TAG, th)
        }
    }

    private fun handleItemDetail(itemDetail: JSONObject) {
        try {
            val spuId = itemDetail.getString("spuId")
            val skuModelList = itemDetail.getJSONArray("skuModelList")
            for (i in 0 until skuModelList.length()) {
                val skuModel = skuModelList.getJSONObject(i)
                val skuId = skuModel.getString("skuId")
                val skuName = skuModel.getString("skuName")
                if (!skuModel.has("spuId")) {
                    skuModel.put("spuId", spuId)
                }
                skuInfo[skuId] = skuModel
                IdMapManager.getInstance(VitalityRewardsMap::class.java).add(skuId, skuName)
            }
            IdMapManager.getInstance(VitalityRewardsMap::class.java).save()
        } catch (th: Throwable) {
            Log.record(TAG, "handleItemDetail err:")
            Log.printStackTrace(TAG, th)
        }
    }

    /*
     * 兑换活力值商品
     * sku
     * spuId, skuId, skuName, exchangedCount, price[amount]
     * exchangedCount == 0......
     */
    @JvmStatic
    fun handleVitalityExchange(skuId: String): Boolean {
        // 检查是否已经达到今日兑换上限
        if (Status.hasFlagToday("forest::VitalityExchangeLimit::" + skuId)) {
            Log.record(TAG, "活力兑换🍃[" + skuId + "]今日已达上限，跳过兑换")
            return false
        }

        if (skuInfo.isEmpty()) {
            initVitality("SC_ASSETS")
        }
        val sku = skuInfo[skuId]
        if (sku == null) {
            Log.record(TAG, "活力兑换🍃找不到要兑换的权益！")
            return false
        }
        try {
            val skuName = sku.getString("skuName")
            val itemStatusList = sku.getJSONArray("itemStatusList")
            for (i in 0 until itemStatusList.length()) {
                val itemStatus = itemStatusList.getString(i)
                val status = ExchangeStatus.valueOf(itemStatus)
                if (status.name == itemStatus) {
                    Log.record(TAG, "活力兑换🍃[" + skuName + "]停止:" + status.nickName)
                    if (ExchangeStatus.REACH_LIMIT.name == itemStatus) {
                        Status.setFlagToday("forest::VitalityExchangeLimit::" + skuId)
                        Log.forest("活力兑换🍃[" + skuName + "]已达上限,停止兑换！")
                    }
                    return false
                }
            }
            val spuId = sku.getString("spuId")
            if (VitalityExchange(spuId, skuId, skuName)) {
                if (skuName.contains("限时")) {
                    Status.setFlagToday("forest::VitalityExchangeLimit::" + skuId)
                }
                return true
            }
            ItemDetailBySpuId(spuId)
        } catch (th: Throwable) {
            Log.record(TAG, "VitalityExchange err")
            Log.printStackTrace(TAG, th)
        }
        return false
    }

    @JvmStatic
    fun VitalityExchange(spuId: String, skuId: String, skuName: String): Boolean {
        try {
            if (VitalityExchange(spuId, skuId)) {
                Status.vitalityExchangeToday(skuId)
                val exchangedCount = Status.getVitalityCount(skuId)
                Log.forest("活力兑换🍃[" + skuName + "]#第" + exchangedCount + "次")
                return true
            }
        } catch (th: Throwable) {
            Log.record(TAG, "VitalityExchange err:" + spuId + "," + skuId)
            Log.printStackTrace(TAG, th)
        }
        return false
    }

    private fun VitalityExchange(spuId: String, skuId: String): Boolean {
        try {
            val jo = JSONObject(runBlocking { AntForestRpcCall.exchangeBenefit(spuId, skuId) })
            if (!jo.optBoolean("success")) {
                val resultCode = jo.optString("resultCode", "")
                if ("QUOTA_USER_NOT_ENOUGH" == resultCode) {
                    Log.forest("活力兑换🍃[兑换次数已达上限]#" + jo.optString("resultDesc", ""))
                    Status.setFlagToday("forest::VitalityExchangeLimit::" + skuId)
                    return false
                }
            }
            return ResChecker.checkRes(TAG + "森林活力值兑换失败:", jo)
        } catch (th: Throwable) {
            Log.record(TAG, "VitalityExchange err:" + spuId + "," + skuId)
            Log.printStackTrace(TAG, th)
        }
        return false
    }

    /**
     * 查找商店道具
     *
     * @param spuName xxx
     */
    @JvmStatic
    fun findSkuInfoBySkuName(spuName: String): JSONObject? {
        try {
            if (skuInfo.isEmpty()) {
                initVitality("SC_ASSETS")
            }
            for (sku in skuInfo.values) {
                if (sku.getString("skuName").contains(spuName)) {
                    return sku
                }
            }
        } catch (e: Exception) {
            Log.record(TAG, "findSkuInfoBySkuName err:")
            Log.printStackTrace(TAG, e)
        }
        return null
    }

    /**
     * 秒杀商品信息
     */
    private data class SecKillItem(
        val skuId: String,
        val spuId: String,
        val skuName: String,
        val secKillStartTime: Long,
        val secKillEndTime: Long,
        val price: Int
    )

    // 已注册定时任务的秒杀商品，防止重复注册
    private val registeredSecKill = java.util.concurrent.ConcurrentHashMap<String, Boolean>()

    /**
     * 扫描当天开启的秒杀活动（含尚未开始与已开始未结束）
     * 通过 com.alipay.antiep.seckill 接口获取秒杀商品列表，
     * 过滤 secKill=true、当天开启、且在秒杀时段内（未结束/未抢完/未达上限）的商品
     */
    private fun scanSecKillActivities(): List<SecKillItem> {
        val secKillList = ArrayList<SecKillItem>()
        try {
            val jo = JSONObject(runBlocking { AntForestRpcCall.secKillActivity() })
            if (!ResChecker.checkRes(TAG + "查询活力值秒杀活动失败:", jo)) {
                return secKillList
            }
            val skuModelList = jo.optJSONArray("secKillSkuModelList") ?: return secKillList
            val now = System.currentTimeMillis()
            for (i in 0 until skuModelList.length()) {
                val skuModel = skuModelList.getJSONObject(i)
                // 只处理秒杀商品
                if (!skuModel.optBoolean("secKill")) continue
                val skuId = skuModel.optString("skuId")
                if (skuId.isEmpty()) continue
                val skuName = skuModel.optString("skuName", skuId)
                val startTime = skuModel.optLong("secKillStartTime", 0)
                val endTime = skuModel.optLong("secKillEndTime", 0)
                // 仅限当天开启的秒杀活动
                if (startTime <= 0 || !TimeUtil.isSameDay(startTime, now)) continue
                // 秒杀已结束则跳过
                if (endTime > 0 && now > endTime) continue
                // 状态为已抢完/已达上限/已结束则跳过
                if (hasUnavailableSecKillStatus(skuModel)) continue
                val price = skuModel.optJSONObject("price")?.optInt("amount") ?: 0
                secKillList.add(
                    SecKillItem(
                        skuId, skuModel.optString("spuId"), skuName, startTime, endTime, price
                    )
                )
            }
        } catch (th: Throwable) {
            Log.record(TAG, "scanSecKillActivities err")
            Log.printStackTrace(TAG, th)
        }
        return secKillList
    }

    /**
     * 判断秒杀商品状态是否不可兑换
     * itemStatusList 含 NO_ENOUGH_STOCK（已抢完）/ REACH_LIMIT（已达上限）/ SECKILL_HAS_END（已结束）时不可抢
     */
    private fun hasUnavailableSecKillStatus(skuModel: JSONObject): Boolean {
        val itemStatusList = skuModel.optJSONArray("itemStatusList") ?: return false
        for (i in 0 until itemStatusList.length()) {
            when (itemStatusList.optString(i)) {
                "NO_ENOUGH_STOCK", "REACH_LIMIT", "SECKILL_HAS_END" -> return true
            }
        }
        return false
    }

    /**
     * 为秒杀活动安排抢购（通用定时任务）
     * 未开始的秒杀在开始前提前 1 秒触发；已开始未结束的立即抢购。
     * 触发后每 0.5 秒请求一次，直到成功或尝试次数超限
     *
     * @param task 父任务（蚂蚁森林），用于 addChildTask 注册定时子任务
     */
    @JvmStatic
    fun scheduleSecKill(task: ModelTask) {
        try {
            val secKillList = scanSecKillActivities()
            if (secKillList.isEmpty()) {
                return
            }
            val now = System.currentTimeMillis()
            for (item in secKillList) {
                // 已注册过则跳过
                if (registeredSecKill.containsKey(item.skuId)) continue
                registeredSecKill[item.skuId] = true
                val taskId = "VITALITY_SEC_KILL|" + item.skuId
                val execTime: Long
                if (item.secKillStartTime > now) {
                    // 尚未开始：提前 1 秒触发
                    execTime = item.secKillStartTime - SEC_KILL_ADVANCE_MILLIS
                    task.addChildTask(
                        ModelTask.ChildModelTask(taskId, "活力值秒杀", {
                            execSecKill(item)
                        }, execTime)
                    )
                    Log.record(TAG, "活力值秒杀🍃[" + item.skuName + "]已安排定时抢购，剩余 " + ((execTime - now) / 1000) + " 秒开始")
                } else {
                    // 已开始未结束：立即抢购（execTime 默认 0，run() 直接走立即执行分支）
                    task.addChildTask(
                        ModelTask.ChildModelTask(
                            taskId,
                            "活力值秒杀",
                            suspendRunnable = { execSecKill(item) }
                        )
                    )
                    Log.record(TAG, "活力值秒杀🍃[" + item.skuName + "]已开始，立即抢购")
                }
            }
        } catch (th: Throwable) {
            Log.record(TAG, "scheduleSecKill err")
            Log.printStackTrace(TAG, th)
        }
    }

    /**
     * 执行秒杀抢购
     * 每 0.5 秒请求一次，直到兑换成功或尝试次数超限
     */
    private suspend fun execSecKill(item: SecKillItem) {
        try {
            var attempt = 0
            while (attempt < SEC_KILL_MAX_ATTEMPTS) {
                attempt++
                Log.record(TAG, "活力值秒杀🍃[" + item.skuName + "]第" + attempt + "次尝试兑换")
                if (execSecKillExchange(item.spuId, item.skuId, item.skuName)) {
                    Log.forest("活力值秒杀🍃[" + item.skuName + "]兑换成功！")
                    return
                }
                delay(SEC_KILL_INTERVAL_MILLIS)
            }
            Log.record(TAG, "活力值秒杀🍃[" + item.skuName + "]尝试" + SEC_KILL_MAX_ATTEMPTS + "次后仍未成功，结束")
        } catch (th: Throwable) {
            Log.record(TAG, "execSecKill err")
            Log.printStackTrace(TAG, th)
        } finally {
            registeredSecKill.remove(item.skuId)
        }
    }

    /**
     * 秒杀兑换 RPC，判断是否兑换成功
     */
    private fun execSecKillExchange(spuId: String, skuId: String, skuName: String): Boolean {
        try {
            val jo = JSONObject(runBlocking { AntForestRpcCall.exchangeSkillBenefit(spuId, skuId) })
            if (jo.optBoolean("success")) {
                Status.vitalityExchangeToday(skuId)
                return true
            }
            val resultCode = jo.optString("resultCode", "")
            if ("QUOTA_USER_NOT_ENOUGH" == resultCode) {
                Status.setFlagToday("forest::VitalityExchangeLimit::" + skuId)
                Log.record(TAG, "活力值秒杀🍃[" + skuName + "]兑换次数已达上限，结束")
                return true
            }
        } catch (th: Throwable) {
            Log.record(TAG, "execSecKillExchange err:" + spuId + "," + skuId)
            Log.printStackTrace(TAG, th)
        }
        return false
    }

    private const val SEC_KILL_ADVANCE_MILLIS: Long = 1000 // 提前 1 秒开始
    private const val SEC_KILL_INTERVAL_MILLIS: Long = 500 // 每 0.5 秒请求一次
    private const val SEC_KILL_MAX_ATTEMPTS: Int = 10 // 最大尝试次数
}
