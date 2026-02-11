package com.example.fintrack.repository.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;
import com.example.fintrack.domain.CardPaymentDiscrepancy;

public class CardPaymentDiscrepancyRowMapper implements RowMapper<CardPaymentDiscrepancy> {
    @Override
    public CardPaymentDiscrepancy mapRow(ResultSet rs, int rowNum) throws SQLException {
        CardPaymentDiscrepancy discrepancy = new CardPaymentDiscrepancy();
        discrepancy.setId(rs.getLong("id"));
        discrepancy.setUserId(rs.getLong("user_id"));
        discrepancy.setPaymentMonth(rs.getString("payment_month"));
        discrepancy.setBankTransactionId(rs.getLong("bank_transaction_id"));
        discrepancy.setExpectedAmount(rs.getLong("expected_amount"));
        discrepancy.setActualAmount(rs.getLong("actual_amount"));
        discrepancy.setDifferenceAmount(rs.getLong("difference_amount"));
        discrepancy.setIsResolved(rs.getBoolean("is_resolved"));
        discrepancy.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        if (rs.getTimestamp("resolved_at") != null) {
            discrepancy.setResolvedAt(rs.getTimestamp("resolved_at").toLocalDateTime());
        }
        return discrepancy;
    }
}
