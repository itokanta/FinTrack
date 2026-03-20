package com.example.fintrack.repository;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.fintrack.domain.CardTransaction;
import com.example.fintrack.domain.TransactionType;
import com.example.fintrack.repository.mapper.CardTransactionRowMapper;

@Repository
public class CardTransactionRepository {
    
    private final NamedParameterJdbcTemplate template;
    private final CardTransactionRowMapper rowMapper = new CardTransactionRowMapper();
    
    public CardTransactionRepository(NamedParameterJdbcTemplate template) {
        this.template = template;
    }
    
    public Optional<CardTransaction> findById(Long id) {
        String sql = "SELECT id, description, amount, transaction_type, transaction_date, fingerprint, user_id, " + 
                    "asset_category_id, expense_category_id, split_pattern_id, transaction_source, card_type, payment_month, is_paid, created_at, csv_import_history_id " +
                    "FROM card_transactions WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("id", id);
        List<CardTransaction> transactions = template.query(sql, params, rowMapper);
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
                    "asset_category_id, expense_category_id, split_pattern_id, transaction_source, card_type, payment_month, is_paid, created_at, csv_import_history_id " +
                    "FROM card_transactions WHERE user_id = :userId ORDER BY created_at ASC";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("userId", userId);
        List<CardTransaction> transactions = template.query(sql, params, rowMapper);
        
        return transactions;
    }
    
    public List<CardTransaction> findBySplitPatternNullAndType(
            Long userId, TransactionType transactionType) {
        String sql = "SELECT id, description, amount, transaction_type, transaction_date, fingerprint, user_id, " +
                    "asset_category_id, expense_category_id, split_pattern_id, transaction_source, card_type, payment_month, is_paid, created_at, csv_import_history_id " +
                    "FROM card_transactions " +
                    "WHERE user_id = :userId AND split_pattern_id IS NULL AND transaction_type = :transactionType " +
                    "ORDER BY transaction_date DESC";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("userId", userId).addValue("transactionType", transactionType.name());
        return template.query(sql, params, rowMapper);
    }
        
    public List<CardTransaction> findByUserIdAndTransactionTypeAndTransactionDateBetween(
            Long userId, TransactionType transactionType, LocalDate startDate, LocalDate endDate,
            int offset, int limit) {
        String sql = "SELECT id, description, amount, transaction_type, transaction_date, fingerprint, user_id, " +
                    "asset_category_id, expense_category_id, split_pattern_id, transaction_source, card_type, payment_month, is_paid, created_at, csv_import_history_id " +
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
        return template.query(sql, params, rowMapper);
    }
    
    public List<String> findDistinctDescriptionsByUserId(Long userId) {
        String sql = "SELECT DISTINCT description FROM card_transactions " +
                    "WHERE user_id = :userId AND description IS NOT NULL " +
                    "ORDER BY description";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("userId", userId);
        return template.queryForList(sql, params, String.class);
    }
    
    public CardTransaction save(CardTransaction transaction) {
        Long splitPatternId;
        if (transaction.getSplitPatternId() != null) {
            splitPatternId = transaction.getSplitPatternId();
        } else {
            splitPatternId = transaction.getIncomeSplitPatternId();
        }

        if (transaction.getId() == null) {
            // INSERT
            String sql = "INSERT INTO card_transactions " +
                        "(description, amount, transaction_type, transaction_date, fingerprint, user_id, " +
                        "asset_category_id, expense_category_id, split_pattern_id, transaction_source, card_type, payment_month, is_paid, csv_import_history_id) " +
                        "VALUES (:description, :amount, :transactionType, :transactionDate, :fingerprint, :userId, " +
                        ":assetCategoryId, :expenseCategoryId, :splitPatternId, :transactionSource, :cardType, :paymentMonth, :isPaid, :csvImportHistoryId) RETURNING id, created_at";
            MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("description", transaction.getDescription())
                .addValue("amount", transaction.getAmount())
                .addValue("transactionType", transaction.getTransactionType().name())
                .addValue("transactionDate", transaction.getTransactionDate())
                .addValue("fingerprint", transaction.getFingerprint())
                .addValue("userId", transaction.getUserId())
                .addValue("assetCategoryId", transaction.getAssetCategoryId())
                .addValue("expenseCategoryId", transaction.getExpenseCategoryId())
                .addValue("splitPatternId", splitPatternId)
                .addValue("transactionSource", transaction.getTransactionSource())
                .addValue("cardType", transaction.getCardType())
                .addValue("paymentMonth", transaction.getPaymentMonth())
                .addValue("isPaid", transaction.getIsPaid())
                .addValue("csvImportHistoryId", transaction.getCsvImportHistoryId());
            Map<String, Object> result = template.queryForMap(sql, params);
            transaction.setId((Long)result.get("id"));
            transaction.setCreatedAt(((Timestamp)result.get("created_at")).toLocalDateTime());
        } else {
            // UPDATE
            String sql = "UPDATE card_transactions SET " +
                        "description = :description, amount = :amount, transaction_type = :transactionType, " +
                        "transaction_date = :transactionDate, fingerprint = :fingerprint, user_id = :userId, " +
                        "asset_category_id = :assetCategoryId, expense_category_id = :expenseCategoryId, split_pattern_id = :splitPatternId, " +
                        "transaction_source = :transactionSource, card_type = :cardType, payment_month = :paymentMonth, is_paid = :isPaid " +
                        "WHERE id = :id";
            MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("description", transaction.getDescription())
                .addValue("amount", transaction.getAmount())
                .addValue("transactionType", transaction.getTransactionType().name())
                .addValue("transactionDate", transaction.getTransactionDate())
                .addValue("fingerprint", transaction.getFingerprint())
                .addValue("userId", transaction.getUserId())
                .addValue("assetCategoryId", transaction.getAssetCategoryId())
                .addValue("expenseCategoryId", transaction.getExpenseCategoryId())
                .addValue("splitPatternId", splitPatternId)
                .addValue("transactionSource", transaction.getTransactionSource())
                .addValue("cardType", transaction.getCardType())
                .addValue("paymentMonth", transaction.getPaymentMonth())
                .addValue("isPaid", transaction.getIsPaid())
                .addValue("id", transaction.getId());
            template.update(sql, params);
        }
        return transaction;
    }

    /**
     * 指定した支払い月のカード取引を取得
     */
    public List<CardTransaction> findByUserIdAndPaymentMonth(Long userId, String paymentMonth) {
        String sql = "SELECT id, description, amount, transaction_type, transaction_date, fingerprint, user_id, " +
                "asset_category_id, expense_category_id, split_pattern_id, transaction_source, card_type, payment_month, is_paid, created_at, csv_import_history_id " +
                "FROM card_transactions " +
                "WHERE user_id = :userId AND payment_month = :paymentMonth " +
                "ORDER BY transaction_date DESC";
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("paymentMonth", paymentMonth);
        return template.query(sql, params, rowMapper);
    }

    /**
     * 指定した支払い月のカード取引合計金額を取得
     */
    public Long sumAmountByUserIdAndPaymentMonth(Long userId, String paymentMonth) {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM card_transactions " +
                "WHERE user_id = :userId AND payment_month = :paymentMonth AND transaction_type = 'EXPENSE'";
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("paymentMonth", paymentMonth);
        return template.queryForObject(sql, params, Long.class);
    }

    /**
     * 指定した支払い月のカード取引を引き落とし済みに更新
     */
    public void markAsPaidByPaymentMonth(Long userId, String paymentMonth) {
        String sql = "UPDATE card_transactions SET is_paid = TRUE " +
                    "WHERE user_id = :userId AND payment_month = :paymentMonth AND card_type = 'CREDIT'";
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("paymentMonth", paymentMonth);
        template.update(sql, params);
    }
}
