package com.example.fintrack.csv;

import com.example.fintrack.csv.dto.TransactionDTO;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 取引の重複判定用 fingerprint（SHA-256）を生成する。
 *
 * @author kanta ito
 */
@Service
public class FingerprintService {
    
    /**
     * TransactionDTOからfingerprintを生成
     * 
     * @param dto TransactionDTO
     * @return fingerprint（SHA-256ハッシュ値）
     */
    public String generateFingerprint(TransactionDTO dto) {
        // 日付+金額+摘要+入出金区分を結合
        String combined = dto.getTransactionDate().toString() +
                         dto.getAmount().toString() +
                         dto.getDescription() +
                         dto.getTransactionType().toString();
        
        return sha256(combined);
    }
    
    /**
     * 文字列のSHA-256ハッシュ値を生成
     * 
     * @param input 入力文字列
     * @return SHA-256ハッシュ値（16進数文字列）
     */
    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            
            // バイト配列を16進数文字列に変換
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256アルゴリズムが見つかりません", e);
        }
    }
}

