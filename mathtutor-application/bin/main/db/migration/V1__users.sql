-- V1__users.sql
-- Tabla de usuarios

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

-- Comentarios
COMMENT ON TABLE users IS 'Usuarios del sistema MathTutor';
COMMENT ON COLUMN users.password_hash IS 'Hash Argon2id de la contraseña';
COMMENT ON COLUMN users.role IS 'Rol: ALUMNO, PROFESOR, ADMIN';
COMMENT ON COLUMN users.anonymized_at IS 'Timestamp de anonimización (RGPD)';