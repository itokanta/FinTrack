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
import org.springframework.jdbc.core.RowMapper;

/**
 * カード引き落とし差異（card_payment_discrepancies テーブル）の DB アクセス。
 *
 * @author kanta ito
 */
@Repository
public class CardPaymentDiscrepancyRepository {

    private static final RowMapper<CardPaymentDiscrepancy> ROW_MAPPER = (rs, rowNum) -> {
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
    };

    private final NamedParameterJdbcTemplate template;

    public CardPaymentDiscrepancyRepository(NamedParameterJdbcTemplate template) {
        this.template = template;
    }

    /**
     * ID でカード引き落とし差異を検索する。
     *
     * @param id 差異 ID
     * @param userId ユーザー ID
     * @return 見つかれば差異を含む Optional、なければ空
     */
    public Optional<CardPaymentDiscrepancy> findById(Long id, Long userId) {
        String sql = "SELECT id, user_id, payment_month, bank_transaction_id, expected_amount, actual_amount, " +
                     "difference_amount, is_resolved, created_at, resolved_at " +
                     "FROM card_payment_discrepancies " +
                     "WHERE id = :id AND user_id = :userId";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("userId", userId);
        List<CardPaymentDiscrepancy> discrepancies = template.query(sql, params, ROW_MAPPER);
        if (discrepancies.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(discrepancies.get(0));
    }

    /**
     * ユーザーのカード引き落とし差異一覧を取得する。
     *
     * @param userId ユーザー ID
     * @return 差異のリスト（支払月・作成日時降順）
     */
    public List<CardPaymentDiscrepancy> findByUserId(Long userId) {
        String sql = "SELECT id, user_id, payment_month, bank_transaction_id, expected_amount, actual_amount, " +
                     "difference_amount, is_resolved, created_at, resolved_at " +
                     "FROM card_payment_discrepancies WHERE user_id = :userId ORDER BY payment_month DESC, created_at DESC";
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        return template.query(sql, params, ROW_MAPPER);
    }

    /**
     * 解消済み／未解消で差異一覧を絞り込む。
     *
     * @param userId ユーザー ID
     * @param isResolved 解消済みなら {@code true}
     * @return 条件に合う差異のリスト
     */
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

    /**
     * カード引き落とし差異を新規登録する。
     *
     * @param discrepancy 登録する差異（is_resolved は常に false で登録）
     * @return ID・作成日時が反映された差異
     */
    public CardPaymentDiscrepancy insert(CardPaymentDiscrepancy discrepancy) {
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
        return discrepancy;
    }

    /**
     * 差異を解消済みに更新する。
     *
     * @param id 差異 ID
     * @param userId ユーザー ID
     */
    public void markAsResolved(Long id, Long userId) {
        String sql = "UPDATE card_payment_discrepancies SET " +
                     "is_resolved = TRUE, resolved_at = :resolvedAt " +
                     "WHERE id = :id AND user_id = :userId";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("resolvedAt", LocalDateTime.now())
                .addValue("id", id)
                .addValue("userId", userId);
        template.update(sql, params);
    }
}
