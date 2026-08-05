package fansirsqi.xposed.sesame.entity

import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.maps.UserMap

/**
 * 表示目标应用用户的实体类，包含 ID 和名称。
 */
class AlipayUser(i: String, n: String) : MapperEntity() {
    init {
        id = i
        name = n
    }

    companion object {
        /**
         * 获取所有用户的列表，不使用任何过滤器。
         * @return 包含所有符合条件的 AlipayUser 对象的列表
         */
        @JvmStatic
        fun getList(): List<AlipayUser> = getList { true } // 默认不过滤

        /**
         * 获取符合过滤条件的用户列表。
         * @param filterFunc 过滤函数，用于筛选用户
         * @return 符合条件的 AlipayUser 对象列表
         */
        @JvmStatic
        fun getList(filterFunc: (UserEntity) -> Boolean): List<AlipayUser> {
            val list = ArrayList<AlipayUser>()
            val userIdMap = UserMap.getUserMap()
            for ((key, userEntity) in userIdMap) {
                try {
                    // 使用过滤器判断是否添加用户
                    if (filterFunc(userEntity)) {
                        list.add(AlipayUser(key, userEntity.fullName))
                    }
                } catch (t: Throwable) {
                    Log.printStackTrace(t) // 捕获并记录异常
                }
            }
            return list
        }
    }
}
