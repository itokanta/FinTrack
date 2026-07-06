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
import com.example.fintrack.domain.dto.TransactionsByType;
import com.example.fintrack.repository.BankTransactionRepository;
import com.example.fintrack.repository.CardTransactionRepository;
import com.example.fintrack.repository.DiscrepancyTransactionRepository;
import com.example.fintrack.repository.SplitTransactionRepository;

/**
 * 銀行・カード取引の検索・一覧取得を扱うサービス。
 *
 * @author kanta ito
 */
@Service
public class TransactionService {

    private final BankTransactionRepository bankTransactionRepository;
    private final CardTransactionRepository cardTransactionRepository;
    private final DiscrepancyTransactionRepository discrepancyTransactionRepository;
    private final SplitTransactionRepository splitTransactionRepository;

    public TransactionService(BankTransactionRepository bankTransactionRepository,
        CardTransactionRepository cardTransactionRepository,
        DiscrepancyTransactionRepository discrepancyTransactionRepository,
        SplitTransactionRepository splitTransactionRepository) {
      this.bankTransactionRepository = bankTransactionRepository;
      this.cardTransactionRepository = cardTransactionRepository;
      this.discrepancyTransactionRepository = discrepancyTransactionRepository;
      this.splitTransactionRepository = splitTransactionRepository;
    }

    /**
     * ユーザーの銀行取引摘要（重複なし）を取得する。
     *
     * @param userId ユーザーID
     * @return 摘要の一覧
     */
    public List<String> findBankDistinctDescriptionsByUserId(Long userId) {
        return bankTransactionRepository.findDistinctDescriptionsByUserId(userId);
    }

    /**
     * ユーザーのカード取引摘要（重複なし）を取得する。
     *
     * @param userId ユーザーID
     * @return 摘要の一覧
     */
    public List<String> findCardDistinctDescriptionsByUserId(Long userId) {
        return cardTransactionRepository.findDistinctDescriptionsByUserId(userId);
    }

    /**
     * 指定月の支出一覧を取得する（銀行＋カード、カード引き落とし行は除外）。
     *
     * @param userId    ユーザーID
     * @param yearMonth 対象年月
     * @param page      ページ番号（現状未使用）
     * @param size      ページサイズ（現状未使用）
     * @return 取引の一覧（日付の新しい順）
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
     * 未分類の支出一覧を取得する（銀行＋カード）。
     *
     * @param userId ユーザーID
     * @return 未分類の支出取引一覧（日付の新しい順）
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
     * 未分類の収入一覧を取得する（銀行＋カード）。
     *
     * @param userId ユーザーID
     * @return 未分類の収入取引一覧（日付の新しい順）
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
     * 指定月の収入一覧を取得する（銀行＋カード）。
     *
     * @param userId    ユーザーID
     * @param yearMonth 対象年月
     * @param page      ページ番号（現状未使用）
     * @param size      ページサイズ（現状未使用）
     * @return 取引の一覧（日付の新しい順）
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
     * 銀行取引をIDで取得する。
     *
     * @param id     取引ID
     * @param userId ユーザーID
     * @return 取引（なければ空）
     */
    public Optional<BankTransaction> getBankTransaction(Long id, Long userId) {
        return bankTransactionRepository.findById(id, userId);
    }

    /**
     * カード取引をIDで取得する。
     *
     * @param id     取引ID
     * @param userId ユーザーID
     * @return 取引（なければ空）
     */
    public Optional<CardTransaction> getCardTransaction(Long id, Long userId) {
        return cardTransactionRepository.findById(id, userId);
    }

    /**
     * 指定月の収入・支出取引をまとめて取得する（銀行・カード・差異取引を統合）。
     *
     * @param userId    ユーザーID
     * @param yearMonth 対象年月
     * @return 収入・支出それぞれの取引エントリ一覧
     */
    public TransactionsByType getTransactionListByUserIdAndTransactionDate(Long userId, YearMonth yearMonth) {
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<TransactionEntry> bankTransactionIncomeList = bankTransactionRepository.findByUserIdAndTransactionDate(userId, TransactionType.INCOME, startDate, endDate);
        List<TransactionEntry> cardTransactionIncomeList = cardTransactionRepository.findByUserIdAndTransactionDate(userId, TransactionType.INCOME, startDate, endDate);
        List<TransactionEntry> discrepancyTransactionIncomeList = discrepancyTransactionRepository.findByUserIdAndTransactionDate(userId, TransactionType.INCOME, startDate, endDate);
        List<TransactionEntry> bankTransactionExpenseList = bankTransactionRepository.findByUserIdAndTransactionDate(userId, TransactionType.EXPENSE, startDate, endDate);
        List<TransactionEntry> cardTransactionExpenseList = cardTransactionRepository.findByUserIdAndTransactionDate(userId, TransactionType.EXPENSE, startDate, endDate);
        List<TransactionEntry> discrepancyTransactionExpenseList = discrepancyTransactionRepository.findByUserIdAndTransactionDate(userId, TransactionType.EXPENSE, startDate, endDate);

        TransactionsByType result = new TransactionsByType();
        result.setIncome(sortTransactionEntryList(bankTransactionIncomeList, cardTransactionIncomeList, discrepancyTransactionIncomeList));
        result.setExpense(sortTransactionEntryList(bankTransactionExpenseList, cardTransactionExpenseList, discrepancyTransactionExpenseList));

        return result;
    }

    /**
     * 指定月の取引を資産カテゴリ別に取得する（単一カテゴリ＋分割明細を含む）。
     *
     * @param userId    ユーザーID
     * @param yearMonth 対象年月
     * @return 資産カテゴリIDをキー、収入・支出一覧を値とするマップ
     */
    public Map<Long, TransactionsByType> getTransactionListByUserIdAndTransactionDateAndSplitMethod(Long userId, YearMonth yearMonth) {
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<TransactionEntry> bankTransactionIncomeList = bankTransactionRepository.findByUserIdAndTransactionDateAndSplitMethodIsSingle(userId, TransactionType.INCOME, startDate, endDate);
        List<TransactionEntry> cardTransactionIncomeList = cardTransactionRepository.findByUserIdAndTransactionDateAndSplitMethodIsSingle(userId, TransactionType.INCOME, startDate, endDate);
        List<TransactionEntry> discrepancyTransactionIncomeList = discrepancyTransactionRepository.findByUserIdAndTransactionDateAndSplitMethodIsSingle(userId, TransactionType.INCOME, startDate, endDate);
        List<TransactionEntry> splitTransactionBankIncomeList = splitTransactionRepository.findByUserIdAndTransactionDateFromBankSplits(userId, TransactionType.INCOME, startDate, endDate);
        List<TransactionEntry> splitTransactionCardIncomeList = splitTransactionRepository.findByUserIdAndTransactionDateFromCardSplits(userId, TransactionType.INCOME, startDate, endDate);
        List<TransactionEntry> splitTransactionDiscrepancyIncomeList = splitTransactionRepository.findByUserIdAndTransactionDateFromDiscrepancySplits(userId, TransactionType.INCOME, startDate, endDate);

        List<TransactionEntry> bankTransactionExpenseList = bankTransactionRepository.findByUserIdAndTransactionDateAndSplitMethodIsSingle(userId, TransactionType.EXPENSE, startDate, endDate);
        List<TransactionEntry> cardTransactionExpenseList = cardTransactionRepository.findByUserIdAndTransactionDateAndSplitMethodIsSingle(userId, TransactionType.EXPENSE, startDate, endDate);
        List<TransactionEntry> discrepancyTransactionExpenseList = discrepancyTransactionRepository.findByUserIdAndTransactionDateAndSplitMethodIsSingle(userId, TransactionType.EXPENSE, startDate, endDate);
        List<TransactionEntry> splitTransactionBankExpenseList = splitTransactionRepository.findByUserIdAndTransactionDateFromBankSplits(userId, TransactionType.EXPENSE, startDate, endDate);
        List<TransactionEntry> splitTransactionCardExpenseList = splitTransactionRepository.findByUserIdAndTransactionDateFromCardSplits(userId, TransactionType.EXPENSE, startDate, endDate);
        List<TransactionEntry> splitTransactionDiscrepancyExpenseList = splitTransactionRepository.findByUserIdAndTransactionDateFromDiscrepancySplits(userId, TransactionType.EXPENSE, startDate, endDate);

        Map<Long, List<TransactionEntry>> incomeTotalListByAssetCategoryId = sortTransactionAndMakeAssetCategoryMap(bankTransactionIncomeList, cardTransactionIncomeList, discrepancyTransactionIncomeList, splitTransactionBankIncomeList, splitTransactionCardIncomeList, splitTransactionDiscrepancyIncomeList);
        Map<Long, List<TransactionEntry>> expenseTotalListByAssetCategoryId = sortTransactionAndMakeAssetCategoryMap(bankTransactionExpenseList, cardTransactionExpenseList, discrepancyTransactionExpenseList, splitTransactionBankExpenseList, splitTransactionCardExpenseList, splitTransactionDiscrepancyExpenseList);

        Map<Long, TransactionsByType> result = new HashMap<>();

        for (Map.Entry<Long, List<TransactionEntry>> entry : incomeTotalListByAssetCategoryId.entrySet()) {
            TransactionsByType transactionsByType = new TransactionsByType();
            transactionsByType.setIncome(entry.getValue());
            transactionsByType.setExpense(List.of());
            result.put(entry.getKey(), transactionsByType);
        }

        for (Map.Entry<Long, List<TransactionEntry>> entry : expenseTotalListByAssetCategoryId.entrySet()) {
            if (!result.containsKey(entry.getKey())) {
                TransactionsByType transactionsByType = new TransactionsByType();
                transactionsByType.setIncome(List.of());
                result.put(entry.getKey(), transactionsByType);
            } 
            result.get(entry.getKey()).setExpense(entry.getValue());
        }
        return result;
    }

    /**
     * 指定月の未分配取引（split_methodがnull）を収入・支出別に取得する。
     *
     * @param userId    ユーザーID
     * @param yearMonth 対象年月
     * @return 未分配の収入・支出取引エントリ一覧
     */
    public TransactionsByType getTransactionListByUserIdAndTransactionDateAndSplitMethodIsNull(Long userId, YearMonth yearMonth) {
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<TransactionEntry> bankTransactionIncomeList = bankTransactionRepository.findByUserIdAndTransactionDateAndSplitMethodIsNull(userId, TransactionType.INCOME, startDate, endDate);
        List<TransactionEntry> cardTransactionIncomeList = cardTransactionRepository.findByUserIdAndTransactionDateAndSplitMethodIsNull(userId, TransactionType.INCOME, startDate, endDate);
        List<TransactionEntry> discrepancyTransactionIncomeList = discrepancyTransactionRepository.findByUserIdAndTransactionDateAndSplitMethodIsNull(userId, TransactionType.INCOME, startDate, endDate);
        List<TransactionEntry> bankTransactionExpenseList = bankTransactionRepository.findByUserIdAndTransactionDateAndSplitMethodIsNull(userId, TransactionType.EXPENSE, startDate, endDate);
        List<TransactionEntry> cardTransactionExpenseList = cardTransactionRepository.findByUserIdAndTransactionDateAndSplitMethodIsNull(userId, TransactionType.EXPENSE, startDate, endDate);
        List<TransactionEntry> discrepancyTransactionExpenseList = discrepancyTransactionRepository.findByUserIdAndTransactionDateAndSplitMethodIsNull(userId, TransactionType.EXPENSE, startDate, endDate);

        TransactionsByType result = new TransactionsByType();
        result.setIncome(sortTransactionEntryList(bankTransactionIncomeList, cardTransactionIncomeList, discrepancyTransactionIncomeList));
        result.setExpense(sortTransactionEntryList(bankTransactionExpenseList, cardTransactionExpenseList, discrepancyTransactionExpenseList));

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

    private Map<Long, List<TransactionEntry>> sortTransactionAndMakeAssetCategoryMap(List<TransactionEntry> transactionEntryList1, List<TransactionEntry> transactionEntryList2, List<TransactionEntry> transactionEntryList3,
                    List<TransactionEntry> transactionEntryList4, List<TransactionEntry> transactionEntryList5, List<TransactionEntry> transactionEntryList6
        ) {
        List<TransactionEntry> transactionEntryList = Stream.of(transactionEntryList1, transactionEntryList2, transactionEntryList3, transactionEntryList4, transactionEntryList5, transactionEntryList6)
                .flatMap(List::stream)
                .sorted(
                    Comparator.comparing(TransactionEntry::getAmount)
                            .thenComparing(TransactionEntry::getTransactionDate)
                            .thenComparing(TransactionEntry::getDescription)
                            .reversed())
                .collect(Collectors.toList());
        Map<Long, List<TransactionEntry>> result = transactionEntryList.stream().collect(Collectors.groupingBy(TransactionEntry::getAssetCategoryId));
        return result;
    }

    /**
     * 銀行・カード取引をまとめた表示用DTO。
     *
     * @author kanta ito
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
