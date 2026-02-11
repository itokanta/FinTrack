package com.example.fintrack.controller;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.fintrack.domain.CardPaymentDiscrepancy;
import com.example.fintrack.repository.AssetCategoryRepository;
import com.example.fintrack.repository.UserRepository;
import com.example.fintrack.service.CardPaymentService;
import com.example.fintrack.service.DiscrepancyTransactionService;
import com.example.fintrack.service.SplitService;

@Controller
@RequestMapping("/discrepancy-transactions")
public class DiscrepancyTransactionController {

  private final DiscrepancyTransactionService discrepancyTransactionService;
  private final CardPaymentService cardPaymentService;
  private final AssetCategoryRepository assetCategoryRepository;
  private final SplitService splitService;
  private final UserRepository userRepository;

  public DiscrepancyTransactionController(
      DiscrepancyTransactionService discrepancyTransactionService,
      CardPaymentService cardPaymentService,
      AssetCategoryRepository assetCategoryRepository,
      SplitService splitService,
      UserRepository userRepository) {
    this.discrepancyTransactionService = discrepancyTransactionService;
    this.cardPaymentService = cardPaymentService;
    this.assetCategoryRepository = assetCategoryRepository;
    this.splitService = splitService;
    this.userRepository = userRepository;
  }

  /**
   * 差異解決フォーム表示
   */
  @GetMapping("/{id}/resolve")
  public String showResolveForm(@PathVariable Long id,
      Authentication authentication,
      Model model) {
    Long userId = getUserId(authentication);

    // 差異情報を取得
    CardPaymentDiscrepancy discrepancy = cardPaymentService.getAllDiscrepancies(userId).stream()
        .filter(d -> d.getId().equals(id))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("差異が見つかりません"));

    // カテゴリ一覧を取得
    model.addAttribute("discrepancy", discrepancy);
    model.addAttribute("assetCategories", assetCategoryRepository.findByUserId(userId));
    
    // 差異の絶対値を計算（テンプレートで使用）
    model.addAttribute("absoluteDifference", Math.abs(discrepancy.getDifferenceAmount()));

    // 差異額に応じた分割パターンを取得（支出 or 収入）
    String transactionType = discrepancy.getDifferenceAmount() > 0 ? "EXPENSE" : "INCOME";
    model.addAttribute("patternSummaries",
        splitService.getPatternSummaries(userId, transactionType));

    return "discrepancy-transactions/resolve-form";
  }

  /**
   * 差異を単一カテゴリに登録
   */
  @PostMapping("/{id}/resolve-single")
  public String resolveDiscrepancySingle(@PathVariable Long id,
      @RequestParam String description,
      @RequestParam Long categoryId,
      RedirectAttributes redirectAttributes) {
    try {
      discrepancyTransactionService.resolveDiscrepancySingle(id, description, categoryId);
      redirectAttributes.addFlashAttribute("message",
          "差異を解決しました。資産カテゴリに登録し、残高を調整しました。");
    } catch (IllegalStateException | IllegalArgumentException e) {
      redirectAttributes.addFlashAttribute("error", e.getMessage());
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("error",
          "差異解決に失敗しました: " + e.getMessage());
    }

    return "redirect:/card-payments/discrepancies";
  }

  /**
   * 差異を手動分割
   */
  @PostMapping("/{id}/resolve-manual")
  public String resolveDiscrepancyManual(@PathVariable Long id,
      @RequestParam String description,
      @RequestParam Map<String, String> params,
      RedirectAttributes redirectAttributes) {
    try {
      // パラメータから category_ で始まるものを抽出
      Map<Long, Long> categoryAmounts = params.entrySet().stream()
          .filter(e -> e.getKey().startsWith("category_"))
          .filter(e -> e.getValue() != null && !e.getValue().trim().isEmpty())
          .collect(Collectors.toMap(
              e -> Long.parseLong(e.getKey().substring("category_".length())),
              e -> Long.parseLong(e.getValue())));

      if (categoryAmounts.isEmpty()) {
        redirectAttributes.addFlashAttribute("error", "分割金額を入力してください");
        return "redirect:/discrepancy-transactions/" + id + "/resolve";
      }

      discrepancyTransactionService.resolveDiscrepancyManual(id, description, categoryAmounts);
      redirectAttributes.addFlashAttribute("message",
          "差異を解決しました。各資産カテゴリに分割登録し、残高を調整しました。");
    } catch (IllegalStateException | IllegalArgumentException e) {
      redirectAttributes.addFlashAttribute("error", e.getMessage());
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("error",
          "差異解決に失敗しました: " + e.getMessage());
    }

    return "redirect:/card-payments/discrepancies";
  }

  /**
   * 差異を分割パターンで解決
   */
  @PostMapping("/{id}/resolve-pattern")
  public String resolveDiscrepancyPattern(@PathVariable Long id,
      @RequestParam String description,
      @RequestParam Long patternId,
      RedirectAttributes redirectAttributes) {
    try {
      discrepancyTransactionService.resolveDiscrepancyPattern(id, description, patternId);
      redirectAttributes.addFlashAttribute("message",
          "差異を解決しました。分割パターンに従って登録し、残高を調整しました。");
    } catch (IllegalStateException | IllegalArgumentException e) {
      redirectAttributes.addFlashAttribute("error", e.getMessage());
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("error",
          "差異解決に失敗しました: " + e.getMessage());
    }

    return "redirect:/card-payments/discrepancies";
  }

  private Long getUserId(Authentication authentication) {
    String username = authentication.getName();
    return userRepository.findByUsername(username)
        .orElseThrow(() -> new IllegalStateException("ユーザーが見つかりません"))
        .getId();
  }
}