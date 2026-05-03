package com.example.fintrack.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardTransaction {
    
    private Long id;
    private String description;
    private Long amount;
    private TransactionType transactionType;
    private LocalDate transactionDate;
    private String fingerprint;
    private Long userId;
    private Long assetCategoryId;
    private Long expenseCategoryId;
    private Long splitPatternId;
    private String transactionSource; // CARD
    private String cardType; // DEBIT or CREDIT
    private String paymentMonth; // YYYY-MM形式（例: "2025-01"）
    private Boolean isPaid; // 引き落とし済みフラグ（クレジットのみ、デビットはNULL）
    private LocalDateTime createdAt;
    private Long csvImportHistoryId;
    private Long updateCount;
    private LocalDateTime updatedAt;
    private SplitMethod splitMethod;
}
