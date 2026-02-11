package com.woojin.paymanagement.presentation.coupon

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.woojin.paymanagement.strings.LocalStrings
import com.woojin.paymanagement.utils.PlatformBackHandler

/**
 * 쿠폰 입력 화면
 * 쿠폰 코드를 입력하여 광고 제거를 받을 수 있습니다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CouponScreen(
    viewModel: CouponViewModel,
    onNavigateBack: () -> Unit,
    onRequestRestart: (() -> Unit)? = null
) {
    // Android 뒤로가기 버튼 처리
    PlatformBackHandler(onBack = onNavigateBack)

    val strings = LocalStrings.current
    val uiState = viewModel.uiState
    val snackbarHostState = remember { SnackbarHostState() }

    // 에러 메시지 표시
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.dismissError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.enterCoupon) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, strings.goBack)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                windowInsets = WindowInsets(0.dp)
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 제목
            Text(
                text = strings.couponCodeInput,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // 설명
            Text(
                text = strings.couponDesc,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 쿠폰 코드 입력 필드
            OutlinedTextField(
                value = uiState.couponCode,
                onValueChange = viewModel::onCouponCodeChange,
                label = { Text(strings.couponCode) },
                placeholder = { Text(strings.enterCouponCode) },
                enabled = !uiState.isApplying,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 적용 버튼
            Button(
                onClick = { viewModel.applyCoupon() },
                enabled = !uiState.isApplying && uiState.couponCode.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isApplying) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Text(if (uiState.isApplying) strings.applyingCoupon else strings.applyCouponButton)
            }
        }
    }

    // 성공 다이얼로그
    if (uiState.showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(strings.couponApplyComplete) },
            text = {
                Text(strings.couponSuccessMessage)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.dismissSuccessDialog()
                        onRequestRestart?.invoke() ?: onNavigateBack()
                    }
                ) {
                    Text(strings.confirm)
                }
            }
        )
    }
}
