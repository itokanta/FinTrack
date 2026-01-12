package com.example.fintrack.repository.mapper;

import com.example.fintrack.domain.SplitIncome;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SplitIncomeRowMapper implements RowMapper<SplitIncome> {
    
    @Override
    public SplitIncome mapRow(ResultSet rs, int rowNum) throws SQLException {
        SplitIncome splitIncome = new SplitIncome();
        splitIncome.setId(rs.getLong("id"));
        splitIncome.setAssetCategoryId(rs.getLong("asset_category_id"));
        
        // split_pattern_idはNULL許容
        long splitPatternId = rs.getLong("split_pattern_id");
        if (rs.wasNull()) {
            splitIncome.setSplitPatternId(null);
        } else {
            splitIncome.setSplitPatternId(splitPatternId);
        }
        
        splitIncome.setSplitMethod(rs.getString("split_method"));
        splitIncome.setAmount(rs.getLong("amount"));
        
        // NULL許容のカラム
        long bankTransactionId = rs.getLong("bank_transaction_id");
        if (rs.wasNull()) {
            splitIncome.setBankTransactionId(null);
        } else {
            splitIncome.setBankTransactionId(bankTransactionId);
        }
        
        long cardTransactionId = rs.getLong("card_transaction_id");
        if (rs.wasNull()) {
            splitIncome.setCardTransactionId(null);
        } else {
            splitIncome.setCardTransactionId(cardTransactionId);
        }
        
        splitIncome.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        splitIncome.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        
        return splitIncome;
    }
}

