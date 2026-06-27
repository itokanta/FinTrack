package com.example.fintrack.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.example.fintrack.domain.BankTransaction;
import com.example.fintrack.domain.CardTransaction;
import com.example.fintrack.domain.SplitMethod;
import com.example.fintrack.domain.TransactionType;
import com.example.fintrack.domain.dto.TransactionEntry;
import com.example.fintrack.repository.BankTransactionRepository;
import com.example.fintrack.repository.CardTransactionRepository;
import com.example.fintrack.repository.DiscrepancyTransactionRepository;

/**
 * 取引管理サービス
 */
@Service
public class TransactionService {

    private final BankTransactionRepository bankTransactionRepository;
    private final CardTransactionRepository cardTransactionRepository;
    private final DiscrepancyTransactionRepository discrepancyTransactionRepository;

    public TransactionService(BankTransactionRepository bankTransactionRepository,
        CardTransactionRepository cardTransactionRepository,
        DiscrepancyTransactionRepository discrepancyTransactionRepository) {
      this.bankTransactionRepository = bankTransactionRepository;
      this.cardTransactionRepository = cardTransactionRepository;
      this.discrepancyTransactionRepository = discrepancyTransactionRepository;
    }

    public List<String> findBankDistinctDescriptionsByUserId(Long userId) {
        return bankTransactionRepository.findDistinctDescriptionsByUserId(userId);
    }

    public List<String> findCardDistinctDescriptionsByUserId(Long userId) {
        return cardTransactionRepository.findDistinctDescriptionsByUserId(userId);
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

        // 未分類の銀行取引を取得（split_pattern_idがNULL）
        List<BankTransaction> bankTransactions = bankTransactionRepository
                .findByUserIdAndSplitPatternIdIsNullAndTransactionType(
                        userId, TransactionType.INCOME);

        for (BankTransaction bt : bankTransactions) {
            result.add(new TransactionWithAsset(bt, null));
        }

        // 未分類のカード取引を取得（split_pattern_idがNULL）
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
    public Optional<BankTransaction> getBankTransaction(Long id, Long userId) {
        return bankTransactionRepository.findById(id, userId);
    }

    /**
     * カード取引を取得
     */
    public Optional<CardTransaction> getCardTransaction(Long id, Long userId) {
        return cardTransactionRepository.findById(id, userId);
    }

    public Map<TransactionType, List<TransactionEntry>> getTransactionListByUserIdAndTransactionDate(Long userId, YearMonth yearMonth) {
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<TransactionEntry> bankTransactionIncomeList = bankTransactionRepository.findByUserIdAndTransactionDate(userId, TransactionType.INCOME, startDate, endDate);
        List<TransactionEntry> cardTransactionIncomeList = cardTransactionRepository.findByUserIdAndTransactionDate(userId, TransactionType.INCOME, startDate, endDate);
        List<TransactionEntry> discrepancyTransactionIncomeList = discrepancyTransactionRepository.findByUserIdAndTransactionDate(userId, TransactionType.INCOME, startDate, endDate);
        List<TransactionEntry> bankTransactionExpenseList = bankTransactionRepository.findByUserIdAndTransactionDate(userId, TransactionType.EXPENSE, startDate, endDate);
        List<TransactionEntry> cardTransactionExpenseList = cardTransactionRepository.findByUserIdAndTransactionDate(userId, TransactionType.EXPENSE, startDate, endDate);
        List<TransactionEntry> discrepancyTransactionExpenseList = discrepancyTransactionRepository.findByUserIdAndTransactionDate(userId, TransactionType.EXPENSE, startDate, endDate);

        List<TransactionEntry> incomeTotalList = sortTransactionEntryList(bankTransactionIncomeList, cardTransactionIncomeList, discrepancyTransactionIncomeList);
        List<TransactionEntry> expenseTotalList = sortTransactionEntryList(bankTransactionExpenseList, cardTransactionExpenseList, discrepancyTransactionExpenseList);

        Map<TransactionType, List<TransactionEntry>> result = new HashMap<>();
        result.put(TransactionType.INCOME, incomeTotalList);
        result.put(TransactionType.EXPENSE, expenseTotalList);

        return result;
    }

    private List<TransactionEntry> sortTransactionEntryList(List<TransactionEntry> transactionEntryList1, List<TransactionEntry> transactionEntryList2, List<TransactionEntry> transactionEntryList3) {
        List<TransactionEntry> result = Stream.of(transactionEntryList1, transactionEntryList2, transactionEntryList3)
                .flatMap(List::stream)
                .sorted(
                    Comparator.comparing(TransactionEntry::getAmount)
                            .thenComparing(TransactionEntry::getTransactionDate)
                            .thenComparing(TransactionEntry::getDescription)
                            .reversed())
                .collect(Collectors.toList());
        return result;
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

        public Long getSplitPatternId() {
            if (bankTransaction != null) {
                return bankTransaction.getSplitPatternId();
            }
            return cardTransaction.getSplitPatternId();
        }

        public SplitMethod getSplitMethod() {
            if (bankTransaction != null) {
                return bankTransaction.getSplitMethod();
            }
            return cardTransaction.getSplitMethod();
        }
    }
}
