package com.example.fintrack.service;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.fintrack.csv.BankType;
import com.example.fintrack.csv.CsvParserFactory;
import com.example.fintrack.csv.CsvReader;
import com.example.fintrack.csv.FingerprintService;
import com.example.fintrack.csv.TransactionNormalizer;
import com.example.fintrack.csv.dto.TransactionDTO;
import com.example.fintrack.csv.parser.CsvParser;
import com.example.fintrack.domain.AutoCategoryRule;
import com.example.fintrack.domain.BankTransaction;
import com.example.fintrack.domain.CardPaymentDiscrepancy;
import com.example.fintrack.domain.CardTransaction;
import com.example.fintrack.domain.CsvImportHistory;
import com.example.fintrack.domain.CsvImportUpdateField;
import com.example.fintrack.domain.CsvImportUpdateHistory;
import com.example.fintrack.domain.SplitMethod;
import com.example.fintrack.domain.TransactionType;
import com.example.fintrack.repository.AssetRepository;
import com.example.fintrack.repository.AutoCategoryRuleRepository;
import com.example.fintrack.repository.BankTransactionRepository;
import com.example.fintrack.repository.CardPaymentDiscrepancyRepository;
import com.example.fintrack.repository.CardTransactionRepository;
import com.example.fintrack.repository.CsvImportHistoryRepository;
import com.example.fintrack.repository.CsvImportUpdateHistoryRepository;
import com.example.fintrack.repository.UserRepository;

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
    private final AssetRepository assetRepository;
    private final UserRepository userRepository;
    private final CardPaymentDiscrepancyRepository discrepancyRepository;
    private final CsvImportHistoryRepository csvImportHistoryRepository;
    private final CsvImportUpdateHistoryRepository csvImportUpdateHistoryRepository;
    public CsvImportService(CsvReader csvReader,
            CsvParserFactory csvParserFactory,
            TransactionNormalizer transactionNormalizer,
            FingerprintService fingerprintService,
            BankTransactionRepository bankTransactionRepository,
            CardTransactionRepository cardTransactionRepository,
            AutoCategoryRuleRepository autoCategoryRuleRepository,
            AssetRepository assetRepository,
            UserRepository userRepository,
            CardPaymentDiscrepancyRepository discrepancyRepository,
            CsvImportHistoryRepository csvImportHistoryRepository,
            CsvImportUpdateHistoryRepository csvImportUpdateHistoryRepository
        ) {
        this.csvReader = csvReader;
        this.csvParserFactory = csvParserFactory;
        this.transactionNormalizer = transactionNormalizer;
        this.fingerprintService = fingerprintService;
        this.bankTransactionRepository = bankTransactionRepository;
        this.cardTransactionRepository = cardTransactionRepository;
        this.autoCategoryRuleRepository = autoCategoryRuleRepository;
        this.assetRepository = assetRepository;
        this.userRepository = userRepository;
        this.discrepancyRepository = discrepancyRepository;
        this.csvImportHistoryRepository = csvImportHistoryRepository;
        this.csvImportUpdateHistoryRepository = csvImportUpdateHistoryRepository;
    }

    /**
     * 銀行取引のCSVをインポート
     * 
     * @param file      CSVファイル
     * @param bankType  金融機関種別
     * @param userId    ユーザーID
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

            TransactionDTO normalized = transactionNormalizer.normalize(dto);
            String fingerprint = fingerprintService.generateFingerprint(normalized);

            newTransactions.computeIfAbsent(fingerprint, k -> new ArrayList<>()).add(normalized);
        }

        // 差分を抽出（新規のみ）
        List<TransactionDTO> toInsert = extractNewTransactions(existingFingerprints, newTransactions);

        // 自動カテゴリルールを取得
        List<AutoCategoryRule> rules = autoCategoryRuleRepository.findByUserId(userId);

        CsvImportHistory csvImportHistory = new CsvImportHistory();
        csvImportHistory.setUserId(userId);
        csvImportHistory.setImportType("BANK");
        csvImportHistory.setCsvName(file.getOriginalFilename());
        csvImportHistory.setRecordCount((long) toInsert.size());
        Long csvImportHistoryId = csvImportHistoryRepository.save(csvImportHistory);

        // インサート
        int count = 0;
        List <BankTransaction> toInsertBankTransactionList = new ArrayList<>();
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
            transaction.setCsvImportHistoryId(csvImportHistoryId);

            if (dto.getDescription() != null
                    && (dto.getDescription().contains("ラクテンカ－ドサービス") || dto.getDescription().contains("ラクテンカ−ドサ−ビ")
                            || dto.getDescription().contains("ラクテンカ−ト゛サ−ヒ゛ス"))) {
                transaction.setIsCardPayment(true);
            } else {
                transaction.setIsCardPayment(false);
            }

            // 自動カテゴリルールを適用
            Long matchedCategoryId = findMatchingCategoryId(dto.getDescription(), dto.getTransactionType(), rules);
            if (matchedCategoryId != null) {
                transaction.setAssetCategoryId(matchedCategoryId);
                transaction.setSplitPatternId(null);
                transaction.setSplitMethod(SplitMethod.SINGLE);
            }

            toInsertBankTransactionList.add(transaction);
        }
        count += toInsertBankTransactionList.size();
        bankTransactionRepository.insert(toInsertBankTransactionList);

        List<BankTransaction> bankTransactionsForImportList = bankTransactionRepository.findByCsvImportHistoryId(userId, csvImportHistoryId);

        Map<Long, Long> assetAmountUpdateMap = new HashMap<>();
        List<BankTransaction> cardPaymentIsTrueList = new ArrayList<>();
        Long assetCategoryIdIsNullAmount = 0L;

        for (BankTransaction bankTransactionForImport : bankTransactionsForImportList){
            if (bankTransactionForImport.getIsCardPayment() != null && bankTransactionForImport.getIsCardPayment()){
                cardPaymentIsTrueList.add(bankTransactionForImport);
                continue;
            }

            Long assetCategoryId = bankTransactionForImport.getAssetCategoryId();
            Long amountDelta;

            if (bankTransactionForImport.getTransactionType() == TransactionType.INCOME) {
                // 収入の場合はプラス（分割前なので未分類に加算）
                amountDelta = bankTransactionForImport.getAmount();
            } else {
                // 支出の場合はマイナス
                amountDelta = -bankTransactionForImport.getAmount();
            }

            if (assetCategoryId == null){
                assetCategoryIdIsNullAmount += amountDelta;
                continue;
            }

            if (assetAmountUpdateMap.containsKey(assetCategoryId)){
                Long mapAmountDelta = assetAmountUpdateMap.get(assetCategoryId);
                assetAmountUpdateMap.put(assetCategoryId, mapAmountDelta + amountDelta);
            } else {
                assetAmountUpdateMap.put(assetCategoryId, amountDelta);
            }
        }

        for (Map.Entry<Long, Long> assetAmountUpdate : assetAmountUpdateMap.entrySet()){
            assetRepository.updateAmountByCategoryId(userId, assetAmountUpdate.getKey(), assetAmountUpdate.getValue());
        }

        if (!assetCategoryIdIsNullAmount.equals(0L)){
            assetRepository.updateAmountByCategoryId(userId, null, assetCategoryIdIsNullAmount);
        }

        for (BankTransaction cardPaymentIsTrue : cardPaymentIsTrueList) {
            String paymentMonth = cardPaymentIsTrue.getTransactionDate()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM"));

            // カード利用額の合計を取得
            Long expectedAmount = cardTransactionRepository.sumAmountByUserIdAndPaymentMonth(userId, paymentMonth);
            Long actualAmount = cardPaymentIsTrue.getAmount();

            if (expectedAmount == null) {
                expectedAmount = 0L;
            }

            // 金額の照合
            if (expectedAmount.equals(actualAmount)) {
                List<CardTransaction> beforeCardTransactionList = cardTransactionRepository.findByUserIdAndPaymentMonth(userId, paymentMonth);
                Map<Long, CardTransaction> beforeByIdMap = new HashMap<>();
                for (CardTransaction beforeCardTransaction : beforeCardTransactionList) {
                    beforeByIdMap.put(beforeCardTransaction.getId(), beforeCardTransaction);
                }
                // 金額が一致する場合のみ is_paid を更新
                cardTransactionRepository.markAsPaidByPaymentMonth(userId, paymentMonth);

                List<CardTransaction> afterCardTransactionList = cardTransactionRepository.findByUserIdAndPaymentMonth(userId, paymentMonth);
                List<CsvImportUpdateHistory> csvImportUpdateHistoryList = new ArrayList<>();

                for (CardTransaction afterCardTransaction : afterCardTransactionList) {
                    CardTransaction beforeTargetCardTransaction = beforeByIdMap.get(afterCardTransaction.getId());
                    if (beforeTargetCardTransaction == null) {
                        throw new IllegalStateException("before transaction not found. id=" + afterCardTransaction.getId());
                    }
                    CsvImportUpdateHistory csvImportUpdateHistory = new CsvImportUpdateHistory();
                    csvImportUpdateHistory.setUserId(userId);
                    csvImportUpdateHistory.setCsvImportHistoryId(csvImportHistoryId);
                    csvImportUpdateHistory.setCardTransactionId(afterCardTransaction.getId());
                    csvImportUpdateHistory.setTargetField(CsvImportUpdateField.IS_PAID);
                    csvImportUpdateHistory.setUpdateCount(afterCardTransaction.getUpdateCount());
                    if (beforeTargetCardTransaction.getIsPaid() != null){
                        csvImportUpdateHistory.setValueBefore(beforeTargetCardTransaction.getIsPaid().toString());
                    }
                    csvImportUpdateHistory.setValueAfter(afterCardTransaction.getIsPaid().toString());

                    csvImportUpdateHistoryList.add(csvImportUpdateHistory);
                }
                csvImportUpdateHistoryRepository.insert(csvImportUpdateHistoryList);
            } else {
                // 差異がある場合、差異データを登録
                Long differenceAmount = actualAmount - expectedAmount;

                CardPaymentDiscrepancy discrepancy = new CardPaymentDiscrepancy();
                discrepancy.setUserId(userId);
                discrepancy.setPaymentMonth(paymentMonth);
                discrepancy.setBankTransactionId(cardPaymentIsTrue.getId());
                discrepancy.setExpectedAmount(expectedAmount);
                discrepancy.setActualAmount(actualAmount);
                discrepancy.setDifferenceAmount(differenceAmount);
                discrepancy.setIsResolved(false);

                discrepancyRepository.save(discrepancy);
            }
        }

        return count;

    }

    /**
     * カード取引のCSVをインポート
     * 
     * @param file      CSVファイル
     * @param bankType  金融機関種別
     * @param userId    ユーザーID
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

            // 同じfingerprintを持つトランザクションをリストにまとめる
            // computeIfAbsent: fingerprintが初出なら新しいArrayListを作成、既存なら既存のListを取得
            // 同じ取引（例：毎月のサブスク）が複数ある場合、同じキーの下でリスト化される
            newTransactions.computeIfAbsent(fingerprint, k -> new ArrayList<>()).add(normalized);
        }

        // 差分を抽出（新規のみ）
        List<TransactionDTO> toInsert = extractNewTransactions(existingFingerprints, newTransactions);

        // paymentMonthが変更されたトランザクションを抽出して更新
        List<CardTransaction> existingTransactions = cardTransactionRepository.findAllByUserId(userId);

        Map<Long, String> paymentMonthBeforeMap = new HashMap<>();
        for (CardTransaction cardTransaction : existingTransactions){
            paymentMonthBeforeMap.put(cardTransaction.getId(),  cardTransaction.getPaymentMonth());
        }

        List<CardTransaction> toUpdate = extractTransactionsToUpdate(newTransactions, existingTransactions);

        // 自動カテゴリルールを取得
        List<AutoCategoryRule> rules = autoCategoryRuleRepository.findByUserId(userId);

        CsvImportHistory csvImportHistory = new CsvImportHistory();
        csvImportHistory.setUserId(userId);
        csvImportHistory.setImportType("CARD");
        csvImportHistory.setCsvName(file.getOriginalFilename());
        csvImportHistory.setRecordCount((long) toInsert.size() + (long) toUpdate.size());
        Long csvImportHistoryId = csvImportHistoryRepository.save(csvImportHistory);

        // インサート
        List<CardTransaction> toInsertCardTransactionList = new ArrayList<>();
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
            transaction.setCsvImportHistoryId(csvImportHistoryId);

            // カード種別を設定
            if (bankType == BankType.RAKUTEN_DEBIT) {

                transaction.setCardType("DEBIT");
                transaction.setIsPaid(null); // デビットは管理対象外
            } else {
                transaction.setCardType("CREDIT");
                transaction.setIsPaid(false); // クレジットは新規登録時は未払い
            }

            // 支払い月を設定
            transaction.setPaymentMonth(dto.getPaymentMonth());

            // 自動カテゴリルールを適用
            Long matchedCategoryId = findMatchingCategoryId(dto.getDescription(), dto.getTransactionType(), rules);
            if (matchedCategoryId != null) {
                transaction.setAssetCategoryId(matchedCategoryId);
                transaction.setSplitPatternId(null);
                transaction.setSplitMethod(SplitMethod.SINGLE);
            }

            toInsertCardTransactionList.add(transaction);
        }
        cardTransactionRepository.insert(toInsertCardTransactionList);

        List<CardTransaction> cardTransactionsForImportList = cardTransactionRepository.findByCsvImportHistoryId(userId, csvImportHistoryId);

        Map<Long, Long> assetAmountUpdateMap = new HashMap<>();
        Long assetCategoryIdIsNullAmount = 0L;

        for (CardTransaction cardTransactionForImport : cardTransactionsForImportList){
            Long assetCategoryId = cardTransactionForImport.getAssetCategoryId();
            Long amountDelta;

            if (cardTransactionForImport.getTransactionType() == TransactionType.INCOME) {
                // 収入の場合はプラス（分割前なので未分類に加算）
                amountDelta = cardTransactionForImport.getAmount();
            } else {
                // 支出の場合はマイナス
                amountDelta = -cardTransactionForImport.getAmount();
            }

            if (assetCategoryId == null){
                assetCategoryIdIsNullAmount += amountDelta;
                continue;
            }

            if (assetAmountUpdateMap.containsKey(assetCategoryId)){
                Long mapAmountDelta = assetAmountUpdateMap.get(assetCategoryId);
                assetAmountUpdateMap.put(assetCategoryId, mapAmountDelta + amountDelta);
            } else {
                assetAmountUpdateMap.put(assetCategoryId, amountDelta);
            }
        }

        for (Map.Entry<Long, Long> assetAmountUpdate : assetAmountUpdateMap.entrySet()){
            assetRepository.updateAmountByCategoryId(userId, assetAmountUpdate.getKey(), assetAmountUpdate.getValue());
        }

        if (!assetCategoryIdIsNullAmount.equals(0L)){
            assetRepository.updateAmountByCategoryId(userId, null, assetCategoryIdIsNullAmount);
        }

        // paymentMonthが変更されたトランザクションを更新
        // extractTransactionsToUpdateメソッドで既にpaymentMonthは更新済み
        List<CsvImportUpdateHistory> csvImportUpdateHistoryList = new ArrayList<>();

        for (CardTransaction transaction : toUpdate) {
            CsvImportUpdateHistory csvImportUpdateHistory = new CsvImportUpdateHistory();
            csvImportUpdateHistory.setUserId(userId);
            csvImportUpdateHistory.setCsvImportHistoryId(csvImportHistoryId);
            csvImportUpdateHistory.setCardTransactionId(transaction.getId());
            csvImportUpdateHistory.setTargetField(CsvImportUpdateField.PAYMENT_MONTH);
            csvImportUpdateHistory.setUpdateCount(transaction.getUpdateCount() + 1);
            csvImportUpdateHistory.setValueBefore(paymentMonthBeforeMap.get(transaction.getId()));
            csvImportUpdateHistory.setValueAfter(transaction.getPaymentMonth());

            csvImportUpdateHistoryList.add(csvImportUpdateHistory);
        }

        csvImportUpdateHistoryRepository.insert(csvImportUpdateHistoryList);
        cardTransactionRepository.updatePaymentMonth(toUpdate);

        return toInsertCardTransactionList.size() + toUpdate.size();
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
     * 
     * <p>
     * 同じfingerprintを持つトランザクションの件数を比較し、CSV側が多い場合のみ
     * 差分を新規追加対象として抽出します。これにより、重複したCSVレコードや
     * 既にインポート済みのレコードの重複登録を防ぎます。
     * </p>
     * 
     * <h3>具体例：</h3>
     * 
     * <pre>
     * 既存のDB: fingerprint="abc123" が3件
     * CSV: fingerprint="abc123" が5件
     * 結果: 差分の2件のみを新規追加対象として返す（5 - 3 = 2）
     * </pre>
     * 
     * @param existingFingerprints 既存のfingerprintと件数のMap（key: fingerprint, value:
     *                             件数）
     * @param newTransactions      CSVから読み込んだトランザクションのMap（key: fingerprint, value:
     *                             DTOリスト）
     * @return 新規追加すべきトランザクションのリスト
     */
    private List<TransactionDTO> extractNewTransactions(
            Map<String, Long> existingFingerprints,
            Map<String, List<TransactionDTO>> newTransactions) {

        List<TransactionDTO> result = new ArrayList<>();

        for (Map.Entry<String, List<TransactionDTO>> entry : newTransactions.entrySet()) {
            String fingerprint = entry.getKey();
            List<TransactionDTO> dtos = entry.getValue();

            // DB上の同じfingerprintの件数を取得（存在しない場合は0）
            long existingCount = existingFingerprints.getOrDefault(fingerprint, 0L);
            long newCount = dtos.size();

            // CSV側の件数が多い場合、差分を新規追加対象とする
            if (newCount > existingCount) {
                int toAdd = (int) (newCount - existingCount);
                // リストの先頭から差分の件数分を取得
                result.addAll(dtos.subList(0, toAdd));
            }
        }

        return result;
    }

    /**
     * paymentMonthが変更されたトランザクションを抽出して更新する
     * 
     * <p>
     * このメソッドは以下の2段階処理を行います：
     * </p>
     * <ol>
     * <li>Phase 1: DB側とCSV側でpaymentMonthが一致するものを照合し、処理済みとしてマーク</li>
     * <li>Phase 2: 一致しなかったものに対して、未処理のCSVデータからpaymentMonthを更新</li>
     * </ol>
     * 
     * <p>
     * 同じfingerprintを持つ複数のトランザクション（例：毎月のサブスクリプション）が存在する場合、
     * 順番に1対1で対応付けを行います。処理済みのCSVデータは次の照合では使用されないため、
     * 正確な対応付けが保証されます。
     * </p>
     * 
     * <h3>具体例：</h3>
     * 
     * <pre>
     * DB側:
     *   - Record 1: fingerprint="abc123", paymentMonth="2026-06"
     *   - Record 2: fingerprint="abc123", paymentMonth="2026-05"
     * 
     * CSV側:
     *   - Record 1: fingerprint="abc123", paymentMonth="2026-07" (変更あり)
     *   - Record 2: fingerprint="abc123", paymentMonth="2026-06" (変更あり)
     * 
     * 結果:
     *   - DB Record 1: 2026-06 → 2026-07 に更新
     *   - DB Record 2: 2026-05 → 2026-06 に更新
     * </pre>
     * 
     * @param newTransactions      CSVから読み込んだトランザクションのMap（key: fingerprint, value:
     *                             DTOリスト）
     * @param existingTransactions DB上の既存トランザクションのリスト
     * @return paymentMonthが更新されたトランザクションのリスト（setPaymentMonth済み）
     */
    private List<CardTransaction> extractTransactionsToUpdate(
            Map<String, List<TransactionDTO>> newTransactions,
            List<CardTransaction> existingTransactions) {
        List<CardTransaction> result = new ArrayList<>();

        // 各fingerprintについて、どのCSVデータ（インデックス）を処理済みかを管理
        // Set<Integer>を使用してO(1)の高速な検索を実現
        Map<String, Set<Integer>> processedIndexMap = new HashMap<>();

        // ==================== Phase 1: 一致する照合 ====================
        // DB側の各トランザクションに対して、CSV側でpaymentMonthが一致するものを探す
        // 一致したものは「変更なし」として処理済みマークし、更新対象から除外する
        for (CardTransaction existingTransaction : existingTransactions) {
            String fingerprint = existingTransaction.getFingerprint();
            boolean isProcessed = false;
            if (newTransactions.containsKey(fingerprint)) {
                List<TransactionDTO> newTransactionList = newTransactions.get(fingerprint);

                // このfingerprintで処理済みのインデックスを取得（なければ新規作成）
                // computeIfAbsentでNullPointerExceptionを防ぐ
                Set<Integer> processedIndexSet = processedIndexMap.computeIfAbsent(fingerprint, k -> new HashSet<>());

                // CSV側のリストを順番にチェック
                for (int i = 0; i < newTransactionList.size(); i++) {
                    // 既に他のトランザクションで使用済みの場合はスキップ
                    if (processedIndexSet.contains(i)) {
                        continue;
                    }

                    // paymentMonthが一致する場合
                    if (Objects.equals(existingTransaction.getPaymentMonth(),
                            newTransactionList.get(i).getPaymentMonth())) {
                        processedIndexSet.add(i);
                        isProcessed = true;
                        break;
                    }
                }
                if (!isProcessed) {
                    result.add(existingTransaction);
                }
            }
        }

        // ==================== Phase 2: paymentMonthの更新 ====================
        // Phase 1で一致しなかったトランザクションに対して、未処理のCSVデータからpaymentMonthを設定
        for (CardTransaction unprocessed : result) {
            String fingerprint = unprocessed.getFingerprint();

            if (newTransactions.containsKey(fingerprint)) {
                List<TransactionDTO> newTransactionList = newTransactions.get(fingerprint);

                // 処理済みインデックスを取得（Phase 1で必ず作成済み）
                Set<Integer> processedIndexSet = processedIndexMap.computeIfAbsent(fingerprint, k -> new HashSet<>());

                // 未処理の最初のCSVデータを使用して更新
                for (int i = 0; i < newTransactionList.size(); i++) {
                    if (processedIndexSet.contains(i)) {
                        continue; // 既に使用済み
                    }

                    // CSV側の新しいpaymentMonthを設定
                    unprocessed.setPaymentMonth(newTransactionList.get(i).getPaymentMonth());
                    processedIndexSet.add(i); // 処理済みとしてマーク
                    break; // 1対1対応なので、設定したら次のトランザクションへ
                }
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
            if (rule.getTransactionType() == transactionType && description.equals(rule.getDescriptionPattern())) {
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
