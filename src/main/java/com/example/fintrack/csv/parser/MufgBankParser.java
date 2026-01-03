package com.example.fintrack.csv.parser;

import com.example.fintrack.csv.dto.TransactionDTO;
import com.example.fintrack.domain.TransactionType;
import org.apache.commons.csv.CSVRecord;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 三菱UFJ銀行のCSVパーサー
 * 
 * CSVフォーマット（Shift-JIS）:
 * "日付","摘要","摘要内容","支払い金額","預かり金額","差引残高","メモ","未資金化区分","入出金区分"
 * "2025/12/7","振込入金","ヒシヤ　ツノハシ",,"192,800","302,982","","","振込支払済"
 */
public class MufgBankParser implements CsvParser {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/M/d");
    
    @Override
    public TransactionDTO parse(CSVRecord record) {
        TransactionDTO dto = new TransactionDTO();
        
        // 日付（yyyy/M/d形式）
        String dateStr = record.get(0);
        dto.setTransactionDate(LocalDate.parse(dateStr, DATE_FORMATTER));
        
        // 支払い金額と預かり金額で入出金を判定
        String paymentStr = record.get(3).replace(",", "").replace("\"", "").trim();
        String depositStr = record.get(4).replace(",", "").replace("\"", "").trim();
        
        if (!paymentStr.isEmpty()) {
            // 支払い（支出）
            dto.setTransactionType(TransactionType.EXPENSE);
            dto.setAmount(Long.parseLong(paymentStr));
        } else if (!depositStr.isEmpty()) {
            // 預かり（収入）
            dto.setTransactionType(TransactionType.INCOME);
            dto.setAmount(Long.parseLong(depositStr));
        } else {
            // 金額が両方空の場合はスキップ
            throw new IllegalArgumentException("金額が設定されていません");
        }
        
        // 摘要と摘要内容を結合
        String summary = record.get(1);
        String detail = record.get(2);
        if (!detail.isEmpty()) {
            dto.setDescription(summary + " " + detail);
        } else {
            dto.setDescription(summary);
        }
        
        return dto;
    }
}

