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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

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

                    // ✅ 온보딩: 위로 붙는 현상 완화 (상단 여백)
                    if (applyStatusBarPadding) {
                        injectOnboardingTopInsetFix(view, topPx = 24)
                    }
                    // ✅ 공통 픽스 (모달 등)
                    injectCommonFixes(view)
                    // ✅ 스플래시 픽스
                    injectSplashFix(view)
                    // ✅ 카카오 방향 픽스
                    if (url.contains("kakao.com")) injectKakaoLtrFix(view)

                    // ✅ 내 플레이스 통합 픽스 (상단/하단 간격 유지)
                    injectMyPlaceUnifiedFix(view)
                    // ✅ 멤버 선택 화면 픽스
                    injectAddPlaceMemberSelectInsetFix(view)

                    // ✅ placemake1: 뒤로가기 아래 여백 줄이기

                    injectPlaceMake1TopInsetFix(view, raisePx = 120) // 56부터 시작 (72/84로 올리면 더 당겨짐)


                    // ✅ SPA 네비게이션 감지 후 (콘솔 로그용)
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
                .statusBarsPadding()
                .padding(top = 20.dp)
        } else {
            Modifier.fillMaxSize()
        }

    AndroidView(
        modifier = webViewModifier,
        factory = { webView }
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
