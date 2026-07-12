package com.example.fintrack.controller;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.fintrack.domain.CardPaymentDiscrepancy;
import com.example.fintrack.domain.CardTransaction;
import com.example.fintrack.security.AuthenticationUserIdResolver;
import com.example.fintrack.service.CardPaymentService;

/**
 * カード支払い月別明細と銀行引き落とし差異を扱うコントローラー。
 *
 * @author kanta ito
 */
@Controller
@RequestMapping("/card-payments")
public class CardPaymentController {
    
    private final CardPaymentService cardPaymentService;
    private final AuthenticationUserIdResolver authenticationUserIdResolver;

    public CardPaymentController(CardPaymentService cardPaymentService,
        AuthenticationUserIdResolver authenticationUserIdResolver) {
      this.cardPaymentService = cardPaymentService;
      this.authenticationUserIdResolver = authenticationUserIdResolver;
    }

    /**
     * 支払い月別のカード明細画面を表示する。月の指定がなければ今月を表示する。
     *
     * @param authentication 認証情報
     * @param paymentMonth 支払い月（yyyy-MM形式、省略可）
     * @param model ビューモデル
     * @return 月別カード明細テンプレート名
     */
    @GetMapping("/by-month")
    public String byMonth(Authentication authentication,
                         @RequestParam(required = false) String paymentMonth,
                         Model model) {
        Long userId = authenticationUserIdResolver.getUserId(authentication);
        
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
     * カード引き落とし差異一覧画面を表示する。未解決と全件の両方を表示する。
     *
     * @param authentication 認証情報
     * @param model ビューモデル
     * @return 差異一覧テンプレート名
     */
    @GetMapping("/discrepancies")
    public String discrepancies(Authentication authentication, Model model) {
        Long userId = authenticationUserIdResolver.getUserId(authentication);
        
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
}
