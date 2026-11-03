package com.mathtutor.persistence.repository;


import com.mathtutor.persistence.PersistenceTestApplication;
import com.mathtutor.persistence.PersistenceTestConfig;
import com.mathtutor.persistence.entity.Exercise;
import com.mathtutor.persistence.entity.ExerciseRun;
import com.mathtutor.persistence.entity.Module;
import com.mathtutor.persistence.entity.Topic;
import com.mathtutor.persistence.entity.TutorSession;
import com.mathtutor.persistence.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de integración para ExerciseRunRepository.
 */
@SpringBootTest(classes = PersistenceTestApplication.class)
@Import(PersistenceTestConfig.class)
@Transactional

class ExerciseRunRepositoryTest {

    @Autowired
    private ExerciseRunRepository runRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private ExerciseRepository exerciseRepository;

    @Autowired
    private TutorSessionRepository sessionRepository;

    @Test
    void shouldSaveAndFindRunBySession() {
        // Given
        User user = User.builder().email("run@test.com").passwordHash("h").role(User.Role.ALUMNO).active(true).build();
        userRepository.save(user);

        Module module = Module.builder().code("RUN_MOD").name("Módulo Run").build();
        moduleRepository.save(module);

        Topic topic = Topic.builder().code("RUN_TOP").name("Tema Run").difficulty(1).module(module).build();
        topicRepository.save(topic);

        Exercise exercise = Exercise.builder()
                .externalId("RUN-EX-001")
                .type(Exercise.ExerciseType.PROCEDIMENTAL)
                .difficulty(2)
                .statementLatex("x = 1")
                .referenceSolutionLatex("x = 1")
                .module(module)
                .topic(topic)
                .build();
        exerciseRepository.save(exercise);

        TutorSession session = TutorSession.builder().user(user).module(module).status(TutorSession.SessionStatus.ACTIVA).build();
        sessionRepository.save(session);

        ExerciseRun run = ExerciseRun.builder()
                .statementLatex("x + 1 = 2")
                .state(ExerciseRun.RunState.COMENZADO)
                .progress(0.0)
                .currentLevel(ExerciseRun.HelpLevel.CONCEPTUAL)
                .attempts(0)
                .exercise(exercise)
                .topic(topic)
                .session(session)
                .build();

        // When
        ExerciseRun saved = runRepository.save(run);
        List<ExerciseRun> runs = runRepository.findBySession(session);

        // Then
        assertThat(runs).hasSize(1);
        assertThat(runs.get(0).getState()).isEqualTo(ExerciseRun.RunState.COMENZADO);
        assertThat(runs.get(0).getCurrentLevel()).isEqualTo(ExerciseRun.HelpLevel.CONCEPTUAL);
        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void shouldFindBySessionAndState() {
        // Given
        User user = User.builder().email("run2@test.com").passwordHash("h").role(User.Role.ALUMNO).active(true).build();
        userRepository.save(user);

        Module module = Module.builder().code("RUN_MOD2").name("Módulo Run 2").build();
        moduleRepository.save(module);

        Topic topic = Topic.builder().code("RUN_TOP2").name("Tema Run 2").difficulty(1).module(module).build();
        topicRepository.save(topic);

        Exercise exercise = Exercise.builder().externalId("RUN-EX-002").type(Exercise.ExerciseType.TEORICO).difficulty(1).statementLatex("s").referenceSolutionLatex("sol").module(module).topic(topic).build();
        exerciseRepository.save(exercise);

        TutorSession session = TutorSession.builder().user(user).module(module).status(TutorSession.SessionStatus.ACTIVA).build();
        sessionRepository.save(session);

        ExerciseRun run1 = ExerciseRun.builder().statementLatex("s1").state(ExerciseRun.RunState.COMENZADO).progress(0.5).currentLevel(ExerciseRun.HelpLevel.CONCEPTUAL).attempts(1).exercise(exercise).topic(topic).session(session).build();
        ExerciseRun run2 = ExerciseRun.builder().statementLatex("s2").state(ExerciseRun.RunState.COMPLETADO).progress(1.0).currentLevel(ExerciseRun.HelpLevel.REVELACION).attempts(3).exercise(exercise).topic(topic).session(session).build();
        runRepository.save(run1);
        runRepository.save(run2);

        // When
        List<ExerciseRun> comenzados = runRepository.findBySessionAndState(session, ExerciseRun.RunState.COMENZADO);
        List<ExerciseRun> completados = runRepository.findBySessionAndState(session, ExerciseRun.RunState.COMPLETADO);

        // Then
        assertThat(comenzados).hasSize(1);
        assertThat(completados).hasSize(1);
    }

    @Test
    void shouldFindByExercise() {
        // Given
        User user = User.builder().email("run3@test.com").passwordHash("h").role(User.Role.ALUMNO).active(true).build();
        userRepository.save(user);

        Module module = Module.builder().code("RUN_MOD3").name("Módulo Run 3").build();
        moduleRepository.save(module);

        Topic topic = Topic.builder().code("RUN_TOP3").name("Tema Run 3").difficulty(1).module(module).build();
        topicRepository.save(topic);

        Exercise exercise = Exercise.builder().externalId("RUN-EX-003").type(Exercise.ExerciseType.PROCEDIMENTAL).difficulty(2).statementLatex("s").referenceSolutionLatex("sol").module(module).topic(topic).build();
        exerciseRepository.save(exercise);

        TutorSession session = TutorSession.builder().user(user).module(module).status(TutorSession.SessionStatus.ACTIVA).build();
        sessionRepository.save(session);

        ExerciseRun run = ExerciseRun.builder().statementLatex("s").state(ExerciseRun.RunState.COMENZADO).progress(0.0).currentLevel(ExerciseRun.HelpLevel.CONCEPTUAL).attempts(0).exercise(exercise).topic(topic).session(session).build();
        runRepository.save(run);

        // When
        List<ExerciseRun> runs = runRepository.findByExercise(exercise);

        // Then
        assertThat(runs).hasSize(1);
    }

    @Test
    void shouldFindByTopic() {
        // Given
        User user = User.builder().email("run4@test.com").passwordHash("h").role(User.Role.ALUMNO).active(true).build();
        userRepository.save(user);

        Module module = Module.builder().code("RUN_MOD4").name("Módulo Run 4").build();
        moduleRepository.save(module);

        Topic topic = Topic.builder().code("RUN_TOP4").name("Tema Run 4").difficulty(1).module(module).build();
        topicRepository.save(topic);

        Exercise exercise = Exercise.builder().externalId("RUN-EX-004").type(Exercise.ExerciseType.TEORICO).difficulty(1).statementLatex("s").referenceSolutionLatex("sol").module(module).topic(topic).build();
        exerciseRepository.save(exercise);

        TutorSession session = TutorSession.builder().user(user).module(module).status(TutorSession.SessionStatus.ACTIVA).build();
        sessionRepository.save(session);

        ExerciseRun run = ExerciseRun.builder().statementLatex("s").state(ExerciseRun.RunState.COMENZADO).progress(0.0).currentLevel(ExerciseRun.HelpLevel.CONCEPTUAL).attempts(0).exercise(exercise).topic(topic).session(session).build();
        runRepository.save(run);

        // When
        List<ExerciseRun> runs = runRepository.findByTopic(topic);

        // Then
        assertThat(runs).hasSize(1);
    }

    @Test
    void shouldFindBySessionOrderByStartedAtDesc() {
        // Given
        User user = User.builder().email("run5@test.com").passwordHash("h").role(User.Role.ALUMNO).active(true).build();
        userRepository.save(user);

        Module module = Module.builder().code("RUN_MOD5").name("Módulo Run 5").build();
        moduleRepository.save(module);

        Topic topic = Topic.builder().code("RUN_TOP5").name("Tema Run 5").difficulty(1).module(module).build();
        topicRepository.save(topic);

        Exercise exercise = Exercise.builder().externalId("RUN-EX-005").type(Exercise.ExerciseType.PROCEDIMENTAL).difficulty(2).statementLatex("s").referenceSolutionLatex("sol").module(module).topic(topic).build();
        exerciseRepository.save(exercise);

        TutorSession session = TutorSession.builder().user(user).module(module).status(TutorSession.SessionStatus.ACTIVA).build();
        sessionRepository.save(session);

        ExerciseRun run1 = ExerciseRun.builder().statementLatex("s1").state(ExerciseRun.RunState.COMPLETADO).progress(1.0).currentLevel(ExerciseRun.HelpLevel.REVELACION).attempts(2).exercise(exercise).topic(topic).session(session).build();
        ExerciseRun run2 = ExerciseRun.builder().statementLatex("s2").state(ExerciseRun.RunState.COMPLETADO).progress(1.0).currentLevel(ExerciseRun.HelpLevel.EJEMPLO_ANALOGO).attempts(1).exercise(exercise).topic(topic).session(session).build();
        runRepository.save(run1);
        runRepository.save(run2);

        // When
        List<ExerciseRun> runs = runRepository.findBySessionOrderByStartedAtDesc(session);

        // Then
        assertThat(runs).hasSize(2);
        // El más reciente primero
        assertThat(runs.get(0).getStartedAt()).isAfterOrEqualTo(runs.get(1).getStartedAt());
    }
}
