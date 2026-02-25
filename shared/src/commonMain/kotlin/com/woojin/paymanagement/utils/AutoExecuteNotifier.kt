package com.woojin.paymanagement.utils

import com.woojin.paymanagement.data.RecurringTransaction

interface AutoExecuteNotifier {
    fun notify(transactions: List<RecurringTransaction>)
}

class NoOpAutoExecuteNotifier : AutoExecuteNotifier {
    override fun notify(transactions: List<RecurringTransaction>) {
        // no-op (iOS 등 알림 미지원 플랫폼)
    }
}
