package com.example.dangbun.ui.webview.fixes.addplace

import android.webkit.WebView

internal object PlaceMake3TopInsetFix {

    internal fun inject(view: WebView) {
        view.evaluateJavascript(provideJs(), null)
    }

    internal fun provideJs(): String {
        return """
(function() {
  try {
    // =========================================================
    // DB_PM3_FIX_V26 (placemake3)
    // - JS SyntaxError 방지(오직 JS만)
    // - placeName은 "place 관련 키" 우선으로 추출(유저명 오검출 최소화)
    // - 타이틀 DOM을 직접 교체(가능하면 overlay 대신 inline)
    // - topGap 스페이서 유지
    // =========================================================
    var TAG = '[DB_PM3_FIX_V26]';
    var INST_KEY = '__db_fix_pm3_v26_installed__:' + (location.pathname || '');
    if (window[INST_KEY]) return;
    window[INST_KEY] = true;

    function log() {
      try { console.log.apply(console, [TAG].concat([].slice.call(arguments))); } catch(e) {}
    }

    function isPlaceMake3() {
      var p = (location.pathname || '').toLowerCase();
      return p.indexOf('placemake3') >= 0;
    }
    if (!isPlaceMake3()) return;

    // ---------- utils ----------
    function escapeHtml(s) {
      return String(s || '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
    }

    function sanitizeName(raw) {
      var n = (raw == null) ? '' : String(raw);
      n = n.replace(/\s+/g, ' ').trim();
      n = n.replace(/^다음\s*/i, '').trim();
      n = n.replace(/^next\s*/i, '').trim();
      n = n.replace(/생성\s*완료!?$/g, '').trim();
      if (n.length > 18) n = n.slice(0, 18).trim();
      return n;
    }

    function isGoodName(v) {
      v = sanitizeName(v);
      if (!v) return false;
      if (v.length > 18) return false;
      if (/[^ \u3131-\u318E\uAC00-\uD7A3A-Za-z0-9]/.test(v)) return false;
      if (/^(생성|완료|플레이스|place)$/i.test(v)) return false;
      return true;
    }

    function getCookie(name) {
      try {
        var m = document.cookie.match(
          new RegExp('(?:^|; )' + name.replace(/([.$?*|{}()\[\]\\\/\+^])/g, '\\$1') + '=([^;]*)')
        );
        return m ? decodeURIComponent(m[1]) : null;
      } catch(e) { return null; }
    }

    // ✅ "placeName"을 유저 name보다 우선해서 뽑는 extractor
    function extractPlaceNameFromAny(raw) {
      if (raw == null) return null;

      var s = String(raw).trim();
      if (isGoodName(s)) return sanitizeName(s);

      if (s && (s[0] === '{' || s[0] === '[')) {
        try {
          var obj = JSON.parse(s);

          function deepFind(x, keyHint) {
            if (x == null) return null;

            if (typeof x === 'string') {
              return isGoodName(x) ? sanitizeName(x) : null;
            }

            if (Array.isArray(x)) {
              for (var i=0;i<x.length;i++) {
                var r = deepFind(x[i], keyHint);
                if (r) return r;
              }
              return null;
            }

            if (typeof x === 'object') {
              // 1) key에 place가 들어간 것 최우선
              for (var p in x) {
                if (!Object.prototype.hasOwnProperty.call(x, p)) continue;
                if (/place/i.test(p)) {
                  var r0 = deepFind(x[p], p);
                  if (r0) return r0;
                }
              }

              // 2) place 관련 대표 키
              var keys = [
                'placeName','place_name','placename','place',
                'placeTitle','place_title','placeLabel','place_label'
              ];
              for (var k=0;k<keys.length;k++) {
                var kk = keys[k];
                if (x[kk] != null) {
                  var r1 = deepFind(x[kk], kk);
                  if (r1) return r1;
                }
              }

              // 3) title/label/value/text (name은 마지막)
              var keys2 = ['title','label','value','text'];
              for (var k2=0;k2<keys2.length;k2++) {
                var kk2 = keys2[k2];
                if (x[kk2] != null) {
                  var r2 = deepFind(x[kk2], kk2);
                  if (r2) return r2;
                }
              }

              // 4) 마지막 fallback: 전체 순회 (유저/토큰 계열 키는 스킵)
              for (var p2 in x) {
                if (!Object.prototype.hasOwnProperty.call(x, p2)) continue;
                if (/user|nick|profile|token|auth|access|refresh|member/i.test(p2)) continue;
                var r3 = deepFind(x[p2], p2);
                if (r3) return r3;
              }

              // 5) 정말 없으면 name 허용(단, 문자열 1개만 있을 때)
              var only = null, cnt = 0;
              for (var p3 in x) {
                if (!Object.prototype.hasOwnProperty.call(x, p3)) continue;
                if (typeof x[p3] === 'string' && isGoodName(x[p3])) {
                  cnt++; only = sanitizeName(x[p3]);
                }
              }
              if (cnt === 1) return only;

              return null;
            }

            return null;
          }

          var found = deepFind(obj, '');
          if (found) return found;
        } catch(e) {}
      }

      // 문자열 패턴에서 placeName 우선
      try {
        var m1 = s.match(/(?:placeName|place_name|placename|place)\s*["']?\s*[:=]\s*["']([^"']{1,30})["']/i);
        if (m1 && m1[1] && isGoodName(m1[1])) return sanitizeName(m1[1]);
      } catch(e) {}

      return null;
    }

    // ✅ 기존 호출 호환을 위해 alias 제공
    function extractNameFromAny(raw) {
      return extractPlaceNameFromAny(raw);
    }

    // ---------- style ----------
    var STYLE_ID = '__db_pm3_fix_v26_css__';
    function ensureStyle() {
      var style = document.getElementById(STYLE_ID);
      if (!style) {
        style = document.createElement('style');
        style.id = STYLE_ID;
        document.head.appendChild(style);
      }
      style.textContent = `
        .db-pm-title, .db-pm-title * {
          text-decoration: none !important;
          border-bottom: none !important;
          box-shadow: none !important;
          -webkit-text-decoration: none !important;
          text-decoration-line: none !important;
        }

        .db-place-name {
          position: relative;
          display: inline-block;
          font-weight: 800;
        }

        .db-clip-on .db-place-name {
          background: linear-gradient(90deg, #7C4DFF 0%, #A95CFF 45%, #6A84F4 100%) !important;
          -webkit-background-clip: text !important;
          background-clip: text !important;
          -webkit-text-fill-color: transparent !important;
          color: transparent !important;
        }

        .db-clip-off .db-place-name { color: #7C4DFF !important; }

        .db-place-name::after {
          display: none !important;
          content: none !important;
        }

        .db-top-spacer { width: 100%; flex: 0 0 auto; }

        .db-pm3-title-overlay {
          position: fixed;
          left: 0;
          right: 0;
          top: 120px;
          display: flex;
          justify-content: center;
          opacity: 1;
          visibility: visible;
          pointer-events: none;
          z-index: 2147483647;
        }

        .db-pm3-title-overlay .db-pm3-title {
          font-size: 20px;
          font-weight: 800;
          line-height: 1.2;
          text-align: center;
          color: #111;
        }
      `;
    }
    ensureStyle();

    // clip 지원 감지
    var SUPPORTS_CLIP = false;
    try {
      SUPPORTS_CLIP = !!(window.CSS && CSS.supports && (
        CSS.supports('-webkit-background-clip', 'text') ||
        CSS.supports('background-clip', 'text')
      ));
    } catch(e) {}
    try {
      document.documentElement.classList.remove('db-clip-on', 'db-clip-off');
      document.documentElement.classList.add(SUPPORTS_CLIP ? 'db-clip-on' : 'db-clip-off');
    } catch(e) {}

    function buildNameHtml(n) {
      n = sanitizeName(n);
      if (SUPPORTS_CLIP) {
        return '<span class="db-place-name">' + escapeHtml(n) + '</span>';
      }
      var colors = ['#7C4DFF', '#A95CFF', '#6A84F4'];
      var out = '<span class="db-place-name">';
      for (var i=0; i<n.length; i++) {
        out += '<span style="color:' + colors[i % colors.length] + ';">' + escapeHtml(n.charAt(i)) + '</span>';
      }
      out += '</span>';
      return out;
    }

    // ---------- title / layout ----------
    function isVisible(el) {
      if (!el) return false;
      var st = null;
      try { st = window.getComputedStyle(el); } catch(e) {}
      if (!st) return false;
      if (st.display === 'none' || st.visibility === 'hidden' || st.opacity === '0') return false;
      var r = el.getBoundingClientRect();
      if (!r || r.width < 80 || r.height < 20) return false;
      if (r.bottom < 0 || r.top > (window.innerHeight || 1000)) return false;
      return true;
    }

    function findTitleEl() {
      var nodes = document.querySelectorAll('h1,h2,h3,div,p,span');
      var best = null;
      var bestScore = -1e9;

      for (var i = 0; i < nodes.length; i++) {
        var el = nodes[i];
        var t = (el.textContent || '').trim();
        if (t.indexOf('생성 완료') < 0) continue;
        if (t.length > 40) continue;
        if (!isVisible(el)) continue;

        var fs = 0;
        try { fs = parseFloat(window.getComputedStyle(el).fontSize) || 0; } catch(e) {}
        var r = el.getBoundingClientRect();
        var score = (fs * 10) + ((r.top >= 0 && r.top <= 380) ? 200 : 0) - Math.abs(r.top - 170);

        if (score > bestScore) {
          bestScore = score;
          best = el;
        }
      }
      return best;
    }

    // ---------- top gap (spacer) ----------
    var SPACER_ID = '__db_pm3_top_spacer__';

    function findLayoutContainer(titleEl) {
      if (titleEl) {
        return titleEl.closest('main') || titleEl.closest('#root') || titleEl.closest('#__next') || document.body;
      }
      return document.querySelector('main') || document.querySelector('#root') || document.querySelector('#__next') || document.body;
    }

    function ensureTopSpacer(container) {
      if (!container) return null;
      var spacer = document.getElementById(SPACER_ID);
      if (!spacer) {
        spacer = document.createElement('div');
        spacer.id = SPACER_ID;
        spacer.className = 'db-top-spacer';
        container.insertBefore(spacer, container.firstChild);
      }
      return spacer;
    }

    function applyTopGap(titleEl) {
      var container = findLayoutContainer(titleEl);
      var spacer = ensureTopSpacer(container);
      if (!spacer || !titleEl) return;

      var DESIRED_TOP = 170;
      var rect = titleEl.getBoundingClientRect();
      var curH = spacer.getBoundingClientRect().height || 0;
      var need = curH + (DESIRED_TOP - rect.top);

      if (need < 0) need = 0;
      if (need > 320) need = 320;

      var prev = spacer.__dbPrevH || -1;
      if (Math.abs(prev - need) >= 1) {
        spacer.style.height = need + 'px';
        spacer.__dbPrevH = need;
        log('✅ topGap applied via spacer:', need + 'px');
      }
    }

    function ensureFallbackTopGap() {
      var container = findLayoutContainer(null);
      var spacer = ensureTopSpacer(container);
      if (!spacer) return;
      if (!spacer.__dbFixedOnce) {
        spacer.style.height = '160px';
        spacer.__dbFixedOnce = true;
        log('✅ fallback topGap applied: 160px');
      }
    }

    // ---------- place name pick ----------
    function readPlaceNameFromKnownKeys(storage) {
      if (!storage) return null;

      var keys = [
        '__DB_PLACE_NAME__',
        'DB_PM1_FINAL', 'db_pm1_final',
        'DB_PM_NAME', '__DB_PM_NAME__'
      ];

      for (var i=0;i<keys.length;i++) {
        var k = keys[i];
        var raw = null;
        try { raw = storage.getItem(k); } catch(e) {}
        var name = extractPlaceNameFromAny(raw);
        if (name) return name;
      }
      return null;
    }

    function collectScored(storage, label) {
      var out = [];
      try {
        for (var i=0; i<storage.length; i++) {
          var k = storage.key(i);
          if (!k) continue;

          // place 관련 키만 우선 수집(유저 name 키 오염 방지)
          var keyLower = String(k).toLowerCase();
          var looksPlaceKey = /place|pm|placemake|myplace|db_pm/.test(keyLower);
          var looksUserKey = /user|nick|profile|token|auth|access|refresh|member/.test(keyLower);
          if (!looksPlaceKey && looksUserKey) continue; // 유저/토큰 계열은 스킵

          var raw = storage.getItem(k);
          var name = extractPlaceNameFromAny(raw);
          if (!name) continue;

          var score = 0;
          if (looksPlaceKey) score += 100;
          if (looksUserKey) score -= 80;
          score += Math.min(name.length, 18);

          out.push({ key: k, name: name, label: label, score: score });
        }
      } catch(e) {}
      return out;
    }

    function extractPlaceFromTitleEl(titleEl) {
      if (!titleEl) return null;
      var t = (titleEl.textContent || '').replace(/\s+/g, ' ').trim();

      // "<이름> 생성 완료!" 형태에서 이름만 뽑기
      var m = t.match(/^(.*?)\s*생성\s*완료!?/);
      if (m && m[1]) {
        var cand = (m[1] || '').replace(/\s+/g, '').trim();
        if (isGoodName(cand)) return sanitizeName(cand);
      }
      return null;
    }

    function scanByKeyHint(storage) {
      // 너무 광범위 스캔 금지: place 관련 키만 훑는다
      var hints = ['place', 'pm', 'placemake', 'db_pm'];
      try {
        for (var i = 0; i < storage.length; i++) {
          var k = storage.key(i);
          if (!k) continue;
          var lk = String(k).toLowerCase();

          var ok = false;
          for (var j = 0; j < hints.length; j++) {
            if (lk.indexOf(hints[j]) >= 0) { ok = true; break; }
          }
          if (!ok) continue;

          var raw = storage.getItem(k);
          var n = extractNameFromAny(raw);
          if (n) return n;
        }
      } catch(e) {}
      return null;
    }

    function pickPlaceName(titleEl) {
      var v = null;
      var rawFinal = null;

      // 1) placemake1에서 저장되는 키(너 로그에 찍히는 DB_PM1_FINAL)
      try { rawFinal = localStorage.getItem('DB_PM1_FINAL'); } catch(e) {}
      if (!rawFinal) { try { rawFinal = sessionStorage.getItem('DB_PM1_FINAL'); } catch(e) {} }
      if (rawFinal) {
        v = extractNameFromAny(rawFinal);
        if (v) return v;
        return null;
      }
      return null;
    }

    function persistPlaceName(name) {
        try { localStorage.setItem('__DB_PLACE_NAME__', name); } catch(e) {}
        try { sessionStorage.setItem('__DB_PLACE_NAME__', name); } catch(e) {}
        try { document.cookie = 'DB_PLACE_NAME=' + encodeURIComponent(name) + '; path=/; max-age=604800'; } catch(e) {}
    }

    function renderInlineTitle(titleEl, name) {
      if (!titleEl || !titleEl.isConnected) return false;
      try {
        if (titleEl.dataset && titleEl.dataset.dbPm3Name === name) {
          var currentText = (titleEl.textContent || '').replace(/\s+/g, '');
          if (currentText.indexOf(name.replace(/\s+/g, '')) >= 0) {
            return false;
          }
        }
        titleEl.classList.add('db-pm-title');
        titleEl.style.textDecoration = 'none';
        titleEl.style.borderBottom = 'none';
        titleEl.style.boxShadow = 'none';
        titleEl.style.pointerEvents = 'none';
        titleEl.style.display = 'inline-block';
        titleEl.style.opacity = '1';
        titleEl.style.visibility = 'visible';
        titleEl.innerHTML = buildNameHtml(name) + ' 생성 완료!';
        if (titleEl.dataset) {
          titleEl.dataset.dbPm3Name = name;
        }
        return true;
      } catch(e) { return false; }
    }

    function renderOverlayTitle(name) {
      try {
        var overlayId = '__db_pm3_title_overlay__';
        var overlay = document.getElementById(overlayId);
        if (!overlay) {
          overlay = document.createElement('div');
          overlay.id = overlayId;
          overlay.className = 'db-pm3-title-overlay';
          overlay.innerHTML = '<div class="db-pm3-title"></div>';
          document.body.appendChild(overlay);
        }
        overlay.style.display = 'flex';
        overlay.style.opacity = '1';
        overlay.style.visibility = 'visible';
        var title = overlay.querySelector('.db-pm3-title');
        if (title && overlay.dataset.dbPm3Name !== name) {
          title.innerHTML = buildNameHtml(name) + ' 생성 완료!';
          overlay.dataset.dbPm3Name = name;
        }
      } catch(e) {}
    }

    function removeOverlayTitle() {
      try {
        var overlay = document.getElementById('__db_pm3_title_overlay__');
        if (overlay && overlay.parentNode) {
          overlay.parentNode.removeChild(overlay);
        }
      } catch(e) {}
    }

    var lastPatchedName = null;
    var lastTitleId = null;
    var reapplyTimer = null;
    var inlineStableCount = 0;

    function scheduleReapply(delayMs) {
      try {
        if (reapplyTimer) {
          clearTimeout(reapplyTimer);
        }
        reapplyTimer = setTimeout(function() {
          reapplyTimer = null;
          patchAll();
        }, delayMs || 60);
      } catch(e) {}
    }

    function patchAll() {
      var titleEl = findTitleEl();
      var name = pickPlaceName(titleEl);

      if (!name) {
        if (lastPatchedName !== null) {
          log('⚠️ no placeName found');
        }
        // 그래도 레이아웃만이라도
        if (titleEl) applyTopGap(titleEl); else ensureFallbackTopGap();
        return false;
      }

      persistPlaceName(name);

      if (titleEl && titleEl.isConnected) {
        applyTopGap(titleEl);
        renderInlineTitle(titleEl, name);
        var visibleText = (titleEl.textContent || '').replace(/\s+/g, '');
        var nameKey = name.replace(/\s+/g, '');
        if (visibleText.indexOf(nameKey) >= 0 && isVisible(titleEl)) {
          inlineStableCount++;
        } else {
          inlineStableCount = 0;
          scheduleReapply(120);
        }
        removeOverlayTitle();
      } else {
        // title을 못 찾으면 최소한 topGap 확보
        ensureFallbackTopGap();
        removeOverlayTitle();
        inlineStableCount = 0;
      }

      var titleId = titleEl ? (titleEl.id || titleEl.className || titleEl.tagName) : 'none';
      if (name !== lastPatchedName || titleId !== lastTitleId) {
        log('✅ patched, placeName=' + name);
        lastPatchedName = name;
        lastTitleId = titleId;
      }
      return true;
    }

    // ---------- run + keepAlive ----------
    patchAll();

    // DOM 리렌더에 즉시 대응
    var obs = null;
    try {
      obs = new MutationObserver(function() {
        scheduleReapply(60);
      });
      obs.observe(document.documentElement, { childList: true, subtree: true, characterData: true });
    } catch(e) {}

    var timer = setInterval(function() {
      patchAll();
    }, 1000);

    try { window.addEventListener('resize', function(){ patchAll(); }); } catch(e) {}

    log('👀 V26 installed');
  } catch (e) {
    try { console.log('[DB_PM3_FIX_V26][ERR]', e); } catch(_e) {}
  }
})();
        """.trimIndent()
    }
}
