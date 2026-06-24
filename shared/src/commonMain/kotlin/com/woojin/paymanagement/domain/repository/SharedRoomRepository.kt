package com.woojin.paymanagement.domain.repository

import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.domain.model.SharedRoom
import com.woojin.paymanagement.domain.model.SharedTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface SharedRoomRepository {

    // 현재 디바이스 ID (익명 인증)
    fun getDeviceId(): String

    // 공유방 생성 → 6자리 코드 반환
    suspend fun createRoom(nickname: String): SharedRoom

    // 코드로 공유방 참여
    suspend fun joinRoom(roomCode: String, nickname: String): SharedRoom?

    // 공유방 나가기
    suspend fun leaveRoom()

    // 현재 참여 중인 방 정보 (없으면 null)
    suspend fun getCurrentRoom(): SharedRoom?

    // 공유 거래 추가
    suspend fun addTransaction(roomId: String, transaction: Transaction)

    // 공유 거래 수정
    suspend fun updateTransaction(roomId: String, transaction: Transaction)

    // 공유 거래 삭제
    suspend fun deleteTransaction(roomId: String, transactionId: String)

    // 실시간 공유 거래 목록 구독 (급여 기간 내 거래만)
    // Result.success = 정상 데이터, Result.failure = 연결 오류
    fun observeTransactions(roomId: String, startDate: LocalDate, endDate: LocalDate): Flow<Result<List<SharedTransaction>>>
}
