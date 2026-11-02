package com.mathtutor.common.port;

import com.mathtutor.common.dto.ExerciseRun;
import com.mathtutor.common.dto.ErrorType;
import com.mathtutor.common.dto.HelpLevel;
import com.mathtutor.common.dto.PedagogicalAction;

/**
 * Puerto para la política pedagógica determinista (orquestador determinista).
 * Decide el nivel de ayuda según estado del ejercicio, diagnóstico CAS e intentos.
 * No usa LLM - es código Java puro (máquina de estados).
 */
public interface PedagogicalPolicy {

    /**
     * Decide la acción pedagógica basada en el estado actual.
     *
     * @param exerciseRun  ejecución del ejercicio actual
     * @param diagnosis    diagnóstico del error (del CAS)
     * @param attempts     número de intentos previos
     * @return acción pedagógica a tomar
     */
    PedagogicalAction decide(ExerciseRun exerciseRun, ErrorType diagnosis, int attempts);
}