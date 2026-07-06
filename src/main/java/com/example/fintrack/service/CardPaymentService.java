package com.example.fintrack.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.fintrack.domain.CardPaymentDiscrepancy;
import com.example.fintrack.domain.CardTransaction;
import com.example.fintrack.repository.CardPaymentDiscrepancyRepository;
import com.example.fintrack.repository.CardTransactionRepository;

/**
 * カード引き落とし金額の照合と差異データの参照を扱うサービス。
 *
 * @author kanta ito
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
   * 指定した支払い月のカード取引一覧を取得する。
   *
   * @param userId       ユーザーID
   * @param paymentMonth 支払い月（yyyy-MM形式）
   * @return カード取引の一覧
   */
  public List<CardTransaction> getCardTransactionsByPaymentMonth(Long userId, String paymentMonth) {
    return cardTransactionRepository.findByUserIdAndPaymentMonth(userId, paymentMonth);
  }

  /**
   * 指定した支払い月のカード取引合計金額を取得する。
   *
   * @param userId       ユーザーID
   * @param paymentMonth 支払い月（yyyy-MM形式）
   * @return 合計金額（取引がなければnullの可能性あり）
   */
  public Long getCardPaymentAmountByMonth(Long userId, String paymentMonth) {
    return cardTransactionRepository.sumAmountByUserIdAndPaymentMonth(userId, paymentMonth);
  }

  /**
   * 未解決のカード差異一覧を取得する。
   *
   * @param userId ユーザーID
   * @return 未解決の差異一覧
   */
  public List<CardPaymentDiscrepancy> getUnresolvedDiscrepancies(Long userId) {
    return discrepancyRepository.findByUserIdAndIsResolved(userId, false);
  }

  /**
   * カード差異の一覧をすべて取得する（解決済み含む）。
   *
   * @param userId ユーザーID
   * @return 差異の一覧
   */
  public List<CardPaymentDiscrepancy> getAllDiscrepancies(Long userId) {
    return discrepancyRepository.findByUserId(userId);
  }

  /**
   * IDでカード差異を1件取得する。
   *
   * @param id     差異ID
   * @param userId ユーザーID
   * @return 差異データ
   * @throws IllegalArgumentException 差異が見つからない場合
   */
  public CardPaymentDiscrepancy findById(Long id, Long userId) {
    return discrepancyRepository.findById(id, userId).orElseThrow(() -> new IllegalArgumentException("カード差異が見つかりません。"));
  }
}
