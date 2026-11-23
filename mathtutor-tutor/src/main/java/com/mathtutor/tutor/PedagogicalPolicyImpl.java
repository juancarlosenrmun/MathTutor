package com.mathtutor.tutor;

import com.mathtutor.common.port.PedagogicalPolicy;
import com.mathtutor.common.dto.ExerciseRun;
import com.mathtutor.common.dto.ErrorType;
import com.mathtutor.common.dto.HelpLevel;
import com.mathtutor.common.dto.PedagogicalAction;
import org.springframework.stereotype.Component;

/**
 * @brief UC-18. Selecciona la ayuda con el diagnóstico y los intentos previos.
 */
@Component
public class PedagogicalPolicyImpl implements PedagogicalPolicy {

    @Override
    public PedagogicalAction decide(ExerciseRun exerciseRun, ErrorType diagnosis, int attempts) {
        return decide(exerciseRun != null && exerciseRun.state() == ExerciseRun.RunState.COMENZADO,
                diagnosis, attempts);
    }

    /** @param attempts Intentos previos del ejercicio, sin contar el turno actual. */
    PedagogicalAction decide(boolean active, ErrorType diagnosis, int attempts) {
        if (attempts <= 0) {
            return PedagogicalAction.conceptual("¿Qué concepto o propiedad relaciona los datos con lo que se pide?");
        }
        if (attempts == 1) {
            return PedagogicalAction.procedural(switch (diagnosis) {
                case SIGNO -> "¿En qué operación podría cambiar el signo?";
                case REGLA_CADENA -> "¿Cuál es la función interior y qué derivada aporta?";
                case REGLA_PRODUCTO -> "¿Cuáles son los dos factores que debes derivar?";
                default -> "¿Qué operación aplicarías para obtener el siguiente paso?";
            });
        }
        if (attempts >= 3 && active) {
            return PedagogicalAction.revelation("Puedes contrastar tu último intento con la referencia.");
        }
        return analogy();
    }

    static PedagogicalAction analogy() {
        return PedagogicalAction.analogy("¿Cómo trasladarías al ejercicio el primer paso de un problema más sencillo?",
                "Usa un problema con datos distintos y explica solo un paso transferible.");
    }
}
