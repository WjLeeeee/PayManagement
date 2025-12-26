package com.woojin.paymanagement.utils

/**
 * 전면광고(Interstitial Ad) 관리 인터페이스
 */
expect class InterstitialAdManager {
    /**
     * 전면광고 로드
     */
    fun loadAd()

    /**
     * 전면광고 표시
     * @param onAdClosed 광고가 닫힌 후 실행될 콜백
     */
    fun showAd(onAdClosed: () -> Unit)

    /**
     * 광고가 로드되었는지 확인
     */
    fun isAdLoaded(): Boolean
}
