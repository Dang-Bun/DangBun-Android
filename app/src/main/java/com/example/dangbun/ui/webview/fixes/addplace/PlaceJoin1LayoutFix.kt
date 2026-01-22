package com.example.dangbun.ui.webview.fixes.addplace

import android.webkit.WebView
import com.example.dangbun.ui.webview.fixes.common.ResponsiveUtils

internal object PlaceJoin1LayoutFix {

    /**
     * raisePx: "참여 코드 입력" 영역(제목+입력+확인)이 들어있는 래퍼를 위로 올림 (클수록 더 위로)
     * liftBottomPx: "완료" 버튼(하단 고정 버튼)을 위로 올림 (클수록 더 위로)
     */
    internal fun inject(
        view: WebView,
        raisePx: Int = 140,
        liftBottomPx: Int = 24,
    ) {
        view.evaluateJavascript(provideJs(raisePx, liftBottomPx), null)
    }

    private fun provideJs(raisePx: Int, liftBottomPx: Int): String {
        return """
        (function() {
          // ✅ 반응형 유틸리티 로드
          if (!window.__dangbun_responsive_utils__) {
            ${ResponsiveUtils.getResponsiveJs()}
          }

          var STYLE_ID = '__db_placejoin1_layout_fix__';
          var CLASS_TOP = 'db-placejoin1-top-raised';
          var HTML_ATTR = 'data-db-placejoin1';
          var BASE_RAISE_PX = ${raisePx};
          var BASE_LIFT_BOTTOM_PX = ${liftBottomPx};

          function isPlaceJoin1() {
            try {
              var path = (location.pathname || '').toLowerCase();
              return (path.indexOf('placejoin1') >= 0);
            } catch(e) { return false; }
          }

          function hasTargetText() {
            try {
              var text = (document.body && document.body.innerText) ? document.body.innerText : '';
              text = text.replace(/\s/g,'');
              return (text.indexOf('참여코드를입력해주세요.') >= 0);
            } catch(e) { return false; }
          }

          function ensureStyle() {
            var style = document.getElementById(STYLE_ID);
            if (!style) {
              style = document.createElement('style');
              style.id = STYLE_ID;
              document.head.appendChild(style);
            }

            // ✅ 화면 크기에 따라 동적으로 계산
            var responsiveRaisePx = window.getResponsivePx ? window.getResponsivePx(BASE_RAISE_PX, 'height') : BASE_RAISE_PX;
            var responsiveLiftPx = window.getResponsivePx ? window.getResponsivePx(BASE_LIFT_BOTTOM_PX, 'height') : BASE_LIFT_BOTTOM_PX;

            // ✅ template literal(백틱) 안 쓰고 + 로만 구성 (SyntaxError 방지)
            style.textContent =
              /* 1) 참여코드 영역 올리기 */
              '.' + CLASS_TOP + '{' +
                'margin-top:-' + responsiveRaisePx + 'px !important;' +
              '}' +

              /* 2) "완료" 하단 버튼 위로 올리기 (PlaceJoin1에서만) */
              'html[' + HTML_ATTR + '="1"] button[style*="position: fixed"][style*="bottom"],' +
              'html[' + HTML_ATTR + '="1"] div[style*="position: fixed"][style*="bottom"],' +
              'html[' + HTML_ATTR + '="1"] a[style*="position: fixed"][style*="bottom"],' +
              'html[' + HTML_ATTR + '="1"] [class*="bottom"][class*="fixed"],' +
              'html[' + HTML_ATTR + '="1"] [class*="Bottom"][class*="Fixed"],' +
              'html[' + HTML_ATTR + '="1"] [class*="sticky"][class*="bottom"],' +
              'html[' + HTML_ATTR + '="1"] [class*="Sticky"][class*="Bottom"]' +
              '{ transform: translateY(-' + responsiveLiftPx + 'px) !important; }';
          }

          function removeStyle() {
            var style = document.getElementById(STYLE_ID);
            if (style && style.parentNode) style.parentNode.removeChild(style);
          }

          function cleanupTopClass() {
            try {
              var nodes = document.querySelectorAll('.' + CLASS_TOP);
              for (var i=0; i<nodes.length; i++) nodes[i].classList.remove(CLASS_TOP);
            } catch(e) {}
          }

          // ✅ "참여 코드를 입력해주세요." 기준으로
          //    input + "확인" 버튼이 같이 있는 컨테이너를 찾음
          function findTopWrapper() {
            try {
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
            } catch(e) {
              return null;
            }
          }

          function apply() {
            var active = isPlaceJoin1() && hasTargetText();

            if (!active) {
              try { document.documentElement.removeAttribute(HTML_ATTR); } catch(e) {}
              removeStyle();
              cleanupTopClass();
              return;
            }

            try { document.documentElement.setAttribute(HTML_ATTR, "1"); } catch(e) {}
            ensureStyle();

            var w = findTopWrapper();
            if (w && !w.classList.contains(CLASS_TOP)) {
              w.classList.add(CLASS_TOP);
              var responsiveRaisePx = window.getResponsivePx ? window.getResponsivePx(BASE_RAISE_PX, 'height') : BASE_RAISE_PX;
              var responsiveLiftPx = window.getResponsivePx ? window.getResponsivePx(BASE_LIFT_BOTTOM_PX, 'height') : BASE_LIFT_BOTTOM_PX;
              console.log('PLACEJOIN1_LAYOUT_APPLIED', 'raisePx=' + responsiveRaisePx, 'liftBottomPx=' + responsiveLiftPx);
            }
          }

          // ✅ 화면 크기 변경 시에도 재계산되도록 리사이즈 이벤트 추가
          if (!window.__dangbun_placejoin1_resize_handler__) {
              window.__dangbun_placejoin1_resize_handler__ = true;
              var resizeTimer;
              window.addEventListener('resize', function() {
                  clearTimeout(resizeTimer);
                  resizeTimer = setTimeout(function() {
                      if (isPlaceJoin1() && hasTargetText()) {
                          ensureStyle(); // 스타일 재계산
                          apply(); // 다시 적용
                      }
                  }, 100);
              });
          }

          apply();
          setInterval(apply, 300);
        })();
        """.trimIndent()
    }
}
