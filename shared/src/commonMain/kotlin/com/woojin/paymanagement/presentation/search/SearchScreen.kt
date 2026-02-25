package com.woojin.paymanagement.presentation.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.woojin.paymanagement.data.IncomeType
import com.woojin.paymanagement.data.PaymentMethod
import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.presentation.addtransaction.getCategoryEmoji
import com.woojin.paymanagement.strings.AppStrings
import com.woojin.paymanagement.strings.LocalStrings
import com.woojin.paymanagement.utils.PlatformBackHandler
import com.woojin.paymanagement.utils.Utils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onNavigateBack: () -> Unit
) {
    val strings = LocalStrings.current
    val uiState = viewModel.uiState

    PlatformBackHandler(onBack = onNavigateBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.transactionSearch) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, strings.goBack)
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

            // 검색 결과 카운트
            if (uiState.keyword.isNotBlank() && !uiState.isLoading) {
                Text(
                    text = strings.searchResultCount(uiState.results.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.keyword.isNotBlank() && uiState.results.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
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
                        SearchResultItem(
                            transaction = transaction,
                            categories = uiState.categories
                        )
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 거래 유형 인디케이터 + 카테고리 이모지
            Box(contentAlignment = Alignment.BottomEnd) {
                Text(
                    text = categoryEmoji,
                    style = MaterialTheme.typography.headlineSmall
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            when (transaction.type) {
                                TransactionType.INCOME -> MaterialTheme.colorScheme.primary
                                TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
                                TransactionType.SAVING -> com.woojin.paymanagement.theme.SavingColor.color
                            }
                        )
                )
            }

            // 내용
            Column(modifier = Modifier.weight(1f)) {
                // 사용처 또는 카테고리
                val title = if (!transaction.merchant.isNullOrBlank()) transaction.merchant
                else transaction.category
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // 카테고리 + 날짜 + 결제수단
                val methodText = getPaymentMethodText(transaction, strings)
                val subLine = buildString {
                    append(transaction.category)
                    append("  ·  ")
                    append(transaction.date)
                    if (methodText.isNotBlank()) {
                        append("  ·  ")
                        append(methodText)
                    }
                }
                Text(
                    text = subLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 메모 (있을 때만)
                if (transaction.memo.isNotBlank()) {
                    Text(
                        text = transaction.memo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 금액
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
                    TransactionType.SAVING -> com.woojin.paymanagement.theme.SavingColor.color
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
