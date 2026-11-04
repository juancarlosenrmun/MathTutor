package com.mathtutor.persistence.repository;


import com.mathtutor.persistence.PersistenceTestApplication;
import com.mathtutor.persistence.PersistenceTestConfig;
import com.mathtutor.persistence.entity.KeyConcept;
import com.mathtutor.persistence.entity.Module;
import com.mathtutor.persistence.entity.Topic;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de integración para KeyConceptRepository.
 */
@SpringBootTest(classes = PersistenceTestApplication.class)
@Import(PersistenceTestConfig.class)
@Transactional

class KeyConceptRepositoryTest {

    @Autowired
    private KeyConceptRepository conceptRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Test
    void shouldSaveAndFindConceptsByTopic() {
        // Given
        Module module = Module.builder().code("CONC_MOD").name("Módulo Conceptos").build();
        moduleRepository.save(module);

        Topic topic = Topic.builder().code("CONC_TOP").name("Tema Conceptos").difficulty(1).module(module).build();
        topicRepository.save(topic);

        KeyConcept concept1 = KeyConcept.builder().text("Concepto clave 1").topic(topic).build();
        KeyConcept concept2 = KeyConcept.builder().text("Concepto clave 2").topic(topic).build();
        conceptRepository.save(concept1);
        conceptRepository.save(concept2);

        // When
        List<KeyConcept> concepts = conceptRepository.findByTopic(topic);

        // Then
        assertThat(concepts).hasSize(2);
        assertThat(concepts.get(0).getText()).isIn("Concepto clave 1", "Concepto clave 2");
    }

    @Test
    void shouldReturnEmptyWhenNoConcepts() {
        // Given
        Module module = Module.builder().code("CONC_MOD2").name("Módulo Conceptos 2").build();
        moduleRepository.save(module);

        Topic topic = Topic.builder().code("CONC_TOP2").name("Tema Conceptos 2").difficulty(1).module(module).build();
        topicRepository.save(topic);

        // When
        List<KeyConcept> concepts = conceptRepository.findByTopic(topic);

        // Then
        assertThat(concepts).isEmpty();
    }
}
