package com.example.dangbun.ui.webview

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.WebView

private const val TAG = "DANGBUN_WV"

internal fun handleUrl(
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
