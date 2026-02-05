package com.example.fintrack.repository;

import com.example.fintrack.domain.Asset;
import com.example.fintrack.domain.AssetCategory;
import com.example.fintrack.repository.mapper.AssetRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class AssetRepository {
    
    private final JdbcTemplate jdbcTemplate;
    private final AssetRowMapper rowMapper = new AssetRowMapper();
    private final AssetCategoryRepository assetCategoryRepository;
    
    public AssetRepository(JdbcTemplate jdbcTemplate, AssetCategoryRepository assetCategoryRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.assetCategoryRepository = assetCategoryRepository;
    }
    
    public Optional<Asset> findById(Long id) {
        String sql = "SELECT id, user_id, asset_category_id, amount, created_at, updated_at FROM assets WHERE id = ?";
        List<Asset> assets = jdbcTemplate.query(sql, rowMapper, id);
        if (assets.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(assets.get(0));
    }
    
    public Optional<Asset> findByUserIdAndAssetCategoryId(Long userId, Long assetCategoryId) {
        String sql;
        List<Asset> assets;
        
        if (assetCategoryId == null) {
            sql = "SELECT id, user_id, asset_category_id, amount, created_at, updated_at " +
                  "FROM assets WHERE user_id = ? AND asset_category_id IS NULL";
            assets = jdbcTemplate.query(sql, rowMapper, userId);
        } else {
            sql = "SELECT id, user_id, asset_category_id, amount, created_at, updated_at " +
                  "FROM assets WHERE user_id = ? AND asset_category_id = ?";
            assets = jdbcTemplate.query(sql, rowMapper, userId, assetCategoryId);
        }
        
        if (assets.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(assets.get(0));
    }
    
    public List<Asset> findByUserId(Long userId) {
        String sql = "SELECT id, user_id, asset_category_id, amount, created_at, updated_at " +
                    "FROM assets WHERE user_id = ? ORDER BY asset_category_id";
        return jdbcTemplate.query(sql, rowMapper, userId);
    }
    
    public List<Asset> findAll() {
        String sql = "SELECT id, user_id, asset_category_id, amount, created_at, updated_at " +
                    "FROM assets ORDER BY user_id, asset_category_id";
        return jdbcTemplate.query(sql, rowMapper);
    }
    
    /**
     * 資産カテゴリの残高を取得（複数レコードの合計）
     */
    public Long getBalanceByCategory(Long userId, Long categoryId) {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM assets " +
                    "WHERE user_id = ? AND asset_category_id = ?";
        return jdbcTemplate.queryForObject(sql, Long.class, userId, categoryId);
    }
    
    /**
     * 未分類の残高を取得（複数レコードの合計）
     */
    public Long getUncategorizedBalance(Long userId) {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM assets " +
                    "WHERE user_id = ? AND asset_category_id IS NULL";
        return jdbcTemplate.queryForObject(sql, Long.class, userId);
    }
    
    /**
     * ユーザーの総資産を取得
     */
    public Long getTotalBalance(Long userId) {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM assets WHERE user_id = ?";
        return jdbcTemplate.queryForObject(sql, Long.class, userId);
    }
    
    /**
     * 資産カテゴリの残高を更新（加算/減算）
     */
    public void updateBalance(Long userId, Long assetCategoryId, Long amountDelta) {
        Optional<Asset> existing = findByUserIdAndAssetCategoryId(userId, assetCategoryId);
        
        if (existing.isPresent()) {
            // 既存レコードを更新
            Asset asset = existing.get();
            asset.setAmount(asset.getAmount() + amountDelta);
            save(asset);
        } else {
            // 新規レコードを作成
            Asset asset = new Asset();
            asset.setUserId(userId);
            asset.setAssetCategoryId(assetCategoryId);
            asset.setAmount(amountDelta);
            save(asset);
        }
    }
    
    /**
     * 資産カテゴリの残高を設定（絶対値）
     */
    public void setBalance(Long userId, Long assetCategoryId, Long amount) {
        Optional<Asset> existing = findByUserIdAndAssetCategoryId(userId, assetCategoryId);
        
        if (existing.isPresent()) {
            // 既存レコードを更新
            Asset asset = existing.get();
            asset.setAmount(amount);
            save(asset);
        } else {
            // 新規レコードを作成
            Asset asset = new Asset();
            asset.setUserId(userId);
            asset.setAssetCategoryId(assetCategoryId);
            asset.setAmount(amount);
            save(asset);
        }
    }
    
    public Asset save(Asset asset) {
        if (asset.getId() == null) {
            // INSERT
            String sql = "INSERT INTO assets " +
                        "(user_id, asset_category_id, amount, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?) RETURNING id";
            LocalDateTime now = LocalDateTime.now();
            Long id = jdbcTemplate.queryForObject(sql, Long.class,
                asset.getUserId(),
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
                        "user_id = ?, asset_category_id = ?, amount = ?, updated_at = ? " +
                        "WHERE id = ?";
            LocalDateTime now = LocalDateTime.now();
            jdbcTemplate.update(sql,
                asset.getUserId(),
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
    
    /**
     * 確定済み総資産を取得（未払いのクレジットカード取引を除外）
     */
    public Long getConfirmedTotalBalance(Long userId) {
        String sql = "SELECT COALESCE(SUM(a.amount), 0) " +
                    "FROM assets a " +
                    "WHERE a.user_id = ?";
        Long assetsTotal = jdbcTemplate.queryForObject(sql, Long.class, userId);
        
        // 未払いのクレジットカード取引の合計を取得
        String creditSql = "SELECT COALESCE(SUM(" +
                          "CASE " +
                          "  WHEN transaction_type = 'INCOME' THEN amount " +
                          "  WHEN transaction_type = 'EXPENSE' THEN -amount " +
                          "  ELSE 0 " +
                          "END), 0) " +
                          "FROM card_transactions " +
                          "WHERE user_id = ? " +
                          "AND card_type = 'CREDIT' " +
                          "AND is_paid = FALSE";
        Long unpaidCreditTotal = jdbcTemplate.queryForObject(creditSql, Long.class, userId);
        
        return assetsTotal - unpaidCreditTotal;
    }
    
    /**
     * カテゴリ別の確定済み残高を取得（当月と前月のクレジットカード取引を除外）
     */
    public Map<AssetCategory, Long> getConfirmedBalanceByCategory(Long userId) {
        // 通常の残高をカテゴリ別に取得
        List<AssetCategory> categories = assetCategoryRepository.findByUserId(userId);
        Map<AssetCategory, Long> normalBalances = new HashMap<>();
        for (AssetCategory category : categories) {
            Long balance = getBalanceByCategory(userId, category.getId());
            normalBalances.put(category, balance);
        }
        
        // 未払いのクレジットカード取引をカテゴリ別に集計
        String sql = "SELECT ct.asset_category_id, " +
                    "SUM(CASE " +
                    "  WHEN ct.transaction_type = 'INCOME' THEN ct.amount " +
                    "  WHEN ct.transaction_type = 'EXPENSE' THEN -ct.amount " +
                    "  ELSE 0 " +
                    "END) as total " +
                    "FROM card_transactions ct " +
                    "WHERE ct.user_id = ? " +
                    "AND ct.card_type = 'CREDIT' " +
                    "AND ct.asset_category_id IS NOT NULL " +
                    "AND ct.is_paid = FALSE " +
                    "GROUP BY ct.asset_category_id";
        
        Map<Long, Long> unpaidCreditTotalsByCategory = new HashMap<>();
        jdbcTemplate.query(sql, rs -> {
            unpaidCreditTotalsByCategory.put(rs.getLong("asset_category_id"), rs.getLong("total"));
        }, userId);
        
        // 通常の残高から未払いのクレジットカード取引を差し引く
        Map<AssetCategory, Long> confirmedBalances = new HashMap<>();
        for (Map.Entry<AssetCategory, Long> entry : normalBalances.entrySet()) {
            Long categoryId = entry.getKey().getId();
            Long normalBalance = entry.getValue();
            Long unpaidCreditTotal = unpaidCreditTotalsByCategory.getOrDefault(categoryId, 0L);
            confirmedBalances.put(entry.getKey(), normalBalance - unpaidCreditTotal);
        }
        
        return confirmedBalances;
    }
    
    /**
     * 未分類の確定済み残高を取得（未払いのクレジットカード取引を除外）
     */
    public Long getConfirmedUncategorizedBalance(Long userId) {
        Long normalUncategorized = getUncategorizedBalance(userId);
        
        // 未払いの未分類クレジットカード取引の合計を取得
        String sql = "SELECT COALESCE(SUM(" +
                    "CASE " +
                    "  WHEN transaction_type = 'INCOME' THEN amount " +
                    "  WHEN transaction_type = 'EXPENSE' THEN -amount " +
                    "  ELSE 0 " +
                    "END), 0) " +
                    "FROM card_transactions " +
                    "WHERE user_id = ? " +
                    "AND card_type = 'CREDIT' " +
                    "AND asset_category_id IS NULL " +
                    "AND is_paid = FALSE";
        Long unpaidCreditUncategorized = jdbcTemplate.queryForObject(sql, Long.class, userId);
        
        return normalUncategorized - unpaidCreditUncategorized;
    }
}
