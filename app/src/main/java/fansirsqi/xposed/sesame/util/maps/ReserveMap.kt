package fansirsqi.xposed.sesame.util.maps

/**
 * 保护地ID映射工具类。
 * 注意：文件名为历史存量数据文件名，不可更改。
 */
class ReserveMap : IdMapManager() {
    override fun thisFileName(): String {
        return "ReserveaMap.json" //保护地ID映射表
    }
}
