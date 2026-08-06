package fansirsqi.xposed.sesame.task.greenFinance

import fansirsqi.xposed.sesame.data.Status
import fansirsqi.xposed.sesame.model.ModelFields
import fansirsqi.xposed.sesame.model.ModelGroup
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField
import fansirsqi.xposed.sesame.task.ModelTask
import fansirsqi.xposed.sesame.util.GlobalThreadPools
import fansirsqi.xposed.sesame.core.json.JsonUtil
import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.util.TimeUtil
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TreeMap
import kotlin.math.ceil
import kotlin.math.min

/**
 * @author Constanline
 * @since 2023/09/08
 */
class GreenFinance : ModelTask() {

    private lateinit var greenFinanceLsxd: BooleanModelField
    private lateinit var greenFinanceLsbg: BooleanModelField
    private lateinit var greenFinanceLscg: BooleanModelField
    private lateinit var greenFinanceLswl: BooleanModelField
    private lateinit var greenFinanceWdxd: BooleanModelField
    private lateinit var greenFinanceDonation: BooleanModelField

    /**
     * 是否收取好友金币
     */
    private lateinit var greenFinancePointFriend: BooleanModelField

    override fun getName(): String {
        return "绿色经营"
    }

    override fun getGroup(): ModelGroup {
        return ModelGroup.OTHER
    }

    override fun getIcon(): String {
        return "GreenFinance.png"
    }

    override fun getFields(): ModelFields {
        val modelFields = ModelFields()
        modelFields.addField(
            BooleanModelField("greenFinanceLsxd", "打卡 | 绿色行动", false).also { greenFinanceLsxd = it })
        modelFields.addField(
            BooleanModelField("greenFinanceLscg", "打卡 | 绿色采购", false).also { greenFinanceLscg = it })
        modelFields.addField(
            BooleanModelField("greenFinanceLsbg", "打卡 | 绿色办公", false).also { greenFinanceLsbg = it })
        modelFields.addField(
            BooleanModelField("greenFinanceWdxd", "打卡 | 绿色销售", false).also { greenFinanceWdxd = it })
        modelFields.addField(
            BooleanModelField("greenFinanceLswl", "打卡 | 绿色物流", false).also { greenFinanceLswl = it })
        modelFields.addField(
            BooleanModelField("greenFinancePointFriend", "收取 | 好友金币", false).also { greenFinancePointFriend = it })
        modelFields.addField(
            BooleanModelField("greenFinanceDonation", "捐助 | 快过期金币", false).also { greenFinanceDonation = it })
        return modelFields
    }

    override fun runJava() {
        try {
            Log.record(TAG, "执行开始-" + getName())
            val s = GreenFinanceRpcCall.greenFinanceIndex()
            val jo = JSONObject(s)
            if (!jo.optBoolean("success")) {
                Log.record(TAG, jo.optString("resultDesc"))
                return
            }
            val result = jo.getJSONObject("result")
            if (!result.getBoolean("greenFinanceSigned")) {
                Log.other("绿色经营📊未开通")
                return
            }
            val mcaGreenLeafResult = result.getJSONObject("mcaGreenLeafResult")
            val greenLeafList = mcaGreenLeafResult.getJSONArray("greenLeafList")
            val currentCode = ""
            var bsnIds = JSONArray()
            for (i in 0..<greenLeafList.length()) {
                val greenLeaf = greenLeafList.getJSONObject(i)
                val code = greenLeaf.getString("code")
                if (currentCode == code || bsnIds.length() == 0) {
                    bsnIds.put(greenLeaf.getString("bsnId"))
                } else {
                    batchSelfCollect(bsnIds)
                    bsnIds = JSONArray()
                }
            }
            if (bsnIds.length() > 0) {
                batchSelfCollect(bsnIds)
            }
            signIn("PLAY102632271")
//            signIn("PLAY102932217");
            signIn("PLAY102232206")
            //执行打卡
            behaviorTick()
            //捐助
            donation()
            //收好友金币
            batchStealFriend()
            //评级奖品
            prizes()
            //绿色经营
            doTask("AP13159535", TAG, "绿色经营📊")
            GlobalThreadPools.sleepCompat(500)
        } catch (th: Throwable) {
            Log.printStackTrace(TAG, "index err:", th)
        } finally {
            Log.record(TAG, "执行结束-" + getName())
        }
    }

    /**
     * 批量收取
     *
     * @param bsnIds Ids
     */
    private fun batchSelfCollect(bsnIds: JSONArray) {
        val s = GreenFinanceRpcCall.batchSelfCollect(bsnIds)
        try {
            val joSelfCollect = JSONObject(s)
            if (joSelfCollect.optBoolean("success")) {
                val totalCollectPoint = joSelfCollect.getJSONObject("result").getInt("totalCollectPoint")
                Log.other("绿色经营📊收集获得" + totalCollectPoint)
            } else {
                Log.record(TAG + ".batchSelfCollect", joSelfCollect.optString("resultDesc"))
            }
        } catch (th: Throwable) {
            Log.printStackTrace(TAG, "batchSelfCollect err:", th)
        }
    }

    /**
     * 签到
     *
     * @param sceneId sceneId
     */
    private fun signIn(sceneId: String) {
        try {
            var s = GreenFinanceRpcCall.signInQuery(sceneId)
            var jo = JSONObject(s)
            if (!jo.optBoolean("success")) {
                Log.record(TAG + ".signIn.signInQuery", jo.optString("resultDesc"))
                return
            }
            val result = jo.getJSONObject("result")
            if (result.getBoolean("isTodaySignin")) {
                return
            }
            s = GreenFinanceRpcCall.signInTrigger(sceneId)
            GlobalThreadPools.sleepCompat(300)
            jo = JSONObject(s)
            if (jo.optBoolean("success")) {
                Log.other("绿色经营📊签到成功")
            } else {
                Log.record(TAG + ".signIn.signInTrigger", jo.optString("resultDesc"))
            }
        } catch (th: Throwable) {
            Log.printStackTrace(TAG, "signIn err:", th)
        }
    }

    /**
     * 打卡
     */
    private fun behaviorTick() {
        //绿色行动
        if (greenFinanceLsxd.value) {
            doTick("lsxd")
        }
        //绿色采购
        if (greenFinanceLscg.value) {
            doTick("lscg")
        }
        //绿色物流
        if (greenFinanceLswl.value) {
            doTick("lswl")
        }
        //绿色办公
        if (greenFinanceLsbg.value) {
            doTick("lsbg")
        }
        //绿色销售
        if (greenFinanceWdxd.value) {
            doTick("wdxd")
        }
    }

    /**
     * 打卡绿色行为
     *
     * @param type 打开类型
     */
    private fun doTick(type: String) {
        try {
            var str = GreenFinanceRpcCall.queryUserTickItem(type)
            var jsonObject = JSONObject(str)
            if (!jsonObject.optBoolean("success")) {
                Log.record(TAG + ".doTick.queryUserTickItem", jsonObject.optString("resultDesc"))
                return
            }
            val jsonArray = jsonObject.getJSONArray("result")
            for (i in 0..<jsonArray.length()) {
                jsonObject = jsonArray.getJSONObject(i)
                if ("Y" == jsonObject.getString("status")) {
                    continue
                }
                str = GreenFinanceRpcCall.submitTick(type, jsonObject.getString("behaviorCode"))
                GlobalThreadPools.sleepCompat(1500)
                val obj = JSONObject(str)
                if (!obj.optBoolean("success")
                    || true.toString() != JsonUtil.getValueByPath(obj, "result.result")
                ) {
                    Log.error("绿色经营📊[" + jsonObject.getString("title") + "]打卡失败")
                    break
                }
                Log.other("绿色经营📊[" + jsonObject.getString("title") + "]打卡成功")
//                ThreadUtil.sleep(executeIntervalInt);
            }
        } catch (th: Throwable) {
            Log.printStackTrace(TAG, "doTick err:", th)
        }
    }

    /**
     * 捐助
     */
    private fun donation() {
        if (!greenFinanceDonation.value) {
            return
        }
        try {
            var str = GreenFinanceRpcCall.queryExpireMcaPoint(1)
            GlobalThreadPools.sleepCompat(300)
            var jsonObject = JSONObject(str)
            if (!jsonObject.optBoolean("success")) {
                Log.record(TAG + ".donation.queryExpireMcaPoint", jsonObject.optString("resultDesc"))
                return
            }
            val strAmount = JsonUtil.getValueByPath(jsonObject, "result.expirePoint.amount")
            if (strAmount.isEmpty() || !strAmount.matches(Regex("-?\\d+(\\.\\d+)?"))) {
                return
            }
            val amount = strAmount.toDouble()
            if (amount <= 0) {
                return
            }
            //不管是否可以捐小于非100的倍数了，，第一次捐200，最后按amount-200*n
            Log.other("绿色经营📊1天内过期的金币[" + amount + "]")
            str = GreenFinanceRpcCall.queryAllDonationProjectNew()
            GlobalThreadPools.sleepCompat(300)
            jsonObject = JSONObject(str)
            if (!jsonObject.optBoolean("success")) {
                Log.record(TAG + ".donation.queryAllDonationProjectNew", jsonObject.optString("resultDesc"))
                return
            }
            val result = jsonObject.getJSONArray("result")
            val dicId = TreeMap<String, String>()
            for (i in 0..<result.length()) {
                jsonObject = JsonUtil.getValueByPathObject(
                    result.getJSONObject(i),
                    "mcaDonationProjectResult.[0]"
                ) as? JSONObject ?: continue
                val pId = jsonObject.optString("projectId")
                if (pId.isEmpty()) {
                    continue
                }
                dicId[pId] = jsonObject.optString("projectName")
            }
            val r = calculateDeductions(amount.toInt(), dicId.size)
            var am = "200"
            for (i in 0..<r[0]) {
                val id = ArrayList(dicId.keys)[i]
                val name = dicId[id]
                if (i == r[0] - 1) {
                    am = r[1].toString()
                }
                str = GreenFinanceRpcCall.donation(id, am)
                GlobalThreadPools.sleepCompat(1000)
                jsonObject = JSONObject(str)
                if (!jsonObject.optBoolean("success")) {
                    Log.record(TAG + ".donation." + id, jsonObject.optString("resultDesc"))
                    return
                }
                Log.other("绿色经营📊成功捐助[" + name + "]" + am + "金币")
            }
        } catch (th: Throwable) {
            Log.printStackTrace(TAG, "donation err:", th)
        }
    }

    /**
     * 评级奖品
     */
    private fun prizes() {
        try {
            if (Status.canGreenFinancePrizesMap()) {
                return
            }
            val campId = "CP14664674"
            var str = GreenFinanceRpcCall.queryPrizes(campId)
            var jsonObject = JSONObject(str)
            if (!jsonObject.optBoolean("success")) {
                Log.record(TAG + ".prizes.queryPrizes", jsonObject.optString("resultDesc"))
                return
            }
            val prizes = JsonUtil.getValueByPathObject(jsonObject, "result.prizes") as? JSONArray
            if (prizes != null) {
                for (i in 0..<prizes.length()) {
                    jsonObject = prizes.getJSONObject(i)
                    val bizTime = jsonObject.getString("bizTime")
                    val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                    val dateTime = formatter.parse(bizTime)
                    if (TimeUtil.getWeekNumber(dateTime) == TimeUtil.getWeekNumber(Date())) {
                        Status.greenFinancePrizesMap()
                        return
                    }
                }
            }
            str = GreenFinanceRpcCall.campTrigger(campId)
            jsonObject = JSONObject(str)
            if (!jsonObject.optBoolean("success")) {
                Log.record(TAG + ".prizes.campTrigger", jsonObject.optString("resultDesc"))
                return
            }
            val obj = JsonUtil.getValueByPathObject(jsonObject, "result.prizes.[0]") as? JSONObject
                ?: return
            Log.other("绿色经营🍬评级奖品[" + obj.getString("prizeName") + "]" + obj.getString("price"))
        } catch (th: Throwable) {
            Log.printStackTrace(TAG, "prizes err:", th)
        }
    }

    /**
     * 收好友金币
     */
    private fun batchStealFriend() {
        try {
            if (Status.canGreenFinancePointFriend() || !greenFinancePointFriend.value) {
                return
            }
            var n = 0
            while (true) {
                try {
                    var str = GreenFinanceRpcCall.queryRankingList(n)
                    GlobalThreadPools.sleepCompat(1500)
                    var jsonObject = JSONObject(str)
                    if (!jsonObject.optBoolean("success")) {
                        Log.error("绿色经营🙋，好友金币巡查失败")
                        break
                    }
                    val result = jsonObject.getJSONObject("result")
                    if (result.getBoolean("lastPage")) {
                        Log.other("绿色经营🙋，好友金币巡查完成")
                        Status.greenFinancePointFriend()
                        return
                    }
                    n = result.getInt("nextStartIndex")
                    val list = result.getJSONArray("rankingList")
                    for (i in 0..<list.length()) {
                        val obj = list.getJSONObject(i)
                        if (!obj.getBoolean("collectFlag")) {
                            continue
                        }
                        val friendId = obj.optString("uid")
                        if (friendId.isEmpty()) {
                            continue
                        }
                        str = GreenFinanceRpcCall.queryGuestIndexPoints(friendId)
                        GlobalThreadPools.sleepCompat(1000)
                        jsonObject = JSONObject(str)
                        if (!jsonObject.optBoolean("success")) {
                            Log.record(TAG + ".batchStealFriend.queryGuestIndexPoints", jsonObject.optString("resultDesc"))
                            continue
                        }
                        val points = JsonUtil.getValueByPathObject(jsonObject, "result.pointDetailList") as? JSONArray
                            ?: continue
                        val jsonArray = JSONArray()
                        for (j in 0..<points.length()) {
                            jsonObject = points.getJSONObject(j)
                            if (!jsonObject.getBoolean("collectFlag")) {
                                jsonArray.put(jsonObject.getString("bsnId"))
                            }
                        }
                        if (jsonArray.length() == 0) {
                            continue
                        }
                        str = GreenFinanceRpcCall.batchSteal(jsonArray, friendId)
                        GlobalThreadPools.sleepCompat(1000)
                        jsonObject = JSONObject(str)
                        if (!jsonObject.optBoolean("success")) {
                            Log.record(TAG + ".batchStealFriend.batchSteal", jsonObject.optString("resultDesc"))
                            continue
                        }
                        Log.other(
                            "绿色经营🤩收[" + obj.optString("nickName") + "]" +
                                    JsonUtil.getValueByPath(jsonObject, "result.totalCollectPoint") + "金币"
                        )
                    }
                } catch (e: Exception) {
                    Log.printStackTrace(e)
                    break
                }
            }
        } catch (th: Throwable) {
            Log.printStackTrace(TAG, "batchStealFriend err:", th)
        }
    }

    /**
     * 计算次数和金额
     *
     * @param amount        最小金额
     * @param maxDeductions 最大次数
     * @return [次数，最后一次的金额]
     */
    private fun calculateDeductions(amount: Int, maxDeductions: Int): IntArray {
        if (amount < 200) {
            // 小于 200 时特殊处理
            return intArrayOf(1, 200)
        }
        // 实际扣款次数，不能超过最大次数
        var actualDeductions = min(maxDeductions, ceil(amount.toDouble() / 200).toInt())
        // 剩余金额
        var remainingAmount = amount - actualDeductions * 200
        // 调整剩余金额为 100 的倍数，且不小于 200
        if (remainingAmount % 100 != 0) {
            // 向上取整到最近的 100 倍数
            remainingAmount = ((remainingAmount + 99) / 100) * 100
        }
        if (remainingAmount < 200) {
            remainingAmount = 200
        }
        // 如果调整后的剩余金额需要扣除更多次数，则调整实际扣款次数
        if (remainingAmount < amount - actualDeductions * 200) {
            actualDeductions = (amount - remainingAmount) / 200
        }
        return intArrayOf(actualDeductions, remainingAmount)
    }

    companion object {
        private val TAG: String = GreenFinance::class.java.simpleName

        /**
         * 公共做任务
         * 使用taskQuery查询任务，taskTrigger触发任务（根据taskProcessStatus状态，报名signup->完成send->领奖receive）
         *
         * @param appletId appletId
         * @param tag 类名
         * @param name 中文说明
         */
        @JvmStatic
        fun doTask(appletId: String, tag: String, name: String) {
            try {
                var s = GreenFinanceRpcCall.taskQuery(appletId)
                var jo = JSONObject(s)
                if (!jo.optBoolean("success")) {
                    Log.record(tag + ".doTask.taskQuery", jo.optString("resultDesc"))
                    return
                }
                val result = jo.getJSONObject("result")
                val taskDetailList = result.getJSONArray("taskDetailList")
                for (i in 0..<taskDetailList.length()) {
                    val taskDetail = taskDetailList.getJSONObject(i)
                    //EVENT_TRIGGER、USER_TRIGGER
                    val type = taskDetail.getString("sendCampTriggerType")
                    if ("USER_TRIGGER" != type && "EVENT_TRIGGER" != type) {
                        continue
                    }
                    val status = taskDetail.getString("taskProcessStatus")
                    val taskId = taskDetail.getString("taskId")
                    if ("TO_RECEIVE" == status) {
                        //领取奖品，任务待领奖
                        s = GreenFinanceRpcCall.taskTrigger(taskId, "receive", appletId)
                        jo = JSONObject(s)
                        if (!jo.optBoolean("success")) {
                            Log.record(tag + ".doTask.receive", jo.optString("resultDesc"))
                            continue
                        }
                    } else if ("NONE_SIGNUP" == status) {
                        //没有报名的，先报名，再完成
                        s = GreenFinanceRpcCall.taskTrigger(taskId, "signup", appletId)
                        jo = JSONObject(s)
                        if (!jo.optBoolean("success")) {
                            Log.record(tag + ".doTask.signup", jo.optString("resultDesc"))
                            continue
                        }
                    }
                    if ("SIGNUP_COMPLETE" == status || "NONE_SIGNUP" == status) {
                        //已报名，待完成，去完成
                        s = GreenFinanceRpcCall.taskTrigger(taskId, "send", appletId)
                        jo = JSONObject(s)
                        if (!jo.optBoolean("success")) {
                            Log.record(tag + ".doTask.send", jo.optString("resultDesc"))
                            continue
                        }
                    } else if ("TO_RECEIVE" != status) {
                        continue
                    }
                    //RECEIVE_SUCCESS一次性已完成的
                    Log.other(name + "[" + JsonUtil.getValueByPath(taskDetail, "taskExtProps.TASK_MORPHO_DETAIL.title") + "]任务完成")
                }
            } catch (th: Throwable) {
                Log.printStackTrace(tag, "doTask err:", th)
            }
        }
    }
}
