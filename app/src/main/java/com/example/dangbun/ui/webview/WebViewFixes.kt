package com.example.dangbun.ui.webview

import android.webkit.WebView

internal fun injectMyPlaceUnifiedFix(view: WebView) {
    MyPlaceFix.injectMyPlaceUnifiedFix(view)
}

internal fun injectCommonFixes(view: WebView) {
    CommonModalFix.injectCommonFixes(view)
}

internal fun injectSplashFix(view: WebView) {
    SplashFix.injectSplashFix(view)
}

internal fun injectKakaoLtrFix(view: WebView) {
    KakaoFix.injectKakaoLtrFix(view)
}

internal fun injectAddPlaceMemberSelectInsetFix(view: WebView) {
    AddPlaceMemberSelectFix.injectAddPlaceMemberSelectInsetFix(view)
}
