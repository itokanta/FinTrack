package com.example.fintrack.csv.parser;

import com.example.fintrack.csv.dto.TransactionDTO;
import com.example.fintrack.domain.TransactionType;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 汎用クレジットカードCSVパーサー
 * フォーマット: 日付,摘要,金額,区分
 */
@Component
public class GenericCreditCardParser implements CsvParser {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    
    @Override
    public TransactionDTO parse(CSVRecord record) {
        TransactionDTO dto = new TransactionDTO();
        
        // 日付（0列目）
        String dateStr = record.get(0).trim();
        dto.setTransactionDate(LocalDate.parse(dateStr, DATE_FORMATTER));
        
        // 摘要（1列目）
        dto.setDescription(record.get(1).trim());
        
        // 金額（2列目）
        String amountStr = record.get(2).trim().replace(",", "").replace("円", "");
        dto.setAmount(Long.parseLong(amountStr));
        
        // 入出金区分（3列目）
        String typeStr = record.get(3).trim();
        if ("収入".equals(typeStr) || "入金".equals(typeStr)) {
            dto.setTransactionType(TransactionType.INCOME);
        } else {
            dto.setTransactionType(TransactionType.EXPENSE);
        }
        
        return dto;
    }
}

