package com.example.fintrack.csv;

import com.example.fintrack.csv.parser.CsvParser;
import com.example.fintrack.csv.parser.GenericCreditCardParser;
import com.example.fintrack.csv.parser.RakutenBankParser;
import org.springframework.stereotype.Component;

/**
 * 金融機関種別に応じた CsvParser を返却するファクトリ
 */
@Component
public class CsvParserFactory {
    
    private final GenericCreditCardParser genericCreditCardParser;
    private final RakutenBankParser rakutenBankParser;
    
    public CsvParserFactory(GenericCreditCardParser genericCreditCardParser,
                           RakutenBankParser rakutenBankParser) {
        this.genericCreditCardParser = genericCreditCardParser;
        this.rakutenBankParser = rakutenBankParser;
    }
    
    /**
     * 金融機関種別に応じたパーサーを取得
     * 
     * @param bankType 金融機関種別
     * @return CsvParser
     * @throws IllegalArgumentException 未対応の金融機関の場合
     */
    public CsvParser getParser(BankType bankType) {
        return switch (bankType) {
            case RAKUTEN -> rakutenBankParser;
            case CREDIT_CARD_GENERIC -> genericCreditCardParser;
            // 他の金融機関は今後追加
            default -> throw new IllegalArgumentException("未対応の金融機関です: " + bankType);
        };
    }
}

