package com.mathtutor.persistence.adapter;

import com.mathtutor.common.dto.*;
import com.mathtutor.common.port.ConversationStore;
import com.mathtutor.persistence.PersistenceTestApplication;
import com.mathtutor.persistence.PersistenceTestConfig;
import com.mathtutor.persistence.entity.*;
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
}
