"""挖掘 gap 低置信度的困难样本，指导补标注 / 过采样 / 重训。

对目录下所有图跑 best.pt（极低阈值 0.01 保证不漏弱检出），统计每张图的
gap 最高置信度，按升序列出低于阈值的图 —— 这些就是模型"不敢确定"的困难样本。

输出同时标注该图是否在训练集中（按像素内容判断，而非文件名，避免把去重时
被剔除的重复图误判成"没见过"），便于区分两类困难：
    - 训过仍难（如置信度 0.3 的 140.png）→ 检查标注，训练时过采样；
    - 真没训过的 → 走 prelabel + review 流程补进训练集。

用法:
    uv run scripts/find_hard_cases.py                       # 默认扫 data/captured/
    uv run scripts/find_hard_cases.py data/captured/ --min-conf 0.5
"""
from __future__ import annotations

import argparse
import hashlib
import pathlib

import cv2
import numpy as np
from ultralytics import YOLO

CLS_GAP = 0


def image_hash(p: pathlib.Path) -> str:
    """基于解码后的像素内容计算哈希，与 prelabel_gap.py 的去重口径一致。

    用像素内容而非文件字节，可容忍 PNG 压缩/元数据差异；用内容而非文件名，
    避免把「去重时被剔除的重复图」误判成「训练集没见过」。
    """
    img = cv2.imdecode(np.frombuffer(p.read_bytes(), dtype=np.uint8), cv2.IMREAD_COLOR)
    if img is None:
        return ""
    return hashlib.md5(img.tobytes()).hexdigest()


def collect_pngs(items: list[str], root: pathlib.Path) -> list[pathlib.Path]:
    pngs: list[pathlib.Path] = []
    for item in items:
        p = pathlib.Path(item)
        if not p.is_absolute():
            p = root / p
        if p.is_dir():
            pngs += sorted(
                q for q in p.rglob("*.png")
                if not q.name.startswith("review_") and not q.name.endswith("_pred.png")
            )
        else:
            pngs.append(p)
    return pngs


def main() -> None:
    ROOT = pathlib.Path(__file__).resolve().parent.parent
    parser = argparse.ArgumentParser(description="挖掘 gap 低置信度困难样本")
    parser.add_argument("inputs", nargs="*", help="图片/目录（默认 data/captured/）")
    parser.add_argument("--weights", default=None, help="权重（默认自动找最近 best.pt）")
    parser.add_argument("--min-conf", type=float, default=0.5,
                        help="gap 置信度低于该值视为困难样本（默认 0.5）")
    args = parser.parse_args()

    if args.weights:
        wp = pathlib.Path(args.weights)
        weights = str(wp if wp.is_absolute() else ROOT / wp)
    else:
        cands = sorted(
            (p for p in (ROOT / "data" / "runs").glob("*/weights/best.pt")),
            key=lambda p: p.stat().st_mtime,
        )
        if not cands:
            print("未找到 best.pt，请先训练或 --weights 指定")
            return
        weights = str(cands[-1])
        print(f"[权重] {weights}")

    model = YOLO(weights)
    inputs = args.inputs or [str(ROOT / "data" / "captured")]
    pngs = collect_pngs(inputs, ROOT)
    if not pngs:
        print("未找到待分析 png")
        return

    train_img_dir = ROOT / "data" / "datasets" / "slider" / "images" / "train"

    # 训练集内容哈希集合（像素内容口径，避免把去重时的重复图误判成"没见过"）
    train_hashes: set[str] = {
        image_hash(p) for p in train_img_dir.glob("*.png")
    }

    print(f"分析 {len(pngs)} 张图（推理阈值 0.01，只统计 gap 最高置信度）…\n")
    rows: list[tuple[str, float, bool]] = []  # (name, gap_conf, in_train)
    results = model.predict(source=[str(p) for p in pngs], conf=0.01, verbose=False)
    for r in results:
        name = pathlib.Path(r.path).name
        gap_conf = 0.0
        if r.boxes is not None:
            for i in range(len(r.boxes)):
                if int(r.boxes.cls[i].item()) == CLS_GAP:
                    gap_conf = max(gap_conf, float(r.boxes.conf[i].item()))
        in_train = image_hash(pathlib.Path(r.path)) in train_hashes
        rows.append((name, gap_conf, in_train))

    rows.sort(key=lambda x: x[1])
    hard = [row for row in rows if row[1] < args.min_conf]

    n_train_hard = sum(1 for _, _, t in hard if t)
    print(f"总图数: {len(rows)}")
    print(f"困难样本（gap 置信度 < {args.min_conf}）: {len(hard)} 张，其中 {n_train_hard} 张已在训练集\n")
    if hard:
        print(f"{'图片':<16} {'gap置信度':<10} 是否在训练集")
        for name, conf, in_train in hard:
            mark = "无检出" if conf == 0.0 else f"{conf:.3f}"
            print(f"{name:<16} {mark:<10} {'是(训过仍难→检查标注/过采样)' if in_train else '否(补进训练集)'}")
    else:
        print("没有困难样本，识别率良好。")


if __name__ == "__main__":
    main()
