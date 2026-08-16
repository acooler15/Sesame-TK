package fansirsqi.xposed.sesame.ui.widget

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fansirsqi.xposed.sesame.core.log.Log
import fansirsqi.xposed.sesame.entity.CooperateEntity
import fansirsqi.xposed.sesame.entity.MapperEntity
import fansirsqi.xposed.sesame.model.SelectModelFieldFunc
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectAndCountModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectAndCountOneModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectOneModelField
import fansirsqi.xposed.sesame.ui.theme.AppTheme
import fansirsqi.xposed.sesame.ui.theme.ThemeManager
import kotlinx.coroutines.launch
import org.json.JSONException
import kotlin.math.max

/**
 * 多选/单选/只读列表对话框（Compose 化）。
 * 通过 ComposeView 桥接弹出，内部使用 Compose AlertDialog + LazyColumn 替代原
 * Material 对话框 + 列表适配器 + 旧布局文件的实现。
 */
class ListDialog {

    enum class ListType {
        RADIO, CHECK, SHOW
    }

    companion object {

        fun show(c: Context, title: CharSequence, selectModelField: SelectOneModelField, listType: ListType) {
            show(c, title, selectModelField.expandValue, selectModelField, false, listType)
        }

        fun show(c: Context, title: CharSequence, selectModelField: SelectAndCountOneModelField, listType: ListType) {
            show(c, title, selectModelField.expandValue, selectModelField, false, listType)
        }

        @Throws(JSONException::class)
        fun show(c: Context, title: CharSequence, selectModelField: SelectModelField, onDismiss: (() -> Unit)? = null) {
            show(c, title, selectModelField, ListType.CHECK, onDismiss)
        }

        fun show(c: Context, title: CharSequence, selectModelField: SelectAndCountModelField) {
            show(c, title, selectModelField, ListType.CHECK)
        }

        @Throws(JSONException::class)
        fun show(c: Context, title: CharSequence, selectModelField: SelectModelField, listType: ListType, onDismiss: (() -> Unit)? = null) {
            show(c, title, selectModelField.expandValue, selectModelField, false, listType, onDismiss)
        }

        fun show(c: Context, title: CharSequence, selectModelField: SelectAndCountModelField, listType: ListType) {
            show(c, title, selectModelField.expandValue, selectModelField, true, listType)
        }

        fun show(c: Context, title: CharSequence, bl: List<MapperEntity>?, selectModelFieldFunc: SelectModelFieldFunc, hasCount: Boolean) {
            show(c, title, bl, selectModelFieldFunc, hasCount, ListType.CHECK)
        }

        /**
         * 核心实现：通过 ComposeView 桥接弹出 Compose 对话框。
         *
         * @param onDismiss 对话框关闭（任意方式）时回调
         */
        fun show(c: Context, title: CharSequence, bl: List<MapperEntity>?, selectModelFieldFunc: SelectModelFieldFunc, hasCount: Boolean, listType: ListType, onDismiss: (() -> Unit)? = null) {
            // 通过 ComposeView 桥接：需要 Activity 获取根视图承载 Compose 内容
            val activity = c as? Activity ?: return
            val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
            val composeView = ComposeView(c)
            composeView.setContent {
                val isDynamicColor by ThemeManager.isDynamicColor.collectAsStateWithLifecycle()
                AppTheme(dynamicColor = isDynamicColor) {
                    ListDialogContent(
                        title = title,
                        bl = bl,
                        selectModelFieldFunc = selectModelFieldFunc,
                        hasCount = hasCount,
                        listType = listType,
                        onDismiss = {
                            rootView.post { rootView.removeView(composeView) }
                            onDismiss?.invoke()
                        }
                    )
                }
            }
            rootView.addView(composeView)
        }

        @Composable
        private fun ListDialogContent(
            title: CharSequence,
            bl: List<MapperEntity>?,
            selectModelFieldFunc: SelectModelFieldFunc,
            hasCount: Boolean,
            listType: ListType,
            onDismiss: () -> Unit
        ) {
            // 等价原列表适配器 setSelectedList：选中的排前，其余按拼音排序（稳定排序）
            val sortedList = remember(bl, selectModelFieldFunc) {
                val base = bl ?: emptyList()
                try {
                    base.sortedWith { o1, o2 ->
                        val contains1 = selectModelFieldFunc.contains(o1.id) == true
                        val contains2 = selectModelFieldFunc.contains(o2.id) == true
                        when {
                            contains1 == contains2 -> o1.compareTo(o2)
                            contains1 -> -1
                            else -> 1
                        }
                    }
                } catch (e: Exception) {
                    Log.record(TAG, "列表排序错误")
                    Log.printStackTrace(e)
                    base
                }
            }

            // 选中状态映射：从 selectModelFieldFunc 初始化，变更时同步写回（等价 CheckBox 状态 + notifyDataSetChanged）
            val selectedMap = remember {
                mutableStateMapOf<String, Int>().apply {
                    sortedList.forEach { item ->
                        if (selectModelFieldFunc.contains(item.id) == true) {
                            put(item.id, selectModelFieldFunc.get(item.id) ?: 0)
                        }
                    }
                }
            }

            // RADIO 模式当前选中项 id
            val radioSelectedId = remember {
                mutableStateOf(sortedList.firstOrNull { selectModelFieldFunc.contains(it.id) == true }?.id)
            }

            val listState: LazyListState = rememberLazyListState()
            val scope = rememberCoroutineScope()
            val context = LocalContext.current

            var searchText by remember { mutableStateOf("") }
            // 搜索定位状态，等价原适配器的 findIndex/findWord
            var findIndex by remember { mutableStateOf(-1) }
            var findWord by remember { mutableStateOf<String?>(null) }

            // 计数模式嵌套对话框状态
            var countDialogItem by remember { mutableStateOf<MapperEntity?>(null) }
            var countText by remember { mutableStateOf("") }

            // 等价原适配器 findItem：从上次位置起循环查找下一个/上一个匹配项
            fun findItem(findThis: String, forward: Boolean): Int {
                if (sortedList.isEmpty()) return -1
                val word = findThis.lowercase()
                if (word != findWord) {
                    findIndex = -1
                    findWord = word
                }
                var current = max(findIndex, 0)
                val size = sortedList.size
                val start = current
                do {
                    current = if (forward) (current + 1) % size else (current - 1 + size) % size
                    if (sortedList[current].name.lowercase().contains(word)) {
                        findIndex = current
                        return findIndex
                    }
                } while (current != start)
                return -1
            }

            // 上一个/下一个：未搜到 Toast「未搜到」，搜到滚动定位（等价 lv_list.setSelection）
            fun search(forward: Boolean) {
                if (searchText.isEmpty()) return
                val index = findItem(searchText, forward)
                if (index < 0) {
                    Toast.makeText(context, "未搜到", Toast.LENGTH_SHORT).show()
                } else {
                    scope.launch { listState.scrollToItem(index) }
                }
            }

            // 列表项点击逻辑（等价原 setOnItemClickListener，hasCount == true 时弹出计数对话框）
            fun onItemClick(item: MapperEntity) {
                if (hasCount != true) {
                    if (listType == ListType.RADIO) {
                        selectModelFieldFunc.clear()
                        val wasSelected = selectedMap.containsKey(item.id)
                        selectedMap.clear()
                        if (wasSelected) {
                            radioSelectedId.value = null
                        } else {
                            selectModelFieldFunc.add(item.id, 0)
                            selectedMap[item.id] = 0
                            radioSelectedId.value = item.id
                        }
                    } else {
                        if (selectedMap.containsKey(item.id)) {
                            selectModelFieldFunc.remove(item.id)
                            selectedMap.remove(item.id)
                        } else {
                            if (selectModelFieldFunc.contains(item.id) != true) selectModelFieldFunc.add(item.id, 0)
                            selectedMap[item.id] = 0
                        }
                    }
                } else {
                    countDialogItem = item
                    val value = selectModelFieldFunc.get(item.id)
                    countText = if (value != null && value >= 0) value.toString() else ""
                }
            }

            AlertDialog(
                onDismissRequest = onDismiss,
                title = {
                    Text(text = title.toString(), style = MaterialTheme.typography.titleLarge)
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // 批量操作按钮：仅 CHECK 且非计数模式显示（等价 layout_batch_process 可见性）
                        if (listType == ListType.CHECK && hasCount != true) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                TextButton(onClick = {
                                    // 等价原适配器 selectAll()
                                    selectModelFieldFunc.clear()
                                    selectedMap.clear()
                                    sortedList.forEach { item ->
                                        selectModelFieldFunc.add(item.id, 0)
                                        selectedMap[item.id] = 0
                                    }
                                }) {
                                    Text(text = "全选")
                                }
                                TextButton(onClick = {
                                    // 等价原适配器 SelectInvert()
                                    sortedList.forEach { item ->
                                        if (selectedMap.containsKey(item.id)) {
                                            selectModelFieldFunc.remove(item.id)
                                            selectedMap.remove(item.id)
                                        } else {
                                            selectModelFieldFunc.add(item.id, 0)
                                            selectedMap[item.id] = 0
                                        }
                                    }
                                }) {
                                    Text(text = "反选")
                                }
                            }
                        }

                        // 列表：RADIO → RadioButton；CHECK → Checkbox；SHOW → 无选择控件且点击不响应
                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                itemsIndexed(sortedList) { index, item ->
                                    val isClickable = listType != ListType.SHOW
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .then(if (isClickable) Modifier.clickable { onItemClick(item) } else Modifier)
                                            .padding(vertical = 8.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        when (listType) {
                                            ListType.RADIO -> RadioButton(
                                                selected = radioSelectedId.value == item.id,
                                                onClick = null
                                            )
                                            ListType.CHECK -> Checkbox(
                                                checked = selectedMap.containsKey(item.id),
                                                onCheckedChange = null
                                            )
                                            ListType.SHOW -> Unit
                                        }
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            // 搜索命中的项高亮（等价原适配器中 findIndex == position 显示红色）
                                            color = if (findIndex == index) Color.Red else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(start = 12.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 搜索行：输入框 + 上一个/下一个
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = searchText,
                                onValueChange = { searchText = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                placeholder = { Text(text = "搜索") }
                            )
                            IconButton(onClick = { search(false) }) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "上一个"
                                )
                            }
                            IconButton(onClick = { search(true) }) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "下一个"
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text(text = "关闭")
                    }
                }
            )

            // 计数模式嵌套对话框（等价原 EditText + Material 对话框）
            countDialogItem?.let { item ->
                AlertDialog(
                    onDismissRequest = { countDialogItem = null },
                    title = {
                        Text(text = item.name, style = MaterialTheme.typography.titleLarge)
                    },
                    text = {
                        OutlinedTextField(
                            value = countText,
                            onValueChange = { countText = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = {
                                Text(text = if (item is CooperateEntity) "浇水克数" else "次数")
                            }
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            if (countText.isNotEmpty()) {
                                try {
                                    val count = countText.toInt()
                                    if (count > 0) {
                                        selectModelFieldFunc.add(item.id, count)
                                        selectedMap[item.id] = count
                                    } else {
                                        selectModelFieldFunc.remove(item.id)
                                        selectedMap.remove(item.id)
                                    }
                                } catch (ignored: Exception) {
                                    // 非数字输入忽略
                                }
                            }
                            countDialogItem = null
                        }) {
                            Text(text = "确认")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { countDialogItem = null }) {
                            Text(text = "取消")
                        }
                    }
                )
            }
        }

        private const val TAG = "ListDialog"
    }
}
