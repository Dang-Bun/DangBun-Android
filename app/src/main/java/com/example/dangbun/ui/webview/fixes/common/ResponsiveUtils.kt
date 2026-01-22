package com.example.dangbun.ui.webview.fixes.common

/**
 * 반응형 유틸리티 함수들
 * 화면 크기에 따라 동적으로 px 값을 계산하여 다양한 디바이스에서 일관된 UI를 제공
 */
internal object ResponsiveUtils {
    
    /**
     * JavaScript 코드로 반응형 계산 함수를 제공
     * 기준 화면 크기(1080x2340)를 기준으로 비율을 계산하여 px 값을 조정
     */
    fun getResponsiveJs(): String {
        return """
        (function() {
          // ✅ 반응형 계산 유틸리티
          if (window.__dangbun_responsive_utils__) return;
          window.__dangbun_responsive_utils__ = true;

          // 기준 화면 크기 (일반적인 안드로이드 폰 기준)
          var BASE_WIDTH = 1080;
          var BASE_HEIGHT = 2340;
          var BASE_SCALE = 0.8; // 현재 viewport scale

          /**
           * 화면 크기에 비례하여 px 값을 계산
           * @param {number} basePx - 기준 px 값 (BASE_WIDTH 기준)
           * @param {string} type - 'width' 또는 'height' (기본값: 'height')
           * @returns {number} 조정된 px 값
           */
          window.getResponsivePx = function(basePx, type) {
            type = type || 'height';
            try {
              var screenSize = type === 'width' ? window.innerWidth : window.innerHeight;
              var baseSize = type === 'width' ? BASE_WIDTH : BASE_HEIGHT;
              var ratio = screenSize / baseSize;
              
              // 최소/최대 비율 제한 (너무 작거나 크게 변하지 않도록)
              ratio = Math.max(0.7, Math.min(1.3, ratio));
              
              return Math.round(basePx * ratio);
            } catch(e) {
              return basePx; // 에러 시 원본 값 반환
            }
          };

          /**
           * 화면 높이에 비례하여 vh 단위로 변환
           * @param {number} basePx - 기준 px 값
           * @returns {string} vh 단위 문자열 (예: "5vh")
           */
          window.getResponsiveVh = function(basePx) {
            try {
              var screenHeight = window.innerHeight || 800;
              var baseHeight = BASE_HEIGHT;
              var ratio = screenHeight / baseHeight;
              ratio = Math.max(0.7, Math.min(1.3, ratio));
              
              var vhValue = (basePx / baseHeight) * 100 * ratio;
              return Math.max(1, Math.round(vhValue * 10) / 10) + 'vh';
            } catch(e) {
              return basePx + 'px';
            }
          };

          /**
           * 화면 너비에 비례하여 vw 단위로 변환
           * @param {number} basePx - 기준 px 값
           * @returns {string} vw 단위 문자열 (예: "5vw")
           */
          window.getResponsiveVw = function(basePx) {
            try {
              var screenWidth = window.innerWidth || 1080;
              var baseWidth = BASE_WIDTH;
              var ratio = screenWidth / baseWidth;
              ratio = Math.max(0.7, Math.min(1.3, ratio));
              
              var vwValue = (basePx / baseWidth) * 100 * ratio;
              return Math.max(1, Math.round(vwValue * 10) / 10) + 'vw';
            } catch(e) {
              return basePx + 'px';
            }
          };

          /**
           * 화면 비율에 따라 동적으로 viewport scale 계산
           * 작은 화면에서는 더 작은 scale, 큰 화면에서는 더 큰 scale
           * @returns {number} 적절한 initial-scale 값
           */
          window.getResponsiveScale = function() {
            try {
              var screenWidth = window.innerWidth || 1080;
              var screenHeight = window.innerHeight || 2340;
              var aspectRatio = screenWidth / screenHeight;
              
              // 기준 화면 비율 (약 0.46)
              var baseAspectRatio = BASE_WIDTH / BASE_HEIGHT;
              
              // 화면이 더 넓으면(가로가 길면) scale을 조금 더 크게
              // 화면이 더 좁으면(세로가 길면) scale을 조금 더 작게
              var scaleAdjustment = (aspectRatio / baseAspectRatio) * 0.1;
              var baseScale = BASE_SCALE;
              
              var finalScale = baseScale + scaleAdjustment;
              
              // 최소/최대 제한
              return Math.max(0.7, Math.min(1.0, finalScale));
            } catch(e) {
              return BASE_SCALE;
            }
          };

          console.log('[RESPONSIVE_UTILS] initialized');
        })();
        """.trimIndent()
    }
}
