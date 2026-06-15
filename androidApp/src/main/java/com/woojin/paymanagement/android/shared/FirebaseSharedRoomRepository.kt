package com.woojin.paymanagement.android.shared

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.woojin.paymanagement.data.IncomeType
import com.woojin.paymanagement.data.PaymentMethod
import com.woojin.paymanagement.data.Transaction
import com.woojin.paymanagement.data.TransactionType
import com.woojin.paymanagement.domain.model.RoomMember
import com.woojin.paymanagement.domain.model.SharedRoom
import com.woojin.paymanagement.domain.model.SharedTransaction
import com.woojin.paymanagement.domain.repository.SharedRoomRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.LocalDate

class FirebaseSharedRoomRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val preferencesManager: com.woojin.paymanagement.utils.PreferencesManager
) : SharedRoomRepository {

    companion object {
        private const val ROOMS = "shared_rooms"
        private const val TRANSACTIONS = "transactions"
        private const val MEMBERS = "members"
    }

    private var transactionListener: ListenerRegistration? = null

    override fun getDeviceId(): String = auth.currentUser?.uid ?: ""

    override suspend fun createRoom(nickname: String): SharedRoom {
        ensureSignedIn()

        val roomCode = generateUniqueRoomCode()
        val deviceId = getDeviceId()
        val now = System.currentTimeMillis()

        val member = mapOf(
            "deviceId" to deviceId,
            "nickname" to nickname,
            "joinedAt" to now
        )

        val roomData = mapOf(
            "roomCode" to roomCode,
            "createdAt" to now
        )

        val roomRef = firestore.collection(ROOMS).document()
        roomRef.set(roomData).await()
        roomRef.collection(MEMBERS).document(deviceId).set(member).await()

        val room = SharedRoom(
            roomId = roomRef.id,
            roomCode = roomCode,
            members = listOf(RoomMember(deviceId = deviceId, nickname = nickname, joinedAt = now)),
            createdAt = now
        )

        saveCurrentRoomId(roomRef.id)
        return room
    }

    override suspend fun joinRoom(roomCode: String, nickname: String): SharedRoom? {
        ensureSignedIn()

        val snapshot = firestore.collection(ROOMS)
            .whereEqualTo("roomCode", roomCode.uppercase())
            .get()
            .await()

        if (snapshot.isEmpty) return null

        val roomDoc = snapshot.documents.first()
        val roomId = roomDoc.id
        val deviceId = getDeviceId()
        val now = System.currentTimeMillis()

        val member = mapOf(
            "deviceId" to deviceId,
            "nickname" to nickname,
            "joinedAt" to now
        )
        roomDoc.reference.collection(MEMBERS).document(deviceId).set(member).await()

        val membersSnapshot = roomDoc.reference.collection(MEMBERS).get().await()
        val members = membersSnapshot.documents.map {
            RoomMember(
                deviceId = it.getString("deviceId") ?: "",
                nickname = it.getString("nickname") ?: "",
                joinedAt = it.getLong("joinedAt") ?: 0L
            )
        }

        val room = SharedRoom(
            roomId = roomId,
            roomCode = roomCode.uppercase(),
            members = members,
            createdAt = roomDoc.getLong("createdAt") ?: 0L
        )

        saveCurrentRoomId(roomId)
        return room
    }

    override suspend fun leaveRoom() {
        val roomId = preferencesManager.getSharedRoomId() ?: return
        val deviceId = getDeviceId()
        val roomRef = firestore.collection(ROOMS).document(roomId)

        // 자신을 제외한 나머지 멤버 확인 (삭제 전에 해야 권한 유지됨)
        val allMembers = roomRef.collection(MEMBERS).get().await()
        val isLastMember = allMembers.documents.all { it.id == deviceId }

        if (isLastMember) {
            // 마지막 멤버면 방 전체 삭제 (아직 멤버이므로 권한 있음)
            val transactions = roomRef.collection(TRANSACTIONS).get().await()
            transactions.documents.forEach { it.reference.delete().await() }
            roomRef.collection(MEMBERS).document(deviceId).delete().await()
            roomRef.delete().await()
        } else {
            roomRef.collection(MEMBERS).document(deviceId).delete().await()
        }

        transactionListener?.remove()
        saveCurrentRoomId(null)
    }

    override suspend fun getCurrentRoom(): SharedRoom? {
        val roomId = preferencesManager.getSharedRoomId() ?: return null
        ensureSignedIn()

        val roomDoc = firestore.collection(ROOMS).document(roomId).get().await()
        if (!roomDoc.exists()) {
            saveCurrentRoomId(null)
            return null
        }

        val deviceId = getDeviceId()
        val membersSnapshot = roomDoc.reference.collection(MEMBERS).get().await()
        val members = membersSnapshot.documents.map {
            RoomMember(
                deviceId = it.getString("deviceId") ?: "",
                nickname = it.getString("nickname") ?: "",
                joinedAt = it.getLong("joinedAt") ?: 0L
            )
        }

        // 내가 멤버가 아니면 (다른 기기에서 나간 방, 빈 방 등) null 반환
        if (members.none { it.deviceId == deviceId }) {
            saveCurrentRoomId(null)
            return null
        }

        return SharedRoom(
            roomId = roomId,
            roomCode = roomDoc.getString("roomCode") ?: "",
            members = members,
            createdAt = roomDoc.getLong("createdAt") ?: 0L
        )
    }

    override suspend fun addTransaction(roomId: String, transaction: Transaction) {
        val deviceId = getDeviceId()
        val data = transaction.toFirestoreMap(deviceId)
        firestore.collection(ROOMS)
            .document(roomId)
            .collection(TRANSACTIONS)
            .document(transaction.id)
            .set(data)
            .await()
    }

    override suspend fun updateTransaction(roomId: String, transaction: Transaction) {
        val deviceId = getDeviceId()
        val data = transaction.toFirestoreMap(deviceId)
        firestore.collection(ROOMS)
            .document(roomId)
            .collection(TRANSACTIONS)
            .document(transaction.id)
            .set(data)
            .await()
    }

    override suspend fun deleteTransaction(roomId: String, transactionId: String) {
        firestore.collection(ROOMS)
            .document(roomId)
            .collection(TRANSACTIONS)
            .document(transactionId)
            .delete()
            .await()
    }

    override fun observeTransactions(roomId: String, startDate: LocalDate, endDate: LocalDate): Flow<Result<List<SharedTransaction>>> = callbackFlow {
        val deviceId = getDeviceId()

        val listener = firestore.collection(ROOMS)
            .document(roomId)
            .collection(TRANSACTIONS)
            .whereGreaterThanOrEqualTo("date", startDate.toString())
            .whereLessThanOrEqualTo("date", endDate.toString())
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(Result.failure(error ?: Exception("Snapshot was null")))
                    return@addSnapshotListener
                }

                val transactions = snapshot.documents.mapNotNull { doc ->
                    doc.toSharedTransaction(deviceId)
                }
                trySend(Result.success(transactions))
            }

        transactionListener = listener
        awaitClose { listener.remove() }
    }

    // --- private helpers ---

    private suspend fun ensureSignedIn() {
        if (auth.currentUser == null) {
            auth.signInAnonymously().await()
        }
    }

    private suspend fun generateUniqueRoomCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        repeat(10) {
            val code = (1..6).map { chars.random() }.joinToString("")
            val existing = firestore.collection(ROOMS).whereEqualTo("roomCode", code).get().await()
            if (existing.isEmpty) return code
        }
        return (1..6).map { chars.random() }.joinToString("")
    }

    private fun saveCurrentRoomId(roomId: String?) {
        preferencesManager.setSharedRoomId(roomId)
    }

    private fun Transaction.toFirestoreMap(deviceId: String): Map<String, Any?> = mapOf(
        "id" to id,
        "amount" to amount,
        "type" to type.name,
        "category" to category,
        "merchant" to merchant,
        "memo" to memo,
        "date" to date.toString(),
        "incomeType" to incomeType?.name,
        "paymentMethod" to paymentMethod?.name,
        "balanceCardId" to balanceCardId,
        "giftCardId" to giftCardId,
        "cardName" to cardName,
        "settlementAmount" to settlementAmount,
        "isSettlement" to isSettlement,
        "deviceId" to deviceId,
        "syncedAt" to System.currentTimeMillis()
    )

    private fun com.google.firebase.firestore.DocumentSnapshot.toSharedTransaction(myDeviceId: String): SharedTransaction? {
        return try {
            val id = getString("id") ?: return null
            val amount = getDouble("amount") ?: return null
            val type = TransactionType.valueOf(getString("type") ?: return null)
            val category = getString("category") ?: return null
            val memo = getString("memo") ?: ""
            val dateStr = getString("date") ?: return null
            val date = LocalDate.parse(dateStr)
            val deviceId = getString("deviceId") ?: ""

            val transaction = Transaction(
                id = id,
                amount = amount,
                type = type,
                category = category,
                merchant = getString("merchant"),
                memo = memo,
                date = date,
                incomeType = getString("incomeType")?.let { IncomeType.valueOf(it) },
                paymentMethod = getString("paymentMethod")?.let { PaymentMethod.valueOf(it) },
                balanceCardId = getString("balanceCardId"),
                giftCardId = getString("giftCardId"),
                cardName = getString("cardName"),
                settlementAmount = getDouble("settlementAmount"),
                isSettlement = getBoolean("isSettlement") ?: false
            )

            SharedTransaction(
                transaction = transaction,
                deviceId = deviceId,
                isMine = deviceId == myDeviceId
            )
        } catch (e: Exception) {
            null
        }
    }
}
