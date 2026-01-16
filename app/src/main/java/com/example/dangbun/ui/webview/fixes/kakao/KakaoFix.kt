package com.example.dangbun.ui.webview.fixes.kakao

import android.webkit.WebView

internal object KakaoFix {
    internal fun injectKakaoLtrFix(view: WebView) {
        val js = """
        (function() {
          try {
            var style = document.getElementById('__dangbun_kakao_ltr__');
            if (!style) {
              style = document.createElement('style');
              style.id = '__dangbun_kakao_ltr__';
              style.innerHTML = `
                html, body { direction: ltr !important; }
                input, textarea {
                  direction: ltr !important;
                  unicode-bidi: plaintext !important;
                  text-align: left !important;
                }
              `;
              document.head.appendChild(style);
            }
          } catch (e) {}
        })();
    """.trimIndent()

        view.evaluateJavascript(js, null)
    }
}
