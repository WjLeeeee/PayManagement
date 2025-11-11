package com.woojin.paymanagement.domain.model

/**
 * 백업/복원 데이터 타입
 */
enum class BackupType(val displayName: String, val fileName: String) {
    ALL("전체", "paymanagement_backup_all"),
    CATEGORIES("카테고리", "paymanagement_backup_categories"),
    BUDGET("예산 설정", "paymanagement_backup_budget"),
    TRANSACTIONS("거래 내역", "paymanagement_backup_transactions"),
    CARDS("잔액권/상품권", "paymanagement_backup_cards")
}
