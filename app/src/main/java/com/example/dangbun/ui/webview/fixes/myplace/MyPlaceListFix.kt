package com.example.dangbun.ui.webview.fixes.myplace

internal object MyPlaceListFix {

    internal fun provideJs(): String {
        return """
            (function() {
              try {
                var GRAY_BG = '#F5F6F8';
                var styleId = '__db_myplace_list_fix_padding_v3__';

                // ✅ [핵심 튜닝] 여기 숫자를 늘려서 간격을 확보합니다.
                var FAB_SAFE_RAISE = 45;    // 버튼을 바닥에서 얼마나 띄울지
                var LIST_BOTTOM_GAP = 100;  // 마지막 아이템과 버튼 사이의 간격 (기존보다 대폭 확대)
                
                // ---------------------------
                // CSS 주입
                // ---------------------------
                var style = document.getElementById(styleId);
                if (!style) {
                  style = document.createElement('style');
                  style.id = styleId;
                  document.head.appendChild(style);
                }

                style.textContent =
                  "html, body, #root, #__next, main {"
                  + "  background-color: " + GRAY_BG + " !important;"
                  + "  margin: 0 !important;"
                  + "  padding: 0 !important;"
                  + "  min-height: 100vh !important;"
                  + "}"
                  + "\n"
                  // 버튼 스타일: 화면 하단 중앙 고정
                  + ".db-add-btn-wrap {"
                  + "  position: fixed !important;"
                  + "  left: 50% !important;"
                  + "  transform: translateX(-50%) !important;"
                  + "  width: calc(100vw - 32px) !important;"
                  + "  max-width: calc(100vw - 32px) !important;"
                  + "  z-index: 2147483647 !important;"
                  + "  margin: 0 !important;"
                  + "  padding: 0 !important;"
                  + "  box-sizing: border-box !important;"
                  + "  background: transparent !important;"
                  + "}"
                  + "\n"
                  + ".db-add-btn-wrap > button {"
                  + "  width: 100% !important;"
                  + "  display: block !important;"
                  + "  margin: 0 auto !important;"
                  + "}";

                function isMyPlace() {
                  try {
                    var path = location.pathname || '';
                    var bodyText = (document.body && document.body.innerText) ? document.body.innerText : '';
                    return (path.indexOf('MyPlace') >= 0 || path.indexOf('myplace') >= 0 || bodyText.indexOf('내 플레이스') >= 0);
                  } catch(e) { return false; }
                }

                function isListView() {
                  try {
                    var text = (document.body && document.body.innerText) ? document.body.innerText : '';
                    return (text.indexOf('새로운 알림') >= 0 || text.indexOf('완료된 청소') >= 0);
                  } catch(e) { return true; }
                }

                function findAddButtonsExact() {
                  var out = [];
                  try {
                    var btns = document.querySelectorAll('button');
                    for (var i = 0; i < btns.length; i++) {
                      var b = btns[i];
                      if (((b.innerText || '')).trim() === '플레이스 추가') {
                         out.push(b);
                      }
                    }
                  } catch(e) {}
                  return out;
                }

                function findFabWrap(btn) {
                  // 버튼의 적절한 부모 컨테이너 찾기
                  return btn.parentElement || btn;
                }

                // ✅ 스크롤이 발생하는 진짜 컨테이너 찾기
                function pickBestScrollHostFrom(btn) {
                  try {
                    // 1. 버튼 부모 중 스크롤 되는 놈 찾기
                    var cur = btn;
                    for (var step = 0; step < 15 && cur; step++) {
                      var p = cur.parentElement;
                      if (!p) break;
                      var st = window.getComputedStyle(p);
                      if ((st.overflowY === 'auto' || st.overflowY === 'scroll') && p.scrollHeight > p.clientHeight) {
                        return p;
                      }
                      cur = p;
                    }
                    // 2. 없으면 body나 root 반환
                    return document.querySelector('#root') || document.querySelector('main') || document.body;
                  } catch(e) {
                    return document.body;
                  }
                }

                // ✅ 강제 여백 주입 (Padding + Spacer)
                function forceBottomSpacing(host, heightPx) {
                  try {
                    if (!host) return;

                    // 1. Padding 적용
                    host.style.setProperty('box-sizing', 'border-box', 'important');
                    var currentPad = parseInt(host.style.paddingBottom || '0');
                    if (currentPad < heightPx) {
                        host.style.setProperty('padding-bottom', heightPx + 'px', 'important');
                    }

                    // 2. Spacer(투명 벽돌) 삽입 - 가장 확실한 방법
                    var spacerId = '__db_list_safe_spacer__';
                    var spacer = document.getElementById(spacerId);
                    if (!spacer) {
                        spacer = document.createElement('div');
                        spacer.id = spacerId;
                        spacer.style.width = '100%';
                        spacer.style.clear = 'both';
                        spacer.style.background = 'transparent';
                        spacer.style.pointerEvents = 'none';
                        spacer.style.display = 'block';
                    }
                    spacer.style.height = heightPx + 'px';
                    spacer.style.minHeight = heightPx + 'px';

                    // host의 맨 마지막 자식으로 붙임
                    if (host.lastElementChild !== spacer) {
                        host.appendChild(spacer);
                    }
                    
                    // 3. Body에도 안전장치로 패딩 적용
                    if (host !== document.body) {
                        document.body.style.setProperty('padding-bottom', heightPx + 'px', 'important');
                    }

                  } catch(e) {}
                }

                function apply() {
                  try {
                    if (!isMyPlace()) return;
                    if (!isListView()) return;

                    var btns = findAddButtonsExact();
                    if (!btns || btns.length === 0) return;
                    
                    // 화면상 가장 위에 있는 버튼 하나만 선택
                    var best = btns[0]; 

                    // 나머지 숨김 처리
                    for (var j = 0; j < btns.length; j++) {
                      if (btns[j] !== best) btns[j].style.display = 'none';
                    }

                    // 버튼 고정 (Fixed)
                    var wrap = findFabWrap(best);
                    if (wrap) {
                      wrap.classList.add('db-add-btn-wrap');
                      wrap.style.setProperty('bottom', FAB_SAFE_RAISE + 'px', 'important');
                    }

                    // ✅ 여백 계산 및 적용
                    var btnH = 56;
                    try { btnH = best.offsetHeight || 56; } catch(e) {}
                    
                    // 버튼높이 + 띄움높이 + 추가여백(180px)
                    var totalSafeHeight = btnH + FAB_SAFE_RAISE + LIST_BOTTOM_GAP;

                    var host = pickBestScrollHostFrom(best);
                    forceBottomSpacing(host, totalSafeHeight);

                  } catch(e) {}
                }

                apply();
                setInterval(apply, 500); // 0.5초마다 강제 체크 (SPA 화면 변경 대응)

              } catch(e) {}
            })();
        """.trimIndent()
    }
}
