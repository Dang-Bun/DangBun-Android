package com.example.dangbun.ui.webview.fixes.addplace

import android.webkit.WebView

internal object PlaceMake3TopInsetFix {

    // downPx: 아래로 내릴 px (클수록 더 아래로)
    internal fun inject(view: WebView, downPx: Int = 120) {
        view.evaluateJavascript(provideJs(downPx), null)
    }

    private fun provideJs(downPx: Int): String {
        return """
        (function() {
          try {
            var STYLE_ID = '__db_placemake3_top_inset_fix__';
            var CLASS_NAME = 'db-placemake3-content-down';
            var HTML_ATTR = 'data-db-placemake3';

            function isPlaceMake3() {
              try {
                var path = (location.pathname || '').toLowerCase();
                return (path.indexOf('placemake3') >= 0);
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
                /* ✅ placemake3에서만 적용 */
                html[${'$'}{HTML_ATTR}="1"] .${'$'}{CLASS_NAME} {
                  margin-top: ${downPx}px !important; /* ✅ 아래로 내리기 */
                }
              `;
            }

            function removeStyle() {
              var style = document.getElementById(STYLE_ID);
              if (style && style.parentNode) style.parentNode.removeChild(style);
            }

            function removeAppliedClass() {
              try {
                var nodes = document.querySelectorAll('.' + CLASS_NAME);
                for (var i=0; i<nodes.length; i++) nodes[i].classList.remove(CLASS_NAME);
              } catch(e) {}
            }

            // ✅ placemake3는 "생성 완료" 같은 결과 화면이라 입력폼이 없을 수 있음
            // 그래서 main 내부에서 가장 큰 컨테이너(세로로 넓은 블록)를 골라 내려줌
            function findContentWrapper() {
              try {
                var root = document.querySelector('main') || document.querySelector('#__next') || document.body;
                if (!root) return null;

                var candidates = root.querySelectorAll('div, section, article');
                var best = null;
                var bestScore = -1;

                for (var i=0; i<candidates.length; i++) {
                  var el = candidates[i];
                  var r = el.getBoundingClientRect();

                  // 화면에 실제로 보이고, 적당히 큰 블록만 후보
                  if (r.width < window.innerWidth * 0.6) continue;
                  if (r.height < 200) continue;
                  if (r.top < 0 || r.top > window.innerHeight) continue;

                  var score = r.height * r.width;
                  if (score > bestScore) {
                    bestScore = score;
                    best = el;
                  }
                }

                return best || root;
              } catch(e) {
                return null;
              }
            }

            function manage() {
              var active = isPlaceMake3();

              if (!active) {
                try { document.documentElement.removeAttribute(HTML_ATTR); } catch(e) {}
                removeAppliedClass();
                removeStyle();
                return;
              }

              try { document.documentElement.setAttribute(HTML_ATTR, "1"); } catch(e) {}
              ensureStyle();

              var w = findContentWrapper();
              if (w && !w.classList.contains(CLASS_NAME)) {
                w.classList.add(CLASS_NAME);
                console.log('PLACEMAKE3_FIX_APPLIED', 'downPx=' + ${downPx});
              }
            }

            manage();
            setInterval(manage, 300);

          } catch(e) {
            console.log('PLACEMAKE3_TOP_INSET_FIX_ERR', e && e.message);
          }
        })();
        """.trimIndent()
    }
}
