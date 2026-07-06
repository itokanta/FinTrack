package com.example.fintrack.domain.dto;

import com.example.fintrack.domain.AssetCategory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 月次サマリー（資産カテゴリ別の収入・支出・合計）を表す DTO。
 *
 * @author kanta ito
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MonthlySummary {
  private AssetCategory assetCategory;
  private Long incomeAmount;
  private Long expenseAmount;
  private Long totalAmount;
}
