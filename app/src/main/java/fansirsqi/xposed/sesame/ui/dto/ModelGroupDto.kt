package fansirsqi.xposed.sesame.ui.dto

import java.io.Serializable

/**
 * 模型组数据传输对象。
 * 用于封装模型组的相关信息，包括组代码、名称和图标。
 */
data class ModelGroupDto(
    /**
     * 模型组代码。
     */
    var code: String? = null,
    /**
     * 模型组名称。
     */
    var name: String? = null,
    /**
     * 模型组图标。
     */
    var icon: String? = null,
) : Serializable
