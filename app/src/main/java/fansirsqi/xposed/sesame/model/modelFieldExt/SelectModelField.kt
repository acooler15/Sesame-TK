package fansirsqi.xposed.sesame.model.modelFieldExt

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import org.json.JSONException
import fansirsqi.xposed.sesame.R
import fansirsqi.xposed.sesame.model.ModelField
import fansirsqi.xposed.sesame.model.SelectModelFieldFunc
import fansirsqi.xposed.sesame.entity.MapperEntity
import fansirsqi.xposed.sesame.ui.widget.ListDialog

/**
 * 数据结构说明
 * Set<String> 表示已选择的数据
 * List<? extends IdAndName> 需要选择的数据
 */
class SelectModelField : ModelField<MutableSet<String?>>, SelectModelFieldFunc {
    private var selectListFunc: SelectListFunc? = null
    private var expandList: List<out MapperEntity>? = null

    constructor(code: String?, name: String?, value: MutableSet<String?>, expandValue: List<out MapperEntity>?) : super(code, name, value) {
        this.expandList = expandValue
    }

    constructor(code: String?, name: String?, value: MutableSet<String?>, selectListFunc: SelectListFunc?) : super(code, name, value) {
        this.selectListFunc = selectListFunc
    }

    constructor(code: String?, name: String?, value: MutableSet<String?>, expandValue: List<out MapperEntity>?, desc: String?) : super(code, name, value, desc) {
        this.expandList = expandValue
    }

    constructor(code: String?, name: String?, value: MutableSet<String?>, selectListFunc: SelectListFunc?, desc: String?) : super(code, name, value, desc) {
        this.selectListFunc = selectListFunc
    }

    override val type: String
        get() = "SELECT"

    override val expandValue: List<out MapperEntity>?
        @Throws(JSONException::class)
        get() = castList(selectListFunc?.getList() ?: expandList)

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
        btn.setOnClickListener { v ->
            try {
                ListDialog.show(v.context, (v as Button).text, this)
            } catch (e: JSONException) {
                throw RuntimeException(e)
            }
        }
        return btn
    }

    override fun clear() {
        value.clear()
    }

    override fun get(id: String?): Int? {
        return 0
    }

    override fun add(id: String?, count: Int?) {
        value.add(id)
    }

    override fun remove(id: String?) {
        value.remove(id)
    }

    override fun contains(id: String?): Boolean {
        return value.contains(id)
    }

    fun interface SelectListFunc {
        @Throws(JSONException::class)
        fun getList(): List<*>
    }
}

// 保持与 Java 版本一致：List<?> 到 List<? extends MapperEntity> 的泛型擦除转换，无运行时检查
@Suppress("UNCHECKED_CAST")
private fun castList(value: Any?): List<out MapperEntity>? = value as List<out MapperEntity>?
