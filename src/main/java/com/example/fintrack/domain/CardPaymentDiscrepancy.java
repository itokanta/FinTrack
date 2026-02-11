package com.example.fintrack.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardPaymentDiscrepancy {
    private Long id;
    private Long userId;
    private String paymentMonth; // YYYY-MM形式
    private Long bankTransactionId; // 銀行引き落としのトランザクションID
    private Long expectedAmount; // 家計簿上の前月カード利用額
    private Long actualAmount; // 実際の引き落とし額
    private Long differenceAmount; // 差異金額（actual - expected）
    private Boolean isResolved; // 解決済みフラグ
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
}
