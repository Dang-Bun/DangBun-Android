package com.example.dangbun.ui.webview

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Message
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

private const val TAG = "DANGBUN_WV"

private const val TARGET_SCALE = 0.8f

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DangbunWebViewScreen(
    url: String = "https://dangbun-frontend-virid.vercel.app/",
    onClose: () -> Unit,
) {
    val context = LocalContext.current

    val webView = remember {
        WebView(context).apply {
            Log.d(TAG, "WebView init, startUrl=$url")

            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true

            // ✅ 화면 맞춤
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = false

            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false

            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.setSupportMultipleWindows(true)

            val defaultUa = settings.userAgentString
            settings.userAgentString = "$defaultUa Mobile"

            webChromeClient = object : WebChromeClient() {
                override fun onCreateWindow(
                    view: WebView,
                    isDialog: Boolean,
                    isUserGesture: Boolean,
                    resultMsg: Message,
                ): Boolean {
                    Log.d(TAG, "onCreateWindow(isDialog=$isDialog, isUserGesture=$isUserGesture)")
                    val transport = resultMsg.obj as WebView.WebViewTransport
                    transport.webView = view
                    resultMsg.sendToTarget()
                    return true
                }

                override fun onReceivedTitle(view: WebView, title: String?) {
                    super.onReceivedTitle(view, title)
                    Log.d(TAG, "onReceivedTitle(title=$title) url=${view.url}")
                }
            }

            webViewClient = object : WebViewClient() {

                // 구버전
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    Log.d(TAG, "shouldOverrideUrlLoading(url=$url) currentUrl=${view.url}")
                    val handled = handleUrl(context, url, view)
                    Log.d(TAG, "shouldOverrideUrlLoading handled=$handled")
                    return handled
                }

                // 신버전
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean {
                    val nextUrl = request.url.toString()
                    Log.d(
                        TAG,
                        "shouldOverrideUrlLoading(requestUrl=$nextUrl) currentUrl=${view.url}"
                    )
                    val handled = handleUrl(context, nextUrl, view)
                    Log.d(TAG, "shouldOverrideUrlLoading handled=$handled")
                    return handled
                }

                override fun onPageStarted(
                    view: WebView,
                    url: String,
                    favicon: android.graphics.Bitmap?
                ) {
                    super.onPageStarted(view, url, favicon)
                    Log.d(TAG, "onPageStarted(url=$url) scale=${view.scale}")
                }

                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)

                    // ✅ (1) 스케일: WebView zoomBy로만 0.8 유지
                    applyTargetScale(view)

                    Log.d(TAG, "onPageFinished(url=$url) title=${view.title} scale=${view.scale}")

                    // ✅ (2) 공통 보정(팝업 중앙 + vh 보정 + 가로 넘침 방지)
                    injectCommonFixes(view)

                    // ✅ (3) 카카오 입력 커서 방향 보정
                    if (url.contains("kakao.com", ignoreCase = true)) {
                        injectKakaoLtrFix(view)
                    }

                    // ✅ (4) 플레이스 생성 완료 화면 중앙 정렬 보정 (스크롤 컨테이너 기반)
                    injectPlaceCompleteCenterFix(view)
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError,
                ) {
                    super.onReceivedError(view, request, error)
                    Log.e(
                        TAG,
                        "onReceivedError(url=${request.url}) code=${error.errorCode} desc=${error.description}"
                    )
                }
            }

            loadUrl(url)
        }
    }

    BackHandler {
        if (webView.canGoBack()) webView.goBack() else onClose()
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { webView },
    )
}

private fun applyTargetScale(view: WebView) {
    // view.scale 이 0이거나 이상하게 나오는 경우가 있어 방어
    val current = view.scale.takeIf { it > 0f } ?: 1f
    val factor = TARGET_SCALE / current

    // zoomBy는 누적이므로 "목표/현재" 비율로 한번에 보정
    try {
        view.zoomBy(factor)
        Log.d(TAG, "applyTargetScale: current=$current factor=$factor -> scale=${view.scale}")
    } catch (e: Throwable) {
        Log.e(TAG, "applyTargetScale failed: ${e.message}")
    }
}

private fun handleUrl(
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

/**
 * 팝업(모달) 중앙 + vh 보정 + 가로 넘침 방지
 */
private fun injectCommonFixes(view: WebView) {
    val js = """
        (function() {
          try {
            // ✅ viewport는 1.0 고정 (0.8은 Android 쪽 zoomBy로 처리)
            var meta = document.querySelector('meta[name="viewport"]');
            if (!meta) {
              meta = document.createElement('meta');
              meta.name = 'viewport';
              document.head.appendChild(meta);
            }
            meta.content = 'width=device-width, initial-scale=0.8, maximum-scale=1.0, user-scalable=no';

            document.documentElement.style.width = '100%';
            document.body.style.width = '100%';
            document.body.style.margin = '0';
            document.body.style.overflowX = 'hidden';

            // appVh 보정
            function setAppVh() {
              var vh = window.innerHeight * 0.01;
              document.documentElement.style.setProperty('--appVh', vh + 'px');
            }
            setAppVh();
            window.addEventListener('resize', setAppVh);

            var style = document.getElementById('__dangbun_wv_fix__');
            if (!style) {
              style = document.createElement('style');
              style.id = '__dangbun_wv_fix__';
              style.innerHTML = `
                * { box-sizing: border-box; max-width: 100vw; }
                
                  /* ✅ 상단 safe-area 만큼 아래로 내리기 */
/* body는 덮일 수 있어서 의미가 약함 */
body { margin: 0 !important; overflow-x: hidden !important; }

/* ✅ 화면을 실제로 덮는 앱 컨테이너를 아래로 내리기 */
#root, #__next, #app, main {
  padding-top: calc(env(safe-area-inset-top) + 40px) !important;
  padding-bottom: max(env(safe-area-inset-bottom), 72px) !important;
  box-sizing: border-box !important;
}

                /* role 기반 다이얼로그는 중앙 고정 */
                [role="dialog"], [aria-modal="true"] {
                  position: fixed !important;
                  top: 50% !important;
                  left: 50% !important;
                  transform: translate(-50%, -50%) !important;
                  margin: 0 !important;
                  max-width: calc(100vw - 32px) !important;
                  max-height: calc(100vh - 32px) !important;
                }

                /* 흔한 모달 컨테이너들 flex 중앙 정렬 */
                .modal, .Modal, .dialog, .Dialog, .popup, .Popup,
                .MuiModal-root, .MuiDialog-root, .MuiDialog-container,
                .ant-modal-wrap, .ant-modal-root, .swal2-container,
                .ReactModal__Overlay {
                  align-items: center !important;
                  justify-content: center !important;
                }
              `;
              document.head.appendChild(style);
            }

            // 다이얼로그 동적 생성 대응
            if (!window.__dangbun_wv_observer__) {
              window.__dangbun_wv_observer__ = new MutationObserver(function() {
                try {
                  var dialogs = document.querySelectorAll('[role="dialog"], [aria-modal="true"]');
                  dialogs.forEach(function(el) {
                    el.style.position = 'fixed';
                    el.style.top = '50%';
                    el.style.left = '50%';
                    el.style.transform = 'translate(-50%, -50%)';
                    el.style.margin = '0';
                    el.style.maxWidth = 'calc(100vw - 32px)';
                    el.style.maxHeight = 'calc(100vh - 32px)';
                  });
                } catch(e) {}
              });
              window.__dangbun_wv_observer__.observe(document.documentElement, { childList: true, subtree: true });
            }
  
            // ✅ "내 플레이스" 상단 여백 확보: 타이틀이 포함된 fixed/sticky 헤더의 top을 내려줌
            function __dangbun_pushMyPlaceHeader__() {
              try {
                var inset = 20; // ← 원하는 만큼 늘리기: 60, 80...

                // 1) 텍스트가 정확히 '내 플레이스'인 요소 찾기
                var titleEl = null;
                var candidates = document.querySelectorAll('h1,h2,h3,header,div,span,p,button,a');
                for (var i = 0; i < candidates.length; i++) {
                  var t = (candidates[i].innerText || '').trim();
                  if (t === '내 플레이스') { titleEl = candidates[i]; break; }
                }
                if (!titleEl) return;

                // 2) 위로 올라가며 fixed/sticky 조상 찾기
                var cur = titleEl;
                var target = null;
                for (var d = 0; d < 12 && cur; d++) {
                  var st = window.getComputedStyle(cur);
                  if (st && (st.position === 'fixed' || st.position === 'sticky')) {
                    target = cur;
                    break;
                  }
                  cur = cur.parentElement;
                }

                // 3) fixed/sticky가 있으면 top을 내리고, 없으면 타이틀에 marginTop을 줌
                if (target) {
                  if (target.__dangbun_pushed__) return;
                  target.__dangbun_pushed__ = true;

                  target.style.top = 'calc(env(safe-area-inset-top) + ' + inset + 'px)';
                  target.style.zIndex = '999999';

                      // ✅ 헤더 높이만큼 본문(#root/main)에 padding-top을 줘서 콘텐츠 시작을 아래로 밀기
                      var headerH = target.getBoundingClientRect().height || 0;
                      var extra = 12; // ✅ 누락돼서 에러났던 값
                      var pad = Math.ceil(headerH + extra);

                      var root = document.querySelector('#root') || document.querySelector('#__next') || document.querySelector('#app');
                      var main = document.querySelector('main');

                      // 우선순위: root -> main -> body
                      var host = root || main || document.body;

                      // 이미 크게 잡혀있는 padding-top이 있으면 더 큰 값으로만 갱신
                      var curPad = parseFloat((window.getComputedStyle(host).paddingTop || '0').replace('px','')) || 0;
                      if (pad > curPad) {
                        host.style.paddingTop = pad + 'px';
                      }
                } else {
                  // fixed/sticky가 아닌 경우: 타이틀 자체를 아래로
                  titleEl.style.marginTop = 'calc(env(safe-area-inset-top) + ' + inset + 'px)';
                }
              } catch(e) {}
            }

            // ✅ 렌더 타이밍 이슈 대비: 몇 번 재시도
            __dangbun_pushMyPlaceHeader__();
            setTimeout(__dangbun_pushMyPlaceHeader__, 100);
            setTimeout(__dangbun_pushMyPlaceHeader__, 300);
            setTimeout(__dangbun_pushMyPlaceHeader__, 700);

setTimeout(__dangbun_pushMyPlaceHeader__, 100);
setTimeout(__dangbun_pushMyPlaceHeader__, 300);
setTimeout(__dangbun_pushMyPlaceHeader__, 700);

/* ✅ 상단 AppBar(뒤로가기 포함) safe-area 보정 */
function __dangbun_pushTopAppBar__() {
  try {
    var TOP_INSET = 32; // ← 뒤로가기(상단바)를 더 내리고 싶으면 40, 48...

    // 상단에 붙어있는 fixed/sticky 컨테이너 후보들 중 "가장 위에 있고(Top), 가로로 긴" 것을 AppBar로 추정
    var candidates = document.querySelectorAll('header, [role="banner"], div, nav, section');
    var best = null;
    var bestScore = -1;

    for (var i = 0; i < candidates.length; i++) {
      var el = candidates[i];
      var st = window.getComputedStyle(el);
      if (!st) continue;

      // fixed/sticky만 대상으로
      if (st.position !== 'fixed' && st.position !== 'sticky') continue;

      var r = el.getBoundingClientRect();
      if (!r || r.width <= 0 || r.height <= 0) continue;

      // 상단 근처 + 너무 작지 않은 가로 바 형태를 선호
      if (r.top > 40) continue;
      if (r.height > 140) continue;      // 너무 큰 오버레이 제외
      if (r.width < window.innerWidth * 0.6) continue;

      // 점수: 더 위 + 더 넓을수록
      var score = (200 - r.top) + (r.width / window.innerWidth) * 100;
      if (score > bestScore) {
        bestScore = score;
        best = el;
      }
    }

    if (!best) return;

    // 이미 적용했으면 종료
    if (best.__dangbun_appbar_pushed__) return;
    best.__dangbun_appbar_pushed__ = true;

    // top 내려주기
    best.style.top = 'calc(env(safe-area-inset-top) + ' + TOP_INSET + 'px)';
    best.style.zIndex = '999999';

    // AppBar가 내려가면, 본문도 그만큼 내려야 겹침이 사라짐
    var appBarH = best.getBoundingClientRect().height || 0;
    var padTop = Math.ceil(appBarH + TOP_INSET + 8);

    var root = document.querySelector('#root') || document.querySelector('#__next') || document.querySelector('#app');
    var main = document.querySelector('main');
    var host = root || main || document.body;

    var curPad = parseFloat((window.getComputedStyle(host).paddingTop || '0').replace('px','')) || 0;
    if (padTop > curPad) host.style.paddingTop = padTop + 'px';

  } catch(e) {}
}

// 렌더 타이밍 대비 재시도 + DOM 변화 감시
__dangbun_pushTopAppBar__();
setTimeout(__dangbun_pushTopAppBar__, 100);
setTimeout(__dangbun_pushTopAppBar__, 300);
setTimeout(__dangbun_pushTopAppBar__, 700);

if (!window.__dangbun_appbar_ob__) {
  window.__dangbun_appbar_ob__ = new MutationObserver(function() {
    __dangbun_pushTopAppBar__();
  });
  window.__dangbun_appbar_ob__.observe(document.documentElement, { childList: true, subtree: true });
}

// 렌더/SPA 타이밍 대비 재시도
__dangbun_adjustFixedControls__();
setTimeout(__dangbun_adjustFixedControls__, 100);
setTimeout(__dangbun_adjustFixedControls__, 300);
setTimeout(__dangbun_adjustFixedControls__, 700);

/* ✅ 상단(뒤로가기) / 하단(다음) safe-area 보정 */
function __dangbun_adjustFixedControls__() {
  try {
    var TOP_INSET = 16;     // ← 뒤로가기 아이콘을 더 내리고 싶으면 24, 32...
    var BOTTOM_LIFT = 16;   // ← 다음 버튼을 더 올리고 싶으면 24, 32...

    // ---------------------------
    // 1) 상단 왼쪽 "뒤로가기" 아이콘(고정/스티키) 찾기 + top 내리기
    // ---------------------------
    (function adjustBack() {
      var clickable = document.querySelectorAll('button,a,[role="button"],div');
      var best = null;
      var bestScore = -1;

      for (var i = 0; i < clickable.length; i++) {
        var el = clickable[i];
        var r = el.getBoundingClientRect();
        if (!r || r.width <= 0 || r.height <= 0) continue;

        // 상단 왼쪽 근처 + 작은 아이콘 형태
        if (r.top > 60 || r.left > 60) continue;
        if (r.width > 120 || r.height > 120) continue;

        var st = window.getComputedStyle(el);
        if (!st) continue;

        // fixed/sticky 조상까지 포함해서 찾기
        var cur = el, target = null;
        for (var d = 0; d < 8 && cur; d++) {
          var cs = window.getComputedStyle(cur);
          if (cs && (cs.position === 'fixed' || cs.position === 'sticky')) { target = cur; break; }
          cur = cur.parentElement;
        }
        target = target || el;

        var ts = window.getComputedStyle(target);
        if (!ts || (ts.position !== 'fixed' && ts.position !== 'sticky')) continue;

        // 점수: 더 왼쪽/더 위일수록 우선
        var score = 200 - (r.left + r.top);
        if (score > bestScore) { bestScore = score; best = target; }
      }

      if (best && !best.__dangbun_back_pushed__) {
        best.__dangbun_back_pushed__ = true;
        best.style.top = 'calc(env(safe-area-inset-top) + ' + TOP_INSET + 'px)';
        best.style.zIndex = '999999';
      }
    })();

    // ---------------------------
    // 2) 하단 "다음" 버튼(고정/스티키) 찾기 + bottom 올리기
    // ---------------------------
    (function adjustNext() {
      var nextEl = null;
      var candidates = document.querySelectorAll('button,a,[role="button"]');
      for (var i = 0; i < candidates.length; i++) {
        var t = (candidates[i].innerText || '').trim();
        if (t === '다음') { nextEl = candidates[i]; break; }
      }
      if (!nextEl) return;

      // fixed/sticky 조상 찾기
      var cur = nextEl, target = null;
      for (var d = 0; d < 10 && cur; d++) {
        var cs = window.getComputedStyle(cur);
        if (cs && (cs.position === 'fixed' || cs.position === 'sticky')) { target = cur; break; }
        cur = cur.parentElement;
      }
      target = target || nextEl;

      var ts = window.getComputedStyle(target);
      if (!ts) return;

      if (!target.__dangbun_next_lifted__) {
        target.__dangbun_next_lifted__ = true;

        // bottom을 safe-area + lift 만큼 올림
        target.style.bottom = 'calc(env(safe-area-inset-bottom) + ' + BOTTOM_LIFT + 'px)';
        target.style.zIndex = '999999';

        // 혹시 width/left/right가 빡세게 박혀있으면 그대로 두되, 안 보이는 경우 대비
        if (!ts.left || ts.left === 'auto') target.style.left = '0';
        if (!ts.right || ts.right === 'auto') target.style.right = '0';
      }

      // 콘텐츠가 버튼에 가려지는 것까지 방지: root/main padding-bottom을 더 크게
      var root = document.querySelector('#root') || document.querySelector('#__next') || document.querySelector('#app');
      var main = document.querySelector('main');
      var host = root || main || document.body;

      var add = 96; // 하단 버튼 영역 확보(필요하면 120으로)
      var curPb = parseFloat((window.getComputedStyle(host).paddingBottom || '0').replace('px','')) || 0;
      if (add > curPb) host.style.paddingBottom = add + 'px';
    })();

  } catch(e) {}
}

// 렌더/SPA 타이밍 대비 재시도
__dangbun_adjustFixedControls__();
setTimeout(__dangbun_adjustFixedControls__, 100);
setTimeout(__dangbun_adjustFixedControls__, 300);
setTimeout(__dangbun_adjustFixedControls__, 700);

// DOM이 늦게 그려져도 따라가도록 옵저버
if (!window.__dangbun_fixed_controls_ob__) {
  window.__dangbun_fixed_controls_ob__ = new MutationObserver(function() {
    __dangbun_adjustFixedControls__();
  });
  window.__dangbun_fixed_controls_ob__.observe(document.documentElement, { childList: true, subtree: true });
}

window.dispatchEvent(new Event('resize'));

          } catch (e) {}
        })();
    """.trimIndent()

    view.evaluateJavascript(js, null)
}

private fun injectKakaoLtrFix(view: WebView) {
    val js = """
        (function() {
          try {
            var style = document.getElementById('__dangbun_kakao_ltr__');
            if (!style) {
              style = document.createElement('style');
              style.id = '__dangbun_kakao_ltr__';
              style.innerHTML = `
                html, body { direction: ltr !important; }
                input, textarea {
                  direction: ltr !important;
                  unicode-bidi: plaintext !important;
                  text-align: left !important;
                }
              `;
              document.head.appendChild(style);
            }
          } catch (e) {}
        })();
    """.trimIndent()

    view.evaluateJavascript(js, null)
}

private fun injectPlaceCompleteCenterFix(view: WebView) {
    val js = """
        (function() {
          try {
            if (!document.body) return;

            // ✅ 완료 화면 후보 URL (필요하면 /place/complete 같은 경로로 더 좁힐 수 있음)
            var url = location.href || '';
            var urlHint = /place|myplace/i.test(url);
            if (!urlHint) return;

            // ✅ "패치 적용" 함수 (조건 충족 시에만 실행)
            function applyIfComplete() {
              try {
                // 완료 화면을 텍스트로 단정하지 말고, "플레이스로 이동" 같은 버튼 존재로 판단
                var btnTexts = Array.from(document.querySelectorAll('button, a, [role="button"]'))
                  .map(function(el){ return (el.innerText || '').trim(); })
                  .filter(function(t){ return t.length > 0; });

                var hasMoveBtn = btnTexts.some(function(t){ return t.indexOf('플레이스로 이동') >= 0; });
                var hasInviteBtn = btnTexts.some(function(t){ return t.indexOf('참여코드') >= 0; });

                // 둘 중 하나라도 있으면 완료 화면으로 간주
                if (!(hasMoveBtn || hasInviteBtn)) return false;

                var h = window.innerHeight || 0;
                if (h <= 0) return false;

                // ✅ body를 뷰포트 기준 중앙정렬 컨테이너로 강제
                document.documentElement.style.height = h + 'px';
                document.body.style.height = h + 'px';
                document.body.style.minHeight = h + 'px';

                document.body.style.margin = '0';
                document.body.style.display = 'flex';
                document.body.style.flexDirection = 'column';
                document.body.style.justifyContent = 'center';
                document.body.style.alignItems = 'center';
                document.body.style.padding = '16px';
                document.body.style.boxSizing = 'border-box';
                document.body.style.overflowX = 'hidden';

                // ✅ root 폭 제한(너무 커지지 않게)
                var root = document.querySelector('#root') || document.querySelector('#__next') || document.querySelector('#app');
                if (root) {
                  root.style.width = '100%';
                  root.style.maxWidth = '480px';
                  root.style.margin = '0 auto';
                }

                // ✅ "큰 컨테이너"를 찾아 위로 당기는 스타일 제거
                var biggest = null;
                var best = 0;
                var nodes = document.querySelectorAll('div, main, section, article');
                for (var i=0; i<nodes.length; i++) {
                  var n = nodes[i];
                  var r = n.getBoundingClientRect();
                  var area = r.width * r.height;
                  if (r.width > 200 && r.height > 200 && area > best) {
                    best = area;
                    biggest = n;
                  }
                }

                if (biggest) {
                  biggest.style.marginTop = '0';
                  biggest.style.paddingTop = '0';
                  biggest.style.transform = 'none';
                  biggest.style.position = 'relative';
                  biggest.style.top = 'auto';
                  biggest.style.left = 'auto';
                  biggest.style.width = '100%';
                  biggest.style.maxWidth = '480px';
                }

                // ✅ 패치가 먹었음을 표시 (중복 적용 방지)
                window.__dangbun_place_complete_applied__ = true;
                return true;
              } catch(e) {
                return false;
              }
            }

            // ✅ 1) 즉시 1번 시도
            if (window.__dangbun_place_complete_applied__) return;
            if (applyIfComplete()) return;

            // ✅ 2) React 렌더링 지연 대비: 2초 동안 짧게 재시도
            var tries = 0;
            var t = setInterval(function() {
              tries++;
              if (window.__dangbun_place_complete_applied__ || applyIfComplete() || tries >= 20) {
                clearInterval(t);
              }
            }, 100);

            // ✅ 3) DOM 변화 감시: 버튼/화면이 나중에 생겨도 적용
            if (!window.__dangbun_place_complete_ob__) {
              window.__dangbun_place_complete_ob__ = new MutationObserver(function() {
                if (!window.__dangbun_place_complete_applied__) {
                  applyIfComplete();
                }
              });
              window.__dangbun_place_complete_ob__.observe(document.documentElement, { childList: true, subtree: true });
            }

            // ✅ 4) resize에도 높이 보정
            if (!window.__dangbun_place_complete_resize__) {
              window.__dangbun_place_complete_resize__ = function() {
                try {
                  var h2 = window.innerHeight || 0;
                  if (h2 > 0) {
                    document.documentElement.style.height = h2 + 'px';
                    document.body.style.height = h2 + 'px';
                    document.body.style.minHeight = h2 + 'px';
                  }
                } catch(e) {}
              };
              window.addEventListener('resize', window.__dangbun_place_complete_resize__);
            }

          } catch(e) {}
        })();
    """.trimIndent()

    view.evaluateJavascript(js, null)
}
