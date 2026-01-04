package com.example.fintrack.repository;

import com.example.fintrack.domain.IncomeSplitPattern;
import com.example.fintrack.repository.mapper.IncomeSplitPatternRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class IncomeSplitPatternRepository {
    
    private final JdbcTemplate jdbcTemplate;
    private final IncomeSplitPatternRowMapper rowMapper = new IncomeSplitPatternRowMapper();
    
    public IncomeSplitPatternRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    public List<IncomeSplitPattern> findById(Long id) {
        String sql = "SELECT * FROM income_split_patterns WHERE id = ? ORDER BY asset_category_id";
        return jdbcTemplate.query(sql, rowMapper, id);
    }
    
    public Long findMaxId() {
        String sql = "SELECT COALESCE(MAX(id), 0) FROM income_split_patterns";
        return jdbcTemplate.queryForObject(sql, Long.class);
    }
    
    public List<IncomeSplitPattern> findByUserId(Long userId) {
        String sql = "SELECT * FROM income_split_patterns WHERE user_id = ?";
        return jdbcTemplate.query(sql, rowMapper, userId);
    }
    
    public List<Long> findDistinctIdsByUserId(Long userId) {
        String sql = "SELECT DISTINCT id FROM income_split_patterns " +
                    "WHERE user_id = ? " +
                    "ORDER BY id";
        return jdbcTemplate.queryForList(sql, Long.class, userId);
    }
    
    public IncomeSplitPattern save(IncomeSplitPattern pattern) {
        // 複合キーなので常にINSERT
        String sql = "INSERT INTO income_split_patterns (id, user_id, asset_category_id, split_ratio, pattern_name, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(sql,
            pattern.getId(),
            pattern.getUserId(),
            pattern.getAssetCategoryId(),
            pattern.getSplitRatio(),
            pattern.getPatternName(),
            now);
        pattern.setCreatedAt(now);
        return pattern;
    }
    
    public void deleteById(Long id) {
        String sql = "DELETE FROM income_split_patterns WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }
}
