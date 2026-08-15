package fansirsqi.xposed.sesame.model

import fansirsqi.xposed.sesame.BuildConfig
import fansirsqi.xposed.sesame.hook.ApplicationHook
import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.util.maps.BeachMap
import fansirsqi.xposed.sesame.util.maps.IdMapManager

/**
 * 基础配置模块
 */
class BaseModel : Model() {
    override fun getName(): String {
        return "基础"
    }

    override fun getGroup(): ModelGroup {
        return ModelGroup.BASE
    }

    override fun getIcon(): String {
        return "BaseModel.png"
    }

    override fun getEnableFieldName(): String {
        return "启用模块"
    }

    override fun getFields(): ModelFields {
        val modelFields = ModelFields()
        val config = ApplicationHook.config
        modelFields.addField(config.stayAwake) //是否保持唤醒状态
        modelFields.addField(config.manualTriggerAutoSchedule) //手动触发是否自动安排下次执行
        modelFields.addField(config.checkInterval) //执行间隔时间
        modelFields.addField(config.taskExecutionRounds) //轮数
        modelFields.addField(config.modelSleepTime) //模块休眠时间范围
        modelFields.addField(config.execAtTimeList) //定时执行的时间点列表
        modelFields.addField(config.wakenAtTimeList) //定时唤醒的时间点列表
        modelFields.addField(config.energyTime) //能量收集的时间范围
        modelFields.addField(config.timedTaskModel) //定时任务模式选择
        modelFields.addField(config.timeoutRestart) //超时是否重启
        modelFields.addField(config.waitWhenException) //异常发生时的等待时间
        modelFields.addField(config.errNotify) //异常通知开关
        modelFields.addField(config.setMaxErrorCount) //异常次数阈值
        modelFields.addField(config.newRpc) //是否启用新接口

        if (BuildConfig.DEBUG) {
            modelFields.addField(config.debugMode) //是否开启抓包调试模式
            modelFields.addField(config.sendHookData) //启用Hook数据转发
            modelFields.addField(config.sendHookDataUrl) //Hook数据转发地址
            modelFields.addField(config.webViewDebug) //是否启用WebView Hook
        }

        modelFields.addField(config.batteryPerm) //是否申请目标应用的后台运行权限
        modelFields.addField(config.recordLog) //是否记录record日志
        modelFields.addField(config.runtimeLog) //是否记录runtime日志
        modelFields.addField(config.showToast) //是否显示气泡提示
        modelFields.addField(config.enableOnGoing) //是否开启状态栏禁删
        modelFields.addField(config.languageSimplifiedChinese) //是否只显示中文并设置时区
        modelFields.addField(config.toastOffsetY) //气泡提示的纵向偏移量
        modelFields.addField(config.toastPerfix)//气泡提示的前缀
        modelFields.addField(config.unlockShellCommand) //解锁 Shell 命令（空=关闭）
        modelFields.addField(config.unlockWaitSeconds) //解锁等待时间（秒）
        modelFields.addField(config.taskMaxConcurrency) //任务最大并发数
        modelFields.addField(config.taskDefaultTimeout) //任务默认超时(毫秒)
        modelFields.addField(config.taskTimeoutWhitelist) //任务超时白名单
        return modelFields
    }

    interface TimedTaskModel {
        companion object {
            const val SYSTEM: Int = 0
            const val PROGRAM: Int = 1
            val nickNames: Array<String?> = arrayOf<String?>("🤖系统计时", "📦程序计时")
        }
    }

    companion object {
        private const val TAG = "BaseModel"

        /**
         * 清理数据，在模块销毁时调用，清空 Reserve 和 Beach 数据。
         */
        fun destroyData() {
            try {
                Log.record(TAG, "🧹清理所有数据")
                IdMapManager.getInstance(BeachMap::class.java).clear()
                //            IdMapManager.getInstance(ReserveMap.class).clear();
//            IdMapManager.getInstance(CooperateMap.class).clear();
//            IdMapManager.getInstance(MemberBenefitsMap.class).clear();
//            IdMapManager.getInstance(ParadiseCoinBenefitIdMap.class).clear();
//            IdMapManager.getInstance(VitalityRewardsMap.class).clear();
                //其他也可以清理清理
            } catch (e: Exception) {
                Log.printStackTrace(e)
            }
        }
    }
}
