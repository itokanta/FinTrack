package com.example.fintrack.service;

import com.example.fintrack.domain.*;
import com.example.fintrack.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 収支分割管理サービス（収入・支出統合版）
 */
@Service
public class SplitService {
    
    private final SplitPatternRepository splitPatternRepository;
    private final SplitTransactionRepository splitTransactionRepository;
    private final AssetCategoryRepository assetCategoryRepository;
    private final ExpenseCategoryRepository expenseCategoryRepository;
    private final AssetRepository assetRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final CardTransactionRepository cardTransactionRepository;
    
    public SplitService(SplitPatternRepository splitPatternRepository,
                       SplitTransactionRepository splitTransactionRepository,
                       AssetCategoryRepository assetCategoryRepository,
                       ExpenseCategoryRepository expenseCategoryRepository,
                       AssetRepository assetRepository,
                       BankTransactionRepository bankTransactionRepository,
                       CardTransactionRepository cardTransactionRepository) {
        this.splitPatternRepository = splitPatternRepository;
        this.splitTransactionRepository = splitTransactionRepository;
        this.assetCategoryRepository = assetCategoryRepository;
        this.expenseCategoryRepository = expenseCategoryRepository;
        this.assetRepository = assetRepository;
        this.bankTransactionRepository = bankTransactionRepository;
        this.cardTransactionRepository = cardTransactionRepository;
    }
    
    /**
     * ユーザーの分割パターンIDリストを取得
     */
    public List<Long> getSplitPatternIds(Long userId, String transactionType) {
        return splitPatternRepository.findDistinctPatternIdsByUserIdAndTransactionType(userId, transactionType);
    }
    
    /**
     * 特定の分割パターンを取得
     */
    public List<SplitPattern> getSplitPatternById(Long patternId) {
        return splitPatternRepository.findById(patternId);
    }
    
    /**
     * ユーザーの分割パターンサマリーを取得
     */
    public List<PatternSummary> getPatternSummaries(Long userId, String transactionType) {
        List<Long> patternIds = splitPatternRepository.findDistinctPatternIdsByUserIdAndTransactionType(userId, transactionType);
        List<PatternSummary> summaries = new ArrayList<>();

        for (Long patternId : patternIds) {
            List<SplitPattern> patterns = splitPatternRepository.findById(patternId);
            if (!patterns.isEmpty()) {
                SplitPattern firstPattern = patterns.get(0);
                String patternName = firstPattern.getPatternName();
                String splitType = firstPattern.getSplitType();
                
                List<PatternDetail> details = patterns.stream()
                        .map(p -> {
                            String categoryName = assetCategoryRepository.findById(p.getAssetCategoryId())
                                    .map(AssetCategory::getCategoryName)
                                    .orElse("不明なカテゴリ");
                            return new PatternDetail(categoryName, p.getSplitRatio(), p.getSplitAmount(), splitType);
                        })
                        .collect(Collectors.toList());
                summaries.add(new PatternSummary(patternId, patternName, splitType, details));
            }
        }
        return summaries;
    }
    
    /**
     * 分割パターンを削除
     */
    @Transactional
    public void deletePattern(Long patternId) {
        splitPatternRepository.deleteByPatternId(patternId);
    }
    
    /**
     * 新しい分割パターンを作成（割合指定）
     */
    @Transactional
    public Long createSplitPatternByRatio(Long userId, String transactionType, String patternName, 
                                         Map<Long, BigDecimal> categoryRatios) {
        // 新しいパターンIDを生成
        Long maxPatternId = splitPatternRepository.findMaxPatternId();
        if (maxPatternId == null) {
            maxPatternId = 0L;
        }
        Long newPatternId = maxPatternId + 1;
        
        // 合計が100%になるか確認
        BigDecimal total = categoryRatios.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (total.compareTo(BigDecimal.valueOf(100)) != 0) {
            throw new IllegalArgumentException("分割割合の合計は100%である必要があります");
        }
        
        // パターンを保存
        for (Map.Entry<Long, BigDecimal> entry : categoryRatios.entrySet()) {
            Long categoryId = entry.getKey();
            
            // カテゴリの存在確認とユーザー所有確認
            if ("INCOME".equals(transactionType)) {
                AssetCategory category = assetCategoryRepository.findById(categoryId)
                        .orElseThrow(() -> new IllegalArgumentException("資産カテゴリが見つかりません"));
                
                if (!category.getUserId().equals(userId)) {
                    throw new IllegalArgumentException("他のユーザーのカテゴリは使用できません");
                }
            } else {
                ExpenseCategory category = expenseCategoryRepository.findById(categoryId)
                        .orElseThrow(() -> new IllegalArgumentException("支出カテゴリが見つかりません"));
                
                if (!category.getUserId().equals(userId)) {
                    throw new IllegalArgumentException("他のユーザーのカテゴリは使用できません");
                }
            }
            
            SplitPattern pattern = new SplitPattern();
            pattern.setPatternId(newPatternId);
            pattern.setUserId(userId);
            pattern.setTransactionType(transactionType);
            pattern.setSplitType("RATIO");
            pattern.setAssetCategoryId(categoryId);
            pattern.setSplitRatio(entry.getValue());
            pattern.setSplitAmount(null);
            pattern.setPatternName(patternName);
            
            splitPatternRepository.save(pattern);
        }
        
        return newPatternId;
    }
    
    /**
     * 新しい分割パターンを作成（金額指定）
     */
    @Transactional
    public Long createSplitPatternByAmount(Long userId, String transactionType, String patternName, 
                                          Map<Long, Long> categoryAmounts) {
        // 新しいパターンIDを生成
        Long maxPatternId = splitPatternRepository.findMaxPatternId();
        if (maxPatternId == null) {
            maxPatternId = 0L;
        }
        Long newPatternId = maxPatternId + 1;
        
        // パターンを保存
        for (Map.Entry<Long, Long> entry : categoryAmounts.entrySet()) {
            Long categoryId = entry.getKey();
            Long amount = entry.getValue();
            
            if (amount <= 0) {
                continue; // 金額が0以下の場合はスキップ
            }
            
            // カテゴリの存在確認とユーザー所有確認
            AssetCategory category = assetCategoryRepository.findById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("資産カテゴリが見つかりません"));
            
            if (!category.getUserId().equals(userId)) {
                throw new IllegalArgumentException("他のユーザーのカテゴリは使用できません");
            }
            
            SplitPattern pattern = new SplitPattern();
            pattern.setPatternId(newPatternId);
            pattern.setUserId(userId);
            pattern.setTransactionType(transactionType);
            pattern.setSplitType("AMOUNT");
            pattern.setAssetCategoryId(categoryId);
            pattern.setSplitRatio(null);
            pattern.setSplitAmount(amount);
            pattern.setPatternName(patternName);
            
            splitPatternRepository.save(pattern);
        }
        
        return newPatternId;
    }
    
    /**
     * 収入をパターンで分割
     */
    @Transactional
    public void splitIncome(Long bankTransactionId, Long cardTransactionId, Long patternId) {
        splitTransaction(bankTransactionId, cardTransactionId, patternId, "INCOME");
    }
    
    /**
     * 支出をパターンで分割
     */
    @Transactional
    public void splitExpense(Long bankTransactionId, Long cardTransactionId, Long patternId) {
        splitTransaction(bankTransactionId, cardTransactionId, patternId, "EXPENSE");
    }
    
    /**
     * 収支をパターンで分割（共通処理）
     */
    private void splitTransaction(Long bankTransactionId, Long cardTransactionId, Long patternId, String transactionType) {
        // 取引を取得
        Long totalAmount;
        Long userId;
        BankTransaction bankTransaction = null;
        CardTransaction cardTransaction = null;
        
        if (bankTransactionId != null) {
            bankTransaction = bankTransactionRepository.findById(bankTransactionId)
                    .orElseThrow(() -> new IllegalArgumentException("銀行取引が見つかりません"));
            totalAmount = bankTransaction.getAmount();
            userId = bankTransaction.getUserId();
            
            // 取引に分割パターンIDを設定、asset_category_idはNULLに
            bankTransaction.setSplitPatternId(patternId);
            bankTransaction.setAssetCategoryId(null);
            bankTransactionRepository.save(bankTransaction);
        } else if (cardTransactionId != null) {
            cardTransaction = cardTransactionRepository.findById(cardTransactionId)
                    .orElseThrow(() -> new IllegalArgumentException("カード取引が見つかりません"));
            totalAmount = cardTransaction.getAmount();
            userId = cardTransaction.getUserId();
            
            // 取引に分割パターンIDを設定、asset_category_idはNULLに
            cardTransaction.setSplitPatternId(patternId);
            cardTransaction.setAssetCategoryId(null);
            cardTransactionRepository.save(cardTransaction);
        } else {
            throw new IllegalArgumentException("取引IDが指定されていません");
        }
        
        // 分割パターンを取得
        List<SplitPattern> patterns = splitPatternRepository.findById(patternId);
        if (patterns.isEmpty()) {
            throw new IllegalArgumentException("分割パターンが見つかりません");
        }
        
        SplitPattern firstPattern = patterns.get(0);
        String splitType = firstPattern.getSplitType();
        
        // 分割金額を計算
        List<SplitTransaction> splits = new ArrayList<>();
        long totalSplit = 0;
        
        if ("RATIO".equals(splitType)) {
            // 割合指定の場合
            for (int i = 0; i < patterns.size(); i++) {
                SplitPattern pattern = patterns.get(i);
                long splitAmount;
                
                if (i == patterns.size() - 1) {
                    // 最後のカテゴリには差額を含める（端数調整）
                    splitAmount = totalAmount - totalSplit;
                } else {
                    splitAmount = totalAmount * pattern.getSplitRatio().longValue() / 100;
                }
                
                totalSplit += splitAmount;
                
                SplitTransaction split = createSplitTransaction(pattern, splitAmount, bankTransactionId, 
                                                               cardTransactionId, patternId, transactionType);
                splits.add(split);
            }
        } else {
            // 金額指定の場合
            for (SplitPattern pattern : patterns) {
                long splitAmount = pattern.getSplitAmount();
                
                if (splitAmount > totalAmount - totalSplit) {
                    throw new IllegalArgumentException("分割パターンの金額が取引金額を超えています");
                }
                
                totalSplit += splitAmount;
                
                SplitTransaction split = createSplitTransaction(pattern, splitAmount, bankTransactionId, 
                                                               cardTransactionId, patternId, transactionType);
                splits.add(split);
            }
            
            // 金額指定の場合、合計が取引金額と一致するか確認
            if (totalSplit != totalAmount) {
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
                                                   Long patternId, String transactionType) {
        SplitTransaction split = new SplitTransaction();
        split.setTransactionType(transactionType);
        split.setAssetCategoryId(pattern.getAssetCategoryId());
        split.setSplitPatternId(patternId);
        split.setSplitMethod("PATTERN");
        split.setAmount(splitAmount);
        split.setBankTransactionId(bankTransactionId);
        split.setCardTransactionId(cardTransactionId);
        
        return split;
    }
    
    /**
     * 収入を手動で金額指定して分割
     */
    @Transactional
    public void splitIncomeManual(Long bankTransactionId, Long cardTransactionId, Map<Long, Long> categoryAmounts) {
        splitManual(bankTransactionId, cardTransactionId, categoryAmounts, "INCOME");
    }
    
    /**
     * 支出を手動で金額指定して分割
     */
    @Transactional
    public void splitExpenseManual(Long bankTransactionId, Long cardTransactionId, Map<Long, Long> categoryAmounts) {
        splitManual(bankTransactionId, cardTransactionId, categoryAmounts, "EXPENSE");
    }
    
    /**
     * 収入を単一カテゴリに分配
     */
    @Transactional
    public void splitIncomeSingle(Long bankTransactionId, Long cardTransactionId, Long categoryId) {
        splitSingle(bankTransactionId, cardTransactionId, categoryId, "INCOME");
    }
    
    /**
     * 支出を単一カテゴリに分配
     */
    @Transactional
    public void splitExpenseSingle(Long bankTransactionId, Long cardTransactionId, Long categoryId) {
        splitSingle(bankTransactionId, cardTransactionId, categoryId, "EXPENSE");
    }
    
    /**
     * 収支を単一カテゴリに分配（共通処理）
     */
    private void splitSingle(Long bankTransactionId, Long cardTransactionId, Long categoryId, String transactionType) {
        // 取引を取得
        Long totalAmount;
        Long userId;
        BankTransaction bankTransaction = null;
        CardTransaction cardTransaction = null;
        
        if (bankTransactionId != null) {
            bankTransaction = bankTransactionRepository.findById(bankTransactionId)
                    .orElseThrow(() -> new IllegalArgumentException("銀行取引が見つかりません"));
            totalAmount = bankTransaction.getAmount();
            userId = bankTransaction.getUserId();
            
            // 取引に分割パターンID=0を設定（単一カテゴリ）
            bankTransaction.setSplitPatternId(0L);
            // asset_category_idを直接設定
            bankTransaction.setAssetCategoryId(categoryId);
            bankTransactionRepository.save(bankTransaction);
        } else if (cardTransactionId != null) {
            cardTransaction = cardTransactionRepository.findById(cardTransactionId)
                    .orElseThrow(() -> new IllegalArgumentException("カード取引が見つかりません"));
            totalAmount = cardTransaction.getAmount();
            userId = cardTransaction.getUserId();
            
            // 取引に分割パターンID=0を設定（単一カテゴリ）
            cardTransaction.setSplitPatternId(0L);
            // asset_category_idを直接設定
            cardTransaction.setAssetCategoryId(categoryId);
            cardTransactionRepository.save(cardTransaction);
        } else {
            throw new IllegalArgumentException("取引IDが指定されていません");
        }
        
        // カテゴリの存在確認とユーザー所有確認
        AssetCategory category = assetCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("資産カテゴリが見つかりません"));
        
        if (!category.getUserId().equals(userId)) {
            throw new IllegalArgumentException("他のユーザーのカテゴリは使用できません");
        }
        
        // 単一カテゴリの場合、split_transactionsには登録しない
        // asset_category_idに直接設定するのみ

        // 残高を更新
        if ("INCOME".equals(transactionType)) {
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
    private void splitManual(Long bankTransactionId, Long cardTransactionId, 
                            Map<Long, Long> categoryAmounts, String transactionType) {
        // 取引を取得
        Long totalAmount;
        Long userId;
        BankTransaction bankTransaction = null;
        CardTransaction cardTransaction = null;
        
        if (bankTransactionId != null) {
            bankTransaction = bankTransactionRepository.findById(bankTransactionId)
                    .orElseThrow(() -> new IllegalArgumentException("銀行取引が見つかりません"));
            totalAmount = bankTransaction.getAmount();
            userId = bankTransaction.getUserId();
            
            // 取引に分割パターンID=-1を設定（手動分割）、asset_category_idはNULLに
            bankTransaction.setSplitPatternId(-1L);
            bankTransaction.setAssetCategoryId(null);
            bankTransactionRepository.save(bankTransaction);
        } else if (cardTransactionId != null) {
            cardTransaction = cardTransactionRepository.findById(cardTransactionId)
                    .orElseThrow(() -> new IllegalArgumentException("カード取引が見つかりません"));
            totalAmount = cardTransaction.getAmount();
            userId = cardTransaction.getUserId();
            
            // 取引に分割パターンID=-1を設定（手動分割）、asset_category_idはNULLに
            cardTransaction.setSplitPatternId(-1L);
            cardTransaction.setAssetCategoryId(null);
            cardTransactionRepository.save(cardTransaction);
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
            
            // カテゴリの存在確認とユーザー所有確認
            AssetCategory category = assetCategoryRepository.findById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("資産カテゴリが見つかりません"));
            
            if (!category.getUserId().equals(userId)) {
                throw new IllegalArgumentException("他のユーザーのカテゴリは使用できません");
            }
            
            SplitTransaction split = new SplitTransaction();
            split.setTransactionType(transactionType);
            split.setAssetCategoryId(categoryId);
            split.setSplitPatternId(null); // 手動分割の場合はNULL
            split.setSplitMethod("MANUAL");
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
    public List<SplitTransaction> getSplitTransactionsByTransaction(Long bankTransactionId, Long cardTransactionId) {
        if (bankTransactionId != null) {
            return splitTransactionRepository.findByBankTransactionId(bankTransactionId);
        } else if (cardTransactionId != null) {
            return splitTransactionRepository.findByCardTransactionId(cardTransactionId);
        }
        return List.of();
    }
    
    /**
     * 残高を更新（収入・支出共通）
     */
    private void updateBalances(Long userId, List<SplitTransaction> splits, String transactionType, Long totalAmount) {
        if ("INCOME".equals(transactionType)) {
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
    
    // DTOs for pattern summaries
    public static class PatternSummary {
        private Long id;
        private String name;
        private String splitType;  // 'RATIO' or 'AMOUNT'
        private List<PatternDetail> details;

        public PatternSummary(Long id, String name, String splitType, List<PatternDetail> details) {
            this.id = id;
            this.name = name;
            this.splitType = splitType;
            this.details = details;
        }

        public Long getId() { return id; }
        public Long getPatternId() { return id; }  // テンプレート用のエイリアス
        public String getName() { return name; }
        public String getPatternName() { return name; }  // テンプレート用のエイリアス
        public String getSplitType() { return splitType; }
        public List<PatternDetail> getDetails() { return details; }
    }

    public static class PatternDetail {
        private String categoryName;
        private BigDecimal splitRatio;
        private Long splitAmount;
        private String splitType;

        public PatternDetail(String categoryName, BigDecimal splitRatio, Long splitAmount, String splitType) {
            this.categoryName = categoryName;
            this.splitRatio = splitRatio;
            this.splitAmount = splitAmount;
            this.splitType = splitType;
        }

        public String getCategoryName() { return categoryName; }
        public BigDecimal getSplitRatio() { return splitRatio; }
        public Long getSplitAmount() { return splitAmount; }
        public String getSplitType() { return splitType; }
    }
}

