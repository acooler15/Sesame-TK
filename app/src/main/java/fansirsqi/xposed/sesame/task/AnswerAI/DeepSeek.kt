package fansirsqi.xposed.sesame.task.AnswerAI

import fansirsqi.xposed.sesame.core.json.JsonUtil.getValueByPath
import fansirsqi.xposed.sesame.core.log.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * DeepSeek帮助类，用于与DeepSeek接口交互以获取AI回答
 * 支持单条文本问题及带有候选答案列表的问题请求
 */
class DeepSeek(apiKey: String?) : AnswerAIInterface {

    override var modelName: String? = "deepseek-reasoner" //"deepseek-chat";

    private val apiKey: String = if (!apiKey.isNullOrEmpty()) apiKey else ""

    // 移除控制字符
    private fun removeControlCharacters(text: String): String {
        return text.replace(Regex("\u005Cp{Cntrl}&&[^\n\t]"), "")
    }

    // 构建请求体的JSON对象
    @Throws(JSONException::class)
    private fun buildRequestJson(text: String): JSONObject {
        val cleanedText = removeControlCharacters(text)
        val requestJson = JSONObject()
        requestJson.put("model", this.modelName)

        val messages = JSONArray()
        val systemMessage = JSONObject()
        systemMessage.put("role", "system")
        systemMessage.put("content", SYSTEM_MESSAGE)
        messages.put(systemMessage)

        val userMessage = JSONObject()
        userMessage.put("role", "user")
        userMessage.put("content", cleanedText)
        messages.put(userMessage)

        requestJson.put("messages", messages)
        requestJson.put("stream", false)
        return requestJson
    }

    // 发送请求并处理响应
    @Throws(IOException::class)
    private fun sendRequest(requestJson: JSONObject): String {
        val client = OkHttpClient.Builder()
                .connectTimeout(TIME_OUT_SECONDS.toLong(), TimeUnit.SECONDS)
                .writeTimeout(TIME_OUT_SECONDS.toLong(), TimeUnit.SECONDS)
                .readTimeout(TIME_OUT_SECONDS.toLong(), TimeUnit.SECONDS)
                .build()
        val body = requestJson.toString().toRequestBody(CONTENT_TYPE.toMediaTypeOrNull())
        val request = Request.Builder()
                .url(BASE_URL)
                .method("POST", body)
                .addHeader("Content-Type", CONTENT_TYPE)
                .addHeader("Authorization", AUTH_HEADER_PREFIX + apiKey)
                .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body
            val json = responseBody.string()
            if (!response.isSuccessful) {
                Log.other("DeepSeek请求失败")
                Log.record(TAG, "DeepSeek接口异常：" + json)
                return ""
            }
            return json
        }
    }

    override fun getAnswerStr(text: String?, model: String?): String {
        modelName = model
        return getAnswerStr(text)
    }

    /**
     * 获取AI回答结果
     *
     * @param text 问题内容
     * @return AI回答结果
     */
    override fun getAnswerStr(text: String?): String {
        var result = ""
        try {
            val requestJson = buildRequestJson(text!!)
            val jsonResponse = sendRequest(requestJson)
            if (jsonResponse.isNotEmpty()) {
                val jsonObject = JSONObject(jsonResponse)
                result = getValueByPath(jsonObject, JSON_PATH)
            }
        } catch (e: IOException) {
            Log.printStackTrace(TAG, e)
        } catch (e: JSONException) {
            Log.printStackTrace(TAG, e)
        }
        return result
    }

    /**
     * 获取答案
     *
     * @param title      问题
     * @param answerList 答案集合
     * @return 空没有获取到
     */
    override fun getAnswer(title: String?, answerList: List<String>?): Int? {
        val answerStr = StringBuilder()
        for (answer in answerList!!) {
            answerStr.append("[").append(answer).append("]")
        }
        val answerResult = getAnswerStr(title + "\n" + answerStr)
        if (answerResult.isNotEmpty()) {
            for (i in answerList.indices) {
                if (answerResult.contains(answerList[i])) {
                    return i
                }
            }
        }
        return -1
    }

    companion object {
        private val TAG: String = DeepSeek::class.java.simpleName
        private const val BASE_URL = "https://api.deepseek.com/v1/chat/completions"
        private const val CONTENT_TYPE = "application/json"
        private const val JSON_PATH = "choices.[0].message.content"
        private const val SYSTEM_MESSAGE = "你是一个拥有丰富的知识，并且能根据知识回答问题的专家。"
        private const val AUTH_HEADER_PREFIX = "Bearer "
        private const val TIME_OUT_SECONDS = 180
    }
}
