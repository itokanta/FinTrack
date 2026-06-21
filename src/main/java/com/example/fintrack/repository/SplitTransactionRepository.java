package com.example.fintrack.repository;

import java.util.List;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import com.example.fintrack.domain.SplitMethod;
import com.example.fintrack.domain.SplitTransaction;
import com.example.fintrack.domain.TransactionType;

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
}
