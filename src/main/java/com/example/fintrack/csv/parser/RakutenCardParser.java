package com.example.fintrack.csv.parser;

import com.example.fintrack.csv.dto.TransactionDTO;
import com.example.fintrack.domain.TransactionType;
import org.apache.commons.csv.CSVRecord;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 楽天カードのCSVパーサー
 * 
 * CSVフォーマット（UTF-8）:
 * "利用日","利用店名・商品名","利用者","支払方法","利用金額","支払手数料","支払総額","12月支払金額","1月繰越残高","新規サイン"
 * "2025/11/30","ﾔｵｺ-ﾅｶﾞﾚﾔﾏｵｵﾀｶﾉﾓ","本人","1回払い","2044","0","2044","2044","0","*"
 * 
 * 注意: 利用日が空欄の行（詳細情報行など）はスキップされます
 */
public class RakutenCardParser implements CsvParser {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    
    @Override
    public TransactionDTO parse(CSVRecord record) {
        // 利用日が空欄の場合はスキップ（詳細情報行など）
        String dateStr = record.get(0);
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        
        TransactionDTO dto = new TransactionDTO();
        
        // 利用日
        dto.setTransactionDate(LocalDate.parse(dateStr, DATE_FORMATTER));
        
        // 利用店名・商品名
        dto.setDescription(record.get(1));
        
        // 利用金額（クレジットカードは常に支出）
        String amountStr = record.get(4).replace(",", "");
        dto.setAmount(Long.parseLong(amountStr));
        dto.setTransactionType(TransactionType.EXPENSE);
        
        return dto;
    }
}

