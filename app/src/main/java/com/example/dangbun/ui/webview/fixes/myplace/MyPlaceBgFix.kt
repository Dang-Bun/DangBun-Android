package com.example.dangbun.ui.webview.fixes.myplace

internal object MyPlaceBgFix {
    // ✅ MyPlaceFix에서 "함수 등록"만 하고, 실제 실행은 applyAll()에서 함
    internal fun provideJs(): String {
        return """
            (function() {
              try {
                if (window.__db_myplace_bg_fix_installed__) return;
                window.__db_myplace_bg_fix_installed__ = true;

                window.__dbApplyMyPlaceBgFix = function() {
                  try {
                    // ✅ myplace 라우터에서만 적용 (다른 화면 영향 방지)
                    var path = (location.pathname || '').toLowerCase();
                    if (path.indexOf('myplace') < 0) return;

                    var GRAY_BG = '#F5F6F8';
                    var styleId = '__db_myplace_bg_fix__';

                    var style = document.getElementById(styleId);
                    if (!style) {
                      style = document.createElement('style');
                      style.id = styleId;
                      document.head.appendChild(style);
                    }

                    // ✅ "WebView 뒤"가 아니라 "웹 페이지 자체" 배경을 회색으로 강제
                    style.innerHTML = `
                      html, body {
                        background: ${'$'}{GRAY_BG} !important;
                      }
                      #__next, #root, main {
                        background: ${'$'}{GRAY_BG} !important;
                      }

                      /* 혹시 최상위 래퍼가 흰색을 덮는 경우 대비 */
                      body > div, main > div {
                        background: transparent !important;
                      }
                    `;
                  } catch(e) {
                    console.log('MYPLACE_BG_FIX_ERR', e && e.message);
                  }
                };
              } catch(e) {}
            })();
            """.trimIndent()
    }
}
