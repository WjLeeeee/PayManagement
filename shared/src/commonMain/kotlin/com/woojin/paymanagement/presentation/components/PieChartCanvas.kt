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

// 라벨 방향 정의
private enum class LabelDirection {
    RIGHT, LEFT, TOP, BOTTOM
}

@Composable
internal fun PieChartCanvas(
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
        val direction: LabelDirection,
        val categoryLayout: androidx.compose.ui.text.TextLayoutResult,
        val amountLayout: androidx.compose.ui.text.TextLayoutResult,
        val percentageLayout: androidx.compose.ui.text.TextLayoutResult,
        val middleAngle: Float,
        var currentLineLength: Float
    )

    val labels = mutableListOf<LabelInfo>()

    // "기타" 카테고리가 있으면 기타를, 없으면 가장 낮은 % 항목을 오른쪽에 배치
    val etcItem = items.find { it.category == "기타" }
    val itemToForceRight = etcItem ?: items.minByOrNull { it.percentage }

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

        // 각도를 0~360도 범위로 정규화
        val normalizedAngleDeg = ((currentAngle + sweepAngle / 2) % 360 + 360) % 360

        // 4방향으로 구분 (기타 항목은 무조건 오른쪽)
        val direction = if (item == itemToForceRight) {
            LabelDirection.RIGHT
        } else {
            when {
                normalizedAngleDeg >= 315 || normalizedAngleDeg < 45 -> LabelDirection.TOP
                normalizedAngleDeg >= 45 && normalizedAngleDeg < 135 -> LabelDirection.RIGHT
                normalizedAngleDeg >= 135 && normalizedAngleDeg < 225 -> LabelDirection.BOTTOM
                else -> LabelDirection.LEFT
            }
        }

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
                direction = direction,
                categoryLayout = categoryLayout,
                amountLayout = amountLayout,
                percentageLayout = percentageLayout,
                middleAngle = middleAngle,
                currentLineLength = lineLength
            )
        )

        currentAngle += sweepAngle
    }

    // 겹침 방지: 4방향 그룹별로 처리
    val topLabels = labels.filter { it.direction == LabelDirection.TOP }.sortedBy { it.lineEndX }
    val bottomLabels = labels.filter { it.direction == LabelDirection.BOTTOM }.sortedBy { it.lineEndX }
    val leftLabels = labels.filter { it.direction == LabelDirection.LEFT }.sortedBy { it.lineEndY }
    val rightLabels = labels.filter { it.direction == LabelDirection.RIGHT }.sortedBy { it.lineEndY }

    fun adjustVerticalLabels(labelList: List<LabelInfo>) {
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

    fun adjustHorizontalLabels(labelList: List<LabelInfo>) {
        val minGap = 8f

        for (i in 1 until labelList.size) {
            val prev = labelList[i - 1]
            val curr = labelList[i]

            // 전체 텍스트 너비 (가장 긴 텍스트 기준)
            val prevWidth = maxOf(
                prev.categoryLayout.size.width,
                prev.amountLayout.size.width,
                prev.percentageLayout.size.width
            )
            val currWidth = maxOf(
                curr.categoryLayout.size.width,
                curr.amountLayout.size.width,
                curr.percentageLayout.size.width
            )

            // 이전 라벨의 오른쪽과 현재 라벨의 왼쪽 계산
            val prevRight = prev.lineEndX + (prevWidth / 2)
            val currLeft = curr.lineEndX - (currWidth / 2)

            // 겹침 확인
            if (prevRight + minGap > currLeft) {
                // 필요한 X 이동 거리 계산
                val neededXShift = (prevRight + minGap + (currWidth / 2)) - curr.lineEndX

                // 현재 선을 연장해서 X 위치 조정
                val angleFromHorizontal = curr.middleAngle
                val cosAngle = cos(angleFromHorizontal)

                if (kotlin.math.abs(cosAngle) > 0.01f) {
                    val additionalLength = neededXShift / cosAngle

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

    adjustVerticalLabels(leftLabels)
    adjustVerticalLabels(rightLabels)
    adjustHorizontalLabels(topLabels)
    adjustHorizontalLabels(bottomLabels)

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
        val gap = 8f

        // 3개 텍스트의 총 높이 계산
        val totalTextHeight = label.categoryLayout.size.height +
                            label.amountLayout.size.height +
                            label.percentageLayout.size.height + 12f // 간격 포함 (4f + 8f)

        when (label.direction) {
            LabelDirection.RIGHT -> {
                // 오른쪽: 수직선(lineEndX) 오른쪽에 배치, Y는 선 끝점 기준 중앙
                val textStartX = label.lineEndX + gap
                val textStartY = label.lineEndY - (totalTextHeight / 2)

                drawText(
                    textLayoutResult = label.categoryLayout,
                    topLeft = Offset(textStartX, textStartY)
                )
                drawText(
                    textLayoutResult = label.amountLayout,
                    topLeft = Offset(textStartX, textStartY + label.categoryLayout.size.height + 4f)
                )
                drawText(
                    textLayoutResult = label.percentageLayout,
                    topLeft = Offset(textStartX, textStartY + label.categoryLayout.size.height + label.amountLayout.size.height + 8f)
                )
            }
            LabelDirection.LEFT -> {
                // 왼쪽: 수직선(lineEndX) 왼쪽에 배치, Y는 선 끝점 기준 중앙
                // 각 텍스트를 오른쪽 정렬 (수직선에서 gap만큼 떨어진 곳이 텍스트의 오른쪽 끝)
                val textRightX = label.lineEndX - gap
                val textStartY = label.lineEndY - (totalTextHeight / 2)

                drawText(
                    textLayoutResult = label.categoryLayout,
                    topLeft = Offset(textRightX - label.categoryLayout.size.width, textStartY)
                )
                drawText(
                    textLayoutResult = label.amountLayout,
                    topLeft = Offset(textRightX - label.amountLayout.size.width, textStartY + label.categoryLayout.size.height + 4f)
                )
                drawText(
                    textLayoutResult = label.percentageLayout,
                    topLeft = Offset(textRightX - label.percentageLayout.size.width, textStartY + label.categoryLayout.size.height + label.amountLayout.size.height + 8f)
                )
            }
            LabelDirection.TOP -> {
                // 위쪽: 수직선(lineEndY) 위쪽에 배치, X는 중앙 정렬
                val textBottomY = label.lineEndY - gap
                val textStartY = textBottomY - totalTextHeight

                drawText(
                    textLayoutResult = label.categoryLayout,
                    topLeft = Offset(label.lineEndX - label.categoryLayout.size.width / 2, textStartY)
                )
                drawText(
                    textLayoutResult = label.amountLayout,
                    topLeft = Offset(label.lineEndX - label.amountLayout.size.width / 2, textStartY + label.categoryLayout.size.height + 4f)
                )
                drawText(
                    textLayoutResult = label.percentageLayout,
                    topLeft = Offset(label.lineEndX - label.percentageLayout.size.width / 2, textStartY + label.categoryLayout.size.height + label.amountLayout.size.height + 8f)
                )
            }
            LabelDirection.BOTTOM -> {
                // 아래쪽: 수직선(lineEndY) 아래쪽에 배치, X는 중앙 정렬
                val textStartY = label.lineEndY + gap

                drawText(
                    textLayoutResult = label.categoryLayout,
                    topLeft = Offset(label.lineEndX - label.categoryLayout.size.width / 2, textStartY)
                )
                drawText(
                    textLayoutResult = label.amountLayout,
                    topLeft = Offset(label.lineEndX - label.amountLayout.size.width / 2, textStartY + label.categoryLayout.size.height + 4f)
                )
                drawText(
                    textLayoutResult = label.percentageLayout,
                    topLeft = Offset(label.lineEndX - label.percentageLayout.size.width / 2, textStartY + label.categoryLayout.size.height + label.amountLayout.size.height + 8f)
                )
            }
        }
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