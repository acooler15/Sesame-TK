package fansirsqi.xposed.sesame.util.maps

/**
 * 合种ID映射工具类。
 */
class CooperateMap : IdMapManager() {
    override fun thisFileName(): String {
        return "cooperateMap.json" //合种ID映射文件名
    }
}
