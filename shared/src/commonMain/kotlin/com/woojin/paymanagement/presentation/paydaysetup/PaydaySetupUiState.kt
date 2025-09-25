package com.woojin.paymanagement.presentation.paydaysetup

import com.woojin.paymanagement.utils.PaydayAdjustment

data class PaydaySetupUiState(
    val selectedPayday: Int = 25,
    val selectedAdjustment: PaydayAdjustment = PaydayAdjustment.BEFORE_WEEKEND,
    val isLoading: Boolean = false,
    val isSetupComplete: Boolean = false,
    val error: String? = null
)