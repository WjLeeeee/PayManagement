package com.woojin.paymanagement.android

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.woojin.paymanagement.App
import com.woojin.paymanagement.database.DatabaseDriverFactory
import com.woojin.paymanagement.utils.PreferencesManager

/**
 * commonMain의 Composable들을 Preview하기 위한 래퍼 함수들
 * Android Studio에서 UI를 미리 볼 수 있도록 합니다.
 */

@Preview(name = "Full App", showBackground = true)
@Composable
fun FullAppPreview() {
    val context = LocalContext.current
    MyApplicationTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White
        ) {
            App(DatabaseDriverFactory(context), PreferencesManager(context))
        }
    }
}

@Preview(name = "App - Phone", device = "spec:width=360dp,height=640dp,dpi=480", showBackground = true)
@Composable
fun AppPhonePreview() {
    val context = LocalContext.current
    MyApplicationTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White
        ) {
            App(DatabaseDriverFactory(context), PreferencesManager(context))
        }
    }
}