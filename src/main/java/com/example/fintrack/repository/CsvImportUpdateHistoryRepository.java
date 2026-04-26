package com.example.fintrack.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import org.springframework.jdbc.core.RowMapper;

import com.example.fintrack.domain.CsvImportUpdateField;
import com.example.fintrack.domain.CsvImportUpdateHistory;

@Repository
public class CsvImportUpdateHistoryRepository {

  private static final RowMapper<CsvImportUpdateHistory> ROW_MAPPER = (rs, rowNum) -> {
    CsvImportUpdateHistory history = new CsvImportUpdateHistory();
    history.setId(rs.getLong("id"));
    history.setCsvImportHistoryId(rs.getLong("csv_import_history_id"));
    history.setBankTransactionId(rs.getObject("bank_transaction_id", Long.class));
    history.setCardTransactionId(rs.getObject("card_transaction_id", Long.class));
    history.setTargetField(CsvImportUpdateField.valueOf(rs.getString("target_field")));
    history.setValueBefore(rs.getString("value_before"));
    history.setValueAfter(rs.getString("value_after"));
    history.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
    history.setUserId(rs.getLong("user_id"));
    history.setUpdateCount(rs.getLong("update_count"));
    history.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
    return history;
  };

  private final NamedParameterJdbcTemplate template;

  public CsvImportUpdateHistoryRepository(NamedParameterJdbcTemplate template) {
    this.template = template;
  }

  public void insert(List<CsvImportUpdateHistory> csvImportUpdateHistoryList) {
    String sql = "INSERT INTO csv_import_update_histories (csv_import_history_id, bank_transaction_id, card_transaction_id, target_field, value_before, value_after, user_id, update_count) " +
                "VALUES (:csvImportHistoryId, :bankTransactionId, :cardTransactionId, :targetField, :valueBefore, :valueAfter, :userId, :updateCount)";
    SqlParameterSource[] params = new SqlParameterSource[csvImportUpdateHistoryList.size()];
    for (int i = 0; i < csvImportUpdateHistoryList.size(); i++) {
      CsvImportUpdateHistory h = csvImportUpdateHistoryList.get(i);
      params[i] = new MapSqlParameterSource()
        .addValue("csvImportHistoryId", h.getCsvImportHistoryId())
        .addValue("bankTransactionId", h.getBankTransactionId())
        .addValue("cardTransactionId", h.getCardTransactionId())
        .addValue("targetField", h.getTargetField().name())
        .addValue("valueBefore", h.getValueBefore())
        .addValue("valueAfter", h.getValueAfter())
        .addValue("userId", h.getUserId())
        .addValue("updateCount", h.getUpdateCount());
    }
    template.batchUpdate(sql, params);
  }

  public Map<Long, List<CsvImportUpdateHistory>> findByCardTransactionIdInTargetCsv(Long userId, Long csvImportHistoryId) {
    String sql = "SELECT id, csv_import_history_id, bank_transaction_id, card_transaction_id, target_field, value_before, value_after, created_at, user_id, update_count, updated_at " +
                "FROM csv_import_update_histories " +
                "WHERE card_transaction_id IN (" +
                  "SELECT card_transaction_id " +
                  "FROM csv_import_update_histories " +
                  "WHERE csv_import_history_id = :csvImportHistoryId AND card_transaction_id IS NOT NULL " +
                ") AND user_id = :userId " +
                "ORDER BY card_transaction_id, update_count";
    MapSqlParameterSource params = new MapSqlParameterSource()
      .addValue("csvImportHistoryId", csvImportHistoryId)
      .addValue("userId", userId);
    List<CsvImportUpdateHistory> csvImportUpdateHistoryList = template.query(sql, params, ROW_MAPPER);

    Map<Long, List<CsvImportUpdateHistory>> result = new HashMap<>();
    for (CsvImportUpdateHistory csvImportUpdateHistory : csvImportUpdateHistoryList){
      Long key = csvImportUpdateHistory.getCardTransactionId();
      if(!result.containsKey(key)){
        result.put(key, new ArrayList<>());
      }
      result.get(key).add(csvImportUpdateHistory);
    }
    return result;
  }

  public void batchUpdateValueBefore(List<CsvImportUpdateHistory> historyList) {
    String sql = "UPDATE csv_import_update_histories SET value_before = :valueBefore WHERE id = :id AND user_id = :userId";
    SqlParameterSource[] params = new SqlParameterSource[historyList.size()];
    for (int i = 0; i < historyList.size(); i++) {
      CsvImportUpdateHistory history = historyList.get(i);
      params[i] = new MapSqlParameterSource()
        .addValue("valueBefore", history.getValueBefore())
        .addValue("id", history.getId())
        .addValue("userId", history.getUserId());
    }
    template.batchUpdate(sql, params);
  }
}
