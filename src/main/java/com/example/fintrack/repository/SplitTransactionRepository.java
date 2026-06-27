package com.example.fintrack.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import com.example.fintrack.domain.SplitMethod;
import com.example.fintrack.domain.SplitTransaction;
import com.example.fintrack.domain.TransactionType;
import com.example.fintrack.domain.dto.MonthlySummary;
import com.example.fintrack.domain.dto.TransactionEntry;
import com.example.fintrack.repository.extractor.MonthlySummaryByAssetCategoryExtractor;
import com.example.fintrack.repository.rowmapper.MonthlySummaryRowMapper;
import com.example.fintrack.repository.rowmapper.TransactionEntryRowMapper;

@Repository
public class SplitTransactionRepository {

    private static final RowMapper<SplitTransaction> ROW_MAPPER = (rs, rowNum) -> {
        SplitTransaction splitTransaction = new SplitTransaction();
        splitTransaction.setId(rs.getLong("id"));
        String transactionType = rs.getString("transaction_type");
        splitTransaction.setTransactionType(transactionType == null ? null : TransactionType.valueOf(transactionType));
        long assetCategoryId = rs.getLong("asset_category_id");
        splitTransaction.setAssetCategoryId(rs.wasNull() ? null : assetCategoryId);
        long splitPatternId = rs.getLong("split_pattern_id");
        splitTransaction.setSplitPatternId(rs.wasNull() ? null : splitPatternId);
        String sm = rs.getString("split_method");
        splitTransaction.setSplitMethod(sm == null ? null : SplitMethod.valueOf(sm));
        splitTransaction.setAmount(rs.getLong("amount"));
        long bankTransactionId = rs.getLong("bank_transaction_id");
        splitTransaction.setBankTransactionId(rs.wasNull() ? null : bankTransactionId);
        long cardTransactionId = rs.getLong("card_transaction_id");
        splitTransaction.setCardTransactionId(rs.wasNull() ? null : cardTransactionId);
        long discrepancyTransactionId = rs.getLong("discrepancy_transaction_id");
        splitTransaction.setDiscrepancyTransactionId(rs.wasNull() ? null : discrepancyTransactionId);
        splitTransaction.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        splitTransaction.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        splitTransaction.setUserId(rs.getLong("user_id"));
        return splitTransaction;
    };

    private static final TransactionEntryRowMapper BASE_MAPPER = new TransactionEntryRowMapper();

    private static final RowMapper<TransactionEntry> TRANSACTION_ENTRY_ROW_MAPPER = (rs, rowNum) -> {
        TransactionEntry transactionEntry = BASE_MAPPER.mapRow(rs, rowNum);
        long bankTransactionId = rs.getLong("bank_transaction_id");
        Long bankId = rs.wasNull() ? null : bankTransactionId;
        long cardTransactionId = rs.getLong("card_transaction_id");
        Long cardId = rs.wasNull() ? null : cardTransactionId;
        long discrepancyTransactionId = rs.getLong("discrepancy_transaction_id");
        Long discrepancyId = rs.wasNull() ? null : discrepancyTransactionId;

        if (bankId != null) {
                transactionEntry.setTransactionSource("銀行");
        } else if (cardId != null) {
                String cardType = rs.getString("card_type");
                transactionEntry.setTransactionSource(switch (cardType){
                        case "DEBIT" -> "デビットカード";
                        case "CREDIT" -> "クレジットカード";
                        default -> "カード";
                });
        } else if (discrepancyId != null) {
                transactionEntry.setTransactionSource("差異取引");
        }
        return transactionEntry;
    };

    private static final MonthlySummaryRowMapper MONTHLY_SUMMARY_ROW_MAPPER = new MonthlySummaryRowMapper();

    private static final MonthlySummaryByAssetCategoryExtractor MONTHLY_SUMMARY_BY_ASSET_CATEGORY_EXTRACTOR =
            new MonthlySummaryByAssetCategoryExtractor();

    private final NamedParameterJdbcTemplate template;

    public SplitTransactionRepository(NamedParameterJdbcTemplate template) {
        this.template = template;
    }

    public List<SplitTransaction> findByBankTransactionIds(List<Long> bankTransactionIds, Long userId) {
        if (bankTransactionIds.isEmpty())
            return List.of();
        String sql = "SELECT id, transaction_type, asset_category_id, split_pattern_id, split_method, amount, " +
                "bank_transaction_id, card_transaction_id, discrepancy_transaction_id, created_at, updated_at, user_id " +
                "FROM split_transactions " +
                "WHERE bank_transaction_id IN (:ids) AND user_id = :userId";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ids", bankTransactionIds)
                .addValue("userId", userId);
        return template.query(sql, params, ROW_MAPPER);
    }

    public List<SplitTransaction> findByCardTransactionIds(List<Long> cardTransactionIds, Long userId) {
        if (cardTransactionIds.isEmpty())
            return List.of();
        String sql = "SELECT id, transaction_type, asset_category_id, split_pattern_id, split_method, amount, " +
                "bank_transaction_id, card_transaction_id, discrepancy_transaction_id, created_at, updated_at, user_id " +
                "FROM split_transactions " +
                "WHERE card_transaction_id IN (:ids) AND user_id = :userId";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ids", cardTransactionIds)
                .addValue("userId", userId);
        return template.query(sql, params, ROW_MAPPER);
    }

    public List<SplitTransaction> findByDiscrepancyTransactionIds(List<Long> discrepancyTransactionIds, Long userId) {
        if (discrepancyTransactionIds.isEmpty()) {
            return List.of();
        }
        String sql = "SELECT id, transaction_type, asset_category_id, split_pattern_id, split_method, amount, " +
                "bank_transaction_id, card_transaction_id, discrepancy_transaction_id, created_at, updated_at, user_id " +
                "FROM split_transactions " +
                "WHERE discrepancy_transaction_id IN (:ids) AND user_id = :userId";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ids", discrepancyTransactionIds)
                .addValue("userId", userId);
        return template.query(sql, params, ROW_MAPPER);
    }

    public List<SplitTransaction> findByBankTransactionId(Long bankTransactionId, Long userId) {
        String sql = "SELECT id, transaction_type, asset_category_id, split_pattern_id, split_method, amount, " +
                "bank_transaction_id, card_transaction_id, discrepancy_transaction_id, created_at, updated_at, user_id " +
                "FROM split_transactions " +
                "WHERE bank_transaction_id = :bankTransactionId AND user_id = :userId";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("bankTransactionId", bankTransactionId)
                .addValue("userId", userId);
        return template.query(sql, params, ROW_MAPPER);
    }

    public List<SplitTransaction> findByCardTransactionId(Long cardTransactionId, Long userId) {
        String sql = "SELECT id, transaction_type, asset_category_id, split_pattern_id, split_method, amount, " +
                "bank_transaction_id, card_transaction_id, discrepancy_transaction_id, created_at, updated_at, user_id " +
                "FROM split_transactions " +
                "WHERE card_transaction_id = :cardTransactionId AND user_id = :userId";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("cardTransactionId", cardTransactionId)
                .addValue("userId", userId);
        return template.query(sql, params, ROW_MAPPER);
    }

    public void insert(List<SplitTransaction> splitTransactionList) {
        String sql = "INSERT INTO split_transactions " +
                "(transaction_type, asset_category_id, split_pattern_id, " +
                "split_method, amount, bank_transaction_id, card_transaction_id, " +
                "discrepancy_transaction_id, user_id) " +
                "VALUES (:transactionType, :assetCategoryId, :splitPatternId, " +
                ":splitMethod, :amount, :bankTransactionId, :cardTransactionId, " +
                ":discrepancyTransactionId, :userId)";
        
        SqlParameterSource[] batchParams = new SqlParameterSource[splitTransactionList.size()];
        for (int i = 0; i < splitTransactionList.size(); i++){
            SplitTransaction splitTransaction = splitTransactionList.get(i);
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("transactionType",
                            splitTransaction.getTransactionType() == null ? null
                                    : splitTransaction.getTransactionType().name())
                    .addValue("assetCategoryId", splitTransaction.getAssetCategoryId())
                    .addValue("splitPatternId", splitTransaction.getSplitPatternId())
                    .addValue("splitMethod",
                            splitTransaction.getSplitMethod() == null ? null : splitTransaction.getSplitMethod().name())
                    .addValue("amount", splitTransaction.getAmount())
                    .addValue("bankTransactionId", splitTransaction.getBankTransactionId())
                    .addValue("cardTransactionId", splitTransaction.getCardTransactionId())
                    .addValue("discrepancyTransactionId", splitTransaction.getDiscrepancyTransactionId())
                    .addValue("userId", splitTransaction.getUserId());
            batchParams[i] = params;
        }

        template.batchUpdate(sql, batchParams);
    }

    public MonthlySummary sumAmountFromBankSplits(Long userId, LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT " +
                "COALESCE(SUM(CASE WHEN st.transaction_type = 'INCOME' THEN st.amount END), 0) AS income_amount, " +
                "COALESCE(SUM(CASE WHEN st.transaction_type = 'EXPENSE' THEN st.amount END), 0) AS expense_amount " +
                "FROM split_transactions st " +
                "INNER JOIN bank_transactions bt ON st.bank_transaction_id = bt.id " +
                "WHERE st.user_id = :userId AND bt.user_id = :userId " +
                "AND st.split_method IN ('MANUAL', 'PATTERN') " +
                "AND bt.transaction_date BETWEEN :startDate AND :endDate " +
                "AND bt.is_card_payment = FALSE";
        return template.queryForObject(sql, makeParam(userId, startDate, endDate), MONTHLY_SUMMARY_ROW_MAPPER);
    }

    public MonthlySummary sumAmountFromCardSplits(Long userId, LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT " +
                "COALESCE(SUM(CASE WHEN st.transaction_type = 'INCOME' THEN st.amount END), 0) AS income_amount, "+
                "COALESCE(SUM(CASE WHEN st.transaction_type = 'EXPENSE' THEN st.amount END), 0) AS expense_amount " +
                "FROM split_transactions st " +
                "INNER JOIN card_transactions ct ON st.card_transaction_id = ct.id " +
                "WHERE st.user_id = :userId AND ct.user_id = :userId " +
                "AND st.split_method IN ('MANUAL', 'PATTERN') " +
                "AND ct.transaction_date BETWEEN :startDate AND :endDate";
        return template.queryForObject(sql, makeParam(userId, startDate, endDate), MONTHLY_SUMMARY_ROW_MAPPER);
    }

    public MonthlySummary sumAmountFromDiscrepancySplits(Long userId, LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT " +
                "COALESCE(SUM(CASE WHEN st.transaction_type = 'INCOME' THEN st.amount END), 0) AS income_amount, " +
                "COALESCE(SUM(CASE WHEN st.transaction_type = 'EXPENSE' THEN st.amount END), 0) AS expense_amount " +
                "FROM split_transactions st " +
                "INNER JOIN discrepancy_transactions dt ON st.discrepancy_transaction_id = dt.id " +
                "WHERE st.user_id = :userId AND dt.user_id = :userId " +
                "AND st.split_method IN ('MANUAL', 'PATTERN') " +
                "AND dt.transaction_date BETWEEN :startDate AND :endDate";
        return template.queryForObject(sql, makeParam(userId, startDate, endDate), MONTHLY_SUMMARY_ROW_MAPPER);
    }

    public List<TransactionEntry> findByUserIdAndTransactionDateFromBankSplits(Long userId, TransactionType transactionType,
                    LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT st.id, st.transaction_type, st.asset_category_id, st.split_pattern_id, st.split_method, st.amount, st.bank_transaction_id, " +
                "st.card_transaction_id, st.discrepancy_transaction_id, st.created_at, st.updated_at, st.user_id, bt.description, bt.transaction_date, bt.expense_category_id " +
                "FROM split_transactions st " +
                "INNER JOIN bank_transactions bt ON st.bank_transaction_id = bt.id " +
                "WHERE st.user_id = :userId AND bt.user_id = :userId " +
                "AND st.transaction_type = :transactionType " +
                "AND st.split_method IN ('MANUAL', 'PATTERN') " +
                "AND bt.transaction_date BETWEEN :startDate AND :endDate " +
                "AND bt.is_card_payment = FALSE " +
                "ORDER BY st.amount DESC, bt.transaction_date DESC";
        return template.query(sql, makeParam(userId, transactionType, startDate, endDate), TRANSACTION_ENTRY_ROW_MAPPER);
    }

    public List<TransactionEntry> findByUserIdAndTransactionDateFromCardSplits(Long userId, TransactionType transactionType,
                    LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT st.id, st.transaction_type, st.asset_category_id, st.split_pattern_id, st.split_method, st.amount, st.bank_transaction_id, " +
                "st.card_transaction_id, st.discrepancy_transaction_id, st.created_at, st.updated_at, st.user_id, ct.description, ct.card_type, ct.transaction_date, ct.expense_category_id " +
                "FROM split_transactions st " +
                "INNER JOIN card_transactions ct ON st.card_transaction_id = ct.id " +
                "WHERE st.user_id = :userId AND ct.user_id = :userId " +
                "AND st.transaction_type = :transactionType " +
                "AND st.split_method IN ('MANUAL', 'PATTERN') " +
                "AND ct.transaction_date BETWEEN :startDate AND :endDate " +
                "ORDER BY st.amount DESC, ct.transaction_date DESC";
        return template.query(sql, makeParam(userId, transactionType, startDate, endDate), TRANSACTION_ENTRY_ROW_MAPPER);
    }

    public List<TransactionEntry> findByUserIdAndTransactionDateFromDiscrepancySplits(Long userId, TransactionType transactionType,
                    LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT st.id, st.transaction_type, st.asset_category_id, st.split_pattern_id, st.split_method, st.amount, st.bank_transaction_id, " +
                "st.card_transaction_id, st.discrepancy_transaction_id, st.created_at, st.updated_at, st.user_id, dt.description, dt.transaction_date, dt.expense_category_id " +
                "FROM split_transactions st " +
                "INNER JOIN discrepancy_transactions dt ON st.discrepancy_transaction_id = dt.id " +
                "WHERE st.user_id = :userId AND dt.user_id = :userId " +
                "AND st.transaction_type = :transactionType " +
                "AND st.split_method IN ('MANUAL', 'PATTERN') " +
                "AND dt.transaction_date BETWEEN :startDate AND :endDate " +
                "ORDER BY st.amount DESC, dt.transaction_date DESC";
        return template.query(sql, makeParam(userId, transactionType, startDate, endDate), TRANSACTION_ENTRY_ROW_MAPPER);
    }

    public Map<Long, MonthlySummary> sumMonthlySummaryByAssetCategoryFromBankSplits(Long userId, LocalDate startDate,
            LocalDate endDate) {
        String sql = "SELECT st.asset_category_id, " +
                "COALESCE(SUM(CASE WHEN st.transaction_type = 'INCOME' THEN st.amount END), 0) AS income_amount, " +
                "COALESCE(SUM(CASE WHEN st.transaction_type = 'EXPENSE' THEN st.amount END), 0) AS expense_amount " +
                "FROM split_transactions st " +
                "INNER JOIN bank_transactions bt ON st.bank_transaction_id = bt.id " +
                "WHERE st.user_id = :userId AND bt.user_id = :userId " +
                "AND st.split_method IN ('MANUAL', 'PATTERN') " +
                "AND bt.transaction_date BETWEEN :startDate AND :endDate " +
                "AND bt.is_card_payment = FALSE " +
                "GROUP BY st.asset_category_id";
        return template.query(sql, makeParam(userId, startDate, endDate), MONTHLY_SUMMARY_BY_ASSET_CATEGORY_EXTRACTOR);
    }

    public Map<Long, MonthlySummary> sumMonthlySummaryByAssetCategoryFromCardSplits(Long userId, LocalDate startDate,
            LocalDate endDate) {
        String sql = "SELECT st.asset_category_id, " +
                "COALESCE(SUM(CASE WHEN st.transaction_type = 'INCOME' THEN st.amount END), 0) AS income_amount, " +
                "COALESCE(SUM(CASE WHEN st.transaction_type = 'EXPENSE' THEN st.amount END), 0) AS expense_amount " +
                "FROM split_transactions st " +
                "INNER JOIN card_transactions ct ON st.card_transaction_id = ct.id " +
                "WHERE st.user_id = :userId AND ct.user_id = :userId " +
                "AND st.split_method IN ('MANUAL', 'PATTERN') " +
                "AND ct.transaction_date BETWEEN :startDate AND :endDate " +
                "GROUP BY st.asset_category_id";
        return template.query(sql, makeParam(userId, startDate, endDate), MONTHLY_SUMMARY_BY_ASSET_CATEGORY_EXTRACTOR);
    }

    public Map<Long, MonthlySummary> sumMonthlySummaryByAssetCategoryFromDiscrepancySplits(Long userId,
            LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT st.asset_category_id, " +
                "COALESCE(SUM(CASE WHEN st.transaction_type = 'INCOME' THEN st.amount END), 0) AS income_amount, " +
                "COALESCE(SUM(CASE WHEN st.transaction_type = 'EXPENSE' THEN st.amount END), 0) AS expense_amount " +
                "FROM split_transactions st " +
                "INNER JOIN discrepancy_transactions dt ON st.discrepancy_transaction_id = dt.id " +
                "WHERE st.user_id = :userId AND dt.user_id = :userId " +
                "AND st.split_method IN ('MANUAL', 'PATTERN') " +
                "AND dt.transaction_date BETWEEN :startDate AND :endDate " +
                "GROUP BY st.asset_category_id";
        return template.query(sql, makeParam(userId, startDate, endDate), MONTHLY_SUMMARY_BY_ASSET_CATEGORY_EXTRACTOR);
    }

    private MapSqlParameterSource makeParam(Long userId, TransactionType transactionType,
            LocalDate startDate, LocalDate endDate) {
        MapSqlParameterSource param = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("transactionType", transactionType.name())
                .addValue("startDate", startDate)
                .addValue("endDate", endDate);
        return param;
    }

    private MapSqlParameterSource makeParam(Long userId, LocalDate startDate, LocalDate endDate) {
        MapSqlParameterSource param = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("startDate", startDate)
                .addValue("endDate", endDate);
        return param;
    }
}
