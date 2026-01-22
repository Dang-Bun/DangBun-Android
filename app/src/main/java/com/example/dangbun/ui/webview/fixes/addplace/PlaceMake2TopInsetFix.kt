package com.example.dangbun.ui.webview.fixes.addplace

import android.webkit.WebView

internal object PlaceMake2TopInsetFix {

    internal fun debug(view: WebView) { inject(view) }

    internal fun inject(
        view: WebView,
        contentStartTop: Int = 60
    ) {
        view.evaluateJavascript(provideJs(contentStartTop), null)
    }

    private fun provideJs(contentStartTop: Int): String {
        return """
            (function() {
              try {
                if (window.__pm2_waiter) clearInterval(window.__pm2_waiter);

                var TOP_POS = $contentStartTop;
                var LOG_TAG = '[DB_PM2_TOUCH_FIX] ';
                var STYLE_ID = 'db-pm2-diet-style';
                
                // 완료 버튼 바닥 여백 (안전거리 확보)
                var BTN_BOTTOM_MARGIN = '56px'; 
                var BACK_BTN_TOP = '12px';

                // ============================================================
                // 👻 유령 퇴치 (이전 화면 '다음' 버튼 삭제)
                // ============================================================
                function killGhostButtons() {
                    var btns = document.querySelectorAll('button');
                    btns.forEach(function(b) {
                        var txt = (b.innerText || '').trim();
                        if (txt === '다음') {
                            b.style.setProperty('display', 'none', 'important');
                        }
                    });
                }

                // ============================================================
                // 🧹 청소부
                // ============================================================
                function cleanup() {
                    var els = document.querySelectorAll('[data-pm2-fixed]');
                    els.forEach(function(el) {
                        el.style.cssText = ''; 
                        el.removeAttribute('data-pm2-fixed');
                    });
                    
                    var style = document.getElementById(STYLE_ID);
                    if (style) style.remove();
                }

                // ============================================================
                // ⚡ 요소별 강제 스타일 주입 (디자인 다이어트)
                // ============================================================
                function forceDiet(container) {
                    var all = container.querySelectorAll('*');
                    all.forEach(function(el) {
                        var txt = (el.innerText || '').trim();
                        var tagName = el.tagName.toLowerCase();
                        
                        // (A) 설명 텍스트
                        if (txt.indexOf('플레이스에서 표시될') === 0 && txt.length > 20) {
                            el.style.setProperty('font-size', '12px', 'important');
                            el.style.setProperty('line-height', '1.3', 'important');
                            el.style.setProperty('margin-top', '4px', 'important');
                            el.style.setProperty('margin-bottom', '10px', 'important');
                            el.style.setProperty('color', '#888888', 'important');
                            el.style.setProperty('white-space', 'normal', 'important');
                        }
                        
                        // (B) 라벨
                        if (txt === '이름' || txt === '이메일') {
                            el.style.setProperty('font-size', '13px', 'important');
                            el.style.setProperty('margin-bottom', '2px', 'important');
                            el.style.setProperty('font-weight', 'bold', 'important');
                            if (el.parentElement) el.parentElement.style.setProperty('margin-bottom', '0', 'important');
                        }
                        
                        // (C) 입력창 (Input) - 210px 고정
                        if (tagName === 'input') {
                            el.style.setProperty('height', '36px', 'important'); 
                            el.style.setProperty('min-height', '36px', 'important');
                            el.style.setProperty('width', '210px', 'important'); // 너비 고정
                            el.style.setProperty('max-width', '65%', 'important');
                            el.style.setProperty('font-size', '14px', 'important');
                            el.style.setProperty('padding', '0 10px', 'important');
                            
                            var parent = el.parentElement;
                            if (parent) {
                                parent.style.setProperty('padding', '0', 'important');
                                parent.style.setProperty('margin-bottom', '8px', 'important');
                                parent.style.setProperty('min-height', 'auto', 'important');
                                parent.style.setProperty('justify-content', 'flex-start', 'important');
                            }
                        }
                        
                        // (D) 제목
                        if (txt.indexOf('정보를') === 0 && txt.indexOf('작성해주세요') > 0) {
                             el.style.setProperty('font-size', '18px', 'important');
                             el.style.setProperty('margin-bottom', '4px', 'important');
                             el.style.setProperty('margin-top', '0', 'important');
                        }
                    });
                }

                // ============================================================
                // 🔒 레이아웃 고정
                // ============================================================
                function fixLayoutAndContent() {
                    var roots = document.querySelectorAll('html, body, #root, #__next, main');
                    roots.forEach(function(el) {
                        if (!el.getAttribute('data-pm2-fixed')) {
                            el.setAttribute('data-pm2-fixed', 'true');
                            el.style.setProperty('overflow', 'hidden', 'important');
                            el.style.setProperty('height', '100%', 'important');
                            el.style.setProperty('width', '100%', 'important');
                            el.style.setProperty('position', 'fixed', 'important');
                            el.style.setProperty('top', '0', 'important');
                            el.style.setProperty('left', '0', 'important');
                            // ⭐ 전체 터치를 막되, 자식 요소의 터치는 허용
                            el.style.setProperty('touch-action', 'none', 'important'); 
                        }
                    });

                    var all = document.querySelectorAll('div');
                    var target = null;
                    for(var i=0; i<all.length; i++) {
                        var txt = (all[i].innerText || '').replace(/\s/g, '');
                        if(txt.indexOf('정보를작성해주세요') >= 0 && (txt.indexOf('이름') >= 0 || txt.indexOf('이메일') >= 0)) {
                            if (all[i].id !== 'root' && all[i].id !== '__next') {
                                target = all[i];
                                break; 
                            }
                        }
                    }

                    if (target) {
                        if (!target.getAttribute('data-pm2-fixed')) target.setAttribute('data-pm2-fixed', 'content');
                        
                        target.style.setProperty('position', 'absolute', 'important');
                        target.style.setProperty('top', TOP_POS + 'px', 'important');
                        target.style.setProperty('left', '0', 'important');
                        target.style.setProperty('width', '100%', 'important');
                        target.style.setProperty('box-sizing', 'border-box', 'important');
                        target.style.setProperty('margin-top', '0', 'important');
                        target.style.setProperty('padding-top', '0', 'important');

                        target.style.setProperty('height', 'auto', 'important'); 
                        target.style.setProperty('overflow', 'visible', 'important'); 
                        target.style.setProperty('padding-left', '20px', 'important');
                        target.style.setProperty('padding-right', '20px', 'important');
                        target.style.setProperty('padding-bottom', '0', 'important');
                        target.style.setProperty('display', 'block', 'important');
                        
                        forceDiet(target); 
                        
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
                             // 회색이면 비활성
                             if (Math.abs(r-g) < 15 && Math.abs(g-b) < 15 && r > 180) return true;
                         }
                    }
                    return false;
                }

                // ============================================================
                // 🖱️ '완료' 버튼 고정 (순정 터치 복구)
                // ============================================================
                function fixButtons() {
                    var btns = document.querySelectorAll('button');
                    var completeBtn = null;
                    var backBtn = null;
                    
                    for (var i = 0; i < btns.length; i++) {
                        var b = btns[i];
                        var txt = (b.innerText || '').trim();
                        if (txt === '완료') completeBtn = b;
                        else if ((b.getAttribute('aria-label') || '').indexOf('뒤로') >= 0 || 
                                   (b.getAttribute('aria-label') || '').indexOf('back') >= 0) {
                            backBtn = b;
                        }
                    }
                    
                    // 뒤로가기 버튼
                    if (backBtn) {
                        if (!backBtn.getAttribute('data-pm2-fixed')) backBtn.setAttribute('data-pm2-fixed', 'true');
                        backBtn.style.setProperty('position', 'fixed', 'important');
                        backBtn.style.setProperty('top', BACK_BTN_TOP, 'important');
                        backBtn.style.setProperty('left', '16px', 'important');
                        backBtn.style.setProperty('z-index', '2147483647', 'important');
                        backBtn.style.setProperty('pointer-events', 'auto', 'important'); // 터치 필수
                    }

                    // 완료 버튼
                    if (!completeBtn) return;

                    if (!completeBtn.getAttribute('data-pm2-fixed')) {
                        completeBtn.setAttribute('data-pm2-fixed', 'true');
                    }
                    
                    // 위치 및 스타일 강제 고정
                    completeBtn.style.setProperty('position', 'fixed', 'important');
                    completeBtn.style.setProperty('bottom', BTN_BOTTOM_MARGIN, 'important'); 
                    completeBtn.style.setProperty('left', '16px', 'important');
                    completeBtn.style.setProperty('right', '16px', 'important');
                    completeBtn.style.setProperty('width', 'calc(100% - 32px)', 'important');
                    completeBtn.style.setProperty('z-index', '2147483647', 'important'); // 최상위
                    completeBtn.style.setProperty('transform', 'none', 'important');
                    completeBtn.style.setProperty('display', 'block', 'important');
                    
                    // ⭐ 터치 관련 속성 복구 (핵심)
                    completeBtn.style.setProperty('touch-action', 'manipulation', 'important');
                    completeBtn.style.setProperty('cursor', 'pointer', 'important');
                    
                    // 비활성 상태에 따른 클릭 제어
                    if (isRealDisabled(completeBtn)) {
                        completeBtn.style.setProperty('opacity', '0.3', 'important');
                        completeBtn.style.setProperty('pointer-events', 'none', 'important'); // 클릭 차단
                    } else {
                        completeBtn.style.setProperty('opacity', '1', 'important');
                        completeBtn.style.setProperty('pointer-events', 'auto', 'important'); // 클릭 허용
                    }
                }

                // ============================================================
                // 🔄 메인 루프
                // ============================================================
                function loop() {
                    var path = (location.pathname || '').toLowerCase();
                    if (path.indexOf('placemake2') >= 0) {
                        killGhostButtons();
                        fixLayoutAndContent();
                        fixButtons();
                    } else {
                        cleanup();
                    }
                }

                window.__pm2_waiter = setInterval(loop, 100);

              } catch(e) { console.error('[DB_PM2_ERR]', e); }
            })();
        """.trimIndent()
    }
}
