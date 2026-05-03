package com.example.fintrack.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiscrepancyTransaction {
    private Long id;
    private String description;
    private Long amount;
    private TransactionType transactionType;
    private LocalDate transactionDate;
    private Long userId;
    private Long assetCategoryId;
    private Long expenseCategoryId;
    private Long splitPatternId;
    private Long cardPaymentDiscrepancyId;
    private LocalDateTime createdAt;
    private SplitMethod splitMethod;
}
