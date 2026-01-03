package com.example.fintrack.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Asset {
    
    private Long id;
    private Long assetCategoryId;
    private Long amount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // リレーション用（JOIN時に使用）
    private AssetCategory assetCategory;
}
