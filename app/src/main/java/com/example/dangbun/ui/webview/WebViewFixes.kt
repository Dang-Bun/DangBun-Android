package com.example.dangbun.ui.webview

import android.webkit.WebView
import com.example.dangbun.ui.webview.fixes.addplace.AddPlaceMemberSelectFix
import com.example.dangbun.ui.webview.fixes.common.CommonModalFix
import com.example.dangbun.ui.webview.fixes.kakao.KakaoFix
import com.example.dangbun.ui.webview.fixes.myplace.MyPlaceFix
import com.example.dangbun.ui.webview.fixes.onboarding.OnboardingTopInsetFix
import com.example.dangbun.ui.webview.fixes.splash.SplashFix

// ✅ 온보딩(상단 여백 내려오기) 픽스
internal fun injectOnboardingTopInsetFix(view: WebView, topPx: Int = 24) {
    OnboardingTopInsetFix.inject(view, topPx)
}

internal fun injectMyPlaceUnifiedFix(view: WebView) {
    MyPlaceFix.inject(view)
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

