package fansirsqi.xposed.sesame.model.modelFieldExt

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.fasterxml.jackson.core.type.TypeReference
import fansirsqi.xposed.sesame.R
import fansirsqi.xposed.sesame.model.ModelField
import fansirsqi.xposed.sesame.ui.widget.StringDialog

/**
 * 表示一个存储字符串列表的字段模型，用于管理和展示列表数据。
 * 提供基本的获取类型、配置值以及视图展示的方法。
 */
open class ListModelField(code: String?, name: String?, value: List<String>) : ModelField<@JvmSuppressWildcards List<String>>(code, name, value) {

    companion object {
        // JSON 类型引用，用于序列化和反序列化 List<String>
        private val typeReference = object : TypeReference<List<String>>() {}
    }

    /**
     * 获取字段的类型。
     *
     * @return 返回字段类型 "LIST"
     */
    override val type: String
        get() = "LIST"

    /**
     * 获取用于展示该字段的视图组件。
     *
     * @param context 上下文环境
     * @return 返回一个按钮视图，用于触发编辑功能
     */
    @SuppressLint("UseCompatLoadingForDrawables")
    override fun getView(context: Context): View {
        val btn = Button(context)
        btn.text = name
        btn.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        btn.setTextColor(ContextCompat.getColor(context, R.color.selection_color))
        // 根据API版本选择合适的方法获取Drawable资源
        val drawable: Drawable
        drawable = context.resources.getDrawable(R.drawable.dialog_list_button, context.theme)
        btn.background = drawable
        btn.gravity = Gravity.START or Gravity.CENTER_VERTICAL
        btn.minHeight = 150
        btn.maxHeight = 180
        btn.setPaddingRelative(40, 0, 40, 0)
        btn.isAllCaps = false
        // 设置按钮点击事件，打开编辑对话框
        btn.setOnClickListener { v -> StringDialog.showEditDialog(v.context, (v as Button).text, this) }
        return btn
    }

    /**
     * 一个子类，用于将字符串列表转换为逗号分隔的字符串，并实现相应的设置和获取功能。
     */
    class ListJoinCommaToStringModelField(code: String?, name: String?, value: List<String>) : ListModelField(code, name, value) {

        /**
         * 设置配置值，将逗号分隔的字符串转换为字符串列表。
         *
         * @param configValue 配置值，逗号分隔的字符串
         */
        override fun setConfigValue(configValue: String?) {
            if (configValue == null) {
                reset()
                return
            }
            // 根据逗号分隔符解析字符串，并过滤掉空字符串
            val list = ArrayList<String>()
            for (str in configValue.split(",")) {
                if (str.isNotEmpty()) {
                    list.add(str)
                }
            }
            value = castValue(list)
        }

        /**
         * 获取配置值，将字符串列表拼接为逗号分隔的字符串。
         *
         * @return 配置值，逗号分隔的字符串
         */
        override val configValue: String
            get() = value.joinToString(",")
    }
}

// 保持与 Java 版本一致：协变列表赋值（泛型擦除转换，无运行时检查）
@Suppress("UNCHECKED_CAST")
private fun <V> castValue(value: Any?): V = value as V
