package com.example.fintrack.controller;

import com.example.fintrack.domain.AssetCategory;
import com.example.fintrack.repository.AssetCategoryRepository;
import com.example.fintrack.repository.UserRepository;
import com.example.fintrack.service.AssetService;
import com.example.fintrack.service.IncomeSplitService;
import com.example.fintrack.service.TransactionService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

/**
 * 資産管理コントローラー
 */
@Controller
@RequestMapping("/assets")
public class AssetController {
    
    private final AssetService assetService;
    private final UserRepository userRepository;
    private final TransactionService transactionService;
    private final AssetCategoryRepository assetCategoryRepository;
    private final IncomeSplitService incomeSplitService;
    
    public AssetController(AssetService assetService,
                          UserRepository userRepository,
                          TransactionService transactionService,
                          AssetCategoryRepository assetCategoryRepository,
                          IncomeSplitService incomeSplitService) {
        this.assetService = assetService;
        this.userRepository = userRepository;
        this.transactionService = transactionService;
        this.assetCategoryRepository = assetCategoryRepository;
        this.incomeSplitService = incomeSplitService;
    }
    
    /**
     * 資産一覧画面
     */
    @GetMapping
    public String assets(Authentication authentication, Model model) {
        Long userId = getUserId(authentication);
        
        // カテゴリ別残高を取得
        Map<AssetCategory, Long> balances = assetService.getBalanceByCategory(userId);
        
        // 総資産を取得
        Long totalBalance = assetService.getTotalBalance(userId);
        
        // カテゴリ未設定の残高を取得
        Long uncategorizedBalance = assetService.getUncategorizedBalance(userId);
        
        model.addAttribute("balances", balances);
        model.addAttribute("totalBalance", totalBalance);
        model.addAttribute("uncategorizedBalance", uncategorizedBalance);
        
        return "assets/index";
    }
    
    /**
     * 確定済み資産画面（当月と前月のクレジットカード取引を除外）
     */
    @GetMapping("/confirmed")
    public String confirmed(Authentication authentication, Model model) {
        Long userId = getUserId(authentication);
        
        // 通常の残高を取得
        Map<AssetCategory, Long> balances = assetService.getBalanceByCategory(userId);
        Long totalBalance = assetService.getTotalBalance(userId);
        Long uncategorizedBalance = assetService.getUncategorizedBalance(userId);
        
        // 確定済み残高を取得（当月と前月のクレジットカード取引を除外）
        Map<AssetCategory, Long> confirmedBalances = assetService.getConfirmedBalanceByCategory(userId);
        Long confirmedTotalBalance = assetService.getConfirmedTotalBalance(userId);
        Long confirmedUncategorizedBalance = assetService.getConfirmedUncategorizedBalance(userId);
        
        // 当月と前月のクレジットカード取引合計を計算
        Long currentMonthCreditTotal = totalBalance - confirmedTotalBalance;
        
        model.addAttribute("balances", balances);
        model.addAttribute("totalBalance", totalBalance);
        model.addAttribute("uncategorizedBalance", uncategorizedBalance);
        model.addAttribute("confirmedBalances", confirmedBalances);
        model.addAttribute("confirmedTotalBalance", confirmedTotalBalance);
        model.addAttribute("confirmedUncategorizedBalance", confirmedUncategorizedBalance);
        model.addAttribute("currentMonthCreditTotal", currentMonthCreditTotal);
        
        return "assets/confirmed";
    }
    
    /**
     * 未分類の取引一覧画面
     */
    @GetMapping("/uncategorized")
    public String uncategorized(@RequestParam(defaultValue = "expense") String type,
                               Authentication authentication, 
                               Model model) {
        Long userId = getUserId(authentication);
        
        List<TransactionService.TransactionWithAsset> transactions;
        
        if ("income".equals(type)) {
            // 未分類の収入取引を取得
            transactions = transactionService.getUncategorizedIncomes(userId);
            
            // 資産カテゴリ一覧を取得
            List<AssetCategory> assetCategories = assetCategoryRepository.findByUserId(userId);
            model.addAttribute("assetCategories", assetCategories);
            
            // 分割パターンサマリーを取得
            List<IncomeSplitService.PatternSummary> patternSummaries = incomeSplitService.getPatternSummaries(userId);
            model.addAttribute("patternSummaries", patternSummaries);
        } else {
            // 未分類の支出取引を取得
            transactions = transactionService.getUncategorizedExpenses(userId);
            
            // 資産カテゴリ一覧を取得
            List<AssetCategory> assetCategories = assetCategoryRepository.findByUserId(userId);
            model.addAttribute("assetCategories", assetCategories);
        }
        
        model.addAttribute("transactions", transactions);
        model.addAttribute("type", type);
        
        return "assets/uncategorized";
    }
    
    /**
     * 未分類の支出を一括でカテゴリ設定
     */
    @PostMapping("/uncategorized/bulk-update-expense")
    public String bulkUpdateExpense(@RequestParam Long assetCategoryId,
                                   @RequestParam(required = false) List<String> transactionIds,
                                   RedirectAttributes redirectAttributes) {
        if (transactionIds == null || transactionIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "取引を選択してください");
            return "redirect:/assets/uncategorized?type=expense";
        }
        
        try {
            int count = 0;
            for (String transactionId : transactionIds) {
                if (transactionId.startsWith("bank_")) {
                    Long bankId = Long.parseLong(transactionId.substring(5));
                    transactionService.updateExpenseAssetCategory(bankId, null, assetCategoryId);
                    count++;
                } else if (transactionId.startsWith("card_")) {
                    Long cardId = Long.parseLong(transactionId.substring(5));
                    transactionService.updateExpenseAssetCategory(null, cardId, assetCategoryId);
                    count++;
                }
            }
            
            redirectAttributes.addFlashAttribute("message", 
                    count + "件の支出のカテゴリを設定しました");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                    "カテゴリの設定に失敗しました: " + e.getMessage());
        }
        
        return "redirect:/assets/uncategorized?type=expense";
    }
    
    /**
     * 未分類の収入を単一カテゴリに一括分配
     */
    @PostMapping("/uncategorized/bulk-split-single")
    public String bulkSplitSingle(@RequestParam Long categoryId,
                                  @RequestParam(required = false) List<String> transactionIds,
                                  RedirectAttributes redirectAttributes) {
        if (transactionIds == null || transactionIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "取引を選択してください");
            return "redirect:/assets/uncategorized?type=income";
        }
        
        try {
            int count = 0;
            for (String transactionId : transactionIds) {
                if (transactionId.startsWith("bank_")) {
                    Long bankId = Long.parseLong(transactionId.substring(5));
                    incomeSplitService.splitIncomeSingle(bankId, null, categoryId);
                    count++;
                } else if (transactionId.startsWith("card_")) {
                    Long cardId = Long.parseLong(transactionId.substring(5));
                    incomeSplitService.splitIncomeSingle(null, cardId, categoryId);
                    count++;
                }
            }
            
            redirectAttributes.addFlashAttribute("message", 
                    count + "件の収入を1つのカテゴリに分配しました");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                    "収入の分配に失敗しました: " + e.getMessage());
        }
        
        return "redirect:/assets/uncategorized?type=income";
    }
    
    /**
     * 未分類の収入を分割パターンで一括分配
     */
    @PostMapping("/uncategorized/bulk-split-income")
    public String bulkSplitIncome(@RequestParam Long patternId,
                                  @RequestParam(required = false) List<String> transactionIds,
                                  RedirectAttributes redirectAttributes) {
        if (transactionIds == null || transactionIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "取引を選択してください");
            return "redirect:/assets/uncategorized?type=income";
        }
        
        try {
            int count = 0;
            for (String transactionId : transactionIds) {
                if (transactionId.startsWith("bank_")) {
                    Long bankId = Long.parseLong(transactionId.substring(5));
                    incomeSplitService.splitIncome(bankId, null, patternId);
                    count++;
                } else if (transactionId.startsWith("card_")) {
                    Long cardId = Long.parseLong(transactionId.substring(5));
                    incomeSplitService.splitIncome(null, cardId, patternId);
                    count++;
                }
            }
            
            redirectAttributes.addFlashAttribute("message", 
                    count + "件の収入を分割パターンで分配しました");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                    "収入の分配に失敗しました: " + e.getMessage());
        }
        
        return "redirect:/assets/uncategorized?type=income";
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
