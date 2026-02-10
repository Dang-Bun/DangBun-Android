package com.example.dangbun.ui.webview

import android.annotation.SuppressLint
import android.app.Activity
import android.net.Uri
import android.net.http.SslError
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsControllerCompat
import com.example.dangbun.ui.webview.fixes.addplace.MyPlaceAddFix
import com.example.dangbun.ui.webview.fixes.addplace.PlaceJoin1LayoutFix
import com.example.dangbun.ui.webview.fixes.addplace.PlaceMake1TopInsetFix
import com.example.dangbun.ui.webview.fixes.addplace.PlaceMake2TopInsetFix
import com.example.dangbun.ui.webview.fixes.addplace.PlaceMake3ShareFix
import com.example.dangbun.ui.webview.fixes.addplace.PlaceMake3TopInsetFix
import android.graphics.Color as AColor

private const val TAG = "DANGBUN_WV"

// ✅ 스플래시 배경(첨부 이미지 근사)
const val SPLASH_BG_HEX = "#6A84F4"

// ✅ 공통 색
private const val GRAY_BG_HEX = "#F5F6F8"
private const val WHITE_BG_HEX = "#FFFFFF"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DangbunWebViewScreen(
    url: String = "https://dangbun-frontend-virid.vercel.app/",
    onClose: () -> Unit,
    applyStatusBarPadding: Boolean = false,
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // ✅ 라우터별 컨테이너 배경
    var containerBg by remember { mutableStateOf(Color.White) }

    // ✅ status bar도 containerBg 색과 동일하게
    DisposableEffect(containerBg, activity) {
        val window = activity?.window
        val prevColor = window?.statusBarColor

        if (window != null) {
            window.statusBarColor = containerBg.toArgb()
            WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        }

        onDispose {
            if (window != null && prevColor != null) {
                window.statusBarColor = prevColor
            }
        }
    }

    /**
     * ✅ DOM 상태 스냅샷 로깅
     */
    fun logDomSnapshot(view: WebView, label: String) {
        val js =
            """
            (function() {
              try {
                var t = document.body ? (document.body.innerText || '') : '';
                var hasDangbun = (t.indexOf('당번') >= 0);
                var root = document.getElementById('__next') || document.getElementById('root');
                var rootChildren = root ? root.children.length : -1;

                return JSON.stringify({
                  label: "$label",
                  href: location.href,
                  path: location.pathname,
                  readyState: document.readyState,
                  bodyTextLen: t.length,
                  hasDangbunText: hasDangbun,
                  imgCount: (document.images ? document.images.length : 0),
                  rootChildren: rootChildren
                });
              } catch(e) {
                return JSON.stringify({ label: "$label", error: String(e) });
              }
            })();
            """.trimIndent()

        view.evaluateJavascript(js) { result ->
            Log.d(TAG, "DOM_SNAPSHOT: $result")
        }
    }

    /**
     * ✅ 루트(/) 스플래시에서만: "중앙정렬 컨테이너"를 찾아 아래로 내림
     * - 다른 화면 영향 없음 (styleId가 cleanup에 의해 제거됨)
     * - JS 문법 깨짐 방지: 템플릿 리터럴/중괄호 혼용 없이 순수 문자열로 작성
     */
    fun injectSplashOffsetFix(view: WebView, shiftPx: Int) {
        val safeShift = shiftPx.coerceIn(0, 400)

        val js =
            """
            (function() {
              try {
                var p = (location.pathname || '');
                if (!(p === '/' || p === '')) return;

                var styleId = '__db_splash_offset_fix__';
                var style = document.getElementById(styleId);
                if (!style) {
                  style = document.createElement('style');
                  style.id = styleId;
                  document.head.appendChild(style);
                }

                // ✅ 1) 기본적으로 root 바로 아래를 아래로 내림
                // ✅ 2) "당번" 텍스트를 포함한 가장 가까운 상위 컨테이너를 추가로 아래로 내림
                var css = '';
                css += 'html, body { height: 100% !important; }';
                css += 'body { margin: 0 !important; padding: 0 !important; }';
                css += '#__next, #root { min-height: 100vh !important; }';
                css += '#__next > *, #root > * { transform: translateY(' + $safeShift + 'px) !important; }';

                // 텍스트 기반 타겟팅 (DOM 구조가 바뀌어도 최대한 따라가게)
                var target = null;
                try {
                  var walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT, null, false);
                  while (walker.nextNode()) {
                    var node = walker.currentNode;
                    if (node && node.nodeValue && node.nodeValue.indexOf('당번') >= 0) {
                      target = node.parentElement;
                      break;
                    }
                  }
                } catch(e) {}

                if (target) {
                  // 너무 작은 span 같은 거면 상위로 끌어올림
                  var el = target;
                  for (var i = 0; i < 6; i++) {
                    if (!el || !el.parentElement) break;
                    var r = el.getBoundingClientRect();
                    // 화면 상단에 너무 붙어있으면 더 큰 컨테이너로
                    if (r && r.height < 120) el = el.parentElement;
                  }
                  css += ' ' + (el ? ('#' + el.id) : '') + ' ';
                  // id가 없으면 class/inline 타겟 대신 직접 스타일 부여
                  try {
                    el.style.setProperty('transform', 'translateY(' + $safeShift + 'px)', 'important');
                    el.style.setProperty('will-change', 'transform', 'important');
                  } catch(e) {}
                }

                style.textContent = css;

                console.log('DB_SPLASH_OFFSET_APPLIED', $safeShift);
              } catch(e) {
                console.log('DB_SPLASH_OFFSET_ERROR', String(e));
              }
            })();
            """.trimIndent()

        view.evaluateJavascript(js, null)
    }

    /**
     * ✅ (기존) 회색 상단띠/배경 강제 주입
     */
    fun injectGrayTopBandKiller(view: WebView) {
        val js =
            """
            (function () {
              var GRAY_BG = '${GRAY_BG_HEX}';
              var styleId = '__db_gray_topband_killer__';
              var style = document.getElementById(styleId);
              if (!style) {
                style = document.createElement('style');
                style.id = styleId;
                document.head.appendChild(style);
              }

              style.textContent =
                'html, body { background:' + GRAY_BG + ' !important; }' +
                'body { margin:0 !important; padding:0 !important; }' +
                '#root, #__next, main { background:' + GRAY_BG + ' !important; min-height:100vh !important; }' +
                'header, nav, [role="banner"] { background:' + GRAY_BG + ' !important; }' +
                '[class*="Header"], [class*="header"], [class*="AppBar"], [class*="appbar"], [class*="Top"], [class*="top"] { background:' + GRAY_BG + ' !important; }' +
                '[style*="safe-area-inset-top"], [style*="env(safe-area-inset-top)"] { background:' + GRAY_BG + ' !important; }' +
                'body:before {' +
                  'content:""; position:fixed; left:0; top:0; right:0; height:220px;' +
                  'background:' + GRAY_BG + '; z-index:-1;' +
                '}' +
                '[class*="SafeArea"], [class*="safearea"], [class*="Inset"], [class*="inset"] { background:' + GRAY_BG + ' !important; }'
              ;

              document.documentElement.style.backgroundColor = GRAY_BG;
              if (document.body) document.body.style.backgroundColor = GRAY_BG;
            })();
            """.trimIndent()

        view.evaluateJavascript(js, null)
    }

    /**
     * ✅ addPlace 진입 시 회색 배경 강제
     */
    fun injectAddPlaceGrayBackground(view: WebView) {
        val js =
            """
            (function () {
              var whiteStyle = document.getElementById('__db_addplace_white_bg__');
              if (whiteStyle && whiteStyle.parentNode) {
                whiteStyle.parentNode.removeChild(whiteStyle);
              }
              var grayStyle = document.getElementById('__db_gray_topband_killer__');
              if (grayStyle && grayStyle.parentNode) {
                grayStyle.parentNode.removeChild(grayStyle);
              }

              var GRAY_BG = '${GRAY_BG_HEX}';
              var styleId = '__db_addplace_gray_bg__';
              var style = document.getElementById(styleId);
              if (!style) {
                style = document.createElement('style');
                style.id = styleId;
                document.head.appendChild(style);
              }

              style.textContent =
                'html, body { background:' + GRAY_BG + ' !important; }' +
                'body { margin:0 !important; padding:0 !important; }' +
                '#root, #__next, main { background:' + GRAY_BG + ' !important; min-height:100vh !important; }' +
                'header, nav, [role="banner"] { background:' + GRAY_BG + ' !important; }' +
                '[class*="Header"], [class*="header"], [class*="AppBar"], [class*="appbar"], [class*="Top"], [class*="top"] { background:' + GRAY_BG + ' !important; }' +
                '[class*="SafeArea"], [class*="safearea"], [class*="Inset"], [class*="inset"] { background:' + GRAY_BG + ' !important; }' +
                'body:before { content:none !important; }'
              ;

              document.documentElement.style.backgroundColor = GRAY_BG;
              if (document.body) document.body.style.backgroundColor = GRAY_BG;
            })();
            """.trimIndent()

        view.evaluateJavascript(js, null)
    }

    // ✅ 라우터 적용 (페이지 로드 + SPA 이동 모두 동일 처리)
    fun applyRouteFix(
        pathRaw: String,
        view: WebView,
    ) {
        val path = pathRaw.lowercase()

        // ✅ 루트(/)는 스플래시: 청소 JS 금지 + 루트에서만 아래로 내리는 Fix 주입
        if (path.isBlank() || path == "/") {
            containerBg = Color(0xFF6A84F4)
            Log.d(TAG, "ROOT_ROUTE: skip cleanup js, path=$pathRaw")

            // ✅ 여기서만 스플래시 위치 보정 (다른 화면 영향 없음)
            injectSplashOffsetFix(view, shiftPx = 320)

            return
        }

        // ✅ (루트가 아닐 때만) 이전 화면 스타일/클래스 제거 (청소)
        view.evaluateJavascript(
            """
            (function() {
              try {
                // 스타일 제거
                var styleIds = [
                  '__db_placemake1_top_inset_fix__',
                  '__db_placemake2_top_inset_fix__',
                  '__db_placemake3_top_inset_fix__',
                  '__db_addplace_gray_bg__',
                  '__db_gray_topband_killer__',
                  '__db_onboarding_top_inset_fix__',
                  '__db_splash_offset_fix__' // ✅ 루트 스플래시 보정 스타일 제거
                ];
                for (var s = 0; s < styleIds.length; s++) {
                  var styleEl = document.getElementById(styleIds[s]);
                  if (styleEl && styleEl.parentNode) {
                    styleEl.parentNode.removeChild(styleEl);
                  }
                }

                // 클래스 제거
                var classesToRemove = [
                  'db-back-button-fixed',
                  'db-next-button-fixed',
                  'db-placemake2-content-raise',
                  'db-force-content-pos',
                  'db-next-btn-moved-to-body'
                ];
                for (var c = 0; c < classesToRemove.length; c++) {
                  var elements = document.querySelectorAll('.' + classesToRemove[c]);
                  for (var i = 0; i < elements.length; i++) {
                    try {
                      elements[i].classList.remove(classesToRemove[c]);
                    } catch(e) {}
                  }
                }

                // 음수 margin 제거
                var mainElements = document.querySelectorAll('main, #root, #__next, body, html');
                for (var j = 0; j < mainElements.length; j++) {
                  var el = mainElements[j];
                  var computedStyle = window.getComputedStyle(el);
                  var marginTop = computedStyle.marginTop;
                  if (marginTop && (marginTop.indexOf('-') >= 0 || parseFloat(marginTop) < -10)) {
                    el.style.setProperty('margin-top', '0', 'important');
                  }
                }

                // 스타일 초기화
                var bodyElements = document.querySelectorAll('html, body, #root, #__next, main');
                for (var k = 0; k < bodyElements.length; k++) {
                  var elem = bodyElements[k];
                  if (elem.tagName === 'HTML' || elem.tagName === 'BODY') {
                    elem.style.setProperty('overflow-y', 'auto', 'important');
                    elem.style.setProperty('overflow-x', 'auto', 'important');
                    elem.style.setProperty('height', 'auto', 'important');
                    elem.style.setProperty('max-height', 'none', 'important');
                    elem.style.setProperty('touch-action', 'auto', 'important');
                  } else {
                    elem.style.setProperty('overflow', 'visible', 'important');
                  }
                }

                // 고정 버튼 초기화
                var fixedButtons = document.querySelectorAll('button[style*="position: fixed"]');
                for (var b = 0; b < fixedButtons.length; b++) {
                  var btn = fixedButtons[b];
                  var currentPath = (location.pathname || '').toLowerCase();
                  if (currentPath.indexOf('onboarding') >= 0) {
                     btn.style.setProperty('position', 'relative', 'important');
                     btn.style.setProperty('bottom', 'auto', 'important');
                  }
                }
              } catch(e) {}
            })();
            """.trimIndent(),
            null
        )

        // ✅ 배경색
        containerBg =
            when {
                path.contains("myplace") -> Color(0xFFF5F6F8)
                path.contains("placemake1") -> Color.White
                path.contains("placemake") -> Color(0xFFF5F6F8)
                path.contains("addplace") -> Color(0xFFF5F6F8)
                else -> Color.White
            }

        // ✅ 회색 배경 강제 주입
        if (path.contains("addplace")) {
            injectAddPlaceGrayBackground(view)
        } else if ((path.contains("placemake") && !path.contains("placemake1")) || path.contains("myplace")) {
            injectGrayTopBandKiller(view)
        }

        // ✅ MyPlace 라우터 픽스
        if (path.contains("myplace")) {
            injectMyPlaceUnifiedFix(view)
        }

        // ✅ addPlace 라우터 픽스
        if (path.contains("addplace")) {
            MyPlaceAddFix.inject(view)
        }

        // ✅ placemake 라우터
        if (path.contains("placemake1")) {
            PlaceMake1TopInsetFix.debug(view)
        }
        if (path.contains("placemake2")) {
            PlaceMake2TopInsetFix.inject(view, contentStartTop = 60)
        }
        if (path.contains("placemake3")) {
            PlaceMake3TopInsetFix.inject(view)
            PlaceMake3ShareFix.inject(view)
        }

        if (path.contains("placejoin1")) {
            PlaceJoin1LayoutFix.inject(view, raisePx = 170, liftBottomPx = 24)
        }

        // ✅ 온보딩 화면
        if (path.contains("onboarding")) {
            injectOnboardingTopInsetFix(view, topPx = 0)
        }
    }

    val webView =
        remember {
            WebView(context).apply {
                Log.d(TAG, "WebView init, startUrl=$url")

                // ✅ WebView 자체 배경 투명
                setBackgroundColor(AColor.TRANSPARENT)
                background = null
                overScrollMode = WebView.OVER_SCROLL_NEVER

                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = false
                settings.setSupportZoom(true)
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                settings.javaScriptCanOpenWindowsAutomatically = true
                settings.setSupportMultipleWindows(true)
                settings.cacheMode = WebSettings.LOAD_DEFAULT

                val defaultUa = settings.userAgentString
                settings.userAgentString = "$defaultUa Mobile"

                webChromeClient =
                    object : WebChromeClient() {
                        override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                            val msg = consoleMessage.message() ?: ""
                            Log.e(
                                TAG,
                                "WV_CONSOLE(${consoleMessage.messageLevel()}): $msg " +
                                    "(${consoleMessage.sourceId()}:${consoleMessage.lineNumber()})",
                            )

                            // ✅ SPA 이동 감지
                            if (msg.startsWith("SPA_NAV_DETECTED")) {
                                val detectedPath = msg.removePrefix("SPA_NAV_DETECTED").trim()
                                this@apply.post {
                                    Log.d(TAG, "WV_SPA_NAV: path=$detectedPath")
                                    applyRouteFix(detectedPath, this@apply)
                                    logDomSnapshot(this@apply, "SPA_NAV:$detectedPath")
                                }
                            }
                            return super.onConsoleMessage(consoleMessage)
                        }
                    }

                webViewClient =
                    object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest,
                        ): Boolean {
                            return handleUrl(context, request.url.toString(), view)
                        }

                        override fun onPageStarted(
                            view: WebView,
                            url: String,
                            favicon: android.graphics.Bitmap?
                        ) {
                            super.onPageStarted(view, url, favicon)
                            Log.d(TAG, "WV_PAGE_STARTED: $url")
                        }

                        override fun onPageCommitVisible(view: WebView, url: String) {
                            super.onPageCommitVisible(view, url)
                            Log.d(TAG, "WV_PAGE_COMMIT_VISIBLE: $url")
                            logDomSnapshot(view, "COMMIT_VISIBLE")
                        }

                        override fun onReceivedHttpError(
                            view: WebView,
                            request: WebResourceRequest,
                            errorResponse: android.webkit.WebResourceResponse
                        ) {
                            super.onReceivedHttpError(view, request, errorResponse)
                            Log.e(
                                TAG,
                                "WV_HTTP_ERROR: url=${request.url} status=${errorResponse.statusCode} reason=${errorResponse.reasonPhrase}"
                            )
                        }

                        override fun onReceivedError(
                            view: WebView,
                            request: WebResourceRequest,
                            error: android.webkit.WebResourceError
                        ) {
                            super.onReceivedError(view, request, error)
                            Log.e(
                                TAG,
                                "WV_WEB_ERROR: url=${request.url} code=${error.errorCode} desc=${error.description}"
                            )
                        }

                        override fun onReceivedSslError(
                            view: WebView,
                            handler: SslErrorHandler,
                            error: SslError
                        ) {
                            super.onReceivedSslError(view, handler, error)
                            Log.e(TAG, "WV_SSL_ERROR: primaryError=${error.primaryError} url=${error.url}")
                        }

                        override fun onPageFinished(
                            view: WebView,
                            url: String,
                        ) {
                            super.onPageFinished(view, url)
                            view.post { view.scrollTo(0, 0) }

                            val path = runCatching { Uri.parse(url).path.orEmpty() }.getOrDefault("")
                            val isRoot = path.isBlank() || path == "/"

                            // ✅ 스플래시 픽스는 항상 주입 (JS 내부에서 splash 여부 판단)
                            injectSplashFix(view)

                            // ✅ 루트(/)에서는 CommonFix 차단
                            if (!isRoot) {
                                injectCommonFixes(view)
                                if (url.contains("kakao.com")) injectKakaoLtrFix(view)
                            }

                            // ✅ 라우터 픽스 적용
                            applyRouteFix(path, view)

                            Log.d(TAG, "WV_PAGE_FINISHED: url=$url path=$path")
                            logDomSnapshot(view, "FINISHED")
                            view.postDelayed({ logDomSnapshot(view, "FINISHED+300ms") }, 300)
                            view.postDelayed({ logDomSnapshot(view, "FINISHED+1500ms") }, 1500)

                            // ✅ SPA 네비게이션 감지 설치
                            view.evaluateJavascript(
                                """
                                (function() {
                                  if (window.__dangbun_spa_hook__) return;
                                  window.__dangbun_spa_hook__ = true;
                                  var notify = function() {
                                    console.log('SPA_NAV_DETECTED', location.pathname);
                                  };
                                  var _ps = history.pushState;
                                  history.pushState = function() { _ps.apply(this, arguments); notify(); };
                                  var _rs = history.replaceState;
                                  history.replaceState = function() { _rs.apply(this, arguments); notify(); };
                                  window.addEventListener('popstate', notify);
                                })();
                                """.trimIndent(),
                                null,
                            )
                        }
                    }

                addJavascriptInterface(DangbunJsBridge(context), "DangbunBridge")
                loadUrl(url)
            }
        }

    BackHandler {
        if (webView.canGoBack()) webView.goBack() else onClose()
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(containerBg),
    ) {
        AndroidView(
            modifier =
                if (applyStatusBarPadding) {
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                } else {
                    Modifier.fillMaxSize()
                },
            factory = { webView },
        )
    }
}
