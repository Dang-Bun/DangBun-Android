package com.example.dangbun.ui.webview.fixes.splash

import android.webkit.WebView
import com.example.dangbun.ui.webview.SPLASH_BG_HEX

internal object SplashFix {
    internal fun injectSplashFix(view: WebView) {
        val js =
            """
            (function() {
              // ✅ JS 파싱 에러 나면 try/catch로도 못 잡히니까, 구조 단순하게 유지
              try {
                var BG = '${SPLASH_BG_HEX}';
                var STYLE_ID = '__db_splash_fix_css__';

                function setViewport(scale) {
                  try {
                    var meta = document.querySelector('meta[name="viewport"]');
                    if (!meta) {
                      meta = document.createElement('meta');
                      meta.name = 'viewport';
                      document.head.appendChild(meta);
                    }
                    meta.setAttribute(
                      'content',
                      'width=device-width, initial-scale=' + scale + ', maximum-scale=' + scale + ', user-scalable=no'
                    );
                  } catch(e) {}
                }

                function isRootPath() {
                  var p = (location && location.pathname) ? location.pathname : '';
                  return (p === '/' || p === '' || p === '/index.html');
                }

                function hasNextButton() {
                  var btns = document.querySelectorAll('button,a,[role="button"]');
                  for (var i = 0; i < btns.length; i++) {
                    var t = (btns[i].innerText || '').trim();
                    if (t === '다음') return true;
                  }
                  return false;
                }

                function isSplash() {
                  if (!isRootPath()) return false;

                  // 텍스트에 '당번'이 있고, '다음' 버튼이 없으면 스플래시로 간주
                  var txt = (document.body && document.body.innerText) ? document.body.innerText : '';
                  if (txt.indexOf('당번') < 0) return false;
                  if (hasNextButton()) return false;

                  return true;
                }

                function ensureStyle() {
                  var style = document.getElementById(STYLE_ID);
                  if (!style) {
                    style = document.createElement('style');
                    style.id = STYLE_ID;
                    document.head.appendChild(style);
                  }

                  // ✅ html에 data attribute 달아두고, CSS는 그 때만 강제 적용 (reset이 안전해짐)
                  style.textContent = `
                    html[data-db-splash="1"], html[data-db-splash="1"] body {
                      margin: 0 !important;
                      padding: 0 !important;
                      width: 100% !important;
                      height: 100% !important;
                      background: ${'$'}{BG} !important;
                      overflow: hidden !important;
                    }

                    html[data-db-splash="1"] #root,
                    html[data-db-splash="1"] #__next,
                    html[data-db-splash="1"] #app {
                      position: fixed !important;
                      inset: 0 !important;
                      width: 100% !important;
                      height: 100% !important;
                      background: ${'$'}{BG} !important;

                      /* ✅ 중앙 정렬 */
                      display: flex !important;
                      flex-direction: column !important;
                      justify-content: center !important;
                      align-items: center !important;

                      /* ✅ 다른 Fix에서 남긴 topShift/transform 제거 */
                      transform: none !important;
                      margin-top: 0 !important;
                      top: 0 !important;

                      box-sizing: border-box !important;
                      padding-top: env(safe-area-inset-top) !important;
                      padding-bottom: env(safe-area-inset-bottom) !important;
                      padding-left: env(safe-area-inset-left) !important;
                      padding-right: env(safe-area-inset-right) !important;
                    }
                  `;
                }

                function applySplash() {
                  if (window.__db_splash_applied__) return;
                  window.__db_splash_applied__ = true;

                  document.documentElement.setAttribute('data-db-splash', '1');
                  ensureStyle();

                  // ✅ 스플래시는 "이중 스케일" 방지: 1.0 고정
                  setViewport(1.0);

                  console.log('[SPLASH_FIX] applied');
                }

                function resetSplash() {
                  if (!window.__db_splash_applied__) return;
                  window.__db_splash_applied__ = false;

                  document.documentElement.removeAttribute('data-db-splash');

                  // ✅ 다른 화면은 기존 정책대로(너 프로젝트 기준 0.8이 기본이면 0.8로 복귀)
                  // 필요 시 여기 값을 너가 쓰는 기본 스케일로 바꿔도 됨.
                  setViewport(0.8);

                  console.log('[SPLASH_FIX] reset');
                }

                function check() {
                  if (isSplash()) applySplash();
                  else resetSplash();
                }

                check();
                setTimeout(check, 50);
                setTimeout(check, 150);
                setTimeout(check, 300);
                setTimeout(check, 700);

                if (!window.__db_splash_hooked__) {
                  window.__db_splash_hooked__ = true;

                  var _ps = history.pushState;
                  history.pushState = function() {
                    var r = _ps.apply(this, arguments);
                    setTimeout(check, 0);
                    return r;
                  };

                  var _rs = history.replaceState;
                  history.replaceState = function() {
                    var r = _rs.apply(this, arguments);
                    setTimeout(check, 0);
                    return r;
                  };

                  window.addEventListener('popstate', function(){ setTimeout(check, 0); });
                  window.addEventListener('resize', function(){ setTimeout(check, 0); });

                  var mo = new MutationObserver(function(){ check(); });
                  mo.observe(document.documentElement, { childList:true, subtree:true });
                }
              } catch(e) {
                console.log('[SPLASH_FIX] error', e);
              }
            })();
            """.trimIndent()

        view.evaluateJavascript(js, null)
    }
}
