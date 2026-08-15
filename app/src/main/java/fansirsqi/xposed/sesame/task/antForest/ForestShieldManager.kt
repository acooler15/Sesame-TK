package fansirsqi.xposed.sesame.task.antForest

import android.annotation.SuppressLint
import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.core.threads.GlobalThreadPools
import fansirsqi.xposed.sesame.core.util.ResChecker
import fansirsqi.xposed.sesame.core.util.TimeFormatter
import fansirsqi.xposed.sesame.core.util.TimeUtil
import fansirsqi.xposed.sesame.data.Status
import fansirsqi.xposed.sesame.task.antForest.Privilege.youthPrivilege
import fansirsqi.xposed.sesame.util.maps.UserMap
import org.json.JSONObject
import java.util.Collections
import kotlin.math.abs

/**
 * 蚂蚁森林保护罩管理
 */
internal class ForestShieldManager(private val task: AntForest) {
    private val TAG = AntForest.TAG

    /**
     * 保护罩结束时间
     */
    @Volatile
    internal var shieldEndTime: Long = 0

    /**
     * 检查保护罩是否覆盖能量成熟期
     *
     * @param userHomeObj 用户主页对象
     * @param produceTime 能量成熟时间
     * @param serverTime 服务器时间
     * @return true表示应该跳过蹲点（保护罩覆盖），false表示可以蹲点
     */
    internal fun shouldSkipWaitingTaskDueToProtection(
        userHomeObj: JSONObject,
        produceTime: Long,
        serverTime: Long
    ): Boolean {
        val shieldEndTime = ForestUtil.getShieldEndTime(userHomeObj)
        val bombEndTime = ForestUtil.getBombCardEndTime(userHomeObj)
        val protectionEndTime = maxOf(shieldEndTime, bombEndTime)
        return protectionEndTime > produceTime
    }

    internal fun isIsProtected(userId: String?): Boolean {
        var isProtected: Boolean
        // Log.forest("is_monday:"+_is_monday);
        if (task.monday) {
            isProtected = task.alternativeAccountList!!.value.contains(userId)
        } else {
            isProtected = task.helpFriendCollectList!!.value.contains(userId)
            if (task.helpFriendCollectType!!.value != AntForest.HelpFriendCollectType.HELP) {
                isProtected = !isProtected
            }
        }
        return isProtected
    }

    internal suspend fun protectFriendEnergy(userHomeObj: JSONObject) {
        try {
            val wateringBubbles = userHomeObj.optJSONArray("wateringBubbles")
            val userEnergy = userHomeObj.optJSONObject("userEnergy")
            val userId =
                if (userEnergy == null) UserMap.currentUid else userEnergy.optString("userId")
            if (wateringBubbles != null && wateringBubbles.length() > 0) {
                for (j in 0..<wateringBubbles.length()) {
                    try {
                        val wateringBubble = wateringBubbles.getJSONObject(j)
                        if ("fuhuo" != wateringBubble.getString("bizType")) {
                            continue
                        }
                        if (wateringBubble.getJSONObject("extInfo").optInt("restTimes", 0) == 0) {
                            Status.protectBubbleToday(task.selfId)
                        }
                        if (!wateringBubble.getBoolean("canProtect")) {
                            continue
                        }
                        val joProtect = JSONObject(AntForestRpcCall.protectBubble(userId))
                        if (!ResChecker.checkRes(TAG + "复活能量失败:", joProtect)) {
                            //Log.record(joProtect.getString("resultDesc"))
                            //Log.runtime(joProtect.toString())
                            continue
                        }
                        val vitalityAmount = joProtect.optInt("vitalityAmount", 0)
                        val fullEnergy = wateringBubble.optInt("fullEnergy", 0)
                        val str =
                            "复活能量🚑[" + UserMap.getMaskName(userId) + "-" + fullEnergy + "g]" + (if (vitalityAmount > 0) "#活力值+$vitalityAmount" else "")
                        Log.forest(str)
                        break
                    } catch (t: Throwable) {
                        Log.printStackTrace(t)
                        break
                    } finally {
                        GlobalThreadPools.sleepCompat(500)
                    }
                }
            }
        } catch (e: Exception) {
            Log.printStackTrace(e)
        }
    }

    /**
     * 兑换能量保护罩
     * 类别 spuid skuid price
     * 限时 CR20230517000497  CR20230516000370  166
     * 永久 CR20230517000497  CR20230516000371  500
     */
    private fun exchangeEnergyShield(): Boolean {
        val spuId = "CR20230517000497"
        val skuId = "CR20230516000370"
        if (!Status.canVitalityExchangeToday(skuId, 1)) {
            return false
        }
        return Vitality.VitalityExchange(spuId, skuId, "保护罩")
    }

    /**
     * 保护罩剩余时间判断
     * 以整数 HHmm 指定保护罩续写阈值。
     * 例如：2355 表示 23 小时 55 分钟，0955 可直接写为 955。
     * 校验规则：0 ≤ HH ≤ 99，0 ≤ mm ≤ 59；非法值将回退为默认值。
     */
    @SuppressLint("DefaultLocale")
    internal fun shouldRenewShield(shieldEnd: Long, nowMillis: Long): Boolean {
        // 解析阈值配置
        var hours: Int
        var minutes: Int
        try {
            val abs = abs(SHIELD_RENEW_THRESHOLD_HHMM)
            hours = abs / 100 // 提取小时部分
            minutes = abs % 100 // 提取分钟部分
            // 可以添加分钟有效性检查
            if (minutes > 59) {
                Log.record(TAG, "[保护罩] 分钟数无效: $minutes, 使用默认值")
                hours = 23
                minutes = 59
            }
        } catch (e: Exception) {
            Log.record(TAG, "[保护罩] 解析阈值配置异常: " + e.message + ", 使用默认值")
            hours = 23
            minutes = 59
        }
        val thresholdMs = hours * TimeFormatter.ONE_HOUR_MS + minutes * TimeFormatter.ONE_MINUTE_MS

        // 检测异常数据
        if (shieldEnd > 0 && shieldEnd < nowMillis - 365 * TimeFormatter.ONE_DAY_MS) {
            Log.record(TAG, "[保护罩] ⚠️ 检测到异常时间数据(${TimeUtil.getCommonDate(shieldEnd)})，跳过检查")
            return false
        }

        if (shieldEnd in 1..nowMillis) { // 已过期
            Log.record(
                TAG,
                "[保护罩] 已过期，立即续写；end=" + TimeUtil.getCommonDate(shieldEnd) + ", now=" + TimeUtil.getCommonDate(
                    nowMillis
                )
            )
            return true
        }

        if (shieldEnd == 0L) { // 未生效
            Log.record(TAG, "[保护罩] 未生效，尝试使用")
            return true
        }
        val remain = shieldEnd - nowMillis
        val needRenew = remain <= thresholdMs
        // 格式化剩余时间和阈值时间为更直观的显示
        val remainTimeStr = TimeFormatter.formatRemainingTime(remain)
        val thresholdTimeStr = String.format("%02d小时%02d分", hours, minutes)
        if (needRenew) {
            Log.record(
                TAG, String.format(
                    "[保护罩] 🔄 需要续写 - 剩余时间[%s] ≤ 续写阈值[%s]",
                    remainTimeStr, thresholdTimeStr
                )
            )
        } else {
            Log.record(
                TAG, String.format(
                    "[保护罩] ✅ 无需续写 - 剩余时间[%s] > 续写阈值[%s]",
                    remainTimeStr, thresholdTimeStr
                )
            )
        }
        return needRenew
    }

    /**
     * 使用保护罩道具
     * 功能：保护自己的能量不被好友偷取，防止能量被收走。
     * 优先使用即将过期的限时保护罩，避免浪费。
     * 支持来源：
     *   - 背包中已有的多种类型保护罩
     *   - 青春特权自动领取（若开启）
     *   - 活力值兑换（若开启且兑换成功）
     *
     * @param bagObject 当前背包的 JSON 对象（可能为 null）
     */
    internal suspend fun useShieldCard(bagObject: JSONObject?) {
        try {
            Log.record(TAG, "尝试使用保护罩...")

            // 定义支持的保护罩类型
            val shieldTypes = listOf(
                "LIMIT_TIME_ENERGY_SHIELD_TREE",   // 限时森林保护罩（通常来自活动/青春特权）
                "LIMIT_TIME_ENERGY_SHIELD",        // 限时能量保护罩
                "ENERGY_SHIELD_YONGJIU",           // 限时能量保护罩（可能为旧版道具）
                "RUIHE_ENERGY_SHIELD",             // 瑞和能量保护罩（合作方专属？）
                "PK_SEASON1_ENERGY_SHIELD_TREE",   // PK赛限定保护罩
                "ENERGY_SHIELD"                    // 通用能量保护罩
            )

            // 步骤1: 从背包中收集所有可用的保护罩
            val availableShields: MutableList<JSONObject> = ArrayList()
            val forestPropVOList = bagObject?.optJSONArray("forestPropVOList")

            if (forestPropVOList != null) {
                for (i in 0..<forestPropVOList.length()) {
                    val prop = forestPropVOList.optJSONObject(i) ?: continue
                    val propType = prop.optJSONObject("propConfigVO")?.optString("propType") ?: ""

                    if (shieldTypes.contains(propType)) {
                        availableShields.add(prop)
                    }
                }
            }

            // 步骤2: 如果没有找到保护罩，尝试获取
            if (availableShields.isEmpty()) {
                // 2.1 若青春特权开启 → 尝试领取并重新查找
                if (task.youthPrivilege?.value == true) {
                    Log.record(TAG, "尝试通过青春特权获取保护罩...")
                    if (youthPrivilege()) {
                        val freshBag = task.querySelfHome()
                        val freshPropList = freshBag?.optJSONArray("forestPropVOList")
                        if (freshPropList != null) {
                            for (i in 0..<freshPropList.length()) {
                                val prop = freshPropList.optJSONObject(i) ?: continue
                                val propType = prop.optJSONObject("propConfigVO")?.optString("propType") ?: ""

                                if ("LIMIT_TIME_ENERGY_SHIELD_TREE" == propType) {
                                    availableShields.add(prop)
                                }
                            }
                        }
                    }
                }

                // 2.2 若仍未找到，且活力值兑换开启 → 尝试兑换
                if (availableShields.isEmpty() && task.shieldCardConstant?.value == true) {
                    Log.record(TAG, "尝试通过活力值兑换保护罩...")
                    if (exchangeEnergyShield()) {
                        // 兑换后通常获得的是 LIMIT_TIME_ENERGY_SHIELD
                        val exchangeBag = task.querySelfHome()
                        val exchangePropList = exchangeBag?.optJSONArray("forestPropVOList")
                        if (exchangePropList != null) {
                            for (i in 0..<exchangePropList.length()) {
                                val prop = exchangePropList.optJSONObject(i) ?: continue
                                val propType = prop.optJSONObject("propConfigVO")?.optString("propType") ?: ""

                                if ("LIMIT_TIME_ENERGY_SHIELD" == propType) {
                                    availableShields.add(prop)
                                }
                            }
                        }
                    }
                }
            }

            // 步骤3: 按过期时间升序排序，优先使用即将过期的保护罩
            if (availableShields.isNotEmpty()) {
                Collections.sort(
                    availableShields,
                    Comparator { p1: JSONObject?, p2: JSONObject? ->
                        val expireTime1 = p1!!.optLong("recentExpireTime", Long.MAX_VALUE)
                        val expireTime2 = p2!!.optLong("recentExpireTime", Long.MAX_VALUE)
                        expireTime1.compareTo(expireTime2)
                    })

                // 步骤4: 逐个尝试使用保护罩
                for (shieldObj in availableShields) {
                    val propType = shieldObj.optJSONObject("propConfigVO")?.optString("propType") ?: ""
                    val propName = shieldObj.optJSONObject("propConfigVO")?.optString("propName") ?: propType
                    Log.record(TAG, "尝试使用保护罩: $propName")
                    if (task.itemManager.usePropBag(shieldObj)) {
                        Log.record(TAG, "保护罩使用成功: $propName")
                        return // 使用成功，直接退出
                    } else {
                        Log.record(TAG, "保护罩使用失败: $propName，尝试下一个...")
                    }
                }
            }
            // 步骤5: 未使用成功（无论是否找到）
            Log.record(TAG, "背包中未找到或无法使用任何可用保护罩")

        } catch (th: Throwable) {
            Log.printStackTrace(TAG, "useShieldCard err", th)
        }
    }

    /**
     * 检查用户是否有保护罩或炸弹（按照原有逻辑）
     */
    internal fun checkUserShieldAndBomb(userHomeObj: JSONObject, userName: String?, userId: String, serverTime: Long): Boolean {
        var hasProtection = false
        val isSelf = userId == UserMap.currentUid

        if (!isSelf) {
            val shieldEndTime = ForestUtil.getShieldEndTime(userHomeObj)
            val bombEndTime = ForestUtil.getBombCardEndTime(userHomeObj)
            maxOf(shieldEndTime, bombEndTime)

            if (shieldEndTime > serverTime) {
                hasProtection = true
                val remainingHours = (shieldEndTime - serverTime) / (1000 * 60 * 60)
                Log.record(TAG, "[$userName]被能量罩❤️保护着哟(还剩${remainingHours}h)，跳过收取")
            }
            if (bombEndTime > serverTime) {
                hasProtection = true
                val remainingHours = (bombEndTime - serverTime) / (1000 * 60 * 60)
                Log.record(TAG, "[$userName]开着炸弹卡💣(还剩${remainingHours}h)，跳过收取")
            }
        }

        return hasProtection
    }

    companion object {
        // 保持向后兼容
        /** 保护罩续写阈值（HHmm），例如 2359 表示 23小时59分  */
        private const val SHIELD_RENEW_THRESHOLD_HHMM = 2359
    }
}
