package com.example.fintrack.csv;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * CSV ファイルを Apache Commons CSV で読み込み、
 * 行単位のデータ（{@link CSVRecord}）に変換する。
 *
 * @author kanta ito
 */
@Component
public class CsvReader {
    
    /**
     * CSVファイルを読み込み、CSVRecordのリストを返す
     * 
     * @param inputStream CSVファイルの入力ストリーム
     * @param hasHeader ヘッダー行の有無
     * @param charset 文字エンコーディング
     * @param skipLines 読み飛ばす行数（ヘッダー行より前の行）
     * @return CSVRecordのリスト
     * @throws IOException ファイル読み込みエラー
     */
    public List<CSVRecord> read(InputStream inputStream, boolean hasHeader, Charset charset, int skipLines) throws IOException {
        // 指定されたエンコーディングでReader作成
        // BufferedReaderでラップしてmark/resetを使用可能にする
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, charset));
        
        // UTF-8の場合はBOM除去
        if (charset.equals(StandardCharsets.UTF_8)) {
            reader.mark(1);
            int firstChar = reader.read();
            if (firstChar != 0xFEFF) {
                reader.reset();
            }
        }
        
        // 指定された行数をスキップ
        for (int i = 0; i < skipLines; i++) {
            reader.readLine();
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
     * CSVファイルを読み込み、CSVRecordのリストを返す
     * 
     * @param inputStream CSVファイルの入力ストリーム
     * @param hasHeader ヘッダー行の有無
     * @param charset 文字エンコーディング
     * @return CSVRecordのリスト
     * @throws IOException ファイル読み込みエラー
     */
    public List<CSVRecord> read(InputStream inputStream, boolean hasHeader, Charset charset) throws IOException {
        return read(inputStream, hasHeader, charset, 0);
    }
    
    /**
     * CSVファイルを読み込み（UTF-8デフォルト）
     * 
     * @param inputStream CSVファイルの入力ストリーム
     * @param hasHeader ヘッダー行の有無
     * @return CSVRecordのリスト
     * @throws IOException ファイル読み込みエラー
     */
    public List<CSVRecord> read(InputStream inputStream, boolean hasHeader) throws IOException {
        return read(inputStream, hasHeader, StandardCharsets.UTF_8);
    }
}
