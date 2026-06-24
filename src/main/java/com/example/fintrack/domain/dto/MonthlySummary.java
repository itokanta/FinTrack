package com.example.fintrack.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MonthlySummary {
  private Long incomeAmount;
  private Long expenseAmount;
  private Long totalAmount;
}
