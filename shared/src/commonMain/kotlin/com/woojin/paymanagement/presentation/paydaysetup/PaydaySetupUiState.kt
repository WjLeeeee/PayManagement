package com.woojin.paymanagement.presentation.paydaysetup

import com.woojin.paymanagement.utils.PayPeriod
import com.woojin.paymanagement.utils.PaydayAdjustment
import kotlinx.datetime.LocalDate

data class PaydaySetupUiState(
    val selectedPayday: Int = 25,
    val selectedAdjustment: PaydayAdjustment = PaydayAdjustment.BEFORE_WEEKEND,
    val isLoading: Boolean = false,
    val isSetupComplete: Boolean = false,
    val error: String? = null,
    // 미리보기: 고른 설정 기준 이번 급여 기간과 다음 실제 월급날
    val currentPeriod: PayPeriod? = null,
    val nextPayday: LocalDate? = null,
    // 다음 월급날의 조정 전 날짜 (주말·공휴일로 옮겨졌을 때만 nextPayday와 다름)
    val nextPaydayNominal: LocalDate? = null
)
