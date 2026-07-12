package com.example.fintrack.controller;

import java.util.Map;
import java.util.stream.Collectors;

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

import com.example.fintrack.domain.CardPaymentDiscrepancy;
import com.example.fintrack.domain.TransactionType;
import com.example.fintrack.security.AuthenticationUserIdResolver;
import com.example.fintrack.service.AssetCategoryService;
import com.example.fintrack.service.CardPaymentService;
import com.example.fintrack.service.DiscrepancyTransactionService;
import com.example.fintrack.service.SplitService;

/**
 * カード引き落とし差異の解決（単一・手動・パターン分割）を扱うコントローラー。
 *
 * @author kanta ito
 */
@Controller
@RequestMapping("/discrepancy-transactions")
public class DiscrepancyTransactionController {

  private static final Logger log = LoggerFactory.getLogger(DiscrepancyTransactionController.class);

  private final DiscrepancyTransactionService discrepancyTransactionService;
  private final CardPaymentService cardPaymentService;
  private final AssetCategoryService assetCategoryService;
  private final SplitService splitService;
  private final AuthenticationUserIdResolver authenticationUserIdResolver;

  public DiscrepancyTransactionController(DiscrepancyTransactionService discrepancyTransactionService,
      CardPaymentService cardPaymentService, AssetCategoryService assetCategoryService, SplitService splitService,
      AuthenticationUserIdResolver authenticationUserIdResolver) {
    this.discrepancyTransactionService = discrepancyTransactionService;
    this.cardPaymentService = cardPaymentService;
    this.assetCategoryService = assetCategoryService;
    this.splitService = splitService;
    this.authenticationUserIdResolver = authenticationUserIdResolver;
  }

  /**
   * 差異解決フォーム画面を表示する。
   *
   * @param id 差異ID
   * @param authentication 認証情報
   * @param model ビューモデル
   * @return 差異解決フォームテンプレート名
   */
  @GetMapping("/{id}/resolve")
  public String showResolveForm(@PathVariable Long id,
      Authentication authentication,
      Model model) {
    Long userId = authenticationUserIdResolver.getUserId(authentication);

    // 差異情報を取得
    CardPaymentDiscrepancy discrepancy = cardPaymentService.findById(id, userId);

    // カテゴリ一覧を取得
    model.addAttribute("discrepancy", discrepancy);
    model.addAttribute("assetCategories", assetCategoryService.findByUserId(userId));
    
    // 差異の絶対値を計算（テンプレートで使用）
    model.addAttribute("absoluteDifference", Math.abs(discrepancy.getDifferenceAmount()));

    // 差異額に応じた分割パターンを取得（支出 or 収入）
    TransactionType transactionType =
        discrepancy.getDifferenceAmount() > 0 ? TransactionType.EXPENSE : TransactionType.INCOME;
    model.addAttribute("splitPatternGroupList",
        splitService.getSplitPatternGroupList(userId, transactionType));

    return "discrepancy-transactions/resolve-form";
  }

  /**
   * 差異を単一の資産カテゴリに登録して解決する。
   *
   * @param id 差異ID
   * @param description 摘要
   * @param categoryId 資産カテゴリID
   * @param redirectAttributes リダイレクト先へのフラッシュ属性
   * @param authentication 認証情報
   * @return 差異一覧へのリダイレクト先
   */
  @PostMapping("/{id}/resolve-single")
  public String resolveDiscrepancySingle(@PathVariable Long id,
      @RequestParam String description,
      @RequestParam Long categoryId,
      RedirectAttributes redirectAttributes,
      Authentication authentication) {
    Long userId = authenticationUserIdResolver.getUserId(authentication);
    try {
      discrepancyTransactionService.resolveDiscrepancySingle(id, description, categoryId, userId);
      redirectAttributes.addFlashAttribute("message",
          "差異を解決しました。資産カテゴリに登録し、残高を調整しました。");
    } catch (Exception e) {
      log.error("差異の単一カテゴリ解決に失敗しました: id={}", id, e);
      redirectAttributes.addFlashAttribute("error", "差異解決に失敗しました");
    }

    return "redirect:/card-payments/discrepancies";
  }

  /**
   * 差異を手動分割して解決する。分割金額が未入力の場合はフォームへ戻す。
   *
   * @param id 差異ID
   * @param description 摘要
   * @param params リクエストパラメータ（category_プレフィックス付きの金額）
   * @param redirectAttributes リダイレクト先へのフラッシュ属性
   * @param authentication 認証情報
   * @return 差異一覧または解決フォームへのリダイレクト先
   */
  @PostMapping("/{id}/resolve-manual")
  public String resolveDiscrepancyManual(@PathVariable Long id,
      @RequestParam String description,
      @RequestParam Map<String, String> params,
      RedirectAttributes redirectAttributes,
      Authentication authentication) {
    Long userId = authenticationUserIdResolver.getUserId(authentication);
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

      discrepancyTransactionService.resolveDiscrepancyManual(id, description, categoryAmounts, userId);
      redirectAttributes.addFlashAttribute("message",
          "差異を解決しました。各資産カテゴリに分割登録し、残高を調整しました。");
    } catch (Exception e) {
      log.error("差異の手動分割解決に失敗しました: id={}", id, e);
      redirectAttributes.addFlashAttribute("error", "差異解決に失敗しました");
    }

    return "redirect:/card-payments/discrepancies";
  }

  /**
   * 差異を分割パターンで解決する。
   *
   * @param id 差異ID
   * @param description 摘要
   * @param patternId 分割パターンID
   * @param redirectAttributes リダイレクト先へのフラッシュ属性
   * @param authentication 認証情報
   * @return 差異一覧へのリダイレクト先
   */
  @PostMapping("/{id}/resolve-pattern")
  public String resolveDiscrepancyPattern(@PathVariable Long id,
      @RequestParam String description,
      @RequestParam Long patternId,
      RedirectAttributes redirectAttributes,
      Authentication authentication) {
    Long userId = authenticationUserIdResolver.getUserId(authentication);
    try {
      discrepancyTransactionService.resolveDiscrepancyPattern(id, description, patternId, userId);
      redirectAttributes.addFlashAttribute("message",
          "差異を解決しました。分割パターンに従って登録し、残高を調整しました。");
    } catch (Exception e) {
      log.error("差異の分割パターン解決に失敗しました: id={}", id, e);
      redirectAttributes.addFlashAttribute("error", "差異解決に失敗しました");
    }

    return "redirect:/card-payments/discrepancies";
  }
}