package com.example.dangbun.ui.webview

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DangbunWebViewScreen(
    url: String = "https://dangbun-frontend-virid.vercel.app/",
    onClose: () -> Unit,
) {
    // 환경 객체 가져오기
    val context = LocalContext.current

    // WebView에 대한 기록 남겨두도록
    val webView =
        remember {
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true

                settings.javaScriptCanOpenWindowsAutomatically = true

                val defaultUa = settings.userAgentString
                settings.userAgentString = "$defaultUa Mobile"

                // 화면에 맞게 축소/리사이즈 (잘림 방지)
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true

                // 사용자가 필요하면 손으로 축소/확대 가능
                settings.setSupportZoom(true)
                settings.builtInZoomControls = true
                settings.displayZoomControls = false

                webViewClient =
                    object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            url: String,
                        ): Boolean {
                            return handleUrl(context, url)
                        }
                    }

                webChromeClient =
                    object : WebChromeClient() {
                        private var injected = false

                        override fun onProgressChanged(view: WebView, newProgress: Int) {
                            super.onProgressChanged(view, newProgress)

                            // 너무 이르면 head가 없을 수 있어서 15~30 정도가 안전
                            if (!injected && newProgress >= 20) {
                                injected = true

                                val js = """
                    (function() {
                      var meta = document.querySelector('meta[name="viewport"]');
                      if (!meta) {
                        meta = document.createElement('meta');
                        meta.name = 'viewport';
                        document.head.appendChild(meta);
                      }
                      meta.content = 'width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no';

                      document.documentElement.style.width = '100%';
                      document.body.style.width = '100%';
                      document.body.style.margin = '0';
                      document.body.style.overflowX = 'hidden';

                      var style = document.getElementById('__wv_fix__');
                      if (!style) {
                        style = document.createElement('style');
                        style.id = '__wv_fix__';
                        style.innerHTML = '*{max-width:100vw; box-sizing:border-box;}';
                        document.head.appendChild(style);
                      }

                      // ✅ 중요: viewport 주입 후 강제 리레이아웃 트리거
                      window.dispatchEvent(new Event('resize'));
                    })();
                """.trimIndent()

                                view.evaluateJavascript(js, null)
                            }
                        }
                    }

                loadUrl(url)
            }
        }

    BackHandler {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            // 뒤로 갈 페이지가 없으면 WebView 화면을 닫는 동작을 실행
            onClose()
        }
    }

    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars),
        factory = { webView },
    )
}

private fun handleUrl(
    context: android.content.Context,
    url: String,
): Boolean {
    // http/https는 WebView가 계속 로드하도록 false
    if (url.startsWith("http://") || url.startsWith("https://")) {
        return false
    }

    // 그 외 스킴(tel:, mailto:, intent: 등)은 외부 앱으로 위임
    return try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        // 처리할 앱이 없다면 WebView가 처리하도록 넘김(또는 true로 막기)
        false
    }
}
