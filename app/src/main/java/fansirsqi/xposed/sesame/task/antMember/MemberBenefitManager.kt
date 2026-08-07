package fansirsqi.xposed.sesame.task.antMember

import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.core.log.Log.record
import fansirsqi.xposed.sesame.core.threads.GlobalThreadPools
import fansirsqi.xposed.sesame.core.util.ResChecker
import fansirsqi.xposed.sesame.data.Status.Companion.canMemberPointExchangeBenefitToday
import fansirsqi.xposed.sesame.data.Status.Companion.hasFlagToday
import fansirsqi.xposed.sesame.data.Status.Companion.memberPointExchangeBenefitToday
import fansirsqi.xposed.sesame.data.Status.Companion.setFlagToday
import fansirsqi.xposed.sesame.util.maps.IdMapManager
import fansirsqi.xposed.sesame.util.maps.MemberBenefitsMap
import fansirsqi.xposed.sesame.util.maps.UserMap
import org.json.JSONObject

internal class MemberBenefitManager(private val member: AntMember) {

    internal fun memberPointExchangeBenefit() {
        if (hasFlagToday("memberBenefit::refresh")) {
            return
        }
        try {
            val userId = UserMap.currentUid
            record(AntMember.TAG, "会员积分商品加载..")
            // 1. 分类配置直接放在函数内部
            val categoryMap = mapOf(
                "公益道具" to listOf("94000SR2025022012011004"),
                "出行旅游" to listOf("94000SR2025010611441006", "94000SR2025010611458001"),
                "餐饮" to listOf("94000SR2025110315351006"),
                "皮肤藏品" to listOf("94000SR2025110315357001", "94000SR2025111015444005"),
                "理财还款" to listOf("94000SR2025011411575008", "94000SR2025091814834002"),
                "红包神券" to listOf("94000SR2025092414916001"),
                "充值缴费" to listOf("94000SR2025011611640002", "94000SR2025091814821018")
            )
            // 3. 遍历分类
            categoryMap.forEach { (catName, ids) ->
                var currentPage = 1
                var hasNextPage = true
                while (hasNextPage) {//此处请求过载，容易风控，循环频繁请求会炸
                    GlobalThreadPools.sleepCompat(1000L)
                    val responseStr = AntMemberRpcCall.queryDeliveryZoneDetail(ids, currentPage, 48)
                    if (responseStr.isNullOrEmpty()) {
                        Log.error(AntMember.TAG, "分类[$catName] 接口返回空字符串")
                        break
                    }
                    val jo = JSONObject(responseStr)
                    if (!ResChecker.checkRes(AntMember.TAG, jo)) {
                        Log.error(AntMember.TAG, "分类[$catName] 校验失败: $responseStr")
                        break
                    }
                    val benefits = jo.optJSONArray("briefConfigInfos")
                    if (benefits == null || benefits.length() == 0) {
                        Log.error(AntMember.TAG, "分类[$catName] 第 $currentPage 页没有权益数据")
                        break
                    }
                    for (i in 0 until benefits.length()) {
                        val rawItem = benefits.getJSONObject(i)
                        // 兼容 benefitInfo 嵌套结构
                        val benefit = if (rawItem.has("benefitInfo")) rawItem.getJSONObject("benefitInfo") else rawItem
                        val name = benefit.optString("name", "未知")
                        val benefitId = benefit.optString("benefitId")
                        val itemId = benefit.optString("itemId")
                        val pointNeeded = benefit.optJSONObject("pricePresentation")?.optString("point") ?: "0"
                        if (benefitId.isEmpty()) {
                            record(AntMember.TAG, "商品[$name] 没有 benefitId，跳过")
                            continue
                        }
                        // 记录 benefitId 映射关系
                        IdMapManager.getInstance(MemberBenefitsMap::class.java).add(benefitId, name)
                        // 校验是否在白名单
                        val inWhiteList = member.memberPointExchangeBenefitList?.value?.contains(benefitId) ?: false
                        if (!inWhiteList) {
                            // 如果不在白名单，保持安静，不刷 record 日志，或者你可以按需开启
                            continue
                        }
                        // 校验频率限制
                        if (!canMemberPointExchangeBenefitToday(benefitId)) {
                            record(AntMember.TAG, "跳过[$name]: 今日已兑换过")
                            continue
                        }
                        // 5. 执行兑换
                        record(AntMember.TAG, "准备兑换[$name], ID: $benefitId, 需积分: $pointNeeded")
                        if (exchangeBenefit(benefitId, itemId, userId)) {
                            Log.other("会员积分🎐兑换[$name]#花费[$pointNeeded 积分]")
                        } else {
                            record(AntMember.TAG, "兑换失败: $name (ItemId: $itemId)")
                        }
                    }
                    val nextPageNum = jo.optInt("nextPageNum", 0)
                    if (nextPageNum > 0 && nextPageNum > currentPage) {
                        currentPage = nextPageNum
                    } else {
                        hasNextPage = false
                    }
                }
                IdMapManager.getInstance(MemberBenefitsMap::class.java).save(userId)
                record(AntMember.TAG, "分类[$catName]处理完毕，已执行中间保存")
            }
            // 7. 保存映射表
            IdMapManager.getInstance(MemberBenefitsMap::class.java).save(userId)
            record(AntMember.TAG, "会员积分🎐全部分类任务处理完毕")
            setFlagToday("memberBenefit::refresh")

        } catch (t: Throwable) {
            record(AntMember.TAG, "memberPointExchangeBenefit 运行异常: ${t.message}")
            Log.printStackTrace(AntMember.TAG, t)
        }
    }

    private fun exchangeBenefit(benefitId: String, itemid: String, userid: String?): Boolean {
        try {
            val resString = AntMemberRpcCall.exchangeBenefit(benefitId, itemid, userid)
            val jo = JSONObject(resString)
            val resultCode = jo.optString("resultCode")

            if (resultCode == "BEYOND_BUYING_TIMES") {
                record(AntMember.TAG, "会员权益兑换已达上限，标记任务今日完成")
                memberPointExchangeBenefitToday(benefitId)
                return true
            }

            if (ResChecker.checkRes(AntMember.TAG + "会员权益兑换失败:", jo)) {
                memberPointExchangeBenefitToday(benefitId)
                return true
            }

        } catch (t: Throwable) {
            Log.printStackTrace(AntMember.TAG, "exchangeBenefit 错误:", t)
        }
        return false
    }
}
