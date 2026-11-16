-- EN-01: completa el esquema de las entidades ya mapeadas.
-- No añade flujos ni casos de uso. Flyway crea el esquema antes de hibernate.validate.

CREATE TABLE modules (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL
);

CREATE TABLE topics (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    difficulty INTEGER NOT NULL,
    module_id BIGINT NOT NULL REFERENCES modules(id)
);

CREATE TABLE exercises (
    id BIGSERIAL PRIMARY KEY,
    external_id VARCHAR(100) NOT NULL UNIQUE,
    type VARCHAR(20) NOT NULL,
    difficulty INTEGER NOT NULL,
    statement_latex TEXT NOT NULL,
    reference_solution_latex TEXT NOT NULL,
    module_id BIGINT REFERENCES modules(id),
    topic_id BIGINT REFERENCES topics(id)
);

CREATE TABLE canonical_steps (
    id BIGSERIAL PRIMARY KEY,
    position INTEGER NOT NULL,
    expression_latex TEXT NOT NULL,
    equivalent_forms TEXT,
    exercise_id BIGINT NOT NULL REFERENCES exercises(id)
);

CREATE TABLE typical_errors (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    seed_hint TEXT,
    canonical_step_id BIGINT NOT NULL REFERENCES canonical_steps(id)
);

CREATE TABLE key_concepts (
    id BIGSERIAL PRIMARY KEY,
    text TEXT NOT NULL,
    topic_id BIGINT NOT NULL REFERENCES topics(id)
);

CREATE TABLE note_chunks (
    id BIGSERIAL PRIMARY KEY,
    content TEXT NOT NULL,
    -- @Lob byte[] existente: almacenamiento OID de PostgreSQL.
    -- El almacén vectorial Spring AI usa su propia tabla vector_store.
    embedding OID,
    chunk_type VARCHAR(30),
    source_file VARCHAR(500),
    module_code VARCHAR(50),
    topic_code VARCHAR(50),
    version VARCHAR(20),
    module_id BIGINT REFERENCES modules(id),
    topic_id BIGINT REFERENCES topics(id)
);
CREATE INDEX idx_note_chunk_module_topic ON note_chunks(module_code, topic_code);
CREATE INDEX idx_note_chunk_embedding ON note_chunks(embedding);

CREATE TABLE tutor_sessions (
    id BIGSERIAL PRIMARY KEY,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_activity_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(20) NOT NULL,
    summary TEXT,
    user_id BIGINT NOT NULL REFERENCES users(id),
    module_id BIGINT REFERENCES modules(id)
);

CREATE TABLE exercise_runs (
    id BIGSERIAL PRIMARY KEY,
    statement_latex TEXT NOT NULL,
    state VARCHAR(20) NOT NULL,
    progress DOUBLE PRECISION NOT NULL,
    current_level VARCHAR(20) NOT NULL,
    attempts INTEGER NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ended_at TIMESTAMP WITH TIME ZONE,
    exercise_id BIGINT REFERENCES exercises(id),
    topic_id BIGINT REFERENCES topics(id),
    session_id BIGINT NOT NULL REFERENCES tutor_sessions(id)
);

CREATE TABLE turns (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(20) NOT NULL,
    user_input_latex TEXT,
    tutor_response TEXT,
    help_level VARCHAR(20),
    event_type VARCHAR(30),
    payload_json TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    exercise_run_id BIGINT NOT NULL REFERENCES exercise_runs(id)
);

CREATE TABLE consent_records (
    id BIGSERIAL PRIMARY KEY,
    consent_type VARCHAR(30) NOT NULL,
    accepted BOOLEAN NOT NULL,
    policy_version VARCHAR(50) NOT NULL,
    consented_at TIMESTAMP WITH TIME ZONE NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id)
);

CREATE TABLE usage_quotas (
    id BIGSERIAL PRIMARY KEY,
    quota_day DATE NOT NULL,
    tokens_used BIGINT NOT NULL,
    token_limit BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id),
    UNIQUE (user_id, quota_day)
);
