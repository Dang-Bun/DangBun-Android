package com.example.dangbun.ui.webview.fixes.addplace

import android.webkit.WebView

internal object PlaceMake2TopInsetFix {

    internal fun inject(view: WebView, raisePx: Int = 120) {
        view.evaluateJavascript(provideJs(raisePx), null)
    }

    private fun provideJs(raisePx: Int): String {
        return """
        (function() {
          try {
            var STYLE_ID = '__db_placemake2_top_inset_fix__';
            var CLASS_NAME = 'db-placemake2-content-raise';

            function isPlaceMake2() {
              try {
                var path = (location.pathname || '').toLowerCase();
                return (path.indexOf('placemake2') >= 0);
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
                /* ✅ 가운데 정렬(세로 중앙) 원인 제거: flex면 상단 정렬로 강제 */
                html, body, #root, #__next, main {
                  align-items: flex-start !important;
                  justify-content: flex-start !important;
                }

                /* ✅ 실제 콘텐츠 래퍼만 위로 당김 */
                .${'$'}{CLASS_NAME} {
                  margin-top: -${raisePx}px !important;
                }
              `;
            }

            function removeStyle() {
              var style = document.getElementById(STYLE_ID);
              if (style && style.parentNode) style.parentNode.removeChild(style);
            }

            function cleanupClass() {
              try {
                var nodes = document.querySelectorAll('.' + CLASS_NAME);
                for (var i = 0; i < nodes.length; i++) {
                  nodes[i].classList.remove(CLASS_NAME);
                }
              } catch(e) {}
            }

            // ✅ "정보를 작성해주세요." 타이틀을 기준으로 콘텐츠 시작 래퍼 찾기
            function findContentWrapper() {
              try {
                var root = document.querySelector('main') || document.querySelector('#__next') || document.body;
                if (!root) return null;

                // 1) 타이틀 텍스트 기준 탐색
                var all = root.querySelectorAll('h1,h2,h3,div,p,span');
                for (var i = 0; i < all.length; i++) {
                  var t = (all[i].innerText || '').replace(/\s/g,'');
                  if (t.indexOf('정보를작성해주세요.') >= 0) {
                    var cur = all[i];
                    for (var up = 0; up < 8; up++) {
                      if (!cur || !cur.parentElement) break;
                      cur = cur.parentElement;
                      var r = cur.getBoundingClientRect();
                      if (r.height > 200 && r.width > (window.innerWidth * 0.7)) return cur;
                    }
                    return all[i].parentElement || all[i];
                  }
                }

                // 2) fallback: 첫 input 기준
                var field = root.querySelector('input, textarea, select');
                if (!field) return null;

                var cur2 = field;
                for (var j = 0; j < 8; j++) {
                  if (!cur2 || !cur2.parentElement) break;
                  cur2 = cur2.parentElement;
                  var rr = cur2.getBoundingClientRect();
                  if (rr.height > 200 && rr.width > (window.innerWidth * 0.7)) return cur2;
                }
                return field.parentElement || field;

              } catch(e) {
                return null;
              }
            }

            function apply() {
              var active = isPlaceMake2();

              if (!active) {
                removeStyle();
                cleanupClass();
                return;
              }

              ensureStyle();

              var w = findContentWrapper();
              if (w && !w.classList.contains(CLASS_NAME)) {
                w.classList.add(CLASS_NAME);
                console.log('PLACEMAKE2_FIX_APPLIED', 'raisePx=' + ${raisePx});
              }
            }

            apply();
            setInterval(apply, 300);

          } catch(e) {
            console.log('PLACEMAKE2_TOP_INSET_FIX_ERR', e && e.message);
          }
        })();
        """.trimIndent()
    }
}
