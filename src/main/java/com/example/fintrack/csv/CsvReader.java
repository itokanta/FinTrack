package com.example.fintrack.csv;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * CSVファイルを Apache Commons CSV を用いて読み込み、
 * 行単位のデータ（CSVRecord）に変換する
 */
@Component
public class CsvReader {
    
    /**
     * CSVファイルを読み込み、CSVRecordのリストを返す
     * 
     * @param inputStream CSVファイルの入力ストリーム
     * @param hasHeader ヘッダー行の有無
     * @return CSVRecordのリスト
     * @throws IOException ファイル読み込みエラー
     */
    public List<CSVRecord> read(InputStream inputStream, boolean hasHeader) throws IOException {
        // BOM除去のためのReader
        Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        
        // BOMがある場合はスキップ
        reader.mark(1);
        int firstChar = reader.read();
        if (firstChar != 0xFEFF) {
            reader.reset();
        }
        
        // CSVFormatの設定
        CSVFormat format = CSVFormat.DEFAULT
                .builder()
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build();
        
        if (hasHeader) {
            format = format.builder().setHeader().setSkipHeaderRecord(true).build();
        }
        
        // CSVParserでパース
        try (CSVParser parser = new CSVParser(reader, format)) {
            return parser.getRecords();
        }
    }
    
    /**
     * CSVファイルパスから読み込み
     * 
     * @param filePath CSVファイルパス
     * @param hasHeader ヘッダー行の有無
     * @return CSVRecordのリスト
     * @throws IOException ファイル読み込みエラー
     */
    public List<CSVRecord> read(String filePath, boolean hasHeader) throws IOException {
        try (InputStream inputStream = new FileInputStream(filePath)) {
            return read(inputStream, hasHeader);
        }
    }
}

