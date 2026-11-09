-- V4__password_reset_tokens.sql
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
COMMENT ON TABLE password_reset_tokens IS 'Tokens para restablecimiento de contraseña';
COMMENT ON COLUMN password_reset_tokens.token_hash IS 'Hash del token (no se guarda en claro)';
COMMENT ON COLUMN password_reset_tokens.used_at IS 'Timestamp de uso (null si no usado)';