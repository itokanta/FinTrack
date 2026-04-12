package com.example.fintrack.repository;

import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;
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

  public void insert(List<CsvImportUpdateHistory> csvImportUpdateHistoryList) {
    String sql = "INSERT INTO csv_import_update_histories (csv_import_history_id, bank_transaction_id, card_transaction_id, target_field, value_before, value_after, user_id, update_count) " +
                "VALUES (:csvImportHistoryId, :bankTransactionId, :cardTransactionId, :targetField, :valueBefore, :valueAfter, :userId, :updateCount)";
    SqlParameterSource[] params = SqlParameterSourceUtils.createBatch(csvImportUpdateHistoryList.toArray());
    template.batchUpdate(sql, params);
  }

  public List<CsvImportUpdateHistory> findAllByCsvImportHistoryId(Long csvImportHistoryId, Long userId) {
    String sql = "SELECT id, csv_import_history_id, bank_transaction_id, card_transaction_id, target_field, value_before, value_after, created_at, user_id, update_count, updated_at " +
                "FROM csv_import_update_histories " +
                "WHERE csv_import_history_id = :csvImportHistoryId AND user_id = :userId";
    MapSqlParameterSource params = new MapSqlParameterSource()
      .addValue("csvImportHistoryId", csvImportHistoryId)
      .addValue("userId", userId);
    return template.query(sql, params, rowMapper);
  }

  public void deleteAllByCsvImportHistoryId(Long csvImportHistoryId, Long userId) {
    String sql = "DELETE FROM csv_import_update_histories WHERE csv_import_history_id = :csvImportHistoryId AND user_id = :userId";
    MapSqlParameterSource params = new MapSqlParameterSource()
      .addValue("csvImportHistoryId", csvImportHistoryId)
      .addValue("userId", userId);
    template.update(sql, params);
  }
}
