package com.example.fintrack.repository;

import com.example.fintrack.domain.SplitTransaction;
import com.example.fintrack.repository.mapper.SplitTransactionRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class SplitTransactionRepository {
    
    private final JdbcTemplate jdbcTemplate;
    private final SplitTransactionRowMapper rowMapper = new SplitTransactionRowMapper();
    
    public SplitTransactionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    public List<SplitTransaction> findByBankTransactionId(Long bankTransactionId) {
        String sql = "SELECT * FROM split_transactions WHERE bank_transaction_id = ?";
        return jdbcTemplate.query(sql, rowMapper, bankTransactionId);
    }
    
    public List<SplitTransaction> findByCardTransactionId(Long cardTransactionId) {
        String sql = "SELECT * FROM split_transactions WHERE card_transaction_id = ?";
        return jdbcTemplate.query(sql, rowMapper, cardTransactionId);
    }
    
    public List<SplitTransaction> findBySplitPatternId(Long splitPatternId) {
        String sql = "SELECT * FROM split_transactions WHERE split_pattern_id = ?";
        return jdbcTemplate.query(sql, rowMapper, splitPatternId);
    }
    
    public List<SplitTransaction> findByAssetCategoryId(Long assetCategoryId) {
        String sql = "SELECT * FROM split_transactions WHERE asset_category_id = ?";
        return jdbcTemplate.query(sql, rowMapper, assetCategoryId);
    }
    
    public List<SplitTransaction> findByExpenseCategoryId(Long expenseCategoryId) {
        String sql = "SELECT * FROM split_transactions WHERE expense_category_id = ?";
        return jdbcTemplate.query(sql, rowMapper, expenseCategoryId);
    }
    
    public SplitTransaction save(SplitTransaction splitTransaction) {
        if (splitTransaction.getId() == null) {
            // INSERT
            String sql = "INSERT INTO split_transactions " +
                        "(transaction_type, asset_category_id, split_pattern_id, " +
                        "split_method, amount, bank_transaction_id, card_transaction_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
            LocalDateTime now = LocalDateTime.now();
            Long id = jdbcTemplate.queryForObject(sql, Long.class,
                splitTransaction.getTransactionType(),
                splitTransaction.getAssetCategoryId(),
                splitTransaction.getSplitPatternId(),
                splitTransaction.getSplitMethod(),
                splitTransaction.getAmount(),
                splitTransaction.getBankTransactionId(),
                splitTransaction.getCardTransactionId(),
                now,
                now);
            splitTransaction.setId(id);
            splitTransaction.setCreatedAt(now);
            splitTransaction.setUpdatedAt(now);
        } else {
            // UPDATE
            String sql = "UPDATE split_transactions SET " +
                        "transaction_type = ?, asset_category_id = ?, " +
                        "split_pattern_id = ?, split_method = ?, amount = ?, " +
                        "bank_transaction_id = ?, card_transaction_id = ?, updated_at = ? " +
                        "WHERE id = ?";
            LocalDateTime now = LocalDateTime.now();
            jdbcTemplate.update(sql,
                splitTransaction.getTransactionType(),
                splitTransaction.getAssetCategoryId(),
                splitTransaction.getSplitPatternId(),
                splitTransaction.getSplitMethod(),
                splitTransaction.getAmount(),
                splitTransaction.getBankTransactionId(),
                splitTransaction.getCardTransactionId(),
                now,
                splitTransaction.getId());
            splitTransaction.setUpdatedAt(now);
        }
        return splitTransaction;
    }
    
    public List<SplitTransaction> saveAll(List<SplitTransaction> splitTransactions) {
        for (SplitTransaction splitTransaction : splitTransactions) {
            save(splitTransaction);
        }
        return splitTransactions;
    }
    
    public void deleteByBankTransactionId(Long bankTransactionId) {
        String sql = "DELETE FROM split_transactions WHERE bank_transaction_id = ?";
        jdbcTemplate.update(sql, bankTransactionId);
    }
    
    public void deleteByCardTransactionId(Long cardTransactionId) {
        String sql = "DELETE FROM split_transactions WHERE card_transaction_id = ?";
        jdbcTemplate.update(sql, cardTransactionId);
    }
}

