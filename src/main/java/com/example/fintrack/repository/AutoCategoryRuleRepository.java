package com.example.fintrack.repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.fintrack.domain.AutoCategoryRule;
import com.example.fintrack.repository.mapper.AutoCategoryRuleRowMapper;

@Repository
public class AutoCategoryRuleRepository {

    private final NamedParameterJdbcTemplate template;
    private static final AutoCategoryRuleRowMapper ROW_MAPPER = new AutoCategoryRuleRowMapper();

    public AutoCategoryRuleRepository(NamedParameterJdbcTemplate template) {
        this.template = template;
    }

    public Optional<AutoCategoryRule> findById(Long id) {
        String sql = "SELECT id, user_id, asset_category_id, description_pattern, transaction_type, created_at " +
                     "FROM auto_category_rules WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource("id", id);
        List<AutoCategoryRule> rules = template.query(sql, params, ROW_MAPPER);
        if (rules.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rules.get(0));
    }

    public List<AutoCategoryRule> findByUserId(Long userId) {
        String sql = "SELECT id, user_id, asset_category_id, description_pattern, transaction_type, created_at " +
                     "FROM auto_category_rules WHERE user_id = :userId ORDER BY created_at DESC";
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        return template.query(sql, params, ROW_MAPPER);
    }

    public AutoCategoryRule save(AutoCategoryRule rule) {
        if (rule.getId() == null) {
            String sql = "INSERT INTO auto_category_rules " +
                         "(user_id, asset_category_id, description_pattern, transaction_type) " +
                         "VALUES (:userId, :assetCategoryId, :descriptionPattern, :transactionType) " +
                         "RETURNING id, created_at";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("userId", rule.getUserId())
                    .addValue("assetCategoryId", rule.getAssetCategoryId())
                    .addValue("descriptionPattern", rule.getDescriptionPattern())
                    .addValue("transactionType", rule.getTransactionType().name());
            Map<String, Object> result = template.queryForMap(sql, params);
            rule.setId(((Number) result.get("id")).longValue());
            rule.setCreatedAt(((Timestamp) result.get("created_at")).toLocalDateTime());
        } else {
            String sql = "UPDATE auto_category_rules SET " +
                         "user_id = :userId, asset_category_id = :assetCategoryId, " +
                         "description_pattern = :descriptionPattern, transaction_type = :transactionType " +
                         "WHERE id = :id";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("userId", rule.getUserId())
                    .addValue("assetCategoryId", rule.getAssetCategoryId())
                    .addValue("descriptionPattern", rule.getDescriptionPattern())
                    .addValue("transactionType", rule.getTransactionType().name())
                    .addValue("id", rule.getId());
            template.update(sql, params);
        }
        return rule;
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM auto_category_rules WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource("id", id);
        template.update(sql, params);
    }
}
