package fansirsqi.xposed.sesame.task.antForest

import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.core.util.TimeUtil
import fansirsqi.xposed.sesame.hook.ApplicationHook
import fansirsqi.xposed.sesame.task.antForest.waiting.AccountToken
import fansirsqi.xposed.sesame.task.antForest.waiting.CallbackCandidate
import fansirsqi.xposed.sesame.task.antForest.waiting.CloseResult
import fansirsqi.xposed.sesame.task.antForest.waiting.EnergyCollectCallback
import fansirsqi.xposed.sesame.task.antForest.waiting.JobKey
import fansirsqi.xposed.sesame.task.antForest.waiting.MutationPlan
import fansirsqi.xposed.sesame.task.antForest.waiting.PersistSnapshot
import fansirsqi.xposed.sesame.task.antForest.waiting.Validation
import fansirsqi.xposed.sesame.task.antForest.waiting.WaitingAccountSession
import fansirsqi.xposed.sesame.task.antForest.waiting.WaitingEngineFactory
import fansirsqi.xposed.sesame.task.antForest.waiting.WaitingLogger
import fansirsqi.xposed.sesame.task.antForest.waiting.WaitingMetrics
import fansirsqi.xposed.sesame.task.antForest.waiting.WaitingPersistenceWriter
import fansirsqi.xposed.sesame.task.antForest.waiting.WaitingProducerHandle
import fansirsqi.xposed.sesame.task.antForest.waiting.WaitingTaskDraft
import fansirsqi.xposed.sesame.task.antForest.waiting.WriteResult
import fansirsqi.xposed.sesame.task.antForest.waiting.bind
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * 能量球蹲点管理器（精确时机版）
 *
 * 门面（Facade）：仅做生命周期编排与公共 API 门面，不承担具体业务逻辑。
 * V3 起引擎完全下沉到 [WaitingAccountSession]（repository + scheduler + executor + cleanup），
 * 门面只持有：
 * - `lifecycleScope` / `generationCounter` / `producerCounter`；
 * - `activeSession`（[AtomicReference]）；
 * - `callbackCandidate`；
 * - `persistenceWriter`（进程级 per-uid 串行 writer，flush 可能跨 session 生命周期）；
 * - `pendingFlushMap`（flush 失败快照，Activate 前重试）。
 *
 * 核心原则：
 * 1. 无保护时：严格按能量球成熟时间收取
 * 2. 有保护时：等到保护结束后立即收取
 * 3. 不提前收取：避免无效请求
 * 4. 精确时机：确保在正确的时间点执行收取
 *
 * @author Sesame-TK Team
 */
object EnergyWaitingManager {
    private const val TAG = WaitingLogger.TAG

    /**
     * 等待任务数据类
     */
    data class WaitingTask(
        val userId: String,
        val userName: String,
        val ownerUid: String = userId,
        val generation: Long = 0L,
        val taskVersion: Long = 0L, // 同 taskId 每次更新时递增，用于拒绝 RPC 期间已更新任务的过期结果（V2 §3.3.5）
        val bubbleId: Long,
        val produceTime: Long,
        val fromTag: String,
        val retryCount: Int = 0,
        val maxRetries: Int = 3,
        val retryNotBefore: Long = 0L, // 所有蹲点 RPC 的统一门禁（退避期间不得执行保护验证/恢复验证/主页查询/收取）
        val shieldEndTime: Long = 0, // 保护罩结束时间
        val bombEndTime: Long = 0,    // 炸弹卡结束时间
        val registeredTime: Long = System.currentTimeMillis() // 任务最初登记时间（持久化侧据此判断是否过期，替代落盘时间 savedTime）
    ) {
        val taskId: String = "${userId}_${bubbleId}"

        fun withRetry(): WaitingTask = this.copy(retryCount = retryCount + 1, taskVersion = taskVersion + 1)

        /**
         * 检查是否是自己的账号
         */
        fun isSelf(): Boolean {
            return userId == ownerUid
        }

        fun belongsTo(ownerUid: String, generation: Long): Boolean {
            return this.ownerUid == ownerUid && this.generation == generation
        }

        /**
         * 检查是否有保护（保护罩或炸弹卡）
         */
        fun hasProtection(currentTime: Long = System.currentTimeMillis()): Boolean {
            return shieldEndTime > currentTime || bombEndTime > currentTime
        }

        /**
         * 获取保护结束时间（取最晚的时间）
         */
        fun getProtectionEndTime(): Long {
            return maxOf(shieldEndTime, bombEndTime)
        }

        /**
         * 获取用户类型标签（用于日志）
         */
        fun getUserTypeTag(): String {
            return if (isSelf()) "⭐️主号|" else "好友|"
        }
    }

    // === 协程作用域与同步原语 ===
    /**
     * 生命周期控制 scope：承载生命周期 actor（账户激活/切换/关闭/广播重启的串行编排），
     * 不承载蹲点等待/RPC（那些在 session scope 内）。
     */
    private val lifecycleScope = CoroutineScope(
        Dispatchers.Default +
                SupervisorJob() +
                CoroutineName("EnergyWaitingLifecycle")
    )

    // === 账户会话状态 ===
    private val generationCounter = AtomicLong(0)
    private val producerCounter = AtomicLong(0)

    /** 当前活跃账户会话（进程级门面只持有这一个） */
    private val activeSession = AtomicReference<WaitingAccountSession?>(null)

    /** 回调候选：AntForest.boot 只登记候选，initHandler 完整成功后由 onInitialized 激活 */
    @Volatile
    private var callbackCandidate: CallbackCandidate? = null

    /** 进程级 per-uid 串行 writer：debounce 落盘与 close flush 共用（flush 可能跨 session 生命周期） */
    private val persistenceWriter = WaitingPersistenceWriter { snapshot ->
        EnergyWaitingPersistence.saveTasksNow(snapshot)
    }

    /** 失败 flush 快照：closeSession FlushFailed 时暂存，Activate 前重试成功后才允许创建新 session */
    private val pendingFlushMap = ConcurrentHashMap<AccountToken, PersistSnapshot>()

    // === 生命周期 actor：单 Channel 串行执行，避免 closeSession 的 cancelAndJoin 与 submit 并发 ===
    private val lifecycleActor = Channel<suspend () -> Unit>(Channel.UNLIMITED).also { channel ->
        lifecycleScope.launch {
            for (cmd in channel) {
                try {
                    cmd()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.printStackTrace(TAG, "生命周期命令执行异常", e)
                }
            }
        }
    }

    /** 提交命令到 actor 串行执行（非阻塞）。 */
    private fun submitLifecycleCommand(block: suspend () -> Unit) {
        lifecycleActor.trySend(block)
    }

    /**
     * 提交蹲点任务（新入口，必须携带生产者句柄，见优化方案 V2 3.1.3）
     *
     * 外部入口保持非阻塞：仅校验句柄归属后投递到生命周期 actor（与 closeSession 串行）。
     */
    fun submit(handle: WaitingProducerHandle, draft: WaitingTaskDraft) {
        val session = activeSession.get() ?: return
        if (handle != session.handle) return // 旧代次 handle 一律拒绝
        if (session.closing.get()) return
        submitLifecycleCommand {
            val task = draft.bind(session.token)
            submitInSession(session, task)
        }
    }

    /**
     * 旧入口（无 handle）：fail-closed。无 producer handle 的调用不能推断归属，直接拒绝；
     * 归属匹配时同样走 actor 串行（不直接写入 repository）。
     */
    fun submit(task: WaitingTask) {
        val session = activeSession.get()
        if (session == null ||
            task.ownerUid != session.token.ownerUid ||
            task.generation != session.token.generation
        ) {
            Log.record(TAG, "蹲点提交被拒绝（旧代次任务或无活跃会话），taskId=${task.taskId}")
            return
        }
        submitLifecycleCommand { submitInSession(session, task) }
    }

    /**
     * 会话内提交（锁内只返回 MutationPlan，锁外执行持久化/worker 信号）。
     */
    private suspend fun submitInSession(session: WaitingAccountSession, task: WaitingTask) {
        session.ensureCurrent(activeSession)
        // 上限过滤：等待时间超过登记上限则 removeAndSnapshot 并丢弃（与旧代码一致）
        val waitTime = task.produceTime - session.components.clock.nowMillis()
        if (waitTime > maxWaitTimeMs()) {
            Log.record(TAG, "蹲点任务等待超限，丢弃：taskId=${task.taskId}, waitTime=${waitTime}ms")
            val plan = session.stateMutex.withLock {
                session.repository.removeAndSnapshot(task.taskId)
            }
            (plan as? MutationPlan.Mutated)?.persistSnapshot?.let { persistenceWriter.writeAndAwait(it) }
            // 唤醒 worker 重选：若该好友 worker 正在等这个任务，移除后需重新选目标
            (plan as? MutationPlan.Mutated)?.changedUserId?.let { session.scheduler.signalUserChanged(it) }
            return
        }
        val plan = session.stateMutex.withLock {
            session.ensureCurrent(activeSession)
            session.repository.upsertAndSnapshot(task)
        }
        when (plan) {
            is MutationPlan.Mutated -> {
                plan.persistSnapshot?.let { persistenceWriter.writeAndAwait(it) }
                plan.changedUserId?.let { session.scheduler.ensureUserWorker(it) }
            }
            else -> Unit
        }
    }

    /**
     * 登记回调候选（V2 3.1.1）：AntForest.boot 只登记候选与不可变 UID，
     * 不启动恢复/清理；initHandler 完整成功后由 [onInitialized] 激活。
     */
    fun registerCallbackCandidate(
        uid: String,
        callback: EnergyCollectCallback,
        bindHandle: (WaitingProducerHandle) -> Unit,
    ) {
        callbackCandidate = CallbackCandidate(uid, callback, bindHandle)
        Log.record(TAG, "已登记蹲点回调候选")
    }

    /**
     * 获取当前正在等待的蹲点任务数量（只读 API，经 activeSession 代理，不加锁：
     * repository 的 ConcurrentHashMap.size 线程安全，与旧代码行为一致）。
     */
    fun getWaitingTaskCount(): Int {
        return activeSession.get()?.repository?.size ?: 0
    }

    /**
     * 获取蹲点任务详细状态（仅显示最近的3个；门面只读 API 用系统时间，与旧代码一致）。
     */
    fun getWaitingTasksStatus(): String {
        val session = activeSession.get() ?: return "无蹲点任务"
        val currentTime = System.currentTimeMillis()  // 门面只读 API，不需 seam 注入
        val tasks = session.repository.values().sortedBy { it.produceTime }
        if (tasks.isEmpty()) return "无蹲点任务"

        val statusBuilder = StringBuilder()
        val displayCount = minOf(3, tasks.size)

        statusBuilder.append("蹲点任务状态 (${tasks.size}个，显示最近${displayCount}个):\n")

        tasks.take(displayCount).forEach { task ->
            val status = WaitingLogger.formatTimeStatus(currentTime, task.produceTime)
            val executeTime = TimeUtil.getCommonDate(task.produceTime)

            val protectionEndTime = task.getProtectionEndTime()
            val hasProtection = protectionEndTime > currentTime
            val protectionInfo = if (hasProtection) {
                val protectionStatus = WaitingLogger.formatTimeStatus(currentTime, protectionEndTime)
                " (保护${protectionStatus.removePrefix("剩余")})"
            } else {
                ""
            }

            statusBuilder.append("  - [${task.userName}] 球[${task.bubbleId}] $status$protectionInfo → $executeTime\n")
        }

        if (tasks.size > displayCount) {
            statusBuilder.append("  ... 还有${tasks.size - displayCount}个任务")
        }

        return statusBuilder.toString().trimEnd()
    }

    // === 生命周期入口：全部提交到 actor 串行执行 ===

    /** 账户切换入口：提交 Close 命令到 actor（V3 §3.1.3）。 */
    fun onAccountSwitch(oldUid: String) {
        submitLifecycleCommand { switchTo(oldUid) }
    }

    /**
     * 服务销毁入口：提交 Destroy 命令到 actor，close + flush 完成后触发 [onComplete]。
     * （广播重启调用点从 ApplicationHook 的 destroyHandler 之后移到该回调中，见 V3 §3.1.3。）
     */
    fun onServiceDestroy(onComplete: (() -> Unit)? = null) {
        submitLifecycleCommand {
            val session = activeSession.get()
            val result = if (session != null) closeSession(session.token) else CloseResult.Closed
            if (result is CloseResult.FlushFailed) {
                Log.record(TAG, "服务销毁 flush 失败，仍发送重启广播（数据可能未保存）")
            }
            onComplete?.invoke()
        }
    }

    /**
     * 初始化完整成功后激活候选（V2 3.1.2）。
     * 在生命周期 actor 内串行执行：pending flush 全部成功且旧 session 已关闭时才允许创建新 generation。
     */
    fun onInitialized() {
        submitLifecycleCommand { activateCandidateIfReady() }
    }

    // === 生命周期 actor 内部实现 ===

    /** 账户切换：ownerUid 校验 → 指标摘要 → closeSession → FlushFailed 阻塞后续 Activate。 */
    private suspend fun switchTo(oldUid: String) {
        val session = activeSession.get()
        if (session == null || session.token.ownerUid != oldUid) {
            // ownerUid 不匹配则忽略（与旧代码语义一致）
            return
        }
        // 输出本次会话的蹲点运行指标摘要（V2 §3.5.2，内部观测）
        val metricNow = session.components.clock.nowMillis()
        WaitingMetrics.logSummary(
            activeTasks = session.repository.size.toLong(),
            friendWorkers = session.scheduler.friendWorkerCount().toLong(),
            retryPendingTasks = session.repository.values().count { it.retryNotBefore > metricNow }.toLong(),
            restoreJobs = session.jobs.countByKey(JobKey.Restore).toLong(),
        )
        val result = closeSession(session.token)
        // close 完成后 activateCandidateIfReady() 由调用方在 onInitialized/onAccountPost 中触发
        if (result is CloseResult.FlushFailed) {
            Log.record(TAG, "切换账号 flush 失败，阻塞后续 Activate")
        }
    }

    /**
     * 关闭 session：完整 Close barrier（V3 §3.1.2）。
     * - 幂等：activeSession 已为 null 时重复 Destroy/Close 返回 [CloseResult.Closed]；
     * - 摘除 activeSession（新提交立即 fail-closed）→ cancelAndJoin → 最终 flush → Committed/Failed。
     */
    private suspend fun closeSession(expectedToken: AccountToken): CloseResult {
        val session = activeSession.get()
            ?: return CloseResult.Closed // 无活跃 session，视为已关闭（幂等：重复 Destroy 命中此分支）

        if (session.token != expectedToken) return CloseResult.StaleCommand

        val closeStart = System.currentTimeMillis()
        session.closing.set(true)
        activeSession.compareAndSet(session, null)              // 摘除：新提交立即 fail-closed

        session.rootJob.cancelAndJoin()                         // 取消 + 等待全部子 Job 退出
        val snapshot = session.stateMutex.withLock {
            session.repository.capturePersistSnapshot()          // 无参方法，uid/generation 在构造时绑定
        }
        val result = when (val r = persistenceWriter.writeAndAwait(snapshot)) {
            WriteResult.Committed, WriteResult.StaleSkipped -> {
                session.repository.clear()
                CloseResult.Closed
            }
            is WriteResult.Failed -> {
                // 不 clear repository：保留内存数据，防止 session 对象被 GC 后数据丢失
                // 保存失败快照到 pendingFlushMap，供后续 retryFlush 重试
                pendingFlushMap[expectedToken] = snapshot
                CloseResult.FlushFailed(r)
            }
        }
        WaitingMetrics.accountCloseMs.set(System.currentTimeMillis() - closeStart)
        return result
    }

    /**
     * 激活候选：先重试所有 pending flush，全部成功后才创建新 session；
     * pending flush 仍失败则阻塞 Activate（下次生命周期命令再重试）。
     */
    private suspend fun activateCandidateIfReady() {
        // 重试所有 pending flush
        val pendingTokens = pendingFlushMap.keys.toList()
        for (token in pendingTokens) {
            val snapshot = pendingFlushMap[token] ?: continue
            val r = persistenceWriter.writeAndAwait(snapshot)
            if (r !is WriteResult.Failed) {
                pendingFlushMap.remove(token)
                Log.record(TAG, "重试 flush 成功：token=${token.generation}")
            } else {
                Log.record(TAG, "重试 flush 仍失败，阻塞 Activate：token=${token.generation}")
                return  // 阻塞 Activate，等待下次重试
            }
        }

        if (activeSession.get() != null) return
        val cand = callbackCandidate ?: return
        activateCandidate(cand)
    }

    private suspend fun activateCandidate(cand: CallbackCandidate) {
        check(activeSession.get() == null) { "activateCandidate requires no active session" }

        val token = AccountToken(
            ownerUid = cand.uid,
            generation = generationCounter.incrementAndGet(),
        )
        val handle = WaitingProducerHandle(
            token = token,
            producerId = producerCounter.incrementAndGet(),
        )
        val session = WaitingAccountSession(
            handle,
            cand.callback,
            lifecycleScope,
            WaitingEngineFactory.createRealComponents(),
            persistenceWriter,
        )
        activeSession.set(session)
        cand.bindHandle(handle)

        // 每个 session 只允许一个 Restore Job；Cleanup 已在 session init 内以 session 级启动
        session.jobs.launchUnique(JobKey.Restore) { restore(session) }
        Log.record(TAG, "账户会话已激活（generation=${token.generation}）")
    }

    /**
     * 类型化恢复规则（V3 §3.2.2）：awaitIdle → 显式 UID 加载 → DeferredByBackoff 保守恢复
     * → 锁外类型化验证 → 锁内 upsert → 规范化快照回写。
     * 每次 RPC 前后都校验 session；被关闭后不回写。
     */
    private suspend fun restore(session: WaitingAccountSession) {
        val uid = session.token.ownerUid
        try {
            persistenceWriter.awaitIdle(uid)
            val records = EnergyWaitingPersistence.loadTasks(uid)   // 返回 List<WaitingTask>
            if (records.isEmpty()) return

            Log.record(TAG, "🔄 从持久化存储恢复${records.size}个蹲点任务...")

            for (record in records) {
                // 返回值检查替代异常路径，避免 SupervisorJob 下异常结束的副作用
                if (!session.isCurrent(activeSession)) return
                val restored = record.copy(
                    ownerUid = uid,
                    generation = session.token.generation,
                    taskVersion = 1L,
                )
                val validation =
                    if (session.components.clock.nowMillis() < restored.retryNotBefore) Validation.DeferredByBackoff
                    else session.components.rpcGateway.validateProtection(restored)   // 锁外 RPC
                if (!session.isCurrent(activeSession)) return

                val plan = session.stateMutex.withLock {
                    if (!session.isCurrent(activeSession)) return@withLock MutationPlan.none()
                    if (session.repository.contains(restored.taskId)) MutationPlan.none()
                    else when (validation) {
                        is Validation.TerminalInvalid -> MutationPlan.none()
                        is Validation.Valid, is Validation.TransientFailure,
                        Validation.DeferredByBackoff ->
                            session.repository.upsertAndSnapshot(restored)
                    }
                }
                when (plan) {
                    is MutationPlan.Mutated -> {
                        plan.persistSnapshot?.let { persistenceWriter.writeAndAwait(it) }
                        plan.changedUserId?.let {
                            session.scheduler.ensureUserWorker(it)
                            session.scheduler.signalUserChanged(it)
                        }
                    }
                    else -> Unit
                }
            }
            // 结束后写回一次规范化快照，使 TerminalInvalid 不再反复验证
            val normalizedSnapshot = session.stateMutex.withLock {
                session.repository.capturePersistSnapshot()  // 此时已 upsert 所有有效任务
            }
            persistenceWriter.writeAndAwait(normalizedSnapshot)
        } catch (e: CancellationException) {
            throw e
        } catch (e: IllegalStateException) {
            // session 被 close/switch，属正常控制流，静默退出
            Log.record(TAG, "restore 中断：session 已失效")
        } catch (e: Exception) {
            Log.error(TAG, "恢复蹲点任务失败: ${e.message}")
            Log.printStackTrace(TAG, e)
        }
    }

    // === 蹲点内部调优常量（集中一处可查，不暴露给用户配置，见优化方案 3.6） ===

    /** 蹲点登记上限倍数：maxWaitTimeMs() = 执行间隔 × 倍数 */
    const val WAIT_TIME_MULTIPLIER = 2

    /**
     * 蹲点登记上限（毫秒）：= 执行间隔 × [WAIT_TIME_MULTIPLIER]。
     *
     * 行为变更（原固定 8h）：主任务每轮都会重新遍历好友并去重登记，球进入成熟前一个轮询周期
     * 的窗口时自然会被下一轮主任务登记兜底，故登记上限随执行间隔动态缩短（默认 50min → 100min），
     * 避免蹲点协程过早挂起、期间被主任务反复确认。一旦主任务持续异常，蹲点可能漏收，需留意。
     */
    fun maxWaitTimeMs(): Long =
        ApplicationHook.config.checkInterval.value.toLong() * WAIT_TIME_MULTIPLIER

    /** 持久化侧保存时间上限倍数：MAX_TASK_AGE_MS = maxWaitTimeMs() × 倍数 */
    const val MAX_TASK_AGE_MULTIPLIER = 2

    /** 定期检查 / 轮询粒度（30s） */
    const val BASE_CHECK_INTERVAL_MS = 30 * 1000L

    /** 僵尸任务阈值：成熟 2 分钟仍未执行视为僵尸任务，清理时重新触发 */
    const val ZOMBIE_THRESHOLD_MS = 2 * 60 * 1000L

    /** 过期任务阈值：成熟 1 小时视为过期，清理时移除 */
    const val EXPIRE_THRESHOLD_MS = 60 * 60 * 1000L
}
