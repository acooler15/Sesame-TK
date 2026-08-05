package fansirsqi.xposed.sesame.model

import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField
import fansirsqi.xposed.sesame.task.ModelTask
import fansirsqi.xposed.sesame.util.Log
import java.lang.reflect.InvocationTargetException
import java.util.Collections
import java.util.LinkedHashMap
import java.util.concurrent.ConcurrentHashMap

abstract class Model {
    val enableField: BooleanModelField

    init {
        // 基础模块默认启用，其他模块默认禁用
        val defaultValue = "基础" == getName()
        enableField = BooleanModelField("enable", getEnableFieldName(), defaultValue)
    }

    open fun getEnableFieldName(): String {
        return "开启" + getName()
    }

    val isEnable: Boolean
        get() = enableField.value

    open fun getType(): ModelType {
        return ModelType.NORMAL
    }

    abstract fun getName(): String?

    abstract fun getGroup(): ModelGroup

    abstract fun getIcon(): String

    abstract fun getFields(): ModelFields?

    open fun prepare() {}

    open fun boot(classLoader: ClassLoader?) {}

    open fun destroy() {}

    companion object {
        private const val TAG = "Model"
        private val modelConfigMap = LinkedHashMap<String, ModelConfig>()
        private val readOnlyModelConfigMap: Map<String, ModelConfig> = Collections.unmodifiableMap(modelConfigMap)
        private val groupModelConfigMap = LinkedHashMap<ModelGroup, MutableMap<String, ModelConfig>>()
        private val modelMap = ConcurrentHashMap<Class<out Model>, Model>()
        private val modelClazzList: List<Class<out Model>> = ModelOrder.allConfig

        @JvmField
        val modelArray: Array<Model?> = arrayOfNulls(modelClazzList.size)

        @JvmStatic
        fun getModelConfigMap(): Map<String, ModelConfig> {
            return readOnlyModelConfigMap
        }

        @JvmStatic
        fun getGroupModelConfig(modelGroup: ModelGroup?): Map<String, ModelConfig> {
            val map = groupModelConfigMap[modelGroup] ?: return Collections.emptyMap()
            return Collections.unmodifiableMap(map)
        }

        @JvmStatic
        fun <T : Model> getModel(modelClazz: Class<T>): T? {
            val model = modelMap[modelClazz]
            return if (modelClazz.isInstance(model)) {
                modelClazz.cast(model)
            } else {
                Log.error(TAG, "Model " + modelClazz.simpleName + " not found.")
                null
            }
        }

        @JvmStatic
        @Synchronized
        fun initAllModel() {
            destroyAllModel()
            var i = 0
            val len = modelClazzList.size
            while (i < len) {
                val modelClazz = modelClazzList[i]
                try {
                    val model = modelClazz.getDeclaredConstructor().newInstance()
                    val modelConfig = ModelConfig(model)
                    modelArray[i] = model
                    modelMap[modelClazz] = model
                    val modelCode = modelConfig.code!!
                    modelConfigMap[modelCode] = modelConfig
                    val group = modelConfig.group!!
                    groupModelConfigMap.getOrPut(group) { LinkedHashMap() }[modelCode] = modelConfig
                } catch (e: IllegalAccessException) {
                    Log.printStackTrace(e)
                } catch (e: InstantiationException) {
                    Log.printStackTrace(e)
                } catch (e: NoSuchMethodException) {
                    Log.printStackTrace(e)
                } catch (e: InvocationTargetException) {
                    Log.printStackTrace(e)
                }
                i++
            }
        }

        @JvmStatic
        @Synchronized
        fun bootAllModel(classLoader: ClassLoader?) {
            for (model in modelArray) {
                try {
                    model!!.prepare()
                } catch (e: Exception) {
                    Log.printStackTrace(e)
                }
                try {
                    if (model!!.enableField.value == true) {
                        model.boot(classLoader)
                    }
                } catch (e: Exception) {
                    Log.printStackTrace(e)
                }
            }
        }

        @JvmStatic
        @Synchronized
        fun destroyAllModel() {
            var i = 0
            val len = modelArray.size
            while (i < len) {
                val model = modelArray[i]
                if (model != null) {
                    try {
                        if (ModelType.TASK == model.getType()) {
                            (model as ModelTask).stopTask()
                        }
                        model.destroy()
                    } catch (e: Exception) {
                        Log.printStackTrace(e)
                    }
                    modelArray[i] = null
                }
                i++
            }
            modelMap.clear()
            modelConfigMap.clear()
        }
    }
}
