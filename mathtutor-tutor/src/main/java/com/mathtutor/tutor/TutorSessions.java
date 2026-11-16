package com.mathtutor.tutor;

import com.mathtutor.common.dto.TutorModule;
import com.mathtutor.common.dto.TutorSessionInfo;
import com.mathtutor.common.port.TutorSessionStore;
import org.springframework.stereotype.Service;
import java.util.List;

/** @brief UC-12: selección de bloque previa al chat, sin modificar sesiones anteriores. */
@Service
public class TutorSessions {
    private static final List<String> MODULES = List.of("funciones", "derivadas-integrales", "metodos-3d");
    private final TutorSessionStore store;

    public TutorSessions(TutorSessionStore store) { this.store = store; }

    /** @return Bloques disponibles sin datos de otras conversaciones. */
    public List<TutorModule> modules() { return store.modules(); }

    /** @return Nueva sesión; repetir una selección nunca reutiliza otra conversación. */
    public TutorSessionInfo start(Long userId, String moduleCode) {
        if (moduleCode == null || !MODULES.contains(moduleCode)) {
            throw new IllegalArgumentException("Selecciona un módulo válido");
        }
        return store.create(userId, moduleCode);
    }

    /** @return Sesión activa autorizada antes de construir el flujo del proveedor. */
    public TutorSessionInfo active(Long userId, Long sessionId) {
        return store.findOwned(sessionId, userId).filter(s -> "ACTIVA".equals(s.status()))
                .orElseThrow(SessionUnavailableException::new);
    }
}
