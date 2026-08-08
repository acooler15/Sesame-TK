package fansirsqi.xposed.sesame.model.modelFieldExt

import com.fasterxml.jackson.annotation.JsonIgnore
import fansirsqi.xposed.sesame.model.ModelField
import fansirsqi.xposed.sesame.model.ModelFieldViewData

class EmptyModelField : ModelField<Any> {
    internal val clickRunner: Runnable?

    constructor(code: String?, name: String?) : super(code, name, castNull()) {
        this.clickRunner = null
    }

    constructor(code: String?, name: String?, clickRunner: Runnable?) : super(code, name, castNull()) {
        this.clickRunner = clickRunner
    }

    override val type: String
        get() = "EMPTY"

    override fun setObjectValue(objectValue: Any?) {
    }

    @get:JsonIgnore
    override val viewData: ModelFieldViewData
        get() = super.viewData.copy(
            clickAction = if (clickRunner != null) {
                ModelFieldViewData.ClickAction.CONFIRM
            } else {
                ModelFieldViewData.ClickAction.TOAST
            },
            hasConfirmAction = clickRunner != null,
        )
}

@Suppress("UNCHECKED_CAST")
private fun <V> castNull(): V = null as V
