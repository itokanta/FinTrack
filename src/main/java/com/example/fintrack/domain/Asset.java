package com.example.fintrack.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 資産カテゴリごとの残高を表すドメインモデル。
 *
 * @author kanta ito
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Asset {
    
    private Long id;
    private Long userId;
    private Long assetCategoryId;
    private Long amount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // リレーション用（JOIN時に使用）
    private AssetCategory assetCategory;
}
