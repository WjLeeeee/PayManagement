package com.woojin.paymanagement.presentation.categorymanagement

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.strings.LocalStrings
import com.woojin.paymanagement.utils.PlatformBackHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(
    viewModel: CategoryManagementViewModel,
    onNavigateBack: () -> Unit
) {
    val strings = LocalStrings.current
    val uiState = viewModel.uiState

    // Android 뒤로가기 버튼 처리
    PlatformBackHandler(onBack = onNavigateBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.categoryManagement) },
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
            // 수입/지출/저축 탭
            TabRow(
                selectedTabIndex = when (uiState.selectedType) {
                    TransactionType.INCOME -> 0
                    TransactionType.EXPENSE -> 1
                    TransactionType.SAVING -> 2
                }
            ) {
                Tab(
                    selected = uiState.selectedType == TransactionType.INCOME,
                    onClick = { viewModel.selectType(TransactionType.INCOME) },
                    text = { Text(strings.income) }
                )
                Tab(
                    selected = uiState.selectedType == TransactionType.EXPENSE,
                    onClick = { viewModel.selectType(TransactionType.EXPENSE) },
                    text = { Text(strings.expense) }
                )
                Tab(
                    selected = uiState.selectedType == TransactionType.SAVING,
                    onClick = { viewModel.selectType(TransactionType.SAVING) },
                    text = { Text(strings.saving) }
                )
            }

            // 카테고리 리스트
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 카테고리 추가 아이템 (맨 위)
                item {
                    AddCategoryItem(
                        onClick = { viewModel.showAddDialog() }
                    )
                }

                // 기존 카테고리들
                items(uiState.categories) { category ->
                    CategoryItem(
                        category = category,
                        onEdit = { viewModel.showEditDialog(category) },
                        onDelete = { viewModel.showDeleteConfirmDialog(category) }
                    )
                }
            }
        }

        // 카테고리 추가 다이얼로그
        if (uiState.isAddDialogVisible) {
            AddCategoryDialog(
                name = uiState.newCategoryName,
                emoji = uiState.newCategoryEmoji,
                onNameChange = { viewModel.updateNewCategoryName(it) },
                onEmojiChange = { viewModel.updateNewCategoryEmoji(it) },
                onConfirm = { viewModel.addCategory() },
                onDismiss = { viewModel.hideAddDialog() }
            )
        }

        // 카테고리 수정 다이얼로그
        if (uiState.isEditDialogVisible) {
            EditCategoryDialog(
                name = uiState.editCategoryName,
                emoji = uiState.editCategoryEmoji,
                onNameChange = { viewModel.updateEditCategoryName(it) },
                onEmojiChange = { viewModel.updateEditCategoryEmoji(it) },
                onConfirm = { viewModel.updateCategory() },
                onDismiss = { viewModel.hideEditDialog() }
            )
        }

        // 변경 확인 다이얼로그
        if (uiState.showConfirmDialog) {
            ConfirmDialog(
                message = uiState.confirmDialogMessage,
                onConfirm = { viewModel.showConfirmDialogForUpdate() },
                onDismiss = { viewModel.hideConfirmDialog() }
            )
        }

        // 삭제 확인 다이얼로그
        if (uiState.isDeleteDialogVisible) {
            uiState.deletingCategory?.let { category ->
                DeleteConfirmDialog(
                    categoryName = category.name,
                    categoryEmoji = category.emoji,
                    onConfirm = { viewModel.confirmDelete() },
                    onDismiss = { viewModel.hideDeleteConfirmDialog() }
                )
            }
        }

        // 에러 표시
        uiState.error?.let { errorMessage ->
            AlertDialog(
                onDismissRequest = { viewModel.clearError() },
                text = { Text(errorMessage) },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text(strings.confirm)
                    }
                }
            )
        }
    }
}

@Composable
private fun AddCategoryItem(
    onClick: () -> Unit
) {
    val strings = LocalStrings.current
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = strings.add,
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.addCategory,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun CategoryItem(
    category: com.woojin.paymanagement.data.Category,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val strings = LocalStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
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
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = category.emoji,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = strings.edit,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = strings.delete,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddCategoryDialog(
    name: String,
    emoji: String,
    onNameChange: (String) -> Unit,
    onEmojiChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.addCategory) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = emoji,
                    onValueChange = onEmojiChange,
                    label = { Text(strings.emojiLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text(strings.categoryName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(strings.add)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

@Composable
private fun EditCategoryDialog(
    name: String,
    emoji: String,
    onNameChange: (String) -> Unit,
    onEmojiChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.editCategory) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = emoji,
                    onValueChange = onEmojiChange,
                    label = { Text(strings.emojiLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text(strings.categoryName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(strings.edit)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

@Composable
private fun ConfirmDialog(
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(strings.continueAction)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

@Composable
private fun DeleteConfirmDialog(
    categoryName: String,
    categoryEmoji: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.deleteCategory) },
        text = {
            Text(strings.deleteCategoryConfirmMessage(categoryEmoji, categoryName))
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(strings.delete)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}
