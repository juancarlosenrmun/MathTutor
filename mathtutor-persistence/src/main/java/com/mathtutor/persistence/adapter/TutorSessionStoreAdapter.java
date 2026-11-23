package com.mathtutor.persistence.adapter;

import com.mathtutor.common.dto.TutorModule;
import com.mathtutor.common.dto.TutorSessionInfo;
import com.mathtutor.common.port.TutorSessionStore;
import com.mathtutor.persistence.entity.TutorSession;
import com.mathtutor.persistence.entity.User;
import com.mathtutor.persistence.repository.ModuleRepository;
import com.mathtutor.persistence.repository.TutorSessionRepository;
import com.mathtutor.persistence.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

/** @brief Adaptador UC-12 con transacciones cortas y proyecciones sin carga diferida. */
@Component
@Transactional(readOnly = true)
public class TutorSessionStoreAdapter implements TutorSessionStore {
    private final TutorSessionRepository sessions;
    private final ModuleRepository modules;
    private final UserRepository users;

    public TutorSessionStoreAdapter(TutorSessionRepository sessions, ModuleRepository modules, UserRepository users) {
        this.sessions = sessions; this.modules = modules; this.users = users;
    }

    @Override public List<TutorModule> modules() {
        return modules.findAll(Sort.by("id")).stream().map(m -> new TutorModule(m.getCode(), m.getName())).toList();
    }

    @Override @Transactional
    public TutorSessionInfo create(Long userId, String moduleCode) {
        var module = modules.findByCode(moduleCode)
                .orElseThrow(() -> new IllegalArgumentException("Módulo no disponible"));
        var user = users.findByIdForUpdate(userId).filter(User::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Cuenta no disponible"));
        var session = TutorSession.builder().user(user).module(module)
                .status(TutorSession.SessionStatus.ACTIVA).build();
        return info(sessions.save(session));
    }

    @Override public Optional<TutorSessionInfo> findOwned(Long sessionId, Long userId) {
        return sessions.findByIdAndUserId(sessionId, userId).map(TutorSessionStoreAdapter::info);
    }

    private static TutorSessionInfo info(TutorSession session) {
        return new TutorSessionInfo(session.getId(), session.getModule().getCode(), session.getStatus().name());
    }
}
