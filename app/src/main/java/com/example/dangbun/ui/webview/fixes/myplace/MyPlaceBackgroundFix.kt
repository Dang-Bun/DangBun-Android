package com.example.dangbun.ui.webview.fixes.myplace

internal object MyPlaceBackgroundFix {
    internal fun provideJs(): String {
        return """
            (function() {
              // ✅ 등록만: MyPlaceFix에서 호출됨
              window.__dbApplyMyPlaceBackgroundFix = function() {
                try {
                  var path = (location.pathname || '');
                  if (path.toLowerCase().indexOf('myplace') < 0) return;

                  var styleId = '__db_myplace_bg_gray__';
                  var style = document.getElementById(styleId);
                  if (!style) {
                    style = document.createElement('style');
                    style.id = styleId;
                    document.head.appendChild(style);
                  }

                  style.innerHTML = `
                    html, body {
                      background: #F5F6F8 !important;
                    }
                    #root, #__next, main {
                      background: #F5F6F8 !important;
                    }

                    /* ✅ 혹시 최상위 컨테이너가 따로 배경을 들고 있으면 같이 눌러버림 */
                    body > div, #__next > div {
                      background: transparent !important;
                    }
                  `;
                } catch(e) {}
              };
            })();
            """.trimIndent()
    }
}
