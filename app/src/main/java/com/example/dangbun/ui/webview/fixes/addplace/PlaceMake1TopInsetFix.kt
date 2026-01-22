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
                var LOG_TAG = '[DB_PM1_FINAL] ';
                var STYLE_ID = 'db-pm1-fit-style';
                
                var BTN_BOTTOM_MARGIN = '48px'; 
                var BACK_BTN_TOP = '12px';

                // ============================================================
                // 💾 데이터 저장 (localStorage 사용 & 강제성 부여)
                // ============================================================
                function forceSaveName() {
                    try {
                        var inputs = document.querySelectorAll('input[type="text"]');
                        var nameVal = '';
                        
                        // 1. "이름" placeholder를 가진 입력창 우선 검색
                        for(var i=0; i<inputs.length; i++) {
                            var ph = (inputs[i].placeholder || '');
                            if (ph.indexOf('이름') >= 0 && inputs[i].value) {
                                nameVal = inputs[i].value;
                                break; 
                            }
                        }
                        
                        // 2. 없으면 값이 있는 첫 번째 입력창 사용
                        if (!nameVal) {
                            for(var j=0; j<inputs.length; j++) {
                                if (inputs[j].value && inputs[j].value.length > 0) {
                                    nameVal = inputs[j].value;
                                    break;
                                }
                            }
                        }
                        
                        if (nameVal) {
                            // ⭐ localStorage 사용 (더 안전함)
                            localStorage.setItem('db_place_name_fixed', nameVal);
                            console.log(LOG_TAG + "Saved to LocalStorage: " + nameVal);
                        }
                    } catch(e) {}
                }

                // ============================================================
                // 🧹 청소부
                // ============================================================
                function cleanup() {
                    var els = document.querySelectorAll('[data-pm1-fixed]');
                    els.forEach(function(el) { el.style.cssText = ''; el.removeAttribute('data-pm1-fixed'); });
                    var style = document.getElementById(STYLE_ID); if (style) style.remove();
                    var ov = document.getElementById('db-touch-overlay'); if(ov) ov.remove();
                }

                // ============================================================
                // 🧼 헤더 초기화
                // ============================================================
                function resetHeaderStyle() {
                    document.documentElement.removeAttribute('data-db-placemake3');
                    var header = document.querySelector('header');
                    if (header) {
                        header.style.background = 'transparent';
                        header.style.boxShadow = 'none';
                        // 이전에 주입된 헤더 타이틀이 있다면 제거
                        var injected = header.querySelector('.db-pm3-injected-native-title');
                        if (injected) injected.remove();
                    }
                }

                function injectFitCSS() {
                    if (document.getElementById(STYLE_ID)) return;
                    var css = `
                        [data-pm1-fixed="content"] > div, [data-pm1-fixed="content"] > div > div { gap: 4px 4px !important; row-gap: 4px !important; padding-bottom: 0 !important; }
                        [data-pm1-fixed="content"] p, [data-pm1-fixed="content"] span, [data-pm1-fixed="content"] label { margin-top: 0 !important; margin-bottom: 0 !important; font-size: 13px !important; }
                        [data-pm1-fixed="content"] img, [data-pm1-fixed="content"] svg { max-width: 42px !important; max-height: 42px !important; }
                        [data-pm1-fixed="content"] h1, [data-pm1-fixed="content"] h2 { margin-bottom: 4px !important; }
                    `;
                    var style = document.createElement('style');
                    style.id = STYLE_ID;
                    style.textContent = css;
                    document.head.appendChild(style);
                }

                function fixLayoutAndContent() {
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
                            el.style.setProperty('touch-action', 'none', 'important');
                        }
                    });

                    var all = document.querySelectorAll('div');
                    var target = null;
                    for(var i=0; i<all.length; i++) {
                        var txt = (all[i].innerText || '').replace(/\s/g, '');
                        if(txt.indexOf('관리할플레이스의이름') >= 0 && txt.indexOf('플레이스의유형') >= 0) {
                            if (all[i].id !== 'root' && all[i].id !== '__next') { target = all[i]; break; }
                        }
                    }

                    if (target) {
                        if (!target.getAttribute('data-pm1-fixed')) target.setAttribute('data-pm1-fixed', 'content');
                        target.style.setProperty('position', 'absolute', 'important');
                        target.style.setProperty('top', TOP_POS + 'px', 'important');
                        target.style.setProperty('left', '0', 'important');
                        target.style.setProperty('width', '100%', 'important');
                        target.style.setProperty('height', 'auto', 'important'); 
                        target.style.setProperty('overflow', 'visible', 'important'); 
                        target.style.setProperty('padding-left', '20px', 'important');
                        target.style.setProperty('padding-right', '20px', 'important');
                        target.style.setProperty('padding-bottom', '0', 'important');
                        target.style.setProperty('display', 'block', 'important');
                        
                        var inputs = target.querySelectorAll('input');
                        inputs.forEach(function(inp) {
                            // 입력할 때마다 저장
                            if (!inp.getAttribute('data-saver')) {
                                inp.setAttribute('data-saver', 'true');
                                inp.addEventListener('input', forceSaveName);
                                inp.addEventListener('blur', forceSaveName);
                            }
                            inp.style.setProperty('pointer-events', 'auto', 'important');
                            inp.style.setProperty('z-index', '999', 'important');
                        });
                    }
                }

                function fixButtons() {
                    var btns = document.querySelectorAll('button');
                    var nextCandidates = [];
                    var backBtn = null;
                    for (var i = 0; i < btns.length; i++) {
                        var b = btns[i];
                        var txt = (b.innerText || '').trim();
                        if (txt === '다음') nextCandidates.push(b);
                        else if ((b.getAttribute('aria-label') || '').indexOf('뒤로') >= 0 || (b.getAttribute('aria-label') || '').indexOf('back') >= 0) backBtn = b;
                    }
                    if (!backBtn) {
                         for (var j = 0; j < btns.length; j++) {
                             var r = btns[j].getBoundingClientRect();
                             if(r.left < 50 && r.top < 100 && r.width < 100 && (btns[j].innerText||'').trim() !== '다음') { backBtn = btns[j]; break; }
                         }
                    }

                    if (backBtn) {
                        if (!backBtn.getAttribute('data-pm1-btn-fixed')) backBtn.setAttribute('data-pm1-btn-fixed', 'true');
                        backBtn.style.setProperty('position', 'fixed', 'important');
                        backBtn.style.setProperty('top', BACK_BTN_TOP, 'important');
                        backBtn.style.setProperty('left', '16px', 'important');
                        backBtn.style.setProperty('z-index', '2147483647', 'important');
                        backBtn.style.setProperty('pointer-events', 'auto', 'important');
                    }

                    if (nextCandidates.length === 0) return;
                    
                    function isRealDisabled(btn) {
                        if (btn.disabled) return true;
                        if (btn.getAttribute('aria-disabled') === 'true') return true;
                        if (btn.classList.contains('disabled')) return true;
                        var style = window.getComputedStyle(btn);
                        var bg = style.backgroundColor; 
                        if (bg.indexOf('rgb') >= 0) {
                             var rgb = bg.match(/\d+/g);
                             if (rgb && rgb.length >= 3) {
                                 if (parseInt(rgb[0]) > 180 && parseInt(rgb[1]) > 180 && parseInt(rgb[2]) > 180) return true;
                             }
                        }
                        return false;
                    }

                    var realNext = null;
                    for(var k=0; k<nextCandidates.length; k++) {
                        if (nextCandidates[k].getAttribute('data-db-real-btn') === 'true') { realNext = nextCandidates[k]; break; }
                    }
                    if (!realNext) {
                        for(var k=0; k<nextCandidates.length; k++) {
                            if (isRealDisabled(nextCandidates[k])) { realNext = nextCandidates[k]; realNext.setAttribute('data-db-real-btn', 'true'); break; }
                        }
                    }
                    if (!realNext) { realNext = nextCandidates[0]; realNext.setAttribute('data-db-real-btn', 'true'); }

                    if (!realNext.getAttribute('data-pm1-fixed')) realNext.setAttribute('data-pm1-fixed', 'true');
                    realNext.style.setProperty('position', 'fixed', 'important');
                    realNext.style.setProperty('bottom', BTN_BOTTOM_MARGIN, 'important');
                    realNext.style.setProperty('left', '16px', 'important');
                    realNext.style.setProperty('right', '16px', 'important');
                    realNext.style.setProperty('width', 'calc(100% - 32px)', 'important');
                    realNext.style.setProperty('z-index', '2147483646', 'important');
                    realNext.style.setProperty('transform', 'none', 'important');
                    realNext.style.setProperty('display', 'block', 'important');
                    realNext.style.setProperty('touch-action', 'manipulation', 'important');
                    realNext.style.setProperty('cursor', 'pointer', 'important');

                    // ⭐ 클릭 시 강제 저장 실행
                    if (!realNext.getAttribute('data-save-listener')) {
                        realNext.setAttribute('data-save-listener', 'true');
                        realNext.addEventListener('click', forceSaveName);
                    }

                    if (isRealDisabled(realNext)) {
                        realNext.style.setProperty('opacity', '0.3', 'important');
                        realNext.style.setProperty('pointer-events', 'none', 'important');
                    } else {
                        realNext.style.setProperty('opacity', '1', 'important');
                        realNext.style.setProperty('pointer-events', 'auto', 'important');
                    }
                    
                    for (var l = 0; l < nextCandidates.length; l++) {
                        if (nextCandidates[l] !== realNext) {
                            nextCandidates[l].style.setProperty('display', 'none', 'important');
                            nextCandidates[l].setAttribute('data-pm1-fixed', 'true');
                        }
                    }
                }

                function loop() {
                    var path = (location.pathname || '').toLowerCase();
                    if (path.indexOf('placemake1') >= 0) {
                        resetHeaderStyle(); 
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
