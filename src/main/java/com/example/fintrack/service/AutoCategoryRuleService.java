package com.example.fintrack.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.fintrack.domain.AutoCategoryRule;
import com.example.fintrack.repository.AutoCategoryRuleRepository;

@Service
public class AutoCategoryRuleService {

  private final AutoCategoryRuleRepository autoCategoryRuleRepository;

  public AutoCategoryRuleService(AutoCategoryRuleRepository autoCategoryRuleRepository) {
    this.autoCategoryRuleRepository = autoCategoryRuleRepository;
  }

  public Optional<AutoCategoryRule> findById(Long id, Long userId) {
    return autoCategoryRuleRepository.findById(id, userId);
  }

  public List<AutoCategoryRule> findByUserId(Long userId) {
    return autoCategoryRuleRepository.findByUserId(userId);
  }

  public AutoCategoryRule save(AutoCategoryRule autoCategoryRule) {
    return autoCategoryRuleRepository.save(autoCategoryRule);
  }

  public void deleteById(Long id, Long userId) {
    autoCategoryRuleRepository.deleteById(id, userId);
  }
}
