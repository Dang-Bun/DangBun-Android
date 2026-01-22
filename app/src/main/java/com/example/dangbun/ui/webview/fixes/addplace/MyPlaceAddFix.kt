package com.example.dangbun.ui.webview.fixes.addplace

import android.webkit.WebView
import com.example.dangbun.ui.webview.fixes.common.ResponsiveUtils

internal object MyPlaceAddFix {
    internal fun inject(view: WebView) {
        view.evaluateJavascript(provideJs(), null)
    }

    internal fun provideJs(): String {
        return """
            (function() {
              try {
                // ==========================================
                // [반응형 설정 값]
                // ==========================================
                // ✅ 반응형 유틸리티 로드
                if (!window.__dangbun_responsive_utils__) {
                  ${ResponsiveUtils.getResponsiveJs()}
                }

                var GRAY_BG = '#F5F6F8';
                // ✅ 기준 값들을 화면 크기에 맞게 동적 계산
                // ✅ 상단 여백 최소화: 뒤로가기 버튼 바로 아래 질문 텍스트 배치
                // ✅ 질문 텍스트를 더 아래로 내림
                var CONTENT_START_TOP_BASE = -200;  // -280 -> -200 (질문 텍스트를 더 아래로 내림)
                var NEXT_BTN_BOTTOM_BASE = 80;  // 60 -> 80 (다음 버튼을 위로 올림)
                var BACK_BTN_DOWN_BASE = 40;  // 20 -> 40 (뒤로가기 버튼을 아래로 내림, translateY는 양수면 아래로)
                
                var CONTENT_START_TOP = window.getResponsivePx ? window.getResponsivePx(CONTENT_START_TOP_BASE, 'height') : CONTENT_START_TOP_BASE;
                var NEXT_BTN_BOTTOM = window.getResponsivePx ? window.getResponsivePx(NEXT_BTN_BOTTOM_BASE, 'height') : NEXT_BTN_BOTTOM_BASE;
                var BACK_BTN_DOWN = window.getResponsivePx ? window.getResponsivePx(BACK_BTN_DOWN_BASE, 'height') : BACK_BTN_DOWN_BASE;

                var STYLE_ID = '__db_addplace_css_hack_final__';
                var MOVED_BTN_CLASS = 'db-next-btn-moved-to-body'; // 납치한 버튼 식별용(이름은 유지)

                // ✅ 스크롤락 설치 여부
                var __dbScrollLockInstalled = false;
                var __dbScrollLockGuard = false;

                // ✅ AddPlace 판단: URL만 사용 (SPA DOM 잔상 오판 방지)
                function isAddPlace() {
                  var path = (location.pathname || '').toLowerCase();
                  return (path.indexOf('addplace') >= 0);
                }

                // ==========================================
                // [스크롤 기능 제거] (AddPlace에서만)
                // ==========================================
                function installScrollLock() {
                  if (__dbScrollLockInstalled) return;
                  __dbScrollLockInstalled = true;

                  try { window.scrollTo(0, 0); } catch(e) {}

                  function prevent(e) {
                    // ✅ AddPlace 아닐 때는 절대 막지 않음 (리스너가 남아도 스크롤 살림)
                    if (!isAddPlace()) return true;
                    try { e.preventDefault(); } catch(err) {}
                    return false;
                  }
                  window.__dbAddPlacePreventScroll__ = prevent;

                  try {
                    document.addEventListener('touchmove', prevent, { passive: false });
                    document.addEventListener('wheel', prevent, { passive: false });
                  } catch(e) {
                    try { document.addEventListener('touchmove', prevent); } catch(e2) {}
                    try { document.addEventListener('wheel', prevent); } catch(e3) {}
                  }

                  window.__dbAddPlaceScrollHandler__ = function() {
                    // ✅ AddPlace 아닐 때는 스크롤 강제 고정도 하지 않음
                    if (!isAddPlace()) return;

                    if (__dbScrollLockGuard) return;
                    __dbScrollLockGuard = true;
                    try { window.scrollTo(0, 0); } catch(e) {}
                    __dbScrollLockGuard = false;
                  };
                  try { window.addEventListener('scroll', window.__dbAddPlaceScrollHandler__, { passive: true }); }
                  catch(e) { try { window.addEventListener('scroll', window.__dbAddPlaceScrollHandler__); } catch(e2) {} }
                }

                function removeScrollLock() {
                  if (!__dbScrollLockInstalled) return;
                  __dbScrollLockInstalled = false;

                  var prevent = window.__dbAddPlacePreventScroll__;
                  if (prevent) {
                    try { document.removeEventListener('touchmove', prevent); } catch(e) {}
                    try { document.removeEventListener('wheel', prevent); } catch(e) {}
                    window.__dbAddPlacePreventScroll__ = null;
                  }

                  var sh = window.__dbAddPlaceScrollHandler__;
                  if (sh) {
                    try { window.removeEventListener('scroll', sh); } catch(e) {}
                    window.__dbAddPlaceScrollHandler__ = null;
                  }
                }

                // ==========================================
                // ✅ AddPlace 밖으로 나가면 스크롤 차단 흔적 강제 원복
                // ==========================================
                function globalTargets() {
                  var out = [];
                  try { out.push(document.documentElement); } catch(e) {}
                  try { out.push(document.body); } catch(e) {}
                  try { var r = document.querySelector('#root'); if (r) out.push(r); } catch(e) {}
                  try { var n = document.querySelector('#__next'); if (n) out.push(n); } catch(e) {}
                  try { var m = document.querySelector('main'); if (m) out.push(m); } catch(e) {}

                  var uniq = [];
                  for (var i=0; i<out.length; i++) {
                    if (out[i] && uniq.indexOf(out[i]) < 0) uniq.push(out[i]);
                  }
                  return uniq;
                }

                function forceRestoreScroll() {
                  try {
                    var ts = globalTargets();
                    for (var i=0; i<ts.length; i++) {
                      var t = ts[i];
                      try { t.style.removeProperty('overflow-y'); } catch(e) {}
                      try { t.style.removeProperty('overflow-x'); } catch(e) {}
                      try { t.style.removeProperty('overflow'); } catch(e) {}
                      try { t.style.removeProperty('touch-action'); } catch(e) {}
                      try { t.style.removeProperty('overscroll-behavior'); } catch(e) {}
                      try { t.style.removeProperty('-webkit-overflow-scrolling'); } catch(e) {}
                    }
                  } catch(e) {}
                }

                // ==========================================
                // [1] 스타일 주입 및 제거
                // ==========================================
                function applyStyles() {
                    var style = document.getElementById(STYLE_ID);
                    if (!style) {
                        style = document.createElement('style');
                        style.id = STYLE_ID;
                        document.head.appendChild(style);
                    }

                    // ✅ 화면 크기에 따라 동적으로 재계산
                    var contentTop = window.getResponsivePx ? window.getResponsivePx(CONTENT_START_TOP_BASE, 'height') : CONTENT_START_TOP_BASE;
                    var btnBottom = window.getResponsivePx ? window.getResponsivePx(NEXT_BTN_BOTTOM_BASE, 'height') : NEXT_BTN_BOTTOM_BASE;

                    style.textContent = `
                        html, body, #root, #__next, main {
                            background-color: ${'$'}{'$'}{GRAY_BG} !important;
                            display: block !important;
                            height: auto !important;
                            min-height: 100% !important;
                            padding-top: 0 !important;
                            margin-top: 0 !important;
                            align-items: flex-start !important;
                            justify-content: flex-start !important;

                            overflow-x: hidden !important;

                            /* ✅ AddPlace에서만 스크롤 제거 */
                            overflow-y: hidden !important;
                            overscroll-behavior: none !important;
                            -webkit-overflow-scrolling: auto !important;
                            touch-action: none !important;
                        }
                        
                        /* ✅ 모든 컨테이너의 상단 여백 강제 제거 */
                        body > *, #root > *, #__next > *, main > *,
                        body > * > *, #root > * > *, #__next > * > *, main > * > *,
                        body > * > * > *, #root > * > * > *, #__next > * > * > *, main > * > * > * {
                            margin-top: 0 !important;
                            padding-top: 0 !important;
                        }

                        /* ✅ 상단 여백 완전 제거: 뒤로가기 버튼과 질문 텍스트 사이 간격 최소화 */
                        header, nav, [role="banner"], [class*="Header"], [class*="header"], [class*="AppBar"], [class*="appbar"],
                        [class*="Top"], [class*="top"], [class*="Nav"], [class*="nav"] {
                            padding-top: 0 !important;
                            margin-top: 0 !important;
                            padding-bottom: 0 !important;
                            margin-bottom: 0 !important;
                            min-height: auto !important;
                            height: auto !important;
                        }

                        /* ✅ 뒤로가기 버튼 영역의 여백 완전 제거 */
                        button[aria-label*="뒤로"], button[aria-label*="back"], 
                        a[aria-label*="뒤로"], a[aria-label*="back"],
                        [class*="Back"], [class*="back"], 
                        [class*="Arrow"], [class*="arrow"],
                        button svg, a svg, [class*="Back"] svg, [class*="Arrow"] svg {
                            margin-top: 0 !important;
                            padding-top: 0 !important;
                            margin-bottom: 0 !important;
                            padding-bottom: 0 !important;
                        }

                        /* ✅ 질문 텍스트 영역의 상단 여백 완전 제거 */
                        main > *, #root > *, #__next > *,
                        main > *:first-child, #root > *:first-child, #__next > *:first-child,
                        main > *:first-child > *, #root > *:first-child > *, #__next > *:first-child > * {
                            margin-top: 0 !important;
                            padding-top: 0 !important;
                        }
                        
                        /* ✅ 질문 텍스트(h1, h2 등)의 여백 완전 제거 및 표시 보장 */
                        h1, h2, h3, p, div, span {
                            margin-top: 0 !important;
                            padding-top: 0 !important;
                        }

                        /* ✅ 매니저/멤버 옵션 간격 조정 */
                        /* 모든 flex 컨테이너의 gap 증가 */
                        [style*="display: flex"], [style*="display:flex"],
                        [class*="flex"], [class*="Flex"] {
                            gap: 20px !important;
                        }

                        /* 체크 표시 아이콘 아래 간격 넓히기 */
                        svg, [class*="check"], [class*="Check"],
                        [class*="icon"], [class*="Icon"] {
                            margin-bottom: 16px !important;
                        }
                        
                        /* ✅ 매니저/멤버 원형 아이콘 뒤의 회색 배경 제거 (CSS로 강제) */
                        div[style*="border-radius: 50%"], div[style*="borderRadius: 50%"],
                        div[style*="border-radius:50%"], div[style*="borderRadius:50%"],
                        button[style*="border-radius: 50%"], button[style*="borderRadius: 50%"],
                        *[style*="border-radius: 50%"], *[style*="borderRadius: 50%"] {
                            background-color: transparent !important;
                            background: transparent !important;
                            background-image: none !important;
                        }
                        
                        /* ✅ 회색 배경을 가진 원형 요소 강제 제거 */
                        div[style*="background-color: rgb(245"], div[style*="background-color:rgb(245"],
                        div[style*="background-color: rgb(240"], div[style*="background-color:rgb(240"],
                        div[style*="background-color: rgb(229"], div[style*="background-color:rgb(229"],
                        div[style*="background-color: rgb(243"], div[style*="background-color:rgb(243"],
                        div[style*="background: rgb(245"], div[style*="background:rgb(245"],
                        div[style*="background: rgb(240"], div[style*="background:rgb(240"] {
                            background-color: transparent !important;
                            background: transparent !important;
                            background-image: none !important;
                        }

                        .db-force-content-pos {
                            position: absolute !important;
                            top: ${'$'}{contentTop}px !important;
                            left: 0px !important;
                            width: 100% !important;
                            margin: 0 !important;
                            padding: 0 !important;
                            transform: none !important;
                            display: block !important;
                        }

                        .${'$'}{MOVED_BTN_CLASS} {
                            position: fixed !important;
                            bottom: ${'$'}{btnBottom}px !important;
                            left: 16px !important;
                            right: 16px !important;
                            width: auto !important;
                            max-width: none !important;
                            display: block !important;
                            z-index: 2147483647 !important;
                            top: auto !important;
                            transform: none !important;
                        }
                    `;
                }

                function removeStyles() {
                    var style = document.getElementById(STYLE_ID);
                    if (style) style.parentNode.removeChild(style);
                }

                // ==========================================
                // [2] 요소 찾기 및 조작
                // ==========================================
                function findContentWrapper() {
                    var nodes = document.querySelectorAll('h1,h2,h3,div,p');
                    for (var i=0; i<nodes.length; i++) {
                        var el = nodes[i];
                        if ((el.innerText || '').replace(/\s/g,'').indexOf('어떤목적으로사용하시나요') >= 0) {
                            if (el.tagName === 'SPAN' || el.tagName === 'STRONG') return el.offsetParent || el.parentElement;
                            return el.parentElement;
                        }
                    }
                    return null;
                }

                function findNextBtn() {
                    var btns = document.querySelectorAll('button');
                    for (var i=0; i<btns.length; i++) {
                        var t = (btns[i].innerText || '').trim();
                        if (t === '다음' && !btns[i].classList.contains(MOVED_BTN_CLASS)) return btns[i];
                    }
                    return null;
                }

                function findBackBtn() {
                    var btns = document.querySelectorAll('button, a, [role="button"]');
                    var best = null;
                    var bestScore = -9999;
                    for (var i=0; i<btns.length; i++) {
                        var el = btns[i];
                        var r = el.getBoundingClientRect();
                        if (r.left < window.innerWidth * 0.3 && r.top < window.innerHeight * 0.2) {
                            var score = 0;
                            if (r.width < 60 && r.height < 60) score += 5;
                            if (el.querySelector('svg, path')) score += 3;
                            if (score > bestScore) { bestScore = score; best = el; }
                        }
                    }
                    return best;
                }

                // ✅ React/Next root 안으로만 붙이기 (이벤트 유지 목적)
                function pickReactRootHost() {
                  try {
                    return document.querySelector('#__next')
                      || document.querySelector('#root')
                      || document.querySelector('main');
                  } catch(e) { return null; }
                }

                // ✅ 매니저/멤버 옵션 간격 조정 함수 (더 강력한 버전)
                function adjustOptionSpacing() {
                  try {
                    // ✅ 간격 값 (여기서 조정하면 됨)
                    // ✅ 매니저 체크 아이콘과 "기존 플레이스에 참여할 거에요." 사이 간격 확 줄임
                    var SPACING_PX = 8;  // 48 -> 8 (간격 확 줄임)
                    
                    // ✅ 모든 텍스트 요소를 찾아서 "기존 플레이스에 참여할 거에요." 찾기
                    var allTexts = document.querySelectorAll('p, div, span, h1, h2, h3, button, a, label');
                    for (var t = 0; t < allTexts.length; t++) {
                      var textEl = allTexts[t];
                      
                      // ✅ 이미 처리한 요소는 스킵 (중복 처리 방지)
                      if (textEl.__dangbun_member_spacing_applied__) continue;
                      
                      var textContent = (textEl.innerText || textEl.textContent || '').replace(/\s/g, '');
                      
                      // "기존 플레이스에 참여할 거에요." 텍스트 찾기
                      if (textContent.indexOf('기존플레이스에참여할거에요') >= 0 || 
                          textContent.indexOf('참여할거에요') >= 0 ||
                          textContent.indexOf('기존플레이스') >= 0) {
                        // 텍스트 요소와 모든 부모 요소에 간격 적용
                        var current = textEl;
                        for (var level = 0; level < 3 && current; level++) {
                          current.style.setProperty('margin-top', SPACING_PX + 'px', 'important');
                          current.style.setProperty('padding-top', SPACING_PX + 'px', 'important');
                          current = current.parentElement;
                        }
                        // ✅ 처리 완료 플래그 설정 (로그는 제거)
                        textEl.__dangbun_member_spacing_applied__ = true;
                      }
                    }
                    
                    // ✅ 매니저 옵션의 체크 표시 찾기
                    var allSvgs = document.querySelectorAll('svg, circle, path');
                    for (var s = 0; s < allSvgs.length; s++) {
                      var svgEl = allSvgs[s];
                      
                      // ✅ 이미 처리한 요소는 스킵 (중복 처리 방지)
                      if (svgEl.__dangbun_manager_spacing_applied__) continue;
                      
                      var svgRect = svgEl.getBoundingClientRect();
                      
                      // 작은 크기의 SVG/원 요소 (체크 표시 후보)
                      if (svgRect.width < 50 && svgRect.height < 50 && svgRect.width > 0 && svgRect.height > 0) {
                        // 매니저 옵션 영역 내에 있는지 확인
                        var checkParent = svgEl.parentElement;
                        var isInManager = false;
                        for (var cp = 0; cp < 6 && checkParent; cp++) {
                          var parentText = (checkParent.innerText || checkParent.textContent || '').replace(/\s/g, '');
                          if (parentText.indexOf('매니저') >= 0 || parentText.indexOf('Manager') >= 0) {
                            isInManager = true;
                            break;
                          }
                          checkParent = checkParent.parentElement;
                        }
                        
                        if (isInManager) {
                          // 체크 표시 아래 간격 적용
                          svgEl.style.setProperty('margin-bottom', SPACING_PX + 'px', 'important');
                          svgEl.style.setProperty('padding-bottom', SPACING_PX + 'px', 'important');
                          if (svgEl.parentElement) {
                            svgEl.parentElement.style.setProperty('margin-bottom', SPACING_PX + 'px', 'important');
                          }
                          if (svgEl.parentElement && svgEl.parentElement.parentElement) {
                            svgEl.parentElement.parentElement.style.setProperty('margin-bottom', SPACING_PX + 'px', 'important');
                          }
                          // ✅ 처리 완료 플래그 설정 (로그는 제거)
                          svgEl.__dangbun_manager_spacing_applied__ = true;
                        }
                      }
                    }
                  } catch(e) {
                    // 에러 로그도 제거 (너무 많이 출력됨)
                  }
                }

                // ==========================================
                // [3] 메인 로직
                // ==========================================
                // ✅ 다른 화면의 스타일 완전 제거 (독립성 보장)
                function cleanupOtherScreens() {
                  try {
                    // 다른 화면의 스타일 제거
                    var otherStyleIds = [
                      '__db_placemake1_top_inset_fix__',
                      '__db_placemake2_top_inset_fix__',
                      '__db_placemake3_top_inset_fix__',
                      '__db_onboarding_top_inset_fix__',
                      '__db_gray_topband_killer__'
                    ];
                    for (var i = 0; i < otherStyleIds.length; i++) {
                      var styleEl = document.getElementById(otherStyleIds[i]);
                      if (styleEl && styleEl.parentNode) {
                        styleEl.parentNode.removeChild(styleEl);
                      }
                    }
                    
                    // 다른 화면의 클래스 제거
                    var otherClasses = [
                      'db-back-button-fixed',
                      'db-next-button-fixed',
                      'db-placemake2-content-raise'
                    ];
                    for (var j = 0; j < otherClasses.length; j++) {
                      var elements = document.querySelectorAll('.' + otherClasses[j]);
                      for (var k = 0; k < elements.length; k++) {
                        try {
                          elements[k].classList.remove(otherClasses[j]);
                        } catch(e) {}
                      }
                    }
                    
                    // 다른 화면의 음수 margin 제거
                    var mainElements = document.querySelectorAll('main, #root, #__next');
                    for (var l = 0; l < mainElements.length; l++) {
                      var el = mainElements[l];
                      var computedStyle = window.getComputedStyle(el);
                      var marginTop = computedStyle.marginTop;
                      if (!isAddPlace() && marginTop && (marginTop.indexOf('-') >= 0 || parseFloat(marginTop) < -10)) {
                        el.style.setProperty('margin-top', '0', 'important');
                      }
                    }
                    
                    // overflow, height 초기화
                    var bodyElements = document.querySelectorAll('html, body');
                    for (var m = 0; m < bodyElements.length; m++) {
                      var elem = bodyElements[m];
                      elem.style.setProperty('overflow-y', 'auto', 'important');
                      elem.style.setProperty('overflow-x', 'auto', 'important');
                      elem.style.setProperty('height', 'auto', 'important');
                      elem.style.setProperty('max-height', 'none', 'important');
                      elem.style.setProperty('touch-action', 'auto', 'important');
                    }
                  } catch(e) {}
                }
                
                // ✅ 매니저/멤버 원형 아이콘 배경 처리 함수 (전역 스코프로 노출)
                function removeGrayBackgroundBehindIcons() {
                    try {
                        // ✅ 모든 원형 요소를 찾아서 처리
                        var allElements = document.querySelectorAll('*');
                        for (var i = 0; i < allElements.length; i++) {
                            var el = allElements[i];
                            if (!el || !el.getBoundingClientRect) continue;
                            
                            var rect = el.getBoundingClientRect();
                            if (rect.width <= 0 || rect.height <= 0) continue;
                            
                            var style = window.getComputedStyle(el);
                            var bgColor = style.backgroundColor || '';
                            var borderRadius = style.borderRadius || '';
                            var width = rect.width;
                            var height = rect.height;
                            
                            // 원형 요소인지 확인 (더 넓은 범위)
                            var isCircular = (borderRadius.indexOf('50%') >= 0 || 
                                             borderRadius.indexOf('999') >= 0 || 
                                             borderRadius.indexOf('1000') >= 0 ||
                                             (Math.abs(width - height) < 15 && width > 40 && width < 400));
                            
                            if (!isCircular || width < 40 || height < 40) continue;
                            
                            // 매니저/멤버 영역인지 확인
                            var parentText = '';
                            var current = el;
                            for (var p = 0; p < 10 && current; p++) {
                                var text = (current.innerText || current.textContent || '').replace(/\s/g, '');
                                if (text.indexOf('매니저') >= 0 || text.indexOf('Manager') >= 0) {
                                    parentText = 'manager';
                                    break;
                                }
                                if (text.indexOf('멤버') >= 0 || text.indexOf('Member') >= 0) {
                                    parentText = 'member';
                                    break;
                                }
                                current = current.parentElement;
                            }
                            
                            // 선택 상태 확인 (배경색이 이미 파란색/초록색이면 건드리지 않음)
                            var bgColorLower = bgColor.toLowerCase();
                            var isBlue = bgColorLower.indexOf('rgb(224') >= 0 || bgColorLower.indexOf('#e0e6ff') >= 0;
                            var isGreen = bgColorLower.indexOf('rgb(224') >= 0 && bgColorLower.indexOf('245') >= 0 || 
                                         bgColorLower.indexOf('#e0f5e0') >= 0;
                            
                            // 이미 선택된 색상이면 건드리지 않음
                            if (isBlue || isGreen) continue;
                            
                            // 회색 배경 감지
                            var isGray = (bgColorLower.indexOf('rgb(245') >= 0 || 
                                         bgColorLower.indexOf('rgb(240') >= 0 || 
                                         bgColorLower.indexOf('rgb(229') >= 0 ||
                                         bgColorLower.indexOf('rgb(243') >= 0 ||
                                         bgColorLower.indexOf('rgb(238') >= 0 ||
                                         bgColorLower.indexOf('rgb(250') >= 0 ||
                                         bgColorLower.indexOf('rgb(251') >= 0 ||
                                         bgColorLower.indexOf('rgb(252') >= 0 ||
                                         bgColorLower.indexOf('rgb(253') >= 0 ||
                                         bgColorLower.indexOf('rgb(254') >= 0 ||
                                         bgColorLower.indexOf('gray') >= 0 ||
                                         bgColorLower.indexOf('grey') >= 0 ||
                                         bgColorLower === 'rgb(245, 246, 248)' ||
                                         bgColorLower === 'rgba(245, 246, 248, 1)' ||
                                         bgColorLower === 'rgba(245, 246, 248, 0.5)' ||
                                         bgColorLower === 'rgba(245, 246, 248, 0.8)');
                            
                            // 매니저 영역이면 회색 배경 제거, 멤버 영역이면 회색 배경으로 통일 (선택되지 않은 경우)
                            if (parentText === 'manager' && isGray) {
                                el.style.setProperty('background-color', 'transparent', 'important');
                                el.style.setProperty('background', 'transparent', 'important');
                                el.style.setProperty('background-image', 'none', 'important');
                            } else if (parentText === 'member' && isGray) {
                                el.style.setProperty('background-color', '#F5F6F8', 'important');
                                el.style.setProperty('background', '#F5F6F8', 'important');
                                el.style.setProperty('background-image', 'none', 'important');
                            }
                        }
                    } catch(e) {
                        // 에러 무시
                    }
                }
                
                // ✅ 전역 스코프에 노출 (외부에서 호출 가능하도록)
                window.removeGrayBackgroundBehindIcons = removeGrayBackgroundBehindIcons;
                
                function manageLayout() {
                    var active = isAddPlace();

                    // ✅ AddPlace가 아니면 무조건 스크롤락/스타일 해제 + 스크롤 원복
                    if (!active) {
                      removeScrollLock();
                      removeStyles();
                      forceRestoreScroll();
                      return;
                    }

                    // ✅ 다른 화면의 스타일 제거
                    cleanupOtherScreens();
                    
                    if (active) {
                        applyStyles();
                        installScrollLock();

                        // ✅ 질문 텍스트를 직접 찾아서 여백 제거 및 표시 보장 (더 적극적으로)
                        function removeTopSpacing() {
                            var questionNodes = document.querySelectorAll('h1, h2, h3, p, div, span, *');
                            for (var q = 0; q < questionNodes.length; q++) {
                                var qEl = questionNodes[q];
                                var qText = (qEl.innerText || qEl.textContent || '').replace(/\s/g, '');
                                if (qText.indexOf('어떤목적으로사용하시나요') >= 0) {
                                    // ✅ 질문 텍스트가 보이도록 보장
                                    qEl.style.setProperty('display', 'block', 'important');
                                    qEl.style.setProperty('visibility', 'visible', 'important');
                                    qEl.style.setProperty('opacity', '1', 'important');
                                    qEl.style.setProperty('height', 'auto', 'important');
                                    qEl.style.setProperty('max-height', 'none', 'important');
                                    
                                    // 질문 텍스트와 모든 부모 요소의 여백 제거 (10단계까지)
                                    var current = qEl;
                                    for (var level = 0; level < 10 && current; level++) {
                                        current.style.setProperty('margin-top', '0', 'important');
                                        current.style.setProperty('padding-top', '0', 'important');
                                        current.style.setProperty('margin-bottom', '0', 'important');
                                        // 부모 요소도 보이도록 보장
                                        current.style.setProperty('display', 'block', 'important');
                                        current.style.setProperty('visibility', 'visible', 'important');
                                        current = current.parentElement;
                                    }
                                }
                            }
                            
                            // ✅ 모든 상단 요소들의 여백도 제거
                            var topElements = document.querySelectorAll('header, nav, [role="banner"], [class*="Header"], [class*="header"]');
                            for (var t = 0; t < topElements.length; t++) {
                                var topEl = topElements[t];
                                topEl.style.setProperty('margin-top', '0', 'important');
                                topEl.style.setProperty('padding-top', '0', 'important');
                                topEl.style.setProperty('margin-bottom', '0', 'important');
                                topEl.style.setProperty('padding-bottom', '0', 'important');
                                topEl.style.setProperty('min-height', 'auto', 'important');
                                topEl.style.setProperty('height', 'auto', 'important');
                            }
                        }
                        
                        removeTopSpacing();
                        setTimeout(removeTopSpacing, 50);
                        setTimeout(removeTopSpacing, 150);
                        setTimeout(removeTopSpacing, 300);

                        // ✅ 질문 텍스트를 직접 찾아서 표시 보장
                        var questionText = null;
                        var allElements = document.querySelectorAll('h1, h2, h3, p, div, span, *');
                        for (var qIdx = 0; qIdx < allElements.length; qIdx++) {
                            var qEl = allElements[qIdx];
                            var qText = (qEl.innerText || qEl.textContent || '').replace(/\s/g, '');
                            if (qText.indexOf('어떤목적으로사용하시나요') >= 0) {
                                questionText = qEl;
                                // 질문 텍스트가 보이도록 강제
                                qEl.style.setProperty('display', 'block', 'important');
                                qEl.style.setProperty('visibility', 'visible', 'important');
                                qEl.style.setProperty('opacity', '1', 'important');
                                qEl.style.setProperty('height', 'auto', 'important');
                                qEl.style.setProperty('max-height', 'none', 'important');
                                qEl.style.setProperty('overflow', 'visible', 'important');
                                
                                // 부모 요소들도 보이도록 보장
                                var parent = qEl.parentElement;
                                for (var pLevel = 0; pLevel < 5 && parent; pLevel++) {
                                    parent.style.setProperty('display', 'block', 'important');
                                    parent.style.setProperty('visibility', 'visible', 'important');
                                    parent.style.setProperty('overflow', 'visible', 'important');
                                    parent = parent.parentElement;
                                }
                                break;
                            }
                        }

                        var content = findContentWrapper();
                        if (content && !content.classList.contains('db-force-content-pos')) {
                            content.classList.add('db-force-content-pos');
                            // 콘텐츠 래퍼가 보이도록 보장
                            content.style.setProperty('display', 'block', 'important');
                            content.style.setProperty('visibility', 'visible', 'important');
                            content.style.setProperty('opacity', '1', 'important');
                            
                            // 콘텐츠 래퍼와 모든 부모 요소의 여백도 제거 (10단계까지)
                            var wrapper = content;
                            for (var w = 0; w < 10 && wrapper; w++) {
                                wrapper.style.setProperty('margin-top', '0', 'important');
                                wrapper.style.setProperty('padding-top', '0', 'important');
                                wrapper.style.setProperty('display', 'block', 'important');
                                wrapper.style.setProperty('visibility', 'visible', 'important');
                                wrapper = wrapper.parentElement;
                            }
                        }

                        // ✅ 매니저/멤버 선택 상태에 따른 배경색 변경
                        function applyManagerMemberBackgroundColors() {
                            try {
                                // 모든 원형 요소 찾기
                                var allElements = document.querySelectorAll('*');
                                for (var i = 0; i < allElements.length; i++) {
                                    var el = allElements[i];
                                    if (!el || !el.getBoundingClientRect) continue;
                                    
                                    var rect = el.getBoundingClientRect();
                                    if (rect.width <= 0 || rect.height <= 0) continue;
                                    
                                    var style = window.getComputedStyle(el);
                                    var borderRadius = style.borderRadius || '';
                                    
                                    // 원형 요소인지 확인
                                    var isCircular = (borderRadius.indexOf('50%') >= 0 || 
                                                     borderRadius.indexOf('999') >= 0 || 
                                                     borderRadius.indexOf('1000') >= 0 ||
                                                     (Math.abs(rect.width - rect.height) < 20 && rect.width > 40 && rect.width < 400));
                                    
                                    if (!isCircular || rect.width < 40 || rect.height < 40) continue;
                                    
                                    // 매니저/멤버 텍스트 찾기 (현재 요소와 자식 요소에서)
                                    var text = (el.innerText || el.textContent || '').replace(/\s/g, '');
                                    var isManager = (text.indexOf('매니저') >= 0 || text.indexOf('Manager') >= 0);
                                    var isMember = (text.indexOf('멤버') >= 0 || text.indexOf('Member') >= 0);
                                    
                                    if (!isManager && !isMember) {
                                        // 부모 요소에서도 확인
                                        var parent = el.parentElement;
                                        for (var p = 0; p < 3 && parent; p++) {
                                            var parentText = (parent.innerText || parent.textContent || '').replace(/\s/g, '');
                                            if (parentText.indexOf('매니저') >= 0 || parentText.indexOf('Manager') >= 0) {
                                                isManager = true;
                                                break;
                                            }
                                            if (parentText.indexOf('멤버') >= 0 || parentText.indexOf('Member') >= 0) {
                                                isMember = true;
                                                break;
                                            }
                                            parent = parent.parentElement;
                                        }
                                    }
                                    
                                    if (!isManager && !isMember) continue;
                                    
                                    // 선택 상태 확인 (체크마크, border 색상, 배경색 등)
                                    var borderColor = style.borderColor || '';
                                    var bgColor = style.backgroundColor || '';
                                    var bgColorLower = bgColor.toLowerCase();
                                    
                                    // 하단에 체크마크가 있는지 확인
                                    var hasCheckmarkBelow = false;
                                    var nextSibling = el.nextElementSibling;
                                    if (nextSibling) {
                                        var checkmarkSvg = nextSibling.querySelector('svg, path[d*="M"], path[d*="m"]');
                                        if (checkmarkSvg) {
                                            var checkmarkRect = checkmarkSvg.getBoundingClientRect();
                                            if (checkmarkRect.width > 0 && checkmarkRect.height > 0) {
                                                hasCheckmarkBelow = true;
                                            }
                                        }
                                    }
                                    
                                    // 내부에 체크마크가 있는지 확인
                                    var hasCheckmarkInside = el.querySelector('svg, path[d*="M"], path[d*="m"]') !== null;
                                    
                                    // 선택 상태 판단
                                    var isSelected = el.getAttribute('aria-selected') === 'true' ||
                                                   el.classList.contains('selected') ||
                                                   el.classList.contains('active') ||
                                                   hasCheckmarkBelow ||
                                                   hasCheckmarkInside ||
                                                   borderColor.indexOf('rgb(59') >= 0 || // 파란색 border
                                                   borderColor.indexOf('rgb(37') >= 0 ||
                                                   borderColor.indexOf('rgb(96') >= 0 || // 파란색 border (다른 색상)
                                                   bgColorLower.indexOf('#e0e6ff') >= 0 || // 이미 파란색 배경
                                                   bgColorLower.indexOf('#e0f5e0') >= 0 || // 이미 초록색 배경
                                                   bgColorLower.indexOf('rgb(224, 230, 255)') >= 0 || // 파란색 배경
                                                   bgColorLower.indexOf('rgb(224, 245, 224)') >= 0; // 초록색 배경
                                    
                                    if (isSelected) {
                                        if (isManager) {
                                            // 매니저 선택 시: 밝은 파란색 배경 (#E0E6FF)
                                            el.style.setProperty('background-color', '#E0E6FF', 'important');
                                            el.style.setProperty('background', '#E0E6FF', 'important');
                                            el.style.setProperty('border-color', '#C0CCFF', 'important');
                                        } else if (isMember) {
                                            // 멤버 선택 시: 밝은 초록색 배경 (#E0F5E0)
                                            el.style.setProperty('background-color', '#E0F5E0', 'important');
                                            el.style.setProperty('background', '#E0F5E0', 'important');
                                            el.style.setProperty('border-color', '#B0E0B0', 'important');
                                        }
                                    } else {
                                        // 선택되지 않은 경우 기본 배경 (투명)
                                        var currentBg = bgColorLower;
                                        // 이미 파란색이나 초록색이 아니면 투명으로 설정
                                        if (currentBg.indexOf('#e0e6ff') < 0 && 
                                            currentBg.indexOf('#e0f5e0') < 0 &&
                                            currentBg.indexOf('rgb(224, 230, 255)') < 0 &&
                                            currentBg.indexOf('rgb(224, 245, 224)') < 0) {
                                            el.style.setProperty('background-color', 'transparent', 'important');
                                            el.style.setProperty('background', 'transparent', 'important');
                                        }
                                    }
                                }
                            } catch(e) {
                                console.log('MANAGER_MEMBER_BG_ERR', e && e.message);
                            }
                        }
                        
                        // ✅ 전역 함수 호출 (안전하게 호출)
                        try {
                          if (typeof removeGrayBackgroundBehindIcons === 'function') {
                            removeGrayBackgroundBehindIcons();
                          } else if (typeof window.removeGrayBackgroundBehindIcons === 'function') {
                            window.removeGrayBackgroundBehindIcons();
                          }
                        } catch(e) {}
                        
                        setTimeout(function() {
                          try {
                            if (typeof removeGrayBackgroundBehindIcons === 'function') {
                              removeGrayBackgroundBehindIcons();
                            } else if (typeof window.removeGrayBackgroundBehindIcons === 'function') {
                              window.removeGrayBackgroundBehindIcons();
                            }
                          } catch(e) {}
                        }, 50);
                        setTimeout(function() {
                          try {
                            if (typeof removeGrayBackgroundBehindIcons === 'function') {
                              removeGrayBackgroundBehindIcons();
                            } else if (typeof window.removeGrayBackgroundBehindIcons === 'function') {
                              window.removeGrayBackgroundBehindIcons();
                            }
                          } catch(e) {}
                        }, 100);
                        setTimeout(function() {
                          try {
                            if (typeof removeGrayBackgroundBehindIcons === 'function') {
                              removeGrayBackgroundBehindIcons();
                            } else if (typeof window.removeGrayBackgroundBehindIcons === 'function') {
                              window.removeGrayBackgroundBehindIcons();
                            }
                          } catch(e) {}
                        }, 200);
                        setTimeout(function() {
                          try {
                            if (typeof removeGrayBackgroundBehindIcons === 'function') {
                              removeGrayBackgroundBehindIcons();
                            } else if (typeof window.removeGrayBackgroundBehindIcons === 'function') {
                              window.removeGrayBackgroundBehindIcons();
                            }
                          } catch(e) {}
                        }, 300);
                        setTimeout(function() {
                          try {
                            if (typeof removeGrayBackgroundBehindIcons === 'function') {
                              removeGrayBackgroundBehindIcons();
                            } else if (typeof window.removeGrayBackgroundBehindIcons === 'function') {
                              window.removeGrayBackgroundBehindIcons();
                            }
                          } catch(e) {}
                        }, 500);
                        setTimeout(function() {
                          try {
                            if (typeof removeGrayBackgroundBehindIcons === 'function') {
                              removeGrayBackgroundBehindIcons();
                            } else if (typeof window.removeGrayBackgroundBehindIcons === 'function') {
                              window.removeGrayBackgroundBehindIcons();
                            }
                          } catch(e) {}
                        }, 800);
                        
                        // ✅ 매니저/멤버 선택 시 배경색 변경
                        applyManagerMemberBackgroundColors();
                        setTimeout(applyManagerMemberBackgroundColors, 50);
                        setTimeout(applyManagerMemberBackgroundColors, 100);
                        setTimeout(applyManagerMemberBackgroundColors, 200);
                        setTimeout(applyManagerMemberBackgroundColors, 300);
                        setTimeout(applyManagerMemberBackgroundColors, 500);
                        
                        // ✅ 클릭 이벤트 리스너 추가 (실시간 배경색 변경)
                        if (!window.__db_manager_member_bg_listeners_added__) {
                            window.__db_manager_member_bg_listeners_added__ = true;
                            document.addEventListener('click', function(e) {
                                setTimeout(function() {
                                    applyManagerMemberBackgroundColors();
                                }, 50);
                            }, true);
                            
                            // MutationObserver로 DOM 변경 감지
                            var bgObserver = new MutationObserver(function() {
                                setTimeout(function() {
                                    applyManagerMemberBackgroundColors();
                                }, 100);
                            });
                            if (document.body) {
                                bgObserver.observe(document.body, {
                                    childList: true,
                                    subtree: true,
                                    attributes: true,
                                    attributeFilter: ['class', 'style', 'aria-selected']
                                });
                            }
                        }

                        // ✅ 매니저/멤버 옵션 간격 조정 (여러 번 실행하여 확실히 적용)
                        adjustOptionSpacing();
                        setTimeout(adjustOptionSpacing, 100);
                        setTimeout(adjustOptionSpacing, 300);
                        setTimeout(adjustOptionSpacing, 500);

                        var btn = findNextBtn();
                        if (btn) {
                            if (!btn.classList.contains(MOVED_BTN_CLASS)) {
                                btn.classList.add(MOVED_BTN_CLASS);

                                // ✅ body로 보내면 클릭이 죽을 수 있어 React root 안으로만 이동
                                var host = pickReactRootHost();
                                if (host) host.appendChild(btn);
                                else document.body.appendChild(btn); // (최후 fallback)
                            }
                        }

                        var backBtn = findBackBtn();
                        if (backBtn) {
                            // ✅ 반응형 계산 - 뒤로가기 버튼을 아래로 내림
                            var backDown = window.getResponsivePx ? window.getResponsivePx(BACK_BTN_DOWN_BASE, 'height') : BACK_BTN_DOWN_BASE;
                            backBtn.style.setProperty('transform', 'translateY(' + backDown + 'px)', 'important');
                            backBtn.style.setProperty('z-index', '2147483647', 'important');
                        }
                    } else {
                        // --- 청소 모드 ---
                        var ghostBtns = document.querySelectorAll('.' + MOVED_BTN_CLASS);
                        for (var i=0; i<ghostBtns.length; i++) {
                            ghostBtns[i].parentNode.removeChild(ghostBtns[i]);
                        }
                    }
                }

                // ✅ 화면 크기 변경 시에도 재계산되도록 리사이즈 이벤트 추가
                if (!window.__dangbun_addplace_resize_handler__) {
                    window.__dangbun_addplace_resize_handler__ = true;
                    var resizeTimer;
                    window.addEventListener('resize', function() {
                        clearTimeout(resizeTimer);
                        resizeTimer = setTimeout(function() {
                            if (isAddPlace()) {
                                applyStyles(); // 스타일 재계산
                            }
                        }, 100);
                    });
                }

                // ✅ 상단 여백 제거도 주기적으로 실행
                if (!window.__dangbun_remove_top_spacing_interval__) {
                    window.__dangbun_remove_top_spacing_interval__ = setInterval(function() {
                        if (isAddPlace()) {
                            var questionNodes = document.querySelectorAll('h1, h2, h3, p, div, span, *');
                            for (var q = 0; q < questionNodes.length; q++) {
                                var qEl = questionNodes[q];
                                var qText = (qEl.innerText || qEl.textContent || '').replace(/\s/g, '');
                                if (qText.indexOf('어떤목적으로사용하시나요') >= 0) {
                                    // ✅ 질문 텍스트가 보이도록 보장
                                    qEl.style.setProperty('display', 'block', 'important');
                                    qEl.style.setProperty('visibility', 'visible', 'important');
                                    qEl.style.setProperty('opacity', '1', 'important');
                                    
                                    var current = qEl;
                                    for (var level = 0; level < 10 && current; level++) {
                                        current.style.setProperty('margin-top', '0', 'important');
                                        current.style.setProperty('padding-top', '0', 'important');
                                        current.style.setProperty('display', 'block', 'important');
                                        current.style.setProperty('visibility', 'visible', 'important');
                                        current = current.parentElement;
                                    }
                                }
                            }
                            var topElements = document.querySelectorAll('header, nav, [role="banner"], [class*="Header"], [class*="header"]');
                            for (var t = 0; t < topElements.length; t++) {
                                var topEl = topElements[t];
                                topEl.style.setProperty('margin-top', '0', 'important');
                                topEl.style.setProperty('padding-top', '0', 'important');
                            }
                        }
                    }, 100);
                }

                // ✅ 간격 조정도 주기적으로 실행
                if (!window.__dangbun_adjust_spacing_interval__) {
                  window.__dangbun_adjust_spacing_interval__ = setInterval(function() {
                    if (isAddPlace()) {
                      adjustOptionSpacing();
                    }
                  }, 200);
                }
                
                // ✅ 회색 배경 제거도 주기적으로 실행
                if (!window.__dangbun_remove_gray_bg_interval__) {
                  window.__dangbun_remove_gray_bg_interval__ = setInterval(function() {
                    if (isAddPlace()) {
                      try {
                        if (typeof removeGrayBackgroundBehindIcons === 'function') {
                          removeGrayBackgroundBehindIcons();
                        } else if (typeof window.removeGrayBackgroundBehindIcons === 'function') {
                          window.removeGrayBackgroundBehindIcons();
                        }
                      } catch(e) {}
                    }
                  }, 200);
                }

                setInterval(manageLayout, 100);
                manageLayout();

              } catch(e) {}
            })();
            """.trimIndent()
    }
}
