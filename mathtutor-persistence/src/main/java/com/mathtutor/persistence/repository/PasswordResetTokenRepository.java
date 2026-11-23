package com.mathtutor.persistence.repository;

import com.mathtutor.persistence.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/**
 * Repositorio para entidad PasswordResetToken.
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    @Query("SELECT t FROM PasswordResetToken t WHERE t.tokenHash = :tokenHash AND t.usedAt IS NULL AND t.expiresAt > :now")
    Optional<PasswordResetToken> findValidByTokenHash(@Param("tokenHash") String tokenHash, @Param("now") Instant now);

    /** @brief Identifica al propietario antes de bloquear el usuario y releer el token. */
    @Query("SELECT t.user.id FROM PasswordResetToken t WHERE t.tokenHash = :tokenHash")
    Optional<Long> findUserIdByTokenHash(@Param("tokenHash") String tokenHash);

    /** @brief Invalida todos los enlaces pendientes cuando se cambia la contraseña. */
    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE PasswordResetToken t SET t.usedAt = :now WHERE t.user.id = :userId AND t.usedAt IS NULL")
    void consumePending(@Param("userId") Long userId, @Param("now") Instant now);

    /** @brief Elimina los enlaces caducados al emitir uno nuevo para la cuenta. */
    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.user.id = :userId AND t.expiresAt <= :now")
    void deleteExpired(@Param("userId") Long userId, @Param("now") Instant now);

    void deleteByExpiresAtBefore(Instant now);
}
