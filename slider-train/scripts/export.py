"""导出训练好的检测模型为 TFLite（供 Android 端 SliderTFLite.kt 使用）。

用法:
    uv run export.py runs/slider/weights/best.pt

说明:
    - 不启用 int8 量化（端侧使用 FLOAT32 TensorBuffer）。
      n 档 detect float32 约 6MB，已远小于旧 seg 模型（38.6MB）。
    - 导出后自动加载 tflite 打印输入/输出 shape，与 ANDROID_INTEGRATION.md
      中的预期规格核对；不符则需同步修改端侧解析代码。
"""
from __future__ import annotations

import argparse
import pathlib

from ultralytics import YOLO


def print_shapes(tflite_path: str) -> None:
    try:
        from tflite_runtime.interpreter import Interpreter  # type: ignore
    except ImportError:
        try:
            from tensorflow.lite.python.interpreter import Interpreter  # type: ignore
        except ImportError:
            print("[skip] 未找到 tflite 解释器，跳过 shape 校验（可忽略，训练不受影响）")
            return

    interp = Interpreter(model_path=tflite_path)
    interp.allocate_tensors()

    def fmt(shape, qtype):
        dims = ", ".join(str(d) for d in shape)
        return f"[{dims}] type={qtype}"

    for i, d in enumerate(interp.get_input_details()):
        print(f"  input[{i}]:  {fmt(d['shape'], d['dtype'])}")
    for i, d in enumerate(interp.get_output_details()):
        print(f"  output[{i}]: {fmt(d['shape'], d['dtype'])}")


def main() -> None:
    parser = argparse.ArgumentParser(description="导出 YOLO 检测模型为 TFLite")
    parser.add_argument("weights", help="训练好的 .pt 权重路径（相对工程根，如 data/runs/slider/weights/best.pt）")
    parser.add_argument("--imgsz", type=int, default=640, help="输入尺寸")
    args = parser.parse_args()

    # 工程根 = scripts/ 的上一级；相对路径解析到工程根
    ROOT = pathlib.Path(__file__).resolve().parent.parent
    wpath = pathlib.Path(args.weights)
    weights = str(wpath if wpath.is_absolute() else ROOT / wpath)

    model = YOLO(weights)
    out = model.export(format="tflite", imgsz=args.imgsz)

    print("\n[shape 校验]")
    print_shapes(str(out))

    print("\n导出完成:", out)
    print("替换到 Android 工程: app/src/main/ml/slider.tflite")
    print("注意: 若 Android Studio 重新生成的 Slider 类接口变化，端侧初始化代码需同步。")
    print("端侧适配步骤详见: ANDROID_INTEGRATION.md")


if __name__ == "__main__":
    main()
