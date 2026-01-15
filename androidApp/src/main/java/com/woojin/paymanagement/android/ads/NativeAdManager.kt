package com.woojin.paymanagement.android.ads

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions

/**
 * 네이티브 광고 관리자
 * 거래 내역 화면의 네이티브 광고 로드 및 관리
 */
class NativeAdManager(private val context: Context) {

    companion object {
        private const val TAG = "NativeAdManager"
        private const val AD_UNIT_ID = "ca-app-pub-9195598687879551/3762445094"
    }

    private var currentNativeAd: NativeAd? = null
    private var adLoader: AdLoader? = null

    /**
     * 네이티브 광고 로드
     * @param onAdLoaded 광고 로드 성공 시 콜백
     * @param onAdFailed 광고 로드 실패 시 콜백
     */
    fun loadAd(
        onAdLoaded: (NativeAd) -> Unit,
        onAdFailed: ((String) -> Unit)? = null
    ) {
        // 기존 광고 정리
        currentNativeAd?.destroy()
        currentNativeAd = null

        val adLoader = AdLoader.Builder(context, AD_UNIT_ID)
            .forNativeAd { ad ->
                Log.d(TAG, "네이티브 광고 로드 성공")

                // 기존 광고가 있으면 destroy
                if (currentNativeAd != null && currentNativeAd != ad) {
                    currentNativeAd?.destroy()
                }

                currentNativeAd = ad
                onAdLoaded(ad)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "네이티브 광고 로드 실패: ${adError.message}")
                    onAdFailed?.invoke(adError.message)
                }

                override fun onAdClicked() {
                    Log.d(TAG, "네이티브 광고 클릭됨")
                }

                override fun onAdImpression() {
                    Log.d(TAG, "네이티브 광고 노출됨")
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                    .build()
            )
            .build()

        this.adLoader = adLoader
        adLoader.loadAd(AdRequest.Builder().build())
    }

    /**
     * 광고 정리 (메모리 누수 방지)
     */
    fun destroy() {
        currentNativeAd?.destroy()
        currentNativeAd = null
        adLoader = null
    }
}
