package com.example.fintrack.repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.fintrack.domain.AutoCategoryRule;
import com.example.fintrack.domain.TransactionType;
import org.springframework.jdbc.core.RowMapper;

/**
 * 自動カテゴリ割当ルール（auto_category_rules テーブル）の DB アクセス。
 *
 * @author kanta ito
 */
@Repository
public class AutoCategoryRuleRepository {

    private static final RowMapper<AutoCategoryRule> ROW_MAPPER = (rs, rowNum) -> {
        AutoCategoryRule rule = new AutoCategoryRule();
        rule.setId(rs.getLong("id"));
        rule.setUserId(rs.getLong("user_id"));
        rule.setAssetCategoryId(rs.getLong("asset_category_id"));
        rule.setDescriptionPattern(rs.getString("description_pattern"));
        rule.setTransactionType(TransactionType.valueOf(rs.getString("transaction_type")));
        rule.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return rule;
    };

    private final NamedParameterJdbcTemplate template;

    public AutoCategoryRuleRepository(NamedParameterJdbcTemplate template) {
        this.template = template;
    }

    /**
     * ID で自動カテゴリルールを検索する。
     *
     * @param id ルール ID
     * @param userId ユーザー ID
     * @return 見つかればルールを含む Optional、なければ空
     */
    public Optional<AutoCategoryRule> findById(Long id, Long userId) {
        String sql = "SELECT id, user_id, asset_category_id, description_pattern, transaction_type, created_at " +
                     "FROM auto_category_rules " +
                     "WHERE id = :id AND user_id = :userId";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("userId", userId);
        List<AutoCategoryRule> rules = template.query(sql, params, ROW_MAPPER);
        if (rules.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rules.get(0));
    }

    /**
     * ユーザーの自動カテゴリルール一覧を取得する。
     *
     * @param userId ユーザー ID
     * @return ルールのリスト（作成日時降順）
     */
    public List<AutoCategoryRule> findByUserId(Long userId) {
        String sql = "SELECT id, user_id, asset_category_id, description_pattern, transaction_type, created_at " +
                     "FROM auto_category_rules WHERE user_id = :userId ORDER BY created_at DESC";
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        return template.query(sql, params, ROW_MAPPER);
    }

    /**
     * 自動カテゴリルールを新規登録または更新する。
     *
     * @param rule 保存するルール
     * @return ID・作成日時が反映されたルール
     */
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
                         "WHERE id = :id AND user_id = :userId";
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

    /**
     * 自動カテゴリルールを削除する。
     *
     * @param id ルール ID
     * @param userId ユーザー ID
     */
    public void deleteById(Long id, Long userId) {
        String sql = "DELETE FROM auto_category_rules WHERE id = :id AND user_id = :userId";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("userId", userId);
        template.update(sql, params);
    }
}
