package com.example.fintrack.domain.dto;

import java.time.LocalDate;

import com.example.fintrack.domain.SplitMethod;
import com.example.fintrack.domain.TransactionType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionEntry {
  private String description;
  private Long amount;
  private TransactionType transactionType;
  private LocalDate transactionDate;
  private Long assetCategoryId;
  private Long expenseCategoryId;
  private SplitMethod splitMethod;
  private String transactionSource;
}