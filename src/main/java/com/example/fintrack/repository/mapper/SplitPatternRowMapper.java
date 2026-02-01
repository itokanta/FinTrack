package com.example.fintrack.repository.mapper;

import com.example.fintrack.domain.SplitPattern;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SplitPatternRowMapper implements RowMapper<SplitPattern> {
    
    @Override
    public SplitPattern mapRow(ResultSet rs, int rowNum) throws SQLException {
        SplitPattern pattern = new SplitPattern();
        pattern.setId(rs.getLong("id"));
        pattern.setPatternId(rs.getLong("pattern_id"));
        pattern.setUserId(rs.getLong("user_id"));
        pattern.setTransactionType(rs.getString("transaction_type"));
        pattern.setSplitType(rs.getString("split_type"));
        pattern.setAssetCategoryId(rs.getLong("asset_category_id"));
        
        // split_ratioはNULL許容（金額指定の場合はNULL）
        if (rs.getBigDecimal("split_ratio") != null) {
            pattern.setSplitRatio(rs.getBigDecimal("split_ratio"));
        }
        
        // split_amountはNULL許容（割合指定の場合はNULL）
        long splitAmount = rs.getLong("split_amount");
        if (rs.wasNull()) {
            pattern.setSplitAmount(null);
        } else {
            pattern.setSplitAmount(splitAmount);
        }
        
        pattern.setPatternName(rs.getString("pattern_name"));
        pattern.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        
        return pattern;
    }
}

