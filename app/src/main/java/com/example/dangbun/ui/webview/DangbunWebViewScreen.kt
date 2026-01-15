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

