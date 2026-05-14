package com.example.fintrack.domain.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatternDetail {
  private String categoryName;
  private BigDecimal splitRatio;
  private Long splitAmount;
  private String splitType;
}
