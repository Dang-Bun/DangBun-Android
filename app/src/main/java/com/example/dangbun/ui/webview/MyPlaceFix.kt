package com.example.dangbun.ui.webview

import android.webkit.WebView

internal object MyPlaceFix {
    internal fun injectMyPlaceUnifiedFix(view: WebView) {
        val js = """
            (function() {
              try {
                var GRAY_BG = '#F5F6F8'; 
                var styleId = '__db_final_ordered_layout_fix__';
                
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
                    min-height: 100vh !important; /* ✅ 하단 빈영역까지 회색으로 채움 */
                  }

                  /* ✅ 리스트가 버튼과 겹치지 않게 하단 여백 확보 */
                  .db-list-safe-bottom {
                    padding-bottom: 110px !important; /* 🔧 여기 수치만 조절 */
                    box-sizing: border-box !important;
                  }

                  /* ✅ 어떤 컨테이너가 실제 스크롤이든 배경을 회색으로 강제 */
                  body, #root, #__next, main {
                    background: ${'$'}{GRAY_BG} !important;
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

                    /* ✅ 화면처럼 좌우 여백만 확보 */
                    padding: 0 16px !important;
                    box-sizing: border-box !important;

                    margin: 10px 0 20px 0 !important;
                    transform: none !important;
                  }


                  /* ✅ [핵심] 버튼 크기 강제 제거(원래 웹 스타일로 복원) */
                  .db-add-btn-wrap button {
                    position: static !important;

                    /* ✅ 버튼을 “원래처럼 크게” */
                    width: 100% !important;
                    max-width: none !important;

                    /* 혹시 button이 inline/auto라면 대비 */
                    display: block !important;
                  }

                `;

                function apply() {
                  var path = location.pathname;
                  var isMyPlace = (path.indexOf('MyPlace') >= 0 || path.indexOf('myplace') >= 0 || document.body.innerText.indexOf('내 플레이스') >= 0);
                  if (!isMyPlace) return;
                  
                  // ✅ (추가) "화면을 덮는 큰 흰 wrapper"가 있으면 회색으로 강제
                  // ✅ (교체) 최상단/전체 트리에서 "화면을 덮는 흰 배경 wrapper"를 잡아서 회색으로 강제
                  try {
                    function isWhiteish(bg) {
                      if (!bg) return false;
                      // rgb(255, 255, 255) / rgba(255,255,255,1) 등
                      return bg === 'rgb(255, 255, 255)' || bg.indexOf('rgba(255, 255, 255') === 0;
                    }

                    // 0) html/body 자체도 inline으로 한번 더 강제 (CSS보다 우선될 때가 많음)
                    try {
                      document.documentElement.style.setProperty('background-color', GRAY_BG, 'important');
                      document.body.style.setProperty('background-color', GRAY_BG, 'important');
                      document.body.style.setProperty('background', GRAY_BG, 'important');
                    } catch(e) {}

                    var best = null;
                    var bestArea = 0;

                    // 1) 후보 범위를 "main"이 아니라 body/#root/#__next 전체로 확장
                    var scopes = [
                      document.body,
                      document.querySelector('#root'),
                      document.querySelector('#__next'),
                      document.querySelector('main')
                    ].filter(Boolean);

                    for (var s = 0; s < scopes.length; s++) {
                      var scope = scopes[s];

                      // 너무 많은 노드 탐색 방지: div/section/article만
                      var candidates = scope.querySelectorAll('div, section, article');
                      for (var i2 = 0; i2 < candidates.length; i2++) {
                        var el = candidates[i2];
                        if (!el || !el.getBoundingClientRect) continue;

                        var rect = el.getBoundingClientRect();

                        // "화면을 덮는" 조건(가로 거의 전체 + 세로 상당 부분)
                        if (rect.width < window.innerWidth * 0.92) continue;
                        if (rect.height < window.innerHeight * 0.60) continue;

                        var st = window.getComputedStyle(el);
                        if (!isWhiteish(st.backgroundColor)) continue;

                        // fixed overlay 같은 것도 잡히게 area 최대를 선택
                        var area = rect.width * rect.height;
                        if (area > bestArea) {
                          bestArea = area;
                          best = el;
                        }
                      }
                    }

                    if (best) {
                      best.style.setProperty('background', GRAY_BG, 'important');
                      best.style.setProperty('background-color', GRAY_BG, 'important');
                      best.style.setProperty('min-height', '100vh', 'important');
                      best.style.setProperty('width', '100%', 'important');
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

                  // C. 하단 버튼 처리 (버블 바로 뒤에 삽입)
                  // C. 하단 버튼 처리 (버블 바로 뒤에 삽입) + ✅ 폭 강제(작은 pill 방지)
                  var btns = document.querySelectorAll('button');
                  for (var k = 0; k < btns.length; k++) {
                    var btn = btns[k];
                    if ((btn.innerText || '').indexOf('플레이스 추가') >= 0) {

                      // ✅ 1) 버튼 자체를 "가로 꽉" 강제 (inline style이 제일 확실)
                      try {
                        btn.style.setProperty('width', '100%', 'important');
                        btn.style.setProperty('min-width', '100%', 'important');
                        btn.style.setProperty('max-width', 'none', 'important');
                        btn.style.setProperty('display', 'block', 'important');
                        btn.style.setProperty('box-sizing', 'border-box', 'important');
                      } catch(e) {}

                      // ✅ 2) 감싸는 래퍼(부모)가 content 폭이면 버튼이 계속 pill이 됨 → 부모도 100%
                      var wrap = btn.parentElement;

                      // 혹시 button 상위에 한 겹 더 감싸져 있을 수 있어서 "버튼 폭이 안 늘어나는" 케이스 대비
                      // (버튼 부모 폭이 너무 작으면 한 단계 더 위로 올라가서 wrap 후보를 잡음)
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

                      // ✅ wrap도 가로 꽉 + 좌우 여백만
                      try {
                        if (wrap) {
                          wrap.style.setProperty('width', '100%', 'important');
                          wrap.style.setProperty('display', 'block', 'important');
                          wrap.style.setProperty('padding', '0 16px', 'important');
                          wrap.style.setProperty('box-sizing', 'border-box', 'important');
                        }
                      } catch(e) {}

                      // ✅ 3) wrap 상위 컨테이너가 flex(center)면 wrap이 줄어드는 경우가 있음 → 상위도 "stretch"
                      try {
                        var p = wrap ? wrap.parentElement : null;
                        for (var up = 0; up < 2 && p; up++) {
                          var st = window.getComputedStyle(p);
                          if (st && st.display === 'flex') {
                            p.style.setProperty('align-items', 'stretch', 'important');
                          }
                          p.style.setProperty('width', '100%', 'important');
                          p = p.parentElement;
                        }
                      } catch(e) {}

                      // ✅ 4) 위치 이동(버블 뒤)
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
