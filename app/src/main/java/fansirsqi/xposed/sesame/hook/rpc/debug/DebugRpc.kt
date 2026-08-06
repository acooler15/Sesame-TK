package fansirsqi.xposed.sesame.hook.rpc.debug

import fansirsqi.xposed.sesame.hook.RequestManager
import fansirsqi.xposed.sesame.task.reserve.ReserveRpcCall
import fansirsqi.xposed.sesame.util.GlobalThreadPools
import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.core.util.ResChecker
import org.json.JSONException
import org.json.JSONObject

object DebugRpc {
    private const val TAG = "Rpc测试"

    fun getName(): String {
        return "Rpc测试"
    }

    fun start(broadcastFun: String?, broadcastData: String?, testType: String?) {
        Thread {
            // testType!! 保持原 Java switch 对 null 抛 NPE 的行为
            when (testType!!) {
                "Rpc" -> {
                    val s = test(broadcastFun, broadcastData)
                    Log.debug("收到测试消息:\n方法:" + broadcastFun + "\n数据:" + broadcastData + "\n结果:" + s)
                }
                "getNewTreeItems" -> // 获取新树上苗🌱信息
                    getNewTreeItems()
                "getTreeItems" -> // 🔍查询树苗余量
                    getTreeItems()
                "queryAreaTrees" -> queryAreaTrees()
                "getUnlockTreeItems" -> getUnlockTreeItems()
                "walkGrid" -> // 走格子
                    walkGrid()
                else -> Log.debug("未知的测试类型: $testType")
            }
        }.start()
    }

    private fun test(funName: String?, data: String?): String {
        return RequestManager.requestString(funName, data)
    }

    fun queryEnvironmentCertDetailList(alias: String?, pageNum: Int, targetUserID: String?): String {
        return DebugRpcCall.queryEnvironmentCertDetailList(alias, pageNum, targetUserID)
    }

    private fun getNewTreeItems() {
        try {
            val s = ReserveRpcCall.queryTreeItemsForExchange()
            var jo = JSONObject(s)
            if (ResChecker.checkRes(TAG, jo)) {
                val ja = jo.getJSONArray("treeItems")
                for (i in 0 until ja.length()) {
                    jo = ja.getJSONObject(i)
                    if (!jo.has("projectType")) continue
                    if (jo.getString("projectType") != "TREE") continue
                    if (jo.getString("applyAction") != "COMING") continue
                    val projectId = jo.getString("itemId")
                    queryTreeForExchange(projectId)
                }
            } else {
                Log.record(TAG, jo.getString("resultDesc"))
            }
        } catch (t: Throwable) {
            Log.record(TAG, "getTreeItems err:")
            Log.printStackTrace(TAG, t)
        }
    }

    /**
     * 查询特定项目下可交换树木的信息。
     *
     * @param projectId 项目ID
     */
    private fun queryTreeForExchange(projectId: String) {
        try {
            // 调用RPC方法查询树木交换信息
            val response = ReserveRpcCall.queryTreeForExchange(projectId)
            val jo = JSONObject(response)
            // 检查RPC调用结果码是否为"SUCCESS"，表示成功
            if (ResChecker.checkRes(TAG, jo)) {
                // 获取可交换树木的信息
                val exchangeableTree = jo.getJSONObject("exchangeableTree")
                // 获取当前预算
                val currentBudget = exchangeableTree.getInt("currentBudget")
                // 获取区域信息
                val region = exchangeableTree.getString("region")
                // 获取树木名称
                val treeName = exchangeableTree.getString("treeName")
                // 默认提示信息为"不可合种"
                var tips = "不可合种"
                // 检查是否可以合种，如果可以，则更新提示信息
                if (exchangeableTree.optBoolean("canCoexchange", false)) {
                    // 获取合种类型信息
                    val coexchangeTypeIdList =
                        exchangeableTree.getJSONObject("extendInfo").getString("cooperate_template_id_list")
                    tips = "可以合种-合种类型：$coexchangeTypeIdList"
                }
                // 记录查询结果
                Log.debug(TAG, "新树上苗🌱[" + region + "-" + treeName + "]#" + currentBudget + "株-" + tips)
            } else {
                // 如果RPC调用失败，记录错误描述和项目ID
                // 注意：这里应该记录projectId而不是s（响应字符串）
                Log.record(jo.getString("resultDesc") + " projectId: " + projectId)
            }
        } catch (e: JSONException) {
            // 处理JSON解析异常
            Log.record(TAG, "JSON解析错误:")
            Log.printStackTrace(TAG, e)
        } catch (t: Throwable) {
            // 处理其他可能的异常
            Log.record(TAG, "查询树木交换信息过程中发生错误:")
            Log.printStackTrace(TAG, t)
        }
    }

    /**
     * 获取可交换的树木项目列表，并对每个可用的项目查询当前预算。
     */
    private fun getTreeItems() {
        try {
            // 调用RPC方法查询可交换的树木项目列表
            val response = ReserveRpcCall.queryTreeItemsForExchange()
            var jo = JSONObject(response)
            // 检查RPC调用结果码是否为"SUCCESS"，表示成功
            if (ResChecker.checkRes(TAG, jo)) {
                // 获取树木项目列表
                val ja = jo.getJSONArray("treeItems")
                // 遍历项目列表
                for (i in 0 until ja.length()) {
                    // 获取单个项目信息
                    jo = ja.getJSONObject(i)
                    // 如果项目信息中不包含"projectType"字段，则跳过当前项目
                    if (!jo.has("projectType")) continue
                    // 如果项目的应用操作不是"AVAILABLE"，则跳过当前项目
                    if (jo.getString("applyAction") != "AVAILABLE") continue
                    // 获取项目ID和项目名称
                    val projectId = jo.getString("itemId")
                    val itemName = jo.getString("itemName")
                    // 对当前项目查询当前预算
                    getTreeCurrentBudget(projectId, itemName)
                    // 在查询每个项目后暂停100毫秒
                    GlobalThreadPools.sleepCompat(100)
                }
            } else {
                // 如果RPC调用失败，记录错误描述
                Log.record(TAG, jo.getString("resultDesc"))
            }
        } catch (e: JSONException) {
            // 处理JSON解析异常
            Log.record(TAG, "JSON解析错误:")
            Log.printStackTrace(TAG, e)
        } catch (t: Throwable) {
            // 处理其他可能的异常
            Log.record(TAG, "获取树木项目列表过程中发生错误:")
            Log.printStackTrace(TAG, t)
        }
    }

    /**
     * 树苗查询
     *
     * @param projectId 项目ID
     * @param treeName  树木名称
     */
    private fun getTreeCurrentBudget(projectId: String, treeName: String) {
        try {
            // 调用RPC方法查询树木交换信息
            val response = ReserveRpcCall.queryTreeForExchange(projectId)
            val jo = JSONObject(response)
            // 检查RPC调用结果码是否为"SUCCESS"，表示成功
            if (ResChecker.checkRes(TAG, jo)) {
                // 获取可交换树木的信息
                val exchangeableTree = jo.getJSONObject("exchangeableTree")
                // 获取当前预算
                val currentBudget = exchangeableTree.getInt("currentBudget")
                // 获取区域信息
                val region = exchangeableTree.getString("region")
                // 记录树木查询结果
                Log.debug(TAG, "树苗查询🌱[" + region + "-" + treeName + "]#剩余:" + currentBudget)
            } else {
                // 如果RPC调用失败，记录错误描述和项目ID
                Log.record(jo.getString("resultDesc") + " projectId: " + projectId)
            }
        } catch (e: JSONException) {
            // 处理JSON解析异常
            Log.record(TAG, "JSON解析错误:")
            Log.printStackTrace(TAG, e)
        } catch (t: Throwable) {
            // 处理其他可能的异常
            Log.record(TAG, "查询树木交换信息过程中发生错误:")
            Log.printStackTrace(TAG, t)
        }
    }

    /**
     * 模拟网格行走过程，处理行走中的事件，如完成迷你游戏和广告任务。
     */
    private fun walkGrid() {
        try {
            // 调用RPC方法模拟网格行走
            val s = DebugRpcCall.walkGrid()
            var jo = JSONObject(s)
            // 检查RPC调用是否成功
            if (jo.getBoolean("success")) {
                val data = jo.getJSONObject("data")
                // 检查是否有地图奖励
                if (!data.has("mapAwards")) return
                val mapAwards = data.getJSONArray("mapAwards")
                val mapAward = mapAwards.getJSONObject(0)
                // 检查是否有迷你游戏信息
                if (mapAward.has("miniGameInfo")) {
                    val miniGameInfo = mapAward.getJSONObject("miniGameInfo")
                    val gameId = miniGameInfo.getString("gameId")
                    val key = miniGameInfo.getString("key")
                    // 模拟等待迷你游戏完成
                    GlobalThreadPools.sleepCompat(4000L)
                    // 调用RPC方法完成迷你游戏
                    jo = JSONObject(DebugRpcCall.miniGameFinish(gameId, key))
                    // 检查迷你游戏是否完成成功
                    if (jo.getBoolean("success")) {
                        val miniGamedata = jo.getJSONObject("data")
                        // 检查是否有广告任务信息
                        if (miniGamedata.has("adVO")) {
                            val adVO = miniGamedata.getJSONObject("adVO")
                            // 检查是否有广告业务编号
                            if (adVO.has("adBizNo")) {
                                val adBizNo = adVO.getString("adBizNo")
                                // 调用RPC方法完成广告任务
                                jo = JSONObject(DebugRpcCall.taskFinish(adBizNo))
                                // 检查广告任务是否完成成功
                                if (jo.getBoolean("success")) {
                                    // 查询广告任务是否真的完成
                                    jo = JSONObject(DebugRpcCall.queryAdFinished(adBizNo, "NEVERLAND_DOUBLE_AWARD_AD"))
                                    // 检查查询结果是否成功
                                    if (jo.getBoolean("success")) {
                                        Log.farm("完成双倍奖励🎁")
                                    }
                                }
                            }
                        }
                    }
                }
                // 获取剩余行走次数
                val leftCount = data.getInt("leftCount")
                // 如果还有剩余次数，继续行走
                if (leftCount > 0) {
                    GlobalThreadPools.sleepCompat(3000L)
                    walkGrid() // 递归调用，继续行走
                }
            } else {
                // 如果RPC调用失败，记录错误信息
                Log.record(jo.getString("errorMsg") + s)
            }
        } catch (e: JSONException) {
            // 处理JSON解析异常
            Log.record(TAG, "JSON解析错误:")
            Log.printStackTrace(TAG, e)
        } catch (t: Throwable) {
            // 处理其他可能的异常
            Log.record(TAG, "行走网格过程中发生错误:")
            Log.printStackTrace(TAG, t)
        }
    }

    private fun queryAreaTrees() {
        try {
            val jo = JSONObject(ReserveRpcCall.queryAreaTrees())
            if (!ResChecker.checkRes(TAG, jo)) {
                return
            }
            val areaTrees = jo.getJSONObject("areaTrees")
            val regionConfig = jo.getJSONObject("regionConfig")
            val regionKeys = regionConfig.keys()
            while (regionKeys.hasNext()) {
                val regionKey = regionKeys.next()
                if (!areaTrees.has(regionKey)) {
                    val region = regionConfig.getJSONObject(regionKey)
                    val regionName = region.optString("regionName")
                    Log.debug(TAG, "未解锁地区🗺️[$regionName]")
                }
            }
        } catch (t: Throwable) {
            Log.record(TAG, "queryAreaTrees err:")
            Log.printStackTrace(TAG, t)
        }
    }

    private fun getUnlockTreeItems() {
        try {
            val jo = JSONObject(ReserveRpcCall.queryTreeItemsForExchange("", "project"))
            if (!ResChecker.checkRes(TAG, jo)) {
                return
            }
            val ja = jo.getJSONArray("treeItems")
            for (i in 0 until ja.length()) {
                val item = ja.getJSONObject(i)
                if (!item.has("projectType")) continue
                val certCountForAlias = item.optInt("certCountForAlias", -1)
                if (certCountForAlias == 0) {
                    val itemName = item.optString("itemName")
                    val region = item.optString("region")
                    val organization = item.optString("organization")
                    Log.debug(TAG, "未解锁项目🐘[" + region + "-" + itemName + "]#" + organization)
                }
            }
        } catch (t: Throwable) {
            Log.record(TAG, "getUnlockTreeItems err:")
            Log.printStackTrace(TAG, t)
        }
    }
}
