"""训练滑块验证码检测模型（YOLO detect，三类：gap/block/refresh）。

用法（在 slider-train 工程根或任意位置）:
    uv run scripts/train.py                          # 从 weights/yolo11n.pt 微调
    uv run scripts/train.py --weights weights/yolo26n.pt
    uv run scripts/train.py --weights data/runs/slider/weights/best.pt   # 续训

路径约定:
    - 数据配置: config/slider.yaml
    - 起始权重: weights/*.pt（或显式传路径）
    - 输出:     data/runs/（project 默认 data/runs，name 默认 slider）
    脚本基于自身位置推导工程根，任何工作目录下运行均可。

底座说明:
    - yolo11n.pt (默认): 成熟稳定，TFLite 导出输出布局确定
      （[1, 4+nc, 8400]，本仓库三类即 [1,7,8400]），端侧解析资料多。
    - yolo26n.pt: 更新更快（NMS-free、去 DFL），但 TFLite 输出布局不同
      （e2e top-k 直出），端侧需按实际导出 shape 适配，见 ANDROID_INTEGRATION.md。

与端侧的一致性约定:
    - imgsz=640、归一化 /255、letterbox 填充 rgb(114,114,114)，均与 Android 端一致。
"""
from __future__ import annotations

import argparse
from pathlib import Path

from ultralytics import YOLO

# 工程根 = scripts/ 的上一级
ROOT = Path(__file__).resolve().parent.parent


def resolve(path: str) -> str:
    """把相对路径解析到工程根；绝对路径原样返回。"""
    p = Path(path)
    return str(p if p.is_absolute() else ROOT / p)


def main() -> None:
    parser = argparse.ArgumentParser(description="训练滑块验证码检测模型（三类）")
    parser.add_argument(
        "--weights",
        default="weights/yolo11n.pt",
        help="起始权重，默认 weights/yolo11n.pt；可换 yolo26n.pt 或续训权重",
    )
    parser.add_argument("--data", default="config/slider.yaml", help="数据配置 yaml（相对工程根）")
    parser.add_argument("--epochs", type=int, default=100, help="训练轮数")
    parser.add_argument("--imgsz", type=int, default=640, help="输入尺寸")
    parser.add_argument("--batch", type=int, default=16, help="批大小")
    parser.add_argument("--device", default="", help="设备，如 '0'、'cpu'，空则自动")
    parser.add_argument("--project", default="data/runs", help="输出根目录（相对工程根）")
    parser.add_argument("--name", default="slider", help="本次实验名")
    args = parser.parse_args()

    model = YOLO(resolve(args.weights))
    model.train(
        data=resolve(args.data),
        epochs=args.epochs,
        imgsz=args.imgsz,
        batch=args.batch,
        device=args.device,
        project=resolve(args.project),
        name=args.name,
    )


if __name__ == "__main__":
    main()
