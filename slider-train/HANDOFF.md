# 滑块验证码模型训练 — 当前工作交接

> 最后更新：2026-08-19（目录重构后）

## 一、项目概述

为 Sesame-TK（Android Xposed 模块）训练滑块验证码缺口检测模型，替换端侧旧的分割模型。

**工作流**：采集样本 → 预标注 → 人工复核 → 生成数据集 → 训练 → 导出 TFLite → 端侧适配

**不做自动验证**：曾尝试用 CDP + 真人轨迹模板自动拖拽滑块验证，jshield 服务端风控始终识别为非人类（前端 isTrusted/movementX/timeStamp 均无差异，判定在服务端）。最终确定纯采集 + 人工复核方案。

## 二、目录结构（重构后）

> 2026-08-19 已将 `captcha-page/` 与 `slider-train/` 合并为单一工程 `slider-train/`，
> 并重排目录、统一输出。所有脚本基于自身位置推导工程根，任意工作目录下运行均可。

```
slider-train/                      统一工程根（uv 管理）
├── pyproject.toml / uv.lock       依赖（torch cu124 GPU 版 + ultralytics + captcha-recognizer）
├── WORKFLOW.md                    完整操作流程手册
├── HANDOFF.md                     本文档（交接）
├── ANDROID_INTEGRATION.md         端侧适配文档
├── collect/                       采集页
│   ├── index.html                 验证码页面
│   ├── serve.py                   采集服务（端口 8900，CDP 端口 9222）
│   ├── collect-auto.js            自动采集器（serve.py 注入）
│   └── _reload.py                 CDP 重载工具
├── scripts/                       脚本
│   ├── prelabel_gap.py            缺口预标注
│   ├── review_server.py           人工复核服务（端口 8901）
│   ├── collect_to_dataset.py      预标注 → YOLO 标签
│   ├── split_dataset.py           复制图片 + 划分 train/val
│   ├── train.py                   训练
│   └── export.py                  导出 TFLite
├── web/
│   └── review.html                复核界面
├── config/
│   └── slider.yaml                数据集配置（gap=0 / block=1 / refresh=2）
├── weights/                       预训练底座（yolo11n.pt 默认）
├── research/                      反混淆研究产物（非运行依赖）
├── data/
│   ├── raw/                       采集原始样本
│   │   ├── *.png / *.json         新采集产物
│   │   ├── review/                预标注 + 复核目录
│   │   └── verified/              （空，自动验证方案已废弃）
│   ├── datasets/slider/           生成的数据集（images/ + labels/）
│   └── runs/                      训练输出（weights/best.pt、results.png）
└── logs/                          运行日志
```

## 三、当前进度

| 步骤 | 状态 | 说明 |
|---|---|---|
| 1. 采集 | 完成（第一批） | 已采 484 张（编号 126-640），已全量预标注 |
| 2. 预标注 | 完成 | 484 张全部 prelabeled（failed 31 张已剔除） |
| 3. 人工复核 | 完成 | 484 张全部 approved（0 rejected） |
| 4. 生成数据集 | 完成 | 436 train / 48 val，图片+标签一一对应 |
| 5. 训练 | 完成 | yolo11n 微调 100 epochs，mAP50=0.995 |
| 6. 导出 | 进行中 | Windows 不支持 LiteRT 导出，需在 WSL/Linux 里导 |
| 7. 端侧适配 | 待执行 | 按 ANDROID_INTEGRATION.md |

**关键产物**：
- 训练权重：`data/runs/slider/weights/best.pt`（mAP50 0.995）
  - 实际在重构时位于 `Sesame-TK/runs/detect/runs/slider-2/`，已迁至 `data/runs/slider/`
- 数据集：`data/datasets/slider/`（train 436 / val 48）
- 复核结果：`data/raw/*_prelabeled.json` 全部 `reviewStatus=approved`

> ⚠️ 训练输出目录说明：第一次训练因工作目录解析用了 `--project .` 导致产物落在
> 工程根外（`Sesame-TK/runs/detect/runs/slider-2`）。已全部迁移到 `data/runs/`。
> 重构后 `train.py` 已改为基于工程根解析，输出固定在 `data/runs/slider/`。

## 四、下一步操作

### 1. 导出 TFLite（当前进行中）

```powershell
# Windows 本机（最新 ultralytics 不支持，需在 WSL/Linux 里导）：
wsl -d Debian -- bash -lc "cd /tmp/yolo_export && source .venv/bin/activate && python export_tflite.py"
# 或降级 ultralytics 后在 Windows 导

# Linux 环境导出命令（若在 WSL 已配好）：
uv run scripts/export.py data/runs/slider/weights/best.pt
```

核对输出 shape：`input [1,640,640,3]` / `output [1,7,8400]`（yolo11n 底座）

### 2. 端侧适配

用导出的 `best_float32.tflite` 覆盖 `app/src/main/ml/slider.tflite`，按 `ANDROID_INTEGRATION.md` 改 `SliderTFLite.kt` / `Captcha2Handler.kt`。

### 3. 若需继续采集扩充样本

```powershell
cd e:\workspace\AndroidStudioProjects\Sesame-TK\slider-train
uv run collect/serve.py                    # http://127.0.0.1:8900/index.html
# 产物落盘 data/raw/（编号自增）
```

预标注 / 复核 / 生成数据集 / 训练的完整命令见 WORKFLOW.md。

## 五、关键注意事项

- **采集必须经 `serve.py`**：双击 `collect/index.html` 直开不会注入采集器
- **采集服务需带 CDP**：`serve.py` 默认启动 Chrome 带 `--remote-debugging-port=9222`
- **类别顺序勿改**：`config/slider.yaml` 定义 gap=0 / block=1 / refresh=2，端侧解析依赖此顺序
- **Windows 导出限制**：最新 ultralytics 的 LiteRT 导出仅支持 Linux/macOS，Windows 需在 WSL/Linux 里导或降级
- **torch 为 GPU 版**：依赖锁在 `uv.lock`（`2.6.0+cu124`），`uv sync` 后仍为 GPU 版；勿手动改为 CPU 版

## 六、已有样本分布

`data/raw/` 里 484 张 approved 样本（供参考）：

```
短距 133-155px：约 15 张
中距 166-183px：约 25 张
长距 215-236px：约 40 张
（第一批以中长距为主，后续采集可留意短距补充）
```

新采集时无需刻意控制距离，验证码自动随机。
