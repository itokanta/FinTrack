package com.example.fintrack.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fintrack.domain.BankTransaction;
import com.example.fintrack.domain.CardTransaction;
import com.example.fintrack.domain.SplitMethod;
import com.example.fintrack.domain.SplitPattern;
import com.example.fintrack.domain.SplitPatternGroup;
import com.example.fintrack.domain.SplitTransaction;
import com.example.fintrack.domain.TransactionType;
import com.example.fintrack.repository.AssetCategoryRepository;
import com.example.fintrack.repository.AssetRepository;
import com.example.fintrack.repository.BankTransactionRepository;
import com.example.fintrack.repository.CardTransactionRepository;
import com.example.fintrack.repository.SplitPatternGroupRepository;
import com.example.fintrack.repository.SplitPatternRepository;
import com.example.fintrack.repository.SplitTransactionRepository;

/**
 * 収支の分割パターン管理と、取引への分配処理をまとめて扱うサービス。
 *
 * @author kanta ito
 */
@Service
public class SplitService {

    private static final Logger log = LoggerFactory.getLogger(SplitService.class);

    private final SplitPatternGroupRepository splitPatternGroupRepository;
    private final SplitPatternRepository splitPatternRepository;
    private final SplitTransactionRepository splitTransactionRepository;
    private final AssetCategoryRepository assetCategoryRepository;
    private final AssetRepository assetRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final CardTransactionRepository cardTransactionRepository;

    public SplitService(SplitPatternGroupRepository splitPatternGroupRepository,
        SplitPatternRepository splitPatternRepository, SplitTransactionRepository splitTransactionRepository,
        AssetCategoryRepository assetCategoryRepository, AssetRepository assetRepository,
        BankTransactionRepository bankTransactionRepository, CardTransactionRepository cardTransactionRepository) {
      this.splitPatternGroupRepository = splitPatternGroupRepository;
      this.splitPatternRepository = splitPatternRepository;
      this.splitTransactionRepository = splitTransactionRepository;
      this.assetCategoryRepository = assetCategoryRepository;
      this.assetRepository = assetRepository;
      this.bankTransactionRepository = bankTransactionRepository;
      this.cardTransactionRepository = cardTransactionRepository;
    }

    /**
     * ユーザーの分割パターン一覧を取得する。
     *
     * @param userId          ユーザーID
     * @param transactionType 収入か支出か
     * @return 分割パターングループの一覧
     */
    public List<SplitPatternGroup> getSplitPatternGroupList(Long userId, TransactionType transactionType) {
        return splitPatternGroupRepository.findByUserIdAndTransactionType(userId, transactionType);
    }

    /**
     * 分割パターンIDと名前の対応マップを作る（削除済みパターンも含む）。
     *
     * @param userId          ユーザーID
     * @param transactionType 収入か支出か
     * @return パターンIDをキー、パターン名を値とするマップ
     */
    public Map<Long, String> getPatternNameMap(Long userId, TransactionType transactionType) {
        List<SplitPatternGroup> splitPatternList = splitPatternGroupRepository.findByUserIdAndTransactionTypeInDeleted(userId, transactionType);
        Map<Long, String> patternNameMap = new HashMap<>();
        for (SplitPatternGroup splitPattern : splitPatternList) {
            patternNameMap.put(splitPattern.getId(), splitPattern.getPatternName());
        }

        return patternNameMap;
    }

    /**
     * 分割パターンと明細行・資産カテゴリをまとめて取得する。
     *
     * @param userId          ユーザーID
     * @param transactionType 収入か支出か
     * @return 明細付きの分割パターングループ一覧
     */
    public List<SplitPatternGroup> getPatternGroupWithSplitPatternList(Long userId, TransactionType transactionType) {
        return splitPatternGroupRepository.findWithSplitPatternAndAssetCategoryByUserIdAndTransactionType(userId, transactionType);
    }
    
    /**
     * 分割パターンを論理削除する。
     *
     * @param patternId 削除対象のパターンID
     * @param userId    ユーザーID
     * @throws IllegalArgumentException パターンが見つからない場合
     */
    @Transactional
    public void deletePattern(Long patternId, Long userId) {
        int updated = splitPatternGroupRepository.deleteById(patternId, userId);
        if (updated == 0) {
            throw new IllegalArgumentException("分割パターンが見つかりません");
        }
        log.info("分割パターン削除 patternId={}", patternId);
    }
    
    /**
     * 割合指定で新しい分割パターンを作成する。
     *
     * @param userId          ユーザーID
     * @param transactionType 収入か支出か
     * @param patternName     パターン名
     * @param categoryRatios  資産カテゴリIDと割合（%）のマップ
     * @throws IllegalArgumentException 割合の合計が100%でない、またはカテゴリが無効な場合
     */
    @Transactional
    public void createSplitPatternByRatio(Long userId, TransactionType transactionType, String patternName, 
                                         Map<Long, BigDecimal> categoryRatios) {
        
        // 合計が100%になるか確認
        BigDecimal total = categoryRatios.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (total.compareTo(BigDecimal.valueOf(100)) != 0) {
            throw new IllegalArgumentException("分割割合の合計は100%である必要があります");
        }

        SplitPatternGroup splitPatternGroup = new SplitPatternGroup();
        splitPatternGroup.setPatternName(patternName);
        splitPatternGroup.setUserId(userId);
        splitPatternGroup.setTransactionType(transactionType);
        splitPatternGroup.setSplitType("RATIO");

        Long patternId = splitPatternGroupRepository.insert(splitPatternGroup);

        List<SplitPattern> splitPatternList = new ArrayList<>();
        
        // パターンを保存
        for (Map.Entry<Long, BigDecimal> entry : categoryRatios.entrySet()) {
            Long categoryId = entry.getKey();
            
            requireAssetCategory(categoryId, userId);
            
            SplitPattern pattern = new SplitPattern();
            pattern.setPatternId(patternId);
            pattern.setUserId(userId);
            pattern.setAssetCategoryId(categoryId);
            pattern.setSplitRatio(entry.getValue());
            pattern.setSplitAmount(null);
            
            splitPatternList.add(pattern);
        }

        splitPatternRepository.insert(splitPatternList);
        log.info("分割パターン作成(RATIO) patternId={} name={} カテゴリ数={}", patternId, patternName, splitPatternList.size());
    }
    
    /**
     * 金額指定で新しい分割パターンを作成する。
     *
     * @param userId           ユーザーID
     * @param transactionType  収入か支出か
     * @param patternName      パターン名
     * @param categoryAmounts  資産カテゴリIDと金額のマップ
     * @throws IllegalArgumentException 有効な金額が1件もない、またはカテゴリが無効な場合
     */
    @Transactional
    public void createSplitPatternByAmount(Long userId, TransactionType transactionType, String patternName, 
                                          Map<Long, Long> categoryAmounts) {

        SplitPatternGroup splitPatternGroup = new SplitPatternGroup();
        splitPatternGroup.setPatternName(patternName);
        splitPatternGroup.setUserId(userId);
        splitPatternGroup.setTransactionType(transactionType);
        splitPatternGroup.setSplitType("AMOUNT");

        Long patternId = splitPatternGroupRepository.insert(splitPatternGroup);

        List<SplitPattern> splitPatternList = new ArrayList<>();

        // パターンを保存
        for (Map.Entry<Long, Long> entry : categoryAmounts.entrySet()) {
            Long categoryId = entry.getKey();
            Long amount = entry.getValue();
            
            if (amount <= 0) {
                continue; // 金額が0以下の場合はスキップ
            }
            
            requireAssetCategory(categoryId, userId);
            
            SplitPattern pattern = new SplitPattern();
            pattern.setPatternId(patternId);
            pattern.setUserId(userId);
            pattern.setAssetCategoryId(categoryId);
            pattern.setSplitRatio(null);
            pattern.setSplitAmount(amount);
            
            splitPatternList.add(pattern);
        }

        if (splitPatternList.isEmpty()){
            throw new IllegalArgumentException("有効な分割金額が1件以上必要です");
        }
        splitPatternRepository.insert(splitPatternList);
        log.info("分割パターン作成(AMOUNT) patternId={} name={} カテゴリ数={}", patternId, patternName, splitPatternList.size());
    }
    
    /**
     * 収入取引を分割パターンで分配する。
     *
     * @param bankTransactionId 銀行取引ID（カード取引の場合はnull）
     * @param cardTransactionId カード取引ID（銀行取引の場合はnull）
     * @param patternId         適用する分割パターンID
     * @param userId            ユーザーID
     * @throws IllegalArgumentException 取引・パターンが見つからない、または金額が合わない場合
     */
    @Transactional
    public void splitIncome(Long bankTransactionId, Long cardTransactionId, Long patternId, Long userId) {
        splitTransaction(bankTransactionId, cardTransactionId, userId, patternId, TransactionType.INCOME);
    }

    /**
     * 複数の収入取引を同じ分割パターンで一括分配する。
     *
     * @param bankTransactionIdList 銀行取引IDの一覧
     * @param cardTransactionIdList カード取引IDの一覧
     * @param patternId             適用する分割パターンID
     * @param userId                ユーザーID
     * @throws IllegalArgumentException 取引・パターンが見つからない、または金額が合わない場合
     */
    @Transactional
    public void splitIncomeAll(List<Long> bankTransactionIdList, List<Long> cardTransactionIdList,
                               Long patternId, Long userId) {
        for (Long bankTransactionId : bankTransactionIdList) {
            splitTransaction(bankTransactionId, null, userId, patternId, TransactionType.INCOME);
        }
        for (Long cardTransactionId : cardTransactionIdList) {
            splitTransaction(null, cardTransactionId, userId, patternId, TransactionType.INCOME);
        }
    }
    
    /**
     * 支出取引を分割パターンで分配する。
     *
     * @param bankTransactionId 銀行取引ID（カード取引の場合はnull）
     * @param cardTransactionId カード取引ID（銀行取引の場合はnull）
     * @param patternId         適用する分割パターンID
     * @param userId            ユーザーID
     * @throws IllegalArgumentException 取引・パターンが見つからない、または金額が合わない場合
     */
    @Transactional
    public void splitExpense(Long bankTransactionId, Long cardTransactionId, Long patternId, Long userId) {
        splitTransaction(bankTransactionId, cardTransactionId, userId, patternId, TransactionType.EXPENSE);
    }

    /**
     * 複数の支出取引を同じ分割パターンで一括分配する。
     *
     * @param bankTransactionIdList 銀行取引IDの一覧
     * @param cardTransactionIdList カード取引IDの一覧
     * @param patternId             適用する分割パターンID
     * @param userId                ユーザーID
     * @throws IllegalArgumentException 取引・パターンが見つからない、または金額が合わない場合
     */
    @Transactional
    public void splitExpenseAll(List<Long> bankTransactionIdList, List<Long> cardTransactionIdList,
                                Long patternId, Long userId) {
        for (Long bankTransactionId : bankTransactionIdList) {
            splitTransaction(bankTransactionId, null, userId, patternId, TransactionType.EXPENSE);
        }
        for (Long cardTransactionId : cardTransactionIdList) {
            splitTransaction(null, cardTransactionId, userId, patternId, TransactionType.EXPENSE);
        }
    }
    
    /**
     * 収支をパターンで分割（共通処理）
     */
    private void splitTransaction(Long bankTransactionId, Long cardTransactionId, Long userId, Long patternId, TransactionType transactionType) {
        // 取引を取得
        Long totalAmount;
        BankTransaction bankTransaction = null;
        CardTransaction cardTransaction = null;
        
        if (bankTransactionId != null) {
            bankTransaction = bankTransactionRepository.findById(bankTransactionId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("銀行取引が見つかりません"));
            totalAmount = bankTransaction.getAmount();
            
            // 取引に分割パターンIDを設定、asset_category_idはNULLに
            bankTransaction.setSplitPatternId(patternId);
            bankTransaction.setSplitMethod(SplitMethod.PATTERN);
            bankTransaction.setAssetCategoryId(null);
            bankTransactionRepository.updateAssetCategoryOrSplitPatternOrExpenseCategory(bankTransaction);
        } else if (cardTransactionId != null) {
            cardTransaction = cardTransactionRepository.findById(cardTransactionId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("カード取引が見つかりません"));
            totalAmount = cardTransaction.getAmount();
            
            // 取引に分割パターンIDを設定、asset_category_idはNULLに
            cardTransaction.setSplitPatternId(patternId);
            cardTransaction.setSplitMethod(SplitMethod.PATTERN);
            cardTransaction.setAssetCategoryId(null);
            cardTransactionRepository.updateAssetCategoryOrSplitPatternOrExpenseCategory(cardTransaction);
        } else {
            throw new IllegalArgumentException("取引IDが指定されていません");
        }
        
        // 分割パターンを取得
        SplitPatternGroup splitPatternGroup = splitPatternGroupRepository.findWithSplitPatternAndAssetCategoryByUserIdAndId(userId, patternId);
        
        // 分割金額を計算
        List<SplitTransaction> splits = new ArrayList<>();
        long totalSplitAmount = 0;

        List<SplitPattern> splitPatternList = splitPatternGroup.getSplitPatternList();
        
        if ("RATIO".equals(splitPatternGroup.getSplitType())) {
            // 割合指定の場合
            for (int i = 0; i < splitPatternList.size(); i++) {
                SplitPattern pattern = splitPatternList.get(i);
                long splitAmount;
                
                if (i == splitPatternList.size() - 1) {
                    // 最後のカテゴリには差額を含める（端数調整）
                    splitAmount = totalAmount - totalSplitAmount;
                } else {
                    splitAmount = totalAmount * pattern.getSplitRatio().longValue() / 100;
                }
                
                totalSplitAmount += splitAmount;
                
                SplitTransaction split = createSplitTransaction(pattern, splitAmount, bankTransactionId, 
                                                               cardTransactionId, patternId, transactionType, userId);
                splits.add(split);
            }
        } else {
            // 金額指定の場合
            for (SplitPattern pattern : splitPatternList) {
                long splitAmount = pattern.getSplitAmount();
                
                if (splitAmount > totalAmount - totalSplitAmount) {
                    throw new IllegalArgumentException("分割パターンの金額が取引金額を超えています");
                }
                
                totalSplitAmount += splitAmount;
                
                SplitTransaction split = createSplitTransaction(pattern, splitAmount, bankTransactionId, 
                                                               cardTransactionId, patternId, transactionType, userId);
                splits.add(split);
            }
            
            // 金額指定の場合、合計が取引金額と一致するか確認
            if (totalSplitAmount != totalAmount) {
                throw new IllegalArgumentException("分割パターンの金額合計が取引金額と一致しません");
            }
        }
        
        // 保存
        splitTransactionRepository.insert(splits);
        
        // 残高を更新
        updateBalances(userId, splits, transactionType, totalAmount);

        log.info("パターン分割 type={} bankTxId={} cardTxId={} patternId={} 明細数={}",
                transactionType, bankTransactionId, cardTransactionId, patternId, splits.size());
    }
    
    private SplitTransaction createSplitTransaction(SplitPattern pattern, long splitAmount, 
                                                   Long bankTransactionId, Long cardTransactionId, 
                                                   Long patternId, TransactionType transactionType, Long userId) {
        SplitTransaction split = new SplitTransaction();
        split.setTransactionType(transactionType);
        split.setAssetCategoryId(pattern.getAssetCategoryId());
        split.setSplitPatternId(patternId);
        split.setSplitMethod(SplitMethod.PATTERN);
        split.setAmount(splitAmount);
        split.setBankTransactionId(bankTransactionId);
        split.setCardTransactionId(cardTransactionId);
        split.setUserId(userId);
        
        return split;
    }
    
    /**
     * 収入取引を手動で金額指定して分配する。
     *
     * @param bankTransactionId 銀行取引ID（カード取引の場合はnull）
     * @param cardTransactionId カード取引ID（銀行取引の場合はnull）
     * @param categoryAmounts   資産カテゴリIDと金額のマップ
     * @param userId            ユーザーID
     * @throws IllegalArgumentException 取引が見つからない、または分割金額の合計が一致しない場合
     */
    @Transactional
    public void splitIncomeManual(Long bankTransactionId, Long cardTransactionId, Map<Long, Long> categoryAmounts, Long userId) {
        splitManual(bankTransactionId, cardTransactionId, userId, categoryAmounts, TransactionType.INCOME);
    }
    
    /**
     * 支出取引を手動で金額指定して分配する。
     *
     * @param bankTransactionId 銀行取引ID（カード取引の場合はnull）
     * @param cardTransactionId カード取引ID（銀行取引の場合はnull）
     * @param categoryAmounts   資産カテゴリIDと金額のマップ
     * @param userId            ユーザーID
     * @throws IllegalArgumentException 取引が見つからない、または分割金額の合計が一致しない場合
     */
    @Transactional
    public void splitExpenseManual(Long bankTransactionId, Long cardTransactionId, Map<Long, Long> categoryAmounts, Long userId) {
        splitManual(bankTransactionId, cardTransactionId, userId, categoryAmounts, TransactionType.EXPENSE);
    }
    
    /**
     * 収入取引を単一の資産カテゴリに全額分配する。
     *
     * @param bankTransactionId 銀行取引ID（カード取引の場合はnull）
     * @param cardTransactionId カード取引ID（銀行取引の場合はnull）
     * @param categoryId        分配先の資産カテゴリID
     * @param userId            ユーザーID
     * @throws IllegalArgumentException 取引またはカテゴリが見つからない場合
     */
    @Transactional
    public void splitIncomeSingle(Long bankTransactionId, Long cardTransactionId, Long categoryId, Long userId) {
        splitSingle(bankTransactionId, cardTransactionId, userId, categoryId, TransactionType.INCOME);
    }

    /**
     * 複数の収入取引を同じ資産カテゴリに一括で全額分配する。
     *
     * @param bankTransactionIdList 銀行取引IDの一覧
     * @param cardTransactionIdList カード取引IDの一覧
     * @param categoryId            分配先の資産カテゴリID
     * @param userId                ユーザーID
     * @throws IllegalArgumentException 取引またはカテゴリが見つからない場合
     */
    @Transactional
    public void splitIncomeSingleAll(List<Long> bankTransactionIdList, List<Long> cardTransactionIdList, Long categoryId, Long userId) {
        for ( Long bankTransactionId : bankTransactionIdList){
            splitSingle(bankTransactionId, null, userId, categoryId, TransactionType.INCOME);
        }

        for ( Long cardTransactionId :cardTransactionIdList){
            splitSingle(null, cardTransactionId, userId, categoryId, TransactionType.INCOME);
        }
    }
    
    /**
     * 支出取引を単一の資産カテゴリに全額分配する。
     *
     * @param bankTransactionId 銀行取引ID（カード取引の場合はnull）
     * @param cardTransactionId カード取引ID（銀行取引の場合はnull）
     * @param categoryId        分配先の資産カテゴリID
     * @param userId            ユーザーID
     * @throws IllegalArgumentException 取引またはカテゴリが見つからない場合
     */
    @Transactional
    public void splitExpenseSingle(Long bankTransactionId, Long cardTransactionId, Long categoryId, Long userId) {
        splitSingle(bankTransactionId, cardTransactionId, userId, categoryId, TransactionType.EXPENSE);
    }

    /**
     * 複数の支出取引を同じ資産カテゴリに一括で全額分配する。
     *
     * @param bankTransactionIdList 銀行取引IDの一覧
     * @param cardTransactionIdList カード取引IDの一覧
     * @param categoryId            分配先の資産カテゴリID
     * @param userId                ユーザーID
     * @throws IllegalArgumentException 取引またはカテゴリが見つからない場合
     */
    @Transactional
    public void splitExpenseSingleAll(List<Long> bankTransactionIdList, List<Long> cardTransactionIdList,
                                      Long categoryId, Long userId) {
        for (Long bankTransactionId : bankTransactionIdList) {
            splitSingle(bankTransactionId, null, userId, categoryId, TransactionType.EXPENSE);
        }
        for (Long cardTransactionId : cardTransactionIdList) {
            splitSingle(null, cardTransactionId, userId, categoryId, TransactionType.EXPENSE);
        }
    }
    
    /**
     * 収支を単一カテゴリに分配（共通処理）
     */
    private void splitSingle(Long bankTransactionId, Long cardTransactionId, Long userId, Long categoryId, TransactionType transactionType) {
        requireAssetCategory(categoryId, userId);

        // 取引を取得
        Long totalAmount;
        BankTransaction bankTransaction = null;
        CardTransaction cardTransaction = null;
        
        if (bankTransactionId != null) {
            bankTransaction = bankTransactionRepository.findById(bankTransactionId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("銀行取引が見つかりません"));
            totalAmount = bankTransaction.getAmount();
            
            // 取引に単一カテゴリを設定
            bankTransaction.setSplitPatternId(null);
            bankTransaction.setSplitMethod(SplitMethod.SINGLE);
            bankTransaction.setAssetCategoryId(categoryId);
            bankTransactionRepository.updateAssetCategoryOrSplitPatternOrExpenseCategory(bankTransaction);
        } else if (cardTransactionId != null) {
            cardTransaction = cardTransactionRepository.findById(cardTransactionId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("カード取引が見つかりません"));
            totalAmount = cardTransaction.getAmount();
            
            // 取引に単一カテゴリを設定
            cardTransaction.setSplitPatternId(null);
            cardTransaction.setSplitMethod(SplitMethod.SINGLE);
            cardTransaction.setAssetCategoryId(categoryId);
            cardTransactionRepository.updateAssetCategoryOrSplitPatternOrExpenseCategory(cardTransaction);
        } else {
            throw new IllegalArgumentException("取引IDが指定されていません");
        }

        // 単一カテゴリの場合、split_transactionsには登録しない
        // asset_category_idに直接設定するのみ

        // 残高を更新
        if (transactionType == TransactionType.INCOME) {
            // 収入の場合: 未分類から全額を引き、指定カテゴリに加算
            assetRepository.updateAmountByCategoryId(userId, null, -totalAmount);
            assetRepository.updateAmountByCategoryId(userId, categoryId, totalAmount);
        } else {
            // 支出の場合: 未分類から全額を戻し、指定カテゴリから減算
            assetRepository.updateAmountByCategoryId(userId, null, totalAmount);
            assetRepository.updateAmountByCategoryId(userId, categoryId, -totalAmount);
        }

        log.info("単一カテゴリ分配 type={} bankTxId={} cardTxId={} categoryId={}",
                transactionType, bankTransactionId, cardTransactionId, categoryId);
    }
    
    /**
     * 収支を手動で金額指定して分割（共通処理）
     */
    private void splitManual(Long bankTransactionId, Long cardTransactionId, Long userId, Map<Long, Long> categoryAmounts, TransactionType transactionType) {
        for (Long categoryId : categoryAmounts.keySet()) {
            requireAssetCategory(categoryId, userId);
        }

        // 取引を取得
        Long totalAmount;
        BankTransaction bankTransaction = null;
        CardTransaction cardTransaction = null;
        
        if (bankTransactionId != null) {
            bankTransaction = bankTransactionRepository.findById(bankTransactionId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("銀行取引が見つかりません"));
            totalAmount = bankTransaction.getAmount();
            
            // 取引に手動分割を設定
            bankTransaction.setSplitPatternId(null);
            bankTransaction.setSplitMethod(SplitMethod.MANUAL);
            bankTransaction.setAssetCategoryId(null);
            bankTransactionRepository.updateAssetCategoryOrSplitPatternOrExpenseCategory(bankTransaction);
        } else if (cardTransactionId != null) {
            cardTransaction = cardTransactionRepository.findById(cardTransactionId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("カード取引が見つかりません"));
            totalAmount = cardTransaction.getAmount();
            
            // 取引に手動分割を設定
            cardTransaction.setSplitPatternId(null);
            cardTransaction.setSplitMethod(SplitMethod.MANUAL);
            cardTransaction.setAssetCategoryId(null);
            cardTransactionRepository.updateAssetCategoryOrSplitPatternOrExpenseCategory(cardTransaction);
        } else {
            throw new IllegalArgumentException("取引IDが指定されていません");
        }
        
        // 合計金額のチェック
        long totalSplit = categoryAmounts.values().stream()
                .mapToLong(Long::longValue)
                .sum();
        
        if (totalSplit != totalAmount) {
            throw new IllegalArgumentException("分割金額の合計が取引金額と一致しません");
        }
        
        // 各カテゴリに分割を作成
        List<SplitTransaction> splits = new ArrayList<>();
        for (Map.Entry<Long, Long> entry : categoryAmounts.entrySet()) {
            Long categoryId = entry.getKey();
            Long amount = entry.getValue();
            
            SplitTransaction split = new SplitTransaction();
            split.setTransactionType(transactionType);
            split.setAssetCategoryId(categoryId);
            split.setSplitPatternId(null); // 手動分割の場合はNULL
            split.setSplitMethod(SplitMethod.MANUAL);
            split.setAmount(amount);
            split.setBankTransactionId(bankTransactionId);
            split.setCardTransactionId(cardTransactionId);
            split.setUserId(userId);
            
            splits.add(split);
        }
        
        // 保存
        splitTransactionRepository.insert(splits);
        
        // 残高を更新
        updateBalances(userId, splits, transactionType, totalAmount);

        log.info("手動分割 type={} bankTxId={} cardTxId={} 明細数={}",
                transactionType, bankTransactionId, cardTransactionId, splits.size());
    }
    
    /**
     * 指定取引に紐づく分割明細を取得する。
     *
     * @param bankTransactionId 銀行取引ID（カード取引の場合はnull）
     * @param cardTransactionId カード取引ID（銀行取引の場合はnull）
     * @param userId            ユーザーID
     * @return 分割明細の一覧（取引ID未指定の場合は空リスト）
     */
    public List<SplitTransaction> getSplitTransactionsByTransaction(Long bankTransactionId, Long cardTransactionId, Long userId) {
        if (bankTransactionId != null) {
            return splitTransactionRepository.findByBankTransactionId(bankTransactionId, userId);
        } else if (cardTransactionId != null) {
            return splitTransactionRepository.findByCardTransactionId(cardTransactionId, userId);
        }
        return List.of();
    }
    
    private void requireAssetCategory(Long categoryId, Long userId) {
        assetCategoryRepository.findById(categoryId, userId)
                .orElseThrow(() -> new IllegalArgumentException("資産カテゴリが見つかりません"));
    }

    /**
     * 残高を更新（収入・支出共通）
     */
    private void updateBalances(Long userId, List<SplitTransaction> splits, TransactionType transactionType, Long totalAmount) {
        if (transactionType == TransactionType.INCOME) {
            // 収入の場合: 未分類から全額を引き、各カテゴリに加算
            assetRepository.updateAmountByCategoryId(userId, null, -totalAmount);
            
            for (SplitTransaction split : splits) {
                assetRepository.updateAmountByCategoryId(userId, split.getAssetCategoryId(), split.getAmount());
            }
        } else {
            // 支出の場合: 未分類から全額を戻し、各カテゴリから減算
            assetRepository.updateAmountByCategoryId(userId, null, totalAmount);
            
            for (SplitTransaction split : splits) {
                assetRepository.updateAmountByCategoryId(userId, split.getAssetCategoryId(), -split.getAmount());
            }
        }
    }
}

