package com.example.fintrack.repository;

import java.util.List;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import com.example.fintrack.domain.CsvImportHistory;

/**
 * CSV インポート履歴（csv_import_histories テーブル）の DB アクセス。
 *
 * @author kanta ito
 */
@Repository
public class CsvImportHistoryRepository {

  private static final RowMapper<CsvImportHistory> ROW_MAPPER = (rs, rowNum) -> {
    CsvImportHistory history = new CsvImportHistory();
    history.setId(rs.getLong("id"));
    history.setUserId(rs.getLong("user_id"));
    history.setImportType(rs.getString("import_type"));
    history.setCsvName(rs.getString("csv_name"));
    history.setImportDate(rs.getTimestamp("import_date").toLocalDateTime());
    history.setRecordCount(rs.getLong("record_count"));
    return history;
  };

  private final NamedParameterJdbcTemplate template;

  public CsvImportHistoryRepository(NamedParameterJdbcTemplate template) {
    this.template = template;
  }

  /**
   * CSV インポート履歴を新規登録する。
   *
   * @param csvImportHistory 登録する履歴
   * @return 採番された ID
   */
  public Long save(CsvImportHistory csvImportHistory) {
    String sql = "INSERT INTO csv_import_histories (user_id, import_type, csv_name, record_count) VALUES (:userId, :importType, :csvName, :recordCount) RETURNING id";
    BeanPropertySqlParameterSource params = new BeanPropertySqlParameterSource(csvImportHistory);
    return template.queryForObject(sql, params, Long.class);
  }

  /**
   * ユーザーの CSV インポート履歴一覧を取得する。
   *
   * @param userId ユーザー ID
   * @return 履歴のリスト（インポート日時降順）
   */
  public List<CsvImportHistory> findAllByUserId(Long userId) {
    String sql = "SELECT id, user_id, import_type, csv_name, import_date, record_count FROM csv_import_histories WHERE user_id = :userId ORDER BY import_date DESC";
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("userId", userId);
    return template.query(sql, params, ROW_MAPPER);
  }

  /**
   * ID で CSV インポート履歴を取得する。
   *
   * @param id 履歴 ID
   * @param userId ユーザー ID
   * @return 該当する履歴
   * @throws org.springframework.dao.EmptyResultDataAccessException 該当レコードがない場合
   */
  public CsvImportHistory findById(Long id, Long userId) {
    String sql = "SELECT id, user_id, import_type, csv_name, import_date, record_count FROM csv_import_histories WHERE id = :id AND user_id = :userId";
   MapSqlParameterSource params = new MapSqlParameterSource()
    .addValue("id", id)
    .addValue("userId", userId);
    return template.queryForObject(sql, params, ROW_MAPPER);
  }

  /**
   * CSV インポート履歴を削除する。
   *
   * @param id 履歴 ID
   * @param userId ユーザー ID
   */
  public void deleteById(Long id, Long userId) {
    String sql = "DELETE FROM csv_import_histories WHERE id = :id AND user_id = :userId";
    MapSqlParameterSource params = new MapSqlParameterSource()
      .addValue("id", id)
      .addValue("userId", userId);
    template.update(sql, params);
  }
}
