package fansirsqi.xposed.sesame.task.antForest.collector

import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.task.antForest.AntForest
import fansirsqi.xposed.sesame.util.maps.UserMap

/**
 * 自己能量收取器
 *
 * 单一职责：立即收取自己的能量（collectSelfEnergyImmediately）。
 * 通过 EnergyBubbleExtractor 严格执行"收自己单个能量球方式 + 阈值"判断逻辑。
 */
internal class SelfEnergyCollector(
    private val task: AntForest,
    private val core: EnergyCollectCore,
    private val extractor: EnergyBubbleExtractor,
) {

    /**
     * 立即收取自己能量（专用方法）
     */
    suspend fun collectSelfEnergyImmediately(tag: String = "立即收取") {
        try {
            // querySelfHome 内部会处理 updateSelfHomePage 逻辑，确保道具倒计时等状态同步
            val selfHomeObj = task.querySelfHome()
            if (selfHomeObj != null) {
                Log.record(AntForest.TAG, "🎯 $tag：开始收取自己能量...")
                val availableBubbles: MutableList<Long> = ArrayList()
                val serverTime = selfHomeObj.optLong("now", System.currentTimeMillis())

                // 调用 extractBubbleInfo，该方法内部调用了 shouldCollectSelfBubble(bubbleCount, canBeRobbedAgain)
                // 从而严格执行了【收自己单个能量球方式】和【阈值】的判断逻辑。
                // 只有符合条件的 bubbleId 才会加入 availableBubbles
                extractor.extractBubbleInfo(selfHomeObj, serverTime, availableBubbles, UserMap.currentUid)

                if (availableBubbles.isNotEmpty()) {
                    Log.record(AntForest.TAG, "🎯 $tag：找到${availableBubbles.size}个符合阈值条件的可收能量球")
                    // 即使 batchRobEnergy 为 true，collectVivaEnergy 也是对传入的 list 进行操作
                    // 因此【一键收取】、【找能量】、【普通收取】都复用了这个逻辑，保证了统一性
                    core.collectVivaEnergy(UserMap.currentUid, selfHomeObj, availableBubbles, "加速卡$tag", skipPropCheck = true)
                } else {
                    Log.record(AntForest.TAG, "🎯 $tag：未找到满足条件的能量球 (可能是被阈值过滤或无能量)")
                }
            }
        } catch (e: Exception) {
            Log.printStackTrace(AntForest.TAG, "collectSelfEnergyImmediately err", e)
        }
    }
}
