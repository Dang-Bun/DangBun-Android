package com.example.dangbun.ui.webview.fixes.onboarding

import android.webkit.WebView

internal object OnboardingTopInsetFix {
    // ✅ 기본값을 0으로 변경하여 상단 여백 최소화
    internal fun inject(view: WebView, topPx: Int = 0) {
        view.evaluateJavascript(provideJs(topPx), null)
    }

    private fun provideJs(topPx: Int): String {
        return """
        (function() {
          try {
            var STYLE_ID = '__db_onboarding_top_inset_fix__';
            var TOP_PX = ${topPx};

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

            function applyStyle() {
              try {
                // ✅ 온보딩 아니면 무조건 해제
                if (!isOnboarding()) {
                  removeStyle();
                  return;
                }

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
                  '}' +
                  'body {' +
                    'padding-top: ' + TOP_PX + 'px !important;' +
                  '}' +
                  // ✅ 건너뛰기 버튼이 보이도록 추가 여백 제거
                  'header, nav, [role="banner"], [class*="Header"], [class*="header"], [class*="AppBar"], [class*="appbar"] {' +
                    'padding-top: 0 !important;' +
                    'margin-top: 0 !important;' +
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
