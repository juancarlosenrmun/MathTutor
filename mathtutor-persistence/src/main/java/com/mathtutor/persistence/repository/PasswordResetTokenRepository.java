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

    void deleteByExpiresAtBefore(Instant now);
}