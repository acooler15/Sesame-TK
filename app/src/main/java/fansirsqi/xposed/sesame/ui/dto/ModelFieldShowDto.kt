package fansirsqi.xposed.sesame.ui.dto

import fansirsqi.xposed.sesame.model.ModelField
import java.io.Serializable

/**
 * 模型字段展示数据传输对象。
 * 用于封装模型字段的展示信息，包括字段代码、名称、类型、扩展键和配置值。
 */
data class ModelFieldShowDto(
    /**
     * 字段代码。
     */
    var code: String? = null,
    /**
     * 字段名称。
     */
    var name: String? = null,
    /**
     * 字段类型。
     */
    var type: String? = null,
    /**
     * 扩展键，用于存储额外的信息。
     */
    var expandKey: Any? = null,
    /**
     * 配置值，用于存储字段的配置信息。
     */
    var configValue: String? = null,
    /**
     * 字段描述。
     */
    var desc: String? = null,
) : Serializable {
    companion object {
        /**
         * 将ModelField对象转换为ModelFieldShowDto对象。
         * 这是一个静态工厂方法，用于创建ModelFieldShowDto实例。
         *
         * @param modelField ModelField对象
         * @return ModelFieldShowDto对象
         */
        @JvmStatic
        fun toShowDto(modelField: ModelField<*>): ModelFieldShowDto {
            return ModelFieldShowDto(
                code = modelField.code,
                name = modelField.name,
                type = modelField.type,
                expandKey = modelField.expandKey,
                configValue = modelField.configValue,
                desc = modelField.desc,
            )
        }
    }
}
