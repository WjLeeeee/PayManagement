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
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.woojin.paymanagement.data.ChartItem

// 커스텀 포매터: 카테고리명 + 백분율
class PieChartValueFormatter : ValueFormatter() {
    override fun getPieLabel(value: Float, pieEntry: PieEntry?): String {
        val category = pieEntry?.label ?: ""
        val percentage = value.toInt()
        return "$category $percentage%"
    }
}

@Composable
actual fun PieChart(
    items: List<ChartItem>,
    modifier: Modifier,
    chartSize: Dp,
    showLegend: Boolean,
    labelTextColor: androidx.compose.ui.graphics.Color,
    valueLineColor: androidx.compose.ui.graphics.Color,
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

                // 내부 라벨 끄기 (외부에 선으로 연결된 라벨 사용)
                setDrawEntryLabels(false)

                // 최소 각도 보장 (작은 조각도 최소한 보이도록)
                minAngleForSlices = 10f  // 최소 10도 보장

                // 회전 및 하이라이트 설정
                isRotationEnabled = false
                isHighlightPerTapEnabled = true

                // 애니메이션
                animateY(1000, Easing.EaseInOutQuad)

                // 외부 공간 확보 (라벨과 선을 위한 여백 - 더 크게)
                setExtraOffsets(20f, 10f, 20f, 10f)

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

                // 외부 라벨 설정
                valueTextSize = 10f
                valueTextColor = labelTextColor.toArgb()
                valueTypeface = Typeface.DEFAULT

                // 값을 바깥쪽에 표시
                yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
                xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE

                // 선 설정 (조각에서 라벨로 연결되는 선 - 더 길게)
                this.valueLineColor = valueLineColor.toArgb()
                valueLinePart1OffsetPercentage = 80f // 조각 중심에서 시작
                valueLinePart1Length = 0.7f // 첫 번째 선 길이 (증가)
                valueLinePart2Length = 0.4f // 두 번째 선 길이 (수평선, 증가)
                isValueLineVariableLength = true // 선 길이 가변
                valueLineWidth = 1f // 선 두께

                // 값 표시 설정 (카테고리명 + 백분율)
                valueFormatter = PieChartValueFormatter()
            }

            val data = PieData(dataSet).apply {
                setValueTextSize(10f)
                setValueTextColor(labelTextColor.toArgb())
            }

            chart.data = data
            chart.invalidate()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(chartSize * 2)
    )
}