package com.example.dangbun.ui.webview.fixes.onboarding

import android.webkit.WebView

internal object OnboardingTopInsetFix {
    internal fun inject(view: WebView, topPx: Int = 180) {
        view.evaluateJavascript(provideJs(topPx), null)
    }

    private fun provideJs(topPx: Int): String {
        return """
        (function() {
          try {
            var styleId = '__db_onboarding_top_inset_fix__';
            var style = document.getElementById(styleId);
            if (!style) {
              style = document.createElement('style');
              style.id = styleId;
              document.head.appendChild(style);
            }

            // ✅ 전체 화면이 위로 붙는 현상 완화: 상단 여백 추가
            style.innerHTML = `
              /* ✅ 온보딩 화면 배경은 흰색으로만 통일 (버튼/카드 배경은 건드리지 않음) */
              html, body, #root, #__next, main {
                background-color: #FFFFFF !important;
              }

              /* ✅ 상단 여백(위로 말려 올라가는 현상 완화) */
              body {
                padding-top: ${'$'}{'$'}{topPx}px !important;
              }

              main, #root, #__next {
                position: relative !important;
                top: ${'$'}{'$'}{topPx}px !important;
              }
            `;


          } catch(e) {
            console.log('ONBOARDING_TOP_INSET_FIX_ERR', e && e.message);
          }
        })();
        """.trimIndent()
    }
}
