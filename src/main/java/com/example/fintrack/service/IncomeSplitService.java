package com.example.fintrack.service;

import com.example.fintrack.domain.*;
import com.example.fintrack.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 収入分割管理サービス
 */
@Service
public class IncomeSplitService {
    
    private final IncomeSplitPatternRepository incomeSplitPatternRepository;
    private final SplitIncomeRepository splitIncomeRepository;
    private final AssetCategoryRepository assetCategoryRepository;
    private final AssetService assetService;
    private final BankTransactionRepository bankTransactionRepository;
    private final CardTransactionRepository cardTransactionRepository;
    
    public IncomeSplitService(IncomeSplitPatternRepository incomeSplitPatternRepository,
                             SplitIncomeRepository splitIncomeRepository,
                             AssetCategoryRepository assetCategoryRepository,
                             AssetService assetService,
                             BankTransactionRepository bankTransactionRepository,
                             CardTransactionRepository cardTransactionRepository) {
        this.incomeSplitPatternRepository = incomeSplitPatternRepository;
        this.splitIncomeRepository = splitIncomeRepository;
        this.assetCategoryRepository = assetCategoryRepository;
        this.assetService = assetService;
        this.bankTransactionRepository = bankTransactionRepository;
        this.cardTransactionRepository = cardTransactionRepository;
    }
    
    /**
     * ユーザーの分割パターン一覧を取得
     */
    public List<Long> getSplitPatternIds(Long userId) {
        return incomeSplitPatternRepository.findDistinctIdsByUserId(userId);
    }
    
    /**
     * 特定の分割パターンを取得
     */
    public List<IncomeSplitPattern> getSplitPatternById(Long patternId) {
        return incomeSplitPatternRepository.findById(patternId);
    }
    
    /**
     * 新しい分割パターンを作成
     */
    @Transactional
    public Long createSplitPattern(String patternName, Map<Long, BigDecimal> categoryRatios) {
        // 新しいパターンIDを生成
        Long maxId = incomeSplitPatternRepository.findMaxId();
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
            AssetCategory category = assetCategoryRepository.findById(entry.getKey())
                    .orElseThrow(() -> new IllegalArgumentException("資産カテゴリが見つかりません"));
            
            IncomeSplitPattern pattern = new IncomeSplitPattern();
            pattern.setId(newPatternId);
            pattern.setAssetCategory(category);
            pattern.setSplitRatio(entry.getValue());
            pattern.setPatternName(patternName);
            
            incomeSplitPatternRepository.save(pattern);
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
        BankTransaction bankTransaction = null;
        CardTransaction cardTransaction = null;
        
        if (bankTransactionId != null) {
            bankTransaction = bankTransactionRepository.findById(bankTransactionId)
                    .orElseThrow(() -> new IllegalArgumentException("銀行取引が見つかりません"));
            totalAmount = bankTransaction.getAmount();
            
            // 取引に分割パターンIDを設定
            bankTransaction.setIncomeSplitPatternId(patternId);
            bankTransactionRepository.save(bankTransaction);
        } else if (cardTransactionId != null) {
            cardTransaction = cardTransactionRepository.findById(cardTransactionId)
                    .orElseThrow(() -> new IllegalArgumentException("カード取引が見つかりません"));
            totalAmount = cardTransaction.getAmount();
            
            // 取引に分割パターンIDを設定
            cardTransaction.setIncomeSplitPatternId(patternId);
            cardTransactionRepository.save(cardTransaction);
        } else {
            throw new IllegalArgumentException("取引IDが指定されていません");
        }
        
        // 分割パターンを取得
        List<IncomeSplitPattern> patterns = incomeSplitPatternRepository.findById(patternId);
        if (patterns.isEmpty()) {
            throw new IllegalArgumentException("分割パターンが見つかりません");
        }
        
        // 分割金額を計算
        List<SplitIncome> splits = new ArrayList<>();
        long totalSplit = 0;
        
        for (int i = 0; i < patterns.size(); i++) {
            IncomeSplitPattern pattern = patterns.get(i);
            long splitAmount;
            
            if (i == patterns.size() - 1) {
                // 最後のカテゴリには差額を含める（端数調整）
                splitAmount = totalAmount - totalSplit;
            } else {
                splitAmount = totalAmount * pattern.getSplitRatio().longValue() / 100;
            }
            
            totalSplit += splitAmount;
            
            SplitIncome split = new SplitIncome();
            split.setAssetCategoryId(pattern.getAssetCategoryId());
            split.setSplitPatternId(patternId);
            split.setAmount(splitAmount);
            split.setBankTransactionId(bankTransactionId);
            split.setCardTransactionId(cardTransactionId);
            
            splits.add(split);
        }
        
        // 保存
        splitIncomeRepository.saveAll(splits);
        
        // 各資産カテゴリの残高を更新
        // まず未分類から全額を引く（CSV取込時に未分類に入っているため）
        assetService.updateBalance(null, -totalAmount);
        
        // 各カテゴリに分割金額を加算
        for (SplitIncome split : splits) {
            assetService.updateBalance(split.getAssetCategoryId(), split.getAmount());
        }
    }
    
    /**
     * 取引の分割状況を取得
     */
    public List<SplitIncome> getSplitIncomesByTransaction(Long bankTransactionId, Long cardTransactionId) {
        if (bankTransactionId != null) {
            return splitIncomeRepository.findByBankTransactionId(bankTransactionId);
        } else if (cardTransactionId != null) {
            return splitIncomeRepository.findByCardTransactionId(cardTransactionId);
        }
        return List.of();
    }
    
    /**
     * 収入を全額1つのカテゴリに分配
     */
    @Transactional
    public void splitIncomeSingle(Long bankTransactionId, Long cardTransactionId, Long categoryId) {
        // 取引を取得
        Long totalAmount;
        BankTransaction bankTransaction = null;
        CardTransaction cardTransaction = null;
        
        if (bankTransactionId != null) {
            bankTransaction = bankTransactionRepository.findById(bankTransactionId)
                    .orElseThrow(() -> new IllegalArgumentException("銀行取引が見つかりません"));
            totalAmount = bankTransaction.getAmount();
            
            // 取引に分割パターンID=0を設定（単一カテゴリ分配）
            bankTransaction.setIncomeSplitPatternId(0L);
            bankTransactionRepository.save(bankTransaction);
        } else if (cardTransactionId != null) {
            cardTransaction = cardTransactionRepository.findById(cardTransactionId)
                    .orElseThrow(() -> new IllegalArgumentException("カード取引が見つかりません"));
            totalAmount = cardTransaction.getAmount();
            
            // 取引に分割パターンID=0を設定（単一カテゴリ分配）
            cardTransaction.setIncomeSplitPatternId(0L);
            cardTransactionRepository.save(cardTransaction);
        } else {
            throw new IllegalArgumentException("取引IDが指定されていません");
        }
        
        // カテゴリの存在確認
        assetCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("資産カテゴリが見つかりません"));
        
        // 分割収入を作成（パターンIDは0を使用）
        SplitIncome split = new SplitIncome();
        split.setAssetCategoryId(categoryId);
        split.setSplitPatternId(0L); // 単一カテゴリ分配の場合は0
        split.setAmount(totalAmount);
        split.setBankTransactionId(bankTransactionId);
        split.setCardTransactionId(cardTransactionId);
        
        splitIncomeRepository.save(split);
        
        // 資産残高を更新
        // まず未分類から全額を引く（CSV取込時に未分類に入っているため）
        assetService.updateBalance(null, -totalAmount);
        
        // 指定カテゴリに全額を加算
        assetService.updateBalance(categoryId, totalAmount);
    }
}
