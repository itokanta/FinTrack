package com.example.fintrack.repository;

import com.example.fintrack.domain.ExpenseCategory;
import com.example.fintrack.repository.mapper.ExpenseCategoryRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class ExpenseCategoryRepository {
    
    private final JdbcTemplate jdbcTemplate;
    private final ExpenseCategoryRowMapper rowMapper = new ExpenseCategoryRowMapper();
    
    public ExpenseCategoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    public Optional<ExpenseCategory> findById(Long id) {
        String sql = "SELECT * FROM expense_categories WHERE id = ?";
        List<ExpenseCategory> categories = jdbcTemplate.query(sql, rowMapper, id);
        if (categories.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(categories.get(0));
    }
    
    public List<ExpenseCategory> findByUserId(Long userId) {
        String sql = "SELECT * FROM expense_categories WHERE user_id = ? ORDER BY id";
        return jdbcTemplate.query(sql, rowMapper, userId);
    }
    
    public ExpenseCategory save(ExpenseCategory category) {
        if (category.getId() == null) {
            // INSERT
            String sql = "INSERT INTO expense_categories (user_id, category_name, created_at) " +
                        "VALUES (?, ?, ?) RETURNING id";
            LocalDateTime now = LocalDateTime.now();
            Long id = jdbcTemplate.queryForObject(sql, Long.class,
                category.getUserId(), category.getCategoryName(), now);
            category.setId(id);
            category.setCreatedAt(now);
        } else {
            // UPDATE
            String sql = "UPDATE expense_categories SET user_id = ?, category_name = ? WHERE id = ?";
            jdbcTemplate.update(sql, category.getUserId(), category.getCategoryName(), category.getId());
        }
        return category;
    }
    
    public void deleteById(Long id) {
        String sql = "DELETE FROM expense_categories WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }
}
