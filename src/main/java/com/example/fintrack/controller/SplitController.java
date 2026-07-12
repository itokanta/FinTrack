package com.example.fintrack.controller;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import com.example.fintrack.domain.AssetCategory;
import com.example.fintrack.domain.SplitPatternGroup;
import com.example.fintrack.domain.TransactionType;
import com.example.fintrack.security.AuthenticationUserIdResolver;
import com.example.fintrack.service.AssetCategoryService;
import com.example.fintrack.service.SplitService;

/**
 * 収支の分割パターン管理と取引への分配を扱うコントローラー。
 *
 * @author kanta ito
 */
@Controller
@RequestMapping("/split")
public class SplitController {

    private static final Logger log = LoggerFactory.getLogger(SplitController.class);

    private final SplitService splitService;
    private final AssetCategoryService assetCategoryService;
    private final AuthenticationUserIdResolver authenticationUserIdResolver;

    public SplitController(SplitService splitService, AssetCategoryService assetCategoryService,
            AuthenticationUserIdResolver authenticationUserIdResolver) {
        this.splitService = splitService;
        this.assetCategoryService = assetCategoryService;
        this.authenticationUserIdResolver = authenticationUserIdResolver;
    }

    /**
     * 収入用の分割パターン一覧画面を表示する。
     *
     * @param authentication 認証情報
     * @param model ビューモデル
     * @return 分割パターン一覧テンプレート名
     */
    @GetMapping("/income/patterns")
    public String incomePatterns(Authentication authentication, Model model) {
        return patterns(authentication, model, TransactionType.INCOME);
    }

    /**
     * 支出用の分割パターン一覧画面を表示する。
     *
     * @param authentication 認証情報
     * @param model ビューモデル
     * @return 分割パターン一覧テンプレート名
     */
    @GetMapping("/expense/patterns")
    public String expensePatterns(Authentication authentication, Model model) {
        return patterns(authentication, model, TransactionType.EXPENSE);
    }

    /**
     * 分割パターン一覧画面の共通処理。取引種別に応じたパターン一覧を表示する。
     *
     * @param authentication 認証情報
     * @param model ビューモデル
     * @param transactionType 取引種別
     * @return 分割パターン一覧テンプレート名
     */
    private String patterns(Authentication authentication, Model model, TransactionType transactionType) {
        Long userId = authenticationUserIdResolver.getUserId(authentication);
        List<SplitPatternGroup> splitPatternGroupList = splitService.getPatternGroupWithSplitPatternList(userId,
                transactionType);

        model.addAttribute("splitPatternGroupList", splitPatternGroupList);
        model.addAttribute("transactionType", transactionType);

        return "split/patterns";
    }

    /**
     * 収入用の分割パターン登録画面を表示する。
     *
     * @param authentication 認証情報
     * @param model ビューモデル
     * @return 分割パターン登録フォームテンプレート名
     */
    @GetMapping("/income/patterns/new")
    public String newIncomePattern(Authentication authentication, Model model) {
        Long userId = authenticationUserIdResolver.getUserId(authentication);
        List<AssetCategory> categories = assetCategoryService.findByUserId(userId);

        model.addAttribute("categories", categories);
        model.addAttribute("transactionType", TransactionType.INCOME);

        return "split/pattern-form";
    }

    /**
     * 支出用の分割パターン登録画面を表示する。
     *
     * @param authentication 認証情報
     * @param model ビューモデル
     * @return 分割パターン登録フォームテンプレート名
     */
    @GetMapping("/expense/patterns/new")
    public String newExpensePattern(Authentication authentication, Model model) {
        Long userId = authenticationUserIdResolver.getUserId(authentication);
        List<AssetCategory> categories = assetCategoryService.findByUserId(userId);

        model.addAttribute("categories", categories);
        model.addAttribute("transactionType", TransactionType.EXPENSE);

        return "split/pattern-form";
    }

    /**
     * 割合指定で分割パターンを登録する。
     *
     * @param authentication 認証情報
     * @param transactionType 取引種別
     * @param patternName パターン名
     * @param params リクエストパラメータ（ratio_プレフィックス付きの割合）
     * @param redirectAttributes リダイレクト先へのフラッシュ属性
     * @return 分割パターン一覧へのリダイレクト先
     */
    @PostMapping("/patterns/ratio")
    public String createPatternByRatio(Authentication authentication,
            @RequestParam TransactionType transactionType,
            @RequestParam String patternName,
            @RequestParam Map<String, String> params,
            RedirectAttributes redirectAttributes) {
        Long userId = authenticationUserIdResolver.getUserId(authentication);

        try {
            // カテゴリIDと割合のマップを作成
            Map<Long, BigDecimal> categoryRatios = new HashMap<>();

            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (entry.getKey().startsWith("ratio_")) {
                    Long categoryId = Long.parseLong(entry.getKey().substring(6));
                    String ratioStr = entry.getValue();

                    // 空文字列やnullの場合はスキップ
                    if (ratioStr != null && !ratioStr.trim().isEmpty()) {
                        BigDecimal ratio = new BigDecimal(ratioStr);
                        if (ratio.compareTo(BigDecimal.ZERO) > 0) {
                            categoryRatios.put(categoryId, ratio);
                        }
                    }
                }
            }

            splitService.createSplitPatternByRatio(userId, transactionType, patternName,
                    categoryRatios);
            redirectAttributes.addFlashAttribute("message",
                    "分割パターンを登録しました");
        } catch (Exception e) {
            log.error("分割パターン（割合指定）の登録に失敗しました", e);
            redirectAttributes.addFlashAttribute("error", "分割パターンの登録に失敗しました");
        }

        return transactionType == TransactionType.INCOME
                ? "redirect:/split/income/patterns"
                : "redirect:/split/expense/patterns";
    }

    /**
     * 金額指定で分割パターンを登録する。
     *
     * @param authentication 認証情報
     * @param transactionType 取引種別
     * @param patternName パターン名
     * @param params リクエストパラメータ（amount_プレフィックス付きの金額）
     * @param redirectAttributes リダイレクト先へのフラッシュ属性
     * @return 分割パターン一覧へのリダイレクト先
     */
    @PostMapping("/patterns/amount")
    public String createPatternByAmount(Authentication authentication,
            @RequestParam TransactionType transactionType,
            @RequestParam String patternName,
            @RequestParam Map<String, String> params,
            RedirectAttributes redirectAttributes) {
        Long userId = authenticationUserIdResolver.getUserId(authentication);

        try {
            // カテゴリIDと金額のマップを作成
            Map<Long, Long> categoryAmounts = new HashMap<>();

            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (entry.getKey().startsWith("amount_")) {
                    Long categoryId = Long.parseLong(entry.getKey().substring(7));
                    String amountStr = entry.getValue();

                    // 空文字列やnullの場合はスキップ
                    if (amountStr != null && !amountStr.trim().isEmpty()) {
                        Long amount = Long.parseLong(amountStr);
                        if (amount > 0) {
                            categoryAmounts.put(categoryId, amount);
                        }
                    }
                }
            }

            splitService.createSplitPatternByAmount(userId, transactionType, patternName,
                    categoryAmounts);
            redirectAttributes.addFlashAttribute("message",
                    "分割パターンを登録しました");
        } catch (Exception e) {
            log.error("分割パターン（金額指定）の登録に失敗しました", e);
            redirectAttributes.addFlashAttribute("error", "分割パターンの登録に失敗しました");
        }

        return transactionType == TransactionType.INCOME
                ? "redirect:/split/income/patterns"
                : "redirect:/split/expense/patterns";
    }

    /**
     * 分割パターンを削除する。
     *
     * @param id 分割パターンID
     * @param transactionType 取引種別（リダイレクト先の判定用）
     * @param authentication 認証情報
     * @param redirectAttributes リダイレクト先へのフラッシュ属性
     * @return 分割パターン一覧へのリダイレクト先
     */
    @PostMapping("/patterns/{id}/delete")
    public String deletePattern(@PathVariable Long id,
            @RequestParam TransactionType transactionType,
            Authentication authentication, RedirectAttributes redirectAttributes) {
        Long userId = authenticationUserIdResolver.getUserId(authentication);
        try {
            splitService.deletePattern(id, userId);
            redirectAttributes.addFlashAttribute("message", "分割パターンを削除しました");
        } catch (Exception e) {
            log.error("分割パターンの削除に失敗しました: id={}", id, e);
            redirectAttributes.addFlashAttribute("error", "分割パターンの削除に失敗しました");
        }

        return transactionType == TransactionType.INCOME
                ? "redirect:/split/income/patterns"
                : "redirect:/split/expense/patterns";
    }

    /**
     * 収入取引を分割パターンで分配する。
     *
     * @param bankTransactionId 銀行取引ID（銀行取引の場合）
     * @param cardTransactionId カード取引ID（カード取引の場合）
     * @param patternId 分割パターンID
     * @param redirectAttributes リダイレクト先へのフラッシュ属性
     * @param authentication 認証情報
     * @return 収入詳細画面へのリダイレクト先
     */
    @PostMapping("/income/split")
    public String splitIncome(@RequestParam(required = false) Long bankTransactionId,
            @RequestParam(required = false) Long cardTransactionId,
            @RequestParam Long patternId,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {
        Long userId = authenticationUserIdResolver.getUserId(authentication);
        try {
            splitService.splitIncome(bankTransactionId, cardTransactionId, patternId, userId);
            redirectAttributes.addFlashAttribute("message", "収入を分配しました");
        } catch (Exception e) {
            log.error("収入の分配に失敗しました", e);
            redirectAttributes.addFlashAttribute("error", "収入の分配に失敗しました");
        }

        String type = bankTransactionId != null ? "bank" : "card";
        Long id = bankTransactionId != null ? bankTransactionId : cardTransactionId;
        return "redirect:/transactions/incomes/" + id + "?type=" + type;
    }

    /**
     * 支出取引を分割パターンで分配する。
     *
     * @param bankTransactionId 銀行取引ID（銀行取引の場合）
     * @param cardTransactionId カード取引ID（カード取引の場合）
     * @param patternId 分割パターンID
     * @param redirectAttributes リダイレクト先へのフラッシュ属性
     * @param authentication 認証情報
     * @return 支出詳細画面へのリダイレクト先
     */
    @PostMapping("/expense/split")
    public String splitExpense(@RequestParam(required = false) Long bankTransactionId,
            @RequestParam(required = false) Long cardTransactionId,
            @RequestParam Long patternId,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {
        Long userId = authenticationUserIdResolver.getUserId(authentication);
        try {
            splitService.splitExpense(bankTransactionId, cardTransactionId, patternId, userId);
            redirectAttributes.addFlashAttribute("message", "支出を分配しました");
        } catch (Exception e) {
            log.error("支出の分配に失敗しました", e);
            redirectAttributes.addFlashAttribute("error", "支出の分配に失敗しました");
        }

        String type = bankTransactionId != null ? "bank" : "card";
        Long id = bankTransactionId != null ? bankTransactionId : cardTransactionId;
        return "redirect:/transactions/expenses/" + id + "?type=" + type;
    }

    /**
     * 収入取引を単一の資産カテゴリに分配する。
     *
     * @param bankTransactionId 銀行取引ID（銀行取引の場合）
     * @param cardTransactionId カード取引ID（カード取引の場合）
     * @param categoryId 資産カテゴリID
     * @param redirectAttributes リダイレクト先へのフラッシュ属性
     * @param authentication 認証情報
     * @return 収入詳細画面へのリダイレクト先
     */
    @PostMapping("/income/split-single")
    public String splitIncomeSingle(@RequestParam(required = false) Long bankTransactionId,
            @RequestParam(required = false) Long cardTransactionId,
            @RequestParam Long categoryId,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {
        Long userId = authenticationUserIdResolver.getUserId(authentication);
        try {
            splitService.splitIncomeSingle(bankTransactionId, cardTransactionId, categoryId, userId);
            redirectAttributes.addFlashAttribute("message", "収入を分配しました");
        } catch (Exception e) {
            log.error("収入の分配に失敗しました", e);
            redirectAttributes.addFlashAttribute("error", "収入の分配に失敗しました");
        }

        String type = bankTransactionId != null ? "bank" : "card";
        Long id = bankTransactionId != null ? bankTransactionId : cardTransactionId;
        return "redirect:/transactions/incomes/" + id + "?type=" + type;
    }

    /**
     * 支出取引を単一の資産カテゴリに分配する。
     *
     * @param bankTransactionId 銀行取引ID（銀行取引の場合）
     * @param cardTransactionId カード取引ID（カード取引の場合）
     * @param categoryId 資産カテゴリID
     * @param redirectAttributes リダイレクト先へのフラッシュ属性
     * @param authentication 認証情報
     * @return 支出詳細画面へのリダイレクト先
     */
    @PostMapping("/expense/split-single")
    public String splitExpenseSingle(@RequestParam(required = false) Long bankTransactionId,
            @RequestParam(required = false) Long cardTransactionId,
            @RequestParam Long categoryId,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {
        Long userId = authenticationUserIdResolver.getUserId(authentication);
        try {
            splitService.splitExpenseSingle(bankTransactionId, cardTransactionId, categoryId, userId);
            redirectAttributes.addFlashAttribute("message", "支出を分配しました");
        } catch (Exception e) {
            log.error("支出の分配に失敗しました", e);
            redirectAttributes.addFlashAttribute("error", "支出の分配に失敗しました");
        }

        String type = bankTransactionId != null ? "bank" : "card";
        Long id = bankTransactionId != null ? bankTransactionId : cardTransactionId;
        return "redirect:/transactions/expenses/" + id + "?type=" + type;
    }

    /**
     * 収入取引を手動で分割分配する。
     *
     * @param bankTransactionId 銀行取引ID（銀行取引の場合）
     * @param cardTransactionId カード取引ID（カード取引の場合）
     * @param params リクエストパラメータ（category_プレフィックス付きの金額）
     * @param redirectAttributes リダイレクト先へのフラッシュ属性
     * @param authentication 認証情報
     * @return 収入詳細画面へのリダイレクト先
     */
    @PostMapping("/income/split-manual")
    public String splitIncomeManual(@RequestParam(required = false) Long bankTransactionId,
            @RequestParam(required = false) Long cardTransactionId,
            @RequestParam Map<String, String> params,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {
        Long userId = authenticationUserIdResolver.getUserId(authentication);

        try {
            // カテゴリIDと金額のマップを作成
            Map<Long, Long> categoryAmounts = new HashMap<>();

            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (entry.getKey().startsWith("category_")) {
                    Long categoryId = Long.parseLong(entry.getKey().substring(9));
                    String amountStr = entry.getValue();

                    // 空文字列やnullの場合はスキップ
                    if (amountStr != null && !amountStr.trim().isEmpty()) {
                        Long amount = Long.parseLong(amountStr);
                        if (amount > 0) {
                            categoryAmounts.put(categoryId, amount);
                        }
                    }
                }
            }

            splitService.splitIncomeManual(bankTransactionId, cardTransactionId, categoryAmounts, userId);
            redirectAttributes.addFlashAttribute("message", "収入を手動分配しました");
        } catch (Exception e) {
            log.error("収入の手動分配に失敗しました", e);
            redirectAttributes.addFlashAttribute("error", "収入の手動分配に失敗しました");
        }

        String type = bankTransactionId != null ? "bank" : "card";
        Long id = bankTransactionId != null ? bankTransactionId : cardTransactionId;
        return "redirect:/transactions/incomes/" + id + "?type=" + type;
    }

    /**
     * 支出取引を手動で分割分配する。
     *
     * @param bankTransactionId 銀行取引ID（銀行取引の場合）
     * @param cardTransactionId カード取引ID（カード取引の場合）
     * @param params リクエストパラメータ（category_プレフィックス付きの金額）
     * @param redirectAttributes リダイレクト先へのフラッシュ属性
     * @param authentication 認証情報
     * @return 支出詳細画面へのリダイレクト先
     */
    @PostMapping("/expense/split-manual")
    public String splitExpenseManual(@RequestParam(required = false) Long bankTransactionId,
            @RequestParam(required = false) Long cardTransactionId,
            @RequestParam Map<String, String> params,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {
        Long userId = authenticationUserIdResolver.getUserId(authentication);

        try {
            // カテゴリIDと金額のマップを作成
            Map<Long, Long> categoryAmounts = new HashMap<>();

            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (entry.getKey().startsWith("category_")) {
                    Long categoryId = Long.parseLong(entry.getKey().substring(9));
                    String amountStr = entry.getValue();

                    // 空文字列やnullの場合はスキップ
                    if (amountStr != null && !amountStr.trim().isEmpty()) {
                        Long amount = Long.parseLong(amountStr);
                        if (amount > 0) {
                            categoryAmounts.put(categoryId, amount);
                        }
                    }
                }
            }

            splitService.splitExpenseManual(bankTransactionId, cardTransactionId, categoryAmounts, userId);
            redirectAttributes.addFlashAttribute("message", "支出を手動分配しました");
        } catch (Exception e) {
            log.error("支出の手動分配に失敗しました", e);
            redirectAttributes.addFlashAttribute("error", "支出の手動分配に失敗しました");
        }

        String type = bankTransactionId != null ? "bank" : "card";
        Long id = bankTransactionId != null ? bankTransactionId : cardTransactionId;
        return "redirect:/transactions/expenses/" + id + "?type=" + type;
    }
}
