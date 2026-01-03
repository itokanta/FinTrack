package com.example.fintrack.repository;

import com.example.fintrack.domain.AssetCategory;
import com.example.fintrack.domain.AutoCategoryRule;
import com.example.fintrack.repository.mapper.AutoCategoryRuleRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class AutoCategoryRuleRepository {
    
    private final JdbcTemplate jdbcTemplate;
    private final AutoCategoryRuleRowMapper rowMapper = new AutoCategoryRuleRowMapper();
    
    public AutoCategoryRuleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    public List<AutoCategoryRule> findAll() {
        String sql = "SELECT * FROM auto_category_rules";
        return jdbcTemplate.query(sql, rowMapper);
    }
    
    public List<AutoCategoryRule> findByUserId(Long userId) {
        String sql = "SELECT acr.* FROM auto_category_rules acr " +
                    "JOIN asset_categories ac ON acr.asset_category_id = ac.id " +
                    "WHERE ac.user_id = ?";
        return jdbcTemplate.query(sql, new RowMapper<AutoCategoryRule>() {
            @Override
            public AutoCategoryRule mapRow(ResultSet rs, int rowNum) throws SQLException {
                AutoCategoryRule rule = new AutoCategoryRule();
                rule.setId(rs.getLong("id"));
                rule.setAssetCategoryId(rs.getLong("asset_category_id"));
                rule.setDescriptionPattern(rs.getString("description_pattern"));
                rule.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                
                // AssetCategoryも取得する場合は別途JOINが必要
                AssetCategory category = new AssetCategory();
                category.setId(rule.getAssetCategoryId());
                rule.setAssetCategory(category);
                
                return rule;
            }
        }, userId);
    }
    
    public List<AutoCategoryRule> findByAssetCategoryId(Long assetCategoryId) {
        String sql = "SELECT * FROM auto_category_rules WHERE asset_category_id = ?";
        return jdbcTemplate.query(sql, rowMapper, assetCategoryId);
    }
    
    public AutoCategoryRule save(AutoCategoryRule rule) {
        if (rule.getId() == null) {
            // INSERT
            String sql = "INSERT INTO auto_category_rules (asset_category_id, description_pattern, created_at) " +
                        "VALUES (?, ?, ?) RETURNING id";
            LocalDateTime now = LocalDateTime.now();
            Long id = jdbcTemplate.queryForObject(sql, Long.class,
                rule.getAssetCategoryId(), rule.getDescriptionPattern(), now);
            rule.setId(id);
            rule.setCreatedAt(now);
        } else {
            // UPDATE
            String sql = "UPDATE auto_category_rules SET asset_category_id = ?, description_pattern = ? WHERE id = ?";
            jdbcTemplate.update(sql, rule.getAssetCategoryId(), rule.getDescriptionPattern(), rule.getId());
        }
        return rule;
    }
}
