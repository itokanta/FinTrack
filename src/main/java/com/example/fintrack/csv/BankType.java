package com.example.fintrack.csv;

/**
 * 対応している金融機関の種別。
 * スキップ行数など、パーサー側の前提もここで持つ。
 *
 * @author kanta ito
 */
public enum BankType {
    RAKUTEN_BANK("楽天銀行", 0),
    RAKUTEN_CARD("楽天カード", 0),
    RAKUTEN_DEBIT("楽天デビットカード", 0),
    MIZUHO_BANK("みずほ銀行", 12),  // 最初の12行はメタデータ
    MUFG_BANK("三菱UFJ銀行", 0);
    
    private final String displayName;
    private final int skipLines;  // ヘッダー行より前にスキップする行数
    
    BankType(String displayName, int skipLines) {
        this.displayName = displayName;
        this.skipLines = skipLines;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public int getSkipLines() {
        return skipLines;
    }
}
