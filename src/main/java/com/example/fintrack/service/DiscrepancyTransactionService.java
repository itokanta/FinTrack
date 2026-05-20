package com.example.fintrack.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fintrack.domain.BankTransaction;
import com.example.fintrack.domain.CardPaymentDiscrepancy;
import com.example.fintrack.domain.CardTransaction;
import com.example.fintrack.domain.CsvImportUpdateField;
import com.example.fintrack.domain.CsvImportUpdateHistory;
import com.example.fintrack.domain.DiscrepancyTransaction;
import com.example.fintrack.domain.SplitMethod;
import com.example.fintrack.domain.SplitPattern;
import com.example.fintrack.domain.SplitPatternGroup;
import com.example.fintrack.domain.SplitTransaction;
import com.example.fintrack.domain.TransactionType;
import com.example.fintrack.repository.AssetRepository;
import com.example.fintrack.repository.BankTransactionRepository;
import com.example.fintrack.repository.CardPaymentDiscrepancyRepository;
import com.example.fintrack.repository.CardTransactionRepository;
import com.example.fintrack.repository.CsvImportUpdateHistoryRepository;
import com.example.fintrack.repository.DiscrepancyTransactionRepository;
import com.example.fintrack.repository.SplitPatternGroupRepository;
import com.example.fintrack.repository.SplitTransactionRepository;

@Service
public class DiscrepancyTransactionService {

  private final DiscrepancyTransactionRepository discrepancyTransactionRepository;
  private final CardPaymentDiscrepancyRepository cardPaymentDiscrepancyRepository;
  private final CardTransactionRepository cardTransactionRepository;
  private final BankTransactionRepository bankTransactionRepository;
  private final AssetRepository assetRepository;
  private final SplitPatternGroupRepository splitPatternGroupRepository;
  private final SplitTransactionRepository splitTransactionRepository;
  private final CsvImportUpdateHistoryRepository csvImportUpdateHistoryRepository;

  public DiscrepancyTransactionService(DiscrepancyTransactionRepository discrepancyTransactionRepository,
      CardPaymentDiscrepancyRepository cardPaymentDiscrepancyRepository,
      CardTransactionRepository cardTransactionRepository, BankTransactionRepository bankTransactionRepository,
      AssetRepository assetRepository, SplitPatternGroupRepository splitPatternGroupRepository,
      SplitTransactionRepository splitTransactionRepository,
      CsvImportUpdateHistoryRepository csvImportUpdateHistoryRepository) {
    this.discrepancyTransactionRepository = discrepancyTransactionRepository;
    this.cardPaymentDiscrepancyRepository = cardPaymentDiscrepancyRepository;
    this.cardTransactionRepository = cardTransactionRepository;
    this.bankTransactionRepository = bankTransactionRepository;
    this.assetRepository = assetRepository;
    this.splitPatternGroupRepository = splitPatternGroupRepository;
    this.splitTransactionRepository = splitTransactionRepository;
    this.csvImportUpdateHistoryRepository = csvImportUpdateHistoryRepository;
  }

  /**
   * 差異を単一カテゴリに登録して解決
   * 
   * @param discrepancyId   差異ID
   * @param description     差異の説明
   * @param assetCategoryId 資産カテゴリID
   */
  @Transactional
  public void resolveDiscrepancySingle(Long discrepancyId, String description, Long assetCategoryId) {
    // 1. 差異情報を取得
    CardPaymentDiscrepancy discrepancy = getAndValidateDiscrepancy(discrepancyId);

    // 2. 差異取引データを作成（split_method = SINGLE: 単一カテゴリ）
    Long absoluteAmount = Math.abs(discrepancy.getDifferenceAmount());
    TransactionType transactionType = determineTransactionType(discrepancy.getDifferenceAmount());

    DiscrepancyTransaction transaction = createDiscrepancyTransaction(
        discrepancy, description, absoluteAmount, transactionType, assetCategoryId, null);
    transaction.setSplitPatternId(null);
    transaction.setSplitMethod(SplitMethod.SINGLE);
    discrepancyTransactionRepository.save(transaction);

    // 3. 資産残高を調整
    Long amountDelta = calculateAmountDelta(transactionType, absoluteAmount);
    assetRepository.updateAmountByCategoryId(discrepancy.getUserId(), assetCategoryId, amountDelta);

    // 4. 後処理
    finalizeDiscrepancyResolution(discrepancy);
  }

  /**
   * 差異を手動分割して解決
   * 
   * @param discrepancyId   差異ID
   * @param description     差異の説明
   * @param categoryAmounts カテゴリID → 金額のマップ
   */
  @Transactional
  public void resolveDiscrepancyManual(Long discrepancyId, String description,
      Map<Long, Long> categoryAmounts) {
    // 1. 差異情報を取得
    CardPaymentDiscrepancy discrepancy = getAndValidateDiscrepancy(discrepancyId);

    // 2. 合計金額の検証
    Long absoluteAmount = Math.abs(discrepancy.getDifferenceAmount());
    long totalAmount = categoryAmounts.values().stream().mapToLong(Long::longValue).sum();

    if (totalAmount != absoluteAmount) {
      throw new IllegalArgumentException(
          "分割金額の合計が差異金額と一致しません（差異: " + absoluteAmount +
              "円, 合計: " + totalAmount + "円）");
    }

    // 3. 差異取引データを1レコードのみ作成（split_method = MANUAL, asset_category_id = NULL）
    TransactionType transactionType = determineTransactionType(discrepancy.getDifferenceAmount());
    
    DiscrepancyTransaction transaction = createDiscrepancyTransaction(
        discrepancy, description, absoluteAmount, transactionType, null, null);
    transaction.setSplitPatternId(null);
    transaction.setSplitMethod(SplitMethod.MANUAL);
    DiscrepancyTransaction savedTransaction = discrepancyTransactionRepository.save(transaction);

    // 4. 各カテゴリに split_transactions レコードを作成
    for (Map.Entry<Long, Long> entry : categoryAmounts.entrySet()) {
      Long assetCategoryId = entry.getKey();
      Long amount = entry.getValue();

      SplitTransaction splitTransaction = new SplitTransaction();
      splitTransaction.setTransactionType(transactionType);
      splitTransaction.setAssetCategoryId(assetCategoryId);
      splitTransaction.setSplitPatternId(null);
      splitTransaction.setSplitMethod(SplitMethod.MANUAL);
      splitTransaction.setAmount(amount);
      splitTransaction.setBankTransactionId(null);
      splitTransaction.setCardTransactionId(null);
      splitTransaction.setDiscrepancyTransactionId(savedTransaction.getId());
      
      splitTransactionRepository.save(splitTransaction);

      // 資産残高を調整
      Long amountDelta = calculateAmountDelta(transactionType, amount);
      assetRepository.updateAmountByCategoryId(discrepancy.getUserId(), assetCategoryId, amountDelta);
    }

    // 5. 後処理
    finalizeDiscrepancyResolution(discrepancy);
  }

  /**
   * 差異を分割パターンで解決
   * 
   * @param discrepancyId 差異ID
   * @param description   差異の説明
   * @param patternId     分割パターンID
   */
  @Transactional
  public void resolveDiscrepancyPattern(Long discrepancyId, String description, Long patternId) {
    // 1. 差異情報を取得
    CardPaymentDiscrepancy discrepancy = getAndValidateDiscrepancy(discrepancyId);

    Long userId = discrepancy.getUserId();

    // 2. 分割パターンを取得
    SplitPatternGroup splitPatternGroup = splitPatternGroupRepository.findWithSplitPatternAndAssetCategoryByUserIdAndId(userId, patternId);

    // 3. 差異額と収支区分を判定
    Long absoluteAmount = Math.abs(discrepancy.getDifferenceAmount());
    TransactionType transactionType = determineTransactionType(discrepancy.getDifferenceAmount());

    // 4. 差異取引データを1レコードのみ作成（split_method = PATTERN, split_pattern_id = patternId, asset_category_id = NULL）
    DiscrepancyTransaction transaction = createDiscrepancyTransaction(
        discrepancy, description, absoluteAmount, transactionType, null, null);
    transaction.setSplitPatternId(patternId);
    transaction.setSplitMethod(SplitMethod.PATTERN);
    DiscrepancyTransaction savedTransaction = discrepancyTransactionRepository.save(transaction);

    // 5. パターンに従って split_transactions レコードを作成
    String splitType = splitPatternGroup.getSplitType();

    List<SplitPattern> splitPatternList = splitPatternGroup.getSplitPatternList();
    Long totalSplitAmount = 0L;

    if ("RATIO".equals(splitType)) {
      // 割合指定の場合
      for (int i = 0; i < splitPatternList.size(); i++) {
        SplitPattern pattern = splitPatternList.get(i);
        Long splitAmount = 0L;

        if (i == splitPatternList.size() - 1){
          splitAmount = absoluteAmount - totalSplitAmount;
        } else {
          splitAmount = absoluteAmount * pattern.getSplitRatio().longValue() / 100;
        }

        totalSplitAmount += splitAmount;

        SplitTransaction splitTransaction = new SplitTransaction();
        splitTransaction.setTransactionType(transactionType);
        splitTransaction.setAssetCategoryId(pattern.getAssetCategoryId());
        splitTransaction.setSplitPatternId(patternId);
        splitTransaction.setSplitMethod(SplitMethod.PATTERN);
        splitTransaction.setAmount(splitAmount);
        splitTransaction.setBankTransactionId(null);
        splitTransaction.setCardTransactionId(null);
        splitTransaction.setDiscrepancyTransactionId(savedTransaction.getId());
        
        splitTransactionRepository.save(splitTransaction);

        // 資産残高を調整
        Long amountDelta = calculateAmountDelta(transactionType, splitAmount);
        assetRepository.updateAmountByCategoryId(discrepancy.getUserId(), pattern.getAssetCategoryId(), amountDelta);
      }
    } else if ("AMOUNT".equals(splitType)) {

      Long patternAmountSum = 0L;
      for (SplitPattern splitPattern : splitPatternList) {
        patternAmountSum += splitPattern.getSplitAmount();
      }

      if (!absoluteAmount.equals(patternAmountSum)){
        throw new IllegalArgumentException("分割パターンの金額合計が取引金額と一致しません");
      }

      // 金額指定の場合
      for (SplitPattern pattern : splitPatternList) {
        Long splitAmount = pattern.getSplitAmount();

        SplitTransaction splitTransaction = new SplitTransaction();
        splitTransaction.setTransactionType(transactionType);
        splitTransaction.setAssetCategoryId(pattern.getAssetCategoryId());
        splitTransaction.setSplitPatternId(patternId);
        splitTransaction.setSplitMethod(SplitMethod.PATTERN);
        splitTransaction.setAmount(splitAmount);
        splitTransaction.setBankTransactionId(null);
        splitTransaction.setCardTransactionId(null);
        splitTransaction.setDiscrepancyTransactionId(savedTransaction.getId());
        
        splitTransactionRepository.save(splitTransaction);

        // 資産残高を調整
        Long amountDelta = calculateAmountDelta(transactionType, splitAmount);
        assetRepository.updateAmountByCategoryId(discrepancy.getUserId(),
            pattern.getAssetCategoryId(), amountDelta);
      }
    } else {
      throw new IllegalArgumentException("不明な分割タイプです: " + splitType);
    }

    // 6. 後処理
    finalizeDiscrepancyResolution(discrepancy);
  }

  // ========== プライベートヘルパーメソッド ==========

  private CardPaymentDiscrepancy getAndValidateDiscrepancy(Long discrepancyId) {
    CardPaymentDiscrepancy discrepancy = cardPaymentDiscrepancyRepository.findById(discrepancyId)
        .orElseThrow(() -> new IllegalArgumentException("差異が見つかりません（ID: " + discrepancyId + "）"));

    if (Boolean.TRUE.equals(discrepancy.getIsResolved())) {
      throw new IllegalStateException("この差異は既に解決済みです");
    }

    return discrepancy;
  }

  private TransactionType determineTransactionType(Long differenceAmount) {
    if (differenceAmount > 0) {
      return TransactionType.EXPENSE; // プラス = 追加支出
    } else if (differenceAmount < 0) {
      return TransactionType.INCOME; // マイナス = 返金・調整
    } else {
      throw new IllegalArgumentException("差異金額が0です");
    }
  }

  private DiscrepancyTransaction createDiscrepancyTransaction(
      CardPaymentDiscrepancy discrepancy,
      String description,
      Long amount,
      TransactionType transactionType,
      Long assetCategoryId,
      Long expenseCategoryId) {

    DiscrepancyTransaction transaction = new DiscrepancyTransaction();
    transaction.setDescription(description);
    transaction.setAmount(amount);
    transaction.setTransactionType(transactionType);
    transaction.setTransactionDate(LocalDate.now());
    transaction.setUserId(discrepancy.getUserId());
    transaction.setBankTransactionId(discrepancy.getBankTransactionId());
    transaction.setAssetCategoryId(assetCategoryId);
    transaction.setExpenseCategoryId(expenseCategoryId);
    transaction.setSplitPatternId(null);
    transaction.setCardPaymentDiscrepancyId(discrepancy.getId());

    return transaction;
  }

  private Long calculateAmountDelta(TransactionType transactionType, Long amount) {
    return transactionType == TransactionType.EXPENSE ? -amount : amount;
  }

  private void finalizeDiscrepancyResolution(CardPaymentDiscrepancy discrepancy) {
    Long userId = discrepancy.getUserId();
    String paymentMonth = discrepancy.getPaymentMonth();
    List<CardTransaction> beforeCardTransactionList = cardTransactionRepository.findByUserIdAndPaymentMonth(userId, paymentMonth);
    Map<Long, CardTransaction> beforeByIdMap = new HashMap<>();
    for (CardTransaction beforeCardTransaction : beforeCardTransactionList) {
        beforeByIdMap.put(beforeCardTransaction.getId(), beforeCardTransaction);
    }

    // カード取引を引き落とし済みに更新
    cardTransactionRepository.markAsPaidByPaymentMonth(userId, paymentMonth);

    List<CardTransaction> afterCardTransactionList = cardTransactionRepository.findByUserIdAndPaymentMonth(discrepancy.getUserId(), discrepancy.getPaymentMonth());
    List<CsvImportUpdateHistory> csvImportUpdateHistoryList = new ArrayList<>();
    Optional<BankTransaction> cardPaymentTransaction = bankTransactionRepository.findById(discrepancy.getBankTransactionId());
    Long csvImportHistoryId = cardPaymentTransaction
        .orElseThrow(() -> new IllegalStateException(
            "card payment bank transaction not found. id=" + discrepancy.getBankTransactionId())).getCsvImportHistoryId();
    if (csvImportHistoryId != null){
      for (CardTransaction afterCardTransaction : afterCardTransactionList) {
        CardTransaction beforeTargetCardTransaction = beforeByIdMap.get(afterCardTransaction.getId());
        if (beforeTargetCardTransaction == null) {
          throw new IllegalStateException("before transaction not found. id=" + afterCardTransaction.getId());
        }
        CsvImportUpdateHistory csvImportUpdateHistory = new CsvImportUpdateHistory();
        csvImportUpdateHistory.setUserId(userId);
        csvImportUpdateHistory.setCsvImportHistoryId(csvImportHistoryId);
        csvImportUpdateHistory.setCardTransactionId(afterCardTransaction.getId());
        csvImportUpdateHistory.setTargetField(CsvImportUpdateField.IS_PAID);
        csvImportUpdateHistory.setUpdateCount(afterCardTransaction.getUpdateCount());
        if (beforeTargetCardTransaction.getIsPaid() != null){
          csvImportUpdateHistory.setValueBefore(beforeTargetCardTransaction.getIsPaid().toString());
        }
        csvImportUpdateHistory.setValueAfter(afterCardTransaction.getIsPaid().toString());

        csvImportUpdateHistoryList.add(csvImportUpdateHistory);
      }
      csvImportUpdateHistoryRepository.insert(csvImportUpdateHistoryList);
    }
    
    // 差異を解決済みにマーク
    cardPaymentDiscrepancyRepository.markAsResolved(discrepancy.getId());
  }
}