package com.example.fintrack.domain.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionsByType {
  private List<TransactionEntry> income;
  private List<TransactionEntry> expense;
}
