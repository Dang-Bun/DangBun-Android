package com.example.dangbun.ui.webview.fixes.addplace

import android.webkit.WebView

internal object PlaceMake1TopInsetFix {

    internal fun debug(view: WebView) { inject(view) }

    internal fun inject(
        view: WebView,
        contentStartTop: Int = 50
    ) {
        view.evaluateJavascript(provideJs(contentStartTop), null)
    }

    private fun provideJs(contentStartTop: Int): String {
        return """
            (function() {
              try {
                if (window.__pm1_waiter) clearInterval(window.__pm1_waiter);

                var TOP_POS = $contentStartTop;
                var LOG_TAG = '[DB_PM1_FIT] ';
                var STYLE_ID = 'db-pm1-fit-style';
                var OVERLAY_ID = 'db-touch-overlay';
                
                // 버튼 바닥 여백
                var BTN_BOTTOM_MARGIN = '48px'; 
                
                // 뒤로가기 버튼 상단 여백 (더 아래로 내림)
                var BACK_BTN_TOP = '16px';

                // ============================================================
                // 🧹 청소부
                // ============================================================
                function cleanup() {
                    var els = document.querySelectorAll('[data-pm1-fixed]');
                    els.forEach(function(el) {
                        el.style.cssText = ''; 
                        el.removeAttribute('data-pm1-fixed');
                    });
                    
                    var style = document.getElementById(STYLE_ID);
                    if (style) style.remove();
                    
                    var overlay = document.getElementById(OVERLAY_ID);
                    if (overlay) overlay.remove();
                }

                // ============================================================
                // 💉 CSS 주입 (초강력 다이어트)
                // ============================================================
                function injectFitCSS() {
                    if (document.getElementById(STYLE_ID)) return;
                    
                    var css = `
                        /* 1. 그리드/플렉스 간격 최소화 */
                        [data-pm1-fixed="content"] > div, 
                        [data-pm1-fixed="content"] > div > div {
                            gap: 4px 4px !important; 
                            row-gap: 4px !important;
                            padding-bottom: 0 !important;
                        }
                        
                        /* 2. 텍스트 여백 완전 제거 & 폰트 조정 */
                        [data-pm1-fixed="content"] p,
                        [data-pm1-fixed="content"] span,
                        [data-pm1-fixed="content"] label {
                            margin-top: 0 !important;
                            margin-bottom: 0 !important;
                            padding-top: 0 !important;
                            padding-bottom: 0 !important;
                            font-size: 13px !important; /* 글자 살짝 줄여서 공간 확보 */
                        }
                        
                        /* 3. 아이콘 크기 대폭 축소 (한 화면에 넣기 위함) */
                        [data-pm1-fixed="content"] img,
                        [data-pm1-fixed="content"] svg {
                            margin: 0 !important;
                            max-width: 42px !important;  /* 56px -> 42px */
                            max-height: 42px !important;
                        }
                        
                        /* 4. 제목 여백 제거 */
                        [data-pm1-fixed="content"] h1, 
                        [data-pm1-fixed="content"] h2, 
                        [data-pm1-fixed="content"] h3 {
                            margin-bottom: 4px !important;
                            padding-bottom: 0 !important;
                        }
                    `;
                    
                    var style = document.createElement('style');
                    style.id = STYLE_ID;
                    style.textContent = css;
                    document.head.appendChild(style);
                }

                // ============================================================
                // 🔒 레이아웃 & 스크롤 제거
                // ============================================================
                function fixLayoutAndContent() {
                    // 전체 고정 (스크롤 X)
                    var roots = document.querySelectorAll('html, body, #root, #__next, main');
                    roots.forEach(function(el) {
                        if (!el.getAttribute('data-pm1-fixed')) {
                            el.setAttribute('data-pm1-fixed', 'true');
                            el.style.setProperty('overflow', 'hidden', 'important');
                            el.style.setProperty('height', '100%', 'important');
                            el.style.setProperty('width', '100%', 'important');
                            el.style.setProperty('position', 'fixed', 'important');
                            el.style.setProperty('top', '0', 'important');
                            el.style.setProperty('left', '0', 'important');
                            el.style.setProperty('touch-action', 'none', 'important'); // 스크롤 터치 차단
                        }
                    });

                    // 콘텐츠 컨테이너
                    var all = document.querySelectorAll('div');
                    var target = null;
                    for(var i=0; i<all.length; i++) {
                        var txt = (all[i].innerText || '').replace(/\s/g, '');
                        if(txt.indexOf('관리할플레이스의이름') >= 0 && txt.indexOf('플레이스의유형') >= 0) {
                            if (all[i].id !== 'root' && all[i].id !== '__next') {
                                target = all[i];
                                break; 
                            }
                        }
                    }

                    if (target) {
                        if (!target.getAttribute('data-pm1-fixed')) target.setAttribute('data-pm1-fixed', 'content');
                        
                        target.style.setProperty('position', 'absolute', 'important');
                        target.style.setProperty('top', TOP_POS + 'px', 'important');
                        target.style.setProperty('left', '0', 'important');
                        target.style.setProperty('width', '100%', 'important');
                        
                        // ⭐ 스크롤 제거 & 높이 고정
                        target.style.setProperty('height', 'auto', 'important'); 
                        target.style.setProperty('overflow', 'visible', 'important'); 
                        
                        target.style.setProperty('padding-left', '20px', 'important');
                        target.style.setProperty('padding-right', '20px', 'important');
                        target.style.setProperty('padding-bottom', '0', 'important');
                        target.style.setProperty('display', 'block', 'important');
                        
                        // 입력창 터치 보장
                        var inputs = target.querySelectorAll('input');
                        inputs.forEach(function(inp) {
                            inp.style.setProperty('pointer-events', 'auto', 'important');
                            inp.style.setProperty('z-index', '999', 'important');
                        });
                    }
                }

                // ============================================================
                // 🧐 버튼 비활성 상태 판별
                // ============================================================
                function isRealDisabled(btn) {
                    if (btn.disabled) return true;
                    if (btn.getAttribute('aria-disabled') === 'true') return true;
                    if (btn.classList.contains('disabled')) return true;

                    var style = window.getComputedStyle(btn);
                    var bg = style.backgroundColor; 
                    if (bg.indexOf('rgb') >= 0) {
                         var rgb = bg.match(/\d+/g);
                         if (rgb && rgb.length >= 3) {
                             var r = parseInt(rgb[0]);
                             var g = parseInt(rgb[1]);
                             var b = parseInt(rgb[2]);
                             if (Math.abs(r-g) < 15 && Math.abs(g-b) < 15 && r > 180) {
                                 return true;
                             }
                         }
                    }
                    return false;
                }

                // ============================================================
                // 🖱️ 진짜 버튼 & 뒤로가기 버튼 고정
                // ============================================================
                function fixButtons() {
                    // 1. 진짜 '다음' 버튼 찾기
                    var btns = document.querySelectorAll('button');
                    var nextCandidates = [];
                    var backBtn = null;
                    
                    for (var i = 0; i < btns.length; i++) {
                        var b = btns[i];
                        var txt = (b.innerText || '').trim();
                        
                        if (txt === '다음') {
                            nextCandidates.push(b);
                        } else if ((b.getAttribute('aria-label') || '').indexOf('뒤로') >= 0 || 
                                   (b.getAttribute('aria-label') || '').indexOf('back') >= 0) {
                            backBtn = b;
                        }
                    }
                    
                    // 뒤로가기 버튼 fallback (아이콘 위치로 추정)
                    if (!backBtn) {
                        for (var j = 0; j < btns.length; j++) {
                             var r = btns[j].getBoundingClientRect();
                             if(r.left < 50 && r.top < 100 && r.width < 100 && (btns[j].innerText||'').trim() !== '다음') { 
                                 backBtn = btns[j]; break; 
                             }
                        }
                    }

                    // 2. 뒤로가기 버튼 위치 조정 (더 아래로)
                    if (backBtn) {
                        if (!backBtn.getAttribute('data-pm1-btn-fixed')) backBtn.setAttribute('data-pm1-btn-fixed', 'true');
                        backBtn.style.setProperty('position', 'fixed', 'important');
                        backBtn.style.setProperty('top', BACK_BTN_TOP, 'important'); // ⭐ 24px 적용
                        backBtn.style.setProperty('left', '16px', 'important');
                        backBtn.style.setProperty('z-index', '2147483647', 'important');
                    }

                    // 3. '다음' 버튼 처리
                    if (nextCandidates.length === 0) return;

                    var realNext = null;
                    for (var k = 0; k < nextCandidates.length; k++) {
                        if (isRealDisabled(nextCandidates[k])) {
                            realNext = nextCandidates[k];
                            break;
                        }
                    }
                    if (!realNext) realNext = nextCandidates[0];

                    if (!realNext.getAttribute('data-pm1-fixed')) {
                        realNext.setAttribute('data-pm1-fixed', 'true');
                    }
                    
                    realNext.style.setProperty('position', 'fixed', 'important');
                    realNext.style.setProperty('bottom', BTN_BOTTOM_MARGIN, 'important');
                    realNext.style.setProperty('left', '16px', 'important');
                    realNext.style.setProperty('right', '16px', 'important');
                    realNext.style.setProperty('width', 'calc(100% - 32px)', 'important');
                    realNext.style.setProperty('z-index', '2147483646', 'important'); // Overlay 아래
                    realNext.style.setProperty('transform', 'none', 'important');
                    realNext.style.setProperty('display', 'block', 'important');
                    
                    // 상태에 따른 투명도
                    if (isRealDisabled(realNext)) {
                        realNext.style.setProperty('opacity', '0.3', 'important');
                    } else {
                        realNext.style.setProperty('opacity', '1', 'important');
                    }
                    
                    // 가짜 숨기기
                    for (var l = 0; l < nextCandidates.length; l++) {
                        if (nextCandidates[l] !== realNext) {
                            nextCandidates[l].style.setProperty('display', 'none', 'important');
                            nextCandidates[l].setAttribute('data-pm1-fixed', 'true');
                        }
                    }

                    // 4. 투명막(Overlay) 설치
                    var overlay = document.getElementById(OVERLAY_ID);
                    if (!overlay) {
                        overlay = document.createElement('div');
                        overlay.id = OVERLAY_ID;
                        document.body.appendChild(overlay);
                        
                        overlay.onclick = function(e) {
                            e.preventDefault();
                            e.stopPropagation();
                            if (!isRealDisabled(realNext)) {
                                realNext.disabled = false;
                                realNext.click(); 
                                var evt = new MouseEvent('click', {bubbles: true, cancelable: true, view: window});
                                realNext.dispatchEvent(evt);
                            }
                        };
                    }
                    overlay.style.cssText = 'position: fixed; bottom: ' + BTN_BOTTOM_MARGIN + '; left: 16px; width: calc(100% - 32px); height: 56px; z-index: 2147483647; background: transparent; touch-action: manipulation;';
                }

                // ============================================================
                // 🔄 메인 루프
                // ============================================================
                function loop() {
                    var path = (location.pathname || '').toLowerCase();
                    if (path.indexOf('placemake1') >= 0) {
                        injectFitCSS();
                        fixLayoutAndContent();
                        fixButtons();
                    } else {
                        cleanup();
                    }
                }

                window.__pm1_waiter = setInterval(loop, 100);

              } catch(e) { console.error('[DB_PM1_ERR]', e); }
            })();
        """.trimIndent()
    }
}
