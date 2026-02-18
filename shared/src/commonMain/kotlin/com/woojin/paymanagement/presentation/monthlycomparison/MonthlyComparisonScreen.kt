package com.woojin.paymanagement.presentation.monthlycomparison

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woojin.paymanagement.presentation.addtransaction.getCategoryEmoji
import com.woojin.paymanagement.strings.LocalStrings
import com.woojin.paymanagement.utils.BackHandler
import com.woojin.paymanagement.utils.Utils
import kotlin.math.round

@Composable
fun MonthlyComparisonScreen(
    viewModel: MonthlyComparisonViewModel,
    onBack: () -> Unit,
    showPreviousPeriodComparison: Boolean = false
) {
    val strings = LocalStrings.current
    BackHandler(onBack = onBack)

    val uiState = viewModel.uiState

    // 스낵바에서 진입한 경우 이전 급여 기간 비교 모드로 시작
    androidx.compose.runtime.LaunchedEffect(showPreviousPeriodComparison) {
        if (showPreviousPeriodComparison) {
            viewModel.startWithPreviousPeriod()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.goBack,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = strings.payPeriodComparison,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Period Navigation
            PeriodNavigationCard(
                currentPeriod = uiState.currentMonth,
                previousPeriod = uiState.previousMonth,
                onPreviousPeriod = { viewModel.moveToPreviousPeriod() },
                onNextPeriod = { viewModel.moveToNextPeriod() },
                canNavigateNext = viewModel.canNavigateNext()
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // Total Comparison Card
                TotalComparisonCard(
                    currentPeriod = uiState.currentMonth,
                    previousPeriod = uiState.previousMonth,
                    currentTotal = uiState.totalCurrentMonth,
                    previousTotal = uiState.totalPreviousMonth,
                    difference = uiState.totalDifference,
                    differencePercentage = uiState.totalDifferencePercentage
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Category Comparisons
                if (uiState.categoryComparisons.isNotEmpty()) {
                    Text(
                        text = strings.categoryComparison,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    uiState.categoryComparisons.forEach { comparison ->
                        CategoryComparisonCard(
                            comparison = comparison,
                            availableCategories = uiState.availableCategories
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                } else {
                    Text(
                        text = strings.noComparisonData,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun PeriodNavigationCard(
    currentPeriod: String,
    previousPeriod: String,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    canNavigateNext: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onPreviousPeriod) {
                    Text("◀", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurface)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = previousPeriod,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "vs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = currentPeriod,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }

                TextButton(
                    onClick = onNextPeriod,
                    enabled = canNavigateNext
                ) {
                    Text(
                        "▶",
                        fontSize = 24.sp,
                        color = if (canNavigateNext)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TotalComparisonCard(
    currentPeriod: String,
    previousPeriod: String,
    currentTotal: Double,
    previousTotal: Double,
    difference: Double,
    differencePercentage: Float
) {
    val strings = LocalStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = strings.totalExpenseComparison,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 이전 기간
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = strings.previous,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = strings.amountWithUnit(Utils.formatAmount(previousTotal)),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // 현재 기간
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = strings.current,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = strings.amountWithUnit(Utils.formatAmount(currentTotal)),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // 차이
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        if (difference != 0.0) {
                            Text(
                                text = when {
                                    difference > 0 -> strings.increasedArrow
                                    difference < 0 -> strings.savingsArrow
                                    else -> strings.same
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = when {
                                    difference > 0 -> MaterialTheme.colorScheme.error
                                    difference < 0 -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        } else {
                            Text(
                                text = strings.same,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = if (difference == 0.0) {
                                strings.amountWithUnit("0")
                            } else {
                                "${if (difference > 0) "+" else ""}${strings.amountWithUnit(Utils.formatAmount(kotlin.math.abs(difference)))}"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                difference > 0 -> MaterialTheme.colorScheme.error
                                difference < 0 -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                        if (differencePercentage != 0f) {
                            Text(
                                text = "(${if (differencePercentage > 0) "+" else ""}${formatToOneDecimal(differencePercentage)}%)",
                                style = MaterialTheme.typography.bodySmall,
                                color = when {
                                    difference > 0 -> MaterialTheme.colorScheme.error
                                    difference < 0 -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryComparisonCard(
    comparison: CategoryComparison,
    availableCategories: List<com.woojin.paymanagement.data.Category>
) {
    val strings = LocalStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 카테고리 이름
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = getCategoryEmoji(comparison.categoryName, availableCategories),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = comparison.categoryName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 지출 비교
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 이전 기간
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = strings.previous,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = strings.amountWithUnit(Utils.formatAmount(comparison.previousMonthAmount)),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // 현재 기간
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = strings.current,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = strings.amountWithUnit(Utils.formatAmount(comparison.currentMonthAmount)),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // 차이
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    if (!comparison.isUnchanged) {
                        Text(
                            text = when {
                                comparison.isIncrease -> "↑"
                                comparison.isDecrease -> "↓"
                                else -> ""
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = when {
                                comparison.isIncrease -> MaterialTheme.colorScheme.error
                                comparison.isDecrease -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                    Text(
                        text = if (comparison.isUnchanged) {
                            strings.amountWithUnit("0")
                        } else {
                            "${if (comparison.difference > 0) "+" else ""}${strings.amountWithUnit(Utils.formatAmount(kotlin.math.abs(comparison.difference)))}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            comparison.isIncrease -> MaterialTheme.colorScheme.error
                            comparison.isDecrease -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                    if (comparison.differencePercentage != 0f) {
                        Text(
                            text = "(${if (comparison.differencePercentage > 0) "+" else ""}${formatToOneDecimal(comparison.differencePercentage)}%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = when {
                                comparison.isIncrease -> MaterialTheme.colorScheme.error
                                comparison.isDecrease -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Float을 소수점 1자리 문자열로 포맷팅 (iOS 호환)
 */
private fun formatToOneDecimal(value: Float): String {
    val rounded = round(value * 10) / 10
    return if (rounded == rounded.toInt().toFloat()) {
        "${rounded.toInt()}.0"
    } else {
        rounded.toString()
    }
}
