package com.example.fintrack.controller;

import com.example.fintrack.domain.AssetCategory;
import com.example.fintrack.domain.SplitIncome;
import com.example.fintrack.repository.AssetCategoryRepository;
import com.example.fintrack.repository.UserRepository;
import com.example.fintrack.service.IncomeSplitService;
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
 * 収入分割管理コントローラー
 */
@Controller
@RequestMapping("/income-split")
public class IncomeSplitController {
    
    private final IncomeSplitService incomeSplitService;
    private final AssetCategoryRepository assetCategoryRepository;
    private final UserRepository userRepository;
    
    public IncomeSplitController(IncomeSplitService incomeSplitService,
                                AssetCategoryRepository assetCategoryRepository,
                                UserRepository userRepository) {
        this.incomeSplitService = incomeSplitService;
        this.assetCategoryRepository = assetCategoryRepository;
        this.userRepository = userRepository;
    }
    
    /**
     * 分割パターン一覧
     */
    @GetMapping("/patterns")
    public String patterns(Authentication authentication, Model model) {
        Long userId = getUserId(authentication);
        List<IncomeSplitService.PatternSummary> patternSummaries = incomeSplitService.getPatternSummaries(userId);
        
        model.addAttribute("patterns", patternSummaries);
        
        return "income-split/patterns";
    }
    
    /**
     * 分割パターン削除
     */
    @PostMapping("/patterns/{id}/delete")
    public String deletePattern(@PathVariable Long id,
                               RedirectAttributes redirectAttributes) {
        try {
            incomeSplitService.deletePattern(id);
            redirectAttributes.addFlashAttribute("message", "分割パターンを削除しました");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                    "分割パターンの削除に失敗しました: " + e.getMessage());
        }
        
        return "redirect:/income-split/patterns";
    }
    
    /**
     * 分割パターン登録画面
     */
    @GetMapping("/patterns/new")
    public String newPattern(Authentication authentication, Model model) {
        Long userId = getUserId(authentication);
        List<AssetCategory> categories = assetCategoryRepository.findByUserId(userId);
        
        model.addAttribute("categories", categories);
        
        return "income-split/pattern-form";
    }
    
    /**
     * 分割パターン登録
     */
    @PostMapping("/patterns")
    public String createPattern(Authentication authentication,
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
                    BigDecimal ratio = new BigDecimal(entry.getValue());
                    categoryRatios.put(categoryId, ratio);
                }
            }
            
            Long patternId = incomeSplitService.createSplitPattern(userId, patternName, categoryRatios);
            redirectAttributes.addFlashAttribute("message", 
                    "分割パターンを登録しました (ID: " + patternId + ")");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                    "分割パターンの登録に失敗しました: " + e.getMessage());
        }
        
        return "redirect:/income-split/patterns";
    }
    
    /**
     * 収入を分割
     */
    @PostMapping("/split")
    public String splitIncome(@RequestParam(required = false) Long bankTransactionId,
                             @RequestParam(required = false) Long cardTransactionId,
                             @RequestParam Long patternId,
                             RedirectAttributes redirectAttributes) {
        try {
            incomeSplitService.splitIncome(bankTransactionId, cardTransactionId, patternId);
            redirectAttributes.addFlashAttribute("message", "収入を分割しました");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                    "収入の分割に失敗しました: " + e.getMessage());
        }
        
        return "redirect:/transactions/incomes";
    }
    
    /**
     * 収入を全額1つのカテゴリに分配
     */
    @PostMapping("/split-single")
    public String splitIncomeSingle(@RequestParam(required = false) Long bankTransactionId,
                                   @RequestParam(required = false) Long cardTransactionId,
                                   @RequestParam Long categoryId,
                                   RedirectAttributes redirectAttributes) {
        try {
            incomeSplitService.splitIncomeSingle(bankTransactionId, cardTransactionId, categoryId);
            redirectAttributes.addFlashAttribute("message", "収入を分配しました");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                    "収入の分配に失敗しました: " + e.getMessage());
        }
        
        return "redirect:/transactions/incomes";
    }
    
    /**
     * 分割詳細
     */
    @GetMapping("/details")
    public String splitDetails(@RequestParam(required = false) Long bankTransactionId,
                              @RequestParam(required = false) Long cardTransactionId,
                              Model model) {
        List<SplitIncome> splits = incomeSplitService.getSplitIncomesByTransaction(
                bankTransactionId, cardTransactionId);
        
        model.addAttribute("splits", splits);
        
        return "income-split/details";
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

