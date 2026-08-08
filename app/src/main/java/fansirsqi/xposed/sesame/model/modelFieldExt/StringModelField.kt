package fansirsqi.xposed.sesame.model.modelFieldExt

import com.fasterxml.jackson.annotation.JsonIgnore
import fansirsqi.xposed.sesame.model.ModelField
import fansirsqi.xposed.sesame.model.ModelFieldViewData

class StringModelField(code: String?, name: String?, value: String?) : ModelField<String>(code, name, castValue(value)) {

    override val type: String
        get() = "STRING"

    override val configValue: String
        get() = castValue(value)

    override fun setConfigValue(configValue: String?) {
        value = castValue(configValue)
    }

    @get:JsonIgnore
    override val viewData: ModelFieldViewData
        get() = super.viewData.copy(clickAction = ModelFieldViewData.ClickAction.EDIT)
}

// 保持与 Java 版本一致：允许 null 以非空声明类型返回（泛型擦除转换，无运行时检查）
@Suppress("UNCHECKED_CAST")
private fun <V> castValue(value: Any?): V = value as V
