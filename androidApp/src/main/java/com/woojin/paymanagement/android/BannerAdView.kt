package com.woojin.paymanagement.android

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * AdMob 배너 광고 Composable
 *
 * @param adUnitId 배너 광고 단위 ID (실제 ID 또는 테스트 ID)
 * @param onNavigateToAdRemoval 광고 제거 화면으로 이동하는 콜백
 * @param modifier Composable modifier
 */
@Composable
fun BannerAdView(
    adUnitId: String,
    onNavigateToAdRemoval: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showHouseBanner by remember { mutableStateOf(false) }
    var isAdLoaded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // 실제 광고
        if (!showHouseBanner) {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { context ->
                    AdView(context).apply {
                        // 배너 광고 크기 설정
                        setAdSize(AdSize.BANNER)
                        // 광고 단위 ID 설정
                        this.adUnitId = adUnitId

                        // 광고 리스너 설정
                        adListener = object : AdListener() {
                            override fun onAdLoaded() {
                                // 광고 로딩 성공
                                isAdLoaded = true
                                showHouseBanner = false
                            }

                            override fun onAdFailedToLoad(error: LoadAdError) {
                                // 광고 로딩 실패 (nofill 포함)
                                isAdLoaded = false
                                showHouseBanner = true
                            }
                        }

                        // 광고 요청
                        loadAd(AdRequest.Builder().build())
                    }
                }
            )
        }

        // 하우스 배너 (광고 실패 시 표시)
        if (showHouseBanner) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable { onNavigateToAdRemoval() }
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🚫",
                    fontSize = 20.sp
                )
                Text(
                    text = " 광고 없이 사용하기",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
