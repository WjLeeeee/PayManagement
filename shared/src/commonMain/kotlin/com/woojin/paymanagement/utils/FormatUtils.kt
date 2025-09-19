package com.woojin.paymanagement.utils

/**
 * 숫자를 천단위 콤마가 포함된 문자열로 포맷팅
 */
fun formatWithCommas(number: Long): String {
    return number.toString().reversed().chunked(3).joinToString(",").reversed()
}

/**
 * 문자열에서 콤마를 제거하여 숫자만 추출
 */
fun removeCommas(text: String): String {
    return text.replace(",", "")
}

/**
 * 콤마가 포함된 문자열을 Double로 변환
 */
fun parseAmountToDouble(amountText: String): Double {
    return removeCommas(amountText).toDoubleOrNull() ?: 0.0
}

/**
 * 콤마가 포함된 문자열을 Long으로 변환
 */
fun parseAmountToLong(amountText: String): Long {
    return removeCommas(amountText).toLongOrNull() ?: 0L
}

/**
 * 숫자를 콤마 포맷으로 변환하고 "원" 단위 추가
 */
fun formatCurrency(amount: Double): String {
    return "${formatWithCommas(amount.toLong())}원"
}