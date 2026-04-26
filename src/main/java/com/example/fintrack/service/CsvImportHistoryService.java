package com.example.fintrack.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fintrack.domain.BankTransaction;
import com.example.fintrack.domain.CardTransaction;
import com.example.fintrack.domain.CsvImportHistory;
import com.example.fintrack.domain.CsvImportUpdateField;
import com.example.fintrack.domain.CsvImportUpdateHistory;
import com.example.fintrack.domain.TransactionType;
import com.example.fintrack.domain.dto.CardTransactionUpdateDetail;
import com.example.fintrack.repository.AssetRepository;
import com.example.fintrack.repository.BankTransactionRepository;
import com.example.fintrack.repository.CardTransactionRepository;
import com.example.fintrack.repository.CsvImportHistoryRepository;
import com.example.fintrack.repository.CsvImportUpdateHistoryRepository;

@Service
public class CsvImportHistoryService {
  private final CsvImportHistoryRepository csvImportHistoryRepository;
  private final CsvImportUpdateHistoryRepository csvImportUpdateHistoryRepository;
  private final CardTransactionRepository cardTransactionRepository;
  private final BankTransactionRepository bankTransactionRepository;
  private final AssetRepository assetRepository;

  public CsvImportHistoryService(CsvImportHistoryRepository csvImportHistoryRepository,
      CsvImportUpdateHistoryRepository csvImportUpdateHistoryRepository,
      CardTransactionRepository cardTransactionRepository,
      BankTransactionRepository bankTransactionRepository,
      AssetRepository assetRepository) {
    this.csvImportHistoryRepository = csvImportHistoryRepository;
    this.csvImportUpdateHistoryRepository = csvImportUpdateHistoryRepository;
    this.cardTransactionRepository = cardTransactionRepository;
    this.bankTransactionRepository = bankTransactionRepository;
    this.assetRepository = assetRepository;
  }

  public List<CardTransaction> getCardTransactionHistory(Long userId, Long csvImportHistoryId){
    return cardTransactionRepository.findByCsvImportHistoryId(userId, csvImportHistoryId);
  }

  public List<BankTransaction> getBankTransactionHistory(Long userId, Long csvImportHistoryId){
    return bankTransactionRepository.findByCsvImportHistoryId(userId, csvImportHistoryId);
  }

  public CsvImportHistory findById(Long Id, Long userId){
    return csvImportHistoryRepository.findById(Id, userId);
  }

  public List<CsvImportHistory> findAllByUserId(Long userId) {
    return csvImportHistoryRepository.findAllByUserId(userId);
  }

  public List<CardTransactionUpdateDetail> findUpdatedByCsvImportHistoryId(Long userId, Long csvImportHistoryId) {
    return cardTransactionRepository.findUpdatedByCsvImportHistoryId(userId, csvImportHistoryId);
  }

  @Transactional
  public void deleteCsvImport(Long userId, Long csvImportHistoryId) {
    Map<Long, List<CsvImportUpdateHistory>> updateHistoryMapByCardTransactionId = csvImportUpdateHistoryRepository.findByCardTransactionIdInTargetCsv(userId, csvImportHistoryId);

    if(!updateHistoryMapByCardTransactionId.isEmpty()){
      List<CardTransaction> rollBackCardTransactionList = cardTransactionRepository.findByIds(new ArrayList<>(updateHistoryMapByCardTransactionId.keySet()), userId);
      Map<Long, CardTransaction> rollBackExecutionMap = new HashMap<>();
      List<CsvImportUpdateHistory> csvImportUpdateHistoryRollBackList = new ArrayList<>();

      for(CardTransaction rollBackCardTransaction : rollBackCardTransactionList){
        Map<CsvImportUpdateField, CsvImportUpdateHistory> targetMap = new HashMap<>();
        Map<CsvImportUpdateField, CsvImportUpdateHistory> nextMap = new HashMap<>();

        for(CsvImportUpdateHistory updateHistory : updateHistoryMapByCardTransactionId.get(rollBackCardTransaction.getId())){
          CsvImportUpdateField targetField = updateHistory.getTargetField();
          if(updateHistory.getCsvImportHistoryId().equals(csvImportHistoryId)){
            targetMap.put(targetField, updateHistory);
            continue;
          }
          if(nextMap.containsKey(targetField)){
            continue;
          }
          if(targetMap.containsKey(targetField)){
            nextMap.put(targetField, updateHistory);
          }
        }

        for (CsvImportUpdateField field : CsvImportUpdateField.values()) {
          CsvImportUpdateHistory target = targetMap.get(field);
          if (target == null) continue;

          CsvImportUpdateHistory next = nextMap.get(field);
          if (next == null) {
            CardTransaction ct = rollBackExecutionMap.computeIfAbsent(
                rollBackCardTransaction.getId(), id -> rollBackCardTransaction);
            switch (field) {
              case IS_PAID -> ct.setIsPaid(Boolean.valueOf(target.getValueBefore()));
              case PAYMENT_MONTH -> ct.setPaymentMonth(target.getValueBefore());
            }
          } else {
            next.setValueBefore(target.getValueBefore());
            csvImportUpdateHistoryRollBackList.add(next);
          }
        }
      }
      
      if(!rollBackExecutionMap.isEmpty()){
        List<CardTransaction> rollBackExecutionList = new ArrayList<>();
        for(Map.Entry<Long, CardTransaction> cardTransaction : rollBackExecutionMap.entrySet()){
          rollBackExecutionList.add(cardTransaction.getValue());
        }
        cardTransactionRepository.rollbackIsPaidAndPaymentMonth(rollBackExecutionList);
      }

      if(!csvImportUpdateHistoryRollBackList.isEmpty()){
        csvImportUpdateHistoryRepository.batchUpdateValueBefore(csvImportUpdateHistoryRollBackList);
      }
    }

    List<CardTransaction> deleteCardTransactionList = cardTransactionRepository.findByCsvImportHistoryId(userId,csvImportHistoryId);
    List<BankTransaction> deleteBankTransactionList = bankTransactionRepository.findByCsvImportHistoryId(userId,csvImportHistoryId);

    if (!deleteCardTransactionList.isEmpty()) {
      Map<Long, Long> assetAmountUpdateMap = new HashMap<>();
      Long assetCategoryIdIsNullAmount = 0L;

      for (CardTransaction cardTransaction : deleteCardTransactionList) {
        Long assetCategoryId = cardTransaction.getAssetCategoryId();
        Long amountDelta;

        if (cardTransaction.getTransactionType() == TransactionType.INCOME) {
          // 収入の場合はマイナス
          amountDelta = -cardTransaction.getAmount();
        } else {
          // 支出の場合はプラス
          amountDelta = cardTransaction.getAmount();
        }

        if (assetCategoryId == null) {
          assetCategoryIdIsNullAmount += amountDelta;
          continue;
        }

        if (assetAmountUpdateMap.containsKey(assetCategoryId)) {
          Long mapAmountDelta = assetAmountUpdateMap.get(assetCategoryId);
          assetAmountUpdateMap.put(assetCategoryId, mapAmountDelta + amountDelta);
        } else {
          assetAmountUpdateMap.put(assetCategoryId, amountDelta);
        }
      }

      for (Map.Entry<Long, Long> assetAmountUpdate : assetAmountUpdateMap.entrySet()) {
        assetRepository.updateAmountByCategoryId(userId, assetAmountUpdate.getKey(), assetAmountUpdate.getValue());
      }

      if (!assetCategoryIdIsNullAmount.equals(0L)) {
        assetRepository.updateAmountByCategoryId(userId, null, assetCategoryIdIsNullAmount);
      }

      cardTransactionRepository.deleteByCsvImportHistoryId(userId, csvImportHistoryId);
    }

    if (!deleteBankTransactionList.isEmpty()) {
      Map<Long, Long> assetAmountUpdateMap = new HashMap<>();
      Long assetCategoryIdIsNullAmount = 0L;

      for (BankTransaction bankTransaction : deleteBankTransactionList) {
        if (bankTransaction.getIsCardPayment() != null && bankTransaction.getIsCardPayment()) {
          continue;
        }

        Long assetCategoryId = bankTransaction.getAssetCategoryId();
        Long amountDelta;

        if (bankTransaction.getTransactionType() == TransactionType.INCOME) {
          // 収入の場合はマイナス
          amountDelta = -bankTransaction.getAmount();
        } else {
          // 支出の場合はプラス
          amountDelta = bankTransaction.getAmount();
        }

        if (assetCategoryId == null) {
          assetCategoryIdIsNullAmount += amountDelta;
          continue;
        }

        if (assetAmountUpdateMap.containsKey(assetCategoryId)) {
          Long mapAmountDelta = assetAmountUpdateMap.get(assetCategoryId);
          assetAmountUpdateMap.put(assetCategoryId, mapAmountDelta + amountDelta);
        } else {
          assetAmountUpdateMap.put(assetCategoryId, amountDelta);
        }
      }

      for (Map.Entry<Long, Long> assetAmountUpdate : assetAmountUpdateMap.entrySet()) {
        assetRepository.updateAmountByCategoryId(userId, assetAmountUpdate.getKey(), assetAmountUpdate.getValue());
      }

      if (!assetCategoryIdIsNullAmount.equals(0L)) {
        assetRepository.updateAmountByCategoryId(userId, null, assetCategoryIdIsNullAmount);
      }

      bankTransactionRepository.deleteByCsvImportHistoryId(userId,csvImportHistoryId);
    }

    csvImportHistoryRepository.deleteById(csvImportHistoryId, userId);
  }
}