package fansirsqi.xposed.sesame.model.modelFieldExt

import com.fasterxml.jackson.annotation.JsonIgnore
import fansirsqi.xposed.sesame.model.ModelField
import fansirsqi.xposed.sesame.model.ModelFieldViewData

open class TextModelField(code: String?, name: String?, value: String?) : ModelField<String?>(code, name, value) {

    override val type: String
        get() = "TEXT"

    override val configValue: String
        // value 类型为 String?，且 ReadOnlyTextModelField 恒返回 null，统一兜底为空串
        get() = value ?: ""

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
