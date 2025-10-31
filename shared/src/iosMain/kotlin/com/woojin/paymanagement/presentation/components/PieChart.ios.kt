package com.woojin.paymanagement.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.woojin.paymanagement.data.ChartItem

/**
 * iOS용 PieChart 구현
 * 기존 Canvas 기반 커스텀 구현 사용
 */
@Composable
actual fun PieChart(
    items: List<ChartItem>,
    modifier: Modifier,
    chartSize: Dp,
    showLegend: Boolean,
    labelTextColor: Color,
    valueLineColor: Color,
    onItemSelected: (String?) -> Unit
) {
    // iOS에서는 기존 Canvas 기반 구현 사용
    // TODO: iOS에서도 탭 이벤트 구현 필요
    PieChartCanvas(
        items = items,
        modifier = modifier,
        chartSize = chartSize,
        showLegend = showLegend
    )
}