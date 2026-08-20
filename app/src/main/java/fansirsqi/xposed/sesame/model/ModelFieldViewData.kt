package fansirsqi.xposed.sesame.model

import java.io.Serializable

/**
 * 模型字段视图数据描述。
 * 纯数据类，不依赖 Android 视图，用于描述字段在配置页的展示与交互方式，
 * 为 Compose 配置页迁移提供数据基础。
 */
data class ModelFieldViewData(
    /** 字段类型，对应 ModelField.type */
    var type: String = "DEFAULT",
    /** 展示名称 */
    var name: String = "",
    /** 展示描述 */
    var desc: String? = null,
    /** 当前配置值展示 */
    var configValue: String = "",
    /** 扩展键（如 CHOICE 的选择数组） */
    var expandKey: Any? = null,
    /** 扩展值（如 Select 系列的选择列表） */
    var expandValue: Any? = null,
    /** 点击交互动作 */
    var clickAction: ClickAction = ClickAction.TOAST,
    /** 是否有确认执行动作（EmptyModelField 有 clickRunner 时为 true） */
    var hasConfirmAction: Boolean = false,
    /** 最小值限制（IntegerModelField） */
    var minLimit: Int? = null,
    /** 最大值限制（IntegerModelField） */
    var maxLimit: Int? = null,
) : Serializable {

    /** 点击交互动作枚举 */
    enum class ClickAction {
        /** 提示无配置项 */
        TOAST,
        /** 切换布尔值 */
        SWITCH,
        /** 打开编辑对话框 */
        EDIT,
        /** 打开只读对话框 */
        READ,
        /** 打开选项对话框 */
        CHOICE,
        /** 打开列表对话框 */
        LIST,
        /** 打开链接 */
        URL,
        /** 确认后执行动作 */
        CONFIRM,
    }
}
