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
    // 환경 객체 가져오기
    val context = LocalContext.current

    // WebView에 대한 기록 남겨두도록
    val webView =
        remember {
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient =
                    object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            url: String,
                        ): Boolean {
                            return handleUrl(context, url)
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
