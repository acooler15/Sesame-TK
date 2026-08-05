package fansirsqi.xposed.sesame.model.modelFieldExt

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import fansirsqi.xposed.sesame.R
import fansirsqi.xposed.sesame.model.ModelField
import fansirsqi.xposed.sesame.ui.widget.ChoiceDialog

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

    override fun getView(context: Context): View {
        val btn = Button(context)
        btn.text = name
        btn.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        btn.setTextColor(ContextCompat.getColor(context, R.color.selection_color))
        btn.background = ContextCompat.getDrawable(context, R.drawable.dialog_list_button)
        btn.gravity = Gravity.START or Gravity.CENTER_VERTICAL
        btn.minHeight = 150
        btn.maxHeight = 180
        btn.setPaddingRelative(40, 0, 40, 0)
        btn.isAllCaps = false
        btn.setOnClickListener { v -> ChoiceDialog.show(v.context, (v as Button).text, this) }
        return btn
    }
}
