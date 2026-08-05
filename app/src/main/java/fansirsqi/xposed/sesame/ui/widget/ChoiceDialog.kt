package fansirsqi.xposed.sesame.ui.widget

import android.content.Context
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import fansirsqi.xposed.sesame.R
import fansirsqi.xposed.sesame.model.modelFieldExt.ChoiceModelField

object ChoiceDialog {

    /**
     * 显示单选对话框（Material3 风格）
     *
     * @param context          当前上下文，用于构建对话框
     * @param title            对话框的标题
     * @param choiceModelField 包含选项数据的 ChoiceModelField 对象
     */
    fun show(context: Context, title: CharSequence, choiceModelField: ChoiceModelField) {
        // 使用 Material3 对话框构造器
        val dialog: AlertDialog = MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setSingleChoiceItems(choiceModelField.expandKey, choiceModelField.value) { _, p2 ->
                choiceModelField.setObjectValue(p2)
            }
            .setPositiveButton(context.getString(R.string.ok), null)
            .create()

        // 设置确认按钮颜色
        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            if (positiveButton != null) {
                positiveButton.setTextColor(ContextCompat.getColor(context, R.color.selection_color))
            }
        }
        dialog.show()
    }

    // 注意：PriorityModelField 相关的方法已被移除，因为现在使用简单的 BooleanModelField 开关模式
}
