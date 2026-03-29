package com.example.fintrack.repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.fintrack.domain.ExpenseCategory;
import com.example.fintrack.repository.mapper.ExpenseCategoryRowMapper;

@Repository
public class ExpenseCategoryRepository {

    private final NamedParameterJdbcTemplate template;
    private static final ExpenseCategoryRowMapper ROW_MAPPER = new ExpenseCategoryRowMapper();

    public ExpenseCategoryRepository(NamedParameterJdbcTemplate template) {
        this.template = template;
    }

    public Optional<ExpenseCategory> findById(Long id) {
        String sql = "SELECT id, user_id, category_name, created_at FROM expense_categories WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource("id", id);
        List<ExpenseCategory> categories = template.query(sql, params, ROW_MAPPER);
        if (categories.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(categories.get(0));
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
            String sql = "UPDATE expense_categories SET user_id = :userId, category_name = :categoryName WHERE id = :id";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("userId", category.getUserId())
                    .addValue("categoryName", category.getCategoryName())
                    .addValue("id", category.getId());
            template.update(sql, params);
        }
        return category;
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM expense_categories WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource("id", id);
        template.update(sql, params);
    }
}
