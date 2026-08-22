"""把网页采集的验证码 DOM 标注转为 YOLO detect 训练标签（四类）。

输入: collect.js 在页面上输出的 JSON 结构（保存为 .json 文件）。
输出: data/datasets/slider/labels/train/<id>.txt（图片复制见 split_dataset.py）

标注映射（与 slider.yaml 类别一致）:
    - 0 gap      「缺口」: 网页 DOM 无独立节点，需人工标注或由旧 slider.tflite
                 预标注后修正（collect.js 已预留 gapimg / backimg-gap 字段）。
    - 1 block    「滑块块」: slotimg 矩形，全自动。
    - 2 refresh  「刷新按钮」: 拼图区右上角 refresh-button 矩形（最右），全自动。
    - 3 feedback 「反馈按钮」: 拼图区右上角 feedback-button 矩形（刷新左侧），全自动。

标签格式（YOLO detect，每行一个目标）:
    <class> <cx> <cy> <w> <h>   # 全部归一化到 0~1

用法:
    uv run collect_to_dataset.py                        # 默认 data/labeled/，只吃复核通过样本
    uv run collect_to_dataset.py data/labeled/          # 指定目录
    uv run collect_to_dataset.py --all                  # 含未复核样本（预览用）
    （默认只消费 reviewStatus=approved 的样本；--all 才包含未复核的。
      支持目录、通配符、单个文件；PowerShell 下通配符不展开也由脚本自行兜底）
"""
from __future__ import annotations

import argparse
import json
import pathlib

CLS_GAP = 0
CLS_BLOCK = 1
CLS_REFRESH = 2
CLS_FEEDBACK = 3


def box_to_detect(cls_id: int, x: float, y: float, w: float, h: float,
                  img_w: float, img_h: float) -> str:
    """左上角+宽高 转 YOLO detect 归一化中心点格式。"""
    cx = (x + w / 2) / img_w
    cy = (y + h / 2) / img_h
    nw = w / img_w
    nh = h / img_h
    return f"{cls_id} {cx:.6f} {cy:.6f} {nw:.6f} {nh:.6f}"


def main() -> None:
    parser = argparse.ArgumentParser(description="网页采集 -> YOLO detect 四类数据集")
    parser.add_argument("jsons", nargs="*",
                        help="collect.js 输出的 JSON 文件/目录（默认 data/labeled/）")
    parser.add_argument("--out", default="data/datasets/slider", help="输出数据集目录（相对工程根）")
    parser.add_argument("--all", action="store_true",
                        help="包含未复核样本（默认只消费 reviewStatus=approved 的样本）")
    args = parser.parse_args()

    # 工程根 = scripts/ 的上一级
    ROOT = pathlib.Path(__file__).resolve().parent.parent
    out_path = pathlib.Path(args.out)
    out = out_path if out_path.is_absolute() else ROOT / out_path
    img_dir = out / "images" / "train"
    lab_dir = out / "labels" / "train"
    img_dir.mkdir(parents=True, exist_ok=True)
    lab_dir.mkdir(parents=True, exist_ok=True)

    stats = {CLS_GAP: 0, CLS_BLOCK: 0, CLS_REFRESH: 0, CLS_FEEDBACK: 0}

    # 默认输入目录 data/labeled/
    items = args.jsons or [str(ROOT / "data" / "labeled")]
    # 展开目录 / 通配符：PowerShell 不会像 Bash 那样自动展开 *，脚本自己兜底，
    # 让 `data/labeled/`、`data/labeled/*_prelabeled.json` 两种写法都可用。
    json_paths: list[pathlib.Path] = []
    for item in items:
        p = pathlib.Path(item)
        if not p.is_absolute():
            p = ROOT / p
        if p.is_dir():
            json_paths += sorted(q for q in p.glob("*_prelabeled.json"))
        elif any(ch in item for ch in "*?["):
            json_paths += sorted(p.parent.glob(p.name))
        else:
            json_paths.append(p)
    if not json_paths:
        print("未找到待转换的 *_prelabeled.json")
        return

    skipped_unreviewed = 0
    for jp in json_paths:
        if not jp.exists():
            print(f"[跳过] 文件不存在: {jp}")
            continue
        data = json.loads(jp.read_text(encoding="utf-8"))
        if not args.all and data.get("reviewStatus") != "approved":
            skipped_unreviewed += 1
            continue
        # 图片需与标注同名存在（前端采集时把拼图区域截图保存为 <name>.png）
        img_w = data.get("imageWidth", 393)
        img_h = data.get("imageHeight", 852)
        lines = []

        slot = data.get("slotimg")
        gap = data.get("gapimg") or data.get("backimg-gap")
        # 拼图区右上角刷新按钮（优先容器节点，其次图标节点）
        refresh = data.get("refreshButton") or data.get("refreshImg")
        # 拼图区右上角反馈按钮（刷新按钮左侧，优先容器节点，其次图标节点）
        feedback = data.get("feedbackButton") or data.get("feedbackImg")

        # 滑块块 → block
        if slot and slot.get("width"):
            lines.append(box_to_detect(CLS_BLOCK, slot["x"], slot["y"],
                                       slot["width"], slot["height"], img_w, img_h))
            stats[CLS_BLOCK] += 1
        # 刷新按钮 → refresh
        if refresh and refresh.get("width"):
            lines.append(box_to_detect(CLS_REFRESH, refresh["x"], refresh["y"],
                                       refresh["width"], refresh["height"], img_w, img_h))
            stats[CLS_REFRESH] += 1
        # 反馈按钮 → feedback
        if feedback and feedback.get("width"):
            lines.append(box_to_detect(CLS_FEEDBACK, feedback["x"], feedback["y"],
                                       feedback["width"], feedback["height"], img_w, img_h))
            stats[CLS_FEEDBACK] += 1
        # 缺口 → gap（需人工/预标注补入 JSON）
        if gap and gap.get("width"):
            lines.append(box_to_detect(CLS_GAP, gap["x"], gap["y"],
                                       gap["width"], gap["height"], img_w, img_h))
            stats[CLS_GAP] += 1

        stem = jp.stem
        (lab_dir / f"{stem}.txt").write_text("\n".join(lines) + "\n", encoding="utf-8")
        print(f"[collect] {stem}: {len(lines)} 个目标 -> {lab_dir / f'{stem}.txt'}")

    print(f"\n完成。图片需放置于 {img_dir}（同名 .png）")
    if skipped_unreviewed:
        print(f"[跳过] {skipped_unreviewed} 个样本未复核通过（reviewStatus != approved）")
    print(f"标注统计: gap={stats[CLS_GAP]} block={stats[CLS_BLOCK]} refresh={stats[CLS_REFRESH]} feedback={stats[CLS_FEEDBACK]}")
    if stats[CLS_GAP] == 0:
        print("[提醒] 缺口(gap)标注为 0：需人工标注或用旧 slider.tflite 预标注后写入 JSON 的 gapimg 字段，再重跑本脚本")
    if stats[CLS_FEEDBACK] == 0:
        print("[提醒] 反馈按钮(feedback)标注为 0：需确认采集页反馈按钮可定位（feedbackButton/feedbackImg 字段）")


if __name__ == "__main__":
    main()
