package com.example.fintrack.repository.mapper;

import com.example.fintrack.domain.AutoCategoryRule;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class AutoCategoryRuleRowMapper implements RowMapper<AutoCategoryRule> {
    
    @Override
    public AutoCategoryRule mapRow(ResultSet rs, int rowNum) throws SQLException {
        AutoCategoryRule rule = new AutoCategoryRule();
        rule.setId(rs.getLong("id"));
        rule.setAssetCategoryId(rs.getLong("asset_category_id"));
        rule.setDescriptionPattern(rs.getString("description_pattern"));
        rule.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return rule;
    }
}

