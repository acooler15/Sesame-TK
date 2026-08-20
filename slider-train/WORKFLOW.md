# 滑块验证码模型训练 — 操作流程手册

> 从零开始：采集样本 → 预标注 → 人工复核 → 生成数据集 → 训练 → 导出 → 更换端侧模型。
> 端侧代码适配细节另见 [ANDROID_INTEGRATION.md](ANDROID_INTEGRATION.md)，本文只讲操作流程。

## 0. 目录与环境

```
slider-train/                      训练工程根（uv 管理，含采集页）
├── pyproject.toml / uv.lock       依赖（uv 管理）
├── WORKFLOW.md / HANDOFF.md       流程手册 / 交接文档
├── ANDROID_INTEGRATION.md         端侧适配文档
├── collect/                       采集页（验证码网页 + 本地服务）
│   ├── index.html                 验证码页面（浏览器打开触发滑块）
│   ├── serve.py                   采集服务（默认 8900 端口，自动注入采集器）
│   ├── collect-auto.js            自动采集器（serve.py 注入，免控制台粘贴）
│   └── _reload.py                 CDP 重载采集页工具
├── scripts/                       训练/标注脚本
│   ├── prelabel_gap.py            缺口预标注（captcha-recognizer）
│   ├── review_server.py           人工复核 Web 服务（默认 8901 端口）
│   ├── collect_to_dataset.py      预标注 json → YOLO detect 标签
│   ├── split_dataset.py           复制图片 + 划分 train/val（约 10%）
│   ├── train.py                   训练（默认 weights/yolo11n.pt 微调）
│   └── export.py                  导出 TFLite（自动打印 shape 校验）
├── web/
│   └── review.html                复核界面（review_server.py 自动加载）
├── config/
│   └── slider.yaml                数据集配置（三类：gap/block/refresh）
├── weights/                       预训练底座模型
│   ├── yolo11n.pt
│   └── yolo26n.pt / yolo26n-seg.*
├── research/                      反混淆研究产物（非运行依赖）
│   ├── _captcha.js
│   └── _strtable.js
├── data/
│   ├── raw/                       采集原始样本（原 captures/）
│   │   ├── <编号>.png / <编号>.json
│   │   ├── review/                预标注 + 复核目录
│   │   └── verified/              （预留）
│   ├── datasets/slider/           生成的数据集（images/ + labels/）
│   └── runs/                      训练输出（weights/best.pt、results.png）
└── logs/                          运行日志
```

环境要求（已初始化过，仅换机器时需要）：
- `mise` 管理 Python；训练工程用 `uv` 管理依赖
- 依赖已含：`ultralytics`、`captcha-recognizer`（onnxruntime + opencv）

> 所有脚本基于自身位置推导工程根，**任意工作目录下运行均可**。
> 以下命令默认在 `slider-train/` 工程根执行（`cd e:\...\slider-train`）。

---

## 1. 采集样本

**1.1 启动采集页**

```powershell
cd e:\workspace\AndroidStudioProjects\Sesame-TK\slider-train
uv run collect/serve.py             # 默认 http://127.0.0.1:8900/index.html
```

> 必须经 `serve.py` 访问（采集器靠它注入），双击 `collect/index.html` 直开无法采集。

> `serve.py` 启动时会自动拉起 Chrome（探测不到则用 Edge，独立 profile，带
> CDP 调试端口 9222）并打开采集页——普通可见窗口，人工可随时接管。
> 「识别验证」模式的拖拽/点击经 CDP 原生输入派发（isTrusted=true），
> 绕过 captcha.js 风险引擎对合成事件的检测（moveTruested）。

**1.2 自动采集**

1. 浏览器打开页面，等待滑块验证码渲染出来（右下角面板提示「验证码已就绪」）；
2. 点右下角面板的**「开始采集」**：采集器自动截图 + 保存 JSON → 自动点刷新按钮换图 → 循环；
3. 产物自动落盘到 `data/raw/`，`<编号>.png` 与 `<编号>.json` 同名成对、编号自增；
4. 攒够样本（或面板提示会话过期）后点**「停止」**；会话过期时刷新页面重开即可继续，
   编号接着上次的往下排。

> 截图由服务端按 `.jshield-captcha-puzzle-container`（拼图区 + 底部滑轨）原点自动合成，
> 与 App 端识别输入的裁剪带一致；滑轨/手柄为近似绘制，仅提供视觉占位。

**数量建议**：300~500 张起步；类别均衡上每张图天然含 1 gap + 1 block + 1 refresh。

**注意**：
- 采集时滑块必须停在初始位置（未拖动），自动模式天然满足；
- 双击 `collect/index.html` 直开（file://）不会注入采集器，必须经 `serve.py` 访问。

---

## 2. 批量预标注缺口

```powershell
cd e:\workspace\AndroidStudioProjects\Sesame-TK\slider-train
uv run scripts/prelabel_gap.py data/raw/          # 整目录
uv run scripts/prelabel_gap.py data/raw/*.png     # 或指定文件
```

每张图产出（写在 data/raw/ 下）：
- `<编号>_prelabeled.json`：gap 识别框 + DOM 坐标已统一到截图坐标系；
- `review_<编号>.png`：红 gap / 绿 block / 蓝 refresh 叠加可视化。

脚本结束会汇总 `ok / low-conf / failed`，并列出**低置信度（<0.6）与失败清单**
——这些是复核时的重点。

---

## 3. 人工复核（Web 界面）

**3.1 启动复核服务**

```powershell
cd e:\workspace\AndroidStudioProjects\Sesame-TK\slider-train
uv run scripts/review_server.py data/raw/         # 默认 http://127.0.0.1:8901
uv run scripts/review_server.py data/raw/ --port 8902   # 端口被占时换
```

启动时控制台会打印：样本总数 / 待复核 / 未预标注。复核界面自动从 `web/review.html` 加载。

**3.2 浏览器操作**

打开 `http://localhost:8901`：

| 操作 | 方式 |
|---|---|
| 修正缺口框 | **拖动红框**整体移动；或**空白处拖拽**重新画一个 |
| 切换编辑目标 | 快捷键 `1`(缺口) / `2`(滑块) / `3`(刷新)，或点操作栏按钮 |
| 通过当前样本 | 点「通过」或按 `A` → 自动跳到下一张待复核 |
| 剔除坏样本 | 点「剔除」或按 `R` |
| 翻页 | `←` / `→` 或点按钮 |

> 三种框（gap/block/refresh）均可手动拖拽/重画，手改的框标记 `source=manual`。
> 顶部统计实时更新：待复核 / 通过 / 剔除。复核结果即时写回
> `*_prelabeled.json`（`reviewStatus=approved/rejected`）。

> 不想起服务时，也可以直接看 `review_*.png` 静态图目检，
> 手工编辑 `*_prelabeled.json` 的 `gapimg` 字段（x/y/width/height，截图像素坐标）。

---

## 4. 生成训练数据集

```powershell
cd e:\workspace\AndroidStudioProjects\Sesame-TK\slider-train
uv run scripts/collect_to_dataset.py data/raw/*_prelabeled.json --only-reviewed
uv run scripts/split_dataset.py     # 复制图片 + 划分约 10% 到 val
```

- `collect_to_dataset.py`：只消费 `reviewStatus=approved` 的样本，产出
  `data/datasets/slider/labels/train/<编号>_prelabeled.txt`（YOLO detect 三类标签）；
- `split_dataset.py`：把 `data/raw/` 对应 png 复制成 `*_prelabeled.png` 到
  `data/datasets/slider/images/train/`，并按固定种子（42）随机划约 10% 到
  `images/val/` + `labels/val/`（图片与标签一一对应）。

结束后核对标签统计行 `gap=N block=N refresh=N`，三类都应 > 0
（gap=0 会打印提醒，说明预标注/复核环节有漏）。

---

## 5. 训练

```powershell
cd e:\workspace\AndroidStudioProjects\Sesame-TK\slider-train
uv run scripts/train.py                                   # 默认 weights/yolo11n.pt 微调，100 epochs
uv run scripts/train.py --epochs 150 --batch 8            # 显存不足时降 batch
uv run scripts/train.py --weights weights/yolo26n.pt      # 换 YOLO26n 底座（见注意事项）
uv run scripts/train.py --weights data/runs/slider/weights/best.pt   # 从上次结果续训
```

- 数据配置默认 `config/slider.yaml`，输出默认 `data/runs/`（`data/runs/slider/weights/best.pt`）；
- 判断收敛：看 `data/runs/slider/results.png` 的 `metrics/mAP50` 曲线是否趋平，
  mAP50 稳定 0.9+ 通常够用（验证码目标单一、背景干净）。

**注意事项**：
- 默认 `yolo11n`：TFLite 输出 `[1,7,8400]`，端侧适配文档按此写；
- `yolo26n` 更新但 TFLite 输出布局不同（NMS-free 直出），选它则端侧解析
  需按实际导出 shape 单独适配。

---

## 6. 导出 TFLite 并校验

```powershell
cd e:\workspace\AndroidStudioProjects\Sesame-TK\slider-train
uv run scripts/export.py data/runs/slider/weights/best.pt
```

导出完成后自动加载 tflite 打印输入/输出 shape。**必须核对**：

```
input[0]:  [1, 640, 640, 3]     ← 预处理不变的前提
output[0]: [1, 7, 8400]         ← 三类 detect；若不符，端侧解析按实际 shape 改
```

> shape 打印依赖 tflite 解释器，缺失时只提示跳过（不影响导出本身），
> 可用 Netron（https://netron.app）打开 tflite 目视核对。

> **Windows 注意**：ultralytics 新版 LiteRT 导出仅支持 Linux/macOS。
> 在 Windows 本机直接导出会报 `LiteRT export only supported on Linux x86 and macOS`。
> 需在 WSL/Linux 里导（见 ANDROID_INTEGRATION.md 附录），或降级 ultralytics。

---

## 7. 更换端侧模型 + 适配

1. 用导出的 `best_float32.tflite` **覆盖** `app/src/main/ml/slider.tflite`；
2. Android Studio 重新生成 `Slider` 类（ml 目录右键或编译触发）；
3. 按 [ANDROID_INTEGRATION.md](ANDROID_INTEGRATION.md) 第 4、5 节改造：
   - `SliderTFLite.kt`：删 mask 相关，`postprocess` 按 `[1,7,8400]` 三类解析，
     按 classId 分派 gap/block/refresh；
   - `Captcha2Handler.kt`：gap 无候选 → 手势点击 refresh 框 → 重识别（上限 2 次）；
4. 真机回归三条路径：正常滑动 / 识别失败自动刷新 / 校正滑动。

---

## 8. 常见问题

| 现象 | 处理 |
|---|---|
| 页面不出验证码 | 确认 `serve.py` 已启动且访问 `http://127.0.0.1:8900/index.html`；刷新页面重新触发 |
| 采集器提示等不到新图 | 验证码会话过期，刷新页面后重新点「开始采集」 |
| 8900/8901 端口被占 | `--port` 换端口 |
| prelabel 大量 failed | 截图范围不对（没截到拼图区）或图太模糊；先看 review 图排查截图规范 |
| 复核界面图片 404 | json 与 png 不同名，或 png 不在服务启动的那个目录里 |
| 标签统计 gap=0 | 预标注失败未被人工补标；对失败样本手工在 `*_prelabeled.json` 写 `gapimg` 后重跑第 4 步 |
| 训练报数据为空 | `data/datasets/slider/images/train` 下没有同名 png，或 `config/slider.yaml` 的 path 指向不对 |
| 导出 shape 不是 [1,7,8400] | 底座/版本差异所致；以打印为准同步改端侧解析，或改回 yolo11n 重训 |
| Windows 导出失败 | 见第 6 节，需在 WSL/Linux 里导或降级 ultralytics |
