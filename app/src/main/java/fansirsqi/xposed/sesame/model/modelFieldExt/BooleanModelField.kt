package fansirsqi.xposed.sesame.model.modelFieldExt

import com.fasterxml.jackson.annotation.JsonIgnore
import fansirsqi.xposed.sesame.model.ModelField
import fansirsqi.xposed.sesame.model.ModelFieldViewData

class BooleanModelField(code: String?, name: String?, value: Boolean) : ModelField<Boolean>(code, name, value) {

    override val type: String
        get() = "BOOLEAN"

    @get:JsonIgnore
    override val viewData: ModelFieldViewData
        get() = super.viewData.copy(clickAction = ModelFieldViewData.ClickAction.SWITCH)
}
