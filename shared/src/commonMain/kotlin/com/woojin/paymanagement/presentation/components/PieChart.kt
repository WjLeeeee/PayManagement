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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woojin.paymanagement.data.ChartItem
import com.woojin.paymanagement.utils.Utils
import kotlin.math.PI
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

    val textMeasurer = rememberTextMeasurer()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Pie Chart with labels
        Canvas(
            modifier = Modifier
                .size(chartSize * 2.5f)
                .padding(16.dp)
        ) {
            drawPieChartWithLabels(items, textMeasurer)
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

private fun DrawScope.drawPieChartWithLabels(items: List<ChartItem>, textMeasurer: TextMeasurer) {
    val total = items.sumOf { it.percentage.toDouble() }.toFloat()
    if (total == 0f) return

    // 차트 반지름은 전체 크기의 30%로 설정
    val chartRadius = size.minDimension * 0.3f
    val lineLength = chartRadius * 0.5f
    var currentAngle = -90f

    // 텍스트 스타일
    val categoryStyle = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Black
    )
    val detailStyle = TextStyle(
        fontSize = 10.sp,
        color = Color.Gray
    )

    // 라벨 정보 저장
    data class LabelInfo(
        val item: ChartItem,
        val lineStartX: Float,
        val lineStartY: Float,
        var lineEndX: Float,
        var lineEndY: Float,
        val isRightSide: Boolean,
        val categoryLayout: androidx.compose.ui.text.TextLayoutResult,
        val amountLayout: androidx.compose.ui.text.TextLayoutResult,
        val percentageLayout: androidx.compose.ui.text.TextLayoutResult,
        val middleAngle: Float,
        var currentLineLength: Float
    )

    val labels = mutableListOf<LabelInfo>()

    // 파이 조각 그리기 및 라벨 정보 수집
    items.forEach { item ->
        val sweepAngle = (item.percentage / total) * 360f

        drawArc(
            color = item.color,
            startAngle = currentAngle,
            sweepAngle = sweepAngle,
            useCenter = true,
            size = Size(chartRadius * 2, chartRadius * 2),
            topLeft = Offset(
                x = center.x - chartRadius,
                y = center.y - chartRadius
            )
        )

        val middleAngle = (currentAngle + sweepAngle / 2) * (PI.toFloat() / 180f)
        val lineStartX = center.x + chartRadius * cos(middleAngle)
        val lineStartY = center.y + chartRadius * sin(middleAngle)
        val lineEndX = center.x + (chartRadius + lineLength) * cos(middleAngle)
        val lineEndY = center.y + (chartRadius + lineLength) * sin(middleAngle)
        val isRightSide = cos(middleAngle) > 0

        val categoryText = item.category
        val amountText = "${Utils.formatAmount(item.amount)}원"
        val percentageText = "${(item.percentage * 10).toInt() / 10.0}%"

        val categoryLayout = textMeasurer.measure(categoryText, categoryStyle)
        val amountLayout = textMeasurer.measure(amountText, detailStyle)
        val percentageLayout = textMeasurer.measure(percentageText, detailStyle)

        labels.add(
            LabelInfo(
                item = item,
                lineStartX = lineStartX,
                lineStartY = lineStartY,
                lineEndX = lineEndX,
                lineEndY = lineEndY,
                isRightSide = isRightSide,
                categoryLayout = categoryLayout,
                amountLayout = amountLayout,
                percentageLayout = percentageLayout,
                middleAngle = middleAngle,
                currentLineLength = lineLength
            )
        )

        currentAngle += sweepAngle
    }

    // 겹침 방지: 왼쪽과 오른쪽 그룹별로 처리
    val leftLabels = labels.filter { !it.isRightSide }.sortedBy { it.lineEndY }
    val rightLabels = labels.filter { it.isRightSide }.sortedBy { it.lineEndY }

    fun adjustLabelPositions(labelList: List<LabelInfo>) {
        val minGap = 8f

        for (i in 1 until labelList.size) {
            val prev = labelList[i - 1]
            val curr = labelList[i]

            // 전체 텍스트 높이
            val prevHeight = prev.categoryLayout.size.height + prev.amountLayout.size.height +
                           prev.percentageLayout.size.height + 12f
            val currHeight = curr.categoryLayout.size.height + curr.amountLayout.size.height +
                           curr.percentageLayout.size.height + 12f

            // 이전 라벨의 하단과 현재 라벨의 상단 계산
            val prevBottom = prev.lineEndY + (prevHeight / 2)
            val currTop = curr.lineEndY - (currHeight / 2)

            // 겹침 확인
            if (prevBottom + minGap > currTop) {
                // 필요한 Y 이동 거리 계산
                val neededYShift = (prevBottom + minGap + (currHeight / 2)) - curr.lineEndY

                // 현재 선을 연장해서 Y 위치 조정
                // Y 이동을 위해 필요한 선 길이 증가 계산
                val angleFromHorizontal = curr.middleAngle
                val sinAngle = sin(angleFromHorizontal)

                if (kotlin.math.abs(sinAngle) > 0.01f) {
                    val additionalLength = neededYShift / sinAngle

                    // 선 길이 증가 제한 (최대 chartRadius만큼)
                    if (additionalLength > 0 && additionalLength < chartRadius) {
                        curr.currentLineLength += additionalLength

                        // 새로운 선 끝점 계산
                        curr.lineEndX = center.x + (chartRadius + curr.currentLineLength) * cos(curr.middleAngle)
                        curr.lineEndY = center.y + (chartRadius + curr.currentLineLength) * sin(curr.middleAngle)
                    }
                }
            }
        }
    }

    adjustLabelPositions(leftLabels)
    adjustLabelPositions(rightLabels)

    // 1단계: 모든 선 먼저 그리기
    labels.forEach { label ->
        drawLine(
            color = label.item.color,
            start = Offset(label.lineStartX, label.lineStartY),
            end = Offset(label.lineEndX, label.lineEndY),
            strokeWidth = 2f
        )
    }

    // 2단계: 모든 텍스트 그리기 (선 위에 표시됨)
    labels.forEach { label ->
        val textStartX = label.lineEndX + (if (label.isRightSide) 8f else -8f)

        // 전체 텍스트 높이 계산
        val totalTextHeight = label.categoryLayout.size.height +
                            label.amountLayout.size.height +
                            label.percentageLayout.size.height + 10f // 간격 포함

        // 선 끝을 중심으로 텍스트 배치
        val textStartY = label.lineEndY - (totalTextHeight / 2)

        // 카테고리 텍스트
        val categoryX = if (label.isRightSide) {
            textStartX
        } else {
            textStartX - label.categoryLayout.size.width
        }
        drawText(
            textLayoutResult = label.categoryLayout,
            topLeft = Offset(categoryX, textStartY)
        )

        // 금액 텍스트
        val amountX = if (label.isRightSide) {
            textStartX
        } else {
            textStartX - label.amountLayout.size.width
        }
        drawText(
            textLayoutResult = label.amountLayout,
            topLeft = Offset(amountX, textStartY + label.categoryLayout.size.height + 4f)
        )

        // 퍼센트 텍스트
        val percentX = if (label.isRightSide) {
            textStartX
        } else {
            textStartX - label.percentageLayout.size.width
        }
        drawText(
            textLayoutResult = label.percentageLayout,
            topLeft = Offset(percentX, textStartY + label.categoryLayout.size.height + label.amountLayout.size.height + 8f)
        )
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