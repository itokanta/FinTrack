package com.example.fintrack.repository;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import org.springframework.jdbc.core.RowMapper;

import com.example.fintrack.domain.CardTransaction;
import com.example.fintrack.domain.SplitMethod;
import com.example.fintrack.domain.TransactionType;
import com.example.fintrack.domain.dto.CardTransactionUpdateDetail;
import com.example.fintrack.domain.dto.MonthlySummary;
import com.example.fintrack.repository.extractor.CardTransactionUpdateDetailExtractor;
import com.example.fintrack.repository.extractor.MonthlySummaryByAssetCategoryExtractor;

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

    private final NamedParameterJdbcTemplate template;

    public CardTransactionRepository(NamedParameterJdbcTemplate template) {
        this.template = template;
    }
    
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
     * fingerprint全件を取得
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
    
    public List<String> findDistinctDescriptionsByUserId(Long userId) {
        String sql = "SELECT DISTINCT description FROM card_transactions " +
                    "WHERE user_id = :userId AND description IS NOT NULL " +
                    "ORDER BY description";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("userId", userId);
        return template.queryForList(sql, params, String.class);
    }

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
     * 指定した支払い月のクレジットカード取引を取得（デビットは支払月・is_paid の扱いが異なるため除外）
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
     * 指定した支払い月のカード取引合計金額を取得
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
     * 指定した支払い月のカード取引を引き落とし済みに更新
     */
    public void markAsPaidByPaymentMonth(Long userId, String paymentMonth) {
        String sql = "UPDATE card_transactions SET is_paid = TRUE, update_count = update_count + 1 " +
                    "WHERE user_id = :userId AND payment_month = :paymentMonth AND card_type = 'CREDIT'";
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("paymentMonth", paymentMonth);
        template.update(sql, params);
    }

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

    public void deleteByCsvImportHistoryId(Long userId, Long csvImportHistoryId) {
        String sql = "DELETE FROM card_transactions WHERE user_id = :userId AND csv_import_history_id = :csvImportHistoryId";
        MapSqlParameterSource param = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("csvImportHistoryId", csvImportHistoryId);

        template.update(sql, param);
    }

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

    public Long sumAmountByUserIdAndTransactionDate(Long userId, TransactionType transactionType, LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT COALESCE(SUM(amount), 0) " +
                "FROM card_transactions " +
                "WHERE user_id = :userId AND transaction_type = :transactionType " +
                "AND transaction_date BETWEEN :startDate AND :endDate " +
                "AND (split_method IS NULL OR split_method = 'SINGLE')";
        return template.queryForObject(sql, makeParam(userId, transactionType, startDate, endDate), Long.class);
    }

    public List<CardTransaction> findByUserIdAndTransactionDate(Long userId, TransactionType transactionType,
            LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT id, description, amount, transaction_type, transaction_date, fingerprint, user_id, " +
                "asset_category_id, expense_category_id, split_pattern_id, split_method, transaction_source, card_type, " +
                "payment_month, is_paid, created_at, csv_import_history_id, update_count, updated_at " +
                "WHERE user_id = :userId AND transaction_type = :transactionType " +
                "AND transaction_date BETWEEN :startDate AND :endDate " +
                "AND (split_method IS NULL OR split_method = 'SINGLE')" +
                "ORDER BY amount, transaction_date DESC";
        return template.query(sql, makeParam(userId, transactionType, startDate, endDate), ROW_MAPPER);
    }

    public Map<Long, MonthlySummary> sumMonthlySummaryByAssetCategory(Long userId, LocalDate startDate,
            LocalDate endDate) {
        String sql = "SELECT asset_category_id, " +
                "COALESCE(SUM(CASE WHEN transaction_type = 'INCOME' THEN amount END), 0) AS income_amount, " +
                "COALESCE(SUM(CASE WHEN transaction_type = 'EXPENSE' THEN amount END), 0) AS expense_amount " +
                "FROM card_transactions " +
                "WHERE user_id = :userId " +
                "AND transaction_date BETWEEN :startDate AND :endDate " +
                "AND (split_method IS NULL OR split_method = 'SINGLE') " +
                "GROUP BY asset_category_id";
        MapSqlParameterSource param = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("startDate", startDate)
                .addValue("endDate", endDate);
        return template.query(sql, param, MONTHLY_SUMMARY_BY_ASSET_CATEGORY_EXTRACTOR);
    }

    private MapSqlParameterSource makeParam(Long userId, TransactionType transactionType,
            LocalDate startDate, LocalDate endDate) {
        MapSqlParameterSource param = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("transactionType", transactionType.name())
                .addValue("startDate", startDate)
                .addValue("endDate", endDate);
        return param;
    }
}
