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
import fansirsqi.xposed.sesame.model.SelectModelFieldFunc
import fansirsqi.xposed.sesame.entity.MapperEntity
import fansirsqi.xposed.sesame.ui.widget.ListDialog

class SelectOneModelField : ModelField<String>, SelectModelFieldFunc {
    private var selectListFunc: SelectListFunc? = null
    private var expandList: List<out MapperEntity>? = null

    constructor(code: String?, name: String?, value: String?, expandValue: List<out MapperEntity>?) : super(code, name, castValue(value)) {
        this.expandList = expandValue
    }

    constructor(code: String?, name: String?, value: String?, selectListFunc: SelectListFunc?) : super(code, name, castValue(value)) {
        this.selectListFunc = selectListFunc
    }

    override val type: String
        get() = "SELECT_ONE"

    override val expandValue: List<out MapperEntity>?
        get() = castList(selectListFunc?.getList() ?: expandList)

    override fun getView(context: Context): View {
        val btn = Button(context)
        btn.text = name
        btn.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        btn.setTextColor(ContextCompat.getColor(context, R.color.selection_color))
        btn.background = ContextCompat.getDrawable(context, R.drawable.dialog_list_button)
        btn.gravity = Gravity.START or Gravity.CENTER_VERTICAL
        btn.minHeight = 150
        btn.setPaddingRelative(40, 0, 40, 0)
        btn.isAllCaps = false
        btn.setOnClickListener { v -> ListDialog.show(v.context, (v as Button).text, this, ListDialog.ListType.RADIO) }
        return btn
    }

    override fun clear() {
        value = defaultValue
    }

    override fun get(id: String?): Int? {
        return 0
    }

    override fun add(id: String?, count: Int?) {
        value = castValue(id)
    }

    override fun remove(id: String?) {
        if (value == id) {
            value = defaultValue
        }
    }

    override fun contains(id: String?): Boolean {
        return value == id
    }

    fun interface SelectListFunc {
        fun getList(): List<*>
    }
}

// 保持与 Java 版本一致：允许 null 以非空声明类型写入/返回（泛型擦除转换，无运行时检查）
@Suppress("UNCHECKED_CAST")
private fun <V> castValue(value: Any?): V = value as V

// 保持与 Java 版本一致：List<?> 到 List<? extends MapperEntity> 的泛型擦除转换，无运行时检查
@Suppress("UNCHECKED_CAST")
private fun castList(value: Any?): List<out MapperEntity>? = value as List<out MapperEntity>?
