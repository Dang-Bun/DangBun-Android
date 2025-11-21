package com.example.dangbun.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

// Material3 용 Light 색상
private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    secondary = Purple,
    tertiary = Mint,
    background = Graybackground,
    surface = Graybackground
)

// Material3 용 Dark 색상 — 필요하면 다크 모드 추가로 디자인 반영 가능
private val DarkColorScheme = darkColorScheme(
    primary = BluePrimary,
    secondary = Purple,
    tertiary = Mint,
    background = Color(0xFF1A1A1A),
    surface = Color(0xFF1A1A1A)
)

// 전체 앱에 사용할 테마
@Composable
fun dangbunTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(
        LocalDangbunColors provides defaultDangbunColors,
        LocalDangbunTypography provides defaultDangbunTypography
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MaterialTheme.typography,
            content = content
        )
    }
}
