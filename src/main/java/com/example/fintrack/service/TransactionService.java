package com.example.fintrack.service;

import com.example.fintrack.domain.*;
import com.example.fintrack.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 取引管理サービス
 */
@Service
public class TransactionService {
    
    private final BankTransactionRepository bankTransactionRepository;
    private final CardTransactionRepository cardTransactionRepository;
    private final AssetService assetService;
    
    public TransactionService(BankTransactionRepository bankTransactionRepository,
                             CardTransactionRepository cardTransactionRepository,
                             AssetService assetService) {
        this.bankTransactionRepository = bankTransactionRepository;
        this.cardTransactionRepository = cardTransactionRepository;
        this.assetService = assetService;
    }
    
    /**
     * 指定月の支出一覧を取得（銀行+カード）
     */
    public List<TransactionWithAsset> getExpensesByMonth(Long userId, YearMonth yearMonth, int page, int size) {
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        
        List<TransactionWithAsset> result = new ArrayList<>();
        
        // 銀行取引を全件取得
        List<BankTransaction> bankTransactions = bankTransactionRepository
                .findByUserIdAndTransactionTypeAndTransactionDateBetween(
                        userId, TransactionType.EXPENSE, startDate, endDate, 0, Integer.MAX_VALUE);
        
        for (BankTransaction bt : bankTransactions) {
            if (bt.getIsCardPayment() == null || !bt.getIsCardPayment()) {
                result.add(new TransactionWithAsset(bt, null));
            }
        }
        
        // カード取引を全件取得
        List<CardTransaction> cardTransactions = cardTransactionRepository
                .findByUserIdAndTransactionTypeAndTransactionDateBetween(
                        userId, TransactionType.EXPENSE, startDate, endDate, 0, Integer.MAX_VALUE);
        
        for (CardTransaction ct : cardTransactions) {
            result.add(new TransactionWithAsset(null, ct));
        }
        
        // 日付でソート（新しい順）
        result.sort((a, b) -> {
            LocalDate dateA = a.getTransactionDate();
            LocalDate dateB = b.getTransactionDate();
            return dateB.compareTo(dateA);
        });
        
        return result;
    }
    
    /**
     * 未分類の支出一覧を取得（銀行+カード）
     * split_pattern_idがNULLかつasset_category_idがNULLの取引のみ
     */
    public List<TransactionWithAsset> getUncategorizedExpenses(Long userId) {
        List<TransactionWithAsset> result = new ArrayList<>();
        
        // 未分類の銀行取引を取得（split_pattern_idがNULLかつasset_category_idがNULLかつ支出）
        List<BankTransaction> bankTransactions = bankTransactionRepository
                .findByUserIdAndSplitPatternIdIsNullAndTransactionType(
                        userId, TransactionType.EXPENSE);
        
        for (BankTransaction bt : bankTransactions) {
            result.add(new TransactionWithAsset(bt, null));
        }
        
        // 未分類のカード取引を取得（split_pattern_idがNULLかつ支出）
        List<CardTransaction> cardTransactions = cardTransactionRepository
                .findBySplitPatternNullAndType(
                        userId, TransactionType.EXPENSE);
        
        for (CardTransaction ct : cardTransactions) {
            result.add(new TransactionWithAsset(null, ct));
        }
        
        // 日付でソート（新しい順）
        result.sort((a, b) -> {
            LocalDate dateA = a.getTransactionDate();
            LocalDate dateB = b.getTransactionDate();
            return dateB.compareTo(dateA);
        });
        
        return result;
    }
    
    /**
     * 未分類の収入一覧を取得（銀行+カード）
     */
    public List<TransactionWithAsset> getUncategorizedIncomes(Long userId) {
        List<TransactionWithAsset> result = new ArrayList<>();
        
        // 未分類の銀行取引を取得（income_split_pattern_idがNULL）
        List<BankTransaction> bankTransactions = bankTransactionRepository
                .findByUserIdAndSplitPatternIdIsNullAndTransactionType(
                        userId, TransactionType.INCOME);
        
        for (BankTransaction bt : bankTransactions) {
            result.add(new TransactionWithAsset(bt, null));
        }
        
        // 未分類のカード取引を取得（income_split_pattern_idがNULL）
        List<CardTransaction> cardTransactions = cardTransactionRepository
                .findBySplitPatternNullAndType(
                        userId, TransactionType.INCOME);
        
        for (CardTransaction ct : cardTransactions) {
            result.add(new TransactionWithAsset(null, ct));
        }
        
        // 日付でソート（新しい順）
        result.sort((a, b) -> {
            LocalDate dateA = a.getTransactionDate();
            LocalDate dateB = b.getTransactionDate();
            return dateB.compareTo(dateA);
        });
        
        return result;
    }
    
    /**
     * 指定月の収入一覧を取得（銀行+カード）
     */
    public List<TransactionWithAsset> getIncomesByMonth(Long userId, YearMonth yearMonth, int page, int size) {
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        
        List<TransactionWithAsset> result = new ArrayList<>();
        
        // 銀行取引を全件取得
        List<BankTransaction> bankTransactions = bankTransactionRepository
                .findByUserIdAndTransactionTypeAndTransactionDateBetween(
                        userId, TransactionType.INCOME, startDate, endDate, 0, Integer.MAX_VALUE);
        
        for (BankTransaction bt : bankTransactions) {
            result.add(new TransactionWithAsset(bt, null));
        }
        
        // カード取引を全件取得
        List<CardTransaction> cardTransactions = cardTransactionRepository
                .findByUserIdAndTransactionTypeAndTransactionDateBetween(
                        userId, TransactionType.INCOME, startDate, endDate, 0, Integer.MAX_VALUE);
        
        for (CardTransaction ct : cardTransactions) {
            result.add(new TransactionWithAsset(null, ct));
        }
        
        // 日付でソート（新しい順）
        result.sort((a, b) -> {
            LocalDate dateA = a.getTransactionDate();
            LocalDate dateB = b.getTransactionDate();
            return dateB.compareTo(dateA);
        });
        
        return result;
    }
    
    /**
     * 銀行取引を取得
     */
    public Optional<BankTransaction> getBankTransaction(Long id) {
        return bankTransactionRepository.findById(id);
    }
    
    /**
     * カード取引を取得
     */
    public Optional<CardTransaction> getCardTransaction(Long id) {
        return cardTransactionRepository.findById(id);
    }
    
    /**
     * 支出の資産カテゴリを更新
     * 残高も適切に移動する
     */
    @Transactional
    public void updateExpenseAssetCategory(Long bankTransactionId, Long cardTransactionId, Long newAssetCategoryId) {
        if (bankTransactionId != null) {
            BankTransaction transaction = bankTransactionRepository.findById(bankTransactionId)
                    .orElseThrow(() -> new IllegalArgumentException("銀行取引が見つかりません"));
            
            Long userId = transaction.getUserId();
            Long oldAssetCategoryId = transaction.getAssetCategoryId();
            Long amount = transaction.getAmount();
            
            // 旧カテゴリに戻す（マイナスを取り消す）
            if (oldAssetCategoryId != null || newAssetCategoryId != null) {
                assetService.updateBalance(userId, oldAssetCategoryId, amount);
                
                // 新カテゴリから引く
                assetService.updateBalance(userId, newAssetCategoryId, -amount);
            }
            
            // 取引のカテゴリを更新
            transaction.setAssetCategoryId(newAssetCategoryId);
            bankTransactionRepository.save(transaction);
            
        } else if (cardTransactionId != null) {
            CardTransaction transaction = cardTransactionRepository.findById(cardTransactionId)
                    .orElseThrow(() -> new IllegalArgumentException("カード取引が見つかりません"));
            
            Long userId = transaction.getUserId();
            Long oldAssetCategoryId = transaction.getAssetCategoryId();
            Long amount = transaction.getAmount();
            
            // 旧カテゴリに戻す（マイナスを取り消す）
            if (oldAssetCategoryId != null || newAssetCategoryId != null) {
                assetService.updateBalance(userId, oldAssetCategoryId, amount);
                
                // 新カテゴリから引く
                assetService.updateBalance(userId, newAssetCategoryId, -amount);
            }
            
            // 取引のカテゴリを更新
            transaction.setAssetCategoryId(newAssetCategoryId);
            cardTransactionRepository.save(transaction);
        }
    }
    
    /**
     * 支出カテゴリを更新
     */
    @Transactional
    public void updateExpenseCategory(Long bankTransactionId, Long cardTransactionId, Long expenseCategoryId) {
        if (bankTransactionId != null) {
            BankTransaction transaction = bankTransactionRepository.findById(bankTransactionId)
                    .orElseThrow(() -> new IllegalArgumentException("銀行取引が見つかりません"));
            transaction.setExpenseCategoryId(expenseCategoryId);
            bankTransactionRepository.save(transaction);
            
        } else if (cardTransactionId != null) {
            CardTransaction transaction = cardTransactionRepository.findById(cardTransactionId)
                    .orElseThrow(() -> new IllegalArgumentException("カード取引が見つかりません"));
            transaction.setExpenseCategoryId(expenseCategoryId);
            cardTransactionRepository.save(transaction);
        }
    }
    
    /**
     * 取引と資産をまとめたDTO
     */
    public static class TransactionWithAsset {
        private final BankTransaction bankTransaction;
        private final CardTransaction cardTransaction;
        
        public TransactionWithAsset(BankTransaction bankTransaction, 
                                   CardTransaction cardTransaction) {
            this.bankTransaction = bankTransaction;
            this.cardTransaction = cardTransaction;
        }
        
        public BankTransaction getBankTransaction() {
            return bankTransaction;
        }
        
        public CardTransaction getCardTransaction() {
            return cardTransaction;
        }
        
        public String getTransactionSource() {
            if (bankTransaction != null) {
                return "銀行";
            }
            if (cardTransaction != null) {
                if ("DEBIT".equals(cardTransaction.getCardType())) {
                    return "デビットカード";
                }
                if ("CREDIT".equals(cardTransaction.getCardType())) {
                    return "クレジットカード";
                }
            }
            return "カード";
        }
        
        public LocalDate getTransactionDate() {
            if (bankTransaction != null) {
                return bankTransaction.getTransactionDate();
            }
            return cardTransaction.getTransactionDate();
        }
        
        public String getDescription() {
            if (bankTransaction != null) {
                return bankTransaction.getDescription();
            }
            return cardTransaction.getDescription();
        }
        
        public Long getAmount() {
            if (bankTransaction != null) {
                return bankTransaction.getAmount();
            }
            return cardTransaction.getAmount();
        }
        
        public Long getTransactionId() {
            if (bankTransaction != null) {
                return bankTransaction.getId();
            }
            return cardTransaction.getId();
        }
        
        public Long getAssetCategoryId() {
            if (bankTransaction != null) {
                return bankTransaction.getAssetCategoryId();
            }
            return cardTransaction.getAssetCategoryId();
        }
        
        public Long getExpenseCategoryId() {
            if (bankTransaction != null) {
                return bankTransaction.getExpenseCategoryId();
            }
            return cardTransaction.getExpenseCategoryId();
        }
        
        public Long getIncomeSplitPatternId() {
            if (bankTransaction != null) {
                return bankTransaction.getIncomeSplitPatternId();
            }
            return cardTransaction.getIncomeSplitPatternId();
        }
        
        public Long getSplitPatternId() {
            if (bankTransaction != null) {
                return bankTransaction.getSplitPatternId();
            }
            return cardTransaction.getSplitPatternId();
        }
    }
}
