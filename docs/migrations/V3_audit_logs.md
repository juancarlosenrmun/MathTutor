# Migración V3: audit_logs

## Descripción
Tabla de auditoría para registrar eventos de seguridad y acciones sensibles (ASVS L1: Logging).

## Esquema
```sql
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    action VARCHAR(100) NOT NULL,
    target_type VARCHAR(50),
    target_id BIGINT,
    actor_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

## Índices
- `idx_audit_logs_actor_id` — Auditoría por usuario
- `idx_audit_logs_created_at` — Consultas temporales / retención
- `idx_audit_logs_action` — Filtrado por tipo de acción

## Columnas clave
| Columna | Tipo | Descripción |
|---------|------|-------------|
| `action` | VARCHAR(100) | Acción: LOGIN, LOGOUT, REGISTER, LOGIN_FAILED, REGISTER_FAILED, REFRESH, REUSE_DETECTED |
| `target_type` | VARCHAR(50) | Tipo entidad objetivo (ej: "user", "refresh_token") |
| `target_id` | BIGINT | ID de la entidad objetivo |
| `actor_id` | BIGINT | FK → users(id), SET NULL al borrar usuario |
| `created_at` | TIMESTAMPTZ | Timestamp del evento |

## Acciones registradas (Auth)
| Acción | Descripción |
|--------|-------------|
| `LOGIN` | Inicio de sesión exitoso |
| `LOGIN_FAILED` | Credenciales inválidas |
| `LOGOUT` | Cierre de sesión |
| `REGISTER` | Registro exitoso |
| `REGISTER_FAILED` | Fallo en registro |
| `REFRESH` | Renovación de access token |
| `REUSE_DETECTED` | **Crítico**: Reuso de refresh token detectado → familia revocada |

## Cumplimiento
- **ASVS 4.2.1**: Logging de eventos de autenticación
- **ASVS 4.2.2**: Logging de fallos de autenticación
- **ASVS 4.3.1**: Logging de eventos de gestión de sesión
- **RGPD**: `actor_id` SET NULL permite anonimización