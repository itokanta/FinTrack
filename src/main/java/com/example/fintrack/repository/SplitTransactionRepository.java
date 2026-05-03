package com.example.fintrack.repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.fintrack.domain.SplitMethod;
import com.example.fintrack.domain.SplitTransaction;

@Repository
public class SplitTransactionRepository {

    private static final RowMapper<SplitTransaction> ROW_MAPPER = (rs, rowNum) -> {
        SplitTransaction splitTransaction = new SplitTransaction();
        splitTransaction.setId(rs.getLong("id"));
        splitTransaction.setTransactionType(rs.getString("transaction_type"));
        splitTransaction.setAssetCategoryId(rs.getLong("asset_category_id"));
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
        return splitTransaction;
    };

    private final NamedParameterJdbcTemplate template;

    public SplitTransactionRepository(NamedParameterJdbcTemplate template) {
        this.template = template;
    }

    public List<SplitTransaction> findByBankTransactionIds(List<Long> bankTransactionIds, Long userId) {
        if (bankTransactionIds.isEmpty()) return List.of();
        String sql = "SELECT st.id, st.transaction_type, st.asset_category_id, st.split_pattern_id, st.split_method, st.amount, " +
                     "st.bank_transaction_id, st.card_transaction_id, st.discrepancy_transaction_id, st.created_at, st.updated_at " +
                     "FROM split_transactions st " +
                     "JOIN bank_transactions bt ON st.bank_transaction_id = bt.id " +
                     "WHERE st.bank_transaction_id IN (:ids) AND bt.user_id = :userId";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ids", bankTransactionIds)
                .addValue("userId", userId);
        return template.query(sql, params, ROW_MAPPER);
    }

    public List<SplitTransaction> findByCardTransactionIds(List<Long> cardTransactionIds, Long userId) {
        if (cardTransactionIds.isEmpty()) return List.of();
        String sql = "SELECT st.id, st.transaction_type, st.asset_category_id, st.split_pattern_id, st.split_method, st.amount, " +
                     "st.bank_transaction_id, st.card_transaction_id, st.discrepancy_transaction_id, st.created_at, st.updated_at " +
                     "FROM split_transactions st " +
                     "JOIN card_transactions ct ON st.card_transaction_id = ct.id " +
                     "WHERE st.card_transaction_id IN (:ids) AND ct.user_id = :userId";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ids", cardTransactionIds)
                .addValue("userId", userId);
        return template.query(sql, params, ROW_MAPPER);
    }

    public List<SplitTransaction> findByBankTransactionId(Long bankTransactionId) {
        String sql = "SELECT id, transaction_type, asset_category_id, split_pattern_id, split_method, amount, " +
                     "bank_transaction_id, card_transaction_id, discrepancy_transaction_id, created_at, updated_at " +
                     "FROM split_transactions WHERE bank_transaction_id = :bankTransactionId";
        MapSqlParameterSource params = new MapSqlParameterSource("bankTransactionId", bankTransactionId);
        return template.query(sql, params, ROW_MAPPER);
    }

    public List<SplitTransaction> findByCardTransactionId(Long cardTransactionId) {
        String sql = "SELECT id, transaction_type, asset_category_id, split_pattern_id, split_method, amount, " +
                     "bank_transaction_id, card_transaction_id, discrepancy_transaction_id, created_at, updated_at " +
                     "FROM split_transactions WHERE card_transaction_id = :cardTransactionId";
        MapSqlParameterSource params = new MapSqlParameterSource("cardTransactionId", cardTransactionId);
        return template.query(sql, params, ROW_MAPPER);
    }

    public SplitTransaction save(SplitTransaction splitTransaction) {
        if (splitTransaction.getId() == null) {
            String sql = "INSERT INTO split_transactions " +
                         "(transaction_type, asset_category_id, split_pattern_id, " +
                         "split_method, amount, bank_transaction_id, card_transaction_id, " +
                         "discrepancy_transaction_id) " +
                         "VALUES (:transactionType, :assetCategoryId, :splitPatternId, " +
                         ":splitMethod, :amount, :bankTransactionId, :cardTransactionId, " +
                         ":discrepancyTransactionId) RETURNING id, created_at, updated_at";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("transactionType", splitTransaction.getTransactionType())
                    .addValue("assetCategoryId", splitTransaction.getAssetCategoryId())
                    .addValue("splitPatternId", splitTransaction.getSplitPatternId())
                    .addValue("splitMethod", splitTransaction.getSplitMethod() == null ? null : splitTransaction.getSplitMethod().name())
                    .addValue("amount", splitTransaction.getAmount())
                    .addValue("bankTransactionId", splitTransaction.getBankTransactionId())
                    .addValue("cardTransactionId", splitTransaction.getCardTransactionId())
                    .addValue("discrepancyTransactionId", splitTransaction.getDiscrepancyTransactionId());
            Map<String, Object> result = template.queryForMap(sql, params);
            splitTransaction.setId(((Number) result.get("id")).longValue());
            splitTransaction.setCreatedAt(((Timestamp) result.get("created_at")).toLocalDateTime());
            splitTransaction.setUpdatedAt(((Timestamp) result.get("updated_at")).toLocalDateTime());
        } else {
            String sql = "UPDATE split_transactions SET " +
                         "transaction_type = :transactionType, asset_category_id = :assetCategoryId, " +
                         "split_pattern_id = :splitPatternId, split_method = :splitMethod, amount = :amount, " +
                         "bank_transaction_id = :bankTransactionId, card_transaction_id = :cardTransactionId, " +
                         "discrepancy_transaction_id = :discrepancyTransactionId " +
                         "WHERE id = :id RETURNING updated_at";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("transactionType", splitTransaction.getTransactionType())
                    .addValue("assetCategoryId", splitTransaction.getAssetCategoryId())
                    .addValue("splitPatternId", splitTransaction.getSplitPatternId())
                    .addValue("splitMethod", splitTransaction.getSplitMethod() == null ? null : splitTransaction.getSplitMethod().name())
                    .addValue("amount", splitTransaction.getAmount())
                    .addValue("bankTransactionId", splitTransaction.getBankTransactionId())
                    .addValue("cardTransactionId", splitTransaction.getCardTransactionId())
                    .addValue("discrepancyTransactionId", splitTransaction.getDiscrepancyTransactionId())
                    .addValue("id", splitTransaction.getId());
            Map<String, Object> result = template.queryForMap(sql, params);
            splitTransaction.setUpdatedAt(((Timestamp) result.get("updated_at")).toLocalDateTime());
        }
        return splitTransaction;
    }

    public List<SplitTransaction> saveAll(List<SplitTransaction> splitTransactions) {
        for (SplitTransaction splitTransaction : splitTransactions) {
            save(splitTransaction);
        }
        return splitTransactions;
    }
}
