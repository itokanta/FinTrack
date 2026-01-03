package com.example.fintrack.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SplitIncome {
    
    private Long id;
    private Long assetCategoryId;
    private Long splitPatternId;
    private Long amount;
    private Long bankTransactionId;
    private Long cardTransactionId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // JOIN時に使用
    private AssetCategory assetCategory;
    private BankTransaction bankTransaction;
    private CardTransaction cardTransaction;
}
