package com.example.fintrack.repository;

import com.example.fintrack.domain.SplitPattern;
import com.example.fintrack.repository.mapper.SplitPatternRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class SplitPatternRepository {
    
    private final JdbcTemplate jdbcTemplate;
    private final SplitPatternRowMapper rowMapper = new SplitPatternRowMapper();
    
    public SplitPatternRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    public List<SplitPattern> findById(Long patternId) {
        String sql = "SELECT * FROM split_patterns WHERE pattern_id = ? " +
                    "ORDER BY asset_category_id";
        return jdbcTemplate.query(sql, rowMapper, patternId);
    }
    
    public Long findMaxPatternId() {
        String sql = "SELECT COALESCE(MAX(pattern_id), 0) FROM split_patterns";
        return jdbcTemplate.queryForObject(sql, Long.class);
    }
    
    public List<SplitPattern> findByUserId(Long userId) {
        String sql = "SELECT * FROM split_patterns WHERE user_id = ?";
        return jdbcTemplate.query(sql, rowMapper, userId);
    }
    
    public List<SplitPattern> findByUserIdAndTransactionType(Long userId, String transactionType) {
        String sql = "SELECT * FROM split_patterns WHERE user_id = ? AND transaction_type = ?";
        return jdbcTemplate.query(sql, rowMapper, userId, transactionType);
    }
    
    public List<Long> findDistinctPatternIdsByUserId(Long userId) {
        String sql = "SELECT DISTINCT pattern_id FROM split_patterns " +
                    "WHERE user_id = ? " +
                    "ORDER BY pattern_id";
        return jdbcTemplate.queryForList(sql, Long.class, userId);
    }
    
    public List<Long> findDistinctPatternIdsByUserIdAndTransactionType(Long userId, String transactionType) {
        String sql = "SELECT DISTINCT pattern_id FROM split_patterns " +
                    "WHERE user_id = ? AND transaction_type = ? " +
                    "ORDER BY pattern_id";
        return jdbcTemplate.queryForList(sql, Long.class, userId, transactionType);
    }
    
    public SplitPattern save(SplitPattern pattern) {
        // idが自動採番される
        String sql = "INSERT INTO split_patterns " +
                    "(pattern_id, user_id, transaction_type, split_type, asset_category_id, " +
                    "split_ratio, split_amount, pattern_name, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        LocalDateTime now = LocalDateTime.now();
        Long id = jdbcTemplate.queryForObject(sql, Long.class,
            pattern.getPatternId(),
            pattern.getUserId(),
            pattern.getTransactionType(),
            pattern.getSplitType(),
            pattern.getAssetCategoryId(),
            pattern.getSplitRatio(),
            pattern.getSplitAmount(),
            pattern.getPatternName(),
            now);
        pattern.setId(id);
        pattern.setCreatedAt(now);
        return pattern;
    }
    
    public void deleteByPatternId(Long patternId) {
        String sql = "DELETE FROM split_patterns WHERE pattern_id = ?";
        jdbcTemplate.update(sql, patternId);
    }
}

