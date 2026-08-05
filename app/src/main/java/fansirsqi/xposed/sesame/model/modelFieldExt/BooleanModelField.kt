package fansirsqi.xposed.sesame.model.modelFieldExt

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Switch
import fansirsqi.xposed.sesame.R
import fansirsqi.xposed.sesame.model.ModelField

class BooleanModelField(code: String?, name: String?, value: Boolean) : ModelField<Boolean>(code, name, value) {

    override val type: String
        get() = "BOOLEAN"

    override fun getView(context: Context): View {
        @SuppressLint("UseSwitchCompatOrMaterialCode") val sw = Switch(context) // 创建 Switch 控件
        sw.text = name // 设置 Switch 的文本为字段名称
        sw.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) // 设置布局参数
        sw.minHeight = 150 // 设置最小高度
        sw.maxHeight = 180 // 设置最大高度
        sw.setPaddingRelative(40, 0, 40, 0) // 设置左右内边距
        sw.isChecked = getValue() // 根据字段值设置 Switch 的选中状态
        // 设置按钮和轨道样式
//        sw.setThumbResource(R.drawable.switch_thumb)
        sw.setTrackResource(R.drawable.switch_track)
        // 设置点击监听器，更新字段值
        sw.setOnClickListener { v -> setObjectValue((v as Switch).isChecked) }
        return sw // 返回创建的 Switch 视图
    }
}
