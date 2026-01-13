package com.example.dangbun.ui.webview

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DangbunWebViewScreen(
    url: String = "https://dangbun-frontend-virid.vercel.app/",
    onClose: () -> Unit,
) {
    val context = LocalContext.current

    val webView =
        remember {
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true

                // 화면에 맞게 보여주기(기본 확대/축소 보정)
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true

                // 줌 허용(선택)
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

                        override fun onPageFinished(view: WebView, url: String) {
                            super.onPageFinished(view, url)

                            // ✅ 모바일 viewport 강제 + 좌우 클리핑 완화
                            val js =
                                """
                                (function() {
                                  // 1) meta viewport 없으면 만들고, 있으면 덮어쓰기
                                  var meta = document.querySelector('meta[name="viewport"]');
                                  if (!meta) {
                                    meta = document.createElement('meta');
                                    meta.name = 'viewport';
                                    document.head.appendChild(meta);
                                  }
                                  meta.content = 'width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no';

                                  // 2) 문서 폭을 화면에 맞추도록 보정 (좌우 잘림 완화)
                                  document.documentElement.style.width = '100%';
                                  document.body.style.width = '100%';
                                  document.body.style.margin = '0';
                                  document.body.style.overflowX = 'hidden';

                                  // 3) 너무 큰 고정폭 요소가 있으면 화면 밖으로 나가지 않게
                                  var style = document.getElementById('__wv_fix__');
                                  if (!style) {
                                    style = document.createElement('style');
                                    style.id = '__wv_fix__';
                                    style.innerHTML = '*{max-width:100vw; box-sizing:border-box;}';
                                    document.head.appendChild(style);
                                  }
                                })();
                                """.trimIndent()

                            view.evaluateJavascript(js, null)
                        }
                    }

                loadUrl(url)
            }
        }

    BackHandler {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            onClose()
        }
    }

    AndroidView(
        factory = { webView },
    )
}

private fun handleUrl(
    context: android.content.Context,
    url: String,
): Boolean {
    if (url.startsWith("http://") || url.startsWith("https://")) {
        return false
    }

    return try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}
