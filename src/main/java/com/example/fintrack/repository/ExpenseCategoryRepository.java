package com.example.fintrack.repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.fintrack.domain.ExpenseCategory;
import org.springframework.jdbc.core.RowMapper;

@Repository
public class ExpenseCategoryRepository {

    private static final RowMapper<ExpenseCategory> ROW_MAPPER = (rs, rowNum) -> {
        ExpenseCategory category = new ExpenseCategory();
        category.setId(rs.getLong("id"));
        category.setUserId(rs.getLong("user_id"));
        category.setCategoryName(rs.getString("category_name"));
        category.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return category;
    };

    private final NamedParameterJdbcTemplate template;

    public ExpenseCategoryRepository(NamedParameterJdbcTemplate template) {
        this.template = template;
    }

    public List<ExpenseCategory> findByUserId(Long userId) {
        String sql = "SELECT id, user_id, category_name, created_at FROM expense_categories WHERE user_id = :userId ORDER BY id";
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        return template.query(sql, params, ROW_MAPPER);
    }

    public ExpenseCategory save(ExpenseCategory category) {
        if (category.getId() == null) {
            String sql = "INSERT INTO expense_categories (user_id, category_name) " +
                         "VALUES (:userId, :categoryName) RETURNING id, created_at";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("userId", category.getUserId())
                    .addValue("categoryName", category.getCategoryName());
            Map<String, Object> result = template.queryForMap(sql, params);
            category.setId(((Number) result.get("id")).longValue());
            category.setCreatedAt(((Timestamp) result.get("created_at")).toLocalDateTime());
        } else {
            String sql = "UPDATE expense_categories SET user_id = :userId, category_name = :categoryName WHERE id = :id AND user_id = :userId";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("userId", category.getUserId())
                    .addValue("categoryName", category.getCategoryName())
                    .addValue("id", category.getId())
                    .addValue("userId", category.getUserId());
            template.update(sql, params);
        }
        return category;
    }

    public void deleteById(Long id, Long userId) {
        String sql = "DELETE FROM expense_categories WHERE id = :id AND user_id = :userId";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("userId", userId);
        template.update(sql, params);
    }
}
