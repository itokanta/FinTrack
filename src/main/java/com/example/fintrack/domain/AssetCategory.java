package com.example.fintrack.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 資産のカテゴリ（口座・現金など）を表すドメインモデル。
 *
 * @author kanta ito
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssetCategory {
    
    private Long id;
    private Long userId;
    private String categoryName;
    private LocalDateTime createdAt;
}
