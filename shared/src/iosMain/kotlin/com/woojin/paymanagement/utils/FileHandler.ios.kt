package com.woojin.paymanagement.utils

/**
 * iOS 플랫폼에서 파일 입출력을 처리하는 구현체
 * TODO: iOS 파일 선택/저장 구현 필요
 */
actual class FileHandler actual constructor() {
    private var pendingSaveData: Pair<String, String>? = null
    private var pendingSaveCallbacks: Pair<() -> Unit, (String) -> Unit>? = null
    private var pendingLoadCallbacks: Pair<(String) -> Unit, (String) -> Unit>? = null

    actual fun setSaveData(
        fileName: String,
        jsonContent: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        pendingSaveData = fileName to jsonContent
        pendingSaveCallbacks = onSuccess to onError
        // iOS 구현 예정
        onError("iOS에서는 아직 지원하지 않습니다")
    }

    actual fun setLoadCallbacks(
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        pendingLoadCallbacks = onSuccess to onError
        // iOS 구현 예정
        onError("iOS에서는 아직 지원하지 않습니다")
    }

    actual fun getPendingSaveData(): Pair<String, String>? = pendingSaveData

    actual fun onSaveSuccess() {
        pendingSaveCallbacks?.first?.invoke()
        clearSavePendingData()
    }

    actual fun onSaveError(error: String) {
        pendingSaveCallbacks?.second?.invoke(error)
        clearSavePendingData()
    }

    actual fun onLoadSuccess(jsonContent: String) {
        pendingLoadCallbacks?.first?.invoke(jsonContent)
        clearLoadPendingData()
    }

    actual fun onLoadError(error: String) {
        pendingLoadCallbacks?.second?.invoke(error)
        clearLoadPendingData()
    }

    private fun clearSavePendingData() {
        pendingSaveData = null
        pendingSaveCallbacks = null
    }

    private fun clearLoadPendingData() {
        pendingLoadCallbacks = null
    }
}
