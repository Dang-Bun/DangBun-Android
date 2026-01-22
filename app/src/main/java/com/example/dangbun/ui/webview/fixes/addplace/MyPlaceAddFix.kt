package com.example.dangbun.ui.webview.fixes.addplace

import android.webkit.WebView
import com.example.dangbun.ui.webview.fixes.common.ResponsiveUtils

internal object MyPlaceAddFix {

    internal fun inject(view: WebView) {
        view.evaluateJavascript(provideJs(), null)
    }

    internal fun provideJs(): String {
        return """
        (function() {
          try {
            // ==========================================
            // [반응형 설정 값]
            // ==========================================
            // ✅ 반응형 유틸리티 로드
            if (!window.__dangbun_responsive_utils__) {
              ${ResponsiveUtils.getResponsiveJs()}
            }

            var GRAY_BG = '#F5F6F8';
            // ✅ 기준 값들을 화면 크기에 맞게 동적 계산
            // ✅ 상단 여백 최소화: 콘텐츠를 더 많이 위로 올림
            var CONTENT_START_TOP_BASE = -180;  // -150 -> -180 (더 위로 올림)
            var NEXT_BTN_BOTTOM_BASE = 80;  // 60 -> 80 (다음 버튼을 위로 올림)
            var BACK_BTN_DOWN_BASE = 40;  // 20 -> 40 (뒤로가기 버튼을 아래로 내림, translateY는 양수면 아래로)
            
            var CONTENT_START_TOP = window.getResponsivePx ? window.getResponsivePx(CONTENT_START_TOP_BASE, 'height') : CONTENT_START_TOP_BASE;
            var NEXT_BTN_BOTTOM = window.getResponsivePx ? window.getResponsivePx(NEXT_BTN_BOTTOM_BASE, 'height') : NEXT_BTN_BOTTOM_BASE;
            var BACK_BTN_DOWN = window.getResponsivePx ? window.getResponsivePx(BACK_BTN_DOWN_BASE, 'height') : BACK_BTN_DOWN_BASE;

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
                var style = document.getElementById(STYLE_ID);
                if (!style) {
                    style = document.createElement('style');
                    style.id = STYLE_ID;
                    document.head.appendChild(style);
                }

                // ✅ 화면 크기에 따라 동적으로 재계산
                var contentTop = window.getResponsivePx ? window.getResponsivePx(CONTENT_START_TOP_BASE, 'height') : CONTENT_START_TOP_BASE;
                var btnBottom = window.getResponsivePx ? window.getResponsivePx(NEXT_BTN_BOTTOM_BASE, 'height') : NEXT_BTN_BOTTOM_BASE;

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

                    /* ✅ 상단 여백 최소화: 뒤로가기 버튼과 텍스트 사이 간격 최소화 */
                    header, nav, [role="banner"], [class*="Header"], [class*="header"], [class*="AppBar"], [class*="appbar"] {
                        padding-top: 0 !important;
                        margin-top: 0 !important;
                        padding-bottom: 0 !important;
                        margin-bottom: 0 !important;
                        min-height: auto !important;
                        height: auto !important;
                    }

                    /* ✅ 뒤로가기 버튼 영역의 여백 완전 제거 */
                    button[aria-label*="뒤로"], button[aria-label*="back"], 
                    a[aria-label*="뒤로"], a[aria-label*="back"],
                    [class*="Back"], [class*="back"], 
                    [class*="Arrow"], [class*="arrow"],
                    svg, path {
                        margin-top: 0 !important;
                        padding-top: 0 !important;
                        margin-bottom: 0 !important;
                        padding-bottom: 0 !important;
                    }

                    /* ✅ 뒤로가기 버튼 바로 아래 요소들의 상단 여백 완전 제거 */
                    main > *:first-child, #root > *:first-child, #__next > *:first-child,
                    main > *:first-child > *:first-child, #root > *:first-child > *:first-child {
                        margin-top: 0 !important;
                        padding-top: 0 !important;
                    }

                    /* ✅ 질문 텍스트 영역의 상단 여백 제거 */
                    h1, h2, h3, p, div {
                        margin-top: 0 !important;
                        padding-top: 0 !important;
                    }

                    /* ✅ 첫 번째 텍스트 요소의 여백 제거 */
                    main h1:first-child, main h2:first-child, main h3:first-child,
                    main p:first-child, main div:first-child,
                    #root h1:first-child, #root h2:first-child, #root h3:first-child,
                    #root p:first-child, #root div:first-child {
                        margin-top: 0 !important;
                        padding-top: 0 !important;
                    }

                    /* ✅ 매니저/멤버 옵션 간격 조정 */
                    /* 모든 flex 컨테이너의 gap 증가 */
                    [style*="display: flex"], [style*="display:flex"],
                    [class*="flex"], [class*="Flex"] {
                        gap: 20px !important;
                    }

                    /* 체크 표시 아이콘 아래 간격 넓히기 */
                    svg, [class*="check"], [class*="Check"],
                    [class*="icon"], [class*="Icon"] {
                        margin-bottom: 16px !important;
                    }

                    .db-force-content-pos {
                        position: absolute !important;
                        top: ${'$'}{contentTop}px !important;
                        left: 0px !important;
                        width: 100% !important;
                        margin: 0 !important;
                        padding: 0 !important;
                        transform: none !important;
                        display: block !important;
                    }

                    .${'$'}{MOVED_BTN_CLASS} {
                        position: fixed !important;
                        bottom: ${'$'}{btnBottom}px !important;
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

            // ✅ 매니저/멤버 옵션 간격 조정 함수
            function adjustOptionSpacing() {
              try {
                // "매니저" 텍스트를 포함한 요소 찾기
                var allElements = document.querySelectorAll('div, p, span, h1, h2, h3');
                var managerOption = null;
                var memberOption = null;

                for (var i = 0; i < allElements.length; i++) {
                  var text = (allElements[i].innerText || '').replace(/\s/g, '');
                  
                  // 매니저 옵션 찾기
                  if (text.indexOf('매니저') >= 0 || text.indexOf('Manager') >= 0) {
                    if (!managerOption) {
                      // 매니저 옵션의 루트 컨테이너 찾기
                      var cur = allElements[i];
                      for (var up = 0; up < 5; up++) {
                        if (!cur || !cur.parentElement) break;
                        cur = cur.parentElement;
                        var rect = cur.getBoundingClientRect();
                        // 적당한 크기의 컨테이너 찾기
                        if (rect.width > window.innerWidth * 0.5 && rect.height > 100) {
                          managerOption = cur;
                          break;
                        }
                      }
                    }
                  }

                  // 멤버 옵션 찾기
                  if (text.indexOf('기존플레이스에참여할거에요') >= 0 || text.indexOf('참여할거에요') >= 0) {
                    if (!memberOption) {
                      var cur2 = allElements[i];
                      for (var up2 = 0; up2 < 5; up2++) {
                        if (!cur2 || !cur2.parentElement) break;
                        cur2 = cur2.parentElement;
                        var rect2 = cur2.getBoundingClientRect();
                        if (rect2.width > window.innerWidth * 0.5 && rect2.height > 100) {
                          memberOption = cur2;
                          break;
                        }
                      }
                    }
                  }
                }

                // 매니저 옵션: 원 아이콘과 체크 표시 사이 간격 넓히기
                if (managerOption) {
                  var children = managerOption.querySelectorAll('div, svg, circle');
                  for (var j = 0; j < children.length; j++) {
                    var child = children[j];
                    var rect = child.getBoundingClientRect();
                    // 원형 아이콘 또는 체크 표시인 경우
                    if (rect.width > 40 && rect.width < 150 && rect.height > 40 && rect.height < 150) {
                      child.style.marginBottom = '20px';
                    }
                  }
                }

                // 멤버 옵션: 체크 표시와 텍스트 사이 간격 넓히기
                if (memberOption) {
                  var checkIcons = memberOption.querySelectorAll('svg, circle, path');
                  for (var k = 0; k < checkIcons.length; k++) {
                    checkIcons[k].style.marginBottom = '20px';
                    // 체크 표시의 부모 요소도 간격 조정
                    var parent = checkIcons[k].parentElement;
                    if (parent) {
                      parent.style.marginBottom = '20px';
                    }
                  }
                }
              } catch(e) {
                console.log('ADJUST_OPTION_SPACING_ERR', e);
              }
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

                    // ✅ 매니저/멤버 옵션 간격 조정
                    adjustOptionSpacing();

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
                        // ✅ 반응형 계산 - 뒤로가기 버튼을 아래로 내림
                        var backDown = window.getResponsivePx ? window.getResponsivePx(BACK_BTN_DOWN_BASE, 'height') : BACK_BTN_DOWN_BASE;
                        backBtn.style.setProperty('transform', 'translateY(' + backDown + 'px)', 'important');
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

            // ✅ 화면 크기 변경 시에도 재계산되도록 리사이즈 이벤트 추가
            if (!window.__dangbun_addplace_resize_handler__) {
                window.__dangbun_addplace_resize_handler__ = true;
                var resizeTimer;
                window.addEventListener('resize', function() {
                    clearTimeout(resizeTimer);
                    resizeTimer = setTimeout(function() {
                        if (isAddPlace()) {
                            applyStyles(); // 스타일 재계산
                        }
                    }, 100);
                });
            }

            setInterval(manageLayout, 100);
            manageLayout();

          } catch(e) {}
        })();
        """.trimIndent()
    }
}
