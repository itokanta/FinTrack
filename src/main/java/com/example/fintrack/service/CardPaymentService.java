package com.example.fintrack.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fintrack.domain.CardPaymentDiscrepancy;
import com.example.fintrack.domain.CardTransaction;
import com.example.fintrack.repository.CardPaymentDiscrepancyRepository;
import com.example.fintrack.repository.CardTransactionRepository;

/**
 * カード支払い管理サービス
 */
@Service
public class CardPaymentService {

  private final CardTransactionRepository cardTransactionRepository;
  private final CardPaymentDiscrepancyRepository discrepancyRepository;

  public CardPaymentService(CardTransactionRepository cardTransactionRepository,
      CardPaymentDiscrepancyRepository discrepancyRepository) {
    this.cardTransactionRepository = cardTransactionRepository;
    this.discrepancyRepository = discrepancyRepository;
  }

  /**
   * 指定した支払い月のカード取引一覧を取得
   */
  public List<CardTransaction> getCardTransactionsByPaymentMonth(Long userId, String paymentMonth) {
    return cardTransactionRepository.findByUserIdAndPaymentMonth(userId, paymentMonth);
  }

  /**
   * 指定した支払い月のカード取引合計金額を取得
   */
  public Long getCardPaymentAmountByMonth(Long userId, String paymentMonth) {
    return cardTransactionRepository.sumAmountByUserIdAndPaymentMonth(userId, paymentMonth);
  }

  /**
   * 未解決のカード差異一覧を取得
   */
  public List<CardPaymentDiscrepancy> getUnresolvedDiscrepancies(Long userId) {
    return discrepancyRepository.findByUserIdAndIsResolved(userId, false);
  }

  /**
   * すべてのカード差異一覧を取得
   */
  public List<CardPaymentDiscrepancy> getAllDiscrepancies(Long userId) {
    return discrepancyRepository.findByUserId(userId);
  }

  /**
   * カード差異を解決済みにする
   */
  @Transactional
  public void markDiscrepancyAsResolved(Long discrepancyId) {
    discrepancyRepository.markAsResolved(discrepancyId);
  }
}
