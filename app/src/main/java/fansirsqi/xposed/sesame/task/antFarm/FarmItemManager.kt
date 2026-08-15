package fansirsqi.xposed.sesame.task.antFarm

import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.core.util.ResChecker
import fansirsqi.xposed.sesame.data.Status
import fansirsqi.xposed.sesame.task.TaskStatus
import kotlinx.coroutines.delay
import org.json.JSONObject

internal class FarmItemManager(private val farm: AntFarm) {

    private var farmTools: Array<AntFarm.FarmTool> = emptyArray()

    /**
     * 加载持有道具信息
     */
    internal suspend fun listFarmTool(): List<AntFarm.FarmTool>? {
        try {
            var jo = JSONObject(AntFarmRpcCall.listFarmTool())
            if (ResChecker.checkRes(AntFarm.TAG, jo)) {
                val jaToolList = jo.getJSONArray("toolList")
                val tempList = mutableListOf<AntFarm.FarmTool>()
                for (i in 0..<jaToolList.length()) {
                    jo = jaToolList.getJSONObject(i)
                    val tool = AntFarm.FarmTool()
                    tool.toolId = jo.optString("toolId", "")
                    tool.toolType = AntFarm.ToolType.valueOf(jo.getString("toolType"))
                    tool.toolCount = jo.getInt("toolCount")
                    tool.toolHoldLimit = jo.optInt("toolHoldLimit", 20)
                    tempList.add(tool)
                }
                farmTools = tempList.toTypedArray()
                return tempList
            }
        } catch (t: Throwable) {
            Log.printStackTrace(AntFarm.TAG, "listFarmTool err:", t)
        }
        return null
    }

    internal val accelerateToolCount: Int
        get() = farmTools.find { it.toolType == AntFarm.ToolType.ACCELERATETOOL }?.toolCount ?: 0

    /**
     * 连续使用加速卡
     *
     * @return true: 使用成功，false: 使用失败
     */
    internal suspend fun useAccelerateTool(): Boolean {
        // 1) 基础开关：外部配置或全局状态限制
        if (!Status.canUseAccelerateTool()) {
            return false
        }
        // 2) 业务上限：命中“今日已达加速上限”标记则直接返回
        if (Status.hasFlagToday("farm::accelerateLimit")) {
            return false
        }
        // 3) 单次/连续逻辑：当未开启“连续使用”且当前已有加速Buff，则不再使用
        if (!farm.useAccelerateToolContinue!!.value && AntFarm.AnimalBuff.ACCELERATING.name == farm.ownerAnimal.animalBuff) {
            return false
        }
        // 4) 同步最新状态，确保消耗速度、已吃量、食槽上限为最新
        farm.syncAnimalStatus(farm.ownerFarmId)

        // 当前小鸡剩余多长时间吃完饲料
        val currentCountdown = farm.countdown?.toDouble() ?: 0.0
        if (currentCountdown <= 0) return false

        var totalFoodHaveEatten = 0.0
        var totalConsumeSpeed = 0.0
        /* 小鸡自己已经吃的食物参数是foodHaveStolen，而不是foodHaveEatten,这是非常关键的问题！
            实际情况是使用加速卡后所吃的饲料才算在foodHaveEatten里，foodHaveEatten即使不使用加速卡也会有个随机？的1以内的值，通常0.1左右，也就是非0
            startEatTime通常是投喂小鸡饲料的时间，但
            小鸡起床后startEatTime（含日期参数的时间）会重新变更为起床的时间，比如6：00起床，而喂食时间实际是昨晚的20：00,startEatTime=20：00,然后小鸡睡觉
            6：00起床，再获取startEatTime则为6：00
            因此剩余饲料量应该使用countdown来进行计算，这是准确的。
         */
        for (animal in farm.animals!!) {
            totalFoodHaveEatten += animal.foodHaveStolen!!
            totalFoodHaveEatten += animal.foodHaveEatten!!
            totalConsumeSpeed += animal.consumeSpeed!!
        }
        // 自己的小鸡每小时消耗的饲料g数
        val  foodConsumePerHour = farm.ownerAnimal.consumeSpeed!! * 60 * 60
        Log.record(
            AntFarm.TAG,
            "加速卡内部计算⏩[totalConsumeSpeed=$totalConsumeSpeed, totalFoodHaveEatten=$totalFoodHaveEatten, limit=${farm.foodInTroughLimitCurrent}]"
        )
        if (totalConsumeSpeed <= 0) return false
        /* 修改为剩余时间大于自定义remainingTime分钟则使用加速卡，也就是说，当你界面上看到的多久之后吃完。目前的逻辑是小于60分钟则不使用加速卡
            这可以避免损失部分时间，但是不利于一次性完成所有任务，因此可以自定义剩余时间，比如设置剩余时间为40（分钟）时，在饲料吃完剩余时间在40
            分钟以上时，比如剩余41分钟，则直接使用加速卡，并进行后续逻辑（把加速卡用完、再游戏改分、再抽抽乐）；但是如果剩余时间是39分钟，则不使用
            加速卡，需等待饲料吃完再次投喂后进入加速卡判断模块继续使用加速卡。
            剩余时间的设置在软件设置里；值为1-59,设置其他值则默认是原逻辑，即60分钟内的不加速。
         */
        var isUseAccelerateTool = false
        var remainingTimeValue = farm.remainingTime.value
        if (remainingTimeValue !in 1..<60){
            remainingTimeValue = 60
            Log.farm("连续使用加速卡加速的剩余时间设置有误，正确值1-59,现不加速剩余时间为1个小时内的饲料")
        }
        // 剩余饲料量应该根据当前吃饲料的总速度 * 剩余时间原计算逻辑是错误的，总速度就是自己的鸡+偷吃的鸡
        var remainingFood = currentCountdown * totalConsumeSpeed
        /* 加速卡逻辑应该是消耗自己小鸡1个小时的食物消耗量，这个量只取决于自己小鸡的食物消耗速度，大约38g左右；
            计算：foodConsumeSpeed（g/s） * 3600 (g)
            因此对于不足一个小时/指定大于剩余时间的加速应该理解为剩余饲料大于这个指定时间的自己小鸡的食物消耗量，
            这种情况下即使有多只偷吃小鸡时也可以按照设置的剩余时间（remainingTime）正确的把加速卡连续使用光。
            也就是说，即使有多只鸡在偷吃/工作，界面上显示还有remainingTime分钟吃完，那使用加速卡也可以加速掉
            剩余食物，然后再次投喂
         */
        /* 1. 定义一个用于记录退出原因的变量，是为了在exitReason == "CONDITION_NOT_MET"，在小鸡饲料剩余时间不足设置
            的remainingTime时进行日志打印，如设置的是40分钟，但是饲料剩余只有30分钟，那打印一下为什么没有把加速卡用完。
         */

        var exitReason = "CONDITION_NOT_MET"
        while (remainingFood >= remainingTimeValue / 60.0 * foodConsumePerHour ) {
            // 检查本地计数器上限，防止无限使用
            if (!Status.canUseAccelerateTool()) {
                Log.record(AntFarm.TAG, "加速卡内部⏩已达到本地使用上限(8次)，停止使用")
                Status.setFlagToday("farm::accelerateLimit")
                exitReason = "REACHED_LIMIT"
                break
            }
            // 可选条件：若勾选“仅心情满值时加速”，且当前心情不为 100，则跳出
            if ((farm.useAccelerateToolWhenMaxEmotion!!.value && farm.finalScore != 100.0)) {
                exitReason = "EMOTION_NOT_MAX"
                break
            }
            if (useFarmTool(farm.ownerFarmId, AntFarm.ToolType.ACCELERATETOOL)) {
                // 用了一张加速卡，那剩余饲料减少自己小鸡1个小时的饲料消耗量，如前述38g左右
                remainingFood -= foodConsumePerHour
                isUseAccelerateTool = true
                Status.useAccelerateTool()
                val timeLeft = remainingFood / totalConsumeSpeed
                if (timeLeft >= 0.0){
                    Log.farm("使用了1张加速卡⏩ 预估剩余时间: ${(timeLeft/60).toInt()} 分钟")
                    // 打印用了几张加速卡
                    Log.farm("今日已使用${Status.INSTANCE.useAccelerateToolCount}张加速卡")
                    delay(1000)
                } else{
                    /* timeLeft也就是饲料剩余时间，小于0则说明饲料吃完了，直接进行投喂，这样可以在一次任务里完成加速
                        卡的使用。如果加速后吃完了，尝试补喂并刷新倒计时。等待8秒是为了防止计算结果的细微差异引起投喂失败
                     */
                    Log.farm("使用加速卡后小鸡饲料吃完，等待8秒后尝试喂鸡")
                    delay(8000)
                    // 等8秒刷新一下小鸡状态，确认是真的处于饥饿状态
                    farm.syncAnimalStatus(farm.ownerFarmId)
                    if (AntFarm.AnimalFeedStatus.HUNGRY.name == farm.ownerAnimal.animalFeedStatus) {
                        if (farm.feedManager.feedAnimal(farm.ownerFarmId)) {
                            // 这里似乎不用在刷新了
                            farm.syncAnimalStatus(farm.ownerFarmId)
                            // 投喂成功后剩余食物变成了180g
                            remainingFood = 180.0
                            Log.farm("加速卡后投喂小鸡成功！")
                            /* 使用加速卡后尝试领取饲料，因为连续使用加速卡会导致饲料缺口，连续使用8张加速卡，最多可
                                能投喂两次，饲料减少360g,这显然会导致游戏改分的判断条件失败，这样就不能在一次软件运行
                                过程中完成所有任务，所以需要根据条件领取饲料。领取逻辑是，游戏改分飞行赛2次可以通常
                                得到180g饲料，我测试没有低于180g的时候，因此可以留180g不领，用飞行赛填补。打小鸡
                                没有饲料奖励
                             */
                            // 判断游戏改分还没完成。按照我的设计，其实这里不用判断，因为任务顺序就是先加速->游戏改分
                            if (!Status.hasFlagToday("farm::farmGameFinished")){
                                if (AntFarm.foodStock < AntFarm.foodStockLimit - farm.gameRewardMax!!.value) {
                                    Log.farm("加速后已喂食，领取饲料奖励")
                                    farm.receiveFarmAwards()
                                } else {
                                    Log.farm("今天游戏改分还没有完成，预留${farm.gameRewardMax!!.value}g的饲料剩余空间，目前饲料${AntFarm.foodStock}g，差${AntFarm.foodStockLimit - AntFarm.foodStock}g满饲料")
                                }
                            } else {
                                Log.farm("加速后已喂食，领取饲料奖励")
                                farm.receiveFarmAwards()
                            }
                        } else {
                            remainingFood = (farm.countdown?.toDouble() ?: 0.0) * totalConsumeSpeed
                            Log.farm("使用加速卡使饲料吃完，投喂小鸡失败！")
                        }
                    } else {
                        // 如果再次同步发现小鸡不是饥饿状态，重新开始计算remainingFood
                        remainingFood = (farm.countdown?.toDouble() ?: 0.0) * totalConsumeSpeed
                    }
                }
            } else {
                Log.record(AntFarm.TAG, "加速卡内部⏩useFarmTool 返回失败，终止循环")
                exitReason = "TOOL_USE_FAILED"
                break
            }
            // 若未开启“连续使用”，只使用 1 次后退出
            if (!farm.useAccelerateToolContinue!!.value) {
                exitReason = "SINGLE_USE_MODE"
                break
            }
        }
        // 这里打印没有连续使用8张加速卡的原因
        when(exitReason){
            "CONDITION_NOT_MET" -> Log.record("剩余可加速的时间少于设置的${remainingTimeValue}分钟，将在下次喂食后再次使用加速卡")
            "SINGLE_USE_MODE" -> Log.record("开启了“仅在满状态使用加速卡")
            "EMOTION_NOT_MAX" -> Log.record("开启了“仅心情满值时加速”，且当前心情不为 100")
        }
        Log.record(AntFarm.TAG, "加速卡内部⏩最终 isUseAccelerateTool=$isUseAccelerateTool")
        return isUseAccelerateTool
    }

    internal suspend fun useFarmTool(targetFarmId: String?, toolType: AntFarm.ToolType): Boolean {
        try {
            var s = AntFarmRpcCall.listFarmTool()
            var jo = JSONObject(s)
            var memo = jo.getString("memo")
            if (ResChecker.checkRes(AntFarm.TAG, jo)) {
                val jaToolList = jo.getJSONArray("toolList")
                for (i in 0..<jaToolList.length()) {
                    jo = jaToolList.getJSONObject(i)
                    if (toolType.name == jo.getString("toolType")) {
                        val toolCount = jo.getInt("toolCount")
                        if (toolCount > 0) {
                            if (toolType == AntFarm.ToolType.FENCETOOL && farm.hasFence) {
                                Log.record(AntFarm.TAG, "🛡️ 篱笆效果尚在（剩余${farm.fenceCountDown/60}分钟），跳过重复使用")
                                return false
                            }
                            var toolId = ""
                            if (jo.has("toolId")) toolId = jo.getString("toolId")
                            s = AntFarmRpcCall.useFarmTool(targetFarmId, toolId, toolType.name)
                            jo = JSONObject(s)
                            memo = jo.getString("memo")
                            if (ResChecker.checkRes(AntFarm.TAG, jo)) {
                                Log.farm("使用了道具🎭[" + toolType.nickName() + "]#剩余" + (toolCount - 1) + "张")
                                if (toolType == AntFarm.ToolType.FENCETOOL) {
                                    farm.hasFence = true
                                    farm.fenceCountDown = 86400
                                }
                                listFarmTool()
                                return true
                            } else {
                                // 针对加速卡：当日达到上限(resultCode=3D16)后，设置当日标记，避免后续重复尝试
                                val resultCode = jo.optString("resultCode")
                                if (toolType == AntFarm.ToolType.ACCELERATETOOL && resultCode == "3D16") {
                                    Status.setFlagToday("farm::accelerateLimit")
                                }
                                Log.record(memo)
                            }
                            Log.record(s)
                        }
                        break
                    }
                }
            } else {
                Log.record(memo)
                Log.record(s)
            }
        } catch (t: Throwable) {
            Log.printStackTrace(AntFarm.TAG, "useFarmTool err:",t)
        }
        return false
    }

    internal suspend fun receiveToolTaskReward() {
        try {
            var s = AntFarmRpcCall.listToolTaskDetails()
            var jo = JSONObject(s)
            var memo = jo.getString("memo")
            if (ResChecker.checkRes(AntFarm.TAG, jo)) {
                val jaList = jo.getJSONArray("list")
                for (i in 0..<jaList.length()) {
                    val joItem = jaList.getJSONObject(i)
                    if (joItem.has("taskStatus")
                        && TaskStatus.FINISHED.name == joItem.getString("taskStatus")
                    ) {
                        val bizInfo = JSONObject(joItem.getString("bizInfo"))
                        val awardType = bizInfo.getString("awardType")
                        val toolType = AntFarm.ToolType.valueOf(awardType)
                        var isFull = false
                        for (farmTool in farmTools) {
                            if (farmTool.toolType == toolType) {
                                if (farmTool.toolCount == farmTool.toolHoldLimit) {
                                    isFull = true
                                }
                                break
                            }
                        }
                        if (isFull) {
                            Log.record(AntFarm.TAG, "领取道具[" + toolType.nickName() + "]#已满，暂不领取")
                            continue
                        }
                        val awardCount = bizInfo.getInt("awardCount")
                        val taskType = joItem.getString("taskType")
                        val taskTitle = bizInfo.getString("taskTitle")
                        s = AntFarmRpcCall.receiveToolTaskReward(awardType, awardCount, taskType)
                        jo = JSONObject(s)
                        memo = jo.getString("memo")
                        if (ResChecker.checkRes(AntFarm.TAG, jo)) {
                            Log.farm("领取道具🎖️[" + taskTitle + "-" + toolType.nickName() + "]#" + awardCount + "张")
                        } else {
                            memo = memo.replace("道具", toolType.nickName().toString())
                            Log.record(memo)
                            Log.record(s)
                        }
                    }
                }
            } else {
                Log.record(memo)
                Log.record(s)
            }
        } catch (t: Throwable) {
            Log.printStackTrace(AntFarm.TAG, "receiveToolTaskReward err:",t)
        }
    }

    /**
     * 手动使用庄园道具
     * @param toolType 道具类型：BIG_EATER_TOOL, NEWEGGTOOL, FENCETOOL
     * @param toolCount 使用数量（仅 NEWEGGTOOL 有效）
     */
    internal suspend fun manualUseFarmTool(toolType: String, toolCount: Int) {
        try {
            if (farm.enterFarm() != null) {
                farm.syncAnimalStatus(farm.ownerFarmId)
                Log.record(AntFarm.TAG, "开始执行手动使用道具: $toolType, 计划数量: $toolCount")
                val farmTools = listFarmTool()
                if (farmTools == null || farmTools.isEmpty()) {
                    Log.record(AntFarm.TAG, "❌ 获取道具列表失败或道具库为空")
                    return
                }

                val tool = farmTools.find { it.toolType?.name == toolType }
                if (tool == null) {
                    Log.record(AntFarm.TAG, "❌ 道具库中没有道具: $toolType")
                    return
                }
                if (toolType == "FENCETOOL" && farm.hasFence) {
                    Log.record(AntFarm.TAG, "❌ 手动执行拦截：篱笆卡效果正在生效中")
                    return
                }

                Log.record(AntFarm.TAG, "当前道具 [${tool.toolType?.nickName()}] 余量: ${tool.toolCount}")

                val actualCount = if (toolType == "NEWEGGTOOL") {
                    if (tool.toolCount < toolCount) {
                        Log.record(AntFarm.TAG, "⚠️ 道具余量不足，将用完剩余的 ${tool.toolCount} 个")
                        tool.toolCount
                    } else {
                        toolCount
                    }
                } else {
                    1 // 其他道具默认使用1次
                }

                if (actualCount <= 0) {
                    Log.record(AntFarm.TAG, "❌ 可用数量为0，终止操作")
                    return
                }

                repeat(actualCount) { index ->
                    val res = AntFarmRpcCall.useFarmTool(farm.ownerFarmId, tool.toolId, tool.toolType?.name)
                    val jo = JSONObject(res)
                    if (ResChecker.checkRes(AntFarm.TAG, jo)) {
                        Log.farm("手动使用道具 [${tool.toolType?.nickName()}] 成功 (${index + 1}/$actualCount)")
                    } else {
                        val msg = jo.optString("memo", "未知错误")
                        Log.record(AntFarm.TAG, "❌ 使用道具失败: $msg")
                        return@repeat
                    }
                    // 使用多个时稍微延迟，避免过快
                    if (actualCount > 1) delay(1000)
                }
            }
        } catch (t: Throwable) {
            Log.record(AntFarm.TAG, "❌ manualUseFarmTool 出错: ${t.message}")
            Log.printStackTrace(t)
        }
    }
}
