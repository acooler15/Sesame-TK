package fansirsqi.xposed.sesame.entity

import fansirsqi.xposed.sesame.util.maps.IdMapManager
import fansirsqi.xposed.sesame.util.maps.ReserveMap
import java.util.Collections

/**
 * 表示目标应用保留项的实体类，包含 ID 和名称。
 */
class ReserveEntity(i: String, n: String) : MapperEntity() {
    init {
        id = i
        name = n
    }

    companion object {
        // 使用 @Volatile 确保多线程环境下的可见性
        @Volatile
        private var list: List<ReserveEntity>? = null

        /**
         * 获取包含所有保留项的列表，首次调用时从 ReserveIdMapUtil 初始化。
         * 使用双重检查锁定机制实现懒加载以提高性能。
         * @return 包含所有 ReserveEntity 对象的不可变列表
         */
        @JvmStatic
        fun getList(): List<ReserveEntity> {
            if (list == null) {
                synchronized(ReserveEntity::class.java) {
                    if (list == null) {
                        val tempList = ArrayList<ReserveEntity>()
                        val idSet: Set<Map.Entry<String, String>> =
                            IdMapManager.getInstance(ReserveMap::class.java).map.entries
                        for (entry in idSet) {
                            tempList.add(ReserveEntity(entry.key, entry.value))
                        }
                        list = Collections.unmodifiableList(tempList)
                    }
                }
            }
            return list!!
        }

        /**
         * 根据给定的 ID 删除相应的 ReserveEntity 对象。
         * 首次调用 getList 方法以确保列表已初始化。
         * @param id 要删除的保留项 ID
         */
        @JvmStatic
        fun remove(id: String) {
            getList()
            synchronized(ReserveEntity::class.java) {
                val tempList = ArrayList(list!!) // 创建可变列表的副本
                val iterator = tempList.iterator()
                while (iterator.hasNext()) {
                    val reserve = iterator.next()
                    if (reserve.id == id) {
                        iterator.remove()
                    }
                }
                list = Collections.unmodifiableList(tempList) // 确保返回不可变列表
            }
        }
    }
}
