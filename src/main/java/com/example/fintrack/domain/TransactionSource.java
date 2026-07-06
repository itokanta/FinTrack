package com.example.fintrack.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 取引の発生元（銀行・カード・差異など）を表す Enum。
 *
 * @author kanta ito
 */
@AllArgsConstructor
@Getter
public enum TransactionSource {
  CREDIT("クレジットカード"),
  DEBIT("デビットカード"),
  BANK("銀行"),
  DISCREPANCY("差異取引");

  private final String sourceName;
}
