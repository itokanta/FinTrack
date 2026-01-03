package com.example.fintrack.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AutoCategoryRule {
    
    private Long id;
    private Long userId;
    private Long assetCategoryId;
    private String descriptionPattern;
    private TransactionType transactionType;
    private LocalDateTime createdAt;
    
    // JOIN時に使用
    private AssetCategory assetCategory;
}
