package fansirsqi.xposed.sesame.model

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.fasterxml.jackson.annotation.JsonIgnore
import com.google.android.material.button.MaterialButton
import fansirsqi.xposed.sesame.R
import fansirsqi.xposed.sesame.core.json.JsonUtil
import fansirsqi.xposed.sesame.core.reflect.TypeUtil
import org.json.JSONException
import java.io.Serializable
import java.lang.reflect.Type
import java.util.Objects

/**
 * 序列化说明：本类仅参与序列化（由 Config.toSaveStr 写出），不参与 Jackson 反序列化
 * （抽象类无法构造，配置加载由 Config 解析 JSON 树完成）。
 * 与旧 Java 版（Lombok @Getter/@Setter 生成 getValue()/setValue()）保持一致：
 * Jackson 序列化 value 属性时优先走 getValue() getter，子类可通过覆写改变输出
 * （如 ReadOnlyTextModelField 返回 null，配合 NON_NULL 序列化为 {}）。
 */
abstract class ModelField<T> : Serializable {
    // 存储字段值的类型
    @JsonIgnore
    @JvmField
    val valueType: Type?

    // 字段代码
    @JsonIgnore
    open var code: String = ""

    // 字段名称
    @JsonIgnore
    open var name: String = ""

    // 默认值
    @JsonIgnore
    @JvmField
    var defaultValue: T = castNull()

    @JsonIgnore
    open var desc: String? = null

    // 当前值（公开字段，与旧 Java 版 public volatile T value 一致）
    @JvmField
    @Volatile
    var value: T = defaultValue

    /**
     * 默认构造函数，初始化字段值类型
     */
    init {
        valueType = TypeUtil.getTypeArgument(this.javaClass.genericSuperclass, 0)
    }

    @Suppress("UNCHECKED_CAST")
    private fun castNull(): T = null as T

    /**
     * 获取当前值（与旧版 Lombok @Getter 生成的 getter 等价，Jackson 序列化时经由该方法输出 value）
     *
     * @return 当前值
     */
    open fun getValue(): T = value

    /**
     * 设置当前值字段
     *
     * @param value 要设置的值
     */
    open fun setValue(value: T) {
        this.value = value
    }

    /**
     * 无参构造函数，仅初始化字段值类型
     */
    constructor()

    /**
     * 构造函数，接受初始值
     *
     * @param value 初始值
     */
    constructor(value: T) {
        defaultValue = value // 设置默认值
        setObjectValue(value) // 设置当前值
    }

    /**
     * 构造函数，接受字段代码、名称和初始值
     *
     * @param code  字段代码
     * @param name  字段名称
     * @param value 字段初始值
     */
    constructor(code: String?, name: String?, value: T) {
        this.code = code ?: ""
        this.name = name ?: ""
        defaultValue = value // 设置默认值
        setObjectValue(value) // 设置当前值
    }

    constructor(code: String?, name: String?, value: T, desc: String?) {
        this.code = code ?: ""
        this.name = name ?: ""
        defaultValue = value
        this.desc = desc
        setObjectValue(value)
    }

    /**
     * 设置当前值
     *
     * @param objectValue 要设置的值
     */
    open fun setObjectValue(objectValue: Any?) {
        var obj = objectValue
        if (obj == null) {
            reset() // 如果传入值为 null，则重置为默认值
            return
        }
        if (valueType === Int::class.javaObjectType && obj is Boolean) {
            obj = if (obj) 1 else 0
        }
        value = JsonUtil.parseObject<T>(obj, valueType!!) // 解析并设置当前值
    }

    /**
     * 获取字段类型
     *
     * @return 字段类型字符串
     */
    @get:JsonIgnore
    open val type: String
        get() = "DEFAULT" // 默认返回类型

    /**
     * 获取扩展键
     *
     * @return 扩展键
     */
    @get:JsonIgnore
    open val expandKey: Any?
        get() = null // 默认返回 null

    /**
     * 获取扩展值
     *
     * @return 扩展值
     */
    @get:JsonIgnore
    open val expandValue: Any?
        @Throws(JSONException::class)
        get() = null // 默认返回 null

    /**
     * 将当前值转换为配置值
     *
     * @param value 当前值
     * @return 配置值
     */
    open fun toConfigValue(value: T): Any? = value // 默认返回当前值

    /**
     * 从配置值转换为对象值
     *
     * @param value 配置值
     * @return 对象值
     */
    open fun fromConfigValue(value: String): Any = value // 默认返回配置值

    /**
     * 获取当前值的配置字符串表示
     *
     * @return 配置字符串
     */
    @get:JsonIgnore
    open val configValue: String
        get() = JsonUtil.formatJson(toConfigValue(value)) // 转换为 JSON 字符串

    /**
     * 设置配置值
     *
     * @param configValue 配置值字符串
     */
    @JsonIgnore
    open fun setConfigValue(configValue: String?) {
        if (configValue == null) {
            reset() // 如果配置值为 null，则重置为默认值
            return
        }
        val objectValue = fromConfigValue(configValue) // 从配置值转换为对象值
        // 如果对象值与配置值相等，则直接解析配置值
        value = if (objectValue == configValue) {
            JsonUtil.parseObject(configValue, valueType!!)
        } else {
            JsonUtil.parseObject(objectValue, valueType!!)
        }
    }

    /**
     * 重置当前值为默认值
     */
    open fun reset() {
        value = defaultValue // 设置当前值为默认值
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ModelField<*>) return false
        if (!other.canEqual(this)) return false
        return valueType == other.valueType && code == other.code && name == other.name &&
                defaultValue == other.defaultValue && desc == other.desc && value == other.value
    }

    open fun canEqual(other: Any?): Boolean = other is ModelField<*>

    override fun hashCode(): Int {
        val prime = 59
        var result = 1
        result = prime * result + Objects.hashCode(valueType)
        result = prime * result + Objects.hashCode(code)
        result = prime * result + Objects.hashCode(name)
        result = prime * result + Objects.hashCode(defaultValue)
        result = prime * result + Objects.hashCode(desc)
        result = prime * result + Objects.hashCode(value)
        return result
    }

    override fun toString(): String {
        return "ModelField(valueType=$valueType, code=$code, name=$name, defaultValue=$defaultValue, desc=$desc, value=$value)"
    }

    /**
     * 获取字段的视图
     *
     * @param context 上下文对象
     * @return 生成的视图
     */
    @JsonIgnore
    open fun getView(context: Context): View {
        val button = MaterialButton(context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle)
        button.text = name
        button.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        button.cornerRadius = 28 // M3 推荐圆角
        button.insetTop = 24 // 上下 padding
        button.insetBottom = 24
        button.setPaddingRelative(40, 0, 40, 0) // 左右 padding
        button.iconPadding = 16
        button.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START)
        button.setRippleColorResource(R.color.selection_color) // 可自定义 ripple
        button.setTextColor(ContextCompat.getColor(context, R.color.selection_color)) // 使用 M3 色彩
        button.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
        // 点击提示
        button.setOnClickListener { Toast.makeText(context, "无配置项", Toast.LENGTH_SHORT).show() }
        return button
    }
}
