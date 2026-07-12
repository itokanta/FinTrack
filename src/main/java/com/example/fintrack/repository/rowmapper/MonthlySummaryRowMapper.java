package com.example.fintrack.repository.rowmapper;


import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import com.example.fintrack.domain.dto.MonthlySummary;

/**
 * 月次収支サマリ（income_amount / expense_amount）用の RowMapper。
 *
 * @author kanta ito
 */
public class MonthlySummaryRowMapper implements RowMapper<MonthlySummary> {

  /**
   * ResultSet の1行を {@link MonthlySummary} に変換する。totalAmount は収入 − 支出で算出する。
   *
   * @param rs ResultSet
   * @param rowNum 行番号（0始まり）
   * @return マッピングした月次サマリ
   * @throws SQLException JDBC エラー
   */
  @Override
  public MonthlySummary mapRow(ResultSet rs, int rowNum) throws SQLException {
    MonthlySummary monthlySummary = new MonthlySummary();
    long incomeAmount = rs.getLong("income_amount");
    long expenseAmount = rs.getLong("expense_amount");
    monthlySummary.setIncomeAmount(incomeAmount);
    monthlySummary.setExpenseAmount(expenseAmount);
    monthlySummary.setTotalAmount(incomeAmount - expenseAmount);

    return monthlySummary;
  }
}
