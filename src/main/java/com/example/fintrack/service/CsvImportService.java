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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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
        
        // エンコーディングを決定
        Charset charset = getCharsetForBankType(bankType);
        
        // CSVを読み込み（スキップ行数を考慮）
        List<CSVRecord> records = csvReader.read(file.getInputStream(), hasHeader, charset, bankType.getSkipLines());
        
        // パーサーを取得
        CsvParser parser = csvParserFactory.getParser(bankType);
        
        // 既存のfingerprintとカウントを取得
        Map<String, Long> existingFingerprints = getExistingBankFingerprints(userId);
        
        // CSVからDTOに変換し、fingerprintを生成
        Map<String, List<TransactionDTO>> newTransactions = new HashMap<>();
        for (CSVRecord record : records) {
            TransactionDTO dto = parser.parse(record);
            
            // パーサーがnullを返した場合はスキップ（除外対象）
            if (dto == null) {
                continue;
            }
            
            // 「ラクテンカ－ドサ－ビス」を除外
            if (dto.getDescription() != null && dto.getDescription().contains("ラクテンカ－ドサ－ビス")) {
                continue;
            }
            
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
            transaction.setTransactionSource("BANK");
            
            // 自動カテゴリルールを適用
            Long matchedCategoryId = findMatchingCategoryId(dto.getDescription(), dto.getTransactionType(), rules);
            if (matchedCategoryId != null) {
                transaction.setAssetCategoryId(matchedCategoryId);
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
            
            assetService.updateBalance(userId, assetCategoryId, amountDelta);
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
        
        // エンコーディングを決定
        Charset charset = getCharsetForBankType(bankType);
        
        // CSVを読み込み（スキップ行数を考慮）
        List<CSVRecord> records = csvReader.read(file.getInputStream(), hasHeader, charset, bankType.getSkipLines());
        
        // パーサーを取得
        CsvParser parser = csvParserFactory.getParser(bankType);
        
        // 既存のfingerprintとカウントを取得
        Map<String, Long> existingFingerprints = getExistingCardFingerprints(userId);
        
        // CSVからDTOに変換し、fingerprintを生成
        Map<String, List<TransactionDTO>> newTransactions = new HashMap<>();
        for (CSVRecord record : records) {
            TransactionDTO dto = parser.parse(record);
            
            // パーサーがnullを返した場合はスキップ（除外対象）
            if (dto == null) {
                continue;
            }
            
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
            transaction.setTransactionSource("CARD");
            
            // カード種別を設定
            if (bankType == BankType.RAKUTEN_DEBIT) {
                transaction.setCardType("DEBIT");
            } else {
                transaction.setCardType("CREDIT");
            }
            
            // 自動カテゴリルールを適用
            Long matchedCategoryId = findMatchingCategoryId(dto.getDescription(), dto.getTransactionType(), rules);
            if (matchedCategoryId != null) {
                transaction.setAssetCategoryId(matchedCategoryId);
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
            
            assetService.updateBalance(userId, assetCategoryId, amountDelta);
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
     * 摘要と収支区分に一致する自動カテゴリルールを検索
     */
    private Long findMatchingCategoryId(String description, TransactionType transactionType, List<AutoCategoryRule> rules) {
        for (AutoCategoryRule rule : rules) {
            // 収支区分が一致し、摘要が完全一致する場合
            if (rule.getTransactionType() == transactionType && 
                description.equals(rule.getDescriptionPattern())) {
                return rule.getAssetCategoryId();
            }
        }
        return null;
    }
    
    /**
     * 金融機関種別に応じたエンコーディングを取得
     */
    private Charset getCharsetForBankType(BankType bankType) {
        return switch (bankType) {
            case RAKUTEN_BANK, RAKUTEN_DEBIT, MIZUHO_BANK, MUFG_BANK -> 
                Charset.forName("Shift_JIS");
            case RAKUTEN_CARD -> 
                StandardCharsets.UTF_8;
        };
    }
}
