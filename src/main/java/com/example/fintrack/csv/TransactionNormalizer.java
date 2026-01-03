package com.example.fintrack.csv;

import com.example.fintrack.csv.dto.TransactionDTO;
import org.springframework.stereotype.Component;

/**
 * 取引データをアプリ内ルールに従って正規化する
 */
@Component
public class TransactionNormalizer {
    
    /**
     * TransactionDTOを正規化
     * 
     * @param dto 正規化前のDTO
     * @return 正規化後のDTO
     */
    public TransactionDTO normalize(TransactionDTO dto) {
        TransactionDTO normalized = new TransactionDTO();
        
        // 日付はそのまま
        normalized.setTransactionDate(dto.getTransactionDate());
        
        // 金額は絶対値に統一
        normalized.setAmount(Math.abs(dto.getAmount()));
        
        // 摘要の正規化
        String description = dto.getDescription();
        description = description.trim();
        description = description.replaceAll("\\s+", " "); // 連続する空白を1つに
        normalized.setDescription(description);
        
        // 入出金区分はそのまま
        normalized.setTransactionType(dto.getTransactionType());
        
        return normalized;
    }
}

