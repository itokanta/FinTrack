package com.example.fintrack.controller;

import com.example.fintrack.domain.AssetCategory;
import com.example.fintrack.domain.AutoCategoryRule;
import com.example.fintrack.domain.TransactionType;
import com.example.fintrack.repository.AssetCategoryRepository;
import com.example.fintrack.repository.AutoCategoryRuleRepository;
import com.example.fintrack.repository.BankTransactionRepository;
import com.example.fintrack.repository.CardTransactionRepository;
import com.example.fintrack.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 自動カテゴリルール管理コントローラー
 */
@Controller
@RequestMapping("/auto-category-rules")
public class AutoCategoryRuleController {
    
    private final AutoCategoryRuleRepository autoCategoryRuleRepository;
    private final AssetCategoryRepository assetCategoryRepository;
    private final UserRepository userRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final CardTransactionRepository cardTransactionRepository;
    
    public AutoCategoryRuleController(AutoCategoryRuleRepository autoCategoryRuleRepository,
                                     AssetCategoryRepository assetCategoryRepository,
                                     UserRepository userRepository,
                                     BankTransactionRepository bankTransactionRepository,
                                     CardTransactionRepository cardTransactionRepository) {
        this.autoCategoryRuleRepository = autoCategoryRuleRepository;
        this.assetCategoryRepository = assetCategoryRepository;
        this.userRepository = userRepository;
        this.bankTransactionRepository = bankTransactionRepository;
        this.cardTransactionRepository = cardTransactionRepository;
    }
    
    /**
     * 自動カテゴリルール一覧
     */
    @GetMapping
    public String index(Authentication authentication, Model model) {
        Long userId = getUserId(authentication);
        
        // ルール一覧を取得
        List<AutoCategoryRule> rules = autoCategoryRuleRepository.findByUserId(userId);
        
        // 資産カテゴリのマップを作成
        List<AssetCategory> assetCategories = assetCategoryRepository.findByUserId(userId);
        Map<Long, String> assetCategoryMap = new HashMap<>();
        for (AssetCategory category : assetCategories) {
            assetCategoryMap.put(category.getId(), category.getCategoryName());
        }
        
        model.addAttribute("rules", rules);
        model.addAttribute("assetCategoryMap", assetCategoryMap);
        
        return "auto-category-rules/index";
    }
    
    /**
     * 新規ルール作成画面
     */
    @GetMapping("/new")
    public String newRule(Authentication authentication, Model model) {
        Long userId = getUserId(authentication);
        
        // 資産カテゴリ一覧を取得
        List<AssetCategory> assetCategories = assetCategoryRepository.findByUserId(userId);
        model.addAttribute("rule", new AutoCategoryRule());
        model.addAttribute("assetCategories", assetCategories);
        model.addAttribute("transactionTypes", TransactionType.values());
        
        return "auto-category-rules/form";
    }
    
    /**
     * ルール編集画面
     */
    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id,
                      Authentication authentication,
                      Model model,
                      RedirectAttributes redirectAttributes) {
        Long userId = getUserId(authentication);
        
        // ルールを取得
        AutoCategoryRule rule = autoCategoryRuleRepository.findById(id)
                .orElse(null);
        
        if (rule == null || !rule.getUserId().equals(userId)) {
            redirectAttributes.addFlashAttribute("error", "ルールが見つかりません");
            return "redirect:/auto-category-rules";
        }
        
        // 資産カテゴリ一覧を取得
        List<AssetCategory> assetCategories = assetCategoryRepository.findByUserId(userId);
        
        model.addAttribute("rule", rule);
        model.addAttribute("assetCategories", assetCategories);
        model.addAttribute("transactionTypes", TransactionType.values());
        
        return "auto-category-rules/form";
    }
    
    /**
     * ルール保存
     */
    @PostMapping
    public String save(@RequestParam(required = false) Long id,
                      @RequestParam Long assetCategoryId,
                      @RequestParam String descriptionPattern,
                      @RequestParam TransactionType transactionType,
                      Authentication authentication,
                      RedirectAttributes redirectAttributes) {
        Long userId = getUserId(authentication);
        
        try {
            AutoCategoryRule rule;
            
            if (id != null) {
                // 更新
                rule = autoCategoryRuleRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("ルールが見つかりません"));
                
                if (!rule.getUserId().equals(userId)) {
                    throw new IllegalArgumentException("権限がありません");
                }
            } else {
                // 新規作成
                rule = new AutoCategoryRule();
                rule.setUserId(userId);
            }
            
            rule.setAssetCategoryId(assetCategoryId);
            rule.setDescriptionPattern(descriptionPattern);
            rule.setTransactionType(transactionType);
            
            autoCategoryRuleRepository.save(rule);
            
            redirectAttributes.addFlashAttribute("message", 
                    id != null ? "ルールを更新しました" : "ルールを作成しました");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                    "ルールの保存に失敗しました: " + e.getMessage());
        }
        
        return "redirect:/auto-category-rules";
    }
    
    /**
     * ルール削除
     */
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                        Authentication authentication,
                        RedirectAttributes redirectAttributes) {
        Long userId = getUserId(authentication);
        
        try {
            AutoCategoryRule rule = autoCategoryRuleRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("ルールが見つかりません"));
            
            if (!rule.getUserId().equals(userId)) {
                throw new IllegalArgumentException("権限がありません");
            }
            
            autoCategoryRuleRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("message", "ルールを削除しました");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                    "ルールの削除に失敗しました: " + e.getMessage());
        }
        
        return "redirect:/auto-category-rules";
    }
    
    /**
     * 摘要検索（過去の取引履歴から）
     */
    @GetMapping("/search-descriptions")
    @ResponseBody
    public List<DescriptionSuggestion> searchDescriptions(@RequestParam String query,
                                                          Authentication authentication) {
        Long userId = getUserId(authentication);
        
        // 銀行取引とカード取引から摘要を取得
        Set<DescriptionSuggestion> suggestions = new LinkedHashSet<>();
        
        // 銀行取引から取得
        List<String> bankDescriptions = bankTransactionRepository.findDistinctDescriptionsByUserId(userId);
        for (String desc : bankDescriptions) {
            if (desc.toLowerCase().contains(query.toLowerCase())) {
                suggestions.add(new DescriptionSuggestion(desc, "銀行"));
            }
        }
        
        // カード取引から取得
        List<String> cardDescriptions = cardTransactionRepository.findDistinctDescriptionsByUserId(userId);
        for (String desc : cardDescriptions) {
            if (desc.toLowerCase().contains(query.toLowerCase())) {
                suggestions.add(new DescriptionSuggestion(desc, "カード"));
            }
        }
        
        return suggestions.stream()
                .limit(20)
                .collect(Collectors.toList());
    }
    
    /**
     * 摘要候補のDTO
     */
    public static class DescriptionSuggestion {
        private String description;
        private String source;
        
        public DescriptionSuggestion(String description, String source) {
            this.description = description;
            this.source = source;
        }
        
        public String getDescription() {
            return description;
        }
        
        public String getSource() {
            return source;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DescriptionSuggestion that = (DescriptionSuggestion) o;
            return Objects.equals(description, that.description);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(description);
        }
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

