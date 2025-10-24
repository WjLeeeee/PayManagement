package com.woojin.paymanagement.utils

/**
 * 플랫폼별 파일 입출력을 처리하는 인터페이스
 */
expect class FileHandler() {
    /**
     * 저장할 데이터를 설정합니다
     */
    fun setSaveData(fileName: String, jsonContent: String, onSuccess: () -> Unit, onError: (String) -> Unit)

    /**
     * 불러오기 콜백을 설정합니다
     */
    fun setLoadCallbacks(onSuccess: (String) -> Unit, onError: (String) -> Unit)

    /**
     * 저장할 데이터를 가져옵니다
     */
    fun getPendingSaveData(): Pair<String, String>?

    /**
     * 저장 성공 시 호출
     */
    fun onSaveSuccess()

    /**
     * 저장 실패 시 호출
     */
    fun onSaveError(error: String)

    /**
     * 로드 성공 시 호출
     */
    fun onLoadSuccess(jsonContent: String)

    /**
     * 로드 실패 시 호출
     */
    fun onLoadError(error: String)
}
