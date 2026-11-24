package com.mathtutor.persistence.adapter;

import com.mathtutor.common.dto.*;
import com.mathtutor.common.port.ConversationStore;
import com.mathtutor.persistence.PersistenceTestApplication;
import com.mathtutor.persistence.PersistenceTestConfig;
import com.mathtutor.persistence.entity.*;
import com.mathtutor.persistence.entity.Exercise;
import com.mathtutor.persistence.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(classes = PersistenceTestApplication.class)
@Import({PersistenceTestConfig.class, ConversationStoreAdapter.class})
@Transactional
class ConversationStoreAdapterTest {
    @Autowired ConversationStore store;
    @Autowired UserRepository users;
    @Autowired ModuleRepository modules;
    @Autowired TutorSessionRepository sessions;
    @Autowired ExerciseRepository exercises;
    @Autowired ConversationTurnRepository turns;
    @Autowired ExerciseRunRepository runs;

    TutorSession session() {
        var user = users.save(User.builder().email(UUID.randomUUID()+"@test.es").passwordHash("h").active(true).role(User.Role.ALUMNO).build());
        var module = modules.save(com.mathtutor.persistence.entity.Module.builder().code(UUID.randomUUID().toString()).name("Funciones").build());
        return sessions.save(TutorSession.builder().user(user).module(module).status(TutorSession.SessionStatus.ACTIVA).build());
    }
    @Test void persistsHistoryContextAndIdempotentResponse() {
        var s = session(); Long user = s.getUser().getId(); UUID id = UUID.randomUUID();
        assertThat(store.reserve(user,s.getId(),id,"hola").status()).isEqualTo("EN_CURSO");
        assertThatThrownBy(() -> store.reserve(user,s.getId(),UUID.randomUUID(),"otra")).isInstanceOf(ConversationConflictException.class);
        store.finish(user,s.getId(),id,new ConversationStore.Result("respuesta","tema",ErrorType.NINGUNO,"COMPLETADO",12,null,null,0,false));
        var replay = store.reserve(user,s.getId(),id,"hola");
        assertThat(replay.response()).isEqualTo("respuesta");
        assertThat(replay.tokensIn()).isNull();
        assertThat(store.context(user,s.getId())).hasSize(1);
        assertThat(store.history(user,s.getId(),0,10)).hasSize(1);
        assertThat(store.sessions(user,0,10)).hasSize(1);
        assertThat(store.runs(user,s.getId(),0,10)).isEmpty();
        assertThat(store.current(user,s.getId())).isNull();
        assertThatThrownBy(() -> store.reserve(user,s.getId(),id,"distinto")).isInstanceOf(ConversationConflictException.class);
    }
    @Test void protectsOwnershipAndTerminalStates() {
        var s = session(); var id = UUID.randomUUID(); Long user = s.getUser().getId();
        assertThatThrownBy(() -> store.history(-1L,s.getId(),0,10)).isInstanceOf(ConversationUnavailableException.class);
        store.reserve(user,s.getId(),id,"hola");
        store.finish(user,s.getId(),id,new ConversationStore.Result("","",ErrorType.OTRO,"CANCELADO",0,null,null,0,false));
        assertThat(store.reserve(user,s.getId(),id,"hola").status()).isEqualTo("CANCELADO");
        assertThat(store.context(user,s.getId())).isEmpty();
    }
    @Test void selectsFreeRunAndCompletesIt() {
        var s = session(); Long user = s.getUser().getId();
        var run = store.select(user,s.getId(),null,"Deriva x^2","2*x");
        assertThat(store.current(user,s.getId()).reference()).isEqualTo("2*x");
        var id = UUID.randomUUID(); store.reserve(user,s.getId(),id,"2*x");
        store.finish(user,s.getId(),id,new ConversationStore.Result("Correcto",null,ErrorType.NINGUNO,"COMPLETADO",1,null,null,0,true));
        assertThat(store.runs(user,s.getId(),0,10).getFirst().progress()).isEqualTo(1);
        assertThat(store.current(user,s.getId())).isNull();
        assertThat(run.id()).isNotNull();
    }
    @Test void bankSelectionValidatesModuleAndDoesNotVerifyTheory() {
        var s=session(); var other=session(); Long user=s.getUser().getId();
        var exercise=exercises.save(Exercise.builder().externalId(UUID.randomUUID().toString()).module(s.getModule())
                .statementLatex("Deriva").referenceSolutionLatex("2*x").type(Exercise.ExerciseType.PROCEDIMENTAL).difficulty(1).build());
        var selected=store.select(user,s.getId(),exercise.getId(),null,null);
        assertThat(selected.reference()).isEqualTo("2*x");
        assertThatThrownBy(() -> store.select(user,s.getId(),null,"libre","x"))
                .isInstanceOf(ConversationConflictException.class);
        assertThat(store.current(user, s.getId()).id()).isEqualTo(selected.id());
        exercise.setModule(other.getModule()); exercises.flush();
        assertThatThrownBy(() -> store.select(user,s.getId(),exercise.getId(),null,null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> store.select(user,s.getId(),-1L,null,null)).isInstanceOf(IllegalArgumentException.class);
        exercise.setModule(s.getModule());
        var request = UUID.randomUUID();
        store.reserve(user, s.getId(), request, "2*x");
        store.finish(user, s.getId(), request, new ConversationStore.Result(
                "Correcto", null, ErrorType.NINGUNO, "COMPLETADO", 1, null, null, 0, true));
        exercise.setType(Exercise.ExerciseType.TEORICO);
        exercises.flush();
        assertThat(store.select(user,s.getId(),exercise.getId(),null,null).reference()).isNull();
    }
    @Test void anInactiveExerciseCanResumeButStillBlocksStartingAnotherInTheSameChat() {
        var s = session();
        long user = s.getUser().getId();
        long original = store.select(user, s.getId(), null, "Deriva x^2", "2*x").id();
        var run = runs.findById(original).orElseThrow();
        run.setLastActivityAt(java.time.Instant.now().minus(java.time.Duration.ofHours(1)));
        runs.flush();
        assertThat(store.runs(user, s.getId(), 0, 10).getFirst().state()).isEqualTo("ABANDONADO");
        assertThatThrownBy(() -> store.select(user, s.getId(), null, "Deriva x^3", "3*x^2"))
                .isInstanceOf(ConversationConflictException.class);
        var request = UUID.randomUUID();
        assertThat(store.reserve(user, s.getId(), request, "2*x").runId()).isEqualTo(original);
        store.finish(user, s.getId(), request, new ConversationStore.Result(
                "Correcto", null, ErrorType.NINGUNO, "COMPLETADO", 1, null, null, 0, true));
        assertThat(store.select(user, s.getId(), null, "Deriva x^3", "3*x^2").id()).isNotEqualTo(original);
    }

    @Test void expiredReservationCannotOverwriteANewerTurn() {
        var s=session(); Long user=s.getUser().getId(); UUID old=UUID.randomUUID(),next=UUID.randomUUID();
        store.reserve(user,s.getId(),old,"antiguo");
        turns.findBySessionIdAndRequestId(s.getId(),old).orElseThrow().setCreatedAt(java.time.Instant.now().minusSeconds(121)); turns.flush();
        store.reserve(user,s.getId(),next,"nuevo");
        store.finish(user,s.getId(),old,new ConversationStore.Result("tardía",null,ErrorType.NINGUNO,"COMPLETADO",1,null,null,0,false));
        assertThat(store.reserve(user,s.getId(),old,"antiguo").status()).isEqualTo("ERROR");
        assertThatThrownBy(() -> store.select(user,s.getId(),null,"libre","x")).isInstanceOf(ConversationConflictException.class);
    }
    @Test void validatesPaginationAndClosedOrMissingSessions() {
        var s=session(); Long user=s.getUser().getId();
        for (int[] pair:new int[][]{{-1,1},{0,0},{0,101}})
            assertThatThrownBy(() -> store.history(user,s.getId(),pair[0],pair[1])).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> store.reserve(user,-1L,UUID.randomUUID(),"x")).isInstanceOf(ConversationUnavailableException.class);
        s.setStatus(TutorSession.SessionStatus.CERRADA); sessions.flush();
        assertThatThrownBy(() -> store.reserve(user,s.getId(),UUID.randomUUID(),"x")).isInstanceOf(ConversationUnavailableException.class);
    }
}
