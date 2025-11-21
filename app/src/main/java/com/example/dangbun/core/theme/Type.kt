package com.example.dangbun.core.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.example.dangbun.R

// ─────────────────────────────────────────────
// Pretendard Font Family 정의
// ─────────────────────────────────────────────
val DangbunFontRegular = FontFamily(Font(R.font.pretendard_regular))
val DangbunFontMedium = FontFamily(Font(R.font.pretendard_medium))
val DangbunFontSemiBold = FontFamily(Font(R.font.pretendard_semibold))
val DangbunFontBold = FontFamily(Font(R.font.pretendard_bold))
val DangbunFontExtraBold = FontFamily(Font(R.font.pretendard_extrabold))

// ─────────────────────────────────────────────
// Typography Token
// ─────────────────────────────────────────────
@Immutable
data class DangbunTypography(
    val title_SB_24: TextStyle,
    val title_M_20: TextStyle,
    val body_R_16: TextStyle,
    val body_R_14: TextStyle,
    val caption_R_12: TextStyle,
)

// ─────────────────────────────────────────────
// 기본 Typography 값
// ─────────────────────────────────────────────
val defaultDangbunTypography = DangbunTypography(
    title_SB_24 = TextStyle(
        fontFamily = DangbunFontSemiBold,
        fontSize = 24.sp,
        lineHeight = 34.sp,
    ),
    title_M_20 = TextStyle(
        fontFamily = DangbunFontMedium,
        fontSize = 20.sp,
        lineHeight = 30.sp,
    ),
    body_R_16 = TextStyle(
        fontFamily = DangbunFontRegular,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    body_R_14 = TextStyle(
        fontFamily = DangbunFontRegular,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    caption_R_12 = TextStyle(
        fontFamily = DangbunFontRegular,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
)

// ─────────────────────────────────────────────
// CompositionLocal Provider
// ─────────────────────────────────────────────
val LocalDangbunTypography = staticCompositionLocalOf { defaultDangbunTypography }
