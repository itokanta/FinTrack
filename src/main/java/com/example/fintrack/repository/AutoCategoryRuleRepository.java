package com.example.fintrack.repository;

import com.example.fintrack.domain.AutoCategoryRule;
import com.example.fintrack.domain.TransactionType;
import com.example.fintrack.repository.mapper.AutoCategoryRuleRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class AutoCategoryRuleRepository {
    
    private final JdbcTemplate jdbcTemplate;
    private final AutoCategoryRuleRowMapper rowMapper = new AutoCategoryRuleRowMapper();
    
    public AutoCategoryRuleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    public Optional<AutoCategoryRule> findById(Long id) {
        String sql = "SELECT id, user_id, asset_category_id, description_pattern, transaction_type, created_at " +
                    "FROM auto_category_rules WHERE id = ?";
        List<AutoCategoryRule> rules = jdbcTemplate.query(sql, rowMapper, id);
        if (rules.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rules.get(0));
    }
    
    public List<AutoCategoryRule> findAll() {
        String sql = "SELECT id, user_id, asset_category_id, description_pattern, transaction_type, created_at " +
                    "FROM auto_category_rules ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, rowMapper);
    }
    
    public List<AutoCategoryRule> findByUserId(Long userId) {
        String sql = "SELECT id, user_id, asset_category_id, description_pattern, transaction_type, created_at " +
                    "FROM auto_category_rules WHERE user_id = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, rowMapper, userId);
    }
    
    public List<AutoCategoryRule> findByUserIdAndTransactionType(Long userId, TransactionType transactionType) {
        String sql = "SELECT id, user_id, asset_category_id, description_pattern, transaction_type, created_at " +
                    "FROM auto_category_rules WHERE user_id = ? AND transaction_type = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, rowMapper, userId, transactionType.name());
    }
    
    public List<AutoCategoryRule> findByAssetCategoryId(Long assetCategoryId) {
        String sql = "SELECT id, user_id, asset_category_id, description_pattern, transaction_type, created_at " +
                    "FROM auto_category_rules WHERE asset_category_id = ?";
        return jdbcTemplate.query(sql, rowMapper, assetCategoryId);
    }
    
    public AutoCategoryRule save(AutoCategoryRule rule) {
        if (rule.getId() == null) {
            // INSERT
            String sql = "INSERT INTO auto_category_rules " +
                        "(user_id, asset_category_id, description_pattern, transaction_type, created_at) " +
                        "VALUES (?, ?, ?, ?, ?) RETURNING id";
            LocalDateTime now = LocalDateTime.now();
            Long id = jdbcTemplate.queryForObject(sql, Long.class,
                rule.getUserId(),
                rule.getAssetCategoryId(),
                rule.getDescriptionPattern(),
                rule.getTransactionType().name(),
                now);
            rule.setId(id);
            rule.setCreatedAt(now);
        } else {
            // UPDATE
            String sql = "UPDATE auto_category_rules SET " +
                        "user_id = ?, asset_category_id = ?, description_pattern = ?, transaction_type = ? " +
                        "WHERE id = ?";
            jdbcTemplate.update(sql,
                rule.getUserId(),
                rule.getAssetCategoryId(),
                rule.getDescriptionPattern(),
                rule.getTransactionType().name(),
                rule.getId());
        }
        return rule;
    }
    
    public void deleteById(Long id) {
        String sql = "DELETE FROM auto_category_rules WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }
}
