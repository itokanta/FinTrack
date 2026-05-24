package com.example.fintrack.csv.parser;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVRecord;

import com.example.fintrack.csv.dto.TransactionDTO;
import com.example.fintrack.domain.TransactionType;

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
    private static final Pattern PAYMENT_MONTH_PATTERN = Pattern.compile("(\\d+)月");
    
    @Override
    public TransactionDTO parse(CSVRecord record) {
        // 利用日が空欄の場合はスキップ（詳細情報行など）
        String dateStr = record.get(0);
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        
        TransactionDTO dto = new TransactionDTO();
        
        // 利用日
        LocalDate transactionDate = LocalDate.parse(dateStr, DATE_FORMATTER);
        dto.setTransactionDate(transactionDate);
        
        // 利用店名・商品名
        dto.setDescription(record.get(1));
        
        // 利用金額（クレジットカードは常に支出）
        String amountStr = record.get(4).replace(",", "");
        dto.setAmount(Long.parseLong(amountStr));
        dto.setTransactionType(TransactionType.EXPENSE);
        
        // 支払い月を設定（8列目: インデックス7）
        if (record.size() > 7) {
            String paymentMonthStr = record.get(7);
            if (paymentMonthStr != null && !paymentMonthStr.trim().isEmpty()) {
                String paymentMonth = convertPaymentMonth(paymentMonthStr, transactionDate);
                dto.setPaymentMonth(paymentMonth);
            }
        }

        return dto;
    }

    /**
     * 支払い月文字列を YYYY-MM 形式に変換
     * 
     * @param paymentMonthStr CSVの支払い月文字列（例: "2月", "3月"）
     * @param transactionDate 利用日
     * @return YYYY-MM形式の支払い月（例: "2026-02"）
     */
    private String convertPaymentMonth(String paymentMonthStr, LocalDate transactionDate) {
        Matcher matcher = PAYMENT_MONTH_PATTERN.matcher(paymentMonthStr);
        if (matcher.find()) {
            int paymentMonth = Integer.parseInt(matcher.group(1));
            int transactionMonth = transactionDate.getMonthValue();
            int year = transactionDate.getYear();

            // 支払い月が利用月より小さい場合は翌年
            // 例: 12月利用 → 1月支払い（翌年）
            if (paymentMonth < transactionMonth) {
                year++;
            }

            return String.format("%d-%02d", year, paymentMonth);
        }

        // パースできない場合は利用日の翌月を返す（フォールバック）
        LocalDate nextMonth = transactionDate.plusMonths(1);
        return String.format("%d-%02d", nextMonth.getYear(), nextMonth.getMonthValue());
    }
}

