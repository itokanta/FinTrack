package com.example.fintrack.repository.mapper;

import com.example.fintrack.domain.ExpenseCategory;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ExpenseCategoryRowMapper implements RowMapper<ExpenseCategory> {
    
    @Override
    public ExpenseCategory mapRow(ResultSet rs, int rowNum) throws SQLException {
        ExpenseCategory category = new ExpenseCategory();
        category.setId(rs.getLong("id"));
        category.setUserId(rs.getLong("user_id"));
        category.setCategoryName(rs.getString("category_name"));
        category.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return category;
    }
}

