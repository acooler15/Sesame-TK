# /// script
# requires-python = ">=3.9"
# dependencies = ["pillow"]
# ///
"""检测采集/标注图片中的重复样本（相同底图 + 相同缺口）。

验证码图库资源有限，采集几百张必然存在重复组合。重复样本会让模型过拟合，
训练前应识别并去重（或至少了解重复程度）。

原理:
    对每张 png 计算像素级哈希，找出内容完全相同的图片。
    由于截图是「底图 + 缺口 + 按钮」的合成结果，若两张图哈希相同，
    说明底图与缺口位置完全一致（即真正重复）。

用法:
    uv run scripts/find_duplicates.py data/captured/
    uv run scripts/find_duplicates.py data/labeled/ --min-group 2
"""
from __future__ import annotations

import argparse
import hashlib
import pathlib


def file_hash(p: pathlib.Path) -> str:
    return hashlib.md5(p.read_bytes()).hexdigest()


def main() -> None:
    parser = argparse.ArgumentParser(description="检测重复样本")
    parser.add_argument("dirs", nargs="+", help="待扫描目录（如 data/captured/）")
    parser.add_argument("--min-group", type=int, default=2,
                        help="至少重复多少次才算一组（默认 2，即出现一次重复就算）")
    args = parser.parse_args()

    # 收集所有 png，跳过 review_* 可视化图
    pngs: list[pathlib.Path] = []
    for d in args.dirs:
        p = pathlib.Path(d)
        if not p.is_dir():
            continue
        pngs += sorted(q for q in p.rglob("*.png") if not q.name.startswith("review_"))

    print(f"扫描 {len(pngs)} 张图片…")
    groups: dict[str, list[pathlib.Path]] = {}
    for png in pngs:
        h = file_hash(png)
        groups.setdefault(h, []).append(png)

    # 统计
    unique = len(groups)
    dup_groups = {h: v for h, v in groups.items() if len(v) >= args.min_group}
    dup_images = sum(len(v) - 1 for v in dup_groups.values())  # 多余的副本数

    print(f"\n总图片数 : {len(pngs)}")
    print(f"唯一图片 : {unique}")
    print(f"重复组数 : {len(dup_groups)}（每组 >= {args.min_group} 张）")
    print(f"多余副本 : {dup_images} 张（可去除的重复数）")

    if dup_groups:
        print(f"\n重复组明细（按组大小降序，前 50 组）:")
        for h, v in sorted(dup_groups.items(), key=lambda kv: -len(kv[1]))[:50]:
            names = ", ".join(p.name for p in v)
            print(f"  x{len(v)} : {names}")


if __name__ == "__main__":
    main()
