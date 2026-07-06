package com.example.fintrack.controller;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.example.fintrack.security.AuthenticationUserIdResolver;
import com.example.fintrack.service.CsvImportService;


/**
 * 銀行・カード取引のCSVインポートを扱うコントローラー。
 *
 * @author kanta ito
 */
@Controller
@RequestMapping("/csv")
public class CsvImportController {

    private static final Logger log = LoggerFactory.getLogger(CsvImportController.class);

    private final CsvImportService csvImportService;
    private final AuthenticationUserIdResolver authenticationUserIdResolver;

    public CsvImportController(CsvImportService csvImportService,
        AuthenticationUserIdResolver authenticationUserIdResolver) {
      this.csvImportService = csvImportService;
      this.authenticationUserIdResolver = authenticationUserIdResolver;
    }

    /**
     * CSVインポート画面を表示する。
     *
     * @param model ビューモデル
     * @return CSVインポートテンプレート名
     */
    @GetMapping("/import")
    public String importForm(Model model) {
        model.addAttribute("bankTypes", BankType.values());
        return "csv/import";
    }
    
    /**
     * 銀行取引CSVをインポートする。
     *
     * @param file アップロードされたCSVファイル
     * @param bankType 金融機関種別
     * @param hasHeader ヘッダー行の有無
     * @param authentication 認証情報
     * @param redirectAttributes リダイレクト先へのフラッシュ属性
     * @return CSVインポート画面へのリダイレクト先
     */
    @PostMapping("/import/bank")
    public String importBank(@RequestParam("file") MultipartFile file,
                            @RequestParam("bankType") BankType bankType,
                            @RequestParam(value = "hasHeader", defaultValue = "false") boolean hasHeader,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        try {
            Long userId = authenticationUserIdResolver.getUserId(authentication);
            int count = csvImportService.importBankTransactions(file, bankType, userId, hasHeader);
            redirectAttributes.addFlashAttribute("message", 
                    count + "件の銀行取引をインポートしました");
        } catch (IOException e) {
            log.error("銀行取引CSVの読み込みに失敗しました", e);
            redirectAttributes.addFlashAttribute("error", "CSVファイルの読み込みに失敗しました");
        } catch (Exception e) {
            log.error("銀行取引CSVのインポートに失敗しました", e);
            redirectAttributes.addFlashAttribute("error", "インポート中にエラーが発生しました");
        }
        
        return "redirect:/csv/import";
    }
    
    /**
     * カード取引CSVをインポートする。
     *
     * @param file アップロードされたCSVファイル
     * @param bankType 金融機関種別
     * @param hasHeader ヘッダー行の有無
     * @param authentication 認証情報
     * @param redirectAttributes リダイレクト先へのフラッシュ属性
     * @return CSVインポート画面へのリダイレクト先
     */
    @PostMapping("/import/card")
    public String importCard(@RequestParam("file") MultipartFile file,
                            @RequestParam("bankType") BankType bankType,
                            @RequestParam(value = "hasHeader", defaultValue = "false") boolean hasHeader,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        try {
            Long userId = authenticationUserIdResolver.getUserId(authentication);
            int count = csvImportService.importCardTransactions(file, bankType, userId, hasHeader);
            redirectAttributes.addFlashAttribute("message", 
                    count + "件のカード取引をインポートしました");
        } catch (IOException e) {
            log.error("カード取引CSVの読み込みに失敗しました", e);
            redirectAttributes.addFlashAttribute("error", "CSVファイルの読み込みに失敗しました");
        } catch (Exception e) {
            log.error("カード取引CSVのインポートに失敗しました", e);
            redirectAttributes.addFlashAttribute("error", "インポート中にエラーが発生しました");
        }
        
        return "redirect:/csv/import";
    }
}

