# Sesame-TK 重构规划文档

> 生成时间：2026-08-05  
> 基于对项目全部源码（283 个文件：94 Java + 189 Kotlin）的逐包审查

---

## 目录

1. [现状评估](#1-现状评估)
2. [目标架构](#2-目标架构)
3. [分阶段重构计划](#3-分阶段重构计划)
4. [Java → Kotlin 迁移规范](#4-java--kotlin-迁移规范)
5. [验证策略](#5-验证策略)
6. [进度跟踪表](#6-进度跟踪表)

---

## 1. 现状评估

### 1.1 项目概览

| 项目属性 | 值 |
|---------|---|
| 包名 | `fansirsqi.xposed.sesame` |
| 最低 SDK | 26 |
| 编译 SDK | 36 |
| JVM 目标 | 17 |
| 构建系统 | Gradle Kotlin DSL |
| 语言 | Java 17 + Kotlin（混合） |
| 文件总数 | 283（Java 94 / Kotlin 189） |

### 1.2 各包职责与文件统计

| 包路径 | 职责 | Java | Kotlin | 小计 | 主要问题 |
|--------|------|------|--------|------|---------|
| `(root)` | 应用入口 `SesameApplication` | 0 | 1 | 1 | — |
| `data/` | 全局配置、运行时信息、状态 | 3 | 2 | 5 | `Config.java` 重度 Lombok + Jackson |
| `entity/` | 数据实体（用户、区域、RPC等） | 7 | 11 | 18 | Java 实体部分用 Lombok |
| `extensions/` | JSON 扩展函数 | 0 | 1 | 1 | — |
| `hook/` | Hook 核心、验证码、RPC 桥 | 0 | 15 | 15 | `ApplicationHook` 977 行过胖 |
| `hook.internal/` | 支付宝内部 API Helper | 0 | 4 | 4 | — |
| `hook.keepalive/` | 智能调度保活 | 0 | 1 | 1 | — |
| `hook.lsp100/` | LSPosed API 100 入口 | 0 | 1 | 1 | **高危：字符串引用入口** |
| `hook.rpc.bridge/` | RPC 桥接口与实现 | 4 | 0 | 4 | **高危：反射调用支付宝类** |
| `hook.rpc.debug/` | RPC 调试 | 2 | 0 | 2 | — |
| `hook.rpc.intervallimit/` | RPC 间隔限制 | 0 | 4 | 4 | — |
| `hook.server/` | HTTP 服务器 | 0 | 3 | 3 | — |
| `hook.server.handlers/` | HTTP 处理器 | 0 | 6 | 6 | — |
| `hook.simple/` | 简单模拟/滑块验证 | 0 | 7 | 7 | — |
| `hook.simple.xpcompat/` | Xposed 兼容引擎 | 0 | 2 | 2 | — |
| `hook.xp82/` | Xposed API 82 入口 | 0 | 1 | 1 | **高危：字符串引用入口** |
| `model/` | 模型框架（配置字段体系） | 6 | 4 | 10 | **核心框架，Java + Lombok** |
| `model.modelFieldExt/` | 模型字段子类 | 11 | 0 | 11 | 全 Java，Lombok，UI View 代码混入 |
| `net/` | 网络 API 客户端 | 0 | 1 | 1 | — |
| `service/` | 后台服务（CommandService等） | 0 | 3 | 3 | — |
| `service.patch/` | Root Shell 补丁 | 0 | 1 | 1 | — |
| `task/` | 任务框架（MainTask, ModelTask等） | 4 | 4 | 8 | Java/Kotlin 混杂 |
| `task.AnswerAI/` | AI 答题 | 6 | 0 | 6 | 全 Java，部分用 Lombok |
| `task.antCooperate/` | 合种 | 0 | 2 | 2 | — |
| `task.antDodo/` | 神奇物种 | 2 | 0 | 2 | Java RpcCall |
| `task.antFarm/` | 蚂蚁庄园 | 2 | 4 | 6 | 混合 |
| `task.antFishPond/` | 鱼塘 | 0 | 4 | 4 | — |
| `task.antForest/` | 蚂蚁森林 | 3 | 14 | 17 | 森林最大模块 |
| `task.antMember/` | 蚂蚁会员 | 1 | 1 | 2 | — |
| `task.antOcean/` | 蚂蚁海洋 | 2 | 0 | 2 | Java |
| `task.antOrchard/` | 蚂蚁果园 | 0 | 3 | 3 | — |
| `task.antSports/` | 蚂蚁运动 | 0 | 2 | 2 | — |
| `task.antStall/` | 蚂蚁摊位 | 0 | 4 | 4 | — |
| `task.customTasks/` | 自定义/手动任务 | 0 | 3 | 3 | — |
| `task.EcoProtection/` | 环保 | 0 | 2 | 2 | — |
| `task.greenFinance/` | 绿色金融 | 2 | 0 | 2 | Java |
| `task.other/` | 其他任务 | 0 | 1 | 1 | — |
| `task.other.credit2101/` | 信用 | 0 | 2 | 2 | — |
| `task.other.haojia/` | 好价 | 0 | 2 | 2 | — |
| `task.reserve/` | 预约 | 2 | 0 | 2 | Java |
| `ui/` | UI Activity | 3 | 6 | 9 | 两套 UI 体系并存 |
| `ui.adapter/` | View 适配器 | 4 | 1 | 5 | Java，传统 View |
| `ui.compose/` | Compose 组件 | 0 | 1 | 1 | — |
| `ui.dto/` | UI 数据传输对象 | 4 | 0 | 4 | 全 Java |
| `ui.extension/` | UI 扩展 | 0 | 5 | 5 | — |
| `ui.model/` | UI 模型 | 0 | 1 | 1 | — |
| `ui.navigation/` | 导航 | 0 | 1 | 1 | — |
| `ui.repository/` | 配置仓库 | 0 | 1 | 1 | — |
| `ui.screen/` | Compose 屏幕 | 0 | 6 | 6 | — |
| `ui.screen.card/` | Compose 卡片组件 | 0 | 4 | 4 | — |
| `ui.screen.components/` | Compose 组件 | 0 | 8 | 8 | — |
| `ui.screen.content/` | Compose 内容 | 0 | 3 | 3 | — |
| `ui.theme/` | 主题 | 0 | 4 | 4 | — |
| `ui.viewmodel/` | ViewModel | 0 | 5 | 5 | — |
| `ui.widget/` | 传统对话框 | 3 | 0 | 3 | 全 Java |
| `util/` | 工具类 | 14 | 24 | 38 | Java 工具类多，JsonUtil/TypeUtil 核心 |
| `util.maps/` | ID 映射 | 8 | 3 | 11 | 多数 Java |

### 1.3 技术栈现状

| 领域 | 使用的方案 | 说明 |
|------|-----------|------|
| JSON | Jackson（`jackson-core/databind/annotations` + `jackson.kotlin`）+ `org.json.JSONObject` + `kotlinx.serialization` | **三套并存**。`JsonUtil.java` 封装 Jackson；RpcCall 中用 `org.json`；部分 Kotlin 文件用 `kotlinx.serialization` |
| UI | 传统 View（ViewBinding + RecyclerView + ViewPager2）+ Jetpack Compose（Material 3） | **两套并存**。新代码已倾向 Compose |
| 代码生成 | Lombok（`compileOnly` + `annotationProcessor`） | 18+ 文件使用 `@Data`/`@Getter`/`@Setter` |
| 异步 | Kotlin 协程（`kotlinx.coroutines`）+ 传统线程池（`GlobalThreadPools`） | `ModelTask` 已迁移到协程 |
| 网络 | OkHttp + NanoHTTPD + 反射调用支付宝 RPC | — |
| ML | TensorFlow Lite（滑块验证） | — |
| Hook | Xposed API 82 + API 100（双入口） | — |

### 1.4 主要问题清单

#### 结构问题
1. **按技术分层而非功能域**：`task/` 下各功能域（antForest、antFarm 等）自包含 Model + RpcCall，但公共能力散落在 `util/`、`entity/`、`model/` 中，缺乏清晰的 core 边界
2. **`ApplicationHook` 过胖**（977 行）：集中了进程管理、Hook 注册、广播接收、任务调度、RPC 初始化、定时唤醒等职责
3. **`model/` 与 `model.modelFieldExt/` 混合了 UI View 代码**：`ModelField.getView()` 等方法直接创建 Android View（`Switch`、`MaterialButton`），数据模型与 UI 强耦合
4. **`util/` 过大**（38 文件）：日志、时间、JSON、权限、通知、文件、线程池等全部平铺在一个包下
5. **`util.maps/` 命名不一致**：`ReserveaMap`（拼写错误，应为 `ReserveMap`）

#### 重复/过时代码
6. **`TaskExecutor.kt` 包名错误**：文件在 `task/` 目录但 `package` 声明为 `fansirsqi.xposed.sesame.util`，且包含游戏上报逻辑，命名误导
7. **JSON 三套方案并存**，缺乏统一策略
8. **Lombok 与 Kotlin data class 并存**，增加编译复杂度
9. **`OldRpcBridge.getVersion()` 返回 `RpcVersion.NEW`**（疑似 bug 或遗留代码）

#### 潜在风险点
10. `Config.java` 使用 Jackson `readerForUpdating(INSTANCE)` 反序列化单例——迁移时需保持序列化兼容性
11. `ModelField` 使用 `TypeUtil.getTypeArgument(this.getClass().getGenericSuperclass(), 0)` 反射获取泛型类型——Java→Kotlin 迁移时泛型签名可能变化
12. `Model.modelClazzList` 通过 `ModelOrder.INSTANCE.getAllConfig()` 反射实例化各 Model 子类——子类改名需同步更新 `ModelOrder`

### 1.5 风险清单（不可动/高危区域）

#### 🔴 极高危——字符串引用的 Hook 入口

| 类全限定名 | 引用位置 | 说明 |
|-----------|---------|------|
| `fansirsqi.xposed.sesame.hook.xp82.HookEntry` | `assets/xposed_init` | Xposed API 82 入口，**不可改名/移动** |
| `fansirsqi.xposed.sesame.hook.lsp100.HookEntry` | `META-INF/xposed/java_init.list` | LSPosed API 100 入口，**不可改名/移动** |

#### 🔴 极高危——支付宝类字符串引用（Hook 目标）

以下字符串在代码中被 `XposedHelpers.findClass()` / `ClassLoader.loadClass()` 引用，属于**支付宝侧类名**，与本项目重构无关，但相关代码块不可随意拆分：

| 支付宝类全限定名 | 引用文件 |
|-----------------|---------|
| `com.alipay.mobile.framework.AlipayApplication` | `ApplicationHook.kt` |
| `com.alipay.mobile.personalbase.service.SocialSdkContactService` | `ApplicationHook.kt` |
| `com.alipay.mobile.quinox.LauncherActivity` | `ApplicationHook.kt` |
| `android.app.LoadedApk` | `ApplicationHook.kt` |
| `com.alipay.mobile.nebulacore.Nebula` | `NewRpcBridge.java` |
| `com.alibaba.ariver.commonability.network.rpc.RpcBridgeExtension` | `NewRpcBridge.java` |
| `com.alibaba.fastjson.JSON` / `com.alibaba.fastjson.JSONObject` | `NewRpcBridge.java`, `OldRpcBridge.java`, `General.kt` |
| `com.alibaba.ariver.engine.api.bridge.extension.BridgeCallback` | `NewRpcBridge.java`, `HookUtil.kt` |
| `com.alipay.mobile.nebulaappproxy.api.rpc.H5RpcUtil` / `H5Response` | `OldRpcBridge.java` |
| `com.alipay.mobile.h5container.api.H5Page` | `OldRpcBridge.java`, `General.kt` |
| `com.alipay.dexaop.power.RuntimePowerService` | `General.kt`（`CURRENT_USING_SERVICE`） |
| `com.eg.android.AlipayGphone.AlipayLogin` | `General.kt`（`CURRENT_USING_ACTIVITY`） |
| `com.alipay.mobile.socialcommonsdk.bizdata.UserIndependentCache` | `HookUtil.kt` |
| `com.alipay.mobile.socialcommonsdk.bizdata.contact.data.AliAccountDaoOp` | `HookUtil.kt` |
| `com.alibaba.ariver.app.api.App` / `Page` | `NewRpcBridge.java`, `HookUtil.kt` |
| `com.alibaba.ariver.engine.api.bridge.model.ApiContext` | `NewRpcBridge.java`, `HookUtil.kt` |
| `com.alibaba.health.pedometer.intergation.rpc.RpcManager` | `AntSports.kt` |
| `com.alipay.mobile.nebulax.xriver.activity.XRiverActivity` 等 | `ApplicationHook.kt`（验证码页面处理器） |

#### 🟡 高危——ProGuard keep 规则

`proguard-rules.pro` 中 `-keep class fansirsqi.xposed.sesame.** { *; }` 保留了所有项目类，这意味着改名后 proguard 规则仍然生效（通配符）。但以下特定 keep 需关注：
- Jackson 注解成员：`@com.fasterxml.jackson.annotation.** *;`——迁移到 Kotlin 后若使用 `kotlinx.serialization` 则此规则可移除
- `Serializable` 实现类：`ModelConfig` 等实现了 `Serializable`

#### 🟡 高危——RPC 协议类

| 类 | 说明 |
|----|------|
| `RpcEntity` | RPC 请求/响应实体，被所有 RpcCall 使用，Jackson 序列化 |
| `RpcBridge` | RPC 桥接口，`NewRpcBridge`/`OldRpcBridge` 实现 |
| `DebugRpc` / `DebugRpcCall` | RPC 调试入口 |

#### 🟡 高危——反射实例化的 Model 子类

`Model.initAllModel()` 通过 `ModelOrder` 列表反射实例化所有 Model 子类。`ModelOrder.kt` 维护类列表，任何 Model 子类的改名/移动必须同步更新 `ModelOrder`。

#### 🟡 高危——JNI/NDK 相关

| 类 | 说明 |
|----|------|
| `Detector.kt` | 声明 `external` 方法，依赖 `src/main/cpp/` 原生代码，**不可改名**（native 方法名与 JNI 绑定） |
| `src/main/cpp/` | C++ NDK 源码，不在本次重构范围 |

#### 🟡 高危——AIDL

| 文件 | 说明 |
|------|------|
| `aidl/fansirsqi/xposed/sesame/ICallback.aidl` | AIDL 接口，生成 Java 存根 |
| `aidl/fansirsqi/xposed/sesame/ICommandService.aidl` | AIDL 接口 |
| `aidl/fansirsqi/xposed/sesame/IStatusListener.aidl` | AIDL 接口 |

AIDL 生成的类全限定名与包路径绑定，**不可随意改名**。

---

## 2. 目标架构

### 2.1 目标包结构树

```
fansirsqi.xposed.sesame
├── SesameApplication.kt              // 应用入口（不动）
├── core/                               // 公共能力层（新增）
│   ├── config/                         // 配置管理（从 data/ 迁入）
│   │   ├── Config.kt                  // ← Config.java
│   │   ├── RuntimeInfo.kt             // ← RuntimeInfo.java
│   │   └── General.kt                 // ← data/General.kt（不动）
│   ├── data/                           // 状态数据
│   │   ├── Status.kt                  // ← data/Status.kt（不动）
│   │   ├── StatusFlags.kt             // ← data/StatusFlags.java
│   │   └── TaskCommon.kt              // ← task/TaskCommon.java
│   ├── json/                           // JSON 统一入口（新）
│   │   └── JsonUtil.kt                 // ← util/JsonUtil.java
│   ├── log/                            // 日志
│   │   ├── Log.kt                      // ← util/Log.kt（不动）
│   │   └── Logback.kt                 // ← util/Logback.kt（不动）
│   ├── reflect/                        // 反射工具
│   │   └── TypeUtil.kt                // ← util/TypeUtil.java
│   ├── util/                           // 通用工具
│   │   ├── Files.kt                   // ← util/Files.kt（不动）
│   │   ├── TimeUtil.kt                // ← util/TimeUtil.java
│   │   ├── StringUtil.kt              // ← util/StringUtil.java
│   │   ├── RandomUtil.kt             // ← util/RandomUtil.java
│   │   ├── ListUtil.kt                // ← util/ListUtil.java
│   │   ├── HanziToPinyin.kt           // ← util/HanziToPinyin.java
│   │   ├── LanguageUtil.kt           // ← util/LanguageUtil.java
│   │   ├── PortUtil.kt                // ← util/PortUtil.java
│   │   ├── TypeUtil.kt                // （如上 reflect/）
│   │   └── ...其他纯工具
│   ├── notify/                        // 通知
│   │   ├── Notify.kt                  // ← util/Notify.kt（不动）
│   │   └── ToastUtil.kt              // ← util/ToastUtil.kt（不动）
│   ├── permission/                    // 权限
│   │   └── PermissionUtil.kt         // ← util/PermissionUtil.kt（不动）
│   ├── threads/                       // 线程/协程
│   │   ├── GlobalThreadPools.kt       // ← util/GlobalThreadPools.kt（不动）
│   │   └── CoroutineUtils.kt         // ← util/CoroutineUtils.kt（不动）
│   └── store/                        // 数据存储
│       └── DataStore.kt              // ← util/DataStore.kt（不动）
├── entity/                            // 数据实体（精简）
│   ├── AlipayUser.kt                 // ← AlipayUser.java
│   ├── AlipayVersion.kt              // （不动）
│   ├── AreaCode.kt                   // ← AreaCode.java
│   ├── MapperEntity.kt               // （不动）
│   ├── RpcEntity.kt                  // （不动，高危）
│   ├── UserEntity.kt                 // （不动）
│   └── ...其他实体
├── model/                             // 配置模型框架
│   ├── Model.kt                      // ← Model.java
│   ├── ModelConfig.kt                // ← ModelConfig.java
│   ├── ModelField.kt                 // ← ModelField.java
│   ├── ModelFields.kt               // ← ModelFields.java
│   ├── ModelGroup.kt                 // ← ModelGroup.java
│   ├── ModelType.kt                  // ← ModelType.java
│   ├── BaseModel.kt                  // （不动）
│   ├── ModelOrder.kt                 // （不动，需同步更新）
│   └── fields/                        // ← modelFieldExt/（重命名）
│       ├── BooleanModelField.kt
│       ├── ChoiceModelField.kt
│       ├── IntegerModelField.kt
│       ├── ListModelField.kt
│       ├── SelectModelField.kt
│       ├── StringModelField.kt
│       ├── TextModelField.kt
│       └── EmptyModelField.kt
├── hook/                              // Hook 层（结构基本不动）
│   ├── ApplicationHook.kt             // （不动，但后续可拆分）
│   ├── ...（现有结构保持）
│   ├── rpc/
│   │   ├── bridge/                   // RPC 桥（不动，高危）
│   │   ├── debug/                    // RPC 调试
│   │   └── intervallimit/            // 间隔限制
│   ├── lsp100/                       // 入口（不动）
│   ├── xp82/                         // 入口（不动）
│   └── ...
├── task/                              // 任务层（按功能域组织）
│   ├── ModelTask.kt                  // 任务基类（不动）
│   ├── MainTask.kt                   // （不动）
│   ├── TaskRunner.kt                 // （不动）
│   ├── TaskStatus.kt                 // ← TaskStatus.java
│   ├── TaskRunnerAdapter.kt         // ← TaskRunnerAdapter.java
│   ├── ChildTaskExecutor.kt         // ← ChildTaskExecutor.java
│   ├── antForest/                    // 蚂蚁森林
│   ├── antFarm/                      // 蚂蚁庄园
│   ├── antFishPond/                  // 鱼塘
│   ├── ...（各功能域自包含）
│   └── AnswerAI/                     // AI 答题
├── service/                           // 后台服务（不动）
├── net/                               // 网络（不动）
├── extensions/                        // 扩展函数（不动）
└── ui/                                // UI 层（保持现有 Compose 迁移方向）
    ├── ...（现有结构保持）
    └── legacy/                        // 标记待移除的传统 View 代码
        ├── SettingActivity.kt         // ← SettingActivity.java
        ├── WebSettingsActivity.kt    // ← WebSettingsActivity.java
        └── widget/                    // ← ui/widget/（传统对话框）
```

### 2.2 包依赖方向规则

```
ui/ → model/, core/, entity/, task/
task/ → model/, entity/, hook/, core/
hook/ → model/, entity/, core/
model/ → entity/, core/
entity/ → core/ (仅日志/反射)
core/ → (无内部依赖，最底层)
```

**禁止反向依赖**：`core/` 不可依赖 `model/`/`task/`/`hook/`/`ui/`。  
**禁止跨层依赖**：`ui/` 不可直接依赖 `hook/`（通过 `task/` 间接交互）。

---

## 3. 分阶段重构计划

### Phase 0：建立基线与保护网

> 目标：确保当前代码能编译通过，建立重构基线

| Task | 标题 | 涉及文件/包 | 操作 | 前置 | 风险 | 验证 |
|------|------|------------|------|------|------|------|
| 0.1 | 编译基线验证 | 全项目 | 验证 | — | 低 | `./gradlew :app:assembleDebug` 通过 |
| 0.2 | 修复已知包名错误 | `task/TaskExecutor.kt` | 修正 `package` 声明为 `fansirsqi.xposed.sesame.task` | 0.1 | 低 | 编译通过 |
| 0.3 | 修复拼写错误 `ReserveaMap` | `util/maps/ReserveaMap.java` | 重命名为 `ReserveMap.java`（类名+文件名+所有引用） | 0.1 | 低 | 编译通过 |

### Phase 1：util/ 公共工具类 Kotlin 化

> 目标：将 `util/` 下的 Java 工具类迁移为 Kotlin，同时迁入 `core/` 包  
> 原则：先纯工具类（无外部依赖），后有依赖的工具类

| Task | 标题 | 涉及文件 | 操作 | 前置 | 风险 | 验证 |
|------|------|---------|------|------|------|------|
| 1.1 | `TypeUtil.java` → Kotlin | `util/TypeUtil.java` | Java→Kotlin，转 `object` | 0.2 | 低 | 编译通过 |
| 1.2 | `StringUtil.java` → Kotlin | `util/StringUtil.java` | Java→Kotlin，转 `object` | 0.2 | 低 | 编译通过 |
| 1.3 | `ListUtil.java` → Kotlin | `util/ListUtil.java` | Java→Kotlin，转 `object` | 0.2 | 低 | 编译通过 |
| 1.4 | `TimeUtil.java` → Kotlin | `util/TimeUtil.java` | Java→Kotlin，转 `object`，保留 `@JvmStatic` | 0.2 | 低 | 编译通过 |
| 1.5 | `TimeFormatter.java` → Kotlin | `util/TimeFormatter.java` | Java→Kotlin | 0.2 | 低 | 编译通过 |
| 1.6 | `TimeCounter.java` → Kotlin | `util/TimeCounter.java` | Java→Kotlin | 0.2 | 低 | 编译通过 |
| 1.7 | `RandomUtil.java` → Kotlin | `util/RandomUtil.java` | Java→Kotlin，转 `object` | 0.2 | 低 | 编译通过 |
| 1.8 | `Average.java` → Kotlin | `util/Average.java` | Java→Kotlin | 0.2 | 低 | 编译通过 |
| 1.9 | `CircularFifoQueue.java` → Kotlin | `util/CircularFifoQueue.java` | Java→Kotlin | 0.2 | 低 | 编译通过 |
| 1.10 | `HanziToPinyin.java` → Kotlin | `util/HanziToPinyin.java` | Java→Kotlin，转 `object`，被 `MapperEntity` 依赖 | 0.2 | 低 | 编译通过 |
| 1.11 | `LanguageUtil.java` → Kotlin | `util/LanguageUtil.java` | Java→Kotlin | 0.2 | 低 | 编译通过 |
| 1.12 | `PortUtil.java` → Kotlin | `util/PortUtil.java` | Java→Kotlin | 0.2 | 低 | 编译通过 |
| 1.13 | `ResChecker.java` → Kotlin | `util/ResChecker.java` | Java→Kotlin | 0.2 | 低 | 编译通过 |
| 1.14 | `JsonUtil.java` → Kotlin | `util/JsonUtil.java` | Java→Kotlin，转 `object`，保留 Jackson `ObjectMapper`，被 `Config`/`ModelField` 依赖 | 1.1 | 中 | 编译通过 |
| 1.15 | `util/maps/` Java 文件 → Kotlin | `BeachMap.java`、`CooperateMap.java`、`IdMapManager.java`、`MemberBenefitsMap.java`、`ParadiseCoinBenefitIdMap.java`、`VipDataIdMap.java`、`VitalityRewardsMap.java` | Java→Kotlin，逐个迁移 | 0.3 | 低 | 每个文件编译通过 |

### Phase 2：model/entity 数据类 Kotlin 化（去 Lombok）

> 目标：将 `model/` 和 `entity/` 下的 Java 文件迁移为 Kotlin，移除 Lombok 依赖

| Task | 标题 | 涉及文件 | 操作 | 前置 | 风险 | 验证 |
|------|------|---------|------|------|------|------|
| 2.1 | `ModelFields.java` → Kotlin | `model/ModelFields.java` | Java→Kotlin，改 `class ModelFields : LinkedHashMap<String, ModelField<*>>()` | 1.14 | 中 | 编译通过 |
| 2.2 | `ModelGroup.java` → Kotlin | `model/ModelGroup.java` | Java→Kotlin，`enum`，去 `@Getter` | 0.2 | 低 | 编译通过 |
| 2.3 | `ModelType.java` → Kotlin | `model/ModelType.java` | Java→Kotlin，`enum` | 0.2 | 低 | 编译通过 |
| 2.4 | `ModelConfig.java` → Kotlin | `model/ModelConfig.java` | Java→Kotlin，`data class`，去 `@Data`，保留 `Serializable` | 2.1 | 中 | 编译通过 |
| 2.5 | `ModelField.java` → Kotlin | `model/ModelField.java` | Java→Kotlin，`abstract class`，去 `@Data`/`@Getter`/`@Setter`，**注意 `TypeUtil.getTypeArgument` 泛型反射** | 1.1, 1.14 | 高 | 编译通过 + 真机验证配置加载 |
| 2.6 | `Model.java` → Kotlin | `model/Model.java` | Java→Kotlin，`abstract class`，去 `@Getter` | 2.4, 2.5 | 中 | 编译通过 + 真机验证模型初始化 |
| 2.7 | `modelFieldExt/` 全部 → Kotlin | 11 个 Java 文件（`BooleanModelField` 等） | Java→Kotlin，逐个迁移，注意 `getView()` 中 Android View 代码暂保留 | 2.5 | 中 | 每个文件编译通过 |
| 2.8 | `entity/AlipayUser.java` → Kotlin | `entity/AlipayUser.java` | Java→Kotlin，`Filter` 接口→函数类型 | 0.2 | 低 | 编译通过 |
| 2.9 | `entity/AreaCode.java` → Kotlin | `entity/AreaCode.java` | Java→Kotlin | 0.2 | 低 | 编译通过 |
| 2.10 | `entity/AlipayBeach.java` → Kotlin | `entity/AlipayBeach.java` | Java→Kotlin | 0.2 | 低 | 编译通过 |
| 2.11 | `entity/CollectEnergyEntity.java` → Kotlin | `entity/CollectEnergyEntity.java` | Java→Kotlin，去 `@Getter` | 0.2 | 低 | 编译通过 |
| 2.12 | `entity/FriendWatch.java` → Kotlin | `entity/FriendWatch.java` | Java→Kotlin，去 `@Getter`/`@Setter` | 0.2 | 低 | 编译通过 |
| 2.13 | `entity/ParadiseCoinBenefit.java` → Kotlin | `entity/ParadiseCoinBenefit.java` | Java→Kotlin | 0.2 | 低 | 编译通过 |
| 2.14 | `entity/ReserveEntity.java` → Kotlin | `entity/ReserveEntity.java` | Java→Kotlin | 0.2 | 低 | 编译通过 |

### Phase 3：data/ 与 task/ 框架类 Kotlin 化

> 目标：将配置管理和任务框架的剩余 Java 文件迁移为 Kotlin

| Task | 标题 | 涉及文件 | 操作 | 前置 | 风险 | 验证 |
|------|------|---------|------|------|------|------|
| 3.1 | `data/Config.java` → Kotlin | `data/Config.java` | Java→Kotlin，`object Config`，去 `@Data`，**保留 Jackson 序列化兼容性**（`@JsonIgnoreProperties` 保留） | 1.14, 2.4 | 高 | 编译通过 + 真机验证配置加载/保存 |
| 3.2 | `data/RuntimeInfo.java` → Kotlin | `data/RuntimeInfo.java` | Java→Kotlin | 0.2 | 低 | 编译通过 |
| 3.3 | `data/StatusFlags.java` → Kotlin | `data/StatusFlags.java` | Java→Kotlin | 0.2 | 低 | 编译通过 |
| 3.4 | `task/TaskCommon.java` → Kotlin | `task/TaskCommon.java` | Java→Kotlin，`object` | 0.2 | 低 | 编译通过 |
| 3.5 | `task/TaskStatus.java` → Kotlin | `task/TaskStatus.java` | Java→Kotlin | 0.2 | 低 | 编译通过 |
| 3.6 | `task/TaskRunnerAdapter.java` → Kotlin | `task/TaskRunnerAdapter.java` | Java→Kotlin | 0.2 | 低 | 编译通过 |
| 3.7 | `task/ChildTaskExecutor.java` → Kotlin | `task/ChildTaskExecutor.java` | Java→Kotlin | 0.2 | 低 | 编译通过 |

### Phase 4：hook/rpc/ 与 task 各功能域整理

> 目标：将 RPC 桥和各功能域的 Java RpcCall 迁移为 Kotlin

| Task | 标题 | 涉及文件 | 操作 | 前置 | 风险 | 验证 |
|------|------|---------|------|------|------|------|
| 4.1 | `hook/rpc/bridge/RpcVersion.java` → Kotlin | `RpcVersion.java` | Java→Kotlin，`enum` | 0.2 | 低 | 编译通过 |
| 4.2 | `hook/rpc/bridge/RpcBridge.java` → Kotlin | `RpcBridge.java` | Java→Kotlin，`interface`，保留 `default` 方法→Kotlin 默认实现 | 4.1 | 中 | 编译通过 |
| 4.3 | `hook/rpc/bridge/NewRpcBridge.java` → Kotlin | `NewRpcBridge.java` | Java→Kotlin，**高危：大量反射调用支付宝类** | 4.2 | 高 | 编译通过 + 真机验证 RPC |
| 4.4 | `hook/rpc/bridge/OldRpcBridge.java` → Kotlin | `OldRpcBridge.java` | Java→Kotlin，**高危：大量反射调用支付宝类** | 4.2 | 高 | 编译通过 + 真机验证 RPC |
| 4.5 | `hook/rpc/debug/DebugRpc.java` → Kotlin | `DebugRpc.java` | Java→Kotlin | 0.2 | 低 | 编译通过 |
| 4.6 | `hook/rpc/debug/DebugRpcCall.java` → Kotlin | `DebugRpcCall.java` | Java→Kotlin | 0.2 | 低 | 编译通过 |
| 4.7 | `task/antForest/AntForestRpcCall.java` → Kotlin | `AntForestRpcCall.java` | Java→Kotlin，`object` | 3.4 | 中 | 编译通过 |
| 4.8 | `task/antForest/GreenLife.java` → Kotlin | `GreenLife.java` | Java→Kotlin | 0.2 | 低 | 编译通过 |
| 4.9 | `task/antForest/Healthcare.java` → Kotlin | `Healthcare.java` | Java→Kotlin | 0.2 | 低 | 编译通过 |
| 4.10 | 各功能域 `*RpcCall.java` → Kotlin | `AntFarmRpcCall`、`DadaDailyRpcCall`、`AntMemberRpcCall`、`AntOceanRpcCall`、`AntDodoRpcCall`、`AntDodo`、`AntOcean`、`Reserve`、`ReserveRpcCall`、`GreenFinance`、`GreenFinanceRpcCall` | Java→Kotlin，逐个迁移 | 3.4 | 中 | 每个文件编译通过 |
| 4.11 | `task/AnswerAI/` 全部 → Kotlin | 6 个 Java 文件 | Java→Kotlin，去 `@Getter`/`@Setter` | 0.2 | 中 | 编译通过 |

### Phase 5（可选）：JSON/UI 技术栈收敛

> 目标：评估并逐步收敛技术栈，低优先级

| Task | 标题 | 涉及文件 | 操作 | 前置 | 风险 | 验证 |
|------|------|---------|------|------|------|------|
| 5.1 | 评估 JSON 方案统一 | 全项目 | 评估 Jackson vs `kotlinx.serialization`，输出报告 | Phase 4 | 低 | — |
| 5.2 | 移除 Lombok 依赖 | `build.gradle.kts` | 在 Phase 2-4 完成后移除 `compileOnly(libs.lombok)` + `annotationProcessor(libs.lombok)` | Phase 4 | 低 | 编译通过 |
| 5.3 | UI 向 Compose 收敛 | `ui/SettingActivity.java`、`ui/WebSettingsActivity.java`、`ui/widget/*.java`、`ui/adapter/*.java` | 逐步用 Compose 替换传统 View | Phase 4 | 中 | 编译通过 + 真机验证 UI |
| 5.4 | `ui/dto/` Java 文件 → Kotlin | 4 个 Java DTO | Java→Kotlin，`data class` | 0.2 | 低 | 编译通过 |
| 5.5 | `ui/widget/` Java 文件 → Kotlin | `ChoiceDialog`、`ListDialog`、`StringDialog` | Java→Kotlin | 0.2 | 低 | 编译通过 |
| 5.6 | `ui/adapter/` Java 文件 → Kotlin | `ContentPagerAdapter`、`ListAdapter`、`OptionsAdapter`、`TabAdapter` | Java→Kotlin | 0.2 | 低 | 编译通过 |
| 5.7 | `ui/ObjReference.java` → Kotlin | `ui/ObjReference.java` | Java→Kotlin，去 `@Data` | 0.2 | 低 | 编译通过 |

---

## 4. Java → Kotlin 迁移规范

### 4.1 命名规范

| 元素 | 规范 | 示例 |
|------|------|------|
| 文件名 | PascalCase，与类名一致 | `JsonUtil.kt` → `JsonUtil.kt` |
| 类名 | PascalCase | `Config` → `Config` |
| 函数名 | camelCase | `formatJson()` → `formatJson()` |
| 常量 | UPPER_SNAKE_CASE | `TAG` → `TAG` |
| 伴生对象 | `companion object` 替代 `static` | — |
| `object` 单例 | PascalCase | `object JsonUtil` |

### 4.2 常见转换模式

#### Lombok `@Data` → `data class`

```kotlin
// Java: @Data public class ModelConfig implements Serializable { ... }
// Kotlin:
data class ModelConfig(
    var code: String = "",
    var name: String = "",
    var group: ModelGroup = ModelGroup.BASE,
    var icon: String = ""
) : Serializable {
    val fields: ModelFields = ModelFields()
    // ...
}
```

#### Lombok `@Getter`/`@Setter` → Kotlin 属性

```kotlin
// Java: @Getter private String code;
// Kotlin:
var code: String = ""
    private set
```

#### 静态工具类 → `object`

```kotlin
// Java: public class JsonUtil { public static String formatJson(Object o) { ... } }
// Kotlin:
object JsonUtil {
    fun formatJson(o: Any?): String { ... }
}
```

#### 静态常量 → `const val`

```kotlin
// Java: private static final String TAG = "JsonUtil";
// Kotlin:
private const val TAG = "JsonUtil"
```

#### 回调接口 → 函数类型

```kotlin
// Java: interface Filter { Boolean apply(UserEntity user); }
// Kotlin:
// 使用函数类型 (UserEntity) -> Boolean 替代
```

#### `@JvmStatic` / `@JvmField` 保留规则

- 当 Java 代码仍调用该成员时，**必须**保留 `@JvmStatic` / `@JvmField`
- 迁移完所有 Java 调用方后可移除
- `ApplicationHook` 的 companion object 中大量使用 `@JvmStatic`/`@JvmField`，因被 Java RpcCall 调用，**暂不移除**

### 4.3 禁止事项

1. **不做超出迁移范围的逻辑重写**——只做语言转换，不改业务逻辑
2. **不改变公开 API 的行为**——方法签名、返回值语义保持一致
3. **不改变序列化格式**——Jackson 注解（`@JsonIgnore`、`@JsonIgnoreProperties`）迁移后保留
4. **不改变泛型签名**——`ModelField<T>` 的泛型被 `TypeUtil` 反射读取，Kotlin 转换后需验证泛型签名一致
5. **不移动 Hook 入口类**——`hook.xp82.HookEntry` 和 `hook.lsp100.HookEntry` 的全限定名不可改变
6. **不移动 JNI 相关类**——`Detector` 的 native 方法名与 C++ 绑定
7. **不改动 `cpp/` 和 `libs/` 下的内容**

---

## 5. 验证策略

### 5.1 每个 Task 完成后的验证命令

```bash
# 快速编译检查（推荐每次 Task 后执行）
./gradlew :app:compileDebugKotlin

# 完整 APK 构建（每个 Phase 结束后执行）
./gradlew :app:assembleDebug
```

### 5.2 Phase 验收标准

| Phase | 验收标准 |
|-------|---------|
| Phase 0 | `assembleDebug` 通过，无编译警告增加 |
| Phase 1 | `assembleDebug` 通过，`util/` 下无 Java 文件（或仅剩无法迁移的） |
| Phase 2 | `assembleDebug` 通过，`model/` 和 `entity/` 下无 Java 文件，Lombok 仅在 `task/AnswerAI/` 等后置项中残留 |
| Phase 3 | `assembleDebug` 通过，`data/` 和 `task/` 框架类全 Kotlin |
| Phase 4 | `assembleDebug` 通过，`hook/rpc/` 全 Kotlin，各功能域 RpcCall 全 Kotlin |
| Phase 5 | `assembleDebug` 通过，Lombok 依赖已移除，UI 已向 Compose 收敛 |

### 5.3 真机验证清单

以下功能在完成对应 Phase 后需在真机上验证：

| 功能 | 涉及 Phase | 验证点 |
|------|-----------|--------|
| 配置加载/保存 | Phase 2-3 | 配置能正确读取、保存、用户切换 |
| 模型初始化 | Phase 2 | 所有 Model 能正确实例化、配置页面正常 |
| RPC 请求 | Phase 4 | 蚂蚁森林能量收取、庄园喂鸡等正常 |
| 任务执行 | Phase 3-4 | 定时执行、手动任务正常 |
| 滑块验证 | Phase 4 | 验证码自动过滑块功能正常 |

### 5.4 提交策略

> 原则：**小步提交、可精确回退**。本项目无单元测试，验证依赖编译+真机，细分提交粒度是唯一的安全网。

| 粒度 | 操作 | 理由 |
|------|------|------|
| 每个 Task 完成 | `git commit` | 编译通过即提交，出问题可精确回退到上一个 Task |
| 每个 Phase 结束 | `git tag phase-N-done` | 标记阶段性基线，便于整体回退 |
| 高危 Task（标记 🔴/🟡） | 提交 + 真机验证后再继续下一个 | RPC 桥、Config 迁移等必须真机确认 |
| Phase 全部完成 | `git tag refactor-complete` | 标记重构里程碑 |

#### 提交信息格式

```
refactor(phase-N): [TaskID] 简要描述

- 变更要点 1
- 变更要点 2
```

**示例：**

```
refactor(phase-1): [1.4] TimeUtil.java → Kotlin

- Java 静态工具类转为 Kotlin object
- 保留 @JvmStatic 供 Java 调用方使用
```

#### 提交前检查清单

- [ ] `./gradlew :app:compileDebugKotlin` 通过
- [ ] 无新增编译警告
- [ ] 提交信息符合格式
- [ ] 高危 Task 已在真机验证（如适用）

#### 进度表更新规则

每完成一个 Task 或 Phase，**必须同步更新进度跟踪表（§6）**，具体要求：

| 触发时机 | 更新内容 |
|---------|---------|
| Task 完成 | 状态列改为「完成」，提交哈希列填入本次 commit 的短哈希（`git rev-parse --short HEAD`） |
| Task 跳过 | 状态列改为「跳过」，提交哈希列填「—」，附注说明原因 |
| Task 进行中 | 状态列改为「进行中」 |
| Phase 结束 | 在该 Phase 最后一个 Task 的下一行插入分隔行 `| — | **Phase N 完成** | — | — | tag: phase-N-done |` |

> 进度表更新与 git commit 在同一提交中完成，确保表格状态与代码版本始终一致。

---

## 6. 进度跟踪表

| Task | 标题 | 风险 | 状态 | 提交哈希 |
|------|------|------|------|----------|
| 0.1 | 编译基线验证 | 低 | 完成 | 75b9afe6 |
| 0.2 | 修复 TaskExecutor.kt 包名错误 | 低 | 完成 | 75b9afe6 |
| 0.3 | 修复 ReserveaMap 拼写错误 | 低 | 完成 | 75b9afe6 |
| — | **Phase 0 完成** | — | — | tag: phase-0-done |
| 1.1 | TypeUtil.java → Kotlin | 低 | 完成 | d7d57715 |
| 1.2 | StringUtil.java → Kotlin | 低 | 完成 | 33d5e6a6 |
| 1.3 | ListUtil.java → Kotlin | 低 | 完成 | f5048976 |
| 1.4 | TimeUtil.java → Kotlin | 低 | 完成 | 83906c88 |
| 1.5 | TimeFormatter.java → Kotlin | 低 | 完成 | 8e058254 |
| 1.6 | TimeCounter.java → Kotlin | 低 | 完成 | ef868970 |
| 1.7 | RandomUtil.java → Kotlin | 低 | 完成 | d185dd5f |
| 1.8 | Average.java → Kotlin | 低 | 完成 | 2f8a7b1f |
| 1.9 | CircularFifoQueue.java → Kotlin | 低 | 完成 | 6849ce1b |
| 1.10 | HanziToPinyin.java → Kotlin | 低 | 完成 | 23063675 |
| 1.11 | LanguageUtil.java → Kotlin | 低 | 完成 | 4ccb5751 |
| 1.12 | PortUtil.java → Kotlin | 低 | 完成 | d7fd7a38 |
| 1.13 | ResChecker.java → Kotlin | 低 | 完成 | 08b90a05 |
| 1.14 | JsonUtil.java → Kotlin | 中 | 完成 | a85f5335 |
| 1.15 | util/maps/ Java 文件 → Kotlin | 低 | 完成 | 1f746923 |
| — | **Phase 1 完成** | — | — | tag: phase-1-done |
| 2.1 | ModelFields.java → Kotlin | 中 | 完成 | eced8b69 |
| 2.2 | ModelGroup.java → Kotlin | 低 | 完成 | 040493da |
| 2.3 | ModelType.java → Kotlin | 低 | 完成 | bae9c720 |
| 2.4 | ModelConfig.java → Kotlin | 中 | 完成 | 7dd4c943 |
| 2.5 | ModelField.java → Kotlin | 高 | 完成 | bf9a6ed7 |
| 2.6 | Model.java → Kotlin | 中 | 完成 | 7c87e055 |
| 2.7 | modelFieldExt/ 全部 → Kotlin | 中 | 完成 | 205e5fd0 |
| 2.8 | AlipayUser.java → Kotlin | 低 | 完成 | 40ae65c0 |
| 2.9 | AreaCode.java → Kotlin | 低 | 完成 | 5deead3a |
| 2.10 | AlipayBeach.java → Kotlin | 低 | 完成 | 2390ce57 |
| 2.11 | CollectEnergyEntity.java → Kotlin | 低 | 完成 | c947d295 |
| 2.12 | FriendWatch.java → Kotlin | 低 | 完成 | c6c7a36d |
| 2.13 | ParadiseCoinBenefit.java → Kotlin | 低 | 完成 | ae870aeb |
| 2.14 | ReserveEntity.java → Kotlin | 低 | 完成 | d9bbb755 |
| — | **Phase 2 完成** | — | — | tag: phase-2-done |
| 3.1 | Config.java → Kotlin | 高 | 完成 | b3cce877 |
| 3.2 | RuntimeInfo.java → Kotlin | 低 | 完成 | ea6d30fb |
| 3.3 | StatusFlags.java → Kotlin | 低 | 待办 | — |
| 3.4 | TaskCommon.java → Kotlin | 低 | 待办 | — |
| 3.5 | TaskStatus.java → Kotlin | 低 | 待办 | — |
| 3.6 | TaskRunnerAdapter.java → Kotlin | 低 | 待办 | — |
| 3.7 | ChildTaskExecutor.java → Kotlin | 低 | 待办 | — |
| 4.1 | RpcVersion.java → Kotlin | 低 | 待办 | — |
| 4.2 | RpcBridge.java → Kotlin | 中 | 待办 | — |
| 4.3 | NewRpcBridge.java → Kotlin | 高 | 待办 | — |
| 4.4 | OldRpcBridge.java → Kotlin | 高 | 待办 | — |
| 4.5 | DebugRpc.java → Kotlin | 低 | 待办 | — |
| 4.6 | DebugRpcCall.java → Kotlin | 低 | 待办 | — |
| 4.7 | AntForestRpcCall.java → Kotlin | 中 | 待办 | — |
| 4.8 | GreenLife.java → Kotlin | 低 | 待办 | — |
| 4.9 | Healthcare.java → Kotlin | 低 | 待办 | — |
| 4.10 | 各功能域 *RpcCall.java → Kotlin | 中 | 待办 | — |
| 4.11 | AnswerAI/ 全部 → Kotlin | 中 | 待办 | — |
| 5.1 | 评估 JSON 方案统一 | 低 | 待办 | — |
| 5.2 | 移除 Lombok 依赖 | 低 | 待办 | — |
| 5.3 | UI 向 Compose 收敛 | 中 | 待办 | — |
| 5.4 | ui/dto/ Java 文件 → Kotlin | 低 | 待办 | — |
| 5.5 | ui/widget/ Java 文件 → Kotlin | 低 | 待办 | — |
| 5.6 | ui/adapter/ Java 文件 → Kotlin | 低 | 待办 | — |
| 5.7 | ObjReference.java → Kotlin | 低 | 待办 | — |

---

## 附录：Lombok 使用清单（18 处）

| 文件 | Lombok 注解 | 迁移 Task |
|------|-----------|----------|
| `data/Config.java` | `@Data` | 3.1 |
| `entity/CollectEnergyEntity.java` | `@Getter` | 2.11（已迁移 Kotlin，Lombok 已移除） |
| `entity/FriendWatch.java` | `@Getter`, `@Setter` | 2.12（已迁移 Kotlin，Lombok 已移除） |
| `entity/RpcEntity.kt` | `@Getter`（Kotlin 中使用 Lombok） | — |
| `entity/VitalityStore.kt` | `@Getter`（Kotlin 中使用 Lombok） | — |
| `hook/VersionHook.kt` | `@Getter`（Kotlin 中使用 Lombok） | — |
| `model/BaseModel.kt` | `@Getter`（Kotlin 中使用 Lombok） | — |
| `model/ModelConfig.java` | `@Data` | 2.4 |
| `model/ModelField.java` | `@Data`, `@Getter`, `@Setter` | 2.5 |
| `model/ModelGroup.java` | `@Getter` | 2.2 |
| `model/modelFieldExt/IntegerModelField.java` | `@Getter` | 2.7（已迁移 Kotlin，Lombok 已移除） |
| `task/AnswerAI/CustomService.java` | `@Getter`, `@Setter` | 4.11 |
| `task/AnswerAI/DeepSeek.java` | `@Getter`, `@Setter` | 4.11 |
| `task/AnswerAI/GeminiAI.java` | `@Getter`, `@Setter` | 4.11 |
| `task/antFarm/AntFarm.kt` | `@ToString`（Kotlin 中使用 Lombok） | — |
| `task/antOcean/AntOcean.java` | `@Getter` | 4.10 |
| `task/ModelTask.kt` | `@Setter`（Kotlin 中使用 Lombok） | — |
| `ui/ObjReference.java` | `@Data` | 5.7 |

> **注意**：部分 Kotlin 文件（`RpcEntity.kt`、`VitalityStore.kt`、`VersionHook.kt`、`BaseModel.kt`、`AntFarm.kt`、`ModelTask.kt`）仍在使用 Lombok 注解。迁移对应 Task 时应一并移除 Lombok，改用 Kotlin 原生语法。
