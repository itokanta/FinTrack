package com.example.fintrack.csv.parser;

import com.example.fintrack.csv.dto.TransactionDTO;
import com.example.fintrack.domain.TransactionType;
import org.apache.commons.csv.CSVRecord;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 楽天銀行のCSVパーサー
 * 
 * CSVフォーマット（Shift-JIS）:
 * 取引日,入出金(円),残高(円),入出金内容
 * 20251201,-1625,23986,VISAデビット A0113457
 */
public class RakutenBankParser implements CsvParser {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    
    @Override
    public TransactionDTO parse(CSVRecord record) {
        // 摘要を取得
        String description = record.get(3);
        
        // VISAデビットの取引は除外（nullを返す）
        if (description != null && description.contains("VISAデビット")) {
            return null;
        }
        
        TransactionDTO dto = new TransactionDTO();
        
        // 取引日（yyyyMMdd形式）
        String dateStr = record.get(0);
        dto.setTransactionDate(LocalDate.parse(dateStr, DATE_FORMATTER));
        
        // 入出金額
        String amountStr = record.get(1).replace(",", "");
        long amount = Long.parseLong(amountStr);
        
        // 金額の正負で入出金を判定
        if (amount < 0) {
            dto.setTransactionType(TransactionType.EXPENSE);
            dto.setAmount(Math.abs(amount));
        } else {
            dto.setTransactionType(TransactionType.INCOME);
            dto.setAmount(amount);
        }
        
        // 摘要
        dto.setDescription(description);
        
        return dto;
    }
}
