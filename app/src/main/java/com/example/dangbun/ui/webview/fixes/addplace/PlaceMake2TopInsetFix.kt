package com.example.dangbun.ui.webview.fixes.addplace

import android.webkit.WebView
import com.example.dangbun.ui.webview.fixes.common.ResponsiveUtils

internal object PlaceMake2TopInsetFix {

    internal fun inject(view: WebView, raisePx: Int = 120) {
        view.evaluateJavascript(provideJs(raisePx), null)
    }

    private fun provideJs(raisePx: Int): String {
        return """
        (function() {
          try {
            // ✅ 반응형 유틸리티 로드
            if (!window.__dangbun_responsive_utils__) {
              ${ResponsiveUtils.getResponsiveJs()}
            }

            var STYLE_ID = '__db_placemake2_top_inset_fix__';
            var CLASS_NAME = 'db-placemake2-content-raise';
            var BASE_RAISE_PX = ${raisePx};

            // ✅ 완료 버튼을 "조금만" 위로 올리는 값 (반응형)
            var COMPLETE_RAISE_PX_BASE = 24;
            var COMPLETE_APPLIED_ATTR = 'data-db-complete-raised';

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

              // ✅ 화면 크기에 따라 동적으로 계산
              var responsivePx = window.getResponsivePx ? window.getResponsivePx(BASE_RAISE_PX, 'height') : BASE_RAISE_PX;

              style.textContent = 
                'html, body, #root, #__next, main {' +
                  'align-items: flex-start !important;' +
                  'justify-content: flex-start !important;' +
                '}' +
                '.' + CLASS_NAME + '{' +
                  'margin-top: -' + responsivePx + 'px !important;' +
                '}';
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

            function getText(el) {
              try {
                return (el.innerText || el.value || '').replace(/\s/g,'');
              } catch(e) { return ''; }
            }

            function findCompleteButton() {
              try {
                var candidates = document.querySelectorAll(
                  'button, a, [role="button"], input[type="button"], input[type="submit"]'
                );
                for (var i = 0; i < candidates.length; i++) {
                  var txt = getText(candidates[i]);
                  if (txt === '완료' || txt.indexOf('완료') >= 0) {
                    return candidates[i];
                  }
                }
                return null;
              } catch(e) {
                return null;
              }
            }

            function findFixedParent(el) {
              try {
                var cur = el;
                for (var i = 0; i < 10; i++) {
                  if (!cur || !cur.parentElement) break;
                  cur = cur.parentElement;

                  var cs = window.getComputedStyle(cur);
                  var pos = cs ? cs.position : '';
                  if (pos === 'fixed' || pos === 'sticky') {
                    // bottom이 0 근처인 footer 후보
                    return cur;
                  }
                }
                return null;
              } catch(e) {
                return null;
              }
            }

            function raiseCompleteArea() {
              try {
                var btn = findCompleteButton();
                if (!btn) return;

                // 이미 적용했으면 스킵
                if (btn.getAttribute(COMPLETE_APPLIED_ATTR) === '1') return;

                // ✅ 반응형 계산
                var completeRaisePx = window.getResponsivePx ? window.getResponsivePx(COMPLETE_RAISE_PX_BASE, 'height') : COMPLETE_RAISE_PX_BASE;

                // 1) 버튼 자체를 살짝 위로
                btn.style.transform = 'translateY(-' + completeRaisePx + 'px)';
                btn.style.willChange = 'transform';
                btn.setAttribute(COMPLETE_APPLIED_ATTR, '1');

                // 2) fixed 부모가 있으면 부모도 같이 올려서 "바닥에 딱 붙는 느낌" 완화
                var fixedParent = findFixedParent(btn);
                if (fixedParent && fixedParent.getAttribute(COMPLETE_APPLIED_ATTR) !== '1') {
                  // bottom 값을 올리거나, transform으로 올림(둘 중 하나만)
                  fixedParent.style.transform = 'translateY(-' + completeRaisePx + 'px)';
                  fixedParent.style.willChange = 'transform';
                  fixedParent.setAttribute(COMPLETE_APPLIED_ATTR, '1');
                }

                console.log('PLACEMAKE2_COMPLETE_RAISED', 'px=' + completeRaisePx);
              } catch(e) {}
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

              // ✅ 완료 버튼/하단영역도 같이 올리기
              raiseCompleteArea();
            }

            // ✅ 화면 크기 변경 시에도 재계산되도록 리사이즈 이벤트 추가
            if (!window.__dangbun_placemake2_resize_handler__) {
                window.__dangbun_placemake2_resize_handler__ = true;
                var resizeTimer;
                window.addEventListener('resize', function() {
                    clearTimeout(resizeTimer);
                    resizeTimer = setTimeout(function() {
                        if (isPlaceMake2()) {
                            ensureStyle(); // 스타일 재계산
                            apply(); // 다시 적용
                        }
                    }, 100);
                });
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
