package fansirsqi.xposed.sesame.task.antSports

import fansirsqi.xposed.sesame.core.app.TaskBlacklist
import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.core.store.DataStore
import fansirsqi.xposed.sesame.core.threads.GlobalThreadPools
import fansirsqi.xposed.sesame.core.util.RandomUtil
import fansirsqi.xposed.sesame.core.util.ResChecker
import fansirsqi.xposed.sesame.core.util.TimeUtil
import fansirsqi.xposed.sesame.data.Status
import fansirsqi.xposed.sesame.data.StatusFlags
import org.json.JSONObject

/**
 * @brief 蚂蚁运动：运动任务子模块
 *
 * @details
 * 承载运动任务面板、运动球任务与文体中心任务逻辑，
 * 由主类 {@link AntSports} 委托调用。
 */
internal class SportsTaskManager(private val sports: AntSports) {

    companion object {
        /** @brief 运动任务完成日期缓存键 */
        private const val SPORTS_TASKS_COMPLETED_DATE = "SPORTS_TASKS_COMPLETED_DATE"
    }

    // ---------------------------------------------------------------------
    // 运动任务面板
    // ---------------------------------------------------------------------

    /**
     * @brief 处理运动任务面板中的任务（含签到、完成、领奖）
     */
    internal suspend fun sportsTasks() {
        try {
            sportsCheckIn()
            val jo = JSONObject(AntSportsRpcCall.queryCoinTaskPanel())

            if (ResChecker.checkRes(AntSports.TAG, jo)) {
                val data = jo.getJSONObject("data")
                val taskList = data.getJSONArray("taskList")

                var totalTasks = 0
                var completedTasks = 0
                var availableTasks = 0

                for (i in 0 until taskList.length()) {
                    val taskDetail = taskList.getJSONObject(i)
                    val taskId = taskDetail.getString("taskId")
                    val taskName = taskDetail.getString("taskName")
                    val taskStatus = taskDetail.getString("taskStatus")
                    val taskType = taskDetail.optString("taskType", "")

                    // 排除自动结算任务
                    if (taskType == "SETTLEMENT") continue

                    // 黑名单过滤
                    if (TaskBlacklist.isTaskInBlacklist(taskId) || TaskBlacklist.isTaskInBlacklist(taskName)) {
                        continue
                    }

                    totalTasks++

                    when (taskStatus) {
                        "HAS_RECEIVED" -> {
                            completedTasks++
                        }
                        "WAIT_RECEIVE" -> {
                            if (receiveTaskReward(taskDetail, taskName)) {
                                completedTasks++
                            }
                        }
                        "WAIT_COMPLETE" -> {
                            availableTasks++
                            if (completeTask(taskDetail, taskName)) {
                                completedTasks++
                            }
                        }
                        else -> {
                            Log.error(AntSports.TAG, "做任务得能量🎈[未知状态：$taskName，状态：$taskStatus]")
                        }
                    }
                }

                Log.record(AntSports.TAG, "运动任务完成情况：$completedTasks/$totalTasks，可执行任务：$availableTasks")

                // 所有任务完成后标记
                if (totalTasks > 0 && completedTasks >= totalTasks && availableTasks == 0) {
                    val today = TimeUtil.getDateStr2()
                    DataStore.put(SPORTS_TASKS_COMPLETED_DATE, today)
                    Status.setFlagToday(StatusFlags.FLAG_ANTSPORTS_DAILY_TASKS_DONE)
                    Log.record(AntSports.TAG, "✅ 所有运动任务已完成，今日不再执行")
                }
            }
        } catch (e: Exception) {
            Log.printStackTrace(e)
        }
    }

    /**
     * @brief 领取单个任务奖励
     *
     * @param taskDetail 任务详情 JSON
     * @param taskName   任务名称
     * @return 是否视为成功
     */
    private suspend fun receiveTaskReward(taskDetail: JSONObject, taskName: String): Boolean {
        return try {
            val assetId = taskDetail.getString("assetId")
            val prizeAmount = taskDetail.getInt("prizeAmount").toString()

            val result = AntSportsRpcCall.pickBubbleTaskEnergy(assetId)
            val resultData = JSONObject(result)

            if (ResChecker.checkRes(AntSports.TAG, result)) {
                Log.other("做任务得能量🎈[$taskName] +$prizeAmount 能量")
                true
            } else {
                val errorMsg = resultData.optString("errorMsg", "未知错误")
                val errorCode = resultData.optString("errorCode", "")
                Log.error(AntSports.TAG, "做任务得能量🎈[领取失败：$taskName，错误：$errorCode - $errorMsg]")
                if (!resultData.optBoolean("retryable", true) || errorCode == "CAMP_TRIGGER_ERROR") {
                    true
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            Log.error(AntSports.TAG, "做任务得能量🎈[领取异常：$taskName，错误：${e.message}]")
            false
        }
    }

    /**
     * @brief 执行任务（可能包含多次完成）
     */
    private suspend fun completeTask(taskDetail: JSONObject, taskName: String): Boolean {
        return try {
            val taskId = taskDetail.getString("taskId")
            val prizeAmount = taskDetail.getString("prizeAmount")
            val currentNum = taskDetail.getInt("currentNum")
            val limitConfigNum = taskDetail.getInt("limitConfigNum")
            val remainingNum = limitConfigNum - currentNum
            val needSignUp = taskDetail.optBoolean("needSignUp", false)

            if (remainingNum <= 0) {
                return true
            }

            // 需要先签到
            if (needSignUp) {
                if (!signUpForTask(taskId, taskName)) {
                    return false
                }
                GlobalThreadPools.sleepCompat(2000)
            }

            for (i in 0 until remainingNum) {
                val result = JSONObject(AntSportsRpcCall.completeExerciseTasks(taskId))
                if (ResChecker.checkRes(AntSports.TAG, result)) {
                    Log.record(
                        AntSports.TAG,
                        "做任务得能量🎈[完成任务：$taskName，得$prizeAmount💰]#(${i + 1}/$remainingNum)"
                    )

                    if (i == remainingNum - 1) {
                        GlobalThreadPools.sleepCompat(2000)
                        sports.receiveCoinAsset()
                    }
                } else {
                    val errorMsg = result.optString("errorMsg", "未知错误")
                    Log.error(
                        AntSports.TAG,
                        "做任务得能量🎈[任务失败：$taskName，错误：$errorMsg]#(${i + 1}/$remainingNum)"
                    )
                    val errorCode = result.optString("errorCode", "")
                    if (errorCode.isNotEmpty()) {
                        TaskBlacklist.autoAddToBlacklist(taskId, taskName, errorCode)
                    }
                    break
                }

                if (remainingNum > 1 && i < remainingNum - 1) {
                    GlobalThreadPools.sleepCompat(10000)
                }
            }
            true
        } catch (e: Exception) {
            Log.error(AntSports.TAG, "做任务得能量🎈[执行异常：$taskName，错误：${e.message}]")
            false
        }
    }

    /**
     * @brief 为任务执行报名
     */
    private suspend fun signUpForTask(taskId: String, taskName: String): Boolean {
        return try {
            val result = AntSportsRpcCall.signUpTask(taskId)
            val resultData = JSONObject(result)

            if (ResChecker.checkRes(AntSports.TAG, resultData)) {
                val data = resultData.optJSONObject("data")
                val taskOrderId = data?.optString("taskOrderId", "") ?: ""
                Log.other("做任务得能量🎈[签到成功：$taskName，订单：$taskOrderId]")
                true
            } else {
                val errorMsg = resultData.optString("errorMsg", "未知错误")
                Log.error(AntSports.TAG, "做任务得能量🎈[签到失败：$taskName，错误：$errorMsg]")
                false
            }
        } catch (e: Exception) {
            Log.error(AntSports.TAG, "做任务得能量🎈[签到异常：$taskName，错误：${e.message}]")
            false
        }
    }

    /**
     * @brief 运动首页推荐能量球任务
     *
     * @details
     * - 使用 {@link AntSportsRpcCall#queryEnergyBubbleModule} 获取 recBubbleList
     * - 对有 channel 的记录执行任务
     * - 成功后统一调用 pickBubbleTaskEnergy 领取奖励
     */
    internal suspend fun sportsEnergyBubbleTask() {
        try {
            val jo = JSONObject(AntSportsRpcCall.queryEnergyBubbleModule())
            if (!ResChecker.checkRes(AntSports.TAG, jo)) {
                Log.error(AntSports.TAG, "queryEnergyBubbleModule fail: $jo")
                return
            }

            val data = jo.optJSONObject("data") ?: return
            if (!data.has("recBubbleList")) return

            val recBubbleList = data.optJSONArray("recBubbleList") ?: return
            if (recBubbleList.length() == 0) return

            var hasCompletedTask = false

            for (i in 0 until recBubbleList.length()) {
                val bubble = recBubbleList.optJSONObject(i) ?: continue

                val id = bubble.optString("id")
                val taskId = bubble.optString("channel", "")
                if (taskId.isEmpty()) continue
                if (TaskBlacklist.isTaskInBlacklist(id)) continue

                val sourceName = bubble.optString("simpleSourceName", "")
                val coinAmount = bubble.optInt("coinAmount", 0)
                Log.record(AntSports.TAG, "运动首页任务[开始完成：$sourceName，taskId=$taskId，coin=$coinAmount]")

                val completeRes = JSONObject(AntSportsRpcCall.completeExerciseTasks(taskId))
                if (ResChecker.checkRes(AntSports.TAG, completeRes)) {
                    hasCompletedTask = true
                    val dataObj = completeRes.optJSONObject("data")
                    val assetCoinAmount = dataObj?.optInt("assetCoinAmount", 0) ?: 0
                    Log.other("运动球任务✅[$sourceName]#奖励$assetCoinAmount💰")
                } else {
                    val errorCode = completeRes.optString("errorCode", "")
                    val errorMsg = completeRes.optString("errorMsg", "")
                    Log.error(AntSports.TAG, "运动球任务❌[$sourceName]#$completeRes 任务：$bubble")

                    if (id.isNotEmpty()) {
                        TaskBlacklist.addToBlacklist(id, sourceName)
                    }
                }

                val sleepMs = RandomUtil.nextInt(10000, 30000)
                GlobalThreadPools.sleepCompat(sleepMs.toLong())
            }

            if (hasCompletedTask) {
                val result = AntSportsRpcCall.pickBubbleTaskEnergy()
                val resultJson = JSONObject(result)
                if (ResChecker.checkRes(AntSports.TAG, resultJson)) {
                    val dataObj = resultJson.optJSONObject("data")
                    val balance = dataObj?.optString("balance", "0") ?: "0"
                    Log.other("拾取能量球成功  当前余额: $balance💰")
                } else {
                    Log.error(AntSports.TAG, "领取能量球任务失败: ${resultJson.optString("errorMsg", "未知错误")}")
                }
            } else {
                Log.record(AntSports.TAG, "未完成任何任务，跳过领取能量球")
            }
        } catch (t: Throwable) {
            Log.printStackTrace(AntSports.TAG, "sportsEnergyBubbleTask err:", t)
        }
    }

    /**
     * @brief 运动签到：先 query 再 signIn
     */
    private suspend fun sportsCheckIn() {
        try {
            val queryJo = JSONObject(AntSportsRpcCall.signInCoinTask("query"))
            if (ResChecker.checkRes(AntSports.TAG, queryJo)) {
                val data = queryJo.getJSONObject("data")
                val isSigned = data.getBoolean("signed")

                if (!isSigned) {
                    val signConfigList = data.getJSONArray("signConfigList")
                    for (i in 0 until signConfigList.length()) {
                        val configItem = signConfigList.getJSONObject(i)
                        val toDay = configItem.getBoolean("toDay")
                        val itemSigned = configItem.getBoolean("signed")

                        if (toDay && !itemSigned) {
                            val coinAmount = configItem.getInt("coinAmount")
                            val signJo = JSONObject(AntSportsRpcCall.signInCoinTask("signIn"))
                            if (ResChecker.checkRes(AntSports.TAG, signJo)) {
                                val signData = signJo.getJSONObject("data")
                                val subscribeConfig = if (signData.has("subscribeConfig"))
                                    signData.getJSONObject("subscribeConfig")
                                else JSONObject()

                                val expireDays = if (subscribeConfig.has("subscribeExpireDays"))
                                    subscribeConfig.getString("subscribeExpireDays")
                                else "未知"
                                val toast = if (signData.has("toast")) signData.getString("toast") else ""

                                Log.other(
                                    "做任务得能量🎈[签到${expireDays}天|" +
                                        coinAmount + "能量，" + toast + "💰]"
                                )
                            } else {
                                Log.record(AntSports.TAG, "签到接口调用失败：$signJo")
                            }
                            break
                        }
                    }
                } else {
                    Log.record(AntSports.TAG, "运动签到今日已签到")
                }
            } else {
                Log.record(AntSports.TAG, "查询签到状态失败：$queryJo")
            }
        } catch (e: Exception) {
            Log.printStackTrace(AntSports.TAG, "sportsCheck_in err", e)
        }
    }

    // ---------------------------------------------------------------------
    // 文体中心
    // ---------------------------------------------------------------------

    /**
     * @brief 文体中心任务组查询并自动完成 TODO 状态任务
     */
    internal suspend fun userTaskGroupQuery(groupId: String) {
        try {
            val s = AntSportsRpcCall.userTaskGroupQuery(groupId)
            var jo = JSONObject(s)
            if (ResChecker.checkRes(AntSports.TAG, jo)) {
                jo = jo.getJSONObject("group")
                val userTaskList = jo.getJSONArray("userTaskList")
                for (i in 0 until userTaskList.length()) {
                    jo = userTaskList.getJSONObject(i)
                    if ("TODO" != jo.getString("status")) continue
                    val taskInfo = jo.getJSONObject("taskInfo")
                    val bizType = taskInfo.getString("bizType")
                    val taskId = taskInfo.getString("taskId")
                    val res = JSONObject(AntSportsRpcCall.userTaskComplete(bizType, taskId))
                    if (ResChecker.checkRes(AntSports.TAG, res)) {
                        val taskName = taskInfo.optString("taskName", taskId)
                        Log.other("完成任务🧾[$taskName]")
                    } else {
                        Log.record(AntSports.TAG, "文体每日任务 $res")
                    }
                }
            } else {
                Log.record(AntSports.TAG, "文体每日任务 $s")
            }
        } catch (t: Throwable) {
            Log.printStackTrace(AntSports.TAG, "userTaskGroupQuery err:", t)
        }
    }

    /**
     * @brief 文体中心走路挑战报名
     */
    internal suspend fun participate() {
        try {
            val s = AntSportsRpcCall.queryAccount()
            var jo = JSONObject(s)
            if (ResChecker.checkRes(AntSports.TAG, jo)) {
                val balance = jo.getDouble("balance")
                if (balance < 100) return

                jo = JSONObject(AntSportsRpcCall.queryRoundList())
                if (ResChecker.checkRes(AntSports.TAG, jo)) {
                    val dataList = jo.getJSONArray("dataList")
                    for (i in 0 until dataList.length()) {
                        jo = dataList.getJSONObject(i)
                        if ("P" != jo.getString("status")) continue
                        if (jo.has("userRecord")) continue
                        val instanceList = jo.getJSONArray("instanceList")
                        var pointOptions = 0
                        val roundId = jo.getString("id")
                        var instanceId: String? = null
                        var resultId: String? = null

                        for (j in instanceList.length() - 1 downTo 0) {
                            val inst = instanceList.getJSONObject(j)
                            if (inst.getInt("pointOptions") < pointOptions) continue
                            pointOptions = inst.getInt("pointOptions")
                            instanceId = inst.getString("id")
                            resultId = inst.getString("instanceResultId")
                        }
                        val res = JSONObject(
                            AntSportsRpcCall.participate(
                                pointOptions,
                                instanceId ?: continue,
                                resultId ?: continue,
                                roundId
                            )
                        )
                        if (ResChecker.checkRes(AntSports.TAG, res)) {
                            val data = res.getJSONObject("data")
                            val roundDescription = data.getString("roundDescription")
                            val targetStepCount = data.getInt("targetStepCount")
                            Log.other("走路挑战🚶🏻‍♂️[$roundDescription]#$targetStepCount")
                        } else {
                            Log.record(AntSports.TAG, "走路挑战赛 $res")
                        }
                    }
                } else {
                    Log.record(AntSports.TAG, "queryRoundList $jo")
                }
            }
        } catch (t: Throwable) {
            Log.printStackTrace(AntSports.TAG, "participate err:", t)
        }
    }

    /**
     * @brief 文体中心奖励领取
     */
    internal suspend fun userTaskRightsReceive() {
        try {
            val s = AntSportsRpcCall.userTaskGroupQuery("SPORTS_DAILY_GROUP")
            var jo = JSONObject(s)
            if (ResChecker.checkRes(AntSports.TAG, jo)) {
                jo = jo.getJSONObject("group")
                val userTaskList = jo.getJSONArray("userTaskList")
                for (i in 0 until userTaskList.length()) {
                    jo = userTaskList.getJSONObject(i)
                    if ("COMPLETED" != jo.getString("status")) continue
                    val userTaskId = jo.getString("userTaskId")
                    val taskInfo = jo.getJSONObject("taskInfo")
                    val taskId = taskInfo.getString("taskId")
                    val res = JSONObject(AntSportsRpcCall.userTaskRightsReceive(taskId, userTaskId))
                    if (ResChecker.checkRes(AntSports.TAG, res)) {
                        val taskName = taskInfo.optString("taskName", taskId)
                        val rightsRuleList = taskInfo.getJSONArray("rightsRuleList")
                        val award = StringBuilder()
                        for (j in 0 until rightsRuleList.length()) {
                            val r = rightsRuleList.getJSONObject(j)
                            award.append(r.getString("rightsName"))
                                .append("*")
                                .append(r.getInt("baseAwardCount"))
                        }
                        Log.other("领取奖励🎖️[$taskName]#$award")
                    } else {
                        Log.record(AntSports.TAG, "文体中心领取奖励")
                        Log.record(res.toString())
                    }
                }
            } else {
                Log.record(AntSports.TAG, "文体中心领取奖励")
                Log.record(s)
            }
        } catch (t: Throwable) {
            Log.printStackTrace(AntSports.TAG, "userTaskRightsReceive err:", t)
        }
    }

    /**
     * @brief 文体中心路径特性查询 + 行走任务/加入路径
     */
    internal suspend fun pathFeatureQuery() {
        try {
            val s = AntSportsRpcCall.pathFeatureQuery()
            var jo = JSONObject(s)
            if (ResChecker.checkRes(AntSports.TAG, jo)) {
                val path = jo.getJSONObject("path")
                val pathId = path.getString("pathId")
                val title = path.getString("title")
                val minGoStepCount = path.getInt("minGoStepCount")
                if (jo.has("userPath")) {
                    val userPath = jo.getJSONObject("userPath")
                    val userPathRecordStatus = userPath.getString("userPathRecordStatus")
                    if ("COMPLETED" == userPathRecordStatus) {
                        pathMapHomepage(pathId)
                        pathMapJoin(title, pathId)
                    } else if ("GOING" == userPathRecordStatus) {
                        pathMapHomepage(pathId)
                        val countDate = TimeUtil.getFormatDate()
                        jo = JSONObject(AntSportsRpcCall.stepQuery(countDate, pathId))
                        if (ResChecker.checkRes(AntSports.TAG, jo)) {
                            val canGoStepCount = jo.getInt("canGoStepCount")
                            if (canGoStepCount >= minGoStepCount) {
                                val userPathRecordId = userPath.getString("userPathRecordId")
                                tiyubizGo(countDate, title, canGoStepCount, pathId, userPathRecordId)
                            }
                        }
                    }
                } else {
                    pathMapJoin(title, pathId)
                }
            } else {
                Log.record(AntSports.TAG, jo.getString("resultDesc"))
            }
        } catch (t: Throwable) {
            Log.printStackTrace(AntSports.TAG, "pathFeatureQuery err:", t)
        }
    }

    /**
     * @brief 文体中心地图首页 & 奖励领取
     */
    private suspend fun pathMapHomepage(pathId: String) {
        try {
            val s = AntSportsRpcCall.pathMapHomepage(pathId)
            var jo = JSONObject(s)
            if (ResChecker.checkRes(AntSports.TAG, jo)) {
                if (!jo.has("userPathGoRewardList")) return
                val userPathGoRewardList = jo.getJSONArray("userPathGoRewardList")
                for (i in 0 until userPathGoRewardList.length()) {
                    jo = userPathGoRewardList.getJSONObject(i)
                    if ("UNRECEIVED" != jo.getString("status")) continue
                    val userPathRewardId = jo.getString("userPathRewardId")
                    val res = JSONObject(AntSportsRpcCall.rewardReceive(pathId, userPathRewardId))
                    if (ResChecker.checkRes(AntSports.TAG, res)) {
                        val detail = res.getJSONObject("userPathRewardDetail")
                        val rightsRuleList = detail.getJSONArray("userPathRewardRightsList")
                        val award = StringBuilder()
                        for (j in 0 until rightsRuleList.length()) {
                            val right = rightsRuleList.getJSONObject(j).getJSONObject("rightsContent")
                            award.append(right.getString("name"))
                                .append("*")
                                .append(right.getInt("count"))
                        }
                        Log.other("文体宝箱🎁[$award]")
                    } else {
                        Log.record(AntSports.TAG, "文体中心开宝箱")
                        Log.record(res.toString())
                    }
                }
            } else {
                Log.record(AntSports.TAG, "文体中心开宝箱")
                Log.record(s)
            }
        } catch (t: Throwable) {
            Log.printStackTrace(AntSports.TAG, "pathMapHomepage err:", t)
        }
    }

    /**
     * @brief 文体中心加入路线
     */
    private suspend fun pathMapJoin(title: String, pathId: String) {
        try {
            val jo = JSONObject(AntSportsRpcCall.pathMapJoin(pathId))
            if (ResChecker.checkRes(AntSports.TAG, jo)) {
                Log.other("加入线路🚶🏻‍♂️[$title]")
                pathFeatureQuery()
            } else {
                Log.record(AntSports.TAG, jo.toString())
            }
        } catch (t: Throwable) {
            Log.printStackTrace(AntSports.TAG, "pathMapJoin err:", t)
        }
    }

    /**
     * @brief 文体中心行走逻辑
     */
    private suspend fun tiyubizGo(
        countDate: String,
        title: String,
        goStepCount: Int,
        pathId: String,
        userPathRecordId: String
    ) {
        try {
            val s = AntSportsRpcCall.tiyubizGo(countDate, goStepCount, pathId, userPathRecordId)
            var jo = JSONObject(s)
            if (ResChecker.checkRes(AntSports.TAG, jo)) {
                jo = jo.getJSONObject("userPath")
                Log.other(
                    "行走线路🚶🏻‍♂️[$title]#前进了" +
                        jo.getInt("userPathRecordForwardStepCount") + "步"
                )
                pathMapHomepage(pathId)
                val completed = "COMPLETED" == jo.getString("userPathRecordStatus")
                if (completed) {
                    Log.other("完成线路🚶🏻‍♂️[$title]")
                    pathFeatureQuery()
                }
            } else {
                Log.record(AntSports.TAG, s)
            }
        } catch (t: Throwable) {
            Log.printStackTrace(AntSports.TAG, "tiyubizGo err:", t)
        }
    }
}
