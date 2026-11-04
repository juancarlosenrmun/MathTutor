package com.mathtutor.common.port;

import com.mathtutor.common.dto.Exercise;

import java.util.Optional;

/**
 * Puerto para reconocimiento de ejercicios del banco (UC-23).
 * Nunca usa LLM ni similitud semántica - solo coincidencia exacta/CAS.
 */
public interface ExerciseRecognizer {

    /**
     * Reconoce si el enunciado corresponde a un ejercicio del banco.
     *
     * @param statement enunciado en LaTeX
     * @return ejercicio si se reconoce, vacío en caso contrario
     */
    Optional<Exercise> recognize(String statement);
}