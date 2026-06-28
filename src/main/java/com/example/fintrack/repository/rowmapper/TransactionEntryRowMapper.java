package com.example.fintrack.repository.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import com.example.fintrack.domain.SplitMethod;
import com.example.fintrack.domain.TransactionType;
import com.example.fintrack.domain.dto.TransactionEntry;

public class TransactionEntryRowMapper implements RowMapper<TransactionEntry> {

  @Override
  public TransactionEntry mapRow(ResultSet rs, int rowNum) throws SQLException {
    TransactionEntry transactionEntry = new TransactionEntry();
    transactionEntry.setDescription(rs.getString("description"));
    transactionEntry.setAmount(rs.getLong("amount"));
    transactionEntry.setTransactionType(TransactionType.valueOf(rs.getString("transaction_type")));
    transactionEntry.setTransactionDate(rs.getDate("transaction_date").toLocalDate());
    Long assetCategoryId = rs.getLong("asset_category_id");
    transactionEntry.setAssetCategoryId(rs.wasNull() ? null : assetCategoryId);
    Long expenseCategoryId = rs.getLong("expense_category_id");
    transactionEntry.setExpenseCategoryId(rs.wasNull() ? null : expenseCategoryId);
    String splitMethod = rs.getString("split_method");
    Long splitPatternId = rs.getLong("split_pattern_id");
    transactionEntry.setSplitPatternId(rs.wasNull() ? null : splitPatternId);
    transactionEntry.setSplitMethod(splitMethod == null ? null : SplitMethod.valueOf(splitMethod));
    return transactionEntry;
  }
}
