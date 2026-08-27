package fansirsqi.xposed.sesame.service

/**
 * 特权 Shell 类型枚举。
 * 跨进程（AIDL）与跨层对齐统一依赖此枚举的 [name]，取代脆弱的字符串魔法值。
 */
enum class ShellType(val displayName: String) {
    /** Root（SafeRootShell） */
    ROOT("Root"),

    /** Shizuku（ShizukuShell） */
    SHIZUKU("Shizuku"),

    /** 无可用特权 Shell */
    NONE("无");

    companion object {
        /**
         * 安全解析：未知或 null 一律归为 [NONE]。
         * 用于客户端（CommandUtil）解析服务端推送的状态。
         */
        fun fromName(name: String?): ShellType =
            entries.firstOrNull { it.name == name } ?: NONE
    }
}
