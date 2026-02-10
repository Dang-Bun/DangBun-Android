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

                function log(msg) {
                    // console.log('[DB_DEBUG] ' + msg); // 필요시 주석 해제
                }

                function isOnboarding() {
                    var path = (location.pathname || '').toLowerCase();
                    return path.indexOf('onboarding') >= 0;
                }

                // ============================================================
                // 🧹 뒷정리 (로그인 화면 진입 시 실행)
                // ============================================================
                function cleanUp() {
                    var style = document.getElementById(STYLE_ID);
                    if (style) style.remove();

                    var roots = document.querySelectorAll('html, body, #root, #__next, main');
                    roots.forEach(function(el) {
                        el.style.removeProperty('overflow'); el.style.removeProperty('overflow-x'); el.style.removeProperty('overflow-y');
                        el.style.removeProperty('height'); el.style.removeProperty('width');
                        el.style.removeProperty('position'); el.style.removeProperty('display');
                        el.style.removeProperty('align-items'); el.style.removeProperty('justify-content');
                    });

                    // 고정했던 버튼들 원상복구
                    var btns = document.querySelectorAll('[data-db-fixed]');
                    btns.forEach(function(btn) {
                        btn.removeAttribute('data-db-fixed');
                        btn.removeAttribute('data-db-listener');
                        btn.style.cssText = ''; // 인라인 스타일 초기화
                    });
                }

                // ============================================================
                // 🕵️‍♀️ 하단 버튼 강력 고정 (반복 적용)
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

                    if (targetBtn) {
                        // 1. 나중에 치우기 위해 표시
                        if (!targetBtn.getAttribute('data-db-fixed')) {
                            targetBtn.setAttribute('data-db-fixed', 'true');
                        }

                        // ⭐ [핵심 변경] 스타일은 매번 강제로 재적용 (웹앱이 덮어쓰는 것 방지)
                        targetBtn.style.setProperty('position', 'fixed', 'important');
                        targetBtn.style.setProperty('bottom', '30px', 'important');
                        targetBtn.style.setProperty('left', '20px', 'important');
                        targetBtn.style.setProperty('right', '20px', 'important');
                        targetBtn.style.setProperty('width', 'auto', 'important'); // left/right에 맞춰 자동 조절
                        targetBtn.style.setProperty('margin', '0', 'important');
                        targetBtn.style.setProperty('padding', '0', 'important'); // 혹시 모를 패딩 제거
                        targetBtn.style.setProperty('box-sizing', 'border-box', 'important');
                        targetBtn.style.setProperty('z-index', '2147483647', 'important');
                        targetBtn.style.setProperty('pointer-events', 'auto', 'important');
                        targetBtn.style.setProperty('touch-action', 'manipulation', 'important');
                        targetBtn.style.setProperty('cursor', 'pointer', 'important');

                        // 자식 요소 클릭 보장
                        Array.from(targetBtn.querySelectorAll('*')).forEach(function(child) {
                            child.style.setProperty('pointer-events', 'auto', 'important');
                        });
                        
                        // 2. 클릭 리스너는 딱 한 번만 연결
                        if (!targetBtn.getAttribute('data-db-listener')) {
                            targetBtn.setAttribute('data-db-listener', 'true');
                            targetBtn.addEventListener('click', function() {
                                log('🖱️ 버튼 클릭됨 -> 뒷정리 예약');
                                // 화면 전환 시간을 고려해 약간 딜레이 후 정리
                                setTimeout(cleanUp, 300); 
                            });
                        }
                    }
                }

                // ============================================================
                // 🎨 스타일 주입
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
                      'html, body { background: #FFFFFF !important; margin: 0 !important; padding: 0 !important; width: 100% !important; height: 100% !important; overflow-x: hidden !important; }' +
                      'body { padding-top: ' + TOP_PX + 'px !important; padding-bottom: 90px !important; }' +
                      '#root, #__next, main { display: flex !important; flex-direction: column !important; justify-content: center !important; align-items: center !important; width: 100% !important; height: 100% !important; overflow: visible !important; }' +
                      'h1, h2, h3, h4, h5, h6, p, span, div[class*="text"] { text-align: center !important; }' +
                      
                      // 이미지 확대 (SVG 아이콘 제외)
                      'img:not(.icon):not([class*="icon"]) { width: 95vw !important; max-width: none !important; height: auto !important; display: block !important; margin-left: 50% !important; transform: translateX(-50%) !important; pointer-events: none !important; z-index: 0 !important; }' +
                      // 아이콘/SVG 보호
                      'svg { max-width: 100% !important; width: auto !important; height: auto !important; margin: 0 auto !important; z-index: 1 !important; transform: none !important; pointer-events: none !important; }' +
                      // 입력폼 보호
                      'input, form, label { text-align: left !important; opacity: 1 !important; visibility: visible !important; display: block !important; pointer-events: auto !important; }' +
                      // 리스트 정렬
                      'ul, ol { padding-left: 0 !important; margin-left: auto !important; margin-right: auto !important; text-align: center !important; pointer-events: none !important; }';

                    fixBottomButton();
                }

                if (!window.__db_onboarding_timer__) {
                    window.__db_onboarding_timer__ = setInterval(applyStyle, 300);
                }
                applyStyle();

              } catch(e) { }
            })();
        """.trimIndent()
    }
}
