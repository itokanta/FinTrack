package com.example.fintrack.repository;

import com.example.fintrack.domain.Asset;
import com.example.fintrack.domain.AssetCategory;
import com.example.fintrack.repository.mapper.AssetRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class AssetRepository {
    
    private final NamedParameterJdbcTemplate template;
    private final AssetRowMapper rowMapper = new AssetRowMapper();
    private final AssetCategoryRepository assetCategoryRepository;
    
    public AssetRepository(NamedParameterJdbcTemplate template, AssetCategoryRepository assetCategoryRepository) {
        this.template = template;
        this.assetCategoryRepository = assetCategoryRepository;
    }
    
    public Optional<Asset> findByUserIdAndAssetCategoryId(Long userId, Long assetCategoryId) {
        String sql;
        List<Asset> assets;
        
        if (assetCategoryId == null) {
            sql = "SELECT id, user_id, asset_category_id, amount, created_at, updated_at " +
                  "FROM assets WHERE user_id = :userId AND asset_category_id IS NULL";
            MapSqlParameterSource param = new MapSqlParameterSource().addValue("userId", userId);
            assets = template.query(sql, param, rowMapper);
        } else {
            sql = "SELECT id, user_id, asset_category_id, amount, created_at, updated_at " +
                  "FROM assets WHERE user_id = :userId AND asset_category_id = :assetCategoryId";
            MapSqlParameterSource param = new MapSqlParameterSource()
                    .addValue("userId", userId)
                    .addValue("assetCategoryId", assetCategoryId);
            assets = template.query(sql, param, rowMapper);
        }
        
        if (assets.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(assets.get(0));
    }
    
    /**
     * 資産カテゴリの残高を取得（複数レコードの合計）
     */
    public Long getBalanceByCategory(Long userId, Long categoryId) {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM assets " +
                    "WHERE user_id = :userId AND asset_category_id = :categoryId";
        MapSqlParameterSource param = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("categoryId", categoryId);
        return template.queryForObject(sql, param, Long.class);
    }
    
    /**
     * 未分類の残高を取得（複数レコードの合計）
     */
    public Long getUncategorizedBalance(Long userId) {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM assets " +
                    "WHERE user_id = :userId AND asset_category_id IS NULL";
        MapSqlParameterSource param = new MapSqlParameterSource().addValue("userId", userId);
        return template.queryForObject(sql, param, Long.class);
    }
    
    /**
     * ユーザーの総資産を取得
     */
    public Long getTotalBalance(Long userId) {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM assets WHERE user_id = :userId";
        MapSqlParameterSource param = new MapSqlParameterSource().addValue("userId", userId);
        return template.queryForObject(sql, param, Long.class);
    }
    
    /**
     * 資産カテゴリの残高を更新（加算/減算）
     */
    public void updateAmountByCategoryId(Long userId, Long assetCategoryId, Long amount) {
        if (assetCategoryId == null) {
            String sql = "UPDATE assets SET " +
                    "amount = amount + :amount " +
                    "WHERE asset_category_id IS NULL AND user_id = :userId";
            MapSqlParameterSource param = new MapSqlParameterSource()
                    .addValue("userId", userId)
                    .addValue("amount", amount);
            template.update(sql, param);
        } else {
            String sql = "UPDATE assets SET " +
                    "amount = amount + :amount " +
                    "WHERE asset_category_id = :assetCategoryId AND user_id = :userId";
            MapSqlParameterSource param = new MapSqlParameterSource()
                    .addValue("userId", userId)
                    .addValue("assetCategoryId", assetCategoryId)
                    .addValue("amount", amount);
            template.update(sql, param);
        }
    }
    
    /**
     * 確定済み総資産を取得（未払いのクレジットカード取引を除外）
     */
    public Long getConfirmedTotalBalance(Long userId) {
        String sql = "SELECT COALESCE(SUM(a.amount), 0) " +
                    "FROM assets a " +
                    "WHERE a.user_id = :userId";
        MapSqlParameterSource param = new MapSqlParameterSource().addValue("userId", userId);
        Long assetsTotal = template.queryForObject(sql, param, Long.class);
        
        // 未払いのクレジットカード取引の合計を取得
        String creditSql = "SELECT COALESCE(SUM(" +
                          "CASE " +
                          "  WHEN transaction_type = 'INCOME' THEN amount " +
                          "  WHEN transaction_type = 'EXPENSE' THEN -amount " +
                          "  ELSE 0 " +
                          "END), 0) " +
                          "FROM card_transactions " +
                          "WHERE user_id = :userId " +
                          "AND card_type = 'CREDIT' " +
                          "AND is_paid = FALSE";
        Long unpaidCreditTotal = template.queryForObject(creditSql, param, Long.class);
        
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
                    "WHERE ct.user_id = :userId " +
                    "AND ct.card_type = 'CREDIT' " +
                    "AND ct.asset_category_id IS NOT NULL " +
                    "AND ct.is_paid = FALSE " +
                    "GROUP BY ct.asset_category_id";
        
        MapSqlParameterSource param = new MapSqlParameterSource().addValue("userId", userId);
        Map<Long, Long> unpaidCreditTotalsByCategory = new HashMap<>();
        template.query(sql, param, rs -> {
            unpaidCreditTotalsByCategory.put(rs.getLong("asset_category_id"), rs.getLong("total"));
        });
        
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
                    "WHERE user_id = :userId " +
                    "AND card_type = 'CREDIT' " +
                    "AND asset_category_id IS NULL " +
                    "AND is_paid = FALSE";
        MapSqlParameterSource param = new MapSqlParameterSource().addValue("userId", userId);
        Long unpaidCreditUncategorized = template.queryForObject(sql, param, Long.class);
        
        return normalUncategorized - unpaidCreditUncategorized;
    }

    public void insert(Asset asset) {
        String sql = "INSERT INTO assets " +
                "(user_id, asset_category_id, amount) " +
                "VALUES (:userId, :assetCategoryId, :amount)";
        MapSqlParameterSource param = new MapSqlParameterSource()
                .addValue("userId", asset.getUserId())
                .addValue("assetCategoryId", asset.getAssetCategoryId())
                .addValue("amount", asset.getAmount());
        template.update(sql, param);
    }
}
