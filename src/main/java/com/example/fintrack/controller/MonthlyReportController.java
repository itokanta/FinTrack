package com.example.fintrack.controller;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.fintrack.domain.ExpenseCategory;
import com.example.fintrack.domain.TransactionType;
import com.example.fintrack.domain.dto.MonthlySummary;
import com.example.fintrack.domain.dto.TransactionsByType;
import com.example.fintrack.security.AuthenticationUserIdResolver;
import com.example.fintrack.service.ExpenseCategoryService;
import com.example.fintrack.service.MonthlyReportService;
import com.example.fintrack.service.SplitService;
import com.example.fintrack.service.TransactionService;

@Controller
@RequestMapping("/monthly-report")
public class MonthlyReportController {
  private final MonthlyReportService monthlyReportService;
  private final TransactionService transactionService;
  private final AuthenticationUserIdResolver authenticationUserIdResolver;
  private final ExpenseCategoryService expenseCategoryService;
  private final SplitService splitService;
  
  public MonthlyReportController(MonthlyReportService monthlyReportService, TransactionService transactionService,
      AuthenticationUserIdResolver authenticationUserIdResolver, ExpenseCategoryService expenseCategoryService,
      SplitService splitService) {
    this.monthlyReportService = monthlyReportService;
    this.transactionService = transactionService;
    this.authenticationUserIdResolver = authenticationUserIdResolver;
    this.expenseCategoryService = expenseCategoryService;
    this.splitService = splitService;
  }

  @GetMapping
  public String assetsReport(Authentication authentication, Model model, @RequestParam(required = false) String targetYearMonth){
    Long userId = authenticationUserIdResolver.getUserId(authentication);

    YearMonth yearMonth;
    if (targetYearMonth != null) {
      yearMonth = YearMonth.parse(targetYearMonth);
    } else {
      yearMonth = YearMonth.now();
    }

    MonthlySummary monthlySummaryTotals = monthlyReportService.getMonthlySummaryTotals(userId, yearMonth);
    Map<Long, MonthlySummary> monthlySummaryByAssetCategory = monthlyReportService.getMonthlySummaryByAssetCategory(userId, yearMonth);
    MonthlySummary monthlySummaryUncategorized = monthlyReportService.getMonthlySummaryUncategorized(userId, yearMonth);
    TransactionsByType transactionsByType = transactionService.getTransactionListByUserIdAndTransactionDate(userId, yearMonth);
    List<ExpenseCategory> expenseCategories = expenseCategoryService.findByUserId(userId);
    Map<Long, String> expenseCategoryMap = expenseCategories.stream()
        .collect(Collectors.toMap(ExpenseCategory::getId, ExpenseCategory::getCategoryName));
    Map<Long, String> splitPatternMap = splitService.getPatternNameMap(userId, TransactionType.EXPENSE);

    model.addAttribute("monthlySummaryTotals", monthlySummaryTotals);
    model.addAttribute("monthlySummaryByAssetCategory", monthlySummaryByAssetCategory);
    model.addAttribute("monthlySummaryUncategorized", monthlySummaryUncategorized);
    model.addAttribute("transactionsByType", transactionsByType);
    model.addAttribute("expenseCategoryMap", expenseCategoryMap);
    model.addAttribute("splitPatternMap", splitPatternMap);
    model.addAttribute("targetYearMonth", yearMonth.toString());

    return "monthly-report/index";
  }
}
