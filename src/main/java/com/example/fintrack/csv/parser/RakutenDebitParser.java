package com.example.fintrack.csv.parser;

import com.example.fintrack.csv.dto.TransactionDTO;
import com.example.fintrack.domain.TransactionType;
import org.apache.commons.csv.CSVRecord;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 楽天デビットカードの CSV パーサー。
 *
 * <p>CSV フォーマット（Shift-JIS）:
 * <pre>
 * "ご利用日","ご利用先","ご利用金額(円)","現地通貨額","通貨略号","レート","利用区分","取引番号","認証番号","お支払い金額(円)","ポイント利用数"
 * "20251201","ヤオコーナガレヤマオオタカノモ","1625","","","","国内","24721485336-181200007890","113457","1625","0"
 * </pre>
 *
 * @author kanta ito
 */
public class RakutenDebitParser implements CsvParser {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    
    /**
     * 楽天デビットの 1 行を {@link TransactionDTO} に変換する。
     *
     * @param record パース済みの CSV 行
     * @return 変換後の取引 DTO
     */
    @Override
    public TransactionDTO parse(CSVRecord record) {
        TransactionDTO dto = new TransactionDTO();
        
        // ご利用日（yyyyMMdd形式）
        String dateStr = record.get(0);
        dto.setTransactionDate(LocalDate.parse(dateStr, DATE_FORMATTER));
        
        // ご利用先
        dto.setDescription(record.get(1));
        
        // ご利用金額（デビットカードは常に支出）
        String amountStr = record.get(2).replace(",", "");
        dto.setAmount(Long.parseLong(amountStr));
        dto.setTransactionType(TransactionType.EXPENSE);
        
        return dto;
    }
}

