package com.example.fintrack.csv.parser;

import com.example.fintrack.csv.dto.TransactionDTO;
import com.example.fintrack.domain.TransactionType;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 楽天銀行CSVパーサー
 * 想定フォーマット: 取引日,入出金(円),残高(円),摘要
 */
@Component
public class RakutenBankParser implements CsvParser {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    
    @Override
    public TransactionDTO parse(CSVRecord record) {
        TransactionDTO dto = new TransactionDTO();
        
        // 取引日（0列目）
        String dateStr = record.get(0).trim();
        dto.setTransactionDate(LocalDate.parse(dateStr, DATE_FORMATTER));
        
        // 入出金額（1列目）
        String amountStr = record.get(1).trim().replace(",", "").replace("円", "");
        Long amount = Long.parseLong(amountStr);
        dto.setAmount(Math.abs(amount));
        
        // 入出金区分（金額の正負で判定）
        if (amount >= 0) {
            dto.setTransactionType(TransactionType.INCOME);
        } else {
            dto.setTransactionType(TransactionType.EXPENSE);
        }
        
        // 摘要（3列目）
        dto.setDescription(record.get(3).trim());
        
        return dto;
    }
}

