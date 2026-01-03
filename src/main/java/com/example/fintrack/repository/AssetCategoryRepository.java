package com.example.fintrack.repository;

import com.example.fintrack.domain.AssetCategory;
import com.example.fintrack.repository.mapper.AssetCategoryRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class AssetCategoryRepository {
    
    private final JdbcTemplate jdbcTemplate;
    private final AssetCategoryRowMapper rowMapper = new AssetCategoryRowMapper();
    
    public AssetCategoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    public Optional<AssetCategory> findById(Long id) {
        String sql = "SELECT * FROM asset_categories WHERE id = ?";
        List<AssetCategory> categories = jdbcTemplate.query(sql, rowMapper, id);
        if (categories.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(categories.get(0));
    }
    
    public List<AssetCategory> findByUserId(Long userId) {
        String sql = "SELECT * FROM asset_categories WHERE user_id = ? ORDER BY id";
        return jdbcTemplate.query(sql, rowMapper, userId);
    }
    
    public AssetCategory save(AssetCategory category) {
        if (category.getId() == null) {
            // INSERT
            String sql = "INSERT INTO asset_categories (user_id, category_name, created_at) " +
                        "VALUES (?, ?, ?) RETURNING id";
            LocalDateTime now = LocalDateTime.now();
            Long id = jdbcTemplate.queryForObject(sql, Long.class,
                category.getUserId(), category.getCategoryName(), now);
            category.setId(id);
            category.setCreatedAt(now);
        } else {
            // UPDATE
            String sql = "UPDATE asset_categories SET user_id = ?, category_name = ? WHERE id = ?";
            jdbcTemplate.update(sql, category.getUserId(), category.getCategoryName(), category.getId());
        }
        return category;
    }
}
