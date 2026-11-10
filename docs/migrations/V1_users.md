# Migración V1: users

## Descripción
Tabla principal de usuarios del sistema MathTutor.

## Esquema
```sql
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('ALUMNO', 'PROFESOR', 'ADMIN')),
    active BOOLEAN NOT NULL DEFAULT true,
    anonymized_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

## Índices
- `idx_users_email` — Búsqueda por email (login, registro)
- `idx_users_active` — Filtrado de usuarios activos

## Columnas clave
| Columna | Tipo | Descripción |
|---------|------|-------------|
| `id` | BIGSERIAL | PK, identificador único |
| `email` | VARCHAR(255) | Email único, usado para login |
| `password_hash` | VARCHAR(255) | Hash Argon2id (memory=64MB, iterations=3, parallelism=4) |
| `role` | VARCHAR(20) | ALUMNO, PROFESOR, ADMIN (CHECK constraint) |
| `active` | BOOLEAN | Soft delete / desactivación |
| `anonymized_at` | TIMESTAMPTZ | RGPD: timestamp de anonimización |
| `created_at` | TIMESTAMPTZ | Auditoría: creación |

## Notas
- La extensión `vector` (pgvector) se crea aquí para estar disponible en migraciones posteriores.
- `password_hash` nunca almacena contraseñas en claro.
- `anonymized_at` implementa el derecho al olvido (RGPD).