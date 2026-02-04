package com.woojin.paymanagement.android.config

import android.util.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

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
            Log.d(TAG, "Remote Config fetch success. feature_type = ${getString(KEY_FEATURE_TYPE)}")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Remote Config fetch failed", e)
            _isReady.value = true // 실패해도 기본값 사용 가능
            false
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

        // 기본값 (Firebase Console에서 값을 설정하기 전까지 사용)
        private val DEFAULT_VALUES = mapOf(
            KEY_FEATURE_TYPE to "default"
        )
    }
}
