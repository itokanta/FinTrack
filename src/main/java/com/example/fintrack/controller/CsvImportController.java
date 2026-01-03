package com.example.fintrack.controller;

import com.example.fintrack.csv.BankType;
import com.example.fintrack.repository.UserRepository;
import com.example.fintrack.service.CsvImportService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

/**
 * CSV取込コントローラー
 */
@Controller
@RequestMapping("/csv")
public class CsvImportController {
    
    private final CsvImportService csvImportService;
    private final UserRepository userRepository;
    
    public CsvImportController(CsvImportService csvImportService,
                              UserRepository userRepository) {
        this.csvImportService = csvImportService;
        this.userRepository = userRepository;
    }
    
    /**
     * CSV取込画面
     */
    @GetMapping("/import")
    public String importForm(Model model) {
        model.addAttribute("bankTypes", BankType.values());
        return "csv/import";
    }
    
    /**
     * 銀行取引CSV取込
     */
    @PostMapping("/import/bank")
    public String importBank(@RequestParam("file") MultipartFile file,
                            @RequestParam("bankType") BankType bankType,
                            @RequestParam(value = "hasHeader", defaultValue = "false") boolean hasHeader,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        try {
            Long userId = getUserId(authentication);
            int count = csvImportService.importBankTransactions(file, bankType, userId, hasHeader);
            redirectAttributes.addFlashAttribute("message", 
                    count + "件の銀行取引をインポートしました");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", 
                    "CSVファイルの読み込みに失敗しました: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                    "インポート中にエラーが発生しました: " + e.getMessage());
        }
        
        return "redirect:/csv/import";
    }
    
    /**
     * カード取引CSV取込
     */
    @PostMapping("/import/card")
    public String importCard(@RequestParam("file") MultipartFile file,
                            @RequestParam("bankType") BankType bankType,
                            @RequestParam(value = "hasHeader", defaultValue = "false") boolean hasHeader,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        try {
            Long userId = getUserId(authentication);
            int count = csvImportService.importCardTransactions(file, bankType, userId, hasHeader);
            redirectAttributes.addFlashAttribute("message", 
                    count + "件のカード取引をインポートしました");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", 
                    "CSVファイルの読み込みに失敗しました: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                    "インポート中にエラーが発生しました: " + e.getMessage());
        }
        
        return "redirect:/csv/import";
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

