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

  public void save(CsvImportHistory csvImportHistory) {
    String sql = "INSERT INTO csv_import_histories (user_id, import_type, csv_name, import_date, record_count) VALUES (:userId, :importType, :csvName, :importDate, :recordCount)";
    BeanPropertySqlParameterSource params = new BeanPropertySqlParameterSource(csvImportHistory);
    template.update(sql, params);
  }

  public List<CsvImportHistory> findAllByUserId(Long userId) {
    String sql = "SELECT id, user_id, import_type, csv_name, import_date, record_count FROM csv_import_histories WHERE user_id = :userId";
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("userId", userId);
    return template.query(sql, params, rowMapper);
  }

  public CsvImportHistory findById(Long id) {
    String sql = "SELECT id, user_id, import_type, csv_name, import_date, record_count FROM csv_import_histories WHERE id = :id";
   MapSqlParameterSource params = new MapSqlParameterSource().addValue("id", id);
    return template.queryForObject(sql, params, rowMapper);
  }

  public void deleteById(Long id) {
    String sql = "DELETE FROM csv_import_histories WHERE id = :id";
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("id", id);
    template.update(sql, params);
  }
}
