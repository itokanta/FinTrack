package com.example.fintrack.service;

import com.example.fintrack.csv.*;
import com.example.fintrack.csv.dto.TransactionDTO;
import com.example.fintrack.csv.parser.CsvParser;
import com.example.fintrack.domain.*;
import com.example.fintrack.repository.*;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

/**
 * CSV取込処理全体の制御を行うサービス
 */
@Service
public class CsvImportService {
    
    private final CsvReader csvReader;
    private final CsvParserFactory csvParserFactory;
    private final TransactionNormalizer transactionNormalizer;
    private final FingerprintService fingerprintService;
    private final BankTransactionRepository bankTransactionRepository;
    private final CardTransactionRepository cardTransactionRepository;
    private final AutoCategoryRuleRepository autoCategoryRuleRepository;
    private final AssetService assetService;
    private final UserRepository userRepository;
    
    public CsvImportService(CsvReader csvReader,
                           CsvParserFactory csvParserFactory,
                           TransactionNormalizer transactionNormalizer,
                           FingerprintService fingerprintService,
                           BankTransactionRepository bankTransactionRepository,
                           CardTransactionRepository cardTransactionRepository,
                           AutoCategoryRuleRepository autoCategoryRuleRepository,
                           AssetService assetService,
                           UserRepository userRepository) {
        this.csvReader = csvReader;
        this.csvParserFactory = csvParserFactory;
        this.transactionNormalizer = transactionNormalizer;
        this.fingerprintService = fingerprintService;
        this.bankTransactionRepository = bankTransactionRepository;
        this.cardTransactionRepository = cardTransactionRepository;
        this.autoCategoryRuleRepository = autoCategoryRuleRepository;
        this.assetService = assetService;
        this.userRepository = userRepository;
    }
    
    /**
     * 銀行取引のCSVをインポート
     * 
     * @param file CSVファイル
     * @param bankType 金融機関種別
     * @param userId ユーザーID
     * @param hasHeader ヘッダー行の有無
     * @return インポートした件数
     */
    @Transactional
    public int importBankTransactions(MultipartFile file, BankType bankType, Long userId, boolean hasHeader) 
            throws IOException {
        
        // ユーザーの存在確認
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("ユーザーが見つかりません"));
        
        // CSVを読み込み
        List<CSVRecord> records = csvReader.read(file.getInputStream(), hasHeader);
        
        // パーサーを取得
        CsvParser parser = csvParserFactory.getParser(bankType);
        
        // 既存のfingerprintとカウントを取得
        Map<String, Long> existingFingerprints = getExistingBankFingerprints(userId);
        
        // CSVからDTOに変換し、fingerprintを生成
        Map<String, List<TransactionDTO>> newTransactions = new HashMap<>();
        for (CSVRecord record : records) {
            TransactionDTO dto = parser.parse(record);
            TransactionDTO normalized = transactionNormalizer.normalize(dto);
            String fingerprint = fingerprintService.generateFingerprint(normalized);
            
            newTransactions.computeIfAbsent(fingerprint, k -> new ArrayList<>()).add(normalized);
        }
        
        // 差分を抽出（新規のみ）
        List<TransactionDTO> toInsert = extractNewTransactions(existingFingerprints, newTransactions);
        
        // 自動カテゴリルールを取得
        List<AutoCategoryRule> rules = autoCategoryRuleRepository.findByUserId(userId);
        
        // インサート
        int count = 0;
        for (TransactionDTO dto : toInsert) {
            String fingerprint = fingerprintService.generateFingerprint(dto);
            
            // BankTransactionを作成
            BankTransaction transaction = new BankTransaction();
            transaction.setDescription(dto.getDescription());
            transaction.setAmount(dto.getAmount());
            transaction.setTransactionType(dto.getTransactionType());
            transaction.setTransactionDate(dto.getTransactionDate());
            transaction.setFingerprint(fingerprint);
            transaction.setUserId(userId);
            
            // 自動カテゴリルールを適用
            AssetCategory matchedCategory = findMatchingCategory(dto.getDescription(), rules);
            if (matchedCategory != null) {
                transaction.setAssetCategoryId(matchedCategory.getId());
            }
            
            BankTransaction saved = bankTransactionRepository.save(transaction);
            
            // 資産残高を更新
            Long assetCategoryId = saved.getAssetCategoryId();
            Long amountDelta;
            
            if (dto.getTransactionType() == TransactionType.INCOME) {
                // 収入の場合はプラス（分割前なので未分類に加算）
                amountDelta = dto.getAmount();
            } else {
                // 支出の場合はマイナス
                amountDelta = -dto.getAmount();
            }
            
            assetService.updateBalance(assetCategoryId, amountDelta);
            count++;
        }
        
        return count;
    }
    
    /**
     * カード取引のCSVをインポート
     * 
     * @param file CSVファイル
     * @param bankType 金融機関種別
     * @param userId ユーザーID
     * @param hasHeader ヘッダー行の有無
     * @return インポートした件数
     */
    @Transactional
    public int importCardTransactions(MultipartFile file, BankType bankType, Long userId, boolean hasHeader) 
            throws IOException {
        
        // ユーザーの存在確認
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("ユーザーが見つかりません"));
        
        // CSVを読み込み
        List<CSVRecord> records = csvReader.read(file.getInputStream(), hasHeader);
        
        // パーサーを取得
        CsvParser parser = csvParserFactory.getParser(bankType);
        
        // 既存のfingerprintとカウントを取得
        Map<String, Long> existingFingerprints = getExistingCardFingerprints(userId);
        
        // CSVからDTOに変換し、fingerprintを生成
        Map<String, List<TransactionDTO>> newTransactions = new HashMap<>();
        for (CSVRecord record : records) {
            TransactionDTO dto = parser.parse(record);
            TransactionDTO normalized = transactionNormalizer.normalize(dto);
            String fingerprint = fingerprintService.generateFingerprint(normalized);
            
            newTransactions.computeIfAbsent(fingerprint, k -> new ArrayList<>()).add(normalized);
        }
        
        // 差分を抽出（新規のみ）
        List<TransactionDTO> toInsert = extractNewTransactions(existingFingerprints, newTransactions);
        
        // 自動カテゴリルールを取得
        List<AutoCategoryRule> rules = autoCategoryRuleRepository.findByUserId(userId);
        
        // インサート
        int count = 0;
        for (TransactionDTO dto : toInsert) {
            String fingerprint = fingerprintService.generateFingerprint(dto);
            
            // CardTransactionを作成
            CardTransaction transaction = new CardTransaction();
            transaction.setDescription(dto.getDescription());
            transaction.setAmount(dto.getAmount());
            transaction.setTransactionType(dto.getTransactionType());
            transaction.setTransactionDate(dto.getTransactionDate());
            transaction.setFingerprint(fingerprint);
            transaction.setUserId(userId);
            
            // 自動カテゴリルールを適用
            AssetCategory matchedCategory = findMatchingCategory(dto.getDescription(), rules);
            if (matchedCategory != null) {
                transaction.setAssetCategoryId(matchedCategory.getId());
            }
            
            CardTransaction saved = cardTransactionRepository.save(transaction);
            
            // 資産残高を更新
            Long assetCategoryId = saved.getAssetCategoryId();
            Long amountDelta;
            
            if (dto.getTransactionType() == TransactionType.INCOME) {
                // 収入の場合はプラス（分割前なので未分類に加算）
                amountDelta = dto.getAmount();
            } else {
                // 支出の場合はマイナス
                amountDelta = -dto.getAmount();
            }
            
            assetService.updateBalance(assetCategoryId, amountDelta);
            count++;
        }
        
        return count;
    }
    
    /**
     * 既存の銀行取引のfingerprintとカウントを取得
     */
    private Map<String, Long> getExistingBankFingerprints(Long userId) {
        return bankTransactionRepository.countByFingerprintGrouped(userId);
    }
    
    /**
     * 既存のカード取引のfingerprintとカウントを取得
     */
    private Map<String, Long> getExistingCardFingerprints(Long userId) {
        return cardTransactionRepository.countByFingerprintGrouped(userId);
    }
    
    /**
     * 新規取引を抽出（既存との差分）
     */
    private List<TransactionDTO> extractNewTransactions(
            Map<String, Long> existingFingerprints,
            Map<String, List<TransactionDTO>> newTransactions) {
        
        List<TransactionDTO> result = new ArrayList<>();
        
        for (Map.Entry<String, List<TransactionDTO>> entry : newTransactions.entrySet()) {
            String fingerprint = entry.getKey();
            List<TransactionDTO> dtos = entry.getValue();
            
            long existingCount = existingFingerprints.getOrDefault(fingerprint, 0L);
            long newCount = dtos.size();
            
            // 差分がある場合のみ追加
            if (newCount > existingCount) {
                int toAdd = (int) (newCount - existingCount);
                result.addAll(dtos.subList(0, toAdd));
            }
        }
        
        return result;
    }
    
    /**
     * 摘要に一致する自動カテゴリルールを検索
     */
    private AssetCategory findMatchingCategory(String description, List<AutoCategoryRule> rules) {
        for (AutoCategoryRule rule : rules) {
            if (description.contains(rule.getDescriptionPattern())) {
                return rule.getAssetCategory();
            }
        }
        return null;
    }
}
