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
                title = { Text("쿠폰 입력") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로가기")
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
                text = "쿠폰 코드 입력",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // 설명
            Text(
                text = "쿠폰 코드를 입력하시면 3일간 광고 없이 사용하실 수 있습니다.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 쿠폰 코드 입력 필드
            OutlinedTextField(
                value = uiState.couponCode,
                onValueChange = viewModel::onCouponCodeChange,
                label = { Text("쿠폰 코드") },
                placeholder = { Text("쿠폰 코드를 입력하세요") },
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
                Text(if (uiState.isApplying) "적용 중..." else "쿠폰 적용하기")
            }
        }
    }

    // 성공 다이얼로그
    if (uiState.showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("쿠폰 적용 완료!") },
            text = {
                Text("쿠폰이 성공적으로 적용되었습니다.\n3일간 광고 없이 사용하실 수 있습니다.\n\n앱을 재시작하면 적용됩니다.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.dismissSuccessDialog()
                        onRequestRestart?.invoke() ?: onNavigateBack()
                    }
                ) {
                    Text("확인")
                }
            }
        )
    }
}
