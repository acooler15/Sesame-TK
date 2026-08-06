package fansirsqi.xposed.sesame.task.antOcean

import com.fasterxml.jackson.core.type.TypeReference
import fansirsqi.xposed.sesame.entity.AlipayBeach
import fansirsqi.xposed.sesame.entity.AlipayUser
import fansirsqi.xposed.sesame.hook.Toast
import fansirsqi.xposed.sesame.model.ModelFields
import fansirsqi.xposed.sesame.model.ModelGroup
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.ChoiceModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectAndCountModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectModelField
import fansirsqi.xposed.sesame.task.ModelTask
import fansirsqi.xposed.sesame.task.TaskStatus
import fansirsqi.xposed.sesame.task.antForest.AntForestRpcCall
import fansirsqi.xposed.sesame.util.DataStore
import fansirsqi.xposed.sesame.util.GlobalThreadPools
import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.core.util.ResChecker
import fansirsqi.xposed.sesame.core.util.StringUtil
import fansirsqi.xposed.sesame.util.maps.BeachMap
import fansirsqi.xposed.sesame.util.maps.IdMapManager
import fansirsqi.xposed.sesame.util.maps.UserMap
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author Constanline
 * @since 2023/08/01
 */
class AntOcean : ModelTask() {

    enum class ApplyAction(val code: Int, val desc: String) {
        AVAILABLE(0, "可用"),
        NO_STOCK(1, "无库存"),
        ENERGY_LACK(2, "能量不足");

        companion object {
            @JvmStatic
            fun fromString(value: String?): ApplyAction? {
                for (action in entries) {
                    if (action.name.equals(value, ignoreCase = true)) {
                        return action
                    }
                }
                Log.error("ApplyAction", "Unknown applyAction: $value")
                return null
            }
        }
    }

    interface protectType {
        companion object {
            const val DONT_PROTECT: Int = 0
            const val PROTECT_ALL: Int = 1
            const val PROTECT_BEACH: Int = 2
            val nickNames: Array<String?> = arrayOf("不保护", "保护全部", "仅保护沙滩")
        }
    }

    private lateinit var dailyOceanTask: BooleanModelField
    private lateinit var cleanOcean: BooleanModelField
    private lateinit var cleanOceanType: ChoiceModelField
    private lateinit var cleanOceanList: SelectModelField
    private lateinit var exchangeProp: BooleanModelField
    private lateinit var usePropByType: BooleanModelField
    private lateinit var protectOceanList: SelectAndCountModelField
    private lateinit var PDL_task: BooleanModelField

    private val oceanTaskTryCount: MutableMap<String, AtomicInteger> = ConcurrentHashMap()

    override fun getName(): String {
        return "神奇海洋"
    }

    override fun getGroup(): ModelGroup {
        return ModelGroup.FOREST
    }

    override fun getIcon(): String {
        return "AntOcean.png"
    }

    override fun getFields(): ModelFields {
        val modelFields = ModelFields()
        modelFields.addField(BooleanModelField("dailyOceanTask", "海洋任务", false).also { dailyOceanTask = it })
        modelFields.addField(BooleanModelField("cleanOcean", "清理 | 开启", false).also { cleanOcean = it })
        modelFields.addField(
            ChoiceModelField(
                "cleanOceanType",
                "清理 | 动作",
                CleanOceanType.DONT_CLEAN,
                CleanOceanType.nickNames
            ).also { cleanOceanType = it })
        modelFields.addField(
            SelectModelField(
                "cleanOceanList",
                "清理 | 好友列表",
                LinkedHashSet<String?>()
            ) { AlipayUser.getList() }.also { cleanOceanList = it })
        modelFields.addField(
            BooleanModelField("exchangeProp", "神奇海洋 | 制作万能拼图", false).also { exchangeProp = it })
        modelFields.addField(BooleanModelField("usePropByType", "神奇海洋 | 使用万能拼图", false).also { usePropByType = it })
        modelFields.addField(
            ChoiceModelField(
                "userprotectType",
                "保护 | 类型",
                protectType.DONT_PROTECT,
                protectType.nickNames
            ).also { userprotectType = it })
        modelFields.addField(
            SelectAndCountModelField(
                "protectOceanList",
                "保护 | 海洋列表",
                LinkedHashMap<String?, Int?>()
            ) { AlipayBeach.getList() }.also { protectOceanList = it })
        modelFields.addField(BooleanModelField("PDL_task", "潘多拉任务", false).also { PDL_task = it })
        return modelFields
    }

    override fun runJava() {
        try {
            Log.record(TAG, "执行开始-" + getName())

            if (!queryOceanStatus()) {
                return
            }
            queryHomePage()

            if (dailyOceanTask.value) {
                receiveTaskAward()
            }

            if (userprotectType.value != protectType.DONT_PROTECT) {
                protectOcean()
            }

            if (exchangeProp.value) {
                exchangeProp()
            }
            if (usePropByType.value) {
                usePropByType()
            }

            if (PDL_task.value) {
                doOceanPDLTask()
            }

        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "start.run err:", t)
        } finally {
            Log.record(TAG, "执行结束-" + getName())
        }
    }

    private fun queryOceanStatus(): Boolean {
        try {
            val jo = JSONObject(AntOceanRpcCall.queryOceanStatus())
            if (ResChecker.checkRes(TAG, jo)) {
                if (!jo.getBoolean("opened")) {
                    enableField.value = false
                    Log.record("请先开启神奇海洋,并完成引导教程")
                    return false
                }
                initBeach()
                return true
            }
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "queryOceanStatus err:", t)
        }
        return false
    }

    private fun queryHomePage() {
        try {
            val joHomePage = JSONObject(AntOceanRpcCall.queryHomePage())
            if (ResChecker.checkRes(TAG + "查询海洋主页失败:", joHomePage)) {
                if (joHomePage.has("bubbleVOList")) {
                    collectEnergy(joHomePage.getJSONArray("bubbleVOList"))
                }
                val userInfoVO = joHomePage.getJSONObject("userInfoVO")
                val rubbishNumber = userInfoVO.optInt("rubbishNumber", 0)
                val userId = userInfoVO.getString("userId")
                cleanOcean(userId, rubbishNumber)
                val ipVO = userInfoVO.optJSONObject("ipVO")
                if (ipVO != null) {
                    val surprisePieceNum = ipVO.optInt("surprisePieceNum", 0)
                    if (surprisePieceNum > 0) {
                        ipOpenSurprise()
                    }
                }

                querySeaAreaDetailList()
                queryMiscInfo()
                queryReplicaHome()
                queryUserRanking()

            } else {
                Log.error(TAG, joHomePage.getString("resultDesc"))
            }
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "queryHomePage err:", t)
        }
    }

    private fun queryMiscInfo() {
        try {
            val s = AntOceanRpcCall.queryMiscInfo()
            val jo = JSONObject(s)
            if (ResChecker.checkRes(TAG + "查询海洋杂项信息失败:", jo)) {
                val miscHandlerVOMap = jo.getJSONObject("miscHandlerVOMap")
                val homeTipsRefresh = miscHandlerVOMap.getJSONObject("HOME_TIPS_REFRESH")
                if (homeTipsRefresh.optBoolean("fishCanBeCombined") || homeTipsRefresh.optBoolean("canBeRepaired")) {
                    querySeaAreaDetailList()
                }
                switchOceanChapter()
            } else {
                Log.error(TAG, "查询海洋杂项信息失败" + jo.getString("resultDesc"))
            }
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "queryMiscInfo err:", t)
        }
    }

    private fun switchOceanChapter() {
        var s = AntOceanRpcCall.queryOceanChapterList()
        try {
            var jo = JSONObject(s)
            if (ResChecker.checkRes(TAG + "查询海洋章节列表失败:", jo)) {
                val currentChapterCode = jo.getString("currentChapterCode")
                val chapterVOs = jo.getJSONArray("userChapterDetailVOList")
                var isFinish = false
                var dstChapterCode = ""
                var dstChapterName = ""
                for (i in 0..<chapterVOs.length()) {
                    val chapterVO = chapterVOs.getJSONObject(i)
                    val repairedSeaAreaNum = chapterVO.getInt("repairedSeaAreaNum")
                    val seaAreaNum = chapterVO.getInt("seaAreaNum")
                    if (chapterVO.getString("chapterCode") == currentChapterCode) {
                        isFinish = repairedSeaAreaNum >= seaAreaNum
                    } else {
                        if (repairedSeaAreaNum >= seaAreaNum || !chapterVO.getBoolean("chapterOpen")) {
                            continue
                        }
                        dstChapterName = chapterVO.getString("chapterName")
                        dstChapterCode = chapterVO.getString("chapterCode")
                    }
                }

                if (isFinish && !StringUtil.isEmpty(dstChapterCode)) {
                    Log.record(TAG, "当前海域已完成，等待切换...")
                    GlobalThreadPools.sleepCompat(5000)

                    // 切换动作
                    s = AntOceanRpcCall.switchOceanChapter(dstChapterCode)
                    jo = JSONObject(s)
                    if (ResChecker.checkRes(TAG + "切换海洋章节失败:", jo)) {
                        Log.forest("神奇海洋🌊切换到[" + dstChapterName + "]系列")
                    } else {
                        Log.error(TAG, jo.getString("resultDesc"))
                    }
                }
            } else {
                Log.error(TAG, jo.getString("resultDesc"))
            }
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "switchOceanChapter err:", t)
        }
    }

    private fun querySeaAreaDetailList() {
        try {
            val s = AntOceanRpcCall.querySeaAreaDetailList()
            val jo = JSONObject(s)
            if (ResChecker.checkRes(TAG + "查询海洋区域详情失败:", jo)) {

                // 1. 检查接取
                if (jo.optBoolean("awardSeaAreaCanCreateExtraCollect", false)) {
                    val availableCode = jo.optString("awardSeaAreaCode", "")
                    Log.record(TAG, "发现海域[" + availableCode + "]限时挑战，正在自动接取...")
                    val createRet = AntOceanRpcCall.createSeaAreaExtraCollect()
                    if (ResChecker.checkRes(TAG + "接取限时挑战:", JSONObject(createRet))) {
                        Log.forest("限时挑战🌊接取成功")
                        querySeaAreaDetailList()
                        return
                    }
                }

                val seaAreaNum = jo.getInt("seaAreaNum")
                val fixSeaAreaNum = jo.getInt("fixSeaAreaNum")
                val currentSeaAreaIndex = jo.getInt("currentSeaAreaIndex")
                if (currentSeaAreaIndex < fixSeaAreaNum && seaAreaNum > fixSeaAreaNum) {
                    queryOceanPropList()
                }

                val seaAreaVOs = jo.getJSONArray("seaAreaVOs")
                for (i in 0..<seaAreaVOs.length()) {
                    val seaAreaVO = seaAreaVOs.getJSONObject(i)
                    // 普通鱼
                    val fishVOs = seaAreaVO.optJSONArray("fishVO")
                    if (fishVOs != null) {
                        for (j in 0..<fishVOs.length()) {
                            val fishVO = fishVOs.getJSONObject(j)
                            if (!fishVO.getBoolean("unlock") && "COMPLETED" == fishVO.getString("status")) {
                                val fishId = fishVO.getString("id")
                                combineFish(fishId, "")
                            }
                        }
                    }
                    val seaAreaExtraCollectVO = seaAreaVO.optJSONObject("seaAreaExtraCollectVO")
                    if (seaAreaExtraCollectVO != null) {
                        val extraFishVOs = seaAreaExtraCollectVO.optJSONArray("fishVO")
                        if (extraFishVOs != null) {
                            for (j in 0..<extraFishVOs.length()) {
                                val fishVO = extraFishVOs.getJSONObject(j)
                                if (!fishVO.getBoolean("unlock") && "COMPLETED" == fishVO.optString("status")) {
                                    val fishId = fishVO.getString("id")
                                    val name = fishVO.optString("name", "未知鱼类")
                                    Log.record(TAG, "发现限时挑战鱼类可合成: $name")
                                    combineFish(fishId, "EXTRA_COLLECT")
                                }
                            }
                        }
                    }
                }
            } else {
                Log.error(TAG, jo.getString("resultDesc"))
            }
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "querySeaAreaDetailList err:", t)
        }
    }

    private fun cleanFriendOcean(fillFlag: JSONObject) {
        if (!fillFlag.optBoolean("canClean")) {
            return
        }
        try {
            val userId = fillFlag.getString("userId")
            var isOceanClean = cleanOceanList.value.contains(userId)
            if (cleanOceanType.value == CleanOceanType.DONT_CLEAN) {
                isOceanClean = !isOceanClean
            }
            if (!isOceanClean) {
                return
            }
            var s = AntOceanRpcCall.queryFriendPage(userId)
            var jo = JSONObject(s)
            if (ResChecker.checkRes(TAG + "查询好友海洋页面失败:", jo)) {
                s = AntOceanRpcCall.cleanFriendOcean(userId)
                jo = JSONObject(s)
                Log.forest("神奇海洋🌊[帮助:" + UserMap.getMaskName(userId) + "清理海域]")
                if (ResChecker.checkRes(TAG + "清理好友海洋失败:", jo)) {
                    val cleanRewardVOS = jo.getJSONArray("cleanRewardVOS")
                    checkReward(cleanRewardVOS)
                } else {
                    Log.error(TAG, jo.getString("resultDesc"))
                }
            } else {
                Log.error(TAG, jo.getString("resultDesc"))
            }
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "queryMiscInfo err:", t)
        }
    }

    private fun queryUserRanking() {
        try {
            val s = AntOceanRpcCall.queryUserRanking()
            val jo = JSONObject(s)
            if (ResChecker.checkRes(TAG + "查询海洋用户排行榜失败:", jo)) {
                val fillFlagVOList = jo.getJSONArray("fillFlagVOList")
                for (i in 0..<fillFlagVOList.length()) {
                    val fillFlag = fillFlagVOList.getJSONObject(i)
                    if (cleanOcean.value) {
                        cleanFriendOcean(fillFlag)
                    }
                }
            } else {
                Log.error(TAG, jo.getString("resultDesc"))
            }
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "queryMiscInfo err:", t)
        }
    }

    private fun receiveTaskAward() {
        try {
            val presetBad: MutableSet<String> = LinkedHashSet(listOf("DEMO", "DEMO1"))

            val typeRef: TypeReference<MutableSet<String>> = object : TypeReference<MutableSet<String>>() {}
            val badTaskSet: MutableSet<String> = DataStore.getOrCreate("badOceanTaskSet", typeRef)
            if (badTaskSet.isEmpty()) {
                badTaskSet.addAll(presetBad)
                DataStore.put("badOceanTaskSet", badTaskSet)
            }
            while (true) {
                var done = false
                val s = AntOceanRpcCall.queryTaskList()
                val jo = JSONObject(s)
                if (!ResChecker.checkRes(TAG + "查询海洋任务列表失败:", jo)) {
                    Log.record(TAG, "查询任务列表失败：" + jo.getString("resultDesc"))
                }
                val jaTaskList = jo.getJSONArray("antOceanTaskVOList")
                for (i in 0..<jaTaskList.length()) {
                    val task = jaTaskList.getJSONObject(i)
                    val bizInfo = JSONObject(task.getString("bizInfo"))
                    val taskTitle = bizInfo.optString("taskTitle")
                    val awardCount = bizInfo.optString("awardCount", "0")
                    val sceneCode = task.getString("sceneCode")
                    val taskType = task.getString("taskType")
                    val taskStatus = task.getString("taskStatus")
                    if (TaskStatus.FINISHED.name == taskStatus) {
                        val joAward = JSONObject(AntOceanRpcCall.receiveTaskAward(sceneCode, taskType))
                        if (ResChecker.checkRes(TAG + "领取海洋任务奖励失败:", joAward)) {
                            Log.forest("海洋奖励🌊[" + taskTitle + "]# " + awardCount + "拼图")
                            done = true
                        } else {
                            Log.error(TAG, "海洋奖励🌊领取失败：$joAward")
                        }
                        GlobalThreadPools.sleepCompat(500)
                    } else if (TaskStatus.TODO.name == taskStatus) {
                        if (badTaskSet.contains(taskTitle)) {
                            Log.record(TAG, "海洋任务🌊[" + taskTitle + "]已在黑名单中，跳过处理")
                            continue
                        }
                        if (taskTitle.contains("答题")) {
                            answerQuestion()
                        } else {
                            val bizKey = sceneCode + "_" + taskType
                            val count = oceanTaskTryCount
                                .computeIfAbsent(bizKey) { AtomicInteger(0) }
                                .incrementAndGet()

                            val joFinishTask = JSONObject(AntOceanRpcCall.finishTask(sceneCode, taskType))
                            val errorCode = joFinishTask.optString("code", "")
                            val desc = joFinishTask.optString("desc", "")
                            if ("400000040" == errorCode || desc.contains("不支持RPC完成")) {
                                Log.error(TAG, "海洋任务🌊[" + taskTitle + "]不支持RPC完成，已加入黑名单")
                                badTaskSet.add(taskTitle)
                                DataStore.put("badOceanTaskSet", badTaskSet)
                                continue
                            }
                            if (count > 1) {
                                badTaskSet.add(taskType)
                                DataStore.put("badOceanTaskSet", badTaskSet)
                            } else {
                                if (ResChecker.checkRes(TAG, joFinishTask)) {
                                    Log.forest("海洋任务🌊完成[" + taskTitle + "]")
                                    done = true
                                } else {
                                    Log.error(TAG, "海洋任务🌊完成失败：$joFinishTask")
                                }
                            }

                        }
                        GlobalThreadPools.sleepCompat(500)
                    }
                }
                if (!done) break
            }
        } catch (e: JSONException) {
            Log.printStackTrace(TAG, "JSON解析错误: ", e)
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "receiveTaskAward err:", t)
        }
    }

    private fun protectOcean() {
        try {
            val s = AntOceanRpcCall.queryCultivationList()
            var jo = JSONObject(s)
            if (ResChecker.checkRes(TAG + "查询海洋培育列表失败:", jo)) {
                val ja = jo.getJSONArray("cultivationItemVOList")
                for (i in 0..<ja.length()) {
                    jo = ja.getJSONObject(i)
                    val templateSubType = jo.getString("templateSubType")
                    val applyAction = jo.getString("applyAction")
                    val cultivationName = jo.getString("cultivationName")
                    val templateCode = jo.getString("templateCode")
                    val projectConfig = jo.getJSONObject("projectConfigVO")
                    val projectCode = projectConfig.getString("code")
                    val map: Map<String?, Int?> = protectOceanList.value
                    for ((key, value) in map) {
                        if (key == templateCode) {
                            val count = value
                            if (count != null && count > 0) {
                                oceanExchangeTree(templateCode, projectCode, cultivationName, count)
                            }
                            break
                        }
                    }
                }
            } else {
                Log.error(TAG, jo.getString("resultDesc"))
            }
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "protectBeach err:", t)
        }
    }

    companion object {
        private val TAG: String = AntOcean::class.java.simpleName

        private lateinit var userprotectType: ChoiceModelField

        @JvmStatic
        fun initBeach() {
            try {
                val response = AntOceanRpcCall.queryCultivationList()
                val jsonResponse = JSONObject(response)
                if (ResChecker.checkRes(TAG + "查询种植列表失败:", jsonResponse)) {
                    val cultivationList = jsonResponse.optJSONArray("cultivationItemVOList")
                    if (cultivationList != null) {
                        for (i in 0..<cultivationList.length()) {
                            val item = cultivationList.getJSONObject(i)
                            val templateSubType = item.getString("templateSubType")
                            val actionStr = item.getString("applyAction")
                            val action = ApplyAction.fromString(actionStr)
                            assert(action != null)
                            if (action == ApplyAction.AVAILABLE) {
                                val templateCode = item.getString("templateCode")
                                val cultivationName = item.getString("cultivationName")
                                val energy = item.getInt("energy")
                                when (userprotectType.value) {
                                    protectType.PROTECT_ALL -> IdMapManager.getInstance(BeachMap::class.java)
                                        .add(templateCode, cultivationName + "(" + energy + "g)")

                                    protectType.PROTECT_BEACH -> if (templateSubType != "BEACH") {
                                        IdMapManager.getInstance(BeachMap::class.java)
                                            .add(templateCode, cultivationName + "(" + energy + "g)")
                                    }

                                    else -> {}
                                }
                            }
                        }
                        Log.record(TAG, "初始化沙滩数据成功。")
                    }
                    IdMapManager.getInstance(BeachMap::class.java).save()
                } else {
                    Log.error(TAG, "initBeach" + jsonResponse.optString("resultDesc", "未知错误"))
                }
            } catch (e: JSONException) {
                Log.printStackTrace(TAG, "JSON 解析错误：", e)
                IdMapManager.getInstance(BeachMap::class.java).load()
            } catch (e: Exception) {
                Log.printStackTrace(TAG, "初始化沙滩任务时出错", e)
                IdMapManager.getInstance(BeachMap::class.java).load()
            }
        }

        private fun collectEnergy(bubbleVOList: JSONArray) {
            try {
                for (i in 0..<bubbleVOList.length()) {
                    val bubble = bubbleVOList.getJSONObject(i)
                    if ("ocean" != bubble.getString("channel")) {
                        continue
                    }
                    if ("AVAILABLE" == bubble.getString("collectStatus")) {
                        val bubbleId = bubble.getLong("id")
                        val userId = bubble.getString("userId")
                        val s = AntForestRpcCall.collectEnergy("", userId, bubbleId)
                        val jo = JSONObject(s)
                        if (ResChecker.checkRes(TAG + "收取海洋能量失败:", jo)) {
                            val retBubbles = jo.optJSONArray("bubbles")
                            if (retBubbles != null) {
                                for (j in 0..<retBubbles.length()) {
                                    val retBubble = retBubbles.optJSONObject(j)
                                    if (retBubble != null) {
                                        val collectedEnergy = retBubble.getInt("collectedEnergy")
                                        Log.forest("神奇海洋🌊收取[" + UserMap.getMaskName(userId) + "]#" + collectedEnergy + "g")
                                        Toast.show("海洋能量🌊收取[" + UserMap.getMaskName(userId) + "]#" + collectedEnergy + "g")
                                    }
                                }
                            }
                        } else {
                            Log.error(TAG, jo.getString("resultDesc"))
                        }
                    }
                }
            } catch (t: Throwable) {
                Log.printStackTrace(TAG, "queryHomePage err:", t)
            }
        }

        private fun cleanOcean(userId: String, rubbishNumber: Int) {
            try {
                for (i in 0..<rubbishNumber) {
                    val s = AntOceanRpcCall.cleanOcean(userId)
                    val jo = JSONObject(s)
                    if (ResChecker.checkRes(TAG + "清理海洋失败:", jo)) {
                        val cleanRewardVOS = jo.getJSONArray("cleanRewardVOS")
                        checkReward(cleanRewardVOS)
                        Log.forest("神奇海洋🌊[清理:" + UserMap.getMaskName(userId) + "海域]")
                    } else {
                        Log.error(TAG, jo.getString("resultDesc"))
                    }
                }
            } catch (t: Throwable) {

                Log.printStackTrace(TAG, "cleanOcean err:", t)
            }
        }

        private fun ipOpenSurprise() {
            try {
                val s = AntOceanRpcCall.ipOpenSurprise()
                val jo = JSONObject(s)
                if (ResChecker.checkRes(TAG + "开启海洋惊喜失败:", jo)) {
                    val rewardVOS = jo.getJSONArray("surpriseRewardVOS")
                    checkReward(rewardVOS)
                } else {
                    Log.error(TAG, jo.getString("resultDesc"))
                }
            } catch (t: Throwable) {
                Log.printStackTrace(TAG, "ipOpenSurprise err:", t)
            }
        }

        private fun checkAndCreateExtraCollect() {
            try {
                val s = AntOceanRpcCall.querySeaAreaDetailList()
                val jo = JSONObject(s)
                if (ResChecker.checkRes(TAG + "复查海洋区域详情:", jo)) {
                    if (jo.optBoolean("awardSeaAreaCanCreateExtraCollect", false)) {
                        val availableCode = jo.optString("awardSeaAreaCode", "")
                        Log.record(TAG, "发现海域[" + availableCode + "]限时挑战已就绪！正在接取...")

                        val createRet = AntOceanRpcCall.createSeaAreaExtraCollect()
                        if (ResChecker.checkRes(TAG + "接取限时挑战:", JSONObject(createRet))) {
                            Log.forest("限时挑战🌊接取成功")
                        }
                    }
                }
            } catch (t: Throwable) {
                Log.printStackTrace(TAG, t)
            }
        }

        private fun combineFish(fishId: String, logType: String) {
            try {
                val s = AntOceanRpcCall.combineFish(fishId)
                val jo = JSONObject(s)
                if (ResChecker.checkRes(TAG + "合成海洋鱼类失败:", jo)) {
                    val fishDetailVO = jo.getJSONObject("fishDetailVO")
                    val name = fishDetailVO.getString("name")

                    if ("EXTRA_COLLECT" == logType) {
                        Log.forest("限时挑战🌊[" + name + "]合成成功")
                    } else {
                        Log.forest("神奇海洋🌊[" + name + "]合成成功")
                    }
                    checkAndCreateExtraCollect()
                } else {
                    Log.error(TAG, jo.getString("resultDesc"))
                }
            } catch (t: Throwable) {
                Log.printStackTrace(TAG, "combineFish err:", t)
            }
        }

        private fun checkReward(rewards: JSONArray) {
            try {
                for (i in 0..<rewards.length()) {
                    val reward = rewards.getJSONObject(i)
                    val name = reward.getString("name")
                    val attachReward = reward.getJSONArray("attachRewardBOList")
                    if (attachReward.length() > 0) {
                        Log.forest("神奇海洋🌊[获得:" + name + "碎片]")
                        var canCombine = true
                        for (j in 0..<attachReward.length()) {
                            val detail = attachReward.getJSONObject(j)
                            if (detail.optInt("count", 0) == 0) {
                                canCombine = false
                                break
                            }
                        }
                        if (canCombine && reward.optBoolean("unlock", false)) {
                            val fishId = reward.getString("id")
                            combineFish(fishId, "")
                        }
                    }
                }
            } catch (t: Throwable) {
                Log.printStackTrace(TAG, "checkReward err:", t)
            }
        }

        private fun collectReplicaAsset(canCollectAssetNum: Int) {
            try {
                for (i in 0..<canCollectAssetNum) {
                    val s = AntOceanRpcCall.collectReplicaAsset()
                    val jo = JSONObject(s)
                    if (ResChecker.checkRes(TAG + "收集海洋科普知识失败:", jo)) {
                        Log.forest("神奇海洋🌊[学习海洋科普知识]#潘多拉能量+1")
                    } else {
                        Log.error(TAG, jo.getString("resultDesc"))
                    }
                }
            } catch (t: Throwable) {
                Log.printStackTrace(TAG, "collectReplicaAsset err:", t)
            }
        }

        private fun unLockReplicaPhase(replicaCode: String, replicaPhaseCode: String) {
            try {
                val s = AntOceanRpcCall.unLockReplicaPhase(replicaCode, replicaPhaseCode)
                val jo = JSONObject(s)
                if (ResChecker.checkRes(TAG + "解锁海洋副本阶段失败:", jo)) {
                    val name = jo.getJSONObject("currentPhaseInfo").getJSONObject("extInfo").getString("name")
                    Log.forest("神奇海洋🌊迎回[" + name + "]")
                } else {
                    Log.error(TAG, jo.getString("resultDesc"))
                }
            } catch (t: Throwable) {
                Log.printStackTrace(TAG, "unLockReplicaPhase err:", t)
            }
        }

        private fun queryReplicaHome() {
            try {
                val s = AntOceanRpcCall.queryReplicaHome()
                val jo = JSONObject(s)
                if (ResChecker.checkRes(TAG + "查询海洋副本主页失败:", jo)) {
                    if (jo.has("userReplicaAssetVO")) {
                        val userReplicaAssetVO = jo.getJSONObject("userReplicaAssetVO")
                        val canCollectAssetNum = userReplicaAssetVO.getInt("canCollectAssetNum")
                        collectReplicaAsset(canCollectAssetNum)
                    }
                    if (jo.has("userCurrentPhaseVO")) {
                        val userCurrentPhaseVO = jo.getJSONObject("userCurrentPhaseVO")
                        val phaseCode = userCurrentPhaseVO.getString("phaseCode")
                        val code = jo.getJSONObject("userReplicaInfoVO").getString("code")
                        if ("COMPLETED" == userCurrentPhaseVO.getString("phaseStatus")) {
                            unLockReplicaPhase(code, phaseCode)
                        }
                    }
                } else {
                    Log.error(TAG, jo.getString("resultDesc"))
                }
            } catch (t: Throwable) {
                Log.printStackTrace(TAG, "queryReplicaHome err:", t)
            }
        }

        private fun queryOceanPropList() {
            try {
                val jo = JSONObject(AntOceanRpcCall.queryOceanPropList())
                if (ResChecker.checkRes(TAG + "查询海洋道具列表失败:", jo)) {
                    checkAndCreateExtraCollect()
                    AntOceanRpcCall.repairSeaArea()
                } else {
                    Log.error(TAG, jo.getString("resultDesc"))
                }
            } catch (t: Throwable) {
                Log.printStackTrace(TAG, "queryOceanPropList err:", t)
            }
        }

        private fun answerQuestion() {
            try {
                val questionResponse = AntOceanRpcCall.getQuestion()
                val questionJson = JSONObject(questionResponse)
                if (questionJson.getBoolean("answered")) {
                    Log.record(TAG, "问题已经被回答过，跳过答题流程")
                    return
                }
                if (questionJson.getInt("resultCode") == 200) {
                    val questionId = questionJson.getString("questionId")
                    val options = questionJson.getJSONArray("options")
                    val answer = options.getString(0)
                    val submitResponse = AntOceanRpcCall.submitAnswer(answer, questionId)
                    val submitJson = JSONObject(submitResponse)
                    if (submitJson.getInt("resultCode") == 200) {
                        Log.forest(TAG, "🌊海洋答题成功")
                    } else {
                        Log.error(TAG, "海洋答题失败：$submitJson")
                    }
                } else {
                    Log.error(TAG, "海洋获取问题失败：$questionJson")
                }
            } catch (t: Throwable) {
                Log.printStackTrace(TAG, "海洋答题错误", t)
            }
        }

        private fun doOceanPDLTask() {
            try {
                Log.record(TAG, "执行潘多拉海域任务")
                val homeResponse = AntOceanRpcCall.PDLqueryReplicaHome()
                val homeJson = JSONObject(homeResponse)
                if (ResChecker.checkRes(TAG + "查询潘多拉海洋副本主页失败:", homeJson)) {
                    val taskListResponse = AntOceanRpcCall.PDLqueryTaskList()
                    val taskListJson = JSONObject(taskListResponse)
                    val antOceanTaskVOList = taskListJson.getJSONArray("antOceanTaskVOList")
                    for (i in 0..<antOceanTaskVOList.length()) {
                        val task = antOceanTaskVOList.getJSONObject(i)
                        val taskStatus = task.getString("taskStatus")
                        if ("FINISHED" == taskStatus) {
                            val bizInfoString = task.getString("bizInfo")
                            val bizInfo = JSONObject(bizInfoString)
                            val taskTitle = bizInfo.getString("taskTitle")
                            val awardCount = bizInfo.getInt("awardCount")
                            val taskType = task.getString("taskType")
                            val receiveTaskResponse = AntOceanRpcCall.PDLreceiveTaskAward(taskType)
                            val receiveTaskJson = JSONObject(receiveTaskResponse)
                            val code = receiveTaskJson.getInt("code")
                            if (code == 100000000) {
                                Log.forest("海洋奖励🌊[领取:" + taskTitle + "]获得潘多拉能量x" + awardCount)
                            } else {
                                if (receiveTaskJson.has("message")) {
                                    Log.record(TAG, "领取任务奖励失败: " + receiveTaskJson.getString("message"))
                                } else {
                                    Log.record(TAG, "领取任务奖励失败，未返回错误信息")
                                }
                            }
                        }
                    }
                } else {
                    Log.record(TAG, "PDLqueryReplicaHome调用失败: " + homeJson.optString("message"))
                }
            } catch (t: Throwable) {
                Log.printStackTrace(TAG, "doOceanPDLTask err:", t)
            }
        }

        private fun oceanExchangeTree(cultivationCode: String, projectCode: String, itemName: String, count: Int) {
            try {
                var s: String
                var jo: JSONObject
                var appliedTimes = queryCultivationDetail(cultivationCode, projectCode, count)
                if (appliedTimes < 0) return
                for (applyCount in 1..count) {
                    s = AntOceanRpcCall.oceanExchangeTree(cultivationCode, projectCode)
                    jo = JSONObject(s)
                    if (ResChecker.checkRes(TAG + "海洋兑换树木失败:", jo)) {
                        val awardInfos = jo.getJSONArray("rewardItemVOs")
                        val award = StringBuilder()
                        for (i in 0..<awardInfos.length()) {
                            jo = awardInfos.getJSONObject(i)
                            award.append(jo.getString("name")).append("*").append(jo.getInt("num"))
                        }
                        val str = "保护海洋生态🏖️[" + itemName + "]#第" + appliedTimes + "次" + "-获得奖励" + award
                        Log.forest(str)
                        GlobalThreadPools.sleepCompat(300)
                    } else {
                        Log.error("保护海洋生态🏖️[" + itemName + "]#发生未知错误，停止申请")
                        break
                    }
                    GlobalThreadPools.sleepCompat(300)
                    appliedTimes = queryCultivationDetail(cultivationCode, projectCode, count)
                    if (appliedTimes < 0) {
                        break
                    } else {
                        GlobalThreadPools.sleepCompat(300)
                    }
                }
            } catch (t: Throwable) {
                Log.printStackTrace(TAG, "海洋保护错误:", t)
            }
        }

        private fun queryCultivationDetail(cultivationCode: String, projectCode: String, count: Int): Int {
            var appliedTimes = -1
            try {
                val s = AntOceanRpcCall.queryCultivationDetail(cultivationCode, projectCode)
                var jo = JSONObject(s)
                if (ResChecker.checkRes(TAG + "查询海洋培育详情失败:", jo)) {
                    val userInfo = jo.getJSONObject("userInfoVO")
                    val currentEnergy = userInfo.getInt("currentEnergy")
                    jo = jo.getJSONObject("cultivationDetailVO")
                    val applyAction = jo.getString("applyAction")
                    val certNum = jo.getInt("certNum")
                    if ("AVAILABLE" == applyAction) {
                        if (currentEnergy >= jo.getInt("energy")) {
                            if (certNum < count) {
                                appliedTimes = certNum + 1
                            }
                        } else {
                            Log.forest("保护海洋🏖️[" + jo.getString("cultivationName") + "]#能量不足停止申请")
                        }
                    } else {
                        Log.forest("保护海洋🏖️[" + jo.getString("cultivationName") + "]#似乎没有了")
                    }
                } else {
                    Log.error(jo.getString("resultDesc"))
                }
            } catch (t: Throwable) {
                Log.printStackTrace(TAG, "queryCultivationDetail err:", t)
            }
            return appliedTimes
        }

        private fun exchangeProp() {
            try {
                var shouldContinue = true
                while (shouldContinue) {
                    val propListJson = AntOceanRpcCall.exchangePropList()
                    val propListObj = JSONObject(propListJson)
                    if (ResChecker.checkRes(TAG + "查询海洋道具兑换列表失败:", propListObj)) {
                        val duplicatePieceNum = propListObj.getInt("duplicatePieceNum")
                        if (duplicatePieceNum < 10) {
                            return
                        }
                        val exchangeResultJson = AntOceanRpcCall.exchangeProp()
                        val exchangeResultObj = JSONObject(exchangeResultJson)
                        val exchangedPieceNum = exchangeResultObj.getString("duplicatePieceNum")
                        val exchangeNum = exchangeResultObj.getString("exchangeNum")
                        if (ResChecker.checkRes(TAG + "海洋道具兑换失败:", exchangeResultObj)) {
                            Log.forest("神奇海洋🏖️[万能拼图]制作" + exchangeNum + "张,剩余" + exchangedPieceNum + "张碎片")
                            GlobalThreadPools.sleepCompat(1000)
                        }
                    } else {
                        shouldContinue = false
                    }
                }
            } catch (t: Throwable) {
                Log.printStackTrace(TAG, "exchangeProp error:", t)
            }
        }

        private fun usePropByType() {
            try {
                val propListJson = AntOceanRpcCall.usePropByTypeList()
                val propListObj = JSONObject(propListJson)
                if (ResChecker.checkRes(TAG + "查询海洋道具使用类型列表失败:", propListObj)) {
                    val oceanPropVOByTypeList = propListObj.getJSONArray("oceanPropVOByTypeList")
                    for (i in 0..<oceanPropVOByTypeList.length()) {
                        val propInfo = oceanPropVOByTypeList.getJSONObject(i)
                        var holdsNum = propInfo.getInt("holdsNum")
                        var pageNum = 0
                        th@ while (holdsNum > 0) {
                            pageNum++
                            val fishListJson = AntOceanRpcCall.queryFishList(pageNum)
                            val fishListObj = JSONObject(fishListJson)
                            if (!ResChecker.checkRes(TAG + "查询海洋鱼类列表失败:", fishListObj)) {
                                break
                            }
                            val fishVOS = fishListObj.optJSONArray("fishVOS")
                                ?: break
                            for (j in 0..<fishVOS.length()) {
                                val fish = fishVOS.getJSONObject(j)
                                val pieces = fish.optJSONArray("pieces")
                                    ?: continue
                                val order = fish.getInt("order")
                                val name = fish.getString("name")
                                val idSet: MutableSet<Int> = HashSet()
                                for (k in 0..<pieces.length()) {
                                    val piece = pieces.getJSONObject(k)
                                    if (piece.optInt("num") == 0) {
                                        idSet.add(piece.getString("id").toInt())
                                        holdsNum--
                                        if (holdsNum <= 0) {
                                            break
                                        }
                                    }
                                }
                                if (idSet.isNotEmpty()) {
                                    val usePropResult = AntOceanRpcCall.usePropByType(order, idSet)
                                    val usePropResultObj = JSONObject(usePropResult)
                                    if (ResChecker.checkRes(TAG + "使用海洋万能拼图失败:", usePropResultObj)) {
                                        val userCount = idSet.size
                                        Log.forest("神奇海洋🏖️[万能拼图]使用" + userCount + "张，获得[" + name + "]剩余" + holdsNum + "张")
                                        GlobalThreadPools.sleepCompat(1000)
                                        if (holdsNum <= 0) {
                                            break@th
                                        }
                                    }
                                }
                            }
                            if (!fishListObj.optBoolean("hasMore")) {
                                break
                            }
                        }
                    }
                }
            } catch (t: Throwable) {
                Log.printStackTrace(TAG, "usePropByType error:", t)
            }
        }
    }

    @Suppress("unused")
    interface CleanOceanType {
        companion object {
            const val CLEAN: Int = 0
            const val DONT_CLEAN: Int = 1
            val nickNames: Array<String?> = arrayOf("选中清理", "选中不清理")
        }
    }
}
