package com.example.fintrack.csv;

import com.example.fintrack.csv.parser.*;
import org.springframework.stereotype.Component;

/**
 * CSVパーサーのファクトリークラス
 */
@Component
public class CsvParserFactory {
    
    /**
     * 金融機関種別に応じたパーサーを取得
     */
    public CsvParser getParser(BankType bankType) {
        return switch (bankType) {
            case RAKUTEN_BANK -> new RakutenBankParser();
            case RAKUTEN_CARD -> new RakutenCardParser();
            case RAKUTEN_DEBIT -> new RakutenDebitParser();
            case MIZUHO_BANK -> new MizuhoBankParser();
            case MUFG_BANK -> new MufgBankParser();
        };
    }
}
