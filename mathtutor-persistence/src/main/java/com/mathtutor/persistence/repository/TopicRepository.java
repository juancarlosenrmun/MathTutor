package com.mathtutor.persistence.repository;

import com.mathtutor.persistence.entity.Module;
import com.mathtutor.persistence.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para entidad Topic.
 */
@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {

    Optional<Topic> findByCode(String code);

    boolean existsByCode(String code);

    List<Topic> findByModule(Module module);

    List<Topic> findByModuleOrderByDifficultyAsc(Module module);
}