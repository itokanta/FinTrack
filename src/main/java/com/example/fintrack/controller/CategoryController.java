package com.example.fintrack.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.example.fintrack.security.AuthenticationUserIdResolver;
import com.example.fintrack.service.AssetCategoryService;
import com.example.fintrack.service.ExpenseCategoryService;

/**
 * カテゴリ管理コントローラー
 */
@Controller
@RequestMapping("/categories")
public class CategoryController {

    private static final Logger log = LoggerFactory.getLogger(CategoryController.class);

    private final AssetCategoryService assetCategoryService;
    private final ExpenseCategoryService expenseCategoryService;
    private final AuthenticationUserIdResolver authenticationUserIdResolver;
 
    public CategoryController(AssetCategoryService assetCategoryService, ExpenseCategoryService expenseCategoryService,
        AuthenticationUserIdResolver authenticationUserIdResolver) {
      this.assetCategoryService = assetCategoryService;
      this.expenseCategoryService = expenseCategoryService;
      this.authenticationUserIdResolver = authenticationUserIdResolver;
    }

    /**
     * カテゴリ管理画面
     */
    @GetMapping
    public String categories(Authentication authentication, Model model) {
        Long userId = authenticationUserIdResolver.getUserId(authentication);
        
        List<AssetCategory> assetCategories = assetCategoryService.findByUserId(userId);
        List<ExpenseCategory> expenseCategories = expenseCategoryService.findByUserId(userId);
        
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
            Long userId = authenticationUserIdResolver.getUserId(authentication);
            
            AssetCategory category = new AssetCategory();
            category.setUserId(userId);
            category.setCategoryName(categoryName.trim());
            
            assetCategoryService.insert(category);
            
            redirectAttributes.addFlashAttribute("message", "資産カテゴリを追加しました");
        } catch (Exception e) {
            log.error("資産カテゴリの追加に失敗しました", e);
            redirectAttributes.addFlashAttribute("error", "資産カテゴリの追加に失敗しました");
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
            Long userId = authenticationUserIdResolver.getUserId(authentication);
            ExpenseCategory category = new ExpenseCategory();
            category.setUserId(userId);
            category.setCategoryName(categoryName.trim());
            
            expenseCategoryService.save(category);
            
            redirectAttributes.addFlashAttribute("message", "支出カテゴリを追加しました");
        } catch (Exception e) {
            log.error("支出カテゴリの追加に失敗しました", e);
            redirectAttributes.addFlashAttribute("error", "支出カテゴリの追加に失敗しました");
        }
        
        return "redirect:/categories";
    }
    
    /**
     * 資産カテゴリ削除
     */
    @PostMapping("/asset/{id}/delete")
    public String deleteAssetCategory(@PathVariable Long id, RedirectAttributes redirectAttributes, Authentication authentication) {
        try {
            Long userId = authenticationUserIdResolver.getUserId(authentication);
            assetCategoryService.deleteById(id, userId);
            redirectAttributes.addFlashAttribute("message", "資産カテゴリを削除しました");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "残高がある資産カテゴリは削除できません");
        } catch (Exception e) {
            log.error("資産カテゴリの削除に失敗しました: id={}", id, e);
            redirectAttributes.addFlashAttribute("error", "資産カテゴリの削除に失敗しました");
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
        try {
            Long userId = authenticationUserIdResolver.getUserId(authentication);
            expenseCategoryService.deleteById(id, userId);
            redirectAttributes.addFlashAttribute("message", "支出カテゴリを削除しました");
        } catch (Exception e) {
            log.error("支出カテゴリの削除に失敗しました: id={}", id, e);
            redirectAttributes.addFlashAttribute("error", "支出カテゴリの削除に失敗しました");
        }
        
        return "redirect:/categories";
    }
}

