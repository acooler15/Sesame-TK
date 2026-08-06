package fansirsqi.xposed.sesame.util.maps

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import fansirsqi.xposed.sesame.util.Files
import fansirsqi.xposed.sesame.core.json.JsonUtil
import fansirsqi.xposed.sesame.util.Log
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * 抽象ID映射工具类。
 * 提供通用的线程安全的ID映射功能，并支持单例管理。
 */
abstract class IdMapManager {
    /**
     * 存储ID映射的并发HashMap。
     */
    private val idMap: MutableMap<String, String> = ConcurrentHashMap()

    /**
     * 只读的ID映射。
     */
    private val readOnlyIdMap: Map<String, String> = Collections.unmodifiableMap(idMap)

    /**
     * 强制子类提供文件名。
     *
     * @return 文件名。
     */
    protected abstract fun thisFileName(): String

    /**
     * 获取只读的ID映射。
     *
     * @return 只读的ID映射。
     */
    val map: Map<String, String>
        get() = readOnlyIdMap

    /**
     * 根据键获取值。
     *
     * @param key 键。
     * @return 键对应的值，如果不存在则返回null。
     */
    fun get(key: String): String? = idMap[key]

    /**
     * 添加或更新ID映射。
     *
     * @param key   键。
     * @param value 值。
     */
    @Synchronized
    fun add(key: String, value: String) {
        idMap[key] = value
    }

    /**
     * 从ID映射中删除键值对。
     *
     * @param key 键。
     */
    @Synchronized
    fun remove(key: String) {
        idMap.remove(key)
    }

    /**
     * 从文件加载ID映射。
     *
     * @param userId 用户ID。
     */
    @Synchronized
    fun load(userId: String?) {
        if (userId.isNullOrEmpty()) {
            Log.record(TAG, "Skip loading map for empty userId")
            load()
        } else {
            idMap.clear()
            try {
                val file = Files.getTargetFileofUser(userId, thisFileName())
                assert(file != null)
                val body = Files.readFromFile(file!!)
                if (body.isNotEmpty()) {
                    val objectMapper = ObjectMapper()
                    val newMap = objectMapper.readValue(body, object : TypeReference<Map<String, String>>() {})
                    idMap.putAll(newMap)
                }
            } catch (e: Exception) {
                Log.printStackTrace(e)
            }
        }
    }

    @Synchronized
    fun load() {
        idMap.clear()
        try {
            val newFile = Files.getTargetFileofDir(CONFIG_DIR, thisFileName())
            val oldFile = Files.getTargetFileofDir(OLD_CONFIG_DIR, thisFileName())
            // 1. 新文件存在，直接加载
            if (newFile.exists()) {
                val body = Files.readFromFile(newFile)
                if (body.isNotEmpty()) {
                    val objectMapper = ObjectMapper()
                    val newMap = objectMapper.readValue(body, object : TypeReference<Map<String, String>>() {})
                    idMap.putAll(newMap)
                }
                if (oldFile.exists()) {
                    oldFile.delete()
                }
                return
            }
            // 2. 新文件不存在，检查旧文件是否存在
            if (oldFile.exists()) {
                Log.record(TAG, "Old configuration file found, migrating to new path...")

                try {
                    val body = Files.readFromFile(oldFile)
                    if (body.isNotEmpty()) {
                        val objectMapper = ObjectMapper()
                        val newMap = objectMapper.readValue(body, object : TypeReference<Map<String, String>>() {})
                        val json = JsonUtil.formatJson(newMap)
                        if (json.isNotEmpty()) {
                            Files.write2File(json, newFile)
                            oldFile.delete()
                            idMap.putAll(newMap)
                        }
                    }
                } catch (e: Exception) {
                    Log.error(TAG, "Migration of old configuration files failed：" + e.message)
                }
            }

            // 3. 新旧文件都不存在，初始化默认配置
            if (!newFile.exists()) {
                Log.record(TAG, "Configuration file not found, initializing an empty configuration file...")
                val json = JsonUtil.formatJson(idMap)
                if (json.isNotEmpty()) {
                    Files.write2File(json, newFile)
                }
            }
        } catch (e: Exception) {
            Log.printStackTrace(TAG, "ID映射管理器初始化失败：", e)
        }
    }

    /**
     * 将ID映射保存到文件。
     *
     * @param userId 用户ID。
     * @return 如果保存成功返回true，否则返回false。
     */
    @Synchronized
    fun save(userId: String?): Boolean {
        try {
            val json = JsonUtil.formatJson(idMap)
            val file = Files.getTargetFileofUser(userId, thisFileName())
            return Files.write2File(json, file!!)
        } catch (e: Exception) {
            Log.printStackTrace(e)
            return false
        }
    }

    @Synchronized
    fun save(): Boolean {
        try {
            val json = JsonUtil.formatJson(idMap)
            val file = Files.getTargetFileofDir(CONFIG_DIR, thisFileName())
            return Files.write2File(json, file)
        } catch (e: Exception) {
            Log.printStackTrace(e)
            return false
        }
    }

    /**
     * 清除ID映射。
     */
    @Synchronized
    fun clear() {
        idMap.clear()
    }

    companion object {
        private val TAG: String = IdMapManager::class.java.simpleName

        private val OLD_CONFIG_DIR = Files.MAIN_DIR // 旧配置目录
        private val CONFIG_DIR = Files.CONFIG_DIR // 配置目录

        private val instances = ConcurrentHashMap<Class<out IdMapManager>, IdMapManager>()

        @JvmStatic
        fun <T : IdMapManager> getInstance(clazz: Class<T>): T {
            var instance = clazz.cast(instances[clazz]) // 尝试从缓存中获取实例
            if (instance == null) { // 如果缓存中没有
                try {
                    instance = clazz.getDeclaredConstructor().newInstance() // 创建新实例
                    instances[clazz] = instance // 将实例放入缓存
                } catch (e: Exception) {
                    throw RuntimeException("Failed to create instance for " + clazz.name, e)
                }
            }
            return instance // 返回实例
        }
    }
}
