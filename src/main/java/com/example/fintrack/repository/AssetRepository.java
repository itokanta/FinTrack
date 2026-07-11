package com.example.fintrack.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.fintrack.domain.Asset;
import com.example.fintrack.domain.AssetCategory;

/**
 * 資産残高（assets テーブル）の DB アクセス。未払いクレジットの除外計算もここで行う。
 *
 * @author kanta ito
 */
@Repository
public class AssetRepository {

    private static final RowMapper<Asset> ROW_MAPPER = (rs, rowNum) -> {
        Asset asset = new Asset();
        asset.setId(rs.getLong("id"));
        asset.setUserId(rs.getLong("user_id"));
        long assetCategoryId = rs.getLong("asset_category_id");
        asset.setAssetCategoryId(rs.wasNull() ? null : assetCategoryId);
        asset.setAmount(rs.getLong("amount"));
        asset.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        asset.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return asset;
    };

    private final NamedParameterJdbcTemplate template;
    private final AssetCategoryRepository assetCategoryRepository;
    
    public AssetRepository(NamedParameterJdbcTemplate template, AssetCategoryRepository assetCategoryRepository) {
        this.template = template;
        this.assetCategoryRepository = assetCategoryRepository;
    }
    
    /**
     * 資産カテゴリの残高を取得する（複数レコードの合計）。
     *
     * @param userId ユーザー ID
     * @param categoryId 資産カテゴリ ID
     * @return 残高合計（レコードがなければ 0）
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
     * 未分類（asset_category_id が NULL）の残高を取得する。
     *
     * @param userId ユーザー ID
     * @return 未分類残高の合計
     */
    public Long getUncategorizedBalance(Long userId) {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM assets " +
                    "WHERE user_id = :userId AND asset_category_id IS NULL";
        MapSqlParameterSource param = new MapSqlParameterSource().addValue("userId", userId);
        return template.queryForObject(sql, param, Long.class);
    }
    
    /**
     * ユーザーの総資産残高を取得する。
     *
     * @param userId ユーザー ID
     * @return 全カテゴリの残高合計
     */
    public Long getTotalBalance(Long userId) {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM assets WHERE user_id = :userId";
        MapSqlParameterSource param = new MapSqlParameterSource().addValue("userId", userId);
        return template.queryForObject(sql, param, Long.class);
    }

    /**
     * ユーザーの資産レコードを全件取得する。
     *
     * @param userId ユーザー ID
     * @return 資産レコードのリスト
     */
    public List<Asset> getAssetListByUserId(Long userId) {
        String sql = "SELECT id, user_id, asset_category_id, amount, created_at, updated_at " +
                    "FROM assets " +
                    "WHERE user_id = :userId";
        MapSqlParameterSource param = new MapSqlParameterSource().addValue("userId", userId);
        return template.query(sql, param, ROW_MAPPER);
    }
    
    /**
     * 資産カテゴリの残高に金額を加算（または減算）する。
     *
     * @param userId ユーザー ID
     * @param assetCategoryId 資産カテゴリ ID（{@code null} のとき未分類）
     * @param amount 加算する金額（減算は負数）
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
     * 確定済み総資産を取得する。未払いクレジットカード取引分を差し引く。
     *
     * @param userId ユーザー ID
     * @return 確定済みの総資産残高
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
     * カテゴリ別の確定済み残高を取得する。未払いクレジットをカテゴリごとに差し引く。
     *
     * @param userId ユーザー ID
     * @return 資産カテゴリ → 確定済み残高の Map
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
     * 未分類の確定済み残高を取得する。未払いの未分類クレジットを差し引く。
     *
     * @param userId ユーザー ID
     * @return 確定済みの未分類残高
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

    /**
     * 資産レコードを新規登録する。
     *
     * @param asset 登録する資産
     */
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

    /**
     * 指定資産カテゴリに紐づく資産レコードをすべて削除する。
     *
     * @param assetCategoryId 資産カテゴリ ID
     * @param userId ユーザー ID
     */
    public void deleteByAssetCategoryId(Long assetCategoryId, Long userId) {
        String sql = "DELETE FROM assets WHERE asset_category_id = :assetCategoryId AND user_id = :userId";
        MapSqlParameterSource param = new MapSqlParameterSource()
                .addValue("assetCategoryId", assetCategoryId)
                .addValue("userId", userId);
        template.update(sql, param);
    }
}
