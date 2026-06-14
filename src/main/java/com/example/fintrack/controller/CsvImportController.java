package com.example.fintrack.controller;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.fintrack.csv.BankType;
import com.example.fintrack.service.CsvImportService;
import com.example.fintrack.service.UserService;


/**
 * CSV取込コントローラー
 */
@Controller
@RequestMapping("/csv")
public class CsvImportController {
    
    private final CsvImportService csvImportService;
    private final UserService userService;

    public CsvImportController(CsvImportService csvImportService, UserService userService) {
      this.csvImportService = csvImportService;
      this.userService = userService;
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
        return userService.findUserIdByUsername(username);
    }
}

