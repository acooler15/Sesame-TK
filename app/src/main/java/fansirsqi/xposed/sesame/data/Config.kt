package fansirsqi.xposed.sesame.data

import com.fasterxml.jackson.databind.JsonNode
import fansirsqi.xposed.sesame.model.Model
import fansirsqi.xposed.sesame.model.ModelFields
import fansirsqi.xposed.sesame.task.TaskCommon
import fansirsqi.xposed.sesame.core.app.Files
import fansirsqi.xposed.sesame.core.json.JsonUtil
import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.core.util.StringUtil
import fansirsqi.xposed.sesame.util.maps.UserMap
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * 配置类，负责加载、保存、管理应用的配置数据。
 */
class Config private constructor() {

    @Volatile
    private var initialized = false

    // 存储模型字段的映射
    private val _modelFieldsMap: MutableMap<String, ModelFields> = ConcurrentHashMap()

    fun getModelFieldsMap(): Map<String, ModelFields> = _modelFieldsMap

    /**
     * 将配置 JSON 中保存的值应用到模型字段，并重建 modelFieldsMap。
     *
     * 注意：ModelField 是抽象类，Jackson 无法构造其实例，因此配置加载
     * 不能依赖 Jackson 数据绑定，而是通过解析 JSON 树手动应用值。
     *
     * @param modelFieldsNode 配置 JSON 中的 "modelFieldsMap" 节点，为 null 时全部恢复默认值
     */
    fun applyModelFieldsJson(modelFieldsNode: JsonNode?) {
        unload()
        _modelFieldsMap.clear()
        val modelConfigMap = Model.getModelConfigMap()
        // 遍历所有模型配置，应用已保存的字段值
        for (modelConfig in modelConfigMap.values) {
            val modelCode = modelConfig.code!!
            val newModelFields = ModelFields()
            val savedModelFields = modelFieldsNode?.get(modelCode)
            for (configModelField in modelConfig.fields.values) {
                if (savedModelFields != null) {
                    val savedField = savedModelFields.get(configModelField.code)
                    if (savedField != null && !savedField.isNull) {
                        // 标准格式为 {"value": ...}；对象节点无 value 键（如 "toastPerfix": {}）
                        // 表示值为 null（序列化时被 NON_NULL 省略），跳过即可；
                        // 非对象节点直接存值，作为兼容格式处理
                        val valueNode = if (savedField.isObject) {
                            savedField.get("value")
                        } else {
                            savedField
                        }
                        if (valueNode != null && !valueNode.isNull) {
                            try {
                                configModelField.setObjectValue(valueNode)
                            } catch (e: Exception) {
                                Log.printStackTrace(TAG, "应用配置值失败: $modelCode/${configModelField.code}", e)
                            }
                        }
                    }
                }
                newModelFields.addField(configModelField)
            }
            _modelFieldsMap[modelCode] = newModelFields
        }
    }

    /**
     * 检查是否存在指定的模型字段
     *
     * @param modelCode 模型代码
     * @return 是否存在该模型字段
     */
    fun hasModelFields(modelCode: String): Boolean = _modelFieldsMap.containsKey(modelCode)

    /**
     * 检查指定模型字段是否存在
     *
     * @param modelCode 模型代码
     * @param fieldCode 字段代码
     * @return 是否存在该字段
     */
    fun hasModelField(modelCode: String, fieldCode: String): Boolean {
        val modelFields = _modelFieldsMap[modelCode] ?: return false
        return modelFields.containsKey(fieldCode)
    }

    companion object {
        private const val TAG = "Config"

        val INSTANCE: Config = Config()

        /**
         * 判断配置文件是否已修改
         *
         * @param userId 用户 ID
         * @return 是否已修改
         */
        fun isModify(userId: String?): Boolean {
            var json: String? = null
            val configV2File: File = if (StringUtil.isEmpty(userId)) {
                Files.getDefaultConfigV2File()
            } else {
                Files.getConfigV2File(userId!!)
            }
            if (configV2File.exists()) {
                json = Files.readFromFile(configV2File)
            }
            if (json != null) {
                val formatted: String = JsonUtil.formatJson(INSTANCE)
                return formatted != json
            }
            return true
        }

        /**
         * 保存配置文件
         *
         * @param userId 用户 ID
         * @param force  是否强制保存
         * @return 保存是否成功
         */
        @Synchronized
        fun save(userId: String?, force: Boolean): Boolean {
            var userId = userId
            if (!force && !isModify(userId)) {
                return true
            }
            val json: String
            try {
                val formatted: String? = JsonUtil.formatJson(INSTANCE)
                if (formatted == null) {
                    throw IllegalStateException("配置格式化失败，返回的 JSON 为空")
                }
                json = formatted
            } catch (e: Exception) {
                Log.printStackTrace(TAG, e)
                Log.record(TAG, "保存用户配置失败，格式化 JSON 时出错")
                return false
            }
            val success: Boolean
            try {
                if (StringUtil.isEmpty(userId)) {
                    userId = "默认"
                    success = Files.setDefaultConfigV2File(json)
                } else {
                    success = Files.setConfigV2File(userId!!, json)
                }
                if (!success) {
                    throw IOException("配置文件保存失败")
                }
                val userName: String = if (StringUtil.isEmpty(userId)) {
                    "默认用户"
                } else {
                    UserMap.get(userId)?.showName ?: "默认"
                }
                Log.record(TAG, "保存 [$userName] 配置")
            } catch (e: Exception) {
                Log.printStackTrace(TAG, e)
                Log.record(TAG, "保存用户配置失败")
                return false
            }
            return true
        }

        fun isLoaded(): Boolean = INSTANCE.initialized

        /**
         * 加载配置文件
         *
         * @param userId 用户 ID
         * @return 配置是否成功加载
         */
        @Synchronized
        fun load(userId: String?): Config {
            Log.record(TAG, "开始加载配置")
            var userName: String
            val configV2File: File
            if (StringUtil.isEmpty(userId)) {
                configV2File = Files.getDefaultConfigV2File()
                userName = "默认"
            } else {
                configV2File = Files.getConfigV2File(userId!!)
                val userEntity = UserMap.get(userId)
                userName = userEntity?.showName ?: userId
            }
            Log.record(TAG, "加载配置: $userName")

            var loaded = false
            try {
                val defaultFile = Files.getDefaultConfigV2File()
                // 优先用户配置，其次默认配置
                val sourceFile = when {
                    configV2File.exists() -> configV2File
                    defaultFile.exists() -> defaultFile
                    else -> null
                }
                if (sourceFile != null) {
                    val json = Files.readFromFile(sourceFile)
                    if (json.isNotBlank()) {
                        // ModelField 为抽象类，Jackson 无法构造，改为手动解析 JSON 树应用配置值
                        val modelFieldsNode = JsonUtil.toNode(json)?.get("modelFieldsMap")
                        INSTANCE.applyModelFieldsJson(modelFieldsNode)
                        loaded = true
                        if (sourceFile != configV2File) {
                            Log.record(TAG, "复制新配置: $userName")
                        }
                    }
                }
                if (!loaded) {
                    // 配置文件不存在或内容为空：以默认配置重建
                    INSTANCE.applyModelFieldsJson(null)
                }
                // 仅在解析成功（或无源文件）时规范化回写，解析失败时保留原文件，避免破坏用户配置
                if (loaded || sourceFile == null) {
                    val formatted = toSaveStr()
                    val oldJson = if (configV2File.exists()) Files.readFromFile(configV2File) else ""
                    if (formatted != oldJson) {
                        Files.write2File(formatted, configV2File)
                    }
                } else {
                    Log.error(TAG, "配置文件解析失败，本次使用默认配置，原文件保持不变: ${configV2File.absolutePath}")
                }
            } catch (t: Throwable) {
                // 任何异常都不覆盖现有配置文件，仅退回默认配置
                Log.printStackTrace(TAG, "加载配置失败", t)
                if (!loaded) {
                    INSTANCE.applyModelFieldsJson(null)
                }
            }
            INSTANCE.initialized = true
            TaskCommon.update()
            return INSTANCE
        }

        /**
         * 卸载当前配置
         */
        @Synchronized
        fun unload() {
            for (modelFields in INSTANCE._modelFieldsMap.values) {
                for (modelField in modelFields.values) {
                    modelField.reset()
                }
            }
        }

        fun toSaveStr(): String {
            // 防御：模型字段未填充时先以默认值重建，避免把空配置写回文件覆盖用户数据
            if (INSTANCE._modelFieldsMap.isEmpty() && Model.getModelConfigMap().isNotEmpty()) {
                INSTANCE.applyModelFieldsJson(null)
            }
            return JsonUtil.formatJson(INSTANCE)
        }
    }
}
