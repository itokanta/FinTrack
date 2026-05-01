package com.example.fintrack.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 分割済みの収支額を管理するドメインモデル
 * (旧: SplitIncome)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SplitTransaction {
    
    private Long id;
    private String transactionType;     // 'INCOME' or 'EXPENSE'
    private Long assetCategoryId;       // 資産カテゴリID（収入・支出共通）
    private Long splitPatternId;        // パターン使用時のみ設定、それ以外はNULL
    private SplitMethod splitMethod;         // 'PATTERN', 'MANUAL', 'SINGLE'
    private Long amount;
    private Long bankTransactionId;
    private Long cardTransactionId;
    private Long discrepancyTransactionId; // 差異取引ID
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // JOIN時に使用
    private AssetCategory assetCategory;
    private BankTransaction bankTransaction;
    private CardTransaction cardTransaction;
}

