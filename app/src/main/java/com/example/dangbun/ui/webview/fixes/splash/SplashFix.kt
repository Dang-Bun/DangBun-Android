package com.example.dangbun.ui.webview.fixes.splash

import android.webkit.WebView
import com.example.dangbun.ui.webview.SPLASH_BG_HEX
import com.example.dangbun.ui.webview.fixes.common.ResponsiveUtils

internal object SplashFix {
    internal fun injectSplashFix(view: WebView) {
        val js =
            """
            (function() {
              try {
                // ✅ 반응형 유틸리티 로드
                if (!window.__dangbun_responsive_utils__) {
                  ${ResponsiveUtils.getResponsiveJs()}
                }

                var BG = '${SPLASH_BG_HEX}';

                function ensureViewportScale() {
                  try {
                    var meta = document.querySelector('meta[name="viewport"]');
                    if (!meta) {
                      meta = document.createElement('meta');
                      meta.name = 'viewport';
                      document.head.appendChild(meta);
                    }
                    // ✅ 화면 크기에 따라 동적으로 scale 계산
                    var scale = window.getResponsiveScale ? window.getResponsiveScale() : 0.8;
                    meta.content = 'width=device-width, initial-scale=' + scale + ', maximum-scale=1.0, user-scalable=no';
                  } catch(e) {}
                }

                function isRootPath() {
                  var p = (location && location.pathname) ? location.pathname : '';
                  return (p === '/' || p === '' || p === '/index.html');
                }

                function isSplash() {
                  if (!isRootPath()) return false;
                  var txt = (document.body && document.body.innerText) ? document.body.innerText : '';
                  if (txt.indexOf('당번') < 0) return false;

                  // '다음' 버튼이 있으면 스플래시가 아님(온보딩)
                  var btns = document.querySelectorAll('button,a,[role="button"]');
                  for (var i=0; i<btns.length; i++) {
                    var t = (btns[i].innerText || '').trim();
                    if (t === '다음') return false;
                  }
                  return true;
                }

                function applySplashCenter() {
                  if (window.__dangbun_splash_center_applied__) return;
                  window.__dangbun_splash_center_applied__ = true;

                  ensureViewportScale();

                  var h = window.innerHeight || 0;
                  var H = h > 0 ? (h + 'px') : '100vh';

                  document.documentElement.style.background = BG;
                  document.body.style.background = BG;

                  document.documentElement.style.height = H;
                  document.body.style.height = H;
                  document.body.style.minHeight = H;

                  document.body.style.margin = '0';
                  document.body.style.overflow = 'hidden';

                  var root = document.querySelector('#root') || document.querySelector('#__next') || document.querySelector('#app');
                  var host = root || document.body;

                  host.style.backgroundColor = BG;
                  host.style.height = H;
                  host.style.minHeight = H;

                  host.style.display = 'flex';
                  host.style.flexDirection = 'column';
                  host.style.justifyContent = 'center';
                  host.style.alignItems = 'center';

                  host.style.width = '100%';
                  host.style.maxWidth = '100vw';
                  host.style.padding = '0';
                  host.style.boxSizing = 'border-box';

                  var child = host.firstElementChild;
                  if (child) {
                    child.style.margin = 'auto';
                  }
                  console.log('WV_SPLASH_CENTER_APPLIED');
                }

                function resetSplashCenter() {
                  // ✅ 온보딩 화면에서는 상단 여백 최소화 (건너뛰기 버튼이 보이도록)
                  try {
                    var root2 = document.querySelector('#root') || document.querySelector('#__next') || document.querySelector('#app');
                    var main2 = document.querySelector('main');
                    var host2 = main2 || root2;

                    if (host2) {
                      // ✅ 상단 여백을 최소화 (기존 60px -> 0px)
                      host2.style.paddingTop = '15px';
                      host2.style.boxSizing = 'border-box';
                    }
                  } catch(e) {}

                  if (!window.__dangbun_splash_center_applied__) return;
                  window.__dangbun_splash_center_applied__ = false;

                  document.documentElement.style.background = '';
                  document.body.style.background = '';

                  var root = document.querySelector('#root') || document.querySelector('#__next') || document.querySelector('#app');
                  var host = root || document.body;

                  host.style.height = '';
                  host.style.display = '';
                  host.style.flexDirection = '';
                  host.style.justifyContent = '';
                  host.style.alignItems = '';
                  host.style.width = '';
                  host.style.maxWidth = '';
                  host.style.boxSizing = '';
                  host.style.backgroundColor = '';

                  document.body.style.backgroundColor = '';
                  document.documentElement.style.backgroundColor = '';

                  document.body.style.margin = '';
                  document.documentElement.style.height = '';
                  document.body.style.height = '';
                  document.body.style.overflow = '';
                }

                function check() {
                  ensureViewportScale();
                  if (isSplash()) applySplashCenter();
                  else resetSplashCenter();
                }

                check();
                setTimeout(check, 50);
                setTimeout(check, 150);
                setTimeout(check, 300);
                setTimeout(check, 700);

                if (!window.__dangbun_viewport_hooked__) {
                  window.__dangbun_viewport_hooked__ = true;

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

                  var mo = new MutationObserver(function(){ check(); });
                  mo.observe(document.documentElement, { childList:true, subtree:true });
                }

              } catch(e) {}
            })();
            """.trimIndent()

        view.evaluateJavascript(js, null)
    }
}
