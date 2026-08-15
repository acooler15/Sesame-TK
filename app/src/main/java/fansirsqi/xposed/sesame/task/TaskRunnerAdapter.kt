package fansirsqi.xposed.sesame.task

import fansirsqi.xposed.sesame.hook.ApplicationHook
import fansirsqi.xposed.sesame.model.Model
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * TaskRunner适配器类
 * <p>
 * 为Java代码提供更友好的CoroutineTaskRunner调用方式
 * 适配了新的 suspend run 方法和 Job 管理机制
 */
class TaskRunnerAdapter {

    private val coroutineTaskRunner: CoroutineTaskRunner

    // 用于追踪当前运行的任务 Job，以便执行 stop()
    private var currentJob: Job? = null

    /**
     * 构造函数 - 使用所有已注册的模型
     */
    constructor() {
        coroutineTaskRunner = CoroutineTaskRunner(Model.modelArray.filterNotNull())
    }

    /**
     * 构造函数 - 使用指定的模型列表
     */
    constructor(models: List<Model>) {
        coroutineTaskRunner = CoroutineTaskRunner(models)
    }

    /**
     * 执行任务 - 简化版本
     */
    fun run() {
        // Mode参数现在已废弃，新版Runner内部自动处理并发
        run(true, null)
    }

    /**
     * 执行任务 - 兼容旧接口
     * @param mode 该参数已被忽略，新版Runner使用内部并发控制
     */
    fun run(isFirst: Boolean, mode: ModelTask.TaskExecutionMode?) {
        run(isFirst, mode, ApplicationHook.config.taskExecutionRounds.value)
    }

    /**
     * 执行任务 - 包含轮数参数（主方法）
     */
    fun run(isFirst: Boolean, mode: ModelTask.TaskExecutionMode?, rounds: Int) {
        // 如果有旧任务在运行，先取消
        stop()

        currentJob = ApplicationHook.applicationScope.launch(Dispatchers.Default) {
            coroutineTaskRunner.run(isFirst, rounds)
        }
    }

    /**
     * 停止任务执行器
     */
    fun stop() {
        val job = currentJob
        if (job != null && job.isActive) {
            job.cancel(null) // 取消协程
            currentJob = null
        }
    }

    companion object {
        /**
         * 静态方法：快速执行所有任务
         */
        @JvmStatic
        fun runAllTasks() {
            runAllTasks(null)
        }

        /**
         * 静态方法：使用指定模式执行所有任务
         */
        @JvmStatic
        fun runAllTasks(mode: ModelTask.TaskExecutionMode?) {
            TaskRunnerAdapter().run(true, mode)
        }
    }
}
