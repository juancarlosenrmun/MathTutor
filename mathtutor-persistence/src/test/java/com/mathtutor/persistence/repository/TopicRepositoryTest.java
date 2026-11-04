package com.mathtutor.persistence.repository;


import com.mathtutor.persistence.PersistenceTestApplication;
import com.mathtutor.persistence.PersistenceTestConfig;
import com.mathtutor.persistence.entity.Module;
import com.mathtutor.persistence.entity.Topic;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de integración para TopicRepository.
 */
@SpringBootTest(classes = PersistenceTestApplication.class)
@Import(PersistenceTestConfig.class)
@Transactional

class TopicRepositoryTest {

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Test
    void shouldSaveAndFindTopicByCode() {
        // Given
        Module module = Module.builder().code("ALG").name("Álgebra").build();
        moduleRepository.save(module);

        Topic topic = Topic.builder()
                .code("POLYNOMIALS")
                .name("Polinomios")
                .difficulty(3)
                .module(module)
                .build();

        // When
        Topic saved = topicRepository.save(topic);
        Optional<Topic> found = topicRepository.findByCode("POLYNOMIALS");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getCode()).isEqualTo("POLYNOMIALS");
        assertThat(found.get().getName()).isEqualTo("Polinomios");
        assertThat(found.get().getDifficulty()).isEqualTo(3);
        assertThat(found.get().getModule().getCode()).isEqualTo("ALG");
        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void shouldFindTopicsByModule() {
        // Given
        Module module = Module.builder().code("ALG2").name("Álgebra 2").build();
        moduleRepository.save(module);

        Topic topic1 = Topic.builder().code("T1").name("Tema 1").difficulty(1).module(module).build();
        Topic topic2 = Topic.builder().code("T2").name("Tema 2").difficulty(2).module(module).build();
        topicRepository.save(topic1);
        topicRepository.save(topic2);

        // When
        List<Topic> topics = topicRepository.findByModule(module);

        // Then
        assertThat(topics).hasSize(2);
    }

    @Test
    void shouldFindTopicsByModuleOrderedByDifficulty() {
        // Given
        Module module = Module.builder().code("ALG3").name("Álgebra 3").build();
        moduleRepository.save(module);

        Topic t1 = Topic.builder().code("HARD").name("Difícil").difficulty(5).module(module).build();
        Topic t2 = Topic.builder().code("EASY").name("Fácil").difficulty(1).module(module).build();
        Topic t3 = Topic.builder().code("MED").name("Medio").difficulty(3).module(module).build();
        topicRepository.save(t1);
        topicRepository.save(t2);
        topicRepository.save(t3);

        // When
        List<Topic> topics = topicRepository.findByModuleOrderByDifficultyAsc(module);

        // Then
        assertThat(topics).hasSize(3);
        assertThat(topics.get(0).getCode()).isEqualTo("EASY");
        assertThat(topics.get(1).getCode()).isEqualTo("MED");
        assertThat(topics.get(2).getCode()).isEqualTo("HARD");
    }

    @Test
    void shouldReturnTrueWhenCodeExists() {
        // Given
        Module module = Module.builder().code("ALG4").name("Álgebra 4").build();
        moduleRepository.save(module);
        Topic topic = Topic.builder().code("EXISTS").name("Existe").difficulty(1).module(module).build();
        topicRepository.save(topic);

        // When
        boolean exists = topicRepository.existsByCode("EXISTS");

        // Then
        assertThat(exists).isTrue();
    }
}
