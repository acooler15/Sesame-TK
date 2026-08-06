package fansirsqi.xposed.sesame.task.antDodo

import com.fasterxml.jackson.core.type.TypeReference
import fansirsqi.xposed.sesame.entity.AlipayUser
import fansirsqi.xposed.sesame.entity.OtherEntityProvider.listPropGroupOptions
import fansirsqi.xposed.sesame.model.ModelFields
import fansirsqi.xposed.sesame.model.ModelGroup
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.ChoiceModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectModelField
import fansirsqi.xposed.sesame.task.ModelTask
import fansirsqi.xposed.sesame.task.TaskStatus
import fansirsqi.xposed.sesame.util.DataStore
import fansirsqi.xposed.sesame.util.GlobalThreadPools
import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.core.util.ResChecker
import fansirsqi.xposed.sesame.core.util.TimeUtil
import fansirsqi.xposed.sesame.util.maps.UserMap
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.LinkedHashSet

class AntDodo : ModelTask() {

    /**
     * 仅限 AntDodo 内部使用的道具组常量定义
     */
    interface PropGroupType {
        companion object {
            /** 当前图鉴抽卡券 🎴 */
            const val COLLECT_ANIMAL: String = "COLLECT_ANIMAL"

            /** 好友卡抽卡券 👥 */
            const val ADD_COLLECT_TO_FRIEND_LIMIT: String = "ADD_COLLECT_TO_FRIEND_LIMIT"

            /** 万能卡 🃏 */
            const val UNIVERSAL_CARD: String = "UNIVERSAL_CARD"
        }
    }

    override fun getName(): String {
        return "神奇物种"
    }

    override fun getGroup(): ModelGroup {
        return ModelGroup.FOREST
    }

    override fun getIcon(): String {
        return "AntDodo.png"
    }

    private lateinit var collectToFriend: BooleanModelField
    private lateinit var collectToFriendType: ChoiceModelField
    private lateinit var collectToFriendList: SelectModelField
    private lateinit var sendFriendCard: SelectModelField

    private lateinit var usepropGroup: SelectModelField //道具使用类型
    private lateinit var usePropUNIVERSALCARDType: ChoiceModelField //万能卡使用方法

    private lateinit var autoGenerateBook: BooleanModelField

    override fun getFields(): ModelFields {
        val modelFields = ModelFields()
        modelFields.addField(BooleanModelField("collectToFriend", "帮抽卡 | 开启", false).also { collectToFriend = it })
        modelFields.addField(
            ChoiceModelField(
                "collectToFriendType",
                "帮抽卡 | 动作",
                CollectToFriendType.COLLECT,
                CollectToFriendType.nickNames
            ).also { collectToFriendType = it })
        modelFields.addField(
            SelectModelField(
                "collectToFriendList",
                "帮抽卡 | 好友列表",
                LinkedHashSet<String?>()
            ) { AlipayUser.getList() }.also { collectToFriendList = it })
        modelFields.addField(
            SelectModelField(
                "sendFriendCard",
                "送卡片好友列表(当前图鉴所有卡片)",
                LinkedHashSet<String?>()
            ) { AlipayUser.getList() }.also { sendFriendCard = it })

        // 道具组类型：使用你刚刚定义的列表提供者
        modelFields.addField(
            SelectModelField(
                "usepropGroup",
                "使用道具类型",
                LinkedHashSet<String?>(),
                listPropGroupOptions()
            ).also { usepropGroup = it })

        modelFields.addField(
            ChoiceModelField(
                "usePropUNIVERSALCARDType",
                "万能卡 | 使用方式",
                UniversalCardUseType.EXCLUDE_CURRENT,
                UniversalCardUseType.nickNames
            ).also { usePropUNIVERSALCARDType = it })
        modelFields.addField(BooleanModelField("autoGenerateBook", "自动合成图鉴", false).also { autoGenerateBook = it })
        return modelFields
    }

    override fun runJava() {
        try {
            Log.record(TAG, "执行开始-" + getName())
            receiveTaskAward()
            propList()
            collect()
            if (collectToFriend.value) {
                collectToFriend()
            }
            if (autoGenerateBook.value) {
                autoGenerateBook() //自动 兑换
            }
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "start Dodo.run err:", t)
        } finally {
            Log.record(TAG, "执行结束-" + getName())
        }
    }

    /*
     * 神奇物种
     */
    private fun lastDay(endDate: String): Boolean {
        val timeStep = System.currentTimeMillis()
        val endTimeStep = TimeUtil.timeToStamp(endDate)
        return timeStep < endTimeStep && (endTimeStep - timeStep) < 86400000L
    }

    fun in8Days(endDate: String): Boolean {
        val timeStep = System.currentTimeMillis()
        val endTimeStep = TimeUtil.timeToStamp(endDate)
        return timeStep < endTimeStep && (endTimeStep - timeStep) < 691200000L
    }

    private fun collect() {
        try {
            val jo = JSONObject(AntDodoRpcCall.queryAnimalStatus())
            if (ResChecker.checkRes(TAG, jo)) {
                val data = jo.getJSONObject("data")
                if (data.getBoolean("collect")) {
                    Log.record(TAG, "神奇物种卡片今日收集完成！")
                } else {
                    collectAnimalCard()
                }
            } else {
                Log.record(TAG, "collect错误" + jo.getString("resultDesc"))
            }
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "AntDodo Collect err:", t)
        }
    }

    private fun collectAnimalCard() {
        try {
            var jo = JSONObject(AntDodoRpcCall.homePage())
            if (ResChecker.checkRes(TAG, jo)) {
                var data = jo.getJSONObject("data")
                val animalBook = data.getJSONObject("animalBook")
                val bookId = animalBook.getString("bookId")
                val endDate = animalBook.getString("endDate") + " 23:59:59"
                receiveTaskAward()
                if (!in8Days(endDate) || lastDay(endDate)) propList()
                val ja = data.getJSONArray("limit")
                var index = -1
                for (i in 0..<ja.length()) {
                    jo = ja.getJSONObject(i)
                    if ("DAILY_COLLECT" == jo.getString("actionCode")) {
                        index = i
                        break
                    }
                }
                val set: Set<String?> = sendFriendCard.value
                if (index >= 0) {
                    val leftFreeQuota = jo.getInt("leftFreeQuota")
                    for (j in 0..<leftFreeQuota) {
                        jo = JSONObject(AntDodoRpcCall.collect())
                        if (ResChecker.checkRes(TAG, jo)) {
                            data = jo.getJSONObject("data")
                            val animal = data.getJSONObject("animal")
                            val ecosystem = animal.getString("ecosystem")
                            val name = animal.getString("name")
                            Log.forest("神奇物种🦕[$ecosystem]#$name")
                            if (set.isNotEmpty()) {
                                for (userId in set) {
                                    if (UserMap.currentUid != userId) {
                                        val fantasticStarQuantity = animal.optInt("fantasticStarQuantity", 0)
                                        if (fantasticStarQuantity == 3) {
                                            sendCard(animal, userId)
                                        }
                                        break
                                    }
                                }
                            }
                        } else {
                            Log.record(TAG, "collectAnimalCard错误" + jo.getString("resultDesc"))
                        }
                    }
                }
                if (set.isNotEmpty()) {
                    for (userId in set) {
                        if (UserMap.currentUid != userId) {
                            sendAntDodoCard(bookId, userId)
                            break
                        }
                    }
                }
            } else {
                Log.record(TAG, "collectAnimalCard错误2 " + jo.getString("resultDesc"))
            }
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "AntDodo CollectAnimalCard err:", t)
        }
    }

    /**
     * 神奇物种任务
     */
    private fun receiveTaskAward() {
        try {
            // 获取不能完成的任务列表
            val presetBad: MutableSet<String> = LinkedHashSet(listOf("HELP_FRIEND_COLLECT"))
            val typeRef: TypeReference<MutableSet<String>> = object : TypeReference<MutableSet<String>>() {}
            val badTaskSet: MutableSet<String> = DataStore.getOrCreate("badDodoTaskList", typeRef)
            if (badTaskSet.isEmpty()) {
                badTaskSet.addAll(presetBad)
                DataStore.put("badDodoTaskList", badTaskSet)
            }
            while (true) {
                var doubleCheck = false
                val response = AntDodoRpcCall.taskList() // 调用任务列表接口
                val jsonResponse = JSONObject(response) // 解析响应为 JSON 对象
                // 检查响应结果码是否成功
                if (!ResChecker.checkRes(TAG, jsonResponse)) {
                    Log.record(TAG, "查询任务列表失败：" + jsonResponse.getString("resultDesc"))
                    break
                }
                // 获取任务组信息列表
                val taskGroupInfoList = jsonResponse.getJSONObject("data").optJSONArray("taskGroupInfoList")
                    ?: return // 如果任务组为空则返回
                // 遍历每个任务组
                for (i in 0..<taskGroupInfoList.length()) {
                    val antDodoTask = taskGroupInfoList.getJSONObject(i)
                    val taskInfoList = antDodoTask.getJSONArray("taskInfoList") // 获取任务信息列表
                    // 遍历每个任务
                    for (j in 0..<taskInfoList.length()) {
                        val taskInfo = taskInfoList.getJSONObject(j)
                        val taskBaseInfo = taskInfo.getJSONObject("taskBaseInfo") // 获取任务基本信息
                        val bizInfo = JSONObject(taskBaseInfo.getString("bizInfo")) // 获取业务信息
                        val taskType = taskBaseInfo.getString("taskType") // 获取任务类型
                        val taskTitle = bizInfo.optString("taskTitle", taskType) // 获取任务标题
                        val awardCount = bizInfo.optString("awardCount", "1") // 获取奖励数量
                        val sceneCode = taskBaseInfo.getString("sceneCode") // 获取场景代码
                        val taskStatus = taskBaseInfo.getString("taskStatus") // 获取任务状态
                        // 如果任务已完成，领取任务奖励
                        if (TaskStatus.FINISHED.name == taskStatus) {
                            val joAward = JSONObject(
                                AntDodoRpcCall.receiveTaskAward(sceneCode, taskType)
                            ) // 领取奖励请求
                            if (joAward.optBoolean("success")) {
                                doubleCheck = true
                                Log.forest("任务奖励🎖️[$taskTitle]#${awardCount}个")
                            } else {
                                Log.record(TAG, "领取失败，$response") // 记录领取失败信息
                            }
                            Log.record(TAG, joAward.toString()) // 打印奖励响应
                        } else if (TaskStatus.TODO.name == taskStatus) {
                            // 如果任务待完成，处理特定类型的任务
                            if (!badTaskSet.contains(taskType)) {
                                // 尝试完成任务
                                val joFinishTask = JSONObject(
                                    AntDodoRpcCall.finishTask(sceneCode, taskType)
                                ) // 完成任务请求
                                if (joFinishTask.optBoolean("success")) {
                                    Log.forest("物种任务🧾️[$taskTitle]")
                                    doubleCheck = true
                                } else {
                                    Log.record(TAG, "完成任务失败，$taskTitle") // 记录完成任务失败信息
                                    badTaskSet.add(taskType)
                                    DataStore.put("badDodoTaskList", badTaskSet)
                                }
                            }
                        }
                        GlobalThreadPools.sleepCompat(500)
                    }
                }
                if (!doubleCheck) break
            }
        } catch (e: JSONException) {
            Log.printStackTrace(TAG, "神奇物种 JSON解析错误: " + e.message, e)
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "AntDodo ReceiveTaskAward 错误:", t) // 打印异常栈
        }
    }

    fun propList() {
        try {
            // 获取道具列表
            val s = AntDodoRpcCall.propList()
            val jo = JSONObject(s)
            if (!ResChecker.checkRes(TAG, jo)) {
                Log.error(TAG, "获取道具列表失败:$jo")
                return
            }

            val propList = jo.getJSONObject("data").getJSONArray("propList")

            // --- A. 初始进度检查 (针对当前图鉴) ---
            var currentCount = 0
            var totalCount = 0
            try {
                val homeJo = JSONObject(AntDodoRpcCall.homePage())
                val homeData = homeJo.optJSONObject("data")
                if (homeData != null) {
                    currentCount = homeData.optInt("curCollectionCategoryCount")
                    val animalBook = homeData.optJSONObject("animalBook")
                    if (animalBook != null) {
                        totalCount = animalBook.optInt("totalCount")
                    }
                }
            } catch (e: Exception) {
                Log.record(TAG, "获取初始进度失败，将尝试默认抽卡")
            }

            // 标记位：如果一开始就满了，后面 COLLECT_ANIMAL 直接跳过
            var isBookFull = (totalCount > 0 && currentCount >= totalCount)

            // 获取 UI 配置 (用户勾选了哪些类型的道具自动使用)
            val selectedConfigs: Set<String?> = usepropGroup.value ?: return

            for (i in 0..<propList.length()) {
                val prop = propList.getJSONObject(i)
                val config = prop.optJSONObject("propConfig")
                val currentPropGroup = config?.optString("propGroup") ?: ""
                val propType = prop.getString("propType")
                val propIdList = prop.getJSONArray("propIdList")
                val holdsNum = prop.getInt("holdsNum")

                if (holdsNum <= 0) continue

                // --- 逻辑分发 ---

                // 1. 万能卡逻辑 (UNIVERSAL_CARD)
                if (PropGroupType.UNIVERSAL_CARD == currentPropGroup &&
                    selectedConfigs.contains(PropGroupType.UNIVERSAL_CARD)
                ) {
                    if (isBookFull) continue

                    for (j in 0..<propIdList.length()) {
                        val pId = propIdList.getString(j)
                        val animalId = getTargetAnimalIdForUniversalCard() // 你原有的找缺失ID函数
                        if (animalId.isNotEmpty()) {
                            val res = AntDodoRpcCall.consumeProp(pId, propType, animalId)
                            if (ResChecker.checkRes(TAG, res)) {
                                currentCount++ // 万能卡必中新卡
                                if (currentCount >= totalCount) isBookFull = true
                                Log.forest("万能卡使用成功，补全动物ID: $animalId | 进度: $currentCount/$totalCount")
                            }
                            GlobalThreadPools.sleepCompat(2000L)
                        }
                    }
                } else if (PropGroupType.ADD_COLLECT_TO_FRIEND_LIMIT == currentPropGroup &&
                    // 2. 好友抽卡逻辑 (ADD_COLLECT_TO_FRIEND_LIMIT)
                    selectedConfigs.contains(PropGroupType.ADD_COLLECT_TO_FRIEND_LIMIT)
                ) {
                    for (j in 0..<propIdList.length()) {
                        val pId = propIdList.getString(j)
                        val res = AntDodoRpcCall.consumePropForFriend(pId, propType)
                        if (ResChecker.checkRes(TAG, res)) {
                            Log.record(TAG, "成功使用 [好友抽卡道具]")
                        }
                        GlobalThreadPools.sleepCompat(2000L)
                    }
                } else if (PropGroupType.COLLECT_ANIMAL == currentPropGroup &&
                    // 3. 普通抽卡券逻辑 (COLLECT_ANIMAL)
                    selectedConfigs.contains(PropGroupType.COLLECT_ANIMAL)
                ) {
                    for (j in 0..<propIdList.length()) {
                        if (isBookFull) {
                            Log.record(TAG, "图鉴已集满，自动关停后续抽卡动作")
                            break
                        }

                        val pId = propIdList.getString(j)
                        val res = AntDodoRpcCall.consumeProp(pId, propType, null)

                        if (ResChecker.checkRes(TAG, res)) {
                            try {
                                val resJo = JSONObject(res)
                                val data = resJo.optJSONObject("data") ?: continue

                                // 提取道具名
                                val pName = data.optJSONObject("propConfig").optString("propName", "抽卡道具")

                                val useResult = data.optJSONObject("useResult")
                                if (useResult != null) {
                                    val animal = useResult.optJSONObject("animal")
                                    val ecosystem = animal?.optString("ecosystem") ?: "当前特辑"
                                    val animalName = animal?.optString("name") ?: "未知物种"

                                    // 解析是否新卡并更新进度
                                    val collectDetail = useResult.optJSONObject("collectDetail")
                                    val isNew = collectDetail != null && collectDetail.optBoolean("newCard")

                                    if (isNew) {
                                        currentCount++
                                        if (currentCount >= totalCount) isBookFull = true
                                    }

                                    Log.forest(
                                        String.format(
                                            "使用[%s] 抽到: %s-%s%s | 进度: %d/%d",
                                            pName, ecosystem, animalName, (if (isNew) " [新!]" else " (重复)"),
                                            currentCount, totalCount
                                        )
                                    )
                                }
                            } catch (t: Throwable) {
                                Log.printStackTrace(TAG, "解析抽卡结果 JSON 异常", t)
                            }
                        } else {
                            Log.error(TAG, "使用道具请求失败: $res")
                        }
                        GlobalThreadPools.sleepCompat(2000L)
                    }
                }
            }
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "propList 处理异常", t)
        }
    }

    /**
     * 发送神奇物种卡片
     * @param bookId 卡片图鉴ID
     * @param targetUser 目标用户ID
     */
    private fun sendAntDodoCard(bookId: String?, targetUser: String?) {
        try {
            val jo = JSONObject(AntDodoRpcCall.queryBookInfo(bookId))
            if (ResChecker.checkRes(TAG, jo)) {
                val animalForUserList = jo.getJSONObject("data").optJSONArray("animalForUserList")
                for (i in 0..<animalForUserList!!.length()) {
                    val animalForUser = animalForUserList.getJSONObject(i)
                    val count = animalForUser.getJSONObject("collectDetail").optInt("count")
                    if (count <= 0) continue
                    val animal = animalForUser.getJSONObject("animal")
                    for (j in 0..<count) {
                        sendCard(animal, targetUser)
                        GlobalThreadPools.sleepCompat(500L)
                    }
                }
            }
        } catch (th: Throwable) {
            Log.printStackTrace(TAG, "AntDodo SendAntDodoCard err:", th)
        }
    }

    private fun sendCard(animal: JSONObject, targetUser: String?) {
        try {
            val animalId = animal.getString("animalId")
            val ecosystem = animal.getString("ecosystem")
            val name = animal.getString("name")
            val jo = JSONObject(AntDodoRpcCall.social(animalId, targetUser))
            if (ResChecker.checkRes(TAG, jo)) {
                Log.forest("赠送卡片🦕[" + UserMap.getMaskName(targetUser) + "]#" + ecosystem + "-" + name)
            } else {
                Log.record(TAG, "sendCard错误" + jo.getString("resultDesc"))
            }
        } catch (th: Throwable) {
            Log.printStackTrace(TAG, "AntDodo SendCard err:", th)
        }
    }

    private fun collectToFriend() {
        try {
            val jo = JSONObject(AntDodoRpcCall.queryFriend())
            if (!ResChecker.checkRes(TAG, jo)) {
                Log.error(TAG, "神奇物种帮好友抽卡失败：" + jo.getString("resultDesc"))
                return
            }

            // 获取可用次数
            var count = 0
            val limitList = jo.getJSONObject("data").getJSONObject("extend").getJSONArray("limit")
            for (i in 0..<limitList.length()) {
                val limit = limitList.getJSONObject(i)
                if ("COLLECT_TO_FRIEND" == limit.getString("actionCode")) {
                    // 检查是否有开始时间限制
                    if (limit.has("startTime") && limit.getLong("startTime") > System.currentTimeMillis()) {
                        Log.record("神奇物种🦕帮好友抽卡未到开放时间: " + limit.getString("startTimeStr"))
                        return
                    }
                    count = limit.getInt("leftLimit")
                    break
                }
            }

            if (count <= 0) {
                Log.record("神奇物种🦕帮好友抽卡次数已用完")
                return
            }

            // 遍历好友列表
            val friendList = jo.getJSONObject("data").getJSONArray("friends")
            var i = 0
            while (i < friendList.length() && count > 0) {
                val friend = friendList.getJSONObject(i)

                // 跳过今日已帮助的好友
                if (friend.getBoolean("dailyCollect")) {
                    i++
                    continue
                }

                val userId = friend.getString("userId")

                // 判断是否应该帮助该好友
                val inList = collectToFriendList.value.contains(userId)
                val shouldCollect = if (collectToFriendType.value == CollectToFriendType.COLLECT) inList else !inList

                if (!shouldCollect) {
                    i++
                    continue
                }

                // 执行抽卡
                val joTarget = JSONObject(AntDodoRpcCall.collecttarget(userId))
                if (ResChecker.checkRes(TAG, joTarget)) {
                    val ecosystem = joTarget.getJSONObject("data").getJSONObject("animal").getString("ecosystem")
                    val name = joTarget.getJSONObject("data").getJSONObject("animal").getString("name")
                    val userName = UserMap.getMaskName(userId)
                    Log.forest("神奇物种🦕帮好友[${userName}]抽卡[${ecosystem}]#$name")
                    count--
                } else {
                    Log.record(TAG, "collecttarget错误" + joTarget.getString("resultDesc"))
                }
                i++
            }
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "AntDodo CollectHelpFriend err:", t)
        }
    }

    /**
     * 辅助逻辑：获取万能卡要兑换的精准动物ID
     */
    private fun getTargetAnimalIdForUniversalCard(): String {
        try {
            val allBooks = getAllBookList()
            if (allBooks.length() == 0) {
                Log.record(TAG, "万能卡：未获取到任何图鉴数据")
                return ""
            }

            var targetBookId: String
            val strategy = usePropUNIVERSALCARDType.value

            var currentDoingBookId = ""
            var bestOtherBookId = ""
            var maxOtherRate = -1.0

            var bestOverallBookId = ""
            var maxOverallRate = -1.0

            for (i in 0..<allBooks.length()) {
                val book = allBooks.optJSONObject(i) // 使用 opt 防止 null
                if (book == null || isBookFinished(book)) continue

                val result = book.optJSONObject("animalBookResult") ?: continue

                val bookId = result.optString("bookId")
                val status = book.optString("bookStatus")

                // --- 进度解析与计算 ---
                val prog = book.optString("collectProgress", "0/0")
                var rate = 0.0
                try {
                    val p = prog.split("/")
                    if (p.size == 2) {
                        val current = p[0].toDouble()
                        val total = p[1].toDouble()
                        if (total > 0) {
                            rate = current / total
                        }
                    }
                } catch (ignored: Exception) {
                }

                // --- 策略分类收集 ---
                // 1. 识别当前正在进行的 (DOING)
                if ("DOING" == status) {
                    currentDoingBookId = bookId
                } else {
                    // 2. 识别非当前图鉴中进度最高的
                    if (rate > maxOtherRate) {
                        maxOtherRate = rate
                        bestOtherBookId = bookId
                    }
                }

                // 3. 识别全局进度最高的
                if (rate > maxOverallRate) {
                    maxOverallRate = rate
                    bestOverallBookId = bookId
                }
            }

            // --- 逻辑分支匹配 ---
            if (strategy == UniversalCardUseType.EXCLUDE_CURRENT) {
                targetBookId = bestOtherBookId
                Log.record(TAG, "万能卡策略 [排除当前]: 选中非DOING最高进度图鉴 $targetBookId")
            } else if (strategy == UniversalCardUseType.PRIORITY_MAX_PROGRESS) {
                targetBookId = bestOverallBookId
                Log.record(TAG, "万能卡策略 [进度优先]: 选中全局最高进度图鉴 $targetBookId")
            } else {
                // 模式：所有。优先进行中，进行中已满则选最高进度
                targetBookId = if (currentDoingBookId.isNotEmpty()) currentDoingBookId else bestOverallBookId
                Log.record(TAG, "万能卡策略 [全部]: 优先进行中图鉴 $targetBookId")
            }

            if (targetBookId.isEmpty()) return ""

            // --- 查询具体缺失卡片 ---
            val detailJson = AntDodoRpcCall.queryBookInfo(targetBookId)
            val detailObj = JSONObject(detailJson)

            // 增加对 detail 接口返回结果的校验
            if (detailObj.optBoolean("success", false) || "SUCCESS" == detailObj.optString("resultCode")) {
                val data = detailObj.optJSONObject("data")
                val animals = data?.optJSONArray("animalForUserList")

                if (animals != null) {
                    for (i in 0..<animals.length()) {
                        val item = animals.optJSONObject(i) ?: continue

                        val collectDetail = item.optJSONObject("collectDetail")
                        // 只有 collect 为 false 才说明是缺的
                        if (collectDetail != null && !collectDetail.optBoolean("collect", false)) {
                            val animalInfo = item.optJSONObject("animal")
                            if (animalInfo != null) {
                                val animalId = animalInfo.optString("animalId")
                                val name = animalInfo.optString("name")
                                Log.record(TAG, "万能卡目标锁定: $name ($animalId)")
                                return animalId
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.record(TAG, "万能卡逻辑执行失败: " + e.message)
        }
        return ""
    }

    interface CollectToFriendType {
        companion object {
            const val COLLECT: Int = 0
            const val DONT_COLLECT: Int = 1
            val nickNames: Array<String?> = arrayOf("选中帮抽卡", "选中不帮抽卡")
        }
    }

    //万能卡使用方法
    interface UniversalCardUseType {
        companion object {
            /** 所有图鉴都可使用 */
            const val ALL_COLLECTION: Int = 0

            /** 排除当前图鉴 */
            const val EXCLUDE_CURRENT: Int = 1

            /** 优先合成进度最高的图鉴 */
            const val PRIORITY_MAX_PROGRESS: Int = 2

            val nickNames: Array<String?> = arrayOf(
                "所有图鉴",
                "除当前图鉴",
                "优先合成进度最高"
            )
        }
    }

    companion object {
        private val TAG: String = AntDodo::class.java.simpleName

        /**
         * 判断某个图鉴是否已经"完成" (不需要再投入万能卡)
         */
        private fun isBookFinished(book: JSONObject?): Boolean {
            if (book == null) return true

            // 1. 优先判断合成状态：如果已经可以合成或者已经合成，则认为该图鉴已完成
            val medalStatus = book.optString("medalGenerationStatus")
            if ("CAN_GENERATE" == medalStatus || "GENERATED" == medalStatus) {
                return true
            }

            // 2. 判断数字进度：例如 "10/10"
            val progress = book.optString("collectProgress", "")
            if (progress.contains("/")) {
                try {
                    val parts = progress.split("/")
                    if (parts.size == 2) {
                        val current = parts[0].trim().toInt()
                        val total = parts[1].trim().toInt()
                        return current >= total // 只要现有的不小于总数，就不需要万能卡
                    }
                } catch (e: Exception) {
                    return false
                }
            }

            return false
        }

        /* 获取所有图鉴列表*/
        /**
         * 获取完整的图鉴数组 (自动处理翻页合并)
         * @return 包含所有图鉴对象的 JSONArray
         *
         * [
         *   {
         *     "animalBookResult": {
         *       "bookId": "dxmlyBook",
         *       "ecosystem": "东喜马拉雅高山森林生态系统",
         *       "name": "东喜马拉雅高山森林生态系统",
         *       "totalCount": 10,
         *       "magicCount": 1,
         *       "rareCount": 2,
         *       "commonCount": 7
         *       // ..
         *     },
         *     "bookStatus": "END",
         *     "bookCollectedStatus": "NOT_COMPLETED",
         *     "collectProgress": "1/10",
         *     "hasRedDot": false
         *   },
         *   {
         *     "animalBookResult": {
         *       "bookId": "zhbhtbhxcr202503",
         *       "name": "当前正在进行的某个图鉴",
         *       "totalCount": 10
         *       // ...
         *     },
         *     "bookStatus": "GOING",
         *     "bookCollectedStatus": "NOT_COMPLETED",
         *     "collectProgress": "5/10",
         *     "hasRedDot": true
         *   }
         *   // ...
         * ]
         */
        @JvmStatic
        fun getAllBookList(): JSONArray {
            val allBooks = JSONArray()
            var pageStart: String? = null // 首页传 null
            var hasMore = true

            try {
                while (hasMore) {
                    // 调用上面修改后的接口
                    val res = AntDodoRpcCall.queryBookList(64, pageStart)
                    val jo = JSONObject(res)

                    if (!ResChecker.checkRes(TAG, jo)) {
                        Log.error(TAG, "queryBookList 失败: " + jo.optString("resultDesc"))
                        break
                    }

                    val data = jo.optJSONObject("data") ?: break

                    // 1. 提取并合并数据
                    val currentList = data.optJSONArray("bookForUserList")
                    if (currentList != null) {
                        for (i in 0..<currentList.length()) {
                            allBooks.put(currentList.get(i))
                        }
                    }

                    // 2. 判断翻页逻辑
                    hasMore = data.optBoolean("hasMore", false)
                    pageStart = data.optString("nextPageStart", null)

                    // 如果没有更多了，或者 nextPageStart 为空，直接跳出
                    if (!hasMore || pageStart.isNullOrEmpty()) {
                        break
                    }

                    // 稍微控制一下频率
                    GlobalThreadPools.sleepCompat(300)
                }
            } catch (th: Throwable) {
                Log.printStackTrace(TAG, "获取全量图鉴异常", th)
            }
            return allBooks
        }
    }

    /**
     * 自动合成图鉴
     */
    private fun autoGenerateBook() {
        try {
            // 1. 直接获取所有页合并后的完整图鉴数组
            val allBooks = getAllBookList()

            if (allBooks.length() == 0) {
                return
            }

            // 2. 遍历全量数组
            for (i in 0..<allBooks.length()) {
                val bookItem = allBooks.getJSONObject(i)

                // 判断是否可以合成勋章
                if ("CAN_GENERATE" != bookItem.optString("medalGenerationStatus")) {
                    continue
                }

                val animalBookResult = bookItem.optJSONObject("animalBookResult")
                if (animalBookResult == null) {
                    Log.record(TAG, "animalBookResult为空，停止合成")
                    continue
                }

                val bookId = animalBookResult.optString("bookId")
                val ecosystem = animalBookResult.optString("ecosystem")

                // 3. 调用合成接口
                val res = AntDodoRpcCall.generateBookMedal(bookId)
                val genResp = JSONObject(res)

                if (ResChecker.checkRes(TAG, genResp)) {
                    Log.forest("神奇物种🦕合成勋章[$ecosystem]")
                } else {
                    Log.record(TAG, "合成勋章失败[$ecosystem]: " + genResp.optString("resultDesc"))
                }

                // 合成操作建议稍微加一点点延迟，保护接口
                GlobalThreadPools.sleepCompat(300)
            }
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "autoGenerateBook err:", t)
        }
    }
}
