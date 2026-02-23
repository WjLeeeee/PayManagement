package com.woojin.paymanagement.android.config

import android.util.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * 업데이트 유형
 */
enum class UpdateType {
    NONE,           // 업데이트 불필요
    OPTIONAL,       // 선택적 업데이트 (취소 가능)
    FORCE           // 강제 업데이트 (취소 불가)
}

/**
 * Firebase Remote Config 관리 클래스
 */
class RemoteConfigManager {

    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

    // Fetch 완료 상태
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    init {
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(0) // 개발 중: 매번 새로고침 (나중에 3600으로 변경)
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)

        // 기본값 설정 (네트워크 없을 때 사용)
        remoteConfig.setDefaultsAsync(DEFAULT_VALUES)
    }

    /**
     * 서버에서 최신 설정을 가져와 활성화
     * 앱 시작 시 호출하세요
     */
    suspend fun fetchAndActivate(): Boolean {
        return try {
            val result = remoteConfig.fetchAndActivate().await()
            _isReady.value = true
            Log.d(TAG, "Remote Config fetch success. force_update_version=${getLong(KEY_FORCE_UPDATE_VERSION)}, optional_update_version=${getLong(KEY_OPTIONAL_UPDATE_VERSION)}")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Remote Config fetch failed", e)
            _isReady.value = true // 실패해도 기본값 사용 가능
            false
        }
    }

    /**
     * 현재 앱의 versionCode를 기준으로 업데이트 유형 판별
     * 우선순위: 강제 > 선택 > 없음
     */
    fun checkUpdateType(currentVersionCode: Int): UpdateType {
        val forceVersion = getLong(KEY_FORCE_UPDATE_VERSION)
        val optionalVersion = getLong(KEY_OPTIONAL_UPDATE_VERSION)

        Log.d(TAG, "checkUpdateType: current=$currentVersionCode, force=$forceVersion, optional=$optionalVersion")

        return when {
            forceVersion > 0 && currentVersionCode < forceVersion -> UpdateType.FORCE
            optionalVersion > 0 && currentVersionCode < optionalVersion -> UpdateType.OPTIONAL
            else -> UpdateType.NONE
        }
    }

    /**
     * String 값 가져오기
     */
    fun getString(key: String): String {
        return remoteConfig.getString(key)
    }

    /**
     * Boolean 값 가져오기
     */
    fun getBoolean(key: String): Boolean {
        return remoteConfig.getBoolean(key)
    }

    /**
     * Long 값 가져오기
     */
    fun getLong(key: String): Long {
        return remoteConfig.getLong(key)
    }

    /**
     * Double 값 가져오기
     */
    fun getDouble(key: String): Double {
        return remoteConfig.getDouble(key)
    }

    companion object {
        private const val TAG = "RemoteConfigManager"

        // Remote Config 키 상수
        const val KEY_FEATURE_TYPE = "feature_type"
        const val KEY_FORCE_UPDATE_VERSION = "force_update_version"
        const val KEY_OPTIONAL_UPDATE_VERSION = "optional_update_version"

        // 기본값 (Firebase Console에서 값을 설정하기 전까지 사용)
        // 0 = 업데이트 체크 안 함
        private val DEFAULT_VALUES = mapOf(
            KEY_FEATURE_TYPE to "default",
            KEY_FORCE_UPDATE_VERSION to 0L,
            KEY_OPTIONAL_UPDATE_VERSION to 0L
        )
    }
}
