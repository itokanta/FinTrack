package com.example.fintrack.domain;

/**
 * 収入の分割方法を表すEnum
 */
public enum SplitMethod {
    /**
     * パターンを使用した分割
     */
    PATTERN,
    
    /**
     * 手動での金額指定分割
     */
    MANUAL,
    
    /**
     * 単一の資産カテゴリに全額登録
     */
    SINGLE
}

