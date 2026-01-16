package com.example.dangbun.ui.webview

import android.webkit.WebView

internal object MyPlaceFix {
    internal fun injectMyPlaceUnifiedFix(view: WebView) {
        val js = """
            (function() {
              try {
                var GRAY_BG = '#F5F6F8';
                var styleId = '__db_final_ordered_layout_fix_v3__';
                
                // ✅ 여기 값만 바꾸면서 튜닝하세요
                var HEADER_TOP_EXTRA = 0;   // 기존 42였던 "추가 여백"
                // 너무 늘리면 "내 플레이스" 텍스트가 아래로 밀려서 안보임
                var HEADER_SPACER_H = 0;     // 필요시 0~16 정도 (보조 여백)
                
                // ✅ 리스트-버튼 사이 하단 여백(이 값만 튜닝)
                var LIST_BOTTOM_GAP = 840;
                
                // ✅ style 태그 upsert
                var style = document.getElementById(styleId);
                if (!style) {
                  style = document.createElement('style');
                  style.id = styleId;
                  document.head.appendChild(style);
                }

                style.innerHTML = `
                  /* 1. 배경색 및 레이아웃 안정화 */
                  html, body, #root, #__next, main {
                    background-color: ${'$'}{GRAY_BG} !important;
                    margin: 0 !important;
                    padding: 0 !important;
                    display: block !important;
                    min-height: 100vh !important;
                  }

                  /* 2. 상단 헤더 박스 (정중앙 유지) */
                  .db-header-wrapper {
                    background-color: ${'$'}{GRAY_BG} !important;
                    width: 100% !important;
                    min-height: 56px !important;

                    padding-top: calc(env(safe-area-inset-top)) !important;
                    padding-bottom: 12px !important;

                    position: relative !important;
                    display: flex !important;
                    justify-content: center !important;
                    align-items: center !important;
                    box-sizing: border-box !important;
                    z-index: 9999 !important;
                  }

                  .db-title-text {
                    font-weight: bold !important;
                    font-size: 18px !important;
                    color: #333 !important;
                    margin: 0 !important;
                    text-align: center !important;
                    white-space: nowrap !important;
                  }

                  .db-menu-icon {
                    position: absolute !important;
                    right: 16px !important;
                    top: 50% !important;
                    transform: translateY(-50%) !important;
                    margin: 0 !important;
                    z-index: 10000 !important;
                  }

                  /* 3. 청소 알림 버블 */
                  #db-bubble-fix {
                    margin: 15px auto 10px auto !important;
                    display: table !important;
                    background-color: transparent !important;
                  }

                  /* 4. FAB 래퍼(가운데 고정) */
                  .db-add-btn-wrap {
                    position: fixed !important;
                    left: 50% !important;
                    bottom: 0 !important;
                    transform: translateX(-50%) !important;

                    width: calc(100vw - 32px) !important;
                    max-width: calc(100vw - 32px) !important;

                    z-index: 2147483647 !important;
                    margin: 0 !important;
                    padding-bottom: 0 !important;
                    box-sizing: border-box !important;
                    background: transparent !important;
                  }

                  .db-add-btn-wrap button {
                    position: static !important;
                    width: 100% !important;
                    max-width: none !important;
                    display: block !important;
                    margin: 0 auto !important;
                  }

                  .db-add-btn-hidden {
                    display: none !important;
                    visibility: hidden !important;
                    pointer-events: none !important;
                    height: 0 !important;
                    margin: 0 !important;
                    padding: 0 !important;
                  }
                `;

                function apply() {
                  var path = location.pathname || '';
                  var bodyText = (document.body && document.body.innerText) ? document.body.innerText : '';
                  var isMyPlace = (
                    path.indexOf('MyPlace') >= 0 ||
                    path.indexOf('myplace') >= 0 ||
                    bodyText.indexOf('내 플레이스') >= 0
                  );
                  if (!isMyPlace) return;

                  // ✅ "리스트 화면"에서만 FAB를 고정하고, 상세 화면에서는 숨김 처리
                  function isListView() {
                    try {
                      var text = (document.body && document.body.innerText) ? document.body.innerText : '';
                      var score = 0;
                      if (text.indexOf('새로운 알림') >= 0) score++;
                      if (text.indexOf('완료된 청소') >= 0) score++;
                      if (text.indexOf('매니저') >= 0) score++;
                      return score >= 2;
                    } catch(e) { return true; }
                  }

                  var inList = isListView();

                  // ✅ 배경색 강제 적용 (화면 덮는 흰색 박스 제거)
                  try {
                    document.documentElement.style.setProperty('background-color', GRAY_BG, 'important');
                    document.body.style.setProperty('background-color', GRAY_BG, 'important');

                    var scopes = [document.body, document.querySelector('#root'), document.querySelector('main')].filter(Boolean);
                    for (var s = 0; s < scopes.length; s++) {
                      var candidates = scopes[s].querySelectorAll('div, section, article');
                      for (var i2 = 0; i2 < candidates.length; i2++) {
                        var el = candidates[i2];
                        var rect = el.getBoundingClientRect();
                        if (rect.width > window.innerWidth * 0.92 && rect.height > window.innerHeight * 0.60) {
                          var st = window.getComputedStyle(el);
                          if (st.backgroundColor === 'rgb(255, 255, 255)') {
                            el.style.setProperty('background', GRAY_BG, 'important');
                            el.style.setProperty('background-color', GRAY_BG, 'important');
                          }
                        }
                      }
                    }
                  } catch(e) {}

                  // A. 헤더 영역 정리
                  try {
                    var tags = document.querySelectorAll('h1,h2,h3,header,div,span');
                    for (var i=0; i<tags.length; i++) {
                      if ((tags[i].innerText || '').trim() === '내 플레이스' && !tags[i].__hooked) {
                        tags[i].__hooked = true;
                        tags[i].classList.add('db-title-text');

                        var header = tags[i].parentElement;
                        while(header && header.offsetWidth < window.innerWidth * 0.8) {
                          header = header.parentElement;
                        }

                        if (header) {
                          try {
                            header.style.setProperty('background-color', GRAY_BG, 'important');
                            header.style.setProperty('background', GRAY_BG, 'important');

                            header.style.setProperty(
                              'padding-top',
                              'calc(env(safe-area-inset-top) + ' + HEADER_TOP_EXTRA + 'px)',
                              'important'
                            );

                            header.style.setProperty('padding-bottom', '12px', 'important');
                            header.style.setProperty('box-sizing', 'border-box', 'important');

                            header.style.setProperty('position', 'relative', 'important');
                            var spacerId = '__db_header_top_spacer__';
                            var spacer = header.querySelector('#' + spacerId);
                            if (!spacer) {
                              spacer = document.createElement('div');
                              spacer.id = spacerId;
                              spacer.style.position = 'absolute';
                              spacer.style.left = '0';
                              spacer.style.right = '0';
                              spacer.style.top = '0';
                              spacer.style.height = HEADER_SPACER_H + 'px';
                              spacer.style.background = GRAY_BG;
                              spacer.style.pointerEvents = 'none';
                              header.appendChild(spacer);
                            } else {
                              spacer.style.height = HEADER_SPACER_H + 'px';
                              spacer.style.background = GRAY_BG;
                            }
                          } catch(e) {}

                          var menu = header.querySelector('svg, button, [class*="menu"]');
                          if (menu && menu !== tags[i]) {
                            menu.classList.add('db-menu-icon');
                          }
                        }
                        break;
                      }
                    }
                  } catch(e) {}

                  // B. 버블 위치 보정
                  var bubbleEl = null;
                  try {
                    var divs = document.querySelectorAll('div,section,p');
                    for (var j=0; j<divs.length; j++) {
                      if ((divs[j].innerText || '').indexOf('오늘 남은 청소는') >= 0) {
                        var bubble = divs[j];
                        for (var d=0; d<3 && bubble.parentElement; d++) {
                          if (getComputedStyle(bubble).borderRadius !== '0px') break;
                          bubble = bubble.parentElement;
                        }
                        bubble.id = 'db-bubble-fix';
                        bubbleEl = bubble;
                        break;
                      }
                    }
                  } catch(e) {}

                  // ✅ 리스트-버튼 겹침 방지: "진짜 스크롤 호스트(host)" 끝에 spacer를 붙인다
                  function ensureBottomSpacerOnHost(host) {
                    try {
                      if (!host) return;

                      var spacerId = '__db_list_bottom_spacer__';
                      var spacer = document.getElementById(spacerId);

                      if (!spacer) {
                        spacer = document.createElement('div');
                        spacer.id = spacerId;
                        spacer.style.width = '1px';
                        spacer.style.pointerEvents = 'none';
                        spacer.style.background = 'transparent';
                      }

                      var spacerH = LIST_BOTTOM_GAP;
                      spacer.style.height = 'calc(' + spacerH + 'px + env(safe-area-inset-bottom))';

                      if (spacer.parentNode !== host) {
                        try { spacer.parentNode && spacer.parentNode.removeChild(spacer); } catch(e) {}
                        host.appendChild(spacer);
                      }
                    } catch(e) {}
                  }

                  // C. 플레이스 추가 버튼 처리
                  var btns = document.querySelectorAll('button');
                  for (var k = 0; k < btns.length; k++) {
                    var btn = btns[k];
                    if (((btn.innerText || '')).indexOf('플레이스 추가') >= 0) {

                      // 0) 상세 화면이면: FAB 고정 해제 + 숨김
                      if (!inList) {
                        try {
                          var wrap0 = btn.parentElement || btn;
                          wrap0.classList.remove('db-add-btn-wrap');
                          wrap0.style.removeProperty('position');
                          wrap0.style.removeProperty('left');
                          wrap0.style.removeProperty('right');
                          wrap0.style.removeProperty('bottom');
                          wrap0.style.removeProperty('width');
                          wrap0.style.removeProperty('transform');
                          wrap0.style.removeProperty('padding');
                          wrap0.style.removeProperty('z-index');
                          wrap0.style.setProperty('display', 'none', 'important');
                        } catch(e) {}
                        break;
                      }

                      function findScrollHost(seed) {
                        try {
                          var cur = seed;
                          for (var step = 0; step < 12 && cur; step++) {
                            var p = cur.parentElement;
                            if (!p) break;
                            var st = window.getComputedStyle(p);
                            var oy = st ? st.overflowY : '';
                            var ch = p.clientHeight || 0;
                            var sh = p.scrollHeight || 0;
                            if ((oy === 'auto' || oy === 'scroll') && sh > ch + 10 && ch >= window.innerHeight * 0.4) {
                              return p;
                            }
                            cur = p;
                          }
                        } catch(e) {}

                        var best = null;
                        var bestScore = 0;
                        try {
                          var nodes = document.querySelectorAll('body *');
                          for (var i = 0; i < nodes.length; i++) {
                            var el = nodes[i];
                            if (!el || !el.getBoundingClientRect) continue;

                            var st2 = window.getComputedStyle(el);
                            var oy2 = st2 ? st2.overflowY : '';
                            if (oy2 !== 'auto' && oy2 !== 'scroll') continue;

                            var ch2 = el.clientHeight || 0;
                            var sh2 = el.scrollHeight || 0;
                            if (sh2 <= ch2 + 10) continue;
                            if (ch2 < window.innerHeight * 0.4) continue;

                            var rect = el.getBoundingClientRect();
                            var area = Math.max(0, rect.width) * Math.max(0, rect.height);
                            var score = (sh2 - ch2) * area;

                            if (score > bestScore) {
                              bestScore = score;
                              best = el;
                            }
                          }
                        } catch(e) {}

                        var docEl = document.scrollingElement || document.documentElement;
                        if (docEl && docEl.scrollHeight > docEl.clientHeight + 10) return docEl;

                        return best || document.querySelector('main') || document.body;
                      }

                      // 1) 스크롤 컨테이너 padding-bottom + spacer(겹침 방지)
                      try {
                        var wrapForMeasure = null;
                        try {
                          wrapForMeasure = (btn.parentElement && btn.parentElement.getBoundingClientRect) ? btn.parentElement : btn;
                        } catch(e) { wrapForMeasure = btn; }

                        var fabH = 56;
                        try {
                          fabH = Math.ceil((wrapForMeasure.getBoundingClientRect().height || 56));
                        } catch(e) {}

                        var extra = LIST_BOTTOM_GAP; // 버튼-리스트 간격
                        var safePx = fabH + extra;

                        var host = findScrollHost(btn);
                        if (host) {
                          host.style.setProperty(
                            'padding-bottom',
                            'calc(' + safePx + 'px + env(safe-area-inset-bottom))',
                            'important'
                          );
                          host.style.setProperty('box-sizing', 'border-box', 'important');
                          host.style.setProperty(
                            'scroll-padding-bottom',
                            'calc(' + safePx + 'px + env(safe-area-inset-bottom))',
                            'important'
                          );

                          // ✅ 핵심: 같은 host 끝에 spacer를 붙여 스크롤 길이 자체를 늘림
                          ensureBottomSpacerOnHost(host);
                        }
                      } catch(e) {}

                      // 2) 래퍼 잡기
                      var wrap = btn.parentElement;
                      try {
                        if (wrap) {
                          var r = wrap.getBoundingClientRect();
                          if (r && r.width < (window.innerWidth * 0.6) && wrap.parentElement) {
                            wrap = wrap.parentElement;
                          }
                        }
                      } catch(e) {}
                      if (!wrap) wrap = btn;

                      // 3) FAB 고정(중앙)
                      try {
                        wrap.classList.add('db-add-btn-wrap');

                        wrap.style.setProperty('position', 'fixed', 'important');
                        wrap.style.setProperty('left', '50%', 'important');
                        wrap.style.setProperty('bottom', '0', 'important');
                        wrap.style.setProperty('width', 'calc(100vw - 32px)', 'important');
                        wrap.style.setProperty('max-width', 'calc(100vw - 32px)', 'important');
                        wrap.style.setProperty('transform', 'translateX(-50%)', 'important');
                        wrap.style.setProperty('z-index', '2147483647', 'important');
                        wrap.style.setProperty('margin', '0', 'important');
                        wrap.style.setProperty('padding-bottom', '0', 'important');
                        wrap.style.setProperty('box-sizing', 'border-box', 'important');
                        wrap.style.setProperty('background', 'transparent', 'important');
                      } catch(e) {}

                      // 4) (선택) 버블 아래로 위치 이동
                      try {
                        if (bubbleEl && bubbleEl.parentNode && wrap && bubbleEl.parentNode.nodeType !== 9) {
                          bubbleEl.parentNode.insertBefore(wrap, bubbleEl.nextSibling);
                        }
                      } catch(e) {}

                      break;
                    }
                  }
                }

                apply();
                var mo = new MutationObserver(function(){ apply(); });
                mo.observe(document.documentElement, { childList: true, subtree: true });

                window.addEventListener('popstate', function() { setTimeout(apply, 120); });
                window.addEventListener('resize', function() { setTimeout(apply, 60); });
              } catch(e) {}
            })();
        """.trimIndent()

        view.evaluateJavascript(js, null)
    }
}
