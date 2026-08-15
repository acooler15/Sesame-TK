package fansirsqi.xposed.sesame.model

import java.io.Serializable

/**
 * 模型配置类，用于保存每个模型的配置信息。
 * 包括模型的基本信息（如名称、组、字段等），并提供方法来访问和操作这些配置项。
 */
class ModelConfig() : Serializable {
    // 模型的唯一标识符，通常是模型类名
    var code: String? = null

    // 模型名称
    var name: String? = null

    // 模型所属的组
    var group: ModelGroup? = null

    // 模型图标（可选）
    var icon: String? = null

    // 模型的所有字段
    val fields = ModelFields()

    /**
     * 使用给定的模型实例来初始化模型配置
     *
     * @param model 模型实例
     */
    constructor(model: Model) : this() {
        // 设置模型的简单类名作为唯一标识符
        code = model.javaClass.simpleName
        // 设置模型的名称
        name = model.getName()
        // 设置模型所属组
        group = model.getGroup()
        // 设置模型的图标文件名称（图标位置app/src/main/assets/web/images/icon/model[/selected]，正常状态和选中状态）
        // 无图标定义时使用default.svg
        icon = model.getIcon()
        // 获取模型的启用字段，并将其加入到字段列表中
        val enableField = model.enableField
        fields[enableField.code] = enableField
        // 获取模型的其他字段，并将其加入到字段列表中
        val modelFields = model.getFields()
        if (modelFields != null) {
            for (entry in modelFields.entries) {
                val modelField = entry.value
                if (modelField != null) {
                    fields[modelField.code] = modelField
                }
            }
        }
    }

    /**
     * 判断模型是否包含指定字段
     *
     * @param fieldCode 字段代码
     * @return 如果模型包含该字段，则返回true，否则返回false
     */
    fun hasModelField(fieldCode: String?): Boolean {
        return fields.containsKey(fieldCode)
    }

    /**
     * 获取指定字段代码的模型字段
     *
     * @param fieldCode 字段代码
     * @return 返回指定字段代码的模型字段
     */
    fun getModelField(fieldCode: String?): ModelField<*>? {
        return fields[fieldCode]
    }

    /**
     * 获取指定字段代码的模型字段扩展类型
     *
     * @param fieldCode 字段代码
     * @param <T>       字段类型
     * @return 返回指定字段代码的模型字段扩展类型
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : ModelField<*>> getModelFieldExt(fieldCode: String?): T {
        return fields[fieldCode] as T
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ModelConfig) return false
        return code == other.code && name == other.name && group == other.group && icon == other.icon && fields == other.fields
    }

    override fun hashCode(): Int {
        var result = code?.hashCode() ?: 0
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (group?.hashCode() ?: 0)
        result = 31 * result + (icon?.hashCode() ?: 0)
        result = 31 * result + fields.hashCode()
        return result
    }

    override fun toString(): String {
        return "ModelConfig(code=$code, name=$name, group=$group, icon=$icon, fields=$fields)"
    }

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
