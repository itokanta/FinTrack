package com.example.fintrack.repository.extractor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

import com.example.fintrack.domain.dto.MonthlySummary;

/**
 * 資産カテゴリ別の月次収支を {@link Map} に集約する ResultSetExtractor。
 *
 * @author kanta ito
 */
public class MonthlySummaryByAssetCategoryExtractor implements ResultSetExtractor<Map<Long, MonthlySummary>>{
  
  /**
   * ResultSet を走査し、資産カテゴリ ID をキーとした月次サマリの Map を組み立てる。
   * asset_category_id が NULL の行はスキップする。
   *
   * @param rs ResultSet
   * @return 資産カテゴリ ID → 月次サマリの Map
   * @throws SQLException JDBC エラー
   * @throws DataAccessException データアクセスエラー
   */
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

