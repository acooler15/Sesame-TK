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
import fansirsqi.xposed.sesame.ui.widget.StringDialog

class StringModelField(code: String?, name: String?, value: String?) : ModelField<String>(code, name, castValue(value)) {

    override val type: String
        get() = "STRING"

    override val configValue: String
        get() = castValue(value)

    override fun setConfigValue(configValue: String?) {
        value = castValue(configValue)
    }

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
        btn.setOnClickListener { v -> StringDialog.showEditDialog(v.context, (v as Button).text, this) }
        return btn
    }
}

// 保持与 Java 版本一致：允许 null 以非空声明类型返回（泛型擦除转换，无运行时检查）
@Suppress("UNCHECKED_CAST")
private fun <V> castValue(value: Any?): V = value as V
