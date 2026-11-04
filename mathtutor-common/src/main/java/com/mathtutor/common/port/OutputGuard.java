package com.mathtutor.common.port;

import com.mathtutor.common.dto.NoteChunk;

/**
 * Puerto para control de salida anti-revelación (UC-19).
 * Verifica que la respuesta del LLM no revela la solución directamente.
 */
public interface OutputGuard {

    /**
     * Verifica la respuesta del LLM contra la solución de referencia y chunks de contexto.
     *
     * @param response       respuesta del LLM
     * @param reference      solución de referencia
     * @param contextChunks  chunks de contexto usados
     * @return resultado de la verificación
     */
    LeakResult check(String response, String reference, java.util.List<NoteChunk> contextChunks);

    /**
     * Resultado de la verificación de fuga.
     */
    record LeakResult(boolean safe, String leakType, String details) {
        public static LeakResult ofSafe() {
            return new LeakResult(true, null, null);
        }

        public static LeakResult ofLeak(String leakType, String details) {
            return new LeakResult(false, leakType, details);
        }
    }
}