package com.example.dangbun.ui.webview

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.dangbun.ui.webview.fixes.addplace.PlaceMake1TopInsetFix
import com.example.dangbun.ui.webview.fixes.addplace.PlaceMake2TopInsetFix
import com.example.dangbun.ui.webview.fixes.addplace.PlaceMake3ShareFix
import com.example.dangbun.ui.webview.fixes.addplace.PlaceMake3TopInsetFix

private const val TAG = "DANGBUN_WV"

// ✅ 스플래시 배경(첨부 이미지 근사)
const val SPLASH_BG_HEX = "#6A84F4"

// ✅ 목표 배율(0.8)
private const val TARGET_SCALE = 0.8f

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DangbunWebViewScreen(
    url: String = "https://dangbun-frontend-virid.vercel.app/",
    onClose: () -> Unit,
    applyStatusBarPadding: Boolean = false,
) {
    val context = LocalContext.current

    // ✅ 라우터별로 "상단 여백(Compose 컨테이너)" 배경을 바꿔줄 상태
    var containerBg by remember { mutableStateOf(Color.White) }

    val webView = remember {
        WebView(context).apply {
            Log.d(TAG, "WebView init, startUrl=$url")
            setBackgroundColor(android.graphics.Color.WHITE)

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
                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
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
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean {
                    return handleUrl(context, request.url.toString(), view)
                }

                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)
                    view.post { view.scrollTo(0, 0) }

                    // ✅ 라우터(path) 추출
                    val path = runCatching { Uri.parse(url).path.orEmpty().lowercase() }.getOrDefault("")

                    // ✅ (핵심) 라우터별로 "상단 여백" 배경색 변경
                    containerBg =
                        when {
                            path.contains("myplace") -> Color(0xFFF5F6F8)     // ✅ 내플레이스: 회색
                            path.contains("addplace") -> Color(0xFFF5F6F8)    // (원하면 같이 회색)
                            path.contains("placemake") -> Color(0xFFF5F6F8)   // (원하면 같이 회색)
                            path.contains("onboarding") -> Color.White        // ✅ 온보딩: 흰색
                            else -> Color.White
                        }

                    // ✅ (항상) 공통 픽스들 - 정말 공통만 남기기
                    injectCommonFixes(view)
                    injectSplashFix(view)
                    if (url.contains("kakao.com")) injectKakaoLtrFix(view)

                    // ✅ 온보딩 라우터에서만: 상단 여백/배경
                    if (path.contains("onboarding")) {
                        if (applyStatusBarPadding) {
                            injectOnboardingTopInsetFix(view, topPx = -45)
                        }
                    }

                    // ✅ MyPlace 라우터에서만: MyPlace 통합 픽스
                    if (path.contains("myplace")) {
                        injectMyPlaceUnifiedFix(view)
                    }

                    // ✅ addPlace 라우터에서만
                    if (path.contains("addplace")) {
                        injectAddPlaceMemberSelectInsetFix(view)
                    }

                    // ✅ placemake 라우터에서만
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

                    // ✅ SPA 네비게이션 감지 (이건 유지)
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

    val webViewModifier =
        if (applyStatusBarPadding) {
            Modifier
                .fillMaxSize()
                .background(containerBg)   // ✅ 상태바 아래 여백 포함해서 이 색으로 칠해짐
                .statusBarsPadding()
                .padding(top = 20.dp)
        } else {
            Modifier
                .fillMaxSize()
                .background(containerBg)
        }

    AndroidView(
        modifier = webViewModifier,
        factory = { webView }
    )
}


