package com.example.fintrack.service;

import java.time.LocalDate;
import java.time.YearMonth;

import org.springframework.stereotype.Service;

import com.example.fintrack.domain.dto.MonthlySummary;
import com.example.fintrack.repository.BankTransactionRepository;
import com.example.fintrack.repository.CardTransactionRepository;
import com.example.fintrack.repository.DiscrepancyTransactionRepository;
import com.example.fintrack.repository.SplitTransactionRepository;

@Service
public class MonthlyReportService {
  private final BankTransactionRepository bankTransactionRepository;
  private final CardTransactionRepository cardTransactionRepository;
  private final DiscrepancyTransactionRepository discrepancyTransactionRepository;
  private final SplitTransactionRepository splitTransactionRepository;

  public MonthlyReportService(BankTransactionRepository bankTransactionRepository,
      CardTransactionRepository cardTransactionRepository,
      DiscrepancyTransactionRepository discrepancyTransactionRepository,
      SplitTransactionRepository splitTransactionRepository) {
    this.bankTransactionRepository = bankTransactionRepository;
    this.cardTransactionRepository = cardTransactionRepository;
    this.discrepancyTransactionRepository = discrepancyTransactionRepository;
    this.splitTransactionRepository = splitTransactionRepository;
  }

  public MonthlySummary getMonthlyTotals(Long userId, YearMonth yearMonth){
    LocalDate startDate = yearMonth.atDay(1);
    LocalDate endDate = yearMonth.atEndOfMonth();
    MonthlySummary monthlySummary = new MonthlySummary(0L, 0L, 0L);

    add(monthlySummary, bankTransactionRepository.sumAmountByUserIdAndTransactionDate(userId, startDate, endDate));
    add(monthlySummary, cardTransactionRepository.sumAmountByUserIdAndTransactionDate(userId, startDate, endDate));
    add(monthlySummary, discrepancyTransactionRepository.sumAmountByUserIdAndTransactionDate(userId, startDate, endDate));
    add(monthlySummary, splitTransactionRepository.sumAmountFromBankSplits(userId, startDate, endDate));
    add(monthlySummary, splitTransactionRepository.sumAmountFromCardSplits(userId, startDate, endDate));
    add(monthlySummary, splitTransactionRepository.sumAmountFromDiscrepancySplits(userId, startDate, endDate));

    return monthlySummary;
  }

  private void add(MonthlySummary result, MonthlySummary addData) {
    result.setIncomeAmount(result.getIncomeAmount() + addData.getIncomeAmount());
    result.setExpenseAmount(result.getExpenseAmount() + addData.getExpenseAmount());
    result.setTotalAmount(result.getTotalAmount() + addData.getTotalAmount());
  }
}