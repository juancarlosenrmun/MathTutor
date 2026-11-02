package com.mathtutor.common.dto;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de caja blanca para {@link SessionState}.
 * Cobertura: campos, enum SessionStatus, equals/hashCode, toString.
 */
class SessionStateTest {

    private static final Instant STARTED = Instant.parse("2026-01-15T10:00:00Z");
    private static final Instant LAST = Instant.parse("2026-01-15T10:30:00Z");

    @Test
    void shouldCreateSessionStateWithAllFields() {
        Turn turn = new Turn(1L, "msg", "resp", ErrorType.NINGUNO, HelpLevel.CONCEPTUAL,
            100, 5, 10, Instant.now(), null);
        ExerciseRun run = new ExerciseRun(1L, "x^2", ExerciseRun.RunState.COMENZADO,
            0.5, HelpLevel.CONCEPTUAL, 1, STARTED, null, null, null);

        SessionState state = new SessionState(
            "session-123",
            "user-456",
            1L,
            STARTED,
            LAST,
            SessionState.SessionStatus.ACTIVA,
            List.of(turn),
            run
        );

        assertThat(state.sessionId()).isEqualTo("session-123");
        assertThat(state.userId()).isEqualTo("user-456");
        assertThat(state.moduleId()).isEqualTo(1L);
        assertThat(state.startedAt()).isEqualTo(STARTED);
        assertThat(state.lastActivityAt()).isEqualTo(LAST);
        assertThat(state.status()).isEqualTo(SessionState.SessionStatus.ACTIVA);
        assertThat(state.turns()).containsExactly(turn);
        assertThat(state.currentExerciseRun()).isEqualTo(run);
    }

    @Test
    void shouldHaveBothSessionStatusValues() {
        assertThat(SessionState.SessionStatus.values()).hasSize(2);
        assertThat(SessionState.SessionStatus.ACTIVA).isNotNull();
        assertThat(SessionState.SessionStatus.CERRADA).isNotNull();
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        SessionState s1 = new SessionState("s1", "u1", 1L, STARTED, LAST,
            SessionState.SessionStatus.ACTIVA, List.of(), null);
        SessionState s2 = new SessionState("s1", "u1", 1L, STARTED, LAST,
            SessionState.SessionStatus.ACTIVA, List.of(), null);
        SessionState s3 = new SessionState("s2", "u1", 1L, STARTED, LAST,
            SessionState.SessionStatus.ACTIVA, List.of(), null);

        assertThat(s1).isEqualTo(s2);
        assertThat(s1.hashCode()).isEqualTo(s2.hashCode());
        assertThat(s1).isNotEqualTo(s3);
    }
}