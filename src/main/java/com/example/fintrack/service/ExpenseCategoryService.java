package com.example.fintrack.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.fintrack.domain.ExpenseCategory;
import com.example.fintrack.repository.ExpenseCategoryRepository;

@Service
public class ExpenseCategoryService {

  private final ExpenseCategoryRepository expenseCategoryRepository;

  public ExpenseCategoryService(ExpenseCategoryRepository expenseCategoryRepository) {
    this.expenseCategoryRepository = expenseCategoryRepository;
  }

  public List<ExpenseCategory> findByUserId(Long userId) {
    return expenseCategoryRepository.findByUserId(userId);
  }

  public void save(ExpenseCategory category) {
    expenseCategoryRepository.save(category);
  }

  public void deleteById(Long id, Long userId) {
    expenseCategoryRepository.deleteById(id, userId);
  }
}
