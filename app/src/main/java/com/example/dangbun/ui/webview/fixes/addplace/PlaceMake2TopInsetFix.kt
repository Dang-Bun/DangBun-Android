package com.example.dangbun.ui.webview.fixes.addplace

import android.webkit.WebView

internal object PlaceMake2TopInsetFix {

    internal fun debug(view: WebView) { inject(view) }

    internal fun inject(
        view: WebView,
        // 상단 시작 위치 (뒤로가기 버튼 아래)
        contentStartTop: Int = 60
    ) {
        view.evaluateJavascript(provideJs(contentStartTop), null)
    }

    private fun provideJs(contentStartTop: Int): String {
        return """
            (function() {
              try {
                // 기존 타이머 클리어
                if (window.__pm2_waiter) clearInterval(window.__pm2_waiter);

                var TOP_POS = $contentStartTop;
                var LOG_TAG = '[DB_PM2_DEBUG] ';

                console.log(LOG_TAG + "🚀 PlaceMake2 Fix Started...");

                function isTargetScreen() {
                    if ((location.pathname || '').toLowerCase().indexOf('placemake2') < 0) return false;
                    // 화면 로딩 확인 (텍스트 기준)
                    var bodyText = (document.body.innerText || '').replace(/\s/g, '');
                    return bodyText.indexOf('정보를작성해주세요') >= 0;
                }

                // 1. 전체 레이아웃 고정 (화면 흔들림 방지)
                function lockLayout() {
                    var roots = document.querySelectorAll('html, body, #root, #__next, main');
                    roots.forEach(function(el) {
                        el.style.setProperty('overflow', 'hidden', 'important'); // 전체 스크롤 막기
                        el.style.setProperty('height', '100%', 'important');
                        el.style.setProperty('width', '100%', 'important');
                        el.style.setProperty('position', 'fixed', 'important');
                        el.style.setProperty('top', '0', 'important');
                        el.style.setProperty('left', '0', 'important');
                        el.style.setProperty('margin', '0', 'important');
                        el.style.setProperty('padding', '0', 'important');
                        // 터치 및 입력 허용
                        el.style.setProperty('touch-action', 'auto', 'important');
                        el.style.setProperty('user-select', 'text', 'important');
                        el.style.setProperty('-webkit-user-select', 'text', 'important');
                    });
                }

                // 2. 콘텐츠 영역 찾아서 스크롤 가능한 영역으로 만들기
                function fixContent() {
                    var all = document.querySelectorAll('h1, h2, h3, div');
                    var target = null;
                    
                    // "정보를 작성해주세요" 텍스트를 포함하는 컨테이너 찾기
                    for(var i=0; i<all.length; i++) {
                        var txt = (all[i].innerText || '').replace(/\s/g, '');
                        if(txt.indexOf('정보를작성해주세요') >= 0) {
                            // 너무 깊은 자식이나 root는 제외
                            if (all[i].tagName === 'DIV' && all[i].id !== 'root' && all[i].id !== '__next') {
                                // "이름", "이메일" 같은 라벨도 포함하는지 확인 (더 정확한 타겟팅)
                                if (txt.indexOf('이름') >= 0 || txt.indexOf('이메일') >= 0) {
                                    target = all[i];
                                    break; 
                                }
                            }
                        }
                    }

                    if (target) {
                        if (!target.getAttribute('data-pm2-fixed')) {
                            console.log(LOG_TAG + "Found Content Container: <" + target.tagName + ">");
                            target.setAttribute('data-pm2-fixed', 'true');
                        }

                        // 절대 좌표로 고정하되, 내부는 스크롤 가능하게 설정
                        target.style.setProperty('position', 'absolute', 'important');
                        target.style.setProperty('top', TOP_POS + 'px', 'important'); // 상단 여백
                        target.style.setProperty('left', '0', 'important');
                        target.style.setProperty('width', '100%', 'important');
                        
                        // 하단 버튼 공간(80px)을 제외하고 높이 설정
                        target.style.setProperty('height', 'calc(100% - ' + (TOP_POS + 80) + 'px)', 'important');
                        
                        // ⭐ 핵심: 내부 스크롤 허용 (폼 입력이 길어질 수 있음)
                        target.style.setProperty('overflow-y', 'auto', 'important');
                        target.style.setProperty('display', 'block', 'important');
                        target.style.setProperty('padding', '0 20px', 'important'); // 좌우 여백
                        target.style.setProperty('margin', '0', 'important');
                        target.style.setProperty('z-index', '10', 'important');
                        
                        // 불필요한 상단 마진 제거
                        var children = target.querySelectorAll('*');
                        children.forEach(function(c) {
                            var style = window.getComputedStyle(c);
                            if (parseInt(style.marginTop) > 0) {
                                c.style.setProperty('margin-top', '0', 'important');
                            }
                        });
                    } else {
                        // console.log(LOG_TAG + "Content Container NOT found yet...");
                    }
                }

                // 3. "완료" 버튼 찾아서 바닥에 고정 (탈옥 전략)
                function fixCompleteButton() {
                    var btns = document.querySelectorAll('button');
                    var targetBtn = null;

                    for (var i = 0; i < btns.length; i++) {
                        var b = btns[i];
                        var txt = (b.innerText || '').trim();
                        if (txt === '완료') {
                            targetBtn = b;
                            break;
                        }
                    }

                    if (targetBtn) {
                        if (!targetBtn.getAttribute('data-pm2-btn-fixed')) {
                            console.log(LOG_TAG + "Found COMPLETE Button!");
                            targetBtn.setAttribute('data-pm2-btn-fixed', 'true');
                        }

                        // 버튼 스타일 강제
                        targetBtn.style.setProperty('position', 'fixed', 'important');
                        targetBtn.style.setProperty('bottom', '24px', 'important');
                        targetBtn.style.setProperty('left', '16px', 'important');
                        targetBtn.style.setProperty('right', '16px', 'important');
                        targetBtn.style.setProperty('width', 'calc(100% - 32px)', 'important');
                        targetBtn.style.setProperty('z-index', '2147483647', 'important'); // 최상위
                        targetBtn.style.setProperty('display', 'block', 'important');
                        targetBtn.style.setProperty('transform', 'none', 'important');
                        
                        // 상태에 따른 스타일 (React가 제어하지만 안전장치)
                        if (!targetBtn.disabled) {
                            targetBtn.style.setProperty('opacity', '1', 'important');
                            targetBtn.style.setProperty('pointer-events', 'auto', 'important');
                        }

                        // ⭐ 조상 요소의 감옥 속성 제거 (Jailbreak)
                        var parent = targetBtn.parentElement;
                        while(parent && parent !== document.body) {
                            var style = window.getComputedStyle(parent);
                            if (style.transform !== 'none') parent.style.setProperty('transform', 'none', 'important');
                            if (style.contain !== 'none') parent.style.setProperty('contain', 'none', 'important');
                            if (style.overflow === 'hidden') parent.style.setProperty('overflow', 'visible', 'important');
                            parent = parent.parentElement;
                        }
                    }
                }
                
                // 4. 뒤로가기 버튼 고정
                function fixBackButton() {
                    var backBtn = document.querySelector('button[aria-label*="뒤로"]');
                    if (!backBtn) {
                         var all = document.querySelectorAll('button');
                         for(var j=0; j<all.length; j++) {
                             var r = all[j].getBoundingClientRect();
                             // 왼쪽 상단 구석에 있는 버튼
                             if(r.left < 50 && r.top < 100 && r.width < 100 && (all[j].innerText||'').trim() !== '완료') { 
                                 backBtn = all[j]; break; 
                             }
                         }
                    }
                    if (backBtn) {
                        backBtn.style.setProperty('position', 'fixed', 'important');
                        backBtn.style.setProperty('top', '10px', 'important');
                        backBtn.style.setProperty('left', '16px', 'important');
                        backBtn.style.setProperty('z-index', '2147483647', 'important');
                    }
                }

                function applyFix() {
                    if (!isTargetScreen()) return;
                    
                    lockLayout();
                    fixContent();
                    fixCompleteButton();
                    fixBackButton();
                }

                // 0.1초마다 실행 (React 렌더링 대응)
                window.__pm2_waiter = setInterval(applyFix, 100);

              } catch(e) { 
                  console.error('[DB_PM2_ERR]', e); 
              }
            })();
        """.trimIndent()
    }
}
