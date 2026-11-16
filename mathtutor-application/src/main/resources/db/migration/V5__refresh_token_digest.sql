-- Los refresh son secretos aleatorios firmados: SHA-256 permite consulta indexada.
-- Los hashes Argon2 anteriores no son convertibles sin el token original.
-- Se revocan para exigir un nuevo inicio de sesion tras esta actualizacion.
UPDATE refresh_tokens
SET revoked = true, revoked_at = CURRENT_TIMESTAMP
WHERE LENGTH(token_hash) <> 64 AND revoked = false;

COMMENT ON COLUMN refresh_tokens.token_hash IS
    'Huella SHA-256 hexadecimal del refresh token de alta entropia; nunca se guarda en claro';
