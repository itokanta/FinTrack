package com.example.fintrack.csv.parser;

import com.example.fintrack.csv.dto.TransactionDTO;
import com.example.fintrack.domain.TransactionType;
import org.apache.commons.csv.CSVRecord;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * みずほ銀行のCSVパーサー
 * 
 * CSVフォーマット（Shift-JIS）:
 * 照会開始日,照会終了日から12行目以降がデータ
 * 取引通番,日付,引落金額,預入金額,残高,摘要,メモ,取引店番号,取引区分,科目区分
 * 20251229000020,2025.12.29,3634,,247422,セテイ-ペイデイ-,,,
 */
public class MizuhoBankParser implements CsvParser {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    
    @Override
    public TransactionDTO parse(CSVRecord record) {
        // レコードの列数チェック（フッター行などをスキップ）
        if (record.size() < 6) {
            return null; // 列数が足りない場合はスキップ
        }
        
        TransactionDTO dto = new TransactionDTO();
        
        // 日付（yyyy.MM.dd形式）
        String dateStr = record.get(1);
        dto.setTransactionDate(LocalDate.parse(dateStr, DATE_FORMATTER));
        
        // 引落金額と預入金額で入出金を判定
        String withdrawalStr = record.get(2).replace(",", "");
        String depositStr = record.get(3).replace(",", "");
        
        if (!withdrawalStr.isEmpty()) {
            // 引落（支出）
            dto.setTransactionType(TransactionType.EXPENSE);
            dto.setAmount(Long.parseLong(withdrawalStr));
        } else if (!depositStr.isEmpty()) {
            // 預入（収入）
            dto.setTransactionType(TransactionType.INCOME);
            dto.setAmount(Long.parseLong(depositStr));
        } else {
            // 金額が両方空の場合はスキップ（エラーレコード）
            throw new IllegalArgumentException("金額が設定されていません");
        }
        
        // 摘要
        dto.setDescription(record.get(5));
        
        return dto;
    }
}

