"""人工复核 Web 服务 — 可视化核对/修正缺口(gap)预标注。

用法:
    uv run review_server.py                 # 默认目录 data/labeled/、端口 8901
    uv run review_server.py data/labeled/ --port 8902

浏览器打开 http://localhost:8901 :
    - 红框 gap（captcha-recognizer 预标注）、绿框 block、蓝框 refresh
    - 拖动红框整体移动；空白处拖拽重画 gap
    - 快捷键: A=通过并下一张, R=剔除, ←/→=翻页

复核结果写回 <name>_prelabeled.json（reviewed=true + reviewStatus=approved/rejected
+ 修正后的 gapimg），collect_to_dataset.py 默认只消费 approved 样本（--all 才含未复核）。

仅依赖 Python 标准库。
"""
from __future__ import annotations

import argparse
import json
import pathlib
import re
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import unquote, urlparse

# review.html 统一放在工程 web/ 下（../web/review.html）
HTML_FILE = pathlib.Path(__file__).resolve().parent.parent / "web" / "review.html"
SAFE_NAME = re.compile(r"^[\w\-]+$")  # 防路径穿越：样本名只允许字母数字下划线连字符


class Store:
    """采集目录的样本索引与读写。"""

    def __init__(self, root: pathlib.Path):
        self.root = root.resolve()
        if not self.root.is_dir():
            raise SystemExit(f"目录不存在: {self.root}")

    def png_path(self, name: str) -> pathlib.Path:
        return self.root / f"{name}.png"

    def json_path(self, name: str) -> pathlib.Path:
        return self.root / f"{name}_prelabeled.json"

    def list_samples(self) -> list[dict]:
        samples: dict[str, dict] = {}
        for png in sorted(self.root.glob("*.png")):
            if png.name.startswith("review_"):
                continue
            samples[png.stem] = {
                "name": png.stem,
                "prelabeled": False,
                "reviewed": False,
                "reviewStatus": None,
                "gapConfidence": None,
            }
        for jp in sorted(self.root.glob("*_prelabeled.json")):
            stem = jp.name[: -len("_prelabeled.json")]
            entry = samples.get(stem)
            if entry is None:
                continue
            try:
                data = json.loads(jp.read_text(encoding="utf-8"))
            except Exception:
                continue
            entry.update(
                prelabeled=True,
                reviewed=bool(data.get("reviewed")),
                reviewStatus=data.get("reviewStatus"),
                gapConfidence=data.get("gapConfidence"),
            )
        return sorted(samples.values(), key=lambda s: s["name"])

    def load_sample(self, name: str) -> dict | None:
        jp = self.json_path(name)
        if not jp.exists():
            return None
        return json.loads(jp.read_text(encoding="utf-8"))

    def save_review(self, name: str, payload: dict) -> dict:
        jp = self.json_path(name)
        data = json.loads(jp.read_text(encoding="utf-8")) if jp.exists() else {}
        if payload.get("gapimg"):
            data["gapimg"] = payload["gapimg"]
            data["gapConfidence"] = payload.get("gapConfidence", data.get("gapConfidence"))
            data["gapSource"] = payload.get("gapSource", data.get("gapSource", "auto"))
        if payload.get("slotimg"):
            data["slotimg"] = payload["slotimg"]
        if payload.get("refreshButton"):
            data["refreshButton"] = payload["refreshButton"]
        if payload.get("feedbackButton"):
            data["feedbackButton"] = payload["feedbackButton"]
        data["reviewed"] = True
        data["reviewStatus"] = payload.get("reviewStatus", "approved")
        data["reviewedAt"] = payload.get("reviewedAt")
        jp.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
        return data


class Handler(BaseHTTPRequestHandler):
    store: Store  # 由 main 注入

    def log_message(self, fmt, *args):  # 安静模式
        pass

    # ---- 响应工具 ----
    def _send(self, code: int, body: bytes, ctype: str) -> None:
        self.send_response(code)
        self.send_header("Content-Type", ctype)
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(body)

    def _json(self, obj, code: int = 200) -> None:
        self._send(code, json.dumps(obj, ensure_ascii=False).encode("utf-8"),
                   "application/json; charset=utf-8")

    def _check_name(self, name: str) -> str | None:
        if not SAFE_NAME.match(name):
            self._json({"error": "非法样本名"}, 400)
            return None
        return name

    # ---- GET ----
    def do_GET(self) -> None:
        path = urlparse(self.path).path
        if path in ("/", "/index.html"):
            self._send(200, HTML_FILE.read_bytes(), "text/html; charset=utf-8")
        elif path == "/api/list":
            self._json(self.store.list_samples())
        elif path.startswith("/image/"):
            name = unquote(path[len("/image/"):]).removesuffix(".png")
            if self._check_name(name) is None:
                return
            fp = self.store.png_path(name)
            if fp.exists():
                self._send(200, fp.read_bytes(), "image/png")
            else:
                self._json({"error": "图片不存在"}, 404)
        elif path.startswith("/api/sample/"):
            name = unquote(path[len("/api/sample/"):])
            if self._check_name(name) is None:
                return
            data = self.store.load_sample(name)
            self._json(data if data is not None else {}, 200 if data else 404)
        else:
            self._json({"error": "not found"}, 404)

    # ---- POST ----
    def do_POST(self) -> None:
        path = urlparse(self.path).path
        if not path.startswith("/api/save/"):
            self._json({"error": "not found"}, 404)
            return
        name = unquote(path[len("/api/save/"):])
        if self._check_name(name) is None:
            return
        length = int(self.headers.get("Content-Length", 0))
        try:
            payload = json.loads(self.rfile.read(length) or b"{}")
        except json.JSONDecodeError:
            self._json({"error": "请求体不是合法 JSON"}, 400)
            return
        if payload.get("reviewStatus") not in ("approved", "rejected"):
            self._json({"error": "reviewStatus 必须为 approved/rejected"}, 400)
            return
        data = self.store.save_review(name, payload)
        self._json({"ok": True, "reviewStatus": data.get("reviewStatus")})


def main() -> None:
    ROOT = pathlib.Path(__file__).resolve().parent.parent
    parser = argparse.ArgumentParser(description="滑块样本人工复核 Web 服务")
    parser.add_argument("dir", nargs="?",
                        help="标注工作目录（默认 data/labeled/）")
    parser.add_argument("--port", type=int, default=8901, help="监听端口，默认 8901")
    parser.add_argument("--host", default="127.0.0.1", help="监听地址，默认 127.0.0.1")
    args = parser.parse_args()

    work_dir = pathlib.Path(args.dir) if args.dir else ROOT / "data" / "labeled"
    if not work_dir.is_absolute():
        work_dir = ROOT / work_dir
    Handler.store = Store(work_dir)
    samples = Handler.store.list_samples()
    pending = sum(1 for s in samples if s["prelabeled"] and not s["reviewed"])
    print(f"样本目录: {Handler.store.root}")
    print(f"样本总数: {len(samples)}，待复核: {pending}，未预标注: "
          f"{sum(1 for s in samples if not s['prelabeled'])}")
    print(f"复核界面: http://{args.host}:{args.port}/  (Ctrl+C 退出)")

    server = ThreadingHTTPServer((args.host, args.port), Handler)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\n已退出")


if __name__ == "__main__":
    main()
