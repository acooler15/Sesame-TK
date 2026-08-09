package fansirsqi.xposed.sesame.ui.widget

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fansirsqi.xposed.sesame.R
import fansirsqi.xposed.sesame.model.modelFieldExt.ChoiceModelField
import fansirsqi.xposed.sesame.ui.theme.AppTheme
import fansirsqi.xposed.sesame.ui.theme.ThemeManager

object ChoiceDialog {

    /**
     * 显示单选对话框（Compose Material3 风格）
     *
     * @param context          当前上下文，用于构建对话框
     * @param title            对话框的标题
     * @param choiceModelField 包含选项数据的 ChoiceModelField 对象
     */
    fun show(context: Context, title: CharSequence, choiceModelField: ChoiceModelField) {
        // 通过 ComposeView 桥接：需要 Activity 获取根视图承载 Compose 内容
        val activity = context as? Activity ?: return
        val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
        val composeView = ComposeView(context)
        composeView.setContent {
            val isDynamicColor by ThemeManager.isDynamicColor.collectAsStateWithLifecycle()
            AppTheme(dynamicColor = isDynamicColor) {
                ChoiceDialogContent(
                    title = title,
                    choiceModelField = choiceModelField,
                    onDismiss = { rootView.post { rootView.removeView(composeView) } }
                )
            }
        }
        rootView.addView(composeView)
    }

    @Composable
    private fun ChoiceDialogContent(
        title: CharSequence,
        choiceModelField: ChoiceModelField,
        onDismiss: () -> Unit
    ) {
        // 初始选中项为当前保存的值
        var selectedIndex by remember { mutableStateOf(choiceModelField.value) }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(text = title.toString(), style = MaterialTheme.typography.titleLarge)
            },
            text = {
                // 单选列表：点击选项即设置值（与原 setSingleChoiceItems 逻辑一致）
                // 显式 Column 包裹：确保选项垂直排列，避免依赖 AlertDialog text 容器的默认方向
                Column(modifier = Modifier.fillMaxWidth()) {
                    choiceModelField.expandKey?.forEachIndexed { index, option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedIndex = index
                                    choiceModelField.setObjectValue(index)
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedIndex == index,
                                onClick = {
                                    selectedIndex = index
                                    choiceModelField.setObjectValue(index)
                                }
                            )
                            Text(
                                text = option ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.ok))
                }
            }
        )
    }
}
