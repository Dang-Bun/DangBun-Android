package com.example.dangbun.ui.webview.fixes.addplace

import android.webkit.WebView

internal object MyPlaceAddFix {

    internal fun inject(view: WebView) {
        view.evaluateJavascript(provideJs(), null)
    }

    internal fun provideJs(): String {
        return """
        (function() {
          try {
            // ==========================================
            // [설정 값 유지]
            // ==========================================
            var GRAY_BG = '#F5F6F8';
            var CONTENT_START_TOP = -110;   // ✅ 절대 변경하지 않음
            var NEXT_BTN_BOTTOM = 60;
            var BACK_BTN_DOWN = 20;

            var STYLE_ID = '__db_addplace_css_hack_final__';
            var MOVED_BTN_CLASS = 'db-next-btn-moved-to-body'; // 납치한 버튼 식별용(이름은 유지)

            // ✅ 스크롤락 설치 여부
            var __dbScrollLockInstalled = false;
            var __dbScrollLockGuard = false;

            // ✅ AddPlace 판단: URL만 사용 (SPA DOM 잔상 오판 방지)
            function isAddPlace() {
              var path = (location.pathname || '').toLowerCase();
              return (path.indexOf('addplace') >= 0);
            }

            // ==========================================
            // [스크롤 기능 제거] (AddPlace에서만)
            // ==========================================
            function installScrollLock() {
              if (__dbScrollLockInstalled) return;
              __dbScrollLockInstalled = true;

              try { window.scrollTo(0, 0); } catch(e) {}

              function prevent(e) {
                // ✅ AddPlace 아닐 때는 절대 막지 않음 (리스너가 남아도 스크롤 살림)
                if (!isAddPlace()) return true;
                try { e.preventDefault(); } catch(err) {}
                return false;
              }
              window.__dbAddPlacePreventScroll__ = prevent;

              try {
                document.addEventListener('touchmove', prevent, { passive: false });
                document.addEventListener('wheel', prevent, { passive: false });
              } catch(e) {
                try { document.addEventListener('touchmove', prevent); } catch(e2) {}
                try { document.addEventListener('wheel', prevent); } catch(e3) {}
              }

              window.__dbAddPlaceScrollHandler__ = function() {
                // ✅ AddPlace 아닐 때는 스크롤 강제 고정도 하지 않음
                if (!isAddPlace()) return;

                if (__dbScrollLockGuard) return;
                __dbScrollLockGuard = true;
                try { window.scrollTo(0, 0); } catch(e) {}
                __dbScrollLockGuard = false;
              };
              try { window.addEventListener('scroll', window.__dbAddPlaceScrollHandler__, { passive: true }); }
              catch(e) { try { window.addEventListener('scroll', window.__dbAddPlaceScrollHandler__); } catch(e2) {} }
            }

            function removeScrollLock() {
              if (!__dbScrollLockInstalled) return;
              __dbScrollLockInstalled = false;

              var prevent = window.__dbAddPlacePreventScroll__;
              if (prevent) {
                try { document.removeEventListener('touchmove', prevent); } catch(e) {}
                try { document.removeEventListener('wheel', prevent); } catch(e) {}
                window.__dbAddPlacePreventScroll__ = null;
              }

              var sh = window.__dbAddPlaceScrollHandler__;
              if (sh) {
                try { window.removeEventListener('scroll', sh); } catch(e) {}
                window.__dbAddPlaceScrollHandler__ = null;
              }
            }

            // ==========================================
            // ✅ AddPlace 밖으로 나가면 스크롤 차단 흔적 강제 원복
            // ==========================================
            function globalTargets() {
              var out = [];
              try { out.push(document.documentElement); } catch(e) {}
              try { out.push(document.body); } catch(e) {}
              try { var r = document.querySelector('#root'); if (r) out.push(r); } catch(e) {}
              try { var n = document.querySelector('#__next'); if (n) out.push(n); } catch(e) {}
              try { var m = document.querySelector('main'); if (m) out.push(m); } catch(e) {}

              var uniq = [];
              for (var i=0; i<out.length; i++) {
                if (out[i] && uniq.indexOf(out[i]) < 0) uniq.push(out[i]);
              }
              return uniq;
            }

            function forceRestoreScroll() {
              try {
                var ts = globalTargets();
                for (var i=0; i<ts.length; i++) {
                  var t = ts[i];
                  try { t.style.removeProperty('overflow-y'); } catch(e) {}
                  try { t.style.removeProperty('overflow-x'); } catch(e) {}
                  try { t.style.removeProperty('overflow'); } catch(e) {}
                  try { t.style.removeProperty('touch-action'); } catch(e) {}
                  try { t.style.removeProperty('overscroll-behavior'); } catch(e) {}
                  try { t.style.removeProperty('-webkit-overflow-scrolling'); } catch(e) {}
                }
              } catch(e) {}
            }

            // ==========================================
            // [1] 스타일 주입 및 제거
            // ==========================================
            function applyStyles() {
                if (document.getElementById(STYLE_ID)) return;

                var style = document.createElement('style');
                style.id = STYLE_ID;
                style.textContent = `
                    html, body, #root, #__next, main {
                        background-color: ${'$'}{'$'}{GRAY_BG} !important;
                        display: block !important;
                        height: auto !important;
                        min-height: 100% !important;
                        padding-top: 0 !important;
                        margin-top: 0 !important;
                        align-items: flex-start !important;
                        justify-content: flex-start !important;

                        overflow-x: hidden !important;

                        /* ✅ AddPlace에서만 스크롤 제거 */
                        overflow-y: hidden !important;
                        overscroll-behavior: none !important;
                        -webkit-overflow-scrolling: auto !important;
                        touch-action: none !important;
                    }

                    .db-force-content-pos {
                        position: absolute !important;
                        top: ${'$'}{CONTENT_START_TOP}px !important;
                        left: 0px !important;
                        width: 100% !important;
                        margin: 0 !important;
                        padding: 0 !important;
                        transform: none !important;
                        display: block !important;
                    }

                    .${'$'}{MOVED_BTN_CLASS} {
                        position: fixed !important;
                        bottom: ${'$'}{NEXT_BTN_BOTTOM}px !important;
                        left: 16px !important;
                        right: 16px !important;
                        width: auto !important;
                        max-width: none !important;
                        display: block !important;
                        z-index: 2147483647 !important;
                        top: auto !important;
                        transform: none !important;
                    }
                `;
                document.head.appendChild(style);
            }

            function removeStyles() {
                var style = document.getElementById(STYLE_ID);
                if (style) style.parentNode.removeChild(style);
            }

            // ==========================================
            // [2] 요소 찾기 및 조작
            // ==========================================
            function findContentWrapper() {
                var nodes = document.querySelectorAll('h1,h2,h3,div,p');
                for (var i=0; i<nodes.length; i++) {
                    var el = nodes[i];
                    if ((el.innerText || '').replace(/\s/g,'').indexOf('어떤목적으로사용하시나요') >= 0) {
                        if (el.tagName === 'SPAN' || el.tagName === 'STRONG') return el.offsetParent || el.parentElement;
                        return el.parentElement;
                    }
                }
                return null;
            }

            function findNextBtn() {
                var btns = document.querySelectorAll('button');
                for (var i=0; i<btns.length; i++) {
                    var t = (btns[i].innerText || '').trim();
                    if (t === '다음' && !btns[i].classList.contains(MOVED_BTN_CLASS)) return btns[i];
                }
                return null;
            }

            function findBackBtn() {
                var btns = document.querySelectorAll('button, a, [role="button"]');
                var best = null;
                var bestScore = -9999;
                for (var i=0; i<btns.length; i++) {
                    var el = btns[i];
                    var r = el.getBoundingClientRect();
                    if (r.left < window.innerWidth * 0.3 && r.top < window.innerHeight * 0.2) {
                        var score = 0;
                        if (r.width < 60 && r.height < 60) score += 5;
                        if (el.querySelector('svg, path')) score += 3;
                        if (score > bestScore) { bestScore = score; best = el; }
                    }
                }
                return best;
            }

            // ✅ React/Next root 안으로만 붙이기 (이벤트 유지 목적)
            function pickReactRootHost() {
              try {
                return document.querySelector('#__next')
                  || document.querySelector('#root')
                  || document.querySelector('main');
              } catch(e) { return null; }
            }

            // ==========================================
            // [3] 메인 로직
            // ==========================================
            function manageLayout() {
                var active = isAddPlace();

                // ✅ AddPlace가 아니면 무조건 스크롤락/스타일 해제 + 스크롤 원복
                if (!active) {
                  removeScrollLock();
                  removeStyles();
                  forceRestoreScroll();
                }

                if (active) {
                    applyStyles();
                    installScrollLock();

                    var content = findContentWrapper();
                    if (content && !content.classList.contains('db-force-content-pos')) {
                        content.classList.add('db-force-content-pos');
                    }

                    var btn = findNextBtn();
                    if (btn) {
                        if (!btn.classList.contains(MOVED_BTN_CLASS)) {
                            btn.classList.add(MOVED_BTN_CLASS);

                            // ✅ body로 보내면 클릭이 죽을 수 있어 React root 안으로만 이동
                            var host = pickReactRootHost();
                            if (host) host.appendChild(btn);
                            else document.body.appendChild(btn); // (최후 fallback)
                        }
                    }

                    var backBtn = findBackBtn();
                    if (backBtn) {
                        backBtn.style.setProperty('transform', 'translateY(' + BACK_BTN_DOWN + 'px)', 'important');
                        backBtn.style.setProperty('z-index', '2147483647', 'important');
                    }
                } else {
                    // --- 청소 모드 ---
                    var ghostBtns = document.querySelectorAll('.' + MOVED_BTN_CLASS);
                    for (var i=0; i<ghostBtns.length; i++) {
                        ghostBtns[i].parentNode.removeChild(ghostBtns[i]);
                    }
                }
            }

            setInterval(manageLayout, 100);
            manageLayout();

          } catch(e) {}
        })();
        """.trimIndent()
    }
}
