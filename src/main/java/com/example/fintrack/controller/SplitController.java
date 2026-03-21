package com.example.fintrack.controller;

import com.example.fintrack.domain.*;
import com.example.fintrack.repository.AssetCategoryRepository;
import com.example.fintrack.repository.UserRepository;
import com.example.fintrack.service.SplitService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 収支分割管理コントローラー
 */
@Controller
@RequestMapping("/split")
public class SplitController {
    
    private final SplitService splitService;
    private final AssetCategoryRepository assetCategoryRepository;
    private final UserRepository userRepository;
    
    public SplitController(SplitService splitService,
                          AssetCategoryRepository assetCategoryRepository,
                          UserRepository userRepository) {
        this.splitService = splitService;
        this.assetCategoryRepository = assetCategoryRepository;
        this.userRepository = userRepository;
    }
    
    /**
     * 分割パターン一覧画面（収入）
     */
    @GetMapping("/income/patterns")
    public String incomePatterns(Authentication authentication, Model model) {
        return patterns(authentication, model, "INCOME");
    }
    
    /**
     * 分割パターン一覧画面（支出）
     */
    @GetMapping("/expense/patterns")
    public String expensePatterns(Authentication authentication, Model model) {
        return patterns(authentication, model, "EXPENSE");
    }
    
    /**
     * 分割パターン一覧画面（共通処理）
     */
    private String patterns(Authentication authentication, Model model, String transactionType) {
        Long userId = getUserId(authentication);
        List<SplitService.PatternSummary> patternSummaries = 
            splitService.getPatternSummaries(userId, transactionType);
        
        model.addAttribute("patternSummaries", patternSummaries);
        model.addAttribute("transactionType", transactionType);
        
        return "split/patterns";
    }
    
    /**
     * 分割パターン登録画面（収入）
     */
    @GetMapping("/income/patterns/new")
    public String newIncomePattern(Authentication authentication, Model model) {
        Long userId = getUserId(authentication);
        List<AssetCategory> categories = assetCategoryRepository.findByUserId(userId);
        
        model.addAttribute("categories", categories);
        model.addAttribute("transactionType", "INCOME");
        
        return "split/pattern-form";
    }
    
    /**
     * 分割パターン登録画面（支出）
     */
    @GetMapping("/expense/patterns/new")
    public String newExpensePattern(Authentication authentication, Model model) {
        Long userId = getUserId(authentication);
        List<AssetCategory> categories = assetCategoryRepository.findByUserId(userId);
        
        model.addAttribute("categories", categories);
        model.addAttribute("transactionType", "EXPENSE");
        
        return "split/pattern-form";
    }
    
    /**
     * 分割パターン登録（割合指定）
     */
    @PostMapping("/patterns/ratio")
    public String createPatternByRatio(Authentication authentication,
                                      @RequestParam String transactionType,
                                      @RequestParam String patternName,
                                      @RequestParam Map<String, String> params,
                                      RedirectAttributes redirectAttributes) {
        try {
            Long userId = getUserId(authentication);
            
            // カテゴリIDと割合のマップを作成
            Map<Long, BigDecimal> categoryRatios = new HashMap<>();
            
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (entry.getKey().startsWith("ratio_")) {
                    Long categoryId = Long.parseLong(entry.getKey().substring(6));
                    String ratioStr = entry.getValue();
                    
                    // 空文字列やnullの場合はスキップ
                    if (ratioStr != null && !ratioStr.trim().isEmpty()) {
                        BigDecimal ratio = new BigDecimal(ratioStr);
                        if (ratio.compareTo(BigDecimal.ZERO) > 0) {
                            categoryRatios.put(categoryId, ratio);
                        }
                    }
                }
            }
            
            Long patternId = splitService.createSplitPatternByRatio(userId, transactionType, patternName, categoryRatios);
            redirectAttributes.addFlashAttribute("message", 
                    "分割パターンを登録しました (ID: " + patternId + ")");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                    "分割パターンの登録に失敗しました: " + e.getMessage());
        }
        
        String redirectUrl = "INCOME".equals(transactionType) 
            ? "redirect:/split/income/patterns" 
            : "redirect:/split/expense/patterns";
        return redirectUrl;
    }
    
    /**
     * 分割パターン登録（金額指定）
     */
    @PostMapping("/patterns/amount")
    public String createPatternByAmount(Authentication authentication,
                                       @RequestParam String transactionType,
                                       @RequestParam String patternName,
                                       @RequestParam Map<String, String> params,
                                       RedirectAttributes redirectAttributes) {
        try {
            Long userId = getUserId(authentication);
            
            // カテゴリIDと金額のマップを作成
            Map<Long, Long> categoryAmounts = new HashMap<>();
            
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (entry.getKey().startsWith("amount_")) {
                    Long categoryId = Long.parseLong(entry.getKey().substring(7));
                    String amountStr = entry.getValue();
                    
                    // 空文字列やnullの場合はスキップ
                    if (amountStr != null && !amountStr.trim().isEmpty()) {
                        Long amount = Long.parseLong(amountStr);
                        if (amount > 0) {
                            categoryAmounts.put(categoryId, amount);
                        }
                    }
                }
            }
            
            Long patternId = splitService.createSplitPatternByAmount(userId, transactionType, patternName, categoryAmounts);
            redirectAttributes.addFlashAttribute("message", 
                    "分割パターンを登録しました (ID: " + patternId + ")");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                    "分割パターンの登録に失敗しました: " + e.getMessage());
        }
        
        String redirectUrl = "INCOME".equals(transactionType) 
            ? "redirect:/split/income/patterns" 
            : "redirect:/split/expense/patterns";
        return redirectUrl;
    }
    
    /**
     * 分割パターン削除
     */
    @PostMapping("/patterns/{id}/delete")
    public String deletePattern(@PathVariable Long id,
                               @RequestParam String transactionType,
                               RedirectAttributes redirectAttributes) {
        try {
            splitService.deletePattern(id);
            redirectAttributes.addFlashAttribute("message", "分割パターンを削除しました");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                    "分割パターンの削除に失敗しました: " + e.getMessage());
        }
        
        String redirectUrl = "INCOME".equals(transactionType) 
            ? "redirect:/split/income/patterns" 
            : "redirect:/split/expense/patterns";
        return redirectUrl;
    }
    
    /**
     * 収入をパターンで分割
     */
    @PostMapping("/income/split")
    public String splitIncome(@RequestParam(required = false) Long bankTransactionId,
                             @RequestParam(required = false) Long cardTransactionId,
                             @RequestParam Long patternId,
                             RedirectAttributes redirectAttributes) {
        try {
            splitService.splitIncome(bankTransactionId, cardTransactionId, patternId);
            redirectAttributes.addFlashAttribute("message", "収入を分配しました");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                    "収入の分配に失敗しました: " + e.getMessage());
        }
        
        String type = bankTransactionId != null ? "bank" : "card";
        Long id = bankTransactionId != null ? bankTransactionId : cardTransactionId;
        return "redirect:/transactions/incomes/" + id + "?type=" + type;
    }
    
    /**
     * 支出をパターンで分割
     */
    @PostMapping("/expense/split")
    public String splitExpense(@RequestParam(required = false) Long bankTransactionId,
                              @RequestParam(required = false) Long cardTransactionId,
                              @RequestParam Long patternId,
                              RedirectAttributes redirectAttributes) {
        try {
            splitService.splitExpense(bankTransactionId, cardTransactionId, patternId);
            redirectAttributes.addFlashAttribute("message", "支出を分配しました");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                    "支出の分配に失敗しました: " + e.getMessage());
        }
        
        String type = bankTransactionId != null ? "bank" : "card";
        Long id = bankTransactionId != null ? bankTransactionId : cardTransactionId;
        return "redirect:/transactions/expenses/" + id + "?type=" + type;
    }
    
    /**
     * 支出を単一カテゴリに分配
     */
    @PostMapping("/expense/split-single")
    public String splitExpenseSingle(@RequestParam(required = false) Long bankTransactionId,
                                    @RequestParam(required = false) Long cardTransactionId,
                                    @RequestParam Long categoryId,
                                    RedirectAttributes redirectAttributes) {
        try {
            splitService.splitExpenseSingle(bankTransactionId, cardTransactionId, categoryId);
            redirectAttributes.addFlashAttribute("message", "支出を分配しました");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                    "支出の分配に失敗しました: " + e.getMessage());
        }
        
        String type = bankTransactionId != null ? "bank" : "card";
        Long id = bankTransactionId != null ? bankTransactionId : cardTransactionId;
        return "redirect:/transactions/expenses/" + id + "?type=" + type;
    }
    
    /**
     * 収入を手動で分割
     */
    @PostMapping("/income/split-manual")
    public String splitIncomeManual(@RequestParam(required = false) Long bankTransactionId,
                                   @RequestParam(required = false) Long cardTransactionId,
                                   @RequestParam Map<String, String> params,
                                   RedirectAttributes redirectAttributes) {
        try {
            // カテゴリIDと金額のマップを作成
            Map<Long, Long> categoryAmounts = new HashMap<>();
            
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (entry.getKey().startsWith("category_")) {
                    Long categoryId = Long.parseLong(entry.getKey().substring(9));
                    String amountStr = entry.getValue();
                    
                    // 空文字列やnullの場合はスキップ
                    if (amountStr != null && !amountStr.trim().isEmpty()) {
                        Long amount = Long.parseLong(amountStr);
                        if (amount > 0) {
                            categoryAmounts.put(categoryId, amount);
                        }
                    }
                }
            }
            
            splitService.splitIncomeManual(bankTransactionId, cardTransactionId, categoryAmounts);
            redirectAttributes.addFlashAttribute("message", "収入を手動分配しました");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                    "収入の手動分配に失敗しました: " + e.getMessage());
        }
        
        String type = bankTransactionId != null ? "bank" : "card";
        Long id = bankTransactionId != null ? bankTransactionId : cardTransactionId;
        return "redirect:/transactions/incomes/" + id + "?type=" + type;
    }
    
    /**
     * 支出を手動で分割
     */
    @PostMapping("/expense/split-manual")
    public String splitExpenseManual(@RequestParam(required = false) Long bankTransactionId,
                                    @RequestParam(required = false) Long cardTransactionId,
                                    @RequestParam Map<String, String> params,
                                    RedirectAttributes redirectAttributes) {
        try {
            // カテゴリIDと金額のマップを作成
            Map<Long, Long> categoryAmounts = new HashMap<>();
            
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (entry.getKey().startsWith("category_")) {
                    Long categoryId = Long.parseLong(entry.getKey().substring(9));
                    String amountStr = entry.getValue();
                    
                    // 空文字列やnullの場合はスキップ
                    if (amountStr != null && !amountStr.trim().isEmpty()) {
                        Long amount = Long.parseLong(amountStr);
                        if (amount > 0) {
                            categoryAmounts.put(categoryId, amount);
                        }
                    }
                }
            }
            
            splitService.splitExpenseManual(bankTransactionId, cardTransactionId, categoryAmounts);
            redirectAttributes.addFlashAttribute("message", "支出を手動分配しました");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                    "支出の手動分配に失敗しました: " + e.getMessage());
        }
        
        String type = bankTransactionId != null ? "bank" : "card";
        Long id = bankTransactionId != null ? bankTransactionId : cardTransactionId;
        return "redirect:/transactions/expenses/" + id + "?type=" + type;
    }
    
    private Long getUserId(Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("ユーザーが見つかりません"))
                .getId();
    }
}

