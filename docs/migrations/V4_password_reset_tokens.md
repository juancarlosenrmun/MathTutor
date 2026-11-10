# Migración V4: password_reset_tokens

## Descripción
Tabla para tokens de restablecimiento de contraseña (flow "olvidé mi contraseña").

## Esquema
```sql
CREATE TABLE password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

## Índices
- `idx_password_reset_tokens_user_id` — Tokens de un usuario
- `idx_password_reset_tokens_expires_at` — Limpieza de tokens expirados
- `idx_password_reset_tokens_used_at` — Filtrado de tokens usados/no usados

## Columnas clave
| Columna | Tipo | Descripción |
|---------|------|-------------|
| `token_hash` | VARCHAR(255) | Hash del token (único, no se guarda en claro) |
| `expires_at` | TIMESTAMPTZ | Expiración (ej: 1 hora) |
| `used_at` | TIMESTAMPTZ | **NULL** = no usado, **timestamp** = ya usado (single-use) |
| `user_id` | BIGINT | FK → users(id), CASCADE delete |
| `created_at` | TIMESTAMPTZ | Auditoría: cuándo se generó |

## Flujo
1. Usuario solicita reset → se genera token aleatorio → se guarda hash en BD → se envía token en claro por email
2. Usuario hace clic en enlace → backend valida: hash coincide + `used_at IS NULL` + `expires_at > now`
3. Si válido: permite cambio de contraseña → marca `used_at = now()` → invalida token (single-use)
4. Limpieza periódica: borrar tokens con `expires_at < now()` o `used_at IS NOT NULL`

## Seguridad
- Token **single-use** (una vez usado, no se puede reutilizar)
- Expiración corta (recomendado: 1 hora)
- Hash Argon2id en BD (igual que refresh tokens)
- Rate limiting en endpoint de solicitud (previene spam email)