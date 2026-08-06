package fansirsqi.xposed.sesame.task.reserve

import fansirsqi.xposed.sesame.data.Status
import fansirsqi.xposed.sesame.entity.ReserveEntity
import fansirsqi.xposed.sesame.model.ModelFields
import fansirsqi.xposed.sesame.model.ModelGroup
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectAndCountModelField
import fansirsqi.xposed.sesame.task.ModelTask
import fansirsqi.xposed.sesame.util.GlobalThreadPools
import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.core.util.RandomUtil
import fansirsqi.xposed.sesame.core.util.ResChecker
import fansirsqi.xposed.sesame.util.maps.IdMapManager
import fansirsqi.xposed.sesame.util.maps.ReserveMap
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.LinkedHashMap

class Reserve : ModelTask() {

    override fun getName(): String {
        return "保护地"
    }

    override fun getGroup(): ModelGroup {
        return ModelGroup.FOREST
    }

    override fun getIcon(): String {
        return "Reserve.png"
    }

    private lateinit var reserveList: SelectAndCountModelField

    override fun getFields(): ModelFields {
        val modelFields = ModelFields()
        modelFields.addField(
            SelectAndCountModelField(
                "reserveList",
                "保护地列表",
                LinkedHashMap<String?, Int?>(),
                { ReserveEntity.getList() },
                "顾名思义"
            ).also { reserveList = it })
        return modelFields
    }

    override fun runJava() {
        try {
            Log.record(TAG, "开始保护地任务")
            initReserve()
            animalReserve()
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "start.run err:", t)
        } finally {
            Log.record(TAG, "保护地任务")
        }
    }

    private fun animalReserve() {
        try {
            Log.record(TAG, "开始执行-" + getName())
            var s = ReserveRpcCall.queryTreeItemsForExchange()
            if (s == null) {
                GlobalThreadPools.sleepCompat(RandomUtil.delay().toLong())
                s = ReserveRpcCall.queryTreeItemsForExchange()
            }
            var jo = JSONObject(s)
            if (ResChecker.checkRes(TAG, jo)) {
                val ja = jo.getJSONArray("treeItems")
                for (i in 0..<ja.length()) {
                    jo = ja.getJSONObject(i)
                    if (!jo.has("projectType")) {
                        continue
                    }
                    if ("RESERVE" != jo.getString("projectType")) {
                        continue
                    }
                    if ("AVAILABLE" != jo.getString("applyAction")) {
                        continue
                    }
                    val projectId = jo.getString("itemId")
                    val itemName = jo.getString("itemName")
                    val map: Map<String?, Int?> = reserveList.value
                    for ((key, value) in map) {
                        if (key == projectId) {
                            val count = value
                            if (count != null && count > 0 && Status.canReserveToday(projectId, count)) {
                                exchangeTree(projectId, itemName, count)
                            }
                            break
                        }
                    }
                }
            } else {
                Log.record(TAG, jo.getString("resultDesc"))
            }
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "animalReserve err:", t)
        } finally {
            Log.record(TAG, "结束执行-" + getName())
        }
    }

    private fun queryTreeForExchange(projectId: String): Boolean {
        try {
            val s = ReserveRpcCall.queryTreeForExchange(projectId)
            var jo = JSONObject(s)
            if (ResChecker.checkRes(TAG, jo)) {
                val applyAction = jo.getString("applyAction")
                val currentEnergy = jo.getInt("currentEnergy")
                jo = jo.getJSONObject("exchangeableTree")
                if ("AVAILABLE" == applyAction) {
                    if (currentEnergy >= jo.getInt("energy")) {
                        return true
                    } else {
                        Log.forest("领保护地🏕️[" + jo.getString("projectName") + "]#能量不足停止申请")
                        return false
                    }
                } else {
                    Log.forest("领保护地🏕️[" + jo.getString("projectName") + "]#似乎没有了")
                    return false
                }
            } else {
                Log.record(jo.getString("resultDesc"))
                Log.record(s)
            }
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "queryTreeForExchange err:", t)
        }
        return false
    }

    private fun exchangeTree(projectId: String, itemName: String, count: Int) {
        var appliedTimes = 0
        try {
            var s: String
            var jo: JSONObject
            var canApply = queryTreeForExchange(projectId)
            if (!canApply) return
            for (applyCount in 1..count) {
                s = ReserveRpcCall.exchangeTree(projectId)
                jo = JSONObject(s)
                if (ResChecker.checkRes(TAG, jo)) {
                    val vitalityAmount = jo.optInt("vitalityAmount", 0)
                    appliedTimes = Status.getReserveTimes(projectId) + 1
                    val str = "领保护地🏕️[" + itemName + "]#第" + appliedTimes + "次" +
                            (if (vitalityAmount > 0) "-活力值+$vitalityAmount" else "")
                    Log.forest(str)
                    Status.reserveToday(projectId, 1)
                } else {
                    //Log.record(jo.getString("resultDesc"));
                    //Log.runtime(jo.toString());
                    Log.error("领保护地🏕️[" + itemName + "]#发生未知错误，停止申请")
                    // Statistics.reserveToday(projectId, count);
                    break
                }
                GlobalThreadPools.sleepCompat(300)
                canApply = queryTreeForExchange(projectId)
                if (!canApply) {
                    // Statistics.reserveToday(projectId, count);
                    break
                } else {
                    GlobalThreadPools.sleepCompat(300)
                }
                if (!Status.canReserveToday(projectId, count)) break
            }
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "exchangeTree err:", t)
        }
    }

    companion object {
        private val TAG: String = Reserve::class.java.simpleName

        /**
         * 初始化保护地任务。通过 ReserveRpc 接口查询可兑换的树项目，将符合条件的保护地任务存入 ReserveIdMapUtil。 条件：项目类型为 "RESERVE" 且状态为 "AVAILABLE"。若调用失败则加载备份的 ReserveIdMapUtil。
         */
        @JvmStatic
        fun initReserve() {
            try {
                val response = ReserveRpcCall.queryTreeItemsForExchange()
                val jsonResponse = JSONObject(response)
                if (ResChecker.checkRes(TAG, jsonResponse)) {
                    val treeItems = jsonResponse.optJSONArray("treeItems")
                    if (treeItems != null) {
                        for (i in 0..<treeItems.length()) {
                            val item = treeItems.getJSONObject(i)
                            // 跳过未定义 projectType 字段的项目
                            if (!item.has("projectType")) {
                                continue
                            }
                            // 过滤出 projectType 为 "RESERVE" 且 applyAction 为 "AVAILABLE" 的项目
                            if ("RESERVE" == item.getString("projectType") && "AVAILABLE" == item.getString("applyAction")) {
                                // 将符合条件的项目添加到 ReserveIdMapUtil
                                val itemId = item.getString("itemId")
                                val itemName = item.getString("itemName")
                                val energy = item.getInt("energy")
                                IdMapManager.getInstance(ReserveMap::class.java).add(itemId, itemName + "(" + energy + "g)")
                            }
                        }
                        Log.record(TAG, "初始化保护地任务成功。")
                    }
                    // 将筛选结果保存到 ReserveIdMapUtil
                    IdMapManager.getInstance(ReserveMap::class.java).save()
                } else {
                    // 若 resultCode 不为 SUCCESS，记录错误描述
                    Log.error(jsonResponse.optString("resultDesc", "未知错误"))
                }
            } catch (e: JSONException) {
                // 捕获 JSON 解析错误并记录日志
                Log.printStackTrace(TAG, "JSON 解析错误：" + e.message, e)
                IdMapManager.getInstance(ReserveMap::class.java).load() // 若出现异常则加载保存的 ReserveIdMapUtil 备份
            } catch (e: Exception) {
                // 捕获所有其他异常并记录
                Log.printStackTrace(TAG, "初始化保护地任务时出错：" + e.message, e)
                IdMapManager.getInstance(ReserveMap::class.java).load() // 加载备份的 ReserveIdMapUtil
            }
        }
    }
}
