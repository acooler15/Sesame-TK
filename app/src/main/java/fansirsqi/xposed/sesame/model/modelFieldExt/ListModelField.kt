package fansirsqi.xposed.sesame.model.modelFieldExt

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.type.TypeReference
import fansirsqi.xposed.sesame.model.ModelField
import fansirsqi.xposed.sesame.model.ModelFieldViewData

/**
 * 表示一个存储字符串列表的字段模型，用于管理和展示列表数据。
 * 提供基本的获取类型、配置值以及视图展示的方法。
 */
open class ListModelField(code: String?, name: String?, value: List<String>) : ModelField<@JvmSuppressWildcards List<String>>(code, name, value) {

    companion object {
        // JSON 类型引用，用于序列化和反序列化 List<String>
        private val typeReference = object : TypeReference<List<String>>() {}
    }

    /**
     * 获取字段的类型。
     *
     * @return 返回字段类型 "LIST"
     */
    override val type: String
        get() = "LIST"

    @get:JsonIgnore
    override val viewData: ModelFieldViewData
        get() = super.viewData.copy(clickAction = ModelFieldViewData.ClickAction.EDIT)

    /**
     * 一个子类，用于将字符串列表转换为逗号分隔的字符串，并实现相应的设置和获取功能。
     */
    class ListJoinCommaToStringModelField(code: String?, name: String?, value: List<String>) : ListModelField(code, name, value) {

        /**
         * 设置配置值，将逗号分隔的字符串转换为字符串列表。
         *
         * @param configValue 配置值，逗号分隔的字符串
         */
        override fun setConfigValue(configValue: String?) {
            if (configValue == null) {
                reset()
                return
            }
            // 根据逗号分隔符解析字符串，并过滤掉空字符串
            val list = ArrayList<String>()
            for (str in configValue.split(",")) {
                if (str.isNotEmpty()) {
                    list.add(str)
                }
            }
            value = castValue(list)
        }

        /**
         * 获取配置值，将字符串列表拼接为逗号分隔的字符串。
         *
         * @return 配置值，逗号分隔的字符串
         */
        override val configValue: String
            get() = value.joinToString(",")
    }
}

// 保持与 Java 版本一致：协变列表赋值（泛型擦除转换，无运行时检查）
@Suppress("UNCHECKED_CAST")
private fun <V> castValue(value: Any?): V = value as V
