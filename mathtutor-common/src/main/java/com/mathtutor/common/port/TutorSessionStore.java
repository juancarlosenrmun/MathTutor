package com.mathtutor.common.port;

import com.mathtutor.common.dto.TutorModule;
import com.mathtutor.common.dto.TutorSessionInfo;
import java.util.List;
import java.util.Optional;

/** @brief Persistencia de sesiones; cada operación termina antes de invocar al LLM. */
public interface TutorSessionStore {
    /** @return Catálogo de bloques ordenado de forma estable. */
    List<TutorModule> modules();
    /** @return Sesión nueva con módulo inmutable y propietario autenticado. */
    TutorSessionInfo create(Long userId, String moduleCode);
    /** @return Vacío tanto para sesiones inexistentes como ajenas. */
    Optional<TutorSessionInfo> findOwned(Long sessionId, Long userId);
}
