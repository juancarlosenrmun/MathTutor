package com.mathtutor.persistence.repository;

import com.mathtutor.persistence.entity.KeyConcept;
import com.mathtutor.persistence.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para entidad KeyConcept.
 */
@Repository
public interface KeyConceptRepository extends JpaRepository<KeyConcept, Long> {

    List<KeyConcept> findByTopic(Topic topic);
}