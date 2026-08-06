package fansirsqi.xposed.sesame.core.json

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.type.TypeFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import fansirsqi.xposed.sesame.core.log.Log
import org.json.JSONArray
import org.json.JSONObject
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object JsonUtil {
    private const val TAG = "JsonUtil"
    private val MAPPER: ObjectMapper = ObjectMapper().apply { // JSON对象映射器
        // 配置 ObjectMapper
        registerModule(KotlinModule.Builder().build()) // 支持 Kotlin data class 构造器反序列化
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false) // 忽略未知属性
        configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false) // 忽略空对象
        setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL) // 忽略空属性
        setTimeZone(TimeZone.getDefault()) // 设置时区
        dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) // 设置日期格式
    }

    /**
     * 将对象转换为 JSON 字符串
     *
     * @param obj 要转换的对象
     * @return JSON 字符串
     */
    fun toJson(obj: Any): String {
        return MAPPER.writeValueAsString(obj) // 执行序列化
    }

    /**
     * 解析 JSON 字符串为指定类型的对象（reified 泛型）
     *
     * @param json JSON 字符串
     * @param <T>  目标类型泛型
     * @return 解析后的对象
     */
    inline fun <reified T> fromJson(json: String): T {
        return fromJson(json, T::class.java) // 执行反序列化
    }

    /**
     * 解析 JSON 字符串为指定类型的对象
     *
     * @param json  JSON 字符串
     * @param clazz 目标类
     * @param <T>   目标类型泛型
     * @return 解析后的对象
     */
    fun <T> fromJson(json: String, clazz: Class<T>): T {
        return MAPPER.readValue(json, clazz) // 执行反序列化
    }

    @JvmField
    val TYPE_FACTORY: TypeFactory = TypeFactory.defaultInstance() // 类型工厂

    @JvmField
    val JSON_FACTORY: JsonFactory = JsonFactory() // JSON工厂

    @JvmStatic
    fun copyMapper(): ObjectMapper = MAPPER.copy() // 复制 ObjectMapper

    /**
     * 将对象转换为格式化的 JSON 字符串
     *
     * @param object 要转换的对象
     * @return 格式化后的 JSON 字符串
     */
    @JvmStatic
    fun formatJson(`object`: Any?): String {
        try {
            if (`object` is JSONObject) {
                return `object`.toString(4) // 使用 4 个空格进行缩进
            }
            return execute { MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(`object`) }
        } catch (e: Exception) {
            Log.record(TAG, "formatJson err:")
            Log.printStackTrace(e)
            return execute { MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(`object`) }
        }
    }

    /**
     * 将对象转换为 JSON 字符串
     *
     * @param object 要转换的对象
     * @param pretty 是否格式化 JSON 字符串
     * @return JSON 字符串
     */
    @JvmStatic
    fun formatJson(`object`: Any?, pretty: Boolean): String {
        try {
            if (`object` is JSONObject) {
                return if (pretty) {
                    `object`.toString(4)
                } else {
                    `object`.toString()
                }
            }
            return if (pretty) {
                execute { MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(`object`) }
            } else {
                execute { MAPPER.writeValueAsString(`object`) }
            }
        } catch (e: Exception) {
            Log.record(TAG, "formatJson err:")
            Log.printStackTrace(e)
            return execute { MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(`object`) }
        }
    }

    /**
     * 创建 JSON 解析器
     *
     * @param body JSON 字符串
     * @return JsonParser 解析器
     */
    @JvmStatic
    fun getJsonParser(body: String): JsonParser {
        return execute { JSON_FACTORY.createParser(body) } // 执行解析器创建
    }

    /**
     * 解析 JSON 字符串为指定类型的对象
     *
     * @param body JSON 字符串
     * @param type 目标类型
     * @param <T>  目标类型泛型
     * @return 解析后的对象
     */
    @JvmStatic
    fun <T> parseObject(body: String, type: Type): T {
        return parseObjectInternal { MAPPER.readValue(body, TYPE_FACTORY.constructType(type)) } // 执行解析
    }

    /**
     * 解析 JSON 字符串为指定类型的对象
     *
     * @param body     JSON 字符串
     * @param javaType 目标 JavaType
     * @param <T>      目标类型泛型
     * @return 解析后的对象
     */
    @JvmStatic
    fun <T> parseObject(body: String, javaType: JavaType): T {
        return parseObjectInternal { MAPPER.readValue(body, javaType) } // 执行解析
    }

    /**
     * 解析 JSON 字符串为指定类型的对象
     *
     * @param body         JSON 字符串
     * @param valueTypeRef 目标类型引用
     * @param <T>          目标类型泛型
     * @return 解析后的对象
     */
    @JvmStatic
    fun <T> parseObject(body: String, valueTypeRef: TypeReference<T>): T {
        return parseObjectInternal { MAPPER.readValue(body, valueTypeRef) } // 执行解析
    }

    /**
     * 解析 JSON 字符串为指定类型的对象
     *
     * @param body  JSON 字符串
     * @param clazz 目标类
     * @param <T>   目标类型泛型
     * @return 解析后的对象
     */
    @JvmStatic
    fun <T> parseObject(body: String, clazz: Class<T>): T {
        return parseObjectInternal { MAPPER.readValue(body, clazz) } // 执行解析
    }

    /**
     * 从 JsonParser 解析为指定类型的对象
     *
     * @param jsonParser JsonParser 实例
     * @param type       目标类型
     * @param <T>        目标类型泛型
     * @return 解析后的对象
     */
    @JvmStatic
    fun <T> parseObject(jsonParser: JsonParser, type: Type): T {
        return parseObjectInternal { MAPPER.readValue(jsonParser, TYPE_FACTORY.constructType(type)) } // 执行解析
    }

    /**
     * 从 JsonParser 解析为指定类型的对象
     *
     * @param jsonParser JsonParser 实例
     * @param javaType   目标 JavaType
     * @param <T>        目标类型泛型
     * @return 解析后的对象
     */
    @JvmStatic
    fun <T> parseObject(jsonParser: JsonParser, javaType: JavaType): T {
        return parseObjectInternal { MAPPER.readValue(jsonParser, javaType) } // 执行解析
    }

    /**
     * 从 JsonParser 解析为指定类型的对象
     *
     * @param jsonParser   JsonParser 实例
     * @param valueTypeRef 目标类型引用
     * @param <T>          目标类型泛型
     * @return 解析后的对象
     */
    @JvmStatic
    fun <T> parseObject(jsonParser: JsonParser, valueTypeRef: TypeReference<T>): T {
        return parseObjectInternal { MAPPER.readValue(jsonParser, valueTypeRef) } // 执行解析
    }

    /**
     * 从 JsonParser 解析为指定类型的对象
     *
     * @param jsonParser JsonParser 实例
     * @param clazz      目标类
     * @param <T>        目标类型泛型
     * @return 解析后的对象
     */
    @JvmStatic
    fun <T> parseObject(jsonParser: JsonParser, clazz: Class<T>): T {
        return parseObjectInternal { MAPPER.readValue(jsonParser, clazz) } // 执行解析
    }

    /**
     * 将对象转换为指定类型的对象
     *
     * @param bean 源对象
     * @param type 目标类型
     * @param <T>  目标类型泛型
     * @return 转换后的对象
     */
    @JvmStatic
    fun <T> parseObject(bean: Any?, type: Type): T {
        return parseObjectInternal { MAPPER.convertValue(bean, TYPE_FACTORY.constructType(type)) } // 执行转换
    }

    /**
     * 将对象转换为指定类型的对象
     *
     * @param bean     源对象
     * @param javaType 目标 JavaType
     * @param <T>      目标类型泛型
     * @return 转换后的对象
     */
    @JvmStatic
    fun <T> parseObject(bean: Any?, javaType: JavaType): T {
        return parseObjectInternal { MAPPER.convertValue(bean, javaType) } // 执行转换
    }

    /**
     * 将对象转换为指定类型的对象
     *
     * @param bean         源对象
     * @param valueTypeRef 目标类型引用
     * @param <T>          目标类型泛型
     * @return 转换后的对象
     */
    @JvmStatic
    fun <T> parseObject(bean: Any?, valueTypeRef: TypeReference<T>): T {
        return parseObjectInternal { MAPPER.convertValue(bean, valueTypeRef) } // 执行转换
    }

    /**
     * 将对象转换为指定类型的对象
     *
     * @param bean  源对象
     * @param clazz 目标类
     * @param <T>   目标类型泛型
     * @return 转换后的对象
     */
    @JvmStatic
    fun <T> parseObject(bean: Any?, clazz: Class<T>): T {
        return parseObjectInternal { MAPPER.convertValue(bean, clazz) } // 执行转换
    }

    /**
     * 解析 JSON 字符串中的指定字段为字符串
     *
     * @param body  JSON 字符串
     * @param field 指定字段名
     * @return 字段值
     */
    @JvmStatic
    fun parseString(body: String, field: String): String? {
        return execute {
            val node = MAPPER.readTree(body)[field] // 获取字段节点
            node?.asText() // 返回字段值
        }
    }

    /**
     * 解析 JSON 字符串中的指定字段为整数
     *
     * @param body  JSON 字符串
     * @param field 指定字段名
     * @return 字段值
     */
    @JvmStatic
    fun parseInteger(body: String, field: String): Int? {
        return execute {
            val node = MAPPER.readTree(body)[field] // 获取字段节点
            node?.asInt() // 返回字段值
        }
    }

    /**
     * 解析 JSON 字符串中的指定字段为整数列表
     *
     * @param body  JSON 字符串
     * @param field 指定字段名
     * @return 字段值列表
     */
    @JvmStatic
    fun parseIntegerList(body: String, field: String): List<Int>? {
        return execute {
            val node = MAPPER.readTree(body)[field] // 获取字段节点
            if (node != null) MAPPER.convertValue(node, object : TypeReference<List<Int>>() {}) else null // 返回字段值列表
        }
    }

    /**
     * 解析 JSON 字符串中的指定字段为布尔值
     *
     * @param body  JSON 字符串
     * @param field 指定字段名
     * @return 字段值
     */
    @JvmStatic
    fun parseBoolean(body: String, field: String): Boolean? {
        return execute {
            val node = MAPPER.readTree(body)[field] // 获取字段节点
            node?.asBoolean() // 返回字段值
        }
    }

    /**
     * 解析 JSON 字符串中的指定字段为短整型
     *
     * @param body  JSON 字符串
     * @param field 指定字段名
     * @return 字段值
     */
    @JvmStatic
    fun parseShort(body: String, field: String): Short? {
        return execute {
            val node = MAPPER.readTree(body)[field] // 获取字段节点
            node?.asInt()?.toShort() // 返回字段值
        }
    }

    /**
     * 解析 JSON 字符串中的指定字段为字节型
     *
     * @param body  JSON 字符串
     * @param field 指定字段名
     * @return 字段值
     */
    @JvmStatic
    fun parseByte(body: String, field: String): Byte? {
        return execute {
            val node = MAPPER.readTree(body)[field] // 获取字段节点
            node?.asInt()?.toByte() // 返回字段值
        }
    }

    /**
     * 解析 JSON 字符串为指定类型的对象列表
     *
     * @param body  JSON 字符串
     * @param clazz 目标类
     * @param <T>   目标类型泛型
     * @return 解析后的对象列表
     */
    @JvmStatic
    fun <T> parseList(body: String, clazz: Class<T>): List<T> {
        return parseObjectInternal { MAPPER.readValue(body, TYPE_FACTORY.constructCollectionType(ArrayList::class.java, clazz)) } // 执行解析
    }

    /**
     * 将 JSON 字符串转换为 JsonNode
     *
     * @param json JSON 字符串
     * @return JsonNode 对象
     */
    @JvmStatic
    fun toNode(json: String?): JsonNode? {
        return if (json == null) null else execute { MAPPER.readTree(json) } // 执行转换
    }

    /**
     * 根据路径获取 JSON 对象中的值
     *
     * @param jsonObject JSON 对象
     * @param path       字段路径（以 "." 分隔）
     * @return 字段值
     */
    @JvmStatic
    fun getValueByPath(jsonObject: JSONObject, path: String): String {
        val value = getValueByPathObject(jsonObject, path) // 获取字段值
        return value?.toString() ?: "" // 返回字段值的字符串形式
    }

    /**
     * 根据路径获取 JSON 对象中的值
     *
     * @param jsonObject JSON 对象
     * @param path       字段路径（以 "." 分隔）
     * @return 字段值
     */
    @JvmStatic
    fun getValueByPathObject(jsonObject: JSONObject, path: String): Any? {
        val parts = path.split("\\.").toTypedArray() // 分割路径
        try {
            var current: Any? = jsonObject // 当前对象
            for (part in parts) {
                current = if (current is JSONObject) {
                    current.get(part) // 从 JSONObject 获取值
                } else if (current is JSONArray) {
                    val index = part.replace("\\D".toRegex(), "").toInt() // 获取数组索引
                    current.get(index) // 从 JSONArray 获取值
                } else {
                    JSONObject(current.toString())[part] // 将当前对象转为 JSONObject 并获取值
                }
            }
            return current // 返回最终的值
        } catch (e: Exception) {
            return null // 异常时返回 null
        }
    }

    /**
     * 安全地创建JSONObject，处理空字符串和null的情况
     *
     * @param jsonStr JSON字符串
     * @return JSONObject对象，如果输入为空则返回空的JSONObject
     */
    @JvmStatic
    fun parseJSONObject(jsonStr: String?): JSONObject {
        try {
            // 检查字符串是否为空或null
            if (jsonStr == null || jsonStr.trim { it <= ' ' }.isEmpty()) {
                Log.record(TAG, "收到空响应，可能是网络异常或服务端错误")
                return JSONObject() // 返回空的JSONObject
            }
            return JSONObject(jsonStr)
        } catch (e: Exception) {
            Log.record(TAG, "JSON解析失败: " + e.message)
            Log.record(TAG, "原始响应: " + (if (jsonStr!!.length > 200) jsonStr.substring(0, 200) + "..." else jsonStr))
            return JSONObject() // 返回空的JSONObject
        }
    }

    /**
     * 安全地创建JSONArray，处理空字符串和null的情况
     *
     * @param jsonStr JSON字符串
     * @return JSONArray对象，如果输入为空则返回空的JSONArray
     */
    @JvmStatic
    fun parseJSONArray(jsonStr: String?): JSONArray {
        try {
            // 检查字符串是否为空或null
            if (jsonStr == null || jsonStr.trim { it <= ' ' }.isEmpty()) {
                Log.record(TAG, "收到空响应，可能是网络异常或服务端错误")
                return JSONArray() // 返回空的JSONArray
            }
            return JSONArray(jsonStr)
        } catch (e: Exception) {
            Log.record(TAG, "JSON数组解析失败: " + e.message)
            return JSONArray() // 返回空的JSONArray
        }
    }

    /**
     * 将 JSONArray 转换为字符串列表
     *
     * @param jsonArray 源 JSONArray
     * @return 字符串列表
     */
    @JvmStatic
    fun jsonArrayToList(jsonArray: JSONArray): List<String> {
        val list = ArrayList<String>() // 创建列表
        for (i in 0 until jsonArray.length()) {
            try {
                list.add(jsonArray.getString(i)) // 添加字符串到列表
            } catch (e: Exception) {
                Log.printStackTrace(e) // 打印异常栈
                list.add("") // 异常时添加空字符串
            }
        }
        return list // 返回列表
    }

    /**
     * 内部方法，执行 JSON 操作并处理异常
     *
     * @param action JSON 操作
     * @param <T>    操作返回类型
     * @return 操作结果
     */
    private fun <T> parseObjectInternal(action: () -> T): T {
        return execute(action) // 执行操作
    }

    /**
     * 执行 JSON 操作并处理异常
     *
     * @param action JSON 操作
     * @param <T>    操作返回类型
     * @return 操作结果
     */
    private fun <T> execute(action: () -> T): T {
        try {
            return action() // 执行操作
        } catch (e: Exception) {
            throw RuntimeException(e) // 异常时抛出运行时异常
        }
    }
}
