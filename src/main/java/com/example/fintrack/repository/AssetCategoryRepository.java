package com.example.fintrack.repository;

import com.example.fintrack.domain.AssetCategory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 資産カテゴリ（asset_categories テーブル）の DB アクセス。
 *
 * @author kanta ito
 */
@Repository
public class AssetCategoryRepository {

    private static final RowMapper<AssetCategory> ROW_MAPPER = (rs, rowNum) -> {
        AssetCategory category = new AssetCategory();
        category.setId(rs.getLong("id"));
        category.setUserId(rs.getLong("user_id"));
        category.setCategoryName(rs.getString("category_name"));
        category.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return category;
    };

    private final NamedParameterJdbcTemplate template;

    public AssetCategoryRepository(NamedParameterJdbcTemplate template) {
        this.template = template;
    }

    /**
     * ID で資産カテゴリを検索する。
     *
     * @param id 資産カテゴリ ID
     * @param userId ユーザー ID
     * @return 見つかればカテゴリを含む Optional、なければ空
     */
    public Optional<AssetCategory> findById(Long id, Long userId) {
        String sql = "SELECT id, user_id, category_name, created_at FROM asset_categories "
                + "WHERE id = :id AND user_id = :userId";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("userId", userId);
        List<AssetCategory> categories = template.query(sql, params, ROW_MAPPER);
        if (categories.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(categories.get(0));
    }

    /**
     * ユーザーの資産カテゴリ一覧を取得する。
     *
     * @param userId ユーザー ID
     * @return 資産カテゴリのリスト（ID 昇順）
     */
    public List<AssetCategory> findByUserId(Long userId) {
        String sql = "SELECT id, user_id, category_name, created_at FROM asset_categories WHERE user_id = :userId ORDER BY id";
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        return template.query(sql, params, ROW_MAPPER);
    }

    /**
     * 資産カテゴリを新規登録する。
     *
     * @param category 登録する資産カテゴリ
     * @return 採番された ID
     */
    public Long insert(AssetCategory category) {
        String sql = "INSERT INTO asset_categories (user_id, category_name) " +
                     "VALUES (:userId, :categoryName) " +
                     "RETURNING id";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", category.getUserId())
                .addValue("categoryName", category.getCategoryName());
        return template.queryForObject(sql, params, Long.class);
    }

    /**
     * 資産カテゴリを削除する。
     *
     * @param id 資産カテゴリ ID
     * @param userId ユーザー ID
     */
    public void deleteById(Long id, Long userId) {
        String sql = "DELETE FROM asset_categories WHERE id = :id AND user_id = :userId";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("userId", userId);
        template.update(sql, params);
    }
}
