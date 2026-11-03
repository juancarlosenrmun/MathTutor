package com.mathtutor.persistence.repository;

import com.mathtutor.persistence.entity.CanonicalStep;
import com.mathtutor.persistence.entity.TypicalError;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para entidad TypicalError.
 */
@Repository
public interface TypicalErrorRepository extends JpaRepository<TypicalError, Long> {

    List<TypicalError> findByCanonicalStep(CanonicalStep canonicalStep);

    Optional<TypicalError> findByCanonicalStepAndCode(CanonicalStep canonicalStep, String code);
}