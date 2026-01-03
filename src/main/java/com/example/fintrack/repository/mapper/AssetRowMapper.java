package com.example.fintrack.repository.mapper;

import com.example.fintrack.domain.Asset;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class AssetRowMapper implements RowMapper<Asset> {
    
    @Override
    public Asset mapRow(ResultSet rs, int rowNum) throws SQLException {
        Asset asset = new Asset();
        asset.setId(rs.getLong("id"));
        
        // NULL許容のカラム
        long assetCategoryId = rs.getLong("asset_category_id");
        if (rs.wasNull()) {
            asset.setAssetCategoryId(null);
        } else {
            asset.setAssetCategoryId(assetCategoryId);
        }
        
        asset.setAmount(rs.getLong("amount"));
        asset.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        asset.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        
        return asset;
    }
}

