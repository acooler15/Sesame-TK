# /// script
# requires-python = ">=3.9"
# dependencies = ["pillow", "captcha-recognizer", "websocket-client"]
# ///
"""验证码页面本地采集服务（模型训练采集用）。

用法:
    uv run serve.py [--port 8900] [--cdp-port 9222] [--no-browser]

浏览器:
    启动时自动探测 Chrome（找不到则用 Edge）以调试端口拉起并打开采集页
    （普通可见窗口，人工可随时接管操作）；cdp 端口已有调试实例则直接复用。
    拖拽/点击经 CDP Input.dispatchMouseEvent 派发为原生输入（isTrusted=true）；
    页面内 dispatchEvent 合成事件 isTrusted 恒为 false，会被 captcha.js 风险
    引擎标记（moveTruested）导致验证被拒——手动拖能过、自动拖不过即此原因。

功能:
    1. 静态服务 captcha-page/ 目录；
    2. 服务 index.html 时自动注入 collect-auto.js（免控制台粘贴）；
    3. POST /api/sample  — 纯采集：合成截图落盘 data/captured/；
    4. POST /api/predict — 识别验证采集：合成截图 + captcha-recognizer 识别缺口，
       计算拖动距离，样本先落盘 data/captured/review/；
    5. POST /api/outcome — 浏览器上报滑动结果：
       成功 → 归档 data/captured/verified/（写成 *_prelabeled.json，reviewStatus=approved，
              gapSource=auto-verified，可直接进数据集）；
       失败 → 留在 data/captured/review/（标记 slide-failed，走人工预标注+复核流程）；
    6. POST /api/drag — 把拖拽/点击事件序列经 CDP Input.dispatchMouseEvent 派发为
       浏览器原生输入（isTrusted=true）。事件含 dt（距上一事件毫秒），服务端按
       绝对时间对齐控速——SDK 读的是事件真实 timeStamp，节奏必须逐点控制。
"""
from __future__ import annotations

import argparse
import io
import json
import os
import re
import socketserver
import subprocess
import sys
import threading
import time
import urllib.parse
import urllib.request
from http.server import SimpleHTTPRequestHandler
from pathlib import Path

from PIL import Image, ImageDraw

HERE = Path(__file__).resolve().parent
# 采集产物统一落到工程 data/captured/（slider-train/data/captured）
# 子目录：captured/ 根=普通采集，captured/review=识别验证待复核，captured/verified=自动验证通过
ROOT = HERE.parent / "data" / "captured"
DIR_CAPTURE = ROOT
DIR_REVIEW = ROOT / "review"
DIR_VERIFIED = ROOT / "verified"
INJECT_TAG = b'<script src="/collect-auto.js?v={v}"></script></body>'
UA = ("Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 "
      "(KHTML, like Gecko) Chrome/139 Mobile Safari/537.36")

# 与 prelabel_gap.py 保持一致：需从视口坐标换算到截图坐标的 DOM rect 字段
VIEWPORT_RECT_FIELDS = ("slotimg", "refreshButton", "refreshImg", "feedbackButton",
                        "feedbackImg", "handler", "sliderContainer")

_slider = None
_slider_lock = threading.Lock()


def get_slider():
    """懒加载 captcha-recognizer（首次加载 onnx 模型较慢）。"""
    global _slider
    if _slider is None:
        from captcha_recognizer.slider import Slider
        _slider = Slider()
    return _slider


# ---------------- CDP 原生输入通道 ----------------
# 页面内 dispatchEvent 合成事件 isTrusted=false，被 captcha.js 风险引擎检测
# （moveTruested）。CDP Input.dispatchMouseEvent 派发的是浏览器原生输入，
# isTrusted=true，与真人鼠标无异。

_cdp = {"port": None, "http_port": None, "page": None}  # main() 初始化
_cdp_lock = threading.Lock()
PROFILE_DIR = HERE / ".chrome-profile"  # 独立 profile：可与日常 Chrome 并存


def find_browser() -> str | None:
    """探测本机 Chrome/Edge 可执行文件路径（Edge 内核与 CDP 协议同源）。"""

    def from_registry(hive, subkey):
        try:
            import winreg
            with winreg.OpenKey(hive, subkey) as k:
                path, _ = winreg.QueryValueEx(k, None)
                if path and Path(path).exists():
                    return path
        except (OSError, ImportError):
            pass
        return None

    candidates = []
    if os.name == "nt":
        # App Paths 注册表最可靠（覆盖自定义安装位置）
        for exe in ("chrome.exe", "msedge.exe"):
            for hive in (0x80000002, 0x80000001):  # HKLM / HKCU
                path = from_registry(
                    hive, rf"SOFTWARE\Microsoft\Windows\CurrentVersion\App Paths\{exe}")
                if path:
                    return path
        pf = os.environ.get("PROGRAMFILES", r"C:\Program Files")
        pf86 = os.environ.get("PROGRAMFILES(X86)", r"C:\Program Files (x86)")
        lam = os.environ.get("LOCALAPPDATA", "")
        candidates = [
            rf"{pf}\Google\Chrome\Application\chrome.exe",
            rf"{pf86}\Google\Chrome\Application\chrome.exe",
            rf"{lam}\Google\Chrome\Application\chrome.exe",
            rf"{pf}\Microsoft\Edge\Application\msedge.exe",
            rf"{pf86}\Microsoft\Edge\Application\msedge.exe",
        ]
    else:
        candidates = ["/usr/bin/google-chrome", "/usr/bin/chromium",
                      "/usr/bin/microsoft-edge",
                      "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"]
    for c in candidates:
        if Path(c).exists():
            return c
    return None


def _cdp_http(cdp_port: int, path: str) -> dict | list | None:
    try:
        with urllib.request.urlopen(
                f"http://127.0.0.1:{cdp_port}{path}", timeout=2) as r:
            return json.loads(r.read())
    except OSError:
        return None


def cdp_ready(cdp_port: int) -> bool:
    return _cdp_http(cdp_port, "/json/version") is not None


def cdp_page_targets(cdp_port: int, http_port: int) -> list[dict]:
    targets = _cdp_http(cdp_port, "/json/list") or []
    return [t for t in targets
            if t.get("type") == "page" and f":{http_port}/" in t.get("url", "")]


def ensure_browser(cdp_port: int, page_url: str, http_port: int) -> str:
    """保证有一个带调试端口的浏览器并打开采集页。返回描述（launched/reused/opened/none）。"""
    if cdp_ready(cdp_port):
        if cdp_page_targets(cdp_port, http_port):
            return "reused"  # 已有调试实例且采集页已打开
        try:  # 在已有实例中新开采集页标签（Chrome 111+ 需 PUT）
            req = urllib.request.Request(
                f"http://127.0.0.1:{cdp_port}/json/new?"
                + urllib.parse.urlencode({"url": page_url}), method="PUT")
            urllib.request.urlopen(req, timeout=2)
            return "opened"
        except OSError:
            return "reused"

    exe = find_browser()
    if not exe:
        return "none"
    subprocess.Popen(
        [exe,
         f"--remote-debugging-port={cdp_port}",
         f"--user-data-dir={PROFILE_DIR}",
         "--remote-allow-origins=*",  # Chrome 111+ websocket 调试连接有 Origin 校验
         "--no-first-run", "--no-default-browser-check",
         "--window-size=440,920",  # 采集页是 393px 宽的移动端快照
         page_url],
        close_fds=True)
    for _ in range(100):  # 最多等 20s 浏览器起来
        if cdp_ready(cdp_port):
            return "launched"
        time.sleep(0.2)
    return "none"


class CdpPage:
    """单页面 target 的 CDP websocket 控制端。"""

    def __init__(self, cdp_port: int, http_port: int):
        self.cdp_port, self.http_port = cdp_port, http_port
        self._ws = None
        self._seq = 0

    def _ws_url(self) -> str:
        targets = cdp_page_targets(self.cdp_port, self.http_port)
        if not targets:
            raise RuntimeError(
                f"CDP: 找不到采集页 target（:{self.http_port}/ 未打开或未注入）")
        return targets[0]["webSocketDebuggerUrl"]

    def _conn(self):
        if self._ws is None or not getattr(self._ws, "connected", False):
            import websocket
            self._ws = websocket.create_connection(self._ws_url(), timeout=10)
        return self._ws

    def send(self, method: str, params: dict) -> dict:
        self._seq += 1
        ws = self._conn()
        ws.send(json.dumps({"id": self._seq, "method": method, "params": params}))
        while True:  # 跳过事件通知，等回执
            msg = json.loads(ws.recv())
            if msg.get("id") == self._seq:
                if "error" in msg:
                    raise RuntimeError(f"CDP {method}: {msg['error']}")
                return msg.get("result") or {}

    def close(self):
        if self._ws is not None:
            try:
                self._ws.close()
            except OSError:
                pass
            self._ws = None


def api_drag(data: dict) -> dict:
    """按事件序列派发原生输入：[{type: move|down|up, x, y, dt(ms)}]。

    使用 CDP Input.dispatchMouseEvent 派发原生鼠标事件（isTrusted=true）。
    dt 为距上一事件的间隔；服务端按绝对时间对齐 sleep，保证事件 timeStamp
    间隔符合人类节奏。
    """
    events = data.get("events") or []
    if not events:
        return {"ok": True, "dispatched": 0}
    if not _cdp["page"]:
        raise RuntimeError("CDP 通道未启用（serve.py 需带 --cdp-port 启动）")

    with _cdp_lock:
        page: CdpPage = _cdp["page"]
        pressed = False
        t0 = time.perf_counter()
        acc_ms = 0.0
        for ev in events:
            acc_ms += float(ev.get("dt") or 0)
            wait = t0 + acc_ms / 1000 - time.perf_counter()
            if wait > 0:
                time.sleep(wait)
            x, y = float(ev["x"]), float(ev["y"])
            et = ev.get("type", "move")
            if et == "down":
                page.send("Input.dispatchMouseEvent", {
                    "type": "mousePressed", "x": x, "y": y,
                    "button": "left", "buttons": 1, "clickCount": 1})
                pressed = True
            elif et == "up":
                page.send("Input.dispatchMouseEvent", {
                    "type": "mouseReleased", "x": x, "y": y,
                    "button": "left", "buttons": 0, "clickCount": 1})
                pressed = False
            else:
                page.send("Input.dispatchMouseEvent", {
                    "type": "mouseMoved", "x": x, "y": y,
                    "button": "none", "buttons": 1 if pressed else 0})
    return {"ok": True, "dispatched": len(events)}


def api_reload(data: dict) -> dict:
    """重载采集页（经 CDP Page.reload，ignoreCache=True）。

    供采集器在验证码会话过期（等不到新图/找不到刷新按钮）时自动调用，
    实现无人值守：重载后由 collect-auto.js 靠 localStorage 标志自动续采。
    """
    if not _cdp["page"]:
        raise RuntimeError("CDP 通道未启用（serve.py 需带 --cdp-port 启动）")
    with _cdp_lock:
        page: CdpPage = _cdp["page"]
        page.send("Page.reload", {"ignoreCache": True})
    return {"ok": True, "reloaded": True}


def api_status(data: dict) -> dict:
    """返回累计已采集样本总数（扫描 data/captured/ 目录，落盘即权威）。"""
    return {"ok": True, "collected": total_collected()}


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


def ensure_dirs() -> None:
    for d in (DIR_CAPTURE, DIR_REVIEW, DIR_VERIFIED):
        d.mkdir(parents=True, exist_ok=True)


def next_index() -> int:
    """跨 data/captured/ review/ verified/ 的全局自增编号，防止跨目录重名。"""
    ensure_dirs()
    nums = []
    for d in (DIR_CAPTURE, DIR_REVIEW, DIR_VERIFIED):
        for p in d.glob("*.json"):
            m = re.match(r"^(\d+)(?:_prelabeled)?\.json$", p.name)
            if m:
                nums.append(int(m.group(1)))
    return max(nums, default=0) + 1


def total_collected() -> int:
    """累计已采集样本总数 = data/captured/ 根目录下普通样本 json 数。

    只统计根目录（DIR_CAPTURE）下的 *.json，排除 _prelabeled 后缀与
    review/verified 子目录（那些是后续流程归档，不属于采集计数）。
    """
    ensure_dirs()
    n = 0
    for p in DIR_CAPTURE.glob("*.json"):
        if re.match(r"^\d+\.json$", p.name):
            n += 1
    return n


def compose_png(rects: dict, urls: dict) -> bytes:
    """按 puzzleContainer 原点合成截图：拼图背景 + 滑块块 + 刷新图标 + 滑轨/手柄近似。"""
    pc = rects.get("puzzleContainer")
    if not pc:
        raise ValueError("缺少 puzzleContainer")
    ox, oy = pc["x"], pc["y"]
    canvas = Image.new("RGB", (round(pc["width"]), round(pc["height"])), "#f5f5f5")

    def paste(url, r):
        if not url or not r:
            return
        req = urllib.request.Request(url, headers={"User-Agent": UA})
        raw = urllib.request.urlopen(req, timeout=15).read()
        im = Image.open(io.BytesIO(raw)).convert("RGBA")
        im = im.resize((max(1, round(r["width"])), max(1, round(r["height"]))))
        canvas.paste(im, (round(r["x"] - ox), round(r["y"] - oy)), im)

    paste(urls.get("backimg"), rects.get("backimg"))
    paste(urls.get("slotimg"), rects.get("slotimg"))
    paste(urls.get("refreshIcon"), rects.get("refreshButton") or rects.get("refreshImg"))
    paste(urls.get("feedbackIcon"), rects.get("feedbackButton") or rects.get("feedbackImg"))

    draw = ImageDraw.Draw(canvas)
    sc = rects.get("sliderContainer")
    if sc:
        x1, y1 = round(sc["x"] - ox), round(sc["y"] - oy)
        draw.rounded_rectangle([x1, y1, x1 + round(sc["width"]), y1 + round(sc["height"])],
                               radius=6, fill="#f0f0f0", outline="#e0e0e0", width=1)
    hd = rects.get("handler")
    if hd:
        x1, y1 = round(hd["x"] - ox), round(hd["y"] - oy)
        draw.rounded_rectangle([x1, y1, x1 + round(hd["width"]), y1 + round(hd["height"])],
                               radius=8, fill="#1677ff")

    buf = io.BytesIO()
    canvas.save(buf, "PNG")
    return buf.getvalue()


def api_predict(data: dict) -> dict:
    """识别验证采集：截图 → 识别 → 拖动距离；样本暂存 review/。"""
    rects = data.get("rects") or {}
    urls = data.get("imageUrls") or {}
    png_bytes = compose_png(rects, urls)
    w, h = Image.open(io.BytesIO(png_bytes)).size
    ensure_dirs()
    name = f"{next_index():03d}"
    png_path = DIR_REVIEW / f"{name}.png"
    png_path.write_bytes(png_bytes)

    box, conf, err = None, None, None
    try:
        with _slider_lock:
            box, conf = get_slider().identify(source=str(png_path))
    except Exception as e:  # noqa: BLE001 — 识别器异常降级为不可预测样本
        err = str(e)

    drag = None
    predicted = bool(box and conf is not None)
    if predicted:
        pc = rects.get("puzzleContainer") or {}
        hd = rects.get("handler") or {}
        # 拖动距离 = 缺口中心(截图系) - 手柄中心(截图系)：
        # 被拖动的是 handler（滑轨上的滑块），slotimg 是固定在缺口上的视觉阴影，
        # 二者 cx 差几像素（不同尺寸/位置），用 slot 算的 drag 会偏 5-10px 直接判死。
        # 视口 px 与合成截图 1:1，手柄 cx 减容器原点即截图系
        gap_cx = (box[0] + box[2]) / 2
        hd_cx_shot = hd.get("cx", hd.get("x", 0) + hd.get("width", 0) / 2) - pc.get("x", 0)
        drag = gap_cx - hd_cx_shot
        if not (5 <= drag <= pc.get("width", 500)):
            predicted, drag = False, None  # 异常框：不拖，样本留给人工

    jsondata = dict(rects)
    jsondata["imageWidth"], jsondata["imageHeight"] = w, h
    if predicted:
        jsondata["predictedGap"] = {
            "x": float(box[0]), "y": float(box[1]),
            "width": float(box[2] - box[0]), "height": float(box[3] - box[1]),
        }
        jsondata["gapConfidence"] = float(conf)
        jsondata["dragDistance"] = float(drag)
    if err:
        jsondata["predictError"] = err
    (DIR_REVIEW / f"{name}.json").write_text(
        json.dumps(jsondata, ensure_ascii=False, indent=2), encoding="utf-8")

    print(f"[predict] {name}: predicted={predicted}"
          + (f" conf={conf:.2f} drag={drag:.0f}px" if predicted else ""), flush=True)
    return {"ok": True, "name": name, "predicted": predicted,
            "box": box, "confidence": conf, "dragDistance": drag}


def api_outcome(data: dict) -> dict:
    """按滑动结果归档：成功 → verified/（可进数据集）；失败 → 留 review/。"""
    name = data.get("name")
    success = bool(data.get("success"))
    src_png = DIR_REVIEW / f"{name}.png"
    src_json = DIR_REVIEW / f"{name}.json"
    if not (src_png.exists() and src_json.exists()):
        return {"ok": False, "error": f"样本不存在: {name}"}

    j = json.loads(src_json.read_text(encoding="utf-8"))
    if not success:
        j["verifyStatus"] = "slide-failed"
        if data.get("reason"):
            j["slideFailureReason"] = data["reason"]
        src_json.write_text(json.dumps(j, ensure_ascii=False, indent=2), encoding="utf-8")
        print(f"[outcome] {name}: slide-failed ({data.get('reason')}) → review/", flush=True)
        return {"ok": True, "filed": "review"}

    # 成功：视口坐标 → 截图坐标，predictedGap → gapimg，直接写成已复核 schema
    origin = j.get("puzzleContainer") or j.get("backimg") or {}
    ox, oy = origin.get("x", 0.0), origin.get("y", 0.0)
    for field in VIEWPORT_RECT_FIELDS:
        if field in j:
            j[field] = shift_rect(j[field], ox, oy)
    gap = j.pop("predictedGap", None)
    if gap:
        j["gapimg"] = gap
    j["gapSource"] = "auto-verified"
    j["reviewStatus"] = "approved"

    ensure_dirs()
    # png 命名与 prelabeled stem 一致，collect_to_dataset 后续复制免重命名
    (DIR_VERIFIED / f"{name}_prelabeled.png").write_bytes(src_png.read_bytes())
    (DIR_VERIFIED / f"{name}_prelabeled.json").write_text(
        json.dumps(j, ensure_ascii=False, indent=2), encoding="utf-8")
    src_png.unlink()
    src_json.unlink()
    print(f"[outcome] {name}: verified → verified/", flush=True)
    return {"ok": True, "filed": "verified"}


class CollectHandler(SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=str(HERE), **kwargs)

    def end_headers(self):
        # 禁用缓存：确保 collect-auto.js / index.html 改动后浏览器总是拉最新版
        self.send_header("Cache-Control", "no-store")
        super().end_headers()

    def do_GET(self):
        if self.path in ("/", "/index.html"):
            html = (HERE / "index.html").read_bytes()
            if b"</body>" in html:
                tag = INJECT_TAG.replace(b"{v}", str(int(time.time())).encode())
                html = html.replace(b"</body>", tag, 1)
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(html)))
            self.end_headers()
            self.wfile.write(html)
            return
        super().do_GET()

    def do_POST(self):
        if self.path == "/api/sample":
            handler = self._api_sample
        elif self.path == "/api/predict":
            handler = api_predict
        elif self.path == "/api/outcome":
            handler = api_outcome
        elif self.path == "/api/drag":
            handler = api_drag
        elif self.path == "/api/reload":
            handler = api_reload
        elif self.path == "/api/status":
            handler = api_status
        else:
            self.send_error(404)
            return
        try:
            length = int(self.headers.get("Content-Length", 0))
            data = json.loads(self.rfile.read(length) or b"{}")
            self._json(handler(data))
        except Exception as e:  # noqa: BLE001 — 失败原因原样回传浏览器面板
            self._json({"ok": False, "error": str(e)}, status=500)

    def _api_sample(self, data: dict) -> dict:
        rects = data.get("rects") or {}
        png = compose_png(rects, data.get("imageUrls") or {})
        name = f"{next_index():03d}"
        (DIR_CAPTURE / f"{name}.png").write_bytes(png)
        (DIR_CAPTURE / f"{name}.json").write_text(
            json.dumps(rects, ensure_ascii=False, indent=2), encoding="utf-8")
        collected = total_collected()
        print(f"[collect] {name}.png 已保存 (累计 {collected} 张)", flush=True)
        return {"ok": True, "name": name, "collected": collected}

    def _json(self, obj, status=200):
        body = json.dumps(obj).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, fmt, *args):
        print("[serve]", fmt % args, flush=True)


class ThreadingTCPServer(socketserver.ThreadingTCPServer):
    daemon_threads = True


def main() -> int:
    parser = argparse.ArgumentParser(description="验证码页面采集服务")
    parser.add_argument("--port", type=int, default=8900, help="监听端口 (默认 8900)")
    parser.add_argument("--host", default="127.0.0.1", help="监听地址 (默认 127.0.0.1)")
    parser.add_argument("--cdp-port", type=int, default=9222,
                        help="浏览器调试端口 (默认 9222，拖拽/点击经 CDP 原生输入)")
    parser.add_argument("--no-browser", action="store_true",
                        help="不自动拉起浏览器（自行打开采集页）")
    args = parser.parse_args()

    ensure_dirs()
    _cdp["port"] = args.cdp_port
    _cdp["http_port"] = args.port
    _cdp["page"] = CdpPage(args.cdp_port, args.port)

    def _launch_browser():
        try:
            page_url = f"http://127.0.0.1:{args.port}/index.html"
            state = ensure_browser(args.cdp_port, page_url, args.port)
            if state == "launched":
                print(f"[serve] 已自动拉起浏览器打开 {page_url}（CDP :{args.cdp_port}）", flush=True)
            elif state in ("reused", "opened"):
                print(f"[serve] 复用 CDP :{args.cdp_port} 上已打开的采集页", flush=True)
            else:
                print(f"[serve] 未找到 Chrome/Edge，请手动打开 {page_url}", flush=True)
        except Exception as e:  # noqa: BLE001 — 拉起失败不阻塞采集服务
            print(f"[serve] 浏览器自动拉起失败: {e}", flush=True)

    with ThreadingTCPServer((args.host, args.port), CollectHandler) as httpd:
        print(f"[serve] 采集服务已启动: http://{args.host}:{args.port}/index.html", flush=True)
        print(f"[serve] 采集产物目录: {ROOT}", flush=True)
        if not args.no_browser:
            threading.Timer(0.5, _launch_browser).start()  # 等 server 监听后再开页
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\n[serve] 已停止", flush=True)
    return 0


if __name__ == "__main__":
    sys.exit(main())
