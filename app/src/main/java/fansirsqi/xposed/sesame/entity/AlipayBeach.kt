package fansirsqi.xposed.sesame.entity

import fansirsqi.xposed.sesame.util.maps.BeachMap
import fansirsqi.xposed.sesame.util.maps.IdMapManager
import java.util.Collections

/**
 * 表示目标应用海滩的实体类，包含 ID 和名称。
 */
class AlipayBeach(i: String, n: String) : MapperEntity() {
    init {
        id = i
        name = n
    }

    companion object {
        // 使用 @Volatile 注解确保多线程环境下的可见性
        @Volatile
        private var list: List<AlipayBeach>? = null

        /**
         * 获取包含所有海滩的列表，首次调用时从 BeachMap 初始化。
         * 使用双重检查锁定机制实现懒加载以提高性能。
         * @return 包含所有 AlipayBeach 对象的不可变列表
         */
        @JvmStatic
        fun getList(): List<AlipayBeach> {
            if (list == null) {
                synchronized(AlipayBeach::class.java) {
                    if (list == null) {
                        val tempList = ArrayList<AlipayBeach>()
                        for ((key, value) in IdMapManager.getInstance(BeachMap::class.java).map) {
                            tempList.add(AlipayBeach(key, value))
                        }
                        list = Collections.unmodifiableList(tempList)
                    }
                }
            }
            return list!!
        }

        /**
         * 根据给定的 ID 删除相应的 AlipayBeach 对象。
         * 首次调用 getList 方法以确保列表已初始化。
         * @param id 要删除的海滩 ID
         */
        @JvmStatic
        fun remove(id: String) {
            getList()
            synchronized(AlipayBeach::class.java) {
                val tempList = ArrayList(list!!) // 创建可变列表的副本
                tempList.removeIf { beach -> beach.id == id }
                list = Collections.unmodifiableList(tempList) // 确保返回不可变列表
            }
        }
    }
}
