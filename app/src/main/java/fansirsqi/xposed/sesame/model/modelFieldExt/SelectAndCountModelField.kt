package fansirsqi.xposed.sesame.model.modelFieldExt

import com.fasterxml.jackson.annotation.JsonIgnore
import fansirsqi.xposed.sesame.entity.MapperEntity
import fansirsqi.xposed.sesame.model.ModelField
import fansirsqi.xposed.sesame.model.ModelFieldViewData
import fansirsqi.xposed.sesame.model.SelectModelFieldFunc

/**
 * 数据结构说明
 * Map<String, Integer> 表示已选择的数据与已经设置的数量映射关系
 * List<? extends IdAndName> 需要选择的数据
 */
class SelectAndCountModelField : ModelField<MutableMap<String?, Int?>>, SelectModelFieldFunc {
    private var selectListFunc: SelectListFunc? = null
    private var expandList: List<out MapperEntity>? = null

    constructor(code: String?, name: String?, value: MutableMap<String?, Int?>, expandValue: List<out MapperEntity>?) : super(code, name, value) {
        this.expandList = expandValue
    }

    constructor(code: String?, name: String?, value: MutableMap<String?, Int?>, selectListFunc: SelectListFunc?) : super(code, name, value) {
        this.selectListFunc = selectListFunc
    }

    constructor(code: String?, name: String?, value: MutableMap<String?, Int?>, expandValue: List<out MapperEntity>?, desc: String?) : super(code, name, value, desc) {
        this.expandList = expandValue
    }

    constructor(code: String?, name: String?, value: MutableMap<String?, Int?>, selectListFunc: SelectListFunc?, desc: String?) : super(code, name, value, desc) {
        this.selectListFunc = selectListFunc
    }

    override val type: String
        get() = "SELECT_AND_COUNT"

    override val expandValue: List<out MapperEntity>?
        get() = castList(selectListFunc?.getList() ?: expandList)

    @get:JsonIgnore
    override val viewData: ModelFieldViewData
        get() = super.viewData.copy(clickAction = ModelFieldViewData.ClickAction.LIST)

    override fun clear() {
        value.clear()
    }

    override fun get(id: String?): Int? {
        return value[id]
    }

    override fun add(id: String?, count: Int?) {
        value[id] = count
    }

    override fun remove(id: String?) {
        value.remove(id)
    }

    override fun contains(id: String?): Boolean {
        return value.containsKey(id)
    }

    fun interface SelectListFunc {
        fun getList(): List<*>
    }
}

// 保持与 Java 版本一致：List<?> 到 List<? extends MapperEntity> 的泛型擦除转换，无运行时检查
@Suppress("UNCHECKED_CAST")
private fun castList(value: Any?): List<out MapperEntity>? = value as List<out MapperEntity>?
