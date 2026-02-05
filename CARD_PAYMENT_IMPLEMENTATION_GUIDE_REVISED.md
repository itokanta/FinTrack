# カード支払い月管理機能 実装ガイド（改訂版）

## CSVフォーマットについて

楽天カードのCSVは以下の形式です：
- 8列目（インデックス7）: 「支払月」カラム（例: "2月", "3月"）
- この値を「2026-02」形式に変換してDBに保存

---

## 1. データベースマイグレーション

### 実行するSQL
```bash
psql -U your_username -d fintrack -f sql/13_add_card_payment_month_management.sql
```

---

## 2. ドメインクラスの修正

### 2-1. `CardTransaction.java` の修正

**ファイル:** `src/main/java/com/example/fintrack/domain/CardTransaction.java`

**修正箇所:** 28行目の `private String cardType;` の下に以下を追加

```java
private String paymentMonth; // YYYY-MM形式（例: "2025-01"）
```

---

### 2-2. `BankTransaction.java` の修正

**ファイル:** `src/main/java/com/example/fintrack/domain/BankTransaction.java`

**修正箇所:** 27行目の `private String transactionSource;` の下に以下を追加

```java
private Boolean isCardPayment; // カード引き落としフラグ
```

---

### 2-3. 新規ドメインクラス `CardPaymentDiscrepancy.java` の作成

**ファイル:** `src/main/java/com/example/fintrack/domain/CardPaymentDiscrepancy.java`

**内容:**
```java
package com.example.fintrack.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardPaymentDiscrepancy {
    
    private Long id;
    private Long userId;
    private String paymentMonth; // YYYY-MM形式
    private Long bankTransactionId; // 銀行引き落としのトランザクションID
    private Long expectedAmount; // 家計簿上の前月カード利用額
    private Long actualAmount; // 実際の引き落とし額
    private Long differenceAmount; // 差異金額（actual - expected）
    private Boolean isResolved; // 解決済みフラグ
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
}
```

---

## 3. RowMapperの修正

### 3-1. `CardTransactionRowMapper.java` の修正

**ファイル:** `src/main/java/com/example/fintrack/repository/mapper/CardTransactionRowMapper.java`

**修正箇所:** 49行目の `transaction.setCardType(rs.getString("card_type"));` の下に以下を追加

```java
transaction.setPaymentMonth(rs.getString("payment_month"));
```

---

### 3-2. `BankTransactionRowMapper.java` の修正

**ファイル:** `src/main/java/com/example/fintrack/repository/mapper/BankTransactionRowMapper.java`

**修正箇所:** 48行目の `transaction.setTransactionSource(rs.getString("transaction_source"));` の下に以下を追加

```java
// is_card_paymentを読み取り
Boolean isCardPayment = rs.getBoolean("is_card_payment");
if (rs.wasNull()) {
    transaction.setIsCardPayment(false);
} else {
    transaction.setIsCardPayment(isCardPayment);
}
```

---

### 3-3. 新規RowMapper `CardPaymentDiscrepancyRowMapper.java` の作成

**ファイル:** `src/main/java/com/example/fintrack/repository/mapper/CardPaymentDiscrepancyRowMapper.java`

**内容:**
```java
package com.example.fintrack.repository.mapper;

import com.example.fintrack.domain.CardPaymentDiscrepancy;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class CardPaymentDiscrepancyRowMapper implements RowMapper<CardPaymentDiscrepancy> {
    
    @Override
    public CardPaymentDiscrepancy mapRow(ResultSet rs, int rowNum) throws SQLException {
        CardPaymentDiscrepancy discrepancy = new CardPaymentDiscrepancy();
        discrepancy.setId(rs.getLong("id"));
        discrepancy.setUserId(rs.getLong("user_id"));
        discrepancy.setPaymentMonth(rs.getString("payment_month"));
        discrepancy.setBankTransactionId(rs.getLong("bank_transaction_id"));
        discrepancy.setExpectedAmount(rs.getLong("expected_amount"));
        discrepancy.setActualAmount(rs.getLong("actual_amount"));
        discrepancy.setDifferenceAmount(rs.getLong("difference_amount"));
        discrepancy.setIsResolved(rs.getBoolean("is_resolved"));
        discrepancy.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        
        if (rs.getTimestamp("resolved_at") != null) {
            discrepancy.setResolvedAt(rs.getTimestamp("resolved_at").toLocalDateTime());
        }
        
        return discrepancy;
    }
}
```

**注意:** `bank_transaction_id` は必ず値が入るため、NULL判定は不要です。

---

## 4. Repositoryの修正

### 4-1. `CardTransactionRepository.java` の修正

**ファイル:** `src/main/java/com/example/fintrack/repository/CardTransactionRepository.java`

#### 修正1: saveメソッドのINSERT文を修正（120-123行目）

**変更前:**
```java
String sql = "INSERT INTO card_transactions " +
            "(description, amount, transaction_type, transaction_date, fingerprint, user_id, " +
            "asset_category_id, expense_category_id, split_pattern_id, transaction_source, card_type, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
```

**変更後:**
```java
String sql = "INSERT INTO card_transactions " +
            "(description, amount, transaction_type, transaction_date, fingerprint, user_id, " +
            "asset_category_id, expense_category_id, split_pattern_id, transaction_source, card_type, payment_month, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
```

#### 修正2: saveメソッドのINSERT実行部分を修正（129-141行目）

**変更前:**
```java
Long id = jdbcTemplate.queryForObject(sql, Long.class,
    transaction.getDescription(),
    transaction.getAmount(),
    transaction.getTransactionType().name(),
    transaction.getTransactionDate(),
    transaction.getFingerprint(),
    transaction.getUserId(),
    transaction.getAssetCategoryId(),
    transaction.getExpenseCategoryId(),
    splitPatternId,
    transaction.getTransactionSource(),
    transaction.getCardType(),
    now);
```

**変更後:**
```java
Long id = jdbcTemplate.queryForObject(sql, Long.class,
    transaction.getDescription(),
    transaction.getAmount(),
    transaction.getTransactionType().name(),
    transaction.getTransactionDate(),
    transaction.getFingerprint(),
    transaction.getUserId(),
    transaction.getAssetCategoryId(),
    transaction.getExpenseCategoryId(),
    splitPatternId,
    transaction.getTransactionSource(),
    transaction.getCardType(),
    transaction.getPaymentMonth(),
    now);
```

#### 修正3: saveメソッドのUPDATE文を修正（146-151行目）

**変更前:**
```java
String sql = "UPDATE card_transactions SET " +
            "description = ?, amount = ?, transaction_type = ?, " +
            "transaction_date = ?, fingerprint = ?, user_id = ?, " +
            "asset_category_id = ?, expense_category_id = ?, split_pattern_id = ?, " +
            "transaction_source = ?, card_type = ? " +
            "WHERE id = ?";
```

**変更後:**
```java
String sql = "UPDATE card_transactions SET " +
            "description = ?, amount = ?, transaction_type = ?, " +
            "transaction_date = ?, fingerprint = ?, user_id = ?, " +
            "asset_category_id = ?, expense_category_id = ?, split_pattern_id = ?, " +
            "transaction_source = ?, card_type = ?, payment_month = ? " +
            "WHERE id = ?";
```

#### 修正4: saveメソッドのUPDATE実行部分を修正（156-168行目）

**変更前:**
```java
jdbcTemplate.update(sql,
    transaction.getDescription(),
    transaction.getAmount(),
    transaction.getTransactionType().name(),
    transaction.getTransactionDate(),
    transaction.getFingerprint(),
    transaction.getUserId(),
    transaction.getAssetCategoryId(),
    transaction.getExpenseCategoryId(),
    splitPatternId,
    transaction.getTransactionSource(),
    transaction.getCardType(),
    transaction.getId());
```

**変更後:**
```java
jdbcTemplate.update(sql,
    transaction.getDescription(),
    transaction.getAmount(),
    transaction.getTransactionType().name(),
    transaction.getTransactionDate(),
    transaction.getFingerprint(),
    transaction.getUserId(),
    transaction.getAssetCategoryId(),
    transaction.getExpenseCategoryId(),
    splitPatternId,
    transaction.getTransactionSource(),
    transaction.getCardType(),
    transaction.getPaymentMonth(),
    transaction.getId());
```

#### 修正5: 新しいメソッドを追加（172行目の最後に追加）

```java
/**
 * 指定した支払い月のカード取引を取得
 */
public List<CardTransaction> findByUserIdAndPaymentMonth(Long userId, String paymentMonth) {
    String sql = "SELECT * FROM card_transactions " +
                "WHERE user_id = ? AND payment_month = ? " +
                "ORDER BY transaction_date DESC";
    return jdbcTemplate.query(sql, rowMapper, userId, paymentMonth);
}

/**
 * 指定した支払い月のカード取引合計金額を取得
 */
public Long sumAmountByUserIdAndPaymentMonth(Long userId, String paymentMonth) {
    String sql = "SELECT COALESCE(SUM(amount), 0) FROM card_transactions " +
                "WHERE user_id = ? AND payment_month = ? AND transaction_type = 'EXPENSE'";
    return jdbcTemplate.queryForObject(sql, Long.class, userId, paymentMonth);
}
```

---

### 4-2. `BankTransactionRepository.java` の修正

**ファイル:** `src/main/java/com/example/fintrack/repository/BankTransactionRepository.java`

#### 修正1: saveメソッドのINSERT文を修正（120-123行目）

**変更前:**
```java
String sql = "INSERT INTO bank_transactions " +
            "(description, amount, transaction_type, transaction_date, fingerprint, user_id, " +
            "asset_category_id, expense_category_id, split_pattern_id, transaction_source, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
```

**変更後:**
```java
String sql = "INSERT INTO bank_transactions " +
            "(description, amount, transaction_type, transaction_date, fingerprint, user_id, " +
            "asset_category_id, expense_category_id, split_pattern_id, transaction_source, is_card_payment, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
```

#### 修正2: saveメソッドのINSERT実行部分を修正（129-140行目）

**変更前:**
```java
Long id = jdbcTemplate.queryForObject(sql, Long.class,
    transaction.getDescription(),
    transaction.getAmount(),
    transaction.getTransactionType().name(),
    transaction.getTransactionDate(),
    transaction.getFingerprint(),
    transaction.getUserId(),
    transaction.getAssetCategoryId(),
    transaction.getExpenseCategoryId(),
    splitPatternId,
    transaction.getTransactionSource(),
    now);
```

**変更後:**
```java
Long id = jdbcTemplate.queryForObject(sql, Long.class,
    transaction.getDescription(),
    transaction.getAmount(),
    transaction.getTransactionType().name(),
    transaction.getTransactionDate(),
    transaction.getFingerprint(),
    transaction.getUserId(),
    transaction.getAssetCategoryId(),
    transaction.getExpenseCategoryId(),
    splitPatternId,
    transaction.getTransactionSource(),
    transaction.getIsCardPayment() != null ? transaction.getIsCardPayment() : false,
    now);
```

#### 修正3: saveメソッドのUPDATE文を修正（145-150行目）

**変更前:**
```java
String sql = "UPDATE bank_transactions SET " +
            "description = ?, amount = ?, transaction_type = ?, " +
            "transaction_date = ?, fingerprint = ?, user_id = ?, " +
            "asset_category_id = ?, expense_category_id = ?, split_pattern_id = ?, " +
            "transaction_source = ? " +
            "WHERE id = ?";
```

**変更後:**
```java
String sql = "UPDATE bank_transactions SET " +
            "description = ?, amount = ?, transaction_type = ?, " +
            "transaction_date = ?, fingerprint = ?, user_id = ?, " +
            "asset_category_id = ?, expense_category_id = ?, split_pattern_id = ?, " +
            "transaction_source = ?, is_card_payment = ? " +
            "WHERE id = ?";
```

#### 修正4: saveメソッドのUPDATE実行部分を修正（155-166行目）

**変更前:**
```java
jdbcTemplate.update(sql,
    transaction.getDescription(),
    transaction.getAmount(),
    transaction.getTransactionType().name(),
    transaction.getTransactionDate(),
    transaction.getFingerprint(),
    transaction.getUserId(),
    transaction.getAssetCategoryId(),
    transaction.getExpenseCategoryId(),
    splitPatternId,
    transaction.getTransactionSource(),
    transaction.getId());
```

**変更後:**
```java
jdbcTemplate.update(sql,
    transaction.getDescription(),
    transaction.getAmount(),
    transaction.getTransactionType().name(),
    transaction.getTransactionDate(),
    transaction.getFingerprint(),
    transaction.getUserId(),
    transaction.getAssetCategoryId(),
    transaction.getExpenseCategoryId(),
    splitPatternId,
    transaction.getTransactionSource(),
    transaction.getIsCardPayment() != null ? transaction.getIsCardPayment() : false,
    transaction.getId());
```

#### 修正5: 新しいメソッドを追加（170行目の最後に追加）

```java
/**
 * カード引き落としの取引を除外して取得
 */
public List<BankTransaction> findByUserIdAndTransactionTypeAndTransactionDateBetweenExcludingCardPayment(
        Long userId, TransactionType transactionType, LocalDate startDate, LocalDate endDate, 
        int offset, int limit) {
    String sql = "SELECT * FROM bank_transactions " +
                "WHERE user_id = ? AND transaction_type = ? AND transaction_date BETWEEN ? AND ? " +
                "AND (is_card_payment = FALSE OR is_card_payment IS NULL) " +
                "ORDER BY transaction_date DESC " +
                "LIMIT ? OFFSET ?";
    return jdbcTemplate.query(sql, rowMapper, userId, transactionType.name(), startDate, endDate, limit, offset);
}
```

---

### 4-3. 新規Repository `CardPaymentDiscrepancyRepository.java` の作成

**ファイル:** `src/main/java/com/example/fintrack/repository/CardPaymentDiscrepancyRepository.java`

**内容:**
```java
package com.example.fintrack.repository;

import com.example.fintrack.domain.CardPaymentDiscrepancy;
import com.example.fintrack.repository.mapper.CardPaymentDiscrepancyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class CardPaymentDiscrepancyRepository {
    
    private final JdbcTemplate jdbcTemplate;
    private final CardPaymentDiscrepancyRowMapper rowMapper = new CardPaymentDiscrepancyRowMapper();
    
    public CardPaymentDiscrepancyRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    public Optional<CardPaymentDiscrepancy> findById(Long id) {
        String sql = "SELECT * FROM card_payment_discrepancies WHERE id = ?";
        List<CardPaymentDiscrepancy> discrepancies = jdbcTemplate.query(sql, rowMapper, id);
        if (discrepancies.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(discrepancies.get(0));
    }
    
    public List<CardPaymentDiscrepancy> findByUserId(Long userId) {
        String sql = "SELECT * FROM card_payment_discrepancies " +
                    "WHERE user_id = ? " +
                    "ORDER BY payment_month DESC, created_at DESC";
        return jdbcTemplate.query(sql, rowMapper, userId);
    }
    
    public List<CardPaymentDiscrepancy> findByUserIdAndIsResolved(Long userId, Boolean isResolved) {
        String sql = "SELECT * FROM card_payment_discrepancies " +
                    "WHERE user_id = ? AND is_resolved = ? " +
                    "ORDER BY payment_month DESC, created_at DESC";
        return jdbcTemplate.query(sql, rowMapper, userId, isResolved);
    }
    
    public CardPaymentDiscrepancy save(CardPaymentDiscrepancy discrepancy) {
        if (discrepancy.getId() == null) {
            // INSERT
            String sql = "INSERT INTO card_payment_discrepancies " +
                        "(user_id, payment_month, bank_transaction_id, expected_amount, actual_amount, " +
                        "difference_amount, is_resolved, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
            LocalDateTime now = LocalDateTime.now();
            Long id = jdbcTemplate.queryForObject(sql, Long.class,
                discrepancy.getUserId(),
                discrepancy.getPaymentMonth(),
                discrepancy.getBankTransactionId(),
                discrepancy.getExpectedAmount(),
                discrepancy.getActualAmount(),
                discrepancy.getDifferenceAmount(),
                discrepancy.getIsResolved() != null ? discrepancy.getIsResolved() : false,
                now);
            discrepancy.setId(id);
            discrepancy.setCreatedAt(now);
        } else {
            // UPDATE
            String sql = "UPDATE card_payment_discrepancies SET " +
                        "user_id = ?, payment_month = ?, bank_transaction_id = ?, " +
                        "expected_amount = ?, actual_amount = ?, difference_amount = ?, " +
                        "is_resolved = ?, resolved_at = ? " +
                        "WHERE id = ?";
            jdbcTemplate.update(sql,
                discrepancy.getUserId(),
                discrepancy.getPaymentMonth(),
                discrepancy.getBankTransactionId(),
                discrepancy.getExpectedAmount(),
                discrepancy.getActualAmount(),
                discrepancy.getDifferenceAmount(),
                discrepancy.getIsResolved(),
                discrepancy.getResolvedAt(),
                discrepancy.getId());
        }
        return discrepancy;
    }
    
    public void markAsResolved(Long id) {
        String sql = "UPDATE card_payment_discrepancies SET " +
                    "is_resolved = TRUE, resolved_at = ? " +
                    "WHERE id = ?";
        jdbcTemplate.update(sql, LocalDateTime.now(), id);
    }
}
```

---

## 5. DTOの修正

### 5-1. `TransactionDTO.java` の修正

**ファイル:** `src/main/java/com/example/fintrack/csv/dto/TransactionDTO.java`

**修正箇所:** クラスの最後に以下のフィールドを追加

```java
private String paymentMonth; // YYYY-MM形式（カード取引の支払い月）
private Boolean isCardPayment; // カード引き落としフラグ
```

---

## 6. CSV Parserの修正

### 6-1. `RakutenCardParser.java` の修正

**ファイル:** `src/main/java/com/example/fintrack/csv/parser/RakutenCardParser.java`

**完全な修正後のコード:**

```java
package com.example.fintrack.csv.parser;

import com.example.fintrack.csv.dto.TransactionDTO;
import com.example.fintrack.domain.TransactionType;
import org.apache.commons.csv.CSVRecord;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 楽天カードのCSVパーサー
 * 
 * CSVフォーマット（UTF-8）:
 * "利用日","利用店名・商品名","利用者","支払方法","利用金額","支払手数料","支払総額","支払月","2月支払金額","3月繰越残高","3月以降支払金額"
 * "2026/01/23","ＶＩＳＡ海外利用　CURSOR, AI POWERED I","本人","1回払い","3293","0","3293","2月","3293","0",""
 * 
 * 注意: 利用日が空欄の行（詳細情報行など）はスキップされます
 */
public class RakutenCardParser implements CsvParser {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final Pattern PAYMENT_MONTH_PATTERN = Pattern.compile("(\\d+)月");
    
    @Override
    public TransactionDTO parse(CSVRecord record) {
        // 利用日が空欄の場合はスキップ（詳細情報行など）
        String dateStr = record.get(0);
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        
        TransactionDTO dto = new TransactionDTO();
        
        // 利用日
        LocalDate transactionDate = LocalDate.parse(dateStr, DATE_FORMATTER);
        dto.setTransactionDate(transactionDate);
        
        // 利用店名・商品名
        dto.setDescription(record.get(1));
        
        // 利用金額（クレジットカードは常に支出）
        String amountStr = record.get(4).replace(",", "");
        dto.setAmount(Long.parseLong(amountStr));
        dto.setTransactionType(TransactionType.EXPENSE);
        
        // 支払い月を設定（8列目: インデックス7）
        // CSVの形式: "2月", "3月" など
        if (record.size() > 7) {
            String paymentMonthStr = record.get(7);
            if (paymentMonthStr != null && !paymentMonthStr.trim().isEmpty()) {
                String paymentMonth = convertPaymentMonth(paymentMonthStr, transactionDate);
                dto.setPaymentMonth(paymentMonth);
            }
        }
        
        return dto;
    }
    
    /**
     * 支払い月文字列を YYYY-MM 形式に変換
     * 
     * @param paymentMonthStr CSVの支払い月文字列（例: "2月", "3月"）
     * @param transactionDate 利用日
     * @return YYYY-MM形式の支払い月（例: "2026-02"）
     */
    private String convertPaymentMonth(String paymentMonthStr, LocalDate transactionDate) {
        Matcher matcher = PAYMENT_MONTH_PATTERN.matcher(paymentMonthStr);
        if (matcher.find()) {
            int month = Integer.parseInt(matcher.group(1));
            
            // 利用日の年を基準に支払い月の年を決定
            int year = transactionDate.getYear();
            int transactionMonth = transactionDate.getMonthValue();
            
            // 利用日の月より支払い月が小さい場合は翌年
            // 例: 12月利用 → 1月支払い（翌年）
            if (month < transactionMonth) {
                year++;
            }
            
            return String.format("%d-%02d", year, month);
        }
        
        // パースできない場合は利用日の翌月を返す
        LocalDate nextMonth = transactionDate.plusMonths(1);
        return String.format("%d-%02d", nextMonth.getYear(), nextMonth.getMonthValue());
    }
}
```

**主な変更点:**
1. `PAYMENT_MONTH_PATTERN` を追加して「2月」などの文字列から数字を抽出
2. `convertPaymentMonth()` メソッドを追加して「2月」→「2026-02」に変換
3. 年跨ぎ（12月利用→1月支払い）のケースに対応

---

## 7. Serviceの修正と新規作成

### 7-1. `CsvImportService.java` の修正

**ファイル:** `src/main/java/com/example/fintrack/service/CsvImportService.java`

#### 修正1: importBankTransactionsメソッド（94行目付近）

**修正箇所:** 「ラクテンカ－ドサ－ビス」を除外している部分を以下に変更

**変更前:**
```java
// 「ラクテンカ－ドサ－ビス」を除外
if (dto.getDescription() != null && dto.getDescription().contains("ラクテンカ－ドサ－ビス")) {
    continue;
}
```

**変更後:**
```java
// 「ラクテンカ－ドサ－ビス」はカード引き落としとしてマーク
if (dto.getDescription() != null && dto.getDescription().contains("ラクテンカ－ドサ－ビス")) {
    dto.setIsCardPayment(true);
}
```

#### 修正2: importBankTransactionsメソッドのBankTransaction作成部分（116-123行目付近）

**修正箇所:** `transaction.setTransactionSource("BANK");` の下に以下を追加

```java
// カード引き落としフラグを設定
if (dto.getIsCardPayment() != null && dto.getIsCardPayment()) {
    transaction.setIsCardPayment(true);
}
```

#### 修正3: importCardTransactionsメソッドのCardTransaction作成部分（209-223行目付近）

**修正箇所:** `transaction.setCardType("CREDIT");` の下に以下を追加

```java
// 支払い月を設定
transaction.setPaymentMonth(dto.getPaymentMonth());
```

---

### 7-2. 新規Service `CardPaymentService.java` の作成

**ファイル:** `src/main/java/com/example/fintrack/service/CardPaymentService.java`

**内容:**
```java
package com.example.fintrack.service;

import com.example.fintrack.domain.BankTransaction;
import com.example.fintrack.domain.CardPaymentDiscrepancy;
import com.example.fintrack.domain.CardTransaction;
import com.example.fintrack.repository.BankTransactionRepository;
import com.example.fintrack.repository.CardPaymentDiscrepancyRepository;
import com.example.fintrack.repository.CardTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * カード支払い管理サービス
 */
@Service
public class CardPaymentService {
    
    private final CardTransactionRepository cardTransactionRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final CardPaymentDiscrepancyRepository discrepancyRepository;
    
    public CardPaymentService(CardTransactionRepository cardTransactionRepository,
                             BankTransactionRepository bankTransactionRepository,
                             CardPaymentDiscrepancyRepository discrepancyRepository) {
        this.cardTransactionRepository = cardTransactionRepository;
        this.bankTransactionRepository = bankTransactionRepository;
        this.discrepancyRepository = discrepancyRepository;
    }
    
    /**
     * 指定した支払い月のカード取引一覧を取得
     */
    public List<CardTransaction> getCardTransactionsByPaymentMonth(Long userId, String paymentMonth) {
        return cardTransactionRepository.findByUserIdAndPaymentMonth(userId, paymentMonth);
    }
    
    /**
     * 指定した支払い月のカード取引合計金額を取得
     */
    public Long getCardPaymentAmountByMonth(Long userId, String paymentMonth) {
        return cardTransactionRepository.sumAmountByUserIdAndPaymentMonth(userId, paymentMonth);
    }
    
    /**
     * カード引き落としと明細を照合
     * 
     * @param bankTransactionId 銀行引き落としのトランザクションID
     * @param userId ユーザーID
     */
    @Transactional
    public void reconcileCardPayment(Long bankTransactionId, Long userId) {
        // 銀行引き落とし取引を取得
        BankTransaction bankTransaction = bankTransactionRepository.findById(bankTransactionId)
                .orElseThrow(() -> new IllegalArgumentException("銀行取引が見つかりません"));
        
        // 引き落とし日から支払い月を計算（引き落とし日の当月分）
        LocalDate withdrawalDate = bankTransaction.getTransactionDate();
        YearMonth paymentMonth = YearMonth.from(withdrawalDate);
        String paymentMonthStr = paymentMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        
        // 家計簿上のカード利用額を取得（支払い月が一致するもの）
        Long expectedAmount = getCardPaymentAmountByMonth(userId, paymentMonthStr);
        
        // 実際の引き落とし額
        Long actualAmount = bankTransaction.getAmount();
        
        // 差異チェック
        if (!expectedAmount.equals(actualAmount)) {
            // 差異がある場合、差異データを登録
            Long differenceAmount = actualAmount - expectedAmount;
            
            CardPaymentDiscrepancy discrepancy = new CardPaymentDiscrepancy();
            discrepancy.setUserId(userId);
            discrepancy.setPaymentMonth(paymentMonthStr);
            discrepancy.setBankTransactionId(bankTransactionId);
            discrepancy.setExpectedAmount(expectedAmount);
            discrepancy.setActualAmount(actualAmount);
            discrepancy.setDifferenceAmount(differenceAmount);
            discrepancy.setIsResolved(false);
            
            discrepancyRepository.save(discrepancy);
            
            // TODO: 差異金額分を未分類のデータとして登録する処理を追加
        }
    }
    
    /**
     * 未解決のカード差異一覧を取得
     */
    public List<CardPaymentDiscrepancy> getUnresolvedDiscrepancies(Long userId) {
        return discrepancyRepository.findByUserIdAndIsResolved(userId, false);
    }
    
    /**
     * すべてのカード差異一覧を取得
     */
    public List<CardPaymentDiscrepancy> getAllDiscrepancies(Long userId) {
        return discrepancyRepository.findByUserId(userId);
    }
    
    /**
     * カード差異を解決済みにする
     */
    @Transactional
    public void markDiscrepancyAsResolved(Long discrepancyId) {
        discrepancyRepository.markAsResolved(discrepancyId);
    }
}
```

---

### 7-3. `AssetRepository.java` の修正

**ファイル:** `src/main/java/com/example/fintrack/repository/AssetRepository.java`

#### 修正箇所: getConfirmedTotalBalanceメソッド（約200行目付近）

**変更前:**
```java
public Long getConfirmedTotalBalance(Long userId) {
    String sql = """
        SELECT COALESCE(SUM(balance), 0) as total_balance
        FROM assets
        WHERE user_id = ?
        AND (asset_category_id IS NOT NULL OR asset_category_id IS NULL)
        AND id NOT IN (
            -- 当月と前月のクレジットカード取引を除外
            SELECT a.id
            FROM assets a
            INNER JOIN card_transactions ct ON a.card_transaction_id = ct.id
            WHERE ct.card_type = 'CREDIT'
            AND ct.transaction_date >= DATE_TRUNC('month', CURRENT_DATE - INTERVAL '1 month')
            AND a.user_id = ?
        )
        """;
    return jdbcTemplate.queryForObject(sql, Long.class, userId, userId);
}
```

**変更後:**
```java
public Long getConfirmedTotalBalance(Long userId) {
    // 今月と来月の支払い月を計算
    String currentMonth = java.time.YearMonth.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
    String nextMonth = java.time.YearMonth.now().plusMonths(1).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
    
    String sql = """
        SELECT COALESCE(SUM(balance), 0) as total_balance
        FROM assets
        WHERE user_id = ?
        AND (asset_category_id IS NOT NULL OR asset_category_id IS NULL)
        AND id NOT IN (
            -- 今月と来月支払い予定のクレジットカード取引を除外
            SELECT a.id
            FROM assets a
            INNER JOIN card_transactions ct ON a.card_transaction_id = ct.id
            WHERE ct.card_type = 'CREDIT'
            AND (ct.payment_month = ? OR ct.payment_month = ?)
            AND a.user_id = ?
        )
        """;
    return jdbcTemplate.queryForObject(sql, Long.class, userId, currentMonth, nextMonth, userId);
}
```

#### 修正箇所: getConfirmedBalanceByCategoryメソッド（約220行目付近）

同様に `payment_month` ベースに変更してください。

---

## 8. 既に編集済みのファイルの追加修正

### 8-1. `CardTransactionRepository.java` に既に追加したメソッドの確認

もし既に以下のメソッドを追加済みの場合は、そのままで問題ありません:
- `findByUserIdAndPaymentMonth()`
- `sumAmountByUserIdAndPaymentMonth()`

### 8-2. `BankTransactionRepository.java` に既に追加したメソッドの確認

もし既に以下のメソッドを追加済みの場合は、そのままで問題ありません:
- `findByUserIdAndTransactionTypeAndTransactionDateBetweenExcludingCardPayment()`

---

## 9. Controllerの修正と新規作成

### 9-1. `TransactionController.java` の修正

**ファイル:** `src/main/java/com/example/fintrack/controller/TransactionController.java`

#### 修正1: CardPaymentServiceを注入（クラスのフィールドに追加）

```java
private final CardPaymentService cardPaymentService;
```

#### 修正2: コンストラクタに追加

```java
public TransactionController(TransactionService transactionService,
                            IncomeSplitService incomeSplitService,
                            com.example.fintrack.service.SplitService splitService,
                            UserRepository userRepository,
                            AssetCategoryRepository assetCategoryRepository,
                            ExpenseCategoryRepository expenseCategoryRepository,
                            CardPaymentService cardPaymentService) {  // 追加
    this.transactionService = transactionService;
    this.incomeSplitService = incomeSplitService;
    this.splitService = splitService;
    this.userRepository = userRepository;
    this.assetCategoryRepository = assetCategoryRepository;
    this.expenseCategoryRepository = expenseCategoryRepository;
    this.cardPaymentService = cardPaymentService;  // 追加
}
```

#### 修正3: 支払い月単位の表示用エンドポイントを追加（298行目の最後に追加）

```java
/**
 * 支払い月単位の支出一覧
 */
@GetMapping("/expenses/by-payment-month")
public String expensesByPaymentMonth(Authentication authentication,
                                     @RequestParam(required = false) String paymentMonth,
                                     Model model) {
    Long userId = getUserId(authentication);
    
    // 支払い月の指定がない場合は今月
    String targetPaymentMonth;
    if (paymentMonth != null) {
        targetPaymentMonth = paymentMonth;
    } else {
        targetPaymentMonth = java.time.YearMonth.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
    }
    
    // 支払い月単位のカード取引を取得
    List<CardTransaction> cardTransactions = cardPaymentService.getCardTransactionsByPaymentMonth(userId, targetPaymentMonth);
    Long totalAmount = cardPaymentService.getCardPaymentAmountByMonth(userId, targetPaymentMonth);
    
    model.addAttribute("paymentMonth", targetPaymentMonth);
    model.addAttribute("cardTransactions", cardTransactions);
    model.addAttribute("totalAmount", totalAmount);
    
    return "transactions/payment-by-month";
}
```

---

### 9-2. 新規Controller `CardPaymentController.java` の作成

**ファイル:** `src/main/java/com/example/fintrack/controller/CardPaymentController.java`

**内容:**
```java
package com.example.fintrack.controller;

import com.example.fintrack.domain.CardPaymentDiscrepancy;
import com.example.fintrack.repository.UserRepository;
import com.example.fintrack.service.CardPaymentService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * カード支払い管理コントローラー
 */
@Controller
@RequestMapping("/card-payments")
public class CardPaymentController {
    
    private final CardPaymentService cardPaymentService;
    private final UserRepository userRepository;
    
    public CardPaymentController(CardPaymentService cardPaymentService,
                                UserRepository userRepository) {
        this.cardPaymentService = cardPaymentService;
        this.userRepository = userRepository;
    }
    
    /**
     * カード差異一覧画面
     */
    @GetMapping("/discrepancies")
    public String discrepancies(Authentication authentication, Model model) {
        Long userId = getUserId(authentication);
        
        // 未解決の差異を取得
        List<CardPaymentDiscrepancy> unresolvedDiscrepancies = 
            cardPaymentService.getUnresolvedDiscrepancies(userId);
        
        // すべての差異を取得
        List<CardPaymentDiscrepancy> allDiscrepancies = 
            cardPaymentService.getAllDiscrepancies(userId);
        
        model.addAttribute("unresolvedDiscrepancies", unresolvedDiscrepancies);
        model.addAttribute("allDiscrepancies", allDiscrepancies);
        
        return "card-payments/discrepancies";
    }
    
    /**
     * カード差異を解決済みにする
     */
    @PostMapping("/discrepancies/{id}/resolve")
    public String resolveDiscrepancy(@PathVariable Long id,
                                    RedirectAttributes redirectAttributes) {
        try {
            cardPaymentService.markDiscrepancyAsResolved(id);
            redirectAttributes.addFlashAttribute("message", "差異を解決済みにしました");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "処理に失敗しました: " + e.getMessage());
        }
        
        return "redirect:/card-payments/discrepancies";
    }
    
    /**
     * ユーザーIDを取得
     */
    private Long getUserId(Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("ユーザーが見つかりません"))
                .getId();
    }
}
```

---

## 10. Viewの作成（変更なし）

View層の実装は元のガイドと同じです。以下のファイルを作成してください:

1. `src/main/resources/templates/transactions/payment-by-month.html`
2. `src/main/resources/templates/card-payments/discrepancies.html`
3. `src/main/resources/templates/transactions/expenses.html` の修正（支払い月カラム追加）
4. `src/main/resources/templates/fragments/header.html` の修正（メニュー追加）

詳細は元のガイド（CARD_PAYMENT_IMPLEMENTATION_GUIDE.md）の9章と10章を参照してください。

---

## 実装の順序

1. **データベースマイグレーション** - SQL実行
2. **ドメインクラス** - フィールド追加
3. **RowMapper** - マッピング追加
4. **Repository** - SQL修正とメソッド追加
5. **DTO** - フィールド追加
6. **CSV Parser** - 支払い月の抽出ロジック実装（重要！）
7. **Service** - ビジネスロジック実装
8. **Controller** - エンドポイント追加
9. **View** - 画面作成・修正
10. **メニュー** - リンク追加

---

## 重要な変更点まとめ

### 元のガイドからの主な変更:

1. **RakutenCardParser.java**
   - CSVの8列目（「2月」「3月」など）から支払い月を抽出
   - `convertPaymentMonth()` メソッドで「2月」→「2026-02」形式に変換
   - 年跨ぎ対応（12月利用→1月支払いの場合は翌年）

2. **CardPaymentDiscrepancyRowMapper.java**
   - `bank_transaction_id` のNULL判定を削除（必ず値が入るため）

3. **TransactionController.java**
   - `CardPaymentService` を注入
   - `expensesByPaymentMonth()` メソッドでサービスを使用

この順序で実装すれば、依存関係の問題なく進められます。
