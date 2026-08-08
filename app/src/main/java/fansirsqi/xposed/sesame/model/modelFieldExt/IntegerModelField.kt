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
import fansirsqi.xposed.sesame.core.log.Log

/**
 * Integer 类型字段类，继承自 ModelField<Integer>
 * 该类用于表示具有最小值和最大值限制的整数字段。
 */
open class IntegerModelField : ModelField<Int> {
    /** 最小值限制 */
    val minLimit: Int?

    /** 最大值限制 */
    val maxLimit: Int?

    /**
     * 构造函数：创建一个没有最小值和最大值限制的 Integer 类型字段
     *
     * @param code 字段代码
     * @param name 字段名称
     * @param value 字段初始值
     */
    constructor(code: String?, name: String?, value: Int) : super(code, name, value) {
        this.minLimit = null // 无最小值限制
        this.maxLimit = null // 无最大值限制
    }

    /**
     * 构造函数：创建一个具有最小值和最大值限制的 Integer 类型字段
     *
     * @param code 字段代码
     * @param name 字段名称
     * @param value 字段初始值
     * @param minLimit 最小值限制
     * @param maxLimit 最大值限制
     */
    constructor(code: String?, name: String?, value: Int, minLimit: Int?, maxLimit: Int?) : super(code, name, value) {
        this.minLimit = minLimit // 设置最小值限制
        this.maxLimit = maxLimit // 设置最大值限制
    }

    /**
     * 获取字段类型
     *
     * @return 返回字段类型的字符串表示 "INTEGER"
     */
    override val type: String
        get() = "INTEGER"

    /**
     * 获取字段的配置值（将当前的值转换为字符串）
     *
     * @return 返回字段的字符串形式的配置值
     */
    override val configValue: String
        get() = value.toString() // 返回字段值的字符串表示

    /**
     * 设置字段的配置值（根据配置值设置新的值，并且在有最小/最大值限制的情况下进行限制）
     *
     * @param configValue 字段的配置值
     */
    override fun setConfigValue(configValue: String?) {
        var newValue: Int?
        // 如果配置值为空，使用默认值
        newValue = if (configValue == null || configValue.trim().isEmpty()) {
            defaultValue
        } else {
            try {
                // 尝试将配置值转换为整数
                configValue.toInt()
            } catch (e: Exception) {
                Log.printStackTrace(e) // 异常处理，打印栈追踪
                defaultValue // 如果转换失败，使用默认值
            }
        }
        // 根据最小值限制调整新值
        if (minLimit != null) {
            newValue = Math.max(minLimit, newValue!!)
        }
        // 根据最大值限制调整新值
        if (maxLimit != null) {
            newValue = Math.min(maxLimit, newValue!!)
        }
        // 设置字段值
        value = castValue(newValue)
    }

    /**
     * 获取视图（返回一个 Button，点击后弹出编辑框）
     *
     * @param context 上下文
     * @return 按钮视图
     */
    override fun getView(context: Context): View {
        val btn = Button(context)
        // 设置按钮的文本为字段名称
        btn.text = name
        // 设置按钮的布局参数
        btn.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        // 设置按钮的文本颜色
        btn.setTextColor(ContextCompat.getColor(context, R.color.selection_color))
        // 设置按钮的背景
        btn.background = ContextCompat.getDrawable(context, R.drawable.dialog_list_button)
        // 设置按钮的文本对齐方式
        btn.gravity = Gravity.START or Gravity.CENTER_VERTICAL
        // 设置按钮的最小高度
        btn.minHeight = 150
        // 设置按钮的最大高度
        btn.maxHeight = 180
        // 设置按钮的左右内边距
        btn.setPaddingRelative(40, 0, 40, 0)
        // 设置按钮的文本不全大写
        btn.isAllCaps = false
        // 设置点击事件，弹出编辑对话框
        btn.setOnClickListener { v -> StringDialog.showEditDialog(v.context, (v as Button).text, this) }
        return btn
    }

    /**
     * MultiplyIntegerModelField 类，继承自 IntegerModelField，处理带乘数的整数类型字段
     * 该类在设置值时会乘以指定的倍数。
     */
    class MultiplyIntegerModelField(
        code: String?,
        name: String?,
        value: Int,
        minLimit: Int?,
        maxLimit: Int?,
        /** 乘数，用于计算最终值；公开以便序列化时写出 multiple 键（与旧版 Lombok @Getter 行为一致） */
        val multiple: Int?
    ) : IntegerModelField(code, name, value * multiple!!, minLimit, maxLimit) {

        /**
         * 获取字段类型
         *
         * @return 返回字段类型的字符串表示 "MULTIPLY_INTEGER"
         */
        override val type: String
            get() = "MULTIPLY_INTEGER"

        /**
         * 设置字段的配置值（乘数影响最终值）
         *
         * @param configValue 字段的配置值
         */
        override fun setConfigValue(configValue: String?) {
            if (configValue == null || configValue.trim().isEmpty()) {
                reset() // 如果配置值为空，则重置字段
                return
            }
            super.setConfigValue(configValue) // 调用父类的 setConfigValue 方法
            try {
                // 根据乘数调整值
                value = value * multiple!! // 使用乘数调整字段值
                return
            } catch (e: Exception) {
                Log.printStackTrace(e) // 异常处理
            }
            reset() // 如果出现异常，重置字段
        }

        /**
         * 获取字段的配置值（返回值除以乘数）
         *
         * @return 配置值（字段值除以乘数）
         */
        override val configValue: String
            get() = (value / multiple!!).toString() // 使用乘数获取实际配置值
    }
}

// 保持与 Java 版本一致：允许将 null 写入非空泛型字段（泛型擦除转换，无运行时检查）
@Suppress("UNCHECKED_CAST")
private fun <V> castValue(value: Any?): V = value as V
