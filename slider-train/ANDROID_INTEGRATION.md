# 新滑块检测模型 — 端侧适配文档

> 本文档描述新模型训练完成后的 Android 端适配方案。
> **在 `slider.tflite` 替换为新模型之前，端侧代码不需要任何改动**；
> 模型就绪后按本文档逐节实施即可。

---

## 1. 新旧模型对比

| 项 | 旧模型（seg） | 新模型（detect） |
|---|---|---|
| 任务 | 实例分割（单类 target） | 检测（三类 gap/block/refresh） |
| 底座 | 不明（38.6MB） | yolo11n（默认）/ yolo26n |
| 体积 | 38.6 MB | 约 6 MB（float32） |
| 输入 | `[1,640,640,3]` float32 NHWC，/255 | 相同（不变） |
| 输出 0 | `[1,37,8400]`（4框+1分+32mask系数） | **`[1,7,8400]`**（4框+3类分数） |
| 输出 1 | `[1,160,160,32]` proto mask | **无**（仅一个输出） |
| 滑块/缺口区分 | 端侧几何规则（最左=滑块 + mask形状IoU） | **classId 直接区分** |
| 刷新按钮 | 不识别 | classId=2，识别失败时点击换图 |

## 2. 类别定义（与 slider.yaml 一致，勿改动顺序）

| classId | 名称 | 含义 | 端侧用途 |
|---|---|---|---|
| 0 | gap | 缺口（背景图上被挖的洞） | 滑动目标（targetX） |
| 1 | block | 滑块拼图块（初始最左） | 距离计算参考（sliderX） |
| 2 | refresh | 拼图区右上角刷新按钮 | gap 识别失败时点击换图 |

## 3. 预处理（不变）

- letterbox 到 640×640，填充色 `rgb(114,114,114)`
- 归一化 `/255`，NHWC
- 现有 `letterbox()` / `loadBitmapToTensorBuffer()` 原样保留

## 4. 输出解析（需重写）

### 4.1 布局

输出为单个 `[1, 7, 8400]` float32（yolo11n 底座）：

```
通道 0-3: cx, cy, w, h        (letterbox 640 空间坐标)
通道 4:   gap 置信度
通道 5:   block 置信度
通道 6:   refresh 置信度
```

扁平数组索引：`channel * 8400 + anchor`（与旧代码同套路）。
每个 anchor 取三类分数最大者为 classId，超过 conf 阈值入选；随后按类内 NMS。

> 若底座用 **yolo26n**：TFLite 输出为 e2e top-k 直出（无 NMS、shape 不同），
> 解析逻辑以 `export.py` 打印的实际 shape 为准单独适配。

### 4.2 SliderTFLite.kt 改造清单

| 位置 | 改动 |
|---|---|
| 常量区 | 删 `MASK_NUM`；`NUM_ANCHORS=8400` 保留；新增 `NUM_CLASSES=3` |
| `DetectionResult` | 删 `maskCoeffs`/`mask` 字段，保留 `x1y1x2y2/score/classId` |
| `predict()` | 只读 `outputFeature0`；不再读 proto（无 output1） |
| `postprocess()` | 重写：按 4.1 布局解析；NMS 保留；坐标还原 `(v - pad)/ratio` 保留 |
| `processMask()` / `cropAndScaleMask()` / `generateMask()` / `calculateShapeIou()` | 整体删除 |
| `identifySlideRecognition()` | 重写分派逻辑，见 4.3 |

### 4.3 结果分派（替代"最左=滑块"规则）

```kotlin
val gaps    = results.filter { it.classId == 0 }.maxByOrNull { it.score }
val blocks  = results.filter { it.classId == 1 }.maxByOrNull { it.score }
val refresh = results.filter { it.classId == 2 }.maxByOrNull { it.score }
```

- `gap == null` → 返回 null（调用方据此触发刷新流程，见第 5 节）
- `block == null` 但 `gap != null` → 用现有像素扫描手柄 x 代偿 sliderX
- `SlideRecognitionResult` 增加字段：`refreshX/refreshY/refreshScore`（可为 null 框）

## 5. Captcha2Handler 刷新流程（新增）

### 5.1 触发条件

`prepareSlidePlan` 中 `identifyShared` 返回 null（**gap 无候选**）时，
不再直接 `terminateRetryable`，改走刷新重试。

### 5.2 流程

```
gap 无候选
  → refresh 框存在？
      ├─ 否 → 维持现有 terminateRetryable("模型识别无结果")
      └─ 是 → 坐标换算（见 5.3）→ dispatchGesture 点击刷新
              → delay(1500~2000ms) 等新图加载
              → 重新 evaluateLightweightPreCheck + identifyShared
              → 刷新次数 < MAX_REFRESH(=2) 可再刷；超限走 terminateRetryable
```

### 5.3 坐标换算（模型输出 → 屏幕点击）

```
模型框(640 letterbox 空间) → 裁剪带坐标:  screen = (model - pad) / ratio
裁剪带坐标 → 全屏截图坐标:              + (cropLeft, cropTop)
全屏截图坐标 → 手势坐标:                 直接用（decorView 截图原点=屏幕原点）
```

与现有 `prepareSlidePlan` 里 targetX 的换算同路数，点击目标取 refresh 框中心。

### 5.4 点击方式

`dispatchGesture` 手势点击（与滑动手势同机制，不依赖无障碍节点 ACTION_CLICK）。

### 5.5 关联改动

- `attemptCorrectiveSwipeIfNeeded`（校正路径）同样消费新的识别返回结构
- 刷新等待期间文案锚点会出现"虚灭"（虚拟树重建），已有
  `CAPTION_GONE_CONFIRM_MS` 二次确认机制可复用

## 6. 交付验收步骤

1. `uv run export.py runs/slider/weights/best.pt` → 核对打印的 shape：
   输入 `[1,640,640,3]`、输出 `[1,7,8400]`（yolo11n 底座）
2. 覆盖 `app/src/main/ml/slider.tflite`，Android Studio 重新生成 `Slider` 类
3. 按第 4、5 节改 `SliderTFLite.kt` / `Captcha2Handler.kt`
4. 真机回归：
   - 正常路径：gap/block 均检出 → 滑动通过 → 文案消失
   - 刷新路径：构造识别失败（如临时提高 conf 阈值）→ 观察点击刷新 → 二次识别
   - 校正路径：滑动后未通过 → 校正滑动仍生效
5. 日志观察点：`识别候选框数量`、每候选的 `classId`、`gap 无候选 → 刷新` 分支日志

## 7. 数据采集与预标注流程（训练侧，供对照）

采集 → 预标注 → 复核 → 合成标签 → 训练，全链路：

```
1. serve.py 起页面，触发滑块验证码
2. collect.js 采集 → <name>.json（DOM rect）+ <name>.png（拼图区截图）
3. uv run prelabel_gap.py <name>.png        # captcha-recognizer 预标注缺口
   → 生成 <name>_prelabeled.json + review_<name>.png（gap红/block绿/refresh蓝）
4. uv run review_server.py <采集目录>        # 人工复核 Web 界面（默认 8901 端口）
   浏览器打开 http://localhost:8901 ：拖红框/空白重画修正 gap，A 通过、R 剔除
   → 结果写回 *_prelabeled.json（reviewStatus=approved/rejected）
5. uv run collect_to_dataset.py *_prelabeled.json --only-reviewed  # 合成三类标签
6. uv run train.py → uv run export.py
```

- `block`/`refresh` 标注由网页 DOM 全自动生成（坐标已由 prelabel 统一到截图坐标系）
- `gap` 由 captcha-recognizer（github.com/chenwei-zhao/captcha-recognizer，MIT）预标注，
  实测置信度 0.9+，人工仅复核；低置信度（<0.6）与失败样本脚本会单独列出
- 采集图分布需对齐端侧输入：从全屏截图裁出的「拼图区+轨道」横带
  （网页可直接截 `.jshield-captcha-puzzle-container` + `.slider-container` 区域）
