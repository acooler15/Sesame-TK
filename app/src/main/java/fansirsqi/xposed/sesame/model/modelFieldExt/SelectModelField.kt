package fansirsqi.xposed.sesame.model.modelFieldExt

import com.fasterxml.jackson.annotation.JsonIgnore
import org.json.JSONException
import fansirsqi.xposed.sesame.model.ModelField
import fansirsqi.xposed.sesame.model.ModelFieldViewData
import fansirsqi.xposed.sesame.model.SelectModelFieldFunc
import fansirsqi.xposed.sesame.entity.MapperEntity

/**
 * 数据结构说明
 * Set<String> 表示已选择的数据
 * List<? extends IdAndName> 需要选择的数据
 */
class SelectModelField : ModelField<MutableSet<String?>>, SelectModelFieldFunc {
    private var selectListFunc: SelectListFunc? = null
    private var expandList: List<MapperEntity>? = null

    constructor(code: String?, name: String?, value: MutableSet<String?>, expandValue: List<MapperEntity>?) : super(code, name, value) {
        this.expandList = expandValue
    }

    constructor(code: String?, name: String?, value: MutableSet<String?>, selectListFunc: SelectListFunc?) : super(code, name, value) {
        this.selectListFunc = selectListFunc
    }

    constructor(code: String?, name: String?, value: MutableSet<String?>, expandValue: List<MapperEntity>?, desc: String?) : super(code, name, value, desc) {
        this.expandList = expandValue
    }

    constructor(code: String?, name: String?, value: MutableSet<String?>, selectListFunc: SelectListFunc?, desc: String?) : super(code, name, value, desc) {
        this.selectListFunc = selectListFunc
    }

    override val type: String
        get() = "SELECT"

    override val expandValue: List<MapperEntity>?
        @Throws(JSONException::class)
        get() = castList(selectListFunc?.getList() ?: expandList)

    @get:JsonIgnore
    override val viewData: ModelFieldViewData
        get() = super.viewData.copy(clickAction = ModelFieldViewData.ClickAction.LIST)

    override fun clear() {
        value.clear()
    }

    override fun get(id: String?): Int? {
        return 0
    }

    override fun add(id: String?, count: Int?) {
        value.add(id)
    }

    override fun remove(id: String?) {
        value.remove(id)
    }

    override fun contains(id: String?): Boolean {
        return value.contains(id)
    }

    fun interface SelectListFunc {
        @Throws(JSONException::class)
        fun getList(): List<*>
    }
}

// 保持与 Java 版本一致：List<?> 到 List<? extends MapperEntity> 的泛型擦除转换，无运行时检查
@Suppress("UNCHECKED_CAST")
private fun castList(value: Any?): List<MapperEntity>? = value as List<MapperEntity>?
