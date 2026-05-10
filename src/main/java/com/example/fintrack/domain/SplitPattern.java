package com.example.fintrack.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 収支の分割パターンを管理するドメインモデル
 * (旧: IncomeSplitPattern)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SplitPattern {
    
    private Long id;                    // プライマリキー（SERIAL）
    private Long patternId;             // パターンID（複数レコードで同じ値を持つ）
    private Long userId;
    private TransactionType transactionType;
    private String splitType;           // 'RATIO' or 'AMOUNT'
    private Long assetCategoryId;       // 資産カテゴリID（収入・支出共通）
    private BigDecimal splitRatio;      // 割合指定の場合に使用（例: 50.00 for 50%）
    private Long splitAmount;           // 金額指定の場合に使用
    private String patternName;
    private LocalDateTime createdAt;
    
    // JOIN時に使用
    private AssetCategory assetCategory;
}

