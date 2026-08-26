package fansirsqi.xposed.sesame.task.antForest.waiting

import fansirsqi.xposed.sesame.task.antForest.EnergyWaitingManager.WaitingTask
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/** 提交方携带的精确生产者句柄：A₁ → B → A₂ 后旧 handle 因 generation/producerId 不匹配被拒绝。 */
data class WaitingProducerHandle(
    val token: AccountToken,
    val producerId: Long,
)

/**
 * 回调候选：AntForest.boot 只登记候选与不可变 UID；initHandler 完整成功后由门面激活。
 */
internal class CallbackCandidate(
    val uid: String,
    val callback: EnergyCollectCallback,
    val bindHandle: (WaitingProducerHandle) -> Unit,
)

/** 能量球登记草稿：由 [fansirsqi.xposed.sesame.task.antForest.EnergyWaitingManager.submit] 绑定到 session token 后成为 [WaitingTask]。 */
data class WaitingTaskDraft(
    val userId: String,
    val userName: String,
    val bubbleId: Long,
    val produceTime: Long,
    val fromTag: String,
    val shieldEndTime: Long = 0L,
    val bombEndTime: Long = 0L,
    val registeredTime: Long = System.currentTimeMillis(),
)

fun WaitingTaskDraft.bind(token: AccountToken): WaitingTask = WaitingTask(
    ownerUid = token.ownerUid,
    generation = token.generation,
    taskVersion = 1L, // 初始版本；每次状态更新递增，用于拒绝 RPC 期间的过期结果
    userId = userId,
    userName = userName,
    bubbleId = bubbleId,
    produceTime = produceTime,
    fromTag = fromTag,
    shieldEndTime = shieldEndTime,
    bombEndTime = bombEndTime,
    registeredTime = registeredTime,
)

/** 提交结果（可测试的纯枚举语义）。 */
enum class SubmitResult {
    Accepted,
    IgnoredDuplicate,
    RejectedStaleSession,
    RejectedProducer,
    RejectedClosing,
}

/** 账户关闭结果：最终 flush 失败必须显式暴露，不能报告为成功。 */
sealed interface CloseResult {
    data object Closed : CloseResult
    data class FlushFailed(val writeResult: WriteResult) : CloseResult
    data object StaleCommand : CloseResult
}

/**
 * 账户会话：承载单个账户 generation 的完整蹲点引擎（repository + scheduler + executor + cleanup）。
 *
 * V3 引擎下沉：进程级门面只持有当前 active session 与生命周期控制，
 * 所有账户业务组件（含 persistenceWriter 注入、seam components）都归属本 session；
 * 账户关闭时整体丢弃（rootJob.cancelAndJoin），无需手动置空回调。
 */
internal class WaitingAccountSession(
    val handle: WaitingProducerHandle,
    val callback: EnergyCollectCallback,
    parentScope: CoroutineScope,
    components: WaitingEngineComponents,
    private val persistenceWriter: WaitingPersistenceWriter,
) {
    // 注：restore 定义在门面 EnergyWaitingManager 中，需跨类访问 session.components
    //     故用 internal val 而非 private（同模块内可见，不暴露给外部）
    internal val components: WaitingEngineComponents = components

    val token: AccountToken = handle.token
    val rootJob = SupervisorJob(parentScope.coroutineContext[Job])
    val scope = CoroutineScope(
        parentScope.coroutineContext +
            rootJob +
            CoroutineName("EnergyWaiting-${token.generation}")
    )
    val stateMutex = Mutex()
    val closing = AtomicBoolean(false)
    val jobs = WaitingJobRegistry(scope)

    // 构造时绑定 generation（替代旧 setGeneration() 调用），
    // persistenceWriter 注入使 PersistenceDebouncer 走 writer 的 per-uid 串行 + stamp 校验。
    val repository = WaitingTaskRepository(scope, token.ownerUid, token.generation, persistenceWriter)

    // 循环依赖：executor 的 onScheduleRetry 引用 scheduler，scheduler 的 execution 引用 executor。
    // 二者用 lateinit var 解循环（与旧门面 init 块模式一致），lambda 延迟求值不会 NPE。
    lateinit var executor: WaitingExecutor
        private set
    lateinit var scheduler: WaitingScheduler
        private set
    lateinit var cleanupService: WaitingCleanupService
        private set

    init {
        executor = WaitingExecutor(
            repository = repository,
            stateMutex = stateMutex,
            callback = callback,           // session 私有，切换时整体丢弃，无需置空
            clock = components.clock,
            jitter = components.jitter,
            rpcGateway = components.rpcGateway,
            slotReserver = RpcSlotReserver(components.clock, components.jitter),  // V3 新增
            onScheduleRetry = { task, error, t -> scheduler.scheduleRetry(task, error, t) },
            onUserChanged = { userId -> scheduler.signalUserChanged(userId) },
        )
        scheduler = WaitingScheduler(
            scope = scope,
            jobs = jobs,
            repository = repository,
            timingCalculator = components.timingCalculator,
            retryPolicy = components.retryPolicy,
            clock = components.clock,
            delayController = components.delayController,
            rpcGateway = components.rpcGateway,
            execution = { userId -> executor.execute(userId) },
        )
        cleanupService = WaitingCleanupService(scope, repository, scheduler, stateMutex, components.rpcGateway, components.clock)
        cleanupService.startPeriodicCleanup()   // session 级启动：随 rootJob 取消自动停止
    }

    /** 校验当前 session 仍为活跃 session 且未在关闭中。在锁内/锁外均可调用。 */
    fun ensureCurrent(activeSession: AtomicReference<WaitingAccountSession?>) {
        check(!closing.get() && activeSession.get() === this) {
            "Session ${token.generation} is no longer current"
        }
    }

    /** 返回值检查版，不抛异常。用于 restore 等正常控制流场景（替代 ensureCurrent 的异常路径）。 */
    fun isCurrent(activeSession: AtomicReference<WaitingAccountSession?>): Boolean {
        return !closing.get() && activeSession.get() === this
    }
}
