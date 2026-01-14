package com.example.dangbun.ui.webview

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Message
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

private const val TAG = "DANGBUN_WV"

// ✅ 스플래시 배경(첨부 이미지 근사)
private const val SPLASH_BG_HEX = "#6A84F4"

// ✅ 목표 배율(0.8)
private const val TARGET_SCALE = 0.8f

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DangbunWebViewScreen(
    url: String = "https://dangbun-frontend-virid.vercel.app/",
    onClose: () -> Unit,
) {
    val context = LocalContext.current

    val webView = remember {
        WebView(context).apply {
            Log.d(TAG, "WebView init, startUrl=$url")

            // ✅ WebView가 투명해져서 아래 배경이 비치는 것 방지
            setBackgroundColor(android.graphics.Color.WHITE)

            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true

            // ✅ viewport는 1.0으로 고정 (스케일은 zoomBy로 처리)
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = false

            // zoom은 켜두되, 컨트롤은 숨김
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false

            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.setSupportMultipleWindows(true)

            settings.cacheMode = WebSettings.LOAD_DEFAULT

            val defaultUa = settings.userAgentString
            settings.userAgentString = "$defaultUa Mobile"

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    Log.d(TAG, "onProgressChanged=$newProgress url=${view.url}")
                }

                override fun onCreateWindow(
                    view: WebView,
                    isDialog: Boolean,
                    isUserGesture: Boolean,
                    resultMsg: Message,
                ): Boolean {
                    Log.d(TAG, "onCreateWindow(isDialog=$isDialog, isUserGesture=$isUserGesture)")
                    val transport = resultMsg.obj as WebView.WebViewTransport
                    transport.webView = view
                    resultMsg.sendToTarget()
                    return true
                }

                override fun onReceivedTitle(view: WebView, title: String?) {
                    super.onReceivedTitle(view, title)
                    Log.d(TAG, "onReceivedTitle(title=$title) url=${view.url}")
                }

                override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                    Log.e(
                        TAG,
                        "WV_CONSOLE(${consoleMessage.messageLevel()}): ${consoleMessage.message()} " +
                            "(${consoleMessage.sourceId()}:${consoleMessage.lineNumber()})"
                    )
                    return super.onConsoleMessage(consoleMessage)
                }
            }

            webViewClient = object : WebViewClient() {

                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    Log.d(TAG, "shouldOverrideUrlLoading(url=$url) currentUrl=${view.url}")
                    val handled = handleUrl(context, url, view)
                    Log.d(TAG, "shouldOverrideUrlLoading handled=$handled")
                    return handled
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean {
                    val nextUrl = request.url.toString()
                    Log.d(
                        TAG,
                        "shouldOverrideUrlLoading(requestUrl=$nextUrl) currentUrl=${view.url}"
                    )
                    val handled = handleUrl(context, nextUrl, view)
                    Log.d(TAG, "shouldOverrideUrlLoading handled=$handled")
                    return handled
                }

                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)

                    view.post { view.scrollTo(0, 0) }

                    Log.d(TAG, "onPageFinished(url=$url) title=${view.title} scale=${view.scale}")

                    // ✅ 콘솔 훅 확인
                    view.evaluateJavascript(
                        "console.log('WV_PING', location.href, navigator.userAgent);",
                        null
                    )

                    // ✅ 스플래시인지 판단해서:
                    // 1) 0.8 배율 강제(zoomBy)
                    // 2) 스플래시 중앙정렬(로고 블록을 fixed-center)
                    injectSplashFix(view)

                    // ✅ 배경 체크 로그
                    view.evaluateJavascript(
                        """
                        (function(){
                          try{
                            var bg = getComputedStyle(document.body).backgroundColor;
                            var root = document.querySelector('#root');
                            var rootBg = root ? getComputedStyle(root).backgroundColor : null;
                            return JSON.stringify({ bodyBg:bg, rootBg:rootBg });
                          }catch(e){ return "ERR:"+e.message; }
                        })();
                        """.trimIndent()
                    ) { result ->
                        Log.d(TAG, "WV_BG=$result")
                    }

                    if (url.contains("kakao.com", ignoreCase = true)) {
                        injectKakaoLtrFix(view)
                    }
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError,
                ) {
                    super.onReceivedError(view, request, error)
                    Log.e(
                        TAG,
                        "onReceivedError(url=${request.url}) code=${error.errorCode} desc=${error.description}"
                    )
                }
            }

            loadUrl(url)
        }
    }

    BackHandler {
        if (webView.canGoBack()) webView.goBack() else onClose()
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { webView },
    )
}

private fun handleUrl(
    context: Context,
    url: String,
    webView: WebView,
): Boolean {
    Log.d(TAG, "handleUrl(url=$url)")

    if (url.startsWith("http://") || url.startsWith("https://")) {
        Log.d(TAG, "handleUrl -> http/https, let WebView load")
        return false
    }

    if (url.startsWith("intent:", ignoreCase = true)) {
        return try {
            val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
            context.startActivity(intent)
            Log.d(TAG, "handleUrl -> intent startActivity OK")
            true
        } catch (e: Exception) {
            Log.e(TAG, "handleUrl -> intent failed: ${e.message}")

            try {
                val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                if (!fallbackUrl.isNullOrBlank()) {
                    Log.d(TAG, "handleUrl -> fallbackUrl=$fallbackUrl")
                    webView.loadUrl(fallbackUrl)
                    true
                } else {
                    false
                }
            } catch (e2: Exception) {
                Log.e(TAG, "handleUrl -> fallback parse failed: ${e2.message}")
                false
            }
        }
    }

    return try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
        Log.d(TAG, "handleUrl -> external scheme startActivity OK")
        true
    } catch (e: ActivityNotFoundException) {
        Log.e(TAG, "handleUrl -> no activity for scheme: $url")
        true
    }
}

private fun injectSplashFix(view: WebView) {
    val js = """
        (function() {
          try {
            var BG = '${SPLASH_BG_HEX}';

            function ensureViewportScale() {
              try {
                var meta = document.querySelector('meta[name="viewport"]');
                if (!meta) {
                  meta = document.createElement('meta');
                  meta.name = 'viewport';
                  document.head.appendChild(meta);
                }
                // ✅ 여기: 질문에서 말한 그 설정 그대로 "강제"
                meta.content = 'width=device-width, initial-scale=0.8, maximum-scale=1.0, user-scalable=no';
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

              // ✅ 실제 화면 높이를 px로도 고정 (0.8 스케일에서 남는 영역 방지)
              var h = window.innerHeight || 0;
              var H = h > 0 ? (h + 'px') : '100vh';
              
              // ✅ 배경 확정
              document.documentElement.style.background = BG;
              document.body.style.background = BG;

              // ✅ 스크롤 제거
              document.documentElement.style.height = H;
              document.body.style.height = H;
              document.body.style.minHeight = H;

              document.body.style.margin = '0';
              document.body.style.overflow = 'hidden';

              // ✅ 핵심: React 루트(#root/#__next/#app)를 화면 중앙 flex로!
              var root = document.querySelector('#root') || document.querySelector('#__next') || document.querySelector('#app');
              var host = root || document.body;

              // ✅ host도 배경 + 높이 확정
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
                
                // ✅ 실제 스플래시 컨텐츠(첫 자식)를 더 강하게 정중앙으로
                var child = host.firstElementChild;
                if (child) {
                  child.style.margin = 'auto';
                }
              console.log('WV_SPLASH_CENTER_APPLIED');
            }

            function resetSplashCenter() {
              // ✅ 온보딩(스플래시가 아닌 화면)에서만 상단 여백 살짝 추가
              try {
                var root2 = document.querySelector('#root') || document.querySelector('#__next') || document.querySelector('#app');
                var main2 = document.querySelector('main');
                var host2 = main2 || root2;

                if (host2) {
                  // 필요하면 16~48px 사이로 조절하세요
                  host2.style.paddingTop = '60px';
                  host2.style.boxSizing = 'border-box';
                }
              } catch(e) {}
            
              if (!window.__dangbun_splash_center_applied__) return;
              window.__dangbun_splash_center_applied__ = false;

              // 배경 원복
              document.documentElement.style.background = '';
              document.body.style.background = '';

              // 루트 스타일 원복
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
              // ✅ 스플래시든 아니든 viewport 0.8은 유지(글씨 잘림 방지 목적)
              ensureViewportScale();

              if (isSplash()) applySplashCenter();
              else resetSplashCenter();
            }

            // 최초 + 재시도
            check();
            setTimeout(check, 50);
            setTimeout(check, 150);
            setTimeout(check, 300);
            setTimeout(check, 700);

            // SPA 라우팅/DOM 변화에서도 계속 유지
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


private fun injectKakaoLtrFix(view: WebView) {
    val js = """
        (function() {
          try {
            var style = document.getElementById('__dangbun_kakao_ltr__');
            if (!style) {
              style = document.createElement('style');
              style.id = '__dangbun_kakao_ltr__';
              style.innerHTML = `
                html, body { direction: ltr !important; }
                input, textarea {
                  direction: ltr !important;
                  unicode-bidi: plaintext !important;
                  text-align: left !important;
                }
              `;
              document.head.appendChild(style);
            }
          } catch (e) {}
        })();
    """.trimIndent()

    view.evaluateJavascript(js, null)
}
