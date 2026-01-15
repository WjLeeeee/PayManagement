package com.woojin.paymanagement.utils

/**
 * iOS 전면광고 관리자 (빈 구현)
 */
actual class InterstitialAdManager {
    actual fun loadAd() {
        // iOS에서는 아직 구현하지 않음
    }

    actual fun showAd(onAdClosed: () -> Unit) {
        // iOS에서는 아직 구현하지 않음
        // 바로 콜백 실행
        onAdClosed()
    }

    actual fun isAdLoaded(): Boolean {
        // iOS에서는 항상 false 반환
        return false
    }
}
