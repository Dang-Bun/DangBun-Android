package com.example.dangbun.ui.webview.fixes.addplace

import android.webkit.WebView

internal object PlaceMake3ShareFix {

    internal fun inject(view: WebView) {
        view.evaluateJavascript(provideJs(), null)
    }

    private fun provideJs(): String {
        return """
        (function() {
          try {
            var ROUTE_KEY = '/placemake3';
            var INSTALLED_KEY = '__db_placemake3_share_fix_installed__';

            function isPlaceMake3() {
              try {
                return (location.pathname || '').indexOf(ROUTE_KEY) >= 0;
              } catch(e) { return false; }
            }

            function extractCodeFromModal() {
              // 모달 안에서 참여코드로 보이는 텍스트를 찾아냄 (v3to7u 같은 형태)
              // 너무 과하면 다른 텍스트 잡을 수 있어서 "영문/숫자 4~10자"만
              try {
                var bodyText = (document.body && document.body.innerText) ? document.body.innerText : '';
                var m = bodyText.match(/\b[a-zA-Z0-9]{4,10}\b/);
                if (!m) return '';
                // 흔히 걸리는 단어들(도메인 조각 등) 방지용: 필요하면 더 추가 가능
                var bad = ['https', 'http', 'vercel', 'dangbun', 'frontend', 'virid', 'modal'];
                var cand = m[0] || '';
                for (var i=0; i<bad.length; i++) {
                  if (cand.toLowerCase().indexOf(bad[i]) >= 0) return '';
                }
                return cand;
              } catch(e) { return ''; }
            }

            function shareText(text) {
              try {
                if (window.DangbunBridge && window.DangbunBridge.shareText) {
                  window.DangbunBridge.shareText(String(text || ''));
                  return true;
                }
              } catch(e) {}
              return false;
            }

            function installClickInterceptor() {
              if (window[INSTALLED_KEY]) return;
              window[INSTALLED_KEY] = true;

              // ✅ 캡처 단계에서 "공유하기" 클릭을 가로채서
              // 사이트의 기본 로직(alert 뜨는 것 포함)이 실행되기 전에 차단
              document.addEventListener('click', function(e) {
                try {
                  if (!isPlaceMake3()) return;

                  var t = e.target;
                  if (!t) return;

                  // 버튼/버튼 내부 요소 클릭 모두 잡기
                  var btn = t.closest ? t.closest('button, [role="button"], a') : null;
                  if (!btn) return;

                  var label = (btn.innerText || '').replace(/\s/g, '');
                  if (label !== '공유하기') return;

                  // ✅ 여기서부터는 "공유하기" 눌렀을 때만 실행
                  try { e.preventDefault(); } catch(err) {}
                  try { e.stopPropagation(); } catch(err) {}
                  try { if (e.stopImmediatePropagation) e.stopImmediatePropagation(); } catch(err) {}

                  var code = extractCodeFromModal();
                  var payload = code ? ('참여코드: ' + code) : '참여코드 공유';

                  // 네이티브 공유 호출
                  var ok = shareText(payload);

                  // 만약 브릿지가 없어서 실패하면, 여기서는 아무 것도 더 하지 않음(=기본 alert도 막았음)
                  // 필요하면 fallback로 clipboard copy 등을 추가할 수 있음
                  return;
                } catch(err2) {}
              }, true); // ✅ capture = true
            }

            // SPA에서도 모달이 나중에 뜨니까, 주기적으로 “설치만” 시도
            function tick() {
              if (!isPlaceMake3()) return;
              installClickInterceptor();
            }

            tick();
            setInterval(tick, 500);

          } catch(e) {}
        })();
        """.trimIndent()
    }
}
