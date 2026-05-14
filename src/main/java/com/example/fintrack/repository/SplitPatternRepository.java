package com.example.fintrack.repository;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.fintrack.domain.SplitPattern;
import com.example.fintrack.domain.TransactionType;
import org.springframework.jdbc.core.RowMapper;

@Repository
public class SplitPatternRepository {

    private static final RowMapper<SplitPattern> ROW_MAPPER = (rs, rowNum) -> {
        SplitPattern pattern = new SplitPattern();
        pattern.setId(rs.getLong("id"));
        pattern.setPatternId(rs.getLong("pattern_id"));
        pattern.setUserId(rs.getLong("user_id"));
        String transactionType = rs.getString("transaction_type");
        pattern.setTransactionType(transactionType == null ? null : TransactionType.valueOf(transactionType));
        pattern.setSplitType(rs.getString("split_type"));
        pattern.setAssetCategoryId(rs.getLong("asset_category_id"));
        if (rs.getBigDecimal("split_ratio") != null) {
            pattern.setSplitRatio(rs.getBigDecimal("split_ratio"));
        }
        long splitAmount = rs.getLong("split_amount");
        pattern.setSplitAmount(rs.wasNull() ? null : splitAmount);
        pattern.setPatternName(rs.getString("pattern_name"));
        pattern.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return pattern;
    };

    private static final RowMapper<SplitPattern> SUMMARY_ROW_MAPPER = (rs, rowNum) -> {
        SplitPattern pattern = new SplitPattern();
        pattern.setId(rs.getLong("id"));
        pattern.setPatternId(rs.getLong("pattern_id"));
        pattern.setUserId(rs.getLong("user_id"));
        String transactionType = rs.getString("transaction_type");
        pattern.setTransactionType(transactionType == null ? null : TransactionType.valueOf(transactionType));
        pattern.setSplitType(rs.getString("split_type"));
        pattern.setAssetCategoryId(rs.getLong("asset_category_id"));
        if (rs.getBigDecimal("split_ratio") != null) {
            pattern.setSplitRatio(rs.getBigDecimal("split_ratio"));
        }
        long splitAmount = rs.getLong("split_amount");
        pattern.setSplitAmount(rs.wasNull() ? null : splitAmount);
        pattern.setPatternName(rs.getString("pattern_name"));
        pattern.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        String assetCategoryName = rs.getString("asset_category_name");
        pattern.setAssetCategoryName(rs.wasNull() ? null : assetCategoryName);
        return pattern;
    };

    private final NamedParameterJdbcTemplate template;

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

    public List<SplitPattern> findWithCategoryByUserIdAndTransactionType(Long userId, TransactionType transactionType) {
        String sql = "SELECT s.id, s.pattern_id, s.user_id, s.transaction_type, s.split_type, s.asset_category_id, " +
                     "s.split_ratio, s.split_amount, s.pattern_name, s.created_at, " +
                     "a.category_name AS asset_category_name " +
                     "FROM split_patterns s " +
                     "LEFT JOIN asset_categories a ON s.asset_category_id = a.id AND a.user_id = s.user_id " +
                     "WHERE s.user_id = :userId AND s.transaction_type = :transactionType " +
                     "ORDER BY s.pattern_id, s.asset_category_id";
        MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("userId", userId)
                    .addValue("transactionType", transactionType.name());
        return template.query(sql, params, SUMMARY_ROW_MAPPER);
    }

    public Long findMaxPatternId() {
        String sql = "SELECT COALESCE(MAX(pattern_id), 0) FROM split_patterns";
        return template.queryForObject(sql, new MapSqlParameterSource(), Long.class);
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
                .addValue("transactionType",
                        pattern.getTransactionType() == null ? null : pattern.getTransactionType().name())
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
