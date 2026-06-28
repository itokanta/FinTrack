package com.example.fintrack.domain.dto;

import com.example.fintrack.domain.AssetCategory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MonthlySummary {
  private AssetCategory assetCategory;
  private Long incomeAmount;
  private Long expenseAmount;
  private Long totalAmount;
}
