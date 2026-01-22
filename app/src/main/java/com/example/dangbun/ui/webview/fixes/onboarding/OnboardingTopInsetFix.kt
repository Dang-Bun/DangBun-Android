package com.example.dangbun.ui.webview.fixes.onboarding

import android.webkit.WebView

internal object OnboardingTopInsetFix {
    // ✅ 기본값을 0으로 변경하여 상단 여백 최소화
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
                var STYLE_ID = '__db_onboarding_top_inset_fix__';
                var TOP_PX = $topPx;

                function isOnboarding() {
                  try {
                    var path = (location.pathname || '').toLowerCase();
                    return path.indexOf('onboarding') >= 0;
                  } catch(e) { return false; }
                }

                function removeStyle() {
                  try {
                    var old = document.getElementById(STYLE_ID);
                    if (old && old.parentNode) old.parentNode.removeChild(old);
                  } catch(e) {}
                }

                // ✅ 다른 화면의 스타일 완전 제거 (독립성 보장)
                function cleanupOtherScreens() {
                  try {
                    // 다른 화면의 스타일 제거
                    var otherStyleIds = [
                      '__db_placemake1_top_inset_fix__',
                      '__db_placemake2_top_inset_fix__',
                      '__db_placemake3_top_inset_fix__',
                      '__db_addplace_css_hack_final__',
                      '__db_addplace_gray_bg__',
                      '__db_gray_topband_killer__'
                    ];
                    for (var i = 0; i < otherStyleIds.length; i++) {
                      var styleEl = document.getElementById(otherStyleIds[i]);
                      if (styleEl && styleEl.parentNode) {
                        styleEl.parentNode.removeChild(styleEl);
                      }
                    }
                    
                    // 다른 화면의 클래스 제거
                    var otherClasses = [
                      'db-back-button-fixed',
                      'db-next-button-fixed',
                      'db-placemake2-content-raise',
                      'db-next-btn-moved-to-body',
                      'db-force-content-pos'
                    ];
                    for (var j = 0; j < otherClasses.length; j++) {
                      var elements = document.querySelectorAll('.' + otherClasses[j]);
                      for (var k = 0; k < elements.length; k++) {
                        try {
                          elements[k].classList.remove(otherClasses[j]);
                        } catch(e) {}
                      }
                    }
                    
                    // 다른 화면의 음수 margin 제거
                    var mainElements = document.querySelectorAll('main, #root, #__next');
                    for (var l = 0; l < mainElements.length; l++) {
                      var el = mainElements[l];
                      var computedStyle = window.getComputedStyle(el);
                      var marginTop = computedStyle.marginTop;
                      if (marginTop && (marginTop.indexOf('-') >= 0 || parseFloat(marginTop) < -10)) {
                        el.style.setProperty('margin-top', '0', 'important');
                      }
                    }
                    
                    // overflow, height 초기화 (하단 버튼이 보이도록)
                    var bodyElements = document.querySelectorAll('html, body, #root, #__next, main');
                    for (var m = 0; m < bodyElements.length; m++) {
                      var elem = bodyElements[m];
                      if (elem.tagName === 'HTML' || elem.tagName === 'BODY') {
                        elem.style.setProperty('overflow-y', 'auto', 'important');
                        elem.style.setProperty('overflow-x', 'auto', 'important');
                        elem.style.setProperty('height', 'auto', 'important');
                        elem.style.setProperty('max-height', 'none', 'important');
                        elem.style.setProperty('touch-action', 'auto', 'important');
                        elem.style.setProperty('overscroll-behavior', 'auto', 'important');
                      } else {
                        elem.style.setProperty('overflow', 'visible', 'important');
                        elem.style.setProperty('height', 'auto', 'important');
                        elem.style.setProperty('max-height', 'none', 'important');
                      }
                    }
                  } catch(e) {}
                }
                
                function applyStyle() {
                  try {
                    // ✅ 온보딩 아니면 무조건 해제
                    if (!isOnboarding()) {
                      removeStyle();
                      return;
                    }

                    // ✅ 다른 화면의 스타일 제거
                    cleanupOtherScreens();

                    var style = document.getElementById(STYLE_ID);
                    if (!style) {
                      style = document.createElement('style');
                      style.id = STYLE_ID;
                      document.head.appendChild(style);
                    }

                    // ✅ 상단 여백 최소화 - 모든 요소의 padding-top 제거
                    style.textContent = 
                      'html, body, #root, #__next, main {' +
                        'background: #FFFFFF !important;' +
                        'margin: 0 !important;' +
                        'padding: 0 !important;' +
                        'padding-top: 0 !important;' +
                        'overflow-y: auto !important;' +
                        'overflow-x: auto !important;' +
                        'height: auto !important;' +
                        'max-height: none !important;' +
                        'touch-action: auto !important;' +
                      '}' +
                      'body {' +
                        'padding-top: ' + TOP_PX + 'px !important;' +
                      '}' +
                      // ✅ 건너뛰기 버튼이 보이도록 추가 여백 제거
                      'header, nav, [role="banner"], [class*="Header"], [class*="header"], [class*="AppBar"], [class*="appbar"] {' +
                        'padding-top: 0 !important;' +
                        'margin-top: 0 !important;' +
                      '}' +
                      // ✅ 하단 버튼이 보이도록 보장
                      'button, a[role="button"], [class*="Button"], [class*="button"] {' +
                        'position: relative !important;' +
                        'bottom: auto !important;' +
                        'top: auto !important;' +
                        'visibility: visible !important;' +
                        'opacity: 1 !important;' +
                        'display: block !important;' +
                      '}';
                  } catch(e) {
                    console.log('ONBOARDING_TOP_INSET_FIX_ERR', e && e.message);
                  }
                }

                // ✅ SPA 대응: 주기적으로 라우터 보고 적용/해제
                applyStyle();
                if (!window.__db_onboarding_inset_timer__) {
                  window.__db_onboarding_inset_timer__ = setInterval(applyStyle, 300);
                }

              } catch(e) {
                console.log('ONBOARDING_TOP_INSET_FIX_ERR', e && e.message);
              }
            })();
            """.trimIndent()
    }
}
