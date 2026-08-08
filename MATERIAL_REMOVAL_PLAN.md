# Sesame-TK Material 依赖移除计划

> 生成时间：2026-08-08
> 前置条件：OPTIMIZATION_PLAN.md Phase 6-12 全部完成（tag: phase-6-done ~ phase-12-done）
> 当前状态：Phase 13 已全部完成（tag: phase-13-done）——material 依赖已彻底移除，项目达成 Compose-only UI 栈（Compose Material3 保留）
> 目标：彻底移除 `com.google.android.material:material` 依赖，实现 Compose-only UI 栈

---

## 目录

1. [背景与依赖审计](#1-背景与依赖审计)
2. [分阶段任务](#2-分阶段任务)
3. [进度跟踪表](#3-进度跟踪表)

---

## 1. 背景与依赖审计

### 1.1 Material 依赖硬引用全景

`com.google.android.material:material`（build.gradle.kts:186, libs.versions.toml:18+63）存在以下硬引用：

| # | 文件 | 引用类型 | 具体 Material API |
|---|------|---------|-----------------|
| M1 | `res/values/styles.xml:4` | 主题 parent | `Theme.Material3.DayNight.NoActionBar` |
| M2 | `res/values/styles.xml:31` | 样式 parent（死代码） | `Widget.Material3.Button`（`Widget.App.Button.Main` 0 布局引用） |
| M3 | `ui/BaseActivity.kt:5` | import | `com.google.android.material.appbar.MaterialToolbar` |
| M4 | `res/layout/base_title.xml:9,15` | XML 视图 | `AppBarLayout` + `MaterialToolbar` |
| M5 | `res/layout/activity_web_settings.xml:7` | XML include | `<include layout="@layout/base_title" />`（传递 M4） |
| M6 | `ui/legacy/WebSettingsActivity.kt:61,101` | 继承+布局 | extends `BaseActivity` + `setContentView(R.layout.activity_web_settings)` |
| M7 | `ui/widget/ListDialog.kt:15,105,179` | import+调用 | `MaterialAlertDialogBuilder`（2 处） |
| M8 | `ui/widget/StringDialog.kt:11,32,80,90,109` | import+调用 | `MaterialAlertDialogBuilder`（4 处） |
| M9 | `ui/widget/ChoiceDialog.kt:7,22` | import+调用 | `MaterialAlertDialogBuilder`（1 处） |
| M10 | `res/layout/dialog_list.xml:14,26,63,74` | XML 视图 | 4× `MaterialButton` |

### 1.2 对话框调用方分布

| 调用方 | 类型 | 调用方法 | 说明 |
|--------|------|---------|------|
| `ui/legacy/SettingScreen.kt` | Compose Composable | `StringDialog.showEditDialog` / `showReadDialog` / `ChoiceDialog.show` / `ListDialog.show`（7 处） | 通过 `performFieldAction()` 从 `LocalContext.current` 调用 |
| `ui/legacy/SettingActivity.kt:132` | ComponentActivity + Compose | `ListDialog.show`（1 处） | 菜单项「单向好友列表」 |
| `ui/legacy/WebSettingsActivity.kt:437` | BaseActivity（传统 View） | `ListDialog.show`（1 处） | 菜单项「单向好友列表」 |
| `model/CustomSettings.kt:297-303` | 模型层（非 UI） | `ListDialog.show` + **反射**访问 `ListDialog.listDialog` 字段设 `OnDismissListener` | ⚠️ 反射耦合，迁移时需处理 |

### 1.3 关联依赖

| 文件 | 依赖 | 迁移后处置 |
|------|------|-----------|
| `ui/BaseActivity.kt` | `AppCompatActivity`（appcompat） | 删除文件（WebSettingsActivity 迁移后无消费者） |
| `ui/ExtendActivity.kt` | `AppCompatActivity`（appcompat） | 改为 `ComponentActivity`（已用 setContent + Compose） |
| `ui/adapter/ListAdapter.kt` | `BaseAdapter` + `R.layout.list_item` | 删除（ListDialog Compose 化后无消费者） |
| `res/layout/list_item.xml` | 纯 Android View（无 Material） | 删除（随 ListAdapter 删除） |
| `res/layout/dialog_list.xml` | 4× `MaterialButton` + `ListView` + `EditText` | 删除（ListDialog Compose 化后无消费者） |
| `res/layout/base_title.xml` | `AppBarLayout` + `MaterialToolbar` + `CoordinatorLayout` | 删除（WebSettingsActivity 迁移后无消费者） |
| `res/layout/activity_web_settings.xml` | `<include base_title>` + `WebView` | 删除（WebSettingsActivity 迁移后无消费者） |

### 1.4 约束

- **不改变业务逻辑**：对话框功能（单选/多选/搜索/全选/反选/计数输入）须完整保留
- **不改序列化格式**：Jackson 配置读写不受影响
- **不改包名/类名**：`ListDialog`/`StringDialog`/`ChoiceDialog` 的包路径和 `show()` 方法签名保持兼容，内部实现替换
- **编译通过即提交**：每个 Task 完成后 `./gradlew :app:compileDebugKotlin` 通过即提交
- **appcompat 暂不移除**：本计划仅移除 material 依赖；appcompat 移除作为后续独立 Phase

---

## 2. 分阶段任务

### Phase 13：Material 依赖移除

> 目标：将所有 `com.google.android.material.*` 引用替换为 Compose 等价物，最终移除 material 依赖
> 原则：渐进式替换，每个 Task 后编译通过；旧实现在新调用方全部切换后才删除

#### 13.1 删除死布局文件（已完成）

| Task | 标题 | 涉及文件 | 操作 | 前置 | 风险 | 验证 |
|------|------|---------|------|------|------|------|
| 13.1 | 删除无引用布局 | `res/layout/item_rpc_debug.xml`、`res/layout/item_extend_function.xml` | 全工程 grep 确认 0 引用后删除 | — | 低 | 编译通过 |

> 已于 2026-08-08 执行：两个文件 0 代码引用，`item_rpc_debug.xml` 已被 `RpcItemCard.kt`（Compose）替代。

#### 13.2 Compose 化 ChoiceDialog（最简单）

> `ChoiceDialog` 仅 42 行，单选对话框，逻辑清晰。

**当前实现**：`MaterialAlertDialogBuilder.setSingleChoiceItems()` → `ChoiceModelField.setObjectValue(index)`

**目标实现**：Compose `AlertDialog` + `RadioButton` 列表

| Task | 标题 | 涉及文件 | 操作 | 前置 | 风险 | 验证 |
|------|------|---------|------|------|------|------|
| 13.2 | Compose 化 ChoiceDialog | `ui/widget/ChoiceDialog.kt` | 将 `MaterialAlertDialogBuilder` 替换为 Compose `AlertDialog` + `RadioButton` 列表；保留 `show(context, title, choiceModelField)` 签名不变；通过 `ComposeView` 桥接（复用 `NativeComposeBridge` 模式：获取 Activity root ViewGroup → addView ComposeView → setContent { AppTheme { AlertDialog(...) } }）；对话框 dismiss 时 removeView | 13.1 | 低 | 编译通过 + 真机验证单选对话框 |
| 13.2-验证 | 调用方验证 | `ui/legacy/SettingScreen.kt:280` | 确认 `ChoiceDialog.show(context, viewData.name, modelField as ChoiceModelField)` 调用正常 | 13.2 | 低 | 真机验证配置页 Choice 字段点击 |

**实现要点**：

```
// 伪代码结构
fun show(context: Context, title: CharSequence, choiceModelField: ChoiceModelField) {
    val activity = context as? Activity ?: return
    val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
    val composeView = ComposeView(context)
    composeView.setContent {
        AppTheme {
            var selectedIndex by remember { mutableStateOf(choiceModelField.value) }
            AlertDialog(
                onDismissRequest = { rootView.post { rootView.removeView(composeView) } },
                title = { Text(title) },
                text = {
                    Column {
                        choiceModelField.expandKey.forEachIndexed { index, label ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = index == selectedIndex, onClick = {
                                    selectedIndex = index
                                    choiceModelField.setObjectValue(index)
                                })
                                Text(label)
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { rootView.post { rootView.removeView(composeView) } }) { Text("确定") } }
            )
        }
    }
    rootView.addView(composeView)
}
```

**桥接工具复用**：项目已有 `ui/extension/ComposeBridge.kt` 中 `NativeComposeBridge.showAlertDialog` 的 ComposeView 桥接模式，可直接参考。

#### 13.3 Compose 化 StringDialog

> `StringDialog` 126 行，包含 4 个方法：`showEditDialog`、`showReadDialog`、`showAlertDialog`、`showSelectionDialog`。

**当前实现**：`MaterialAlertDialogBuilder.setView(EditText)` 等

**目标实现**：Compose `AlertDialog` + `TextField` / `Text`

| Task | 标题 | 涉及文件 | 操作 | 前置 | 风险 | 验证 |
|------|------|---------|------|------|------|------|
| 13.3 | Compose 化 StringDialog | `ui/widget/StringDialog.kt` | 替换 4 个方法的 `MaterialAlertDialogBuilder` 为 Compose `AlertDialog`；`showEditDialog`→`TextField`+确认回调 `modelField.setConfigValue`；`showReadDialog`→只读 `Text`（`InputType.TYPE_NULL` 等价）；`showAlertDialog`→`Text` + HTML 解析（复用 `parseHtml()` 扩展）；`showSelectionDialog`→`AlertDialog` + `items` 列表；签名不变；桥接方式同 13.2 | 13.2 | 中 | 编译通过 + 真机验证编辑/只读/选择对话框 |
| 13.3-验证 | 调用方验证 | `ui/legacy/SettingScreen.kt:274,277` | 确认 `StringDialog.showEditDialog` / `showReadDialog` 调用正常 | 13.3 | 低 | 真机验证配置页 EDIT/READ 字段点击 |

**实现要点**：

- `showEditDialog`：`TextField` 的 `value` 绑定 `mutableStateOf(modelField.configValue.toString())`，确认时调 `modelField.setConfigValue(text)`
- `showReadDialog`：`Text(text = modelField.configValue.toString())`，只读不可编辑
- `showAlertDialog`：使用已有 `ui/extension/parseHtml` 扩展函数处理 HTML
- `showSelectionDialog`：`AlertDialog` 的 `text` 参数用 `Column` + `TextButton` 列表模拟 `setItems`

#### 13.4 Compose 化 ListDialog（最复杂）

> `ListDialog` 211 行，包含搜索、全选/反选、单选/多选/只读三种模式、计数输入子对话框。依赖 `ListAdapter`（BaseAdapter）+ `dialog_list.xml` + `list_item.xml`。

**当前实现**：
- `MaterialAlertDialogBuilder.setView(getListView(c))` → inflate `dialog_list.xml`
- `ListAdapter`（`BaseAdapter`）渲染 `list_item.xml`（TextView + CheckBox）
- 搜索：`EditText` + 上一个/下一个按钮
- 批量：全选/反选按钮
- 计数：点击项弹出嵌套 `MaterialAlertDialogBuilder` + `EditText` 输入次数

**目标实现**：Compose `AlertDialog` + `LazyColumn` + `Checkbox`/`RadioButton` + 搜索栏

| Task | 标题 | 涉及文件 | 操作 | 前置 | 风险 | 验证 |
|------|------|---------|------|------|------|------|
| 13.4a | Compose 化 ListDialog 主体 | `ui/widget/ListDialog.kt` | 替换 `MaterialAlertDialogBuilder` + `ListAdapter` + `dialog_list.xml` 为 Compose `AlertDialog` + `LazyColumn`；保留全部 `show()` 重载签名不变；搜索栏用 `TextField` + `IconButton`；列表项用 `Row` + `Checkbox`/`RadioButton`；计数输入用嵌套 Compose `AlertDialog` + `TextField` | 13.3 | 高 | 编译通过 + 真机验证列表/搜索/全选/反选/计数 |
| 13.4b | 处理 CustomSettings 反射耦合 | `model/CustomSettings.kt:297-303` | 当前代码通过反射 `ListDialog::class.java.getDeclaredField("listDialog")` 获取内部 `AlertDialog` 设 `OnDismissListener`；Compose 化后该字段不存在；改为在 `ListDialog.show()` 增加可选 `onDismiss: (() -> Unit)? = null` 参数，`CustomSettings` 传 `onDismiss = { save(uid) }` | 13.4a | 中 | 编译通过 + 真机验证黑名单设置保存 |
| 13.4c | 删除旧 ListAdapter 和布局 | `ui/adapter/ListAdapter.kt`、`res/layout/list_item.xml`、`res/layout/dialog_list.xml` | 确认 `ListAdapter` 无其他引用后删除整文件和两个布局 | 13.4b | 低 | 编译通过 |
| 13.4-验证 | 调用方验证 | `ui/legacy/SettingScreen.kt:286-298`（5 处）、`ui/legacy/SettingActivity.kt:132`、`ui/legacy/WebSettingsActivity.kt:437`、`model/CustomSettings.kt:297` | 确认全部 `ListDialog.show` 调用正常 | 13.4c | 中 | 真机验证各入口列表对话框 |

**实现要点**：

1. `ListDialog` 保持 `companion object` + 静态 `show()` 方法签名，内部改用 ComposeView 桥接
2. 状态管理：`var searchText by remember { mutableStateOf("") }`、`var selectedIds by remember { mutableStateOf(selectModelFieldFunc.toMap()) }`
3. 过滤逻辑：`val filteredList = baseList.filter { it.name.contains(searchText) }`
4. `ListType.RADIO` → `RadioButton`；`ListType.CHECK` → `Checkbox`；`ListType.SHOW` → 无选择控件
5. 计数模式（`hasCount == true`）：点击项弹出嵌套 `AlertDialog` + `TextField`（提示"浇水克数"或"次数"）
6. 新增 `show()` 重载参数 `onDismiss: (() -> Unit)? = null`

#### 13.5 迁移 WebSettingsActivity 至 Compose

> `WebSettingsActivity` 493 行，是 `BaseActivity` 的唯一消费者，也是最后一个使用传统 View 布局的 Activity。

**当前实现**：
- extends `BaseActivity()`（→ `AppCompatActivity` + `MaterialToolbar`）
- `setContentView(R.layout.activity_web_settings)` → `base_title.xml` + `WebView`
- `onCreateOptionsMenu` / `onOptionsItemSelected` → 传统菜单
- 菜单项调用 `ListDialog.show()`、`AlertDialog.Builder`（android.app）

**目标实现**：
- extends `ComponentActivity`
- `setContent { AppTheme { WebSettingsContent() } }`
- `WebView` 包装进 `AndroidView { }`
- 菜单 → Compose `Scaffold` + `TopAppBar` + `DropdownMenu`

| Task | 标题 | 涉及文件 | 操作 | 前置 | 风险 | 验证 |
|------|------|---------|------|------|------|------|
| 13.5a | WebSettingsActivity 迁移 Compose | `ui/legacy/WebSettingsActivity.kt` | `BaseActivity()`→`ComponentActivity()`；`setContentView`→`setContent { AppTheme { ... } }`；`WebView`→`AndroidView`；菜单→`Scaffold`+`TopAppBar`+`DropdownMenu`；`onBackPressedDispatcher` 保留；`exportLauncher`/`importLauncher` 保留；JS 接口 `WebViewCallback` 全部保留不变；`ListDialog.show` 已在 13.4 完成迁移；`AlertDialog.Builder(context)`（菜单项 3 删除配置）改用 Compose `AlertDialog` | 13.4 | 高 | 编译通过 + 真机验证 WebView 配置页全部功能 |
| 13.5b | 删除 BaseActivity 和旧布局 | `ui/BaseActivity.kt`、`res/layout/base_title.xml`、`res/layout/activity_web_settings.xml` | 确认 `BaseActivity` 无其他消费者（全工程仅 `WebSettingsActivity` 继承）后删除三文件 | 13.5a | 低 | 编译通过 |
| 13.5-验证 | 真机验证清单 | — | 1. WebView 配置页从模块入口打开（UiMode.Web）；2. 各 Tab 切换与字段编辑/保存；3. 菜单 7 项：导出/导入/删除配置/单向好友/切换 UI/保存/复制 ID；4. 返回键保存行为；5. 深色模式/动态取色 | 13.5b | — | 真机验证 |

**实现要点**：

1. `AndroidView` 包装 `WebView`：

```kotlin
AndroidView(
    factory = { ctx ->
        WebView(ctx).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            // ... 全部 WebSettings 配置保留
            webViewClient = object : WebViewClient() { /* 保留原逻辑 */ }
            addJavascriptInterface(WebViewCallback(), "HOOK")
            loadUrl("file:///android_asset/web/semi_index.html")
        }
    },
    modifier = Modifier.fillMaxSize()
)
```

2. 菜单项 3（删除配置）的 `AlertDialog.Builder` 改用 `CommonAlertDialog` 或 Compose `AlertDialog`
3. `WatermarkInjector.inject(this)` → `WatermarkLayer { ... }`（参考 `SettingActivity.kt` 模式）
4. `baseSubtitle` / `baseTitle` → `TopAppBar` 的 `title`/`subtitle`

#### 13.6 ExtendActivity 改为 ComponentActivity

> `ExtendActivity` 已使用 `setContent` + Compose，但仍继承 `AppCompatActivity`。改为 `ComponentActivity` 消除多余的 appcompat 继承（为后续 appcompat 移除铺路，本阶段仅改继承关系）。

| Task | 标题 | 涉及文件 | 操作 | 前置 | 风险 | 验证 |
|------|------|---------|------|------|------|------|
| 13.6 | ExtendActivity 继承迁移 | `ui/ExtendActivity.kt` | `AppCompatActivity()`→`ComponentActivity()`；移除 `import androidx.appcompat.app.AppCompatActivity`；其余代码不变（已用 setContent + Compose） | 13.5 | 低 | 编译通过 + 真机验证扩展功能页 |

#### 13.7 清理主题

> `styles.xml` 中 `AppTheme` parent 为 `Theme.Material3.DayNight.NoActionBar`（Material 库提供），`Widget.App.Button.Main` parent 为 `Widget.Material3.Button`（死样式）。

| Task | 标题 | 涉及文件 | 操作 | 前置 | 风险 | 验证 |
|------|------|---------|------|------|------|------|
| 13.7a | 更换 AppTheme parent | `res/values/styles.xml:4` | `parent="Theme.Material3.DayNight.NoActionBar"`→`parent="Theme.AppCompat.DayNight.NoActionBar"`（appcompat 提供，不依赖 material）；验证 AndroidManifest.xml `android:theme="@style/AppTheme"` 无需改 | 13.5 | 中 | 编译通过 + 真机验证状态栏/导航栏/深色模式 |
| 13.7b | 删除死样式 Widget.App.Button.Main | `res/values/styles.xml:31-44` | 全工程 grep 确认 0 布局引用后删除整个 `<style name="Widget.App.Button.Main">` 节点 | 13.7a | 低 | 编译通过 |
| 13.7c | 清理 MenuTheme（可选） | `res/values/styles.xml:23-29` | `MenuTheme` 原应用于 `MaterialToolbar`（`base_title.xml:20`），base_title 删除后无消费者；确认无其他引用后删除 | 13.7b | 低 | 编译通过 |

**主题选择决策**：

| 方案 | parent | 优点 | 缺点 | 决策 |
|------|--------|------|------|------|
| A | `Theme.AppCompat.DayNight.NoActionBar` | appcompat 已有依赖，DayNight 深色模式原生支持 | 仍依赖 appcompat | ✅ 采用 |
| B | `android:Theme.Material.Light.NoActionBar` | 无第三方依赖 | 框架主题样式有限，深色模式需手动处理 | ❌ 不采用（后续 appcompat 移除时再评估） |
| C | `android:Theme.DeviceDefault.DayNight` | 无第三方依赖，DayNight 支持 | API 兼容性需评估 | ❌ 不采用 |

> 注：所有 UI 主题色实际由 Compose `AppTheme` composable 中的 `MaterialTheme` 控制，XML 主题仅是 Activity 窗口外壳。方案 A 足够。

#### 13.8 移除 Material 依赖

| Task | 标题 | 涉及文件 | 操作 | 前置 | 风险 | 验证 |
|------|------|---------|------|------|------|------|
| 13.8a | 全项目 Material 引用清零验证 | — | `grep -r "com.google.android.material" app/src/` 确认 0 匹配；`grep -r "MaterialAlertDialogBuilder" app/src/` 确认 0 匹配；`grep -r "MaterialToolbar" app/src/` 确认 0 匹配；`grep -r "MaterialButton" app/src/main/res/` 确认 0 匹配 | 13.7 | 低 | grep 结果为空 |
| 13.8b | 移除 build.gradle.kts 依赖 | `app/build.gradle.kts:186` | 删除 `implementation(libs.material)` 行；更新注释 | 13.8a | 低 | 编译通过 |
| 13.8c | 移除 libs.versions.toml 条目 | `gradle/libs.versions.toml:18,63` | 删除 `material = "1.13.0"`（versions 段）和 `material = { module = "com.google.android.material:material", version.ref = "material" }`（libraries 段） | 13.8b | 低 | 编译通过 |
| 13.8d | 完整构建验证 | — | `./gradlew.bat :app:assembleDebug` 全量构建通过 | 13.8c | 中 | assembleDebug 成功 |

#### 13.9 文档同步

| Task | 标题 | 涉及文件 | 操作 | 前置 | 风险 | 验证 |
|------|------|---------|------|------|------|------|
| 13.9 | 文档同步 | `OPTIMIZATION_PLAN.md`、`MATERIAL_REMOVAL_PLAN.md` | 更新进度表状态、回填提交哈希、打标 `phase-13-done`；在 OPTIMIZATION_PLAN.md §1.2 遗留问题中标注 material 已移除 | 13.8d | 低 | 文档与代码状态一致 |

---

## 3. 进度跟踪表

| Task | 标题 | 风险 | 状态 | 提交哈希 |
|------|------|------|------|----------|
| 13.1 | 删除无引用布局 | 低 | 完成 | f0e50863 |
| 13.2 | Compose 化 ChoiceDialog | 低 | 完成 | dcf229a5 |
| 13.3 | Compose 化 StringDialog | 中 | 完成 | bfcfdc06 |
| 13.4a | Compose 化 ListDialog 主体 | 高 | 完成 | 17546d81 |
| 13.4b | 处理 CustomSettings 反射耦合 | 中 | 完成 | 16c14a25 |
| 13.4c | 删除旧 ListAdapter 和布局 | 低 | 完成 | 67c9b4ba |
| 13.5a | WebSettingsActivity 迁移 Compose | 高 | 完成 | d1e50579 |
| 13.5b | 删除 BaseActivity 和旧布局 | 低 | 完成 | 907d14bb |
| 13.6 | ExtendActivity 继承迁移 | 低 | 完成 | 8d9d8156 |
| 13.7a | 更换 AppTheme parent | 中 | 完成 | c6f1f849 |
| 13.7b | 删除死样式 Widget.App.Button.Main | 低 | 完成 | c6f1f849 |
| 13.7c | 清理 MenuTheme | 低 | 完成 | c6f1f849 |
| 13.8a | 全项目 Material 引用清零验证 | 低 | 完成 | 6ad58e4a |
| 13.8b | 移除 build.gradle.kts 依赖 | 低 | 完成 | 6ad58e4a |
| 13.8c | 移除 libs.versions.toml 条目 | 低 | 完成 | 6ad58e4a |
| 13.8d | 完整构建验证 | 中 | 完成 | 6ad58e4a |
| 13.9 | 文档同步 | 低 | 完成 | 8b4ed336 |
| — | **Phase 13 完成** | — | — | tag: phase-13-done |

---

## 4. 执行约束与风险提示

### 4.1 任务依赖顺序

```
13.1（已完成）→ 13.2 → 13.3 → 13.4a → 13.4b → 13.4c
                                                      ↓
                                              13.5a → 13.5b → 13.6 → 13.7a → 13.7b → 13.7c
                                                                                        ↓
                                                                               13.8a → 13.8b → 13.8c → 13.8d → 13.9
```

> 13.2→13.3→13.4 必须顺序执行（ChoiceDialog 最简单可验证桥接模式，StringDialog 中等，ListDialog 最复杂）。
> 13.5 依赖 13.4 完成（WebSettingsActivity 菜单调用 ListDialog）。
> 13.7 依赖 13.5 完成（base_title.xml 删除后 MenuTheme 无消费者）。
> 13.8 依赖 13.7 完成（主题 parent 不再引用 Material）。

### 4.2 高风险项

| Task | 风险点 | 缓解措施 |
|------|--------|---------|
| 13.4a | ListDialog 功能复杂（搜索/全选/反选/计数/嵌套对话框） | 逐功能验证：先验证基本列表显示，再验证搜索，再验证批量操作，最后验证计数输入 |
| 13.4b | CustomSettings 反射耦合 `ListDialog::class.java.getDeclaredField("listDialog")` | 新增 `onDismiss` 参数替代反射；确认 `CustomSettings.kt` 全部调用点已改 |
| 13.5a | WebSettingsActivity 493 行，WebView JS 接口众多 | JS 接口（`WebViewCallback` 内部类）全部保留不变；仅改外壳 Activity 类型和菜单 UI；逐菜单项验证 |

### 4.3 ComposeView 桥接模式参考

项目已有 `ui/extension/ComposeBridge.kt` 中 `NativeComposeBridge.showAlertDialog` 的桥接模式：

```
Activity → findViewById<ViewGroup>(android.R.id.content)
         → ComposeView(context).setContent { AppTheme { AlertDialog(...) } }
         → rootView.addView(composeView)
         → dismiss: rootView.post { rootView.removeView(composeView) }
```

三个对话框的 Compose 化均复用此模式，确保从任何 `Context`（含非 Compose Activity）都能弹出 Compose 对话框。

### 4.4 后续展望（已完成）

> ✅ 以下三项已于 2026-08-08 由 `APPCOMPAT_REMOVAL_PLAN.md`（Phase 14，tag: phase-14-done）全部完成：

- **appcompat 依赖移除**：AlertDialog 三处调用点（CustomSettings/UiExtensions）改为 `android.app.AlertDialog`，PermissionUtil 参数类型收窄为 `Activity`，AppTheme parent 改框架 `android:Theme.Material.Light.NoActionBar`（含新增 values-night 变体），依赖条目已从 build.gradle.kts 与 libs.versions.toml 移除
- **viewBinding 移除**：buildFeatures 中 `viewBinding = false`（代码 0 使用，已关闭）
- **constraintlayout 移除**：依赖条目已删除；14.5d 依赖树核查发现 constraintlayout 曾传递引入 appcompat:1.2.0，移除后传递链一并消失
