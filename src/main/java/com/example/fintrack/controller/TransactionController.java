package com.example.fintrack.controller;

import com.example.fintrack.domain.*;
import com.example.fintrack.repository.AssetCategoryRepository;
import com.example.fintrack.repository.ExpenseCategoryRepository;
import com.example.fintrack.repository.UserRepository;
import com.example.fintrack.service.IncomeSplitService;
import com.example.fintrack.service.TransactionService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 取引管理コントローラー
 */
@Controller
@RequestMapping("/transactions")
public class TransactionController {
    
    private final TransactionService transactionService;
    private final IncomeSplitService incomeSplitService;
    private final UserRepository userRepository;
    private final AssetCategoryRepository assetCategoryRepository;
    private final ExpenseCategoryRepository expenseCategoryRepository;
    
    public TransactionController(TransactionService transactionService,
                                IncomeSplitService incomeSplitService,
                                UserRepository userRepository,
                                AssetCategoryRepository assetCategoryRepository,
                                ExpenseCategoryRepository expenseCategoryRepository) {
        this.transactionService = transactionService;
        this.incomeSplitService = incomeSplitService;
        this.userRepository = userRepository;
        this.assetCategoryRepository = assetCategoryRepository;
        this.expenseCategoryRepository = expenseCategoryRepository;
    }
    
    /**
     * 支出一覧
     */
    @GetMapping("/expenses")
    public String expenses(Authentication authentication,
                          @RequestParam(required = false) String month,
                          @RequestParam(defaultValue = "0") int page,
                          Model model) {
        Long userId = getUserId(authentication);
        
        // 月の指定がない場合は今月
        YearMonth yearMonth;
        if (month != null) {
            yearMonth = YearMonth.parse(month);
        } else {
            yearMonth = YearMonth.now();
        }
        
        List<TransactionService.TransactionWithAsset> expenses = 
                transactionService.getExpensesByMonth(userId, yearMonth, page, 20);
        
        // カテゴリ情報を取得してMapに変換
        List<AssetCategory> assetCategories = assetCategoryRepository.findByUserId(userId);
        Map<Long, String> assetCategoryMap = assetCategories.stream()
                .collect(Collectors.toMap(AssetCategory::getId, AssetCategory::getCategoryName));
        
        List<ExpenseCategory> expenseCategories = expenseCategoryRepository.findByUserId(userId);
        Map<Long, String> expenseCategoryMap = expenseCategories.stream()
                .collect(Collectors.toMap(ExpenseCategory::getId, ExpenseCategory::getCategoryName));
        
        model.addAttribute("expenses", expenses);
        model.addAttribute("assetCategoryMap", assetCategoryMap);
        model.addAttribute("expenseCategoryMap", expenseCategoryMap);
        model.addAttribute("currentMonth", yearMonth);
        model.addAttribute("prevMonth", yearMonth.minusMonths(1));
        model.addAttribute("nextMonth", yearMonth.plusMonths(1));
        model.addAttribute("page", page);
        
        return "transactions/expenses";
    }
    
    /**
     * 収入一覧
     */
    @GetMapping("/incomes")
    public String incomes(Authentication authentication,
                         @RequestParam(required = false) String month,
                         @RequestParam(defaultValue = "0") int page,
                         Model model) {
        Long userId = getUserId(authentication);
        
        // 月の指定がない場合は今月
        YearMonth yearMonth;
        if (month != null) {
            yearMonth = YearMonth.parse(month);
        } else {
            yearMonth = YearMonth.now();
        }
        
        List<TransactionService.TransactionWithAsset> incomes = 
                transactionService.getIncomesByMonth(userId, yearMonth, page, 20);
        
        model.addAttribute("incomes", incomes);
        model.addAttribute("currentMonth", yearMonth);
        model.addAttribute("prevMonth", yearMonth.minusMonths(1));
        model.addAttribute("nextMonth", yearMonth.plusMonths(1));
        model.addAttribute("page", page);
        
        return "transactions/incomes";
    }
    
    /**
     * 支出詳細
     */
    @GetMapping("/expenses/{id}")
    public String expenseDetail(@PathVariable Long id,
                               @RequestParam String type,
                               Authentication authentication,
                               Model model) {
        Long userId = getUserId(authentication);
        
        if ("bank".equals(type)) {
            BankTransaction transaction = transactionService.getBankTransaction(id)
                    .orElseThrow(() -> new IllegalArgumentException("取引が見つかりません"));
            model.addAttribute("transaction", transaction);
            model.addAttribute("type", "bank");
        } else {
            CardTransaction transaction = transactionService.getCardTransaction(id)
                    .orElseThrow(() -> new IllegalArgumentException("取引が見つかりません"));
            model.addAttribute("transaction", transaction);
            model.addAttribute("type", "card");
        }
        
        // カテゴリ一覧を取得
        model.addAttribute("assetCategories", assetCategoryRepository.findByUserId(userId));
        model.addAttribute("expenseCategories", expenseCategoryRepository.findByUserId(userId));
        
        return "transactions/expense-detail";
    }
    
    /**
     * 収入詳細
     */
    @GetMapping("/incomes/{id}")
    public String incomeDetail(@PathVariable Long id,
                              @RequestParam String type,
                              Authentication authentication,
                              Model model) {
        Long userId = getUserId(authentication);
        
        if ("bank".equals(type)) {
            BankTransaction transaction = transactionService.getBankTransaction(id)
                    .orElseThrow(() -> new IllegalArgumentException("取引が見つかりません"));
            model.addAttribute("transaction", transaction);
            model.addAttribute("type", "bank");
        } else {
            CardTransaction transaction = transactionService.getCardTransaction(id)
                    .orElseThrow(() -> new IllegalArgumentException("取引が見つかりません"));
            model.addAttribute("transaction", transaction);
            model.addAttribute("type", "card");
        }
        
        // 資産カテゴリ一覧を取得
        List<AssetCategory> assetCategories = assetCategoryRepository.findByUserId(userId);
        model.addAttribute("assetCategories", assetCategories);
        
        // 資産カテゴリのマップを作成（ID -> 名前）
        Map<Long, String> assetCategoryMap = assetCategories.stream()
                .collect(Collectors.toMap(AssetCategory::getId, AssetCategory::getCategoryName));
        model.addAttribute("assetCategoryMap", assetCategoryMap);
        
        // 分割パターンサマリーを取得
        List<IncomeSplitService.PatternSummary> patternSummaries = incomeSplitService.getPatternSummaries(userId);
        model.addAttribute("patternSummaries", patternSummaries);
        
        // 分割済みの情報を取得
        List<SplitIncome> splitIncomes = incomeSplitService.getSplitIncomesByTransaction(
            "bank".equals(type) ? id : null,
            "card".equals(type) ? id : null
        );
        model.addAttribute("splitIncomes", splitIncomes);
        model.addAttribute("isSplit", !splitIncomes.isEmpty());
        
        return "transactions/income-detail";
    }
    
    /**
     * 支出カテゴリを更新
     */
    @PostMapping("/expenses/{id}/update-category")
    public String updateExpenseCategory(@PathVariable Long id,
                                       @RequestParam String type,
                                       @RequestParam(required = false) Long assetCategoryId,
                                       @RequestParam(required = false) Long expenseCategoryId,
                                       RedirectAttributes redirectAttributes) {
        try {
            Long bankTransactionId = "bank".equals(type) ? id : null;
            Long cardTransactionId = "card".equals(type) ? id : null;
            
            // 資産カテゴリを更新（残高も移動）
            if (assetCategoryId != null) {
                transactionService.updateExpenseAssetCategory(bankTransactionId, cardTransactionId, assetCategoryId);
            }
            
            // 支出カテゴリを更新
            if (expenseCategoryId != null) {
                transactionService.updateExpenseCategory(bankTransactionId, cardTransactionId, expenseCategoryId);
            }
            
            redirectAttributes.addFlashAttribute("message", "カテゴリを更新しました");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "カテゴリの更新に失敗しました: " + e.getMessage());
        }
        
        return "redirect:/transactions/expenses/" + id + "?type=" + type;
    }
    
    /**
     * 収入を手動分割する
     */
    @PostMapping("/incomes/{id}/split-manual")
    public String splitIncomeManual(@PathVariable Long id,
                                   @RequestParam String type,
                                   @RequestParam Map<String, String> params,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        try {
            Long userId = getUserId(authentication);
            Long bankTransactionId = "bank".equals(type) ? id : null;
            Long cardTransactionId = "card".equals(type) ? id : null;
            
            // パラメータから categoryAmount_ で始まるものを抽出
            Map<Long, Long> categoryAmounts = params.entrySet().stream()
                    .filter(e -> e.getKey().startsWith("categoryAmount_"))
                    .filter(e -> e.getValue() != null && !e.getValue().trim().isEmpty())
                    .collect(Collectors.toMap(
                        e -> Long.parseLong(e.getKey().substring("categoryAmount_".length())),
                        e -> Long.parseLong(e.getValue())
                    ));
            
            if (categoryAmounts.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "分割金額を入力してください");
                return "redirect:/transactions/incomes/" + id + "?type=" + type;
            }
            
            incomeSplitService.splitIncomeManual(bankTransactionId, cardTransactionId, categoryAmounts);
            redirectAttributes.addFlashAttribute("message", "収入を手動分割しました");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "分割に失敗しました: " + e.getMessage());
        }
        
        return "redirect:/transactions/incomes/" + id + "?type=" + type;
    }
    
    /**
     * ユーザーIDを取得
     */
    private Long getUserId(Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("ユーザーが見つかりません"))
                .getId();
    }
}

