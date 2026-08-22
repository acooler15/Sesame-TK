"""缺口(gap)预标注脚本 — 用 captcha-recognizer 识别缺口，人工只需复核。

流程定位（完整链路）:
    collect.js 采集  ->  本脚本预标注 gap  ->  人工复核 review 图  ->
    collect_to_dataset.py 合成四类标签  ->  train.py 训练

功能:
    1. 对每张采集图跑 captcha-recognizer（YOLO 缺口检测），得到 gap box + 置信度；
    2. 若存在同名 .json（collect.js 输出）:
       - 把 gap box（截图坐标系）写入 json 的 gapimg 字段；
       - 把 slotimg / refreshButton / refreshImg / feedbackButton / feedbackImg 等
         视口坐标统一减去 puzzleContainer 原点（退回 backimg 原点），并按截图
         实际尺寸更新 imageWidth/imageHeight —— 保证 collect_to_dataset.py 归一化正确；
       - 回写为 <name>_prelabeled.json（不覆盖原始采集文件）；
    3. 生成 review_<name>.png 可视化（gap=红 / block=绿 / refresh=蓝 / feedback=紫 + 置信度），
       人工只需查看 review 图，改错直接编辑 *_prelabeled.json 的 gapimg；
    4. 汇总输出 失败/低置信度 清单，指导重点复核；
    5. 自动去重：验证码图库资源有限，采集样本中相同底图+相同缺口会重复出现，
       按像素内容哈希保留首张、剔除后续重复（计入 duplicate 统计），避免过拟合；
    6. 已人工复核（reviewed=true）的样本默认跳过、不覆盖人工成果；--force 可强制重标。

用法:
    uv run prelabel_gap.py                           # 默认整目录 data/captured/，自动去重
    uv run prelabel_gap.py data/captured/*.png       # 指定文件
    uv run prelabel_gap.py data/captured/            # 目录形式，递归找 *.png（跳过 review_*）
    uv run prelabel_gap.py data/captured/ --min-conf 0.6
    uv run prelabel_gap.py data/captured/ --force    # 对已复核样本也重新预标注

输出:
    默认写到工程 data/labeled/（与采集目录解耦的标注工作区）：
    {stem}_prelabeled.json + review_{stem}.png + 源图副本
    人工复核 data/labeled/ 后，交给 collect_to_dataset.py / split_dataset.py 出数据集。

依赖: uv add captcha-recognizer（内部 onnxruntime + opencv）
"""
from __future__ import annotations

import argparse
import hashlib
import json
import pathlib
import sys

import cv2
from captcha_recognizer.slider import Slider

# 需要从视口坐标换算到截图坐标的 DOM rect 字段（减去容器原点）
VIEWPORT_RECT_FIELDS = ("slotimg", "refreshButton", "refreshImg", "feedbackButton",
                        "feedbackImg", "handler", "sliderContainer")


def shift_rect(rect_obj: dict, ox: float, oy: float) -> dict:
    """视口坐标 rect -> 截图坐标（减去容器原点），保留 cx/cy 派生量。"""
    if not rect_obj:
        return rect_obj
    out = dict(rect_obj)
    for key in ("x", "cx"):
        if key in out:
            out[key] = out[key] - ox
    for key in ("y", "cy"):
        if key in out:
            out[key] = out[key] - oy
    return out


def draw_box(img, box, color, label: str) -> None:
    x1, y1, x2, y2 = (int(round(v)) for v in box)
    cv2.rectangle(img, (x1, y1), (x2, y2), color, 2)
    cv2.putText(img, label, (x1, max(0, y1 - 6)),
                cv2.FONT_HERSHEY_SIMPLEX, 0.5, color, 1, cv2.LINE_AA)


def process_one(slider: Slider, png: pathlib.Path, out_dir: pathlib.Path,
                min_conf: float) -> tuple[str, str | None]:
    """处理单张图。返回 (状态, 输出json路径或失败原因)。

    状态: ok / low-conf / failed
    """
    img = cv2.imdecode(numpy_from_path(png), cv2.IMREAD_COLOR)
    if img is None:
        return "failed", "图片读取失败"
    h, w = img.shape[:2]

    # 1. 缺口识别（截图坐标系）
    box = confidence = None
    err: str | None = None
    try:
        box, confidence = slider.identify(source=str(png))
    except Exception as e:  # noqa: BLE001 — 第三方识别器异常统一降级为 failed
        err = str(e)

    status = "ok" if box and confidence is not None and confidence >= min_conf else \
             ("low-conf" if box and confidence is not None else "failed")

    # 输出目录与采集目录解耦时，先复制源图到工作区（png + json + review 图成组）。
    # 识别失败也复制：让无 gap 结果的样本进入标注工作区，供人工在复核界面补标。
    if out_dir != png.parent:
        (out_dir / png.name).write_bytes(png.read_bytes())

    # 2. 写回 json（若有）
    jp = png.with_suffix(".json")
    out_json: pathlib.Path | None = None
    out_json = out_dir / f"{png.stem}_prelabeled.json"
    if jp.exists():
        data = json.loads(jp.read_text(encoding="utf-8"))
        # 容器原点：优先 puzzleContainer，退回 backimg
        origin = data.get("puzzleContainer") or data.get("backimg") or {}
        ox, oy = origin.get("x", 0.0), origin.get("y", 0.0)

        # DOM rect 视口坐标 -> 截图坐标
        for field in VIEWPORT_RECT_FIELDS:
            if field in data:
                data[field] = shift_rect(data[field], ox, oy)

        # gap 写入（box=[x1,y1,x2,y2] -> x/y/w/h，截图坐标系，天然一致）
        # 识别失败时不写 gapimg，留给人工补标
        if box and confidence is not None:
            data["gapimg"] = {
                "x": float(box[0]), "y": float(box[1]),
                "width": float(box[2] - box[0]), "height": float(box[3] - box[1]),
            }
            data["gapConfidence"] = float(confidence)
        else:
            data["predictError"] = err or "无识别结果"
        data["imageWidth"], data["imageHeight"] = w, h

        out_json.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")

    # 3. review 可视化
    review = img.copy()
    if box and confidence is not None:
        draw_box(review, box, (0, 0, 255), f"gap {confidence:.2f}")
    if jp.exists() and out_json is not None:
        data = json.loads(out_json.read_text(encoding="utf-8"))
        for field, color, name in (
            ("slotimg", (0, 200, 0), "block"),
            ("refreshButton", (255, 160, 0), "refresh"),
            ("feedbackButton", (200, 0, 255), "feedback"),
        ):
            r = data.get(field)
            if r and r.get("width"):
                draw_box(review, (r["x"], r["y"], r["x"] + r["width"], r["y"] + r["height"]),
                         color, name)
    cv2.imwrite(str(out_dir / f"review_{png.stem}.png"), review)

    if status == "failed":
        return "failed", err or "无识别结果"
    return status, str(out_json)


def numpy_from_path(p: pathlib.Path):
    import numpy as np
    return np.frombuffer(p.read_bytes(), dtype=np.uint8)


def image_hash(p: pathlib.Path) -> str:
    """基于解码后的像素内容计算哈希，识别内容相同的重复图。

    验证码图库资源有限，采集几百张必然出现大量重复（相同底图+相同缺口）。
    用像素内容而非文件字节做哈希，可容忍 PNG 压缩/元数据的细微差异。
    """
    img = cv2.imdecode(numpy_from_path(p), cv2.IMREAD_COLOR)
    if img is None:
        return ""
    return hashlib.md5(img.tobytes()).hexdigest()


def main() -> None:
    ROOT = pathlib.Path(__file__).resolve().parent.parent
    parser = argparse.ArgumentParser(description="captcha-recognizer 缺口预标注")
    parser.add_argument("inputs", nargs="*",
                        help="采集 png 或其所在目录（默认 data/captured/）")
    parser.add_argument("--review-dir", default=None,
                        help="review 图与 prelabeled json 输出目录（默认 data/labeled/）")
    parser.add_argument("--min-conf", type=float, default=0.6,
                        help="置信度低于该值标记 low-conf（默认 0.6，重点复核）")
    parser.add_argument("--force", action="store_true",
                        help="对输入范围内所有文件强制重新预标注（含已复核的，覆盖其 reviewStatus/手改 gapimg）")
    args = parser.parse_args()

    # 默认输入目录 data/captured/；输出目录默认 data/labeled/（均相对工程根）
    inputs = args.inputs or [str(ROOT / "data" / "captured")]
    default_out = ROOT / "data" / "labeled"

    pngs: list[pathlib.Path] = []
    for item in inputs:
        p = pathlib.Path(item)
        if not p.is_absolute():
            p = ROOT / p
        if p.is_dir():
            pngs += sorted(q for q in p.rglob("*.png") if not q.name.startswith("review_"))
        else:
            pngs.append(p)
    if not pngs:
        print("未找到待标注 png")
        sys.exit(1)

    slider = Slider()
    stats = {"ok": 0, "low-conf": 0, "failed": 0, "duplicate": 0}
    attention: list[str] = []
    default_out.mkdir(parents=True, exist_ok=True)

    # 输出目录：--review-dir 未指定时用 data/labeled/，指定时相对工程根解析
    out_dir = pathlib.Path(args.review_dir) if args.review_dir else default_out
    if not out_dir.is_absolute():
        out_dir = ROOT / out_dir

    # 像素内容去重：验证码图库有限，重复样本会过拟合，预标注时直接剔除。
    # 保留首次出现的样本，后续相同内容的图跳过（不识别、不产出标注文件）。
    seen_hashes: dict[str, str] = {}
    for png in pngs:
        h = image_hash(png)
        if h and h in seen_hashes:
            stats["duplicate"] += 1
            print(f"[-] {png.name}: duplicate (内容与首张 {seen_hashes[h]} 相同，剔除)")
            continue
        if h:
            seen_hashes[h] = png.name

        # 已人工复核过的样本跳过，保留 reviewStatus/修正后的 gapimg，避免重跑预标注冲掉人工成果
        # --force 时无视复核状态，强制重新预标注
        out_json = out_dir / f"{png.stem}_prelabeled.json"
        if not args.force and out_json.exists():
            prev = json.loads(out_json.read_text(encoding="utf-8"))
            if prev.get("reviewed"):
                print(f"[~] {png.name}: 已人工复核，跳过（保留 reviewStatus={prev.get('reviewStatus')}）")
                continue

        status, info = process_one(slider, png, out_dir, args.min_conf)
        stats[status] += 1
        mark = {"ok": "+", "low-conf": "?", "failed": "x"}[status]
        print(f"[{mark}] {png.name}: {status}" + (f" ({info})" if status != "ok" else ""))
        if status != "ok":
            attention.append(f"{png.name}: {info}")

    print(f"\n完成: ok={stats['ok']} low-conf={stats['low-conf']} "
          f"failed={stats['failed']} duplicate={stats['duplicate']}")
    if attention:
        print("\n需人工处理（复核 review 图后手改 *_prelabeled.json 的 gapimg，或剔除该样本）:")
        for line in attention:
            print("  -", line)


if __name__ == "__main__":
    main()
