package com.woojin.paymanagement.presentation.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.woojin.paymanagement.data.IncomeType
import com.woojin.paymanagement.data.PaymentMethod
import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.presentation.addtransaction.getCategoryEmoji
import com.woojin.paymanagement.strings.AppStrings
import com.woojin.paymanagement.strings.LocalStrings
import com.woojin.paymanagement.theme.SavingColor
import com.woojin.paymanagement.utils.PlatformBackHandler
import com.woojin.paymanagement.utils.Utils
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onNavigateBack: () -> Unit
) {
    val strings = LocalStrings.current
    val uiState = viewModel.uiState

    PlatformBackHandler(onBack = onNavigateBack)

    // 날짜 범위 선택 다이얼로그
    if (uiState.showDatePicker) {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        SearchDateRangePickerDialog(
            initialStartDate = uiState.startDate ?: today,
            initialEndDate = uiState.endDate ?: today,
            onDateRangeSelected = { start, end -> viewModel.onDateRangeSelected(start, end) },
            onDismiss = { viewModel.onShowDatePicker(false) }
        )
    }

    // 필터 바텀시트
    if (uiState.showFilterSheet) {
        SearchFilterSheet(
            uiState = uiState,
            strings = strings,
            onTypeToggle = { viewModel.onTypeToggle(it) },
            onCategoryToggle = { viewModel.onCategoryToggle(it) },
            onDateRangeClick = { viewModel.onShowDatePicker(true) },
            onClearDateRange = { viewModel.onClearDateRange() },
            onClearFilters = { viewModel.clearFilters() },
            onDismiss = { viewModel.onShowFilterSheet(false) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.transactionSearch) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, strings.goBack)
                    }
                },
                actions = {
                    BadgedBox(
                        badge = {
                            if (uiState.activeFilterCount > 0) {
                                Badge { Text(uiState.activeFilterCount.toString()) }
                            }
                        }
                    ) {
                        TextButton(onClick = { viewModel.onShowFilterSheet(true) }) {
                            Text(strings.filter)
                        }
                    }
                },
                windowInsets = WindowInsets(0.dp)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 검색 입력창
            OutlinedTextField(
                value = uiState.keyword,
                onValueChange = { viewModel.onKeywordChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(strings.searchHint) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = strings.search)
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // 활성 필터 칩
            if (uiState.hasActiveFilters) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.selectedTypes.forEach { type ->
                        val label = when (type) {
                            TransactionType.INCOME -> strings.income
                            TransactionType.EXPENSE -> strings.expense
                            TransactionType.SAVING -> strings.saving
                        }
                        FilterChip(
                            selected = true,
                            onClick = { viewModel.onTypeToggle(type) },
                            label = { Text(label) }
                        )
                    }
                    uiState.selectedCategories.forEach { cat ->
                        FilterChip(
                            selected = true,
                            onClick = { viewModel.onCategoryToggle(cat) },
                            label = { Text(cat) }
                        )
                    }
                    if (uiState.startDate != null || uiState.endDate != null) {
                        val dateLabel = buildString {
                            uiState.startDate?.let { append("${it.monthNumber}/${it.dayOfMonth}") }
                            append(" ~ ")
                            uiState.endDate?.let { append("${it.monthNumber}/${it.dayOfMonth}") }
                        }
                        FilterChip(
                            selected = true,
                            onClick = { viewModel.onClearDateRange() },
                            label = { Text(dateLabel) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // 결과 카운트
            val showResults = uiState.keyword.isNotBlank() || uiState.hasActiveFilters
            if (showResults && !uiState.isLoading) {
                Text(
                    text = strings.searchResultCount(uiState.results.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (showResults && uiState.results.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = strings.noSearchResults,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.results) { transaction ->
                        SearchResultItem(transaction = transaction, categories = uiState.categories)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SearchFilterSheet(
    uiState: SearchUiState,
    strings: AppStrings,
    onTypeToggle: (TransactionType) -> Unit,
    onCategoryToggle: (String) -> Unit,
    onDateRangeClick: () -> Unit,
    onClearDateRange: () -> Unit,
    onClearFilters: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 헤더
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.filter,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onClearFilters) {
                    Text(strings.filterReset)
                }
            }

            HorizontalDivider()

            // 거래 유형
            Text(
                text = strings.filterTransactionType,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    TransactionType.INCOME to strings.income,
                    TransactionType.EXPENSE to strings.expense,
                    TransactionType.SAVING to strings.saving
                ).forEach { (type, label) ->
                    val isSelected = type in uiState.selectedTypes
                    val chipColor = when {
                        !isSelected -> MaterialTheme.colorScheme.surfaceVariant
                        type == TransactionType.INCOME -> Color(0xFFE3F2FD)
                        type == TransactionType.EXPENSE -> Color(0xFFFFEBEE)
                        else -> SavingColor.lightBackground
                    }
                    val borderColor = when {
                        !isSelected -> Color.Transparent
                        type == TransactionType.INCOME -> MaterialTheme.colorScheme.primary
                        type == TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
                        else -> SavingColor.color
                    }
                    Box(
                        modifier = Modifier
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = borderColor,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .background(color = chipColor, shape = RoundedCornerShape(20.dp))
                            .clickable { onTypeToggle(type) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // 카테고리
            if (uiState.categories.isNotEmpty()) {
                Text(
                    text = strings.filterCategory,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.categories.distinctBy { it.name }.forEach { category ->
                        val isSelected = category.name in uiState.selectedCategories
                        val emoji = getCategoryEmoji(category.name, uiState.categories)
                        Box(
                            modifier = Modifier
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .background(
                                    color = if (isSelected) Color(0xFFE3F2FD)
                                            else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { onCategoryToggle(category.name) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = emoji, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // 날짜 범위
            Text(
                text = strings.filterDateRange,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            val hasDate = uiState.startDate != null || uiState.endDate != null
            val dateLabel = if (hasDate) {
                buildString {
                    uiState.startDate?.let {
                        append("${it.year}.${it.monthNumber.toString().padStart(2, '0')}.${it.dayOfMonth.toString().padStart(2, '0')}")
                    }
                    append(" ~ ")
                    uiState.endDate?.let {
                        append("${it.year}.${it.monthNumber.toString().padStart(2, '0')}.${it.dayOfMonth.toString().padStart(2, '0')}")
                    }
                }
            } else {
                strings.filterStartDate + " ~ " + strings.filterEndDate
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDateRangeClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hasDate) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (hasDate) {
                    TextButton(onClick = onClearDateRange) {
                        Text(strings.filterReset)
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchDateRangePickerDialog(
    initialStartDate: LocalDate,
    initialEndDate: LocalDate,
    onDateRangeSelected: (LocalDate, LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    var displayMonth by remember { mutableStateOf(initialStartDate.monthNumber) }
    var displayYear by remember { mutableStateOf(initialStartDate.year) }
    var tempStartDate by remember { mutableStateOf<LocalDate?>(null) }
    var tempEndDate by remember { mutableStateOf<LocalDate?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // 월 네비게이션
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        if (displayMonth > 1) displayMonth-- else { displayMonth = 12; displayYear-- }
                    }) {
                        Text("◀", color = MaterialTheme.colorScheme.primary,
                            fontSize = MaterialTheme.typography.titleLarge.fontSize)
                    }
                    Text(
                        text = strings.monthYear(displayYear, displayMonth),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = {
                        if (displayMonth < 12) displayMonth++ else { displayMonth = 1; displayYear++ }
                    }) {
                        Text("▶", color = MaterialTheme.colorScheme.primary,
                            fontSize = MaterialTheme.typography.titleLarge.fontSize)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 요일 헤더 + 날짜 그리드
                val daysInMonth = when (displayMonth) {
                    2 -> if (displayYear % 4 == 0 && (displayYear % 100 != 0 || displayYear % 400 == 0)) 29 else 28
                    4, 6, 9, 11 -> 30
                    else -> 31
                }
                val firstDayOfMonth = LocalDate(displayYear, displayMonth, 1)
                val firstDayOfWeek = firstDayOfMonth.dayOfWeek.ordinal + 1
                val startOffset = if (firstDayOfWeek == 7) 0 else firstDayOfWeek

                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier.fillMaxWidth().height(280.dp)
                ) {
                    items(strings.weekdaysShort) { day ->
                        Box(
                            modifier = Modifier.aspectRatio(1f).padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(day, style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    items(startOffset) { Spacer(modifier = Modifier.aspectRatio(1f)) }
                    items(daysInMonth) { dayIndex ->
                        val day = dayIndex + 1
                        val date = LocalDate(displayYear, displayMonth, day)
                        val isStart = date == tempStartDate
                        val isEnd = date == tempEndDate
                        val isInRange = tempStartDate != null && tempEndDate != null &&
                                        date > tempStartDate!! && date < tempEndDate!!
                        val isToday = date == today

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .background(
                                    when {
                                        isStart || isEnd -> MaterialTheme.colorScheme.primary
                                        isInRange -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                        isToday -> MaterialTheme.colorScheme.surfaceVariant
                                        else -> Color.Transparent
                                    },
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    when {
                                        tempStartDate == null -> { tempStartDate = date; tempEndDate = null }
                                        tempEndDate == null -> {
                                            if (date >= tempStartDate!!) tempEndDate = date
                                            else { tempStartDate = date; tempEndDate = null }
                                        }
                                        else -> { tempStartDate = date; tempEndDate = null }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = when {
                                    isStart || isEnd -> Color.White
                                    isInRange -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                                fontWeight = if (isStart || isEnd || isToday) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = when {
                        tempStartDate == null -> strings.selectStartDate
                        tempEndDate == null -> strings.selectEndDate
                        else -> "${tempStartDate!!.year}.${tempStartDate!!.monthNumber.toString().padStart(2,'0')}.${tempStartDate!!.dayOfMonth.toString().padStart(2,'0')} ~ ${tempEndDate!!.year}.${tempEndDate!!.monthNumber.toString().padStart(2,'0')}.${tempEndDate!!.dayOfMonth.toString().padStart(2,'0')}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (tempStartDate != null && tempEndDate != null) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text(strings.cancel, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Button(
                        onClick = {
                            if (tempStartDate != null && tempEndDate != null) {
                                onDateRangeSelected(tempStartDate!!, tempEndDate!!)
                            }
                        },
                        enabled = tempStartDate != null && tempEndDate != null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(strings.confirm, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultItem(
    transaction: Transaction,
    categories: List<com.woojin.paymanagement.data.Category>
) {
    val strings = LocalStrings.current
    val categoryEmoji = getCategoryEmoji(transaction.category, categories)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                Text(text = categoryEmoji, style = MaterialTheme.typography.headlineSmall)
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            when (transaction.type) {
                                TransactionType.INCOME -> MaterialTheme.colorScheme.primary
                                TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
                                TransactionType.SAVING -> SavingColor.color
                            }
                        )
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                val title = if (!transaction.merchant.isNullOrBlank()) transaction.merchant
                            else transaction.category
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val methodText = getPaymentMethodText(transaction, strings)
                val subLine = buildString {
                    append(transaction.category)
                    append("  ·  ")
                    append(transaction.date)
                    if (methodText.isNotBlank()) { append("  ·  "); append(methodText) }
                }
                Text(text = subLine, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (transaction.memo.isNotBlank()) {
                    Text(text = transaction.memo, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Text(
                text = "${when (transaction.type) {
                    TransactionType.INCOME -> "+"
                    TransactionType.EXPENSE -> "-"
                    TransactionType.SAVING -> "-"
                }}${strings.amountWithUnit(Utils.formatAmount(transaction.displayAmount))}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = when (transaction.type) {
                    TransactionType.INCOME -> MaterialTheme.colorScheme.primary
                    TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
                    TransactionType.SAVING -> SavingColor.color
                }
            )
        }
    }
}

private fun getPaymentMethodText(transaction: Transaction, strings: AppStrings): String {
    return when (transaction.type) {
        TransactionType.EXPENSE -> when (transaction.paymentMethod) {
            PaymentMethod.CASH -> strings.cash
            PaymentMethod.CARD -> transaction.cardName ?: strings.card
            PaymentMethod.BALANCE_CARD -> strings.balanceCard
            PaymentMethod.GIFT_CARD -> strings.giftCard
            null -> ""
        }
        TransactionType.INCOME -> when (transaction.incomeType) {
            IncomeType.CASH -> strings.cash
            IncomeType.BALANCE_CARD -> strings.balanceCard
            IncomeType.GIFT_CARD -> strings.giftCard
            null -> ""
        }
        TransactionType.SAVING -> ""
    }
}
