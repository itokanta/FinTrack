package com.example.fintrack.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.fintrack.domain.BankTransaction;
import com.example.fintrack.domain.CardTransaction;
import com.example.fintrack.domain.CsvImportHistory;
import com.example.fintrack.domain.dto.CardTransactionUpdateDetail;
import com.example.fintrack.service.CsvImportHistoryService;
import com.example.fintrack.service.UserService;
import org.springframework.web.bind.annotation.PostMapping;


@Controller
@RequestMapping("/csv")
public class CsvImportHistoryController {

  private final UserService userService;
  private final CsvImportHistoryService csvImportHistoryService;
  
  public CsvImportHistoryController(UserService userService, CsvImportHistoryService csvImportHistoryService) {
    this.userService = userService;
    this.csvImportHistoryService = csvImportHistoryService;
  }

  @GetMapping("/import/history")
  public String getMethodName(Authentication authentication, Model model) {
    Long userId = getUserId(authentication);
    List<CsvImportHistory> histories = csvImportHistoryService.findAllByUserId(userId);
    model.addAttribute("histories", histories);
    return "csv/import-history";
  }

  @GetMapping("/import/history/{id}")
  public String getMethodName(@PathVariable Long csvImportId, Authentication authentication, Model model) {
    Long userId = getUserId(authentication);

    CsvImportHistory history = csvImportHistoryService.findById(csvImportId, userId);
    model.addAttribute("history", history);

    if(history.getImportType().equals("BANK")){
      List<BankTransaction> bankTransactionList = csvImportHistoryService.getBankTransactionHistory(userId, csvImportId);
      model.addAttribute("bankTransactionList", bankTransactionList);
    } else if (history.getImportType().equals("CARD")){
      List<CardTransaction> cardTransactionList = csvImportHistoryService.getCardTransactionHistory(userId, csvImportId);
      model.addAttribute("cardTransactionList", cardTransactionList);

      List<CardTransactionUpdateDetail> cardTransactionUpdateDetailList = csvImportHistoryService.findUpdatedByCsvImportHistoryId(userId, csvImportId);
      model.addAttribute("cardTransactionUpdateDetailList", cardTransactionUpdateDetailList);
    }

    return "csv/import-history-detail";
  }

  @PostMapping("/import/history/{id}/delete")
  public String postMethodName(@PathVariable Long csvImportId, Authentication authentication, RedirectAttributes redirectAttributes) {
      Long userId = getUserId(authentication);

      try {
        csvImportHistoryService.deleteCsvImport(userId, csvImportId);
        redirectAttributes.addFlashAttribute("message", "CSV登録履歴を削除しました");
      } catch (Exception e) {
        redirectAttributes.addFlashAttribute("error", "削除に失敗しました：" + e.getMessage());
      }
      
      return "redirect:/csv/import/history";
  }
  

  /**
   * ユーザーIDを取得
   */
  private Long getUserId(Authentication authentication) {
    String username = authentication.getName();
    return userService.findByUsername(username)
        .orElseThrow(() -> new IllegalStateException("ユーザーが見つかりません"))
        .getId();
  }
}
