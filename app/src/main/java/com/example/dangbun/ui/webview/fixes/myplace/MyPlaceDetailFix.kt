package com.example.dangbun.ui.webview.fixes.myplace

import android.webkit.WebView

internal object MyPlaceDetailFix {
    internal fun inject(view: WebView) {
        view.evaluateJavascript(provideJs(), null)
        view.evaluateJavascript("try{window.__dbApplyMyPlaceDetailFix&&window.__dbApplyMyPlaceDetailFix();}catch(e){}", null)
    }

    internal fun provideJs(): String {
        return """
            (function() {
              try {
                window.__dbApplyMyPlaceDetailFix = function() {
                  try {
                    // TODO
                  } catch(e) {}
                };
              } catch(e) {}
            })();
            """.trimIndent()
    }
}
