package com.example.fintrack.repository.extractor;

import com.example.fintrack.domain.CardTransaction;
import com.example.fintrack.domain.CsvImportUpdateField;
import com.example.fintrack.domain.CsvImportUpdateHistory;
import com.example.fintrack.domain.TransactionType;
import com.example.fintrack.domain.dto.CardTransactionUpdateDetail;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

public class CardTransactionUpdateDetailExtractor
        implements ResultSetExtractor<List<CardTransactionUpdateDetail>> {

    @Override
    public List<CardTransactionUpdateDetail> extractData(ResultSet rs)
            throws SQLException, DataAccessException {

        Map<Long, CardTransactionUpdateDetail> cardTransactionUpdateDetailMap = new LinkedHashMap<>();

        while (rs.next()) {
            Long ctId = rs.getLong("ct_id");

            if (!cardTransactionUpdateDetailMap.containsKey(ctId)) {
                CardTransaction ct = new CardTransaction();
                ct.setId(ctId);
                ct.setDescription(rs.getString("ct_description"));
                ct.setAmount(rs.getLong("ct_amount"));
                ct.setTransactionType(TransactionType.valueOf(rs.getString("ct_transaction_type")));
                ct.setTransactionDate(rs.getDate("ct_transaction_date").toLocalDate());
                ct.setFingerprint(rs.getString("ct_fingerprint"));
                ct.setUserId(rs.getLong("ct_user_id"));
                ct.setTransactionSource(rs.getString("ct_transaction_source"));
                ct.setCardType(rs.getString("ct_card_type"));
                ct.setPaymentMonth(rs.getString("ct_payment_month"));
                ct.setCreatedAt(rs.getTimestamp("ct_created_at").toLocalDateTime());
                ct.setUpdateCount(rs.getLong("ct_update_count"));
                ct.setUpdatedAt(rs.getTimestamp("ct_updated_at").toLocalDateTime());

                long assetCategoryId = rs.getLong("ct_asset_category_id");
                ct.setAssetCategoryId(rs.wasNull() ? null : assetCategoryId);

                long expenseCategoryId = rs.getLong("ct_expense_category_id");
                ct.setExpenseCategoryId(rs.wasNull() ? null : expenseCategoryId);

                long splitPatternId = rs.getLong("ct_split_pattern_id");
                ct.setSplitPatternId(rs.wasNull() ? null : splitPatternId);

                long csvImportHistoryId = rs.getLong("ct_csv_import_history_id");
                ct.setCsvImportHistoryId(rs.wasNull() ? null : csvImportHistoryId);

                Boolean isPaid = rs.getBoolean("ct_is_paid");
                ct.setIsPaid(rs.wasNull() ? null : isPaid);

                CardTransactionUpdateDetail detail = new CardTransactionUpdateDetail();
                detail.setCardTransaction(ct);
                detail.setUpdates(new ArrayList<>());
                cardTransactionUpdateDetailMap.put(ctId, detail);
            }

            CsvImportUpdateHistory history = new CsvImportUpdateHistory();
            history.setId(rs.getLong("cuh_id"));
            history.setCsvImportHistoryId(rs.getLong("cuh_csv_import_history_id"));
            history.setCardTransactionId(rs.getObject("cuh_card_transaction_id", Long.class));
            history.setBankTransactionId(rs.getObject("cuh_bank_transaction_id", Long.class));
            history.setTargetField(CsvImportUpdateField.valueOf(rs.getString("cuh_target_field")));
            history.setValueBefore(rs.getString("cuh_value_before"));
            history.setValueAfter(rs.getString("cuh_value_after"));
            history.setUserId(rs.getLong("cuh_user_id"));
            history.setUpdateCount(rs.getLong("cuh_update_count"));
            history.setCreatedAt(rs.getTimestamp("cuh_created_at").toLocalDateTime());
            history.setUpdatedAt(rs.getTimestamp("cuh_updated_at").toLocalDateTime());

            cardTransactionUpdateDetailMap.get(ctId).getUpdates().add(history);
        }

        return new ArrayList<>(cardTransactionUpdateDetailMap.values());
    }
}
