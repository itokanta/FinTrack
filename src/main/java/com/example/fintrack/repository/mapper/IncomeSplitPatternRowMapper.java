package com.example.fintrack.repository.mapper;

import com.example.fintrack.domain.IncomeSplitPattern;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class IncomeSplitPatternRowMapper implements RowMapper<IncomeSplitPattern> {
    
    @Override
    public IncomeSplitPattern mapRow(ResultSet rs, int rowNum) throws SQLException {
        IncomeSplitPattern pattern = new IncomeSplitPattern();
        pattern.setId(rs.getLong("id"));
        pattern.setUserId(rs.getLong("user_id"));
        pattern.setAssetCategoryId(rs.getLong("asset_category_id"));
        pattern.setSplitRatio(rs.getBigDecimal("split_ratio"));
        pattern.setPatternName(rs.getString("pattern_name"));
        pattern.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return pattern;
    }
}

