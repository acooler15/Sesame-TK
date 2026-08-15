package fansirsqi.xposed.sesame.task.antSports

import android.annotation.SuppressLint
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import fansirsqi.xposed.sesame.data.Status
import fansirsqi.xposed.sesame.data.StatusFlags
import fansirsqi.xposed.sesame.entity.AlipayUser
import fansirsqi.xposed.sesame.hook.ApplicationHook
import fansirsqi.xposed.sesame.model.ModelFields
import fansirsqi.xposed.sesame.model.ModelGroup
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.ChoiceModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.IntegerModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.StringModelField
import fansirsqi.xposed.sesame.task.ModelTask
import fansirsqi.xposed.sesame.task.TaskCommon
import fansirsqi.xposed.sesame.core.store.DataStore
import fansirsqi.xposed.sesame.core.threads.GlobalThreadPools
import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.core.util.ResChecker
import fansirsqi.xposed.sesame.core.app.TaskBlacklist
import fansirsqi.xposed.sesame.core.util.TimeUtil
import fansirsqi.xposed.sesame.util.maps.UserMap
import org.json.JSONObject
import java.util.Random

import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.delay

/**
 * @file AntSports.kt
 * @brief 支付宝蚂蚁运动主任务逻辑（Kotlin 重构版）。
 *
 * @details
 * 负责统一调度蚂蚁运动相关的所有自动化逻辑，包括：
 * - 步数同步与行走路线（旧版 & 新版路线）
 * - 运动任务面板任务、首页能量球任务
 * - 首页金币收集、慈善捐步
 * - 文体中心任务 / 行走路线
 * - 抢好友大战（训练好友 + 抢购好友）
 * - 健康岛（Neverland）任务、泡泡、走路建造
 *
 * 所有 RPC 调用均通过 {@link AntSportsRpcCall} 与 {@link AntSportsRpcCall.NeverlandRpcCall} 完成。
 */
@SuppressLint("DefaultLocale")
class AntSports : ModelTask() {

    companion object {
        /** @brief 日志 TAG */
        internal val TAG: String = AntSports::class.java.simpleName

        /** @brief 训练好友 0 金币达上限日期缓存键 */
        private const val TRAIN_FRIEND_ZERO_COIN_DATE = "TRAIN_FRIEND_ZERO_COIN_DATE"
    }

    /** @brief 走路兑换子模块 */
    internal val exchangeManager = SportsExchangeManager(this)

    /** @brief 运动任务子模块 */
    internal val sportsTaskManager = SportsTaskManager(this)

    /** @brief 临时步数缓存（-1 表示未初始化） */
    internal var tmpStepCount: Int = -1

    // 配置字段
    private lateinit var walk: BooleanModelField
    internal lateinit var walkPathTheme: ChoiceModelField
    internal var walkPathThemeId: String? = null
    internal lateinit var walkCustomPath: BooleanModelField
    internal lateinit var walkCustomPathId: StringModelField
    private lateinit var openTreasureBox: BooleanModelField
    private lateinit var receiveCoinAssetField: BooleanModelField
    private lateinit var donateCharityCoin: BooleanModelField
    internal lateinit var donateCharityCoinType: ChoiceModelField
    internal lateinit var donateCharityCoinAmount: IntegerModelField
    internal lateinit var minExchangeCount: IntegerModelField
    internal lateinit var latestExchangeTime: IntegerModelField
    internal lateinit var syncStepCount: IntegerModelField
    private lateinit var tiyubiz: BooleanModelField
    private lateinit var battleForFriends: BooleanModelField
    private lateinit var battleForFriendType: ChoiceModelField
    private lateinit var originBossIdList: SelectModelField
    private lateinit var sportsTasksField: BooleanModelField
    private lateinit var sportsEnergyBubble: BooleanModelField

    // 训练好友相关配置
    private lateinit var trainFriend: BooleanModelField
    private lateinit var zeroCoinLimit: IntegerModelField

    /** @brief 记录训练好友连续获得 0 金币的次数 */
    private var zeroTrainCoinCount: Int = 0

    // 健康岛任务
    private lateinit var neverlandTask: BooleanModelField
    private lateinit var neverlandGrid: BooleanModelField
    private lateinit var neverlandGridStepCount: IntegerModelField


    /**
     * @brief 任务名称
     */
    override fun getName(): String = "运动"

    /**
     * @brief 所属任务分组
     */
    override fun getGroup(): ModelGroup = ModelGroup.SPORTS

    /**
     * @brief 图标文件名
     */
    override fun getIcon(): String = "AntSports.png"

    /**
     * @brief 定义本任务所需的所有配置字段
     */
    override fun getFields(): ModelFields {
        val modelFields = ModelFields()

        // 行走路线
        modelFields.addField(BooleanModelField("walk", "行走路线 | 开启", false).also { walk = it })
        modelFields.addField(
            ChoiceModelField(
                "walkPathTheme",
                "行走路线 | 主题",
                WalkPathTheme.DA_MEI_ZHONG_GUO,
                WalkPathTheme.nickNames
            ).also { walkPathTheme = it }
        )
        modelFields.addField(
            BooleanModelField("walkCustomPath", "行走路线 | 开启自定义路线", false).also { walkCustomPath = it }
        )
        modelFields.addField(
            StringModelField(
                "walkCustomPathId",
                "行走路线 | 自定义路线代码(debug)",
                "p0002023122214520001"
            ).also { walkCustomPathId = it }
        )

        // 旧版路线相关
        modelFields.addField(
            BooleanModelField("openTreasureBox", "开启宝箱", false).also { openTreasureBox = it }
        )

        // 运动任务 & 能量球
        modelFields.addField(
            BooleanModelField("sportsTasks", "开启运动任务", false).also { sportsTasksField = it }
        )
        modelFields.addField(
            BooleanModelField(
                "sportsEnergyBubble",
                "运动球任务(开启后有概率出现滑块验证)",
                false
            ).also { sportsEnergyBubble = it }
        )

        // 首页金币 & 捐步
        modelFields.addField(
            BooleanModelField("receiveCoinAsset", "收能量🎈", false).also { receiveCoinAssetField = it }
        )
        modelFields.addField(
            BooleanModelField("donateCharityCoin", "捐能量🎈 | 开启", false).also { donateCharityCoin = it }
        )
        modelFields.addField(
            ChoiceModelField(
                "donateCharityCoinType",
                "捐能量🎈 | 方式",
                DonateCharityCoinType.ONE,
                DonateCharityCoinType.nickNames
            ).also { donateCharityCoinType = it }
        )
        modelFields.addField(
            IntegerModelField("donateCharityCoinAmount", "捐能量🎈 | 数量(每次)", 100)
                .also { donateCharityCoinAmount = it }
        )

        // 健康岛任务
        modelFields.addField(
            BooleanModelField("neverlandTask", "健康岛 | 任务", false).also { neverlandTask = it }
        )
        modelFields.addField(
            BooleanModelField("neverlandGrid", "健康岛 | 自动走路建造", false).also { neverlandGrid = it }
        )
        modelFields.addField(
            IntegerModelField("neverlandGridStepCount", "健康岛 | 今日走路最大次数", 20)
                .also { neverlandGridStepCount = it }
        )

        // 抢好友相关
        modelFields.addField(
            BooleanModelField("battleForFriends", "抢好友 | 开启", false).also { battleForFriends = it }
        )
        modelFields.addField(
            ChoiceModelField(
                "battleForFriendType",
                "抢好友 | 动作",
                BattleForFriendType.ROB,
                BattleForFriendType.nickNames
            ).also { battleForFriendType = it }
        )
        modelFields.addField(
            SelectModelField(
                "originBossIdList",
                "抢好友 | 好友列表",
                LinkedHashSet(),
                AlipayUser::getList
            ).also { originBossIdList = it }
        )

        // 训练好友相关
        modelFields.addField(
            BooleanModelField("trainFriend", "训练好友 | 开启", false).also { trainFriend = it }
        )
        modelFields.addField(
            IntegerModelField("zeroCoinLimit", "训练好友 | 0金币上限次数当天关闭", 5)
                .also { zeroCoinLimit = it }
        )

        // 文体中心 & 捐步 & 步数同步
        modelFields.addField(BooleanModelField("tiyubiz", "文体中心", false).also { tiyubiz = it })
        modelFields.addField(
            IntegerModelField("minExchangeCount", "最小捐步步数", 0).also { minExchangeCount = it }
        )
        modelFields.addField(
            IntegerModelField("latestExchangeTime", "最晚捐步时间(24小时制)", 22)
                .also { latestExchangeTime = it }
        )
        modelFields.addField(
            IntegerModelField("syncStepCount", "自定义同步步数", 22000).also { syncStepCount = it }
        )

        // 本地字段：能量兑换双击卡
        val coinExchangeDoubleCard = BooleanModelField(
            "coinExchangeDoubleCard",
            "能量🎈兑换限时能量双击卡",
            false
        )
        modelFields.addField(coinExchangeDoubleCard)

        return modelFields
    }

    /**
     * @brief Xposed 启动时 hook 步数读取逻辑，实现自定义步数同步
     */
    override fun boot(classLoader: ClassLoader?) {
        try {
            XposedHelpers.findAndHookMethod(
                "com.alibaba.health.pedometer.core.datasource.PedometerAgent",
                classLoader,
                "readDailyStep",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val originStep = param.result as Int
                        val step = exchangeManager.tmpStepCount()
                        // 早于 8 点或步数小于自定义步数时进行 hook
                        if (TaskCommon.IS_AFTER_8AM && originStep < step) {
                            param.result = step
                        }
                    }
                }
            )
            Log.record(TAG, "hook readDailyStep successfully")
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "hook readDailyStep err:", t)
        }
    }

    /**
     * @brief 任务主入口
     */
    override suspend fun runSuspend() {
        Log.record(TAG, "执行开始-" + getName())

        try {
            val loader = ApplicationHook.classLoader
            if (loader == null) {
                Log.error(TAG, "ClassLoader is null, 跳过运动任务")
                return
            }

            // 健康岛整体任务（任务大厅 + 泡泡 + 走路建造）
            if (neverlandTask.value || neverlandGrid.value) {
                Log.record(TAG, "开始执行健康岛")
                NeverlandTaskHandler().runNeverland()
                Log.record(TAG, "健康岛结束")
            }

            // 步数同步
            if (!Status.hasFlagToday(StatusFlags.FLAG_ANTSPORTS_SYNC_STEP_DONE) &&
                TimeUtil.isNowAfterOrCompareTimeStr("0600")) {
                exchangeManager.syncStepTask()
            }

            // 运动任务
            if (!Status.hasFlagToday(StatusFlags.FLAG_ANTSPORTS_DAILY_TASKS_DONE) &&
                sportsTasksField.value) {
                sportsTaskManager.sportsTasks()
            }

            // 运动球任务
            if (sportsEnergyBubble.value) {
                sportsTaskManager.sportsEnergyBubbleTask()
            }

            // 新版行走路线
            if (walk.value) {
                exchangeManager.getWalkPathThemeIdOnConfig()
                exchangeManager.walk()
            }

            // 旧版路线：只开宝箱
            if (openTreasureBox.value && !walk.value) {
                exchangeManager.queryMyHomePage(loader)
            }

            // 捐能量
            if (donateCharityCoin.value && Status.canDonateCharityCoin()) {
                exchangeManager.queryProjectList(loader)
            }

            // 捐步
            val currentUid = UserMap.currentUid
            if (minExchangeCount.value > 0 &&
                currentUid != null &&
                Status.canExchangeToday(currentUid)) {
                exchangeManager.queryWalkStep(loader)
            }

            // 文体中心
            if (tiyubiz.value) {
                sportsTaskManager.userTaskGroupQuery("SPORTS_DAILY_SIGN_GROUP")
                sportsTaskManager.userTaskGroupQuery("SPORTS_DAILY_GROUP")
                sportsTaskManager.userTaskRightsReceive()
                sportsTaskManager.pathFeatureQuery()
                sportsTaskManager.participate()
            }

            // 抢好友大战
            if (battleForFriends.value) {
                queryClubHome()
                queryTrainItem()
                buyMember()
            }

            // 首页金币
            if (receiveCoinAssetField.value) {
                receiveCoinAsset()
            }

        } catch (t: Throwable) {
            Log.record(TAG, "runJava error:")
            Log.printStackTrace(TAG, t)
        } finally {
            Log.record(TAG, "执行结束-" + getName())
        }
    }

    /**
     * @brief 首页金币收集逻辑
     */
    internal suspend fun receiveCoinAsset() {
        try {
            val s = AntSportsRpcCall.queryCoinBubbleModule()
            var jo = JSONObject(s)
            if (ResChecker.checkRes(TAG, jo)) {
                val data = jo.getJSONObject("data")
                if (!data.has("receiveCoinBubbleList")) return

                val ja = data.getJSONArray("receiveCoinBubbleList")
                for (i in 0 until ja.length()) {
                    jo = ja.getJSONObject(i)
                    val assetId = jo.getString("assetId")
                    val coinAmount = jo.getInt("coinAmount")
                    val res = JSONObject(AntSportsRpcCall.receiveCoinAsset(assetId, coinAmount))
                    if (ResChecker.checkRes(TAG, res)) {
                        Log.other("收集金币💰[$coinAmount 个]")
                    } else {
                        Log.record(TAG, "首页收集金币 $res")
                    }
                }
            } else {
                Log.record(TAG, s)
            }
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "receiveCoinAsset err:", t)
        }
    }

    // ---------------------------------------------------------------------
    // 抢好友大战
    // ---------------------------------------------------------------------

    /**
     * @brief 抢好友主页查询 + 训练好友收益泡泡收集
     */
    private suspend fun queryClubHome() {
        try {
            val maxCount = zeroCoinLimit.value
            if (zeroTrainCoinCount >= maxCount) {
                val today = TimeUtil.getDateStr2()
                DataStore.put(TRAIN_FRIEND_ZERO_COIN_DATE, today)
                Log.record(TAG, "✅ 训练好友获得0金币已达${maxCount}次上限，今日不再执行")
                return
            }
            val clubHomeData = JSONObject(AntSportsRpcCall.queryClubHome())
            processBubbleList(clubHomeData.optJSONObject("mainRoom"))
            val roomList = clubHomeData.optJSONArray("roomList")
            if (roomList != null) {
                for (i in 0 until roomList.length()) {
                    val room = roomList.optJSONObject(i)
                    processBubbleList(room)
                }
            }
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "queryClubHome err:", t)
        }
    }

    /**
     * @brief 训练好友收益泡泡收集逻辑
     */
    private suspend fun processBubbleList(obj: JSONObject?) {
        if (obj == null || !obj.has("bubbleList")) return
        try {
            val bubbleList = obj.getJSONArray("bubbleList")
            for (j in 0 until bubbleList.length()) {
                val bubble = bubbleList.getJSONObject(j)
                val bubbleId = bubble.optString("bubbleId")

                val responseStr = AntSportsRpcCall.pickBubbleTaskEnergy(bubbleId, false)
                val responseJson = JSONObject(responseStr)

                if (!ResChecker.checkRes(TAG, responseJson)) {
                    Log.error(TAG, "收取训练好友 失败: $responseStr")
                    continue
                }

                var amount = 0
                val data = responseJson.optJSONObject("data")
                if (data != null) {
                    val changeAmountStr = data.optString("changeAmount", "0")
                    amount = changeAmountStr.toIntOrNull() ?: 0
                }

                Log.other("训练好友💰️ [获得:$amount 金币]")

                if (amount <= 0) {
                    zeroTrainCoinCount++
                    val maxCount = zeroCoinLimit.value
                    if (zeroTrainCoinCount >= maxCount) {
                        val today = TimeUtil.getDateStr2()
                        DataStore.put(TRAIN_FRIEND_ZERO_COIN_DATE, today)
                        Log.record(TAG, "✅ 连续获得0金币已达${maxCount}次，今日停止执行")
                        return
                    } else {
                        Log.record(TAG, "训练好友0金币计数: $zeroTrainCoinCount/$maxCount")
                    }
                }

                GlobalThreadPools.sleepCompat(1000)
            }
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "processBubbleList 异常:", t)
        }
    }

    /**
     * @brief 训练好友：选取可训练好友并执行一次训练
     */
    private suspend fun queryTrainItem() {
        try {
            val clubHomeData = JSONObject(AntSportsRpcCall.queryClubHome())
            val roomList = clubHomeData.optJSONArray("roomList") ?: return

            for (i in 0 until roomList.length()) {
                val room = roomList.optJSONObject(i) ?: continue
                val memberList = room.optJSONArray("memberList") ?: continue

                for (j in 0 until memberList.length()) {
                    val member = memberList.optJSONObject(j) ?: continue
                    val trainInfo = member.optJSONObject("trainInfo")
                    if (trainInfo == null || trainInfo.optBoolean("training", false)) continue

                    val memberId = member.optString("memberId")
                    val originBossId = member.optString("originBossId")
                    val userName = UserMap.getMaskName(originBossId) ?: originBossId

                    val responseData = AntSportsRpcCall.queryTrainItem()
                    val responseJson = JSONObject(responseData)
                    if (!ResChecker.checkRes(TAG, responseJson)) {
                        Log.record(
                            TAG,
                            "queryTrainItem rpc failed: ${responseJson.optString("resultDesc")}"
                        )
                        return
                    }

                    var bizId = responseJson.optString("bizId", "")
                    if (bizId.isEmpty() && responseJson.has("taskDetail")) {
                        bizId = responseJson.getJSONObject("taskDetail").optString("taskId", "")
                    }

                    val trainItemList = responseJson.optJSONArray("trainItemList")
                    if (bizId.isEmpty() || trainItemList == null || trainItemList.length() == 0) {
                        Log.record(TAG, "queryTrainItem response missing bizId or trainItemList")
                        return
                    }

                    var bestItem: JSONObject? = null
                    var bestProduction = -1
                    for (k in 0 until trainItemList.length()) {
                        val item = trainItemList.optJSONObject(k) ?: continue
                        val production = item.optInt("production", 0)
                        if (production > bestProduction) {
                            bestProduction = production
                            bestItem = item
                        }
                    }

                    if (bestItem == null) return

                    val itemType = bestItem.optString("itemType")
                    val trainItemName = bestItem.optString("name")

                    val trainMemberResponse = AntSportsRpcCall.trainMember(
                        bizId,
                        itemType,
                        memberId,
                        originBossId
                    )
                    val trainMemberJson = JSONObject(trainMemberResponse)
                    if (!ResChecker.checkRes(TAG, trainMemberJson)) {
                        Log.record(
                            TAG,
                            "trainMember request failed: ${trainMemberJson.optString("resultDesc")}"
                        )
                        return
                    }

                    Log.other("训练好友🥋[训练:$userName $trainItemName]")
                    GlobalThreadPools.sleepCompat(1000)
                    return
                }
            }
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "queryTrainItem err:", t)
        }
    }

    /**
     * @brief 抢好友大战：抢购好友逻辑
     */
    private suspend fun buyMember() {
        try {
            val clubHomeResponse = AntSportsRpcCall.queryClubHome()
            GlobalThreadPools.sleepCompat(500)
            val clubHomeJson = JSONObject(clubHomeResponse)

            if ("ENABLE" != clubHomeJson.optString("clubAuth")) {
                Log.record(TAG, "抢好友大战🧑‍🤝‍🧑未授权开启")
                return
            }

            val assetsInfo = clubHomeJson.optJSONObject("assetsInfo") ?: return
            val coinBalance = assetsInfo.optInt("energyBalance", 0)
            if (coinBalance <= 0) {
                Log.record(TAG, "抢好友大战🧑‍🤝‍🧑当前能量为0，跳过抢好友")
                return
            }

            val roomList = clubHomeJson.optJSONArray("roomList") ?: return

            for (i in 0 until roomList.length()) {
                val room = roomList.optJSONObject(i) ?: continue
                val memberList = room.optJSONArray("memberList")

                if (memberList != null && memberList.length() > 0) continue

                val roomId = room.optString("roomId")
                if (roomId.isEmpty()) continue

                val memberPriceResult = AntSportsRpcCall.queryMemberPriceRanking(coinBalance)
                GlobalThreadPools.sleepCompat(500)
                val memberPriceJson = JSONObject(memberPriceResult)
                if (!memberPriceJson.optBoolean("success", true)) {
                    Log.error(TAG, "queryMemberPriceRanking err: ${memberPriceJson.optString("resultDesc")}")
                    continue
                }

                val memberDetailList = memberPriceJson.optJSONArray("memberDetailList") ?: run {
                    Log.record(TAG, "抢好友大战🧑‍🤝‍🧑暂无可抢好友")
                    continue
                }

                for (j in 0 until memberDetailList.length()) {
                    val detail = memberDetailList.optJSONObject(j) ?: continue
                    val memberModel = detail.optJSONObject("memberModel") ?: continue

                    val originBossId = memberModel.optString("originBossId")
                    val memberIdFromRank = memberModel.optString("memberId")
                    if (originBossId.isEmpty() || memberIdFromRank.isEmpty()) continue

                    var isTarget = originBossIdList.value.contains(originBossId)
                    if (battleForFriendType.value == BattleForFriendType.DONT_ROB) {
                        isTarget = !isTarget
                    }
                    if (!isTarget) continue

                    val priceInfoObj = memberModel.optJSONObject("priceInfo") ?: continue
                    val price = priceInfoObj.optInt("price", Int.MAX_VALUE)
                    if (price > coinBalance) continue

                    val clubMemberResult = AntSportsRpcCall.queryClubMember(memberIdFromRank, originBossId)
                    GlobalThreadPools.sleepCompat(500)
                    val clubMemberDetailJson = JSONObject(clubMemberResult)
                    if (!clubMemberDetailJson.optBoolean("success", true) ||
                        !clubMemberDetailJson.has("member")
                    ) continue

                    val memberObj = clubMemberDetailJson.getJSONObject("member")
                    val currentBossId = memberObj.optString("currentBossId")
                    val memberId = memberObj.optString("memberId")
                    val priceInfoFull = memberObj.optJSONObject("priceInfo") ?: continue

                    if (currentBossId.isEmpty() || memberId.isEmpty()) continue

                    val priceInfoStr = priceInfoFull.toString()

                    val buyMemberResult = AntSportsRpcCall.buyMember(
                        currentBossId,
                        memberId,
                        originBossId,
                        priceInfoStr,
                        roomId
                    )
                    GlobalThreadPools.sleepCompat(500)
                    val buyMemberResponse = JSONObject(buyMemberResult)

                    if (ResChecker.checkRes(TAG, buyMemberResponse)) {
                        val userName = UserMap.getMaskName(originBossId) ?: originBossId
                        Log.other("抢购好友🥋[成功:将 $userName 抢回来]")
                        if (trainFriend.value) {
                            queryTrainItem()
                        }
                        return
                    } else if ("CLUB_AMOUNT_NOT_ENOUGH" == buyMemberResponse.optString("resultCode")) {
                        Log.record(TAG, "[能量🎈不足，无法完成抢购好友！]")
                        return
                    } else if ("CLUB_MEMBER_TRADE_PROTECT" == buyMemberResponse.optString("resultCode")) {
                        Log.record(TAG, "[暂时无法抢购好友，给Ta一段独处的时间吧！]")
                    }
                }
            }
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "buyMember err:", t)
        }
    }

    // ---------------------------------------------------------------------
    // 健康岛任务处理器（内部类）
    // ---------------------------------------------------------------------

    /**
     * @brief 健康岛任务处理器
     *
     * <p>整体流程：</p>
     * <ol>
     *   <li>签到（querySign + takeSign）</li>
     *   <li>任务大厅循环处理（queryTaskCenter + taskSend / adtask.finish）</li>
     *   <li>健康岛浏览任务（queryTaskInfo + energyReceive）</li>
     *   <li>捡泡泡（queryBubbleTask + pickBubbleTaskEnergy）</li>
     *   <li>走路建造 / 旧版行走（queryBaseinfo + queryMapInfo/Build/WalkGrid 等）</li>
     * </ol>
     */
    @Suppress("GrazieInspection")
    inner class NeverlandTaskHandler {

        private val TAG = "Neverland"

        /** @brief 最大失败次数（优先使用全局配置，默认 5 次） */
        private val MAX_ERROR_COUNT: Int =
            if (ApplicationHook.config.setMaxErrorCount.value > 0) ApplicationHook.config.setMaxErrorCount.value else 5

        /** @brief 任务循环间隔（毫秒） */
        private val TASK_LOOP_DELAY: Long = 1000

        /**
         * @brief 健康岛任务入口
         */
        suspend fun runNeverland() {
            try {
                Log.record(TAG, "开始执行健康岛任务")
                if (neverlandTask.value) {
                    // 1. 签到
                    neverlandDoSign()
                    // 2. 任务大厅循环处理
                    loopHandleTaskCenter()
                    // 3. 浏览任务
                    handleHealthIslandTask()
                    // 4. 捡泡泡
                    neverlandPickAllBubble()
                }

                if (neverlandGrid.value) {
                    // 5. 自动走路建造
                    neverlandAutoTask()
                }

                Log.record(TAG, "健康岛任务结束")
            } catch (t: Throwable) {
                Log.printStackTrace(TAG, "runNeverland err:", t)
            }
        }

        // ---------------------------------------------------------------
        // 1. 健康岛签到
        // ---------------------------------------------------------------

        /**
         * @brief 健康岛签到流程
         */
        private suspend fun neverlandDoSign() {
            try {
                if (Status.hasFlagToday("AntSports::neverlandDoSign::已签到")) return

                Log.record(TAG, "健康岛 · 检查签到状态")
                val jo = JSONObject(AntSportsRpcCall.NeverlandRpcCall.querySign(3, "jkdsportcard"))

                if (!ResChecker.checkRes(TAG + "查询签到失败:", jo) ||
                    !ResChecker.checkRes(TAG, jo) ||
                    jo.optJSONObject("data") == null
                ) {
                    val errorCode = jo.optString("errorCode", "")
                    if ("ALREADY_SIGN_IN" == errorCode ||
                        "已签到" == jo.optString("errorMsg", "")
                    ) {
                        Status.setFlagToday("AntSports::neverlandDoSign::已签到")
                    }
                    return
                }

                val data = jo.getJSONObject("data")
                val signInfo = data.optJSONObject("continuousSignInfo")
                if (signInfo != null && signInfo.optBoolean("signedToday", false)) {
                    Log.record(
                        TAG,
                        "今日已签到 ✔ 连续：${signInfo.optInt("continuitySignedDayCount")} 天"
                    )
                    return
                }

                Log.record(TAG, "健康岛 · 正在签到…")
                val signRes = JSONObject(AntSportsRpcCall.NeverlandRpcCall.takeSign(3, "jkdsportcard"))

                if (!ResChecker.checkRes(TAG + "签到失败:", signRes) ||
                    !ResChecker.checkRes(TAG, signRes) ||
                    signRes.optJSONObject("data") == null
                ) {
                    Log.error(TAG, "takeSign raw=$signRes")
                    Status.setFlagToday("AntSports::neverlandDoSign::已签到")
                    return
                }

                val signData = signRes.getJSONObject("data")
                val reward = signData.optJSONObject("continuousDoSignInVO")
                val rewardAmount = reward?.optInt("rewardAmount", 0) ?: 0
                val rewardType = reward?.optString("rewardType", "") ?: ""
                val signInfoAfter = signData.optJSONObject("continuousSignInfo")
                val newContinuity = signInfoAfter?.optInt("continuitySignedDayCount", -1) ?: -1

                Log.other(
                    "健康岛签到成功 🎉 +" + rewardAmount + rewardType +
                        " 连续：" + newContinuity + " 天"
                )
                Status.setFlagToday("AntSports::neverlandDoSign::已签到")
            } catch (t: Throwable) {
                Log.printStackTrace(TAG, "neverlandDoSign err:$t", t)
            }
        }

        // ---------------------------------------------------------------
        // 2. 任务大厅循环处理
        // ---------------------------------------------------------------

        /**
         * @brief 循环处理健康岛任务大厅中的 PROMOKERNEL_TASK & LIGHT_TASK
         */
        private suspend fun loopHandleTaskCenter() {
            var errorCount = 0
            Log.record(TAG, "开始循环处理任务大厅（失败限制：$MAX_ERROR_COUNT 次）")

            while (true) {
                try {
                    if (errorCount >= MAX_ERROR_COUNT) {
                        Log.error(TAG, "任务处理失败次数达到上限，停止循环")
                        Status.setFlagToday(StatusFlags.FLAG_ANTSPORTS_TASK_CENTER_DONE)
                        break
                    }

                    val taskCenterResp = JSONObject(AntSportsRpcCall.NeverlandRpcCall.queryTaskCenter())
                    if (!ResChecker.checkRes(TAG, taskCenterResp) ||
                        taskCenterResp.optJSONObject("data") == null
                    ) {
                        errorCount++
                        GlobalThreadPools.sleepCompat(TASK_LOOP_DELAY)
                        continue
                    }

                    val taskList = taskCenterResp.getJSONObject("data").optJSONArray("taskCenterTaskVOS")
                    if (taskList == null || taskList.length() == 0) {
                        Log.other("任务中心为空，无任务可处理")
                        break
                    }

                    val pendingTasks = mutableListOf<JSONObject>()
                    for (i in 0 until taskList.length()) {
                        val task = taskList.optJSONObject(i) ?: continue

                        val title = task.optString("title", task.optString("taskName", "未知任务"))
                        val type = task.optString("taskType", "")
                        val status = task.optString("taskStatus", "")
                        val taskId = task.optString("id", task.optString("taskId", ""))

                        if ("NOT_SIGNUP" == status) {
                            Log.record(TAG, "任务 [$title] 需要手动报名，已自动拉黑并跳过")
                            if (taskId.isNotEmpty()) {
                                TaskBlacklist.addToBlacklist(taskId, title)
                            }
                            continue
                        }

                        if (TaskBlacklist.isTaskInBlacklist(taskId)) continue

                        if (("PROMOKERNEL_TASK" == type || "LIGHT_TASK" == type) &&
                            "FINISHED" != status
                        ) {
                            pendingTasks.add(task)
                        }
                    }

                    if (pendingTasks.isEmpty()) {
                        Log.record(TAG, "没有可处理或领取的任务，退出循环")
                        break
                    }

                    Log.record(TAG, "本次发现 ${pendingTasks.size} 个可处理任务（含待领取）")

                    var currentBatchError = 0
                    for (task in pendingTasks) {
                        val ok = handleSingleTask(task)
                        if (!ok) currentBatchError++
                        GlobalThreadPools.sleepCompat(3000)
                    }

                    errorCount += currentBatchError
                    Log.record(TAG, "当前批次执行完毕，准备下一次刷新检查")
                    GlobalThreadPools.sleepCompat(TASK_LOOP_DELAY)
                } catch (t: Throwable) {
                    errorCount++
                    Log.printStackTrace(TAG, "循环异常", t)
                }
            }
        }

        /**
         * @brief 处理单个大厅任务
         */
        private suspend fun handleSingleTask(task: JSONObject): Boolean {
            return try {
                val title = task.optString("title", "未知任务")
                val type = task.optString("taskType", "")
                val status = task.optString("taskStatus", "")
                val jumpLink = task.optString("jumpLink", "")

                Log.record(TAG, "任务：[$title] 状态：$status 类型：$type")

                if ("TO_RECEIVE" == status) {
                    try {
                        task.put("scene", "MED_TASK_HALL")
                        if (!task.has("source")) {
                            task.put("source", "jkdsportcard")
                        }

                        val res = JSONObject(AntSportsRpcCall.NeverlandRpcCall.taskReceive(task))
                        if (res.optBoolean("success", false)) {
                            val data = res.optJSONObject("data")
                            var rewardDetail = ""
                            if (data != null && data.has("userItems")) {
                                val items = data.getJSONArray("userItems")
                                val sb = StringBuilder()
                                for (i in 0 until items.length()) {
                                    val item = items.getJSONObject(i)
                                    val name = item.optString("name", "未知奖励")
                                    val amount = item.optInt("modifyCount", 0)
                                    val total = item.optInt("count", 0)
                                    sb.append("[").append(name)
                                        .append(" +").append(amount)
                                        .append(" (余:").append(total).append(")] ")
                                }
                                rewardDetail = sb.toString()
                            }
                            Log.record(TAG, "完成[$title]✔$rewardDetail")
                            return true
                        } else {
                            val errorMsg = res.optString("errorMsg", "未知错误")
                            val errorCode = res.optString("errorCode", "UNKNOWN")
                            Log.error(TAG, "❌ 奖励领取失败 [$errorCode]: $errorMsg")
                            return false
                        }
                    } catch (e: Exception) {
                        Log.error(TAG, "领取流程异常: ${e.message}")
                        return false
                    }
                }

                if ("SIGNUP_COMPLETE" == status || "INIT" == status) {
                    return when (type) {
                        "PROMOKERNEL_TASK" -> handlePromoKernelTask(task, title)
                        "LIGHT_TASK" -> handleLightTask(task, title, jumpLink)
                        else -> {
                            Log.error(TAG, "未处理的任务类型：$type")
                            false
                        }
                    }
                }

                Log.record(TAG, "任务状态为 $status，跳过执行")
                true
            } catch (e: Exception) {
                Log.printStackTrace(TAG, "handleSingleTask 异常", e)
                false
            }
        }

        // ---------------------------------------------------------------
        // 3. 健康岛浏览任务
        // ---------------------------------------------------------------

        /**
         * @brief 处理健康岛浏览任务（LIGHT_FEEDS_TASK）
         */
        private suspend fun handleHealthIslandTask() {
            try {
                Log.record(TAG, "开始检查健康岛浏览任务")
                var hasTask = true
                while (hasTask) {
                    val taskInfoResp = JSONObject(
                        AntSportsRpcCall.NeverlandRpcCall.queryTaskInfo(
                            "health-island",
                            "LIGHT_FEEDS_TASK"
                        )
                    )

                    if (!ResChecker.checkRes(TAG + "查询健康岛浏览任务失败:", taskInfoResp) ||
                        taskInfoResp.optJSONObject("data") == null
                    ) {
                        Log.error(TAG, "健康岛浏览任务查询失败 [$taskInfoResp] 请关闭此功能")
                        return
                    }

                    val taskInfos = taskInfoResp.getJSONObject("data").optJSONArray("taskInfos")
                    if (taskInfos == null || taskInfos.length() == 0) {
                        Log.record(TAG, "健康岛浏览任务列表为空")
                        hasTask = false
                        continue
                    }

                    for (i in 0 until taskInfos.length()) {
                        val taskInfo = taskInfos.getJSONObject(i)
                        val encryptValue = taskInfo.optString("encryptValue")
                        val energyNum = taskInfo.optInt("energyNum", 0)
                        val viewSec = taskInfo.optInt("viewSec", 15)

                        if (encryptValue.isEmpty()) {
                            Log.error(TAG, "健康岛任务 encryptValue 为空，跳过")
                            continue
                        }

                        Log.record(TAG, "健康岛浏览任务：能量+$energyNum，需等待${viewSec}秒")
                        GlobalThreadPools.sleepCompat((viewSec / 3).toLong())

                        val receiveResp = JSONObject(
                            AntSportsRpcCall.NeverlandRpcCall.energyReceive(
                                encryptValue,
                                energyNum,
                                "LIGHT_FEEDS_TASK",
                                null
                            )
                        )
                        if (ResChecker.checkRes(TAG + "领取健康岛任务奖励:", receiveResp) &&
                            ResChecker.checkRes(TAG, receiveResp)
                        ) {
                            Log.other("✅ 健康岛浏览任务完成，获得能量+$energyNum")
                        } else {
                            Log.error(TAG, "健康岛任务领取失败: $receiveResp")
                        }

                        GlobalThreadPools.sleepCompat(1000)
                    }
                }
            } catch (t: Throwable) {
                Log.printStackTrace(TAG, "handleHealthIslandTask err", t)
            }
        }

        // ---------------------------------------------------------------
        // 4. PROMOKERNEL_TASK / LIGHT_TASK 处理
        // ---------------------------------------------------------------

        /**
         * @brief 处理 PROMOKERNEL_TASK（活动类任务）
         */
        private suspend fun handlePromoKernelTask(task: JSONObject, title: String): Boolean {
            return try {
                task.put("scene", "MED_TASK_HALL")
                val res = JSONObject(AntSportsRpcCall.NeverlandRpcCall.taskSend(task))
                if (ResChecker.checkRes(TAG, res)) {
                    Log.other("✔ 活动任务完成：$title")
                    true
                } else {
                    Log.error(TAG, "taskSend 失败: $task 响应：$res")
                    false
                }
            } catch (e: Exception) {
                Log.printStackTrace(TAG, "handlePromoKernelTask 处理 PROMOKERNEL_TASK 异常（$title）", e)
                false
            }
        }

        /**
         * @brief 处理 LIGHT_TASK（浏览类任务）
         */
        private suspend fun handleLightTask(task: JSONObject, title: String, jumpLink: String): Boolean {
            return try {
                var bizId = task.optString("bizId", "")
                if (bizId.isEmpty()) {
                    val logExtMap = task.optJSONObject("logExtMap")
                    if (logExtMap != null) {
                        bizId = logExtMap.optString("bizId", "")
                    }
                }

                if (bizId.isEmpty()) {
                    Log.error(TAG, "LIGHT_TASK 未找到 bizId：$title jumpLink=$jumpLink")
                    return false
                }

                val res = JSONObject(AntSportsRpcCall.NeverlandRpcCall.finish(bizId))
                if (res.optBoolean("success", false) ||
                    "0" == res.optString("errCode", "")
                ) {
                    var rewardMsg = ""
                    val extendInfo = res.optJSONObject("extendInfo")
                    if (extendInfo != null) {
                        val rewardInfo = extendInfo.optJSONObject("rewardInfo")
                        if (rewardInfo != null) {
                            val amount = rewardInfo.optString("rewardAmount", "0")
                            rewardMsg = " (获得奖励: $amount 能量)"
                        }
                    }
                    Log.other("✔ 浏览任务完成：$title$rewardMsg")
                    true
                } else {
                    Log.error(TAG, "完成 LIGHT_TASK 失败: $title 返回: $res")
                    false
                }
            } catch (e: Exception) {
                Log.printStackTrace(TAG, "handleLightTask 处理 LIGHT_TASK 异常（$title）", e)
                false
            }
        }

        // ---------------------------------------------------------------
        // 5. 捡泡泡
        // ---------------------------------------------------------------

        /**
         * @brief 健康岛捡泡泡 + 浏览类泡泡任务
         */
        private suspend fun neverlandPickAllBubble() {
            try {
                Log.record(TAG, "健康岛 · 检查可领取泡泡")

                val jo = JSONObject(AntSportsRpcCall.NeverlandRpcCall.queryBubbleTask())

                if (!ResChecker.checkRes(TAG + "查询泡泡失败:", jo) ||
                    jo.optJSONObject("data") == null
                ) {
                    Log.error(TAG, "queryBubbleTask raw=$jo")
                    return
                }

                val arr = jo.getJSONObject("data").optJSONArray("bubbleTaskVOS")
                if (arr == null || arr.length() == 0) {
                    Log.other("无泡泡可领取")
                    return
                }

                val ids = mutableListOf<String>()
                val encryptValues = mutableListOf<String>()

                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    val bubbleTaskStatus = item.optString("bubbleTaskStatus")
                    val encryptValue = item.optString("encryptValue")
                    val energyNum = item.optInt("energyNum", 0)
                    val viewSec = item.optInt("viewSec", 15)

                    if ("INIT" == bubbleTaskStatus && encryptValue.isNotEmpty()) {
                        encryptValues.add(encryptValue)
                        Log.record(
                            TAG,
                            "找到可浏览任务： ${item.optString("title")}，能量+$energyNum，需等待${viewSec}秒"
                        )
                    } else if (!item.optBoolean("initState") &&
                        item.optString("medEnergyBallInfoRecordId").isNotEmpty()
                    ) {
                        ids.add(item.getString("medEnergyBallInfoRecordId"))
                    }
                }

                if (ids.isEmpty() && encryptValues.isEmpty()) {
                    Log.record(TAG, "没有可领取的泡泡任务")
                    return
                }

                if (ids.isNotEmpty()) {
                    Log.record(TAG, "健康岛 · 正在领取 ${ids.size} 个泡泡…")
                    val pick = JSONObject(AntSportsRpcCall.NeverlandRpcCall.pickBubbleTaskEnergy(ids))

                    if (!ResChecker.checkRes(TAG + "领取泡泡失败:", pick) ||
                        pick.optJSONObject("data") == null
                    ) {
                        Log.error(TAG, "pickBubbleTaskEnergy raw=$pick")
                        return
                    }

                    val data = pick.getJSONObject("data")
                    val changeAmount = data.optString("changeAmount", "0")
                    val balance = data.optString("balance", "0")
                    if (changeAmount == "0") {
                        Log.record(TAG, "健康岛 · 本次未获得任何能量")
                    } else {
                        Log.other("捡泡泡成功 🎈 +$changeAmount 余额：$balance")
                    }
                }

                for (encryptValue in encryptValues) {
                    Log.record(TAG, "开始浏览任务，任务 encryptValue: $encryptValue")

                    for (i in 0 until arr.length()) {
                        val item = arr.getJSONObject(i)
                        if (encryptValue == item.optString("encryptValue")) {
                            val energyNum = item.optInt("energyNum", 0)
                            val viewSec = item.optInt("viewSec", 15)
                            val title = item.optString("title")

                            GlobalThreadPools.sleepCompat(viewSec * 1000L)

                            val receiveResp = JSONObject(
                                AntSportsRpcCall.NeverlandRpcCall.energyReceive(
                                    encryptValue,
                                    energyNum,
                                    "LIGHT_FEEDS_TASK",
                                    "adBubble"
                                )
                            )

                            if (ResChecker.checkRes(TAG + "领取泡泡任务奖励:", receiveResp)) {
                                Log.other("✅ 浏览任务[$title]完成，获得能量+$energyNum")
                            } else {
                                Log.error(TAG, "浏览任务领取失败: $receiveResp")
                            }

                            GlobalThreadPools.sleepCompat((1000 + Math.random() * 1000).toLong())
                            break
                        }
                    }
                }
            } catch (t: Throwable) {
                Log.printStackTrace(TAG, "neverlandPickAllBubble err:", t)
            }
        }

        // ---------------------------------------------------------------
        // 6. 自动走路建造（步数限制 + 能量限制）
        // ---------------------------------------------------------------

        /**
         * @brief 检查今日步数是否达到上限
         * @return 剩余可走步数（<=0 表示已达上限）
         */
        private fun checkDailyStepLimit(): Int {
            var stepCount = Status.getIntFlagToday(StatusFlags.FLAG_NEVERLAND_STEP_COUNT) ?: 0
            val maxStepLimit = neverlandGridStepCount.value
            val remainSteps = maxStepLimit - stepCount

            Log.record(
                TAG,
                String.format(
                    "今日步数统计: 已走 %d/%d 步, 剩余 %d 步",
                    stepCount,
                    maxStepLimit,
                    max(0, remainSteps)
                )
            )
            return remainSteps
        }

        /**
         * @brief 记录步数增加
         * @param addedSteps 本次增加的步数
         * @return 更新后的总步数
         */
        private fun recordStepIncrease(addedSteps: Int): Int {
            if (addedSteps <= 0) {
                return Status.getIntFlagToday(StatusFlags.FLAG_NEVERLAND_STEP_COUNT) ?: 0
            }
            var currentSteps = Status.getIntFlagToday(StatusFlags.FLAG_NEVERLAND_STEP_COUNT) ?: 0
            val newSteps = currentSteps + addedSteps
            Status.setIntFlagToday(StatusFlags.FLAG_NEVERLAND_STEP_COUNT, newSteps)
            val maxLimit = neverlandGridStepCount.value
            Log.record(
                TAG,
                String.format(
                    "步数增加: +%d 步, 当前总计 %d/%d 步",
                    addedSteps,
                    newSteps,
                    maxLimit
                )
            )
            return newSteps
        }

        /**
         * @brief 健康岛走路建造任务入口
         */
        private suspend fun neverlandAutoTask() {
            try {
                Log.record(TAG, "健康岛 · 启动走路建造任务")

                val baseInfo = JSONObject(AntSportsRpcCall.NeverlandRpcCall.queryBaseinfo())
                if (!ResChecker.checkRes(TAG + " 查询基础信息失败:", baseInfo) ||
                    baseInfo.optJSONObject("data") == null
                ) {
                    Log.error(TAG, "queryBaseinfo 失败, 响应数据: $baseInfo")
                    return
                }

                val baseData = baseInfo.getJSONObject("data")
                val isNewGame = baseData.optBoolean("newGame", false)
                var branchId = baseData.optString("branchId", "MASTER")
                var mapId = baseData.optString("mapId", "")
                val mapName = baseData.optString("mapName", "未知地图")

                Log.record(
                    TAG,
                    String.format(
                        "当前地图: [%s](%s) | 模式: %s",
                        mapName,
                        mapId,
                        if (isNewGame) "新游戏建造" else "旧版行走"
                    )
                )

                var remainSteps = checkDailyStepLimit()
                if (remainSteps <= 0) {
                    Log.record(TAG, "今日步数已达上限, 任务结束")
                    return
                }

                var leftEnergy = queryUserEnergy()
                if (leftEnergy < 5) {
                    Log.record(TAG, "剩余能量不足(< 5), 无法执行任务")
                    return
                }

                if (isNewGame) {
                    executeAutoBuild(branchId, mapId, remainSteps, leftEnergy, mapName)
                } else {
                    executeAutoWalk(branchId, mapId, remainSteps, leftEnergy, mapName)
                }

                Log.record(TAG, "健康岛自动走路建造执行完成 ✓")
            } catch (t: Throwable) {
                Log.printStackTrace(TAG, "neverlandAutoTask 发生异常$t", t)
            }
        }

        /**
         * @brief 查询用户剩余能量
         */
        private suspend fun queryUserEnergy(): Int {
            return try {
                val energyResp = JSONObject(AntSportsRpcCall.NeverlandRpcCall.queryUserEnergy())
                if (!ResChecker.checkRes(TAG + " 查询用户能量失败:", energyResp) ||
                    energyResp.optJSONObject("data") == null
                ) {
                    Log.error(TAG, "queryUserEnergy 失败, 响应数据: $energyResp")
                    0
                } else {
                    val balance = energyResp.getJSONObject("data").optInt("balance", 0)
                    Log.record(TAG, "当前剩余能量: $balance")
                    balance
                }
            } catch (t: Throwable) {
                Log.printStackTrace(TAG, "queryUserEnergy err", t)
                0
            }
        }

        /**
         * @brief 执行旧版行走任务（能量泵走路模式）
         */
        private suspend fun executeAutoWalk(
            branchId: String,
            mapId: String,
            remainSteps: Int,
            leftEnergyInit: Int,
            mapName: String
        ) {
            var leftEnergy = leftEnergyInit
            try {
                Log.record(TAG, "开始执行旧版行走任务")
                val mapInfoResp = JSONObject(
                    AntSportsRpcCall.NeverlandRpcCall.queryMapInfo(mapId, branchId)
                )

                if (!ResChecker.checkRes(TAG + " queryMapInfo 失败:", mapInfoResp) ||
                    mapInfoResp.optJSONObject("data") == null
                ) {
                    Log.error(TAG, "queryMapInfo 失败，终止走路任务")
                    return
                }

                val mapInfo = mapInfoResp.getJSONObject("data")
                if (!mapInfo.optBoolean("canWalk", false)) {
                    Log.record(TAG, "当前地图不可走(canWalk=false)，跳过走路任务")
                    return
                }

                val mapStarData = mapInfo.optJSONObject("starData")
                var lastCurrStar = mapStarData?.optInt("curr", 0) ?: 0

                for (i in 0 until remainSteps) {
                    if (leftEnergy < 5) {
                        Log.record(TAG, "[$mapName] 能量不足(< 5), 停止走路任务")
                        break
                    }

                    val walkResp = JSONObject(
                        AntSportsRpcCall.NeverlandRpcCall.walkGrid(branchId, mapId, false)
                    )

                    if (!ResChecker.checkRes(TAG + " walkGrid 失败:", walkResp) ||
                        walkResp.optJSONObject("data") == null
                    ) {
                        val errorCode = walkResp.optString("errorCode", "")
                        Log.error(
                            TAG,
                            String.format(
                                "walkGrid 失败, 错误码: %s, 响应数据: %s",
                                errorCode,
                                walkResp
                            )
                        )
                        break
                    }

                    val walkData = walkResp.getJSONObject("data")
                    leftEnergy = walkData.optInt("leftCount", leftEnergy)

                    recordStepIncrease(1)
                    val stepThisTime = extractStepIncrease(walkData)

                    val starData = walkData.optJSONObject("starData")
                    val currStar = starData?.optInt("curr", lastCurrStar) ?: lastCurrStar
                    val maxStar = starData?.optInt("count", 0) ?: Int.MAX_VALUE
                    val starIncreased = currStar > lastCurrStar
                    lastCurrStar = currStar

                    var redPocketAdd = 0
                    val userItems = walkData.optJSONArray("userItems")
                    if (userItems != null && userItems.length() > 0) {
                        val item = userItems.optJSONObject(0)
                        if (item != null) {
                            redPocketAdd = item.optInt("modifyCount", item.optInt("count", 0))
                        }
                    }

                    val sb = StringBuilder()
                    sb.append("[").append(mapName).append("] 前进 ")
                        .append(stepThisTime).append(" 步，")

                    if (starIncreased) {
                        sb.append("获得 🌟")
                    } else if (redPocketAdd > 0) {
                        sb.append("获得 🧧 +").append(redPocketAdd)
                    } else {
                        sb.append("啥也没有")
                    }

                    Log.other(sb.toString())

                    tryReceiveStageReward(branchId, mapId, starData)

                    if (currStar >= maxStar) {
                        Log.other("[$mapName] 当前地图已完成星星，准备切换地图")
                        chooseAvailableMap()
                        break
                    }
                    delay(888)
                }
                Log.record(TAG, "自动走路任务完成 ✓")
            } catch (t: Throwable) {
                Log.printStackTrace(TAG, "executeAutoWalk err", t)
            }
        }

        /**
         * @brief 若有未领取的关卡奖励则尝试领取
         */
        private suspend fun tryReceiveStageReward(branchId: String, mapId: String, starData: JSONObject?) {
            if (starData == null) return

            val rewardLevel = starData.optInt("rewardLevel", -1)
            if (rewardLevel <= 0) return

            val recordArr = starData.optJSONArray("stageRewardRecord")
            if (recordArr != null) {
                for (i in 0 until recordArr.length()) {
                    if (recordArr.optInt(i, -1) == rewardLevel) return
                }
            }

            Log.other(String.format("检测到未领取关卡奖励 🎁 map=%s 等级: %d，尝试领取…", mapId, rewardLevel))

            val rewardStr = try {
                AntSportsRpcCall.NeverlandRpcCall.mapStageReward(branchId, rewardLevel, mapId)
            } catch (t: Throwable) {
                Log.printStackTrace(TAG, "mapStageReward RPC 调用异常", t)
                return
            }.trim()

            if (rewardStr.isEmpty()) {
                Log.error(TAG, "mapStageReward 返回空字符串")
                return
            }
            if (!rewardStr.startsWith("{")) {
                Log.error(TAG, "mapStageReward 返回非 JSON: $rewardStr")
                return
            }

            val rewardResp = try {
                JSONObject(rewardStr)
            } catch (t: Throwable) {
                Log.printStackTrace(TAG, "mapStageReward JSON 解析失败", t)
                return
            }

            if (!ResChecker.checkRes(TAG, rewardResp)) {
                val errCode = rewardResp.optString("errorCode", "")
                if ("ASSET_ITEM_NOT_EXISTED" == errCode) {
                    Log.other("关卡奖励已被领取或不存在（可忽略）")
                } else {
                    Log.error(TAG, "领取关卡奖励失败: $rewardResp")
                }
                return
            }

            val data = rewardResp.optJSONObject("data")
            val receiveResult = data?.optJSONObject("receiveResult")
            if (receiveResult == null) {
                Log.record(TAG, "关卡奖励领取成功 🎉（无奖励详情）")
                return
            }

            val prizes = receiveResult.optJSONArray("prizes")
            val balance = receiveResult.optString("balance", "")

            if (prizes != null && prizes.length() > 0) {
                val sb = StringBuilder()
                for (i in 0 until prizes.length()) {
                    val p = prizes.optJSONObject(i) ?: continue
                    sb.append(p.optString("title", "未知奖励"))
                        .append(" x")
                        .append(p.optString("modifyCount", "1"))
                    if (i != prizes.length() - 1) sb.append("，")
                }
                Log.other(
                    String.format(
                        "Lv.%s 奖励领取成功 🎉 %s | 当前余额: %s",
                        rewardLevel,
                        sb.toString(),
                        balance
                    )
                )
            } else {
                Log.other("关卡奖励领取成功 🎉（无可展示奖励）")
            }
        }

        /**
         * @brief 查询地图列表，优先返回 DOING 地图，否则随机选择 LOCKED 地图并切换
         */
        private suspend fun chooseAvailableMap(): JSONObject? {
            return try {
                val mapResp = JSONObject(AntSportsRpcCall.NeverlandRpcCall.queryMapList())
                if (!ResChecker.checkRes(TAG + " 查询地图失败:", mapResp)) {
                    Log.error(TAG, "queryMapList 失败: $mapResp")
                    return null
                }

                val data = mapResp.optJSONObject("data")
                val mapList = data?.optJSONArray("mapList")
                if (mapList == null || mapList.length() == 0) {
                    Log.error(TAG, "地图列表为空")
                    return null
                }

                var doingMap: JSONObject? = null
                val lockedMaps = mutableListOf<JSONObject>()
                for (i in 0 until mapList.length()) {
                    val map = mapList.getJSONObject(i)
                    val status = map.optString("status")
                    if ("DOING" == status) {
                        doingMap = map
                        break
                    } else if ("LOCKED" == status) {
                        lockedMaps.add(map)
                    }
                }

                if (doingMap != null) {
                    Log.other(
                        "当前 DOING 地图: " + doingMap.optString("mapName") +
                            doingMap.optString("mapId") + " → 执行一次强制切换确保状态一致"
                    )
                    return chooseMap(doingMap)
                }

                if (lockedMaps.isEmpty()) {
                    Log.error(TAG, "没有 DOING 且没有可选的 LOCKED 地图")
                    return null
                }

                val chosenLocked = lockedMaps[Random().nextInt(lockedMaps.size)]
                Log.other("随机选择 LOCKED 地图: " + chosenLocked.optString("mapId"))
                chooseMap(chosenLocked)
            } catch (t: Throwable) {
                Log.printStackTrace(TAG, "chooseAvailableMap err", t)
                null
            }
        }

        /**
         * @brief 切换当前地图
         */
        private suspend fun chooseMap(map: JSONObject): JSONObject? {
            return try {
                val mapId = map.optString("mapId")
                val branchId = map.optString("branchId")
                val resp = JSONObject(
                    AntSportsRpcCall.NeverlandRpcCall.chooseMap(branchId, mapId)
                )
                if (ResChecker.checkRes(TAG, resp)) {
                    Log.record(TAG, "切换地图成功: $mapId")
                    map
                } else {
                    Log.error(TAG, "切换地图失败: $resp")
                    null
                }
            } catch (t: Throwable) {
                Log.printStackTrace(TAG, "chooseMap err", t)
                null
            }
        }

        /**
         * @brief 从 walkData 中提取步数增量
         */
        private fun extractStepIncrease(walkData: JSONObject): Int {
            return try {
                val mapAwards = walkData.optJSONArray("mapAwards")
                if (mapAwards != null && mapAwards.length() > 0) {
                    mapAwards.getJSONObject(0).optInt("step", 0)
                } else {
                    0
                }
            } catch (t: Throwable) {
                Log.printStackTrace(TAG, t)
                0
            }
        }

        /**
         * @brief 执行自动建造任务（新游戏模式）
         */
        private suspend fun executeAutoBuild(
            branchIdInit: String,
            mapIdInit: String,
            remainStepsInit: Int,
            leftEnergyInit: Int,
            mapName: String
        ) {
            var branchId = branchIdInit
            var mapId = mapIdInit
            var remainSteps = remainStepsInit
            var leftEnergy = leftEnergyInit
            try {
                Log.other(String.format("开始执行建造任务, 地图: %s", mapId))

                val resp = AntSportsRpcCall.NeverlandRpcCall.queryMapInfoNew(mapId)
                val mapInfo = JSONObject(resp)

                if (!ResChecker.checkRes(TAG + " 查询建造地图失败", mapInfo)) {
                    Log.error(TAG, "查询建造地图失败 $mapInfo")
                    return
                }
                val data = mapInfo.optJSONObject("data")
                if (data == null) {
                    Log.error(TAG, "地图Data 为空，无法解析")
                    return
                }

                val mapEnergyFinal = data.optInt("mapEnergyFinal")
                val mapEnergyProcess = data.optInt("mapEnergyProcess")
                val buildings = data.optJSONArray("buildingConfigInfos")
                var lastBuildingIndex = -1
                if (buildings != null && buildings.length() > 0) {
                    lastBuildingIndex = buildings.getJSONObject(buildings.length() - 1)
                        .optInt("buildingIndex", -1)
                    Log.record(TAG, "最后一个建筑 Index: $lastBuildingIndex")
                }

                if (mapEnergyProcess == mapEnergyFinal) {
                    Log.record(TAG, "当前地图已建造完成，准备切换地图...")
                    val choiceMapInfo = chooseAvailableMap()
                    if (choiceMapInfo == null) {
                        Log.error(TAG, "切换地图失败，可能无可用地图，任务终止。")
                        return
                    }
                    if (choiceMapInfo.optBoolean("newIsLandFlg", true)) {
                        branchId = choiceMapInfo.optString("branchId")
                        mapId = choiceMapInfo.optString("mapId")
                        Log.record(TAG, "成功切换到可建造的新地图: $mapId，继续执行建造。")
                    } else {
                        Log.record(TAG, "已切换至走路地图: $mapId，将在下次运行时执行，任务终止。")
                        return
                    }
                }

                while (remainSteps > 0 && leftEnergy >= 5) {
                    val maxMulti = min(10, remainSteps)
                    val energyBasedMulti = leftEnergy / 5
                    val multiNum = min(maxMulti, energyBasedMulti)

                    val buildResp = JSONObject(
                        AntSportsRpcCall.NeverlandRpcCall.build(branchId, mapId, multiNum)
                    )
                    if (!ResChecker.checkRes(TAG + " build 失败:", buildResp)) {
                        Log.error(
                            TAG,
                            String.format(
                                "build 失败, multiNum=%d, 响应: %s",
                                multiNum,
                                buildResp
                            )
                        )
                        break
                    }

                    val buildData = buildResp.optJSONObject("data")
                    if (buildData == null || buildData.length() == 0) {
                        Log.record(TAG, "⚠️ build响应数据为空，当前地图已达限制，任务重新进入地图完成处理流程。")
                        chooseAvailableMap()
                        return
                    }

                    val before = buildData.optJSONObject("beforeStageInfo")
                    val end = buildData.optJSONObject("endStageInfo")
                    var actualUsedEnergy = 0

                    if (before != null && end != null) {
                        val bIdxBefore = before.optInt("buildingIndex")
                        val bIdxEnd = end.optInt("buildingIndex")
                        actualUsedEnergy = if (bIdxEnd > bIdxBefore) {
                            (before.optInt("buildingEnergyFinal") -
                                before.optInt("buildingEnergyProcess")) +
                                end.optInt("buildingEnergyProcess")
                        } else {
                            end.optInt("buildingEnergyProcess") -
                                before.optInt("buildingEnergyProcess")
                        }
                    } else {
                        actualUsedEnergy = multiNum * 5
                    }

                    leftEnergy -= actualUsedEnergy
                    val stepIncrease = calculateBuildSteps(buildData, multiNum)
                    val totalSteps = recordStepIncrease(stepIncrease)
                    remainSteps -= stepIncrease

                    val awardInfo = extractAwardInfo(buildData)

                    Log.other(
                        String.format(
                            "建造进度 🏗️ 倍数: x%d | 能量: %d | 本次: +%d | 今日: %d/%d%s",
                            multiNum,
                            leftEnergy,
                            stepIncrease,
                            totalSteps,
                            neverlandGridStepCount.value,
                            awardInfo
                        )
                    )
                    GlobalThreadPools.sleepCompat(1000)
                }
                Log.other("自动建造任务完成 ✓")
            } catch (t: Throwable) {
                Log.printStackTrace(TAG, "executeAutoBuild err", t)
            }
        }

        /**
         * @brief 计算建造实际产生的步数
         */
        private fun calculateBuildSteps(buildData: JSONObject?, defaultMulti: Int): Int {
            return try {
                val buildResults = buildData?.optJSONArray("buildResults")
                if (buildResults != null && buildResults.length() > 0) {
                    buildResults.length()
                } else {
                    defaultMulti
                }
            } catch (t: Throwable) {
                Log.printStackTrace(TAG, t)
                defaultMulti
            }
        }

        /**
         * @brief 从建造数据中提取奖励信息
         */
        private fun extractAwardInfo(buildData: JSONObject?): String {
            return try {
                val awards = buildData?.optJSONArray("awards")
                if (awards != null && awards.length() > 0) {
                    String.format(" | 获得奖励: %d 项", awards.length())
                } else {
                    ""
                }
            } catch (t: Throwable) {
                Log.printStackTrace(TAG, t)
                ""
            }
        }
    }

    // ---------------------------------------------------------------------
    // 配置用枚举/常量
    // ---------------------------------------------------------------------

    /**
     * @brief 抢好友模式
     */
    interface BattleForFriendType {
        companion object {
            const val ROB = 0
            const val DONT_ROB = 1
            val nickNames = arrayOf("选中抢", "选中不抢")
        }
    }
}
