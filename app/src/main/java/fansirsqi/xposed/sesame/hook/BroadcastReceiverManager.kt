package fansirsqi.xposed.sesame.hook

import fansirsqi.xposed.sesame.hook.schedule.TaskScheduler
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.content.ContextCompat
import fansirsqi.xposed.sesame.core.log.Log.record
import fansirsqi.xposed.sesame.core.log.Log.printStackTrace
import fansirsqi.xposed.sesame.core.threads.GlobalThreadPools.execute
import fansirsqi.xposed.sesame.hook.ApplicationHook.Companion.appContext
import fansirsqi.xposed.sesame.hook.ApplicationHook.Companion.classLoader
import fansirsqi.xposed.sesame.hook.ApplicationHook.Companion.finalProcessName
import fansirsqi.xposed.sesame.hook.rpc.debug.DebugRpc
import fansirsqi.xposed.sesame.model.Model
import fansirsqi.xposed.sesame.task.customTasks.CustomTask
import fansirsqi.xposed.sesame.task.customTasks.ManualTask
import fansirsqi.xposed.sesame.task.customTasks.ManualTaskModel

object BroadcastReceiverManager {
    private const val TAG = ApplicationHook.TAG

    private object BroadcastActions {
        const val RESTART: String = "com.eg.android.AlipayGphone.sesame.restart"
        const val RE_LOGIN: String = "com.eg.android.AlipayGphone.sesame.reLogin"
        const val STATUS: String = "com.eg.android.AlipayGphone.sesame.status"
        const val RPC_TEST: String = "com.eg.android.AlipayGphone.sesame.rpctest"
        const val MANUAL_TASK: String = "com.eg.android.AlipayGphone.sesame.manual_task"
    }

    // 广播接收器实例，用于注销
    private var mBroadcastReceiver: AlipayBroadcastReceiver? = null

    internal class AlipayBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val action = intent.action ?: return

            if (finalProcessName != null && finalProcessName!!.endsWith(":widgetProvider")) {
                return  // 忽略小组件进程
            }

            when (action) {
                BroadcastActions.RESTART -> execute(Runnable {
                    val targetUserId = intent.getStringExtra("userId")
                    val currentUserId = HookUtil.getUserId(classLoader!!)
                    if (targetUserId != null && targetUserId != currentUserId) {
                        record(TAG, "忽略非当前用户的重启广播: target=$targetUserId, current=$currentUserId")
                        return@Runnable
                    }
                    ApplicationHook.initHandler()
                })

                BroadcastActions.RE_LOGIN -> TaskScheduler.reOpenApp()
                BroadcastActions.RPC_TEST -> handleRpcTest(intent)
                BroadcastActions.MANUAL_TASK -> {
                    record(TAG, "🚀 收到手动庄园任务指令")
                    execute {
                        val taskName = intent.getStringExtra("task")
                        if (taskName != null) {
                            val normalizedTaskName = taskName.replace("+", "_")
                            try {
                                val task = CustomTask.valueOf(normalizedTaskName)
                                val extraParams = HashMap<String, Any>()
                                when (task) {
                                    CustomTask.FOREST_WHACK_MOLE -> {
                                        extraParams["whackMoleMode"] = intent.getIntExtra("whackMoleMode", 1)
                                        extraParams["whackMoleGames"] = intent.getIntExtra("whackMoleGames", 5)
                                    }

                                    CustomTask.FOREST_ENERGY_RAIN -> {
                                        extraParams["exchangeEnergyRainCard"] = intent.getBooleanExtra("exchangeEnergyRainCard", false)
                                    }

                                    CustomTask.FARM_SPECIAL_FOOD -> {
                                        extraParams["specialFoodCount"] = intent.getIntExtra("specialFoodCount", 0)
                                    }

                                    CustomTask.FARM_USE_TOOL -> {
                                        extraParams["toolType"] = intent.getStringExtra("toolType") ?: ""
                                        extraParams["toolCount"] = intent.getIntExtra("toolCount", 1)
                                    }

                                    else -> {
                                        record(TAG, "❌ 无效的任务指令: $taskName")
                                    }
                                }
                                ManualTask.runSingle(task, extraParams)
                            } catch (e: Exception) {
                                record(TAG, "❌ 无效的任务指令: $taskName -> ${e.message}")
                            }
                        } else {
                            for (model in Model.modelArray) {
                                if (model is ManualTaskModel) {
                                    model.startTask(true, 1)
                                    break
                                }
                            }
                        }
                    }
                }
            }
        }

        private fun handleRpcTest(intent: Intent) {
            execute({
                record(TAG, "RPC测试: $intent")
                try {
                    DebugRpc.start(
                        intent.getStringExtra("method"),
                        intent.getStringExtra("data"),
                        intent.getStringExtra("type")
                    )
                } catch (_: Throwable) { /* ignore */
                }
            })
        }
    }

    fun registerBroadcastReceiver(context: Context) {
        if (mBroadcastReceiver != null) return  // 防止重复注册

        try {
            mBroadcastReceiver = AlipayBroadcastReceiver()
            val filter = IntentFilter()
            filter.addAction(BroadcastActions.RESTART)
            filter.addAction(BroadcastActions.RE_LOGIN)
            filter.addAction(BroadcastActions.STATUS)
            filter.addAction(BroadcastActions.RPC_TEST)
            filter.addAction(BroadcastActions.MANUAL_TASK)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(mBroadcastReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                ContextCompat.registerReceiver(
                    context,
                    mBroadcastReceiver,
                    filter,
                    ContextCompat.RECEIVER_NOT_EXPORTED
                )
            }
            record(TAG, "BroadcastReceiver registered")
        } catch (th: Throwable) {
            mBroadcastReceiver = null
            printStackTrace(TAG, "Register Receiver failed", th)
        }
    }

    fun unregisterBroadcastReceiver(context: Context?) {
        if (mBroadcastReceiver == null || context == null) return
        try {
            context.unregisterReceiver(mBroadcastReceiver)
            record(TAG, "BroadcastReceiver unregistered")
        } catch (_: Throwable) {
            // ignore: receiver not registered
        } finally {
            mBroadcastReceiver = null
        }
    }

    fun sendBroadcast(action: String?) {
        if (appContext != null) appContext!!.sendBroadcast(Intent(action))
    }

    fun sendBroadcastShell(api: String?, message: String?) {
        if (appContext == null) return
        val intent = Intent("fansirsqi.xposed.sesame.SHELL")
        intent.putExtra(api, message)
        appContext!!.sendBroadcast(intent, null)
    }

    fun reLoginByBroadcast() {
        sendBroadcast(BroadcastActions.RE_LOGIN)
    }

    fun restartByBroadcast() {
        sendBroadcast(BroadcastActions.RESTART)
    }
}
