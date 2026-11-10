-- V3__audit_logs.sql
-- Tabla de auditoría

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

-- Comentarios
COMMENT ON TABLE audit_logs IS 'Log de auditoría para acciones sensibles';
COMMENT ON COLUMN audit_logs.action IS 'Acción realizada (LOGIN, LOGOUT, REGISTER, etc.)';
COMMENT ON COLUMN audit_logs.target_type IS 'Tipo de entidad objetivo';
COMMENT ON COLUMN audit_logs.target_id IS 'ID de la entidad objetivo';