-- V2__refresh_tokens.sql
-- Tabla para tokens de refresco con rotación y detección de reuso

CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token_hash VARCHAR(255) NOT NULL UNIQUE,  -- Hash Argon2id del refresh token
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    family_id VARCHAR(36) NOT NULL,  -- UUID para familia de tokens (rotación)
    revoked BOOLEAN NOT NULL DEFAULT false,
    revoked_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices para refresh_tokens
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_family_id ON refresh_tokens(family_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
CREATE INDEX idx_refresh_tokens_revoked ON refresh_tokens(revoked);
CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens(token_hash);

-- Comentarios
COMMENT ON TABLE refresh_tokens IS 'Tokens de refresco JWT con rotación y detección de reuso';
COMMENT ON COLUMN refresh_tokens.token_hash IS 'Hash Argon2id del token de refresco (no se guarda en claro)';
COMMENT ON COLUMN refresh_tokens.family_id IS 'ID de familia para rastrear rotación de tokens';
COMMENT ON COLUMN refresh_tokens.revoked IS 'Si el token ha sido revocado (logout, rotación, o reuso detectado)';
COMMENT ON COLUMN refresh_tokens.revoked_at IS 'Timestamp de revocación';