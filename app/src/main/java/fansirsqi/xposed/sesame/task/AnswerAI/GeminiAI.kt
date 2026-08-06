package fansirsqi.xposed.sesame.task.AnswerAI

import fansirsqi.xposed.sesame.core.json.JsonUtil.getValueByPath
import fansirsqi.xposed.sesame.core.log.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * GeminiAI帮助类，用于与Gemini接口交互以获取AI回答
 * 支持单条文本问题及带有候选答案列表的问题请求
 */
class GeminiAI(token: String?) : AnswerAIInterface {

    override var modelName: String? = "gemini-2.5-flash"

    private val token: String = if (!token.isNullOrEmpty()) token else ""

    // 移除控制字符
    private fun removeControlCharacters(text: String): String {
        return text.replace(Regex("\u005Cp{Cntrl}&&[^\n\t]"), "")
    }

    /**
     * 构建请求体
     *
     * @param text 问题内容
     * @return 请求体的JSON字符串
     */
    private fun buildRequestBody(text: String): String {
        val cleanedText = removeControlCharacters(text)
        return String.format("{" + "\"contents\":[{" + "\"parts\":[{" + "\"text\":\"%s\"" + "}]" + "}]" + "}", PREFIX + cleanedText)
    }

    /**
     * 构建请求URL
     *
     * @return 完整的请求URL
     */
    private fun buildRequestUrl(): String {
        return String.format("%s/v1beta/models/%s:generateContent?key=%s",
                BASE_URL, this.modelName, token)
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
            val client = OkHttpClient.Builder()
                    .connectTimeout(TIME_OUT_SECONDS.toLong(), TimeUnit.SECONDS)
                    .writeTimeout(TIME_OUT_SECONDS.toLong(), TimeUnit.SECONDS)
                    .readTimeout(TIME_OUT_SECONDS.toLong(), TimeUnit.SECONDS)
                    .build()

            val content = buildRequestBody(text!!)
            val body = content.toRequestBody(CONTENT_TYPE.toMediaTypeOrNull())
            val url = buildRequestUrl()
            val request = Request.Builder()
                    .url(url)
                    .method("POST", body)
                    .addHeader("Content-Type", CONTENT_TYPE)
                    .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body
                if (responseBody == null) {
                    return result
                }
                val json = responseBody.string()
                if (!response.isSuccessful) {
                    Log.other("Gemini请求失败")
                    Log.record(TAG, "Gemini接口异常：" + json)
                    return result
                }
                val jsonObject = JSONObject(json)
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
        try {
            val answerStr = StringBuilder()
            for (i in answerList!!.indices) {
                answerStr.append(i + 1).append(".[")
                        .append(answerList[i]).append("]\n")
            }

            val question = "问题：" + title + "\n\n" +
                    "答案列表：\n\n" + answerStr + "\n\n" +
                    "请只返回答案列表中的序号"

            // 同步调用，主线程等待结果
            val answerResult = getAnswerStr(question)

            if (answerResult.isNotEmpty()) {
                try {
                    val index = answerResult.trim().toInt() - 1
                    if (index >= 0 && index < answerList.size) {
                        return index
                    }
                } catch (e: NumberFormatException) {
                    // 如果不是纯数字，尝试模糊匹配答案内容
                    Log.other("AI🧠回答，非序号格式：" + answerResult)
                }

                // 模糊匹配答案内容
                for (i in answerList.indices) {
                    if (answerResult.contains(answerList[i])) {
                        return i
                    }
                }
            }
        } catch (e: Exception) {
            Log.printStackTrace(TAG, e)
        }
        return -1
    }

    companion object {
        private val TAG: String = GeminiAI::class.java.simpleName
        private const val BASE_URL = "https://api.genai.gd.edu.kg/google"
        private const val CONTENT_TYPE = "application/json"
        private const val JSON_PATH = "candidates.[0].content.parts.[0].text"
        private const val PREFIX = "只回答答案 "
        private const val TIME_OUT_SECONDS = 180
    }
}
