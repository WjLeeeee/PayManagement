package com.woojin.paymanagement.presentation.paydaysetup

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.woojin.paymanagement.utils.PaydayAdjustment

@Composable
fun PaydaySetupScreen(
    viewModel: PaydaySetupViewModel,
    onSetupComplete: (payday: Int, adjustment: PaydayAdjustment) -> Unit
) {
    val uiState = viewModel.uiState

    LaunchedEffect(uiState.isSetupComplete) {
        if (uiState.isSetupComplete) {
            onSetupComplete(uiState.selectedPayday, uiState.selectedAdjustment)
        }
    }

    PaydaySetupContent(
        uiState = uiState,
        onPaydaySelected = viewModel::selectPayday,
        onAdjustmentSelected = viewModel::selectAdjustment,
        onCompleteSetup = viewModel::completeSetup,
        onErrorDismiss = viewModel::clearError
    )
}

@Composable
fun PaydaySetupContent(
    uiState: PaydaySetupUiState,
    onPaydaySelected: (Int) -> Unit,
    onAdjustmentSelected: (PaydayAdjustment) -> Unit,
    onCompleteSetup: () -> Unit,
    onErrorDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        PaydaySetupHeader(
            title = "월급날 설정",
            description = "월급날을 선택하시면 해당 날짜 기준으로\n한 달 단위로 관리됩니다"
        )

        Spacer(modifier = Modifier.height(40.dp))

        PaydaySelector(
            selectedPayday = uiState.selectedPayday,
            onPaydaySelected = onPaydaySelected
        )

        Spacer(modifier = Modifier.height(40.dp))

        PaydayAdjustmentSelector(
            selectedAdjustment = uiState.selectedAdjustment,
            onAdjustmentSelected = onAdjustmentSelected
        )

        if (uiState.error != null) {
            Spacer(modifier = Modifier.height(16.dp))
            ErrorMessage(
                error = uiState.error,
                onDismiss = onErrorDismiss
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        PaydaySetupButton(
            onClick = onCompleteSetup,
            isLoading = uiState.isLoading
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun PaydaySetupContent(
    selectedPayday: Int,
    selectedAdjustment: PaydayAdjustment,
    onPaydaySelected: (Int) -> Unit,
    onAdjustmentSelected: (PaydayAdjustment) -> Unit,
    onCompleteSetup: () -> Unit,
    isLoading: Boolean,
    error: String?,
    onErrorDismiss: () -> Unit
) {
    val uiState = PaydaySetupUiState(
        selectedPayday = selectedPayday,
        selectedAdjustment = selectedAdjustment,
        isLoading = isLoading,
        error = error
    )

    PaydaySetupContent(
        uiState = uiState,
        onPaydaySelected = onPaydaySelected,
        onAdjustmentSelected = onAdjustmentSelected,
        onCompleteSetup = onCompleteSetup,
        onErrorDismiss = onErrorDismiss
    )
}