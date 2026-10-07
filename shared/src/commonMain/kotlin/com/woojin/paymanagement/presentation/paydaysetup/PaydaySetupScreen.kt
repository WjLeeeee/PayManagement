package com.woojin.paymanagement.presentation.paydaysetup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.woojin.paymanagement.strings.LocalStrings
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
    val strings = LocalStrings.current

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // 상단 결과 배너 (고르면 바로 바뀜)
            PaydayHeroBanner(
                payday = uiState.selectedPayday,
                currentPeriod = uiState.currentPeriod,
                nextPayday = uiState.nextPayday
            )

            Spacer(modifier = Modifier.height(24.dp))

            PaydaySectionTitle(strings.selectPaydayPrompt)
            Spacer(modifier = Modifier.height(12.dp))
            PaydayGrid(
                selectedPayday = uiState.selectedPayday,
                onPaydaySelected = onPaydaySelected
            )

            Spacer(modifier = Modifier.height(24.dp))

            PaydaySectionTitle(strings.paydayOverlapTitle)
            Spacer(modifier = Modifier.height(10.dp))
            PaydayAdjustmentSegment(
                selectedAdjustment = uiState.selectedAdjustment,
                onAdjustmentSelected = onAdjustmentSelected
            )
            Spacer(modifier = Modifier.height(8.dp))
            PaydayShiftNote(
                nextPayday = uiState.nextPayday,
                nextPaydayNominal = uiState.nextPaydayNominal
            )

            if (uiState.error != null) {
                Spacer(modifier = Modifier.height(16.dp))
                ErrorMessage(
                    error = uiState.error,
                    onDismiss = onErrorDismiss
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        PaydaySetupButton(
            onClick = onCompleteSetup,
            isLoading = uiState.isLoading,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp)
        )
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