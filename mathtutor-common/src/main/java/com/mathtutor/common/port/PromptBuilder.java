package com.mathtutor.common.port;

import com.mathtutor.common.dto.NoteChunk;
import com.mathtutor.common.dto.PedagogicalAction;
import com.mathtutor.common.dto.Prompt;

/**
 * Puerto para construcción del prompt que se envía al LLM.
 */
public interface PromptBuilder {

    /**
     * Construye el prompt para el LLM.
     *
     * @param action          acción pedagógica decidida
     * @param diagnosis       diagnóstico del error
     * @param contextChunks   chunks de contexto recuperados (RAG)
     * @param constraints     restricciones adicionales
     * @return prompt listo para enviar al LLM
     */
    Prompt build(PedagogicalAction action, String diagnosis, java.util.List<NoteChunk> contextChunks, String constraints);
}