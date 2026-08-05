package fansirsqi.xposed.sesame.ui.dto

import fansirsqi.xposed.sesame.model.ModelField
import org.json.JSONException
import java.io.Serializable

/**
 * 模型字段信息数据传输对象。
 * 用于封装模型字段的详细信息，包括字段代码、名称、类型、扩展键、扩展值和配置值。
 */
data class ModelFieldInfoDto(
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
     * 扩展值，用于存储额外的信息。
     */
    var expandValue: Any? = null,
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
         * 将ModelField对象转换为ModelFieldInfoDto对象。
         * @param modelField ModelField对象
         * @return ModelFieldInfoDto对象
         */
        @JvmStatic
        @Throws(JSONException::class)
        fun toInfoDto(modelField: ModelField<*>): ModelFieldInfoDto {
            return ModelFieldInfoDto(
                code = modelField.code,
                name = modelField.name,
                type = modelField.type,
                expandKey = modelField.expandKey,
                expandValue = modelField.expandValue,
                configValue = modelField.configValue,
                desc = modelField.desc,
            )
        }
    }
}
