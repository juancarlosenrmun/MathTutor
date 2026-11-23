package com.mathtutor.persistence.repository;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

class ExerciseStatisticsTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);

    @Test
    void inactivityStartsAfterTheConfiguredLimit() {
        var statistics = new ExerciseStatistics(jdbc, 5, .5);
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        Instant limit = now.minusSeconds(300);
        assertThat(statistics.expired(limit.minusNanos(1), now)).isTrue();
        assertThat(statistics.expired(limit, now)).isFalse();
        assertThat(statistics.expired(limit.plusNanos(1), now)).isFalse();
    }

    @Test
    void validatesThresholdConfigurationAtStartup() {
        assertThatIllegalArgumentException().isThrownBy(() -> new ExerciseStatistics(jdbc, 0, .5));
        for (double invalid : new double[]{Double.NaN, -.01, 1.01}) {
            assertThatIllegalArgumentException().isThrownBy(() -> new ExerciseStatistics(jdbc, 30, invalid));
        }
        assertThat(new ExerciseStatistics(jdbc, 30, 0)).isNotNull();
        assertThat(new ExerciseStatistics(jdbc, 30, 1)).isNotNull();
    }
}
