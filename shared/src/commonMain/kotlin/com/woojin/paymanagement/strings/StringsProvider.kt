package com.woojin.paymanagement.strings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Supported languages in the app
 */
enum class Language(val code: String, val displayName: String) {
    KOREAN("ko", "한국어"),
    ENGLISH("en", "English");

    companion object {
        fun fromCode(code: String): Language {
            return entries.find { it.code == code } ?: KOREAN
        }
    }
}

/**
 * CompositionLocal for providing strings throughout the app
 */
val LocalStrings = staticCompositionLocalOf<AppStrings> { KoreanStrings }

/**
 * Get the appropriate AppStrings implementation for the given language
 */
fun getStringsForLanguage(language: Language): AppStrings {
    return when (language) {
        Language.KOREAN -> KoreanStrings
        Language.ENGLISH -> EnglishStrings
    }
}

/**
 * Composable function to provide strings based on the selected language
 */
@Composable
fun ProvideStrings(
    language: Language,
    content: @Composable () -> Unit
) {
    val strings = getStringsForLanguage(language)
    CompositionLocalProvider(LocalStrings provides strings) {
        content()
    }
}

/**
 * Composable function to provide strings based on language code
 */
@Composable
fun ProvideStrings(
    languageCode: String,
    content: @Composable () -> Unit
) {
    ProvideStrings(Language.fromCode(languageCode), content)
}
