package fansirsqi.xposed.sesame.ui.dto

import java.io.Serializable

/**
 * 模型数据传输对象。
 * 用于封装模型的代码、名称、组代码以及模型字段展示信息。
 */
data class ModelDto(
    /**
     * 模型代码。
     */
    var modelCode: String? = null,
    /**
     * 模型名称。
     */
    var modelName: String? = null,
    /**
     * 模型图标
     */
    var modelIcon: String? = null,
    /**
     * 组代码。
     */
    var groupCode: String? = null,
    /**
     * 模型字段展示信息列表。
     */
    var modelFields: List<ModelFieldShowDto>? = null,
) : Serializable
