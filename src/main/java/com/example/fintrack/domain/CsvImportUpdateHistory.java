package com.example.fintrack.domain;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CsvImportUpdateHistory {
  private Long id;
  private Long userId;
  private Long csvImportHistoryId;
  private Long bankTransactionId;
  private Long cardTransactionId;
  private CsvImportUpdateField targetField;
  private String valueBefore;
  private String valueAfter;
  private LocalDateTime createdAt;
}
