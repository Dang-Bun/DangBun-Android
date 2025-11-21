package com.example.dangbun.core.theme.preview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.dangbun.core.theme.LocalDangbunColors
import com.example.dangbun.core.theme.LocalDangbunTypography
import com.example.dangbun.core.theme.dangbunTheme

@Composable
fun ThemePreviewTest() {
    val colors = LocalDangbunColors.current
    val typo = LocalDangbunTypography.current

    Column(modifier = Modifier.padding(16.dp)) {
        Text("당번 타이틀 24", style = typo.title_SB_24, color = colors.bluePrimary)
        Text("본문 16 테스트", style = typo.body_R_16)
        Text("캡션 12 테스트", style = typo.caption_R_12)
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewDangbunTheme() {
    dangbunTheme {
        ThemePreviewTest()
    }
}
