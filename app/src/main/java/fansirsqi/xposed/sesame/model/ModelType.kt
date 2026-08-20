package fansirsqi.xposed.sesame.model

enum class ModelType(
    val code: Int,
    @get:JvmName("getName") val displayName: String
) {
    NORMAL(0, "普通模块"),
    TASK(1, "任务模块");

    companion object {
        private val MAP: Map<Int, ModelType> = entries.associateBy { it.code }

        /**
         * 根据 code 获取枚举
         *
         * @param code 标识码
         * @return 对应的枚举，如果未找到则返回 null
         */
        @JvmStatic
        fun getByCode(code: Int): ModelType? = MAP[code]
    }
}
