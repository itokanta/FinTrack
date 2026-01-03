package com.example.fintrack.repository.mapper;

import com.example.fintrack.domain.BankTransaction;
import com.example.fintrack.domain.TransactionType;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class BankTransactionRowMapper implements RowMapper<BankTransaction> {
    
    @Override
    public BankTransaction mapRow(ResultSet rs, int rowNum) throws SQLException {
        BankTransaction transaction = new BankTransaction();
        transaction.setId(rs.getLong("id"));
        transaction.setDescription(rs.getString("description"));
        transaction.setAmount(rs.getLong("amount"));
        transaction.setTransactionType(TransactionType.valueOf(rs.getString("transaction_type")));
        transaction.setTransactionDate(rs.getDate("transaction_date").toLocalDate());
        transaction.setFingerprint(rs.getString("fingerprint"));
        transaction.setUserId(rs.getLong("user_id"));
        
        // NULL許容のカラム
        long assetCategoryId = rs.getLong("asset_category_id");
        if (rs.wasNull()) {
            transaction.setAssetCategoryId(null);
        } else {
            transaction.setAssetCategoryId(assetCategoryId);
        }
        
        long expenseCategoryId = rs.getLong("expense_category_id");
        if (rs.wasNull()) {
            transaction.setExpenseCategoryId(null);
        } else {
            transaction.setExpenseCategoryId(expenseCategoryId);
        }
        
        long incomeSplitPatternId = rs.getLong("income_split_pattern_id");
        if (rs.wasNull()) {
            transaction.setIncomeSplitPatternId(null);
        } else {
            transaction.setIncomeSplitPatternId(incomeSplitPatternId);
        }
        
        transaction.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return transaction;
    }
}

