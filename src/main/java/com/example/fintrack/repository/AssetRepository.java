package com.example.fintrack.repository;

import com.example.fintrack.domain.Asset;
import com.example.fintrack.repository.mapper.AssetRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class AssetRepository {
    
    private final JdbcTemplate jdbcTemplate;
    private final AssetRowMapper rowMapper = new AssetRowMapper();
    
    public AssetRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    public Optional<Asset> findById(Long id) {
        String sql = "SELECT * FROM assets WHERE id = ?";
        List<Asset> assets = jdbcTemplate.query(sql, rowMapper, id);
        if (assets.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(assets.get(0));
    }
    
    public Optional<Asset> findByAssetCategoryId(Long assetCategoryId) {
        String sql = "SELECT * FROM assets WHERE asset_category_id = ?";
        List<Asset> assets = jdbcTemplate.query(sql, rowMapper, assetCategoryId);
        if (assets.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(assets.get(0));
    }
    
    public List<Asset> findAll() {
        String sql = "SELECT * FROM assets ORDER BY asset_category_id";
        return jdbcTemplate.query(sql, rowMapper);
    }
    
    /**
     * 資産カテゴリの残高を取得
     */
    public Long getBalanceByCategory(Long categoryId) {
        Optional<Asset> asset = findByAssetCategoryId(categoryId);
        if (asset.isPresent()) {
            return asset.get().getAmount();
        }
        return 0L;
    }
    
    /**
     * 未分類の残高を取得
     */
    public Long getUncategorizedBalance() {
        Optional<Asset> asset = findByAssetCategoryId(null);
        if (asset.isPresent()) {
            return asset.get().getAmount();
        }
        return 0L;
    }
    
    /**
     * 資産カテゴリの残高を更新（加算/減算）
     */
    public void updateBalance(Long assetCategoryId, Long amountDelta) {
        Optional<Asset> existing = findByAssetCategoryId(assetCategoryId);
        
        if (existing.isPresent()) {
            // 既存レコードを更新
            Asset asset = existing.get();
            asset.setAmount(asset.getAmount() + amountDelta);
            save(asset);
        } else {
            // 新規レコードを作成
            Asset asset = new Asset();
            asset.setAssetCategoryId(assetCategoryId);
            asset.setAmount(amountDelta);
            save(asset);
        }
    }
    
    /**
     * 資産カテゴリの残高を設定（絶対値）
     */
    public void setBalance(Long assetCategoryId, Long amount) {
        Optional<Asset> existing = findByAssetCategoryId(assetCategoryId);
        
        if (existing.isPresent()) {
            // 既存レコードを更新
            Asset asset = existing.get();
            asset.setAmount(amount);
            save(asset);
        } else {
            // 新規レコードを作成
            Asset asset = new Asset();
            asset.setAssetCategoryId(assetCategoryId);
            asset.setAmount(amount);
            save(asset);
        }
    }
    
    public Asset save(Asset asset) {
        if (asset.getId() == null) {
            // INSERT
            String sql = "INSERT INTO assets " +
                        "(asset_category_id, amount, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?) RETURNING id";
            LocalDateTime now = LocalDateTime.now();
            Long id = jdbcTemplate.queryForObject(sql, Long.class,
                asset.getAssetCategoryId(),
                asset.getAmount(),
                now,
                now);
            asset.setId(id);
            asset.setCreatedAt(now);
            asset.setUpdatedAt(now);
        } else {
            // UPDATE
            String sql = "UPDATE assets SET " +
                        "asset_category_id = ?, amount = ?, updated_at = ? " +
                        "WHERE id = ?";
            LocalDateTime now = LocalDateTime.now();
            jdbcTemplate.update(sql,
                asset.getAssetCategoryId(),
                asset.getAmount(),
                now,
                asset.getId());
            asset.setUpdatedAt(now);
        }
        return asset;
    }
    
    public void deleteById(Long id) {
        String sql = "DELETE FROM assets WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }
}
