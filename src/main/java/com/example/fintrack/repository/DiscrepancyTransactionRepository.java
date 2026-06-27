package com.example.fintrack.repository;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.fintrack.domain.DiscrepancyTransaction;
import com.example.fintrack.domain.SplitMethod;
import com.example.fintrack.domain.TransactionSource;
import com.example.fintrack.domain.TransactionType;
import com.example.fintrack.domain.dto.MonthlySummary;
import com.example.fintrack.domain.dto.TransactionEntry;
import com.example.fintrack.repository.extractor.MonthlySummaryByAssetCategoryExtractor;
import com.example.fintrack.repository.rowmapper.MonthlySummaryRowMapper;
import com.example.fintrack.repository.rowmapper.TransactionEntryRowMapper;

@Repository
public class DiscrepancyTransactionRepository {

    private static final MonthlySummaryByAssetCategoryExtractor MONTHLY_SUMMARY_BY_ASSET_CATEGORY_EXTRACTOR =
            new MonthlySummaryByAssetCategoryExtractor();

    private static final RowMapper<DiscrepancyTransaction> ROW_MAPPER = (rs, rowNum) -> {
        DiscrepancyTransaction dt = new DiscrepancyTransaction();
        dt.setId(rs.getLong("id"));
        dt.setDescription(rs.getString("description"));
        dt.setAmount(rs.getLong("amount"));
        String transactionType = rs.getString("transaction_type");
        dt.setTransactionType(transactionType == null ? null : TransactionType.valueOf(transactionType));
        dt.setTransactionDate(rs.getDate("transaction_date").toLocalDate());
        dt.setUserId(rs.getLong("user_id"));
        long bankTransactionId = rs.getLong("bank_transaction_id");
        dt.setBankTransactionId(rs.wasNull() ? null : bankTransactionId);
        long assetCategoryId = rs.getLong("asset_category_id");
        dt.setAssetCategoryId(rs.wasNull() ? null : assetCategoryId);
        long expenseCategoryId = rs.getLong("expense_category_id");
        dt.setExpenseCategoryId(rs.wasNull() ? null : expenseCategoryId);
        long splitPatternId = rs.getLong("split_pattern_id");
        dt.setSplitPatternId(rs.wasNull() ? null : splitPatternId);
        dt.setCardPaymentDiscrepancyId(rs.getLong("card_payment_discrepancy_id"));
        dt.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        String splitMethod = rs.getString("split_method");
        dt.setSplitMethod(splitMethod == null ? null : SplitMethod.valueOf(splitMethod));
        return dt;
    };

    private static final MonthlySummaryRowMapper MONTHLY_SUMMARY_ROW_MAPPER = new MonthlySummaryRowMapper();

    private static final TransactionEntryRowMapper BASE_MAPPER = new TransactionEntryRowMapper();

    private static final RowMapper<TransactionEntry> TRANSACTION_ENTRY_ROW_MAPPER = (rs, rowNum) -> {
        TransactionEntry transactionEntry = BASE_MAPPER.mapRow(rs, rowNum);
        transactionEntry.setDiscrepancyTransactionId(rs.getLong("id"));
        transactionEntry.setTransactionSource(TransactionSource.DISCREPANCY.getSourceName());
        return transactionEntry;
    };

    private final NamedParameterJdbcTemplate template;

    public DiscrepancyTransactionRepository(NamedParameterJdbcTemplate template) {
        this.template = template;
    }

    public DiscrepancyTransaction save(DiscrepancyTransaction discrepancyTransaction) {
        String sql = "INSERT INTO discrepancy_transactions " +
                     "(description, amount, transaction_type, transaction_date, user_id, bank_transaction_id, " +
                     "asset_category_id, expense_category_id, split_pattern_id, split_method, card_payment_discrepancy_id) " +
                     "VALUES (:description, :amount, :transactionType, :transactionDate, :userId, :bankTransactionId, " +
                     ":assetCategoryId, :expenseCategoryId, :splitPatternId, :splitMethod, :cardPaymentDiscrepancyId) " +
                     "RETURNING id, created_at";
        SplitMethod splitMethod = discrepancyTransaction.getSplitMethod();
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("description", discrepancyTransaction.getDescription())
                .addValue("amount", discrepancyTransaction.getAmount())
                .addValue("transactionType", discrepancyTransaction.getTransactionType().name())
                .addValue("transactionDate", discrepancyTransaction.getTransactionDate())
                .addValue("userId", discrepancyTransaction.getUserId())
                .addValue("bankTransactionId", discrepancyTransaction.getBankTransactionId())
                .addValue("assetCategoryId", discrepancyTransaction.getAssetCategoryId())
                .addValue("expenseCategoryId", discrepancyTransaction.getExpenseCategoryId())
                .addValue("splitPatternId", discrepancyTransaction.getSplitPatternId())
                .addValue("splitMethod", splitMethod != null ? splitMethod.name() : null)
                .addValue("cardPaymentDiscrepancyId", discrepancyTransaction.getCardPaymentDiscrepancyId());
        Map<String, Object> result = template.queryForMap(sql, params);
        discrepancyTransaction.setId(((Number) result.get("id")).longValue());
        discrepancyTransaction.setCreatedAt(((Timestamp) result.get("created_at")).toLocalDateTime());
        return discrepancyTransaction;
    }

    public List<DiscrepancyTransaction> findByBankTransactionIds(List<Long> bankTransactionIdList, Long userId) {
        if (bankTransactionIdList.isEmpty()) {
            return List.of();
        }
        String sql = "SELECT id, description, amount, transaction_type, transaction_date, user_id, " +
                    "bank_transaction_id, asset_category_id, expense_category_id, split_pattern_id, " +
                    "card_payment_discrepancy_id, created_at, split_method " +
                    "FROM discrepancy_transactions " +
                    "WHERE user_id = :userId AND bank_transaction_id IN (:ids)";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ids", bankTransactionIdList)
                .addValue("userId", userId);
        return template.query(sql, params, ROW_MAPPER);
    }

    public List<TransactionEntry> findByUserIdAndTransactionDate(Long userId, TransactionType transactionType, LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT id, description, amount, transaction_type, transaction_date, user_id, " +
                "bank_transaction_id, asset_category_id, expense_category_id, split_pattern_id, " +
                "card_payment_discrepancy_id, created_at, split_method " +
                "FROM discrepancy_transactions " +
                "WHERE user_id = :userId AND transaction_type = :transactionType " +
                "AND transaction_date BETWEEN :startDate AND :endDate " +
                "ORDER BY amount, transaction_date DESC";
        return template.query(sql, makeParam(userId, transactionType, startDate, endDate), TRANSACTION_ENTRY_ROW_MAPPER);
    }

    public List<TransactionEntry> findByUserIdAndTransactionDateAndSplitMethodIsNullOrSingle(Long userId, TransactionType transactionType,
            LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT id, description, amount, transaction_type, transaction_date, user_id, " +
                "bank_transaction_id, asset_category_id, expense_category_id, split_pattern_id, " +
                "card_payment_discrepancy_id, created_at, split_method " +
                "FROM discrepancy_transactions " +
                "WHERE user_id = :userId AND transaction_type = :transactionType " +
                "AND transaction_date BETWEEN :startDate AND :endDate " +
                "AND (split_method IS NULL OR split_method = 'SINGLE') " +
                "ORDER BY amount, transaction_date DESC";
        return template.query(sql, makeParam(userId, transactionType, startDate, endDate), TRANSACTION_ENTRY_ROW_MAPPER);
    }

    public MonthlySummary sumAmountByUserIdAndTransactionDate(Long userId, LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT " +
                "COALESCE(SUM(CASE WHEN transaction_type = 'INCOME' THEN amount END), 0) AS income_amount, " +
                "COALESCE(SUM(CASE WHEN transaction_type = 'EXPENSE' THEN amount END), 0) AS expense_amount " +
                "FROM discrepancy_transactions " +
                "WHERE user_id = :userId " +
                "AND transaction_date BETWEEN :startDate AND :endDate " +
                "AND (split_method IS NULL OR split_method = 'SINGLE')";
        return template.queryForObject(sql, makeParam(userId, startDate, endDate), MONTHLY_SUMMARY_ROW_MAPPER);
    }

    public Map<Long, MonthlySummary> sumMonthlySummaryByAssetCategory(Long userId, LocalDate startDate,
            LocalDate endDate) {
        String sql = "SELECT asset_category_id, " +
                "COALESCE(SUM(CASE WHEN transaction_type = 'INCOME' THEN amount END), 0) AS income_amount, " +
                "COALESCE(SUM(CASE WHEN transaction_type = 'EXPENSE' THEN amount END), 0) AS expense_amount " +
                "FROM discrepancy_transactions " +
                "WHERE user_id = :userId " +
                "AND transaction_date BETWEEN :startDate AND :endDate " +
                "AND split_method = 'SINGLE' " +
                "GROUP BY asset_category_id";
        return template.query(sql, makeParam(userId, startDate, endDate), MONTHLY_SUMMARY_BY_ASSET_CATEGORY_EXTRACTOR);
    }

    public MonthlySummary sumMonthlySummaryBySplitMethodIsNull(Long userId, LocalDate startDate,
            LocalDate endDate) {
        String sql = "SELECT " +
                "COALESCE(SUM(CASE WHEN transaction_type = 'INCOME' THEN amount END), 0) AS income_amount, " +
                "COALESCE(SUM(CASE WHEN transaction_type = 'EXPENSE' THEN amount END), 0) AS expense_amount " +
                "FROM discrepancy_transactions " +
                "WHERE user_id = :userId " +
                "AND transaction_date BETWEEN :startDate AND :endDate " +
                "AND split_method IS NULL";
        return template.queryForObject(sql, makeParam(userId, startDate, endDate), MONTHLY_SUMMARY_ROW_MAPPER);
    }

    private MapSqlParameterSource makeParam(Long userId, LocalDate startDate, LocalDate endDate) {
        MapSqlParameterSource param = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("startDate", startDate)
                .addValue("endDate", endDate);
        return param;
    }

    private MapSqlParameterSource makeParam(Long userId, TransactionType transactionType, LocalDate startDate,
            LocalDate endDate) {
        MapSqlParameterSource param = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("transactionType", transactionType.name())
                .addValue("startDate", startDate)
                .addValue("endDate", endDate);
        return param;
    }
}
