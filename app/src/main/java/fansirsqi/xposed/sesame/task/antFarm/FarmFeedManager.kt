package fansirsqi.xposed.sesame.task.antFarm

import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.core.store.DataStore
import fansirsqi.xposed.sesame.core.util.ResChecker
import fansirsqi.xposed.sesame.core.util.TimeUtil
import fansirsqi.xposed.sesame.data.Status
import fansirsqi.xposed.sesame.hook.Toast
import fansirsqi.xposed.sesame.model.BaseModel
import fansirsqi.xposed.sesame.task.ModelTask.ChildModelTask
import fansirsqi.xposed.sesame.util.maps.UserMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.time.LocalDate

internal class FarmFeedManager(private val farm: AntFarm) {

    /**
     * 将服务端的饲喂状态代码转换为可读中文
     */
    internal fun toFeedStatusName(status: String?): String {
        return when (status) {
            AntFarm.AnimalFeedStatus.HUNGRY.name -> "饥饿"
            AntFarm.AnimalFeedStatus.EATING.name -> "进食中"
            AntFarm.AnimalFeedStatus.SLEEPY.name -> "睡觉中"
            else -> status ?: "未知"
        }
    }

    internal suspend fun handleAutoFeedAnimal(isChildTask: Boolean = false) {

//        val sleepTimeStr = sleepTime!!.value
//        if (sleepTimeStr != "-1") {
//            val now = TimeUtil.getNow()
//            val sleepCal = TimeUtil.getTodayCalendarByTimeStr(sleepTimeStr)
//            // 如果当前时间在睡觉时间之前，且差距小于 30 分钟
//            if (now.before(sleepCal) && (sleepCal.timeInMillis - now.timeInMillis) < 30 * 60 * 1000) {
//                Log.record(TAG, "马上要睡觉了，暂不投喂，让它饿着吧")
//                return
//            }
//            // 如果已经过了睡觉时间，理论上也不应该喂，但原逻辑会在后面 animalSleepAndWake 处理睡觉
//            if (now.after(sleepCal)) {
//                Log.record(TAG, "已过睡觉时间，暂不投喂")
//                return
//            }
//        }

        if (AntFarm.AnimalInteractStatus.HOME.name != farm.ownerAnimal.animalInteractStatus) {
            return  // 小鸡不在家，不执行喂养逻辑
        }

        if (AntFarm.AnimalFeedStatus.SLEEPY.name == farm.ownerAnimal.animalFeedStatus) {
            Log.record(AntFarm.TAG, "投喂小鸡🥣[小鸡正在睡觉中，暂停投喂]")
            return
        }

        // 1. 如果不够一次喂食180g时尝试领取奖励，首次运行时unreceiveTaskAward=0
        if (farm.receiveFarmTaskAward!!.value && AntFarm.foodStock <180) {
            Log.record(AntFarm.TAG, "饲料小于180g，尝试领取饲料奖励")
            farm.receiveFarmAwards() // 该步骤会自动计算饲料数量，不需要重复刷新状态
        }

        // 2. 判断是否需要喂食
        if (AntFarm.AnimalFeedStatus.HUNGRY.name == farm.ownerAnimal.animalFeedStatus) {
            if (farm.feedAnimal!!.value) {
                Log.record("小鸡在挨饿~Tk 尝试为你自动喂食")
                if (feedAnimal(farm.ownerFarmId)) {
                    // 刷新状态
                    farm.syncAnimalStatus(farm.ownerFarmId)
                }
            }
        }

        // 3. 使用加饭卡（仅当正在吃饭且开启配置）
        if (farm.useBigEaterTool!!.value && AntFarm.AnimalFeedStatus.EATING.name == farm.ownerAnimal.animalFeedStatus) {
            // 若服务端已标记今日使用过（或当前有效），本地直接跳过
            if (farm.serverUseBigEaterTool) {
                Log.record("服务端标记已使用加饭卡，跳过使用")
                // 这里可选：尝试与本地计数对齐（仅在计数为0时+1，避免重复累加）
                val today = LocalDate.now().toString()
                val uid = UserMap.currentUid
                val usedKey = "AF_BIG_EATER_USED_COUNT|$uid|$today"
                val usedCount = DataStore.get(usedKey, Int::class.java) ?: 0
                if (usedCount == 0) {
                    DataStore.put(usedKey, 1)
                }
            } else {
                // 使用 DataStore 记录“当日已用次数”，每日上限为 2 次（按账号维度）
                val today = LocalDate.now().toString()
                val uid = UserMap.currentUid
                val usedKey = "AF_BIG_EATER_USED_COUNT|$uid|$today"
                val usedCount = DataStore.get(usedKey, Int::class.java) ?: 0

                if (usedCount >= 2) {
                    Log.record("今日加饭卡已使用${usedCount}/2，跳过使用")
                } else {
                    val result = farm.itemManager.useFarmTool(farm.ownerFarmId, AntFarm.ToolType.BIG_EATER_TOOL)
                    if (result) {
                        Log.farm("使用道具🎭[加饭卡]！")
                        DataStore.put(usedKey, usedCount + 1)
                        delay(1000)
                        // 刷新状态
                        farm.syncAnimalStatus(farm.ownerFarmId)
                    } else {
                        Log.record("⚠️使用道具🎭[加饭卡]失败，可能卡片不足或状态异常~")
                    }
                }
            }
        }

        // 4. 判断是否需要使用加速道具（仅在正在吃饭时尝试）
        if (farm.useAccelerateTool!!.value && AntFarm.AnimalFeedStatus.EATING.name == farm.ownerAnimal.animalFeedStatus) {
            // 记录调试日志：加速卡判定前的关键状态
            Log.record(
                AntFarm.TAG,
                "加速卡判断⏩[动物状态=" + toFeedStatusName(farm.ownerAnimal.animalFeedStatus) +
                        ", 今日封顶=" + Status.hasFlagToday("farm::accelerateLimit") + "]"
            )
            val accelerated = farm.itemManager.useAccelerateTool()
            if (accelerated) {
                Log.farm("使用道具🎭[加速卡]⏩成功")
                // 刷新状态
                farm.syncAnimalStatus(farm.ownerFarmId)
            }
        }

        // 在蹲点喂食逻辑中判断是否需要执行游戏改分及抽抽乐
        if (isChildTask) {
            if (farm.recordFarmGame!!.value) {
                farm.handleFarmGameLogic()
            }
            if (farm.enableChouchoule!!.value) {
                farm.handleChouChouLeLogic()
            }
        }

        // 5. 计算并安排下一次自动喂食任务（仅当小鸡不在睡觉时）
        if (AntFarm.AnimalFeedStatus.SLEEPY.name != farm.ownerAnimal.animalFeedStatus) {
            try {
                /* 创建蹲点任务时间点前先同步countdown，因为可能因为好友小鸡在两次执行间隔间偷吃而引起蹲点时间变动。
                    比如投喂后程序第一次计算了剩余时间是4小时40分钟，那中间有小鸡偷吃，时间就少于4：40分钟了。再用原来
                    的时间显然有误,除非其他逻辑同步了小鸡状态才会修正，这里直接同步+修正
                 */
                farm.syncAnimalStatus(farm.ownerFarmId)
                // 直接使用服务器计算的权威倒计时（单位：秒）
                val remainingSec = farm.countdown?.toDouble()?.coerceAtLeast(0.0)
                // 如果倒计时为0，跳过任务创建
                remainingSec?.let {
                    if (it > 0) {
                        // 计算下次执行时间（毫秒）
                        val nextFeedTime = System.currentTimeMillis() + (remainingSec * 1000).toLong()
                        // 调试日志：显示服务器倒计时详情
                        Log.record(
                            AntFarm.TAG, "服务器倒计时🕐[小鸡状态=" + toFeedStatusName(farm.ownerAnimal.animalFeedStatus) +
                                    ", 剩余=${remainingSec.toInt()}秒" +
                                    ", 执行时间=" + TimeUtil.getCommonDate(nextFeedTime) + "]"
                        )
                        val taskId = "FA|${farm.ownerFarmId}"
                        farm.addChildTask(
                            ChildModelTask(
                                id = taskId,
                                group = "FA",
                                suspendRunnable = {
                                    try {
                                        Log.record(AntFarm.TAG, "🔔 蹲点投喂任务触发")
                                        // 重新进入庄园，获取最新状态
                                        farm.enterFarm()
                                        // 同步最新状态
                                        farm.syncAnimalStatus(farm.ownerFarmId)
                                        // 遣返
                                        if (farm.sendBackAnimal!!.value) {
                                            farm.sendBackAnimal()
                                        }
                                        // 雇佣小鸡
                                        if (farm.hireAnimal!!.value) {
                                            farm.hireAnimal()
                                        }
                                        // 喂鸡
                                        handleAutoFeedAnimal(true)
                                        Log.record(AntFarm.TAG, "🔄 下一次蹲点任务已创建")
                                    } catch (e: Exception) {
                                        Log.printStackTrace(AntFarm.TAG,"蹲点投喂任务执行失败", e)
                                    }
                                },
                                execTime = nextFeedTime,
                                useSmartScheduler = farm.useSmartSchedulerManager!!.value
                            )
                        )
                        Log.record(UserMap.getCurrentMaskName() + "小鸡的蹲点投喂时间[" + TimeUtil.getCommonDate(nextFeedTime)+"]")
                    } else {
                        Log.record(AntFarm.TAG, "蹲点投喂🥣[倒计时为0，开始投喂]")
                        if (feedAnimal(farm.ownerFarmId)) {
                            // 刷新状态
                            farm.syncAnimalStatus(farm.ownerFarmId)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.printStackTrace(AntFarm.TAG, "创建蹲点任务失败: ${e.message}",e)
            }
        } else {
            // 小鸡在睡觉，跳过创建蹲点投喂任务
            // 注意：已存在的任务会在小鸡醒来时被新任务自动替换
            Log.record(AntFarm.TAG, "蹲点投喂🥣[小鸡正在睡觉，暂不安排投喂任务]")
        }

        // 6. 其他功能（换装、领取饲料）
        // 小鸡换装
        if (farm.listOrnaments!!.value && Status.canOrnamentToday()) {
            farm.listOrnaments()
        }
    }

    internal suspend fun feedAnimal(farmId: String?): Boolean {
        try {
            // 检查小鸡是否在睡觉，如果在睡觉则直接返回
            if (AntFarm.AnimalFeedStatus.SLEEPY.name == farm.ownerAnimal.animalFeedStatus) {
                Log.record(AntFarm.TAG, "投喂小鸡🥣[小鸡正在睡觉中，跳过投喂]")
                return false
            }


            // 检查小鸡是否正在吃饭，如果在吃饭则直接返回
            // EATING: 小鸡正在进食状态，此时不能重复投喂，会返回"不要着急，还没吃完呢"错误
            if (AntFarm.AnimalFeedStatus.EATING.name == farm.ownerAnimal.animalFeedStatus) {
                Log.record(AntFarm.TAG, "投喂小鸡🥣[小鸡正在吃饭中，跳过投喂]")
                return false
            }

            if (AntFarm.foodStock < 180) {
                Log.record(AntFarm.TAG, "喂鸡饲料不足，停止本次投喂尝试")
                return false // 明确返回 false
            } else {
                val jo = JSONObject(AntFarmRpcCall.feedAnimal(farmId))
                if (ResChecker.checkRes(AntFarm.TAG, jo)) {
                    // 安全获取foodStock字段，如果不存在则显示未知
                    val remainingFood = jo.optInt("foodStock", 0).coerceAtLeast(0)
                    Log.farm("${UserMap.getCurrentMaskName()}投喂小鸡🥣[180g]#剩余饲料${remainingFood}g")

                    val interval = BaseModel.checkInterval.value
                    var timeSendBackAnimal = 0
                    if (farm.timeSendBack!!.value in 10..interval){
                        timeSendBackAnimal = farm.timeSendBack!!.value
                    } else if(farm.timeSendBack!!.value > interval){
                        Log.record(AntFarm.TAG, "设置个合理的喂食后赶鸡时间，建议 30 分钟")
                    }
                    if (farm.sendBackAnimal!!.value && timeSendBackAnimal > 0) {
                        try {
                            val taskId = "KC|${farm.ownerFarmId}"
                            val kcTime =
                                TimeUtil.getCommonDate(System.currentTimeMillis() + timeSendBackAnimal * 60 * 1000L)
                            val task = ChildModelTask(
                                id = taskId,
                                group = "KC",
                                suspendRunnable = {
                                    try {
                                        Log.record(AntFarm.TAG, "🔔 蹲点赶鸡任务触发")
                                        farm.enterFarm()
                                        farm.syncAnimalStatus(farm.ownerFarmId)
                                        farm.sendBackAnimal()
                                    } catch (e: Exception) {
                                        Log.error(AntFarm.TAG, "蹲点赶鸡任务执行失败: ${e.message}")
                                        Log.printStackTrace(AntFarm.TAG, e)
                                    }
                                },
                                execTime = System.currentTimeMillis() + timeSendBackAnimal * 60 * 1000L,
                                useSmartScheduler = farm.useSmartSchedulerManager!!.value
                            )
                            farm.addChildTask(task)
                            Log.record(UserMap.getCurrentMaskName() + "${timeSendBackAnimal}分钟后${kcTime}蹲点赶小鸡")

                        } catch (e: Exception) {
                            Log.printStackTrace(AntFarm.TAG, "创建蹲点赶鸡失败: ${e.message}", e)
                        }
                    }
                    return true
                } else {
                    // 检查特定的错误码
                    val resultCode = jo.optString("resultCode", "")
                    val memo = jo.optString("memo", "")
                    if ("311" == resultCode) {
                        Log.record(AntFarm.TAG, "投喂小鸡🥣[$memo]")
                    } else {
                        Log.record(AntFarm.TAG, "投喂小鸡失败: $jo")
                    }
                }
            }
        } catch (t: Throwable) {
            Log.printStackTrace(AntFarm.TAG, "feedAnimal err:", t)
        }
        return false
    }

    internal suspend fun feedFriend() {
        try {
            val feedFriendAnimalMap: Map<String?, Int?> = farm.feedFriendAnimalList!!.value
            for (entry in feedFriendAnimalMap.entries) {
                val userId: String = entry.key!!
                val maxDailyCount: Int = entry.value!!

                // 智能冲突避免：如果是自己的账号
                if (userId == UserMap.currentUid) {
                    if (farm.feedAnimal!!.value) {
                        // 已开启"自动喂小鸡" → 优先使用蹲点机制（更精准），跳过好友列表喂食
                        Toast.show(
                            "⚠️ 配置冲突提醒\n" +
                                    "已开启「自动喂小鸡」，将使用蹲点机制（精准时间）\n" +
                                    "好友列表中的自己（配置${maxDailyCount}次）已被忽略\n" +
                                    "建议：无需在好友列表中添加自己"
                        )
                        continue
                    } else {
                        // 未开启"自动喂小鸡" → 使用好友列表机制（尊重次数限制）
                        // 继续执行后续逻辑
                    }
                }

                if (!Status.canFeedFriendToday(userId, maxDailyCount)) continue
                val jo = JSONObject(AntFarmRpcCall.enterFarm(userId, userId))
                delay(3 * 1000L) //延迟3秒
                if (ResChecker.checkRes(AntFarm.TAG, jo)) {
                    val subFarmVOjo = jo.getJSONObject("farmVO").getJSONObject("subFarmVO")
                    val friendFarmId = subFarmVOjo.getString("farmId")
                    val jaAnimals = subFarmVOjo.getJSONArray("animals")
                    for (j in 0..<jaAnimals.length()) {
                        val animalsjo = jaAnimals.getJSONObject(j)

                        val masterFarmId = animalsjo.getString("masterFarmId")
                        if (masterFarmId == friendFarmId) { //遍历到的鸡 如果在自己的庄园
                            val animalStatusVO = animalsjo.getJSONObject("animalStatusVO")
                            val animalInteractStatus =
                                animalStatusVO.getString("animalInteractStatus") //动物互动状态
                            val animalFeedStatus =
                                animalStatusVO.getString("animalFeedStatus") //动物饲料状态
                            if (AntFarm.AnimalInteractStatus.HOME.name == animalInteractStatus && AntFarm.AnimalFeedStatus.HUNGRY.name == animalFeedStatus) { //状态是饥饿 并且在庄园
                                val user = UserMap.getMaskName(userId) //喂 给我喂
                                if (AntFarm.foodStock < 180) {
                                    if (farm.unreceiveTaskAward > 0) {
                                        Log.record(AntFarm.TAG, "✨还有待领取的饲料")
                                        farm.receiveFarmAwards() //先去领个饲料
                                    }
                                }
                                //第二次检查
                                if (AntFarm.foodStock >= 180) {
                                    if (Status.hasFlagToday("farm::feedFriendLimit")) {
                                        return
                                    }
                                    val feedFriendAnimaljo =
                                        JSONObject(AntFarmRpcCall.feedFriendAnimal(friendFarmId))
                                    if (ResChecker.checkRes(AntFarm.TAG, feedFriendAnimaljo)) {
                                        AntFarm.foodStock = feedFriendAnimaljo.getInt("foodStock")
                                        Log.farm("帮喂好友🥣[" + user + "]的小鸡[180g]#剩余" + AntFarm.foodStock + "g")
                                        Status.feedFriendToday(
                                            AntFarmRpcCall.farmId2UserId(
                                                friendFarmId
                                            )
                                        )
                                    } else {
                                        Log.error(
                                            AntFarm.TAG,
                                            "😞喂[$user]的鸡失败$feedFriendAnimaljo"
                                        )
                                        Status.setFlagToday("farm::feedFriendLimit")
                                        break
                                    }
                                } else {
                                    Log.record(AntFarm.TAG, "😞喂鸡[$user]饲料不足")
                                }
                            }
                            break
                        }
                    }
                }else{
                    val username=UserMap.getMaskName(userId)
                    Log.error(AntFarm.TAG, "😞进入用户 $userId[$username] 的庄园失败> $jo")
                }
            }
        } catch (e: CancellationException) {
            // 协程取消异常必须重新抛出，不能吞掉
             Log.record(AntFarm.TAG, "feedFriend 协程被取消")
            throw e
        } catch (t: Throwable) {
            Log.printStackTrace(AntFarm.TAG, "feedFriendAnimal err:", t)
        }
    }

    internal suspend fun notifyFriend() {
        if (AntFarm.foodStock >= AntFarm.foodStockLimit) return
        try {
            var hasNext = false
            var pageStartSum = 0
            var s: String?
            var jo: JSONObject
            do {
                s = AntFarmRpcCall.rankingList(pageStartSum)
                // 检查空响应
                if (s.isNullOrEmpty()) {
                    Log.record(AntFarm.TAG, "notifyFriend.rankingList: 收到空响应，终止通知")
                    break // 跳出do-while循环
                }
                jo = JSONObject(s)
                var memo = jo.getString("memo")
                if (ResChecker.checkRes(AntFarm.TAG, jo)) {
                    hasNext = jo.getBoolean("hasNext")
                    val jaRankingList = jo.getJSONArray("rankingList")
                    pageStartSum += jaRankingList.length()
                    for (i in 0..<jaRankingList.length()) {
                        jo = jaRankingList.getJSONObject(i)
                        val userId = jo.getString("userId")
                        val userName = UserMap.getMaskName(userId)
                        var isNotifyFriend = farm.notifyFriendList!!.value.contains(userId)
                        if (farm.notifyFriendType!!.value == AntFarm.NotifyFriendType.DONT_NOTIFY) {
                            isNotifyFriend = !isNotifyFriend
                        }
                        if (!isNotifyFriend || userId == UserMap.currentUid) {
                            continue
                        }
                        val starve =
                            jo.has("actionType") && "starve_action" == jo.getString("actionType")
                        if (jo.getBoolean("stealingAnimal") && !starve) {
                            s = AntFarmRpcCall.enterFarm(userId, userId)
                            // 循环内的空响应检查：静默跳过该好友，继续处理下一个
                            if (s.isNullOrEmpty()) {
                                continue // 跳过当前好友，处理下一个
                            }
                            jo = JSONObject(s)
                            memo = jo.getString("memo")
                            if (ResChecker.checkRes(AntFarm.TAG, jo)) {
                                jo = jo.getJSONObject("farmVO").getJSONObject("subFarmVO")
                                val friendFarmId = jo.getString("farmId")
                                val jaAnimals = jo.getJSONArray("animals")
                                var notified = (farm.notifyFriend!!.value)
                                for (j in 0..<jaAnimals.length()) {
                                    jo = jaAnimals.getJSONObject(j)
                                    val animalId = jo.getString("animalId")
                                    val masterFarmId = jo.getString("masterFarmId")
                                    if (masterFarmId != friendFarmId && masterFarmId != farm.ownerFarmId) {
                                        if (notified) continue
                                        jo = jo.getJSONObject("animalStatusVO")
                                        notified =
                                            notifyFriend(jo, friendFarmId, animalId, userName)
                                    }
                                }
                            } else {
                                Log.record(memo)
                                Log.record(s)
                            }
                        }
                    }
                } else {
                    Log.record(memo)
                    Log.record(s)
                }
            } while (hasNext)
            Log.record(AntFarm.TAG, "饲料剩余[" + AntFarm.foodStock + "g]")
        } catch (t: Throwable) {
            Log.printStackTrace(AntFarm.TAG, "notifyFriend err:",t)
        }
    }

    internal suspend fun notifyFriend(
        joAnimalStatusVO: JSONObject,
        friendFarmId: String?,
        animalId: String?,
        user: String?
    ): Boolean {
        try {
            if (AntFarm.AnimalInteractStatus.STEALING.name == joAnimalStatusVO.getString("animalInteractStatus") && AntFarm.AnimalFeedStatus.EATING.name == joAnimalStatusVO.getString(
                    "animalFeedStatus"
                )
            ) {
                val jo = JSONObject(AntFarmRpcCall.notifyFriend(animalId, friendFarmId))
                if (ResChecker.checkRes(AntFarm.TAG, jo)) {
                    val rewardCount = jo.getDouble("rewardCount")
                    if (jo.getBoolean("refreshFoodStock")) AntFarm.foodStock =
                        jo.getDouble("finalFoodStock").toInt()
                    else farm.add2FoodStock(rewardCount.toInt())
                    Log.farm("通知好友📧[" + user + "]被偷吃#奖励" + rewardCount + "g")
                    return true
                }
            }
        } catch (t: Throwable) {
            Log.printStackTrace(AntFarm.TAG, "notifyFriend err:", t)
        }
        return false
    }
}
