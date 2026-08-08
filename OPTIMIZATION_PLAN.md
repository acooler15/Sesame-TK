# Sesame-TK 后续优化计划

> 生成时间：2026-08-06
> 前置条件：REFACTOR_PLAN.md Phase 0-5 全部完成（Java→Kotlin 迁移、Lombok 移除、UI 收敛）
> 当前状态：项目源码 100% Kotlin，6 个 phase tag 已打标

---

## 目录

1. [背景与遗留问题](#1-背景与遗留问题)
2. [优化目标](#2-优化目标)
3. [分阶段优化计划](#3-分阶段优化计划)
4. [进度跟踪表](#4-进度跟踪表)

---

## 1. 背景与遗留问题

### 1.1 已完成基线

| 里程碑 | 状态 | 验证 |
|--------|------|------|
| Phase 0-5 Java→Kotlin 迁移 | ✅ 全部完成 | `tag: phase-0-done` ~ `phase-5-done` |
| Lombok 依赖移除 | ✅ 完成 | `build.gradle.kts` 无 lombok 引用 |
| `kotlinx-serialization` 死依赖清理 | ✅ 完成 | |
| `fastjson` 死条目清理 | ✅ 完成 | |

### 1.2 遗留问题清单

经逐项审查，重构计划完成后仍存在以下结构性问题：

#### 🔴 P0：已知 Bug

| 编号 | 问题 | 位置 | 影响 |
|------|------|------|------|
| B1 | `OldRpcBridge.getVersion()` 返回 `RpcVersion.NEW` 而非 `RpcVersion.OLD` | `OldRpcBridge.kt:26-28` | 字段 `rpcVersion` 赋值错误；但该字段写入后从未被读取，当前无运行时影响 |
| B2 | `rpcVersion` 字段为死代码 | `ApplicationHook.kt:598,740,806` | 写入后从未读取，`getVersion()` 方法可考虑废弃或启用 |
| B3 | `JsonHelper.mapper` 与 `JsonUtil.MAPPER` 配置不一致 | `JsonHelper.kt:7` vs `JsonUtil.kt` | `JsonHelper` 未配置 `NON_NULL` 等序列化策略，`StatusManager` 用其序列化 `Status` 可能产生与 `Config` 序列化不一致的行为 |

#### 🟡 P1：`core/` 包重组未落地

重构计划 §2.1 定义了 `core/` 目标包结构，但从未作为 Task 执行。`util/` 仍为 **37 个文件平铺** 在单一包下，缺乏职责分层：

```
util/
├── AssetUtil.kt          Average.kt           CircularFifoQueue.kt
├── CommandUtil.kt        CoroutineUtils.kt    DataStore.kt
├── defaultBlacklist.kt   Detector.kt          DirectoryWatcher.kt
├── FansirsqiUtil.kt      Files.kt             GlobalThreadPools.kt
├── HanziToPinyin.kt      IconManager.kt       JsonHelper.kt
├── JsonUtil.kt           LanguageUtil.kt      ListUtil.kt
├── Log.kt                Logback.kt           ModuleStatus.kt
├── NetworkUtils.kt       Notify.kt            PermissionUtil.kt
├── PortUtil.kt           RandomUtil.kt        ResChecker.kt
├── StatusManager.kt      StringUtil.kt        SwipeUtil.kt
├── TaskBlacklist.kt      TimeCounter.kt       TimeFormatter.kt
├── TimeUtil.kt           ToastUtil.kt         TypeUtil.kt
├── UnlockUtil.kt         WakeLockManager.kt   maps/
```

#### 🟡 P2：God Class 未拆分

| 文件 | 行数 | 职责过载说明 |
|------|------|-------------|
| `task/antForest/AntForest.kt` | 5091 | 能量收取、保护罩、道具、统计、好友列表等全部集中 |
| `task/antFarm/AntFarm.kt` | 4983 | 喂鸡、打扫、捐蛋、道具、商店等全部集中 |
| `task/antSports/AntSports.kt` | 3007 | 运动、走路、兑换等全部集中 |
| `task/antMember/AntMember.kt` | 3000 | 会员积分、权益兑换等全部集中 |
| `hook/ApplicationHook.kt` | 975 | 进程管理、Hook 注册、广播接收、任务调度、RPC 初始化集中 |

#### 🟢 P3：冗余注解与死依赖

| 编号 | 问题 | 范围 |
|------|------|------|
| A1 | `@JvmStatic`/`@JvmField`/`@JvmOverloads` 注解残留 | 25 处，分布在 8 个文件中；所有 Java 调用方已迁移，注解失去意义 |
| A2 | `libs.versions.toml` 死条目 | `android`、`desugar`、`junit`/`androidx-junit`/`espresso-core`、`ui-tooling-preview-android`、`rikka-hidden-stub` |
| A3 | `material3` 重复定义 | `libs.material3`（显式版本）与 `libs.androidx-material3`（BOM 管理）同时引用 |

#### 🟢 P4：Model-UI 耦合

`ModelField` 及其子类仍直接创建 Android View（`Button`、`LinearLayout`、`MaterialButton`），数据模型与传统 UI 强耦合。`modelFieldExt/` 下 4 个子类（`SelectOneModelField`、`SelectAndCountModelField`、`SelectAndCountOneModelField`、`SelectModelField`）的 `getView(context: Context): View` 方法直接实例化传统 View 组件。

---

## 2. 优化目标

### 2.1 目标

1. 修复已知 Bug，消除潜在运行时风险
2. 将 `util/` 平铺包重组为 `core/` 分层结构，建立清晰的公共能力边界
3. 将超 3000 行的 God Class 按功能域拆分为可维护的子模块
4. 清理冗余注解和死依赖，降低编译复杂度
5. 解耦 Model 与 UI，为 Compose 全面迁移扫清障碍

### 2.2 约束

- **不改变业务逻辑**：拆分和重组仅做代码移动/重组，不重写功能
- **不改变序列化格式**：Jackson 注解和配置保持兼容
- **不移动高危入口**：`hook.xp82.HookEntry`、`hook.lsp100.HookEntry` 全限定名不变
- **不改动 JNI/AIDL**：`Detector` native 方法名、AIDL 包路径不变（例外：Phase 9 为刻意移除 `Detector` JNI，移除后该约束自然失效）
- **编译通过即提交**：每个 Task 完成后 `./gradlew :app:compileDebugKotlin` 通过即提交

---

## 3. 分阶段优化计划

### Phase 6：Bug 修复与死代码清理

> 目标：修复已知 Bug，清理无效代码，消除潜在风险

| Task | 标题 | 涉及文件 | 操作 | 前置 | 风险 | 验证 |
|------|------|---------|------|------|------|------|
| 6.1 | 修复 `OldRpcBridge.getVersion()` 返回值 | `OldRpcBridge.kt` | 将 `RpcVersion.NEW` 改为 `RpcVersion.OLD` | — | 低 | 编译通过 |
| 6.2 | 评估并清理 `rpcVersion` 死字段 | `ApplicationHook.kt`、`RpcBridge.kt` | 确认无读取方后移除 `rpcVersion` 字段；若 `getVersion()` 无其他消费方则标记 `@Deprecated` 或移除 | 6.1 | 低 | 编译通过 |
| 6.3 | 合并 `JsonHelper` 至 `JsonUtil` | `JsonHelper.kt`、`StatusManager.kt` | 将 `StatusManager` 中 `JsonHelper.toJson`/`fromJson` 调用改为使用 `JsonUtil`；评估 `NON_NULL` 配置差异后统一；删除 `JsonHelper.kt` | — | 中 | 编译通过 + 真机验证 Status 序列化 |

### Phase 7：`util/` → `core/` 包重组

> 目标：将 37 个平铺文件按职责分层迁入 `core/` 子包
> 原则：只移动文件+改 package 声明+更新 import，不改代码逻辑

#### 7.1 目标包结构

```
fansirsqi.xposed.sesame
├── core/
│   ├── json/          // JsonUtil
│   ├── log/          // Log, Logback
│   ├── reflect/      // TypeUtil
│   ├── util/         // 纯工具：StringUtil, ListUtil, TimeUtil, TimeFormatter,
│   │                  //   TimeCounter, RandomUtil, Average, CircularFifoQueue,
│   │                  //   HanziToPinyin, LanguageUtil, PortUtil, ResChecker
│   ├── threads/      // GlobalThreadPools, CoroutineUtils
│   ├── notify/       // Notify, ToastUtil
│   ├── permission/   // PermissionUtil
│   ├── store/        // DataStore
│   └── app/          // AssetUtil, Files, IconManager, ModuleStatus,
│                      //   NetworkUtils, StatusManager, SwipeUtil, UnlockUtil,
│                      //   WakeLockManager, CommandUtil, FansirsqiUtil,
│                      //   DirectoryWatcher, defaultBlacklist, TaskBlacklist
├── util/
│   └── maps/         // 保持不动（ID 映射，引用方多）
│                      // Detector.kt 原保留原位（JNI 绑定），已在 Phase 9 随 libchecker.so 一并移除
└── ...（其余包不变）
```

| Task | 标题 | 涉及文件 | 操作 | 前置 | 风险 | 验证 |
|------|------|---------|------|------|------|------|
| 7.1 | 创建 `core/` 包结构 | 新建目录 | 创建 `core/json/`、`core/log/`、`core/reflect/`、`core/util/`、`core/threads/`、`core/notify/`、`core/permission/`、`core/store/`、`core/app/` 目录 | Phase 6 | 低 | 目录存在 |
| 7.2 | `core/json/` 迁移 | `JsonUtil.kt` | 移动文件，改 package，更新全项目 import | 7.1 | 中 | 编译通过 |
| 7.3 | `core/log/` 迁移 | `Log.kt`、`Logback.kt` | 移动文件，改 package，更新 import | 7.1 | 中 | 编译通过 |
| 7.4 | `core/reflect/` 迁移 | `TypeUtil.kt` | 移动文件，改 package，更新 import（注意 `ModelField` 泛型反射依赖） | 7.1 | 中 | 编译通过 |
| 7.5 | `core/util/` 迁移 | `StringUtil.kt`、`ListUtil.kt`、`TimeUtil.kt`、`TimeFormatter.kt`、`TimeCounter.kt`、`RandomUtil.kt`、`Average.kt`、`CircularFifoQueue.kt`、`HanziToPinyin.kt`、`LanguageUtil.kt`、`PortUtil.kt`、`ResChecker.kt` | 批量移动，改 package，更新 import | 7.1 | 中 | 编译通过 |
| 7.6 | `core/threads/` 迁移 | `GlobalThreadPools.kt`、`CoroutineUtils.kt` | 移动文件，改 package，更新 import | 7.1 | 中 | 编译通过 |
| 7.7 | `core/notify/` 迁移 | `Notify.kt`、`ToastUtil.kt` | 移动文件，改 package，更新 import | 7.1 | 中 | 编译通过 |
| 7.8 | `core/permission/` 迁移 | `PermissionUtil.kt` | 移动文件，改 package，更新 import | 7.1 | 低 | 编译通过 |
| 7.9 | `core/store/` 迁移 | `DataStore.kt` | 移动文件，改 package，更新 import | 7.1 | 低 | 编译通过 |
| 7.10 | `core/app/` 迁移 | `AssetUtil.kt`、`Detector.kt`、`Files.kt`、`IconManager.kt`、`ModuleStatus.kt`、`NetworkUtils.kt`、`StatusManager.kt`、`SwipeUtil.kt`、`UnlockUtil.kt`、`WakeLockManager.kt`、`CommandUtil.kt`、`FansirsqiUtil.kt`、`DirectoryWatcher.kt`、`defaultBlacklist.kt`、`TaskBlacklist.kt` | 批量移动，改 package，更新 import；`Detector.kt` 注意 JNI 绑定（不改名，仅改 package 需验证 native 方法签名） | 7.1 | 高 | 编译通过 + 真机验证滑块功能 |

### Phase 8：God Class 拆分

> 目标：将超 3000 行的功能域文件按子功能拆分为独立类
> 原则：提取独立逻辑为单独文件，保持公开 API 不变（对外仍通过主类委托调用）

| Task | 标题 | 涉及文件 | 操作 | 前置 | 风险 | 验证 |
|------|------|---------|------|------|------|------|
| 8.1 | `ApplicationHook` 拆分 | `ApplicationHook.kt` | 提取广播接收器→`BroadcastReceiverManager`；提取任务调度→`TaskScheduler`；保留 Hook 注册和进程管理核心 | Phase 7 | 高 | 编译通过 + 真机验证模块加载 |
| 8.2 | `AntForest` 拆分 | `AntForest.kt`（5091 行） | 按子功能拆分：能量收取→`ForestEnergyCollector`、保护罩→`ForestShieldManager`、道具→`ForestItemManager`、统计→`ForestStatistics`；主类保留为协调入口 | 8.1 | 高 | 编译通过 + 真机验证森林功能 |
| 8.3 | `AntFarm` 拆分 | `AntFarm.kt`（4983 行） | 按子功能拆分：喂鸡→`FarmFeedManager`、捐蛋→`FarmDonateManager`、道具→`FarmItemManager`、商店→`FarmShopManager`；主类保留为协调入口 | 8.2 | 高 | 编译通过 + 真机验证庄园功能 |
| 8.4 | `AntSports` 拆分 | `AntSports.kt`（3007 行） | 按子功能拆分：走路兑换→`SportsExchangeManager`、运动任务→`SportsTaskManager` | 8.3 | 中 | 编译通过 |
| 8.5 | `AntMember` 拆分 | `AntMember.kt`（3000 行） | 按子功能拆分：积分→`MemberPointsManager`、权益兑换→`MemberBenefitManager` | 8.4 | 中 | 编译通过 |

### Phase 9：移除 libchecker.so 与 Detector native 依赖

> 目标：彻底移除 libchecker.so 二进制及其 JNI 封装 `Detector`，消除闭源 native 依赖，APK 体积减少约 20MB
> 原则：随 so 移除的环境门禁、依赖检查、BaseUrl 生成等 native 功能一并废弃；dexkit / tflite 等其他 so 的加载流程不受影响

#### 9.0 背景：libchecker.so 影响面

| 类别 | 位置 | 说明 |
|------|------|------|
| 二进制文件 | `app/src/main/jniLibs/{arm64-v8a, armeabi-v7a, x86, x86_64}/libchecker.so` | 4 个 ABI，合计约 20MB，已被 git 跟踪 |
| 误入库文件 | `arm64-v8a/libchecker.so.{id0,id1,id2,nam,til}` | IDA 分析数据库残留，未被 git 跟踪但存在于工作区，约 19.6MB |
| JNI 封装 | `util/Detector.kt` | 7 个 `external` native 方法：`init`、`tips`、`isEmbeddedNative`、`dangerous`、`genWua`、`loadLibraryWithContextNative`、`getApiUrlWithKey` |
| Hook 侧调用 | `hook/ApplicationHook.kt` | 环境门禁 `isLegitimateEnvironment()`→`dangerous()`；`loadNativeLibs(checkerDestFile)` |
| UI 侧调用 | `ui/MainActivity.kt`、`ui/extension/UiExtensions.kt`、`ui/viewmodel/ExtendViewModel.kt` | `initNativeDetector()`；设置页跳转前置检查 `loadLibrary("checker")`；"获取BaseUrl" 调试菜单 |
| so 分发链路 | `ui/viewmodel/MainViewModel.kt`、`core/app/AssetUtil.kt` | `copyAssets()` 复制 checker；`CHEKCE_SO`/`checkerDestFile` 常量 |

#### 9.x 行为决策（已确认）

| 决策点 | 结论 |
|--------|------|
| `ApplicationHook` 环境门禁 | 整段移除，放行所有环境（不再调用 `dangerous()`，不再提前 return） |
| 设置页跳转前置检查 | 移除 `loadLibrary("checker")` 门禁，直接进入设置页 |
| "获取BaseUrl" 调试菜单 | 连同 `handleGetBaseUrl()` 一并移除 |
| `genWua()` | 确认无任何调用方，随 `Detector.kt` 删除 |
| 编号策略 | 原 Phase 9/10 顺延为 Phase 10/11，任务号同步顺延 |

| Task | 标题 | 涉及文件 | 操作 | 前置 | 风险 | 验证 |
|------|------|---------|------|------|------|------|
| 9.1 | Hook 侧调用清理 | `ApplicationHook.kt`、`MainViewModel.kt` | 移除 `Detector` import 与环境门禁（`isLegitimateEnvironment`/`dangerous`）；移除 `loadLibs()` 中 `loadNativeLibs(checkerDestFile)` 及 `checkerDestFile` import；移除 `copyAssets()` 中 checker 复制行 | Phase 8 | 中 | 编译通过 + 真机验证模块加载 |
| 9.2 | UI 侧调用清理 | `MainActivity.kt`、`UiExtensions.kt`、`ExtendViewModel.kt` | 移除 `initNativeDetector()` 及其调用；`performNavigationToSettings` 去掉 `loadLibrary("checker")` 门禁与 `Detector.tips()` 分支，直接跳转；移除"获取BaseUrl"菜单项与 `handleGetBaseUrl()` | 9.1 | 中 | 编译通过 + 真机验证设置页跳转 |
| 9.3 | 删除 Detector.kt 与 AssetUtil checker 常量 | `util/Detector.kt`、`core/app/AssetUtil.kt` | 删除 `Detector.kt` 整个文件；移除 `CHEKCE_SO`、`checkerDestFile`；修正 `copySoFileToStorage()` 日志中误用的 `checkerDestFile`（改为 `destFile`） | 9.2 | 低 | 编译通过 |
| 9.4 | 删除 so 二进制与 IDA 残留 | `app/src/main/jniLibs/`、`.gitignore` | `git rm` 4 个 `libchecker.so`；删除 5 个 IDA 残留文件；`.gitignore` 追加 `*.id0`、`*.id1`、`*.id2`、`*.nam`、`*.til` | 9.3 | 中 | `:app:assembleDebug` 通过 + APK 体积下降约 20MB |
| 9.5 | 文档同步 | `OPTIMIZATION_PLAN.md` | 核对 §7.1 包结构注释与进度表 7.10 备注和移除结果一致；同步 Phase 9 进度表状态、回填提交哈希、打标 `phase-9-done` | 9.4 | 低 | 文档与代码状态一致 |

### Phase 10：冗余注解清理与死依赖移除

> 目标：移除无意义的 `@JvmStatic`/`@JvmField` 注解和未使用的依赖条目

| Task | 标题 | 涉及文件 | 操作 | 前置 | 风险 | 验证 |
|------|------|---------|------|------|------|------|
| 10.1 | 移除 `@JvmStatic`/`@JvmField`（第一批） | `BaseModel.kt`、`VitalityStore.kt`、`RpcEntity.kt`、`RpcVersion.kt` | 逐个移除注解，验证无 Kotlin 编译错误 | Phase 9 | 低 | 编译通过 |
| 10.2 | 移除 `@JvmStatic`/`@JvmField`（第二批） | `ListDialog.kt`（8 处）、`OptionsAdapter.kt`（1 处）、`ListAdapter.kt`（4 处） | 移除注解 | 10.1 | 低 | 编译通过 |
| 10.3 | 移除 `@JvmStatic`/`@JvmField`（第三批） | `AntFarm.kt`（4 处）、`ModelTask.kt`（3 处） | 移除注解 | 10.2 | 低 | 编译通过 |
| 10.4 | 清理 `libs.versions.toml` 死条目 | `libs.versions.toml` | 移除 `android`、`desugar`、`junit`、`androidx-junit`、`espresso-core`、`ui-tooling-preview-android`、`rikka-hidden-stub` 条目 | — | 低 | 编译通过 |
| 10.5 | 合并 `material3` 重复定义 | `libs.versions.toml`、`build.gradle.kts` | 移除 `libs.material3`（显式版本），统一使用 BOM 管理的 `libs.androidx.material3` | 10.4 | 低 | 编译通过 |

### Phase 11：Model-UI 解耦（可选）

> 目标：将 `ModelField` 体系与 Android View 解耦，为 Compose 配置页迁移扫清障碍
> 风险较高，依赖 Phase 9 拆分完成后再评估

| Task | 标题 | 涉及文件 | 操作 | 前置 | 风险 | 验证 |
|------|------|---------|------|------|------|------|
| 11.1 | 提取 `ModelField` 视图描述 | `ModelField.kt`、`modelFieldExt/*.kt` | 将 `getView()` 返回的 View 创建逻辑提取为 `ModelFieldViewData` 数据类（描述字段类型+选项），不再直接创建 View | Phase 10 | 高 | 编译通过 + 真机验证配置页 |
| 11.2 | Compose 配置页迁移 | `ui/legacy/SettingActivity.kt` | 用 Compose 替换传统 View 配置页，消费 `ModelFieldViewData` 渲染 | 11.1 | 高 | 编译通过 + 真机验证配置 UI |
| 11.3 | 移除传统 View 依赖（评估） | `build.gradle.kts` | 评估移除 `recyclerview`、`viewpager2`、`material`（传统）的可行性，确认无残留引用后移除 | 11.2 | 中 | 编译通过 |

---

## 4. 进度跟踪表

| Task | 标题 | 风险 | 状态 | 提交哈希 |
|------|------|------|------|----------|
| 6.1 | 修复 OldRpcBridge.getVersion() 返回值 | 低 | 完成 | 07228786 |
| 6.2 | 评估并清理 rpcVersion 死字段 | 低 | 完成 | 463ff815 |
| 6.3 | 合并 JsonHelper 至 JsonUtil | 中 | 完成 | cee08adc |
| — | **Phase 6 完成** | — | — | tag: phase-6-done |
| 7.1 | 创建 core/ 包结构 | 低 | 完成 | e0c96641 |
| 7.2 | core/json/ 迁移 | 中 | 完成 | c6158c63 |
| 7.3 | core/log/ 迁移 | 中 | 完成 | dfbed35a |
| 7.4 | core/reflect/ 迁移 | 中 | 完成 | 58eec5a1 |
| 7.5 | core/util/ 迁移 | 中 | 完成 | 43bcceae |
| 7.6 | core/threads/ 迁移 | 中 | 完成 | 787946a4 |
| 7.7 | core/notify/ 迁移 | 中 | 完成 | 00e73f5c |
| 7.8 | core/permission/ 迁移 | 低 | 完成 | abc2728c |
| 7.9 | core/store/ 迁移 | 低 | 完成 | 01f7e14d |
| 7.10 | core/app/ 迁移 | 高 | 完成 | 53446a33（Detector.kt 当时因 JNI 绑定保留原位，已在 Phase 9 移除） |
| — | **Phase 7 完成** | — | — | tag: phase-7-done |
| 8.1 | ApplicationHook 拆分 | 高 | 完成 | f24b7554 |
| 8.2 | AntForest 拆分 | 高 | 完成 | a4f8b99b |
| 8.3 | AntFarm 拆分 | 高 | 完成 | 07bc3ddc |
| 8.4 | AntSports 拆分 | 中 | 完成 | d6add1f3 |
| 8.5 | AntMember 拆分 | 中 | 完成 | 3b44de25 |
| — | **Phase 8 完成** | — | — | tag: phase-8-done |
| 9.1 | Hook 侧调用清理 | 中 | 完成 | e55b2e39 |
| 9.2 | UI 侧调用清理 | 中 | 完成 | 590df987 |
| 9.3 | 删除 Detector.kt 与 AssetUtil checker 常量 | 低 | 完成 | 23d5fb72 |
| 9.4 | 删除 so 二进制与 IDA 残留 | 中 | 完成 | 5e661065 |
| 9.5 | 文档同步 | 低 | 完成 | 99c05f63 |
| — | **Phase 9 完成** | — | — | tag: phase-9-done |
| 10.1 | 移除 @JvmStatic/@JvmField（第一批） | 低 | 完成 | 19395890 |
| 10.2 | 移除 @JvmStatic/@JvmField（第二批） | 低 | 完成 | a075105f |
| 10.3 | 移除 @JvmStatic/@JvmField（第三批） | 低 | 完成 | 3be6779c |
| 10.4 | 清理 libs.versions.toml 死条目 | 低 | 完成 | __HASH__ |
| 10.5 | 合并 material3 重复定义 | 低 | 待开始 | — |
| — | **Phase 10 完成** | — | — | — |
| 11.1 | 提取 ModelField 视图描述 | 高 | 待开始 | — |
| 11.2 | Compose 配置页迁移 | 高 | 待开始 | — |
| 11.3 | 移除传统 View 依赖（评估） | 中 | 待开始 | — |
| — | **Phase 11 完成** | — | — | — |

---

## 附录：@JvmStatic/@JvmField 分布清单

| 文件 | 注解 | 数量 | 对应 Task |
|------|------|------|-----------|
| `model/BaseModel.kt` | `@JvmStatic` | 1 | 10.1 |
| `entity/VitalityStore.kt` | `@JvmStatic` | 2 | 10.1 |
| `entity/RpcEntity.kt` | `@JvmOverloads` | 1 | 10.1 |
| `hook/rpc/bridge/RpcVersion.kt` | `@JvmStatic` | 1 | 10.1 |
| `ui/widget/ListDialog.kt` | `@JvmField`×1, `@JvmStatic`×7 | 8 | 10.2 |
| `ui/adapter/OptionsAdapter.kt` | `@JvmStatic` | 1 | 10.2 |
| `ui/adapter/ListAdapter.kt` | `@JvmField`×2, `@JvmStatic`×2 | 4 | 10.2 |
| `task/antFarm/AntFarm.kt` | `@JvmField`×3, `@JvmStatic`×1 | 4 | 10.3 |
| `task/ModelTask.kt` | `@JvmStatic` | 3 | 10.3 |
| **合计** | | **25** | |

> **注意**：移除前需确认无反射调用（Xposed 框架可能通过反射访问 companion object 成员）。若发现反射依赖则保留对应注解并标注原因。

---

## 附录：libs.versions.toml 死条目清单

| 条目 | 定义位置 | 引用情况 | 对应 Task |
|------|---------|---------|-----------|
| `android` (`com.google.android:android`) | `versions.toml:47` | `build.gradle.kts` 未引用 | 10.4 |
| `desugar` (`com.android.tools:desugar_jdk_libs`) | `versions.toml:84` | `build.gradle.kts:204` 已注释 | 10.4 |
| `junit` (`junit:junit`) | `versions.toml:86` | 测试已禁用，未引用 | 10.4 |
| `androidx-junit` (`androidx.test.ext:junit`) | `versions.toml:87` | 测试已禁用，未引用 | 10.4 |
| `espresso-core` (`androidx.test.espresso:espresso-core`) | `versions.toml:88` | 测试已禁用，未引用 | 10.4 |
| `ui-tooling-preview-android` (`androidx.compose.ui:ui-tooling-preview-android`) | `versions.toml:85` | 未引用（已有 BOM 管理的 `androidx-ui-tooling-preview`） | 10.4 |
| `rikka-hidden-stub` (`dev.rikka.hidden:stub`) | `versions.toml:67` | `build.gradle.kts:146` 已注释 | 10.4 |
| `material3` (`androidx.compose.material3:material3` 显式版本) | `versions.toml:91` | 与 `androidx-material3`（BOM 管理）重复 | 10.5 |

---

## 进度表更新规则

（与 REFACTOR_PLAN.md §5.4 一致）

| 触发时机 | 更新内容 |
|---------|---------|
| Task 完成 | 状态列改为「完成」，提交哈希列填入 commit 短哈希 |
| Task 跳过 | 状态列改为「跳过」，附注说明原因 |
| Task 进行中 | 状态列改为「进行中」 |
| Phase 结束 | 插入分隔行 `| — | **Phase N 完成** | — | — | tag: phase-N-done |` |

> 进度表更新与 git commit 在同一提交中完成，确保表格状态与代码版本始终一致。
