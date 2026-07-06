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

import com.example.fintrack.domain.CardTransaction;
import com.example.fintrack.domain.SplitMethod;
import com.example.fintrack.domain.TransactionSource;
import com.example.fintrack.domain.TransactionType;
import com.example.fintrack.domain.dto.CardTransactionUpdateDetail;
import com.example.fintrack.domain.dto.MonthlySummary;
import com.example.fintrack.domain.dto.TransactionEntry;
import com.example.fintrack.repository.extractor.CardTransactionUpdateDetailExtractor;
import com.example.fintrack.repository.extractor.MonthlySummaryByAssetCategoryExtractor;
import com.example.fintrack.repository.rowmapper.MonthlySummaryRowMapper;
import com.example.fintrack.repository.rowmapper.TransactionEntryRowMapper;

/**
 * カード取引（card_transactions テーブル）の DB アクセス。
 *
 * @author kanta ito
 */
@Repository
public class CardTransactionRepository {

    private static final MonthlySummaryByAssetCategoryExtractor MONTHLY_SUMMARY_BY_ASSET_CATEGORY_EXTRACTOR =
            new MonthlySummaryByAssetCategoryExtractor();

    private static final RowMapper<CardTransaction> ROW_MAPPER = (rs, rowNum) -> {
        CardTransaction transaction = new CardTransaction();
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
        transaction.setCardType(rs.getString("card_type"));
        transaction.setPaymentMonth(rs.getString("payment_month"));

        Boolean isPaid = rs.getBoolean("is_paid");
        transaction.setIsPaid(rs.wasNull() ? null : isPaid);

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
        transactionEntry.setCardTransactionId(rs.getLong("id"));
        transactionEntry.setTransactionSource(TransactionSource.valueOf(rs.getString("card_type")).getSourceName());
        return transactionEntry;
    };

    private final NamedParameterJdbcTemplate template;

    public CardTransactionRepository(NamedParameterJdbcTemplate template) {
        this.template = template;
    }
    
    /**
     * ID でカード取引を検索する。
     *
     * @param id カード取引 ID
     * @param userId ユーザー ID
     * @return 見つかれば取引を含む Optional、なければ空
     */
    public Optional<CardTransaction> findById(Long id, Long userId) {
        String sql = "SELECT id, description, amount, transaction_type, transaction_date, fingerprint, user_id, " + 
                    "asset_category_id, expense_category_id, split_pattern_id, split_method, transaction_source, card_type, " +
                    "payment_month, is_paid, created_at, csv_import_history_id, update_count, updated_at " +
                    "FROM card_transactions " +
                    "WHERE id = :id AND user_id = :userId";
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("userId", userId);
        List<CardTransaction> transactions = template.query(sql, params, ROW_MAPPER);
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
                    "FROM card_transactions " +
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
     * ユーザーのカード取引をすべて取得する。
     *
     * @param userId ユーザー ID
     * @return カード取引のリスト（作成日時昇順）
     */
    public List<CardTransaction> findAllByUserId(Long userId) {
        String sql = "SELECT id, description, amount, transaction_type, transaction_date, fingerprint, user_id, " +
                    "asset_category_id, expense_category_id, split_pattern_id, split_method, transaction_source, card_type, payment_month, is_paid, created_at, csv_import_history_id, update_count, updated_at " +
                    "FROM card_transactions " +
                    "WHERE user_id = :userId " +
                    "ORDER BY created_at ASC";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("userId", userId);
        List<CardTransaction> transactions = template.query(sql, params, ROW_MAPPER);
        
        return transactions;
    }
    
    /**
     * 未分配のカード取引を取引種別で取得する。
     *
     * @param userId ユーザー ID
     * @param transactionType 収入／支出
     * @return カード取引のリスト（取引日降順）
     */
    public List<CardTransaction> findBySplitPatternNullAndType(
            Long userId, TransactionType transactionType) {
        String sql = "SELECT id, description, amount, transaction_type, transaction_date, fingerprint, user_id, " +
                    "asset_category_id, expense_category_id, split_pattern_id, split_method, transaction_source, card_type, payment_month, is_paid, created_at, csv_import_history_id, update_count, updated_at " +
                    "FROM card_transactions " +
                    "WHERE user_id = :userId AND split_pattern_id IS NULL AND split_method IS NULL AND transaction_type = :transactionType " +
                    "ORDER BY transaction_date DESC";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("userId", userId).addValue("transactionType", transactionType.name());
        return template.query(sql, params, ROW_MAPPER);
    }
        
    /**
     * 期間・取引種別でカード取引をページング取得する。
     *
     * @param userId ユーザー ID
     * @param transactionType 収入／支出
     * @param startDate 開始日（含む）
     * @param endDate 終了日（含む）
     * @param offset オフセット
     * @param limit 取得件数上限
     * @return カード取引のリスト（取引日降順）
     */
    public List<CardTransaction> findByUserIdAndTransactionTypeAndTransactionDateBetween(
            Long userId, TransactionType transactionType, LocalDate startDate, LocalDate endDate,
            int offset, int limit) {
        String sql = "SELECT id, description, amount, transaction_type, transaction_date, fingerprint, user_id, " +
                    "asset_category_id, expense_category_id, split_pattern_id, split_method, transaction_source, card_type, payment_month, is_paid, created_at, csv_import_history_id, update_count, updated_at " +
                    "FROM card_transactions " +
                    "WHERE user_id = :userId AND transaction_type = :transactionType AND transaction_date BETWEEN :startDate AND :endDate " +
                    "ORDER BY transaction_date DESC " +
                    "LIMIT :limit OFFSET :offset";
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("transactionType", transactionType.name())
            .addValue("startDate", startDate)
            .addValue("endDate", endDate)
            .addValue("limit", limit)
            .addValue("offset", offset);
        return template.query(sql, params, ROW_MAPPER);
    }
    
    /**
     * ユーザーが持つ摘要の重複なし一覧を取得する。
     *
     * @param userId ユーザー ID
     * @return 摘要文字列のリスト（昇順）
     */
    public List<String> findDistinctDescriptionsByUserId(Long userId) {
        String sql = "SELECT DISTINCT description FROM card_transactions " +
                    "WHERE user_id = :userId AND description IS NOT NULL " +
                    "ORDER BY description";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("userId", userId);
        return template.queryForList(sql, params, String.class);
    }

    /**
     * カード取引を一括登録する。
     *
     * @param transactionList 登録する取引のリスト
     */
    public void insert(List<CardTransaction> transactionList) {
        String sql = "INSERT INTO card_transactions " +
                "(description, amount, transaction_type, transaction_date, fingerprint, user_id, asset_category_id, " +
                "expense_category_id, split_pattern_id, split_method, transaction_source, card_type, payment_month, is_paid, csv_import_history_id, update_count) " +
                "VALUES (:description, :amount, :transactionType, :transactionDate, :fingerprint, :userId, :assetCategoryId, " +
                ":expenseCategoryId, :splitPatternId, :splitMethod, :transactionSource, :cardType, :paymentMonth, :isPaid, :csvImportHistoryId, :updateCount)";

        SqlParameterSource[] params = new SqlParameterSource[transactionList.size()];
        for (int i = 0; i < transactionList.size(); i++) {
            CardTransaction transaction = transactionList.get(i);
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
                    .addValue("cardType", transaction.getCardType())
                    .addValue("paymentMonth", transaction.getPaymentMonth())
                    .addValue("isPaid", transaction.getIsPaid())
                    .addValue("csvImportHistoryId", transaction.getCsvImportHistoryId())
                    .addValue("updateCount", 0L);
        }

        template.batchUpdate(sql, params);
    }
    
    /**
     * 資産カテゴリ・支出カテゴリ・分割パターン・分割方法を更新する。
     *
     * @param transaction 更新内容を持つカード取引（id・userId 必須）
     */
    public void updateAssetCategoryOrSplitPatternOrExpenseCategory(CardTransaction transaction) {
            String sql = "UPDATE card_transactions SET " +
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
     * 指定支払月のクレジットカード取引を取得する（デビットは除外）。
     *
     * @param userId ユーザー ID
     * @param paymentMonth 支払月（YYYY-MM 形式）
     * @return カード取引のリスト（取引日降順）
     */
    public List<CardTransaction> findByUserIdAndPaymentMonth(Long userId, String paymentMonth) {
        String sql = "SELECT id, description, amount, transaction_type, transaction_date, fingerprint, user_id, " +
                "asset_category_id, expense_category_id, split_pattern_id, split_method, transaction_source, card_type, payment_month, is_paid, created_at, csv_import_history_id, update_count, updated_at " +
                "FROM card_transactions " +
                "WHERE user_id = :userId AND payment_month = :paymentMonth AND card_type = 'CREDIT' " +
                "ORDER BY transaction_date DESC";
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("paymentMonth", paymentMonth);
        return template.query(sql, params, ROW_MAPPER);
    }

    /**
     * 指定支払月のクレジットカード支出合計を取得する。
     *
     * @param userId ユーザー ID
     * @param paymentMonth 支払月（YYYY-MM 形式）
     * @return 支出合計（該当なしなら 0）
     */
    public Long sumAmountByUserIdAndPaymentMonth(Long userId, String paymentMonth) {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM card_transactions " +
                "WHERE user_id = :userId AND payment_month = :paymentMonth AND transaction_type = 'EXPENSE' AND card_type = 'CREDIT'";
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("paymentMonth", paymentMonth);
        return template.queryForObject(sql, params, Long.class);
    }

    /**
     * 指定支払月のクレジットカード取引を引き落とし済みに更新する。
     *
     * @param userId ユーザー ID
     * @param paymentMonth 支払月（YYYY-MM 形式）
     */
    public void markAsPaidByPaymentMonth(Long userId, String paymentMonth) {
        String sql = "UPDATE card_transactions SET is_paid = TRUE, update_count = update_count + 1 " +
                    "WHERE user_id = :userId AND payment_month = :paymentMonth AND card_type = 'CREDIT'";
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("paymentMonth", paymentMonth);
        template.update(sql, params);
    }

    /**
     * クレジットカード取引の支払月を一括更新する。
     *
     * @param cardTransactionList 更新対象のカード取引リスト
     */
    public void updatePaymentMonth(List<CardTransaction> cardTransactionList) {
        String sql = "UPDATE card_transactions SET payment_month = :paymentMonth, update_count = update_count + 1 " +
                "WHERE id = :id AND user_id = :userId AND card_type = 'CREDIT'";
        SqlParameterSource[] params = new SqlParameterSource[cardTransactionList.size()];
        for (int i = 0; i < cardTransactionList.size(); i++) {
            CardTransaction cardTransaction = cardTransactionList.get(i);
            params[i] = new MapSqlParameterSource()
                .addValue("paymentMonth", cardTransaction.getPaymentMonth())
                .addValue("id", cardTransaction.getId())
                .addValue("userId", cardTransaction.getUserId());
        }
        template.batchUpdate(sql, params);
    }

    /**
     * CSV インポート履歴 ID に紐づくカード取引一覧を取得する。
     *
     * @param userId ユーザー ID
     * @param csvImportHistoryId CSV インポート履歴 ID
     * @return カード取引のリスト（取引日降順）
     */
    public List<CardTransaction> findByCsvImportHistoryId(Long userId, Long csvImportHistoryId) {
        String sql = "SELECT id, description, amount, transaction_type, transaction_date, fingerprint, user_id, " +
                "asset_category_id, expense_category_id, split_pattern_id, split_method, transaction_source, card_type, " +
                "payment_month, is_paid, created_at, csv_import_history_id, update_count, updated_at " +
                "FROM card_transactions " +
                "WHERE user_id = :userId AND csv_import_history_id = :csvImportHistoryId " +
                "ORDER BY transaction_date DESC";
        MapSqlParameterSource param = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("csvImportHistoryId", csvImportHistoryId);

        return template.query(sql, param, ROW_MAPPER);
    }

    /**
     * ID のリストでカード取引を一括取得する。
     *
     * @param ids カード取引 ID のリスト
     * @param userId ユーザー ID
     * @return カード取引のリスト
     */
    public List<CardTransaction> findByIds(List<Long> ids, Long userId) {
        String sql = "SELECT id, description, amount, transaction_type, transaction_date, fingerprint, user_id, " +
                "asset_category_id, expense_category_id, split_pattern_id, split_method, transaction_source, card_type, " +
                "payment_month, is_paid, created_at, csv_import_history_id, update_count, updated_at " +
                "FROM card_transactions " +
                "WHERE id IN (:ids) AND user_id = :userId";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ids", ids)
                .addValue("userId", userId);
        return template.query(sql, params, ROW_MAPPER);
    }

    /**
     * カード取引の is_paid・payment_month を一括ロールバックする。
     *
     * @param cardTransactionList ロールバック後の値を持つカード取引リスト
     */
    public void rollbackIsPaidAndPaymentMonth(List<CardTransaction> cardTransactionList) {
        String sql = "UPDATE card_transactions SET is_paid = :isPaid, payment_month = :paymentMonth " +
                "WHERE id = :id AND user_id = :userId";
        SqlParameterSource[] params = new SqlParameterSource[cardTransactionList.size()];
        for (int i = 0; i < cardTransactionList.size(); i++) {
            CardTransaction cardTransaction = cardTransactionList.get(i);
            params[i] = new MapSqlParameterSource()
                .addValue("isPaid", cardTransaction.getIsPaid())
                .addValue("paymentMonth", cardTransaction.getPaymentMonth())
                .addValue("id", cardTransaction.getId())
                .addValue("userId", cardTransaction.getUserId());
        }
        template.batchUpdate(sql, params);
    }

    /**
     * CSV インポート履歴に紐づくカード取引をすべて削除する。
     *
     * @param userId ユーザー ID
     * @param csvImportHistoryId CSV インポート履歴 ID
     */
    public void deleteByCsvImportHistoryId(Long userId, Long csvImportHistoryId) {
        String sql = "DELETE FROM card_transactions WHERE user_id = :userId AND csv_import_history_id = :csvImportHistoryId";
        MapSqlParameterSource param = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("csvImportHistoryId", csvImportHistoryId);

        template.update(sql, param);
    }

    /**
     * 指定 CSV で更新されたカード取引と更新履歴をまとめて取得する。
     *
     * @param userId ユーザー ID
     * @param csvImportHistoryId CSV インポート履歴 ID
     * @return カード取引更新詳細のリスト
     */
    public List<CardTransactionUpdateDetail> findUpdatedByCsvImportHistoryId(Long userId, Long csvImportHistoryId) {
        String sql =
            "SELECT " +
            "  ct.id AS ct_id, ct.description AS ct_description, ct.amount AS ct_amount, " +
            "  ct.transaction_type AS ct_transaction_type, ct.transaction_date AS ct_transaction_date, " +
            "  ct.fingerprint AS ct_fingerprint, ct.user_id AS ct_user_id, " +
            "  ct.asset_category_id AS ct_asset_category_id, ct.expense_category_id AS ct_expense_category_id, " +
            "  ct.split_pattern_id AS ct_split_pattern_id, ct.transaction_source AS ct_transaction_source, " +
            "  ct.card_type AS ct_card_type, ct.payment_month AS ct_payment_month, " +
            "  ct.is_paid AS ct_is_paid, ct.created_at AS ct_created_at, " +
            "  ct.csv_import_history_id AS ct_csv_import_history_id, " +
            "  ct.update_count AS ct_update_count, ct.updated_at AS ct_updated_at, " +
            "  cuh.id AS cuh_id, cuh.csv_import_history_id AS cuh_csv_import_history_id, " +
            "  cuh.bank_transaction_id AS cuh_bank_transaction_id, " +
            "  cuh.card_transaction_id AS cuh_card_transaction_id, " +
            "  cuh.target_field AS cuh_target_field, cuh.value_before AS cuh_value_before, " +
            "  cuh.value_after AS cuh_value_after, cuh.user_id AS cuh_user_id, " +
            "  cuh.update_count AS cuh_update_count, cuh.created_at AS cuh_created_at, " +
            "  cuh.updated_at AS cuh_updated_at " +
            "FROM card_transactions ct " +
            "INNER JOIN csv_import_update_histories cuh ON ct.id = cuh.card_transaction_id " +
            "WHERE cuh.csv_import_history_id = :csvImportHistoryId " +
            "  AND ct.user_id = :userId " +
            "ORDER BY ct.transaction_date DESC, cuh.update_count";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("csvImportHistoryId", csvImportHistoryId)
                .addValue("userId", userId);
        return template.query(sql, params, new CardTransactionUpdateDetailExtractor());
    }

    /**
     * 期間・取引種別でカード取引を {@link TransactionEntry} として取得する。
     *
     * @param userId ユーザー ID
     * @param transactionType 収入／支出
     * @param startDate 開始日（含む）
     * @param endDate 終了日（含む）
     * @return 取引エントリのリスト
     */
    public List<TransactionEntry> findByUserIdAndTransactionDate(Long userId, TransactionType transactionType, LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT id, description, amount, transaction_type, transaction_date, fingerprint, user_id, " +
                "asset_category_id, expense_category_id, split_pattern_id, split_method, transaction_source, card_type, " +
                "payment_month, is_paid, created_at, csv_import_history_id, update_count, updated_at " +
                "FROM card_transactions " +
                "WHERE user_id = :userId AND transaction_type = :transactionType " +
                "AND transaction_date BETWEEN :startDate AND :endDate";
        return template.query(sql, makeParam(userId, transactionType, startDate, endDate), TRANSACTION_ENTRY_ROW_MAPPER);
    }

    /**
     * 期間・取引種別で、分割方法が SINGLE のカード取引を取得する。
     *
     * @param userId ユーザー ID
     * @param transactionType 収入／支出
     * @param startDate 開始日（含む）
     * @param endDate 終了日（含む）
     * @return 取引エントリのリスト
     */
    public List<TransactionEntry> findByUserIdAndTransactionDateAndSplitMethodIsSingle(Long userId, TransactionType transactionType,
            LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT id, description, amount, transaction_type, transaction_date, fingerprint, user_id, " +
                "asset_category_id, expense_category_id, split_pattern_id, split_method, transaction_source, card_type, " +
                "payment_month, is_paid, created_at, csv_import_history_id, update_count, updated_at " +
                "FROM card_transactions " +
                "WHERE user_id = :userId AND transaction_type = :transactionType " +
                "AND transaction_date BETWEEN :startDate AND :endDate " +
                "AND split_method = 'SINGLE'";
        return template.query(sql, makeParam(userId, transactionType, startDate, endDate), TRANSACTION_ENTRY_ROW_MAPPER);
    }

    /**
     * 期間・取引種別で、未分配のカード取引を取得する。
     *
     * @param userId ユーザー ID
     * @param transactionType 収入／支出
     * @param startDate 開始日（含む）
     * @param endDate 終了日（含む）
     * @return 取引エントリのリスト
     */
    public List<TransactionEntry> findByUserIdAndTransactionDateAndSplitMethodIsNull(Long userId, TransactionType transactionType,
            LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT id, description, amount, transaction_type, transaction_date, fingerprint, user_id, " +
                "asset_category_id, expense_category_id, split_pattern_id, split_method, transaction_source, card_type, " +
                "payment_month, is_paid, created_at, csv_import_history_id, update_count, updated_at " +
                "FROM card_transactions " +
                "WHERE user_id = :userId AND transaction_type = :transactionType " +
                "AND transaction_date BETWEEN :startDate AND :endDate " +
                "AND split_method IS NULL";
        return template.query(sql, makeParam(userId, transactionType, startDate, endDate), TRANSACTION_ENTRY_ROW_MAPPER);
    }

    /**
     * 期間内のカード取引の収支合計を集計する（未分配・SINGLE のみ）。
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
                "FROM card_transactions " +
                "WHERE user_id = :userId " +
                "AND transaction_date BETWEEN :startDate AND :endDate " +
                "AND (split_method IS NULL OR split_method = 'SINGLE')";
        return template.queryForObject(sql, makeParam(userId, startDate, endDate), MONTHLY_SUMMARY_ROW_MAPPER);
    }

    /**
     * 期間内のカード取引を資産カテゴリ別に集計する（SINGLE のみ）。
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
                "FROM card_transactions " +
                "WHERE user_id = :userId " +
                "AND transaction_date BETWEEN :startDate AND :endDate " +
                "AND split_method = 'SINGLE' " +
                "GROUP BY asset_category_id";
        return template.query(sql, makeParam(userId, startDate, endDate), MONTHLY_SUMMARY_BY_ASSET_CATEGORY_EXTRACTOR);
    }

    /**
     * 期間内の未分配カード取引の収支合計を集計する。
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
                "FROM card_transactions " +
                "WHERE user_id = :userId " +
                "AND transaction_date BETWEEN :startDate AND :endDate " +
                "AND split_method IS NULL";
        return template.queryForObject(sql, makeParam(userId, startDate, endDate), MONTHLY_SUMMARY_ROW_MAPPER);
    }

    private MapSqlParameterSource makeParam(Long userId, LocalDate startDate, LocalDate endDate) {
        MapSqlParameterSource param = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("startDate", startDate)
                .addValue("endDate", endDate);
        return param;
    }
    
    private MapSqlParameterSource makeParam(Long userId, TransactionType transactionType, LocalDate startDate,
            LocalDate endDate) {
        MapSqlParameterSource param = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("transactionType", transactionType.name())
                .addValue("startDate", startDate)
                .addValue("endDate", endDate);
        return param;
    }
}
