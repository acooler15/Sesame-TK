package fansirsqi.xposed.sesame.task.antForest

import android.annotation.SuppressLint
import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.core.threads.GlobalThreadPools
import fansirsqi.xposed.sesame.core.util.ResChecker
import fansirsqi.xposed.sesame.core.util.TimeFormatter
import fansirsqi.xposed.sesame.core.util.TimeUtil
import fansirsqi.xposed.sesame.data.Status
import fansirsqi.xposed.sesame.hook.Toast
import fansirsqi.xposed.sesame.task.ModelTask
import fansirsqi.xposed.sesame.task.antForest.Privilege.youthPrivilege
import fansirsqi.xposed.sesame.util.maps.UserMap
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.Objects
import java.util.function.Consumer
import java.util.function.Supplier

/**
 * 蚂蚁森林道具管理
 */
internal class ForestItemManager(private val task: AntForest) {
    private val TAG = AntForest.TAG

    /**
     * 双击卡结束时间
     */
    @Volatile
    internal var doubleEndTime: Long = 0

    /**
     * 隐身卡结束时间
     */
    @Volatile
    internal var stealthEndTime: Long = 0

    /**
     * 炸弹卡结束时间
     */
    @Volatile
    internal var energyBombCardEndTime: Long = 0

    /**
     * 1.1倍能量卡结束时间
     */
    @Volatile
    internal var robExpandCardEndTime: Long = 0

    private val doubleCardLockObj = Mutex()

    private var cachedBagObject: JSONObject? = null
    private var lastQueryPropListTime: Long = 0

    internal var canConsumeAnimalProp = false

    internal suspend fun collectGivenProps(givenProps: JSONArray) {
        try {
            for (i in 0..<givenProps.length()) {
                val jo = givenProps.getJSONObject(i)
                val giveConfigId = jo.getString("giveConfigId")
                val giveId = jo.getString("giveId")
                val propConfig = jo.getJSONObject("propConfig")
                val propName = propConfig.getString("propName")
                try {
                    val response = AntForestRpcCall.collectProp(giveConfigId, giveId)
                    val responseObj = JSONObject(response)
                    if (ResChecker.checkRes(TAG + "领取道具失败:", responseObj)) {
                        val str = "领取道具🎭[$propName]"
                        Log.forest(str)
                        Toast.show(str)
                    } else {
                        Log.record(
                            TAG,
                            "领取道具🎭[" + propName + "]失败:" + responseObj.getString("resultDesc")
                        )
                        Log.record(response)
                    }
                } catch (e: Exception) {
                    Log.printStackTrace(TAG, "领取道具时发生错误: " + e.message, e)
                }
                GlobalThreadPools.sleepCompat(1000L)
            }
        } catch (e: JSONException) {
            Log.printStackTrace(TAG, "givenProps JSON解析错误: " + e.message, e)
        }
    }

    /**
     * 处理用户派遣道具, 如果用户有派遣道具，则收取派遣动物滴能量
     *
     * @param selfHomeObj 用户主页信息的JSON对象
     */
    internal suspend fun handleUserProps(selfHomeObj: JSONObject) {
        try {
            val usingUserProps = if (task.isTeam(selfHomeObj)) {
                selfHomeObj.optJSONObject("teamHomeResult")
                    ?.optJSONObject("mainMember")
                    ?.optJSONArray("usingUserProps")
                    ?: JSONArray()  // 提供默认值
            } else {
                selfHomeObj.optJSONArray("usingUserPropsNew") ?: JSONArray()
            }
            canConsumeAnimalProp = true
            if (usingUserProps.length() == 0) {
                return  // 如果没有使用中的用户道具，直接返回
            }
            //            Log.runtime(TAG, "尝试遍历使用中的道具:" + usingUserProps);
            for (i in 0..<usingUserProps.length()) {
                val jo = usingUserProps.getJSONObject(i)
                if ("animal" != jo.getString("propGroup")) {
                    continue  // 如果当前道具不是动物类型，跳过
                }
                canConsumeAnimalProp = false // 设置标志位，表示不可再使用动物道具
                val extInfo = JSONObject(jo.getString("extInfo"))
                if (extInfo.optBoolean("isCollected")) {
                    Log.record(TAG, "动物派遣能量已被收取")
                    continue  // 如果动物能量已经被收取，跳过
                }
                val propId = jo.getString("propId")
                val propType = jo.getString("propType")
                val shortDay = extInfo.getString("shortDay")
                val animalName = extInfo.getJSONObject("animal").getString("name")
                val response = AntForestRpcCall.collectAnimalRobEnergy(propId, propType, shortDay)
                val responseObj = JSONObject(response)
                if (ResChecker.checkRes(TAG + "收取动物派遣能量失败:", responseObj)) {
                    val energy = extInfo.optInt("energy", 0)
                    ForestStatistics.totalCollected += energy
                    val str = "收取[" + animalName + "]派遣能量🦩[" + energy + "g]"
                    Toast.show(str)
                    Log.forest(str)
                } else {
                    Log.record(TAG, "收取动物能量失败: " + responseObj.getString("resultDesc"))
                    Log.record(response)
                }
                GlobalThreadPools.sleepCompat(300L)
                break // 收取到一个动物能量后跳出循环
            }
        } catch (e: JSONException) {
            Log.printStackTrace(e)
        } catch (e: Exception) {
            Log.printStackTrace(TAG, "handleUserProps err", e)
        }
    }

    /**
     * 收取能量炸弹卡炸落的能量
     * 基于抓包数据：alipay.antforest.forest.h5.collectBombCardEnergy
     *
     * @param selfHomeObj 用户主页信息的JSON对象
     */
    internal suspend fun collectEnergyBomb(selfHomeObj: JSONObject) {
        try {
            val usingUserProps = if (task.isTeam(selfHomeObj)) {
                selfHomeObj.optJSONObject("teamHomeResult")
                    ?.optJSONObject("mainMember")
                    ?.optJSONArray("usingUserProps")
                    ?: JSONArray()
            } else {
                selfHomeObj.optJSONArray("usingUserPropsNew") ?: JSONArray()
            }

            if (usingUserProps.length() == 0) return

            for (i in 0..<usingUserProps.length()) {
                val jo = usingUserProps.getJSONObject(i)
                // 筛选能量炸弹卡
                if ("energyBombCard" != jo.getString("propGroup")) {
                    continue
                }

                // 检查是否有可收取的剩余能量
                val extInfoStr = jo.optString("extInfo")
                if (extInfoStr.isEmpty()) continue

                val extInfo = JSONObject(extInfoStr)
                val remainEnergy = extInfo.optInt("remainEnergy", 0)

                if (remainEnergy > 0) {
                    val propId = jo.getString("propId")
                    val propName = jo.getString("propName")

                    Log.record(TAG, "发现[$propName]有 $remainEnergy g能量待收取，尝试收取...")

                    // 调用 AntForestRpcCall 中的静态方法
                    val response = AntForestRpcCall.collectBombCardEnergy(propId)

                    val responseObj = JSONObject(response)
                    if (ResChecker.checkRes(TAG + "收取炸弹卡能量失败:", responseObj)) {
                        val collected = responseObj.optInt("collectEnergy", 0)
                        ForestStatistics.totalCollected += collected
                        val str = "收取炸弹卡能量💥[$collected g]"
                        Toast.show(str)
                        Log.forest(str)

                        // 收取成功后更新主页数据，避免重复显示
                        updateSelfHomePage()
                    } else {
                        Log.record(TAG, "收取炸弹卡失败: " + responseObj.getString("resultDesc"))
                    }
                }
            }
        } catch (e: Exception) {
            Log.printStackTrace(TAG, "collectEnergyBomb err", e)
        }
    }

    @Throws(JSONException::class)
    internal suspend fun updateSelfHomePage() {
        val s = AntForestRpcCall.queryHomePage()
        GlobalThreadPools.sleepCompat(100)
        val joHomePage = JSONObject(s)
        updateSelfHomePage(joHomePage)
    }

    /**
     * 更新使用中的的道具剩余时间
     *
     * @param joHomePage 首页 JSON 对象
     */
    internal suspend fun updateSelfHomePage(joHomePage: JSONObject) {
        try {

            val usingUserProps: JSONArray = if (task.isTeam(joHomePage)) {
                // 组队模式
                joHomePage.optJSONObject("teamHomeResult")
                    ?.optJSONObject("mainMember")
                    ?.optJSONArray("usingUserProps")
                    ?: JSONArray()
            } else {
                // 单人模式
                joHomePage.optJSONArray("usingUserPropsNew")
                    ?: JSONArray()
            }
            for (i in 0..<usingUserProps.length()) {
                val userUsingProp = usingUserProps.getJSONObject(i)
                val propGroup = userUsingProp.getString("propGroup")
                val propName = userUsingProp.optString("propName")
                when (propGroup) {
                    "doubleClick" -> {
                        doubleEndTime = userUsingProp.getLong("endTime")
                        Log.record(TAG, "$propName 剩余时间⏰：" + task.formatTimeDifference(doubleEndTime - System.currentTimeMillis()))
                    }

                    "stealthCard" -> {
                        stealthEndTime = userUsingProp.getLong("endTime")
                        Log.record(TAG, "$propName 剩余时间⏰️：" + task.formatTimeDifference(stealthEndTime - System.currentTimeMillis()))
                    }

                    "shield" -> {
                        task.shieldManager.shieldEndTime = userUsingProp.getLong("endTime")
                        Log.record(TAG, "$propName 剩余时间⏰：" + task.formatTimeDifference(task.shieldManager.shieldEndTime - System.currentTimeMillis()))
                    }

                    "energyBombCard" -> {
                        energyBombCardEndTime = userUsingProp.getLong("endTime")
                        Log.record(TAG, "$propName 剩余时间⏰：" + task.formatTimeDifference(energyBombCardEndTime - System.currentTimeMillis()))
                    }

                    "robExpandCard" -> {
                        val extInfo = userUsingProp.optString("extInfo")
                        robExpandCardEndTime = userUsingProp.getLong("endTime")
                        Log.record(TAG, "$propName 剩余时间⏰：" + task.formatTimeDifference(robExpandCardEndTime - System.currentTimeMillis()))
                        if (!extInfo.isEmpty()) {
                            val extInfoObj = JSONObject(extInfo)
                            val leftEnergy = extInfoObj.optString("leftEnergy", "0").toDouble()
                            if (leftEnergy > task.robExpandCardLimt!!.value || ("true" == extInfoObj.optString("overLimitToday", "false") && leftEnergy >= 1)) {
                                val propId = userUsingProp.getString("propId")
                                val propType = userUsingProp.getString("propType")
                                val jo = JSONObject(AntForestRpcCall.collectRobExpandEnergy(propId, propType))
                                if (ResChecker.checkRes(TAG, jo)) {
                                    val collectEnergy = jo.optInt("collectEnergy")
                                    Log.forest("翻倍能量🌳[" + collectEnergy + "g][$propName]")
                                }
                            }
                        }
                    }
                    else -> {
                         Log.record(TAG, "跳过非目标道具:$userUsingProp")
                    }
                }
            }
        } catch (th: Throwable) {
            Log.printStackTrace(TAG, "updateDoubleTime err", th)
        }
    }

    /**
     * 兑换隐身卡
     */
    private fun exchangeStealthCard(): Boolean {
        val skuId = "SK20230521000206"
        val spuId = "SP20230521000082"
        if (!Status.canVitalityExchangeToday(skuId, 1)) {
            return false
        }
        return Vitality.VitalityExchange(spuId, skuId, "隐身卡")
    }

    /**
     * 兑换双击卡
     * 优先兑换31天双击卡，失败后尝试限时双击卡
     */
    private fun exchangeDoubleCard(): Boolean {
        // 尝试兑换31天双击卡
        if (Vitality.handleVitalityExchange("SK20240805004754")) {
            return true
        }
        // 失败后尝试兑换限时双击卡
        return Vitality.handleVitalityExchange("CR20230516000363")
    }

    internal suspend fun usePropBeforeCollectEnergy(userId: String?, skipPropCheck: Boolean = false) {
        try {
            // 🚀 快速收取通道：跳过道具检查，直接返回
            if (skipPropCheck) {
                Log.record(TAG, "⚡ 快速收取通道：跳过道具检查，加速蹲点收取")
                return
            }

            /*
             * 在收集能量之前决定是否使用增益类道具卡。
             *
             * 主要逻辑:
             * 1. 定义时间常量，用于判断道具剩余有效期。
             * 2. 获取当前时间及各类道具的到期时间，计算剩余时间。
             * 3. 根据以下条件判断是否需要使用特定道具:
             *    - needDouble: 双击卡开关已打开，且当前没有生效的双击卡。
             *    - needrobExpand: 1.1倍能量卡开关已打开，且当前没有生效的卡。
             *    - needStealth: 隐身卡开关已打开，且当前没有生效的隐身卡。
             *    - needShield: 保护罩开关已打开，炸弹卡开关已关闭，且保护罩剩余时间不足一天。
             *    - needEnergyBombCard: 炸弹卡开关已打开，保护罩开关已关闭，且炸弹卡剩余时间不足三天。
             *    - needBubbleBoostCard: 加速卡开关已打开。
             * 4. 如果有任何一个道具需要使用，则同步查询背包信息，并调用相应的使用道具方法。
             */

            val now = System.currentTimeMillis()
            // 双击卡判断
            val needDouble =
                task.doubleCard!!.value != AntForest.ApplyPropType.CLOSE && shouldRenewDoubleCard(
                    doubleEndTime,
                    now
                )

            val needrobExpand =
                task.robExpandCard!!.value != AntForest.ApplyPropType.CLOSE && robExpandCardEndTime < now
            val needStealth =
                task.stealthCard!!.value != AntForest.ApplyPropType.CLOSE && stealthEndTime < now

            // 保护罩判断
            val needShield =
                (task.shieldCard!!.value != AntForest.ApplyPropType.CLOSE) && task.energyBombCardType!!.value == AntForest.ApplyPropType.CLOSE
                        && task.shieldManager.shouldRenewShield(task.shieldManager.shieldEndTime, now)
            // 炸弹卡判断
            val needEnergyBombCard =
                (task.energyBombCardType!!.value != AntForest.ApplyPropType.CLOSE) && task.shieldCard!!.value == AntForest.ApplyPropType.CLOSE
                        && shouldRenewEnergyBomb(energyBombCardEndTime, now)

            val needBubbleBoostCard = task.bubbleBoostCard!!.value != AntForest.ApplyPropType.CLOSE

            Log.record(
                TAG, "道具使用检查: needDouble=" + needDouble + ", needrobExpand=" + needrobExpand +
                        ", needStealth=" + needStealth + ", needShield=" + needShield +
                        ", needEnergyBombCard=" + needEnergyBombCard + ", needBubbleBoostCard=" + needBubbleBoostCard
            )
            if (needDouble || needStealth || needShield || needEnergyBombCard || needrobExpand || needBubbleBoostCard) {
                doubleCardLockObj.withLock {
                    val bagObject = queryPropList()
                    // Log.runtime(TAG, "bagObject=" + (bagObject == null ? "null" : bagObject.toString()));
                    if (needDouble) useDoubleCard(bagObject!!) // 使用双击卡

                    if (needrobExpand) userobExpandCard() // 使用1.1倍能量卡

                    if (needStealth) useStealthCard(bagObject) // 使用隐身卡

                    if (needBubbleBoostCard) useCardBoot(
                        task.bubbleBoostTime!!.value,
                        "加速卡"
                    ) {
                        runBlocking { useBubbleBoostCard() }
                    } // 使用加速卡
                    if (needShield) {
                        Log.record(TAG, "尝试使用保护罩罩")
                        task.shieldManager.useShieldCard(bagObject)
                    } else if (needEnergyBombCard) {
                        Log.record(TAG, "准备使用能量炸弹卡")
                        useEnergyBombCard(bagObject)
                    }
                }
            } else {
                Log.record(TAG, "没有需要使用的道具")
            }
        } catch (e: Exception) {
            Log.printStackTrace(e)
        }
    }

    internal fun useCardBoot(targetTimeValue: List<String>, propName: String?, func: Runnable) {
        for (targetTimeStr in targetTimeValue) {
            if ("-1" == targetTimeStr) {
                return
            }
            val targetTimeCalendar = TimeUtil.getTodayCalendarByTimeStr(targetTimeStr) ?: return
            val targetTime = targetTimeCalendar.getTimeInMillis()
            val now = System.currentTimeMillis()
            if (now > targetTime) {
                continue
            }
            val targetTaskId = "TAGET|$targetTime"
            if (!task.hasChildTask(targetTaskId)) {
                task.addChildTask(ModelTask.ChildModelTask(targetTaskId, "TAGET", func, targetTime))
                Log.record(
                    TAG,
                    "添加定时使用" + propName + "[" + UserMap.getCurrentMaskName() + "]在[" + TimeUtil.getCommonDate(
                        targetTime
                    ) + "]执行"
                )
            } else {
                task.addChildTask(ModelTask.ChildModelTask(targetTaskId, "TAGET", func, targetTime))
            }
        }
    }

    /**
     * 炸弹卡剩余时间判断
     * 当炸弹卡剩余时间低于3天时，需要续用
     * 最多可续用到4天
     */
    @SuppressLint("DefaultLocale")
    private fun shouldRenewEnergyBomb(bombEnd: Long, nowMillis: Long): Boolean {
        // 炸弹卡最长有效期为4天
        val maxBombDuration = 4 * TimeFormatter.ONE_DAY_MS
        // 炸弹卡续用阈值为3天
        val bombRenewThreshold = 3 * TimeFormatter.ONE_DAY_MS
        // 检测异常数据
        if (bombEnd > 0 && bombEnd < nowMillis - 365 * TimeFormatter.ONE_DAY_MS) {
            Log.record(TAG, "[炸弹卡] ⚠️ 检测到异常时间数据(${TimeUtil.getCommonDate(bombEnd)})，跳过检查")
            return false
        }

        if (bombEnd in 1..nowMillis) { // 已过期
            Log.record(
                TAG,
                "[炸弹卡] 已过期，立即续写；end=" + TimeUtil.getCommonDate(bombEnd) + ", now=" + TimeUtil.getCommonDate(
                    nowMillis
                )
            )
            return true
        }

        if (bombEnd == 0L) { // 未生效
            Log.record(TAG, "[炸弹卡] 未生效，尝试使用")
            return true
        }
        val remain = bombEnd - nowMillis
        // 如果剩余时间小于阈值且续写后总时长未超过最大有效期，则需要续用
        // 续写后结束时间 = bombEnd + 1天，续写后总时长 = 续写后结束时间 - 现在时间
        val renewDuration = TimeFormatter.ONE_DAY_MS // 每次续写增加1天
        val afterRenewRemain = remain + renewDuration // 续写后的剩余时间
        val needRenew =
            remain <= bombRenewThreshold && afterRenewRemain <= maxBombDuration

        val remainTimeStr = TimeFormatter.formatRemainingTime(remain)
        val thresholdTimeStr = TimeFormatter.formatRemainingTime(bombRenewThreshold)

        if (needRenew) {
            Log.record(
                TAG, String.format(
                    "[炸弹卡] 🔄 需要续写 - 剩余时间[%s] ≤ 续写阈值[%s]",
                    remainTimeStr, thresholdTimeStr
                )
            )
        } else {
            Log.record(
                TAG, String.format(
                    "[炸弹卡] ✅ 无需续写 - 剩余时间[%s] > 续写阈值[%s]",
                    remainTimeStr, thresholdTimeStr
                )
            )
        }
        return needRenew
    }

    /**
     * 双击卡剩余时间判断
     * 当双击卡剩余时间低于31天时，需要续用
     * 最多可续用到31+31天，但不建议，因为平时有5分钟、3天、7天等短期双击卡
     */
    @SuppressLint("DefaultLocale")
    private fun shouldRenewDoubleCard(doubleEnd: Long, nowMillis: Long): Boolean {
        // 双击卡最长有效期为62天（31+31）
        // 双击卡续用阈值为31天
        val doubleRenewThreshold = 31 * TimeFormatter.ONE_DAY_MS  // 改为小写开头

        // 如果doubleEnd为0或很久以前的时间（超过1年），说明数据未初始化或有问题
        if (doubleEnd > 0 && doubleEnd < nowMillis - 365 * TimeFormatter.ONE_DAY_MS) {
            Log.record(TAG, "[双击卡] ⚠️ 检测到异常时间数据(${TimeUtil.getCommonDate(doubleEnd)})，跳过检查")
            return false // 数据异常，不续用
        }

        if (doubleEnd in 1..nowMillis) { // 已过期
            Log.record(
                TAG,
                "[双击卡] 已过期，立即续写；end=" + TimeUtil.getCommonDate(doubleEnd) + ", now=" + TimeUtil.getCommonDate(
                    nowMillis
                )
            )
            return true
        }

        if (doubleEnd == 0L) { // 未生效（初始值）
            Log.record(TAG, "[双击卡] 未生效，尝试使用")
            return true
        }

        val remain = doubleEnd - nowMillis
        // 如果剩余时间小于阈值，则需要续用
        val needRenew = remain <= doubleRenewThreshold  // 使用修正后的变量名
        val remainTimeStr = TimeFormatter.formatRemainingTime(remain)
        val thresholdTimeStr = TimeFormatter.formatRemainingTime(doubleRenewThreshold)  // 使用修正后的变量名

        if (needRenew) {
            Log.record(
                TAG, String.format(
                    "[双击卡] 🔄 需要续写 - 剩余时间[%s] ≤ 续写阈值[%s]",
                    remainTimeStr, thresholdTimeStr
                )
            )
        } else {
            Log.record(
                TAG, String.format(
                    "[双击卡] ✅ 无需续写 - 剩余时间[%s] > 续写阈值[%s]",
                    remainTimeStr, thresholdTimeStr
                )
            )
        }
        return needRenew
    }

    /**
     * 检查当前时间是否在设置的使用双击卡时间内
     *
     * @return 如果当前时间在双击卡的有效时间范围内，返回true；否则返回false。
     */
    private fun hasDoubleCardTime(): Boolean {
        val currentTimeMillis = System.currentTimeMillis()
        return TimeUtil.checkInTimeRange(currentTimeMillis, task.doubleCardTime!!.value)
    }

    internal suspend fun giveProp() {
        val set = task.whoYouWantToGiveTo!!.value
        if (!set.isEmpty()) {
            for (userId in set) {
                if (task.selfId != userId) {
                    giveProp(userId)
                    break
                }
            }
        }
    }

    /**
     * 向指定用户赠送道具。 这个方法首先查询可用的道具列表，然后选择一个道具赠送给目标用户。 如果有多个道具可用，会尝试继续赠送，直到所有道具都赠送完毕。
     *
     * @param targetUserId 目标用户的ID。
     */
    private suspend fun giveProp(targetUserId: String?) {
        try {
            do {
                // 查询道具列表
                val propListJo = JSONObject(AntForestRpcCall.queryPropList(true))
                if (ResChecker.checkRes(TAG + "查询道具列表失败:", propListJo)) {
                    val forestPropVOList = propListJo.optJSONArray("forestPropVOList")
                    if (forestPropVOList != null && forestPropVOList.length() > 0) {
                        val propJo = forestPropVOList.getJSONObject(0)
                        val giveConfigId =
                            propJo.getJSONObject("giveConfigVO").getString("giveConfigId")
                        val holdsNum = propJo.optInt("holdsNum", 0)
                        val propName = propJo.getJSONObject("propConfigVO").getString("propName")
                        val propId = propJo.getJSONArray("propIdList").getString(0)
                        val giveResultJo = JSONObject(
                            AntForestRpcCall.giveProp(
                                giveConfigId,
                                propId,
                                targetUserId
                            )
                        )
                        if (ResChecker.checkRes(TAG + "赠送道具失败:", giveResultJo)) {
                            Log.forest("赠送道具🎭[" + UserMap.getMaskName(targetUserId) + "]#" + propName)
                            GlobalThreadPools.sleepCompat(1500)
                        } else {
                            val rt = giveResultJo.getString("resultDesc")
                            Log.record(rt)
                            Log.record(giveResultJo.toString())
                            if (rt.contains("异常")) {
                                return
                            }
                        }
                        // 如果持有数量大于1或道具列表中有多于一个道具，则继续赠送
                        if (holdsNum <= 1 && forestPropVOList.length() == 1) {
                            break
                        }
                    }
                } else {
                    // 如果查询道具列表失败，则记录失败的日志
                    Log.record(TAG, "赠送道具查询结果" + propListJo.getString("resultDesc"))
                }
                // 等待1.5秒后再继续
            } while (true)
        } catch (th: Throwable) {
            // 打印异常信息
            Log.printStackTrace(TAG, "giveProp err", th)
        }
    }

    private fun queryPropList(): JSONObject? {
        return queryPropList(false)
    }

    private fun queryPropList(forceRefresh: Boolean): JSONObject? = synchronized(task) {
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedBagObject != null && now - lastQueryPropListTime < 5000) {
            return@synchronized cachedBagObject
        }
        try {
            Log.record(TAG, "刷新背包...")
            val response = runBlocking { AntForestRpcCall.queryPropList(false) }
            // 检查响应是否为空，避免解析空字符串导致异常
            if (response.isNullOrBlank()) {
                Log.record(TAG, "刷新背包失败: 响应为空")
                return@synchronized null
            }
            val bagObject = JSONObject(response)
            if (bagObject.optBoolean("success")) {
                cachedBagObject = bagObject
                lastQueryPropListTime = now
                return@synchronized bagObject
            } else {
                Log.record(TAG, "刷新背包失败: " + bagObject.optString("resultDesc"))
            }
        } catch (th: Throwable) {
            task.handleException("queryPropList", th)
        }
        return@synchronized null
    }

    /**
     * 查找背包道具
     *
     * @param bagObject 背包对象
     * @param propType  道具类型 LIMIT_TIME_ENERGY_SHIELD_TREE,...
     */
    private fun findPropBag(bagObject: JSONObject?, propType: String): JSONObject? {
        if (Objects.isNull(bagObject)) {
            return null
        }
        try {
            val forestPropVOList = bagObject!!.getJSONArray("forestPropVOList")
            for (i in 0..<forestPropVOList.length()) {
                val forestPropVO = forestPropVOList.getJSONObject(i)
                val propConfigVO = forestPropVO.getJSONObject("propConfigVO")
                val currentPropType = propConfigVO.getString("propType")
                // String propName = propConfigVO.getString("propName");
                if (propType == currentPropType) {
                    return forestPropVO // 找到后直接返回
                }
            }
        } catch (e: Exception) {
            Log.printStackTrace(TAG, "findPropBag err", e)
        }

        return null // 未找到或出错时返回 null
    }

    /**
     * 返回背包道具信息
     */
    internal fun showBag() {
        val bagObject = queryPropList(true)
        if (Objects.isNull(bagObject)) {
            return
        }
        try {
            val forestPropVOList = bagObject?.optJSONArray("forestPropVOList") ?: return

            val logBuilder = StringBuilder("\n======= 背包道具列表 =======\n")
            for (i in 0..<forestPropVOList.length()) {
                val prop = forestPropVOList.optJSONObject(i) ?: continue

                val propConfig = prop.optJSONObject("propConfigVO") ?: continue

                val propName = propConfig.optString("propName")
                val propType = prop.optString("propType")
                val holdsNum = prop.optInt("holdsNum")
                val expireTime = prop.optLong("recentExpireTime", 0)
                logBuilder.append("道具: ").append(propName)
                    .append(" | 数量: ").append(holdsNum)
                    .append(" | 类型: ").append(propType)
                if (expireTime > 0) {
                    val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        .format(Date(expireTime))
                    logBuilder.append(" | 过期时间: ").append(formattedDate)
                }
                logBuilder.append("\n")
            }
            logBuilder.append("==========================")
            Log.record(TAG, logBuilder.toString())
        } catch (e: Exception) {
            Log.printStackTrace(TAG, "showBag err", e)
        }
    }

    /**
     * 使用背包道具
     *
     * @param propJsonObj 道具对象
     * @param needRefreshHome 是否需要刷新主页（默认true。加速卡等紧接着会查询主页的场景可设为false以优化延迟）
     */
    internal suspend fun usePropBag(propJsonObj: JSONObject?, needRefreshHome: Boolean = true): Boolean {
        if (propJsonObj == null) {
            Log.record(TAG, "要使用的道具不存在！")
            return false
        }
        try {
            val propId = propJsonObj.getJSONArray("propIdList").getString(0)
            val propConfigVO = propJsonObj.getJSONObject("propConfigVO")
            val propType = propConfigVO.getString("propType")
            val holdsNum = propJsonObj.optInt("holdsNum") // 当前持有数量
            val propName = propConfigVO.getString("propName")
            propEmoji(propName)
            val jo: JSONObject?
            val isRenewable = isRenewableProp(propType)
            Log.record(
                TAG,
                "道具 $propName (类型: $propType), 是否可续用: $isRenewable, 当前持有数量: $holdsNum"
            )
            val propGroup = AntForestRpcCall.getPropGroup(propType)
            if (isRenewable) {
                // 第一步：发送检查/尝试使用请求 (secondConfirm=false)
                val checkResponseStr = AntForestRpcCall.consumeProp(propGroup, propId, propType, false)
                val checkResponse = JSONObject(checkResponseStr)
                // Log.record(TAG, "发送检查请求: " + checkResponse);
                var resData = checkResponse.optJSONObject("resData")
                if (resData == null) {
                    resData = checkResponse
                }
                val status = resData.optString("usePropStatus")
                if ("NEED_CONFIRM_CAN_PROLONG" == status || "REPLACE" == status) {
                    // 情况1: 需要二次确认 (真正地续写)
                    Log.record(TAG, propName + "需要二次确认，发送确认请求...")
                    GlobalThreadPools.sleepCompat(2000)
                    val confirmResponseStr =
                        AntForestRpcCall.consumeProp(propGroup, propId, propType, true)
                    jo = JSONObject(confirmResponseStr)
                    // 提取道具名称用于日志显示
                    val userPropVO = jo.optJSONObject("userPropVO")
                    val usedPropName = userPropVO?.optString("propName") ?: propName
                    Log.record(TAG, "已使用$usedPropName")

                } else {
                    // 其他所有情况都视为最终结果，通常是失败
                    // Log.record(TAG, "道具状态异常或使用失败12:"+ status)
                    jo = checkResponse
                }
            } else {
                // 非续用类道具，直接使用
                val consumeResponse = AntForestRpcCall.consumeProp2(propGroup, propId, propType)
                jo = JSONObject(consumeResponse)
                // 提取道具名称用于日志显示
                val userPropVO = jo.optJSONObject("userPropVO")
                val usedPropName = userPropVO?.optString("propName") ?: propName
                Log.record(TAG, "已使用$usedPropName")
            }

            // 统一结果处理
            if (ResChecker.checkRes(TAG + "使用道具失败:", jo)) {
                // ⚡ 优化点：根据参数决定是否执行耗时的刷新操作
                if (needRefreshHome) {
                    updateSelfHomePage()
                }
                return true
            } else {
                var errorData = jo.optJSONObject("resData")
                if (errorData == null) {
                    errorData = jo
                }
                val resultDesc = errorData.optString("resultDesc", "未知错误")
                Log.record("使用道具失败: $resultDesc")
                Toast.show(resultDesc)
                return false
            }
        } catch (th: Throwable) {
            Log.printStackTrace(TAG, "usePropBag err", th)
            return false
        }
    }

    /**
     * 判断是否是可续用类道具
     */
    private fun isRenewableProp(propType: String): Boolean {
        return propType.contains("SHIELD") // 保护罩
                || propType.contains("BOMB_CARD") // 炸弹卡
                || propType.contains("DOUBLE_CLICK") // 双击卡
    }

    /**
     * 使用双击卡道具
     * 功能：在指定时间内，使好友的一个能量球可以收取两次
     *
     * @param bagObject 背包的JSON对象
     */
    private suspend fun useDoubleCard(bagObject: JSONObject) {
        try {
            // 前置检查1: 检查今日使用次数是否已达上限
            if (!Status.canDoubleToday()) {
                Log.record(TAG, "双击卡使用条件检查: 今日次数已达上限")
                return
            }
            // 前置检查2: 校验背包数据是否有效
            if (!bagObject.optBoolean("success")) {
                Log.record(TAG, "背包数据异常，无法使用双击卡$bagObject")
                return
            }

            val forestPropVOList = bagObject.optJSONArray("forestPropVOList") ?: return

            // 永动机逻辑：如果背包内没有双击卡且开启了永动机，尝试兑换
            var hasProp = false
            for (i in 0..<forestPropVOList.length()) {
                val prop = forestPropVOList.optJSONObject(i)
                if (prop != null && "doubleClick" == prop.optString("propGroup")) {
                    hasProp = true
                    break
                }
            }

            if (!hasProp && task.doubleCardConstant!!.value) {
                Log.record(TAG, "背包中没有双击卡，尝试兑换...")
                if (exchangeDoubleCard()) {
                    // 重新获取背包数据
                    val newBagObject = queryPropList()
                    if (newBagObject != null) {
                        val newForestPropVOList = newBagObject.optJSONArray("forestPropVOList")
                        if (newForestPropVOList != null) {
                            // 递归调用，使用新的背包数据
                            useDoubleCard(newBagObject)
                            return
                        }
                    }
                }
            }

            // 步骤1: 根据用户UI设置，筛选出需要使用的双击卡
            val doubleClickProps: MutableList<JSONObject> = ArrayList()
            val choice = task.doubleCard!!.value
            for (i in 0..<forestPropVOList.length()) {
                val prop = forestPropVOList.optJSONObject(i)
                if (prop != null && "doubleClick" == prop.optString("propGroup")) {
                    if (choice == AntForest.ApplyPropType.ALL) {
                        // 设置为"所有道具": 添加所有双击卡
                        doubleClickProps.add(prop)
                    } else if (choice == AntForest.ApplyPropType.ONLY_LIMIT_TIME) {
                        // 设置为"限时道具": 只添加用于续期的卡 (名字含LIMIT_TIME或DAYS)
                        val propType = prop.optString("propType")
                        if (propType.contains("LIMIT_TIME") || propType.contains("DAYS")) {
                            doubleClickProps.add(prop)
                        }
                    }
                }
            }
            if (doubleClickProps.isEmpty()) {
                Log.record(TAG, "根据设置，背包中没有需要使用的双击卡")
                return
            }

            // 步骤2: 按过期时间升序排序，，避免浪费
            Collections.sort(
                doubleClickProps,
                Comparator { p1: JSONObject?, p2: JSONObject? ->
                    val expireTime1 = p1!!.optLong("recentExpireTime", Long.MAX_VALUE)
                    val expireTime2 = p2!!.optLong("recentExpireTime", Long.MAX_VALUE)
                    expireTime1.compareTo(expireTime2)
                })

            Log.record(TAG, "扫描到" + doubleClickProps.size + "种双击卡，将按过期顺序尝试使用...")

            // 步骤3: 遍历筛选并排序后的列表，逐个尝试使用
            var success = false
            for (propObj in doubleClickProps) {
                val propType = propObj.optString("propType")
                val propName =
                    propObj.optJSONObject("propConfigVO")?.optString("propName") ?: ""

                // 特定条件检查1: 如果是普通的5分钟卡，需要检查是否在指定时间段内
                if ("ENERGY_DOUBLE_CLICK" == propType && !hasDoubleCardTime()) {
                    Log.record(TAG, "跳过[$propName]，当前不在指定使用时间段内")
                    continue  // 跳过，尝试下一张
                }

                if ("LIMIT_TIME_ENERGY_DOUBLE_CLICK" == propType && choice == AntForest.ApplyPropType.ONLY_LIMIT_TIME) {
                    val expireTime = propObj.optLong("recentExpireTime", 0)
                    // 修改：24 改为 48 小时，日志信息同步更新
                    if (expireTime > 0 && (expireTime - System.currentTimeMillis() > 2 * 24 * 60 * 60 * 1000L)) {
                        Log.record(TAG, "跳过[$propName]，该卡有效期剩余超过2天 (仅限时模式)")
                        continue  // 跳过，尝试下一张
                    }
                }

                // 尝试使用道具
                Log.record(TAG, "尝试使用卡: $propName")
                if (usePropBag(propObj)) {
                    // 使用成功，更新状态并结束循环
                    doubleEndTime = System.currentTimeMillis() + 5 * TimeFormatter.ONE_MINUTE_MS
                    Status.doubleToday()
                    success = true
                    break
                }
            }

            if (!success) {
                Log.record(TAG, "所有可用的双击卡均不满足使用条件")
            }
        } catch (th: Throwable) {
            task.handleException("useDoubleCard", th)
        }
    }

    /**
     * 使用隐身卡道具
     * 功能：隐藏收取行为，避免被好友发现偷取能量
     *
     * @param bagObject 背包的JSON对象
     */
    private suspend fun useStealthCard(bagObject: JSONObject?) {
        val config = PropConfig(
            "隐身卡",
            arrayOf<String>("LIMIT_TIME_STEALTH_CARD", "STEALTH_CARD"),
            null,  // 无特殊条件
            { this.exchangeStealthCard() },
            { time: Long? -> stealthEndTime = time!! + TimeFormatter.ONE_DAY_MS }
        )
        usePropTemplate(bagObject, config, task.stealthCardConstant!!.value)
    }


    /**
     * 使用加速卡道具
     * 功能：加速能量球成熟时间，让等待中的能量球提前成熟，并立即收取自己的能量
     */
    private suspend fun useBubbleBoostCard(bag: JSONObject? = queryPropList()) {
        try {
            // 先检查自己是否有未成熟的能量球
            val selfHomeObj = task.querySelfHome()
            if (selfHomeObj == null) {
                Log.record(TAG, "无法获取自己主页信息，跳过使用加速卡")
                return
            }
            // 检查是否有未来才会成熟的能量球（bubbleCount > 0且produceTime > serverTime）
            val serverTime = selfHomeObj.optLong("now", System.currentTimeMillis())
            val bubbles = selfHomeObj.optJSONArray("bubbles")
            var hasWaitingBubbles = false
            if (bubbles != null && bubbles.length() > 0) {
                for (i in 0..<bubbles.length()) {
                    val bubble = bubbles.getJSONObject(i)
                    val bubbleCount = bubble.getInt("fullEnergy")
                    if (bubbleCount <= 0) {
                        continue // 跳过能量为0的能量球
                    }
                    val produceTime = bubble.optLong("produceTime", 0L)
                    // 判断是否有未来才会成熟的能量球（produceTime > 0 且 > serverTime）
                    if (produceTime > 0 && produceTime > serverTime) {
                        hasWaitingBubbles = true
                        break
                    }
                }
            }
            if (!hasWaitingBubbles) {
                Log.record(TAG, "自己当前没有未来才会成熟的能量球，不使用加速卡")
                return
            }

            // 在背包中查询限时加速器
            var jo = findPropBag(bag, "LIMIT_TIME_ENERGY_BUBBLE_BOOST")
            if (jo == null) {
                youthPrivilege()
                jo = findPropBag(queryPropList(), "LIMIT_TIME_ENERGY_BUBBLE_BOOST")
                if (jo == null) {
                    jo = findPropBag(bag, "BUBBLE_BOOST")
                }
            }
            if (jo != null) {
                val propName = jo.getJSONObject("propConfigVO").getString("propName")
                // ⚡ 优化点：传入 needRefreshHome = false，避免重复请求和等待
                // 因为紧接着调用的 collectSelfEnergyImmediately 会再次查询主页，那次查询会包含最新的道具状态和能量球状态
                if (usePropBag(jo, needRefreshHome = false)) {
                    Log.forest("使用加速卡🌪[$propName]")
                    task.energyCollector.collectSelfEnergyImmediately("加速卡")
                }
            } else {
                Log.record(TAG, "背包中无可用加速卡")
            }
        } catch (th: Throwable) {
            Log.printStackTrace(TAG, "useBubbleBoostCard err", th)
        }
    }

    /**
     * 使用1.1倍能量卡道具
     * 功能：增加能量收取倍数，收取好友能量时获得1.1倍效果
     */
    private suspend fun userobExpandCard(bag: JSONObject? = queryPropList()) {
        try {
            var jo = findPropBag(bag, "VITALITY_ROB_EXPAND_CARD_1.1_3DAYS")
            if (jo != null && usePropBag(jo)) {
                robExpandCardEndTime = System.currentTimeMillis() + 1000 * 60 * 5
            }
            jo = findPropBag(bag, "SHAMO_ROB_EXPAND_CARD_1.5_1DAYS")
            if (jo != null && usePropBag(jo)) {
                robExpandCardEndTime = System.currentTimeMillis() + 1000 * 60 * 5
            }
        } catch (th: Throwable) {
            Log.printStackTrace(TAG, "useBubbleBoostCard err", th)
        }
    }

    internal suspend fun useEnergyRainChanceCard() {
        try {
            if (Status.hasFlagToday("AntForest::useEnergyRainChanceCard")) {
                return
            }
            val propTypes = arrayOf("LIMIT_TIME_ENERGY_RAIN_CHANCE", "ENERGY_RAIN_CHANCE")
            for (propType in propTypes) {
                while (true) {
                    val jo = findPropBag(queryPropList(true), propType) ?: break
                    if (usePropBag(jo)) {
                        Log.record(TAG, "成功使用一个能量雨道具: $propType")
                        delay(2000)
                    } else {
                        break
                    }
                }

                if (propType == "LIMIT_TIME_ENERGY_RAIN_CHANCE") {
                    val skuInfo = Vitality.findSkuInfoBySkuName("能量雨次卡")
                    if (skuInfo != null) {
                        val skuId = skuInfo.getString("skuId")
                        if (Status.canVitalityExchangeToday(skuId, 1)) {
                            if (Vitality.VitalityExchange(
                                    skuInfo.getString("spuId"),
                                    skuId,
                                    "限时能量雨机会"
                                )
                            ) {
                                delay(1000)
                                val joExchanged = findPropBag(queryPropList(true), propType)
                                if (joExchanged != null && usePropBag(joExchanged)) {
                                    delay(1000)
                                }
                            }
                        }
                    }
                }
            }
            Status.setFlagToday("AntForest::useEnergyRainChanceCard")
            Log.record(TAG, "所有能量雨卡已处理完毕")
        } catch (th: Throwable) {
            Log.printStackTrace(TAG, "useEnergyRainChanceCard err", th)
        }
    }

    /**
     * 使用炸弹卡道具
     * 功能：对有保护罩的好友使用，可以破坏其保护罩并收取能量
     * 注意：与保护罩功能冲突，通常二选一使用
     *
     * @param bagObject 背包的JSON对象
     */
    private suspend fun useEnergyBombCard(bagObject: JSONObject?) {
        try {
            Log.record(TAG, "尝试使用炸弹卡...")
            var jo = findPropBag(bagObject, "ENERGY_BOMB_CARD")
            if (jo == null) {
                Log.record(TAG, "背包中没有炸弹卡，尝试兑换...")
                val skuInfo = Vitality.findSkuInfoBySkuName("能量炸弹卡")
                if (skuInfo == null) {
                    Log.record(TAG, "活力值商店中未找到炸弹卡。")
                    return
                }

                val skuId = skuInfo.getString("skuId")
                if (Status.canVitalityExchangeToday(skuId, 1)) {
                    if (Vitality.VitalityExchange(
                            skuInfo.getString("spuId"),
                            skuId,
                            "能量炸弹卡"
                        )
                    ) {
                        jo = findPropBag(queryPropList(), "ENERGY_BOMB_CARD")
                    }
                } else {
                    Log.record(TAG, "今日炸弹卡兑换次数已达上限。")
                }
            }

            if (jo != null) {
                Log.record(TAG, "找到炸弹卡，准备使用: $jo")
                if (usePropBag(jo)) {
                    // 使用成功后刷新真实结束时间
                    updateSelfHomePage()
                    Log.record(TAG, "能量炸弹卡使用成功，已刷新结束时间")
                }
            } else {
                Log.record(TAG, "背包中未找到任何可用炸弹卡。")
                updateSelfHomePage()
            }
        } catch (th: Throwable) {
            Log.printStackTrace(TAG, "useEnergyBombCard err", th)
        }
    }

    /**
     * 道具使用配置类
     */
    @JvmRecord
    private data class PropConfig(
        val propName: String?, val propTypes: Array<String>?,
        val condition: Supplier<Boolean?>?,
        val exchangeFunction: Supplier<Boolean?>?,
        val endTimeUpdater: Consumer<Long?>?
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as PropConfig
            if (propName != other.propName) return false
            if (!propTypes.contentEquals(other.propTypes)) return false
            if (condition != other.condition) return false
            if (exchangeFunction != other.exchangeFunction) return false
            if (endTimeUpdater != other.endTimeUpdater) return false
            return true
        }

        override fun hashCode(): Int {
            var result = propName?.hashCode() ?: 0
            result = 31 * result + (propTypes?.contentHashCode() ?: 0)
            result = 31 * result + (condition?.hashCode() ?: 0)
            result = 31 * result + (exchangeFunction?.hashCode() ?: 0)
            result = 31 * result + (endTimeUpdater?.hashCode() ?: 0)
            return result
        }
    }

    /**
     * 通用道具使用模板方法
     *
     * @param bagObject    背包对象
     * @param config       道具配置
     * @param constantMode 是否开启永动机模式
     */
    private suspend fun usePropTemplate(bagObject: JSONObject?, config: PropConfig, constantMode: Boolean) {
        try {
            if (config.condition != null && !config.condition.get()!!) {
                Log.record(TAG, "不满足使用" + config.propName + "的条件")
                return
            }
            Log.record(TAG, "尝试使用" + config.propName + "...")
            // 按优先级查找道具
            var propObj: JSONObject? = null
            for (propType in config.propTypes!!) {
                propObj = findPropBag(bagObject, propType)
                if (propObj != null) break
            }
            // 如果背包中没有道具且开启永动机，尝试兑换
            if (propObj == null && constantMode && config.exchangeFunction != null) {
                Log.record(TAG, "背包中没有" + config.propName + "，尝试兑换...")
                if (config.exchangeFunction.get() == true) {
                    // 重新查找兑换后的道具
                    for (propType in config.propTypes) {
                        propObj = findPropBag(queryPropList(), propType)
                        if (propObj != null) break
                    }
                }
            }
            if (propObj != null) {
                // 针对限时双击卡的时间检查
                if ("双击卡" == config.propName) {
                    val propType = propObj.optString("propType")
                    if ("ENERGY_DOUBLE_CLICK" == propType && !hasDoubleCardTime()) {
                        Log.record(TAG, "跳过双击卡[$propType]，当前不在指定使用时间段内")
                        return
                    }
                }
                Log.record(TAG, "找到" + config.propName + "，准备使用: " + propObj)
                if (usePropBag(propObj)) {
                    config.endTimeUpdater?.accept(System.currentTimeMillis())
                }
            } else {
                Log.record(TAG, "背包中未找到任何可用的" + config.propName)
                updateSelfHomePage()
            }
        } catch (th: Throwable) {
            task.handleException("use" + config.propName, th)
        }
    }

    companion object {
        private fun propEmoji(propName: String): String {
            val tag: String = if (propName.contains("保")) {
                "🛡️"
            } else if (propName.contains("双")) {
                "👥"
            } else if (propName.contains("加")) {
                "🌪"
            } else if (propName.contains("雨")) {
                "🌧️"
            } else if (propName.contains("炸")) {
                "💥"
            } else {
                "🥳"
            }
            return tag
        }
    }
}
