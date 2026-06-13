package com.example.fintrack.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.fintrack.domain.AssetCategory;
import com.example.fintrack.domain.ExpenseCategory;
import com.example.fintrack.repository.ExpenseCategoryRepository;
import com.example.fintrack.repository.UserRepository;
import com.example.fintrack.service.AssetCategoryService;

/**
 * カテゴリ管理コントローラー
 */
@Controller
@RequestMapping("/categories")
public class CategoryController {
    
    private final AssetCategoryService assetCategoryService;
    private final ExpenseCategoryRepository expenseCategoryRepository;
    private final UserRepository userRepository;
    
    public CategoryController(AssetCategoryService assetCategoryService,
                             ExpenseCategoryRepository expenseCategoryRepository,
                             UserRepository userRepository) {
        this.assetCategoryService = assetCategoryService;
        this.expenseCategoryRepository = expenseCategoryRepository;
        this.userRepository = userRepository;
    }
    
    /**
     * カテゴリ管理画面
     */
    @GetMapping
    public String categories(Authentication authentication, Model model) {
        Long userId = getUserId(authentication);
        
        List<AssetCategory> assetCategories = assetCategoryService.findByUserId(userId);
        List<ExpenseCategory> expenseCategories = expenseCategoryRepository.findByUserId(userId);
        
        model.addAttribute("assetCategories", assetCategories);
        model.addAttribute("expenseCategories", expenseCategories);
        
        return "categories/index";
    }
    
    /**
     * 資産カテゴリ追加
     */
    @PostMapping("/asset")
    public String addAssetCategory(@RequestParam String categoryName,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        try {
            Long userId = getUserId(authentication);
            
            AssetCategory category = new AssetCategory();
            category.setUserId(userId);
            category.setCategoryName(categoryName.trim());
            
            assetCategoryService.insert(category);
            
            redirectAttributes.addFlashAttribute("message", "資産カテゴリを追加しました");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "資産カテゴリの追加に失敗しました: " + e.getMessage());
        }
        
        return "redirect:/categories";
    }
    
    /**
     * 支出カテゴリ追加
     */
    @PostMapping("/expense")
    public String addExpenseCategory(@RequestParam String categoryName,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        try {
            Long userId = getUserId(authentication);
            
            ExpenseCategory category = new ExpenseCategory();
            category.setUserId(userId);
            category.setCategoryName(categoryName.trim());
            
            expenseCategoryRepository.save(category);
            
            redirectAttributes.addFlashAttribute("message", "支出カテゴリを追加しました");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "支出カテゴリの追加に失敗しました: " + e.getMessage());
        }
        
        return "redirect:/categories";
    }
    
    /**
     * 資産カテゴリ削除
     */
    @PostMapping("/asset/{id}/delete")
    public String deleteAssetCategory(@PathVariable Long id, RedirectAttributes redirectAttributes, Authentication authentication) {
        Long userId = getUserId(authentication);
        try {
            assetCategoryService.deleteById(id, userId);
            redirectAttributes.addFlashAttribute("message", "資産カテゴリを削除しました");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "資産カテゴリの削除に失敗しました: " + e.getMessage());
        }
        
        return "redirect:/categories";
    }
    
    /**
     * 支出カテゴリ削除
     */
    @PostMapping("/expense/{id}/delete")
    public String deleteExpenseCategory(@PathVariable Long id,
                                       RedirectAttributes redirectAttributes,
                                       Authentication authentication) {
        Long userId = getUserId(authentication);
        try {
            expenseCategoryRepository.deleteById(id, userId);
            redirectAttributes.addFlashAttribute("message", "支出カテゴリを削除しました");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "支出カテゴリの削除に失敗しました: " + e.getMessage());
        }
        
        return "redirect:/categories";
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

