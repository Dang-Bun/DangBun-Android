package com.example.dangbun.ui.webview.fixes.myplace

import android.webkit.WebView
import com.example.dangbun.ui.webview.fixes.addplace.MyPlaceAddFix

internal object MyPlaceFix {
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
                ${MyPlaceBackgroundFix.provideJs()}
                
                // 2) 등록된 함수들을 순서대로 실행 (리스트는 항상 마지막)
                function applyAll() {
                  try { window.__dbApplyMyPlaceHeaderFix && window.__dbApplyMyPlaceHeaderFix(); } catch(e) {}
                  try { window.__dbApplyMyPlaceDetailFix && window.__dbApplyMyPlaceDetailFix(); } catch(e) {}
                  try { window.__dbApplyMyPlaceAddFix && window.__dbApplyMyPlaceAddFix(); } catch(e) {}
                  try { window.__dbApplyMyPlaceBackgroundFix && window.__dbApplyMyPlaceBackgroundFix(); } catch(e) {}
                  try { window.__dbApplyMyPlaceListFix && window.__dbApplyMyPlaceListFix(); } catch(e) {}
                  
                  // ✅ DEBUG: 리스트가 DOM에 실제로 생성되는지 확인
                  try {
                    var cards = document.querySelectorAll('a[href*="place"], [data-testid*="place"], .place, .place-card');
                    (function(){
                      try {
                        var now = Date.now();
                        var last = window.__db_myplace_debug_last_ts__ || 0;
                        if (now - last < 1000) return;   // ✅ 1초 이내면 로그 스킵
                        window.__db_myplace_debug_last_ts__ = now;

                        console.log('[MYPLACE_DEBUG] cards=', cards.length, 'path=', location.pathname);
                      } catch(e) {}
                    })();

                  } catch(e) {}
                }
                function scheduleApplyAll() {
                  try {
                    if (window.__db_myplace_apply_scheduled__) return;
                    window.__db_myplace_apply_scheduled__ = true;
                    setTimeout(function() {
                      window.__db_myplace_apply_scheduled__ = false;
                      applyAll();
                    }, 60);
                  } catch(e) {}
                }


                applyAll();
                setTimeout(applyAll, 80);
                setTimeout(applyAll, 200);
                setTimeout(applyAll, 450);

                // 3) ✅ 관찰자는 1개만 (중복 적용 방지)
                if (!window.__db_myplace_unified_ob__) {
                  window.__db_myplace_unified_ob__ = new MutationObserver(function(){ scheduleApplyAll(); });
                  window.__db_myplace_unified_ob__.observe(document.documentElement, { childList: true, subtree: true });
                }

                window.addEventListener('popstate', function(){ scheduleApplyAll(); });
                window.addEventListener('resize', function(){ scheduleApplyAll(); });

              } catch(e) {}
            })();
            """.trimIndent()

        view.evaluateJavascript(js, null)
    }
}
