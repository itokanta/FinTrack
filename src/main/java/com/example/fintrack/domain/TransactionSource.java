package com.example.fintrack.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TransactionSource {
  CREDIT("クレジットカード"),
  DEBIT("デビットカード"),
  BANK("銀行"),
  DISCREPANCY("差異取引");

  private final String sourceName;
}
