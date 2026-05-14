package com.example.fintrack.domain.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatternSummary {
  private Long id;
  private String name;
  private String splitType; // 'RATIO' or 'AMOUNT'
  private List<PatternDetail> details;
}
