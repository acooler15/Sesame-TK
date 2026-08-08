package fansirsqi.xposed.sesame.model.modelFieldExt

import com.fasterxml.jackson.annotation.JsonIgnore
import fansirsqi.xposed.sesame.entity.KVMap
import fansirsqi.xposed.sesame.model.ModelField
import fansirsqi.xposed.sesame.model.ModelFieldViewData
import fansirsqi.xposed.sesame.model.SelectModelFieldFunc
import fansirsqi.xposed.sesame.entity.MapperEntity

class SelectAndCountOneModelField : ModelField<KVMap<String?, Int?>>, SelectModelFieldFunc {
    private var selectListFunc: SelectListFunc? = null
    private var expandList: List<out MapperEntity>? = null

    constructor(code: String?, name: String?, value: KVMap<String?, Int?>, expandValue: List<out MapperEntity>?) : super(code, name, value) {
        this.expandList = expandValue
    }

    constructor(code: String?, name: String?, value: KVMap<String?, Int?>, selectListFunc: SelectListFunc?) : super(code, name, value) {
        this.selectListFunc = selectListFunc
    }

    override val type: String
        get() = "SELECT_AND_COUNT_ONE"

    override val expandValue: List<out MapperEntity>?
        get() = castList(selectListFunc?.getList() ?: expandList)

    @get:JsonIgnore
    override val viewData: ModelFieldViewData
        get() = super.viewData.copy(clickAction = ModelFieldViewData.ClickAction.LIST)

    override fun clear() {
        value = defaultValue
    }

    override fun get(id: String?): Int? {
        val kvMap: KVMap<String?, Int?>? = value
        if (kvMap != null && kvMap.key == id) {
            return kvMap.value
        }
        return 0
    }

    override fun add(id: String?, count: Int?) {
        value = KVMap(id, count)
    }

    override fun remove(id: String?) {
        val kvMap: KVMap<String?, Int?>? = value
        if (kvMap != null && kvMap.key == id) {
            value = defaultValue
        }
    }

    override fun contains(id: String?): Boolean {
        val kvMap: KVMap<String?, Int?>? = value
        return kvMap != null && kvMap.key == id
    }

    fun interface SelectListFunc {
        fun getList(): List<*>
    }
}

// 保持与 Java 版本一致：List<?> 到 List<? extends MapperEntity> 的泛型擦除转换，无运行时检查
@Suppress("UNCHECKED_CAST")
private fun castList(value: Any?): List<out MapperEntity>? = value as List<out MapperEntity>?
