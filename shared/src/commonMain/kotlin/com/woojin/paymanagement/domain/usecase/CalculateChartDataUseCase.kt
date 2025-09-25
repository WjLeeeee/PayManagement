package com.woojin.paymanagement.domain.usecase

import com.woojin.paymanagement.data.ChartData
import com.woojin.paymanagement.data.ChartDataCalculator
import com.woojin.paymanagement.data.Transaction

class CalculateChartDataUseCase {
    operator fun invoke(transactions: List<Transaction>): ChartData {
        return ChartDataCalculator.calculateChartData(transactions)
    }
}