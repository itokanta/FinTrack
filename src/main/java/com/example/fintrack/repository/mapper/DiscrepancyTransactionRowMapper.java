package com.example.fintrack.repository.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import com.example.fintrack.domain.DiscrepancyTransaction;
import com.example.fintrack.domain.TransactionType;

public class DiscrepancyTransactionRowMapper implements RowMapper<DiscrepancyTransaction> {

    @Override
    public DiscrepancyTransaction mapRow(ResultSet rs, int rowNum) throws SQLException {
        DiscrepancyTransaction discrepancyTransaction = new DiscrepancyTransaction();
        discrepancyTransaction.setId(rs.getLong("id"));
        discrepancyTransaction.setDescription(rs.getString("description"));
        discrepancyTransaction.setAmount(rs.getLong("amount"));
        discrepancyTransaction.setTransactionType(TransactionType.valueOf(rs.getString("transaction_type")));
        discrepancyTransaction.setTransactionDate(rs.getDate("transaction_date").toLocalDate());
        discrepancyTransaction.setUserId(rs.getLong("user_id"));
        
        long assetCategoryId = rs.getLong("asset_category_id");
        if (rs.wasNull()) {
            discrepancyTransaction.setAssetCategoryId(null);
        } else {
            discrepancyTransaction.setAssetCategoryId(assetCategoryId);
        }

        long expenseCategoryId = rs.getLong("expense_category_id");
        if (rs.wasNull()) {
            discrepancyTransaction.setExpenseCategoryId(null);
        } else {
            discrepancyTransaction.setExpenseCategoryId(expenseCategoryId);
        }

        long splitPatternId = rs.getLong("split_pattern_id");
        if (rs.wasNull()) {
            discrepancyTransaction.setSplitPatternId(null);
        } else {
            discrepancyTransaction.setSplitPatternId(splitPatternId);
        }

        discrepancyTransaction.setCardPaymentDiscrepancyId(rs.getLong("card_payment_discrepancy_id"));
        discrepancyTransaction.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return discrepancyTransaction;
    }
}
