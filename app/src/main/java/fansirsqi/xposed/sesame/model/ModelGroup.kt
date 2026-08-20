package fansirsqi.xposed.sesame.model

enum class ModelGroup(
    val code: String,
    @get:JvmName("getName") val displayName: String,
    val icon: String
) {
    BASE("BASE", "基础", "svg/group/base.svg"),
    FOREST("FOREST", "森林", "svg/group/forest.svg"),
    FARM("FARM", "庄园", "svg/group/farm.svg"),
    STALL("STALL", "新村", "svg/group/stall.svg"),
    ORCHARD("ORCHARD", "农场", "svg/group/orchard.svg"),
    SPORTS("SPORTS", "运动", "svg/group/sports.svg"),
    MEMBER("MEMBER", "会员", "svg/group/member.svg"),
    OTHER("OTHER", "其他", "svg/group/other.svg");

    companion object {
        private val MAP: Map<String, ModelGroup> = entries.associateBy { it.code }

        @JvmStatic
        fun getByCode(code: String?): ModelGroup? = MAP[code]

        @JvmStatic
        fun getName(code: String?): String? = getByCode(code)?.displayName
    }
}
