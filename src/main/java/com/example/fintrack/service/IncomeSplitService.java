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
 * 収入分割管理サービス
 */
@Service
public class IncomeSplitService {
    
    private final SplitPatternRepository splitPatternRepository;
    private final SplitTransactionRepository splitTransactionRepository;
    private final AssetCategoryRepository assetCategoryRepository;
    private final AssetRepository assetRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final CardTransactionRepository cardTransactionRepository;
    
    private static final String TRANSACTION_TYPE_INCOME = "INCOME";
    private static final String SPLIT_TYPE_RATIO = "RATIO";
    
    public IncomeSplitService(SplitPatternRepository splitPatternRepository,
                             SplitTransactionRepository splitTransactionRepository,
                             AssetCategoryRepository assetCategoryRepository,
                             AssetRepository assetRepository,
                             BankTransactionRepository bankTransactionRepository,
                             CardTransactionRepository cardTransactionRepository) {
        this.splitPatternRepository = splitPatternRepository;
        this.splitTransactionRepository = splitTransactionRepository;
        this.assetCategoryRepository = assetCategoryRepository;
        this.assetRepository = assetRepository;
        this.bankTransactionRepository = bankTransactionRepository;
        this.cardTransactionRepository = cardTransactionRepository;
    }
    
    /**
     * ユーザーの分割パターン一覧を取得
     */
    public List<Long> getSplitPatternIds(Long userId) {
        return splitPatternRepository.findDistinctPatternIdsByUserIdAndTransactionType(userId, TRANSACTION_TYPE_INCOME);
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
    public List<PatternSummary> getPatternSummaries(Long userId) {
        List<Long> patternIds = splitPatternRepository.findDistinctPatternIdsByUserIdAndTransactionType(userId, TRANSACTION_TYPE_INCOME);
        List<PatternSummary> summaries = new ArrayList<>();

        for (Long patternId : patternIds) {
            List<SplitPattern> patterns = splitPatternRepository.findById(patternId);
            if (!patterns.isEmpty()) {
                String patternName = patterns.get(0).getPatternName();
                List<PatternDetail> details = patterns.stream()
                        .map(p -> {
                            String categoryName = assetCategoryRepository.findById(p.getAssetCategoryId())
                                    .map(AssetCategory::getCategoryName)
                                    .orElse("不明なカテゴリ");
                            return new PatternDetail(categoryName, p.getSplitRatio());
                        })
                        .collect(Collectors.toList());
                summaries.add(new PatternSummary(patternId, patternName, details));
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
     * 新しい分割パターンを作成
     */
    @Transactional
    public Long createSplitPattern(Long userId, String patternName, Map<Long, BigDecimal> categoryRatios) {
        // 新しいパターンIDを生成
        Long maxId = splitPatternRepository.findMaxPatternId();
        if (maxId == null) {
            maxId = 0L;
        }
        Long newPatternId = maxId + 1;
        
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
            AssetCategory category = assetCategoryRepository.findById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("資産カテゴリが見つかりません"));
            
            if (!category.getUserId().equals(userId)) {
                throw new IllegalArgumentException("他のユーザーのカテゴリは使用できません");
            }
            
            SplitPattern pattern = new SplitPattern();
            pattern.setPatternId(newPatternId);
            pattern.setUserId(userId);
            pattern.setTransactionType(TRANSACTION_TYPE_INCOME);
            pattern.setSplitType(SPLIT_TYPE_RATIO);
            pattern.setAssetCategoryId(categoryId);
            pattern.setSplitRatio(entry.getValue());
            pattern.setSplitAmount(null);
            pattern.setPatternName(patternName);
            
            splitPatternRepository.save(pattern);
        }
        
        return newPatternId;
    }
    
    /**
     * 収入を分割して資産に登録
     */
    @Transactional
    public void splitIncome(Long bankTransactionId, Long cardTransactionId, Long patternId) {
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
            
            // 取引に分割パターンIDを設定
            bankTransaction.setSplitPatternId(patternId);
            bankTransactionRepository.updateAssetCategoryOrSplitPatternOrExpenseCategory(bankTransaction);
        } else if (cardTransactionId != null) {
            cardTransaction = cardTransactionRepository.findById(cardTransactionId)
                    .orElseThrow(() -> new IllegalArgumentException("カード取引が見つかりません"));
            totalAmount = cardTransaction.getAmount();
            userId = cardTransaction.getUserId();
            
            // 取引に分割パターンIDを設定
            cardTransaction.setSplitPatternId(patternId);
            cardTransactionRepository.updateAssetCategoryOrSplitPatternOrExpenseCategory(cardTransaction);
        } else {
            throw new IllegalArgumentException("取引IDが指定されていません");
        }
        
        // 分割パターンを取得
        List<SplitPattern> patterns = splitPatternRepository.findById(patternId);
        if (patterns.isEmpty()) {
            throw new IllegalArgumentException("分割パターンが見つかりません");
        }
        
        // 分割金額を計算
        List<SplitTransaction> splits = new ArrayList<>();
        long totalSplit = 0;
        
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
            
            SplitTransaction split = new SplitTransaction();
            split.setTransactionType(TRANSACTION_TYPE_INCOME);
            split.setAssetCategoryId(pattern.getAssetCategoryId());
            split.setSplitPatternId(patternId);
            split.setSplitMethod("PATTERN");
            split.setAmount(splitAmount);
            split.setBankTransactionId(bankTransactionId);
            split.setCardTransactionId(cardTransactionId);
            
            splits.add(split);
        }
        
        // 保存
        splitTransactionRepository.saveAll(splits);
        
        // 各資産カテゴリの残高を更新
        // まず未分類から全額を引く（CSV取込時に未分類に入っているため）
        assetRepository.updateAmountByCategoryId(userId, null, -totalAmount);
        
        // 各カテゴリに分割金額を加算
        for (SplitTransaction split : splits) {
            assetRepository.updateAmountByCategoryId(userId, split.getAssetCategoryId(), split.getAmount());
        }
    }
    
    /**
     * 取引の分割状況を取得
     */
    public List<SplitTransaction> getSplitIncomesByTransaction(Long bankTransactionId, Long cardTransactionId) {
        if (bankTransactionId != null) {
            return splitTransactionRepository.findByBankTransactionId(bankTransactionId);
        } else if (cardTransactionId != null) {
            return splitTransactionRepository.findByCardTransactionId(cardTransactionId);
        }
        return List.of();
    }
    
    /**
     * 収入を手動で金額指定して分割
     */
    @Transactional
    public void splitIncomeManual(Long bankTransactionId, Long cardTransactionId, Map<Long, Long> categoryAmounts) {
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
            
            // 取引に分割パターンID=-1を設定（手動分割）
            bankTransaction.setSplitPatternId(-1L);
            bankTransactionRepository.updateAssetCategoryOrSplitPatternOrExpenseCategory(bankTransaction);
        } else if (cardTransactionId != null) {
            cardTransaction = cardTransactionRepository.findById(cardTransactionId)
                    .orElseThrow(() -> new IllegalArgumentException("カード取引が見つかりません"));
            totalAmount = cardTransaction.getAmount();
            userId = cardTransaction.getUserId();
            
            // 取引に分割パターンID=-1を設定（手動分割）
            cardTransaction.setSplitPatternId(-1L);
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
        
        // 各カテゴリに分割収入を作成
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
            split.setTransactionType(TRANSACTION_TYPE_INCOME);
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
        
        // 資産残高を更新
        // まず未分類から全額を引く（CSV取込時に未分類に入っているため）
        assetRepository.updateAmountByCategoryId(userId, null, -totalAmount);
        
        // 各カテゴリに分割金額を加算
        for (SplitTransaction split : splits) {
            assetRepository.updateAmountByCategoryId(userId, split.getAssetCategoryId(), split.getAmount());
        }
    }
    
    /**
     * 収入を全額1つのカテゴリに分配
     */
    @Transactional
    public void splitIncomeSingle(Long bankTransactionId, Long cardTransactionId, Long categoryId) {
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
            
            // 取引に分割パターンID=0を設定（単一カテゴリ分配）
            bankTransaction.setSplitPatternId(0L);
            bankTransactionRepository.updateAssetCategoryOrSplitPatternOrExpenseCategory(bankTransaction);
        } else if (cardTransactionId != null) {
            cardTransaction = cardTransactionRepository.findById(cardTransactionId)
                    .orElseThrow(() -> new IllegalArgumentException("カード取引が見つかりません"));
            totalAmount = cardTransaction.getAmount();
            userId = cardTransaction.getUserId();
            
            // 取引に分割パターンID=0を設定（単一カテゴリ分配）
            cardTransaction.setSplitPatternId(0L);
            cardTransactionRepository.updateAssetCategoryOrSplitPatternOrExpenseCategory(cardTransaction);
        } else {
            throw new IllegalArgumentException("取引IDが指定されていません");
        }
        
        // カテゴリの存在確認
        assetCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("資産カテゴリが見つかりません"));
        
        // 分割収入を作成（パターンIDはNULL）
        SplitTransaction split = new SplitTransaction();
        split.setTransactionType(TRANSACTION_TYPE_INCOME);
        split.setAssetCategoryId(categoryId);
        split.setSplitPatternId(null); // 単一カテゴリ分配の場合はNULL
        split.setSplitMethod("SINGLE");
        split.setAmount(totalAmount);
        split.setBankTransactionId(bankTransactionId);
        split.setCardTransactionId(cardTransactionId);
        
        splitTransactionRepository.save(split);
        
        // 資産残高を更新
        // まず未分類から全額を引く（CSV取込時に未分類に入っているため）
        assetRepository.updateAmountByCategoryId(userId, null, -totalAmount);
        
        // 指定カテゴリに全額を加算
        assetRepository.updateAmountByCategoryId(userId, categoryId, totalAmount);
    }

    
    // DTOs for pattern summaries
    public static class PatternSummary {
        private Long id;
        private String name;
        private List<PatternDetail> details;

        public PatternSummary(Long id, String name, List<PatternDetail> details) {
            this.id = id;
            this.name = name;
            this.details = details;
        }

        public Long getId() { return id; }
        public Long getPatternId() { return id; }  // テンプレート用のエイリアス
        public String getName() { return name; }
        public String getPatternName() { return name; }  // テンプレート用のエイリアス
        public List<PatternDetail> getDetails() { return details; }
    }

    public static class PatternDetail {
        private String categoryName;
        private BigDecimal splitRatio;

        public PatternDetail(String categoryName, BigDecimal splitRatio) {
            this.categoryName = categoryName;
            this.splitRatio = splitRatio;
        }

        public String getCategoryName() { return categoryName; }
        public BigDecimal getSplitRatio() { return splitRatio; }
    }
}
