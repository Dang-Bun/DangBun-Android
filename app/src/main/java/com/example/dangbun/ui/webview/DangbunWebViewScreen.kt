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
     * ✅ (기존) 회색 상단띠/배경 강제 주입
     * - myplace / placemake 에서만 사용
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
     * ✅ (수정) addPlace 진입 시:
     * 1) 이전에 깔린 스타일 제거
     * 2) 회색 배경 강제 적용 (흰색/회색 혼재 문제 해결)
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

        // ✅ 화면 전환 시 이전 화면의 스타일 제거 (청소)
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
                  '__db_onboarding_top_inset_fix__'
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
                  // 온보딩이 아니면 초기화
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

        // ✅ 배경색 로직 수정 (placemake1은 흰색으로!)
        containerBg =
            when {
                path.contains("myplace") -> Color(0xFFF5F6F8)
                path.contains("placemake1") -> Color.White // 🔥 [수정] 흰색
                path.contains("placemake") -> Color(0xFFF5F6F8) // 나머지 placemake2,3은 회색
                path.contains("addplace") -> Color(0xFFF5F6F8)
                else -> Color.White
            }

        // ✅ 회색 배경 강제 주입 로직
        if (path.contains("addplace")) {
            injectAddPlaceGrayBackground(view)
        } else if ((path.contains("placemake") && !path.contains("placemake1")) || path.contains("myplace")) {
            // placemake1은 흰색이므로 여기서 제외, 나머지는 회색 강제
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
            // 🔥 [수정] inject 대신 debug를 호출합니다.
            // PlaceMake1TopInsetFix.inject(view, contentStartTop = 80)
            PlaceMake1TopInsetFix.debug(view)
        }
        if (path.contains("placemake2")) {
            // raisePx 대신 contentStartTop을 사용하세요.
            // 140은 상단 여백(px)입니다. 화면에 맞게 조절 가능합니다.
            PlaceMake2TopInsetFix.inject(view, contentStartTop = 140)
        }
        if (path.contains("placemake3")) {
            PlaceMake3TopInsetFix.inject(view, downPx = 120)
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
                                    applyRouteFix(detectedPath, this@apply)
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

                        override fun onPageFinished(
                            view: WebView,
                            url: String,
                        ) {
                            super.onPageFinished(view, url)
                            view.post { view.scrollTo(0, 0) }

                            val path = runCatching { Uri.parse(url).path.orEmpty() }.getOrDefault("")

                            // ✅ 공통 픽스
                            injectCommonFixes(view)
                            injectSplashFix(view)
                            if (url.contains("kakao.com")) injectKakaoLtrFix(view)

                            // ✅ 페이지 로드 시 라우터 픽스 적용
                            applyRouteFix(path, view)

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
