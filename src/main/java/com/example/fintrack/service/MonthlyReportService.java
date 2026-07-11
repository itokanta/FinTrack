package com.example.fintrack.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.fintrack.domain.Asset;
import com.example.fintrack.domain.AssetCategory;
import com.example.fintrack.domain.dto.MonthlySummary;
import com.example.fintrack.repository.AssetCategoryRepository;
import com.example.fintrack.repository.AssetRepository;
import com.example.fintrack.repository.BankTransactionRepository;
import com.example.fintrack.repository.CardTransactionRepository;
import com.example.fintrack.repository.DiscrepancyTransactionRepository;
import com.example.fintrack.repository.SplitTransactionRepository;

/**
 * 月次の収支サマリーを集計するサービス。
 *
 * @author kanta ito
 */
@Service
public class MonthlyReportService {
  private final BankTransactionRepository bankTransactionRepository;
  private final CardTransactionRepository cardTransactionRepository;
  private final DiscrepancyTransactionRepository discrepancyTransactionRepository;
  private final SplitTransactionRepository splitTransactionRepository;
  private final AssetCategoryRepository assetCategoryRepository;
  private final AssetRepository assetRepository;

  public MonthlyReportService(BankTransactionRepository bankTransactionRepository,
      CardTransactionRepository cardTransactionRepository,
      DiscrepancyTransactionRepository discrepancyTransactionRepository,
      SplitTransactionRepository splitTransactionRepository, AssetCategoryRepository assetCategoryRepository,
      AssetRepository assetRepository) {
    this.bankTransactionRepository = bankTransactionRepository;
    this.cardTransactionRepository = cardTransactionRepository;
    this.discrepancyTransactionRepository = discrepancyTransactionRepository;
    this.splitTransactionRepository = splitTransactionRepository;
    this.assetCategoryRepository = assetCategoryRepository;
    this.assetRepository = assetRepository;
  }

  /**
   * 指定月の収支合計サマリーを取得する（全ソースを集計）。
   *
   * @param userId    ユーザーID
   * @param yearMonth 対象年月
   * @return 収入・支出・合計を含むサマリー
   */
  public MonthlySummary getMonthlySummaryTotals(Long userId, YearMonth yearMonth){
    LocalDate startDate = yearMonth.atDay(1);
    LocalDate endDate = yearMonth.atEndOfMonth();
    Long assetAmount = assetRepository.getTotalBalance(userId);
    MonthlySummary monthlySummary = new MonthlySummary(null, 0L, 0L, 0L, assetAmount);

    add(monthlySummary, bankTransactionRepository.sumAmountByUserIdAndTransactionDate(userId, startDate, endDate));
    add(monthlySummary, cardTransactionRepository.sumAmountByUserIdAndTransactionDate(userId, startDate, endDate));
    add(monthlySummary, discrepancyTransactionRepository.sumAmountByUserIdAndTransactionDate(userId, startDate, endDate));

    return monthlySummary;
  }

  /**
   * 指定月の収支サマリーを資産カテゴリ別に取得する。
   *
   * @param userId    ユーザーID
   * @param yearMonth 対象年月
   * @return 資産カテゴリIDをキー、サマリーを値とするマップ
   */
  public Map<Long, MonthlySummary> getMonthlySummaryByAssetCategory(Long userId, YearMonth yearMonth) {
    LocalDate startDate = yearMonth.atDay(1);
    LocalDate endDate = yearMonth.atEndOfMonth();
    List<AssetCategory> assetCategoryList = assetCategoryRepository.findByUserId(userId);
    List<Asset> assetList = assetRepository.getAssetListByUserId(userId);
    Map<Long, Long> assetAmountMap = assetList.stream().collect(Collectors.toMap(Asset::getAssetCategoryId, Asset::getAmount));
    Map<Long, MonthlySummary> monthlySummaryMap = new HashMap<>();

    for (AssetCategory assetCategory : assetCategoryList) {
      monthlySummaryMap.put(assetCategory.getId(), new MonthlySummary(assetCategory, 0L, 0L, 0L, assetAmountMap.get(assetCategory.getId())));
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

  /**
   * 指定月の未分配取引の収支サマリーを取得する。
   *
   * @param userId    ユーザーID
   * @param yearMonth 対象年月
   * @return 未分類の収入・支出・合計を含むサマリー
   */
  public MonthlySummary getMonthlySummaryUncategorized(Long userId, YearMonth yearMonth) {
    LocalDate startDate = yearMonth.atDay(1);
    LocalDate endDate = yearMonth.atEndOfMonth();
    Long assetAmount = assetRepository.getUncategorizedBalance(userId);
    MonthlySummary monthlySummary = new MonthlySummary(null, 0L, 0L, 0L, assetAmount);

    add(monthlySummary, bankTransactionRepository.sumMonthlySummaryBySplitMethodIsNull(userId, startDate, endDate));
    add(monthlySummary, cardTransactionRepository.sumMonthlySummaryBySplitMethodIsNull(userId, startDate, endDate));
    add(monthlySummary, discrepancyTransactionRepository.sumMonthlySummaryBySplitMethodIsNull(userId, startDate, endDate));

    return monthlySummary;
  }

  /**
   * 指定月・指定資産カテゴリの収支合計サマリーを取得する。
   * 銀行・カード・差異解消の SINGLE 取引と、各ソースの手動・パターン分割を集計する。
   *
   * @param userId          ユーザー ID
   * @param yearMonth       対象年月
   * @param assetCategoryId 資産カテゴリ ID
   * @return 収入・支出・合計を含むサマリー
   */
  public MonthlySummary getMonthlySummaryByAssetCategoryId(Long userId, YearMonth yearMonth, Long assetCategoryId) {
    LocalDate startDate = yearMonth.atDay(1);
    LocalDate endDate = yearMonth.atEndOfMonth();
    AssetCategory assetCategory = assetCategoryRepository.findById(assetCategoryId, userId).orElseThrow(() -> new IllegalArgumentException("資産カテゴリが存在しません"));
    Long assetAmount = assetRepository.getBalanceByCategory(userId, assetCategoryId);
    MonthlySummary monthlySummary = new MonthlySummary(assetCategory, 0L, 0L, 0L, assetAmount);

    add(monthlySummary, bankTransactionRepository.sumSingleAmountByAssetCategoryId(userId, assetCategoryId, startDate, endDate));
    add(monthlySummary, cardTransactionRepository.sumSingleAmountByAssetCategoryId(userId, assetCategoryId, startDate, endDate));
    add(monthlySummary, discrepancyTransactionRepository.sumSingleAmountByAssetCategoryId(userId, assetCategoryId, startDate, endDate));
    add(monthlySummary, splitTransactionRepository.sumAmountByAssetCategoryIdFromBankSplits(userId, assetCategoryId, startDate, endDate));
    add(monthlySummary, splitTransactionRepository.sumAmountByAssetCategoryIdFromCardSplits(userId, assetCategoryId, startDate, endDate));
    add(monthlySummary, splitTransactionRepository.sumAmountByAssetCategoryIdFromDiscrepancySplits(userId, assetCategoryId, startDate, endDate));

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
        resultMap.put(assetCategory.getId(), new MonthlySummary(assetCategory, 0L, 0L, 0L, 0L));
      }

      MonthlySummary monthlySummary = resultMap.get(assetCategory.getId());
      monthlySummary.setIncomeAmount(monthlySummary.getIncomeAmount() + addData.getValue().getIncomeAmount());
      monthlySummary.setExpenseAmount(monthlySummary.getExpenseAmount() + addData.getValue().getExpenseAmount());
      monthlySummary.setTotalAmount(monthlySummary.getTotalAmount() + addData.getValue().getTotalAmount());
    }
  }
}