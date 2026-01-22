package com.example.dangbun.ui.webview.fixes.addplace

import android.webkit.WebView
import com.example.dangbun.ui.webview.fixes.common.ResponsiveUtils

internal object PlaceMake3TopInsetFix {
    // downPx: 아래로 내릴 px (클수록 더 아래로)
    internal fun inject(
        view: WebView,
        downPx: Int = 120,
    ) {
        view.evaluateJavascript(provideJs(downPx), null)
    }

    private fun provideJs(downPx: Int): String {
        return """
            (function() {
              try {
                // ✅ 반응형 유틸리티 로드
                if (!window.__dangbun_responsive_utils__) {
                  ${ResponsiveUtils.getResponsiveJs()}
                }

                var STYLE_ID = '__db_placemake3_top_inset_fix__';
                var CLASS_NAME = 'db-placemake3-content-down';
                var HTML_ATTR = 'data-db-placemake3';
                var BASE_DOWN_PX = $downPx;

                // ✅ 하단 "플레이스 이동" 버튼을 살짝 위로 올릴 값 (반응형)
                var MOVE_BTN_RAISE_PX_BASE = 24;
                var MOVE_APPLIED_ATTR = 'data-db-movebtn-raised';

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

                  // ✅ 화면 크기에 따라 동적으로 계산
                  var responsivePx = window.getResponsivePx ? window.getResponsivePx(BASE_DOWN_PX, 'height') : BASE_DOWN_PX;

                  style.textContent = 
                    'html[' + HTML_ATTR + '="1"] .' + CLASS_NAME + '{' +
                      'margin-top: ' + responsivePx + 'px !important;' +
                    '}';
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

                function getText(el) {
                  try {
                    return (el.innerText || el.value || '').replace(/\s/g,'');
                  } catch(e) { return ''; }
                }

                function findMoveButton() {
                  try {
                    // ✅ 텍스트 후보: "플레이스 이동", "이동", "완료", "다음"
                    var keywords = ['플레이스이동', '이동', '완료', '다음'];

                    var candidates = document.querySelectorAll(
                      'button, a, [role="button"], input[type="button"], input[type="submit"]'
                    );

                    for (var i = 0; i < candidates.length; i++) {
                      var txt = getText(candidates[i]);
                      if (!txt) continue;

                      for (var k = 0; k < keywords.length; k++) {
                        if (txt.indexOf(keywords[k]) >= 0) return candidates[i];
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
                        return cur;
                      }
                    }
                    return null;
                  } catch(e) {
                    return null;
                  }
                }

                function raiseMoveButton() {
                  try {
                    var btn = findMoveButton();
                    if (!btn) return;

                    if (btn.getAttribute(MOVE_APPLIED_ATTR) === '1') return;

                    // ✅ 반응형 계산
                    var moveRaisePx = window.getResponsivePx ? window.getResponsivePx(MOVE_BTN_RAISE_PX_BASE, 'height') : MOVE_BTN_RAISE_PX_BASE;

                    // 1) 버튼 자체를 위로 올림
                    btn.style.transform = 'translateY(-' + moveRaisePx + 'px)';
                    btn.style.willChange = 'transform';
                    btn.setAttribute(MOVE_APPLIED_ATTR, '1');

                    // 2) fixed 부모가 있으면 부모도 같이 올림
                    var fixedParent = findFixedParent(btn);
                    if (fixedParent && fixedParent.getAttribute(MOVE_APPLIED_ATTR) !== '1') {
                      fixedParent.style.transform = 'translateY(-' + moveRaisePx + 'px)';
                      fixedParent.style.willChange = 'transform';
                      fixedParent.setAttribute(MOVE_APPLIED_ATTR, '1');
                    }

                    console.log('PLACEMAKE3_MOVE_BTN_RAISED', 'px=' + moveRaisePx);
                  } catch(e) {}
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
                    console.log('PLACEMAKE3_FIX_APPLIED', 'downPx=' + $downPx);
                  }

                  // ✅ 하단 버튼도 살짝 위로
                  raiseMoveButton();
                }

                // ✅ 화면 크기 변경 시에도 재계산되도록 리사이즈 이벤트 추가
                if (!window.__dangbun_placemake3_resize_handler__) {
                    window.__dangbun_placemake3_resize_handler__ = true;
                    var resizeTimer;
                    window.addEventListener('resize', function() {
                        clearTimeout(resizeTimer);
                        resizeTimer = setTimeout(function() {
                            if (isPlaceMake3()) {
                                ensureStyle(); // 스타일 재계산
                                manage(); // 다시 적용
                            }
                        }, 100);
                    });
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
