package com.example.fintrack.repository;

import java.sql.Timestamp;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.fintrack.domain.DiscrepancyTransaction;

@Repository
public class DiscrepancyTransactionRepository {

    private final NamedParameterJdbcTemplate template;

    public DiscrepancyTransactionRepository(NamedParameterJdbcTemplate template) {
        this.template = template;
    }

    public DiscrepancyTransaction save(DiscrepancyTransaction discrepancyTransaction) {
        String sql = "INSERT INTO discrepancy_transactions " +
                     "(description, amount, transaction_type, transaction_date, user_id, " +
                     "asset_category_id, expense_category_id, split_pattern_id, card_payment_discrepancy_id) " +
                     "VALUES (:description, :amount, :transactionType, :transactionDate, :userId, " +
                     ":assetCategoryId, :expenseCategoryId, :splitPatternId, :cardPaymentDiscrepancyId) " +
                     "RETURNING id, created_at";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("description", discrepancyTransaction.getDescription())
                .addValue("amount", discrepancyTransaction.getAmount())
                .addValue("transactionType", discrepancyTransaction.getTransactionType().name())
                .addValue("transactionDate", discrepancyTransaction.getTransactionDate())
                .addValue("userId", discrepancyTransaction.getUserId())
                .addValue("assetCategoryId", discrepancyTransaction.getAssetCategoryId())
                .addValue("expenseCategoryId", discrepancyTransaction.getExpenseCategoryId())
                .addValue("splitPatternId", discrepancyTransaction.getSplitPatternId())
                .addValue("cardPaymentDiscrepancyId", discrepancyTransaction.getCardPaymentDiscrepancyId());
        Map<String, Object> result = template.queryForMap(sql, params);
        discrepancyTransaction.setId(((Number) result.get("id")).longValue());
        discrepancyTransaction.setCreatedAt(((Timestamp) result.get("created_at")).toLocalDateTime());
        return discrepancyTransaction;
    }
}
