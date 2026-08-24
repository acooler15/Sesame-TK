package fansirsqi.xposed.sesame.model

import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.ChoiceModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.IntegerModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.IntegerModelField.MultiplyIntegerModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.ListModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.ListModelField.ListJoinCommaToStringModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.StringModelField
import fansirsqi.xposed.sesame.core.util.ListUtil

/**
 * 全局配置对象
 *
 * 取代原 BaseModel.companion 中全局共享的静态配置字段，
 * 由 ApplicationHook 持有实例（ApplicationHook.config），
 * 供运行时统一读取，避免构造时快照导致的配置不生效问题。
 */
class SesameConfig {
    /**
     * 手动触发是否自动安排下次执行
     */
    @JvmField
    val manualTriggerAutoSchedule: BooleanModelField = BooleanModelField("manualTriggerAutoSchedule", "手动触发目标应用运行", false) //一般人不开这个

    /**
     * 执行间隔时间（分钟）
     */
    @JvmField
    val checkInterval: MultiplyIntegerModelField = MultiplyIntegerModelField("checkInterval", "执行间隔(分钟)", 50, 1, 12 * 60, 60000) //此处调整至30分钟执行一次，可能会比平常耗电一点。。

    /**
     * 任务执行轮数配置
     */
    @JvmField
    val taskExecutionRounds: IntegerModelField = IntegerModelField("taskExecutionRounds", "任务执行轮数", 1, 1, 99) //1轮就好，没必要2轮

    /**
     * 定时执行的时间点列表
     */
    @JvmField
    val execAtTimeList: ListJoinCommaToStringModelField = ListJoinCommaToStringModelField(
        "execAtTimeList", "定时执行(关闭:-1)", ListUtil.newArrayList(
            "0010", "0030", "0100", "0700", "0730", "1200", "1230", "1700", "1730", "2000", "2030", "2359"
        )
    )

    /**
     * 能量收集的时间范围
     */
    @JvmField
    val energyTime: ListJoinCommaToStringModelField = ListJoinCommaToStringModelField("energyTime", "只收能量时间(范围|关闭:-1)", ListUtil.newArrayList("0700-0730"))

    /**
     * 模块休眠时间范围
     */
    @JvmField
    val modelSleepTime: ListJoinCommaToStringModelField =
        ListJoinCommaToStringModelField("modelSleepTime", "模块休眠时间(范围|关闭:-1)", ListUtil.newArrayList("0200-0201"))

    /**
     * 超时是否重启
     */
    @JvmField
    val timeoutRestart: BooleanModelField = BooleanModelField("timeoutRestart", "超时重启", true)

    /**
     * 异常发生时的等待时间（分钟）
     */
    @JvmField
    val waitWhenException: MultiplyIntegerModelField = MultiplyIntegerModelField("waitWhenException", "异常等待时间(分钟)", 60, 0, 24 * 60, 60000)

    /**
     * 异常通知开关
     */
    @JvmField
    val errNotify: BooleanModelField = BooleanModelField("errNotify", "开启异常通知", false)

    @JvmField
    val setMaxErrorCount: IntegerModelField = IntegerModelField("setMaxErrorCount", "异常次数阈值", 8)

    /**
     * 是否启用新接口（最低支持版本 v10.3.96.8100）
     */
    @JvmField
    val newRpc: BooleanModelField = BooleanModelField("newRpc", "使用新接口(最低支持v10.3.96.8100)", true)

    /**
     * 是否开启抓包调试模式
     */
    @JvmField
    val debugMode: BooleanModelField = BooleanModelField("debugMode", "开启抓包(基于新接口)", false)

    /**
     * 是否申请目标应用的后台运行权限
     */
    @JvmField
    val batteryPerm: BooleanModelField = BooleanModelField("batteryPerm", "为目标应用申请后台运行权限", true)

    /**
     * 是否记录record日志
     */
    @JvmField
    val recordLog: BooleanModelField = BooleanModelField("recordLog", "全部 | 记录record日志", true)

    /**
     * 是否记录runtime日志
     */
    @JvmField
    val runtimeLog: BooleanModelField = BooleanModelField("runtimeLog", "全部 | 记录runtime日志", false)

    /**
     * 是否显示气泡提示
     */
    @JvmField
    val showToast: BooleanModelField = BooleanModelField("showToast", "气泡提示", true)

    @JvmField
    val toastPerfix: StringModelField = StringModelField("toastPerfix", "气泡前缀", "")

    /**
     * 气泡提示的纵向偏移量
     */
    @JvmField
    val toastOffsetY: IntegerModelField = IntegerModelField("toastOffsetY", "气泡纵向偏移", 99)

    /**
     * 只显示中文并设置时区
     */
    @JvmField
    val languageSimplifiedChinese: BooleanModelField = BooleanModelField("languageSimplifiedChinese", "只显示中文并设置时区", true)

    /**
     * 是否开启状态栏禁删
     */
    @JvmField
    val enableOnGoing: BooleanModelField = BooleanModelField("enableOnGoing", "开启状态栏禁删", false)

    @JvmField
    val sendHookData: BooleanModelField = BooleanModelField("sendHookData", "启用Hook数据转发", false)

    @JvmField
    val sendHookDataUrl: StringModelField = StringModelField("sendHookDataUrl", "Hook数据转发地址", "http://127.0.0.1:9527/hook")

    /**
     * 是否启用 WebView Hook（拦截 XRiver/WebView 中的网络请求 URL 和页面内容）
     */
    @JvmField
    val webViewDebug: BooleanModelField = BooleanModelField("webViewDebug", "启用 WebView Hook", false)

    /**
     * 是否启用内置解锁（关闭=不解锁，滑块只处理已解锁场景）
     */
    @JvmField
    val enableBuiltinUnlock: BooleanModelField = BooleanModelField("enableBuiltinUnlock", "启用内置解锁", false)

    /**
     * 锁屏类型：0=自动检测（默认），1=PIN，2=混合密码；仅 OEM 检测失败时手动覆盖
     */
    @JvmField
    val unlockType: ChoiceModelField = ChoiceModelField(
        "unlockType", "锁屏类型(默认自动)",
        UnlockType.AUTO, UnlockType.nickNames
    )

    /**
     * 锁屏密码（PIN/混合密码共用）。日志输出必须脱敏（H2）
     */
    @JvmField
    val unlockCredential: StringModelField = StringModelField("unlockCredential", "锁屏密码", "")

    /**
     * 解锁验证超时（秒）：单轮轮询总时长
     */
    @JvmField
    val unlockTimeoutSeconds: IntegerModelField = IntegerModelField("unlockTimeoutSeconds", "解锁超时(秒)", 15, 3, 60)

    /**
     * 解锁整轮重试次数
     */
    @JvmField
    val unlockRetryCount: IntegerModelField = IntegerModelField("unlockRetryCount", "解锁重试次数", 3, 1, 5)

    /**
     * 任务最大并发数，防止请求过于频繁触发风控。
     */
    @JvmField
    val taskMaxConcurrency: IntegerModelField = IntegerModelField("taskMaxConcurrency", "任务最大并发数", 3)

    /**
     * 任务默认超时时间（毫秒）。
     */
    @JvmField
    val taskDefaultTimeout: IntegerModelField = IntegerModelField("taskDefaultTimeout", "任务默认超时(毫秒)", 10 * 60 * 1000)

    /**
     * 超时白名单：在此名单中的任务"启动即视为完成"，不受超时限制。
     *
     * 适用场景：某些任务（如蚂蚁森林、庄园、运动）是长期运行的守护型任务，
     * 不应被超时机制中断。加入白名单后，任务启动后立即标记为完成，
     * 实际执行由任务内部的自调度逻辑控制。
     */
    @JvmField
    val taskTimeoutWhitelist: ListModelField = ListModelField(
        "taskTimeoutWhitelist", "任务超时白名单", listOf("森林", "庄园")
    )
}

/** 锁屏类型选择项（配合 unlockType 字段使用） */
interface UnlockType {
    companion object {
        const val AUTO: Int = 0
        const val PIN: Int = 1
        const val PASSWORD: Int = 2
        val nickNames: Array<String?> = arrayOf("🤖自动检测", "🔢数字密码", "🔤混合密码")
    }
}
