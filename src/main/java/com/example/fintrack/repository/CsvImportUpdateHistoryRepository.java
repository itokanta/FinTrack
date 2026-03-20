package com.example.fintrack.repository;

import java.util.List;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import com.example.fintrack.domain.CsvImportUpdateHistory;
import com.example.fintrack.repository.mapper.CsvImportUpdateHistoryRowMapper;

@Repository
public class CsvImportUpdateHistoryRepository {

  private final NamedParameterJdbcTemplate template;
  private final CsvImportUpdateHistoryRowMapper rowMapper = new CsvImportUpdateHistoryRowMapper();

  public CsvImportUpdateHistoryRepository(NamedParameterJdbcTemplate template) {
    this.template = template;
  }

  public void save(CsvImportUpdateHistory csvImportUpdateHistory) {
    String sql = "INSERT INTO csv_import_update_histories (csv_import_history_id, bank_transaction_id, card_transaction_id, target_field, value_before, value_after) VALUES (:csvImportHistoryId, :bankTransactionId, :cardTransactionId, :targetField, :valueBefore, :valueAfter)";
    BeanPropertySqlParameterSource params = new BeanPropertySqlParameterSource(csvImportUpdateHistory);
    template.update(sql, params);
  }

  public List<CsvImportUpdateHistory> findAllByCsvImportHistoryId(Long csvImportHistoryId) {
    String sql = "SELECT id, csv_import_history_id, bank_transaction_id, card_transaction_id, target_field, value_before, value_after, created_at FROM csv_import_update_histories WHERE csv_import_history_id = :csvImportHistoryId";
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("csvImportHistoryId", csvImportHistoryId);
    return template.query(sql, params, rowMapper);
  }

  public void deleteAllByCsvImportHistoryId(Long csvImportHistoryId) {
    String sql = "DELETE FROM csv_import_update_histories WHERE csv_import_history_id = :csvImportHistoryId";
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("csvImportHistoryId", csvImportHistoryId);
    template.update(sql, params);
  }
}
