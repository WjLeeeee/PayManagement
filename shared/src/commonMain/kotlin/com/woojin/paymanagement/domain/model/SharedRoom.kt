package com.woojin.paymanagement.domain.model

import com.woojin.paymanagement.data.Transaction
import kotlinx.serialization.Serializable

@Serializable
data class SharedRoom(
    val roomId: String,
    val roomCode: String,
    val members: List<RoomMember> = emptyList(),
    val createdAt: Long = 0L
)

@Serializable
data class RoomMember(
    val deviceId: String,
    val nickname: String = "",
    val joinedAt: Long = 0L
)

data class SharedTransaction(
    val transaction: Transaction,
    val deviceId: String,
    val isMine: Boolean
)
