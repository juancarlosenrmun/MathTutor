package com.mathtutor.persistence.repository;


import com.mathtutor.persistence.PersistenceTestApplication;
import com.mathtutor.persistence.PersistenceTestConfig;
import com.mathtutor.persistence.entity.CanonicalStep;
import com.mathtutor.persistence.entity.Exercise;
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
 * Pruebas de integración para CanonicalStepRepository.
 */
@SpringBootTest(classes = PersistenceTestApplication.class)
@Import(PersistenceTestConfig.class)
@Transactional

class CanonicalStepRepositoryTest {

    @Autowired
    private CanonicalStepRepository stepRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private ExerciseRepository exerciseRepository;

    @Test
    void shouldSaveAndFindStepsByExerciseOrderedByPosition() {
        // Given
        Module module = Module.builder().code("STEP_MOD").name("Módulo Pasos").build();
        moduleRepository.save(module);

        Topic topic = Topic.builder().code("STEP_TOP").name("Tema Pasos").difficulty(1).module(module).build();
        topicRepository.save(topic);

        Exercise exercise = Exercise.builder()
                .externalId("STEP-EX-001")
                .type(Exercise.ExerciseType.PROCEDIMENTAL)
                .difficulty(2)
                .statementLatex("s")
                .referenceSolutionLatex("sol")
                .module(module)
                .topic(topic)
                .build();
        exerciseRepository.save(exercise);

        CanonicalStep step1 = CanonicalStep.builder().position(1).expressionLatex("x + 1 = 2").equivalentForms("[\"x = 1\"]").exercise(exercise).build();
        CanonicalStep step2 = CanonicalStep.builder().position(2).expressionLatex("x = 1").equivalentForms("[]").exercise(exercise).build();
        CanonicalStep step3 = CanonicalStep.builder().position(3).expressionLatex("Verificación: 1 + 1 = 2").equivalentForms("[]").exercise(exercise).build();
        stepRepository.save(step1);
        stepRepository.save(step2);
        stepRepository.save(step3);

        // When
        List<CanonicalStep> steps = stepRepository.findByExerciseOrderByPositionAsc(exercise);

        // Then
        assertThat(steps).hasSize(3);
        assertThat(steps.get(0).getPosition()).isEqualTo(1);
        assertThat(steps.get(1).getPosition()).isEqualTo(2);
        assertThat(steps.get(2).getPosition()).isEqualTo(3);
    }

    @Test
    void shouldFindByExerciseAndPosition() {
        // Given
        Module module = Module.builder().code("STEP_MOD2").name("Módulo Pasos 2").build();
        moduleRepository.save(module);

        Topic topic = Topic.builder().code("STEP_TOP2").name("Tema Pasos 2").difficulty(1).module(module).build();
        topicRepository.save(topic);

        Exercise exercise = Exercise.builder().externalId("STEP-EX-002").type(Exercise.ExerciseType.TEORICO).difficulty(1).statementLatex("s").referenceSolutionLatex("sol").module(module).topic(topic).build();
        exerciseRepository.save(exercise);

        CanonicalStep step = CanonicalStep.builder().position(5).expressionLatex("Paso 5").equivalentForms("[]").exercise(exercise).build();
        stepRepository.save(step);

        // When
        Optional<CanonicalStep> found = stepRepository.findByExerciseAndPosition(exercise, 5);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getPosition()).isEqualTo(5);
        assertThat(found.get().getExpressionLatex()).isEqualTo("Paso 5");
    }

    @Test
    void shouldReturnEmptyWhenPositionNotExists() {
        // Given
        Module module = Module.builder().code("STEP_MOD3").name("Módulo Pasos 3").build();
        moduleRepository.save(module);

        Topic topic = Topic.builder().code("STEP_TOP3").name("Tema Pasos 3").difficulty(1).module(module).build();
        topicRepository.save(topic);

        Exercise exercise = Exercise.builder().externalId("STEP-EX-003").type(Exercise.ExerciseType.TEORICO).difficulty(1).statementLatex("s").referenceSolutionLatex("sol").module(module).topic(topic).build();
        exerciseRepository.save(exercise);

        // When
        Optional<CanonicalStep> found = stepRepository.findByExerciseAndPosition(exercise, 999);

        // Then
        assertThat(found).isEmpty();
    }
}
