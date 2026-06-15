package com.woojin.paymanagement.presentation.sharedroom

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woojin.paymanagement.domain.model.SharedRoom
import com.woojin.paymanagement.domain.repository.SharedModeManager
import com.woojin.paymanagement.domain.repository.SharedRoomRepository
import kotlinx.coroutines.launch

data class SharedRoomUiState(
    val isLoading: Boolean = false,
    val currentRoom: SharedRoom? = null,
    val nicknameInput: String = "",
    val joinCodeInput: String = "",
    val errorMessage: String? = null,
    val isJoining: Boolean = false
)

class SharedRoomViewModel(
    private val sharedRoomRepository: SharedRoomRepository
) : ViewModel() {

    var uiState by mutableStateOf(SharedRoomUiState())
        private set

    init {
        loadCurrentRoom()
    }

    private fun loadCurrentRoom() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true)
            val room = runCatching { sharedRoomRepository.getCurrentRoom() }.getOrNull()
            uiState = uiState.copy(isLoading = false, currentRoom = room)
        }
    }

    fun createRoom() {
        val nickname = uiState.nicknameInput.trim()
        if (nickname.isEmpty()) return

        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null)
            val room = runCatching { sharedRoomRepository.createRoom(nickname) }.getOrElse {
                uiState = uiState.copy(isLoading = false, errorMessage = it.message)
                return@launch
            }
            uiState = uiState.copy(isLoading = false, currentRoom = room)
        }
    }

    fun updateNickname(name: String) {
        uiState = uiState.copy(nicknameInput = name)
    }

    fun updateJoinCode(code: String) {
        uiState = uiState.copy(joinCodeInput = code.uppercase().take(6))
    }

    fun joinRoom() {
        val code = uiState.joinCodeInput.trim()
        val nickname = uiState.nicknameInput.trim()
        if (code.length != 6 || nickname.isEmpty()) return

        viewModelScope.launch {
            uiState = uiState.copy(isJoining = true, errorMessage = null)
            val room = runCatching { sharedRoomRepository.joinRoom(code, nickname) }.getOrElse {
                uiState = uiState.copy(isJoining = false, errorMessage = it.message)
                return@launch
            }
            if (room == null) {
                uiState = uiState.copy(isJoining = false, errorMessage = "존재하지 않는 방 코드입니다.")
            } else {
                uiState = uiState.copy(isJoining = false, currentRoom = room)
            }
        }
    }

    fun leaveRoom() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true)
            runCatching { sharedRoomRepository.leaveRoom() }
            // SharedModeManager 초기화
            SharedModeManager.sharedRoomId = null
            SharedModeManager.isSharedMode = false
            SharedModeManager.cachedSharedTransactions = emptyList()
            uiState = uiState.copy(isLoading = false, currentRoom = null, joinCodeInput = "")
        }
    }

    fun clearError() {
        uiState = uiState.copy(errorMessage = null)
    }

    fun getDeviceId(): String = sharedRoomRepository.getDeviceId()
}
