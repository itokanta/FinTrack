package com.example.fintrack.repository;

import com.example.fintrack.domain.SplitIncome;
import com.example.fintrack.repository.mapper.SplitIncomeRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class SplitIncomeRepository {
    
    private final JdbcTemplate jdbcTemplate;
    private final SplitIncomeRowMapper rowMapper = new SplitIncomeRowMapper();
    
    public SplitIncomeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    public List<SplitIncome> findByBankTransactionId(Long bankTransactionId) {
        String sql = "SELECT * FROM split_incomes WHERE bank_transaction_id = ?";
        return jdbcTemplate.query(sql, rowMapper, bankTransactionId);
    }
    
    public List<SplitIncome> findByCardTransactionId(Long cardTransactionId) {
        String sql = "SELECT * FROM split_incomes WHERE card_transaction_id = ?";
        return jdbcTemplate.query(sql, rowMapper, cardTransactionId);
    }
    
    public List<SplitIncome> findBySplitPatternId(Long splitPatternId) {
        String sql = "SELECT * FROM split_incomes WHERE split_pattern_id = ?";
        return jdbcTemplate.query(sql, rowMapper, splitPatternId);
    }
    
    public List<SplitIncome> findByAssetCategoryId(Long assetCategoryId) {
        String sql = "SELECT * FROM split_incomes WHERE asset_category_id = ?";
        return jdbcTemplate.query(sql, rowMapper, assetCategoryId);
    }
    
    public SplitIncome save(SplitIncome splitIncome) {
        if (splitIncome.getId() == null) {
            // INSERT
            String sql = "INSERT INTO split_incomes " +
                        "(asset_category_id, split_pattern_id, amount, bank_transaction_id, card_transaction_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id";
            LocalDateTime now = LocalDateTime.now();
            Long id = jdbcTemplate.queryForObject(sql, Long.class,
                splitIncome.getAssetCategoryId(),
                splitIncome.getSplitPatternId(),
                splitIncome.getAmount(),
                splitIncome.getBankTransactionId(),
                splitIncome.getCardTransactionId(),
                now,
                now);
            splitIncome.setId(id);
            splitIncome.setCreatedAt(now);
            splitIncome.setUpdatedAt(now);
        } else {
            // UPDATE
            String sql = "UPDATE split_incomes SET " +
                        "asset_category_id = ?, split_pattern_id = ?, amount = ?, " +
                        "bank_transaction_id = ?, card_transaction_id = ?, updated_at = ? " +
                        "WHERE id = ?";
            LocalDateTime now = LocalDateTime.now();
            jdbcTemplate.update(sql,
                splitIncome.getAssetCategoryId(),
                splitIncome.getSplitPatternId(),
                splitIncome.getAmount(),
                splitIncome.getBankTransactionId(),
                splitIncome.getCardTransactionId(),
                now,
                splitIncome.getId());
            splitIncome.setUpdatedAt(now);
        }
        return splitIncome;
    }
    
    public List<SplitIncome> saveAll(List<SplitIncome> splitIncomes) {
        for (SplitIncome splitIncome : splitIncomes) {
            save(splitIncome);
        }
        return splitIncomes;
    }
}
