package com.example.fintrack.repository;

import com.example.fintrack.domain.BankTransaction;
import com.example.fintrack.domain.TransactionType;
import com.example.fintrack.repository.mapper.BankTransactionRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class BankTransactionRepository {
    
    private final JdbcTemplate jdbcTemplate;
    private final BankTransactionRowMapper rowMapper = new BankTransactionRowMapper();
    
    public BankTransactionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    public Optional<BankTransaction> findById(Long id) {
        String sql = "SELECT * FROM bank_transactions WHERE id = ?";
        List<BankTransaction> transactions = jdbcTemplate.query(sql, rowMapper, id);
        if (transactions.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(transactions.get(0));
    }
    
    public boolean existsByFingerprint(String fingerprint) {
        String sql = "SELECT COUNT(*) FROM bank_transactions WHERE fingerprint = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, fingerprint);
        return count != null && count > 0;
    }
    
    public Map<String, Long> countByFingerprintGrouped(Long userId) {
        String sql = "SELECT fingerprint, COUNT(*) as count " +
                    "FROM bank_transactions " +
                    "WHERE user_id = ? " +
                    "GROUP BY fingerprint";
        
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, userId);
        Map<String, Long> fingerprintCounts = new HashMap<>();
        
        for (Map<String, Object> row : results) {
            String fingerprint = (String) row.get("fingerprint");
            Long count = ((Number) row.get("count")).longValue();
            fingerprintCounts.put(fingerprint, count);
        }
        
        return fingerprintCounts;
    }
    
    public List<BankTransaction> findByUserIdAndAssetCategoryIdIsNullAndTransactionType(
            Long userId, TransactionType transactionType) {
        String sql = "SELECT * FROM bank_transactions " +
                    "WHERE user_id = ? AND asset_category_id IS NULL AND transaction_type = ? " +
                    "ORDER BY transaction_date DESC";
        return jdbcTemplate.query(sql, rowMapper, userId, transactionType.name());
    }
    
    public List<BankTransaction> findByUserIdAndIncomeSplitPatternIdIsNullAndTransactionType(
            Long userId, TransactionType transactionType) {
        String sql = "SELECT * FROM bank_transactions " +
                    "WHERE user_id = ? AND split_pattern_id IS NULL AND transaction_type = ? " +
                    "ORDER BY transaction_date DESC";
        return jdbcTemplate.query(sql, rowMapper, userId, transactionType.name());
    }
    
    public List<BankTransaction> findByUserIdAndSplitPatternIdIsNullAndTransactionType(
            Long userId, TransactionType transactionType) {
        String sql = "SELECT * FROM bank_transactions " +
                    "WHERE user_id = ? AND split_pattern_id IS NULL AND transaction_type = ? " +
                    "ORDER BY transaction_date DESC";
        return jdbcTemplate.query(sql, rowMapper, userId, transactionType.name());
    }
    
    public List<BankTransaction> findByUserIdAndSplitPatternIdIsNullAndAssetCategoryIdIsNullAndTransactionType(
            Long userId, TransactionType transactionType) {
        String sql = "SELECT * FROM bank_transactions " +
                    "WHERE user_id = ? AND split_pattern_id IS NULL AND asset_category_id IS NULL AND transaction_type = ? " +
                    "ORDER BY transaction_date DESC";
        return jdbcTemplate.query(sql, rowMapper, userId, transactionType.name());
    }
    
    public List<BankTransaction> findByUserIdAndTransactionDateBetween(
            Long userId, LocalDate startDate, LocalDate endDate, int offset, int limit) {
        String sql = "SELECT * FROM bank_transactions " +
                    "WHERE user_id = ? AND transaction_date BETWEEN ? AND ? " +
                    "ORDER BY transaction_date DESC " +
                    "LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, rowMapper, userId, startDate, endDate, limit, offset);
    }
    
    public List<BankTransaction> findByUserIdAndTransactionTypeAndTransactionDateBetween(
            Long userId, TransactionType transactionType, LocalDate startDate, LocalDate endDate, 
            int offset, int limit) {
        String sql = "SELECT * FROM bank_transactions " +
                    "WHERE user_id = ? AND transaction_type = ? AND transaction_date BETWEEN ? AND ? " +
                    "ORDER BY transaction_date DESC " +
                    "LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, rowMapper, userId, transactionType.name(), startDate, endDate, limit, offset);
    }
    
    public List<String> findDistinctDescriptionsByUserId(Long userId) {
        String sql = "SELECT DISTINCT description FROM bank_transactions " +
                    "WHERE user_id = ? AND description IS NOT NULL " +
                    "ORDER BY description";
        return jdbcTemplate.queryForList(sql, String.class, userId);
    }
    
    public BankTransaction save(BankTransaction transaction) {
        // 後方互換性: incomeSplitPatternIdが設定されている場合はそれを使用
        Long splitPatternId;
        if (transaction.getSplitPatternId() != null) {
            splitPatternId = transaction.getSplitPatternId();
        } else {
            splitPatternId = transaction.getIncomeSplitPatternId();
        }

        Boolean isCardPayment;
        if (transaction.getIsCardPayment() != null) {
            isCardPayment = transaction.getIsCardPayment();
        } else {
            isCardPayment = false;
        }

        if (transaction.getId() == null) {
            // INSERT
            String sql = "INSERT INTO bank_transactions " +
                        "(description, amount, transaction_type, transaction_date, fingerprint, user_id, " +
                        "asset_category_id, expense_category_id, split_pattern_id, transaction_source, is_card_payment, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
            LocalDateTime now = LocalDateTime.now();

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
                isCardPayment,
                now);
            transaction.setId(id);
            transaction.setCreatedAt(now);
        } else {
            // UPDATE
            String sql = "UPDATE bank_transactions SET " +
                        "description = ?, amount = ?, transaction_type = ?, " +
                        "transaction_date = ?, fingerprint = ?, user_id = ?, " +
                        "asset_category_id = ?, expense_category_id = ?, split_pattern_id = ?, " +
                        "transaction_source = ?, is_card_payment = ? " +
                        "WHERE id = ?";
            // 後方互換性: incomeSplitPatternIdが設定されている場合はそれを使用

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
                isCardPayment,
                transaction.getId());
        }
        return transaction;
    }

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
}
