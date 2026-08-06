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
- **不改动 JNI/AIDL**：`Detector` native 方法名、AIDL 包路径不变
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
│   └── app/          // AssetUtil, Detector, Files, IconManager, ModuleStatus,
│                      //   NetworkUtils, StatusManager, SwipeUtil, UnlockUtil,
│                      //   WakeLockManager, CommandUtil, FansirsqiUtil,
│                      //   DirectoryWatcher, defaultBlacklist, TaskBlacklist
├── util/
│   └── maps/         // 保持不动（ID 映射，引用方多）
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

### Phase 9：冗余注解清理与死依赖移除

> 目标：移除无意义的 `@JvmStatic`/`@JvmField` 注解和未使用的依赖条目

| Task | 标题 | 涉及文件 | 操作 | 前置 | 风险 | 验证 |
|------|------|---------|------|------|------|------|
| 9.1 | 移除 `@JvmStatic`/`@JvmField`（第一批） | `BaseModel.kt`、`VitalityStore.kt`、`RpcEntity.kt`、`RpcVersion.kt` | 逐个移除注解，验证无 Kotlin 编译错误 | Phase 8 | 低 | 编译通过 |
| 9.2 | 移除 `@JvmStatic`/`@JvmField`（第二批） | `ListDialog.kt`（8 处）、`OptionsAdapter.kt`（1 处）、`ListAdapter.kt`（4 处） | 移除注解 | 9.1 | 低 | 编译通过 |
| 9.3 | 移除 `@JvmStatic`/`@JvmField`（第三批） | `AntFarm.kt`（4 处）、`ModelTask.kt`（3 处） | 移除注解 | 9.2 | 低 | 编译通过 |
| 9.4 | 清理 `libs.versions.toml` 死条目 | `libs.versions.toml` | 移除 `android`、`desugar`、`junit`、`androidx-junit`、`espresso-core`、`ui-tooling-preview-android`、`rikka-hidden-stub` 条目 | — | 低 | 编译通过 |
| 9.5 | 合并 `material3` 重复定义 | `libs.versions.toml`、`build.gradle.kts` | 移除 `libs.material3`（显式版本），统一使用 BOM 管理的 `libs.androidx.material3` | 9.4 | 低 | 编译通过 |

### Phase 10：Model-UI 解耦（可选）

> 目标：将 `ModelField` 体系与 Android View 解耦，为 Compose 配置页迁移扫清障碍
> 风险较高，依赖 Phase 8 拆分完成后再评估

| Task | 标题 | 涉及文件 | 操作 | 前置 | 风险 | 验证 |
|------|------|---------|------|------|------|------|
| 10.1 | 提取 `ModelField` 视图描述 | `ModelField.kt`、`modelFieldExt/*.kt` | 将 `getView()` 返回的 View 创建逻辑提取为 `ModelFieldViewData` 数据类（描述字段类型+选项），不再直接创建 View | Phase 9 | 高 | 编译通过 + 真机验证配置页 |
| 10.2 | Compose 配置页迁移 | `ui/legacy/SettingActivity.kt` | 用 Compose 替换传统 View 配置页，消费 `ModelFieldViewData` 渲染 | 10.1 | 高 | 编译通过 + 真机验证配置 UI |
| 10.3 | 移除传统 View 依赖（评估） | `build.gradle.kts` | 评估移除 `recyclerview`、`viewpager2`、`material`（传统）的可行性，确认无残留引用后移除 | 10.2 | 中 | 编译通过 |

---

## 4. 进度跟踪表

| Task | 标题 | 风险 | 状态 | 提交哈希 |
|------|------|------|------|----------|
| 6.1 | 修复 OldRpcBridge.getVersion() 返回值 | 低 | 完成 | 07228786 |
| 6.2 | 评估并清理 rpcVersion 死字段 | 低 | 完成 | 463ff815 |
| 6.3 | 合并 JsonHelper 至 JsonUtil | 中 | 完成 | cee08adc |
| — | **Phase 6 完成** | — | — | tag: phase-6-done |
| 7.1 | 创建 core/ 包结构 | 低 | 待开始 | — |
| 7.2 | core/json/ 迁移 | 中 | 待开始 | — |
| 7.3 | core/log/ 迁移 | 中 | 待开始 | — |
| 7.4 | core/reflect/ 迁移 | 中 | 待开始 | — |
| 7.5 | core/util/ 迁移 | 中 | 待开始 | — |
| 7.6 | core/threads/ 迁移 | 中 | 待开始 | — |
| 7.7 | core/notify/ 迁移 | 中 | 待开始 | — |
| 7.8 | core/permission/ 迁移 | 低 | 待开始 | — |
| 7.9 | core/store/ 迁移 | 低 | 待开始 | — |
| 7.10 | core/app/ 迁移 | 高 | 待开始 | — |
| — | **Phase 7 完成** | — | — | — |
| 8.1 | ApplicationHook 拆分 | 高 | 待开始 | — |
| 8.2 | AntForest 拆分 | 高 | 待开始 | — |
| 8.3 | AntFarm 拆分 | 高 | 待开始 | — |
| 8.4 | AntSports 拆分 | 中 | 待开始 | — |
| 8.5 | AntMember 拆分 | 中 | 待开始 | — |
| — | **Phase 8 完成** | — | — | — |
| 9.1 | 移除 @JvmStatic/@JvmField（第一批） | 低 | 待开始 | — |
| 9.2 | 移除 @JvmStatic/@JvmField（第二批） | 低 | 待开始 | — |
| 9.3 | 移除 @JvmStatic/@JvmField（第三批） | 低 | 待开始 | — |
| 9.4 | 清理 libs.versions.toml 死条目 | 低 | 待开始 | — |
| 9.5 | 合并 material3 重复定义 | 低 | 待开始 | — |
| — | **Phase 9 完成** | — | — | — |
| 10.1 | 提取 ModelField 视图描述 | 高 | 待开始 | — |
| 10.2 | Compose 配置页迁移 | 高 | 待开始 | — |
| 10.3 | 移除传统 View 依赖（评估） | 中 | 待开始 | — |
| — | **Phase 10 完成** | — | — | — |

---

## 附录：@JvmStatic/@JvmField 分布清单

| 文件 | 注解 | 数量 | 对应 Task |
|------|------|------|-----------|
| `model/BaseModel.kt` | `@JvmStatic` | 1 | 9.1 |
| `entity/VitalityStore.kt` | `@JvmStatic` | 2 | 9.1 |
| `entity/RpcEntity.kt` | `@JvmOverloads` | 1 | 9.1 |
| `hook/rpc/bridge/RpcVersion.kt` | `@JvmStatic` | 1 | 9.1 |
| `ui/widget/ListDialog.kt` | `@JvmField`×1, `@JvmStatic`×7 | 8 | 9.2 |
| `ui/adapter/OptionsAdapter.kt` | `@JvmStatic` | 1 | 9.2 |
| `ui/adapter/ListAdapter.kt` | `@JvmField`×2, `@JvmStatic`×2 | 4 | 9.2 |
| `task/antFarm/AntFarm.kt` | `@JvmField`×3, `@JvmStatic`×1 | 4 | 9.3 |
| `task/ModelTask.kt` | `@JvmStatic` | 3 | 9.3 |
| **合计** | | **25** | |

> **注意**：移除前需确认无反射调用（Xposed 框架可能通过反射访问 companion object 成员）。若发现反射依赖则保留对应注解并标注原因。

---

## 附录：libs.versions.toml 死条目清单

| 条目 | 定义位置 | 引用情况 | 对应 Task |
|------|---------|---------|-----------|
| `android` (`com.google.android:android`) | `versions.toml:47` | `build.gradle.kts` 未引用 | 9.4 |
| `desugar` (`com.android.tools:desugar_jdk_libs`) | `versions.toml:84` | `build.gradle.kts:204` 已注释 | 9.4 |
| `junit` (`junit:junit`) | `versions.toml:86` | 测试已禁用，未引用 | 9.4 |
| `androidx-junit` (`androidx.test.ext:junit`) | `versions.toml:87` | 测试已禁用，未引用 | 9.4 |
| `espresso-core` (`androidx.test.espresso:espresso-core`) | `versions.toml:88` | 测试已禁用，未引用 | 9.4 |
| `ui-tooling-preview-android` (`androidx.compose.ui:ui-tooling-preview-android`) | `versions.toml:85` | 未引用（已有 BOM 管理的 `androidx-ui-tooling-preview`） | 9.4 |
| `rikka-hidden-stub` (`dev.rikka.hidden:stub`) | `versions.toml:67` | `build.gradle.kts:146` 已注释 | 9.4 |
| `material3` (`androidx.compose.material3:material3` 显式版本) | `versions.toml:91` | 与 `androidx-material3`（BOM 管理）重复 | 9.5 |

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
