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

/**
 * Repositorio para entidad ExerciseRun.
 */
@Repository
public interface ExerciseRunRepository extends JpaRepository<ExerciseRun, Long> {

    List<ExerciseRun> findBySession(TutorSession session);

    List<ExerciseRun> findBySessionAndState(TutorSession session, ExerciseRun.RunState state);

    List<ExerciseRun> findByExercise(Exercise exercise);

    List<ExerciseRun> findByTopic(Topic topic);

    @Query("SELECT r FROM ExerciseRun r WHERE r.session = :session ORDER BY r.startedAt DESC")
    List<ExerciseRun> findBySessionOrderByStartedAtDesc(@Param("session") TutorSession session);

    Optional<ExerciseRun> findBySessionAndExercise(TutorSession session, Exercise exercise);
}