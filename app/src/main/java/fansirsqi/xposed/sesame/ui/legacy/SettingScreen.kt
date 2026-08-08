package fansirsqi.xposed.sesame.ui.legacy

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fansirsqi.xposed.sesame.R
import fansirsqi.xposed.sesame.model.Model
import fansirsqi.xposed.sesame.model.ModelConfig
import fansirsqi.xposed.sesame.model.ModelField
import fansirsqi.xposed.sesame.model.ModelFieldViewData
import fansirsqi.xposed.sesame.model.modelFieldExt.ChoiceModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.EmptyModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectAndCountModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectAndCountOneModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectOneModelField
import fansirsqi.xposed.sesame.ui.compose.CommonAlertDialog
import fansirsqi.xposed.sesame.ui.extension.openUrl
import fansirsqi.xposed.sesame.ui.widget.ChoiceDialog
import fansirsqi.xposed.sesame.ui.widget.ListDialog
import fansirsqi.xposed.sesame.ui.widget.StringDialog
import fansirsqi.xposed.sesame.core.notify.ToastUtil
import org.json.JSONException

/**
 * Compose 配置页：顶部菜单 + Tab 导航 + 字段列表，交互逻辑与旧版 View 实现一致
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(
    title: String,
    onMenuAction: (Int) -> Unit,
) {
    val context = LocalContext.current
    val configs = remember { Model.getModelConfigMap().values.toList() }
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var confirmField by remember { mutableStateOf<EmptyModelField?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "菜单")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            // 菜单项顺序与原 onCreateOptionsMenu 一致
                            val menuItems = listOf(
                                "导出配置", "导入配置", "删除配置", "单向好友", "切换WEBUI", "保存", "复制ID"
                            )
                            menuItems.forEachIndexed { index, menuText ->
                                DropdownMenuItem(
                                    text = { Text(menuText) },
                                    onClick = {
                                        menuExpanded = false
                                        if (index + 1 == 3) {
                                            // 删除配置需先确认
                                            showDeleteDialog = true
                                        } else {
                                            onMenuAction(index + 1)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (configs.isEmpty()) {
                Text(
                    text = "暂无配置项",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.outline
                )
                return@Column
            }
            val safeIndex = selectedIndex.coerceIn(0, configs.size - 1)
            // Tab 导航，选中高亮语义与旧 TabAdapter 一致
            PrimaryScrollableTabRow(selectedTabIndex = safeIndex, edgePadding = 8.dp) {
                configs.forEachIndexed { index, config ->
                    Tab(
                        selected = index == safeIndex,
                        onClick = { selectedIndex = index },
                        text = { Text(config.name ?: "") }
                    )
                }
            }
            val currentConfig = configs[safeIndex]
            val fields = remember(currentConfig) { currentConfig.fields.values.toList() }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(fields) { modelField ->
                    FieldItem(
                        modelField = modelField,
                        onConfirm = { confirmField = it }
                    )
                }
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }

    // 删除配置确认对话框，行为与原菜单项 3 一致
    if (showDeleteDialog) {
        CommonAlertDialog(
            showDialog = true,
            onDismissRequest = { showDeleteDialog = false },
            onConfirm = { onMenuAction(3) },
            title = "警告",
            text = "确认删除该配置？",
            confirmText = context.getString(R.string.ok),
            dismissText = context.getString(R.string.cancel)
        )
    }
    // 字段确认执行对话框，行为与原 EMPTY 字段 clickRunner 确认一致
    confirmField?.let { field ->
        CommonAlertDialog(
            showDialog = true,
            onDismissRequest = { confirmField = null },
            onConfirm = { field.clickRunner?.run() },
            title = "警告",
            text = "确认执行该操作？",
            confirmText = context.getString(R.string.ok),
            dismissText = context.getString(R.string.cancel)
        )
    }
}

/**
 * 渲染单个配置项：SWITCH 渲染开关行，其余类型渲染按钮行
 */
@Composable
private fun FieldItem(
    modelField: ModelField<*>,
    onConfirm: (EmptyModelField) -> Unit,
) {
    val viewData = modelField.viewData
    if (viewData.clickAction == ModelFieldViewData.ClickAction.SWITCH) {
        var checked by remember(modelField) { mutableStateOf(modelField.value as? Boolean ?: false) }
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = viewData.name, style = MaterialTheme.typography.titleMedium)
                    viewData.desc?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Switch(
                    checked = checked,
                    onCheckedChange = {
                        checked = it
                        modelField.setObjectValue(it)
                    }
                )
            }
        }
    } else {
        val context = LocalContext.current
        Surface(
            onClick = { performFieldAction(context, modelField, onConfirm) },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(text = viewData.name, style = MaterialTheme.typography.titleMedium)
                viewData.desc?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

/**
 * 字段点击交互，与 ContentPagerAdapter.createFieldView 的绑定逻辑一一对应
 */
private fun performFieldAction(
    context: Context,
    modelField: ModelField<*>,
    onConfirm: (EmptyModelField) -> Unit,
) {
    val viewData = modelField.viewData
    when (viewData.clickAction) {
        ModelFieldViewData.ClickAction.EDIT -> {
            StringDialog.showEditDialog(context, viewData.name, modelField)
        }
        ModelFieldViewData.ClickAction.READ -> {
            StringDialog.showReadDialog(context, viewData.name, modelField)
        }
        ModelFieldViewData.ClickAction.CHOICE -> {
            ChoiceDialog.show(context, viewData.name, modelField as ChoiceModelField)
        }
        ModelFieldViewData.ClickAction.LIST -> {
            when (modelField) {
                is SelectModelField -> {
                    try {
                        ListDialog.show(context, viewData.name, modelField)
                    } catch (e: JSONException) {
                        throw RuntimeException(e)
                    }
                }
                is SelectAndCountModelField -> {
                    ListDialog.show(context, viewData.name, modelField)
                }
                is SelectOneModelField -> {
                    ListDialog.show(context, viewData.name, modelField, ListDialog.ListType.RADIO)
                }
                is SelectAndCountOneModelField -> {
                    ListDialog.show(context, viewData.name, modelField, ListDialog.ListType.RADIO)
                }
                else -> ToastUtil.makeText(context, "无配置项", Toast.LENGTH_SHORT).show()
            }
        }
        ModelFieldViewData.ClickAction.URL -> {
            context.openUrl(modelField.configValue)
        }
        ModelFieldViewData.ClickAction.CONFIRM -> {
            onConfirm(modelField as EmptyModelField)
        }
        ModelFieldViewData.ClickAction.TOAST -> {
            ToastUtil.makeText(context, "无配置项", Toast.LENGTH_SHORT).show()
        }
        ModelFieldViewData.ClickAction.SWITCH -> Unit
    }
}
