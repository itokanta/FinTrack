package com.example.fintrack.domain;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
