package com.woojin.paymanagement.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.woojin.paymanagement.data.ChartItem

/**
 * 플랫폼별로 다른 차트 라이브러리를 사용하기 위한 expect/actual 패턴
 * - Android: MPAndroidChart
 * - iOS: 커스텀 Canvas 구현 또는 iOS 차트 라이브러리
 */

@Composable
expect fun PieChart(
    items: List<ChartItem>,
    modifier: Modifier = Modifier,
    chartSize: Dp = 200.dp,
    showLegend: Boolean = true,
    onItemSelected: (String?) -> Unit = {}
)