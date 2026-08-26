package fansirsqi.xposed.sesame.task.antForest.waiting

import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.task.antForest.EnergyWaitingManager.WaitingTask
import fansirsqi.xposed.sesame.task.antForest.toPersistData
import kotlinx.coroutines.CoroutineScope
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 蹲点任务数据仓库
 *
 * 单一职责：waitingTasks 的增删查改 + 去抖持久化（checked 写入）。
 * 数据存储与调度逻辑解耦，替换持久化实现（如改用 Room）只动本类。
 *
 * 构造时绑定 ownerUid + generation（不再由门面调用 setGeneration），
 * 持久化写入统一走注入的 [persistenceWriter]（per-uid 串行 + stamp 校验），
 * 使 debounce 落盘与 close flush 共用一条写路径（V3 §3.1.1 第 8 点）。
 */
internal class WaitingTaskRepository(
    scope: CoroutineScope,
    private val ownerUid: String,
    generation: Long,
    private val persistenceWriter: WaitingPersistenceWriter,
) {

    private val waitingTasks = ConcurrentHashMap<String, WaitingTask>()

    /** 会话私有 revision：每次状态变化递增，跨 session 比较须与 generation 组成写入戳。 */
    private val revision = AtomicLong(0)

    /** 当前 session generation（构造时绑定，供持久化写入戳使用）。 */
    @Volatile
    private var currentGeneration: Long = generation

    private val persistenceDebouncer = PersistenceDebouncer(scope, PERSISTENCE_DEBOUNCE_MS) {
        val result = persistenceWriter.writeAndAwait(capturePersistSnapshot())
        if (result is WriteResult.Failed) {
            Log.printStackTrace(WaitingLogger.TAG, "蹲点持久化写入失败", result.error)
        }
    }

    /** 当前蹲点任务数量 */
    val size: Int
        get() = waitingTasks.size

    /** 是否为空 */
    val isEmpty: Boolean
        get() = waitingTasks.isEmpty()

    /** 查询单个任务 */
    fun get(taskId: String): WaitingTask? = waitingTasks[taskId]

    /** 任务是否存在 */
    fun contains(taskId: String): Boolean = waitingTasks.containsKey(taskId)

    /** 覆盖写入（put 语义，同名 taskId 更新；供 executor 结果应用与 scheduleRetry 使用） */
    fun put(taskId: String, task: WaitingTask) {
        waitingTasks[taskId] = task
        revision.incrementAndGet()
    }

    /** 移除任务 */
    fun remove(taskId: String): WaitingTask? {
        val removed = waitingTasks.remove(taskId)
        if (removed != null) {
            revision.incrementAndGet()
        }
        return removed
    }

    /** 全部任务值 */
    fun values(): Collection<WaitingTask> = waitingTasks.values

    /** 按好友过滤 */
    fun tasksOf(userId: String): List<WaitingTask> =
        waitingTasks.values.filter { it.userId == userId }

    /**
     * 插入或更新任务，返回包含持久化快照的 [MutationPlan]。须在 stateMutex 内调用。
     * 若 taskId 已存在且 produceTime 相同，返回 [MutationPlan.None]（幂等跳过）。
     * 若 taskId 已存在且 produceTime 变化，递增 taskVersion，
     * 防止 applyBatchResultIfCurrent 的 taskVersion 匹配失效。
     */
    fun upsertAndSnapshot(task: WaitingTask): MutationPlan {
        val existing = waitingTasks[task.taskId]
        if (existing != null && existing.produceTime == task.produceTime) {
            return MutationPlan.none()
        }
        val toStore = if (existing != null) task.copy(taskVersion = existing.taskVersion + 1) else task
        waitingTasks[task.taskId] = toStore
        revision.incrementAndGet()
        return MutationPlan.Mutated(
            persistSnapshot = capturePersistSnapshot(),
            changedUserId = toStore.userId,
            result = SubmitResult.Accepted,
        )
    }

    /**
     * 移除任务，返回包含持久化快照的 [MutationPlan]。须在 stateMutex 内调用。
     * （用于 maxWaitTime 超限过滤）
     */
    fun removeAndSnapshot(taskId: String): MutationPlan {
        val removed = waitingTasks.remove(taskId) ?: return MutationPlan.none()
        revision.incrementAndGet()
        return MutationPlan.Mutated(
            persistSnapshot = capturePersistSnapshot(),
            changedUserId = removed.userId,
            result = SubmitResult.IgnoredDuplicate,
        )
    }

    /**
     * 捕获该好友已到点任务的精确请求快照（bubbleId → 版本戳），须在锁内调用。
     * 只纳入 `maxOf(produceTime, retryNotBefore) <= now` 的任务（V2 §3.2.3）。
     * token 由本仓库绑定的 ownerUid/generation 内部构造。
     */
    fun captureDueCollectRequest(userId: String, now: Long): WaitingCollectRequest {
        val token = AccountToken(ownerUid, currentGeneration)
        val expected = waitingTasks.values
            .filter { it.userId == userId && maxOf(it.produceTime, it.retryNotBefore) <= now }
            .associate { it.bubbleId to TaskStamp(it.taskId, it.taskVersion, it.produceTime) }
        return WaitingCollectRequest(token, userId, expected)
    }

    /** 快照（拷贝一份 Map，供持久化/批量操作） */
    fun snapshot(): Map<String, WaitingTask> = waitingTasks.toMap()

    /** 清空内存任务（账户切换/close 时使用） */
    fun clear() {
        waitingTasks.clear()
        revision.incrementAndGet()
    }

    /** 捕获当前 revision 下的不可变持久化快照（供 debounce 落盘与最终 flush 使用）。 */
    fun capturePersistSnapshot(): PersistSnapshot = PersistSnapshot(
        uid = ownerUid,
        stamp = WriteStamp(generation = currentGeneration, repositoryRevision = revision.get()),
        items = waitingTasks.values.map { it.toPersistData() },
    )

    /** 触发一次去抖持久化保存（1 秒内的多次变更合并为一次落盘） */
    fun schedulePersistenceSave() = persistenceDebouncer.trigger()

    /** 取消待执行的保存（账户切换等需要立即切换落盘目标的场景） */
    fun cancelPendingSave() = persistenceDebouncer.cancel()

    /**
     * 移除蹲点任务并触发去抖持久化
     * @param taskId 任务ID
     * @param reason 移除原因（非空时输出日志）
     */
    fun removeAndPersist(taskId: String, reason: String = "") {
        waitingTasks.remove(taskId)
        revision.incrementAndGet()
        schedulePersistenceSave()
        if (reason.isNotEmpty()) {
            Log.record(WaitingLogger.TAG, "移除蹲点[$taskId]：$reason")
        }
    }

    private companion object {
        // === 持久化去抖 ===
        const val PERSISTENCE_DEBOUNCE_MS = 1000L        // 1 秒内的多次变更合并为一次落盘
    }
}
