package com.example.dangbun.ui.webview.fixes.addplace

import android.webkit.WebView

internal object MyPlaceAddFix {

    internal fun inject(view: WebView) {
        view.evaluateJavascript(provideJs(), null)
        // ✅ 등록된 함수를 바로 실행(없어도 안전)
        view.evaluateJavascript(
            "try{window.__dbApplyMyPlaceAddFix&&window.__dbApplyMyPlaceAddFix();}catch(e){}",
            null
        )
    }

    internal fun provideJs(): String {
        return """
            (function() {
              try {
                // ...
                window.__dbApplyMyPlaceAddFix = function() {
                  try {
                    // ...
                  } catch(e) {}
                };
              } catch(e) {}
            })();
        """.trimIndent()
    }
}
