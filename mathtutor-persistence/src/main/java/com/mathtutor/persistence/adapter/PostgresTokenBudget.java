package com.mathtutor.persistence.adapter;

import com.mathtutor.common.port.LlmGateway;
import com.mathtutor.common.port.TokenBudget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

/** @brief Aplica el límite en una operación SQL atómica, incluso entre procesos y sesiones. */
@Component
public class PostgresTokenBudget implements TokenBudget {
    private static final Logger LOG = LoggerFactory.getLogger(PostgresTokenBudget.class);
    private final JdbcClient jdbc;
    private final long dailyLimit;
    private final BigDecimal inputRate;
    private final BigDecimal outputRate;

    public PostgresTokenBudget(JdbcClient jdbc, @Value("${tutor.quota.daily-tokens:100000}") long dailyLimit,
                              @Value("${tutor.quota.usd-per-million-input:-1}") BigDecimal inputRate,
                              @Value("${tutor.quota.usd-per-million-output:-1}") BigDecimal outputRate) {
        if (dailyLimit <= 0 || dailyLimit > Long.MAX_VALUE / 5) {
            throw new IllegalArgumentException("Límite diario de tokens no válido");
        }
        this.jdbc = jdbc;
        this.dailyLimit = dailyLimit;
        this.inputRate = inputRate;
        this.outputRate = outputRate;
    }

    @Override @Transactional
    public Optional<Reservation> reserve(long user, int input, int output) {
        var reservation = new Reservation(user, LocalDate.now(ZoneOffset.UTC), input, output);
        jdbc.sql("""
                INSERT INTO usage_quotas (user_id, quota_day, tokens_used, token_limit, created_at)
                VALUES (:user, :day, 0, :limit, CURRENT_TIMESTAMP)
                ON CONFLICT (user_id, quota_day) DO NOTHING
                """).param("user", user).param("day", reservation.day()).param("limit", dailyLimit).update();
        var consumption = jdbc.sql("""
                UPDATE usage_quotas SET tokens_used = tokens_used + :amount
                WHERE user_id = :user AND quota_day = :day AND :amount <= token_limit - tokens_used
                RETURNING tokens_used, token_limit
                """).param("user", user).param("day", reservation.day()).param("amount", reservation.total())
                .query((row, index) -> new Consumption(row.getLong(1), row.getLong(2))).optional();
        return consumption.map(value -> {
            long threshold = value.limit() - value.limit() / 5;
            if (value.used() >= threshold && value.used() - reservation.total() < threshold) {
                LOG.warn("event=tutor_quota_80_percent budget_day={} tokens_committed={} daily_limit={}",
                        reservation.day(), value.used(), value.limit());
            }
            return reservation;
        });
    }

    @Override @Transactional
    public void settle(Reservation reservation, LlmGateway.Usage usage) {
        int input = measured(usage == null ? null : usage.inputTokens(), reservation.input());
        int output = measured(usage == null ? null : usage.outputTokens(), reservation.output());
        boolean priced = inputRate.signum() >= 0 && outputRate.signum() >= 0;
        BigDecimal cost = priced ? inputRate.multiply(BigDecimal.valueOf(input))
                .add(outputRate.multiply(BigDecimal.valueOf(output))).movePointLeft(6) : BigDecimal.ZERO;
        var average = jdbc.sql("""
                UPDATE usage_quotas
                SET tokens_used = tokens_used + :difference,
                    cost_total = cost_total + :cost, priced_turns = priced_turns + :priced
                WHERE user_id = :user AND quota_day = :day
                RETURNING cost_total / NULLIF(priced_turns, 0)
                """).param("difference", (long) input + output - reservation.total())
                .param("cost", cost).param("priced", priced ? 1 : 0)
                .param("user", reservation.user()).param("day", reservation.day())
                .query((row, index) -> Optional.ofNullable(row.getBigDecimal(1))).single();
        LOG.info("event=tutor_turn_cost mean_turn_cost_usd={} input_tokens={} output_tokens={}",
                average.map(BigDecimal::toPlainString).orElse("sin_tarifa"), input, output);
    }

    private int measured(Integer tokens, int estimate) {
        return tokens == null || tokens < 0 ? estimate : tokens;
    }

    private record Consumption(long used, long limit) {}
}
