package com.example.fintrack.controller;

import com.example.fintrack.domain.CardPaymentDiscrepancy;
import com.example.fintrack.domain.CardTransaction;
import com.example.fintrack.service.CardPaymentService;
import com.example.fintrack.service.UserService;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * カード支払い管理コントローラー
 */
@Controller
@RequestMapping("/card-payments")
public class CardPaymentController {
    
    private final CardPaymentService cardPaymentService;
    private final UserService userService;
    
    public CardPaymentController(CardPaymentService cardPaymentService, UserService userService) {
      this.cardPaymentService = cardPaymentService;
      this.userService = userService;
    }

    /**
     * 支払い月別カード明細
     */
    @GetMapping("/by-month")
    public String byMonth(Authentication authentication,
                         @RequestParam(required = false) String paymentMonth,
                         Model model) {
        Long userId = getUserId(authentication);
        
        // 支払い月の指定がない場合は今月
        String targetPaymentMonth;
        if (paymentMonth != null) {
            targetPaymentMonth = paymentMonth;
        } else {
            targetPaymentMonth = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        }
        
        // 支払い月単位のカード取引を取得
        List<CardTransaction> cardTransactions = cardPaymentService.getCardTransactionsByPaymentMonth(userId, targetPaymentMonth);
        Long totalAmount = cardPaymentService.getCardPaymentAmountByMonth(userId, targetPaymentMonth);
        
        model.addAttribute("paymentMonth", targetPaymentMonth);
        model.addAttribute("cardTransactions", cardTransactions);
        model.addAttribute("totalAmount", totalAmount);
        
        return "card-payments/by-month";
    }
    
    /**
     * カード差異一覧画面
     */
    @GetMapping("/discrepancies")
    public String discrepancies(Authentication authentication, Model model) {
        Long userId = getUserId(authentication);
        
        // 未解決の差異を取得
        List<CardPaymentDiscrepancy> unresolvedDiscrepancies = 
            cardPaymentService.getUnresolvedDiscrepancies(userId);
        
        // すべての差異を取得
        List<CardPaymentDiscrepancy> allDiscrepancies = 
            cardPaymentService.getAllDiscrepancies(userId);
        
        model.addAttribute("unresolvedDiscrepancies", unresolvedDiscrepancies);
        model.addAttribute("allDiscrepancies", allDiscrepancies);
        
        return "card-payments/discrepancies";
    }
    
    /**
     * カード差異を解決済みにする
     */
    @PostMapping("/discrepancies/{id}/resolve")
    public String resolveDiscrepancy(@PathVariable Long id,
                                    RedirectAttributes redirectAttributes) {
        try {
            cardPaymentService.markDiscrepancyAsResolved(id);
            redirectAttributes.addFlashAttribute("message", "差異を解決済みにしました");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "処理に失敗しました: " + e.getMessage());
        }
        
        return "redirect:/card-payments/discrepancies";
    }
    
    /**
     * ユーザーIDを取得
     */
    private Long getUserId(Authentication authentication) {
        String username = authentication.getName();
        return userService.findUserIdByUsername(username);
    }
}
