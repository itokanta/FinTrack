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
import com.example.fintrack.domain.DiscrepancyTransaction;
import com.example.fintrack.domain.SplitMethod;
import com.example.fintrack.domain.SplitTransaction;
import com.example.fintrack.domain.TransactionType;
import com.example.fintrack.domain.dto.CardTransactionUpdateDetail;
import com.example.fintrack.repository.AssetRepository;
import com.example.fintrack.repository.BankTransactionRepository;
import com.example.fintrack.repository.CardTransactionRepository;
import com.example.fintrack.repository.CsvImportHistoryRepository;
import com.example.fintrack.repository.CsvImportUpdateHistoryRepository;
import com.example.fintrack.repository.DiscrepancyTransactionRepository;
import com.example.fintrack.repository.SplitTransactionRepository;

@Service
public class CsvImportHistoryService {
  private final CsvImportHistoryRepository csvImportHistoryRepository;
  private final CsvImportUpdateHistoryRepository csvImportUpdateHistoryRepository;
  private final CardTransactionRepository cardTransactionRepository;
  private final BankTransactionRepository bankTransactionRepository;
  private final AssetRepository assetRepository;
  private final SplitTransactionRepository splitTransactionRepository;
  private final DiscrepancyTransactionRepository discrepancyTransactionRepository;

  public CsvImportHistoryService(CsvImportHistoryRepository csvImportHistoryRepository,
      CsvImportUpdateHistoryRepository csvImportUpdateHistoryRepository,
      CardTransactionRepository cardTransactionRepository, BankTransactionRepository bankTransactionRepository,
      AssetRepository assetRepository, SplitTransactionRepository splitTransactionRepository,
      DiscrepancyTransactionRepository discrepancyTransactionRepository) {
    this.csvImportHistoryRepository = csvImportHistoryRepository;
    this.csvImportUpdateHistoryRepository = csvImportUpdateHistoryRepository;
    this.cardTransactionRepository = cardTransactionRepository;
    this.bankTransactionRepository = bankTransactionRepository;
    this.assetRepository = assetRepository;
    this.splitTransactionRepository = splitTransactionRepository;
    this.discrepancyTransactionRepository = discrepancyTransactionRepository;
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
    Map<Long, Long> assetAmountUpdateMap = new HashMap<>();
    Long assetCategoryIdIsNullAmount = 0L;

    if (!deleteCardTransactionList.isEmpty()) {
      List<Long> isSplitCardTransactionIdList = new ArrayList<>();

      for (CardTransaction cardTransaction : deleteCardTransactionList) {
        Long assetCategoryId = cardTransaction.getAssetCategoryId();
        Long amountDelta;

        if (cardTransaction.getSplitMethod() == SplitMethod.MANUAL || cardTransaction.getSplitMethod() == SplitMethod.PATTERN){
          isSplitCardTransactionIdList.add(cardTransaction.getId());
          continue;
        }

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

      if (!isSplitCardTransactionIdList.isEmpty()) {
        List<SplitTransaction> splitTransactionList = splitTransactionRepository.findByCardTransactionIds(isSplitCardTransactionIdList);
        Long nullAmount = splitTransactionAmountRollback(splitTransactionList, assetAmountUpdateMap);
        assetCategoryIdIsNullAmount += nullAmount;
      }

      cardTransactionRepository.deleteByCsvImportHistoryId(userId, csvImportHistoryId);
    }

    if (!deleteBankTransactionList.isEmpty()) {
      List<Long> isCardPaymentIdList = new ArrayList<>();
      List<Long> isSplitBankTransactionIdList = new ArrayList<>();
      List<Long> isSplitDiscrepancyTransactionIdList = new ArrayList<>();

      for (BankTransaction bankTransaction : deleteBankTransactionList) {
        if (bankTransaction.getIsCardPayment() != null && bankTransaction.getIsCardPayment()) {
          isCardPaymentIdList.add(bankTransaction.getId());
          continue;
        }

        if (bankTransaction.getSplitMethod() == SplitMethod.MANUAL || bankTransaction.getSplitMethod() == SplitMethod.PATTERN) {
          isSplitBankTransactionIdList.add(bankTransaction.getId());
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

      if (!isCardPaymentIdList.isEmpty()){
        List<DiscrepancyTransaction> discrepancyTransactionList = discrepancyTransactionRepository.findByBankTransactionIds(isCardPaymentIdList, userId);

        for (DiscrepancyTransaction discrepancyTransaction : discrepancyTransactionList) {
          if (discrepancyTransaction.getSplitMethod() == SplitMethod.MANUAL || discrepancyTransaction.getSplitMethod() == SplitMethod.PATTERN){
            isSplitDiscrepancyTransactionIdList.add(discrepancyTransaction.getId());
            continue;
          }

          Long assetCategoryId = discrepancyTransaction.getAssetCategoryId();
          Long amountDelta;

          if (discrepancyTransaction.getTransactionType() == TransactionType.INCOME) {
            // 収入の場合はマイナス
            amountDelta = -discrepancyTransaction.getAmount();
          } else {
            // 支出の場合はプラス
            amountDelta = discrepancyTransaction.getAmount();
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
      }

      if (!isSplitBankTransactionIdList.isEmpty()) {
        List<SplitTransaction> splitTransactionList = splitTransactionRepository.findByBankTransactionIds(isSplitBankTransactionIdList);
        Long nullAmount = splitTransactionAmountRollback(splitTransactionList, assetAmountUpdateMap);
        assetCategoryIdIsNullAmount += nullAmount;
      }

      if (!isSplitDiscrepancyTransactionIdList.isEmpty()){
        List<SplitTransaction> splitTransactionList = splitTransactionRepository.findByDiscrepancyTransactionIds(isSplitDiscrepancyTransactionIdList);
        Long nullAmount = splitTransactionAmountRollback(splitTransactionList, assetAmountUpdateMap);
        assetCategoryIdIsNullAmount += nullAmount;
      }

      bankTransactionRepository.deleteByCsvImportHistoryId(userId,csvImportHistoryId);
    }

    for (Map.Entry<Long, Long> assetAmountUpdate : assetAmountUpdateMap.entrySet()) {
      assetRepository.updateAmountByCategoryId(userId, assetAmountUpdate.getKey(), assetAmountUpdate.getValue());
    }

    if (!assetCategoryIdIsNullAmount.equals(0L)) {
      assetRepository.updateAmountByCategoryId(userId, null, assetCategoryIdIsNullAmount);
    }

    csvImportHistoryRepository.deleteById(csvImportHistoryId, userId);
  }

  private Long splitTransactionAmountRollback(List<SplitTransaction> spListTransactionList, Map<Long, Long> assetAmountUpdateMap){
    Long assetCategoryIdIsNullAmount = 0L;

    for (SplitTransaction splitTransaction : spListTransactionList) {
      Long assetCategoryId = splitTransaction.getAssetCategoryId();
      Long amountDelta;

      if (splitTransaction.getTransactionType() == TransactionType.INCOME) {
        // 収入の場合はマイナス
        amountDelta = -splitTransaction.getAmount();
      } else {
        // 支出の場合はプラス
        amountDelta = splitTransaction.getAmount();
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

    return assetCategoryIdIsNullAmount;
  }
}