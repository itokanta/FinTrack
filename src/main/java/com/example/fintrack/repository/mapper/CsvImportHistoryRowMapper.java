package com.example.fintrack.repository.mapper;

import org.springframework.jdbc.core.RowMapper;
import com.example.fintrack.domain.CsvImportHistory;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CsvImportHistoryRowMapper implements RowMapper<CsvImportHistory> {

  @Override
  public CsvImportHistory mapRow(ResultSet rs, int rowNum) throws SQLException {
    CsvImportHistory csvImportHistory = new CsvImportHistory();
    csvImportHistory.setId(rs.getLong("id"));
    csvImportHistory.setUserId(rs.getLong("user_id"));
    csvImportHistory.setImportType(rs.getString("import_type"));
    csvImportHistory.setCsvName(rs.getString("csv_name"));
    csvImportHistory.setImportDate(rs.getTimestamp("import_date").toLocalDateTime());
    csvImportHistory.setRecordCount(rs.getLong("record_count"));
    return csvImportHistory;
  }
}
