"""训练滑块验证码检测模型（YOLO26，四类：gap/block/refresh/feedback）。

用法（在 slider-train 工程根或任意位置）:
    uv run scripts/train.py                          # 从 weights/yolo26n.pt 微调
    # 从中断处继续训练（last.pt + --resume，恢复 optimizer/学习率/epoch 计数）:
    uv run scripts/train.py --weights data/runs/slider/weights/last.pt --resume
    # 仅用某个权重重新开始一段新训练（不恢复训练状态）:
    uv run scripts/train.py --weights data/runs/slider/weights/best.pt

路径约定:
    - 数据配置: config/slider.yaml
    - 起始权重: weights/*.pt（或显式传路径）
    - 输出:     data/runs/（project 默认 data/runs，name 默认 slider）
    脚本基于自身位置推导工程根，任何工作目录下运行均可。

底座说明:
    - yolo26n.pt (默认): NMS-free 端到端（e2e）检测，去 DFL。
      TFLite 导出默认走一对一(e2e)头，输出 [1, 300, 6]
      （x1,y1,x2,y2,conf,classId），端侧无需 NMS，直接取框。

与端侧的一致性约定:
    - imgsz=640、归一化 /255、letterbox 填充 rgb(114,114,114)，均与 Android 端一致。
"""
from __future__ import annotations

import argparse
import tempfile
from pathlib import Path

import yaml
from ultralytics import YOLO

# 工程根 = scripts/ 的上一级
ROOT = Path(__file__).resolve().parent.parent


def resolve(path: str) -> str:
    """把相对路径解析到工程根；绝对路径原样返回。"""
    p = Path(path)
    return str(p if p.is_absolute() else ROOT / p)


def resolve_data_yaml(data_arg: str) -> str:
    """把数据配置 yaml 里的相对 `path` 解析为绝对路径，再交给 Ultralytics。

    Ultralytics 对 yaml 内的 path/train/val 都是相对「当前工作目录」解析，
    不依赖 yaml 文件位置。这里统一约定 yaml 的 `path` 以工程根为基准，
    先解析成绝对路径（若已是绝对路径则不动）写到临时 yaml，
    保证无论 CWD 在哪都能正确找到数据集。
    """
    src = Path(resolve(data_arg))
    cfg = yaml.safe_load(src.read_text(encoding="utf-8"))
    p = Path(cfg["path"])
    cfg["path"] = str(p if p.is_absolute() else ROOT / p)

    tmp = Path(tempfile.mkstemp(suffix=".yaml")[1])
    tmp.write_text(yaml.safe_dump(cfg, allow_unicode=True, sort_keys=False),
                   encoding="utf-8")
    return str(tmp)


def main() -> None:
    parser = argparse.ArgumentParser(description="训练滑块验证码检测模型（YOLO26 四类）")
    parser.add_argument(
        "--weights",
        default="weights/yolo26n.pt",
        help="起始权重，默认 weights/yolo26n.pt；可传续训权重",
    )
    parser.add_argument("--data", default="config/slider.yaml", help="数据配置 yaml（相对工程根）")
    parser.add_argument("--epochs", type=int, default=100, help="训练轮数")
    parser.add_argument("--imgsz", type=int, default=640, help="输入尺寸")
    parser.add_argument("--batch", type=int, default=16, help="批大小")
    parser.add_argument("--device", default="", help="设备，如 '0'、'cpu'，空则自动")
    parser.add_argument("--project", default="data/runs", help="输出根目录（相对工程根）")
    parser.add_argument("--name", default="slider", help="本次实验名")
    parser.add_argument("--resume", action="store_true",
                        help="从中断处继续训练（用 --weights 传 last.pt，恢复 optimizer/学习率/epoch 计数）")
    args = parser.parse_args()

    model = YOLO(resolve(args.weights))
    model.train(
        data=resolve_data_yaml(args.data),
        epochs=args.epochs,
        imgsz=args.imgsz,
        batch=args.batch,
        device=args.device,
        project=resolve(args.project),
        name=args.name,
        exist_ok=True,
        resume=args.resume,
        # 滑块验证码禁用水平翻转：block 恒在左、gap 分布有左右语义，
        # 翻转会造出现实中不存在的样本（block 在右），干扰 gap 学习。
        fliplr=0.0,
    )


if __name__ == "__main__":
    main()
