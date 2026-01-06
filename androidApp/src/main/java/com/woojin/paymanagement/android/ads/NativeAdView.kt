package com.woojin.paymanagement.android.ads

import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView as GoogleNativeAdView
import com.woojin.paymanagement.android.R

/**
 * 거래 내역 화면용 네이티브 광고 컴포넌트
 * Google AdMob 정책에 맞게 NativeAdView 사용
 */
@Composable
fun NativeAdItem(
    nativeAd: NativeAd,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            val inflater = LayoutInflater.from(context)
            val adView = inflater.inflate(R.layout.native_ad_layout, null) as GoogleNativeAdView

            // Headline (필수)
            val headlineView = adView.findViewById<TextView>(R.id.ad_headline)
            headlineView.text = nativeAd.headline
            adView.headlineView = headlineView

            // Body
            val bodyView = adView.findViewById<TextView>(R.id.ad_body)
            nativeAd.body?.let {
                bodyView.text = it
                adView.bodyView = bodyView
            }

            // MediaView (이미지/비디오)
            val mediaView = adView.findViewById<com.google.android.gms.ads.nativead.MediaView>(R.id.ad_media)
            nativeAd.mediaContent?.let {
                adView.mediaView = mediaView
            }

            // Icon (MediaView가 없을 때 대체)
            val iconView = adView.findViewById<ImageView>(R.id.ad_icon)
            nativeAd.icon?.let {
                iconView.setImageDrawable(it.drawable)
                iconView.visibility = android.view.View.VISIBLE
                adView.iconView = iconView
            }

            // Advertiser (광고주)
            val advertiserView = adView.findViewById<TextView>(R.id.ad_advertiser)
            nativeAd.advertiser?.let {
                advertiserView.text = it
                adView.advertiserView = advertiserView
            } ?: run {
                advertiserView.visibility = android.view.View.GONE
            }

            // Call to Action 버튼
            val ctaView = adView.findViewById<Button>(R.id.ad_call_to_action)
            nativeAd.callToAction?.let {
                ctaView.text = it
                adView.callToActionView = ctaView
            } ?: run {
                ctaView.visibility = android.view.View.GONE
            }

            // 광고 설정 (필수)
            adView.setNativeAd(nativeAd)

            adView
        }
    )
}
