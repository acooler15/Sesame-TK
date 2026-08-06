package fansirsqi.xposed.sesame.task.AnswerAI

import fansirsqi.xposed.sesame.core.json.JsonUtil
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
 * @author Byseven
 * @date 2025/1/30
 * @apiNote
 */
class TongyiAI(token: String?) : AnswerAIInterface {

    private val TAG: String = TongyiAI::class.java.simpleName

    override var modelName: String? = "qwen-turbo"

    private val token: String = if (!token.isNullOrEmpty()) token else ""

    /**
     * 获取AI回答结果
     *
     * @param text 问题内容
     * @return AI回答结果
     */
    override fun getAnswerStr(text: String?): String {
        var result = ""
        var response: okhttp3.Response? = null
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS) // 设置连接超时时间为 30 秒
                .writeTimeout(30, TimeUnit.SECONDS)   // 设置写超时时间为 30 秒
                .readTimeout(30, TimeUnit.SECONDS)    // 设置读超时时间为 30 秒
                .build()
            val contentObject = JSONObject()
            contentObject.put("role", "user")
            contentObject.put("content", text)
            val messageArray = JSONArray()
            messageArray.put(contentObject)
            val bodyObject = JSONObject()
            bodyObject.put("model", this.modelName)
            bodyObject.put("messages", messageArray)
            val body = bodyObject.toString().toRequestBody(CONTENT_TYPE.toMediaTypeOrNull())
            val request = Request.Builder()
                .url(URL)
                .method("POST", body)
                .addHeader("Authorization", "Bearer " + this.token)
                .addHeader("Content-Type", CONTENT_TYPE)
                .build()
            response = client.newCall(request).execute()
            val responseBody = response.body
            if (responseBody == null) {
                return result
            }
            val json = responseBody.string()
            if (!response.isSuccessful) {
                Log.other("Tongyi请求失败")
                Log.record(TAG, "Tongyi接口异常：" + json)
                return result
            }
            val jsonObject = JSONObject(json)
            result = JsonUtil.getValueByPath(jsonObject, JSON_PATH)
        } catch (e: JSONException) {
            Log.printStackTrace(TAG, e)
        } catch (e: IOException) {
            Log.printStackTrace(TAG, e)
        } finally {
            response?.close()
        }
        return result
    }

    override fun getAnswerStr(text: String?, model: String?): String {
        modelName = model
        return getAnswerStr(text)
    }

    /**
     * 获取答案
     *
     * @param title     问题
     * @param answerList 答案集合
     * @return 空没有获取到
     */
    override fun getAnswer(title: String?, answerList: List<String>?): Int? {
        val size = answerList!!.size
        val answerStr = StringBuilder()
        for (i in 0 until size) {
            answerStr.append(i + 1).append(".[").append(answerList[i]).append("]\n")
        }
        val answerResult = getAnswerStr("问题：" + title + "\n\n" + "答案列表：\n\n" + answerStr + "\n\n" + "请只返回答案列表中的序号")
        if (!answerResult.isNullOrEmpty()) {
            try {
                val index = answerResult.toInt() - 1
                if (index >= 0 && index < size) {
                    return index
                }
            } catch (e: Exception) {
                Log.record(TAG, "AI🧠回答，返回数据：" + answerResult)
            }
            for (i in 0 until size) {
                if (answerResult.contains(answerList[i])) {
                    return i
                }
            }
        }
        return -1
    }

    companion object {
        private const val URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
        private const val CONTENT_TYPE = "application/json"
        private const val JSON_PATH = "choices.[0].message.content"
    }
}
