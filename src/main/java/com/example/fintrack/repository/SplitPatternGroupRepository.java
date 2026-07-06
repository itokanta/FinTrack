package com.example.fintrack.repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.fintrack.domain.SplitPattern;
import com.example.fintrack.domain.SplitPatternGroup;
import com.example.fintrack.domain.TransactionType;

/**
 * 分割パターングループ（split_pattern_groups テーブル）と明細の DB アクセス。
 *
 * @author kanta ito
 */
@Repository
public class SplitPatternGroupRepository {

  private final NamedParameterJdbcTemplate template;

  public SplitPatternGroupRepository(NamedParameterJdbcTemplate template) {
    this.template = template;
  }

  private static final RowMapper<SplitPatternGroup> ROW_MAPPER = (rs, rowNum) -> {
    SplitPatternGroup group = new SplitPatternGroup();
    group.setId(rs.getLong("id"));
    group.setPatternName(rs.getString("pattern_name"));
    group.setUserId(rs.getLong("user_id"));
    String transactionType = rs.getString("transaction_type");
    group.setTransactionType(transactionType == null ? null : TransactionType.valueOf(transactionType));
    group.setSplitType(rs.getString("split_type"));
    group.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
    Timestamp deletedAt = rs.getTimestamp("deleted_at");
    group.setDeletedAt(deletedAt == null ? null : deletedAt.toLocalDateTime());
    return group;
  };

  private static final ResultSetExtractor<List<SplitPatternGroup>> EXTRACTOR = rs -> {
    Map<Long, SplitPatternGroup> splitPatternGroupMap = new LinkedHashMap<>();

    while (rs.next()) {
      Long sgId = rs.getLong("sg_id");

      if (!splitPatternGroupMap.containsKey(sgId)) {
        SplitPatternGroup splitPatternGroup = new SplitPatternGroup();
        splitPatternGroup.setId(sgId);
        splitPatternGroup.setPatternName(rs.getString("pattern_name"));
        splitPatternGroup.setUserId(rs.getLong("sg_user_id"));
        String transactionType = rs.getString("transaction_type");
        splitPatternGroup.setTransactionType(transactionType == null ? null : TransactionType.valueOf(transactionType));
        splitPatternGroup.setSplitType(rs.getString("split_type"));
        splitPatternGroup.setCreatedAt(rs.getTimestamp("sg_created_at").toLocalDateTime());
        Timestamp deletedAt = rs.getTimestamp("deleted_at");
        splitPatternGroup.setDeletedAt(deletedAt == null ? null : deletedAt.toLocalDateTime());
        splitPatternGroup.setSplitPatternList(new ArrayList<>());

        splitPatternGroupMap.put(sgId, splitPatternGroup);
      }

      SplitPattern splitPattern = new SplitPattern();
      long sId = rs.getLong("s_id");
      if (rs.wasNull()) {
        throw new IllegalArgumentException(
            "分割パターンに明細が登録されていません（パターンID: " + sgId + "）");
      }
      splitPattern.setId(sId);
      splitPattern.setPatternId(rs.getLong("pattern_id"));
      splitPattern.setUserId(rs.getLong("s_user_id"));
      splitPattern.setAssetCategoryId(rs.getLong("asset_category_id"));
      BigDecimal splitRatio = rs.getBigDecimal("split_ratio");
      splitPattern.setSplitRatio(rs.wasNull() ? null : splitRatio);
      Long splitAmount = rs.getLong("split_amount");
      splitPattern.setSplitAmount(rs.wasNull() ? null : splitAmount);
      splitPattern.setCreatedAt(rs.getTimestamp("s_created_at").toLocalDateTime());
      splitPattern.setAssetCategoryName(rs.getString("asset_category_name"));

      splitPatternGroupMap.get(sgId).getSplitPatternList().add(splitPattern);
    }
    return new ArrayList<>(splitPatternGroupMap.values());
  };

  /**
   * 分割パターングループを新規登録する。
   *
   * @param splitPatternGroup 登録するグループ
   * @return 採番されたグループ ID
   */
  public Long insert(SplitPatternGroup splitPatternGroup) {
    String sql = "INSERT INTO split_pattern_groups " +
                 "(pattern_name, user_id, transaction_type, split_type) " +
                 "VALUES (:patternName, :userId, :transactionType, :splitType) " +
                 "RETURNING id";
    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("patternName", splitPatternGroup.getPatternName())
        .addValue("userId", splitPatternGroup.getUserId())
        .addValue("transactionType", splitPatternGroup.getTransactionType().name())
        .addValue("splitType", splitPatternGroup.getSplitType());
    return template.queryForObject(sql, params, Long.class);
  }

  /**
   * 分割パターングループと明細・資産カテゴリ名を JOIN して1件取得する。
   *
   * @param userId ユーザー ID
   * @param id グループ ID
   * @return 明細付きの分割パターングループ
   * @throws IllegalArgumentException 該当グループがない、または明細が未登録の場合
   */
  public SplitPatternGroup findWithSplitPatternAndAssetCategoryByUserIdAndId(Long userId, Long id) {
    String sql = "SELECT sg.id AS sg_id , sg.pattern_name, sg.user_id AS sg_user_id, sg.transaction_type, sg.split_type, sg.created_at AS sg_created_at, sg.deleted_at, " +
        "s.id AS s_id, s.pattern_id, s.user_id AS s_user_id, s.asset_category_id, s.split_ratio, s.split_amount, s.created_at AS s_created_at, " +
        "a.category_name AS asset_category_name " +
        "FROM split_pattern_groups sg " +
        "LEFT JOIN split_patterns s ON sg.id = s.pattern_id AND sg.user_id = s.user_id " +
        "LEFT JOIN asset_categories a ON s.asset_category_id  = a.id AND a.user_id = s.user_id " +
        "WHERE sg.user_id = :userId AND sg.id = :id AND sg.deleted_at IS NULL " +
        "ORDER BY sg.id, s.asset_category_id";
    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("userId", userId)
        .addValue("id", id);
    
    List<SplitPatternGroup> splitPatternGroupList = template.query(sql, params, EXTRACTOR);
    if (splitPatternGroupList.isEmpty()){
      throw new IllegalArgumentException("分割パターンが見つかりません");
    }
    return splitPatternGroupList.get(0);
  }

  /**
   * 取引種別で絞り、明細・資産カテゴリ名付きの分割パターングループ一覧を取得する。
   *
   * @param userId ユーザー ID
   * @param transactionType 収入／支出
   * @return 明細付きグループのリスト
   * @throws IllegalArgumentException 明細が未登録のグループが含まれる場合
   */
  public List<SplitPatternGroup> findWithSplitPatternAndAssetCategoryByUserIdAndTransactionType(Long userId, TransactionType transactionType){
    String sql = "SELECT sg.id AS sg_id , sg.pattern_name, sg.user_id AS sg_user_id, sg.transaction_type, sg.split_type, sg.created_at AS sg_created_at, sg.deleted_at, " +
                "s.id AS s_id, s.pattern_id, s.user_id AS s_user_id, s.asset_category_id, s.split_ratio, s.split_amount, s.created_at AS s_created_at, "+
                "a.category_name AS asset_category_name " +
                "FROM split_pattern_groups sg " +
                "LEFT JOIN split_patterns s ON sg.id = s.pattern_id AND sg.user_id = s.user_id " +
                "LEFT JOIN asset_categories a ON s.asset_category_id  = a.id AND a.user_id = s.user_id " +
                "WHERE sg.user_id = :userId AND sg.transaction_type = :transactionType AND sg.deleted_at IS NULL " +
                "ORDER BY sg.id, s.asset_category_id";
    MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("transactionType", transactionType.name());
    return template.query(sql, params, EXTRACTOR);
  }

  /**
   * 取引種別で絞った分割パターングループ一覧を取得する（明細は含まない）。
   *
   * @param userId ユーザー ID
   * @param transactionType 収入／支出
   * @return グループのリスト（削除済みは除外）
   */
  public List<SplitPatternGroup> findByUserIdAndTransactionType(Long userId, TransactionType transactionType) {
    String sql = "SELECT id, pattern_name, user_id, transaction_type, split_type, created_at, deleted_at " +
        "FROM split_pattern_groups " +
        "WHERE user_id = :userId AND transaction_type = :transactionType AND deleted_at IS NULL " +
        "ORDER BY id";
    MapSqlParameterSource param = new MapSqlParameterSource()
        .addValue("userId", userId)
        .addValue("transactionType", transactionType.name());
    return template.query(sql, param, ROW_MAPPER);
  }

  /**
   * 取引種別で絞った分割パターングループ一覧を取得する（削除済みも含む）。
   *
   * @param userId ユーザー ID
   * @param transactionType 収入／支出
   * @return グループのリスト
   */
  public List<SplitPatternGroup> findByUserIdAndTransactionTypeInDeleted(Long userId, TransactionType transactionType) {
    String sql = "SELECT id, pattern_name, user_id, transaction_type, split_type, created_at, deleted_at " +
        "FROM split_pattern_groups " +
        "WHERE user_id = :userId AND transaction_type = :transactionType " +
        "ORDER BY id";
    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("userId", userId)
        .addValue("transactionType", transactionType.name());
    return template.query(sql, params, ROW_MAPPER);
  }

  /**
   * 分割パターングループを論理削除する（deleted_at を設定）。
   *
   * @param id グループ ID
   * @param userId ユーザー ID
   * @return 更新された行数
   */
  public int deleteById(Long id, Long userId) {
    String sql = "UPDATE split_pattern_groups SET deleted_at = CURRENT_TIMESTAMP " +
                 "WHERE id = :id AND user_id = :userId AND deleted_at IS NULL";
    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("id", id)
        .addValue("userId", userId);
    return template.update(sql, params);
  }
}
