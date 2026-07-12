package com.example.fintrack.repository;

import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import com.example.fintrack.domain.SplitPattern;

/**
 * 分割パターン明細（split_patterns テーブル）の DB アクセス。
 *
 * @author kanta ito
 */
@Repository
public class SplitPatternRepository {

    private final NamedParameterJdbcTemplate template;

    public SplitPatternRepository(NamedParameterJdbcTemplate template) {
        this.template = template;
    }

    /**
     * 分割パターン明細を一括登録する。
     *
     * @param splitPatternList 登録する明細のリスト
     */
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
