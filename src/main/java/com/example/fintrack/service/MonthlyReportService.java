package com.example.fintrack.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.fintrack.domain.AssetCategory;
import com.example.fintrack.domain.dto.MonthlySummary;
import com.example.fintrack.repository.AssetCategoryRepository;
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
  private final AssetCategoryRepository assetCategoryRepository;

  public MonthlyReportService(BankTransactionRepository bankTransactionRepository,
      CardTransactionRepository cardTransactionRepository,
      DiscrepancyTransactionRepository discrepancyTransactionRepository,
      SplitTransactionRepository splitTransactionRepository, AssetCategoryRepository assetCategoryRepository) {
    this.bankTransactionRepository = bankTransactionRepository;
    this.cardTransactionRepository = cardTransactionRepository;
    this.discrepancyTransactionRepository = discrepancyTransactionRepository;
    this.splitTransactionRepository = splitTransactionRepository;
    this.assetCategoryRepository = assetCategoryRepository;
  }

  public MonthlySummary getMonthlySummaryTotals(Long userId, YearMonth yearMonth){
    LocalDate startDate = yearMonth.atDay(1);
    LocalDate endDate = yearMonth.atEndOfMonth();
    MonthlySummary monthlySummary = new MonthlySummary(null, 0L, 0L, 0L);

    add(monthlySummary, bankTransactionRepository.sumAmountByUserIdAndTransactionDate(userId, startDate, endDate));
    add(monthlySummary, cardTransactionRepository.sumAmountByUserIdAndTransactionDate(userId, startDate, endDate));
    add(monthlySummary, discrepancyTransactionRepository.sumAmountByUserIdAndTransactionDate(userId, startDate, endDate));
    add(monthlySummary, splitTransactionRepository.sumAmountFromBankSplits(userId, startDate, endDate));
    add(monthlySummary, splitTransactionRepository.sumAmountFromCardSplits(userId, startDate, endDate));
    add(monthlySummary, splitTransactionRepository.sumAmountFromDiscrepancySplits(userId, startDate, endDate));

    return monthlySummary;
  }

  public Map<Long, MonthlySummary> getMonthlySummaryByAssetCategory(Long userId, YearMonth yearMonth) {
    LocalDate startDate = yearMonth.atDay(1);
    LocalDate endDate = yearMonth.atEndOfMonth();
    List<AssetCategory> assetCategoryList = assetCategoryRepository.findByUserId(userId);
    Map<Long, MonthlySummary> monthlySummaryMap = new HashMap<>();

    for (AssetCategory assetCategory : assetCategoryList) {
      monthlySummaryMap.put(assetCategory.getId(), new MonthlySummary(assetCategory, 0L, 0L, 0L));
    }

    Map<Long, AssetCategory> assetCategoryMap = assetCategoryList.stream().collect(Collectors.toMap(AssetCategory::getId, ac -> ac));

    add(monthlySummaryMap, bankTransactionRepository.sumMonthlySummaryByAssetCategory(userId, startDate, endDate), assetCategoryMap, userId);
    add(monthlySummaryMap, cardTransactionRepository.sumMonthlySummaryByAssetCategory(userId, startDate, endDate), assetCategoryMap, userId);
    add(monthlySummaryMap, discrepancyTransactionRepository.sumMonthlySummaryByAssetCategory(userId, startDate, endDate), assetCategoryMap, userId);
    add(monthlySummaryMap, splitTransactionRepository.sumMonthlySummaryByAssetCategoryFromBankSplits(userId, startDate, endDate), assetCategoryMap, userId);
    add(monthlySummaryMap, splitTransactionRepository.sumMonthlySummaryByAssetCategoryFromCardSplits(userId, startDate, endDate), assetCategoryMap, userId);
    add(monthlySummaryMap, splitTransactionRepository.sumMonthlySummaryByAssetCategoryFromDiscrepancySplits(userId, startDate, endDate), assetCategoryMap, userId);

    return monthlySummaryMap;
  }

  public MonthlySummary getMonthlySummaryUncategorized(Long userId, YearMonth yearMonth) {
    LocalDate startDate = yearMonth.atDay(1);
    LocalDate endDate = yearMonth.atEndOfMonth();
    MonthlySummary monthlySummary = new MonthlySummary(null, 0L, 0L, 0L);

    add(monthlySummary, bankTransactionRepository.sumMonthlySummaryBySplitMethodIsNull(userId, startDate, endDate));
    add(monthlySummary, cardTransactionRepository.sumMonthlySummaryBySplitMethodIsNull(userId, startDate, endDate));
    add(monthlySummary, discrepancyTransactionRepository.sumMonthlySummaryBySplitMethodIsNull(userId, startDate, endDate));

    return monthlySummary;
  }

  private void add(MonthlySummary result, MonthlySummary addData) {
    result.setIncomeAmount(result.getIncomeAmount() + addData.getIncomeAmount());
    result.setExpenseAmount(result.getExpenseAmount() + addData.getExpenseAmount());
    result.setTotalAmount(result.getTotalAmount() + addData.getTotalAmount());
  }

  private void add(Map<Long, MonthlySummary> resultMap, Map<Long, MonthlySummary> addDataMap, Map<Long, AssetCategory> assetCategoryMap, Long userId) {
    for (Map.Entry<Long, MonthlySummary> addData : addDataMap.entrySet()){
      Long addDataKey = addData.getKey();

      if (addDataKey == null) {
        continue;
      }

      AssetCategory assetCategory = assetCategoryMap.get(addDataKey);
      
      if (assetCategory == null) {
        AssetCategory unknownAssetCategory = new AssetCategory();
        unknownAssetCategory.setId(addDataKey);
        unknownAssetCategory.setUserId(userId);
        unknownAssetCategory.setCategoryName("削除済みカテゴリ " + unknownAssetCategory.getId());
        assetCategoryMap.put(unknownAssetCategory.getId(), unknownAssetCategory);
        assetCategory = unknownAssetCategory;
      }

      if (!resultMap.containsKey(assetCategory.getId())) {
        resultMap.put(assetCategory.getId(), new MonthlySummary(assetCategory, 0L, 0L, 0L));
      }

      MonthlySummary monthlySummary = resultMap.get(assetCategory.getId());
      monthlySummary.setIncomeAmount(monthlySummary.getIncomeAmount() + addData.getValue().getIncomeAmount());
      monthlySummary.setExpenseAmount(monthlySummary.getExpenseAmount() + addData.getValue().getExpenseAmount());
      monthlySummary.setTotalAmount(monthlySummary.getTotalAmount() + addData.getValue().getTotalAmount());
    }
  }
}