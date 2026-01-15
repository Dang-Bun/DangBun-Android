package com.example.dangbun.ui.webview

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Message
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
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

// ✅ 스플래시 배경(첨부 이미지 근사)
private const val SPLASH_BG_HEX = "#6A84F4"

// ✅ 목표 배율(0.8)
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

            // ✅ WebView가 투명해져서 아래 배경이 비치는 것 방지
            setBackgroundColor(android.graphics.Color.WHITE)

            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true

            // ✅ viewport는 1.0으로 고정 (스케일은 zoomBy로 처리)
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = false

            // zoom은 켜두되, 컨트롤은 숨김
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false

            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.setSupportMultipleWindows(true)

            settings.cacheMode = WebSettings.LOAD_DEFAULT

            val defaultUa = settings.userAgentString
            settings.userAgentString = "$defaultUa Mobile"

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    Log.d(TAG, "onProgressChanged=$newProgress url=${view.url}")
                }

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

                override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                    Log.e(
                        TAG,
                        "WV_CONSOLE(${consoleMessage.messageLevel()}): ${consoleMessage.message()} " +
                            "(${consoleMessage.sourceId()}:${consoleMessage.lineNumber()})"
                    )
                    return super.onConsoleMessage(consoleMessage)
                }
            }

            webViewClient = object : WebViewClient() {

                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    Log.d(TAG, "shouldOverrideUrlLoading(url=$url) currentUrl=${view.url}")
                    val handled = handleUrl(context, url, view)
                    Log.d(TAG, "shouldOverrideUrlLoading handled=$handled")
                    return handled
                }

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

                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)

                    view.post { view.scrollTo(0, 0) }

                    Log.d(TAG, "onPageFinished(url=$url) title=${view.title} scale=${view.scale}")

                    // ✅ 콘솔 훅 확인
                    view.evaluateJavascript(
                        "console.log('WV_PING', location.href, navigator.userAgent);",
                        null
                    )

                    // ✅ 팝업(모달) 중앙정렬 고정 (위로 붙는 현상 방지)
                    injectCommonFixes(view)

                    // ✅ 스플래시인지 판단해서:
                    // 1) 0.8 배율 강제(zoomBy)
                    // 2) 스플래시 중앙정렬(로고 블록을 fixed-center)
                    injectSplashFix(view)

                    // ✅ 배경 체크 로그
                    view.evaluateJavascript(
                        """
                        (function(){
                          try{
                            var bg = getComputedStyle(document.body).backgroundColor;
                            var root = document.querySelector('#root');
                            var rootBg = root ? getComputedStyle(root).backgroundColor : null;
                            return JSON.stringify({ bodyBg:bg, rootBg:rootBg });
                          }catch(e){ return "ERR:"+e.message; }
                        })();
                        """.trimIndent()
                    ) { result ->
                        Log.d(TAG, "WV_BG=$result")
                    }

                    if (url.contains("kakao.com", ignoreCase = true)) {
                        injectKakaoLtrFix(view)
                    }

                    injectMyPlaceTopInsetFix(view)
                    injectMyPlaceBottomCtaInsetFix(view)
                    injectAddPlaceMemberSelectInsetFix(view)
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
 * ✅ 팝업(모달) 중앙 정렬 + 위로 붙는 현상 방지 (최소 패치)
 * - 전역 padding-top / appbar 이동 같은 부작용 로직 제거
 * - role=dialog / aria-modal 기반 중앙 고정 + overlay flex 중앙만 적용
 * - 중복 주입/로그 폭발 방지: window flag로 1회만 적용
 */
private fun injectCommonFixes(view: WebView) {
    val js = """
        (function() {
          try {
            // ✅ 한 번만 적용(중복 주입/로그 폭발 방지)
            if (window.__dangbun_modal_center_once__) return;
            window.__dangbun_modal_center_once__ = true;

            // ✅ viewport는 기존 목적대로 유지
            var meta = document.querySelector('meta[name="viewport"]');
            if (!meta) {
              meta = document.createElement('meta');
              meta.name = 'viewport';
              document.head.appendChild(meta);
            }
            meta.content = 'width=device-width, initial-scale=0.8, maximum-scale=1.0, user-scalable=no';

            // ✅ 가로 넘침 방지
            document.documentElement.style.width = '100%';
            document.body.style.width = '100%';
            document.body.style.margin = '0';
            document.body.style.overflowX = 'hidden';

            // ✅ 모달 중앙 정렬 CSS
            var style = document.getElementById('__dangbun_modal_center_fix__');
            if (!style) {
              style = document.createElement('style');
              style.id = '__dangbun_modal_center_fix__';
              style.innerHTML = `
                * { box-sizing: border-box; max-width: 100vw; }

                /* ✅ dialog 자체를 중앙 고정 */
                [role="dialog"], [aria-modal="true"] {
                  position: fixed !important;
                  top: 50% !important;
                  left: 50% !important;
                  transform: translate(-50%, -50%) !important;
                  margin: 0 !important;
                  max-width: calc(100vw - 32px) !important;
                  max-height: calc(100vh - 32px) !important;
                }

                /* ✅ overlay/wrap이 flex면 중앙으로 (위로 붙는 원인 제거) */
                .MuiDialog-container,
                .MuiModal-root,
                .ant-modal-wrap,
                .swal2-container,
                .ReactModal__Overlay,
                [data-overlay="true"],
                [class*="overlay"],
                [class*="Overlay"],
                [class*="modal"],
                [class*="Modal"] {
                  align-items: center !important;
                  justify-content: center !important;
                }
              `;
              document.head.appendChild(style);
            }

            // ✅ 늦게 생성되는 다이얼로그도 중앙 보정
            if (!window.__dangbun_modal_center_ob__) {
              window.__dangbun_modal_center_ob__ = new MutationObserver(function() {
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
              window.__dangbun_modal_center_ob__.observe(document.documentElement, { childList: true, subtree: true });
            }

            console.log('WV_MODAL_CENTER_APPLIED_ONCE', location.pathname);

          } catch (e) {}
        })();
    """.trimIndent()

    view.evaluateJavascript(js, null)
}

private fun injectSplashFix(view: WebView) {
    val js = """
        (function() {
          try {
            var BG = '${SPLASH_BG_HEX}';

            function ensureViewportScale() {
              try {
                var meta = document.querySelector('meta[name="viewport"]');
                if (!meta) {
                  meta = document.createElement('meta');
                  meta.name = 'viewport';
                  document.head.appendChild(meta);
                }
                meta.content = 'width=device-width, initial-scale=0.8, maximum-scale=1.0, user-scalable=no';
              } catch(e) {}
            }

            function isRootPath() {
              var p = (location && location.pathname) ? location.pathname : '';
              return (p === '/' || p === '' || p === '/index.html');
            }

            function isSplash() {
              if (!isRootPath()) return false;
              var txt = (document.body && document.body.innerText) ? document.body.innerText : '';
              if (txt.indexOf('당번') < 0) return false;

              // '다음' 버튼이 있으면 스플래시가 아님(온보딩)
              var btns = document.querySelectorAll('button,a,[role="button"]');
              for (var i=0; i<btns.length; i++) {
                var t = (btns[i].innerText || '').trim();
                if (t === '다음') return false;
              }
              return true;
            }

            function applySplashCenter() {
              if (window.__dangbun_splash_center_applied__) return;
              window.__dangbun_splash_center_applied__ = true;

              ensureViewportScale();

              var h = window.innerHeight || 0;
              var H = h > 0 ? (h + 'px') : '100vh';

              document.documentElement.style.background = BG;
              document.body.style.background = BG;

              document.documentElement.style.height = H;
              document.body.style.height = H;
              document.body.style.minHeight = H;

              document.body.style.margin = '0';
              document.body.style.overflow = 'hidden';

              var root = document.querySelector('#root') || document.querySelector('#__next') || document.querySelector('#app');
              var host = root || document.body;

              host.style.backgroundColor = BG;
              host.style.height = H;
              host.style.minHeight = H;

              host.style.display = 'flex';
              host.style.flexDirection = 'column';
              host.style.justifyContent = 'center';
              host.style.alignItems = 'center';

              host.style.width = '100%';
              host.style.maxWidth = '100vw';
              host.style.padding = '0';
              host.style.boxSizing = 'border-box';

              var child = host.firstElementChild;
              if (child) {
                child.style.margin = 'auto';
              }
              console.log('WV_SPLASH_CENTER_APPLIED');
            }

            function resetSplashCenter() {
              // ✅ 온보딩(스플래시가 아닌 화면)에서만 상단 여백 살짝 추가
              try {
                var root2 = document.querySelector('#root') || document.querySelector('#__next') || document.querySelector('#app');
                var main2 = document.querySelector('main');
                var host2 = main2 || root2;

                if (host2) {
                  host2.style.paddingTop = '60px';
                  host2.style.boxSizing = 'border-box';
                }
              } catch(e) {}

              if (!window.__dangbun_splash_center_applied__) return;
              window.__dangbun_splash_center_applied__ = false;

              document.documentElement.style.background = '';
              document.body.style.background = '';

              var root = document.querySelector('#root') || document.querySelector('#__next') || document.querySelector('#app');
              var host = root || document.body;

              host.style.height = '';
              host.style.display = '';
              host.style.flexDirection = '';
              host.style.justifyContent = '';
              host.style.alignItems = '';
              host.style.width = '';
              host.style.maxWidth = '';
              host.style.boxSizing = '';
              host.style.backgroundColor = '';

              document.body.style.backgroundColor = '';
              document.documentElement.style.backgroundColor = '';

              document.body.style.margin = '';
              document.documentElement.style.height = '';
              document.body.style.height = '';
              document.body.style.overflow = '';
            }

            function check() {
              ensureViewportScale();
              if (isSplash()) applySplashCenter();
              else resetSplashCenter();
            }

            check();
            setTimeout(check, 50);
            setTimeout(check, 150);
            setTimeout(check, 300);
            setTimeout(check, 700);

            if (!window.__dangbun_viewport_hooked__) {
              window.__dangbun_viewport_hooked__ = true;

              var _ps = history.pushState;
              history.pushState = function() {
                var r = _ps.apply(this, arguments);
                setTimeout(check, 0);
                return r;
              };

              var _rs = history.replaceState;
              history.replaceState = function() {
                var r = _rs.apply(this, arguments);
                setTimeout(check, 0);
                return r;
              };

              window.addEventListener('popstate', function(){ setTimeout(check, 0); });

              var mo = new MutationObserver(function(){ check(); });
              mo.observe(document.documentElement, { childList:true, subtree:true });
            }

          } catch(e) {}
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

private fun injectMyPlaceTopInsetFix(view: WebView) {
    val js = """
        (function() {
          try {
            function isMyPlace() {
              var href = (location && location.href) ? location.href : '';
              var path = (location && location.pathname) ? location.pathname : '';

              var hint = /myplace|place|내플레이스|내\s*플레이스/i.test(href) || /myplace|place/i.test(path);
              if (!hint) return false;

              var nodes = document.querySelectorAll('h1,h2,h3,header,div,span,p');
              for (var i=0; i<nodes.length; i++) {
                var t = (nodes[i].innerText || '').trim();
                if (t === '내 플레이스') return true;
              }
              return false;
            }

            function findTitleEl() {
              var candidates = document.querySelectorAll('h1,h2,h3,header,div,span,p');
              for (var i=0; i<candidates.length; i++) {
                var t = (candidates[i].innerText || '').trim();
                if (t === '내 플레이스') return candidates[i];
              }
              return null;
            }

            // ✅ "흰 띠" 제거: 상단 첫 컨텐츠(오늘 남은 청소는...)의 위 여백을 강제로 줄임
            function tightenFirstContentGap() {
              try {
                var TIGHTEN = 22; // ✅ 14~32 사이로 조절 (클수록 흰 띠가 더 줄어듦)

                // 1) '오늘 남은 청소는' 문구가 있는 요소 찾기
                var nodes = document.querySelectorAll('div,section,main,span,p,button');
                var target = null;
                for (var i=0; i<nodes.length; i++) {
                  var txt = (nodes[i].innerText || '').trim();
                  if (txt.indexOf('오늘 남은 청소는') >= 0) { target = nodes[i]; break; }
                }
                if (!target) return;

                // 2) 너무 작은 span/p가 잡히면, 적당히 큰 래퍼로 올려잡기
                var wrap = target;
                for (var up=0; up<8 && wrap && wrap.parentElement; up++) {
                  var r = wrap.getBoundingClientRect();
                  if (r.width >= (window.innerWidth * 0.7)) break;
                  wrap = wrap.parentElement;
                }
                if (!wrap) return;

                // 3) 위쪽 여백 "음수"로 당기지 말고, 기존 값에서 TIGHTEN 만큼만 줄이되 0 아래로는 안 내려가게(clamp)
                var curMt = parseFloat((getComputedStyle(wrap).marginTop || '0').replace('px','')) || 0;
                var curPt = parseFloat((getComputedStyle(wrap).paddingTop || '0').replace('px','')) || 0;

                // 내 플레이스와 남은 청소 멘트 박스 사이의 간격
                var MIN_GAP = 40; // 8~16 사이로 취향 조절

                var newMt = Math.max(MIN_GAP, curMt - TIGHTEN);
                var newPt = Math.max(0, curPt - TIGHTEN);

                wrap.style.setProperty('margin-top', newMt + 'px', 'important');
                wrap.style.setProperty('padding-top', newPt + 'px', 'important');

                // 4) 바로 다음 리스트 래퍼도 위 여백이 있으면 같이 줄이기
                var next = wrap.nextElementSibling;
                if (next) {
                  next.style.setProperty('margin-top', '0px', 'important');
                  next.style.setProperty('padding-top', '0px', 'important');
                }
              } catch(e) {}
            }

            function apply() {
              if (!isMyPlace()) return;

              var TOP_EXTRA = 28;

              var titleEl = findTitleEl();
              if (!titleEl) return;

              // ✅ "내 플레이스" 텍스트 아래 여백 줄이기(기존 유지)
              try {
                titleEl.style.setProperty('margin-bottom', '8px', 'important');
                titleEl.style.setProperty('padding-bottom', '0px', 'important');

                var next0 = titleEl.nextElementSibling;
                if (next0) {
                  next0.style.setProperty('margin-top', '8px', 'important');
                  next0.style.setProperty('padding-top', '0px', 'important');
                }
              } catch(e) {}

              // ✅ header 탐색 (titleEl부터 위로 올라가며 fixed/sticky 찾기)
              var cur = titleEl;
              var header = null;
              for (var d=0; d<12 && cur; d++) {
                var st = window.getComputedStyle(cur);
                if (st && (st.position === 'fixed' || st.position === 'sticky')) { header = cur; break; }
                cur = cur.parentElement;
              }

              if (header) {
                // ✅ TOP 위치 갱신 가능하게
                var desiredTop = 'calc(env(safe-area-inset-top) + ' + TOP_EXTRA + 'px)';
                if (header.__dangbun_myplace_last_top__ !== desiredTop) {
                  header.__dangbun_myplace_last_top__ = desiredTop;
                  header.style.setProperty('top', desiredTop, 'important');
                }
                header.style.zIndex = '999999';

                // ✅ 본문 paddingTop은 "헤더 높이만큼" 기본 확보
                var h = header.getBoundingClientRect().height || 0;

                var root = document.querySelector('#root') || document.querySelector('#__next') || document.querySelector('#app');
                var main = document.querySelector('main');
                var host = main || root || document.body;

                host.style.setProperty('padding-top', Math.ceil(h) + 'px', 'important');
                host.style.setProperty('box-sizing', 'border-box', 'important');
              } else {
                // fixed header 못 찾으면 title 자체에 여백 부여
                titleEl.style.setProperty('margin-top', 'calc(env(safe-area-inset-top) + ' + TOP_EXTRA + 'px)', 'important');
              }

              // ✅ 흰 띠(첫 컨텐츠 위 간격) 제거 로직 적용
              tightenFirstContentGap();
            }

            apply();
            setTimeout(apply, 80);
            setTimeout(apply, 200);
            setTimeout(apply, 450);
            setTimeout(apply, 800);

            if (!window.__dangbun_myplace_ob__) {
              window.__dangbun_myplace_ob__ = new MutationObserver(function() { apply(); });
              window.__dangbun_myplace_ob__.observe(document.documentElement, { childList:true, subtree:true });
            }
          } catch(e) {}
        })();
    """.trimIndent()

    view.evaluateJavascript(js, null)
}


private fun injectAddPlaceMemberSelectInsetFix(view: WebView) {
    val js =
        """
        (function() {
          try {
            function hasNextButton() {
              var btns = document.querySelectorAll('button,a,[role="button"]');
              for (var i = 0; i < btns.length; i++) {
                var t = (btns[i].innerText || '').trim();
                if (t === '다음') return btns[i];
              }
              return null;
            }

            function isManagerMemberSelectScreen() {
              var txt = (document.body && document.body.innerText) ? document.body.innerText : '';
              // ✅ '매니저/멤버' 키워드 + '다음' 버튼이 같이 있을 때만 적용
              if (!/(매니저|멤버)/.test(txt)) return false;
              return !!hasNextButton();
            }

            function applyTopInset() {
              var TOP_EXTRA = 44; // ✅ 뒤로가기 아이콘(헤더)이 너무 위에 붙는 것 내려주기

              // fixed/sticky 헤더 후보 중 "상단에 붙어있는" 요소를 하나 잡음
              var header = null;
              var candidates = document.querySelectorAll('header,[role="banner"],div');
              for (var i = 0; i < candidates.length; i++) {
                var st = window.getComputedStyle(candidates[i]);
                if ((st.position === 'fixed' || st.position === 'sticky')) {
                  var topv = parseFloat((st.top || '0').replace('px','')) || 0;
                  if (topv <= 0) { header = candidates[i]; break; }
                }
              }

              if (header && !header.__dangbun_select_top__) {
                header.__dangbun_select_top__ = true;

                header.style.top = 'calc(env(safe-area-inset-top) + ' + TOP_EXTRA + 'px)';
                header.style.zIndex = '999999';

                var h = header.getBoundingClientRect().height || 0;
                var pad = Math.ceil(h + TOP_EXTRA + 8);

                var root =
                  document.querySelector('#root') ||
                  document.querySelector('#__next') ||
                  document.querySelector('#app') ||
                  document.body;

                var curPad = parseFloat((getComputedStyle(root).paddingTop || '0').replace('px','')) || 0;
                if (pad > curPad) {
                  root.style.paddingTop = pad + 'px';
                  root.style.boxSizing = 'border-box';
                }
              }
            }

            function applyBottomInset() {
              var nextBtn = hasNextButton();
              if (!nextBtn) return;

              // ✅ '다음' 버튼의 부모를 타고 올라가서 "가로 폭이 넓은" 컨테이너를 잡음
              var wrap = nextBtn.parentElement;
              for (var k = 0; k < 4 && wrap; k++) {
                var r = wrap.getBoundingClientRect();
                if (r.width >= (window.innerWidth * 0.9)) break;
                wrap = wrap.parentElement;
              }

  if (!wrap) return;

  // ✅ 이미 적용했으면 종료
  if (wrap.__dangbun_select_bottom__) return;
  wrap.__dangbun_select_bottom__ = true;

  // ✅ 1) 포탈 컨테이너(body 직속) 생성: transform 영향 완전 차단
  var portal = document.getElementById('__dangbun_bottom_portal__');
  if (!portal) {
    portal = document.createElement('div');
    portal.id = '__dangbun_bottom_portal__';
    document.body.appendChild(portal);
  }

  // ✅ 2) 원래 위치에 placeholder를 만들어 레이아웃 붕괴 방지
  var placeholder = document.createElement('div');
  placeholder.id = '__dangbun_bottom_placeholder__';

  // wrap의 원래 높이만큼 확보(없으면 기본값)
  var h = wrap.getBoundingClientRect().height || 72;
  placeholder.style.height = Math.ceil(h) + 'px';

  // wrap이 원래 있던 자리에 placeholder 삽입
  if (wrap.parentNode) {
    wrap.parentNode.insertBefore(placeholder, wrap);
  }

  // ✅ 3) wrap을 body 직속 portal로 "이사"
  portal.appendChild(wrap);

  // ✅ 4) portal 자체를 화면 하단에 고정 + 가운데 정렬
  portal.style.position = 'fixed';
  portal.style.left = '0';
  portal.style.right = '0';
// ✅ 현재 스케일(0.8 등) 보정해서 실제로는 더 띄우기
var scale = (window.visualViewport && window.visualViewport.scale) ? window.visualViewport.scale : 1;

// "기기 기준"으로 72px 정도 띄우고 싶으면 CSS px는 72/scale로 줘야 함
var desiredDevicePx = 72;
portal.style.bottom = (desiredDevicePx / scale) + 'px';
  portal.style.zIndex = '999999';
  portal.style.pointerEvents = 'none'; // portal은 클릭 막고, 안의 wrap만 클릭 허용

  portal.style.display = 'flex';
  portal.style.justifyContent = 'center';

  // ✅ 5) wrap은 portal 안에서 “정상 플로우”로 두고 폭만 지정(중앙 정렬은 portal이 담당)
  wrap.style.position = 'relative';
  wrap.style.left = 'auto';
  wrap.style.right = 'auto';
  wrap.style.bottom = 'auto';
  wrap.style.transform = 'none';

  wrap.style.width = 'calc(100vw - 32px)';
  wrap.style.maxWidth = '520px';
  wrap.style.margin = '0';
  wrap.style.padding = '0';
  wrap.style.pointerEvents = 'auto'; // wrap은 클릭 가능

  // ✅ 6) 본문이 버튼에 가려지지 않도록 바닥 여백 확보
  var root2 =
    document.querySelector('#root') ||
    document.querySelector('#__next') ||
    document.querySelector('#app') ||
    document.body;

  var addPad = Math.ceil(h + 32);
  var curPb = parseFloat((getComputedStyle(root2).paddingBottom || '0').replace('px','')) || 0;
  if (addPad > curPb) {
    root2.style.paddingBottom = addPad + 'px';
    root2.style.boxSizing = 'border-box';
  }
}

            }

            function apply() {
              if (!isManagerMemberSelectScreen()) return;

              // ✅ 화면 전용으로 1회 적용 + 변동 대응
              applyTopInset();
              applyBottomInset();
            }

            apply();
            setTimeout(apply, 80);
            setTimeout(apply, 200);
            setTimeout(apply, 450);

            if (!window.__dangbun_select_inset_ob__) {
              window.__dangbun_select_inset_ob__ = new MutationObserver(function() { apply(); });
              window.__dangbun_select_inset_ob__.observe(document.documentElement, { childList: true, subtree: true });
            }
          } catch (e) {}
        })();
        """.trimIndent()

    view.evaluateJavascript(js, null)
}

private fun injectMyPlaceBottomCtaInsetFix(view: WebView) {
    val js = """
        (function() {
          try {
            function isMyPlace() {
              var nodes = document.querySelectorAll('h1,h2,h3,header,div,span,p');
              for (var i=0; i<nodes.length; i++) {
                var t = (nodes[i].innerText || '').trim();
                if (t === '내 플레이스') return true;
              }
              return false;
            }

            function toPx(v) {
              var n = parseFloat((v || '0').toString().replace('px',''));
              return isNaN(n) ? 0 : n;
            }

            function containsText(el, txt) {
              if (!el) return false;
              var t = (el.textContent || '').replace(/\s+/g,' ').trim();
              return t.indexOf(txt) >= 0;
            }

            // ✅ "플레이스 추가" 텍스트가 button이 아니라 내부 span/div에 있을 수 있어서
            //    문서 전체에서 텍스트 포함 요소를 찾고, 그 부모 중 fixed/sticky 하단 바를 우선 탐색
            function findCtaWrapByText() {
              var all = document.querySelectorAll('body *');
              for (var i=0; i<all.length; i++) {
                var el = all[i];
                if (!containsText(el, '플레이스 추가')) continue;

                var cur = el;
                for (var d=0; d<10 && cur; d++) {
                  var st = getComputedStyle(cur);
                  if (st && (st.position === 'fixed' || st.position === 'sticky')) {
                    var r = cur.getBoundingClientRect();
                    // 화면 하단에 붙어 있고 가로폭이 충분한 바 형태만
                    if (r.width >= window.innerWidth * 0.6 && r.bottom >= (window.innerHeight - 16)) {
                      return cur;
                    }
                  }
                  cur = cur.parentElement;
                }
              }
              return null;
            }

            // ✅ 텍스트 탐색이 실패해도, "하단 고정 바" 자체를 형태로 찾아서 처리 (fallback)
            function findBottomFixedBarFallback() {
              var best = null;
              var bestScore = -1;

              var all = document.querySelectorAll('body *');
              for (var i=0; i<all.length; i++) {
                var el = all[i];
                if (!el || !el.getBoundingClientRect) continue;

                var st = getComputedStyle(el);
                if (!(st.position === 'fixed' || st.position === 'sticky')) continue;

                var r = el.getBoundingClientRect();
                var vh = window.innerHeight || 0;
                var vw = window.innerWidth || 0;

                // 하단에 붙은 바 후보
                var nearBottom = (r.bottom >= vh - 16);
                if (!nearBottom) continue;

                // 너무 작은 뱃지/토스트 제외
                if (r.height < 44 || r.height > 180) continue;

                // 폭이 큰 바만
                if (r.width < vw * 0.6) continue;

                // 점수: 화면에서 차지하는 면적
                var score = r.width * r.height;

                if (score > bestScore) {
                  bestScore = score;
                  best = el;
                }
              }
              return best;
            }
            
            function findCtaByTextAnyPosition() {
              var best = null;
              var bestScore = -1;

              var all = document.querySelectorAll('body *');
              for (var i=0; i<all.length; i++) {
                var el = all[i];
                if (!el || !el.getBoundingClientRect) continue;

                var txt = (el.textContent || '').replace(/\s+/g,' ').trim();
                if (txt.indexOf('플레이스 추가') < 0) continue;

                var r = el.getBoundingClientRect();
                var vh = window.innerHeight || 0;
                var vw = window.innerWidth || 0;

                // 버튼/바 형태 후보만 (너무 작은 텍스트 조각 제외)
                if (r.height < 36 || r.height > 220) continue;
                if (r.width < vw * 0.45) continue;

                // 화면 하단에 가까울수록 우선 (겹침 문제는 하단 CTA가 원인)
                var distToBottom = Math.abs(vh - r.bottom);
                if (distToBottom > 200) continue;

                // 점수: 하단에 더 가깝고 + 면적이 클수록
                var score = (r.width * r.height) - (distToBottom * 50);

                if (score > bestScore) {
                  bestScore = score;
                  best = el;
                }
              }

              // 텍스트가 내부 span/div에만 있을 수 있으니, best를 찾았으면
              // 부모로 5단계 정도 올라가며 "더 바(bar) 같은" 큰 박스를 선택
              if (best) {
                var cur = best;
                var chosen = best;
                for (var d=0; d<5 && cur && cur.parentElement; d++) {
                  var p = cur.parentElement;
                  var pr = p.getBoundingClientRect();
                  var vw2 = window.innerWidth || 0;
                  if (pr.width >= vw2 * 0.6 && pr.height >= 44 && pr.height <= 220) {
                    chosen = p;
                  }
                  cur = p;
                }
                return chosen;
              }

              return null;
            }

            // ✅ "진짜 스크롤 컨테이너" 탐색 (overflow-y: auto/scroll)
            function findRealScrollHost() {
              var best = null;
              var bestScore = -1;

              var els = document.querySelectorAll('body *');
              for (var i=0; i<els.length; i++) {
                var el = els[i];
                if (!el || !el.getBoundingClientRect) continue;

                var st = getComputedStyle(el);
                var oy = st.overflowY;

                if (!(oy === 'auto' || oy === 'scroll')) continue;
                if (el.scrollHeight <= el.clientHeight + 1) continue;

                var r = el.getBoundingClientRect();
                var vh = window.innerHeight || 0;
                if (r.height < vh * 0.4) continue;

                var score = r.height * r.width;
                if (score > bestScore) {
                  bestScore = score;
                  best = el;
                }
              }

              return best
                || document.querySelector('main')
                || document.querySelector('#root')
                || document.scrollingElement
                || document.documentElement
                || document.body;
            }

            function applyOnce() {
              if (!isMyPlace()) return { ok:false, reason:'not_myplace' };

              var wrap = findCtaWrapByText() || findBottomFixedBarFallback();
              var host = findRealScrollHost();

              // ✅ 스케일 보정
              var scale = (window.visualViewport && window.visualViewport.scale) ? window.visualViewport.scale : 1;

              // ✅ CTA를 못 찾아도 기본 inset을 적용 (fallback)
              var rect = wrap ? wrap.getBoundingClientRect() : null;
              var h = rect ? (rect.height || 0) : 0;

              // CTA 높이가 0이거나 wrap을 못 찾으면 "버튼 영역"을 기본값으로 가정
              var usedFallback = false;
              if (!wrap || h < 44) {
              // 내 플레이스 리스트와 플레이스 추가 버튼 사이의 간격 조정
                h = 48;            // 대략 버튼 높이(여유 포함)
                usedFallback = true;
              }

              var extraDevicePx = 28; // 기기/네비게이션바 여유
              var padCssPx = Math.ceil((h + extraDevicePx) / scale);

              // 현재 padding-bottom보다 작으면 안 건드리고, 더 커야만 갱신
              var curPb = toPx(getComputedStyle(host).paddingBottom);

              function setPb(el, px) {
                if (!el) return false;
                var v = 'calc(' + px + 'px + env(safe-area-inset-bottom))';
                // ✅ !important로 강제
                el.style.setProperty('padding-bottom', v, 'important');
                el.style.setProperty('box-sizing', 'border-box', 'important');
                return true;
              }

              var applied = [];

              function applyToLikelyScrollContainers(px) {
                var vh = window.innerHeight || 0;
                var vw = window.innerWidth || 0;

                // 1) 기본 후보들
                var base = [
                  host,
                  document.scrollingElement,
                  document.documentElement,
                  document.body,
                  document.getElementById('root'),
                  document.querySelector('main')
                ];

                for (var i = 0; i < base.length; i++) {
                  if (setPb(base[i], px)) applied.push('base#' + i);
                }

                // 2) "실제로 스크롤되는" 컨테이너 전수 조사
                //    (overflowY가 auto/scroll이 아니어도 scrollHeight가 크면 후보로 봄)
                var els = document.querySelectorAll('body *');
                for (var j = 0; j < els.length; j++) {
                  var el = els[j];
                  if (!el || !el.getBoundingClientRect) continue;

                  var r = el.getBoundingClientRect();

                  // 너무 작은 요소 제외
                  if (r.height < vh * 0.45) continue;
                  if (r.width < vw * 0.6) continue;

                  // 실제로 스크롤 가능한지(내용이 더 긴지)
                  if (el.scrollHeight <= el.clientHeight + 1) continue;

                  if (setPb(el, px)) applied.push('scrollCandidate');
                }
              }

              function ensureSpacer(el, px) {
                if (!el) return false;

                var id = '__dangbun_cta_spacer__';
                var spacer = el.querySelector ? el.querySelector('#' + id) : null;

                if (!spacer) {
                  spacer = document.createElement('div');
                  spacer.id = id;
                  spacer.style.width = '100%';
                  spacer.style.pointerEvents = 'none';
                  el.appendChild(spacer);
                }

                spacer.style.height = px + 'px';
                spacer.style.minHeight = px + 'px';
                return true;
              }

              // ✅ 실행
              applyToLikelyScrollContainers(padCssPx);

              // ✅ 스페이서까지 박아주기(마지막 아이템이 CTA 아래로 들어가도 스크롤로 빠져나오게)
              ensureSpacer(host, padCssPx) && applied.push('spacer@host');
              ensureSpacer(document.scrollingElement, padCssPx) && applied.push('spacer@scrollingElement');

              var root2 = document.getElementById('root');
              ensureSpacer(root2, padCssPx) && applied.push('spacer@root');

              // (선택) 높이 계산 깨지는 케이스 방지용
              document.documentElement.style.setProperty('min-height', '100%', 'important');
              document.body.style.setProperty('min-height', '100%', 'important');


            return {
              ok:true,
              usedFallback: usedFallback,
              foundWrap: !!wrap,
              appliedTo: applied,
              scale: scale,
              padCssPx: padCssPx,
              ctaH: h,
              hostTag: host ? host.tagName : null,
              hostId: host ? host.id : null,
              hostClass: host ? (host.className || null) : null
            };
            }

            // ✅ 렌더 타이밍이 늦는 케이스 대비: 더 오래 재시도
            var r0 = applyOnce();
            setTimeout(applyOnce, 80);
            setTimeout(applyOnce, 200);
            setTimeout(applyOnce, 450);
            setTimeout(applyOnce, 800);
            setTimeout(applyOnce, 1300);
            setTimeout(applyOnce, 2000);

            if (!window.__dangbun_myplace_cta_ob_v3__) {
              window.__dangbun_myplace_cta_ob_v3__ = new MutationObserver(function(){ applyOnce(); });
              window.__dangbun_myplace_cta_ob_v3__.observe(document.documentElement, { childList:true, subtree:true });
            }

            if (!window.__dangbun_myplace_cta_resize_v3__) {
              window.__dangbun_myplace_cta_resize_v3__ = true;
              window.addEventListener('resize', function(){ setTimeout(applyOnce, 0); });
              if (window.visualViewport) {
                window.visualViewport.addEventListener('resize', function(){ setTimeout(applyOnce, 0); });
              }
            }

            return JSON.stringify(r0);
          } catch(e) {
            return JSON.stringify({ ok:false, reason:'js_error', msg: String(e && e.message) });
          }
        })();
    """.trimIndent()

    view.evaluateJavascript(js) { result ->
        Log.d(TAG, "WV_MYPLACE_CTA_INSET_V3=$result")
    }
}
