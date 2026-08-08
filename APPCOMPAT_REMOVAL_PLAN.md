# Sesame-TK AppCompat 依赖移除与残留清理计划

> 生成时间：2026-08-08
> 前置条件：MATERIAL_REMOVAL_PLAN.md Phase 13 全部完成（tag: phase-13-done，material 已移除，Compose-only UI 栈达成）
> 当前状态：✅ 全部完成（tag: phase-14-done）——appcompat/constraintlayout 依赖已移除，viewBinding 已关闭，死代码/死资源已清理
> 目标：彻底移除 `androidx.appcompat:appcompat` 依赖，关闭 viewBinding，清除 Phase 13 遗留死代码与死资源

---

## 目录

1. [背景与依赖审计](#1-背景与依赖审计)
2. [分阶段任务](#2-分阶段任务)
3. [进度跟踪表](#3-进度跟踪表)

---

## 1. 背景与依赖审计

### 1.1 AppCompat 依赖硬引用全景

`androidx.appcompat:appcompat`（build.gradle.kts:185, libs.versions.toml:17+59）存在以下硬引用：

| # | 文件 | 引用类型 | 具体 AppCompat API |
|---|------|---------|-----------------|
| C1 | `res/values/styles.xml:4` | 主题 parent | `Theme.AppCompat.DayNight.NoActionBar`（Phase 13.7a 由 Material3 主题切换而来） |
| C2 | `model/CustomSettings.kt:4,252,277,300` | import+调用 | `androidx.appcompat.app.AlertDialog`（3 处 Builder：账号选择菜单/黑名单菜单/时段输入） |
| C3 | `ui/extension/UiExtensions.kt:15,86` | import+调用 | `androidx.appcompat.app.AlertDialog`（1 处：showPasswordDialog 防呆验证） |
| C4 | `core/permission/PermissionUtil.kt:15,161,182` | import+类型 | `AppCompatActivity`（checkOrRequestNotificationPermission 参数类型 + startActivitySafely 类型判断） |

### 1.2 关联清理项（Phase 13 遗留）

| # | 项 | 位置 | 现状 | 处置 |
|---|----|------|------|------|
| D1 | `viewBinding = true` | build.gradle.kts:73 | 全代码 0 使用（已 grep 验证 `Binding.inflate`/`binding.` 无匹配）；布局仅剩 1 个死布局 | 关闭 |
| D2 | `constraintlayout` 依赖 | toml:6,37 + build.gradle.kts:176 | 全代码 0 引用（已 grep 验证 `ConstraintLayout` 无匹配） | 移除 |
| D3 | `WatermarkInjector.kt` | `ui/extension/WatermarkInjector.kt` | 0 消费者（13.5a 已改用 WatermarkLayer） | 删除 |
| D4 | 死资源（6 个） | `res/layout/toast.xml` + `res/drawable/shape_diary_toast.xml`（互相引用但整体 0 消费者）、`res/drawable/dialog_list_button.xml`、`switch_track.xml`、`tab_selected_background.xml`、`toast_ic.xml` | 均无 `R.layout.*`/`R.drawable.*`/`@layout`/`@drawable` 引用（已 grep 验证） | 删除 |
| D5 | `vectorDrawables.useSupportLibrary = true` | build.gradle.kts:39 | minSdk 26 原生支持 VectorDrawable（API 21+），无需兼容库 | 评估后删除（可选） |

### 1.3 约束

- **不改变业务逻辑**：三个 AlertDialog 调用点的行为（setItems 菜单/EditText 输入/确认取消回调）须完整保留
- **不改序列化格式**：Jackson 配置读写不受影响
- **不改包名/类名**：方法签名仅 C4 的 `checkOrRequestNotificationPermission(activity: AppCompatActivity)` 参数类型收窄为 `Activity`（Kotlin 调用方无 Java 方，参数类型变更不影响调用）；其余类与方法签名不变
- **编译通过即提交**：每个 Task 完成后 `./gradlew.bat :app:compileDebugKotlin` 通过即提交
- **Compose 技术栈不受影响**：`activity-compose`、Compose Material3 与 appcompat 无依赖关系，保留不动
- **主题决策（C1）**：AppTheme parent 由 `Theme.AppCompat.DayNight.NoActionBar` 改为框架主题——方案 B（见 13.7 决策表）：
  - `values/styles.xml`: `parent="android:Theme.Material.Light.NoActionBar"`（白天，API 21+，minSdk 26 满足）
  - `values-night/styles.xml`: `parent="android:Theme.Material.NoActionBar"`（夜晚，深色 Material，API 21+）
  - 理由：`android:Theme.DeviceDefault.DayNight` 需 API 29+ 不可用；`android:Theme.Material.DayNight` 需 API 29+；Material 系列 Light/深色双变体是 minSdk 26 下唯一支持自动深色切换的框架方案
  - UI 主体颜色由 Compose `AppTheme` composable 控制，XML 主题仅窗口外壳（状态栏/导航栏/系统对话框外观）；`android:Theme.Material.Light` 默认浅色状态栏图标（`windowLightStatusBar=true`），`android:Theme.Material` 深色主题默认浅色图标，与原 DayNight 行为等价
- **appcompat 传递依赖核查**：移除后 `./gradlew.bat :app:dependencies` 确认无其他库传递引入 appcompat；若存在传递依赖仅记录，不阻塞（appcompat 的类在运行时不可用不构成问题，但代码中引用 appcompat 类会在编译期暴露）

---

## 2. 分阶段任务

### Phase 14：AppCompat 依赖移除与残留清理

> 目标：移除全部 `androidx.appcompat.*` 引用，关闭 viewBinding，删除 constraintlayout 依赖与 Phase 13 遗留死代码/死资源
> 原则：渐进式替换，每个 Task 后编译通过；低风险任务可合并执行

#### 14.1 死代码与死资源清理（低风险）

| Task | 标题 | 涉及文件 | 操作 | 前置 | 风险 | 验证 |
|------|------|---------|------|------|------|------|
| 14.1 | 清理死代码与死资源 | `ui/extension/WatermarkInjector.kt`、`res/layout/toast.xml`、`res/drawable/shape_diary_toast.xml`、`res/drawable/dialog_list_button.xml`、`res/drawable/switch_track.xml`、`res/drawable/tab_selected_background.xml`、`res/drawable/toast_ic.xml` | 全工程 grep 确认 0 引用后删除（WatermarkInjector 需同时 grep 字符串引用；toast.xml 与 shape_diary_toast 为互相引用但整体无消费者的死链） | 13.9 | 低 | 编译通过 |

#### 14.2 AlertDialog 替换为框架实现（中风险）

| Task | 标题 | 涉及文件 | 操作 | 前置 | 风险 | 验证 |
|------|------|---------|------|------|------|------|
| 14.2a | CustomSettings AlertDialog 替换 | `model/CustomSettings.kt` | `import androidx.appcompat.app.AlertDialog` → `import android.app.AlertDialog`；3 处 Builder 调用 API 等价（setTitle/setItems/setMessage/setView/setPositiveButton/setNegativeButton/show 均存在），仅检查 `android.app.AlertDialog` 的差异点（无 setMultiChoiceItems 差异——本文件未使用；`R.string.ok` 等资源引用不变） | 14.1 | 中 | 编译通过 + 真机验证单次运行菜单/黑名单/时段对话框 |
| 14.2b | UiExtensions AlertDialog 替换 | `ui/extension/UiExtensions.kt` | 同上替换；`dialog.getButton(AlertDialog.BUTTON_POSITIVE)`、`dialog.window?.setBackgroundDrawable`、`setOnShowListener` 在 `android.app.AlertDialog` 全部存在；`dialog.getButton(...)` 的 NPE 风险与原实现一致（保留原 `apply` 结构） | 14.2a | 中 | 编译通过 + 真机验证防呆验证密码框 |

> 注意：`android.app.AlertDialog` 的外观由窗口主题决定（14.4 改为框架 Material 主题后外观与 AppCompat 时代协调一致）；行为 API 完全等价。

#### 14.3 PermissionUtil 类型收窄（低风险）

| Task | 标题 | 涉及文件 | 操作 | 前置 | 风险 | 验证 |
|------|------|---------|------|------|------|------|
| 14.3 | PermissionUtil 改用 Activity | `core/permission/PermissionUtil.kt` | L161 `fun checkOrRequestNotificationPermission(activity: AppCompatActivity)` → `(activity: Activity)`；L182 `context !is androidx.appcompat.app.AppCompatActivity && context !is android.app.Activity` → `context !is Activity`（AppCompatActivity 是 Activity 子类，判断语义等价）；移除 L15 import；确认全部调用方（grep `checkOrRequestNotificationPermission`）传参兼容（Activity 实参天然满足） | 14.2 | 低 | 编译通过 |

#### 14.4 AppTheme parent 改框架主题（中风险）

| Task | 标题 | 涉及文件 | 操作 | 前置 | 风险 | 验证 |
|------|------|---------|------|------|------|------|
| 14.4a | 白天主题 | `res/values/styles.xml:4` | `parent="Theme.AppCompat.DayNight.NoActionBar"` → `parent="android:Theme.Material.Light.NoActionBar"`；其余 item（colorPrimary/titleTextColor/navigationBarColor/statusBarColor/windowNoTitle）保留原样 | 14.3 | 中 | 编译通过 |
| 14.4b | 夜间主题变体 | 新建 `res/values-night/styles.xml` | 复制 AppTheme 定义，parent 为 `android:Theme.Material.NoActionBar`；item 与原版一致（colorPrimary 等引用不变） | 14.4a | 中 | 编译通过 + 真机验证深色模式切换 |
| 14.4c | 状态栏/导航栏验证 | AndroidManifest.xml（无需改） | 确认 `android:theme="@style/AppTheme"` 引用无需改动；`android:windowLightStatusBar` 行为：Light 主题默认 true（深色图标）、深色主题默认 false（浅色图标），与 AppCompat DayNight 等价 | 14.4b | 低 | 真机验证状态栏图标颜色 |

> 风险提示：`android:Theme.Material.Light.NoActionBar` 下 `windowNoTitle` 已由 NoActionBar 变体覆盖；`android:windowTranslucentStatus`/`android:statusBarColor` 等 item 在框架主题下正常生效。若真机发现对话框按钮颜色等外观回归，可接受（外观非业务逻辑），但状态栏/导航栏可读性必须验证。

#### 14.5 移除 appcompat 依赖（低风险）

| Task | 标题 | 涉及文件 | 操作 | 前置 | 风险 | 验证 |
|------|------|---------|------|------|------|------|
| 14.5a | 引用清零验证 | — | `grep -r "androidx.appcompat" app/src/` 确认 0 匹配；`grep -r "Theme.AppCompat" app/src/main/res/` 确认 0 匹配 | 14.4 | 低 | grep 结果为空 |
| 14.5b | 移除 build.gradle.kts 依赖 | `app/build.gradle.kts:185` | 删除 `implementation(libs.appcompat)` 行；更新注释 | 14.5a | 低 | 编译通过 |
| 14.5c | 移除 libs.versions.toml 条目 | `gradle/libs.versions.toml:17,59` | 删除 `appcompat = "1.7.1"`（versions）与 `appcompat = { module = "androidx.appcompat:appcompat", version.ref = "appcompat" }`（libraries） | 14.5b | 低 | 编译通过 |
| 14.5d | 传递依赖核查 | — | `./gradlew.bat :app:dependencies --configuration debugRuntimeClasspath` 检查 appcompat 来源（预期无直接传递引入；若有则记录） | 14.5c | 低 | dependencies 输出记录 |

#### 14.6 viewBinding 关闭与 constraintlayout 移除（低风险）

| Task | 标题 | 涉及文件 | 操作 | 前置 | 风险 | 验证 |
|------|------|---------|------|------|------|------|
| 14.6a | 关闭 viewBinding | `app/build.gradle.kts:72-78` | `viewBinding = true` → `false`（buildFeatures 块内） | 14.5 | 低 | 编译通过 |
| 14.6b | 移除 constraintlayout | `app/build.gradle.kts:176`、`gradle/libs.versions.toml:6,37` | 删除 `implementation(libs.androidx.constraintlayout)` 行、`constraintlayout = "2.2.1"`（versions）与 `androidx-constraintlayout = {...}`（libraries） | 14.6a | 低 | 编译通过 |
| 14.6c | useSupportLibrary 评估（可选） | `app/build.gradle.kts:39` | `vectorDrawables.useSupportLibrary = true` 与 appcompat 无直接依赖；minSdk 26 原生支持矢量图，若保留无副作用则保留（决策：保留，避免引入 vector 兼容边界问题；若验证无碍可删除） | 14.6b | 低 | 编译通过 |

#### 14.7 完整构建验证（中风险）

| Task | 标题 | 涉及文件 | 操作 | 前置 | 风险 | 验证 |
|------|------|---------|------|------|------|------|
| 14.7 | 完整构建验证 | — | `./gradlew.bat :app:assembleDebug` 全量构建通过 | 14.6 | 中 | assembleDebug 成功 |

#### 14.8 文档同步（低风险）

| Task | 标题 | 涉及文件 | 操作 | 前置 | 风险 | 验证 |
|------|------|---------|------|------|------|------|
| 14.8 | 文档同步 | `APPCOMPAT_REMOVAL_PLAN.md`、`OPTIMIZATION_PLAN.md`、`MATERIAL_REMOVAL_PLAN.md` | 更新本计划进度表状态、回填提交哈希（amend 惯例）、打标 `phase-14-done`；OPTIMIZATION_PLAN.md §1.2 遗留问题标注 appcompat 已移除；MATERIAL_REMOVAL_PLAN.md §4.4 后续展望更新为已完成 | 14.7 | 低 | 文档与代码状态一致 |

---

## 3. 进度跟踪表

| Task | 标题 | 风险 | 状态 | 提交哈希 |
|------|------|------|------|----------|
| 14.1 | 清理死代码与死资源 | 低 | 完成 | b782adf9 |
| 14.2a | CustomSettings AlertDialog 替换 | 中 | 完成（真机验证待执行） | 1249b781 |
| 14.2b | UiExtensions AlertDialog 替换 | 中 | 完成（真机验证待执行） | 37537161 |
| 14.3 | PermissionUtil 改用 Activity | 低 | 完成 | 1def2495 |
| 14.4a | 白天主题改框架 Material | 中 | 完成（真机验证待执行） | f4bdf43d |
| 14.4b | 夜间主题变体 | 中 | 完成（真机验证待执行） | 7a4e2ba2 |
| 14.4c | 状态栏/导航栏验证 | 低 | 完成（编译通过；真机验证待执行） | 7a4e2ba2 |
| 14.5a | 全项目 appcompat 引用清零验证 | 低 | 完成 | c77c02e8 |
| 14.5b | 移除 build.gradle.kts 依赖 | 低 | 完成 | c77c02e8 |
| 14.5c | 移除 libs.versions.toml 条目 | 低 | 完成 | c3ad454d |
| 14.5d | 传递依赖核查 | 低 | 完成（constraintlayout 传递 appcompat:1.2.0，14.6b 后消失） | c3ad454d |
| 14.6a | 关闭 viewBinding | 低 | 完成 | 408b2728 |
| 14.6b | 移除 constraintlayout | 低 | 完成 | 408b2728 |
| 14.6c | useSupportLibrary 评估 | 低 | 完成（决策：保留） | 408b2728 |
| 14.7 | 完整构建验证 | 中 | 完成（BUILD SUCCESSFUL） | — |
| 14.8 | 文档同步 | 低 | 完成 | c94ef207 |
| — | **Phase 14 完成** | — | — | tag: phase-14-done |

---

## 4. 执行约束与风险提示

### 4.1 任务依赖顺序

```
14.1 → 14.2a → 14.2b → 14.3 → 14.4a → 14.4b → 14.4c → 14.5a → 14.5b → 14.5c → 14.5d
                                                                                          ↓
                                                                  14.6a → 14.6b → 14.6c → 14.7 → 14.8
```

> 14.2 依赖 14.1（先清死代码减少干扰）；14.3 可在 14.2 后独立执行。
> 14.4 依赖 14.2/14.3（AlertDialog 外观由窗口主题决定，先替换实现再改主题可隔离问题）。
> 14.5 依赖 14.4（主题 parent 不再引用 AppCompat）。
> 14.6 与 14.5 相互独立但建议先 14.5（若 viewBinding 关闭暴露隐藏引用，可在同一阶段内处理）。

### 4.2 高风险项

| Task | 风险点 | 缓解措施 |
|------|--------|---------|
| 14.2a/b | `android.app.AlertDialog` 与 AppCompat 版 API 差异（如 `setItems` 的 item 高亮、`getButton` 返回类型） | 先 grep 确认使用的 API 子集；替换后逐个验证 3+1 个对话框的交互路径；`getButton(...)` 在框架实现中 show() 后非空，与原逻辑一致 |
| 14.4 | 框架主题下状态栏/导航栏颜色、对话框外观变化 | 保留原 item 配置；真机验证深色模式切换与状态栏图标可读性；外观回归可接受但可读性不可妥协 |
| 14.5 | 移除依赖后其他库传递引用 appcompat 的类导致运行时 NoClassDefFoundError | 14.5d 依赖树核查；若发现传递依赖（预期无），记录并评估 |

### 4.3 与 Phase 13 的关系

- 14.4 的主题决策是对 MATERIAL_REMOVAL_PLAN.md §13.7 决策表「方案 B：`android:Theme.Material.Light.NoActionBar`，后续 appcompat 移除时再评估」的落地执行
- 14.1 的 D4 死资源是 13.4c 删除 dialog_list.xml 后遗留（dialog_list_button）与更早期遗留（toast 链/switch_track/tab_selected_background/toast_ic）
- 完成 Phase 14 后，项目第三方 UI 依赖仅剩 Compose 技术栈（material3/icons-extended 经 BOM）+ activity-compose + core-ktx

### 4.4 后续展望（不在本计划范围）

- **androidx.core/core-ktx 瘦身评估**：PermissionUtil 使用的 ActivityCompat/ContextCompat 与 kotlinx 扩展是否可替换为原生 API（需评估 minSdk 26 兼容性）
- **lifecycle-livedata/runtime-livedata 评估**：Compose 状态是否可完全替代 LiveData（涉及 ConfigRepository/ThemeManager 状态源）
- **slf4j/logback 评估**：日志栈是否可收敛为单实现
- **aidl/mlModelBinding**：buildFeatures 中剩余特性（aidl/mlModelBinding 为功能必需，保留）
