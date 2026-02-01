package com.example.fintrack.repository.mapper;

import com.example.fintrack.domain.CardTransaction;
import com.example.fintrack.domain.TransactionType;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class CardTransactionRowMapper implements RowMapper<CardTransaction> {
    
    @Override
    public CardTransaction mapRow(ResultSet rs, int rowNum) throws SQLException {
        CardTransaction transaction = new CardTransaction();
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
        
        // split_pattern_idを読み取り、後方互換性のためincomeSplitPatternIdにも設定
        long splitPatternId = rs.getLong("split_pattern_id");
        if (rs.wasNull()) {
            transaction.setSplitPatternId(null);
            transaction.setIncomeSplitPatternId(null);
        } else {
            transaction.setSplitPatternId(splitPatternId);
            transaction.setIncomeSplitPatternId(splitPatternId);
        }
        
        transaction.setTransactionSource(rs.getString("transaction_source"));
        transaction.setCardType(rs.getString("card_type"));
        transaction.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return transaction;
    }
}

