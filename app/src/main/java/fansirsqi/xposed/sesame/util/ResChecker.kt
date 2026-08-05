package fansirsqi.xposed.sesame.util

import org.json.JSONException
import org.json.JSONObject
import java.util.regex.Pattern

object ResChecker {
    private val TAG: String = ResChecker::class.java.simpleName

    private fun core(TAG: String, jo: JSONObject): Boolean {
        try {
//            Log.runtime(TAG, "Checking JSON success: " + jo)
            // 检查 success 或 isSuccess 字段为 true
            if (jo.optBoolean("success") || jo.optBoolean("isSuccess")) {
                return true
            }
            // 检查 resultCode
            val resCode = jo.opt("resultCode")
            if (resCode != null) {
                if (resCode is Int && resCode == 200) {
                    return true
                } else if (resCode is String &&
                    Pattern.matches("(?i)SUCCESS|100", resCode)
                ) {
                    return true
                }
            }
            // 检查 memo 字段
            if ("SUCCESS".equals(jo.optString("memo", ""), ignoreCase = true)) {
                return true
            }

            // 特殊情况：如果是"人数过多"或"小鸡睡觉"等系统状态，我们认为这不是一个需要记录的"失败"
            val resultDesc = jo.optString("resultDesc", "")
            val memo = jo.optString("memo", "")
            val desc = jo.optString("desc", "")
            val resultCode = jo.optString("resultCode", "")

            // 需要忽略的关键词列表（同时检查 resultDesc 和 memo）
            val ignoreKeywords = arrayOf(
                "当前参与人数过多", "请稍后再试", "手速太快", "频繁", "操作过于频繁",
                "我的小鸡在睡觉中", "小鸡在睡觉", "无法操作", "有人抢在你",
                "饲料槽已满", "当日达到上限", "适可而止", "不支持rpc完成的任务", "不支持rpc调用", "任务全局配置不存在",
                "庄园的小鸡太多了", "同一好友新村，只能摆一个小摊哦", "今日助力次数已用完", "收摊成功"
            )
            for (keyword in ignoreKeywords) {
                if (resultDesc.contains(keyword) || memo.contains(keyword) || desc.contains(keyword)) {
                    return false // 返回false，但不打印错误日志
                }
            }
            // 特殊的 resultCode 检查
            if ("I07" == resultCode || "ILLEGAL_ARGUMENT" == resultCode || "I09" == resultCode) {
                return false // 返回false，但不打印错误日志
            }
            // 获取调用栈信息以确定错误来源
            val stackTrace = Thread.currentThread().stackTrace
            val callerInfo = getString(stackTrace)
            Log.error(TAG, "Check failed: [来源: $callerInfo] $jo")
            return false
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "Error checking JSON success:", t)
            return false
        }
    }

    private fun getString(stackTrace: Array<StackTraceElement>): String {
        val callerInfo = StringBuilder()
        var foundCount = 0
        // 最多显示4层调用栈
        val MAX_STACK_DEPTH = 4
        val PROJECT_PACKAGE = "fansirsqi.xposed.sesame"

        // 寻找项目包名下的调用者
        for (element in stackTrace) {
            val className = element.className
            // 只显示项目包名下的类，跳过ResChecker
            if (className.startsWith(PROJECT_PACKAGE) && !className.contains("ResChecker")) {
                // 获取类名（保留项目包名后的部分）
                val relativeClassName = className.substring(PROJECT_PACKAGE.length + 1)
                if (foundCount > 0) {
                    callerInfo.append(" <- ")
                }
                callerInfo.append(relativeClassName)
                    .append(".")
                    .append(element.methodName)
                    .append(":")
                    .append(element.lineNumber)

                foundCount++
                if (foundCount >= MAX_STACK_DEPTH) {
                    break
                }
            }
        }

        return callerInfo.toString()
    }

    /**
     * 检查JSON对象是否表示成功
     *
     * 成功条件包括：<br></br>
     * - success == true<br></br>
     * - isSuccess == true<br></br>
     * - resultCode == 200 或 "SUCCESS" 或 "100"<br></br>
     * - memo == "SUCCESS"<br></br>
     *
     * @param jo JSON对象
     * @return true 如果成功
     */
    @JvmStatic
    fun checkRes(TAG: String, jo: JSONObject): Boolean = core(TAG, jo)

    /**
     * 检查JSON对象是否表示成功
     *
     * 成功条件包括：<br></br>
     * - success == true<br></br>
     * - isSuccess == true<br></br>
     * - resultCode == 200 或 "SUCCESS" 或 "100"<br></br>
     * - memo == "SUCCESS"<br></br>
     *
     * @param jsonStr JSON对象的字符串表示
     * @return true 如果成功
     */
    @JvmStatic
    @Throws(JSONException::class)
    fun checkRes(TAG: String, jsonStr: String): Boolean {
        val jo = JSONObject(jsonStr)
        return checkRes(TAG, jo)
    }
}
