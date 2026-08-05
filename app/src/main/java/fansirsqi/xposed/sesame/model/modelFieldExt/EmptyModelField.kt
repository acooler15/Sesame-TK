package fansirsqi.xposed.sesame.model.modelFieldExt

import android.app.AlertDialog
import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.fasterxml.jackson.annotation.JsonIgnore
import fansirsqi.xposed.sesame.R
import fansirsqi.xposed.sesame.model.ModelField

class EmptyModelField : ModelField<Any> {
    private val clickRunner: Runnable?

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

    @JsonIgnore
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
        val runner = clickRunner
        if (runner != null) {
            btn.setOnClickListener {
                AlertDialog.Builder(context)
                    .setTitle("警告")
                    .setMessage("确认执行该操作？")
                    .setPositiveButton(R.string.ok) { _, _ -> runner.run() }
                    .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                    .create()
                    .show()
            }
        } else {
            btn.setOnClickListener { Toast.makeText(context, "无配置项", Toast.LENGTH_SHORT).show() }
        }
        return btn
    }
}

@Suppress("UNCHECKED_CAST")
private fun <V> castNull(): V = null as V
