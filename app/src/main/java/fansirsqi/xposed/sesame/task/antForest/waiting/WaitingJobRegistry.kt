package fansirsqi.xposed.sesame.task.antForest.waiting

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/** 会话内所有业务 Job 的唯一登记表。 */
internal class WaitingJobRegistry(private val scope: CoroutineScope) {
    private val jobs = ConcurrentHashMap<JobKey, Job>()

    /**
     * 按 key 幂等启动唯一 Job。
     * 用 [ConcurrentHashMap.computeIfAbsent] 原子操作避免旧 LAZY+isActive 竞态
     * （start() 前 Job 处于 NEW 态 isActive==false，并发调用可能重复建 Job 并覆盖）。
     */
    fun launchUnique(key: JobKey, block: suspend CoroutineScope.() -> Unit): Job {
        return jobs.computeIfAbsent(key) { _ ->
            val job = scope.launch(start = CoroutineStart.LAZY, block = block)
            job.invokeOnCompletion { jobs.remove(key, job) }
            job.start()
            job
        }
    }

    suspend fun cancelAndJoinAll() {
        val snapshot = jobs.values.toList()
        snapshot.forEach(Job::cancel)
        snapshot.joinAll()
        jobs.clear()
    }

    /** 指定 Job 是否活跃（指标用，V2 §3.5.2：Restore 必须为 0/1）。 */
    fun countByKey(key: JobKey): Int = if (jobs[key]?.isActive == true) 1 else 0

    /** 统计满足 [predicate] 且活跃的 Job 数（指标用，如好友 worker 数）。 */
    fun countActiveByType(predicate: (JobKey) -> Boolean): Int =
        jobs.count { (key, job) -> predicate(key) && job.isActive }
}

internal sealed interface JobKey {
    data object Restore : JobKey
    data object Cleanup : JobKey
    data object Persist : JobKey
    data class FriendWorker(val userId: String) : JobKey
}
