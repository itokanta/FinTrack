package com.example.fintrack.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 * 収支分割管理サービス（収入・支出統合版）
 */
@Service
public class SplitService {
    
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
     * ユーザーの分割パターンを取得
     */
    public List<SplitPatternGroup> getSplitPatternGroupList(Long userId, TransactionType transactionType) {
        return splitPatternGroupRepository.findByUserIdAndTransactionType(userId, transactionType);
    }

    public Map<Long, String> getPatternNameMap(Long userId, TransactionType transactionType) {
        List<SplitPatternGroup> splitPatternList = splitPatternGroupRepository.findByUserIdAndTransactionTypeInDeleted(userId, transactionType);
        Map<Long, String> patternNameMap = new HashMap<>();
        for (SplitPatternGroup splitPattern : splitPatternList) {
            patternNameMap.put(splitPattern.getId(), splitPattern.getPatternName());
        }

        return patternNameMap;
    }

    public List<SplitPatternGroup> getPatternGroupWithSplitPatternList(Long userId, TransactionType transactionType) {
        return splitPatternGroupRepository.findWithSplitPatternAndAssetCategoryByUserIdAndTransactionType(userId, transactionType);
    }
    
    /**
     * 分割パターンを削除
     */
    @Transactional
    public void deletePattern(Long patternId, Long userId) {
        int updated = splitPatternGroupRepository.deleteById(patternId, userId);
        if (updated == 0) {
            throw new IllegalArgumentException("分割パターンが見つかりません");
        }
    }
    
    /**
     * 新しい分割パターンを作成（割合指定）
     */
    @Transactional
    public Long createSplitPatternByRatio(Long userId, TransactionType transactionType, String patternName, 
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
        
        return patternId;
    }
    
    /**
     * 新しい分割パターンを作成（金額指定）
     */
    @Transactional
    public Long createSplitPatternByAmount(Long userId, TransactionType transactionType, String patternName, 
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

        return patternId;
    }
    
    /**
     * 収入をパターンで分割
     */
    @Transactional
    public void splitIncome(Long bankTransactionId, Long cardTransactionId, Long patternId, Long userId) {
        splitTransaction(bankTransactionId, cardTransactionId, userId, patternId, TransactionType.INCOME);
    }
    
    /**
     * 支出をパターンで分割
     */
    @Transactional
    public void splitExpense(Long bankTransactionId, Long cardTransactionId, Long patternId, Long userId) {
        splitTransaction(bankTransactionId, cardTransactionId, userId, patternId, TransactionType.EXPENSE);
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
                                                               cardTransactionId, patternId, transactionType);
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
                                                               cardTransactionId, patternId, transactionType);
                splits.add(split);
            }
            
            // 金額指定の場合、合計が取引金額と一致するか確認
            if (totalSplitAmount != totalAmount) {
                throw new IllegalArgumentException("分割パターンの金額合計が取引金額と一致しません");
            }
        }
        
        // 保存
        splitTransactionRepository.saveAll(splits);
        
        // 残高を更新
        updateBalances(userId, splits, transactionType, totalAmount);
    }
    
    private SplitTransaction createSplitTransaction(SplitPattern pattern, long splitAmount, 
                                                   Long bankTransactionId, Long cardTransactionId, 
                                                   Long patternId, TransactionType transactionType) {
        SplitTransaction split = new SplitTransaction();
        split.setTransactionType(transactionType);
        split.setAssetCategoryId(pattern.getAssetCategoryId());
        split.setSplitPatternId(patternId);
        split.setSplitMethod(SplitMethod.PATTERN);
        split.setAmount(splitAmount);
        split.setBankTransactionId(bankTransactionId);
        split.setCardTransactionId(cardTransactionId);
        
        return split;
    }
    
    /**
     * 収入を手動で金額指定して分割
     */
    @Transactional
    public void splitIncomeManual(Long bankTransactionId, Long cardTransactionId, Map<Long, Long> categoryAmounts, Long userId) {
        splitManual(bankTransactionId, cardTransactionId, userId, categoryAmounts, TransactionType.INCOME);
    }
    
    /**
     * 支出を手動で金額指定して分割
     */
    @Transactional
    public void splitExpenseManual(Long bankTransactionId, Long cardTransactionId, Map<Long, Long> categoryAmounts, Long userId) {
        splitManual(bankTransactionId, cardTransactionId, userId, categoryAmounts, TransactionType.EXPENSE);
    }
    
    /**
     * 収入を単一カテゴリに分配
     */
    @Transactional
    public void splitIncomeSingle(Long bankTransactionId, Long cardTransactionId, Long categoryId, Long userId) {
        splitSingle(bankTransactionId, cardTransactionId, userId, categoryId, TransactionType.INCOME);
    }
    
    /**
     * 支出を単一カテゴリに分配
     */
    @Transactional
    public void splitExpenseSingle(Long bankTransactionId, Long cardTransactionId, Long categoryId, Long userId) {
        splitSingle(bankTransactionId, cardTransactionId, userId, categoryId, TransactionType.EXPENSE);
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
            
            splits.add(split);
        }
        
        // 保存
        splitTransactionRepository.saveAll(splits);
        
        // 残高を更新
        updateBalances(userId, splits, transactionType, totalAmount);
    }
    
    /**
     * 取引の分割状況を取得
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

