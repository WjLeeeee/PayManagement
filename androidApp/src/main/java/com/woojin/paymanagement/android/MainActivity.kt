package com.woojin.paymanagement.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.woojin.paymanagement.App
import com.woojin.paymanagement.database.DatabaseDriverFactory
import com.woojin.paymanagement.utils.PreferencesManager
import com.woojin.paymanagement.utils.NotificationPermissionChecker

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 상태바 스타일을 light로 설정 (아이콘 어둡게, 배경 흰색)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                scrim = Color.White.toArgb(),
                darkScrim = Color.White.toArgb()
            ),
            navigationBarStyle = SystemBarStyle.light(
                scrim = Color.White.toArgb(),
                darkScrim = Color.White.toArgb()
            )
        )
        
        setContent {
            MyApplicationTheme {
                StatusBarOverlayScreen()
            }
        }
    }
}

@Composable
fun StatusBarOverlayScreen() {
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column {
                // 상태바 크기만큼 패딩 추가
                Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                
                // 실제 앱 콘텐츠
                App(
                    databaseDriverFactory = DatabaseDriverFactory(context = context),
                    preferencesManager = PreferencesManager(context = context),
                    notificationPermissionChecker = NotificationPermissionChecker(context = context)
                )
            }
        }
    }
}

