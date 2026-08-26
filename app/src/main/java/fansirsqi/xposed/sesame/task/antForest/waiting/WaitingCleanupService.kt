package fansirsqi.xposed.sesame.task.antForest.waiting

import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.task.antForest.ForestUtil
import fansirsqi.xposed.sesame.task.antForest.UserEnergyPatternManager
import fansirsqi.xposed.sesame.task.antForest.EnergyWaitingManager
import fansirsqi.xposed.sesame.task.antForest.EnergyWaitingManager.WaitingTask
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 蹲点清理服务
 *
 * 单一职责：过期/僵尸任务清理（cleanExpiredTasks）+ 定期清理循环（startPeriodicCleanup）+ 全面重新验证（revalidateAllWaitingTasks）。
 * 锁内仅做快速状态操作（filter / 启动协程 / 去抖保存），无 RPC 与 delay；全面验证（含 RPC）在锁外执行。
 *
 * V3：时钟走注入的 [clock]，全面验证迁移到类型化 [WaitingRpcGateway.queryFriendHomePage]，
 * 删除固定 delay(200)（RPC 节流统一由 executor 的 slotReserver 控制），session 级启动（随 rootJob 取消）。
 */
internal class WaitingCleanupService(
    private val scope: CoroutineScope,
    private val repository: WaitingTaskRepository,
    private val scheduler: WaitingScheduler,
    private val mutex: Mutex,
    private val rpcGateway: WaitingRpcGateway,
    private val clock: WaitingClock,
) {

    /**
     * 清理过期任务
     *
     * @param enableRevalidation 是否强制执行全面验证
     */
    fun cleanExpiredTasks(enableRevalidation: Boolean = false) {
        scope.launch {
            // 获取锁，确保线程安全
            mutex.withLock {
                val now = clock.nowMillis()

                // 1. 僵尸任务检测：成熟超过 ZOMBIE_THRESHOLD_MS 未执行
                // 逻辑：保护期结束时间 或 产出时间 已经过去很久了，但任务还在列表中
                val matureTasks = repository.values().filter { task ->
                    val protectionEndTime = task.getProtectionEndTime()
                    // 取保护结束时间和产出时间中较大的一个作为“应该收取的时间”
                    val collectTime = if (protectionEndTime > now) protectionEndTime else task.produceTime
                    now > collectTime + EnergyWaitingManager.ZOMBIE_THRESHOLD_MS
                }

                // 重新触发已成熟任务（尝试唤醒僵尸任务，入口按 taskId 去重）
                if (matureTasks.isNotEmpty()) {
                    val taskNames = matureTasks.map { it.userName }.take(3).joinToString(",")
                    val moreText = if (matureTasks.size > 3) "等${matureTasks.size}个" else ""
                    Log.record(WaitingLogger.TAG, "🔄 重新触发蹲点：[${taskNames}${moreText}]已成熟但未执行")

                    matureTasks.forEach { task ->
                        // 重新启动倒计时协程
                        scheduler.start(task)
                    }
                }

                // 2. 过期任务：成熟超过 EXPIRE_THRESHOLD_MS
                // 逻辑：这种任务通常已经失效或无法收取，需要从内存中移除
                val expiredTasks = repository.values().filter { task ->
                    now > task.produceTime + EnergyWaitingManager.EXPIRE_THRESHOLD_MS
                }

                if (expiredTasks.isNotEmpty()) {
                    val taskNames = expiredTasks.map { it.userName }.take(3).joinToString(",")
                    val moreText = if (expiredTasks.size > 3) "等${expiredTasks.size}个" else ""
                    Log.record(WaitingLogger.TAG, "🧹 清理过期蹲点：[${taskNames}${moreText}]")

                    // 执行移除并去抖持久化
                    expiredTasks.forEach { task ->
                        repository.removeAndPersist(task.taskId)
                    }
                } else if (enableRevalidation) {
                    // 仅在手动调试或强制模式下打印此日志，避免刷屏
                    Log.record(WaitingLogger.TAG, "定期清理检查：无过期任务")
                }

                // 日志摘要：仅在有实际操作时输出，避免 30s 定期清理刷屏
                if (matureTasks.isNotEmpty() || expiredTasks.isNotEmpty()) {
                    Log.record(WaitingLogger.TAG, "清理维护完成，当前活跃蹲点${repository.size}个")
                }
            }

            // 3. 手动触发全面验证（锁外执行，内部自管锁，避免持锁期间 RPC/delay）
            if (enableRevalidation) {
                revalidateAllWaitingTasks()
            }
        }
    }

    /**
     * 启动定期清理任务（session 级：随 session rootJob.cancel() 取消）。
     */
    fun startPeriodicCleanup() {
        scope.launch {
            while (isActive) {
                try {
                    // 使用固定间隔进行清理
                    delay(EnergyWaitingManager.BASE_CHECK_INTERVAL_MS)
                    cleanExpiredTasks()

                    // 定期清理用户模式数据
                    UserEnergyPatternManager.cleanupExpiredPatterns()
                } catch (_: CancellationException) {
                    break
                } catch (e: Exception) {
                    Log.printStackTrace(WaitingLogger.TAG, "定期清理任务异常", e)
                }
            }
        }
    }

    /**
     * 重新验证所有蹲点任务的有效性（第2层防护）
     * 适用于管理器启动或任务恢复后的场景，确保移除已有保护罩覆盖的任务
     *
     * 并发模型：锁外收集任务快照并逐个 RPC 验证（类型化 queryFriendHomePage，不持锁、无固定 delay），
     * 最后在锁内批量移除无效任务，避免长时间占用互斥锁阻塞其他蹲点操作。
     */
    internal fun revalidateAllWaitingTasks() {
        scope.launch {
            // 锁外收集待验证快照（仅读，不需要持锁）
            val tasksToRevalidate = repository.values().toList()
            if (tasksToRevalidate.isEmpty()) {
                Log.record(WaitingLogger.TAG, "无需验证：当前无蹲点任务")
                return@launch
            }
            Log.record(WaitingLogger.TAG, "🔄 开始重新验证${tasksToRevalidate.size}个蹲点任务...")

            val tasksToRemove = mutableListOf<String>()

            // 锁外逐个验证（RPC 不持锁；节流由 executor 的 slotReserver 统一控制，内部不再 delay）
            tasksToRevalidate.forEach { task ->
                try {
                    // 自己的账号：无论是否有保护罩都保留（到时间后直接收取）
                    if (task.isSelf()) {
                        Log.record(WaitingLogger.TAG, "  ⭐️ 保留[${task.getUserTypeTag()}${task.userName}]球[${task.bubbleId}]：到时间直接收取")
                        return@forEach
                    }

                    // 好友账号：类型化查询用户主页以获取最新的保护罩状态
                    when (val r = rpcGateway.queryFriendHomePage(task.userId, task.fromTag)) {
                        is RpcResult.Failed -> {
                            Log.record(WaitingLogger.TAG, "  验证[${task.getUserTypeTag()}${task.userName}]：无法获取主页信息(${r.kind})，保留任务")
                            return@forEach
                        }
                        is RpcResult.Ok -> {
                            val userHomeObj = r.value
                            // 好友账号：如果保护罩覆盖能量成熟期则移除
                            if (ForestUtil.shouldSkipWaitingDueToProtection(userHomeObj, task.produceTime)) {
                                val protectionEndTime = ForestUtil.getProtectionEndTime(userHomeObj)
                                val timeDifference = protectionEndTime - task.produceTime
                                val formattedTimeDifference = WaitingLogger.formatTime(timeDifference)

                                Log.record(
                                    WaitingLogger.TAG,
                                    "  ❌ 移除[${task.getUserTypeTag()}${task.userName}]球[${task.bubbleId}]：保护罩覆盖能量成熟期($formattedTimeDifference)"
                                )
                                tasksToRemove.add(task.taskId)
                            } else {
                                Log.record(WaitingLogger.TAG, "  ✅ 保留[${task.getUserTypeTag()}${task.userName}]球[${task.bubbleId}]：可正常收取")
                            }
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.record(WaitingLogger.TAG, "  验证任务[${task.taskId}]时出错: ${e.message}，保留任务")
                }
            }

            // 锁内批量移除无效任务（验证期间任务可能已被其他路径移除，按实际删除数统计）
            mutex.withLock {
                var removedCount = 0
                tasksToRemove.forEach { taskId ->
                    if (repository.remove(taskId) != null) {
                        removedCount++
                    }
                }
                if (removedCount > 0) {
                    repository.schedulePersistenceSave()
                }

                val validCount = tasksToRevalidate.size - removedCount
                if (removedCount > 0) {
                    Log.record(WaitingLogger.TAG, "🧹 验证完成：移除${removedCount}个无效任务，保留${validCount}个有效任务")
                } else {
                    Log.record(WaitingLogger.TAG, "✅ 验证完成：所有${validCount}个任务均有效")
                }
            }
        }
    }
}
