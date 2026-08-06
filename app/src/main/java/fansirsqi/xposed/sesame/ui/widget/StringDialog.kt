package fansirsqi.xposed.sesame.ui.widget

import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.text.InputType
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import fansirsqi.xposed.sesame.R
import fansirsqi.xposed.sesame.model.ModelField
import fansirsqi.xposed.sesame.core.log.Log

/**
 * 字符串对话框工具类。
 * 提供了显示编辑对话框和读取对话框的静态方法。
 */
object StringDialog {
    private var modelField: ModelField<*>? = null

    fun showEditDialog(c: Context, title: CharSequence, modelField: ModelField<*>) {
        StringDialog.modelField = modelField
        val editDialog = getEditDialog(c)
        editDialog.setTitle(title)
        editDialog.show()
    }

    private fun getEditDialog(c: Context): AlertDialog {
        val edt = EditText(c)
        val editDialog = MaterialAlertDialogBuilder(c)
            .setTitle("title")
            .setView(edt)
            .setPositiveButton(c.getString(R.string.ok)) { _, _ ->
                try {
                    val text = edt.text
                    if (text.toString().isEmpty()) {
                        modelField!!.setConfigValue(null)
                    } else {
                        modelField!!.setConfigValue(text.toString())
                    }
                } catch (e: Throwable) {
                    Log.printStackTrace(e)
                }
            }
            .setNegativeButton(c.getString(R.string.cancel)) { dialog, _ -> dialog.dismiss() }
            .create()

        editDialog.setOnShowListener {
            val positiveButton = editDialog.getButton(DialogInterface.BUTTON_POSITIVE)
            if (positiveButton != null) {
                positiveButton.setTextColor(ContextCompat.getColor(c, R.color.selection_color))
            }
        }

        edt.setText(modelField!!.configValue.toString())
        return editDialog
    }

    fun showReadDialog(c: Context, title: CharSequence, modelField: ModelField<*>) {
        showReadDialog(c, title, modelField, null)
    }

    fun showReadDialog(c: Context, title: CharSequence, modelField: ModelField<*>, msg: String?) {
        StringDialog.modelField = modelField
        val readDialog = getReadDialog(c)
        if (msg != null) {
            readDialog.setMessage(msg)
        }
        readDialog.setTitle(title)
        readDialog.show()
    }

    private fun getReadDialog(c: Context): AlertDialog {
        val edt = EditText(c)
        edt.setInputType(InputType.TYPE_NULL)
        edt.setTextColor(Color.GRAY)
        edt.setText(modelField!!.configValue.toString())
        return MaterialAlertDialogBuilder(c)
            .setTitle("读取")
            .setView(edt)
            .setPositiveButton(c.getString(R.string.ok)) { dialog, _ -> dialog.dismiss() }
            .create()
    }

    fun showAlertDialog(c: Context, title: String, msg: String, positiveButton: String) {
        val parsedMsg = HtmlCompat.fromHtml(msg, HtmlCompat.FROM_HTML_MODE_LEGACY)

        val alertDialog = MaterialAlertDialogBuilder(c)
            .setTitle(title)
            .setMessage(parsedMsg)
            .setPositiveButton(positiveButton) { dialog, _ -> dialog.dismiss() }
            .create()

        alertDialog.show()

        val button = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE)
        if (button != null) {
            button.setTextColor(ContextCompat.getColor(c, R.color.textColorPrimary))
        }
    }

    fun showSelectionDialog(
        c: Context, title: String, items: Array<CharSequence>,
        onItemClick: DialogInterface.OnClickListener,
        positiveButton: String, onDismiss: DialogInterface.OnDismissListener
    ): AlertDialog {
        val alertDialog = MaterialAlertDialogBuilder(c)
            .setTitle(title)
            .setItems(items, onItemClick)
            .setOnDismissListener(onDismiss)
            .setPositiveButton(positiveButton) { dialog, _ -> dialog.dismiss() }
            .create()

        alertDialog.show()

        val button = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE)
        if (button != null) {
            button.setTextColor(ContextCompat.getColor(c, R.color.selection_color))
        }

        return alertDialog
    }
}
