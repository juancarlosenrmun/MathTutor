-- Migración inicial: esquema de autenticación
-- Tablas: users, audit_logs, password_reset_tokens

-- Extensión pgvector para embeddings (necesaria para otros módulos)
CREATE EXTENSION IF NOT EXISTS vector;

-- Tabla de usuarios
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('ALUMNO', 'PROFESOR', 'ADMIN')),
    active BOOLEAN NOT NULL DEFAULT true,
    anonymized_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices para users
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_active ON users(active);

-- Tabla de auditoría
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    action VARCHAR(100) NOT NULL,
    target_type VARCHAR(50),
    target_id BIGINT,
    actor_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices para audit_logs
CREATE INDEX idx_audit_logs_actor_id ON audit_logs(actor_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);

-- Tabla de tokens de restablecimiento de contraseña
CREATE TABLE password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices para password_reset_tokens
CREATE INDEX idx_password_reset_tokens_user_id ON password_reset_tokens(user_id);
CREATE INDEX idx_password_reset_tokens_expires_at ON password_reset_tokens(expires_at);
CREATE INDEX idx_password_reset_tokens_used_at ON password_reset_tokens(used_at);

-- Comentarios
COMMENT ON TABLE users IS 'Usuarios del sistema MathTutor';
COMMENT ON COLUMN users.password_hash IS 'Hash Argon2id de la contraseña';
COMMENT ON COLUMN users.role IS 'Rol: ALUMNO, PROFESOR, ADMIN';
COMMENT ON COLUMN users.anonymized_at IS 'Timestamp de anonimización (RGPD)';

COMMENT ON TABLE audit_logs IS 'Log de auditoría para acciones sensibles';
COMMENT ON COLUMN audit_logs.action IS 'Acción realizada (LOGIN, LOGOUT, REGISTER, etc.)';
COMMENT ON COLUMN audit_logs.target_type IS 'Tipo de entidad objetivo';
COMMENT ON COLUMN audit_logs.target_id IS 'ID de la entidad objetivo';

COMMENT ON TABLE password_reset_tokens IS 'Tokens para restablecimiento de contraseña';
COMMENT ON COLUMN password_reset_tokens.token_hash IS 'Hash del token (no se guarda en claro)';
COMMENT ON COLUMN password_reset_tokens.used_at IS 'Timestamp de uso (null si no usado)';