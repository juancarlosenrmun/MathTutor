package com.mathtutor.persistence.repository;

import com.mathtutor.persistence.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para RefreshToken con consultas personalizadas para rotación y detección de reuso.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.tokenHash = :tokenHash")
    Optional<RefreshToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    /**
     * Busca un refresh token por su hash.
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Obtiene el propietario sin asociar la entidad al contexto de persistencia.
     * Permite adquirir el bloqueo del usuario antes de releer el token bloqueado.
     */
    @Query("SELECT rt.userId FROM RefreshToken rt WHERE rt.tokenHash = :tokenHash")
    Optional<Long> findUserIdByTokenHash(@Param("tokenHash") String tokenHash);

    /**
     * Busca todos los tokens de una familia (para detección de reuso).
     */
    List<RefreshToken> findByFamilyId(String familyId);

    /**
     * Busca tokens activos (no revocados) de un usuario.
     */
    List<RefreshToken> findByUserIdAndRevokedFalse(Long userId);

    /**
     * Elimina tokens expirados (para limpieza periódica).
     */
    void deleteByExpiresAtBefore(Instant now);

    /**
     * Revoca todos los tokens de una familia (detección de reuso).
     * Retorna el número de tokens revocados.
     */
    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken rt SET rt.revoked = true, rt.revokedAt = :revokedAt WHERE rt.familyId = :familyId AND rt.revoked = false")
    int revokeFamily(@Param("familyId") String familyId, @Param("revokedAt") Instant revokedAt);

    /** @brief Invalida las sesiones renovables de la cuenta tras un cambio de contraseña. */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true, rt.revokedAt = :now WHERE rt.userId = :userId AND rt.revoked = false")
    void revokeAllForUser(@Param("userId") Long userId, @Param("now") Instant now);
}
