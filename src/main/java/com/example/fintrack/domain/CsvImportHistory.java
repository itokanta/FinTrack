package com.example.fintrack.domain;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CSV インポートの実行履歴を表すドメインモデル。
 *
 * @author kanta ito
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CsvImportHistory {
  private Long id;
  private Long userId;
  private String importType;
  private String csvName;
  private LocalDateTime importDate;
  private Long recordCount;
}
