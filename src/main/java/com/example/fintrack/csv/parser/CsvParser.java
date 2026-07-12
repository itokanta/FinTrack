package com.example.fintrack.csv.parser;

import com.example.fintrack.csv.dto.TransactionDTO;
import org.apache.commons.csv.CSVRecord;

/**
 * {@link CSVRecord} を解析し、共通取引データ（{@link TransactionDTO}）へ変換するインターフェース。
 *
 * @author kanta ito
 */
public interface CsvParser {
    /**
     * CSV の 1 行を {@link TransactionDTO} に変換する。
     *
     * @param record パース済みの CSV 行
     * @return 変換後の取引 DTO。スキップ対象の行は {@code null}
     */
    TransactionDTO parse(CSVRecord record);
}

