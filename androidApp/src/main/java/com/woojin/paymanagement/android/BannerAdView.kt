package com.woojin.paymanagement.android

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * AdMob 배너 광고 Composable
 *
 * @param adUnitId 배너 광고 단위 ID (실제 ID 또는 테스트 ID)
 * @param modifier Composable modifier
 */
@Composable
fun BannerAdView(
    adUnitId: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(MaterialTheme.colorScheme.surface),
        factory = { context ->
            AdView(context).apply {
                // 배너 광고 크기 설정
                setAdSize(AdSize.BANNER)
                // 광고 단위 ID 설정
                this.adUnitId = adUnitId
                // 광고 요청
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
