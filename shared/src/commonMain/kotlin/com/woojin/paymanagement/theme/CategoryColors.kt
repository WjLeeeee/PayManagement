package com.woojin.paymanagement.theme

import androidx.compose.ui.graphics.Color

object CategoryColors {

    // 카테고리별 색상 정의
    private val categoryColors = mapOf(
        // 수입 카테고리
        "급여" to Color(0xFF1E88E5),      // 신뢰감 있는 파란색 (직장/안정감)
        "식비" to Color(0xFF4CAF50),      // 신선한 초록색 (음식/건강)
        "당근" to Color(0xFFFF7043),      // 당근색 (당근마켓 브랜드 컬러)
        "K-패스 환급" to Color(0xFF00ACC1), // 시원한 청록색 (교통/환급)
        "투자수익" to Color(0xFF388E3C),   // 진한 초록색 (돈/수익)
        "기타수입" to Color(0xFF9C27B0),   // 보라색 (기타/특별함)

        // 지출 카테고리
        "데이트" to Color(0xFFEC407A),     // 분홍색 (로맨스/특별함)
        "생활비" to Color(0xFF78909C),     // 차분한 청회색 (일상/생활 전반)
        "생활용품" to Color(0xFF607D8B),   // 회색 (일상용품)
        "쇼핑" to Color(0xFFE91E63),       // 핑크색 (쇼핑/소비)
        "문화생활" to Color(0xFF5E35B1),    // 깊은 보라색(예술/창의성)
        "경조사" to Color(0xFFFFD54F),     // 따듯한 노란색 (축하/마음)
        "자기계발" to Color(0xFF00ACC1),    // 활기찬 청록색, (건강/활력)
        "공과금" to Color(0xFFFBC02D),     // 밝은 노란색 (공공/에너지/필수)
        "대출이자" to Color(0xFFD32F2F),   // 경고적인 빨간색 (부담/의무지출)
        "모임통장" to Color(0xFF26A69A),   // 친근한 청록색 (사회적/공동체)
        "교통비" to Color(0xFF2196F3),     // 파란색 (교통/이동)
        "적금" to Color(0xFF43A047),       // 초록색 (저축/미래)
        "투자" to Color(0xFF7B1FA2),       // 진한 보라색 (투자/장기)
        "정기결제" to Color(0xFFFF9800),   // 주황색 (정기/자동화)
        "기타지출" to Color(0xFF795548)    // 갈색 (기타/일반)
    )

    // 기본 색상 팔레트 (카테고리에 정의되지 않은 경우 사용)
    private val defaultColors = listOf(
        Color(0xFF2196F3), Color(0xFF4CAF50), Color(0xFF9C27B0),
        Color(0xFF00BCD4), Color(0xFF3F51B5), Color(0xFF009688),
        Color(0xFF607D8B), Color(0xFFFF5722), Color(0xFFE91E63),
        Color(0xFF795548), Color(0xFFFF9800), Color(0xFF8BC34A),
        Color(0xFFFFEB3B), Color(0xFF673AB7), Color(0xFF9E9E9E)
    )

    /**
     * 카테고리에 해당하는 색상을 반환합니다.
     * 정의되지 않은 카테고리의 경우 기본 색상 팔레트에서 순환하여 반환합니다.
     */
    fun getColor(category: String, colorIndex: Int = 0): Color {
        return categoryColors[category] ?: defaultColors[colorIndex % defaultColors.size]
    }

    /**
     * 모든 정의된 카테고리 색상을 반환합니다.
     */
    fun getAllCategoryColors(): Map<String, Color> {
        return categoryColors
    }

    /**
     * 기본 색상 팔레트를 반환합니다.
     */
    fun getDefaultColors(): List<Color> {
        return defaultColors
    }

    /**
     * 새로운 카테고리의 색상을 동적으로 할당합니다.
     */
    fun assignColorForNewCategory(existingCategories: List<String>): Color {
        val usedColors = existingCategories.mapNotNull { categoryColors[it] }.toSet()
        val availableColors = defaultColors.filter { it !in usedColors }
        return availableColors.firstOrNull() ?: defaultColors.random()
    }
}