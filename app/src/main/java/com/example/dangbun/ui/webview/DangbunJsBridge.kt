package com.example.dangbun.ui.webview

import android.content.Context
import android.content.Intent
import android.webkit.JavascriptInterface

internal class DangbunJsBridge(
    private val context: Context,
) {
    @JavascriptInterface
    fun shareText(text: String?) {
        val shareText = (text ?: "").trim()
        if (shareText.isEmpty()) return

        val intent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
        val chooser = Intent.createChooser(intent, "공유하기")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
