package com.example.dangbun.core.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────
// 기본 브랜드 컬러 (Figma 기준)
// ─────────────────────────────────────────────
val BluePrimary = Color(0xFF4D83FD)
val Purple = Color(0xFFFA9EFA)
val Mint = Color(0xFF78F5BE)
val GrayLight = Color(0xFFF9F9F9)
val Graybackground = Color(0xFFEAF0FF)

// ─────────────────────────────────────────────
// Dangbun Color Data Class (디자인 토큰)
// ─────────────────────────────────────────────
@Immutable
data class DangbunColors(
    val bluePrimary: Color,
    val purple: Color,
    val mint: Color,
    val grayLight: Color,
    val grayBackground: Color,
)

// ─────────────────────────────────────────────
// 기본 라이트 모드 컬러 세트
// ─────────────────────────────────────────────
val defaultDangbunColors =
    DangbunColors(
        bluePrimary = BluePrimary,
        purple = Purple,
        mint = Mint,
        grayLight = GrayLight,
        grayBackground = Graybackground,
    )

// ─────────────────────────────────────────────
// CompositionLocal Provider
// ─────────────────────────────────────────────
val LocalDangbunColors = staticCompositionLocalOf { defaultDangbunColors }
