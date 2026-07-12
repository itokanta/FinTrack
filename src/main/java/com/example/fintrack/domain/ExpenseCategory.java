package com.example.fintrack.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 支出のカテゴリを表すドメインモデル。
 *
 * @author kanta ito
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseCategory {
    
    private Long id;
    private Long userId;
    private String categoryName;
    private LocalDateTime createdAt;
}
