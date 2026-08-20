package fansirsqi.xposed.sesame.hook.rpc.bridge

enum class RpcVersion(
    val code: String
) {
    OLD("OLD"),
    NEW("NEW");

    companion object {
        private val MAP: Map<String, RpcVersion> = entries.associateBy { it.code }

        fun getByCode(code: String?): RpcVersion? = MAP[code]
    }
}
