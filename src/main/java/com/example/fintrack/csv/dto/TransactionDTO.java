package com.example.fintrack.csv.dto;

import com.example.fintrack.domain.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * CSV 取込直後の中間取引データを表す DTO。
 *
 * @author kanta ito
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDTO {
    private LocalDate transactionDate;
    private Long amount;
    private String description;
    private TransactionType transactionType;
    private String paymentMonth;
}

