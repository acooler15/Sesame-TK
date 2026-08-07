package fansirsqi.xposed.sesame.task.antMember

import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.core.log.Log.record
import fansirsqi.xposed.sesame.core.threads.CoroutineUtils
import fansirsqi.xposed.sesame.core.util.ResChecker
import fansirsqi.xposed.sesame.data.Status.Companion.canMemberSignInToday
import fansirsqi.xposed.sesame.data.Status.Companion.memberSignInToday
import fansirsqi.xposed.sesame.util.maps.UserMap
import kotlinx.coroutines.delay
import org.json.JSONException
import org.json.JSONObject

internal class MemberPointsManager(private val member: AntMember) {

    /**
     * 会员签到
     */
    /**
     * 会员签到
     */
    internal suspend fun doMemberSign(): Unit = CoroutineUtils.run {
        try {
            if (canMemberSignInToday(UserMap.currentUid)) {
                val s = AntMemberRpcCall.queryMemberSigninCalendar()
                delay(500)
                val jo = JSONObject(s)
                if (ResChecker.checkRes(AntMember.TAG + "会员签到失败:", jo)) {
                    Log.other(
                        "会员签到📅[" + jo.getString("signinPoint") + "积分]#已签到" + jo.getString(
                            "signinSumDay"
                        ) + "天"
                    )
                    memberSignInToday(UserMap.currentUid)
                } else {
                    record(jo.getString("resultDesc"))
                    record(s)
                }
            }
            queryPointCert(1, 8)
        } catch (t: Throwable) {
            Log.printStackTrace(AntMember.TAG, "doMemberSign err:", t)
        }
    }

    /**
     * 会员任务-逛一逛
     * 单次执行 1
     */
    internal suspend fun doAllMemberAvailableTask(): Unit = CoroutineUtils.run {
        try {
            val str = AntMemberRpcCall.queryAllStatusTaskList()
            delay(500)
            val jsonObject = JSONObject(str)
            if (!ResChecker.checkRes(AntMember.TAG, jsonObject)) {
                Log.error(
                    "${AntMember.TAG}.doAllMemberAvailableTask", "会员任务响应失败: " + jsonObject.getString("resultDesc")
                )
                return@run
            }
            if (!jsonObject.has("availableTaskList")) {
                return@run
            }
            val taskList = jsonObject.getJSONArray("availableTaskList")
            for (j in 0 until taskList.length()) {
                val task = taskList.getJSONObject(j)
                processTask(task)
            }
        } catch (t: Throwable) {
            Log.printStackTrace(AntMember.TAG, "doAllMemberAvailableTask err:", t)
        }
    }

    /**
     * 执行会员任务 类型1
     * @param task 单个任务对象
     */
    @Throws(JSONException::class)
    internal suspend fun processTask(task: JSONObject): Unit = CoroutineUtils.run {
        val taskConfigInfo = task.getJSONObject("taskConfigInfo")
        val name = taskConfigInfo.getString("name")
        val id = taskConfigInfo.getLong("id")
        val awardParamPoint = taskConfigInfo.getJSONObject("awardParam").getString("awardParamPoint")
        val targetBusiness = taskConfigInfo.getJSONArray("targetBusiness").getString(0)
        val targetBusinessArray: Array<String?> = targetBusiness.split("#".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (targetBusinessArray.size < 3) {
            Log.error(AntMember.TAG, "processTask target param err:" + targetBusinessArray.contentToString())
            return@run
        }
        val bizType = targetBusinessArray[0]
        val bizSubType = targetBusinessArray[1]
        val bizParam = targetBusinessArray[2]
        delay(16000)
        val str = AntMemberRpcCall.executeTask(bizParam, bizSubType, bizType, id)
        val jo = JSONObject(str)
        if (!ResChecker.checkRes(AntMember.TAG + "执行会员任务失败:", jo)) {
            Log.error(AntMember.TAG, "执行任务失败:" + jo.optString("resultDesc"))
            return@run
        }
        if (checkMemberTaskFinished(id)) {
            Log.other("会员任务🎖️[$name]#获得积分$awardParamPoint")
        }
    }

    /**
     * 查询指定会员任务是否完成
     * @param taskId 任务id
     */
    private suspend fun checkMemberTaskFinished(taskId: Long): Boolean {
        return try {
            val str = AntMemberRpcCall.queryAllStatusTaskList()
            delay(500)
            val jsonObject = JSONObject(str)
            if (!ResChecker.checkRes(AntMember.TAG + "查询会员任务状态失败:", jsonObject)) {
                Log.error(
                    "${AntMember.TAG}.checkMemberTaskFinished", "会员任务响应失败: " + jsonObject.getString("resultDesc")
                )
            }
            if (!jsonObject.has("availableTaskList")) {
                return true
            }
            val taskList = jsonObject.getJSONArray("availableTaskList")
            for (i in 0..<taskList.length()) {
                val taskConfigInfo = taskList.getJSONObject(i).getJSONObject("taskConfigInfo")
                val id = taskConfigInfo.getLong("id")
                if (taskId == id) {
                    return false
                }
            }
            true
        } catch (_: JSONException) {
            false
        }
    }

    /**
     * 会员积分收取
     * @param page 第几页
     * @param pageSize 每页数据条数
     */
    private suspend fun queryPointCert(page: Int, pageSize: Int) {
        try {
            var s = AntMemberRpcCall.queryPointCert(page, pageSize)
            delay(500)
            var jo = JSONObject(s)
            if (ResChecker.checkRes(AntMember.TAG + "查询会员积分证书失败:", jo)) {
                val hasNextPage = jo.getBoolean("hasNextPage")
                val jaCertList = jo.getJSONArray("certList")
                for (i in 0..<jaCertList.length()) {
                    jo = jaCertList.getJSONObject(i)
                    val bizTitle = jo.getString("bizTitle")
                    val id = jo.getString("id")
                    val pointAmount = jo.getInt("pointAmount")
                    s = AntMemberRpcCall.receivePointByUser(id)
                    jo = JSONObject(s)
                    if (ResChecker.checkRes(AntMember.TAG + "会员积分领取失败:", jo)) {
                        Log.other("会员积分🎖️[领取" + bizTitle + "]#" + pointAmount + "积分")
                    } else {
                        record(jo.getString("resultDesc"))
                        record(s)
                    }
                }
                if (hasNextPage) {
                    queryPointCert(page + 1, pageSize)
                }
            } else {
                record(jo.getString("resultDesc"))
                record(s)
            }
        } catch (t: Throwable) {
            Log.printStackTrace(AntMember.TAG, "queryPointCert err:", t)
        }
    }
}
