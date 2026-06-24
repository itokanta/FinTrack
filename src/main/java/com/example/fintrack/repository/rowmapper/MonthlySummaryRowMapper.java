package com.example.fintrack.repository.rowmapper;


import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import com.example.fintrack.domain.dto.MonthlySummary;

public class MonthlySummaryRowMapper implements RowMapper<MonthlySummary> {

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
