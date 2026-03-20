package com.example.fintrack.repository.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;
import com.example.fintrack.domain.CsvImportUpdateField;
import com.example.fintrack.domain.CsvImportUpdateHistory;

public class CsvImportUpdateHistoryRowMapper implements RowMapper<CsvImportUpdateHistory> {
  @Override
  public CsvImportUpdateHistory mapRow(ResultSet rs, int rowNum) throws SQLException {
    CsvImportUpdateHistory csvImportUpdateHistory = new CsvImportUpdateHistory();
    csvImportUpdateHistory.setId(rs.getLong("id"));
    csvImportUpdateHistory.setCsvImportHistoryId(rs.getLong("csv_import_history_id"));
    csvImportUpdateHistory.setBankTransactionId(rs.getLong("bank_transaction_id"));
    csvImportUpdateHistory.setCardTransactionId(rs.getLong("card_transaction_id"));
    csvImportUpdateHistory.setTargetField(CsvImportUpdateField.fromDbValue(rs.getString("target_field")));
    csvImportUpdateHistory.setValueBefore(rs.getString("value_before"));
    csvImportUpdateHistory.setValueAfter(rs.getString("value_after"));
    csvImportUpdateHistory.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
    return csvImportUpdateHistory;
  }
}
