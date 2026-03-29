package com.example.fintrack.repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.fintrack.domain.CardPaymentDiscrepancy;
import com.example.fintrack.repository.mapper.CardPaymentDiscrepancyRowMapper;

@Repository
public class CardPaymentDiscrepancyRepository {

    private final NamedParameterJdbcTemplate template;
    private static final CardPaymentDiscrepancyRowMapper ROW_MAPPER = new CardPaymentDiscrepancyRowMapper();

    public CardPaymentDiscrepancyRepository(NamedParameterJdbcTemplate template) {
        this.template = template;
    }

    public Optional<CardPaymentDiscrepancy> findById(Long id) {
        String sql = "SELECT id, user_id, payment_month, bank_transaction_id, expected_amount, actual_amount, " +
                     "difference_amount, is_resolved, created_at, resolved_at " +
                     "FROM card_payment_discrepancies WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource("id", id);
        List<CardPaymentDiscrepancy> discrepancies = template.query(sql, params, ROW_MAPPER);
        if (discrepancies.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(discrepancies.get(0));
    }

    public List<CardPaymentDiscrepancy> findByUserId(Long userId) {
        String sql = "SELECT id, user_id, payment_month, bank_transaction_id, expected_amount, actual_amount, " +
                     "difference_amount, is_resolved, created_at, resolved_at " +
                     "FROM card_payment_discrepancies WHERE user_id = :userId ORDER BY payment_month DESC, created_at DESC";
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        return template.query(sql, params, ROW_MAPPER);
    }

    public List<CardPaymentDiscrepancy> findByUserIdAndIsResolved(Long userId, Boolean isResolved) {
        String sql = "SELECT id, user_id, payment_month, bank_transaction_id, expected_amount, actual_amount, " +
                     "difference_amount, is_resolved, created_at, resolved_at " +
                     "FROM card_payment_discrepancies WHERE user_id = :userId AND is_resolved = :isResolved " +
                     "ORDER BY payment_month DESC, created_at DESC";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("isResolved", isResolved);
        return template.query(sql, params, ROW_MAPPER);
    }

    public CardPaymentDiscrepancy save(CardPaymentDiscrepancy discrepancy) {
        if (discrepancy.getId() == null) {
            String sql = "INSERT INTO card_payment_discrepancies " +
                         "(user_id, payment_month, bank_transaction_id, expected_amount, actual_amount, " +
                         "difference_amount, is_resolved) " +
                         "VALUES (:userId, :paymentMonth, :bankTransactionId, :expectedAmount, :actualAmount, " +
                         ":differenceAmount, false) RETURNING id, created_at";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("userId", discrepancy.getUserId())
                    .addValue("paymentMonth", discrepancy.getPaymentMonth())
                    .addValue("bankTransactionId", discrepancy.getBankTransactionId())
                    .addValue("expectedAmount", discrepancy.getExpectedAmount())
                    .addValue("actualAmount", discrepancy.getActualAmount())
                    .addValue("differenceAmount", discrepancy.getDifferenceAmount());
            Map<String, Object> result = template.queryForMap(sql, params);
            discrepancy.setId(((Number) result.get("id")).longValue());
            discrepancy.setCreatedAt(((Timestamp) result.get("created_at")).toLocalDateTime());
        } else {
            String sql = "UPDATE card_payment_discrepancies SET " +
                         "user_id = :userId, payment_month = :paymentMonth, bank_transaction_id = :bankTransactionId, " +
                         "expected_amount = :expectedAmount, actual_amount = :actualAmount, " +
                         "difference_amount = :differenceAmount, is_resolved = :isResolved, resolved_at = :resolvedAt " +
                         "WHERE id = :id";
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("userId", discrepancy.getUserId())
                    .addValue("paymentMonth", discrepancy.getPaymentMonth())
                    .addValue("bankTransactionId", discrepancy.getBankTransactionId())
                    .addValue("expectedAmount", discrepancy.getExpectedAmount())
                    .addValue("actualAmount", discrepancy.getActualAmount())
                    .addValue("differenceAmount", discrepancy.getDifferenceAmount())
                    .addValue("isResolved", discrepancy.getIsResolved())
                    .addValue("resolvedAt", discrepancy.getResolvedAt())
                    .addValue("id", discrepancy.getId());
            template.update(sql, params);
        }
        return discrepancy;
    }

    public void markAsResolved(Long id) {
        String sql = "UPDATE card_payment_discrepancies SET " +
                     "is_resolved = TRUE, resolved_at = :resolvedAt " +
                     "WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("resolvedAt", LocalDateTime.now())
                .addValue("id", id);
        template.update(sql, params);
    }
}
