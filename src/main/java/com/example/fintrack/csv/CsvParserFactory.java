package com.example.fintrack.csv;

import com.example.fintrack.csv.parser.*;
import org.springframework.stereotype.Component;

/**
 * 金融機関種別に応じた CSV パーサーを返すファクトリー。
 *
 * @author kanta ito
 */
@Component
public class CsvParserFactory {
    
    /**
     * 金融機関種別に応じたパーサーを取得する。
     *
     * @param bankType 金融機関種別
     * @return 対応する {@link CsvParser} 実装
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
