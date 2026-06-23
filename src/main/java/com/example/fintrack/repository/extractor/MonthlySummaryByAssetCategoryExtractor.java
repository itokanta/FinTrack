package com.example.fintrack.repository.extractor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

import com.example.fintrack.domain.dto.MonthlySummary;

public class MonthlySummaryByAssetCategoryExtractor implements ResultSetExtractor<Map<Long, MonthlySummary>>{
  
  @Override
  public Map<Long, MonthlySummary> extractData(ResultSet rs) throws SQLException, DataAccessException {
    Map<Long, MonthlySummary> monthlySummaryMap = new HashMap<>();

    while (rs.next()) {
      long assetCategoryId = rs.getLong("asset_category_id");
      Long key = rs.wasNull() ? null : assetCategoryId;
      long incomeAmount = rs.getLong("income_amount");
      long expenseAmount = rs.getLong("expense_amount");

      if(key != null ){
        MonthlySummary monthlySummary = new MonthlySummary();
        monthlySummary.setIncomeAmount(incomeAmount);
        monthlySummary.setExpenseAmount(expenseAmount);
        monthlySummary.setTotalAmount(incomeAmount - expenseAmount);
        monthlySummaryMap.put(key, monthlySummary);
      }
    }

    return monthlySummaryMap;
  }
}

