package com.example.fintrack.domain.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 収入・支出に分けた取引一覧をまとめる DTO。
 *
 * @author kanta ito
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionsByType {
  private List<TransactionEntry> income;
  private List<TransactionEntry> expense;
}
