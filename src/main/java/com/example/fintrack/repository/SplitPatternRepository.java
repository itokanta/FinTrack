package com.example.fintrack.repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.fintrack.domain.SplitPattern;
import com.example.fintrack.repository.mapper.SplitPatternRowMapper;

@Repository
public class SplitPatternRepository {

    private final NamedParameterJdbcTemplate template;
    private static final SplitPatternRowMapper ROW_MAPPER = new SplitPatternRowMapper();

    public SplitPatternRepository(NamedParameterJdbcTemplate template) {
        this.template = template;
    }

    public List<SplitPattern> findById(Long patternId) {
        String sql = "SELECT id, pattern_id, user_id, transaction_type, split_type, asset_category_id, " +
                     "split_ratio, split_amount, pattern_name, created_at " +
                     "FROM split_patterns WHERE pattern_id = :patternId ORDER BY asset_category_id";
        MapSqlParameterSource params = new MapSqlParameterSource("patternId", patternId);
        return template.query(sql, params, ROW_MAPPER);
    }

    public Long findMaxPatternId() {
        String sql = "SELECT COALESCE(MAX(pattern_id), 0) FROM split_patterns";
        return template.queryForObject(sql, new MapSqlParameterSource(), Long.class);
    }

    public List<Long> findDistinctPatternIdsByUserIdAndTransactionType(Long userId, String transactionType) {
        String sql = "SELECT DISTINCT pattern_id FROM split_patterns " +
                     "WHERE user_id = :userId AND transaction_type = :transactionType ORDER BY pattern_id";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("transactionType", transactionType);
        return template.queryForList(sql, params, Long.class);
    }

    public SplitPattern save(SplitPattern pattern) {
        // idが自動採番される
        String sql = "INSERT INTO split_patterns " +
                     "(pattern_id, user_id, transaction_type, split_type, asset_category_id, " +
                     "split_ratio, split_amount, pattern_name) " +
                     "VALUES (:patternId, :userId, :transactionType, :splitType, :assetCategoryId, " +
                     ":splitRatio, :splitAmount, :patternName) RETURNING id, created_at";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("patternId", pattern.getPatternId())
                .addValue("userId", pattern.getUserId())
                .addValue("transactionType", pattern.getTransactionType())
                .addValue("splitType", pattern.getSplitType())
                .addValue("assetCategoryId", pattern.getAssetCategoryId())
                .addValue("splitRatio", pattern.getSplitRatio())
                .addValue("splitAmount", pattern.getSplitAmount())
                .addValue("patternName", pattern.getPatternName());
        Map<String, Object> result = template.queryForMap(sql, params);
        pattern.setId(((Number) result.get("id")).longValue());
        pattern.setCreatedAt(((Timestamp) result.get("created_at")).toLocalDateTime());
        return pattern;
    }

    public void deleteByPatternId(Long patternId) {
        String sql = "DELETE FROM split_patterns WHERE pattern_id = :patternId";
        MapSqlParameterSource params = new MapSqlParameterSource("patternId", patternId);
        template.update(sql, params);
    }
}
