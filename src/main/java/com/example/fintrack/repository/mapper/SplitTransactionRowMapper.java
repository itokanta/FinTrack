package com.example.fintrack.repository.mapper;

import com.example.fintrack.domain.SplitTransaction;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SplitTransactionRowMapper implements RowMapper<SplitTransaction> {
    
    @Override
    public SplitTransaction mapRow(ResultSet rs, int rowNum) throws SQLException {
        SplitTransaction splitTransaction = new SplitTransaction();
        splitTransaction.setId(rs.getLong("id"));
        splitTransaction.setTransactionType(rs.getString("transaction_type"));
        splitTransaction.setAssetCategoryId(rs.getLong("asset_category_id"));
        
        // split_pattern_idはNULL許容
        long splitPatternId = rs.getLong("split_pattern_id");
        if (rs.wasNull()) {
            splitTransaction.setSplitPatternId(null);
        } else {
            splitTransaction.setSplitPatternId(splitPatternId);
        }
        
        splitTransaction.setSplitMethod(rs.getString("split_method"));
        splitTransaction.setAmount(rs.getLong("amount"));
        
        // NULL許容のカラム
        long bankTransactionId = rs.getLong("bank_transaction_id");
        if (rs.wasNull()) {
            splitTransaction.setBankTransactionId(null);
        } else {
            splitTransaction.setBankTransactionId(bankTransactionId);
        }
        
        long cardTransactionId = rs.getLong("card_transaction_id");
        if (rs.wasNull()) {
            splitTransaction.setCardTransactionId(null);
        } else {
            splitTransaction.setCardTransactionId(cardTransactionId);
        }
        
        splitTransaction.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        splitTransaction.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        
        return splitTransaction;
    }
}

