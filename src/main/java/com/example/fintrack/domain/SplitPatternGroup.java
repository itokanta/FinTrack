package com.example.fintrack.domain;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分割パターンのグループ（名前・種別）を表すドメインモデル。
 * 詳細行は {@link SplitPattern} のリストとして持つ。
 *
 * @author kanta ito
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SplitPatternGroup {
  private Long id;
  private String patternName;
  private Long userId;
  private TransactionType transactionType;
  private String splitType;
  private LocalDateTime createdAt;
  private LocalDateTime deletedAt;

  //パターン詳細取得用
  private List<SplitPattern> splitPatternList;
}
