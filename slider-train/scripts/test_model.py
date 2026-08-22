"""用训练好的 best.pt 对验证码截图跑推理，检验四类识别效果。

用途:
    - 拿训练集之外的图（data/captured/ 未进训练的去重后剩余图、或全新采集的图）
      跑 best.pt，检验模型真实泛化能力，而非 val 集自嗨；
    - 打印每张图的四类框（gap/block/refresh/feedback）与置信度；
    - 可选生成可视化结果图（--save），红 gap / 绿 block / 蓝 refresh / 紫 feedback。

用法:
    uv run scripts/test_model.py data/captured/                 # 目录
    uv run scripts/test_model.py data/captured/001.png          # 单张
    uv run scripts/test_model.py data/captured/ --save          # 同时保存可视化
    uv run scripts/test_model.py data/captured/ --conf 0.5      # 自定义置信度阈值
    uv run scripts/test_model.py data/captured/ --weights data/runs/slider/weights/best.pt

输出:
    - 控制台逐张打印识别结果（各类别框 + 置信度）；
    - --save 时在工程根 data/test_out/ 下输出 <name>_pred.png 可视化图，
      颜色约定：红 gap / 绿 block / 蓝 refresh / 紫 feedback。
"""
from __future__ import annotations

import argparse
import pathlib

import cv2
from ultralytics import YOLO

# 类别索引与颜色（与 config/slider.yaml / SliderTFLite.kt 一致，勿改顺序）
CLASSES = {0: "gap", 1: "block", 2: "refresh", 3: "feedback"}
COLORS = {0: (0, 0, 255), 1: (0, 200, 0), 2: (255, 160, 0), 3: (200, 0, 255)}  # BGR


def collect_pngs(items: list[str], root: pathlib.Path) -> list[pathlib.Path]:
    """展开输入为 png 列表（目录递归，跳过 review_* / *_pred.png）。"""
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
    parser = argparse.ArgumentParser(description="用 best.pt 对验证码截图跑推理")
    parser.add_argument("inputs", nargs="*",
                        help="图片/目录（默认 data/captured/）")
    parser.add_argument("--weights", default=None,
                        help="权重路径（默认自动找最近的 data/runs/*/weights/best.pt）")
    parser.add_argument("--conf", type=float, default=0.5, help="置信度阈值（默认 0.5）")
    parser.add_argument("--save", action="store_true", help="保存可视化预测图到 data/test_out/")
    args = parser.parse_args()

    # 权重：显式传入 > 自动找最近 best.pt
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
        print("未找到待测试 png")
        return

    out_dir = ROOT / "data" / "test_out"
    if args.save:
        out_dir.mkdir(parents=True, exist_ok=True)

    print(f"\n待测图片: {len(pngs)} 张，置信度阈值 {args.conf}\n")

    for png in pngs:
        results = model.predict(source=str(png), conf=args.conf, verbose=False)
        r = results[0]
        boxes = r.boxes
        by_class: dict[int, list] = {}
        if boxes is not None:
            for i in range(len(boxes)):
                cls = int(boxes.cls[i].item())
                conf = float(boxes.conf[i].item())
                xyxy = boxes.xyxy[i].tolist()
                by_class.setdefault(cls, []).append((conf, xyxy))

        # 打印结果
        print(f"{png.name}:")
        if not by_class:
            print("   (无检测结果)")
        for cls_id in sorted(by_class):
            name = CLASSES.get(cls_id, str(cls_id))
            for conf, xyxy in by_class[cls_id]:
                x1, y1, x2, y2 = (round(v, 1) for v in xyxy)
                print(f"   {name:<8} conf={conf:.3f}  box=({x1},{y1},{x2},{y2})")
        # 重点提示：gap 缺失意味着端侧会走刷新重试
        if 0 not in by_class:
            print("   [提示] 未识别到 gap，端侧将触发刷新重试")

        # 保存可视化（自绘，颜色与 prelabel_gap.py 的 review 图约定一致：
        # 红 gap / 绿 block / 蓝 refresh / 紫 feedback；不用 r.save() 的默认调色板）
        if args.save:
            im = cv2.imread(str(png))
            if im is not None:
                for cls_id, dets in by_class.items():
                    color = COLORS.get(cls_id, (128, 128, 128))
                    label = CLASSES.get(cls_id, str(cls_id))
                    for conf, xyxy in dets:
                        x1, y1, x2, y2 = (int(round(v)) for v in xyxy)
                        cv2.rectangle(im, (x1, y1), (x2, y2), color, 2)
                        cv2.putText(im, f"{label} {conf:.2f}", (x1, max(12, y1 - 6)),
                                    cv2.FONT_HERSHEY_SIMPLEX, 0.5, color, 1, cv2.LINE_AA)
                cv2.imwrite(str(out_dir / f"{png.stem}_pred.png"), im)
    if args.save:
        print(f"\n完成。可视化输出: {out_dir}")
        print("颜色说明: 红=gap(缺口) 绿=block(滑块) 蓝=refresh(刷新) 紫=feedback(反馈)")
    else:
        print("\n完成。")


if __name__ == "__main__":
    main()
