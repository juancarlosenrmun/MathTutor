package com.mathtutor.persistence.repository;

import com.mathtutor.persistence.entity.Exercise;
import com.mathtutor.persistence.entity.Module;
import com.mathtutor.persistence.entity.Topic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para entidad Exercise.
 */
@Repository
public interface ExerciseRepository extends JpaRepository<Exercise, Long> {

    Optional<Exercise> findByExternalId(String externalId);

    boolean existsByExternalId(String externalId);

    List<Exercise> findByModule(Module module);

    List<Exercise> findByTopic(Topic topic);

    List<Exercise> findByModuleAndDifficultyBetween(Module module, int minDifficulty, int maxDifficulty);

    List<Exercise> findByTopicAndDifficultyBetween(Topic topic, int minDifficulty, int maxDifficulty);

    List<Exercise> findByType(Exercise.ExerciseType type);

    Page<Exercise> findByModule(Module module, Pageable pageable);

    Page<Exercise> findByTopic(Topic topic, Pageable pageable);
}