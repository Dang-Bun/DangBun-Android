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
            var CONTENT_START_TOP = -110;   // ✅ 절대 변경하지 않음
            var NEXT_BTN_BOTTOM = 60;
            var BACK_BTN_DOWN = 20;

            var STYLE_ID = '__db_addplace_css_hack_final__';
            var MOVED_BTN_CLASS = 'db-next-btn-moved-to-body'; // 납치한 버튼 식별용

            // ✅ 스크롤락(스크롤 기능 제거) 설치 여부
            var __dbScrollLockInstalled = false;
            var __dbScrollLockGuard = false;

            function isAddPlace() {
              var path = (location.pathname || '').toLowerCase();
              if (path.indexOf('addplace') >= 0) return true;

              var text = document.body ? document.body.innerText : '';
              return (text.replace(/\s/g,'').indexOf('어떤목적으로사용하시나요') >= 0);
            }

            // ==========================================
            // [스크롤 기능 제거] (레이아웃은 건드리지 않음)
            // ==========================================
            function installScrollLock() {
              if (__dbScrollLockInstalled) return;
              __dbScrollLockInstalled = true;

              try { window.scrollTo(0, 0); } catch(e) {}

              // ✅ touchmove / wheel 차단
              function prevent(e) {
                try { e.preventDefault(); } catch(err) {}
                return false;
              }
              window.__dbAddPlacePreventScroll__ = prevent;

              try {
                document.addEventListener('touchmove', prevent, { passive: false });
                document.addEventListener('wheel', prevent, { passive: false });
              } catch(e) {
                // 일부 구형 환경 fallback
                try { document.addEventListener('touchmove', prevent); } catch(e2) {}
                try { document.addEventListener('wheel', prevent); } catch(e3) {}
              }

              // ✅ 혹시 스크롤이 발생하면 0으로 되돌림(가드로 무한루프 방지)
              window.__dbAddPlaceScrollHandler__ = function() {
                if (__dbScrollLockGuard) return;
                __dbScrollLockGuard = true;
                try { window.scrollTo(0, 0); } catch(e) {}
                __dbScrollLockGuard = false;
              };
              try { window.addEventListener('scroll', window.__dbAddPlaceScrollHandler__, { passive: true }); } catch(e) {
                try { window.addEventListener('scroll', window.__dbAddPlaceScrollHandler__); } catch(e2) {}
              }
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
            // [1] 스타일 주입 및 제거
            // ==========================================
            function applyStyles() {
                if (document.getElementById(STYLE_ID)) return;

                var style = document.createElement('style');
                style.id = STYLE_ID;
                style.textContent = `
                    /* 이 화면에서만 적용될 레이아웃 초기화 */
                    html, body, #root, #__next, main {
                        display: block !important;
                        height: auto !important;
                        min-height: 100% !important;
                        padding-top: 0 !important;
                        margin-top: 0 !important;
                        align-items: flex-start !important;
                        justify-content: flex-start !important;

                        overflow-x: hidden !important;

                        /* ✅ 스크롤 기능 제거 */
                        overflow-y: hidden !important;
                        overscroll-behavior: none !important;
                        -webkit-overflow-scrolling: auto !important;
                        touch-action: none !important;
                    }

                    /* 내용 강제 이동(값 유지) */
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

                    /* 납치된 버튼 스타일 */
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

            // ==========================================
            // [3] 메인 로직
            // ==========================================
            function manageLayout() {
                var active = isAddPlace();

                if (active) {
                    applyStyles();

                    // ✅ 스크롤 기능 제거 (레이아웃값은 건드리지 않음)
                    installScrollLock();

                    // 1) 내용 이동(값 유지)
                    var content = findContentWrapper();
                    if (content && !content.classList.contains('db-force-content-pos')) {
                        content.classList.add('db-force-content-pos');
                    }

                    // 2) 버튼 납치 및 이동
                    var btn = findNextBtn();
                    if (btn) {
                        if (!btn.classList.contains(MOVED_BTN_CLASS)) {
                            btn.classList.add(MOVED_BTN_CLASS);
                            document.body.appendChild(btn);
                        }
                    }

                    // 3) 뒤로가기 버튼 조정
                    var backBtn = findBackBtn();
                    if (backBtn) {
                        backBtn.style.setProperty('transform', 'translateY(' + BACK_BTN_DOWN + 'px)', 'important');
                        backBtn.style.setProperty('z-index', '2147483647', 'important');
                    }

                } else {
                    removeStyles();
                    removeScrollLock();

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
