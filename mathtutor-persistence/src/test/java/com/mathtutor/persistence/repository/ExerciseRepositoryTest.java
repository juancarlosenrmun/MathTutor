package com.mathtutor.persistence.repository;


import com.mathtutor.persistence.PersistenceTestApplication;
import com.mathtutor.persistence.PersistenceTestConfig;
import com.mathtutor.persistence.entity.Exercise;
import com.mathtutor.persistence.entity.Module;
import com.mathtutor.persistence.entity.Topic;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de integración para ExerciseRepository.
 */
@SpringBootTest(classes = PersistenceTestApplication.class)
@Import(PersistenceTestConfig.class)
@Transactional

class ExerciseRepositoryTest {

    @Autowired
    private ExerciseRepository exerciseRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Test
    void shouldSaveAndFindExerciseByExternalId() {
        // Given
        Module module = Module.builder().code("EX_MOD").name("Módulo Ejercicios").build();
        moduleRepository.save(module);

        Topic topic = Topic.builder().code("EX_TOP").name("Tema Ejercicios").difficulty(2).module(module).build();
        topicRepository.save(topic);

        Exercise exercise = Exercise.builder()
                .externalId("EX-001")
                .type(Exercise.ExerciseType.PROCEDIMENTAL)
                .difficulty(3)
                .statementLatex("x + 2 = 5")
                .referenceSolutionLatex("x = 3")
                .module(module)
                .topic(topic)
                .build();

        // When
        Exercise saved = exerciseRepository.save(exercise);
        Optional<Exercise> found = exerciseRepository.findByExternalId("EX-001");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getExternalId()).isEqualTo("EX-001");
        assertThat(found.get().getType()).isEqualTo(Exercise.ExerciseType.PROCEDIMENTAL);
        assertThat(found.get().getDifficulty()).isEqualTo(3);
        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void shouldFindByModule() {
        // Given
        Module module = Module.builder().code("EX_MOD2").name("Módulo 2").build();
        moduleRepository.save(module);

        Exercise ex1 = Exercise.builder().externalId("EX-002").type(Exercise.ExerciseType.TEORICO).difficulty(1).statementLatex("s1").referenceSolutionLatex("sol1").module(module).build();
        Exercise ex2 = Exercise.builder().externalId("EX-003").type(Exercise.ExerciseType.PROCEDIMENTAL).difficulty(2).statementLatex("s2").referenceSolutionLatex("sol2").module(module).build();
        exerciseRepository.save(ex1);
        exerciseRepository.save(ex2);

        // When
        List<Exercise> exercises = exerciseRepository.findByModule(module);

        // Then
        assertThat(exercises).hasSize(2);
    }

    @Test
    void shouldFindByTopic() {
        // Given
        Module module = Module.builder().code("EX_MOD3").name("Módulo 3").build();
        moduleRepository.save(module);

        Topic topic = Topic.builder().code("EX_TOP2").name("Tema 2").difficulty(1).module(module).build();
        topicRepository.save(topic);

        Exercise ex = Exercise.builder().externalId("EX-004").type(Exercise.ExerciseType.INTERPRETACION).difficulty(2).statementLatex("s").referenceSolutionLatex("sol").module(module).topic(topic).build();
        exerciseRepository.save(ex);

        // When
        List<Exercise> exercises = exerciseRepository.findByTopic(topic);

        // Then
        assertThat(exercises).hasSize(1);
        assertThat(exercises.get(0).getTopic().getCode()).isEqualTo("EX_TOP2");
    }

    @Test
    void shouldFindByModuleAndDifficultyRange() {
        // Given
        Module module = Module.builder().code("EX_MOD4").name("Módulo 4").build();
        moduleRepository.save(module);

        Exercise ex1 = Exercise.builder().externalId("EX-005").type(Exercise.ExerciseType.TEORICO).difficulty(1).statementLatex("s").referenceSolutionLatex("sol").module(module).build();
        Exercise ex2 = Exercise.builder().externalId("EX-006").type(Exercise.ExerciseType.TEORICO).difficulty(3).statementLatex("s").referenceSolutionLatex("sol").module(module).build();
        Exercise ex3 = Exercise.builder().externalId("EX-007").type(Exercise.ExerciseType.TEORICO).difficulty(5).statementLatex("s").referenceSolutionLatex("sol").module(module).build();
        exerciseRepository.save(ex1);
        exerciseRepository.save(ex2);
        exerciseRepository.save(ex3);

        // When
        List<Exercise> exercises = exerciseRepository.findByModuleAndDifficultyBetween(module, 2, 4);

        // Then
        assertThat(exercises).hasSize(1);
        assertThat(exercises.get(0).getExternalId()).isEqualTo("EX-006");
    }

    @Test
    void shouldFindByType() {
        // Given
        Module module = Module.builder().code("EX_MOD5").name("Módulo 5").build();
        moduleRepository.save(module);

        Exercise ex1 = Exercise.builder().externalId("EX-008").type(Exercise.ExerciseType.TEORICO).difficulty(1).statementLatex("s").referenceSolutionLatex("sol").module(module).build();
        Exercise ex2 = Exercise.builder().externalId("EX-009").type(Exercise.ExerciseType.PROCEDIMENTAL).difficulty(1).statementLatex("s").referenceSolutionLatex("sol").module(module).build();
        exerciseRepository.save(ex1);
        exerciseRepository.save(ex2);

        // When
        List<Exercise> teoricos = exerciseRepository.findByType(Exercise.ExerciseType.TEORICO);
        List<Exercise> procedimentales = exerciseRepository.findByType(Exercise.ExerciseType.PROCEDIMENTAL);

        // Then
        assertThat(teoricos).hasSize(1);
        assertThat(procedimentales).hasSize(1);
    }

    @Test
    void shouldReturnTrueWhenExternalIdExists() {
        // Given
        Module module = Module.builder().code("EX_MOD6").name("Módulo 6").build();
        moduleRepository.save(module);
        Exercise ex = Exercise.builder().externalId("EXISTS-EX").type(Exercise.ExerciseType.TEORICO).difficulty(1).statementLatex("s").referenceSolutionLatex("sol").module(module).build();
        exerciseRepository.save(ex);

        // When
        boolean exists = exerciseRepository.existsByExternalId("EXISTS-EX");

        // Then
        assertThat(exists).isTrue();
    }
}
