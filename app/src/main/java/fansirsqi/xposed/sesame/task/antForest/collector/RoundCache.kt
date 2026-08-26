package fansirsqi.xposed.sesame.task.antForest.collector

import fansirsqi.xposed.sesame.util.maps.UserMap
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * 轮次缓存
 *
 * 单一职责：统一管理四类内存缓存，并在每轮任务结束时整体清空：
 * - [userNameCache]：userId -> userName
 * - [processedUsersCache]：本轮已处理过的用户 ID（避免重复处理）
 * - [emptyForestCache]：本轮已确认没有能量的好友（避免重复检查）
 * - [skipUsersCache]：有保护罩或其他需要跳过的用户（记录跳过原因）
 */
internal class RoundCache {

    private val userNameCache: ConcurrentHashMap<String, String> = ConcurrentHashMap()

    private val processedUsersCache: ConcurrentHashMap.KeySetView<String, Boolean> = ConcurrentHashMap.newKeySet()

    private val emptyForestCache: ConcurrentHashMap<String, Long> = ConcurrentHashMap()

    private val skipUsersCache: ConcurrentHashMap<String, String> = ConcurrentHashMap()

    /** 用户是否已在本轮处理过 */
    fun containsProcessed(userId: String?): Boolean =
        !userId.isNullOrEmpty() && processedUsersCache.contains(userId)

    /** 标记用户为本轮已处理 */
    fun addProcessed(userId: String?) {
        if (!userId.isNullOrEmpty()) {
            processedUsersCache.add(userId)
        }
    }

    /** 好友是否已在本轮确认为空森林 */
    fun containsEmpty(userId: String?): Boolean =
        !userId.isNullOrEmpty() && emptyForestCache.containsKey(userId)

    /** 标记好友本轮为空森林（记录时间戳，避免本轮重复检查） */
    fun markEmpty(userId: String?) {
        if (!userId.isNullOrEmpty()) {
            emptyForestCache[userId] = System.currentTimeMillis()
        }
    }

    /** 用户是否在跳过列表中 */
    fun containsSkip(userId: String?): Boolean =
        !userId.isNullOrEmpty() && skipUsersCache.containsKey(userId)

    /** 将用户加入跳过列表（内存缓存） */
    fun addSkip(userId: String?, reason: String) {
        if (!userId.isNullOrEmpty()) {
            skipUsersCache[userId] = reason
        }
    }

    /**
     * 统一获取和缓存用户名的方法
     * @param userId 用户ID
     * @param userHomeObj 用户主页对象（可选）
     * @param fromTag 来源标记（可选）
     * @return 用户名
     */
    fun getAndCacheUserName(userId: String?, userHomeObj: JSONObject?, fromTag: String?): String? {
        // 输入验证：userId为空时直接返回
        if (userId.isNullOrEmpty()) {
            return null
        }

        // 1. 尝试从缓存获取
        val cachedUserName = userNameCache.get(userId)
        if (!cachedUserName.isNullOrEmpty() && cachedUserName != userId) {
            // 如果缓存的不是userId本身，且不为空，则返回缓存值
            return cachedUserName
        }

        // 2. 根据上下文解析用户名
        var userName = resolveUserNameFromContext(userId, userHomeObj, fromTag)

        // 3. Fallback处理：如果解析失败，使用userId作为显示名
        if (userName.isNullOrEmpty()) {
            userName = userId
        }

        // 4. 存入缓存（只缓存有效的用户名）
        if (userName.isNotEmpty()) {
            userNameCache[userId] = userName
        }

        return userName
    }

    /** 统一获取用户名的简化方法（无上下文） */
    fun getAndCacheUserName(userId: String?): String? {
        return getAndCacheUserName(userId, null, null)
    }

    /** 从上下文中解析用户名 */
    private fun resolveUserNameFromContext(
        userId: String?,
        userHomeObj: JSONObject?,
        fromTag: String?
    ): String? {
        var userName: String? = null

        if ("pk" == fromTag && userHomeObj != null) {
            val userEnergy = userHomeObj.optJSONObject("userEnergy")
            if (userEnergy != null) {
                userName = "PK榜好友|" + userEnergy.optString("displayName")
            }
        } else {
            userName = UserMap.getMaskName(userId)
            if ((userName == null || userName == userId) && userHomeObj != null) {
                val userEnergy = userHomeObj.optJSONObject("userEnergy")
                if (userEnergy != null) {
                    val displayName = userEnergy.optString("displayName")
                    if (!displayName.isEmpty()) {
                        userName = displayName
                    }
                }
            }
        }
        return userName
    }

    /** 清空本轮全部缓存（每轮任务结束时调用） */
    fun clearAll() {
        userNameCache.clear()
        processedUsersCache.clear()
        // 清空本轮的空森林缓存，以便下一轮（如下次"执行间隔"到达）重新检查所有好友
        emptyForestCache.clear()
        // 清空跳过用户缓存，下一轮重新检测保护罩状态
        skipUsersCache.clear()
    }
}
