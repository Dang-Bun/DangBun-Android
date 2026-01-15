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

                      /* ✅ [추가] 플레이스 추가 버튼이 떠 있는 고정 영역을 아래로 내림 (겹침 완화) */
                      try {
                        // 버튼/래퍼 기준으로 위로 올라가며 fixed/sticky 컨테이너 탐색
                        var fixedHost = wrap || btn;
                        for (var t = 0; t < 6 && fixedHost; t++) {
                          var st = window.getComputedStyle(fixedHost);
                          if (st && (st.position === 'fixed' || st.position === 'sticky')) break;
                          fixedHost = fixedHost.parentElement;
                        }

                        // 찾았으면 "바닥에 붙이기"
                        if (fixedHost) {
                          fixedHost.style.setProperty('bottom', '0px', 'important');
                          fixedHost.style.setProperty('margin-bottom', '0px', 'important');
                          fixedHost.style.setProperty('transform', 'none', 'important');

                          // 혹시 safe-area 때문에 위로 떠있는 케이스는 padding으로만 처리
                          fixedHost.style.setProperty('padding-bottom', 'env(safe-area-inset-bottom)', 'important');
                        }

                        // 버튼 자체도 불필요한 여백 제거
                        btn.style.setProperty('margin-bottom', '0px', 'important');
                      } catch(e) {}                      
                      
                      // 3. 위치 이동 (버블 뒤)
                      if (bubbleEl && bubbleEl.parentNode && wrap) {
                        bubbleEl.parentNode.insertBefore(wrap, bubbleEl.nextSibling);
                      } else if (wrap) {
                        var container = document.querySelector('main') || document.querySelector('#root');
                        if (container) container.prepend(wrap);
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
