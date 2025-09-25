package com.woojin.paymanagement.domain.model

import com.woojin.paymanagement.utils.PaydayAdjustment

data class PaydaySetup(
    val payday: Int,
    val adjustment: PaydayAdjustment
) {
    init {
        require(payday in 1..31) { "Payday must be between 1 and 31" }
    }
}