package com.woojin.paymanagement.domain.repository

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.domain.model.SharedTransaction
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate

object SharedModeManager {
    var isSharedMode: Boolean = false
    var sharedRoomId: String? = null
    var myDeviceId: String = ""
    var cachedSharedTransactions: List<SharedTransaction> by mutableStateOf(emptyList())
}

/**
 * 현재 공유방의 거래를 기간을 정해 한 번 불러옴 (기간을 안 주면 전체).
 * cachedSharedTransactions는 달력에서 보고 있는 급여 기간만 담고 있어서, 다른 기간이 필요한 화면은 이걸 씀
 */
suspend fun SharedRoomRepository.loadSharedTransactions(
    startDate: LocalDate = LocalDate(1970, 1, 1),
    endDate: LocalDate = LocalDate(9999, 12, 31)
): List<Transaction> {
    val roomId = SharedModeManager.sharedRoomId ?: return emptyList()
    return observeTransactions(roomId, startDate, endDate).first().getOrNull().orEmpty().map { it.transaction }
}
