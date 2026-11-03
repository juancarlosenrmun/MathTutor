package com.mathtutor.persistence.repository;

import com.mathtutor.persistence.entity.CanonicalStep;
import com.mathtutor.persistence.entity.Exercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para entidad CanonicalStep.
 */
@Repository
public interface CanonicalStepRepository extends JpaRepository<CanonicalStep, Long> {

    List<CanonicalStep> findByExerciseOrderByPositionAsc(Exercise exercise);

    Optional<CanonicalStep> findByExerciseAndPosition(Exercise exercise, int position);
}