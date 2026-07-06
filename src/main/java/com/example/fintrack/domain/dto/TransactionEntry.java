package com.example.fintrack.domain.dto;

import java.time.LocalDate;

import com.example.fintrack.domain.SplitMethod;
import com.example.fintrack.domain.TransactionType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 一覧表示用の取引 1 件分のデータを表す DTO。
 * 銀行・カード・差異取引のいずれかを ID で参照する。
 *
 * @author kanta ito
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionEntry {
  private Long bankTransactionId;
  private Long cardTransactionId;
  private Long discrepancyTransactionId;
  private String description;
  private Long amount;
  private TransactionType transactionType;
  private LocalDate transactionDate;
  private Long assetCategoryId;
  private Long expenseCategoryId;
  private SplitMethod splitMethod;
  private Long splitPatternId;
  private String transactionSource;
}