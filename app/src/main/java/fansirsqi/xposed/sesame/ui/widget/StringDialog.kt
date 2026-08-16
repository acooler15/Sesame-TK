package fansirsqi.xposed.sesame.ui.widget

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.model.ModelField
import fansirsqi.xposed.sesame.ui.extension.parseHtml
import fansirsqi.xposed.sesame.ui.theme.AppTheme
import fansirsqi.xposed.sesame.ui.theme.ThemeManager

/**
 * 字符串对话框工具类（Compose 化）。
 * 提供编辑、只读、HTML 展示、选择四种对话框，均通过 ComposeView 桥接弹出。
 */
object StringDialog {

    /**
     * 显示编辑对话框（TextField + 确定/取消）
     *
     * @param c          当前上下文，需为 Activity 才能弹出
     * @param title      对话框标题
     * @param modelField 编辑目标，确定时将输入内容写回其配置值
     */
    fun showEditDialog(c: Context, title: CharSequence, modelField: ModelField<*>) {
        // 通过 ComposeView 桥接：需要 Activity 获取根视图承载 Compose 内容
        val activity = c as? Activity ?: return
        val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
        val composeView = ComposeView(c)
        composeView.setContent {
            val isDynamicColor by ThemeManager.isDynamicColor.collectAsStateWithLifecycle()
            AppTheme(dynamicColor = isDynamicColor) {
                EditDialogContent(
                    title = title,
                    modelField = modelField,
                    onDismiss = { rootView.post { rootView.removeView(composeView) } }
                )
            }
        }
        rootView.addView(composeView)
    }

    @Composable
    private fun EditDialogContent(
        title: CharSequence,
        modelField: ModelField<*>,
        onDismiss: () -> Unit
    ) {
        // 初始值绑定当前配置值（与原 EditText.setText 一致）
        var text by remember { mutableStateOf(modelField.configValue) }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(text = title.toString(), style = MaterialTheme.typography.titleLarge)
            },
            text = {
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    try {
                        if (text.isEmpty()) {
                            modelField.setConfigValue(null)
                        } else {
                            modelField.setConfigValue(text)
                        }
                    } catch (e: Throwable) {
                        Log.printStackTrace(e)
                    }
                    onDismiss()
                }) {
                    Text(text = "确认")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(text = "取消")
                }
            }
        )
    }

    /**
     * 显示只读对话框（等价 InputType.TYPE_NULL，无输入框）
     */
    fun showReadDialog(c: Context, title: CharSequence, modelField: ModelField<*>) {
        showReadDialog(c, title, modelField, null)
    }

    /**
     * 显示只读对话框，msg 非空时在配置值上方附加展示
     */
    fun showReadDialog(c: Context, title: CharSequence, modelField: ModelField<*>, msg: String?) {
        // 通过 ComposeView 桥接：需要 Activity 获取根视图承载 Compose 内容
        val activity = c as? Activity ?: return
        val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
        val composeView = ComposeView(c)
        composeView.setContent {
            val isDynamicColor by ThemeManager.isDynamicColor.collectAsStateWithLifecycle()
            AppTheme(dynamicColor = isDynamicColor) {
                ReadDialogContent(
                    title = title,
                    modelField = modelField,
                    msg = msg,
                    onDismiss = { rootView.post { rootView.removeView(composeView) } }
                )
            }
        }
        rootView.addView(composeView)
    }

    @Composable
    private fun ReadDialogContent(
        title: CharSequence,
        modelField: ModelField<*>,
        msg: String?,
        onDismiss: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(text = title.toString(), style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Column {
                    if (msg != null) {
                        Text(text = msg, style = MaterialTheme.typography.bodyMedium)
                    }
                    // 灰色弱化展示，等价原 edt.setTextColor(Color.GRAY)
                    Text(
                        text = modelField.configValue,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(text = "确认")
                }
            }
        )
    }

    /**
     * 显示带 HTML 内容的提示对话框（复用 parseHtml 扩展保留富文本语义）
     */
    fun showAlertDialog(c: Context, title: String, msg: String, positiveButton: String) {
        // 通过 ComposeView 桥接：需要 Activity 获取根视图承载 Compose 内容
        val activity = c as? Activity ?: return
        val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
        val composeView = ComposeView(c)
        composeView.setContent {
            val isDynamicColor by ThemeManager.isDynamicColor.collectAsStateWithLifecycle()
            AppTheme(dynamicColor = isDynamicColor) {
                AlertDialogContent(
                    title = title,
                    msg = msg,
                    positiveButton = positiveButton,
                    onDismiss = { rootView.post { rootView.removeView(composeView) } }
                )
            }
        }
        rootView.addView(composeView)
    }

    @Composable
    private fun AlertDialogContent(
        title: String,
        msg: String,
        positiveButton: String,
        onDismiss: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(text = title, style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Text(
                    text = msg.parseHtml(),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(text = positiveButton)
                }
            }
        )
    }

    /**
     * 显示选择对话框（Column + 可点击行列表模拟原 setItems 行为）
     *
     * 点击某项时回调 [onItemClick] 并关闭对话框；任意方式关闭时回调 [onDismiss]。
     */
    fun showSelectionDialog(
        c: Context, title: String, items: Array<CharSequence>,
        onItemClick: DialogInterface.OnClickListener,
        positiveButton: String, onDismiss: DialogInterface.OnDismissListener
    ) {
        // 通过 ComposeView 桥接：需要 Activity 获取根视图承载 Compose 内容
        val activity = c as? Activity ?: return
        val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
        val composeView = ComposeView(c)
        composeView.setContent {
            val isDynamicColor by ThemeManager.isDynamicColor.collectAsStateWithLifecycle()
            AppTheme(dynamicColor = isDynamicColor) {
                SelectionDialogContent(
                    title = title,
                    items = items,
                    onItemClick = onItemClick,
                    positiveButton = positiveButton,
                    onDismiss = {
                        // 等价原 setOnDismissListener；Compose 无 DialogInterface，传 null
                        onDismiss.onDismiss(null)
                        rootView.post { rootView.removeView(composeView) }
                    }
                )
            }
        }
        rootView.addView(composeView)
    }

    @Composable
    private fun SelectionDialogContent(
        title: String,
        items: Array<CharSequence>,
        onItemClick: DialogInterface.OnClickListener,
        positiveButton: String,
        onDismiss: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(text = title, style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .heightIn(max = 360.dp)
                ) {
                    items.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onItemClick.onClick(null, index)
                                    onDismiss()
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp)
                        ) {
                            Text(
                                text = item.toString(),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(text = positiveButton)
                }
            }
        )
    }
}
