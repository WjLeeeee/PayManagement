package com.woojin.paymanagement.data

data class Category(
    val id: String,
    val name: String,
    val emoji: String,
    val type: TransactionType,
    val isActive: Boolean = true,
    val sortOrder: Int = 0
)
