package com.mathtutor.tutor;

import com.mathtutor.common.port.ConversationStore.BankExercise;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/** @brief UC-24. Ordena las etapas de práctica del módulo según RF-09. */
@Component
public class ProgressionPlanner {
    private static final List<String> STAGES = List.of("PROCEDIMENTAL", "TEORICO", "INTERPRETACION");

    /**
     * @brief Selecciona la primera etapa pendiente que tiene un ejercicio en el banco.
     * @param candidates Ejercicio inicial de cada tipo disponible en el módulo.
     * @param practicedTypes Etapas acreditadas por la actividad del alumno.
     * @return Ejercicio para la etapa siguiente, si queda alguna disponible.
     */
    public Optional<BankExercise> next(List<BankExercise> candidates, Set<String> practicedTypes) {
        return STAGES.stream()
                .filter(type -> !practicedTypes.contains(type))
                .flatMap(type -> candidates.stream().filter(exercise -> type.equals(exercise.type())))
                .findFirst();
    }
}
