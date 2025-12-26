package com.woojin.paymanagement.utils

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * Android 전면광고 관리자
 */
actual class InterstitialAdManager(
    private val context: Context,
    private val activityProvider: () -> Activity?,
    private val adUnitId: String
) {
    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false

    /**
     * 전면광고 로드
     */
    actual fun loadAd() {
        // 이미 로드 중이거나 로드된 광고가 있으면 스킵
        if (isLoading || interstitialAd != null) {
            return
        }

        isLoading = true
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    println("✅ Interstitial Ad 로드 성공")
                    interstitialAd = ad
                    isLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    println("❌ Interstitial Ad 로드 실패: ${error.message}")
                    interstitialAd = null
                    isLoading = false
                }
            }
        )
    }

    /**
     * 전면광고 표시
     * @param onAdClosed 광고가 닫힌 후 실행될 콜백
     */
    actual fun showAd(onAdClosed: () -> Unit) {
        val activity = activityProvider()
        val ad = interstitialAd

        if (activity == null) {
            println("❌ Activity가 없어서 광고를 표시할 수 없습니다")
            onAdClosed()
            return
        }

        if (ad == null) {
            println("⚠️ 로드된 광고가 없어서 바로 종료합니다")
            onAdClosed()
            return
        }

        // 광고 콜백 설정
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                println("✅ Interstitial Ad 닫힘")
                interstitialAd = null
                onAdClosed()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                println("❌ Interstitial Ad 표시 실패: ${error.message}")
                interstitialAd = null
                onAdClosed()
            }

            override fun onAdShowedFullScreenContent() {
                println("✅ Interstitial Ad 표시됨")
            }
        }

        // 광고 표시
        ad.show(activity)
    }

    /**
     * 광고가 로드되었는지 확인
     */
    actual fun isAdLoaded(): Boolean {
        return interstitialAd != null
    }
}
