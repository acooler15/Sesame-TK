package fansirsqi.xposed.sesame.model

import com.fasterxml.jackson.annotation.JsonIgnore
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
abstract class ModelField<T>(initValue: T) : Serializable {
    // 存储字段值的类型
    @JsonIgnore
    val valueType: Type?

    // 字段代码
    @JsonIgnore
    open var code: String = ""

    // 字段名称
    @JsonIgnore
    open var name: String = ""

    // 默认值（由各带参构造函数显式传入，杜绝隐式 null 默认值）
    @JsonIgnore
    var defaultValue: T = initValue

    @JsonIgnore
    open var desc: String? = null

    // 当前值（序列化经属性 getter 输出，与旧 Java 版 public volatile T value 行为一致）
    @Volatile
    open var value: T = initValue

    /**
     * 默认构造函数，初始化字段值类型并设置当前值
     */
    init {
        valueType = TypeUtil.getTypeArgument(this.javaClass.genericSuperclass, 0)
        setObjectValue(initValue) // 经类型转换链路设置当前值
    }

    /**
     * 构造函数，接受字段代码、名称和初始值
     *
     * @param code  字段代码
     * @param name  字段名称
     * @param value 字段初始值
     */
    constructor(code: String?, name: String?, value: T) : this(value) {
        this.code = code ?: ""
        this.name = name ?: ""
    }

    constructor(code: String?, name: String?, value: T, desc: String?) : this(value) {
        this.code = code ?: ""
        this.name = name ?: ""
        this.desc = desc
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
     * 获取字段的视图数据描述
     */
    @get:JsonIgnore
    open val viewData: ModelFieldViewData
        get() {
            // configValue 声明为非空 String，但子类实现（如 StringModelField）可能返回 null，统一兜底为空串
            val rawConfigValue: String? = configValue
            return ModelFieldViewData(
                type = type,
                name = name,
                desc = desc,
                configValue = rawConfigValue ?: "",
                expandKey = expandKey,
                expandValue = expandValue,
            )
        }
}
