"""把已复核的预标注图片复制到数据集目录，并按约 10% 随机划分 train/val。

标签已由 collect_to_dataset.py 生成到 data/datasets/slider/labels/train/*_prelabeled.txt，
本脚本只负责把 data/labeled/ 里对应的 png 复制成 *_prelabeled.png，并划分 val。
"""
from __future__ import annotations

import pathlib
import random
import shutil

# 工程根 = scripts/ 的上一级
ROOT = pathlib.Path(__file__).resolve().parent.parent
BASE = ROOT / "data" / "datasets" / "slider"
IMG = BASE / "images"
LAB = BASE / "labels"
SRC = ROOT / "data" / "labeled"  # 标注工作区源图（prelabel_gap 已复制 png 到此）
SEED = 42
VAL_RATIO = 0.1


def main() -> None:
    train_lab = LAB / "train"
    train_img = IMG / "train"
    val_img = IMG / "val"
    val_lab = LAB / "val"
    for d in (train_img, val_img, val_lab):
        d.mkdir(parents=True, exist_ok=True)

    stems = sorted(p.stem for p in train_lab.glob("*_prelabeled.txt"))
    if not stems:
        print("没有找到标签，请先运行 collect_to_dataset.py")
        return

    rng = random.Random(SEED)
    shuffled = stems[:]
    rng.shuffle(shuffled)
    n_val = max(1, round(len(shuffled) * VAL_RATIO))
    valset = set(shuffled[:n_val])

    copied = moved_img = moved_lab = 0
    for s in stems:
        split = "val" if s in valset else "train"
        # s 形如 "126_prelabeled"，对应源图 126.png
        base_no = s[: -len("_prelabeled")]
        src_png = SRC / f"{base_no}.png"
        if not src_png.exists():
            print(f"[缺图] {src_png}")
            continue
        shutil.copy2(src_png, train_img / f"{s}.png")
        copied += 1
        # 移入 val（含图片与标签）
        if split == "val":
            shutil.move(str(train_img / f"{s}.png"), val_img / f"{s}.png")
            shutil.move(str(train_lab / f"{s}.txt"), val_lab / f"{s}.txt")
            moved_img += 1
            moved_lab += 1

    print(f"复制图片到 train: {copied}")
    print(f"val 划分: 图片 {moved_img} 个, 标签 {moved_lab} 个")
    print(f"train 图片数: {len(list(train_img.glob('*.png')))}")
    print(f"val 图片数: {len(list(val_img.glob('*.png')))}")
    print(f"train 标签数: {len(list(train_lab.glob('*.txt')))}")
    print(f"val 标签数: {len(list(val_lab.glob('*.txt')))}")


if __name__ == "__main__":
    main()
