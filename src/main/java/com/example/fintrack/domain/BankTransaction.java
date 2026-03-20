package com.example.fintrack.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BankTransaction {
    
    private Long id;
    private String description;
    private Long amount;
    private TransactionType transactionType;
    private LocalDate transactionDate;
    private String fingerprint;
    private Long userId;
    private Long assetCategoryId;
    private Long expenseCategoryId;
    private Long incomeSplitPatternId;  // 後方互換性のため残す（非推奨）
    private Long expenseSplitPatternId; // 後方互換性のため残す（非推奨）
    private Long splitPatternId;        // 統合版（推奨）
    private Boolean isCardPayment; // カード引き落としフラグ
    private String transactionSource; // BANK
    private LocalDateTime createdAt;
    private Long csvImportHistoryId;
}
