# Migración V2: refresh_tokens

## Descripción
Tabla para tokens de refresco JWT con **rotación automática** y **detección de reuso** (token reuse detection).

## Esquema
```sql
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
```

## Índices
- `idx_refresh_tokens_user_id` — Tokens de un usuario
- `idx_refresh_tokens_family_id` — **Crítico**: revocación de familia completa al detectar reuso
- `idx_refresh_tokens_expires_at` — Limpieza de tokens expirados
- `idx_refresh_tokens_revoked` — Filtrado de tokens revocados
- `idx_refresh_tokens_token_hash` — Lookup por hash (validación)

## Columnas clave
| Columna | Tipo | Descripción |
|---------|------|-------------|
| `token_hash` | VARCHAR(255) | Hash Argon2id del refresh token (único) |
| `user_id` | BIGINT | FK → users(id), CASCADE delete |
| `family_id` | VARCHAR(36) | UUID de familia para rotación |
| `revoked` | BOOLEAN | Si el token está revocado |
| `revoked_at` | TIMESTAMPTZ | Cuándo fue revocado |
| `expires_at` | TIMESTAMPTZ | Expiración (7 días por defecto) |

## Rotación y detección de reuso
1. **Login**: Se crea familia nueva (`family_id` aleatorio), se emite refresh token
2. **Refresh**: Se valida token → se revoca token actual → se emite nuevo token en **misma familia**
3. **Reuso detectado**: Si se presenta un token ya revocado de la misma familia → **revoca toda la familia** (token theft protection)
4. **Logout**: Revoca token actual (y opcionalmente toda la familia)

## Seguridad
- Tokens **nunca** se guardan en claro (solo hash Argon2id)
- `family_id` permite rastrear toda la cadena de rotación
- Revocación en cascada previene uso de tokens robados