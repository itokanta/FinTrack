package com.example.fintrack.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.fintrack.domain.ExpenseCategory;
import com.example.fintrack.repository.ExpenseCategoryRepository;

/**
 * 支出カテゴリの登録・削除を扱うサービス。
 *
 * @author kanta ito
 */
@Service
public class ExpenseCategoryService {

  private final ExpenseCategoryRepository expenseCategoryRepository;

  public ExpenseCategoryService(ExpenseCategoryRepository expenseCategoryRepository) {
    this.expenseCategoryRepository = expenseCategoryRepository;
  }

  /**
   * ユーザーの支出カテゴリ一覧を取得する。
   *
   * @param userId ユーザーID
   * @return 支出カテゴリの一覧
   */
  public List<ExpenseCategory> findByUserId(Long userId) {
    return expenseCategoryRepository.findByUserId(userId);
  }

  /**
   * 支出カテゴリを保存する（新規・更新どちらも）。
   *
   * @param category 保存するカテゴリ
   */
  public void save(ExpenseCategory category) {
    expenseCategoryRepository.save(category);
  }

  /**
   * 支出カテゴリを削除する。
   *
   * @param id     カテゴリID
   * @param userId ユーザーID
   */
  public void deleteById(Long id, Long userId) {
    expenseCategoryRepository.deleteById(id, userId);
  }
}
