package fansirsqi.xposed.sesame.hook.rpc

import org.json.JSONArray
import org.json.JSONObject

/**
 * RPC 请求体统一构建器。
 * 支付宝 RPC 协议标准形态为数组包裹 `[{...}]`（抓包确认，见 docs 排查记录）。
 * 新代码一律使用本构建器，禁止手工拼接 / 转义字面量。
 */
object RpcRequestData {

    /** 单元素数组请求体 `[{...}]` —— 覆盖 99% 场景 */
    fun array(build: JSONObject.() -> Unit): String =
        JSONArray().put(JSONObject().apply(build)).toString()

    /** 多元素数组请求体 `[{...},{...}]`（极少数 API） */
    fun arrayOf(vararg items: JSONObject): String =
        JSONArray().apply { items.forEach { put(it) } }.toString()

    // 注意：协议统一为数组形态 `[{...}]`（抓包确认，全量扫描 540/540 实际调用均为数组），
    // 不再提供对象形态构建方法——任何对象形态请求（如历史上 queryMainPage/collectInsuredGold 的写法）都是错误的。

    /**
     * 标准字段注入 —— **有才写，绝不默认注入**。
     * 已统计：78% 的请求体原本不含 requestType 等字段（如广告类请求），
     * 调用前务必核对原请求是否携带这些字段，缺失的不得通过本方法补齐。
     */
    fun JSONObject.putStandard(
        requestType: String? = null,
        sceneCode: String? = null,
        source: String? = null,
        version: String? = null,
    ) {
        if (!requestType.isNullOrEmpty()) put("requestType", requestType)
        if (!sceneCode.isNullOrEmpty()) put("sceneCode", sceneCode)
        if (!source.isNullOrEmpty()) put("source", source)
        if (!version.isNullOrEmpty()) put("version", version)
    }
}

/**
 * RPC 标准字段常量（与 [RpcRequestData] 同文件）。
 *
 * 注意：requestType 为【开放集合】，以下仅为已抓包确认的取值，新增前先核对，不要当成穷举。
 */
object RpcConst {
    /** requestType 取值（开放集合，非穷举） */
    object Type {
        const val RPC = "RPC"
        const val RPC_L = "rpc"      // antiep 系列用小写，已见于 AntForest
        const val NORMAL = "NORMAL"
        const val H5 = "H5"          // 与 Src.H5 同值，但此处是 requestType 槽位
    }

    /** source 取值 */
    object Src {
        const val H5 = "H5"
        // 其余 source 值随抓包补充
    }
}
