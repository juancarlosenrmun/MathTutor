-- UC-20/25: turnos sin ejercicio y reservas persistentes sin retener JDBC durante streaming.
ALTER TABLE exercise_runs ADD COLUMN verified_steps INTEGER NOT NULL DEFAULT 0;
ALTER TABLE exercise_runs ADD COLUMN reference_solution TEXT;
CREATE TABLE conversation_turns (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES tutor_sessions(id),
    exercise_run_id BIGINT REFERENCES exercise_runs(id),
    request_id UUID NOT NULL,
    message TEXT NOT NULL,
    response TEXT,
    topic VARCHAR(255),
    diagnosis VARCHAR(255),
    verdict VARCHAR(255),
    help_level VARCHAR(255),
    latency_ms BIGINT NOT NULL DEFAULT 0,
    tokens_in INTEGER,
    tokens_out INTEGER,
    status VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE(session_id, request_id)
);
CREATE INDEX conversation_history_idx ON conversation_turns(session_id, created_at, id);
CREATE UNIQUE INDEX conversation_active_idx ON conversation_turns(session_id) WHERE status = 'EN_CURSO';
