package fansirsqi.xposed.sesame.model.modelFieldExt

import com.fasterxml.jackson.annotation.JsonIgnore
import fansirsqi.xposed.sesame.model.ModelField
import fansirsqi.xposed.sesame.model.ModelFieldViewData

open class TextModelField(code: String?, name: String?, value: String?) : ModelField<String?>(code, name, value) {

    override val type: String
        get() = "TEXT"

    override val configValue: String
        get() = castValue(value)

    override fun setConfigValue(configValue: String?) {
        value = configValue
    }

    @get:JsonIgnore
    override val viewData: ModelFieldViewData
        get() = super.viewData.copy(clickAction = ModelFieldViewData.ClickAction.READ)

    class UrlTextModelField(code: String?, name: String?, value: String?) : ReadOnlyTextModelField(code, name, value) {

        override val type: String
            get() = "URL_TEXT"

        @get:JsonIgnore
        override val viewData: ModelFieldViewData
            get() = super.viewData.copy(clickAction = ModelFieldViewData.ClickAction.URL)
    }

    open class ReadOnlyTextModelField(code: String?, name: String?, value: String?) : TextModelField(code, name, value) {

        override val type: String
            get() = "READ_TEXT"

        override var value: String?
            get() = null
            set(value) {
                super.value = value
            }

        override fun setConfigValue(configValue: String?) {
        }
    }
}

// 保持与 Java 版本一致：允许 null 以非空声明类型返回（泛型擦除转换，无运行时检查）
@Suppress("UNCHECKED_CAST")
private fun <V> castValue(value: Any?): V = value as V
