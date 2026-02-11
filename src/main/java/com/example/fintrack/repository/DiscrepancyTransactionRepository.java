package com.example.fintrack.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.fintrack.domain.DiscrepancyTransaction;
import com.example.fintrack.repository.mapper.DiscrepancyTransactionRowMapper;

@Repository
public class DiscrepancyTransactionRepository {

    private final JdbcTemplate jdbcTemplate;
    private final DiscrepancyTransactionRowMapper rowMapper = new DiscrepancyTransactionRowMapper();

    public DiscrepancyTransactionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<DiscrepancyTransaction> findById(Long id) {
        String sql = "SELECT * FROM discrepancy_transactions WHERE id = ?";
        List<DiscrepancyTransaction> transactions = jdbcTemplate.query(sql, rowMapper, id);
        if (transactions.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(transactions.get(0));
    }

    public List<DiscrepancyTransaction> findByUserId(Long userId) {
        String sql = "SELECT * FROM discrepancy_transactions WHERE user_id = ?";
        return jdbcTemplate.query(sql, rowMapper, userId);
    }

    public List<DiscrepancyTransaction> findByCardPaymentDiscrepancyId(Long cardPaymentDiscrepancyId) {
        String sql = "SELECT * FROM discrepancy_transactions WHERE card_payment_discrepancy_id = ?";
        return jdbcTemplate.query(sql, rowMapper, cardPaymentDiscrepancyId);
    }
    public DiscrepancyTransaction save(DiscrepancyTransaction discrepancyTransaction) {
        String sql = "INSERT INTO discrepancy_transactions " +
                "(description, amount, transaction_type, transaction_date, user_id, " +
                "asset_category_id, expense_category_id, split_pattern_id, " +
                "card_payment_discrepancy_id, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        
        LocalDateTime now = LocalDateTime.now();
        Long id = jdbcTemplate.queryForObject(sql, Long.class,
            discrepancyTransaction.getDescription(),
            discrepancyTransaction.getAmount(),
            discrepancyTransaction.getTransactionType().name(),
            discrepancyTransaction.getTransactionDate(),
            discrepancyTransaction.getUserId(),
            discrepancyTransaction.getAssetCategoryId(),
            discrepancyTransaction.getExpenseCategoryId(),
            discrepancyTransaction.getSplitPatternId(),
            discrepancyTransaction.getCardPaymentDiscrepancyId(),
            now);
        discrepancyTransaction.setId(id);
        discrepancyTransaction.setCreatedAt(now);
        return discrepancyTransaction;
    }
}
