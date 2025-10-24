package com.woojin.paymanagement.utils

/**
 * Android 플랫폼에서 파일 입출력을 처리하는 구현체
 */
actual class FileHandler actual constructor() {
    private var pendingSaveData: Pair<String, String>? = null // (fileName, jsonContent)
    private var pendingSaveCallbacks: Pair<() -> Unit, (String) -> Unit>? = null // (onSuccess, onError)
    private var pendingLoadCallbacks: Pair<(String) -> Unit, (String) -> Unit>? = null // (onSuccess, onError)

    actual fun setSaveData(
        fileName: String,
        jsonContent: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        pendingSaveData = fileName to jsonContent
        pendingSaveCallbacks = onSuccess to onError
    }

    actual fun setLoadCallbacks(
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        pendingLoadCallbacks = onSuccess to onError
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
