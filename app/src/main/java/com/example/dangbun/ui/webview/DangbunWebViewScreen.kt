package com.example.dangbun.ui.webview

import android.annotation.SuppressLint
import android.app.Activity
import android.net.Uri
import android.util.Log
import android.webkit.ConsoleMessage
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
import com.example.dangbun.ui.webview.fixes.addplace.PlaceMake1TopInsetFix
import com.example.dangbun.ui.webview.fixes.addplace.PlaceMake2TopInsetFix
import com.example.dangbun.ui.webview.fixes.addplace.PlaceMake3ShareFix
import com.example.dangbun.ui.webview.fixes.addplace.PlaceMake3TopInsetFix
import android.graphics.Color as AColor

private const val TAG = "DANGBUN_WV"

// ✅ 스플래시 배경(첨부 이미지 근사)
const val SPLASH_BG_HEX = "#6A84F4"

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
     * ✅ addPlace/placemake에서 상단 흰띠를 "무조건" 회색으로 덮는 주입
     * - try/catch 사용 안 함 (문법 에러 방지)
     * - 백틱(template literal) 사용 안 함 (문법 에러 방지)
     */
    fun injectGrayTopBandKiller(view: WebView) {
        val js = """
            (function () {
              var GRAY_BG = '#F5F6F8';
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

                /* ✅ 상단 흰띠는 대부분 "헤더/상단 래퍼"가 흰 배경을 들고 있음 */
                'header, nav, [role="banner"] { background:' + GRAY_BG + ' !important; }' +
                '[class*="Header"], [class*="header"], [class*="AppBar"], [class*="appbar"], [class*="Top"], [class*="top"] { background:' + GRAY_BG + ' !important; }' +

                /* ✅ safe-area / inset padding 영역이 흰색으로 깔리는 경우 */
                '[style*="safe-area-inset-top"], [style*="env(safe-area-inset-top)"] { background:' + GRAY_BG + ' !important; }' +

                /* ✅ 최상단 여백/패딩만 흰색인 경우가 많아서 상단쪽 배경을 넓게 회색 처리 */
                'body:before {' +
                  'content:""; position:fixed; left:0; top:0; right:0; height:220px;' +
                  'background:' + GRAY_BG + '; z-index:-1;' +
                '}' +

                /* ✅ 혹시 웹이 상단에 흰색 오버레이를 별도 div로 두는 경우 대비 */
                '[class*="SafeArea"], [class*="safearea"], [class*="Inset"], [class*="inset"] { background:' + GRAY_BG + ' !important; }'
              ;

              // 인라인 스타일로도 한 번 더
              document.documentElement.style.backgroundColor = GRAY_BG;
              if (document.body) document.body.style.backgroundColor = GRAY_BG;
            })();
        """.trimIndent()

        view.evaluateJavascript(js, null)
    }

    // ✅ 라우터 적용 (페이지 로드 + SPA 이동 모두 동일 처리)
    fun applyRouteFix(pathRaw: String, view: WebView) {
        val path = pathRaw.lowercase()

        containerBg =
            when {
                path.contains("myplace") -> Color(0xFFF5F6F8)
                path.contains("addplace") -> Color(0xFFF5F6F8)
                path.contains("placemake") -> Color(0xFFF5F6F8)
                else -> Color.White
            }

        // ✅ 회색 화면군은 "무조건" 상단 흰띠 제거 주입
        if (path.contains("addplace") || path.contains("placemake") || path.contains("myplace")) {
            injectGrayTopBandKiller(view)
        }

        // ✅ MyPlace 라우터 픽스
        if (path.contains("myplace")) {
            injectMyPlaceUnifiedFix(view)
        }

        // ✅ addPlace 라우터 픽스
        // ⚠️ 기존 injectAddPlaceMemberSelectInsetFix(view) 는 현재 /addPlace에서 JS SyntaxError를 유발할 가능성이 높아서 일단 호출하지 않습니다.
        // if (path.contains("addplace")) { injectAddPlaceMemberSelectInsetFix(view) }

        // ✅ placemake 라우터 픽스
        if (path.contains("placemake1")) {
            PlaceMake1TopInsetFix.inject(view, raisePx = 120)
        }
        if (path.contains("placemake2")) {
            PlaceMake2TopInsetFix.inject(view, raisePx = 140)
        }
        if (path.contains("placemake3")) {
            PlaceMake3TopInsetFix.inject(view, downPx = 120)
            PlaceMake3ShareFix.inject(view)
        }
    }

    val webView = remember {
        WebView(context).apply {
            Log.d(TAG, "WebView init, startUrl=$url")

            // ✅ WebView 자체 배경 투명 + 오버스크롤 제거(상단 흰 여백 느낌 줄이기)
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

            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                    val msg = consoleMessage.message() ?: ""
                    Log.e(
                        TAG,
                        "WV_CONSOLE(${consoleMessage.messageLevel()}): $msg " +
                            "(${consoleMessage.sourceId()}:${consoleMessage.lineNumber()})"
                    )

                    // ✅ SPA 이동도 즉시 처리
                    if (msg.startsWith("SPA_NAV_DETECTED")) {
                        val detectedPath = msg.removePrefix("SPA_NAV_DETECTED").trim()
                        this@apply.post {
                            applyRouteFix(detectedPath, this@apply)
                        }
                    }
                    return super.onConsoleMessage(consoleMessage)
                }
            }

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean {
                    return handleUrl(context, request.url.toString(), view)
                }

                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)
                    view.post { view.scrollTo(0, 0) }

                    val path = runCatching { Uri.parse(url).path.orEmpty() }.getOrDefault("")

                    // ✅ 공통 픽스
                    injectCommonFixes(view)
                    injectSplashFix(view)
                    if (url.contains("kakao.com")) injectKakaoLtrFix(view)

                    // ✅ 페이지 로드에서도 동일 적용
                    applyRouteFix(path, view)

                    // ✅ SPA 네비게이션 감지 설치(유지)
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
                        null
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
        modifier = Modifier
            .fillMaxSize()
            .background(containerBg)
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
            factory = { webView }
        )
    }
}
