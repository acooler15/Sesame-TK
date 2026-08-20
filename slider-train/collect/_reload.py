# /// script
# requires-python = ">=3.9"
# dependencies = ["websocket-client"]
# ///
"""reload 采集页以重新注入 collect-auto.js。"""
import json, urllib.request, websocket
targets = json.loads(urllib.request.urlopen("http://127.0.0.1:9222/json/list", timeout=3).read())
t = next(x for x in targets if x.get("type")=="page" and ":8900/" in x.get("url",""))
ws = websocket.create_connection(t["webSocketDebuggerUrl"], timeout=10)
ws.send(json.dumps({"id":1,"method":"Page.reload","params":{"ignoreCache":True}}))
print("reload 已发送")
ws.close()
