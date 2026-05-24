package com.example.fintrack.repository;

import com.example.fintrack.domain.AssetCategory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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

    public Optional<AssetCategory> findById(Long id) {
        String sql = "SELECT id, user_id, category_name, created_at FROM asset_categories WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource("id", id);
        List<AssetCategory> categories = template.query(sql, params, ROW_MAPPER);
        if (categories.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(categories.get(0));
    }

    public List<AssetCategory> findByUserId(Long userId) {
        String sql = "SELECT id, user_id, category_name, created_at FROM asset_categories WHERE user_id = :userId ORDER BY id";
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        return template.query(sql, params, ROW_MAPPER);
    }

    public Long insert(AssetCategory category) {
        String sql = "INSERT INTO asset_categories (user_id, category_name) " +
                     "VALUES (:userId, :categoryName) " +
                     "RETURNING id";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", category.getUserId())
                .addValue("categoryName", category.getCategoryName());
        return template.queryForObject(sql, params, Long.class);
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM asset_categories WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource("id", id);
        template.update(sql, params);
    }
}
