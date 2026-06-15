package com.woojin.paymanagement.domain.repository

import com.woojin.paymanagement.domain.model.SharedTransaction

object SharedModeManager {
    var isSharedMode: Boolean = false
    var sharedRoomId: String? = null
    var myDeviceId: String = ""
    var cachedSharedTransactions: List<SharedTransaction> = emptyList()
}
