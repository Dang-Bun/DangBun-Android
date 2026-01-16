package com.example.dangbun.ui.webview.fixes.myplace

internal object MyPlaceListFix {

    internal fun provideJs(): String {
        return """
            (function() {
              try {
                var GRAY_BG = '#F5F6F8';
                var styleId = '__db_myplace_list_fix_css_only_v1__';

                // ✅ 튜닝 포인트
                var FAB_SAFE_RAISE = 18;    // 바닥(갤럭시 바)에서 버튼 띄우기(px)
                var LIST_BOTTOM_GAP = 24;   // 마지막 아이템과 버튼 사이 여유(px)
                var EXTRA_PAD = 8;          // 추가 여백(px)

                // ---------------------------
                // CSS 주입 (백틱 템플릿 사용 X: 파싱 에러 방지)
                // ---------------------------
                var style = document.getElementById(styleId);
                if (!style) {
                  style = document.createElement('style');
                  style.id = styleId;
                  document.head.appendChild(style);
                }

                style.textContent =
                  "html, body, #root, #__next, main {"
                  + "  background-color: " + GRAY_BG + " !important;"
                  + "  margin: 0 !important;"
                  + "  padding: 0 !important;"
                  + "  min-height: 100vh !important;"
                  + "}"
                  + "\n"
                  + ".db-add-btn-wrap {"
                  + "  position: fixed !important;"
                  + "  left: 50% !important;"
                  + "  transform: translateX(-50%) !important;"
                  + "  width: calc(100vw - 32px) !important;"
                  + "  max-width: calc(100vw - 32px) !important;"
                  + "  z-index: 2147483647 !important;"
                  + "  margin: 0 !important;"
                  + "  padding: 0 !important;"
                  + "  box-sizing: border-box !important;"
                  + "  background: transparent !important;"
                  + "}"
                  + "\n"
                  + ".db-add-btn-wrap > button {"
                  + "  width: 100% !important;"
                  + "  display: block !important;"
                  + "  margin: 0 auto !important;"
                  + "}";

                function isMyPlace() {
                  try {
                    var path = location.pathname || '';
                    var bodyText = (document.body && document.body.innerText) ? document.body.innerText : '';
                    return (
                      path.indexOf('MyPlace') >= 0 ||
                      path.indexOf('myplace') >= 0 ||
                      bodyText.indexOf('내 플레이스') >= 0
                    );
                  } catch(e) { return false; }
                }

                function isListView() {
                  try {
                    var text = (document.body && document.body.innerText) ? document.body.innerText : '';
                    var score = 0;
                    if (text.indexOf('새로운 알림') >= 0) score++;
                    if (text.indexOf('완료된 청소') >= 0) score++;
                    if (text.indexOf('매니저') >= 0) score++;
                    return score >= 2;
                  } catch(e) { return true; }
                }

                function px(n) { return Math.max(0, Math.floor(n || 0)); }

                // ✅ "플레이스 추가" 버튼만 정확히 찾기(완전일치)
                function findAddButtonsExact() {
                  var out = [];
                  try {
                    var btns = document.querySelectorAll('button');
                    for (var i = 0; i < btns.length; i++) {
                      var b = btns[i];
                      var t = ((b.innerText || '')).trim();
                      if (t !== '플레이스 추가') continue;

                      var r = null;
                      try { r = b.getBoundingClientRect(); } catch(e) { r = null; }
                      if (!r) continue;

                      // ✅ 카드 내부 작은 버튼 등 오탐 방지: 충분히 큰 버튼만
                      if (r.width < window.innerWidth * 0.55) continue;
                      if (r.height < 36) continue;

                      out.push(b);
                    }
                  } catch(e) {}
                  return out;
                }

                // ✅ 버튼 wrapper 찾기(너무 위로 안 올라가게 제한)
                function findFabWrap(btn) {
                  try {
                    var cur = btn;
                    for (var step = 0; step < 10 && cur; step++) {
                      var p = cur.parentElement;
                      if (!p) break;

                      var r = null;
                      try { r = p.getBoundingClientRect(); } catch(e) { r = null; }
                      if (!r) { cur = p; continue; }

                      var wOk = r.width >= window.innerWidth * 0.75;
                      var hOk = r.height > 0 && r.height <= 280;

                      if (wOk && hOk) return p;
                      cur = p;
                    }
                  } catch(e) {}
                  return btn.parentElement || btn;
                }

                function isScrollable(el) {
                  try {
                    if (!el) return false;
                    var st = window.getComputedStyle(el);
                    var oy = st ? st.overflowY : '';
                    if (oy !== 'auto' && oy !== 'scroll') return false;
                    return (el.scrollHeight > el.clientHeight + 10);
                  } catch(e) { return false; }
                }

                function pickBestScrollHost() {
                  // 1) scrollingElement 우선
                  try {
                    var docEl = document.scrollingElement || document.documentElement;
                    if (docEl && docEl.scrollHeight > docEl.clientHeight + 10) return docEl;
                  } catch(e) {}

                  // 2) main/root/body 중 스크롤 가능한 큰 컨테이너
                  try {
                    var cands = [
                      document.querySelector('main'),
                      document.querySelector('#root'),
                      document.querySelector('#__next'),
                      document.body,
                      document.documentElement
                    ].filter(Boolean);

                    for (var i = 0; i < cands.length; i++) {
                      if (isScrollable(cands[i])) return cands[i];
                    }
                  } catch(e) {}

                  return document.body;
                }

                function ensureBottomSpacer(host, safePx) {
                  try {
                    if (!host) return;

                    var spacerId = '__db_list_bottom_spacer__';
                    var spacer = document.getElementById(spacerId);
                    if (!spacer) {
                      spacer = document.createElement('div');
                      spacer.id = spacerId;
                      spacer.style.width = '1px';
                      spacer.style.pointerEvents = 'none';
                      spacer.style.background = 'transparent';
                    }

                    spacer.style.height = (safePx + EXTRA_PAD) + 'px';

                    if (spacer.parentNode !== host) {
                      try { spacer.parentNode && spacer.parentNode.removeChild(spacer); } catch(e) {}
                      host.appendChild(spacer);
                    } else {
                      if (host.lastElementChild !== spacer) host.appendChild(spacer);
                    }
                  } catch(e) {}
                }

                function applyBottomPadding(host, safePx) {
                  try {
                    if (!host) return;

                    host.style.setProperty('box-sizing', 'border-box', 'important');
                    host.style.setProperty('padding-bottom', (safePx + EXTRA_PAD) + 'px', 'important');
                    host.style.setProperty('scroll-padding-bottom', (safePx + EXTRA_PAD) + 'px', 'important');

                    ensureBottomSpacer(host, safePx);
                  } catch(e) {}
                }

                function resetWrap(wrap) {
                  try {
                    if (!wrap) return;
                    wrap.classList.remove('db-add-btn-wrap');
                    wrap.style.removeProperty('position');
                    wrap.style.removeProperty('left');
                    wrap.style.removeProperty('right');
                    wrap.style.removeProperty('bottom');
                    wrap.style.removeProperty('transform');
                    wrap.style.removeProperty('width');
                    wrap.style.removeProperty('max-width');
                    wrap.style.removeProperty('z-index');
                    wrap.style.removeProperty('margin');
                    wrap.style.removeProperty('padding');
                    wrap.style.removeProperty('box-sizing');
                    wrap.style.removeProperty('background');
                  } catch(e) {}
                }

                function apply() {
                  try {
                    if (!isMyPlace()) return;
                    if (!isListView()) return;

                    // ✅ 버튼들 찾기
                    var btns = findAddButtonsExact();
                    if (!btns || btns.length === 0) return;

                    // ✅ 여러 개면 하나만 살리고 나머지는 숨김 (중복 방지)
                    //    - DOM 이동 안 하니까 이벤트는 그대로 유지됨
                    var best = btns[0];
                    var bestTop = -999999;
                    for (var i = 0; i < btns.length; i++) {
                      var r = btns[i].getBoundingClientRect();
                      if (r.top > bestTop) { bestTop = r.top; best = btns[i]; }
                    }

                    for (var j = 0; j < btns.length; j++) {
                      if (btns[j] === best) continue;
                      try { btns[j].style.setProperty('display', 'none', 'important'); } catch(e) {}
                    }

                    // ✅ wrapper 고정 (DOM 이동 없음)
                    var wrap = findFabWrap(best);

                    // 혹시 과거에 잘못 적용된 wrap이 여러 개면 정리
                    try {
                      var oldWraps = document.querySelectorAll('.db-add-btn-wrap');
                      for (var k = 0; k < oldWraps.length; k++) {
                        if (oldWraps[k] !== wrap) resetWrap(oldWraps[k]);
                      }
                    } catch(e) {}

                    if (wrap) {
                      wrap.classList.add('db-add-btn-wrap');

                      // ✅ 갤럭시 바 뒤에 숨지 않게 bottom 올리기
                      wrap.style.setProperty('bottom', (FAB_SAFE_RAISE) + 'px', 'important');

                      // 아래는 class에도 있지만, SPA에서 style이 덮일 수 있어 한 번 더 강제
                      wrap.style.setProperty('position', 'fixed', 'important');
                      wrap.style.setProperty('left', '50%', 'important');
                      wrap.style.setProperty('transform', 'translateX(-50%)', 'important');
                      wrap.style.setProperty('width', 'calc(100vw - 32px)', 'important');
                      wrap.style.setProperty('max-width', 'calc(100vw - 32px)', 'important');
                      wrap.style.setProperty('z-index', '2147483647', 'important');
                      wrap.style.setProperty('margin', '0', 'important');
                      wrap.style.setProperty('padding', '0', 'important');
                      wrap.style.setProperty('box-sizing', 'border-box', 'important');
                      wrap.style.setProperty('background', 'transparent', 'important');
                    }

                    // ✅ 리스트 마지막 아이템 접근용 하단 여백
                    var btnH = 56;
                    try { btnH = px(best.getBoundingClientRect().height || 56); } catch(e) {}

                    var safePx = px(btnH + FAB_SAFE_RAISE + LIST_BOTTOM_GAP);
                    var host = pickBestScrollHost();
                    applyBottomPadding(host, safePx);

                  } catch(e) {}
                }

                apply();
                setTimeout(apply, 80);
                setTimeout(apply, 200);
                setTimeout(apply, 450);

                if (!window.__db_myplace_list_ob__) {
                  window.__db_myplace_list_ob__ = new MutationObserver(function(){ apply(); });
                  window.__db_myplace_list_ob__.observe(document.documentElement, { childList: true, subtree: true });
                }

                window.addEventListener('resize', function(){ setTimeout(apply, 80); });

              } catch(e) {}
            })();
        """.trimIndent()
    }
}
