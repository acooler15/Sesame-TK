package fansirsqi.xposed.sesame.task.antForest.waiting

import fansirsqi.xposed.sesame.task.antForest.WaitingTaskPersistData
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/** 账户令牌：提交方必须携带激活时分配的精确生产者句柄，不能只凭 UID 判断归属。 */
data class AccountToken(
    val ownerUid: String,
    val generation: Long,
)

/** 写入戳：(generation, repositoryRevision)。跨 session 比较必须使用写入戳，不能只比本地 revision。 */
data class WriteStamp(
    val generation: Long,
    val repositoryRevision: Long,
) {
    fun isNewerThan(other: WriteStamp): Boolean =
        generation > other.generation ||
            (generation == other.generation && repositoryRevision > other.repositoryRevision)
}

/** 不可变持久化快照：uid、写入戳与数据必须一起捕获。 */
data class PersistSnapshot(
    val uid: String,
    val stamp: WriteStamp,
    val items: List<WaitingTaskPersistData>,
)

/** checked 写入结果：失败不能被报告为成功。 */
sealed interface WriteResult {
    data object Committed : WriteResult

    /** 因旧 generation/写入戳被跳过的写入（计入 persistenceStaleSkipped 指标）。 */
    data object StaleSkipped : WriteResult

    data class Failed(val error: Throwable) : WriteResult
}

/**
 * 同 UID 写入的串行 writer（per-uid Mutex：不同 UID 的 flush 互不阻塞）。
 * 只提交该 UID 最新 stamp：旧 generation 的 snapshot 不能覆盖新 generation。
 * 存储动作通过 [storage] 注入，便于纯 JVM 测试（生产装配为 EnergyWaitingPersistence::saveTasksNow）。
 */
class WaitingPersistenceWriter(
    private val storage: suspend (PersistSnapshot) -> WriteResult,
) {
    private val mutexPerUid = ConcurrentHashMap<String, Mutex>()
    private val lastStampPerUid = ConcurrentHashMap<String, WriteStamp>()

    private fun mutexFor(uid: String): Mutex =
        mutexPerUid.computeIfAbsent(uid) { Mutex() }

    suspend fun writeAndAwait(snapshot: PersistSnapshot): WriteResult = mutexFor(snapshot.uid).withLock {
        WaitingMetrics.persistenceRequested.incrementAndGet()
        val last = lastStampPerUid[snapshot.uid]
        if (last != null && !snapshot.stamp.isNewerThan(last)) {
            WaitingMetrics.persistenceStaleSkipped.incrementAndGet()
            return@withLock WriteResult.StaleSkipped
        }
        val result = storage(snapshot)
        if (result is WriteResult.Committed) {
            lastStampPerUid[snapshot.uid] = snapshot.stamp
            WaitingMetrics.persistenceWritten.incrementAndGet()
        }
        result
    }

    /** 等待目标 UID 的所有待写入完成（close barrier 前置，不阻塞其他 UID 的写入）。 */
    suspend fun awaitIdle(uid: String) {
        mutexFor(uid).withLock { }
    }
}
