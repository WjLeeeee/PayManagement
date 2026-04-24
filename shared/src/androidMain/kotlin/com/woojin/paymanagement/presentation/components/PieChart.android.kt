package com.woojin.paymanagement.presentation.components

import android.graphics.Typeface
import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.PieChart as MPPieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import androidx.compose.runtime.rememberUpdatedState
import com.woojin.paymanagement.data.ChartItem

@Composable
actual fun PieChart(
    items: List<ChartItem>,
    modifier: Modifier,
    chartSize: Dp,
    showLegend: Boolean,
    labelTextColor: androidx.compose.ui.graphics.Color,
    valueLineColor: androidx.compose.ui.graphics.Color,
    selectedCategory: String?,
    onItemSelected: (String?) -> Unit
) {
    val entries = remember(items) {
        items.map { item -> PieEntry(item.percentage, item.category) }
    }

    val colors = remember(items) {
        items.map { it.color.toArgb() }
    }

    // 항상 최신 콜백을 참조하도록 유지
    val currentOnItemSelected = rememberUpdatedState(onItemSelected)

    // 리스너를 stable하게 유지 (factory/update 양쪽에서 동일 인스턴스 사용)
    val chartListener = remember {
        object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                currentOnItemSelected.value((e as? PieEntry)?.label)
            }
            override fun onNothingSelected() {
                currentOnItemSelected.value(null)
            }
        }
    }

    AndroidView(
        factory = { context ->
            MPPieChart(context).apply {
                description.isEnabled = false
                setUsePercentValues(true)

                setDrawHoleEnabled(true)
                setHoleRadius(48f)
                setTransparentCircleRadius(51f)
                setTransparentCircleAlpha(60)
                setHoleColor(android.graphics.Color.TRANSPARENT)

                setDrawCenterText(true)
                setDrawEntryLabels(false)

                minAngleForSlices = 10f
                isRotationEnabled = false
                isHighlightPerTapEnabled = true

                animateY(800, Easing.EaseInOutQuad)
                setExtraOffsets(8f, 8f, 8f, 8f)

                setOnChartValueSelectedListener(chartListener)

                legend.isEnabled = showLegend
                if (showLegend) {
                    legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                    legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                    legend.orientation = Legend.LegendOrientation.HORIZONTAL
                    legend.setDrawInside(false)
                    legend.textSize = 12f
                }
            }
        },
        update = { chart ->
            val dataSet = PieDataSet(entries, "").apply {
                this.colors = colors
                sliceSpace = 2f
                selectionShift = 6f
                setDrawValues(false)
            }

            // data 업데이트 중 onNothingSelected가 selectedCategory를 초기화하지 않도록
            // 리스너를 잠시 해제했다가 복구
            chart.setOnChartValueSelectedListener(null)
            chart.data = PieData(dataSet)
            chart.setOnChartValueSelectedListener(chartListener)

            // 중앙 텍스트 업데이트
            if (selectedCategory != null) {
                val selectedEntry = entries.firstOrNull { it.label == selectedCategory }
                val pct = selectedEntry?.y?.toInt() ?: 0
                val displayName = if (selectedCategory.length > 8) "${selectedCategory.take(7)}…" else selectedCategory
                val spannable = SpannableString("$displayName\n$pct%").apply {
                    setSpan(RelativeSizeSpan(0.85f), 0, displayName.length, 0)
                    setSpan(RelativeSizeSpan(1.1f), displayName.length + 1, length, 0)
                    setSpan(StyleSpan(Typeface.BOLD), displayName.length + 1, length, 0)
                }
                chart.setCenterText(spannable)
                chart.setCenterTextColor(labelTextColor.toArgb())
                chart.setCenterTextSize(13f)
            } else {
                chart.centerText = ""
            }

            // 선택 항목 하이라이트
            if (selectedCategory != null) {
                val selectedIndex = entries.indexOfFirst { it.label == selectedCategory }
                if (selectedIndex >= 0) {
                    chart.highlightValue(selectedIndex.toFloat(), 0)
                } else {
                    chart.highlightValue(null)
                }
            } else {
                chart.highlightValue(null)
            }

            chart.invalidate()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(chartSize * 2)
    )
}
