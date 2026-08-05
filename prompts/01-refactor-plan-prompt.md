# 提示词一：重构规划提示词

> 使用方式：将本文件全部内容粘贴给 AI 模型（建议在项目根目录上下文中使用），让其输出一份《重构规划文档》。

---

## 角色

你是一名资深 Android 架构师，精通 Kotlin、Java、Xposed 模块开发与大型遗留项目重构。你擅长制定**低风险、可验证、可增量执行**的重构计划。

## 项目背景

这是一个 Xposed 模块项目（Sesame-TK，芝麻粒-TK），包名为 `fansirsqi.xposed.sesame`，主要功能是在支付宝 App 中 Hook 并实现自动化任务（如蚂蚁森林、蚂蚁庄园、蚂蚁鱼塘等）。

### 现状

- 源码根目录：`app/src/main/java/fansirsqi/xposed/sesame/`
- 顶层包结构：`data/`、`entity/`、`extensions/`、`hook/`、`model/`、`net/`、`service/`、`task/`、`ui/`、`util/`，入口为 `SesameApplication.kt`
- **Java 与 Kotlin 代码混杂**：约 90+ 个 Java 文件、190+ 个 Kotlin 文件，部分包（如 `model/`、`util/`）以 Java 为主，部分包（如 `hook/`、`task/`）以 Kotlin 为主
- 同时存在两套 JSON 方案：Jackson（`jackson-core/databind/annotations` + `jackson.kotlin`）与 `kotlinx.serialization`
- 同时存在两套 UI 体系：传统 View 体系（ViewBinding、RecyclerView、ViewPager2）与 Jetpack Compose（Material 3）
- Java 代码依赖 Lombok（编译期注解处理）
- 构建系统：Gradle Kotlin DSL，`compileSdk 36`，`minSdk 26`，Java/Kotlin 目标均为 JVM 17
- 特殊依赖：Xposed API 82/100（compileOnly）、libxposed service、Shizuku、DexKit、NanoHTTPD、TensorFlow Lite、C++ NDK 源码（`src/main/cpp`）
- 项目通过 Xposed Hook 注入支付宝进程运行，**无法在常规模拟器中做单测验证**，验证主要依赖编译通过 + 人工在真机上运行

### 重构目标

1. **语言统一**：代码尽量使用 Kotlin。除以下情况外，Java 文件应逐步迁移为 Kotlin：
   - Lombok 重度依赖且迁移收益低的类（可后置处理）
   - 被 Xposed/反射以「全限定类名字符串」引用、改名会破坏 Hook 的类（需单独标记，谨慎处理）
   - AIDL、JNI/NDK 相关胶水代码
2. **结构清晰**：
   - 按「功能域」而非「技术分层」重新组织包结构（每个任务模块自包含：Model + RpcCall + Task 逻辑）
   - 公共能力（网络、日志、Hook 工具、配置）沉淀到明确的 core 层
   - 消除重复/死代码、统一命名规范
3. **技术栈收敛**（作为可选项评估，不强求）：
   - JSON 方案统一为一种（推荐 kotlinx.serialization）
   - UI 体系向 Compose 收敛（低优先级，可后置）
   - 移除 Lombok

## 你的任务

**只做规划，不修改任何代码。** 请先全面阅读项目代码（优先阅读包结构、核心入口、各包的代表性文件、build.gradle.kts），然后输出一份《重构规划文档》，写入项目根目录的 `REFACTOR_PLAN.md`。

## 规划文档必须包含的内容

1. **现状评估**
   - 各包的职责、Java/Kotlin 文件数量统计、代码耦合情况
   - 识别出的主要问题清单（结构问题、重复代码、过时用法、潜在风险点）
   - **风险清单**：特别标注被反射/Hook 以字符串引用的类、被支付宝侧依赖的 RPC 协议字段、proguard 规则中 keep 的类——这些是重构中的「不可动」或「高危」区域
2. **目标架构**
   - 目标包结构树（文字形式），每个包的职责一句话说明
   - 包之间的依赖方向规则（哪些包可以依赖哪些包）
3. **分阶段重构计划**
   - 将整个重构拆分为 **N 个阶段（Phase）**，每个阶段再拆分为**可独立提交的小任务（Task）**
   - 每个 Task 必须包含：编号、标题、涉及的文件/包、操作类型（移动/重命名/Java转Kotlin/删除/合并/抽取）、前置依赖 Task、风险等级（低/中/高）、验证方式
   - 排序原则：**先低风险后高风险、先工具类后业务类、先移动后改写**，每个阶段结束后项目必须能编译通过
   - 建议的阶段划分参考：Phase 0 建立基线与保护网 → Phase 1 util/公共工具类 Kotlin 化 → Phase 2 model/entity 数据类 Kotlin 化（去 Lombok）→ Phase 3 包结构重组 → Phase 4 task 各功能域整理 → Phase 5（可选）JSON/UI 技术栈收敛
4. **Java → Kotlin 迁移规范**
   - 命名规范（文件、类、常量、伴生对象）
   - 常见转换模式：Lombok `@Data` → `data class`、静态工具类 → `object`/顶层函数、getter/setter → 属性、回调接口 → lambda/函数类型、线程 → 协程（如适合）
   - 禁止事项：不做超出迁移范围的逻辑重写；不改变公开 API 的行为；`@JvmStatic`/`@JvmField` 在仍有 Java 调用方时必须保留
5. **验证策略**
   - 每个 Task 完成后的验证命令（如 `./gradlew :app:compileDebugKotlin` 或 `assembleDebug`）
   - 每个 Phase 的验收标准
6. **进度跟踪表**
   - 一个 Markdown 表格，列出所有 Task 及其状态（待办/进行中/完成/跳过），供执行阶段更新

## 约束与要求

- 你的规划必须**基于实际代码**，不得臆造文件或不存在的结构
- 规划要**保守**：拿不准的高危改动（如 Hook 入口、RPC 协议类）宁可后置或标注「保持不动」
- 不考虑引入新的第三方库；不改动 `cpp/` NDK 代码与 `libs/` 下的 aar/jar
- 输出文档使用简体中文
