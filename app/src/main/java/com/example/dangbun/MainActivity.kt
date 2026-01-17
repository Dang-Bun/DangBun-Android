package com.example.dangbun

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.dangbun.ui.webview.DangbunWebViewScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ 1) edge-to-edge 잠시 꺼서 상단 회색빛이 Window/상태바 때문인지 확인
        // enableEdgeToEdge()

        // ✅ 2) Window 배경을 흰색으로 고정 (WebView 밖으로 비치는 영역 제거)
        window.decorView.setBackgroundColor(android.graphics.Color.WHITE)

        setContent {
            DangbunWebViewScreen(
                url = "https://dangbun-frontend-virid.vercel.app/onboarding",
                onClose = { finish() },
                applyStatusBarPadding = true
            )
        }
    }

}

@Composable
fun greeting(
    name: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "Hello $name!",
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
fun greetingPreview() {
    DangbunWebViewScreen(onClose = { })
}
