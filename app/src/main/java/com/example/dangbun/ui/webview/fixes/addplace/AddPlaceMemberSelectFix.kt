package com.example.dangbun.ui.webview.fixes.addplace

import android.webkit.WebView

// “매니저/멤버 선택 화면”처럼 ‘다음’ 버튼이 있는 화면
internal object AddPlaceMemberSelectFix {
    internal fun injectAddPlaceMemberSelectInsetFix(view: WebView) {
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

            // ✅ 추가: "회색 배경을 가진 실제 래퍼"를 찾아 inline important로 흰색 강제
            function forceWhiteBackgroundByInline() {
              try {
                // 기준점: '다음' 버튼이 있으면 그쪽, 없으면 body
                var base = hasNextButton() || document.body;
                if (!base) return;

                // base에서 위로 올라가면서 "실제로 배경색을 가진" 래퍼를 찾음
                var target = null;
                var el = base;
                for (var i = 0; i < 12 && el; i++) {
                  var bg = '';
                  try { bg = window.getComputedStyle(el).backgroundColor; } catch(e) {}
                  if (bg && bg !== 'rgba(0, 0, 0, 0)' && bg !== 'transparent') {
                    target = el;
                    break;
                  }
                  el = el.parentElement;
                }
                if (!target) target = document.body;

                // ✅ inline + important 로 흰색 강제 (CSS보다 우선)
                var nodes = [document.documentElement, document.body, target];
                for (var k = 0; k < nodes.length; k++) {
                  var n = nodes[k];
                  if (!n) continue;
                  n.style.setProperty('background', '#FFFFFF', 'important');
                  n.style.setProperty('background-color', '#FFFFFF', 'important');
                }
              } catch(e) {}
            }

            function apply() {
              if (!isManagerMemberSelectScreen()) return;

              // ✅ 회색 끼임 제거(실제 배경 래퍼를 inline으로 흰색 강제)
              forceWhiteBackgroundByInline();

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
}
