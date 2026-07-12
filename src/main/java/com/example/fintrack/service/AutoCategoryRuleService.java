package com.example.fintrack.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.fintrack.domain.AutoCategoryRule;
import com.example.fintrack.repository.AutoCategoryRuleRepository;

/**
 * CSVインポート時の自動カテゴリ割当ルールを扱うサービス。
 *
 * @author kanta ito
 */
@Service
public class AutoCategoryRuleService {

  private final AutoCategoryRuleRepository autoCategoryRuleRepository;

  public AutoCategoryRuleService(AutoCategoryRuleRepository autoCategoryRuleRepository) {
    this.autoCategoryRuleRepository = autoCategoryRuleRepository;
  }

  /**
   * IDで自動カテゴリルールを1件取得する。
   *
   * @param id     ルールID
   * @param userId ユーザーID
   * @return 見つかったルール（なければ空）
   */
  public Optional<AutoCategoryRule> findById(Long id, Long userId) {
    return autoCategoryRuleRepository.findById(id, userId);
  }

  /**
   * ユーザーの自動カテゴリルール一覧を取得する。
   *
   * @param userId ユーザーID
   * @return ルールの一覧
   */
  public List<AutoCategoryRule> findByUserId(Long userId) {
    return autoCategoryRuleRepository.findByUserId(userId);
  }

  /**
   * 自動カテゴリルールを保存する（新規・更新どちらも）。
   *
   * @param autoCategoryRule 保存するルール
   * @return 保存後のルール
   */
  public AutoCategoryRule save(AutoCategoryRule autoCategoryRule) {
    return autoCategoryRuleRepository.save(autoCategoryRule);
  }

  /**
   * 自動カテゴリルールを削除する。
   *
   * @param id     ルールID
   * @param userId ユーザーID
   */
  public void deleteById(Long id, Long userId) {
    autoCategoryRuleRepository.deleteById(id, userId);
  }
}
