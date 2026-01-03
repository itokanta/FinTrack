# CSVパーサー実装ガイド

## 対応金融機関

以下の5つの金融機関のCSVフォーマットに対応しています：

1. **楽天銀行** (`RAKUTEN_BANK`)
2. **楽天カード（JCB）** (`RAKUTEN_CARD_JCB`)
3. **楽天カード（VISA）** (`RAKUTEN_CARD_VISA`)
4. **楽天デビットカード** (`RAKUTEN_DEBIT`)
5. **みずほ銀行** (`MIZUHO_BANK`)
6. **三菱UFJ銀行** (`MUFG_BANK`)

## CSVフォーマット詳細

### 1. 楽天銀行
- **エンコーディング**: Shift-JIS
- **ヘッダー**: あり
- **フォーマット**:
  ```
  取引日,入出金(円),残高(円),入出金内容
  20251201,-1625,23986,VISAデビット A0113457
  ```
- **日付形式**: `yyyyMMdd`
- **金額判定**: 負数=支出、正数=収入

### 2. 楽天カード（JCB/VISA）
- **エンコーディング**: UTF-8
- **ヘッダー**: あり
- **フォーマット**:
  ```
  "利用日","利用店名・商品名","利用者","支払方法","利用金額","支払手数料","支払総額","12月支払金額","1月繰越残高","新規サイン"
  "2025/11/30","ﾔｵｺ-ﾅｶﾞﾚﾔﾏｵｵﾀｶﾉﾓ","本人","1回払い","2044","0","2044","2044","0","*"
  ```
- **日付形式**: `yyyy/MM/dd`
- **金額判定**: 常に支出（クレジットカード）

### 3. 楽天デビットカード
- **エンコーディング**: Shift-JIS
- **ヘッダー**: あり
- **フォーマット**:
  ```
  "ご利用日","ご利用先","ご利用金額(円)","現地通貨額","通貨略号","レート","利用区分","取引番号","認証番号","お支払い金額(円)","ポイント利用数"
  "20251201","ヤオコーナガレヤマオオタカノモ","1625","","","","国内","24721485336-181200007890","113457","1625","0"
  ```
- **日付形式**: `yyyyMMdd`
- **金額判定**: 常に支出（デビットカード）

### 4. みずほ銀行
- **エンコーディング**: Shift-JIS
- **ヘッダー**: あり（12行目から）
- **フォーマット**:
  ```
  取引通番,日付,引落金額,預入金額,残高,摘要,メモ,取引店番号,取引区分,科目区分
  20251229000020,2025.12.29,3634,,247422,セテイ-ペイデイ-,,,
  ```
- **日付形式**: `yyyy.MM.dd`
- **金額判定**: 引落金額=支出、預入金額=収入

### 5. 三菱UFJ銀行
- **エンコーディング**: Shift-JIS
- **ヘッダー**: あり
- **フォーマット**:
  ```
  "日付","摘要","摘要内容","支払い金額","預かり金額","差引残高","メモ","未資金化区分","入出金区分"
  "2025/12/7","振込入金","ヒシヤ　ツノハシ",,"192,800","302,982","","","振込支払済"
  ```
- **日付形式**: `yyyy/M/d`
- **金額判定**: 支払い金額=支出、預かり金額=収入

## パーサークラス

各金融機関用のパーサークラスを実装しています：

| 金融機関 | パーサークラス | ファイル |
|---------|--------------|---------|
| 楽天銀行 | `RakutenBankParser` | `RakutenBankParser.java` |
| 楽天カード（JCB/VISA） | `RakutenCardParser` | `RakutenCardParser.java` |
| 楽天デビットカード | `RakutenDebitParser` | `RakutenDebitParser.java` |
| みずほ銀行 | `MizuhoBankParser` | `MizuhoBankParser.java` |
| 三菱UFJ銀行 | `MufgBankParser` | `MufgBankParser.java` |

## エンコーディング対応

`CsvImportService`で金融機関に応じたエンコーディングを自動選択：

```java
private Charset getCharsetForBankType(BankType bankType) {
    return switch (bankType) {
        case RAKUTEN_BANK, RAKUTEN_DEBIT, MIZUHO_BANK, MUFG_BANK -> 
            Charset.forName("Shift_JIS");
        case RAKUTEN_CARD_JCB, RAKUTEN_CARD_VISA -> 
            StandardCharsets.UTF_8;
    };
}
```

## 使用方法

1. CSV取込画面（`/csv/import`）にアクセス
2. 金融機関を選択
3. CSVファイルをアップロード
4. ヘッダー行の有無を選択
5. 「取込」ボタンをクリック

## 新しい金融機関の追加方法

### 1. BankTypeに追加

```java
public enum BankType {
    // ... 既存の定義 ...
    NEW_BANK("新しい銀行");
}
```

### 2. パーサークラスを作成

```java
public class NewBankParser implements CsvParser {
    @Override
    public TransactionDTO parse(CSVRecord record) {
        // パース処理を実装
    }
}
```

### 3. CsvParserFactoryに追加

```java
public CsvParser getParser(BankType bankType) {
    return switch (bankType) {
        // ... 既存の定義 ...
        case NEW_BANK -> new NewBankParser();
    };
}
```

### 4. エンコーディングを設定

```java
private Charset getCharsetForBankType(BankType bankType) {
    return switch (bankType) {
        // ... 既存の定義 ...
        case NEW_BANK -> StandardCharsets.UTF_8; // または Shift_JIS
    };
}
```

## 注意事項

1. **エンコーディング**
   - 日本の銀行は主にShift-JISを使用
   - クレジットカード会社はUTF-8が多い
   - 文字化けする場合はエンコーディングを確認

2. **日付フォーマット**
   - 各金融機関で異なるため、DateTimeFormatterを適切に設定

3. **金額の符号**
   - 銀行：正負で入出金を判定
   - カード：常に支出として扱う

4. **ヘッダー行**
   - みずほ銀行は12行目からデータが始まる
   - 取込時に適切にスキップする必要がある

5. **重複チェック**
   - フィンガープリント（SHA-256ハッシュ）で重複を検出
   - 同じ取引は複数回取り込まれない

