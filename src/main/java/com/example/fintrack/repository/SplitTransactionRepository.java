package com.example.fintrack.repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.fintrack.domain.SplitTransaction;
import com.example.fintrack.repository.mapper.SplitTransactionRowMapper;

@Repository
public class SplitTransactionRepository {

    private final NamedParameterJdbcTemplate template;
    private static final SplitTransactionRowMapper ROW_MAPPER = new SplitTransactionRowMapper();

    public SplitTransactionRepository(NamedParameterJdbcTemplate template) {
        this.template = template;
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
                    .addValue("splitMethod", splitTransaction.getSplitMethod())
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
                    .addValue("splitMethod", splitTransaction.getSplitMethod())
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
