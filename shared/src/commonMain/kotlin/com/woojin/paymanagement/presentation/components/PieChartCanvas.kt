package com.woojin.paymanagement.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woojin.paymanagement.data.ChartItem
import com.woojin.paymanagement.utils.Utils
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin


@Composable
internal fun PieChartCanvas(
    items: List<ChartItem>,
    modifier: Modifier = Modifier,
    chartSize: Dp = 200.dp,
    showLegend: Boolean = true,
    labelTextColor: Color = Color.Black,
    valueLineColor: Color = Color.Gray,
    selectedCategory: String? = null
) {
    if (items.isEmpty()) {
        EmptyChart(modifier, chartSize)
        return
    }

    val textMeasurer = rememberTextMeasurer()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Pie Chart with labels
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(8.dp)
        ) {
            drawPieChartWithLabels(items, textMeasurer, labelTextColor, valueLineColor, selectedCategory)
        }

        if (showLegend) {
            Spacer(modifier = Modifier.height(16.dp))
            ChartLegend(items = items, labelTextColor = labelTextColor, valueLineColor = valueLineColor)
        }
    }
}

@Composable
private fun EmptyChart(modifier: Modifier, chartSize: Dp) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(
            modifier = Modifier.size(chartSize)
        ) {
            drawCircle(
                color = Color.LightGray,
                radius = size.minDimension / 2,
                center = center
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "데이터가 없습니다",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

private fun DrawScope.drawPieChartWithLabels(
    items: List<ChartItem>,
    textMeasurer: TextMeasurer,
    labelTextColor: Color,
    valueLineColor: Color,
    selectedCategory: String? = null
) {
    val total = items.sumOf { it.percentage.toDouble() }.toFloat()
    if (total == 0f) return

    // 파이 차트 반지름: 캔버스 중앙에 크게 그림
    val chartRadius = size.minDimension * 0.38f
    var currentAngle = -90f

    // 슬라이스 내부 % 텍스트 스타일
    val percentStyle = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )

    items.forEach { item ->
        val sweepAngle = (item.percentage / total) * 360f
        val isSelected = item.category == selectedCategory
        val radius = if (isSelected) chartRadius * 1.08f else chartRadius

        // 파이 조각 그리기
        drawArc(
            color = item.color,
            startAngle = currentAngle,
            sweepAngle = sweepAngle,
            useCenter = true,
            size = Size(radius * 2, radius * 2),
            topLeft = Offset(center.x - radius, center.y - radius)
        )

        // 5% 이상인 슬라이스에만 내부에 % 텍스트 표시
        if (item.percentage >= 5f) {
            val middleAngle = (currentAngle + sweepAngle / 2) * (PI.toFloat() / 180f)
            val textRadius = radius * 0.65f
            val textCenterX = center.x + textRadius * cos(middleAngle)
            val textCenterY = center.y + textRadius * sin(middleAngle)

            val percentText = "${(item.percentage * 10).toInt() / 10.0}%"
            val percentLayout = textMeasurer.measure(percentText, percentStyle)

            drawText(
                textLayoutResult = percentLayout,
                topLeft = Offset(
                    textCenterX - percentLayout.size.width / 2f,
                    textCenterY - percentLayout.size.height / 2f
                )
            )
        }

        currentAngle += sweepAngle
    }
}

@Composable
private fun ChartLegend(
    items: List<ChartItem>,
    labelTextColor: Color,
    valueLineColor: Color
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        items.forEach { item ->
            LegendItem(item = item, labelTextColor = labelTextColor, valueLineColor = valueLineColor)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun LegendItem(
    item: ChartItem,
    labelTextColor: Color,
    valueLineColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(item.color)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column {
                Text(
                    text = item.category,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = labelTextColor
                )
                Text(
                    text = "${Utils.formatAmount(item.amount)}원",
                    style = MaterialTheme.typography.bodySmall,
                    color = valueLineColor
                )
            }
        }

        Text(
            text = "${(item.percentage * 10).toInt() / 10.0}%",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = labelTextColor
        )
    }
}