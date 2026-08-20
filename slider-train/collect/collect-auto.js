// 滑块验证码自动采集器 — 由 serve.py 服务页面时自动注入，无需控制台粘贴。
//
// 唯一模式「采集」：等渲染 → 上报 /api/sample（服务端合成截图落盘 captures/）
//                   → 点右上角刷新按钮换图 → 循环。
// 采集后用 prelabel_gap.py 预标注 → review_server.py 人工复核 → 训练。
// 注意：右上角有两个同 class 图标，左侧是「问题反馈」，最右侧才是刷新。
(function () {
  'use strict';
  if (window.__captchaAutoCollect) return;

  const $ = (s) => document.querySelector(s);
  const rect = (el) => {
    if (!el) return null;
    const r = el.getBoundingClientRect();
    return { x: r.left, y: r.top, width: r.width, height: r.height,
             cx: r.left + r.width / 2, cy: r.top + r.height / 2 };
  };
  const bgUrl = (el) => {
    if (!el) return null;
    const m = getComputedStyle(el).backgroundImage.match(/url\(["']?(.*?)["']?\)/);
    return m ? m[1] : null;
  };
  const imgUrl = (el) => (el && el.tagName === 'IMG' ? (el.currentSrc || el.src) : bgUrl(el));

  const SEL = {
    pc: '.jshield-captcha-puzzle-container.puzzle',
    back: '.jshield-captcha-puzzle-container .backimg.puzzle',
    slot: '.jshield-captcha-puzzle-container .slotimg.puzzle',
    slider: '.jshield-captcha-puzzle-container .slider-container.puzzle',
    handler: '.dv_drag_verify.baseslider .dv_handler.baseslider',
    refresh: '.jshield-captcha-puzzle-container .refresh-button-img.puzzle',
    tips: '.verify-tip, .verify-tip-error',
  };

  const state = { running: false, count: 0, fail: 0 };
  const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

  function snapshot() {
    const pc = $(SEL.pc), back = $(SEL.back);
    if (!pc || !back || !imgUrl(back)) return null;
    return { pc, back, slot: $(SEL.slot) };
  }

  async function waitRender(timeout, lastBackUrl) {
    const deadline = Date.now() + timeout;
    for (;;) {
      const s = snapshot();
      if (s && imgUrl(s.back) !== lastBackUrl) return s;
      if (Date.now() > deadline || !state.running) return null;
      await sleep(250);
    }
  }

  function refreshEl() {
    const cands = [...document.querySelectorAll(SEL.refresh)]
      .filter((el) => el.getBoundingClientRect().width > 10);
    if (!cands.length) return null;
    return cands.reduce((a, b) =>
      (a.getBoundingClientRect().x >= b.getBoundingClientRect().x ? a : b));
  }

  function collectPayload(s) {
    const rEl = refreshEl();
    return {
      rects: {
        capturedAt: Date.now(),
        puzzleContainer: rect(s.pc),
        backimg: rect(s.back),
        slotimg: rect(s.slot),
        sliderContainer: rect($(SEL.slider)),
        handler: rect($(SEL.handler)),
        refreshButton: rect(rEl),
        refreshImg: rect(rEl),
        tips: [...document.querySelectorAll(SEL.tips)]
          .map((el) => ({ text: (el.textContent || '').trim(), rect: rect(el) })),
      },
      imageUrls: {
        backimg: imgUrl(s.back),
        slotimg: imgUrl(s.slot),
        refreshIcon: imgUrl(rEl),
      },
    };
  }

  async function postJson(url, payload) {
    const res = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
    const j = await res.json().catch(() => ({}));
    if (!res.ok || !j.ok) throw new Error(j.error || ('HTTP ' + res.status));
    return j;
  }

  // CDP 原生输入点击（经 serve.py → Input.dispatchMouseEvent，isTrusted=true）。
  async function clickRefresh() {
    const el = refreshEl();
    if (!el) return false;
    const r = el.getBoundingClientRect();
    const x = r.x + r.width / 2, y = r.y + r.height / 2;
    try {
      await postJson('/api/drag', { events: [
        { type: 'down', dt: 0, x, y },
        { type: 'up', dt: 50 + Math.random() * 80,
          x: x + (Math.random() - 0.5) * 2, y: y + (Math.random() - 0.5) * 2 },
      ]});
      return true;
    } catch (e) {
      setStatus('CDP 点击失败: ' + e.message);
      return false;
    }
  }

  // ---------- 采集循环 ----------
  async function captureLoop() {
    let lastBackUrl = null;
    while (state.running) {
      const s = await waitRender(15000, lastBackUrl);
      if (!s) {
        if (++state.fail >= 3) return stop('连续 3 次等不到新图，已停止（会话可能过期，刷新页面重开）');
        continue;
      }
      lastBackUrl = imgUrl(s.back);
      try {
        const j = await postJson('/api/sample', collectPayload(s));
        state.count++; state.fail = 0;
        setCounts();
        setStatus(`${j.name}.png 已保存`);
      } catch (e) {
        if (++state.fail >= 3) return stop('保存连续失败 x3: ' + e.message);
        setStatus('保存失败: ' + e.message);
      }
      await sleep(900 + Math.random() * 600);
      if (!state.running) break;
      if (!await clickRefresh() && ++state.fail >= 3) return stop('找不到刷新按钮，已停止');
    }
  }

  // ---------- 悬浮控制面板 ----------
  const setStatus = (msg) => {
    const el = document.getElementById('__cac_status');
    if (el) el.textContent = msg;
  };
  const setCounts = () => {
    const el = document.getElementById('__cac_count');
    if (!el) return;
    el.textContent = state.count ? `已采集 ${state.count}` : '';
  };
  const setBtn = () => {
    const cap = document.getElementById('__cac_btn_cap');
    const stp = document.getElementById('__cac_btn_stop');
    if (cap) cap.disabled = !!state.running;
    if (stp) stp.disabled = !state.running;
  };

  function buildUi() {
    if (document.getElementById('__cac_panel')) return;
    const p = document.createElement('div');
    p.id = '__cac_panel';
    p.style.cssText = [
      'position:fixed', 'right:12px', 'bottom:12px', 'z-index:99999',
      'background:#fff', 'border:1px solid #ddd', 'border-radius:8px',
      'padding:8px 10px', 'font:12px/1.6 monospace',
      'box-shadow:0 2px 8px rgba(0,0,0,.15)',
      'display:flex', 'gap:8px', 'align-items:center',
    ].join(';');
    p.innerHTML = '<span id="__cac_status">等待验证码渲染…</span>'
      + '<span id="__cac_count"></span>'
      + '<button id="__cac_btn_cap">采集</button>'
      + '<button id="__cac_btn_stop">停止</button>';
    const btnCss = 'padding:2px 10px;font:12px monospace;cursor:pointer';
    p.querySelector('#__cac_btn_cap').style.cssText = btnCss;
    p.querySelector('#__cac_btn_stop').style.cssText = btnCss;
    p.querySelector('#__cac_btn_cap').onclick = () => start();
    p.querySelector('#__cac_btn_stop').onclick = () => stop('已手动停止');
    (document.body || document.documentElement).appendChild(p);
    setBtn();
  }

  function start() {
    if (state.running) return;
    state.running = true; state.fail = 0; state.count = 0;
    setBtn(); setCounts();
    setStatus('采集中…');
    captureLoop();
  }

  function stop(msg) {
    state.running = false;
    setBtn(); setCounts(); setStatus(msg || '已停止');
  }

  function boot() {
    buildUi();
    const t = setInterval(() => {
      if (snapshot()) {
        clearInterval(t);
        if (!state.running) setStatus('验证码已就绪');
      }
    }, 500);
    setTimeout(() => clearInterval(t), 60000);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', boot);
  } else {
    boot();
  }

  window.__captchaAutoCollect = { start, stop, state };
})();
