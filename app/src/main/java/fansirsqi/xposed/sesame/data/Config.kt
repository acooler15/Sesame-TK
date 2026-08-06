package fansirsqi.xposed.sesame.data

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import com.fasterxml.jackson.databind.node.ObjectNode
import fansirsqi.xposed.sesame.model.Model
import fansirsqi.xposed.sesame.model.ModelFields
import fansirsqi.xposed.sesame.task.TaskCommon
import fansirsqi.xposed.sesame.util.Files
import fansirsqi.xposed.sesame.core.json.JsonUtil
import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.util.StringUtil
import fansirsqi.xposed.sesame.util.maps.UserMap
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * 配置类，负责加载、保存、管理应用的配置数据。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class Config private constructor() {

    @Volatile
    private var initialized = false

    // 存储模型字段的映射
    private val _modelFieldsMap: MutableMap<String, ModelFields> = ConcurrentHashMap()

    fun getModelFieldsMap(): Map<String, ModelFields> = _modelFieldsMap

    /**
     * 设置新的模型字段配置
     *
     * @param newModels 新的模型字段映射
     */
    fun setModelFieldsMap(newModels: Map<String, ModelFields>?) {
        _modelFieldsMap.clear()
        val modelConfigMap = Model.getModelConfigMap()
        // 如果传入的 newModels 为 null，初始化为空
        val models = newModels ?: emptyMap()
        // 遍历所有模型配置，合并字段配置
        for (modelConfig in modelConfigMap.values) {
            val modelCode = modelConfig.code!!
            val newModelFields = ModelFields()
            val configModelFields = modelConfig.fields
            val modelFields = models[modelCode]
            if (modelFields != null) {
                // 如果已有模型字段，则按值覆盖配置
                for (configModelField in configModelFields.values) {
                    val modelField = modelFields[configModelField.code]
                    try {
                        if (modelField != null) {
                            val value = modelField.value
                            if (value != null) {
                                configModelField.setObjectValue(value)
                            }
                        }
                    } catch (e: Exception) {
                        Log.printStackTrace(e)
                    }
                    newModelFields.addField(configModelField)
                }
            } else {
                // 如果没有找到对应的模型字段，则直接添加配置字段
                for (configModelField in configModelFields.values) {
                    newModelFields.addField(configModelField)
                }
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

        @JvmField
        val INSTANCE: Config = Config()

        /**
         * 判断配置文件是否已修改
         *
         * @param userId 用户 ID
         * @return 是否已修改
         */
        @JvmStatic
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
        @JvmStatic
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

        @JvmStatic
        fun isLoaded(): Boolean = INSTANCE.initialized

        /**
         * 加载配置文件
         *
         * @param userId 用户 ID
         * @return 配置是否成功加载
         */
        @JvmStatic
        @Synchronized
        fun load(userId: String?): Config {
            Log.record(TAG, "开始加载配置")
            var userName = ""
            var configV2File: File? = null
            try {
                if (StringUtil.isEmpty(userId)) {
                    configV2File = Files.getDefaultConfigV2File()
                    userName = "默认"
                    if (!configV2File.exists()) {
                        Log.record(TAG, "默认配置文件不存在，初始化新配置")
                        unload()
                        Files.write2File(toSaveStr(), configV2File)
                    }
                } else {
                    configV2File = Files.getConfigV2File(userId!!)
                    val userEntity = UserMap.get(userId)
                    userName = userEntity?.showName ?: userId!!
                }

                Log.record(TAG, "加载配置: $userName")
                val configV2FileExists = configV2File.exists()
                val defaultConfigV2FileExists = Files.getDefaultConfigV2File().exists()

                if (configV2FileExists) {
                    val json = Files.readFromFile(configV2File)
                    try {
                        JsonUtil.copyMapper().readerForUpdating(INSTANCE).readValue<Config>(json)
                    } catch (e: UnrecognizedPropertyException) {
                        Log.error(TAG, "配置文件中存在无法识别的字段: '${e.propertyName}'，将尝试移除并重新加载。")
                        try {
                            // 移除无法识别的字段并重新解析
                            val mapper: ObjectMapper = JsonUtil.copyMapper()
                            val rootNode: JsonNode = mapper.readTree(json)
                            (rootNode as ObjectNode).remove(e.propertyName)
                            val cleanedJson = mapper.writeValueAsString(rootNode)
                            mapper.readerForUpdating(INSTANCE).readValue<Config>(cleanedJson)
                            Log.error(TAG, "成功移除问题字段并加载配置。")
                            // 保存修复后的配置
                            Files.write2File(toSaveStr(), configV2File)
                            Log.error(TAG, "已保存修复后的配置文件。")
                        } catch (innerEx: Exception) {
                            Log.printStackTrace(TAG, "移除问题字段后，加载配置仍然失败。", innerEx)
                            throw innerEx // 抛出内部异常，触发重置逻辑
                        }
                    }
                    val formatted = toSaveStr()
                    if (formatted != null && formatted != json) {
                        Files.write2File(formatted, configV2File)
                    }
                } else if (defaultConfigV2FileExists) {
                    val json = Files.readFromFile(Files.getDefaultConfigV2File())
                    JsonUtil.copyMapper().readerForUpdating(INSTANCE).readValue<Config>(json)
                    Log.record(TAG, "复制新配置: $userName")
                    Files.write2File(json, configV2File)
                } else {
                    unload()
                    Files.write2File(toSaveStr(), configV2File)
                }
            } catch (t: Throwable) {
                Log.printStackTrace(TAG, "重置配置失败", t)
                try {
                    unload()
                    if (configV2File != null) {
                        Files.write2File(toSaveStr(), configV2File)
                    }
                } catch (e: Exception) {
                    Log.printStackTrace(TAG, "重置配置失败", e)
                }
            }
            INSTANCE.initialized = true
            TaskCommon.update()
            return INSTANCE
        }

        /**
         * 卸载当前配置
         */
        @JvmStatic
        @Synchronized
        fun unload() {
            for (modelFields in INSTANCE._modelFieldsMap.values) {
                for (modelField in modelFields.values) {
                    modelField.reset()
                }
            }
        }

        @JvmStatic
        fun toSaveStr(): String = JsonUtil.formatJson(INSTANCE)
    }
}
