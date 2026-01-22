package com.example.dangbun.ui.webview.fixes.common

import android.webkit.WebView

internal object CommonModalFix {
    internal fun injectCommonFixes(view: WebView) {
        val js =
            """
            (function() {
              try {
                // ✅ 한 번만 적용(중복 주입/로그 폭발 방지)
                if (window.__dangbun_modal_center_once__) return;
                window.__dangbun_modal_center_once__ = true;

                // ✅ 반응형 유틸리티 로드
                if (!window.__dangbun_responsive_utils__) {
                  ${ResponsiveUtils.getResponsiveJs()}
                }

                // ✅ viewport는 화면 크기에 맞게 동적으로 조정
                var meta = document.querySelector('meta[name="viewport"]');
                if (!meta) {
                  meta = document.createElement('meta');
                  meta.name = 'viewport';
                  document.head.appendChild(meta);
                }
                
                // 화면 크기에 따라 동적으로 scale 계산
                var scale = window.getResponsiveScale ? window.getResponsiveScale() : 0.8;
                meta.content = 'width=device-width, initial-scale=' + scale + ', maximum-scale=1.0, user-scalable=no';

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
}
