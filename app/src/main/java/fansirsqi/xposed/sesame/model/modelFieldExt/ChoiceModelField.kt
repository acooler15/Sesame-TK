package fansirsqi.xposed.sesame.model.modelFieldExt

import com.fasterxml.jackson.annotation.JsonIgnore
import fansirsqi.xposed.sesame.model.ModelField
import fansirsqi.xposed.sesame.model.ModelFieldViewData

class ChoiceModelField : ModelField<Int> {
    private var choiceArray: Array<out String?>? = null

    constructor(code: String?, name: String?, value: Int) : super(code, name, value)

    constructor(code: String?, name: String?, value: Int, choiceArray: Array<out String?>?) : super(code, name, value) {
        this.choiceArray = choiceArray
    }

    constructor(code: String?, name: String?, value: Int, desc: String?) : super(code, name, value, desc)

    constructor(code: String?, name: String?, value: Int, choiceArray: Array<out String?>?, desc: String?) : super(code, name, value, desc) {
        this.choiceArray = choiceArray
    }

    override val type: String
        get() = "CHOICE"

    override val expandKey: Array<out String?>?
        get() = choiceArray

    @get:JsonIgnore
    override val viewData: ModelFieldViewData
        get() = super.viewData.copy(clickAction = ModelFieldViewData.ClickAction.CHOICE)
}
