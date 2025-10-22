package com.woojin.paymanagement.android

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFF64B5F6), // 수입 색상 (밝은 파란색 - 다크 모드 시인성 향상)
            secondary = Color(0xFF03DAC5),
            tertiary = Color(0xFF3700B3),
            error = Color(0xFFEF5350), // 지출 색상 (밝은 빨간색)
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E),
            surfaceVariant = Color(0xFF2C2C2C),
            onSurface = Color.White,
            onSurfaceVariant = Color(0xFFB0B0B0),
            secondaryContainer = Color(0xFF1A2530),
            tertiaryContainer = Color(0xFF2D1A30),
            onSecondaryContainer = Color.White,
            onTertiaryContainer = Color.White
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF2196F3), // 수입 색상 (파란색)
            secondary = Color(0xFF03DAC5),
            tertiary = Color(0xFF3700B3),
            error = Color(0xFFF44336), // 지출 색상 (빨간색)
            background = Color.White,
            surface = Color.White,
            surfaceVariant = Color(0xFFF5F5F5),
            onSurface = Color.Black,
            onSurfaceVariant = Color(0xFF666666),
            secondaryContainer = Color(0xFFFFFEF7),
            tertiaryContainer = Color(0xFFFFFAFA),
            onSecondaryContainer = Color.Black,
            onTertiaryContainer = Color.Black
        )
    }
    val typography = Typography(
        bodyMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp
        )
    )
    val shapes = Shapes(
        small = RoundedCornerShape(4.dp),
        medium = RoundedCornerShape(4.dp),
        large = RoundedCornerShape(0.dp)
    )

    MaterialTheme(
        colorScheme = colors,
        typography = typography,
        shapes = shapes,
        content = content
    )
}
