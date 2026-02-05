package com.example.fintrack.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.fintrack.domain.CardPaymentDiscrepancy;
import com.example.fintrack.repository.mapper.CardPaymentDiscrepancyRowMapper;

@Repository
public class CardPaymentDiscrepancyRepository {

    private final JdbcTemplate jdbcTemplate;
    private final CardPaymentDiscrepancyRowMapper rowMapper = new CardPaymentDiscrepancyRowMapper();

    public CardPaymentDiscrepancyRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<CardPaymentDiscrepancy> findById(Long id) {
        String sql = "SELECT * FROM card_payment_discrepancies WHERE id = ?";
        List<CardPaymentDiscrepancy> discrepancies = jdbcTemplate.query(sql, rowMapper, id);
        if (discrepancies.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(discrepancies.get(0));
    }

    public List<CardPaymentDiscrepancy> findByUserId(Long userId) {
        String sql = "SELECT * FROM card_payment_discrepancies WHERE user_id = ? ORDER BY payment_month DESC, created_at DESC";
        return jdbcTemplate.query(sql, rowMapper, userId);
    }

    public List<CardPaymentDiscrepancy> findByUserIdAndIsResolved(Long userId, Boolean isResolved) {
        String sql = "SELECT * FROM card_payment_discrepancies WHERE user_id = ? AND is_resolved = ? ORDER BY payment_month DESC, created_at DESC";
        return jdbcTemplate.query(sql, rowMapper, userId, isResolved);
    }

    public CardPaymentDiscrepancy save(CardPaymentDiscrepancy discrepancy) {
      if (discrepancy.getId() == null) {
        // INSERT
        String sql = "INSERT INTO card_payment_discrepancies " +
            "(user_id, payment_month, bank_transaction_id, expected_amount, actual_amount, " +
            "difference_amount, is_resolved, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        LocalDateTime now = LocalDateTime.now();
        Long id = jdbcTemplate.queryForObject(sql, Long.class,
            discrepancy.getUserId(),
            discrepancy.getPaymentMonth(),
            discrepancy.getBankTransactionId(),
            discrepancy.getExpectedAmount(),
            discrepancy.getActualAmount(),
            discrepancy.getDifferenceAmount(),
            false,
            now);
        discrepancy.setId(id);
        discrepancy.setCreatedAt(now);
      } else {
        // UPDATE
        String sql = "UPDATE card_payment_discrepancies SET " +
            "user_id = ?, payment_month = ?, bank_transaction_id = ?, " +
            "expected_amount = ?, actual_amount = ?, difference_amount = ?, " +
            "is_resolved = ?, resolved_at = ? " +
            "WHERE id = ?";
        jdbcTemplate.update(sql,
            discrepancy.getUserId(),
            discrepancy.getPaymentMonth(),
            discrepancy.getBankTransactionId(),
            discrepancy.getExpectedAmount(),
            discrepancy.getActualAmount(),
            discrepancy.getDifferenceAmount(),
            discrepancy.getIsResolved(),
            discrepancy.getResolvedAt(),
            discrepancy.getId());
      }
      return discrepancy;
    }

    public void markAsResolved(Long id) {
      String sql = "UPDATE card_payment_discrepancies SET " +
          "is_resolved = TRUE, resolved_at = ? " +
          "WHERE id = ?";
      jdbcTemplate.update(sql, LocalDateTime.now(), id);
    }
}
