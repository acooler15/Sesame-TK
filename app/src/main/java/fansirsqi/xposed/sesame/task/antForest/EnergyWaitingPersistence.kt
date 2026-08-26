package fansirsqi.xposed.sesame.task.antForest

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.core.type.TypeReference
import fansirsqi.xposed.sesame.core.store.DataStore
import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.task.antForest.waiting.PersistSnapshot
import fansirsqi.xposed.sesame.task.antForest.waiting.WriteResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 蹲点任务持久化数据类
 * 用于序列化和反序列化，存储到 DataStore
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class WaitingTaskPersistData(
    val userId: String = "",
    val userName: String = "",
    val bubbleId: Long = 0L,
    val produceTime: Long = 0L,
    val fromTag: String = "",
    val retryCount: Int = 0,
    val maxRetries: Int = 3,
    val retryNotBefore: Long = 0L,
    val shieldEndTime: Long = 0L,
    val bombEndTime: Long = 0L,
    val savedTime: Long = System.currentTimeMillis(),
    val ownerUid: String? = null,
    val schemaVersion: Int = 1
) {
    /**
     * 转换为运行时任务对象（savedTime 承载任务最初登记时间 registeredTime）
     * @param bindUid 显式请求 UID：V1 旧记录（ownerUid==null）绑定到该 UID；
     *                调用方须先通过 [loadTasks] 完成 ownerUid 校验，此处不再重复校验。
     */
    fun toWaitingTask(bindUid: String): EnergyWaitingManager.WaitingTask {
        return EnergyWaitingManager.WaitingTask(
            ownerUid = ownerUid ?: bindUid,
            generation = 0L,
            userId = userId,
            userName = userName,
            bubbleId = bubbleId,
            produceTime = produceTime,
            fromTag = fromTag,
            retryCount = retryCount,
            maxRetries = maxRetries,
            retryNotBefore = retryNotBefore,
            shieldEndTime = shieldEndTime,
            bombEndTime = bombEndTime,
            registeredTime = savedTime
        )
    }
}

/**
 * 运行时任务 → 持久化数据（一行转换，见优化方案 3.8）
 *
 * savedTime 语义修正：写入任务最初登记时间 registeredTime，而非"触发 saveTasks 的落盘时间"，
 * 使持久化侧的过期过滤（MAX_TASK_AGE_MS）与登记上限（maxWaitTimeMs）口径一致。
 */
fun EnergyWaitingManager.WaitingTask.toPersistData(): WaitingTaskPersistData {
    return WaitingTaskPersistData(
        ownerUid = ownerUid,
        userId = userId,
        userName = userName,
        bubbleId = bubbleId,
        produceTime = produceTime,
        fromTag = fromTag,
        retryCount = retryCount,
        maxRetries = maxRetries,
        retryNotBefore = retryNotBefore,
        shieldEndTime = shieldEndTime,
        bombEndTime = bombEndTime,
        savedTime = registeredTime
    )
}

/**
 * 蹲点任务持久化管理器
 *
 * 职责：
 * 1. 保存蹲点任务到 DataStore
 * 2. 从 DataStore 恢复蹲点任务
 * 3. 验证恢复的任务是否仍然有效
 * 4. 过滤过期或无效的任务
 */
object EnergyWaitingPersistence {
    private const val TAG = "EnergyWaitingPersistence"

    /**
     * 任务最大保存时间：= 登记上限 × [EnergyWaitingManager.MAX_TASK_AGE_MULTIPLIER]（默认 100min × 2 = 200min），
     * 与 [EnergyWaitingManager.maxWaitTimeMs] 联动，保证恢复过滤与登记上限口径一致。
     */
    private val MAX_TASK_AGE_MS: Long
        get() = EnergyWaitingManager.maxWaitTimeMs() * EnergyWaitingManager.MAX_TASK_AGE_MULTIPLIER

    // 协程作用域
    private val persistenceScope = CoroutineScope(Dispatchers.IO)

    /**
     * 获取指定账号的 DataStore 存储键
     * 每个账号使用独立的键，避免多账号切换时数据混淆
     *
     * @param uid 用户 uid（null/空 时使用默认键）
     * @return 包含用户 uid 的存储键，如果 uid 为空则使用默认键
     */
    private fun buildDataStoreKey(uid: String): String {
        require(uid.isNotEmpty()) { "waiting persistence requires explicit uid" }
        return "energy_waiting_tasks_$uid"
    }

    /**
     * 保存蹲点任务到 DataStore（异步 fire-and-forget，兼容旧调用方）
     *
     * key 与数据快照均在调用线程确定，避免异步落盘期间账户切换读到新 uid 而写错 key。
     * 需要确认写入结果的调用方应使用 [saveTasksNow]。
     *
     * @param tasks 当前活跃的蹲点任务
     * @param uid 目标账户 uid（必须显式传入，禁止在异步代码内读取当前 UID）
     */
    fun saveTasks(tasks: Map<String, EnergyWaitingManager.WaitingTask>, uid: String) {
        val persistDataList = tasks.values.map { it.toPersistData() }
        persistenceScope.launch {
            val result = writeToDataStore(uid, persistDataList)
            if (result is WriteResult.Failed) {
                Log.printStackTrace(TAG, "保存蹲点任务失败:", result.error)
            } else {
                Log.record(TAG, "✅ 保存${persistDataList.size}个蹲点任务到持久化存储 (key: ${buildDataStoreKey(uid)})")
            }
        }
    }

    /**
     * checked 保存：等待写入结果，可被最终 flush 依赖。
     * 在注入的 IO dispatcher 执行，不在调用线程触碰同步 DataStore。
     */
    suspend fun saveTasksNow(snapshot: PersistSnapshot): WriteResult = withContext(Dispatchers.IO) {
        writeToDataStore(snapshot.uid, snapshot.items)
    }

    private fun writeToDataStore(uid: String, persistDataList: List<WaitingTaskPersistData>): WriteResult {
        return try {
            val dataStoreKey = buildDataStoreKey(uid)
            if (DataStore.putChecked(dataStoreKey, persistDataList)) {
                WriteResult.Committed
            } else {
                WriteResult.Failed(RuntimeException("DataStore.saveToDisk reported failure"))
            }
        } catch (e: Exception) {
            Log.printStackTrace(TAG, "保存蹲点任务失败:", e)
            WriteResult.Failed(e)
        }
    }

    /**
     * 从 DataStore 加载蹲点任务（已过滤过期任务，并完成 ownerUid 归属校验）
     *
     * 迁移规则：
     * - V1 旧记录（ownerUid == null）：绑定到显式请求的 [uid]；
     * - V2 记录（ownerUid != uid）：记录错误并拒绝恢复，防止跨账户数据串写。
     *
     * @param uid 目标账户 uid（显式传入）
     * @return 恢复的任务列表
     */
    suspend fun loadTasks(uid: String): List<EnergyWaitingManager.WaitingTask> = withContext(Dispatchers.IO) {
        try {
            val dataStoreKey = buildDataStoreKey(uid)
            val typeRef = object : TypeReference<List<WaitingTaskPersistData>>() {}
            val persistDataList = DataStore.getOrCreate(dataStoreKey, typeRef)

            if (persistDataList.isEmpty()) {
                Log.record(TAG, "持久化存储中无蹲点任务 (key: $dataStoreKey)")
                return@withContext emptyList()
            }

            val currentTime = System.currentTimeMillis()
            val validTasks = mutableListOf<EnergyWaitingManager.WaitingTask>()
            var expiredCount = 0
            var tooOldCount = 0
            var foreignOwnerCount = 0

            persistDataList.forEach { persistData ->
                // 检查0：ownerUid 归属校验（V2 记录必须与请求 UID 一致）
                if (persistData.ownerUid != null && persistData.ownerUid != uid) {
                    foreignOwnerCount++
                    Log.record(TAG, "  拒绝[${persistData.userName}]：ownerUid[${persistData.ownerUid}]与请求 UID 不一致")
                    return@forEach
                }

                // 检查1：任务保存时间是否过久
                val taskAge = currentTime - persistData.savedTime
                if (taskAge > MAX_TASK_AGE_MS) {
                    tooOldCount++
                    Log.record(TAG, "  跳过[${persistData.userName}]：保存时间超过${taskAge / 1000 / 60 / 60}小时")
                    return@forEach
                }

                // 检查2：能量是否已经过期超过1小时
                if (currentTime > persistData.produceTime + 60 * 60 * 1000L) {
                    expiredCount++
                    Log.record(TAG, "  跳过[${persistData.userName}]：能量已过期超过1小时")
                    return@forEach
                }

                // 任务有效，绑定到请求 UID 后加入列表
                validTasks.add(persistData.toWaitingTask(uid))
            }

            Log.record(
                TAG,
                "📥 从持久化存储恢复${validTasks.size}个有效任务（跳过${expiredCount}个过期，${tooOldCount}个过旧，${foreignOwnerCount}个归属不符）"
            )

            validTasks
        } catch (e: Exception) {
            Log.printStackTrace(TAG, "加载蹲点任务失败:", e)
            emptyList()
        }
    }

    /**
     * 清空指定 UID 的持久化任务（checked）
     */
    suspend fun clearTasks(uid: String): WriteResult = withContext(Dispatchers.IO) {
        try {
            val dataStoreKey = buildDataStoreKey(uid)
            val committed = DataStore.putChecked(dataStoreKey, emptyList<WaitingTaskPersistData>())
            if (committed) {
                Log.record(TAG, "清空持久化存储 (key: $dataStoreKey)")
                WriteResult.Committed
            } else {
                WriteResult.Failed(RuntimeException("DataStore.saveToDisk reported failure"))
            }
        } catch (e: Exception) {
            Log.error(TAG, "清空持久化存储失败: ${e.message}")
            WriteResult.Failed(e)
        }
    }
}