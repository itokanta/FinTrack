package com.example.fintrack.controller;

import com.example.fintrack.domain.AssetCategory;
import com.example.fintrack.repository.UserRepository;
import com.example.fintrack.service.AssetService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 資産管理コントローラー
 */
@Controller
@RequestMapping("/assets")
public class AssetController {
    
    private final AssetService assetService;
    private final UserRepository userRepository;
    
    public AssetController(AssetService assetService,
                          UserRepository userRepository) {
        this.assetService = assetService;
        this.userRepository = userRepository;
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
        Long totalBalance = assetService.getTotalBalance();
        
        // カテゴリ未設定の残高を取得
        Long uncategorizedBalance = assetService.getUncategorizedBalance();
        
        model.addAttribute("balances", balances);
        model.addAttribute("totalBalance", totalBalance);
        model.addAttribute("uncategorizedBalance", uncategorizedBalance);
        
        return "assets/index";
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
