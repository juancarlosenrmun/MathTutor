package com.mathtutor.persistence.repository;


import com.mathtutor.persistence.PersistenceTestApplication;
import com.mathtutor.persistence.PersistenceTestConfig;
import com.mathtutor.persistence.entity.CanonicalStep;
import com.mathtutor.persistence.entity.Exercise;
import com.mathtutor.persistence.entity.Module;
import com.mathtutor.persistence.entity.Topic;
import com.mathtutor.persistence.entity.TypicalError;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de integración para TypicalErrorRepository.
 */
@SpringBootTest(classes = PersistenceTestApplication.class)
@Import(PersistenceTestConfig.class)
@Transactional

class TypicalErrorRepositoryTest {

    @Autowired
    private TypicalErrorRepository errorRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private ExerciseRepository exerciseRepository;

    @Autowired
    private CanonicalStepRepository stepRepository;

    @Test
    void shouldSaveAndFindErrorsByCanonicalStep() {
        // Given
        Module module = Module.builder().code("ERR_MOD").name("Módulo Errores").build();
        moduleRepository.save(module);

        Topic topic = Topic.builder().code("ERR_TOP").name("Tema Errores").difficulty(1).module(module).build();
        topicRepository.save(topic);

        Exercise exercise = Exercise.builder().externalId("ERR-EX-001").type(Exercise.ExerciseType.PROCEDIMENTAL).difficulty(2).statementLatex("s").referenceSolutionLatex("sol").module(module).topic(topic).build();
        exerciseRepository.save(exercise);

        CanonicalStep step = CanonicalStep.builder().position(1).expressionLatex("x + 1 = 2").equivalentForms("[]").exercise(exercise).build();
        stepRepository.save(step);

        TypicalError error1 = TypicalError.builder().code("E001").seedHint("Revisa la suma").canonicalStep(step).build();
        TypicalError error2 = TypicalError.builder().code("E002").seedHint("Signo incorrecto").canonicalStep(step).build();
        errorRepository.save(error1);
        errorRepository.save(error2);

        // When
        List<TypicalError> errors = errorRepository.findByCanonicalStep(step);

        // Then
        assertThat(errors).hasSize(2);
    }

    @Test
    void shouldFindByCanonicalStepAndCode() {
        // Given
        Module module = Module.builder().code("ERR_MOD2").name("Módulo Errores 2").build();
        moduleRepository.save(module);

        Topic topic = Topic.builder().code("ERR_TOP2").name("Tema Errores 2").difficulty(1).module(module).build();
        topicRepository.save(topic);

        Exercise exercise = Exercise.builder().externalId("ERR-EX-002").type(Exercise.ExerciseType.TEORICO).difficulty(1).statementLatex("s").referenceSolutionLatex("sol").module(module).topic(topic).build();
        exerciseRepository.save(exercise);

        CanonicalStep step = CanonicalStep.builder().position(1).expressionLatex("Paso").equivalentForms("[]").exercise(exercise).build();
        stepRepository.save(step);

        TypicalError error = TypicalError.builder().code("UNIQUE_CODE").seedHint("Pista").canonicalStep(step).build();
        errorRepository.save(error);

        // When
        Optional<TypicalError> found = errorRepository.findByCanonicalStepAndCode(step, "UNIQUE_CODE");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getCode()).isEqualTo("UNIQUE_CODE");
        assertThat(found.get().getSeedHint()).isEqualTo("Pista");
    }

    @Test
    void shouldReturnEmptyWhenCodeNotExists() {
        // Given
        Module module = Module.builder().code("ERR_MOD3").name("Módulo Errores 3").build();
        moduleRepository.save(module);

        Topic topic = Topic.builder().code("ERR_TOP3").name("Tema Errores 3").difficulty(1).module(module).build();
        topicRepository.save(topic);

        Exercise exercise = Exercise.builder().externalId("ERR-EX-003").type(Exercise.ExerciseType.TEORICO).difficulty(1).statementLatex("s").referenceSolutionLatex("sol").module(module).topic(topic).build();
        exerciseRepository.save(exercise);

        CanonicalStep step = CanonicalStep.builder().position(1).expressionLatex("Paso").equivalentForms("[]").exercise(exercise).build();
        stepRepository.save(step);

        // When
        Optional<TypicalError> found = errorRepository.findByCanonicalStepAndCode(step, "NONEXISTENT");

        // Then
        assertThat(found).isEmpty();
    }
}
