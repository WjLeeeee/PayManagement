package com.woojin.paymanagement.presentation.components

import android.graphics.Typeface
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
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.woojin.paymanagement.data.ChartItem

@Composable
actual fun PieChart(
    items: List<ChartItem>,
    modifier: Modifier,
    chartSize: Dp,
    showLegend: Boolean,
    onItemSelected: (String?) -> Unit
) {
    val entries = remember(items) {
        items.map { item ->
            PieEntry(item.percentage, item.category)
        }
    }

    val colors = remember(items) {
        items.map { it.color.toArgb() }
    }

    AndroidView(
        factory = { context ->
            MPPieChart(context).apply {
                // 차트 기본 설정
                description.isEnabled = false
                setUsePercentValues(true)
                setDrawHoleEnabled(false)
                setDrawEntryLabels(true)
                setEntryLabelColor(android.graphics.Color.BLACK)
                setEntryLabelTextSize(7f)
                setEntryLabelTypeface(Typeface.DEFAULT)

                // 최소 각도 보장 (작은 조각도 최소한 보이도록)
                minAngleForSlices = 10f  // 최소 10도 보장

                // 회전 및 하이라이트 설정
                isRotationEnabled = false
                isHighlightPerTapEnabled = true

                // 애니메이션
                animateY(1000, Easing.EaseInOutQuad)

                // 선택 이벤트 리스너
                setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                    override fun onValueSelected(e: Entry?, h: Highlight?) {
                        val pieEntry = e as? PieEntry
                        onItemSelected(pieEntry?.label)
                    }

                    override fun onNothingSelected() {
                        onItemSelected(null)
                    }
                })

                // 범례 설정
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
                selectionShift = 5f
                valueTextSize = 11f
                valueTextColor = android.graphics.Color.BLACK
                valueTypeface = Typeface.DEFAULT

                // 값 표시 설정 (백분율)
                valueFormatter = PercentFormatter(chart)
            }

            val data = PieData(dataSet).apply {
                setValueTextSize(7f)
                setValueTextColor(android.graphics.Color.BLACK)
            }

            chart.data = data
            chart.invalidate()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(chartSize * 2)
    )
}