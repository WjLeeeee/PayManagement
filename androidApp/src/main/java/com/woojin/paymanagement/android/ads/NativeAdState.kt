package com.woojin.paymanagement.android.ads

import com.google.android.gms.ads.nativead.NativeAd

/**
 * 네이티브 광고의 로딩 상태
 */
sealed class NativeAdState {
    object Loading : NativeAdState()
    data class Success(val ad: NativeAd) : NativeAdState()
    object Failed : NativeAdState()
}
