package com.example.fintrack.repository;

import java.util.List;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import com.example.fintrack.domain.CsvImportHistory;
import com.example.fintrack.repository.mapper.CsvImportHistoryRowMapper;

@Repository
public class CsvImportHistoryRepository {

  private final NamedParameterJdbcTemplate template;
  private final CsvImportHistoryRowMapper rowMapper = new CsvImportHistoryRowMapper();

  public CsvImportHistoryRepository(NamedParameterJdbcTemplate template) {
    this.template = template;
  }

  public Long save(CsvImportHistory csvImportHistory) {
    String sql = "INSERT INTO csv_import_histories (user_id, import_type, csv_name, record_count) VALUES (:userId, :importType, :csvName, :recordCount) RETURNING id";
    BeanPropertySqlParameterSource params = new BeanPropertySqlParameterSource(csvImportHistory);
    return template.queryForObject(sql, params, Long.class);
  }

  public List<CsvImportHistory> findAllByUserId(Long userId) {
    String sql = "SELECT id, user_id, import_type, csv_name, import_date, record_count FROM csv_import_histories WHERE user_id = :userId ORDER BY import_date DESC";
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("userId", userId);
    return template.query(sql, params, rowMapper);
  }

  public CsvImportHistory findById(Long id, Long userId) {
    String sql = "SELECT id, user_id, import_type, csv_name, import_date, record_count FROM csv_import_histories WHERE id = :id AND user_id = :userId";
   MapSqlParameterSource params = new MapSqlParameterSource()
    .addValue("id", id)
    .addValue("userId", userId);
    return template.queryForObject(sql, params, rowMapper);
  }

  public void deleteById(Long id, Long userId) {
    String sql = "DELETE FROM csv_import_histories WHERE id = :id AND user_id = :userId";
    MapSqlParameterSource params = new MapSqlParameterSource()
      .addValue("id", id)
      .addValue("userId", userId);
    template.update(sql, params);
  }
}
