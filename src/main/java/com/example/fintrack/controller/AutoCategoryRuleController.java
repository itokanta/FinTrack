package com.example.fintrack.controller;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.fintrack.domain.AssetCategory;
import com.example.fintrack.domain.AutoCategoryRule;
import com.example.fintrack.domain.TransactionType;
import com.example.fintrack.security.AuthenticationUserIdResolver;
import com.example.fintrack.service.AssetCategoryService;
import com.example.fintrack.service.AutoCategoryRuleService;
import com.example.fintrack.service.TransactionService;

/**
 * 自動カテゴリルールの一覧・作成・編集・削除を扱うコントローラー。
 *
 * @author kanta ito
 */
@Controller
@RequestMapping("/auto-category-rules")
public class AutoCategoryRuleController {

    private static final Logger log = LoggerFactory.getLogger(AutoCategoryRuleController.class);

    private final AutoCategoryRuleService autoCategoryRuleService;
    private final AssetCategoryService assetCategoryService;
    private final AuthenticationUserIdResolver authenticationUserIdResolver;
    private final TransactionService transactionService;

    public AutoCategoryRuleController(AutoCategoryRuleService autoCategoryRuleService,
        AssetCategoryService assetCategoryService, AuthenticationUserIdResolver authenticationUserIdResolver,
        TransactionService transactionService) {
      this.autoCategoryRuleService = autoCategoryRuleService;
      this.assetCategoryService = assetCategoryService;
      this.authenticationUserIdResolver = authenticationUserIdResolver;
      this.transactionService = transactionService;
    }

    /**
     * 自動カテゴリルール一覧画面を表示する。
     *
     * @param authentication 認証情報
     * @param model ビューモデル
     * @return ルール一覧テンプレート名
     */
    @GetMapping
    public String index(Authentication authentication, Model model) {
        Long userId = authenticationUserIdResolver.getUserId(authentication);

        // ルール一覧を取得
        List<AutoCategoryRule> rules = autoCategoryRuleService.findByUserId(userId);

        // 資産カテゴリのマップを作成
        List<AssetCategory> assetCategories = assetCategoryService.findByUserId(userId);
        Map<Long, String> assetCategoryMap = new HashMap<>();
        for (AssetCategory category : assetCategories) {
            assetCategoryMap.put(category.getId(), category.getCategoryName());
        }

        model.addAttribute("rules", rules);
        model.addAttribute("assetCategoryMap", assetCategoryMap);

        return "auto-category-rules/index";
    }

    /**
     * 新規ルール作成画面を表示する。
     *
     * @param authentication 認証情報
     * @param model ビューモデル
     * @return ルール作成フォームテンプレート名
     */
    @GetMapping("/new")
    public String newRule(Authentication authentication, Model model) {
        Long userId = authenticationUserIdResolver.getUserId(authentication);

        // 資産カテゴリ一覧を取得
        List<AssetCategory> assetCategories = assetCategoryService.findByUserId(userId);
        model.addAttribute("rule", new AutoCategoryRule());
        model.addAttribute("assetCategories", assetCategories);
        model.addAttribute("transactionTypes", TransactionType.values());

        return "auto-category-rules/form";
    }

    /**
     * ルール編集画面を表示する。ルールが見つからない場合は一覧へリダイレクトする。
     *
     * @param id ルールID
     * @param authentication 認証情報
     * @param model ビューモデル
     * @param redirectAttributes リダイレクト先へのフラッシュ属性
     * @return ルール編集フォームテンプレート名、または一覧へのリダイレクト先
     */
    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        Long userId = authenticationUserIdResolver.getUserId(authentication);

        // ルールを取得
        AutoCategoryRule rule = autoCategoryRuleService.findById(id, userId)
                .orElse(null);

        if (rule == null) {
            redirectAttributes.addFlashAttribute("error", "ルールが見つかりません");
            return "redirect:/auto-category-rules";
        }

        // 資産カテゴリ一覧を取得
        List<AssetCategory> assetCategories = assetCategoryService.findByUserId(userId);

        model.addAttribute("rule", rule);
        model.addAttribute("assetCategories", assetCategories);
        model.addAttribute("transactionTypes", TransactionType.values());

        return "auto-category-rules/form";
    }

    /**
     * ルールを新規作成または更新する。
     *
     * @param id ルールID（更新時のみ指定）
     * @param assetCategoryId 割り当て先の資産カテゴリID
     * @param descriptionPattern 摘要のマッチパターン
     * @param transactionType 取引種別
     * @param authentication 認証情報
     * @param redirectAttributes リダイレクト先へのフラッシュ属性
     * @return ルール一覧へのリダイレクト先
     */
    @PostMapping
    public String save(@RequestParam(required = false) Long id,
            @RequestParam Long assetCategoryId,
            @RequestParam String descriptionPattern,
            @RequestParam TransactionType transactionType,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        Long userId = authenticationUserIdResolver.getUserId(authentication);

        try {
            AutoCategoryRule rule;

            if (id != null) {
                // 更新
                rule = autoCategoryRuleService.findById(id, userId)
                        .orElseThrow(() -> new IllegalArgumentException("ルールが見つかりません"));

            } else {
                // 新規作成
                rule = new AutoCategoryRule();
                rule.setUserId(userId);
            }

            rule.setAssetCategoryId(assetCategoryId);
            rule.setDescriptionPattern(descriptionPattern);
            rule.setTransactionType(transactionType);

            autoCategoryRuleService.save(rule);

            redirectAttributes.addFlashAttribute("message",
                    id != null ? "ルールを更新しました" : "ルールを作成しました");
        } catch (Exception e) {
            log.error("自動カテゴリルールの保存に失敗しました", e);
            redirectAttributes.addFlashAttribute("error", "ルールの保存に失敗しました");
        }

        return "redirect:/auto-category-rules";
    }

    /**
     * ルールを削除する。
     *
     * @param id ルールID
     * @param authentication 認証情報
     * @param redirectAttributes リダイレクト先へのフラッシュ属性
     * @return ルール一覧へのリダイレクト先
     */
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        Long userId = authenticationUserIdResolver.getUserId(authentication);

        try {
            autoCategoryRuleService.deleteById(id, userId);
            redirectAttributes.addFlashAttribute("message", "ルールを削除しました");
        } catch (Exception e) {
            log.error("自動カテゴリルールの削除に失敗しました: id={}", id, e);
            redirectAttributes.addFlashAttribute("error", "ルールの削除に失敗しました");
        }

        return "redirect:/auto-category-rules";
    }

    /**
     * 過去の銀行・カード取引から摘要候補を検索する。最大20件まで返す。
     *
     * @param query 検索キーワード
     * @param authentication 認証情報
     * @return 摘要候補の一覧
     */
    @GetMapping("/search-descriptions")
    @ResponseBody
    public List<DescriptionSuggestion> searchDescriptions(@RequestParam String query,
            Authentication authentication) {
        Long userId = authenticationUserIdResolver.getUserId(authentication);

        // 銀行取引とカード取引から摘要を取得
        Set<DescriptionSuggestion> suggestions = new LinkedHashSet<>();

        // 銀行取引から取得
        List<String> bankDescriptions = transactionService.findBankDistinctDescriptionsByUserId(userId);
        for (String desc : bankDescriptions) {
            if (desc.toLowerCase().contains(query.toLowerCase())) {
                suggestions.add(new DescriptionSuggestion(desc, "銀行"));
            }
        }

        // カード取引から取得
        List<String> cardDescriptions = transactionService.findCardDistinctDescriptionsByUserId(userId);
        for (String desc : cardDescriptions) {
            if (desc.toLowerCase().contains(query.toLowerCase())) {
                suggestions.add(new DescriptionSuggestion(desc, "カード"));
            }
        }

        return suggestions.stream()
                .limit(20)
                .collect(Collectors.toList());
    }

    /**
     * 摘要候補を表すDTO。
     *
     * @author kanta ito
     */
    public static class DescriptionSuggestion {
        private String description;
        private String source;

        public DescriptionSuggestion(String description, String source) {
            this.description = description;
            this.source = source;
        }

        public String getDescription() {
            return description;
        }

        public String getSource() {
            return source;
        }

        /**
         * 摘要テキストが同じかどうかで等価性を判定する。
         *
         * @param o 比較対象
         * @return 等しい場合は {@code true}
         */
        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            DescriptionSuggestion that = (DescriptionSuggestion) o;
            return Objects.equals(description, that.description);
        }

        /**
         * 摘要テキストに基づくハッシュコードを返す。
         *
         * @return ハッシュコード
         */
        @Override
        public int hashCode() {
            return Objects.hash(description);
        }
    }
}
