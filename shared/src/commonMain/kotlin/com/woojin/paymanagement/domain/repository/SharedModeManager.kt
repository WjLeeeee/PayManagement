package com.woojin.paymanagement.domain.repository

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.woojin.paymanagement.domain.model.SharedTransaction

object SharedModeManager {
    var isSharedMode: Boolean = false
    var sharedRoomId: String? = null
    var myDeviceId: String = ""
    var cachedSharedTransactions: List<SharedTransaction> by mutableStateOf(emptyList())
}
