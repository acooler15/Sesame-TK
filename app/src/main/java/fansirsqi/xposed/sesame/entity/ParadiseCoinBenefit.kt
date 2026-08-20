package fansirsqi.xposed.sesame.entity

import fansirsqi.xposed.sesame.util.maps.IdMapManager
import fansirsqi.xposed.sesame.util.maps.ParadiseCoinBenefitIdMap

class ParadiseCoinBenefit(i: String, n: String) : MapperEntity() {
    init {
        id = i
        name = n
    }

    companion object {
        @JvmStatic
        fun getList(): List<ParadiseCoinBenefit> {
            val list = ArrayList<ParadiseCoinBenefit>()
            val idSet = IdMapManager.getInstance(ParadiseCoinBenefitIdMap::class.java).map
            for ((key, value) in idSet) {
                list.add(ParadiseCoinBenefit(key, value))
            }
            return list
        }
    }
}
