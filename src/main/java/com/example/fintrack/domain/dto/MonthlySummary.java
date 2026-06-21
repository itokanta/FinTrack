package com.example.fintrack.domain.dto;

import lombok.Data;

@Data
public class MonthlySummary {
  private Long incomeAmount;
  private Long expenseAmount;
  private Long totalAmount;
}
