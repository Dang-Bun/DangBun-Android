package com.example.dangbun.ui.webview.fixes.myplace

internal object MyPlaceHeaderFix {

    internal fun provideJs(): String {
        return """
            (function() {
              try {
                var GRAY_BG = '#F5F6F8';

                // ✅ 여기 값만 바꾸면서 튜닝하세요
                var HEADER_TOP_EXTRA = 0;
                var HEADER_SPACER_H = 0;

                window.__dbApplyMyPlaceHeaderFix = function() {
                  try {
                    // A. 헤더 영역 정리
                    var tags = document.querySelectorAll('h1,h2,h3,header,div,span');
                    for (var i=0; i<tags.length; i++) {
                      var t = tags[i];
                      if (!t) continue;

                      var txt = (t.innerText || '').trim();
                      if (txt === '내 플레이스' && !t.__hooked) {
                        t.__hooked = true;

                        var header = t.parentElement;
                        while(header && header.offsetWidth < window.innerWidth * 0.8) {
                          header = header.parentElement;
                        }

                        if (header) {
                          try {
                            header.style.setProperty('background-color', GRAY_BG, 'important');
                            header.style.setProperty('background', GRAY_BG, 'important');

                            header.style.setProperty(
                              'padding-top',
                              'calc(env(safe-area-inset-top) + ' + HEADER_TOP_EXTRA + 'px)',
                              'important'
                            );

                            header.style.setProperty('padding-bottom', '12px', 'important');
                            header.style.setProperty('box-sizing', 'border-box', 'important');

                            header.style.setProperty('position', 'relative', 'important');

                            var spacerId = '__db_header_top_spacer__';
                            var spacer = header.querySelector('#' + spacerId);
                            if (!spacer) {
                              spacer = document.createElement('div');
                              spacer.id = spacerId;
                              spacer.style.position = 'absolute';
                              spacer.style.left = '0';
                              spacer.style.right = '0';
                              spacer.style.top = '0';
                              spacer.style.height = HEADER_SPACER_H + 'px';
                              spacer.style.background = GRAY_BG;
                              spacer.style.pointerEvents = 'none';
                              header.appendChild(spacer);
                            } else {
                              spacer.style.height = HEADER_SPACER_H + 'px';
                              spacer.style.background = GRAY_BG;
                            }
                          } catch(e) {}
                        }
                        break;
                      }
                    }
                  } catch(e) {}
                };
              } catch(e) {}
            })();
        """.trimIndent()
    }
}
