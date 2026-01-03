package com.example.fintrack.repository.mapper;

import com.example.fintrack.domain.AssetCategory;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class AssetCategoryRowMapper implements RowMapper<AssetCategory> {
    
    @Override
    public AssetCategory mapRow(ResultSet rs, int rowNum) throws SQLException {
        AssetCategory category = new AssetCategory();
        category.setId(rs.getLong("id"));
        category.setUserId(rs.getLong("user_id"));
        category.setCategoryName(rs.getString("category_name"));
        category.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return category;
    }
}

