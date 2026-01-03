package com.example.fintrack.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncomeSplitPattern {
    
    private Long id;
    private Long assetCategoryId;
    private BigDecimal splitRatio;
    private String patternName;
    private LocalDateTime createdAt;
    
    // JOIN時に使用
    private AssetCategory assetCategory;
}
