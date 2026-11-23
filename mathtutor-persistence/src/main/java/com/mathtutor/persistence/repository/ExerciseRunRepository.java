package com.mathtutor.persistence.repository;

import com.mathtutor.persistence.entity.ExerciseRun;
import com.mathtutor.persistence.entity.TutorSession;
import com.mathtutor.persistence.entity.Exercise;
import com.mathtutor.persistence.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Repositorio para entidad ExerciseRun.
 */
@Repository
public interface ExerciseRunRepository extends JpaRepository<ExerciseRun, Long> {

    List<ExerciseRun> findBySessionId(Long sessionId, org.springframework.data.domain.Pageable page);
    Optional<ExerciseRun> findFirstBySessionIdAndStateOrderByStartedAtDescIdDesc(Long sessionId, ExerciseRun.RunState state);

    List<ExerciseRun> findBySession(TutorSession session);

    List<ExerciseRun> findBySessionAndState(TutorSession session, ExerciseRun.RunState state);

    List<ExerciseRun> findByExercise(Exercise exercise);

    List<ExerciseRun> findByTopic(Topic topic);

    @Query("SELECT r FROM ExerciseRun r WHERE r.session = :session ORDER BY r.startedAt DESC")
    List<ExerciseRun> findBySessionOrderByStartedAtDesc(@Param("session") TutorSession session);

    Optional<ExerciseRun> findBySessionAndExercise(TutorSession session, Exercise exercise);

    /** @brief Acredita cada etapa con el historial del mismo alumno y módulo, incluidas sesiones cerradas. */
    @Query("""
            SELECT DISTINCT e.type FROM ExerciseRun r JOIN r.exercise e JOIN r.session s
            WHERE s.user.id = :userId AND s.module.id = :moduleId
              AND ((e.type = :procedural AND r.state = :completed)
                   OR (e.type <> :procedural AND r.attempts > 0))
            """)
    Set<Exercise.ExerciseType> findPracticedTypes(Long userId, Long moduleId,
            Exercise.ExerciseType procedural, ExerciseRun.RunState completed);
}
