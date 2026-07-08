package com.example.fintrack.repository;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import com.example.fintrack.domain.BankTransaction;
import com.example.fintrack.domain.SplitMethod;
import com.example.fintrack.domain.TransactionSource;
import com.example.fintrack.domain.TransactionType;
import com.example.fintrack.domain.dto.MonthlySummary;
import com.example.fintrack.domain.dto.TransactionEntry;
import com.example.fintrack.repository.extractor.MonthlySummaryByAssetCategoryExtractor;
import com.example.fintrack.repository.rowmapper.MonthlySummaryRowMapper;
import com.example.fintrack.repository.rowmapper.TransactionEntryRowMapper;

/**
 * 銀行取引（bank_transactions テーブル）の DB アクセス。
 *
 * @author kanta ito
 */
@Repository
public class BankTransactionRepository {

    private static final MonthlySummaryByAssetCategoryExtractor MONTHLY_SUMMARY_BY_ASSET_CATEGORY_EXTRACTOR =
            new MonthlySummaryByAssetCategoryExtractor();

    private static final RowMapper<BankTransaction> ROW_MAPPER = (rs, rowNum) -> {
        BankTransaction transaction = new BankTransaction();
        transaction.setId(rs.getLong("id"));
        transaction.setDescription(rs.getString("description"));
        transaction.setAmount(rs.getLong("amount"));
        transaction.setTransactionType(TransactionType.valueOf(rs.getString("transaction_type")));
        transaction.setTransactionDate(rs.getDate("transaction_date").toLocalDate());
        transaction.setFingerprint(rs.getString("fingerprint"));
        transaction.setUserId(rs.getLong("user_id"));

        long assetCategoryId = rs.getLong("asset_category_id");
        transaction.setAssetCategoryId(rs.wasNull() ? null : assetCategoryId);

        long expenseCategoryId = rs.getLong("expense_category_id");
        transaction.setExpenseCategoryId(rs.wasNull() ? null : expenseCategoryId);

        long splitPatternId = rs.getLong("split_pattern_id");
        transaction.setSplitPatternId(rs.wasNull() ? null : splitPatternId);

        transaction.setTransactionSource(rs.getString("transaction_source"));

        Boolean isCardPayment = rs.getBoolean("is_card_payment");
        transaction.setIsCardPayment(rs.wasNull() ? false : isCardPayment);

        transaction.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());

        long csvImportHistoryId = rs.getLong("csv_import_history_id");
        transaction.setCsvImportHistoryId(rs.wasNull() ? null : csvImportHistoryId);

        transaction.setUpdateCount(rs.getLong("update_count"));
        transaction.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());

        String splitType = rs.getString("split_method");
        transaction.setSplitMethod(splitType != null ? SplitMethod.valueOf(splitType) : null);

        return transaction;
    };

    private static final MonthlySummaryRowMapper MONTHLY_SUMMARY_ROW_MAPPER = new MonthlySummaryRowMapper();

    private static final TransactionEntryRowMapper BASE_MAPPER = new TransactionEntryRowMapper();

    private static final RowMapper<TransactionEntry> TRANSACTION_ENTRY_ROW_MAPPER = (rs, rowNum) -> {
        TransactionEntry transactionEntry = BASE_MAPPER.mapRow(rs, rowNum);
        transactionEntry.setBankTransactionId(rs.getLong("id"));
        transactionEntry.setTransactionSource(TransactionSource.BANK.getSourceName());
        return transactionEntry;
    };

    private final NamedParameterJdbcTemplate template;

    public BankTransactionRepository(NamedParameterJdbcTemplate template) {
        this.template = template;
    }
    
    /**
     * ID で銀行取引を検索する。
     *
     * @param id 銀行取引 ID
     * @param userId ユーザー ID
     * @return 見つかれば取引を含む Optional、なければ空
     */
    public Optional<BankTransaction> findById(Long id, Long userId) {
        String sql = "SELECT id, description, amount, transaction_type, transaction_date, fingerprint, " +
                    "user_id, asset_category_id, expense_category_id, split_pattern_id, split_method, transaction_source, " +
                    "is_card_payment, created_at, csv_import_history_id, update_count, updated_at " +
                    "FROM bank_transactions " +
                    "WHERE id = :id AND user_id = :userId";
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("userId", userId);
        List<BankTransaction> transactions = template.query(sql, params, ROW_MAPPER);
        if (transactions.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(transactions.get(0));
    }
    
    /**
     * フィンガープリントごとの登録件数を集計する（重複チェック用）。
     *
     * @param userId ユーザー ID
     * @return フィンガープリント → 件数の Map
     */
    public Map<String, Long> countByFingerprintGrouped(Long userId) {
        String sql = "SELECT fingerprint, COUNT(fingerprint) as count " +
                    "FROM bank_transactions " +
                    "WHERE user_id = :userId " +
                    "GROUP BY fingerprint";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("userId", userId);
        List<Map<String, Object>> results = template.queryForList(sql, params);
        Map<String, Long> fingerprintCounts = new HashMap<>();
        
        for (Map<String, Object> row : results) {
            String fingerprint = (String) row.get("fingerprint");
            Long count = ((Number) row.get("count")).longValue();
            fingerprintCounts.put(fingerprint, count);
        }
        
        return fingerprintCounts;
    }
    
    /**
     * 未分配の銀行取引を取引種別で取得する（カード引き落としは除外）。
     *
     * @param userId ユーザー ID
     * @param transactionType 収入／支出
     * @return 銀行取引のリスト（取引日降順）
     */
    public List<BankTransaction> findByUserIdAndSplitPatternIdIsNullAndTransactionType(
            Long userId, TransactionType transactionType) {
        String sql = "SELECT id, description, amount, transaction_type, transaction_date, fingerprint, user_id, asset_category_id, expense_category_id, split_pattern_id, split_method, " +
                    "transaction_source, is_card_payment, created_at, csv_import_history_id, update_count, updated_at " +
                    "FROM bank_transactions " +
                    "WHERE user_id = :userId AND split_pattern_id IS NULL AND split_method IS NULL AND is_card_payment = FALSE AND transaction_type = :transactionType " +
                    "ORDER BY transaction_date DESC";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("userId", userId).addValue("transactionType", transactionType.name());
        return template.query(sql, params, ROW_MAPPER);
    }
    
    /**
     * 期間・取引種別で銀行取引をページング取得する。
     *
     * @param userId ユーザー ID
     * @param transactionType 収入／支出
     * @param startDate 開始日（含む）
     * @param endDate 終了日（含む）
     * @param offset オフセット
     * @param limit 取得件数上限
     * @return 銀行取引のリスト（取引日降順）
     */
    public List<BankTransaction> findByUserIdAndTransactionTypeAndTransactionDateBetween(
            Long userId, TransactionType transactionType, LocalDate startDate, LocalDate endDate, 
            int offset, int limit) {
        String sql = "SELECT id, description, amount, transaction_type, transaction_date, fingerprint, user_id, asset_category_id, expense_category_id, split_pattern_id, split_method, " +
                    "transaction_source, is_card_payment, created_at, csv_import_history_id, update_count, updated_at " +
                    "FROM bank_transactions " +
                    "WHERE user_id = :userId AND transaction_type = :transactionType AND transaction_date BETWEEN :startDate AND :endDate " +
                    "ORDER BY transaction_date DESC " +
                    "LIMIT :limit OFFSET :offset";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("userId", userId).addValue("transactionType", transactionType.name())
                        .addValue("startDate", startDate).addValue("endDate", endDate).addValue("limit", limit).addValue("offset", offset);
        return template.query(sql, params, ROW_MAPPER);
    }
    
    /**
     * ユーザーが持つ摘要の重複なし一覧を取得する（自動ルール登録用）。
     *
     * @param userId ユーザー ID
     * @return 摘要文字列のリスト（昇順）
     */
    public List<String> findDistinctDescriptionsByUserId(Long userId) {
        String sql = "SELECT DISTINCT description FROM bank_transactions " +
                    "WHERE user_id = :userId AND description IS NOT NULL " +
                    "ORDER BY description";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("userId", userId);
        return template.queryForList(sql, params, String.class);
    }

    /**
     * 銀行取引を一括登録する。
     *
     * @param transactionList 登録する取引のリスト
     */
    public void insert(List<BankTransaction> transactionList) {
        String sql = "INSERT INTO bank_transactions " +
                "(description, amount, transaction_type, transaction_date, fingerprint, user_id, asset_category_id, " +
                "expense_category_id, split_pattern_id, split_method, transaction_source, is_card_payment, csv_import_history_id, update_count) " +
                "VALUES (:description, :amount, :transactionType, :transactionDate, :fingerprint, :userId, :assetCategoryId, :expenseCategoryId, :splitPatternId, " +
                ":splitMethod, :transactionSource, :isCardPayment, :csvImportHistoryId, :updateCount)";

        SqlParameterSource[] params = new SqlParameterSource[transactionList.size()];
        for (int i = 0; i < transactionList.size(); i++){
            BankTransaction transaction = transactionList.get(i);
            params[i] = new MapSqlParameterSource()
                .addValue("description", transaction.getDescription())
                .addValue("amount", transaction.getAmount())
                .addValue("transactionType", transaction.getTransactionType().name())
                .addValue("transactionDate", transaction.getTransactionDate())
                .addValue("fingerprint", transaction.getFingerprint())
                .addValue("userId", transaction.getUserId())
                .addValue("assetCategoryId", transaction.getAssetCategoryId())
                .addValue("expenseCategoryId", transaction.getExpenseCategoryId())
                .addValue("splitPatternId", transaction.getSplitPatternId())
                .addValue("splitMethod", transaction.getSplitMethod() != null ? transaction.getSplitMethod().name() : null)
                .addValue("transactionSource", transaction.getTransactionSource())
                .addValue("isCardPayment", transaction.getIsCardPayment())
                .addValue("csvImportHistoryId", transaction.getCsvImportHistoryId())
                .addValue("updateCount", 0L);
        }

        template.batchUpdate(sql, params);
    }
    
    /**
     * 資産カテゴリ・支出カテゴリ・分割パターン・分割方法を更新する。
     *
     * @param transaction 更新内容を持つ銀行取引（id・userId 必須）
     */
    public void updateAssetCategoryOrSplitPatternOrExpenseCategory(BankTransaction transaction) {
        String sql = "UPDATE bank_transactions SET " +
                    "asset_category_id = :assetCategoryId, expense_category_id = :expenseCategoryId, " +
                    "split_pattern_id = :splitPatternId, split_method = :splitMethod " +
                    "WHERE id = :id AND user_id = :userId";
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("assetCategoryId", transaction.getAssetCategoryId())
            .addValue("expenseCategoryId", transaction.getExpenseCategoryId())
            .addValue("splitPatternId", transaction.getSplitPatternId())
            .addValue("splitMethod", transaction.getSplitMethod() != null ? transaction.getSplitMethod().name() : null)
            .addValue("id", transaction.getId())
            .addValue("userId", transaction.getUserId());

        template.update(sql, params);
    }

    /**
     * CSV インポート履歴 ID に紐づく銀行取引一覧を取得する。
     *
     * @param userId ユーザー ID
     * @param csvImportHistoryId CSV インポート履歴 ID
     * @return 銀行取引のリスト（取引日降順）
     */
    public List<BankTransaction> findByCsvImportHistoryId(Long userId, Long csvImportHistoryId) {
        String sql = "SELECT id, description, amount, transaction_type, transaction_date, fingerprint, " +
                "user_id, asset_category_id, expense_category_id, split_pattern_id, split_method, transaction_source, " +
                "is_card_payment, created_at, csv_import_history_id, update_count, updated_at " +
                "FROM bank_transactions " +
                "WHERE user_id = :userId AND csv_import_history_id = :csvImportHistoryId " +
                "ORDER BY transaction_date DESC";
        MapSqlParameterSource param = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("csvImportHistoryId", csvImportHistoryId);
        
        return template.query(sql, param, ROW_MAPPER);
    }

    /**
     * CSV インポート履歴に紐づく銀行取引をすべて削除する。
     *
     * @param userId ユーザー ID
     * @param csvImportHistoryId CSV インポート履歴 ID
     */
    public void deleteByCsvImportHistoryId(Long userId, Long csvImportHistoryId) {
        String sql = "DELETE FROM bank_transactions WHERE user_id = :userId AND csv_import_history_id = :csvImportHistoryId";
        MapSqlParameterSource param = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("csvImportHistoryId", csvImportHistoryId);
        
        template.update(sql, param);
    }

    /**
     * 期間・取引種別で銀行取引を {@link TransactionEntry} として取得する（カード引き落としは除外）。
     *
     * @param userId ユーザー ID
     * @param transactionType 収入／支出
     * @param startDate 開始日（含む）
     * @param endDate 終了日（含む）
     * @return 取引エントリのリスト
     */
    public List<TransactionEntry> findByUserIdAndTransactionDate(Long userId, TransactionType transactionType, LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT id, description, amount, transaction_type, transaction_date, fingerprint, " +
                "user_id, asset_category_id, expense_category_id, split_pattern_id, split_method, transaction_source, " +
                "is_card_payment, created_at, csv_import_history_id, update_count, updated_at " +
                "FROM bank_transactions " +
                "WHERE user_id = :userId AND transaction_type = :transactionType " +
                "AND transaction_date BETWEEN :startDate AND :endDate " +
                "AND is_card_payment = FALSE";
        return template.query(sql, makeParam(userId, transactionType, startDate, endDate), TRANSACTION_ENTRY_ROW_MAPPER);
    }

    /**
     * 期間・取引種別で、分割方法が SINGLE かつ指定した資産カテゴリの銀行取引を取得する。
     *
     * @param userId ユーザー ID
     * @param transactionType 収入／支出
     * @param startDate 開始日（含む）
     * @param endDate 終了日（含む）
     * @param assetCategoryId 資産カテゴリ ID
     * @return 取引エントリのリスト
     */
    public List<TransactionEntry> findByUserIdAndTransactionDateAndSplitMethodIsSingle(Long userId, TransactionType transactionType,
            LocalDate startDate, LocalDate endDate, Long assetCategoryId) {
        String sql = "SELECT id, description, amount, transaction_type, transaction_date, fingerprint, " +
                "user_id, asset_category_id, expense_category_id, split_pattern_id, split_method, transaction_source, " +
                "is_card_payment, created_at, csv_import_history_id, update_count, updated_at " +
                "FROM bank_transactions " +
                "WHERE user_id = :userId AND transaction_type = :transactionType " +
                "AND transaction_date BETWEEN :startDate AND :endDate " +
                "AND split_method = 'SINGLE' " +
                "AND is_card_payment = FALSE " +
                "AND asset_category_id = :assetCategoryId";
        MapSqlParameterSource param = makeParam(userId, transactionType, startDate, endDate)
                .addValue("assetCategoryId", assetCategoryId);
        return template.query(sql, param, TRANSACTION_ENTRY_ROW_MAPPER);
    }

    /**
     * 期間・取引種別で、未分配の銀行取引を取得する。
     *
     * @param userId ユーザー ID
     * @param transactionType 収入／支出
     * @param startDate 開始日（含む）
     * @param endDate 終了日（含む）
     * @return 取引エントリのリスト
     */
    public List<TransactionEntry> findByUserIdAndTransactionDateAndSplitMethodIsNull(Long userId, TransactionType transactionType,
            LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT id, description, amount, transaction_type, transaction_date, fingerprint, " +
                "user_id, asset_category_id, expense_category_id, split_pattern_id, split_method, transaction_source, " +
                "is_card_payment, created_at, csv_import_history_id, update_count, updated_at " +
                "FROM bank_transactions " +
                "WHERE user_id = :userId AND transaction_type = :transactionType " +
                "AND transaction_date BETWEEN :startDate AND :endDate " +
                "AND split_method IS NULL " +
                "AND is_card_payment = FALSE";
        return template.query(sql, makeParam(userId, transactionType, startDate, endDate), TRANSACTION_ENTRY_ROW_MAPPER);
    }

    /**
     * 期間内の銀行取引の収支合計を集計する（分割状態を問わず全件、カード引き落とし除外）。
     *
     * @param userId ユーザー ID
     * @param startDate 開始日（含む）
     * @param endDate 終了日（含む）
     * @return 月次収支サマリ
     */
    public MonthlySummary sumAmountByUserIdAndTransactionDate(Long userId, LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT " +
                "COALESCE(SUM(CASE WHEN transaction_type = 'INCOME' THEN amount END), 0) AS income_amount, " +
                "COALESCE(SUM(CASE WHEN transaction_type = 'EXPENSE' THEN amount END), 0) AS expense_amount " +
                "FROM bank_transactions " +
                "WHERE user_id = :userId " +
                "AND transaction_date BETWEEN :startDate AND :endDate " +
                "AND is_card_payment = FALSE";
        return template.queryForObject(sql, makeParam(userId, startDate, endDate), MONTHLY_SUMMARY_ROW_MAPPER);
    }

    /**
     * 期間内の銀行取引のうち、分割方法が SINGLE かつ指定した資産カテゴリの収支合計を集計する（カード引き落とし除外）。
     *
     * @param userId ユーザー ID
     * @param startDate 開始日（含む）
     * @param endDate 終了日（含む）
     * @param assetCategoryId 資産カテゴリ ID
     * @return 月次収支サマリ
     */
    public MonthlySummary sumSingleAmountByAssetCategoryId(Long userId, LocalDate startDate, LocalDate endDate, Long assetCategoryId) {
        String sql = "SELECT " +
                "COALESCE(SUM(CASE WHEN transaction_type = 'INCOME' THEN amount END), 0) AS income_amount, " +
                "COALESCE(SUM(CASE WHEN transaction_type = 'EXPENSE' THEN amount END), 0) AS expense_amount " +
                "FROM bank_transactions " +
                "WHERE user_id = :userId " +
                "AND transaction_date BETWEEN :startDate AND :endDate " +
                "AND is_card_payment = FALSE " +
                "AND split_method = 'SINGLE' " +
                "AND asset_category_id = :assetCategoryId";
        MapSqlParameterSource param = makeParam(userId, startDate, endDate)
                .addValue("assetCategoryId", assetCategoryId);
        return template.queryForObject(sql, param, MONTHLY_SUMMARY_ROW_MAPPER);
    }

    /**
     * 期間内の銀行取引を資産カテゴリ別に集計する（SINGLE、カード引き落とし除外）。
     *
     * @param userId ユーザー ID
     * @param startDate 開始日（含む）
     * @param endDate 終了日（含む）
     * @return 資産カテゴリ ID → 月次サマリの Map
     */
    public Map<Long, MonthlySummary> sumMonthlySummaryByAssetCategory(Long userId, LocalDate startDate,
            LocalDate endDate) {
        String sql = "SELECT asset_category_id, " +
                "COALESCE(SUM(CASE WHEN transaction_type = 'INCOME' THEN amount END), 0) AS income_amount, " +
                "COALESCE(SUM(CASE WHEN transaction_type = 'EXPENSE' THEN amount END), 0) AS expense_amount " +
                "FROM bank_transactions " +
                "WHERE user_id = :userId " +
                "AND transaction_date BETWEEN :startDate AND :endDate " +
                "AND split_method = 'SINGLE' " +
                "AND is_card_payment = FALSE " +
                "GROUP BY asset_category_id";
        return template.query(sql, makeParam(userId, startDate, endDate), MONTHLY_SUMMARY_BY_ASSET_CATEGORY_EXTRACTOR);
    }

    /**
     * 期間内の未分配銀行取引の収支合計を集計する（カード引き落とし除外）。
     *
     * @param userId ユーザー ID
     * @param startDate 開始日（含む）
     * @param endDate 終了日（含む）
     * @return 月次収支サマリ
     */
    public MonthlySummary sumMonthlySummaryBySplitMethodIsNull(Long userId, LocalDate startDate,
            LocalDate endDate) {
        String sql = "SELECT " +
                "COALESCE(SUM(CASE WHEN transaction_type = 'INCOME' THEN amount END), 0) AS income_amount, " +
                "COALESCE(SUM(CASE WHEN transaction_type = 'EXPENSE' THEN amount END), 0) AS expense_amount " +
                "FROM bank_transactions " +
                "WHERE user_id = :userId " +
                "AND transaction_date BETWEEN :startDate AND :endDate " +
                "AND split_method IS NULL " +
                "AND is_card_payment = FALSE";
        return template.queryForObject(sql, makeParam(userId, startDate, endDate), MONTHLY_SUMMARY_ROW_MAPPER);
    }

    private MapSqlParameterSource makeParam(Long userId, LocalDate startDate, LocalDate endDate) {
        MapSqlParameterSource param = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("startDate", startDate)
                .addValue("endDate", endDate);
        return param;
    }

    private MapSqlParameterSource makeParam(Long userId, TransactionType transactionType, LocalDate startDate, LocalDate endDate) {
        MapSqlParameterSource param = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("transactionType", transactionType.name())
                .addValue("startDate", startDate)
                .addValue("endDate", endDate);
        return param;
    }
}
