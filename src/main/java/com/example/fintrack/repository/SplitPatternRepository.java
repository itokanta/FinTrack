package com.example.fintrack.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import com.example.fintrack.domain.SplitPattern;
import org.springframework.jdbc.core.RowMapper;

@Repository
public class SplitPatternRepository {

    private static final RowMapper<SplitPattern> ROW_MAPPER = (rs, rowNum) -> {
        SplitPattern pattern = new SplitPattern();
        pattern.setId(rs.getLong("id"));
        pattern.setPatternId(rs.getLong("pattern_id"));
        pattern.setUserId(rs.getLong("user_id"));
        pattern.setAssetCategoryId(rs.getLong("asset_category_id"));
        BigDecimal splitRatio = rs.getBigDecimal("split_ratio");
        pattern.setSplitRatio(rs.wasNull() ? null : splitRatio);
        Long splitAmount = rs.getLong("split_amount");
        pattern.setSplitAmount(rs.wasNull() ? null : splitAmount);
        pattern.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return pattern;
    };

    private final NamedParameterJdbcTemplate template;

    public SplitPatternRepository(NamedParameterJdbcTemplate template) {
        this.template = template;
    }

    public void insert(List<SplitPattern> splitPatternList) {
        String sql = "INSERT INTO split_patterns " +
                     "(pattern_id, user_id, asset_category_id, split_ratio, split_amount) " +
                     "VALUES (:patternId, :userId, :assetCategoryId, :splitRatio, :splitAmount)";
        SqlParameterSource[] batchParams = new SqlParameterSource[splitPatternList.size()];
        for (int i = 0; i < splitPatternList.size(); i++) {
            SplitPattern pattern = splitPatternList.get(i);
            batchParams[i] = new MapSqlParameterSource()
                    .addValue("patternId", pattern.getPatternId())
                    .addValue("userId", pattern.getUserId())
                    .addValue("assetCategoryId", pattern.getAssetCategoryId())
                    .addValue("splitRatio", pattern.getSplitRatio())
                    .addValue("splitAmount", pattern.getSplitAmount());
        }
        template.batchUpdate(sql, batchParams);
    }

}
