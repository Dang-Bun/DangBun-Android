package com.example.dangbun.ui.webview.fixes.addplace

import android.webkit.WebView

internal object PlaceMake1TopInsetFix {

    internal fun inject(view: WebView, raisePx: Int = 30) {
        view.evaluateJavascript(provideJs(raisePx), null)
    }

    private fun provideJs(raisePx: Int): String {
        return """
        (function() {
          try {
            var STYLE_ID = '__db_placemake1_top_inset_fix__';
            var CLASS_NAME = 'db-placemake1-content-raise';

            function isPlaceMake1() {
              try {
                var path = (location.pathname || '').toLowerCase();
                return (path.indexOf('placemake1') >= 0);
              } catch(e) { return false; }
            }

            function ensureStyle() {
              var style = document.getElementById(STYLE_ID);
              if (!style) {
                style = document.createElement('style');
                style.id = STYLE_ID;
                document.head.appendChild(style);
              }

              style.textContent = `
                .${'$'}{CLASS_NAME} {
                  margin-top: -${raisePx}px !important;  /* ✅ 헤더 아래 콘텐츠만 위로 당김 */
                }
              `;
            }

            // ✅ 헤더(뒤로가기) 아래 "콘텐츠 시작점"을 잡기:
            // main 안에서 첫 input/textarea/select를 찾고, 그 근처 상위 컨테이너를 올림
            function findContentWrapper() {
              try {
                var root = document.querySelector('main') || document.querySelector('#__next') || document.body;
                if (!root) return null;

                var field = root.querySelector('input, textarea, select');
                if (!field) return null;

                // 적당한 컨테이너 레벨까지 올라감 (너무 위로 올라가면 헤더까지 영향)
                var cur = field;
                for (var i = 0; i < 6; i++) {
                  if (!cur || !cur.parentElement) break;
                  cur = cur.parentElement;

                  // 화면에서 어느 정도 "블록"처럼 보이는 애를 선택
                  var r = cur.getBoundingClientRect();
                  if (r.height > 120 && r.width > (window.innerWidth * 0.6)) {
                    return cur;
                  }
                }

                return field.parentElement || field;
              } catch(e) {
                return null;
              }
            }

            function apply() {
              if (!isPlaceMake1()) return;

              ensureStyle();

              var w = findContentWrapper();
              if (w && !w.classList.contains(CLASS_NAME)) {
                w.classList.add(CLASS_NAME);
                console.log('PLACEMAKE1_FIX_APPLIED', 'raisePx=' + ${raisePx});
              }
            }

            apply();
            setInterval(apply, 300);

          } catch(e) {
            console.log('PLACEMAKE1_TOP_INSET_FIX_ERR', e && e.message);
          }
        })();
        """.trimIndent()
    }
}
