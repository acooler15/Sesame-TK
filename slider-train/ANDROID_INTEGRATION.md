# 滑块检测模型 — 端侧适配文档

> 训练完成后 Android 端按本文档逐节实施即可。

---

## 1. 模型规格

- 底座：yolo26n（NMS-free 端到端检测，去 DFL）
- 输入：`[1, 3, 640, 640]` float32（NCHW），归一化 `/255`
- 输出：`[1, 300, 6]`（一对一 e2e 头，每张图最多 300 个检测框，无需 NMS）

## 2. 类别定义（与 slider.yaml 一致，勿改动顺序）

| classId | 名称 | 含义 | 端侧用途 |
|---|---|---|---|
| 0 | gap | 缺口（背景图上被挖的洞） | 滑动目标（targetX） |
| 1 | block | 滑块拼图块（初始最左） | 距离计算参考（sliderX） |
| 2 | refresh | 拼图区右上角刷新按钮（最右） | gap 识别失败时点击换图 |
| 3 | feedback | 拼图区右上角反馈按钮（刷新左侧） | 区分反馈/刷新，避免误点 |

## 3. 预处理

- letterbox 到 640×640，填充色 `rgb(114,114,114)`
- 归一化 `/255`，NCHW（通道在前）

## 4. 输出解析

### 4.1 布局

输出为单个 `[1, 300, 6]` float32，每行一个候选框：

```
[x1, y1, x2, y2, confidence, class_id]
```

- 坐标为 letterbox 640 空间像素值，需还原到原图（`(v - pad) / ratio`）
- `confidence` 低于阈值（0.5）丢弃
- `class_id` 直接区分四类，无需 NMS（模型已内置端到端筛选）

### 4.2 SliderTFLite.kt 改造清单

| 位置 | 改动 |
|---|---|
| 常量区 | `NUM_CLASSES=4`；新增 `CLASS_FEEDBACK=3` |
| `predict()` | 读取输出 `[1, 300, 6]`，按行解析，无 NMS |
| `postprocess()` | 按 4.1 布局解析；坐标还原 `(v - pad)/ratio` 保留 |
| `identifySlideRecognition()` | 按 classId 分派，见 4.3 |

### 4.3 结果分派

```kotlin
val gaps     = results.filter { it.classId == 0 }.maxByOrNull { it.score }
val blocks   = results.filter { it.classId == 1 }.maxByOrNull { it.score }
val refresh  = results.filter { it.classId == 2 }.maxByOrNull { it.score }
val feedback = results.filter { it.classId == 3 }.maxByOrNull { it.score }
```

- `gap == null` → 返回 null（调用方据此触发刷新流程，见第 5 节）
- `block == null` 但 `gap != null` → 用现有像素扫描手柄 x 代偿 sliderX
- `SlideRecognitionResult` 增加字段：`refreshX/refreshY/refreshScore`（可为 null 框）

## 5. Captcha2Handler 刷新流程

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

1. `uv run export.py` → 核对打印的 shape：
   输入 `[1,640,640,3]`、输出 `[1,300,6]`
2. 覆盖 `app/src/main/ml/slider.tflite`，Android Studio 重新生成 `Slider` 类
3. 按第 4、5 节核对 `SliderTFLite.kt` / `Captcha2Handler.kt`
4. 真机回归：
   - 正常路径：gap/block 均检出 → 滑动通过 → 文案消失
   - 刷新路径：构造识别失败（如临时提高 conf 阈值）→ 观察点击刷新 → 二次识别
   - 校正路径：滑动后未通过 → 校正滑动仍生效
5. 日志观察点：`识别候选框数量`、每候选的 `classId`、`gap 无候选 → 刷新` 分支日志

---

> 训练侧（采集 → 预标注 → 复核 → 合成标签 → 训练）的完整操作流程见
> [WORKFLOW.md](WORKFLOW.md)，本文档只负责端侧代码适配。
