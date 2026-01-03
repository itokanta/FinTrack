package com.example.fintrack.csv.parser;

import com.example.fintrack.csv.dto.TransactionDTO;
import org.apache.commons.csv.CSVRecord;

/**
 * CSVRecord を解析し、共通取引データ（TransactionDTO）へ変換するインターフェース
 */
public interface CsvParser {
    /**
     * CSVRecordをTransactionDTOに変換
     * 
     * @param record CSVRecord
     * @return TransactionDTO
     */
    TransactionDTO parse(CSVRecord record);
}

