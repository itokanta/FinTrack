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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.fintrack.domain.BankTransaction;
import com.example.fintrack.domain.CardTransaction;
import com.example.fintrack.domain.CsvImportHistory;
import com.example.fintrack.domain.dto.CardTransactionUpdateDetail;
import com.example.fintrack.security.AuthenticationUserIdResolver;
import com.example.fintrack.service.CsvImportHistoryService;


@Controller
@RequestMapping("/csv")
public class CsvImportHistoryController {

  private static final Logger log = LoggerFactory.getLogger(CsvImportHistoryController.class);

  private final AuthenticationUserIdResolver authenticationUserIdResolver;
  private final CsvImportHistoryService csvImportHistoryService;
  

  public CsvImportHistoryController(AuthenticationUserIdResolver authenticationUserIdResolver,
      CsvImportHistoryService csvImportHistoryService) {
    this.authenticationUserIdResolver = authenticationUserIdResolver;
    this.csvImportHistoryService = csvImportHistoryService;
  }

  @GetMapping("/import/history")
  public String getMethodName(Authentication authentication, Model model) {
    Long userId = authenticationUserIdResolver.getUserId(authentication);
    List<CsvImportHistory> histories = csvImportHistoryService.findAllByUserId(userId);
    model.addAttribute("histories", histories);
    return "csv/import-history";
  }

  @GetMapping("/import/history/{id}")
  public String getMethodName(@PathVariable Long id, Authentication authentication, Model model) {
    Long userId = authenticationUserIdResolver.getUserId(authentication);

    CsvImportHistory history = csvImportHistoryService.findById(id, userId);
    model.addAttribute("history", history);

    if(history.getImportType().equals("BANK")){
      List<BankTransaction> bankTransactionList = csvImportHistoryService.getBankTransactionHistory(userId, id);
      model.addAttribute("bankTransactionList", bankTransactionList);
    } else if (history.getImportType().equals("CARD")){
      List<CardTransaction> cardTransactionList = csvImportHistoryService.getCardTransactionHistory(userId, id);
      model.addAttribute("cardTransactionList", cardTransactionList);
    }
    
    List<CardTransactionUpdateDetail> cardTransactionUpdateDetailList = csvImportHistoryService.findUpdatedByCsvImportHistoryId(userId, id);
    model.addAttribute("cardTransactionUpdateDetailList", cardTransactionUpdateDetailList);

    return "csv/import-history-detail";
  }

  @PostMapping("/import/history/{id}/delete")
  public String postMethodName(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
      Long userId = authenticationUserIdResolver.getUserId(authentication);

      try {
        csvImportHistoryService.deleteCsvImport(userId, id);
        redirectAttributes.addFlashAttribute("message", "CSV登録履歴を削除しました");
      } catch (Exception e) {
        log.error("CSV登録履歴の削除に失敗しました: id={}", id, e);
        redirectAttributes.addFlashAttribute("error", "削除に失敗しました");
      }
      
      return "redirect:/csv/import/history";
  }
}
