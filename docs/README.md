# Sesame-TK 文档中心

> 本目录存放芝麻粒-TK 的开发文档。所有文档基于源码静态分析编写，路径均相对于 `app/src/main/java/fansirsqi/xposed/sesame/`（下称 `<root>`）。
> 阅读顺序建议：**开发文档 → 功能模块文档 → 任务执行流程分析**。

## 文档索引

| 文档 | 面向读者 | 内容 |
| --- | --- | --- |
| [开发文档.md](file:///e:/workspace/AndroidStudioProjects/Sesame-TK/docs/开发文档.md) | 二次开发者 | 项目概览、技术栈、架构分层、双 Hook 入口、关键子系统、构建与签名、二次开发指南、排错 |
| [功能模块文档.md](file:///e:/workspace/AndroidStudioProjects/Sesame-TK/docs/功能模块文档.md) | 使用者 / 二次开发者 | 全部 15 个功能模块（Model）的职责、关键配置项、支撑性模块与辅助服务能力 |
| [任务执行流程分析.md](file:///e:/workspace/AndroidStudioProjects/Sesame-TK/docs/任务执行流程分析.md) | 核心开发者 | 定时任务调度链路、蚂蚁森林蹲点引擎（会话级 WaitingAccountSession）、通用 ChildModelTask 蹲点、异常恢复机制、调优常量速查 |

## 仓库周边工程

除主 APK（`app/`）外，仓库还包含若干辅助工程，均不在模块运行时依赖内：

| 目录 | 用途 | 入口 |
| --- | --- | --- |
| `serve-debug/` | 本地调试服务：接收模块「Hook 数据转发」（默认 `http://127.0.0.1:9527/hook`）+ Web 配置页本地预览 | `uv run main.py` / `uv run webui.py` |
| `slider-train/` | 滑块验证码 YOLO 模型训练流水线（采集 → 预标注 → 训练 → 导出 TFLite） | 详见其 `WORKFLOW.md`、`ANDROID_INTEGRATION.md` |
| `scripts/build_and_share.sh` | 本地构建 debug APK 并上传 FTP 的自动化脚本 | bash 脚本，支持 `--variant/--clean-dex/--no-upload` |

## 文档维护约定

1. **同步修改**：涉及任务调度、蹲点引擎、RPC 链路、配置默认值的代码变更，需同步更新对应文档章节。
2. **引用规范**：引用源码统一使用相对 `<root>` 的路径（如 `hook/ApplicationHook.kt`），关键结论附行号。
3. **事实校验**：文档中的默认值、常量、流程描述须以代码为准；发现不符以代码为真相源并修正文档。
