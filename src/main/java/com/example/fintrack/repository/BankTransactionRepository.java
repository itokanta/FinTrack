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
import com.example.fintrack.domain.BankTransaction;
import com.example.fintrack.domain.TransactionType;
import com.example.fintrack.repository.mapper.BankTransactionRowMapper;

@Repository
public class BankTransactionRepository {
    
    private final NamedParameterJdbcTemplate template;
    private final BankTransactionRowMapper rowMapper = new BankTransactionRowMapper();
    
    public BankTransactionRepository(NamedParameterJdbcTemplate template) {
        this.template = template;
    }
    
    public Optional<BankTransaction> findById(Long id) {
        String sql = "SELECT id, description, amount, transaction_type, transaction_date, fingerprint, " +
                    "user_id, asset_category_id, expense_category_id, split_pattern_id, transaction_source, " +
                    "is_card_payment, created_at, csv_import_history_id, update_count, updated_at " +
                    "FROM bank_transactions " +
                    "WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("id", id);
        List<BankTransaction> transactions = template.query(sql, params, rowMapper);
        if (transactions.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(transactions.get(0));
    }
    
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
    
    public List<BankTransaction> findByUserIdAndSplitPatternIdIsNullAndTransactionType(
            Long userId, TransactionType transactionType) {
        String sql = "SELECT id, description, amount, transaction_type, transaction_date, fingerprint, user_id, asset_category_id, expense_category_id, split_pattern_id, " +
                    "transaction_source, is_card_payment, created_at, csv_import_history_id, update_count, updated_at " +
                    "FROM bank_transactions " +
                    "WHERE user_id = :userId AND split_pattern_id IS NULL AND is_card_payment = FALSE AND transaction_type = :transactionType " +
                    "ORDER BY transaction_date DESC";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("userId", userId).addValue("transactionType", transactionType.name());
        return template.query(sql, params, rowMapper);
    }
    
    public List<BankTransaction> findByUserIdAndTransactionTypeAndTransactionDateBetween(
            Long userId, TransactionType transactionType, LocalDate startDate, LocalDate endDate, 
            int offset, int limit) {
        String sql = "SELECT id, description, amount, transaction_type, transaction_date, fingerprint, user_id, asset_category_id, expense_category_id, split_pattern_id, " +
                    "transaction_source, is_card_payment, created_at, csv_import_history_id, update_count, updated_at " +
                    "FROM bank_transactions " +
                    "WHERE user_id = :userId AND transaction_type = :transactionType AND transaction_date BETWEEN :startDate AND :endDate " +
                    "ORDER BY transaction_date DESC " +
                    "LIMIT :limit OFFSET :offset";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("userId", userId).addValue("transactionType", transactionType.name())
                        .addValue("startDate", startDate).addValue("endDate", endDate).addValue("limit", limit).addValue("offset", offset);
        return template.query(sql, params, rowMapper);
    }
    
    public List<String> findDistinctDescriptionsByUserId(Long userId) {
        String sql = "SELECT DISTINCT description FROM bank_transactions " +
                    "WHERE user_id = :userId AND description IS NOT NULL " +
                    "ORDER BY description";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("userId", userId);
        return template.queryForList(sql, params, String.class);
    }

    public BankTransaction insert(BankTransaction transaction) {
        String sql = "INSERT INTO bank_transactions " +
                "(description, amount, transaction_type, transaction_date, fingerprint, user_id, asset_category_id, " +
                "expense_category_id, split_pattern_id, transaction_source, is_card_payment, csv_import_history_id, update_count) " +
                "VALUES (:description, :amount, :transactionType, :transactionDate, :fingerprint, :userId, :assetCategoryId, :expenseCategoryId, :splitPatternId, " +
                ":transactionSource, :isCardPayment, :csvImportHistoryId, :updateCount) RETURNING id, created_at, updated_at";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("description", transaction.getDescription())
                .addValue("amount", transaction.getAmount())
                .addValue("transactionType", transaction.getTransactionType().name())
                .addValue("transactionDate", transaction.getTransactionDate())
                .addValue("fingerprint", transaction.getFingerprint())
                .addValue("userId", transaction.getUserId())
                .addValue("assetCategoryId", transaction.getAssetCategoryId())
                .addValue("expenseCategoryId", transaction.getExpenseCategoryId())
                .addValue("splitPatternId", transaction.getSplitPatternId())
                .addValue("transactionSource", transaction.getTransactionSource())
                .addValue("isCardPayment", transaction.getIsCardPayment())
                .addValue("csvImportHistoryId", transaction.getCsvImportHistoryId())
                .addValue("updateCount", 0L);

        Map<String, Object> result = template.queryForMap(sql, params);
        transaction.setId(((Number) result.get("id")).longValue());
        transaction.setCreatedAt(((Timestamp) result.get("created_at")).toLocalDateTime());
        transaction.setUpdatedAt(((Timestamp) result.get("updated_at")).toLocalDateTime());
        return transaction;
    }
    
    public void updateAssetCategoryOrSplitPatternOrExpenseCategory(BankTransaction transaction) {
        String sql = "UPDATE bank_transactions SET " +
                    "asset_category_id = :assetCategoryId, expense_category_id = :expenseCategoryId, split_pattern_id = :splitPatternId " +
                    "WHERE id = :id AND user_id = :userId";
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("assetCategoryId", transaction.getAssetCategoryId())
            .addValue("expenseCategoryId", transaction.getExpenseCategoryId())
            .addValue("splitPatternId", transaction.getSplitPatternId())
            .addValue("id", transaction.getId())
            .addValue("userId", transaction.getUserId());

        template.update(sql, params);
    }

    public List<BankTransaction> findByCsvImportHistoryId(Long userId, Long csvImportHistoryId) {
        String sql = "SELECT id, description, amount, transaction_type, transaction_date, fingerprint, " +
                "user_id, asset_category_id, expense_category_id, split_pattern_id, transaction_source, " +
                "is_card_payment, created_at, csv_import_history_id, update_count, updated_at " +
                "FROM bank_transactions " +
                "WHERE user_id = :userId AND csv_import_history_id = :csvImportHistoryId " +
                "ORDER BY transaction_date DESC";
        MapSqlParameterSource param = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("csvImportHistoryId", csvImportHistoryId);
        
        return template.query(sql, param, rowMapper);
    }

    public void deleteByCsvImportHistoryId(Long userId, Long csvImportHistoryId) {
        String sql = "DELETE FROM bank_transactions WHERE user_id = :userId AND csv_import_history_id = :csvImportHistoryId";
        MapSqlParameterSource param = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("csvImportHistoryId", csvImportHistoryId);
        
        template.update(sql, param);
    }
}
