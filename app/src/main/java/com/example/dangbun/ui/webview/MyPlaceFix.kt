package com.example.dangbun.ui.webview

import android.webkit.WebView

internal object MyPlaceFix {
    internal fun injectMyPlaceUnifiedFix(view: WebView) {
        val js = """
            (function() {
              try {
                var GRAY_BG = '#F5F6F8'; 
                var styleId = '__db_final_ordered_layout_fix_v3__';
                
                // ✅ [핵심] style 태그가 있으면 갱신(덮어쓰기), 없으면 생성
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
                    background-color: ${'$'}{'$'}{GRAY_BG} !important;
                    width: 100% !important;
                    min-height: 56px !important;
                    padding-top: calc(env(safe-area-inset-top) + 12px) !important;
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

                  /* 4. 플레이스 추가 버튼: 버블 아래 리스트 위에 위치 */
                  .db-add-btn-wrap {
                    position: relative !important;
                    display: block !important;
                    width: 100% !important;
                    padding: 18px 16px 0 16px !important;
                    box-sizing: border-box !important;
                    margin: 0 0 20px 0 !important;
                    transform: none !important;
                  }

                  /* ✅ 버튼 크기 강제 제거(원래 웹 스타일로 복원) */
                  .db-add-btn-wrap button {
                    position: static !important;
                    width: 100% !important;
                    max-width: none !important;
                    display: block !important;
                  }
                `;

                function apply() {
                  var path = location.pathname;
                  var isMyPlace = (path.indexOf('MyPlace') >= 0 || path.indexOf('myplace') >= 0 || document.body.innerText.indexOf('내 플레이스') >= 0);
                  if (!isMyPlace) return;
                  
                  // 배경색 강제 적용 (화면 덮는 흰색 박스 제거)
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
                  var tags = document.querySelectorAll('h1,h2,h3,header,div,span');
                  for (var i=0; i<tags.length; i++) {
                    if (tags[i].innerText.trim() === '내 플레이스' && !tags[i].__hooked) {
                      tags[i].__hooked = true;
                      tags[i].classList.add('db-title-text');
                      
                      var header = tags[i].parentElement;
                      while(header && header.offsetWidth < window.innerWidth * 0.8) {
                        header = header.parentElement;
                      }
                      
                      if (header) {
                        header.classList.add('db-header-wrapper');
                        var menu = header.querySelector('svg, button, [class*="menu"]');
                        if (menu && menu !== tags[i]) {
                          menu.classList.add('db-menu-icon');
                        }
                      }
                      break;
                    }
                  }

                  // B. 버블 위치 보정
                  var bubbleEl = null;
                  var divs = document.querySelectorAll('div,section,p');
                  for (var j=0; j<divs.length; j++) {
                    if (divs[j].innerText.indexOf('오늘 남은 청소는') >= 0) {
                      var bubble = divs[j];
                      for(var d=0; d<3 && bubble.parentElement; d++) {
                        if (getComputedStyle(bubble).borderRadius !== '0px') break;
                        bubble = bubble.parentElement;
                      }
                      bubble.id = 'db-bubble-fix';
                      bubbleEl = bubble;
                      break;
                    }
                  }

                  // C. 하단 버튼 처리 및 리스트 하단 여백 추가
                  var btns = document.querySelectorAll('button');
                  for (var k = 0; k < btns.length; k++) {
                    var btn = btns[k];
                    if ((btn.innerText || '').indexOf('플레이스 추가') >= 0) {
                      
                      // 1. 리스트 하단 여백(padding-bottom) 늘리기
                      try {
                        function findScrollHost(seed) {
                          // ✅ 1) 가장 우선: 문서 스크롤 자체가 있으면 그걸 사용
                          var docEl = document.scrollingElement || document.documentElement;
                          if (docEl && docEl.scrollHeight > docEl.clientHeight + 10) return docEl;

                          // ✅ 2) 전역에서 "스크롤 가능한 큰 컨테이너"를 찾아서 선택
                          var best = null;
                          var bestScore = 0;

                          var nodes = document.querySelectorAll('body *');
                          for (var i = 0; i < nodes.length; i++) {
                            var el = nodes[i];
                            if (!el || !el.getBoundingClientRect) continue;

                            var st = window.getComputedStyle(el);
                            var oy = st ? st.overflowY : '';
                            if (oy !== 'auto' && oy !== 'scroll') continue;

                            var ch = el.clientHeight || 0;
                            var sh = el.scrollHeight || 0;
                            if (sh <= ch + 10) continue;

                            // 화면 높이의 절반 이상 차지하는 "메인 스크롤"을 우선
                            if (ch < window.innerHeight * 0.5) continue;

                            // 점수: (스크롤 가능한 양) * (컨테이너 크기)
                            var score = (sh - ch) * ch;
                            if (score > bestScore) {
                              bestScore = score;
                              best = el;
                            }
                          }

                          return best || document.querySelector('main') || document.body;
                        }


                        // ✅ 기존(24px)에서 120px로 대폭 늘려서 여유 공간 확보
                        var btnH = Math.ceil(btn.getBoundingClientRect().height || 56);
                        var safePx = (btnH + 220); 
                        
                        var host = findScrollHost(btn);
                        if (host) {
                          host.style.setProperty(
                            'padding-bottom',
                            'calc(' + safePx + 'px + env(safe-area-inset-bottom))',
                            'important'
                          );
                          host.style.setProperty('box-sizing', 'border-box', 'important');
                        }
                      } catch(e) {}

                      // 2. 버튼 래퍼 처리 (크기 보존)
                      var wrap = btn.parentElement;
                      try {
                        if (wrap) {
                          var r = wrap.getBoundingClientRect();
                          if (r && r.width < (window.innerWidth * 0.6) && wrap.parentElement) {
                            wrap = wrap.parentElement;
                          }
                        }
                      } catch(e) {}

                      if (wrap && !wrap.classList.contains('db-add-btn-wrap')) {
                        wrap.classList.add('db-add-btn-wrap');
                      }

                      /* ✅ [추가] 플레이스 추가 버튼을 FAB처럼 "화면에 따라오게" 고정 */
                      try {
                        // 0) wrap이 없으면 btn 기준으로 처리
                        var fabWrap = wrap || btn;

                        // 1) 스크롤 컨텐츠(리스트) 안에 있으면 일부 환경에서 잘릴 수 있어서 body로 이동
                        //    (중복 이동 방지 플래그)
                        if (fabWrap && !fabWrap.__dbFabPinned) {
                          fabWrap.__dbFabPinned = true;
                          document.body.appendChild(fabWrap);
                        }

                        // 2) FAB 스타일 적용: 화면 하단 고정 + 좌우 패딩 유지
                        if (fabWrap) {
                          fabWrap.style.setProperty('position', 'fixed', 'important');
                          fabWrap.style.setProperty('left', '0', 'important');
                          fabWrap.style.setProperty('right', '0', 'important');
                          fabWrap.style.setProperty('bottom', '0', 'important');
                          fabWrap.style.setProperty('width', '100%', 'important');
                          fabWrap.style.setProperty('z-index', '2147483647', 'important'); // 최상단
                          fabWrap.style.setProperty('margin', '0', 'important');
                          fabWrap.style.setProperty('transform', 'none', 'important');

                          // 좌우 16px, 아래는 safe-area만 반영
                          fabWrap.style.setProperty('padding', '0 16px env(safe-area-inset-bottom) 16px', 'important');
                          fabWrap.style.setProperty('box-sizing', 'border-box', 'important');

                          // 배경은 투명(원래 화면처럼)
                          fabWrap.style.setProperty('background', 'transparent', 'important');
                        }

                        // 3) 리스트(스크롤 컨테이너) 하단 여백은 FAB 높이만큼 유지(겹침 방지)
                        //    (이미 safePx 로 padding-bottom 주고 있어서, 혹시 모자라면 여기서 보강)
                        var fabH = Math.ceil((fabWrap && fabWrap.getBoundingClientRect) ? fabWrap.getBoundingClientRect().height : 0);
                        if (fabH > 0) {
                          var host2 = findScrollHost(btn);
                          if (host2) {
                            host2.style.setProperty(
                              'padding-bottom',
                              'calc(' + (fabH + 24) + 'px + env(safe-area-inset-bottom))',
                              'important'
                            );
                            host2.style.setProperty('box-sizing', 'border-box', 'important');
                          }
                        }
                      } catch(e) {}
                      
                      // 3. 위치 이동 (버블 뒤)
                      // ✅ FAB로 body에 붙인 경우에는 DOM 재배치(insertBefore/prepend) 시도하면 HierarchyRequestError가 날 수 있어서 스킵
                      var isFabPinned = !!(wrap && wrap.__dbFabPinned);

                      if (!isFabPinned) {
                        if (bubbleEl && bubbleEl.parentNode && wrap && bubbleEl.parentNode.nodeType !== 9) {
                          // nodeType 9 = document (document에는 element를 insertBefore 하면 에러 날 수 있음)
                          bubbleEl.parentNode.insertBefore(wrap, bubbleEl.nextSibling);
                        } else if (wrap) {
                          var container = document.querySelector('main') || document.querySelector('#root');
                          if (container && container.nodeType !== 9) container.prepend(wrap);
                        }
                      }


                      break;
                    }
                  }
                }

                apply();
                var mo = new MutationObserver(apply);
                mo.observe(document.documentElement, { childList: true, subtree: true });
                window.addEventListener('popstate', function() { setTimeout(apply, 100); });
              } catch(e) {}
            })();
        """.trimIndent()

        view.evaluateJavascript(js, null)
    }
}
