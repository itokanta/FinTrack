package com.example.fintrack.service;

import java.time.LocalDate;
import java.time.YearMonth;

import org.springframework.stereotype.Service;

import com.example.fintrack.domain.TransactionType;
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

    MonthlySummary monthlySummary = new MonthlySummary();

    Long incomeAmount = sumByTransactionType(userId, TransactionType.INCOME, startDate, endDate);

    Long expenseAmount = sumByTransactionType(userId, TransactionType.EXPENSE, startDate, endDate);

    monthlySummary.setIncomeAmount(incomeAmount);
    monthlySummary.setExpenseAmount(expenseAmount);
    monthlySummary.setTotalAmount(incomeAmount - expenseAmount);

    return monthlySummary;
  }

  private Long sumByTransactionType(Long userId, TransactionType transactionType, LocalDate startDate, LocalDate endDate) {
    Long sumAmount = bankTransactionRepository.sumAmountByUserIdAndTransactionDate(userId, transactionType,startDate, endDate) +
        cardTransactionRepository.sumAmountByUserIdAndTransactionDate(userId, transactionType, startDate, endDate) +
        discrepancyTransactionRepository.sumAmountByUserIdAndTransactionDate(userId, transactionType, startDate, endDate) +
        splitTransactionRepository.sumAmountFromBankSplits(userId, transactionType, startDate, endDate) +
        splitTransactionRepository.sumAmountFromCardSplits(userId, transactionType, startDate, endDate) +
        splitTransactionRepository.sumAmountFromDiscrepancySplits(userId, transactionType, startDate, endDate);
    return sumAmount;
  }
}
