package com.example.fintrack.domain.dto;

import com.example.fintrack.domain.TransactionType;

import lombok.Data;

@Data
public class MonthlySummary {
  private TransactionType transactionType;
  private Long amount;
}
