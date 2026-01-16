package com.example.dangbun.ui.webview.fixes.myplace

import android.webkit.WebView
import com.example.dangbun.ui.webview.fixes.addplace.MyPlaceAddFix

internal object MyPlaceFix {

    /**
     * ✅ MyPlace 관련 fix들을 "한 번에" 주입하고,
     * ✅ 내부에서 applyAll()을 MutationObserver로 반복 실행해 값이 계속 유지되게 함.
     *
     * - 분리된 파일들의 provideJs()는 "함수 등록"만 하므로,
     *   여기서 라우터(applyAll + observer)를 반드시 만들어줘야 실제 적용됩니다.
     * - 리스트 Fix는 항상 마지막에 실행(수치 우선권)
     */
    internal fun inject(view: WebView) {
        val js =
            """
            (function() {
              try {
                // 1) 각 파일의 provideJs로 "함수 등록" 먼저
                ${MyPlaceHeaderFix.provideJs()}
                ${MyPlaceDetailFix.provideJs()}
                ${MyPlaceAddFix.provideJs()}
                ${MyPlaceListFix.provideJs()}

                // 2) 등록된 함수들을 순서대로 실행 (리스트는 항상 마지막)
                function applyAll() {
                  try { window.__dbApplyMyPlaceHeaderFix && window.__dbApplyMyPlaceHeaderFix(); } catch(e) {}
                  try { window.__dbApplyMyPlaceDetailFix && window.__dbApplyMyPlaceDetailFix(); } catch(e) {}
                  try { window.__dbApplyMyPlaceAddFix && window.__dbApplyMyPlaceAddFix(); } catch(e) {}
                  try { window.__dbApplyMyPlaceListFix && window.__dbApplyMyPlaceListFix(); } catch(e) {}
                }

                applyAll();
                setTimeout(applyAll, 80);
                setTimeout(applyAll, 200);
                setTimeout(applyAll, 450);

                // 3) ✅ 관찰자는 1개만 (중복 적용 방지)
                if (!window.__db_myplace_unified_ob__) {
                  window.__db_myplace_unified_ob__ = new MutationObserver(function(){ applyAll(); });
                  window.__db_myplace_unified_ob__.observe(document.documentElement, { childList: true, subtree: true });
                }

                window.addEventListener('popstate', function(){ setTimeout(applyAll, 120); });
                window.addEventListener('resize', function(){ setTimeout(applyAll, 60); });

              } catch(e) {}
            })();
            """.trimIndent()

        view.evaluateJavascript(js, null)
    }
}
