package com.example.dangbun.ui.webview.fixes.onboarding

import android.webkit.WebView

internal object OnboardingTopInsetFix {
    internal fun inject(
        view: WebView,
        topPx: Int = 0,
    ) {
        view.evaluateJavascript(provideJs(topPx), null)
    }

    private fun provideJs(topPx: Int): String {
        return """
            (function() {
              try {
                var TOP_PX = $topPx;

                var STYLE_ID = '__db_onboarding_top_inset_fix__';
                var MASK_ID  = '__db_onb_bottom_mask__';
                var DOT_PORTAL_ID = '__db_onb_dots_portal__';

                // ✅ 확대/위치 (현재 잘 맞춘 값 유지)
                var IMG_SCALE   = 1.10;
                var IMG_SHIFT_Y = 6;

                // ✅ 버튼/하단 영역
                var BTN_BOTTOM = 18;

                // ✅ 마스크/닷
                var MASK_HEIGHT = 120;          // 하단 흰 영역
                var DOT_GAP_FROM_BTN = 4;       // 버튼 위 여백(원하는 느낌이면 6~12 사이 미세조정)
                var DOT_FORCE_MIN_BOTTOM = 90; // 너무 아래로 가려지는 것 방지용 최소 bottom

                function isOnboarding() {
                  var path = (location.pathname || '').toLowerCase();
                  return path.indexOf('onboarding') >= 0;
                }

                // ============================================================
                // 🧹 뒷정리 (온보딩 이탈 시 실행)
                // ============================================================
                function cleanUp() {
                  try {
                    var style = document.getElementById(STYLE_ID);
                    if (style) style.remove();

                    var mask = document.getElementById(MASK_ID);
                    if (mask) mask.remove();

                    var portal = document.getElementById(DOT_PORTAL_ID);
                    if (portal) portal.remove();

                    if (window.__db_onb_dots_observer__) {
                      try { window.__db_onb_dots_observer__.disconnect(); } catch(e) {}
                      window.__db_onb_dots_observer__ = null;
                    }

                    // root 스타일 원복
                    var roots = document.querySelectorAll('html, body, #root, #__next, main');
                    roots.forEach(function(el) {
                      el.style.removeProperty('overflow'); el.style.removeProperty('overflow-x'); el.style.removeProperty('overflow-y');
                      el.style.removeProperty('height'); el.style.removeProperty('width');
                      el.style.removeProperty('position'); el.style.removeProperty('display');
                      el.style.removeProperty('align-items'); el.style.removeProperty('justify-content');
                      el.style.removeProperty('padding-top'); el.style.removeProperty('padding-bottom');
                    });

                    // 고정했던 버튼 원상복구
                    var btns = document.querySelectorAll('[data-db-fixed]');
                    btns.forEach(function(btn) {
                      btn.removeAttribute('data-db-fixed');
                      btn.removeAttribute('data-db-listener');
                      btn.style.cssText = '';
                    });

                    // 숨겼던 원본 dots 원복
                    var srcDots = document.querySelectorAll('[data-db-dots-src="true"]');
                    srcDots.forEach(function(d) {
                      d.removeAttribute('data-db-dots-src');
                      d.style.removeProperty('opacity');
                      d.style.removeProperty('visibility');
                      d.style.removeProperty('pointer-events');
                    });
                  } catch(e) {}
                }

                // ============================================================
                // 🧱 하단 흰색 마스크 설치
                // ============================================================
                function ensureBottomMask() {
                  if (!isOnboarding()) return;

                  var mask = document.getElementById(MASK_ID);
                  if (!mask) {
                    mask = document.createElement('div');
                    mask.id = MASK_ID;
                    document.body.appendChild(mask);
                  }

                  mask.style.setProperty('position', 'fixed', 'important');
                  mask.style.setProperty('left', '0', 'important');
                  mask.style.setProperty('right', '0', 'important');
                  mask.style.setProperty('bottom', '0', 'important');
                  mask.style.setProperty('height', MASK_HEIGHT + 'px', 'important');
                  mask.style.setProperty('background', '#FFFFFF', 'important');

                  // ✅ 흰 마스크는 "슬라이딩 이미지 위"에 확실히 올라오게 (dots보다 한 단계 아래)
                  mask.style.setProperty('z-index', '2147483645', 'important');
                  mask.style.setProperty('pointer-events', 'none', 'important');
                }

                // ============================================================
                // 🕵️‍♀️ 하단 버튼 강력 고정
                // ============================================================
                function fixBottomButton() {
                  if (!isOnboarding()) return;

                  var btns = document.querySelectorAll('button, [role="button"], a');
                  var targetBtn = null;

                  for (var i = btns.length - 1; i >= 0; i--) {
                    var b = btns[i];
                    if (b.offsetWidth > 0 && b.offsetHeight > 0) {
                      var txt = (b.innerText || '').trim();
                      if (txt.length > 0 || b.querySelector('img') || b.querySelector('svg')) {
                        targetBtn = b;
                        break;
                      }
                    }
                  }

                  if (!targetBtn) return;

                  targetBtn.setAttribute('data-db-fixed', 'true');

                  targetBtn.style.setProperty('position', 'fixed', 'important');
                  targetBtn.style.setProperty('bottom', BTN_BOTTOM + 'px', 'important');
                  targetBtn.style.setProperty('left', '20px', 'important');
                  targetBtn.style.setProperty('right', '20px', 'important');
                  targetBtn.style.setProperty('width', 'auto', 'important');
                  targetBtn.style.setProperty('margin', '0', 'important');
                  targetBtn.style.setProperty('padding', '0', 'important');
                  targetBtn.style.setProperty('box-sizing', 'border-box', 'important');
                  targetBtn.style.setProperty('z-index', '2147483647', 'important');
                  targetBtn.style.setProperty('pointer-events', 'auto', 'important');

                  Array.from(targetBtn.querySelectorAll('*')).forEach(function(child) {
                    child.style.setProperty('pointer-events', 'auto', 'important');
                  });

                  if (!targetBtn.getAttribute('data-db-listener')) {
                    targetBtn.setAttribute('data-db-listener', 'true');
                    targetBtn.addEventListener('click', function() {
                      setTimeout(cleanUp, 300);
                    });
                  }
                }

                // ============================================================
                // 🟣 dots 탐색 유틸 (점처럼 보이는 요소 판단)
                // ============================================================
                function isDotLike(el) {
                  try {
                    if (!el) return false;
                    var r = el.getBoundingClientRect();
                    if (!r) return false;
                    if (r.width < 4 || r.height < 4) return false;
                    if (r.width > 40 || r.height > 40) return false;

                    var cs = window.getComputedStyle(el);
                    var br = (cs.borderRadius || '').toString();
                    var round = (br.indexOf('50%') >= 0) || (parseFloat(br) >= 8) || (br.indexOf('999') >= 0);

                    // 배경이 투명인 애는 점 가능성이 낮음(예외는 있으니 완전 배제는 X)
                    return round;
                  } catch(e) { return false; }
                }

                function scoreDotsContainer(el) {
                  try {
                    if (!el || !el.children) return -1;
                    var kids = el.children;
                    var n = kids.length;
                    if (n < 3 || n > 10) return -1;

                    var ok = 0;
                    for (var i=0; i<n; i++) {
                      if (isDotLike(kids[i])) ok++;
                    }
                    if (ok < 3) return -1;

                    var r = el.getBoundingClientRect();
                    if (!r) return -1;
                    if (r.width < 40 || r.width > 340) return -1;
                    if (r.height > 140) return -1;

                    var vw = window.innerWidth, vh = window.innerHeight;
                    var cx = (r.left + r.right) / 2;
                    var centerDist = Math.abs(cx - vw / 2);

                    // 하단 근처 가산점
                    var desiredBottom = vh * 0.92;
                    var bottomDist = Math.abs(r.bottom - desiredBottom);

                    return 1000 - centerDist - bottomDist + ok * 25;
                  } catch(e) { return -1; }
                }

                function findBestDotsContainer() {
                  var all = document.querySelectorAll('body *');
                  var best = null;
                  var bestScore = -1;

                  for (var i=0; i<all.length; i++) {
                    var el = all[i];
                    var s = scoreDotsContainer(el);
                    if (s < 0) continue;

                    // class/aria 힌트 가산점
                    try {
                      var cls = (el.className || '').toString().toLowerCase();
                      var aria = ((el.getAttribute && (el.getAttribute('aria-label') || '')) || '').toLowerCase();
                      var hinted =
                        (cls.indexOf('dot') >= 0 || cls.indexOf('indicator') >= 0 || cls.indexOf('pagination') >= 0 || cls.indexOf('page') >= 0 ||
                         aria.indexOf('dot') >= 0 || aria.indexOf('indicator') >= 0 || aria.indexOf('page') >= 0);
                      if (hinted) s += 120;
                    } catch(e) {}

                    if (s > bestScore) { bestScore = s; best = el; }
                  }
                  return best;
                }

                // ============================================================
                // 🟣 dots 포탈 생성 (body 직속)
                // ============================================================
                function ensureDotsPortal() {
                  if (!isOnboarding()) return null;

                  var portal = document.getElementById(DOT_PORTAL_ID);
                  if (!portal) {
                    portal = document.createElement('div');
                    portal.id = DOT_PORTAL_ID;
                    document.body.appendChild(portal);
                  }

                  portal.style.setProperty('position', 'fixed', 'important');
                  portal.style.setProperty('left', '0', 'important');
                  portal.style.setProperty('right', '0', 'important');
                  portal.style.setProperty('display', 'flex', 'important');
                  portal.style.setProperty('justify-content', 'center', 'important');
                  portal.style.setProperty('align-items', 'center', 'important');
                  portal.style.setProperty('pointer-events', 'none', 'important');
                  portal.style.setProperty('z-index', '2147483646', 'important'); // mask(45) < dots(46) < btn(47)
                  return portal;
                }

                function getActiveIndexFromSource(src) {
                  try {
                    var kids = src.children || [];
                    var n = kids.length;
                    if (!n) return 0;

                    // 1) aria-current / aria-selected / class active 우선
                    for (var i=0; i<n; i++) {
                      var k = kids[i];
                      var ac = (k.getAttribute && k.getAttribute('aria-current')) || '';
                      var as = (k.getAttribute && k.getAttribute('aria-selected')) || '';
                      var cls = (k.className || '').toString().toLowerCase();
                      if (ac === 'true' || as === 'true' || cls.indexOf('active') >= 0 || cls.indexOf('selected') >= 0) return i;
                    }

                    // 2) style 차이(배경색/opacity)로 추정
                    var best = 0;
                    var bestScore = -1;
                    for (var j=0; j<n; j++) {
                      var cs = window.getComputedStyle(kids[j]);
                      var bg = (cs.backgroundColor || '');
                      var op = parseFloat(cs.opacity || '1');
                      var w = parseFloat(cs.width || '0');
                      var score = 0;
                      if (bg && bg !== 'rgba(0, 0, 0, 0)' && bg !== 'transparent') score += 5;
                      score += op * 2;
                      score += (w >= 10 ? 1 : 0);
                      if (score > bestScore) { bestScore = score; best = j; }
                    }
                    return best;
                  } catch(e) {
                    return 0;
                  }
                }

                function renderCustomDots(portal, count, activeIndex) {
                  if (!portal) return;

                  // ✅ 완전 흰 배경 위에 떠 있는 느낌(웹처럼)
                  portal.style.setProperty('background', 'transparent', 'important');

                  // 내부 렌더
                  var html = '<div style="display:flex;align-items:center;justify-content:center;gap:10px;">';
                  for (var i=0; i<count; i++) {
                    var isActive = (i === activeIndex);
                    var size = isActive ? 10 : 8;
                    var color = isActive ? '#4A7BFF' : '#D0D0D0';
                    html += '<span style="width:'+size+'px;height:'+size+'px;border-radius:999px;background:'+color+';display:inline-block;"></span>';
                  }
                  html += '</div>';

                  portal.innerHTML = html;
                }

                // ============================================================
                // 🟣 dots를 "레이어 밖"으로 빼서(포탈) 버튼 바로 위에 배치
                // ============================================================
                function fixIndicatorDots() {
                  if (!isOnboarding()) return;

                  var src = findBestDotsContainer();
                  if (!src) return;

                  // 원본 dots는 숨김(레이어/클리핑 문제 회피)
                  src.setAttribute('data-db-dots-src', 'true');
                  src.style.setProperty('opacity', '0', 'important');
                  src.style.setProperty('visibility', 'hidden', 'important');
                  src.style.setProperty('pointer-events', 'none', 'important');

                  var portal = ensureDotsPortal();
                  if (!portal) return;

                  // 버튼 높이 기반으로 "버튼 바로 위" 계산
                  var btn = document.querySelector('[data-db-fixed="true"]');
                  var btnH = 56;
                  if (btn) {
                    var br = btn.getBoundingClientRect();
                    if (br && br.height) btnH = Math.max(44, Math.min(90, br.height));
                  }

                  var computedBottom = (BTN_BOTTOM + btnH + DOT_GAP_FROM_BTN);
                  computedBottom = Math.max(DOT_FORCE_MIN_BOTTOM, computedBottom);

                  // ✅ dots가 MASK 영역 밖으로 올라가면 “흰 배경 위” 느낌이 깨짐 → mask 안에 제한
                  computedBottom = Math.min(MASK_HEIGHT - 14, computedBottom);

                  portal.style.setProperty('bottom', computedBottom + 'px', 'important');

                  // dots 개수/활성 인덱스 추정
                  var count = 5;
                  try {
                    var n = (src.children && src.children.length) ? src.children.length : 0;
                    if (n >= 3 && n <= 10) count = n;
                  } catch(e) {}

                  var activeIndex = getActiveIndexFromSource(src);
                  renderCustomDots(portal, count, activeIndex);

                  // 상태 변하면 portal 업데이트
                  if (window.__db_onb_dots_observer__) {
                    try { window.__db_onb_dots_observer__.disconnect(); } catch(e) {}
                    window.__db_onb_dots_observer__ = null;
                  }

                  try {
                    window.__db_onb_dots_observer__ = new MutationObserver(function() {
                      try {
                        var ai = getActiveIndexFromSource(src);
                        renderCustomDots(portal, count, ai);
                      } catch(e) {}
                    });
                    window.__db_onb_dots_observer__.observe(src, {
                      subtree: true,
                      childList: true,
                      attributes: true,
                      characterData: true
                    });
                  } catch(e) {}
                }

                // ============================================================
                // 🎨 스타일 주입 (온보딩에서만)
                // ============================================================
                function applyStyle() {
                  if (!isOnboarding()) {
                    cleanUp();
                    return;
                  }

                  var style = document.getElementById(STYLE_ID);
                  if (!style) {
                    style = document.createElement('style');
                    style.id = STYLE_ID;
                    document.head.appendChild(style);
                  }

                  style.textContent =
                    // 스크롤 금지
                    'html, body { background:#FFFFFF !important; margin:0 !important; padding:0 !important; width:100% !important; height:100% !important; overflow:hidden !important; overscroll-behavior:none !important; }' +
                    // 하단 마스크 높이 확보
                    'body { padding-top:' + TOP_PX + 'px !important; padding-bottom:' + MASK_HEIGHT + 'px !important; }' +
                    // 중앙정렬 유지
                    '#root, #__next, main { display:flex !important; flex-direction:column !important; justify-content:center !important; align-items:center !important; width:100% !important; height:100% !important; overflow:hidden !important; }' +
                    'h1, h2, h3, h4, h5, h6, p, span, div[class*="text"] { text-align:center !important; }' +

                    // ✅ 이미지: 확대 + 하단 살짝 잘림 + 가운데 정렬
                    'img:not(.icon):not([class*="icon"]) {' +
                      'width: calc(100vw * ' + IMG_SCALE + ') !important;' +
                      'max-width: none !important;' +
                      'height: auto !important;' +
                      'display:block !important;' +
                      'position: relative !important;' +
                      'left: 50% !important;' +
                      'margin: 0 !important;' +
                      'transform: translate(-50%, ' + IMG_SHIFT_Y + 'px) !important;' +
                      'pointer-events:none !important;' +
                      'z-index:0 !important;' +
                    '}' +

                    // 아이콘/SVG 보호
                    'svg { max-width:100% !important; width:auto !important; height:auto !important; margin:0 auto !important; z-index:1 !important; transform:none !important; pointer-events:none !important; }' +

                    // 입력폼 보호
                    'input, form, label { text-align:left !important; opacity:1 !important; visibility:visible !important; display:block !important; pointer-events:auto !important; }';

                  ensureBottomMask();
                  fixBottomButton();
                  fixIndicatorDots();
                }

                if (!window.__db_onboarding_timer__) {
                  window.__db_onboarding_timer__ = setInterval(applyStyle, 300);
                }
                applyStyle();

              } catch(e) {}
            })();
        """.trimIndent()
    }
}
