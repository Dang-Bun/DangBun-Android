package com.example.dangbun.ui.webview.fixes.onboarding

import android.webkit.WebView

internal object OnboardingTopInsetFix {
    internal fun inject(view: WebView, topPx: Int = 24) {
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

                // ✅ body padding만 사용 (root/top 이동은 제거)
                style.textContent = `
                  html, body, #root, #__next, main {
                    background: #FFFFFF !important;
                    margin: 0 !important;
                    padding: 0 !important;
                  }

                  body {
                    padding-top: ${topPx}px !important;
                  }
                `;
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
