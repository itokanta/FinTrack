package com.example.fintrack.repository;

import com.example.fintrack.domain.CardTransaction;
import com.example.fintrack.domain.TransactionType;
import com.example.fintrack.repository.mapper.CardTransactionRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class CardTransactionRepository {
    
    private final JdbcTemplate jdbcTemplate;
    private final CardTransactionRowMapper rowMapper = new CardTransactionRowMapper();
    
    public CardTransactionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    public Optional<CardTransaction> findById(Long id) {
        String sql = "SELECT * FROM card_transactions WHERE id = ?";
        List<CardTransaction> transactions = jdbcTemplate.query(sql, rowMapper, id);
        if (transactions.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(transactions.get(0));
    }
    
    public boolean existsByFingerprint(String fingerprint) {
        String sql = "SELECT COUNT(*) FROM card_transactions WHERE fingerprint = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, fingerprint);
        return count != null && count > 0;
    }
    
    public Map<String, Long> countByFingerprintGrouped(Long userId) {
        String sql = "SELECT fingerprint, COUNT(*) as count " +
                    "FROM card_transactions " +
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
    
    /**
     * fingerprint全件を取得
     */
    public List<CardTransaction> findAllByUserId(Long userId) {
        String sql = "SELECT * FROM card_transactions WHERE user_id = ? ORDER BY created_at ASC";
        List<CardTransaction> transactions = jdbcTemplate.query(sql, rowMapper, userId);
        
        return transactions;
    }
    
    public List<CardTransaction> findByUserIdAndAssetCategoryIdIsNullAndTransactionType(
            Long userId, TransactionType transactionType) {
        String sql = "SELECT * FROM card_transactions " +
                    "WHERE user_id = ? AND asset_category_id IS NULL AND transaction_type = ? " +
                    "ORDER BY transaction_date DESC";
        return jdbcTemplate.query(sql, rowMapper, userId, transactionType.name());
    }
    
    public List<CardTransaction> findByUserIdAndIncomeSplitPatternIdIsNullAndTransactionType(
            Long userId, TransactionType transactionType) {
        String sql = "SELECT * FROM card_transactions " +
                    "WHERE user_id = ? AND split_pattern_id IS NULL AND transaction_type = ? " +
                    "ORDER BY transaction_date DESC";
        return jdbcTemplate.query(sql, rowMapper, userId, transactionType.name());
    }
    
    public List<CardTransaction> findByUserIdAndSplitPatternIdIsNullAndTransactionType(
            Long userId, TransactionType transactionType) {
        String sql = "SELECT * FROM card_transactions " +
                    "WHERE user_id = ? AND split_pattern_id IS NULL AND transaction_type = ? " +
                    "ORDER BY transaction_date DESC";
        return jdbcTemplate.query(sql, rowMapper, userId, transactionType.name());
    }
    
    public List<CardTransaction> findByUserIdAndSplitPatternIdIsNullAndAssetCategoryIdIsNullAndTransactionType(
            Long userId, TransactionType transactionType) {
        String sql = "SELECT * FROM card_transactions " +
                    "WHERE user_id = ? AND split_pattern_id IS NULL AND asset_category_id IS NULL AND transaction_type = ? " +
                    "ORDER BY transaction_date DESC";
        return jdbcTemplate.query(sql, rowMapper, userId, transactionType.name());
    }
    
    public List<CardTransaction> findByUserIdAndTransactionDateBetween(
            Long userId, LocalDate startDate, LocalDate endDate, int offset, int limit) {
        String sql = "SELECT * FROM card_transactions " +
                    "WHERE user_id = ? AND transaction_date BETWEEN ? AND ? " +
                    "ORDER BY transaction_date DESC " +
                    "LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, rowMapper, userId, startDate, endDate, limit, offset);
    }
    
    public List<CardTransaction> findByUserIdAndTransactionTypeAndTransactionDateBetween(
            Long userId, TransactionType transactionType, LocalDate startDate, LocalDate endDate,
            int offset, int limit) {
        String sql = "SELECT * FROM card_transactions " +
                    "WHERE user_id = ? AND transaction_type = ? AND transaction_date BETWEEN ? AND ? " +
                    "ORDER BY transaction_date DESC " +
                    "LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, rowMapper, userId, transactionType.name(), startDate, endDate, limit, offset);
    }
    
    public List<String> findDistinctDescriptionsByUserId(Long userId) {
        String sql = "SELECT DISTINCT description FROM card_transactions " +
                    "WHERE user_id = ? AND description IS NOT NULL " +
                    "ORDER BY description";
        return jdbcTemplate.queryForList(sql, String.class, userId);
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
                        "asset_category_id, expense_category_id, split_pattern_id, transaction_source, card_type, payment_month, is_paid, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
            LocalDateTime now = LocalDateTime.now();
            // 後方互換性: incomeSplitPatternIdが設定されている場合はそれを使用

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
                transaction.getIsPaid(),
                now);
            transaction.setId(id);
            transaction.setCreatedAt(now);
        } else {
            // UPDATE
            String sql = "UPDATE card_transactions SET " +
                        "description = ?, amount = ?, transaction_type = ?, " +
                        "transaction_date = ?, fingerprint = ?, user_id = ?, " +
                        "asset_category_id = ?, expense_category_id = ?, split_pattern_id = ?, " +
                        "transaction_source = ?, card_type = ?, payment_month = ?, is_paid = ? " +
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
                transaction.getCardType(),
                transaction.getPaymentMonth(),
                transaction.getIsPaid(),
                transaction.getId());
        }
        return transaction;
    }

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
    
    /**
     * 未払いのクレジットカード取引合計を取得
     */
    public Long sumUnpaidCreditAmount(Long userId) {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM card_transactions " +
                    "WHERE user_id = ? AND card_type = 'CREDIT' " +
                    "AND is_paid = FALSE " +
                    "AND transaction_type = 'EXPENSE'";
        return jdbcTemplate.queryForObject(sql, Long.class, userId);
    }
    
    /**
     * 指定した支払い月のカード取引を引き落とし済みに更新
     */
    public void markAsPaidByPaymentMonth(Long userId, String paymentMonth) {
        String sql = "UPDATE card_transactions SET is_paid = TRUE " +
                    "WHERE user_id = ? AND payment_month = ? AND card_type = 'CREDIT'";
        jdbcTemplate.update(sql, userId, paymentMonth);
    }
}
