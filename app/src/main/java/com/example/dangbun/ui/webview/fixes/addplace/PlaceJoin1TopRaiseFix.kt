package com.example.dangbun.ui.webview.fixes.addplace

import android.webkit.WebView

internal object PlaceJoin1TopRaiseFix {

    /**
     * raisePx: 위로 올릴 px (클수록 더 위로)
     */
    internal fun inject(view: WebView, raisePx: Int = 140) {
        view.evaluateJavascript(provideJs(raisePx), null)
    }

    private fun provideJs(raisePx: Int): String {
        return """
        (function() {
          var STYLE_ID = '__db_placejoin1_top_raise_fix__';
          var CLASS_NAME = 'db-placejoin1-top-raised';

          function isPlaceJoin1() {
            var path = (location.pathname || '').toLowerCase();
            return (path.indexOf('placejoin1') >= 0);
          }

          function hasTargetText() {
            var text = (document.body && document.body.innerText) ? document.body.innerText : '';
            text = text.replace(/\s/g,'');
            return (text.indexOf('참여코드를입력해주세요.') >= 0);
          }

          function ensureStyle() {
            var style = document.getElementById(STYLE_ID);
            if (!style) {
              style = document.createElement('style');
              style.id = STYLE_ID;
              document.head.appendChild(style);
            }
            style.textContent =
              '.' + CLASS_NAME + '{' +
                'margin-top:-' + ${raisePx} + 'px !important;' +
              '}';
          }

          function removeStyle() {
            var style = document.getElementById(STYLE_ID);
            if (style && style.parentNode) style.parentNode.removeChild(style);
          }

          function cleanupClass() {
            try {
              var nodes = document.querySelectorAll('.' + CLASS_NAME);
              for (var i=0; i<nodes.length; i++) nodes[i].classList.remove(CLASS_NAME);
            } catch(e) {}
          }

          // ✅ "참여 코드를 입력해주세요."를 기준으로 래퍼를 위로 탐색하면서
          //    input + "확인" 버튼이 같이 들어있는 컨테이너를 찾음
          function findWrapper() {
            var root = document.querySelector('main') || document.querySelector('#__next') || document.body;
            if (!root) return null;

            var all = root.querySelectorAll('h1,h2,h3,div,p,span');
            var titleEl = null;

            for (var i=0; i<all.length; i++) {
              var t = (all[i].innerText || '').replace(/\s/g,'');
              if (t.indexOf('참여코드를입력해주세요.') >= 0) { titleEl = all[i]; break; }
            }
            if (!titleEl) return null;

            var cur = titleEl;
            for (var up=0; up<10; up++) {
              if (!cur || !cur.parentElement) break;
              cur = cur.parentElement;

              var hasInput = !!cur.querySelector('input, textarea');
              var hasConfirm = false;

              var btns = cur.querySelectorAll('button, a, div, span');
              for (var b=0; b<btns.length; b++) {
                var bt = (btns[b].innerText || '').trim();
                if (bt === '확인') { hasConfirm = true; break; }
              }

              if (hasInput && hasConfirm) return cur;
            }

            return titleEl.parentElement || titleEl;
          }

          function apply() {
            if (!isPlaceJoin1() || !hasTargetText()) {
              removeStyle();
              cleanupClass();
              return;
            }

            ensureStyle();

            var w = findWrapper();
            if (w && !w.classList.contains(CLASS_NAME)) {
              w.classList.add(CLASS_NAME);
              console.log('PLACEJOIN1_TOP_RAISE_APPLIED', 'raisePx=' + ${raisePx});
            }
          }

          apply();
          setInterval(apply, 300);
        })();
        """.trimIndent()
    }
}
