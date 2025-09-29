package com.woojin.paymanagement.presentation.tutorial

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class TutorialStep(
    val id: String,
    val targetBounds: Rect?,
    val title: String,
    val description: String,
    val tooltipPosition: TooltipPosition = TooltipPosition.BOTTOM
)

enum class TooltipPosition {
    TOP, BOTTOM, LEFT, RIGHT, CENTER
}

@Composable
fun CalendarTutorialOverlay(
    currentStep: TutorialStep?,
    totalSteps: Int,
    currentStepIndex: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onComplete: () -> Unit,
    calendarGridBounds: Rect? = null,
    modifier: Modifier = Modifier
) {
    if (currentStep == null) return

    val density = LocalDensity.current

    // EdgeToEdge 대응을 위한 상태바 높이 계산
    val statusBarHeight = with(density) {
        WindowInsets.statusBars.getTop(this).toDp()
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 어두운 오버레이와 스포트라이트 효과
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            // 전체 화면을 어둡게
            val overlayColor = Color.Black.copy(alpha = 0.7f)
            drawRect(
                color = overlayColor,
                size = size
            )

            // 스포트라이트 효과 (하이라이트 영역)
            currentStep.targetBounds?.let { originalBounds ->
                // EdgeToEdge 대응: 상태바 높이만큼 bounds를 아래로 이동
                val adjustedBounds = Rect(
                    originalBounds.left,
                    originalBounds.top - statusBarHeight.toPx(),
                    originalBounds.right,
                    originalBounds.bottom - statusBarHeight.toPx()
                )

                val inflatedBounds = Rect(
                    adjustedBounds.left - 16.dp.toPx(),
                    adjustedBounds.top - 16.dp.toPx(),
                    adjustedBounds.right + 16.dp.toPx(),
                    adjustedBounds.bottom + 16.dp.toPx()
                )

                val path = Path().apply {
                    addRoundRect(
                        RoundRect(
                            inflatedBounds,
                            CornerRadius(12.dp.toPx())
                        )
                    )
                }

                clipPath(path, ClipOp.Difference) {
                    drawRect(
                        color = overlayColor,
                        size = size
                    )
                }

                // 하이라이트 테두리
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.8f),
                    topLeft = adjustedBounds.topLeft - Offset(16.dp.toPx(), 16.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(
                        adjustedBounds.width + 32.dp.toPx(),
                        adjustedBounds.height + 32.dp.toPx()
                    ),
                    cornerRadius = CornerRadius(12.dp.toPx()),
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }

        // 툴팁
        TutorialTooltip(
            step = currentStep,
            totalSteps = totalSteps,
            currentStepIndex = currentStepIndex,
            onNext = onNext,
            onSkip = onSkip,
            onComplete = onComplete,
            calendarGridBounds = calendarGridBounds,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun TutorialTooltip(
    step: TutorialStep,
    totalSteps: Int,
    currentStepIndex: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onComplete: () -> Unit,
    calendarGridBounds: Rect?,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val statusBarHeight = with(density) {
        WindowInsets.statusBars.getTop(this).toDp()
    }

    Box(modifier = modifier) {
        Card(
            modifier = Modifier
                .then(
                    if (calendarGridBounds != null) {
                        // 캘린더 그리드 중앙에 툴팁 배치
                        with(density) {
                            val statusBarPx = statusBarHeight.toPx()
                            val adjustedBounds = Rect(
                                calendarGridBounds.left,
                                calendarGridBounds.top - statusBarPx,
                                calendarGridBounds.right,
                                calendarGridBounds.bottom - statusBarPx
                            )

                            val centerX = (adjustedBounds.left + adjustedBounds.right) / 2
                            val centerY = (adjustedBounds.top + adjustedBounds.bottom) / 2

                            Modifier.offset(
                                x = (centerX - 160.dp.toPx()).toDp(), // 툴팁 너비 절반만큼 왼쪽으로
                                y = (centerY - 100.dp.toPx()).toDp()  // 툴팁 높이 절반만큼 위로
                            )
                        }
                    } else {
                        // calendarGridBounds가 없을 때는 캘린더 그리드 예상 위치에 배치 (급여기간요약 아래쪽)
                        Modifier.align(Alignment.Center).offset(y = (-65).dp)
                    }
                )
                .padding(24.dp)
                .widthIn(max = 320.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // 헤더
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${currentStepIndex + 1}/$totalSteps",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )

                    IconButton(
                        onClick = onSkip,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "건너뛰기",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 제목
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 설명
                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 버튼들
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (currentStepIndex > 0) {
                        TextButton(onClick = onSkip) {
                            Text(
                                "건너뛰기",
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    val isLastStep = currentStepIndex == totalSteps - 1

                    Button(
                        onClick = if (isLastStep) onComplete else onNext,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2196F3)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text(
                            text = if (isLastStep) "완료" else "다음",
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
