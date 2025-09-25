package com.woojin.paymanagement.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woojin.paymanagement.data.ChartItem
import com.woojin.paymanagement.utils.Utils
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PieChart(
    items: List<ChartItem>,
    modifier: Modifier = Modifier,
    chartSize: Dp = 200.dp,
    showLegend: Boolean = true
) {
    if (items.isEmpty()) {
        EmptyChart(modifier, chartSize)
        return
    }
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Pie Chart
        Canvas(
            modifier = Modifier.size(chartSize)
        ) {
            drawPieChart(items)
        }
        
        if (showLegend) {
            Spacer(modifier = Modifier.height(16.dp))
            ChartLegend(items = items)
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

private fun DrawScope.drawPieChart(items: List<ChartItem>) {
    val total = items.sumOf { it.percentage.toDouble() }.toFloat()
    if (total == 0f) return
    
    val radius = size.minDimension / 2
    var currentAngle = -90f // 12시 방향부터 시작
    
    items.forEach { item ->
        val sweepAngle = (item.percentage / total) * 360f
        
        drawArc(
            color = item.color,
            startAngle = currentAngle,
            sweepAngle = sweepAngle,
            useCenter = true,
            size = Size(radius * 2, radius * 2),
            topLeft = Offset(
                x = center.x - radius,
                y = center.y - radius
            )
        )
        
        currentAngle += sweepAngle
    }
}

@Composable
private fun ChartLegend(items: List<ChartItem>) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        items.forEach { item ->
            LegendItem(item = item)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun LegendItem(item: ChartItem) {
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
                    color = Color.Black
                )
                Text(
                    text = "${Utils.formatAmount(item.amount)}원",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
        
        Text(
            text = "${(item.percentage * 10).toInt() / 10.0}%",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
    }
}